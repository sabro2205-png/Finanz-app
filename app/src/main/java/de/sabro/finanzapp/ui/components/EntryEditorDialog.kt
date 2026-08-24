package de.sabro.finanzapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import de.sabro.finanzapp.util.parseAmountToCents

data class EntryFormResult(
    val type: EntryType,
    val category: ExpenseCategory?,
    val title: String,
    val amountCents: Long,
    val recurring: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    var category by remember {
        mutableStateOf(existing?.category ?: ExpenseCategory.OTHER)
    }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var amountText by remember {
        mutableStateOf(existing?.let { formatAmountForInput(it.amountCents) } ?: "")
    }
    var recurring by remember { mutableStateOf(existing?.recurring ?: false) }
    var showError by remember { mutableStateOf(false) }

    val amountCents = parseAmountToCents(amountText)
    val valid = title.isNotBlank() && amountCents != null && amountCents > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Neuer Posten" else "Posten bearbeiten")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Period.label(period),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    EntryType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(index, EntryType.entries.size)
                        ) {
                            Text(option.label)
                        }
                    }
                }

                if (type == EntryType.EXPENSE) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Kategorie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            text = if (recurring)
                                "Gilt ab ${Period.label(period)} in jedem Monat"
                            else
                                "Gilt nur in ${Period.label(period)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = recurring, onCheckedChange = { recurring = it })
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
                            recurring = recurring
                        )
                    )
                } else {
                    showError = true
                }
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

private fun placeholderFor(type: EntryType, category: ExpenseCategory): String = when {
    type == EntryType.INCOME -> "z. B. Gehalt"
    category == ExpenseCategory.SUBSCRIPTION -> "z. B. Netflix"
    category == ExpenseCategory.FINANCING -> "z. B. Autokredit"
    category == ExpenseCategory.RENT -> "z. B. Warmmiete"
    else -> "z. B. Lebensmittel"
}
