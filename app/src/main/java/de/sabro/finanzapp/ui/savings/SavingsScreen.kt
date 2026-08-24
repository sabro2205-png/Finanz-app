package de.sabro.finanzapp.ui.savings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.sabro.finanzapp.data.SavingsEntry
import de.sabro.finanzapp.domain.SavingsMonth
import de.sabro.finanzapp.domain.SavingsUiState
import de.sabro.finanzapp.ui.components.LegendDot
import de.sabro.finanzapp.ui.components.PeriodSwitcher
import de.sabro.finanzapp.ui.components.SavingsChart
import de.sabro.finanzapp.ui.components.StatBlock
import de.sabro.finanzapp.ui.theme.amountColor
import de.sabro.finanzapp.ui.theme.expenseColor
import de.sabro.finanzapp.ui.theme.incomeColor
import de.sabro.finanzapp.ui.theme.savingsColor
import de.sabro.finanzapp.util.Period
import de.sabro.finanzapp.util.formatMoney
import de.sabro.finanzapp.util.formatMoneySigned

@Composable
fun SavingsScreen(
    state: SavingsUiState,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onAddForMonth: (Int) -> Unit,
    onEntryClick: (SavingsEntry) -> Unit,
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
                subtitle = "Erspartes",
                onPrevious = onPreviousYear,
                onNext = onNextYear
            )
        }

        item { SavingsSummaryCard(state) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Entwicklung ${state.year}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    SavingsChart(
                        monthlyNet = state.months.map { it.netCents },
                        runningBalance = state.months.map { it.runningBalanceCents },
                        barColor = incomeColor(),
                        negativeBarColor = expenseColor(),
                        lineColor = savingsColor()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(incomeColor(), "Sparrate im Monat")
                        LegendDot(savingsColor(), "Gesamtstand")
                    }
                }
            }
        }

        item {
            Text(
                text = "Monate",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        for (month in state.months) {
            item(key = "savings-${month.period}") {
                SavingsMonthCard(
                    month = month,
                    onAdd = { onAddForMonth(month.period) },
                    onEntryClick = onEntryClick
                )
            }
        }

        item {
            Text(
                text = "Hinweis: Erspartes wird bewusst getrennt geführt und hat keinen " +
                    "Einfluss auf Einnahmen, Ausgaben oder das Monatsergebnis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SavingsSummaryCard(state: SavingsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Gespart in ${state.year}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = formatMoneySigned(state.yearTotalCents),
                style = MaterialTheme.typography.headlineMedium,
                color = amountColor(state.yearTotalCents)
            )

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth()) {
                StatBlock(
                    label = "Stand Jahresbeginn",
                    value = formatMoney(state.startBalanceCents),
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = "Stand Jahresende",
                    value = formatMoney(state.endBalanceCents),
                    valueColor = savingsColor(),
                    modifier = Modifier.weight(1f),
                    alignment = Alignment.End
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth()) {
                StatBlock(
                    label = "Ø pro Sparmonat",
                    value = formatMoney(state.averagePerMonth),
                    modifier = Modifier.weight(1f)
                )
                val best = state.bestMonth
                StatBlock(
                    label = "Bester Monat",
                    value = if (best == null) "–"
                    else "${Period.MONTH_SHORT[Period.monthOf(best.period) - 1]} · ${formatMoney(best.netCents)}",
                    modifier = Modifier.weight(1f),
                    alignment = Alignment.End
                )
            }
        }
    }
}

@Composable
private fun SavingsMonthCard(
    month: SavingsMonth,
    onAdd: () -> Unit,
    onEntryClick: (SavingsEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = Period.monthName(month.period),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Gesamtstand: ${formatMoney(month.runningBalanceCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (month.netCents == 0L) "–" else formatMoneySigned(month.netCents),
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor(month.netCents)
                )
                IconButton(onClick = onAdd) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Sparbetrag in ${Period.monthName(month.period)} hinzufügen"
                    )
                }
            }

            month.entries.forEach { entry ->
                HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEntryClick(entry) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = formatMoneySigned(entry.amountCents),
                        style = MaterialTheme.typography.bodyMedium,
                        color = amountColor(entry.amountCents)
                    )
                }
            }
        }
    }
}
