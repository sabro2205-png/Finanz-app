package de.sabro.finanzapp.domain

import de.sabro.finanzapp.data.EntryType
import de.sabro.finanzapp.data.ExpenseCategory
import de.sabro.finanzapp.data.FinanceEntry
import de.sabro.finanzapp.data.SavingsEntry
import de.sabro.finanzapp.util.Period

data class CategoryGroup(
    val category: ExpenseCategory,
    val entries: List<FinanceEntry>,
    val totalCents: Long
)

data class MonthUiState(
    val period: Int = Period.current(),
    val income: List<FinanceEntry> = emptyList(),
    val expenseGroups: List<CategoryGroup> = emptyList(),
    val incomeTotal: Long = 0L,
    val expenseTotal: Long = 0L,
    val previousBalance: Long = 0L,
    val hasPreviousData: Boolean = false
) {
    /** Einnahmen minus Ausgaben – Erspartes fliesst hier bewusst nicht ein. */
    val balance: Long get() = incomeTotal - expenseTotal
    val balanceDelta: Long get() = balance - previousBalance
    val isEmpty: Boolean get() = income.isEmpty() && expenseGroups.isEmpty()
}

data class MonthSummary(
    val period: Int,
    val incomeCents: Long,
    val expenseCents: Long
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

data class YearUiState(
    val year: Int = Period.currentYear(),
    val months: List<MonthSummary> = emptyList(),
    val incomeTotal: Long = 0L,
    val expenseTotal: Long = 0L
) {
    val balanceTotal: Long get() = incomeTotal - expenseTotal
    /** Nur Monate mit Daten zaehlen fuer den Durchschnitt. */
    val activeMonths: Int get() = months.count { it.incomeCents != 0L || it.expenseCents != 0L }
    val averageBalance: Long get() = if (activeMonths == 0) 0L else balanceTotal / activeMonths
}

data class SavingsMonth(
    val period: Int,
    val entries: List<SavingsEntry>,
    val netCents: Long,
    /** Kumulierter Sparstand am Ende dieses Monats. */
    val runningBalanceCents: Long
)

/** Ein Punkt der Sparkurve: entweder erfasst oder hochgerechnet. */
data class SavingsPoint(
    val period: Int,
    val balanceCents: Long,
    val forecast: Boolean
)

data class SavingsForecast(
    /** Monate mit Einträgen, die nicht in der Zukunft liegen. */
    val basisMonths: Int = 0,
    val averageCents: Long = 0L,
    /** Künftige Monate des angezeigten Jahres ohne Eintrag. */
    val openMonths: Int = 0,
    val balanceTodayCents: Long = 0L,
    val points: List<SavingsPoint> = emptyList(),
    val endOfYearCents: Long = 0L,
    val inTwelveMonthsCents: Long = 0L
) {
    /** Nur anzeigen, wenn es eine Grundlage und etwas hochzurechnen gibt. */
    val available: Boolean get() = basisMonths > 0 && openMonths > 0
}

data class SavingsUiState(
    val year: Int = Period.currentYear(),
    val startBalanceCents: Long = 0L,
    val months: List<SavingsMonth> = emptyList(),
    val forecast: SavingsForecast = SavingsForecast()
) {
    val yearTotalCents: Long get() = months.sumOf { it.netCents }
    val endBalanceCents: Long get() = startBalanceCents + yearTotalCents
    val monthsWithEntries: Int get() = months.count { it.entries.isNotEmpty() }
    val averagePerMonth: Long
        get() = if (monthsWithEntries == 0) 0L else yearTotalCents / monthsWithEntries
    val bestMonth: SavingsMonth?
        get() = months.filter { it.entries.isNotEmpty() }.maxByOrNull { it.netCents }
}

/**
 * Reine Rechenlogik ohne Android-Abhaengigkeiten, damit sie sich
 * mit `./gradlew test` pruefen laesst.
 */
object FinanceCalculator {

    /** Gilt der Posten im angegebenen Monat? */
    fun isActiveIn(entry: FinanceEntry, period: Int): Boolean =
        entry.startPeriod <= period && (entry.endPeriod == null || entry.endPeriod >= period)

    fun buildMonthState(
        period: Int,
        current: List<FinanceEntry>,
        previous: List<FinanceEntry>
    ): MonthUiState {
        val income = current.filter { it.type == EntryType.INCOME }
        val expenses = current.filter { it.type == EntryType.EXPENSE }

        val groups = ExpenseCategory.ordered.mapNotNull { category ->
            val items = expenses.filter { (it.category ?: ExpenseCategory.OTHER) == category }
            if (items.isEmpty()) null
            else CategoryGroup(category, items, items.sumOf { it.amountCents })
        }

        val prevIncome = previous.filter { it.type == EntryType.INCOME }.sumOf { it.amountCents }
        val prevExpense = previous.filter { it.type == EntryType.EXPENSE }.sumOf { it.amountCents }

        return MonthUiState(
            period = period,
            income = income,
            expenseGroups = groups,
            incomeTotal = income.sumOf { it.amountCents },
            expenseTotal = expenses.sumOf { it.amountCents },
            previousBalance = prevIncome - prevExpense,
            hasPreviousData = previous.isNotEmpty()
        )
    }

    fun buildYearState(year: Int, entries: List<FinanceEntry>): YearUiState {
        val months = (1..12).map { month ->
            val period = Period.of(year, month)
            val active = entries.filter { isActiveIn(it, period) }
            MonthSummary(
                period = period,
                incomeCents = active.filter { it.type == EntryType.INCOME }.sumOf { it.amountCents },
                expenseCents = active.filter { it.type == EntryType.EXPENSE }.sumOf { it.amountCents }
            )
        }
        return YearUiState(
            year = year,
            months = months,
            incomeTotal = months.sumOf { it.incomeCents },
            expenseTotal = months.sumOf { it.expenseCents }
        )
    }

    fun buildSavingsState(
        year: Int,
        entries: List<SavingsEntry>,
        startBalanceCents: Long,
        allEntries: List<SavingsEntry> = entries,
        today: Int = Period.current()
    ): SavingsUiState {
        var running = startBalanceCents
        val months = (1..12).map { month ->
            val period = Period.of(year, month)
            val items = entries.filter { it.period == period }
            val net = items.sumOf { it.amountCents }
            running += net
            SavingsMonth(period, items, net, running)
        }
        return SavingsUiState(
            year = year,
            startBalanceCents = startBalanceCents,
            months = months,
            forecast = buildSavingsForecast(year, allEntries, today)
        )
    }

    /**
     * Prognose auf Basis des bisherigen Sparverhaltens.
     *
     * Grundlage ist der Durchschnitt über alle Monate, in denen bereits etwas
     * erfasst wurde und die nicht in der Zukunft liegen. Ein vergangener Monat
     * ohne Eintrag ist eine Tatsache (nichts gespart) und wird nicht
     * hochgerechnet – nur künftige Monate ohne Eintrag.
     */
    fun buildSavingsForecast(
        year: Int,
        allEntries: List<SavingsEntry>,
        today: Int = Period.current()
    ): SavingsForecast {
        val netByPeriod: Map<Int, Long> = allEntries
            .groupBy { it.period }
            .mapValues { (_, items) -> items.sumOf { it.amountCents } }

        val basis = netByPeriod.filterKeys { it <= today }
        val basisMonths = basis.size
        val averageCents =
            if (basisMonths == 0) 0L
            else Math.round(basis.values.sum().toDouble() / basisMonths)

        val balanceToday = allEntries.filter { it.period <= today }.sumOf { it.amountCents }

        var running = allEntries.filter { it.period < Period.firstOfYear(year) }.sumOf { it.amountCents }
        var openMonths = 0
        val points = (1..12).map { month ->
            val period = Period.of(year, month)
            val entered = netByPeriod[period]
            val isForecast = entered == null && period > today && basisMonths > 0
            when {
                entered != null -> running += entered
                isForecast -> { running += averageCents; openMonths++ }
            }
            SavingsPoint(period, running, isForecast)
        }

        return SavingsForecast(
            basisMonths = basisMonths,
            averageCents = averageCents,
            openMonths = openMonths,
            balanceTodayCents = balanceToday,
            points = points,
            endOfYearCents = points.last().balanceCents,
            inTwelveMonthsCents = balanceToday + averageCents * 12
        )
    }

    /** Anzahl Monate, die ein befristeter Posten ab [from] noch läuft. */
    fun remainingMonths(entry: FinanceEntry, from: Int): Int? {
        val end = entry.endPeriod ?: return null
        return (end - from + 1).coerceAtLeast(0)
    }
}
