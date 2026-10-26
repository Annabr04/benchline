package dev.annabrackat.benchline

/**
 * Training orders shared by the app and the domain tests.
 * Initials only. Not a patient record.
 */
object SeedData {
    fun orders(now: Long): List<FillOrder> {
        fun ago(minutes: Int) = now - minutes * 60_000L
        return listOf(
            FillOrder(
                "jp-stat", "1004790", "J.P.", "prednisone", 21, Priority.STAT,
                FillStatus.PHARMACIST_CHECK, ndcVerified = true, lasaAcknowledged = true,
                countedQuantity = 21, enteredAt = ago(7),
            ),
            FillOrder("ar-waiter", "1004812", "A.R.", "hydroxyzine", 30, Priority.WAITER, FillStatus.DATA_ENTRY, enteredAt = ago(18)),
            FillOrder("kn-waiter", "1004820", "K.N.", "atorvastatin", 30, Priority.WAITER, FillStatus.RECEIVED, enteredAt = ago(4)),
            FillOrder("mc-routine", "1004818", "M.C.", "lisinopril", 90, Priority.ROUTINE, FillStatus.IN_FILL, ndcVerified = true, enteredAt = ago(42)),
            FillOrder("dw-routine", "1004766", "D.W.", "trazodone", 30, Priority.ROUTINE, FillStatus.DATA_ENTRY, enteredAt = ago(55)),
            FillOrder(
                "sl-reject", "1004777", "S.L.", "metformin", 180, Priority.ROUTINE,
                FillStatus.EXCEPTION, statusBeforeException = FillStatus.DATA_ENTRY,
                exception = ExceptionKind.INSURANCE_REJECT, enteredAt = ago(130),
            ),
        )
    }
}
