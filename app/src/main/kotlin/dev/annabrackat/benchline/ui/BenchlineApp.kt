package dev.annabrackat.benchline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.annabrackat.benchline.DomainResult
import dev.annabrackat.benchline.ExceptionKind
import dev.annabrackat.benchline.FillOrder
import dev.annabrackat.benchline.FillStatus
import dev.annabrackat.benchline.Formulary
import dev.annabrackat.benchline.Priority
import dev.annabrackat.benchline.Role
import dev.annabrackat.benchline.SeedData
import dev.annabrackat.benchline.Workflow
import dev.annabrackat.benchline.buildHandoff

class BenchViewModel : ViewModel() {
    var role by mutableStateOf(Role.TECHNICIAN)
        private set
    var orders by mutableStateOf(SeedData.orders(System.currentTimeMillis()))
        private set
    var selectedId by mutableStateOf<String?>("jp-stat")
        private set
    var tab by mutableIntStateOf(0)
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    fun toggleRole() {
        role = if (role == Role.TECHNICIAN) Role.PHARMACIST else Role.TECHNICIAN
        notice = null
    }

    fun select(id: String?) {
        selectedId = id
        notice = null
    }

    fun show(tabIndex: Int) {
        tab = tabIndex
    }

    fun advance(id: String) = apply(id) { order ->
        if (order.status == FillStatus.EXCEPTION) Workflow.resolveException(order)
        else Workflow.advance(order, role, Formulary.byId(order.drugId)!!)
    }

    fun verify(id: String, raw: String, ack: Boolean) = apply(id) { order ->
        Workflow.verifyNdc(order, raw, ack, Formulary.byId(order.drugId)!!)
    }

    fun count(id: String, value: Int) = apply(id) { Workflow.recordCount(it, value) }

    fun flag(id: String, kind: ExceptionKind) = apply(id) { Workflow.flagException(it, kind) }

    fun reset() {
        orders = SeedData.orders(System.currentTimeMillis())
        role = Role.TECHNICIAN
        selectedId = "jp-stat"
        notice = null
    }

    private fun apply(id: String, block: (FillOrder) -> DomainResult<FillOrder>) {
        val order = orders.find { it.id == id } ?: return
        when (val result = block(order)) {
            is DomainResult.Ok -> {
                orders = orders.map { if (it.id == id) result.value else it }
                notice = null
            }
            is DomainResult.Err -> notice = result.message
        }
    }
}

@Composable
fun BenchlineApp(model: BenchViewModel = viewModel()) {
    val open = model.orders.count { it.status != FillStatus.READY }
    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Benchline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("$open open · training data", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = model::toggleRole) {
                Text(if (model.role == Role.TECHNICIAN) "Tech" else "RPh")
            }
        }
        Column(Modifier.weight(1f)) {
            when (model.tab) {
                0 -> QueuePage(model)
                1 -> VerifyPage(model)
                else -> HandoffPage(model)
            }
        }
        Row(Modifier.fillMaxWidth().background(Surface)) {
            listOf("Queue", "Verify", "Handoff").forEachIndexed { index, label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f).clickable { model.show(index) }.padding(vertical = 16.dp),
                    color = if (model.tab == index) Green else Muted,
                    fontWeight = if (model.tab == index) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun QueuePage(model: BenchViewModel) {
    val selected = model.orders.find { it.id == model.selectedId }
    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "STAT prednisone is waiting on a pharmacist. Switch to RPh to release it.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(model.orders.sortedBy { if (it.status == FillStatus.READY) 2 else if (it.priority == Priority.STAT) 0 else 1 }) { order ->
            val drug = Formulary.byId(order.drugId)
            Column(
                Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(16.dp)).clickable { model.select(order.id) }.padding(12.dp),
            ) {
                Text(order.rxNumber, fontFamily = FontFamily.Monospace)
                Text(drug?.name ?: order.drugId, fontWeight = FontWeight.Medium)
                Text("${order.patientCode} · qty ${order.quantity} · ${order.status.name}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (selected != null) {
            item { OrderActions(model, selected) }
        }
    }
}

@Composable
private fun OrderActions(model: BenchViewModel, order: FillOrder) {
    val drug = Formulary.byId(order.drugId)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        Text(drug?.name ?: "", fontWeight = FontWeight.SemiBold)
        model.notice?.let { Text(it, color = Stat) }
        if (order.status == FillStatus.IN_FILL) {
            var draft by remember(order.id) { mutableStateOf(order.countedQuantity?.toString() ?: "") }
            OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Counted") })
            Button(onClick = { draft.toIntOrNull()?.let { model.count(order.id, it) } }) { Text("Save count") }
        }
        Button(
            onClick = { model.advance(order.id) },
            enabled = order.status != FillStatus.READY,
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Surface),
        ) {
            Text(actionLabel(order.status))
        }
        if (order.status != FillStatus.READY && order.status != FillStatus.EXCEPTION) {
            TextButton(onClick = { model.flag(order.id, ExceptionKind.INSURANCE_REJECT) }) {
                Text("Flag insurance reject")
            }
        }
    }
}

@Composable
private fun VerifyPage(model: BenchViewModel) {
    val active = model.orders.filter { it.status != FillStatus.READY }
    val order = active.find { it.id == model.selectedId } ?: active.firstOrNull()
    if (order == null) {
        Text("No open order.", Modifier.padding(16.dp))
        return
    }
    val drug = Formulary.byId(order.drugId) ?: return
    var raw by remember(order.id) { mutableStateOf("") }
    var ack by remember(order.id) { mutableStateOf(false) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${order.rxNumber} · ${drug.name}", fontWeight = FontWeight.Medium)
        TextButton(onClick = { raw = drug.ndc10 }) { Text("Use ${drug.ndc10}") }
        OutlinedTextField(value = raw, onValueChange = { raw = it }, label = { Text("NDC or barcode") }, modifier = Modifier.fillMaxWidth())
        if (drug.lasaWith != null) {
            val partner = Formulary.partnerOf(drug)?.name ?: "the pair"
            TextButton(onClick = { ack = !ack }) {
                Text(if (ack) "Compared with $partner" else "Tap after comparing with $partner")
            }
        }
        model.notice?.let { Text(it, color = Stat) }
        Button(
            onClick = { model.verify(order.id, raw, ack) },
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Surface),
        ) { Text("Verify product") }
    }
}

@Composable
private fun HandoffPage(model: BenchViewModel) {
    val brief = buildHandoff(model.orders)
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${brief.openCount} open · ${brief.readyCount} ready · ${brief.lasaOpen.size} LASA")
        Text(brief.spoken)
        TextButton(onClick = model::reset) { Text("Reset training bench") }
    }
}

private fun actionLabel(status: FillStatus) = when (status) {
    FillStatus.RECEIVED -> "Start data entry"
    FillStatus.DATA_ENTRY -> "Send to fill"
    FillStatus.IN_FILL -> "Send to pharmacist"
    FillStatus.PHARMACIST_CHECK -> "Release"
    FillStatus.EXCEPTION -> "Resolve exception"
    FillStatus.READY -> "Released"
}

private val Paper = Color(0xFFF3EFE6)
private val Surface = Color(0xFFFFFDF8)
private val Green = Color(0xFF1E4D3A)
private val Muted = Color(0xFF6F685F)
private val Line = Color(0xFFE3DCD0)
private val Stat = Color(0xFF8C2F2F)
