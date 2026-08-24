package de.sabro.finanzapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import de.sabro.finanzapp.data.SavingsPot
import de.sabro.finanzapp.domain.PotSummary
import de.sabro.finanzapp.ui.theme.PotColors
import de.sabro.finanzapp.util.formatAmountForInput
import de.sabro.finanzapp.util.formatMoney
import de.sabro.finanzapp.util.parseAmountToCents

/**
 * Anlegen und Aendern eines Spartopfes.
 *
 * [onSaved] liefert den Namen, die Farbe und ein freiwilliges Ziel.
 * [onDelete] bekommt die Ziel-Topf-Id, in die die Buchungen wandern sollen,
 * oder null, wenn sie mitgeloescht werden.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PotEditorDialog(
    existing: PotSummary?,
    otherPots: List<PotSummary>,
    bookingCount: Int,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSaved: (name: String, colorIndex: Int, targetCents: Long?) -> Unit,
    onDelete: ((moveToPotId: Long?) -> Unit)? = null
) {
    var name by remember { mutableStateOf(existing?.pot?.name.orEmpty()) }
    var colorIndex by remember { mutableIntStateOf(existing?.pot?.colorIndex ?: 0) }
    var hasTarget by remember { mutableStateOf(existing?.pot?.targetCents != null) }
    var targetText by remember {
        mutableStateOf(existing?.pot?.targetCents?.let { formatAmountForInput(it) } ?: "")
    }
    var moveTo by remember { mutableStateOf<Long?>(null) }
    var deleteArmed by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val targetCents = parseAmountToCents(targetText)
    val valid = name.isNotBlank() && (!hasTarget || (targetCents != null && targetCents > 0L))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Neuer Topf" else "Topf bearbeiten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (existing == null)
                        "Zum Beispiel Notgroschen, Urlaubskasse oder Neues Auto"
                    else
                        "Enthält ${formatMoney(existing.balanceCents)} aus $bookingCount " +
                            if (bookingCount == 1) "Buchung" else "Buchungen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z. B. Notgroschen") },
                    singleLine = true,
                    isError = showError && name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Farbe",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PotColors.indices.forEach { index ->
                            val selected = colorIndex == index
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        RoundedCornerShape(11.dp)
                                    )
                                    .clickable { colorIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(20.dp)
                                        .background(PotColors[index], RoundedCornerShape(7.dp))
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Sparziel", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (hasTarget) "Zeigt den Fortschritt zum Ziel"
                                   else "Ohne Ziel – der Topf sammelt einfach",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = hasTarget, onCheckedChange = { hasTarget = it })
                }

                if (hasTarget) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Zielbetrag in €") },
                        placeholder = { Text("z. B. 3.000") },
                        singleLine = true,
                        isError = showError && !(targetCents != null && targetCents > 0L),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showError && !valid) {
                    Text(
                        text = "Bitte einen Namen angeben" +
                            if (hasTarget) " und ein Ziel größer als 0." else ".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (existing != null && onDelete != null && canDelete) {
                    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (bookingCount > 0) {
                            Text(
                                text = "Beim Löschen " +
                                    (if (bookingCount == 1) "die Buchung" else "die $bookingCount Buchungen") +
                                    " verschieben nach:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                otherPots.forEach { other ->
                                    FilterChip(
                                        selected = moveTo == other.pot.id,
                                        onClick = { moveTo = other.pot.id; deleteArmed = true },
                                        label = { Text(other.pot.name) },
                                        leadingIcon = {
                                            Box(
                                                Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        PotColors[other.pot.colorIndex % PotColors.size],
                                                        RoundedCornerShape(3.dp)
                                                    )
                                            )
                                        }
                                    )
                                }
                                FilterChip(
                                    selected = deleteArmed && moveTo == null,
                                    onClick = { moveTo = null; deleteArmed = true },
                                    label = { Text("Mit löschen") }
                                )
                            }
                        }
                        TextButton(
                            enabled = bookingCount == 0 || deleteArmed,
                            onClick = { onDelete(moveTo) }
                        ) {
                            Text("Topf löschen", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else if (existing != null && !canDelete) {
                    Text(
                        text = "Der letzte Topf lässt sich nicht löschen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (valid) onSaved(name.trim(), colorIndex, if (hasTarget) targetCents else null)
                else showError = true
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
