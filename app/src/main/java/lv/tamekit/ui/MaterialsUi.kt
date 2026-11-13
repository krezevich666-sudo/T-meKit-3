package lv.tamekit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lv.tamekit.R
import lv.tamekit.data.Material
import lv.tamekit.data.MovementType
import lv.tamekit.domain.MoveResult
import lv.tamekit.domain.Parsing
import lv.tamekit.domain.StockCalculator
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date

private fun typeLabel(t: MovementType) = when (t) {
    MovementType.IN -> R.string.type_in
    MovementType.OUT -> R.string.type_out
    MovementType.ADJUSTMENT -> R.string.type_adj
}

@Composable
internal fun MaterialsScreen(vm: MainViewModel) {
    val list by vm.materials.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    var showAdd by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val shown = remember(list, query) {
        val q = query.trim()
        list.filter { it.name.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true) }
    }
    var detailId by remember { mutableStateOf<Long?>(null) }
    Box(Modifier.fillMaxSize()) {
        if (list.isEmpty()) Text(stringResource(R.string.materials_empty), Modifier.align(Alignment.Center))
        Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text(stringResource(R.string.search)) }, singleLine = true
        )
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.id }) { m ->
                val bal = balances[m.id] ?: BigDecimal.ZERO
                val low = m.minimumStock.signum() > 0 && bal <= m.minimumStock
                ListItem(
                    modifier = Modifier.clickable { detailId = m.id },
                    headlineContent = { Text(m.name) },
                    supportingContent = {
                        Text(
                            "${formatEur(m.purchasePriceCents, locale)}/${m.unit} · ${stringResource(R.string.stock)}: ${qty(bal)} ${m.unit}",
                            color = if (low) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    },
                    trailingContent = {
                        IconButton({ vm.removeMaterial(m) }) { Icon(Icons.Filled.Delete, contentDescription = null) }
                    }
                )
                HorizontalDivider()
            }
        }
        }
        FloatingActionButton({ showAdd = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
    if (showAdd) AddMaterialDialog(vm) { showAdd = false }
    list.firstOrNull { it.id == detailId }?.let { m ->
        StockDialog(vm, m, balances[m.id] ?: BigDecimal.ZERO) { detailId = null }
    }
}

@Composable
private fun AddMaterialDialog(vm: MainViewModel, onClose: () -> Unit) {
    val defaultUnit = stringResource(R.string.unit_default)
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(defaultUnit) }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val cents = Parsing.cents(price)
    val stockQ = if (stock.isBlank()) BigDecimal.ZERO else Parsing.decimal(stock)
    val minQ = if (min.isBlank()) BigDecimal.ZERO else Parsing.decimal(min)
    val valid = name.isNotBlank() && cents != null && stockQ != null && minQ != null
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.add_material)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Field(name, { name = it }, R.string.name, error = tried && name.isBlank())
                Field(unit, { unit = it }, R.string.unit)
                Field(price, { price = it }, R.string.price, decimal = true, error = tried && cents == null)
                Field(stock, { stock = it }, R.string.stock, decimal = true, error = tried && stockQ == null)
                Field(min, { min = it }, R.string.min_stock, decimal = true, error = tried && minQ == null)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (valid) {
                    vm.addMaterial(
                        Material(
                            name = name.trim(), unit = unit.trim().ifBlank { defaultUnit },
                            purchasePriceCents = cents!!, minimumStock = minQ!!
                        ),
                        stockQ!!
                    )
                    onClose()
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClose) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun StockDialog(vm: MainViewModel, m: Material, balance: BigDecimal, onClose: () -> Unit) {
    val moves by vm.movements.collectAsStateWithLifecycle()
    val history = moves.filter { it.materialId == m.id }.take(15)
    val locale = LocalConfiguration.current.locales[0]
    val df = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale) }
    var type by remember { mutableStateOf(MovementType.IN) }
    var q by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val value = Parsing.decimal(q)
    val tooMuch = type == MovementType.OUT && value != null && value > balance
    val valid = value != null && StockCalculator.isValidEntry(type, value)
    val ok = valid && !tooMuch
    var rejected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
        title = { Text(m.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${stringResource(R.string.stock)}: ${qty(balance)} ${m.unit}", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MovementType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t, onClick = { type = t }, modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    stringResource(typeLabel(t)), maxLines = 1, softWrap = false, fontSize = 11.sp,
                                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                }
                Field(
                    q, { q = it },
                    if (type == MovementType.ADJUSTMENT) R.string.new_balance else R.string.quantity,
                    decimal = true, error = tried && !valid
                )
                if ((tried && tooMuch) || rejected) Text(stringResource(R.string.not_enough), color = MaterialTheme.colorScheme.error)
                Field(note, { note = it }, R.string.note)
                Button(onClick = {
                    tried = true
                    if (ok) {
                        scope.launch { if (vm.move(m, type, value!!, note.trim()) == MoveResult.OK) { q = ""; note = ""; tried = false; rejected = false } else rejected = true }
                        // lauki tiek notīrīti pēc veiksmīgas kustības
                    }
                }) { Text(stringResource(R.string.save)) }
                HorizontalDivider()
                history.forEach { h ->
                    val base = if (h.description == "OPENING") stringResource(R.string.opening) else stringResource(typeLabel(h.movementType))
                    val extra = if (h.description.isNotBlank() && h.description != "OPENING") " · ${h.description}" else ""
                    val sign = if (h.quantity.signum() > 0) "+" else ""
                    Text(
                        "${df.format(Date(h.createdAt))} · $base$extra: $sign${qty(h.quantity)} ${m.unit}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = { TextButton(onClose) { Text(stringResource(R.string.close)) } }
    )
}
