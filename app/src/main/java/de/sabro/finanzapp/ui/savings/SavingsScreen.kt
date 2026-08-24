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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.remember
import de.sabro.finanzapp.data.SavingsPot
import de.sabro.finanzapp.domain.PotSummary
import de.sabro.finanzapp.ui.components.ShareBar
import de.sabro.finanzapp.ui.theme.PotColors

@Composable
fun SavingsScreen(
    state: SavingsUiState,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onAddForMonth: (Int) -> Unit,
    onEntryClick: (SavingsEntry) -> Unit,
    onSelectPot: (Long?) -> Unit,
    onEditPot: (PotSummary) -> Unit,
    onNewPot: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val potsById = remember(state.pots) { state.pots.associate { it.pot.id to it.pot } }

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

        item { PotFilterRow(state, onSelectPot, onNewPot) }

        item { SavingsSummaryCard(state) }

        if (state.potFilter == null && state.pots.isNotEmpty()) {
            item { PotsCard(state, onEditPot) }
        }

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
                        balancePoints = state.forecast.points,
                        barColor = incomeColor(),
                        negativeBarColor = expenseColor(),
                        lineColor = savingsColor()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LegendDot(incomeColor(), "Sparrate")
                        LegendDot(expenseColor(), "Entnahme")
                        LegendDot(savingsColor(), "Gesamtstand")
                        if (state.forecast.available) LegendDot(savingsColor(), "Prognose")
                    }
                }
            }
        }

        item { ForecastCard(state) }

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
                    potsById = potsById,
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
                text = state.filteredPot?.let { "${it.pot.name} · ${state.year}" }
                    ?: "Gespart in ${state.year}",
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
                    label = if (state.year < Period.currentYear()) "Stand Jahresende" else "Erfasster Stand",
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
    potsById: Map<Long, SavingsPot>,
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
                val pot = potsById[entry.potId]
                HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEntryClick(entry) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .background(
                                PotColors[(pot?.colorIndex ?: 0) % PotColors.size],
                                RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(Modifier.size(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = pot?.name ?: "Ohne Topf",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.title.isNotBlank()) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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

/** Hochrechnung auf Basis des bisherigen Sparverhaltens. */
@Composable
private fun ForecastCard(state: SavingsUiState) {
    val f = state.forecast

    if (f.basisMonths == 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Prognose", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Sobald du für ein paar Monate Sparbeträge erfasst hast, rechne ich dir " +
                        "hier hoch, wo du am Jahresende voraussichtlich stehst.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (!f.available) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Prognose", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Für ${state.year} sind alle Monate erfasst – es bleibt nichts " +
                        "hochzurechnen. Blättere auf ${state.year + 1}, um die Prognose zu sehen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Voraussichtlich Ende ${state.year}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "≈ ${formatMoney(f.endOfYearCents)}",
                style = MaterialTheme.typography.headlineMedium,
                color = amountColor(f.endOfYearCents)
            )
            Text(
                text = "Hochgerechnet für ${f.openMonths} " +
                    (if (f.openMonths == 1) "offenen Monat" else "offene Monate") +
                    " mit ${formatMoney(f.averageCents)} pro Monat – dem Durchschnitt aus " +
                    "${f.basisMonths} ${if (f.basisMonths == 1) "Monat" else "Monaten"}, " +
                    "in denen du bisher etwas erfasst hast.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth()) {
                StatBlock(
                    label = "In 12 Monaten",
                    value = "≈ ${formatMoney(f.inTwelveMonthsCents)}",
                    valueColor = savingsColor(),
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = "Ø Sparrate",
                    value = "${formatMoney(f.averageCents)} / Monat",
                    modifier = Modifier.weight(1f),
                    alignment = Alignment.End
                )
            }

            Text(
                text = "Reine Hochrechnung aus der Vergangenheit – keine Zusage. Ein einzelner " +
                    "Ausreißer nach oben oder unten verschiebt den Durchschnitt spürbar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/** Alle Töpfe oder einer davon. */
@Composable
private fun PotFilterRow(
    state: SavingsUiState,
    onSelectPot: (Long?) -> Unit,
    onNewPot: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = state.potFilter == null,
                onClick = { onSelectPot(null) },
                label = { Text("Alle Töpfe") }
            )
        }
        items(state.pots, key = { it.pot.id }) { summary ->
            FilterChip(
                selected = state.potFilter == summary.pot.id,
                onClick = { onSelectPot(summary.pot.id) },
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
        item {
            AssistChip(onClick = onNewPot, label = { Text("+ Topf") })
        }
    }
}

/** Aufteilung des Ersparten auf die Töpfe, mit Ziel-Fortschritt falls gesetzt. */
@Composable
private fun PotsCard(state: SavingsUiState, onEditPot: (PotSummary) -> Unit) {
    val positiveTotal = state.pots.sumOf { it.balanceCents.coerceAtLeast(0L) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Column(Modifier.padding(horizontal = 14.dp)) {
                Text("Aufteilung", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${state.pots.size} ${if (state.pots.size == 1) "Topf" else "Töpfe"} · " +
                        "gesamt ${formatMoney(state.potTotalCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (positiveTotal > 0L) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    state.pots.filter { it.balanceCents > 0L }.forEach { summary ->
                        Box(
                            Modifier
                                .weight(summary.balanceCents.toFloat())
                                .fillMaxHeight()
                                .background(
                                    PotColors[summary.pot.colorIndex % PotColors.size],
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            state.pots.forEachIndexed { index, summary ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp))
                PotRow(summary, onClick = { onEditPot(summary) })
            }
        }
    }
}

@Composable
private fun PotRow(summary: PotSummary, onClick: () -> Unit) {
    val color = PotColors[summary.pot.colorIndex % PotColors.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = summary.pot.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val target = summary.pot.targetCents
            if (target != null) {
                Text(
                    text = "Ziel ${formatMoney(target)} · ${summary.targetReachedPercent} % erreicht",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                ShareBar(
                    fraction = (summary.targetReachedPercent ?: 0) / 100f,
                    color = color,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    height = 5.dp
                )
            } else {
                Text(
                    text = "${summary.sharePercent} % des Ersparten",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = formatMoney(summary.balanceCents),
            style = MaterialTheme.typography.bodyMedium,
            color = if (summary.balanceCents < 0L) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
