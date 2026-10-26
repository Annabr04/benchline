package dev.annabrackat.benchline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DomainTest {
    private val lisinopril = Formulary.byId("lisinopril")!!
    private val hydroxyzine = Formulary.byId("hydroxyzine")!!
    private val prednisone = Formulary.byId("prednisone")!!

    @Test
    fun labelSegmentsPadToElevenDigits() {
        val four = Ndc.normalize("9901-1001-01") as DomainResult.Ok
        assertEquals("09901100101", four.value.ndc11)
        val product = Ndc.normalize("99002-040-10") as DomainResult.Ok
        assertEquals("99002004010", product.value.ndc11)
        val pack = Ndc.normalize("99003-2001-1") as DomainResult.Ok
        assertEquals("99003200101", pack.value.ndc11)
    }

    @Test
    fun rawTenDigitsAreRejected() {
        assertIs<DomainResult.Err>(Ndc.normalize("9901100101"))
    }

    @Test
    fun gtinSampleAndBarcodeRoundTrip() {
        assertTrue(Ndc.isValidGtin("036000291452"))
        assertFalse(Ndc.isValidGtin("036000291453"))
        val barcode = lisinopril.ndc11 + Ndc.checkDigit(lisinopril.ndc11)
        val scanned = Ndc.normalize(barcode) as DomainResult.Ok
        assertEquals(lisinopril.ndc11, scanned.value.ndc11)
        assertEquals("barcode", scanned.value.source)
        val flipped = lisinopril.ndc11 + ((Ndc.checkDigit(lisinopril.ndc11) + 1) % 10)
        assertIs<DomainResult.Err>(Ndc.normalize(flipped))
    }

    @Test
    fun receivedAdvancesOnlyToDataEntry() {
        val stepped = Workflow.advance(sample(FillStatus.RECEIVED), Role.TECHNICIAN, lisinopril)
        assertIs<DomainResult.Ok<FillOrder>>(stepped)
        assertEquals(FillStatus.DATA_ENTRY, stepped.value.status)
    }

    @Test
    fun fillRequiresVerificationAndLasaAck() {
        val blocked = Workflow.advance(sample(FillStatus.DATA_ENTRY), Role.TECHNICIAN, lisinopril)
        assertEquals("ndc", (blocked as DomainResult.Err).code)
        val lasa = Workflow.verifyNdc(
            sample(FillStatus.DATA_ENTRY, drugId = "hydroxyzine"),
            hydroxyzine.ndc10,
            false,
            hydroxyzine,
        )
        assertEquals("lasa", (lasa as DomainResult.Err).code)
        val acked = Workflow.verifyNdc(
            sample(FillStatus.DATA_ENTRY, drugId = "hydroxyzine"),
            hydroxyzine.ndc10,
            true,
            hydroxyzine,
        ) as DomainResult.Ok
        assertTrue(acked.value.ndcVerified)
        assertTrue(acked.value.lasaAcknowledged)
    }

    @Test
    fun countAndRoleGuardTheEndOfTheLine() {
        val short = Workflow.advance(
            sample(FillStatus.IN_FILL, quantity = 90, ndcVerified = true, countedQuantity = 89),
            Role.TECHNICIAN,
            lisinopril,
        )
        assertEquals("count", (short as DomainResult.Err).code)
        val tech = Workflow.advance(
            sample(
                FillStatus.PHARMACIST_CHECK,
                drugId = "prednisone",
                quantity = 21,
                ndcVerified = true,
                lasaAcknowledged = true,
                countedQuantity = 21,
            ),
            Role.TECHNICIAN,
            prednisone,
        )
        assertEquals("role", (tech as DomainResult.Err).code)
        val released = Workflow.advance(
            sample(
                FillStatus.PHARMACIST_CHECK,
                drugId = "prednisone",
                quantity = 21,
                ndcVerified = true,
                lasaAcknowledged = true,
                countedQuantity = 21,
            ),
            Role.PHARMACIST,
            prednisone,
        ) as DomainResult.Ok
        assertEquals(FillStatus.READY, released.value.status)
    }

    @Test
    fun exceptionRestoresThePreviousStatus() {
        val flagged = Workflow.flagException(sample(FillStatus.DATA_ENTRY), ExceptionKind.INSURANCE_REJECT)
        val restored = Workflow.resolveException((flagged as DomainResult.Ok).value) as DomainResult.Ok
        assertEquals(FillStatus.DATA_ENTRY, restored.value.status)
        assertEquals(null, restored.value.exception)
        assertIs<DomainResult.Err>(Workflow.recordCount(sample(FillStatus.RECEIVED), 90))
    }

    @Test
    fun handoffKeepsOpenStatAndExceptions() {
        val brief = buildHandoff(SeedData.orders(0))
        assertEquals(1, brief.statOpen.size)
        assertTrue(brief.statOpen.all { it.status != FillStatus.READY })
        assertEquals(1, brief.exceptions.size)
    }

    private fun sample(
        status: FillStatus,
        drugId: String = "lisinopril",
        quantity: Int = 30,
        ndcVerified: Boolean = false,
        lasaAcknowledged: Boolean = false,
        countedQuantity: Int? = null,
    ) = FillOrder(
        id = "t",
        rxNumber = "1000001",
        patientCode = "T.S.",
        drugId = drugId,
        quantity = quantity,
        priority = Priority.ROUTINE,
        status = status,
        ndcVerified = ndcVerified,
        lasaAcknowledged = lasaAcknowledged,
        countedQuantity = countedQuantity,
    )
}

