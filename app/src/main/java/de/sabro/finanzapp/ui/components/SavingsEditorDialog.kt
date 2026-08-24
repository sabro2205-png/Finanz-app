package de.sabro.finanzapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.sabro.finanzapp.data.SavingsEntry
import de.sabro.finanzapp.domain.PotSummary
import de.sabro.finanzapp.ui.theme.PotColors
import de.sabro.finanzapp.util.Period
import de.sabro.finanzapp.util.formatAmountForInput
import androidx.compose.ui.unit.dp
import de.sabro.finanzapp.util.parseAmountToCents

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavingsEditorDialog(
    period: Int,
    existing: SavingsEntry?,
    pots: List<PotSummary>,
    preselectedPotId: Long?,
    onDismiss: () -> Unit,
    onSave: (potId: Long, title: String, amountCents: Long) -> Unit,
    onNewPot: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var potId by remember {
        mutableLongStateOf(
            existing?.potId ?: preselectedPotId ?: pots.firstOrNull()?.pot?.id ?: 0L
        )
    }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var amountText by remember {
        mutableStateOf(existing?.let { formatAmountForInput(it.amountCents) } ?: "")
    }
    var showError by remember { mutableStateOf(false) }

    val amountCents = parseAmountToCents(amountText)
    val valid = amountCents != null && amountCents != 0L && pots.any { it.pot.id == potId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Geld einzahlen" else "Buchung bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = Period.label(period),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Topf",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pots.forEach { summary ->
                            FilterChip(
                                selected = potId == summary.pot.id,
                                onClick = { potId = summary.pot.id },
                                label = { Text(summary.pot.name) },
                                leadingIcon = {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .background(
                                                PotColors[summary.pot.colorIndex % PotColors.size],
                                                RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                            )
                        }
                        AssistChip(onClick = onNewPot, label = { Text("+ Neuer Topf") })
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Betrag in €") },
                    placeholder = { Text("z. B. 250 – für eine Entnahme -250") },
                    singleLine = true,
                    isError = showError && (amountCents == null || amountCents == 0L),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Negative Beträge werden als Entnahme gewertet. " +
                        "Erspartes wird nicht mit Einnahmen oder Ausgaben verrechnet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showError && !valid) {
                    Text(
                        text = "Bitte einen Topf wählen und einen Betrag ungleich 0 eingeben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (existing != null && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Buchung löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (valid) onSave(potId, title.trim(), amountCents!!) else showError = true
            }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
