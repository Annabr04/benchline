package dev.annabrackat.benchline

enum class Role { TECHNICIAN, PHARMACIST }

enum class Priority { STAT, WAITER, ROUTINE }

enum class FillStatus {
    RECEIVED,
    DATA_ENTRY,
    IN_FILL,
    PHARMACIST_CHECK,
    READY,
    EXCEPTION,
}

enum class ExceptionKind {
    INSURANCE_REJECT,
    OUT_OF_STOCK,
    CLARIFICATION,
    LASA_HOLD,
}

data class FillOrder(
    val id: String,
    val rxNumber: String,
    val patientCode: String,
    val drugId: String,
    val quantity: Int,
    val priority: Priority,
    val status: FillStatus,
    val statusBeforeException: FillStatus? = null,
    val exception: ExceptionKind? = null,
    val ndcVerified: Boolean = false,
    val lasaAcknowledged: Boolean = false,
    val countedQuantity: Int? = null,
    val enteredAt: Long = 0L,
)

data class Drug(
    val id: String,
    val ndc10: String,
    val ndc11: String,
    val name: String,
    val strength: String,
    val lasaWith: String? = null,
)

sealed interface DomainResult<out T> {
    data class Ok<T>(val value: T) : DomainResult<T>
    data class Err(val code: String, val message: String) : DomainResult<Nothing>
}
