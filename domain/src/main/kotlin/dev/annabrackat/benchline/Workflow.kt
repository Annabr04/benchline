package dev.annabrackat.benchline

object Workflow {
    fun advance(order: FillOrder, role: Role, drug: Drug): DomainResult<FillOrder> {
        if (order.status == FillStatus.READY) {
            return err("terminal", "Ready orders are already off the line.")
        }
        if (order.status == FillStatus.EXCEPTION) {
            return err("terminal", "Resolve the exception before moving this order.")
        }
        if (order.status == FillStatus.DATA_ENTRY) {
            if (!order.ndcVerified) return err("ndc", "Verify the NDC before filling.")
            if (drug.lasaWith != null && !order.lasaAcknowledged) {
                return err("lasa", "Acknowledge the look-alike pair before filling.")
            }
        }
        if (order.status == FillStatus.IN_FILL && order.countedQuantity != order.quantity) {
            return err("count", "Counted quantity must match the prescribed quantity.")
        }
        if (order.status == FillStatus.PHARMACIST_CHECK && role != Role.PHARMACIST) {
            return err("role", "Only a pharmacist can release this order.")
        }
        val next = nextOf(order.status) ?: return err("terminal", "This order cannot move forward.")
        return DomainResult.Ok(order.copy(status = next))
    }

    fun flagException(order: FillOrder, kind: ExceptionKind): DomainResult<FillOrder> {
        if (order.status == FillStatus.READY) {
            return err("terminal", "Ready orders are already off the line.")
        }
        if (order.status == FillStatus.EXCEPTION) {
            return err("state", "This order is already in exception.")
        }
        return DomainResult.Ok(
            order.copy(
                statusBeforeException = order.status,
                status = FillStatus.EXCEPTION,
                exception = kind,
            ),
        )
    }

    fun resolveException(order: FillOrder): DomainResult<FillOrder> {
        val previous = order.statusBeforeException
        if (order.status != FillStatus.EXCEPTION || previous == null) {
            return err("state", "Only an order in exception can be resolved.")
        }
        return DomainResult.Ok(
            order.copy(status = previous, statusBeforeException = null, exception = null),
        )
    }

    fun verifyNdc(
        order: FillOrder,
        raw: String,
        acknowledgeLasa: Boolean,
        drug: Drug,
    ): DomainResult<FillOrder> {
        if (order.status == FillStatus.READY || order.status == FillStatus.EXCEPTION) {
            return err("terminal", "Verification waits until this order is back on the line.")
        }
        val parsed = Ndc.normalize(raw)
        if (parsed is DomainResult.Err) return parsed
        val code = (parsed as DomainResult.Ok).value
        if (code.ndc11 != drug.ndc11) {
            return err("mismatch", "Parsed ${code.display}, which is not the product on this order.")
        }
        if (drug.lasaWith != null && !acknowledgeLasa) {
            return err(
                "lasa",
                "This drug has a look-alike pair. Compare the bottle and acknowledge it before verifying.",
            )
        }
        return DomainResult.Ok(
            order.copy(ndcVerified = true, lasaAcknowledged = drug.lasaWith == null || acknowledgeLasa),
        )
    }

    fun recordCount(order: FillOrder, count: Int): DomainResult<FillOrder> {
        if (order.status != FillStatus.IN_FILL) {
            return err("state", "Count the product while the order is in fill.")
        }
        if (count < 0) return err("range", "Count must be a whole number, zero or more.")
        return DomainResult.Ok(order.copy(countedQuantity = count))
    }

    private fun nextOf(status: FillStatus): FillStatus? = when (status) {
        FillStatus.RECEIVED -> FillStatus.DATA_ENTRY
        FillStatus.DATA_ENTRY -> FillStatus.IN_FILL
        FillStatus.IN_FILL -> FillStatus.PHARMACIST_CHECK
        FillStatus.PHARMACIST_CHECK -> FillStatus.READY
        else -> null
    }

    private fun err(code: String, message: String) = DomainResult.Err(code, message)
}
