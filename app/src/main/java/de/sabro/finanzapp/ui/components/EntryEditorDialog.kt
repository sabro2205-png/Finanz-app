package de.sabro.finanzapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.sabro.finanzapp.data.EntryType
import de.sabro.finanzapp.data.ExpenseCategory
import de.sabro.finanzapp.data.FinanceEntry
import de.sabro.finanzapp.util.Period
import de.sabro.finanzapp.util.formatAmountForInput
import de.sabro.finanzapp.util.formatMoney
import de.sabro.finanzapp.util.parseAmountToCents

data class EntryFormResult(
    val type: EntryType,
    val category: ExpenseCategory?,
    val title: String,
    val amountCents: Long,
    val recurring: Boolean,
    val startPeriod: Int,
    /** null bedeutet: laeuft unbefristet weiter. */
    val endPeriod: Int?
)

private enum class Picking { NONE, START, END }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryEditorDialog(
    period: Int,
    existing: FinanceEntry?,
    initialType: EntryType = EntryType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (EntryFormResult) -> Unit,
    onDelete: (() -> Unit)? = null,
    onStopFromHere: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(existing?.type ?: initialType) }
    var category by remember { mutableStateOf(existing?.category ?: ExpenseCategory.OTHER) }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var amountText by remember {
        mutableStateOf(existing?.let { formatAmountForInput(it.amountCents) } ?: "")
    }
    var recurring by remember { mutableStateOf(existing?.recurring ?: false) }
    var startPeriod by remember { mutableIntStateOf(existing?.startPeriod ?: period) }
    // Bei einmaligen Posten laeuft das Ende immer mit dem Start mit.
    var endPeriod by remember { mutableStateOf(existing?.endPeriod ?: period) }
    var picking by remember { mutableStateOf(Picking.NONE) }
    var pickYear by remember { mutableIntStateOf(Period.yearOf(existing?.startPeriod ?: period)) }
    var showError by remember { mutableStateOf(false) }

    val amountCents = parseAmountToCents(amountText)
    val valid = title.isNotBlank() && amountCents != null && amountCents > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Neuer Posten" else "Posten bearbeiten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (recurring) "Wiederkehrender Posten" else Period.label(startPeriod),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    EntryType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(index, EntryType.entries.size)
                        ) { Text(option.label) }
                    }
                }

                if (type == EntryType.EXPENSE) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FieldLabel("Kategorie")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExpenseCategory.ordered.forEach { option ->
                                FilterChip(
                                    selected = category == option,
                                    onClick = { category = option },
                                    label = { Text(option.label) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bezeichnung") },
                    placeholder = { Text(placeholderFor(type, category)) },
                    singleLine = true,
                    isError = showError && title.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Betrag in €") },
                    placeholder = { Text("z. B. 49,99") },
                    singleLine = true,
                    isError = showError && (amountCents == null || amountCents <= 0L),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Monatlich wiederkehrend", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (recurring) "Läuft über mehrere Monate"
                                   else "Gilt nur in einem einzelnen Monat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = recurring,
                        onCheckedChange = {
                            recurring = it
                            // Einschalten: offenes Ende. Ausschalten: zurueck auf den Startmonat.
                            endPeriod = if (it) null else startPeriod
                            picking = Picking.NONE
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel(if (recurring) "Zeitraum" else "Monat")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RangeButton(
                            caption = if (recurring) "von" else "im Monat",
                            value = Period.label(startPeriod),
                            selected = picking == Picking.START,
                            modifier = Modifier.weight(1f)
                        ) {
                            picking = if (picking == Picking.START) Picking.NONE else Picking.START
                            if (picking == Picking.START) pickYear = Period.yearOf(startPeriod)
                        }

                        if (recurring) {
                            Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            RangeButton(
                                caption = "bis",
                                value = endPeriod?.let { Period.label(it) } ?: "unbefristet",
                                selected = picking == Picking.END,
                                modifier = Modifier.weight(1f)
                            ) {
                                picking = if (picking == Picking.END) Picking.NONE else Picking.END
                                if (picking == Picking.END) pickYear = Period.yearOf(endPeriod ?: startPeriod)
                            }
                        }
                    }

                    if (picking != Picking.NONE) {
                        MonthGridPicker(
                            year = pickYear,
                            selected = if (picking == Picking.START) startPeriod else endPeriod,
                            minimum = if (picking == Picking.END) startPeriod else null,
                            showOpenEnd = picking == Picking.END,
                            onYearChange = { pickYear = it },
                            onOpenEnd = { endPeriod = null; picking = Picking.NONE },
                            onSelect = { chosen ->
                                if (picking == Picking.START) {
                                    startPeriod = chosen
                                    if (!recurring) endPeriod = chosen
                                    else endPeriod?.let { if (it < chosen) endPeriod = chosen }
                                } else {
                                    endPeriod = chosen
                                }
                                picking = Picking.NONE
                            }
                        )
                    }

                    if (recurring) {
                        Text(
                            text = rangeInfo(startPeriod, endPeriod, amountCents),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showError && !valid) {
                    Text(
                        text = "Bitte Bezeichnung und einen Betrag größer als 0 eingeben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (existing != null && (onDelete != null || onStopFromHere != null)) {
                    Column(Modifier.padding(top = 4.dp)) {
                        if (onStopFromHere != null && existing.recurring) {
                            TextButton(onClick = onStopFromHere) {
                                Text("Ab ${Period.monthName(period)} beenden")
                            }
                        }
                        if (onDelete != null) {
                            TextButton(onClick = onDelete) {
                                Text(
                                    text = if (existing.recurring) "Komplett löschen (alle Monate)" else "Löschen",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (valid) {
                    onSave(
                        EntryFormResult(
                            type = type,
                            category = if (type == EntryType.EXPENSE) category else null,
                            title = title.trim(),
                            amountCents = amountCents!!,
                            recurring = recurring,
                            startPeriod = startPeriod,
                            endPeriod = if (recurring) endPeriod else startPeriod
                        )
                    )
                } else {
                    showError = true
                }
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RangeButton(
    caption: String,
    value: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = if (selected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = caption.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthGridPicker(
    year: Int,
    selected: Int?,
    minimum: Int?,
    showOpenEnd: Boolean,
    onYearChange: (Int) -> Unit,
    onOpenEnd: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodSwitcher(
                title = year.toString(),
                onPrevious = { onYearChange(year - 1) },
                onNext = { onYearChange(year + 1) }
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 4
            ) {
                (1..12).forEach { month ->
                    val candidate = Period.of(year, month)
                    FilterChip(
                        selected = candidate == selected,
                        enabled = minimum == null || candidate >= minimum,
                        onClick = { onSelect(candidate) },
                        label = { Text(Period.MONTH_SHORT[month - 1]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (showOpenEnd) {
                TextButton(onClick = onOpenEnd) { Text("Kein Ende – läuft unbefristet") }
            }
        }
    }
}

/** "Laufzeit 29 Monate · Gesamt 4.335,50 €" bzw. der Hinweis auf offenes Ende. */
private fun rangeInfo(startPeriod: Int, endPeriod: Int?, amountCents: Long?): String {
    if (endPeriod == null) return "Läuft ab ${Period.label(startPeriod)} unbefristet weiter."
    val months = endPeriod - startPeriod + 1
    val total = if (amountCents != null && amountCents > 0L)
        " · Gesamt ${formatMoney(amountCents * months)}" else ""
    return "Laufzeit $months ${if (months == 1) "Monat" else "Monate"}$total."
}

private fun placeholderFor(type: EntryType, category: ExpenseCategory): String = when {
    type == EntryType.INCOME -> "z. B. Gehalt"
    category == ExpenseCategory.SUBSCRIPTION -> "z. B. Netflix"
    category == ExpenseCategory.FINANCING -> "z. B. Autokredit"
    category == ExpenseCategory.RENT -> "z. B. Warmmiete"
    else -> "z. B. Lebensmittel"
}
