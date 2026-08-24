package de.sabro.finanzapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.sabro.finanzapp.data.SavingsEntry
import de.sabro.finanzapp.util.Period
import de.sabro.finanzapp.util.formatAmountForInput
import de.sabro.finanzapp.util.parseAmountToCents

@Composable
fun SavingsEditorDialog(
    period: Int,
    existing: SavingsEntry?,
    onDismiss: () -> Unit,
    onSave: (title: String, amountCents: Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var amountText by remember {
        mutableStateOf(existing?.let { formatAmountForInput(it.amountCents) } ?: "")
    }
    var showError by remember { mutableStateOf(false) }

    val amountCents = parseAmountToCents(amountText)
    val valid = title.isNotBlank() && amountCents != null && amountCents != 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Sparbetrag hinzufügen" else "Sparbetrag bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = Period.label(period),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bezeichnung") },
                    placeholder = { Text("z. B. Tagesgeld") },
                    singleLine = true,
                    isError = showError && title.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

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
                        text = "Bitte Bezeichnung und einen Betrag ungleich 0 eingeben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (existing != null && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (valid) onSave(title.trim(), amountCents!!) else showError = true
            }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
