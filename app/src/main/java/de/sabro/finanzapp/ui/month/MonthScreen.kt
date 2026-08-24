package de.sabro.finanzapp.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.sabro.finanzapp.data.EntryType
import de.sabro.finanzapp.data.FinanceEntry
import de.sabro.finanzapp.domain.CategoryGroup
import de.sabro.finanzapp.domain.FinanceCalculator
import de.sabro.finanzapp.domain.MonthUiState
import de.sabro.finanzapp.ui.components.EmptyHint
import de.sabro.finanzapp.ui.components.PeriodSwitcher
import de.sabro.finanzapp.ui.components.ShareBar
import de.sabro.finanzapp.ui.components.StatBlock
import de.sabro.finanzapp.ui.theme.CategoryColors
import de.sabro.finanzapp.ui.theme.amountColor
import de.sabro.finanzapp.ui.theme.expenseColor
import de.sabro.finanzapp.ui.theme.incomeColor
import de.sabro.finanzapp.util.Period
import de.sabro.finanzapp.util.formatMoney
import de.sabro.finanzapp.util.formatMoneySigned
import de.sabro.finanzapp.util.percentOf

@Composable
fun MonthScreen(
    state: MonthUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: () -> Unit,
    onEntryClick: (FinanceEntry) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PeriodSwitcher(
                title = Period.label(state.period),
                subtitle = if (state.period == Period.current()) "Aktueller Monat" else null,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onTitleClick = onPickMonth
            )
        }

        item { BalanceCard(state) }

        if (state.income.isNotEmpty() || state.expenseGroups.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Einnahmen",
                    total = formatMoney(state.incomeTotal),
                    color = incomeColor()
                )
            }
            if (state.income.isEmpty()) {
                item { EmptyHint("Noch keine Einnahmen in diesem Monat.") }
            } else {
                item {
                    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column {
                            state.income.forEachIndexed { index, entry ->
                                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                                EntryRow(entry, state.period, incomeColor()) { onEntryClick(entry) }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Ausgaben",
                    total = formatMoney(state.expenseTotal),
                    color = expenseColor()
                )
            }
            if (state.expenseGroups.isEmpty()) {
                item { EmptyHint("Noch keine Ausgaben in diesem Monat.") }
            } else {
                itemsIndexedGroups(state, onEntryClick)
            }
        } else {
            item { EmptyMonthCard() }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedGroups(
    state: MonthUiState,
    onEntryClick: (FinanceEntry) -> Unit
) {
    state.expenseGroups.forEachIndexed { index, group ->
        item(key = "group-${group.category.name}") {
            CategoryCard(
                group = group,
                expenseTotal = state.expenseTotal,
                viewPeriod = state.period,
                accent = CategoryColors[index % CategoryColors.size],
                onEntryClick = onEntryClick
            )
        }
    }
}

@Composable
private fun BalanceCard(state: MonthUiState) {
    val balance = state.balance
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

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Bleibt übrig",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = formatMoney(balance),
                style = MaterialTheme.typography.headlineMedium,
                color = amountColor(balance)
            )

            Spacer(Modifier.height(10.dp))

            val usedFraction = if (state.incomeTotal > 0L) {
                (state.expenseTotal.toFloat() / state.incomeTotal.toFloat())
            } else if (state.expenseTotal > 0L) 1f else 0f

            ShareBar(
                fraction = usedFraction,
                color = if (usedFraction > 1f) expenseColor() else MaterialTheme.colorScheme.primary,
                height = 8.dp
            )

            Spacer(Modifier.height(6.dp))

            val hint = when {
                state.incomeTotal <= 0L -> "Noch keine Einnahmen erfasst."
                balance < 0L -> "Ausgaben übersteigen die Einnahmen um ${formatMoney(-balance)}."
                else -> "${percentOf(state.expenseTotal, state.incomeTotal)} % der Einnahmen sind verplant."
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (state.hasPreviousData) {
                Text(
                    text = "Gegenüber ${Period.monthName(state.period - 1)}: " +
                        formatMoneySigned(state.balanceDelta),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, total: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(total, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun CategoryCard(
    group: CategoryGroup,
    expenseTotal: Long,
    viewPeriod: Int,
    accent: Color,
    onEntryClick: (FinanceEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(accent, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(group.category.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${group.entries.size} Posten · " +
                            "${percentOf(group.totalCents, expenseTotal)} % der Ausgaben",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatMoney(group.totalCents),
                    style = MaterialTheme.typography.titleMedium,
                    color = expenseColor()
                )
            }

            Spacer(Modifier.height(8.dp))
            ShareBar(
                fraction = if (expenseTotal > 0L) group.totalCents.toFloat() / expenseTotal else 0f,
                color = accent,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(4.dp))

            group.entries.forEach { entry ->
                EntryRow(entry, viewPeriod, MaterialTheme.colorScheme.onSurface) { onEntryClick(entry) }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: FinanceEntry, viewPeriod: Int, accentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.recurring) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = rangeText(entry, viewPeriod),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = formatMoney(entry.amountCents),
            style = MaterialTheme.typography.bodyMedium,
            color = if (entry.type == EntryType.INCOME) accentColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyMonthCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Noch nichts erfasst", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Tippe unten rechts auf „+“, um eine Einnahme oder Ausgabe " +
                    "hinzuzufügen. Feste Posten wie Miete, Finanzierung oder Abos " +
                    "kannst du als „monatlich wiederkehrend“ anlegen – sie tauchen " +
                    "dann automatisch in jedem Folgemonat auf.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "seit Jan 2026" bzw. "Jan 2026 – Mai 2028 · noch 21×" */
private fun rangeText(entry: FinanceEntry, viewPeriod: Int): String {
    val end = entry.endPeriod ?: return "monatlich seit ${Period.shortLabel(entry.startPeriod)}"
    val span = "${Period.shortLabel(entry.startPeriod)} – ${Period.shortLabel(end)}"
    val left = FinanceCalculator.remainingMonths(entry, viewPeriod) ?: 0
    return if (left > 0) "$span · noch ${left}×" else span
}
