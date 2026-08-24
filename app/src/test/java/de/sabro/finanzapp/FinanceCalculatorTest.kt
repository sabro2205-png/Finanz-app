package de.sabro.finanzapp

import de.sabro.finanzapp.data.EntryType
import de.sabro.finanzapp.data.ExpenseCategory
import de.sabro.finanzapp.data.FinanceEntry
import de.sabro.finanzapp.data.SavingsEntry
import de.sabro.finanzapp.domain.FinanceCalculator
import de.sabro.finanzapp.util.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculatorTest {

    private val jan = Period.of(2026, 1)
    private val feb = Period.of(2026, 2)
    private val mar = Period.of(2026, 3)

    private fun income(amount: Long, start: Int, recurring: Boolean = true) = FinanceEntry(
        type = EntryType.INCOME,
        category = null,
        title = "Gehalt",
        amountCents = amount,
        startPeriod = start,
        endPeriod = if (recurring) null else start,
        recurring = recurring
    )

    private fun expense(
        amount: Long,
        start: Int,
        category: ExpenseCategory,
        title: String = "Posten",
        recurring: Boolean = true,
        end: Int? = null
    ) = FinanceEntry(
        type = EntryType.EXPENSE,
        category = category,
        title = title,
        amountCents = amount,
        startPeriod = start,
        endPeriod = if (recurring) end else start,
        recurring = recurring
    )

    // ------------------------------------------------------------ Monat

    @Test
    fun `Ausgaben werden von den Einnahmen abgezogen`() {
        val entries = listOf(
            income(300_000, jan),
            expense(90_000, jan, ExpenseCategory.RENT),
            expense(25_000, jan, ExpenseCategory.FINANCING),
            expense(1_999, jan, ExpenseCategory.SUBSCRIPTION),
            expense(40_000, jan, ExpenseCategory.OTHER)
        )

        val state = FinanceCalculator.buildMonthState(jan, entries, emptyList())

        assertEquals(300_000L, state.incomeTotal)
        assertEquals(156_999L, state.expenseTotal)
        assertEquals(143_001L, state.balance)
    }

    @Test
    fun `Ausgaben werden nach Kategorie gestaffelt`() {
        val entries = listOf(
            expense(90_000, jan, ExpenseCategory.RENT, "Warmmiete"),
            expense(1_299, jan, ExpenseCategory.SUBSCRIPTION, "Netflix"),
            expense(999, jan, ExpenseCategory.SUBSCRIPTION, "Spotify")
        )

        val groups = FinanceCalculator.buildMonthState(jan, entries, emptyList()).expenseGroups

        // Reihenfolge folgt ExpenseCategory.ordered: Miete vor Abos
        assertEquals(listOf(ExpenseCategory.RENT, ExpenseCategory.SUBSCRIPTION), groups.map { it.category })
        assertEquals(90_000L, groups[0].totalCents)
        assertEquals(2_298L, groups[1].totalCents)
        assertEquals(2, groups[1].entries.size)
    }

    @Test
    fun `Vergleich mit dem Vormonat`() {
        val current = listOf(income(300_000, feb), expense(100_000, feb, ExpenseCategory.RENT))
        val previous = listOf(income(300_000, jan), expense(150_000, jan, ExpenseCategory.RENT))

        val state = FinanceCalculator.buildMonthState(feb, current, previous)

        assertEquals(200_000L, state.balance)
        assertEquals(150_000L, state.previousBalance)
        assertEquals(50_000L, state.balanceDelta)
        assertTrue(state.hasPreviousData)
    }

    @Test
    fun `Ausgaben ohne Kategorie zaehlen zu Sonstige`() {
        val ohneKategorie = FinanceEntry(
            type = EntryType.EXPENSE,
            category = null,
            title = "Unbekannt",
            amountCents = 5_000,
            startPeriod = jan,
            endPeriod = jan,
            recurring = false
        )

        val groups = FinanceCalculator.buildMonthState(jan, listOf(ohneKategorie), emptyList()).expenseGroups

        assertEquals(listOf(ExpenseCategory.OTHER), groups.map { it.category })
        assertEquals(5_000L, groups[0].totalCents)
    }

    // ------------------------------------------------------------ Gueltigkeit

    @Test
    fun `laufender Posten gilt auch in Folgemonaten`() {
        val miete = expense(90_000, jan, ExpenseCategory.RENT)

        assertTrue(FinanceCalculator.isActiveIn(miete, jan))
        assertTrue(FinanceCalculator.isActiveIn(miete, mar))
        assertFalse(FinanceCalculator.isActiveIn(miete, jan - 1))
    }

    @Test
    fun `einmaliger Posten gilt nur in seinem Monat`() {
        val einmalig = expense(5_000, feb, ExpenseCategory.OTHER, recurring = false)

        assertFalse(FinanceCalculator.isActiveIn(einmalig, jan))
        assertTrue(FinanceCalculator.isActiveIn(einmalig, feb))
        assertFalse(FinanceCalculator.isActiveIn(einmalig, mar))
    }

    @Test
    fun `beendeter Posten gilt nach dem Enddatum nicht mehr`() {
        val abo = expense(1_299, jan, ExpenseCategory.SUBSCRIPTION, end = feb)

        assertTrue(FinanceCalculator.isActiveIn(abo, jan))
        assertTrue(FinanceCalculator.isActiveIn(abo, feb))
        assertFalse(FinanceCalculator.isActiveIn(abo, mar))
    }

    // ------------------------------------------------------------ Jahr

    @Test
    fun `Jahresuebersicht verteilt laufende Posten auf alle Monate`() {
        val entries = listOf(
            income(300_000, jan),
            expense(90_000, jan, ExpenseCategory.RENT)
        )

        val year = FinanceCalculator.buildYearState(2026, entries)

        assertEquals(12, year.months.size)
        assertEquals(12 * 300_000L, year.incomeTotal)
        assertEquals(12 * 90_000L, year.expenseTotal)
        assertEquals(12 * 210_000L, year.balanceTotal)
        assertEquals(12, year.activeMonths)
        assertEquals(210_000L, year.averageBalance)
    }

    @Test
    fun `Jahresuebersicht beruecksichtigt den Startmonat`() {
        val entries = listOf(income(200_000, Period.of(2026, 7)))

        val year = FinanceCalculator.buildYearState(2026, entries)

        assertEquals(0L, year.months[5].incomeCents)   // Juni
        assertEquals(200_000L, year.months[6].incomeCents) // Juli
        assertEquals(6 * 200_000L, year.incomeTotal)
        assertEquals(6, year.activeMonths)
    }

    @Test
    fun `leeres Jahr fuehrt nicht zu Division durch Null`() {
        val year = FinanceCalculator.buildYearState(2026, emptyList())

        assertEquals(0, year.activeMonths)
        assertEquals(0L, year.averageBalance)
    }

    // ------------------------------------------------------------ Erspartes

    @Test
    fun `Erspartes wird ueber das Jahr kumuliert`() {
        val entries = listOf(
            SavingsEntry(period = jan, title = "Tagesgeld", amountCents = 20_000),
            SavingsEntry(period = jan, title = "ETF", amountCents = 10_000),
            SavingsEntry(period = mar, title = "Tagesgeld", amountCents = 15_000)
        )

        val state = FinanceCalculator.buildSavingsState(2026, entries, startBalanceCents = 100_000)

        assertEquals(30_000L, state.months[0].netCents)
        assertEquals(130_000L, state.months[0].runningBalanceCents)
        assertEquals(0L, state.months[1].netCents)
        assertEquals(130_000L, state.months[1].runningBalanceCents) // Februar haelt den Stand
        assertEquals(145_000L, state.months[2].runningBalanceCents)
        assertEquals(145_000L, state.months[11].runningBalanceCents) // bis Dezember stabil

        assertEquals(45_000L, state.yearTotalCents)
        assertEquals(145_000L, state.endBalanceCents)
        assertEquals(2, state.monthsWithEntries)
        assertEquals(jan, state.bestMonth?.period)
    }

    @Test
    fun `Entnahmen senken den Sparstand`() {
        val entries = listOf(
            SavingsEntry(period = jan, title = "Sparen", amountCents = 50_000),
            SavingsEntry(period = feb, title = "Urlaub", amountCents = -80_000)
        )

        val state = FinanceCalculator.buildSavingsState(2026, entries, startBalanceCents = 0)

        assertEquals(50_000L, state.months[0].runningBalanceCents)
        assertEquals(-30_000L, state.months[1].runningBalanceCents)
        assertEquals(-30_000L, state.yearTotalCents)
    }

    @Test
    fun `Erspartes veraendert das Monatsergebnis nicht`() {
        val entries = listOf(
            income(300_000, jan),
            expense(100_000, jan, ExpenseCategory.RENT)
        )
        val month = FinanceCalculator.buildMonthState(jan, entries, emptyList())

        // Sparbetraege liegen in einer eigenen Tabelle und gehen in keine Summe ein.
        FinanceCalculator.buildSavingsState(2026, listOf(SavingsEntry(period = jan, title = "ETF", amountCents = 99_999)), 0)

        assertEquals(300_000L, month.incomeTotal)
        assertEquals(100_000L, month.expenseTotal)
        assertEquals(200_000L, month.balance)
    }

    @Test
    fun `leeres Sparjahr liefert Nullwerte`() {
        val state = FinanceCalculator.buildSavingsState(2026, emptyList(), 0)

        assertEquals(0L, state.yearTotalCents)
        assertEquals(0L, state.averagePerMonth)
        assertEquals(null, state.bestMonth)
    }
}
