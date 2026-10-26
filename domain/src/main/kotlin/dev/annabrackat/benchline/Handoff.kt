package dev.annabrackat.benchline

data class HandoffBrief(
    val openCount: Int,
    val readyCount: Int,
    val statOpen: List<FillOrder>,
    val waiterOpen: List<FillOrder>,
    val exceptions: List<FillOrder>,
    val lasaOpen: List<FillOrder>,
    val spoken: String,
)

fun buildHandoff(orders: List<FillOrder>): HandoffBrief {
    val open = orders.filter { it.status != FillStatus.READY }
    val statOpen = open.filter { it.priority == Priority.STAT }
    val waiterOpen = open.filter { it.priority == Priority.WAITER }
    val exceptions = orders.filter { it.status == FillStatus.EXCEPTION }
    val lasaOpen = open.filter { Formulary.byId(it.drugId)?.lasaWith != null }
    val orderWord = if (open.size == 1) "order" else "orders"
    val waiterWord = if (waiterOpen.size == 1) "waiter" else "waiters"
    val exceptionWord = if (exceptions.size == 1) "exception" else "exceptions"
    val lasaWord = if (lasaOpen.size == 1) "pair is" else "pairs are"
    val spoken = "Benchline handoff. ${open.size} $orderWord still open, including ${statOpen.size} STAT and ${waiterOpen.size} $waiterWord. " +
        "${exceptions.size} $exceptionWord need a follow-up. ${lasaOpen.size} look-alike $lasaWord still on the line. " +
        "Initials only. Nothing in this handoff is a real patient record."
    return HandoffBrief(open.size, orders.size - open.size, statOpen, waiterOpen, exceptions, lasaOpen, spoken)
}
