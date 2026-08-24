package de.sabro.finanzapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.sabro.finanzapp.util.Period

/** Schnellauswahl von Monat und Jahr. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthPickerDialog(
    period: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    var year by remember { mutableIntStateOf(Period.yearOf(period)) }
    val selectedYear = Period.yearOf(period)
    val selectedMonth = Period.monthOf(period)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monat wählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodSwitcher(
                    title = year.toString(),
                    onPrevious = { year -= 1 },
                    onNext = { year += 1 }
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4
                ) {
                    for (month in 1..12) {
                        FilterChip(
                            selected = year == selectedYear && month == selectedMonth,
                            onClick = { onSelect(Period.of(year, month)) },
                            label = { Text(Period.MONTH_SHORT[month - 1]) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(Period.current()) }) { Text("Heute") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
