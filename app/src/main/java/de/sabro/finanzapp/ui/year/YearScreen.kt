package de.sabro.finanzapp.ui.year

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.sabro.finanzapp.domain.MonthSummary
import de.sabro.finanzapp.domain.YearUiState
import de.sabro.finanzapp.ui.components.GroupedYearBarChart
import de.sabro.finanzapp.ui.components.LegendDot
import de.sabro.finanzapp.ui.components.PeriodSwitcher
import de.sabro.finanzapp.ui.components.StatBlock
import de.sabro.finanzapp.ui.theme.amountColor
import de.sabro.finanzapp.ui.theme.expenseColor
import de.sabro.finanzapp.ui.theme.incomeColor
import de.sabro.finanzapp.util.Period
import de.sabro.finanzapp.util.formatMoney

@Composable
fun YearScreen(
    state: YearUiState,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthClick: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PeriodSwitcher(
                title = state.year.toString(),
                subtitle = "Jahresvergleich",
                onPrevious = onPreviousYear,
                onNext = onNextYear
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        StatBlock(
                            label = "Einnahmen",
                            value = formatMoney(state.incomeTotal),
                            valueColor = incomeColor(),
                            modifier = Modifier.weight(1f)
                        )
                        StatBlock(
                            label = "Ausgaben",
                            value = formatMoney(state.expenseTotal),
                            valueColor = expenseColor(),
                            modifier = Modifier.weight(1f),
                            alignment = Alignment.End
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        StatBlock(
                            label = "Ergebnis ${state.year}",
                            value = formatMoney(state.balanceTotal),
                            valueColor = amountColor(state.balanceTotal),
                            modifier = Modifier.weight(1f)
                        )
                        StatBlock(
                            label = "Ø pro aktivem Monat",
                            value = formatMoney(state.averageBalance),
                            valueColor = amountColor(state.averageBalance),
                            modifier = Modifier.weight(1f),
                            alignment = Alignment.End
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Monate im Vergleich", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    GroupedYearBarChart(
                        seriesA = state.months.map { it.incomeCents },
                        seriesB = state.months.map { it.expenseCents },
                        colorA = incomeColor(),
                        colorB = expenseColor()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(incomeColor(), "Einnahmen")
                        LegendDot(expenseColor(), "Ausgaben")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    TableHeader()
                    state.months.forEach { month ->
                        HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                        MonthRow(month) { onMonthClick(month.period) }
                    }
                }
            }
        }

        item {
            Text(
                text = "Tippe auf einen Monat, um ihn im Reiter „Monat“ zu öffnen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        HeaderCell("Monat", Modifier.weight(1f), TextAlign.Start)
        HeaderCell("Ein", Modifier.weight(1f), TextAlign.End)
        HeaderCell("Aus", Modifier.weight(1f), TextAlign.End)
        HeaderCell("Rest", Modifier.weight(1f), TextAlign.End)
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        modifier = modifier
    )
}

@Composable
private fun MonthRow(month: MonthSummary, onClick: () -> Unit) {
    val isCurrent = month.period == Period.current()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Period.MONTH_SHORT[Period.monthOf(month.period) - 1] + if (isCurrent) " •" else "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatMoney(month.incomeCents),
            style = MaterialTheme.typography.bodySmall,
            color = incomeColor(),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatMoney(month.expenseCents),
            style = MaterialTheme.typography.bodySmall,
            color = expenseColor(),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatMoney(month.balanceCents),
            style = MaterialTheme.typography.bodySmall,
            color = amountColor(month.balanceCents),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
