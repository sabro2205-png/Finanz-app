package de.sabro.finanzapp

import de.sabro.finanzapp.data.EntryType
import de.sabro.finanzapp.data.ExpenseCategory
import de.sabro.finanzapp.data.FinanceEntry
import de.sabro.finanzapp.data.SavingsEntry
import de.sabro.finanzapp.domain.FinanceCalculator
import de.sabro.finanzapp.util.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsForecastTest {

    private val today = Period.of(2026, 8)

    private fun saving(year: Int, month: Int, cents: Long) =
        SavingsEntry(period = Period.of(year, month), title = "Sparen", amountCents = cents)

    @Test
    fun `Durchschnitt bildet sich nur aus Monaten bis heute`() {
        val entries = listOf(
            saving(2026, 1, 20_000),
            saving(2026, 2, 30_000),
            // liegt in der Zukunft und darf den Durchschnitt nicht beeinflussen
            saving(2026, 12, 900_000)
        )

        val f = FinanceCalculator.buildSavingsForecast(2026, entries, today)

        assertEquals(2, f.basisMonths)
        assertEquals(25_000L, f.averageCents)
    }

    @Test
    fun `mehrere Eintraege im selben Monat zaehlen als ein Monat`() {
        val entries = listOf(
            saving(2026, 1, 15_000),
            saving(2026, 1, 20_000),
            saving(2026, 2, 25_000)
        )

        val f = FinanceCalculator.buildSavingsForecast(2026, entries, today)

        assertEquals(2, f.basisMonths)
        assertEquals(30_000L, f.averageCents) // (35.000 + 25.000) / 2
    }

    @Test
    fun `kuenftige Monate ohne Eintrag werden hochgerechnet`() {
        // Jan–Aug je 200 €
        val entries = (1..8).map { saving(2026, it, 20_000) }

        val f = FinanceCalculator.buildSavingsForecast(2026, entries, today)

        assertEquals(8, f.basisMonths)
        assertEquals(20_000L, f.averageCents)
        assertEquals(4, f.openMonths)          // Sep bis Dez
        assertEquals(160_000L, f.balanceTodayCents)
        assertEquals(240_000L, f.endOfYearCents) // 1.600 € + 4 × 200 €
        assertEquals(160_000L + 12 * 20_000L, f.inTwelveMonthsCents)
        assertTrue(f.available)
    }

    @Test
    fun `vergangene Monate ohne Eintrag gelten als null gespart`() {
        // Nur Januar erfasst, Februar bis August blieben leer
        val entries = listOf(saving(2026, 1, 30_000))

        val f = FinanceCalculator.buildSavingsForecast(2026, entries, today)

        assertEquals(1, f.basisMonths)
        assertEquals(30_000L, f.averageCents)
        // Februar bis August werden nicht hochgerechnet
        assertEquals(30_000L, f.points[6].balanceCents)  // Juli
        assertFalse(f.points[6].forecast)
        assertEquals(4, f.openMonths)                    // nur Sep bis Dez
    }

    @Test
    fun `Punkte sind als erfasst oder prognostiziert markiert`() {
        val entries = (1..8).map { saving(2026, it, 10_000) }

        val points = FinanceCalculator.buildSavingsForecast(2026, entries, today).points

        assertEquals(12, points.size)
        assertTrue(points.take(8).none { it.forecast })
        assertTrue(points.drop(8).all { it.forecast })
    }

    @Test
    fun `Entnahmen druecken den Durchschnitt`() {
        val entries = listOf(
            saving(2026, 1, 30_000),
            saving(2026, 2, 30_000),
            saving(2026, 3, -30_000)
        )

        val f = FinanceCalculator.buildSavingsForecast(2026, entries, today)

        assertEquals(3, f.basisMonths)
        assertEquals(10_000L, f.averageCents) // (300 + 300 - 300) / 3
    }

    @Test
    fun `Vorjahre zaehlen zur Grundlage und zum Startstand`() {
        val entries = (1..12).map { saving(2025, it, 25_000) } + (1..8).map { saving(2026, it, 25_000) }

        val f = FinanceCalculator.buildSavingsForecast(2026, entries, today)

        assertEquals(20, f.basisMonths)
        assertEquals(25_000L, f.averageCents)
        assertEquals(500_000L, f.balanceTodayCents)
        assertEquals(600_000L, f.endOfYearCents) // + 4 offene Monate
    }

    @Test
    fun `ohne Daten gibt es keine Prognose`() {
        val f = FinanceCalculator.buildSavingsForecast(2026, emptyList(), today)

        assertEquals(0, f.basisMonths)
        assertEquals(0L, f.averageCents)
        assertFalse(f.available)
        assertTrue(f.points.none { it.forecast })
    }

    @Test
    fun `vollstaendig vergangenes Jahr hat nichts hochzurechnen`() {
        val entries = (1..12).map { saving(2025, it, 20_000) }

        val f = FinanceCalculator.buildSavingsForecast(2025, entries, today)

        assertEquals(0, f.openMonths)
        assertFalse(f.available)
        assertEquals(240_000L, f.endOfYearCents)
    }

    @Test
    fun `Prognose laesst Einnahmen und Ausgaben unberuehrt`() {
        val entries = (1..8).map { saving(2026, it, 50_000) }
        val f = FinanceCalculator.buildSavingsForecast(2026, entries, today)
        assertTrue(f.endOfYearCents > 0)

        val month = FinanceCalculator.buildMonthState(
            today,
            listOf(
                FinanceEntry(type = EntryType.INCOME, category = null, title = "Gehalt",
                    amountCents = 300_000, startPeriod = today, endPeriod = null, recurring = true),
                FinanceEntry(type = EntryType.EXPENSE, category = ExpenseCategory.RENT, title = "Miete",
                    amountCents = 100_000, startPeriod = today, endPeriod = null, recurring = true)
            ),
            emptyList()
        )
        assertEquals(200_000L, month.balance)
    }

    // ------------------------------------------------------- Restlaufzeit

    @Test
    fun `Restlaufzeit eines befristeten Postens`() {
        val kredit = FinanceEntry(
            type = EntryType.EXPENSE, category = ExpenseCategory.FINANCING, title = "Autokredit",
            amountCents = 28_900, startPeriod = Period.of(2026, 1),
            endPeriod = Period.of(2028, 5), recurring = true
        )

        assertEquals(29, FinanceCalculator.remainingMonths(kredit, Period.of(2026, 1)))
        assertEquals(22, FinanceCalculator.remainingMonths(kredit, Period.of(2026, 8)))
        assertEquals(1, FinanceCalculator.remainingMonths(kredit, Period.of(2028, 5)))
        assertEquals(0, FinanceCalculator.remainingMonths(kredit, Period.of(2028, 6)))
    }

    @Test
    fun `unbefristeter Posten hat keine Restlaufzeit`() {
        val miete = FinanceEntry(
            type = EntryType.EXPENSE, category = ExpenseCategory.RENT, title = "Miete",
            amountCents = 100_000, startPeriod = Period.of(2026, 1), endPeriod = null, recurring = true
        )
        assertNull(FinanceCalculator.remainingMonths(miete, Period.of(2026, 8)))
    }

    @Test
    fun `befristeter Posten gilt genau im Zeitraum`() {
        val kredit = FinanceEntry(
            type = EntryType.EXPENSE, category = ExpenseCategory.FINANCING, title = "Küche",
            amountCents = 14_950, startPeriod = Period.of(2026, 1),
            endPeriod = Period.of(2028, 5), recurring = true
        )

        assertFalse(FinanceCalculator.isActiveIn(kredit, Period.of(2025, 12)))
        assertTrue(FinanceCalculator.isActiveIn(kredit, Period.of(2026, 1)))
        assertTrue(FinanceCalculator.isActiveIn(kredit, Period.of(2027, 6)))
        assertTrue(FinanceCalculator.isActiveIn(kredit, Period.of(2028, 5)))
        assertFalse(FinanceCalculator.isActiveIn(kredit, Period.of(2028, 6)))
    }

    @Test
    fun `Jahresansicht beruecksichtigt das Ende eines Zeitraums`() {
        val kredit = FinanceEntry(
            type = EntryType.EXPENSE, category = ExpenseCategory.FINANCING, title = "Küche",
            amountCents = 10_000, startPeriod = Period.of(2026, 1),
            endPeriod = Period.of(2026, 5), recurring = true
        )

        val year = FinanceCalculator.buildYearState(2026, listOf(kredit))

        assertEquals(10_000L, year.months[4].expenseCents)  // Mai
        assertEquals(0L, year.months[5].expenseCents)       // Juni
        assertEquals(5 * 10_000L, year.expenseTotal)
    }
}
