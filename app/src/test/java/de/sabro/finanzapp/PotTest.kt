package de.sabro.finanzapp

import de.sabro.finanzapp.data.SavingsEntry
import de.sabro.finanzapp.data.SavingsPot
import de.sabro.finanzapp.domain.FinanceCalculator
import de.sabro.finanzapp.util.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PotTest {

    private val notgroschen = SavingsPot(id = 1, name = "Notgroschen", colorIndex = 0, targetCents = 500_000)
    private val urlaub = SavingsPot(id = 2, name = "Urlaubskasse", colorIndex = 1, targetCents = null)
    private val auto = SavingsPot(id = 3, name = "Neues Auto", colorIndex = 2, targetCents = 1_200_000)
    private val pots = listOf(notgroschen, urlaub, auto)

    private fun booking(potId: Long, month: Int, cents: Long) =
        SavingsEntry(potId = potId, period = Period.of(2026, month), amountCents = cents)

    @Test
    fun `Inhalt je Topf wird getrennt summiert`() {
        val bookings = listOf(
            booking(1, 1, 30_000), booking(1, 2, 30_000),
            booking(2, 1, 10_000),
            booking(3, 3, 200_000)
        )

        val summaries = FinanceCalculator.buildPotSummaries(pots, bookings)

        assertEquals(60_000L, summaries[0].balanceCents)
        assertEquals(10_000L, summaries[1].balanceCents)
        assertEquals(200_000L, summaries[2].balanceCents)
    }

    @Test
    fun `Anteile ergeben zusammen rund hundert Prozent`() {
        val bookings = listOf(booking(1, 1, 50_000), booking(2, 1, 30_000), booking(3, 1, 20_000))

        val summaries = FinanceCalculator.buildPotSummaries(pots, bookings)

        assertEquals(50, summaries[0].sharePercent)
        assertEquals(30, summaries[1].sharePercent)
        assertEquals(20, summaries[2].sharePercent)
    }

    @Test
    fun `Zielfortschritt nur bei gesetztem Ziel`() {
        val bookings = listOf(booking(1, 1, 250_000), booking(2, 1, 90_000))

        val summaries = FinanceCalculator.buildPotSummaries(pots, bookings)

        assertEquals(50, summaries[0].targetReachedPercent)  // 2.500 von 5.000
        assertNull(summaries[1].targetReachedPercent)        // kein Ziel
        assertEquals(0, summaries[2].targetReachedPercent)   // noch leer
    }

    @Test
    fun `Zielfortschritt bleibt zwischen null und hundert`() {
        val ueberfuellt = FinanceCalculator.buildPotSummaries(
            listOf(notgroschen), listOf(booking(1, 1, 900_000))
        ).first()
        assertEquals(100, ueberfuellt.targetReachedPercent)

        val ueberzogen = FinanceCalculator.buildPotSummaries(
            listOf(notgroschen), listOf(booking(1, 1, -50_000))
        ).first()
        assertEquals(0, ueberzogen.targetReachedPercent)
        assertEquals(-50_000L, ueberzogen.balanceCents)
    }

    @Test
    fun `negative Bestaende verzerren die Anteile nicht`() {
        val bookings = listOf(booking(1, 1, 80_000), booking(2, 1, 20_000), booking(3, 1, -40_000))

        val summaries = FinanceCalculator.buildPotSummaries(pots, bookings)

        assertEquals(80, summaries[0].sharePercent)
        assertEquals(20, summaries[1].sharePercent)
        assertEquals(0, summaries[2].sharePercent)
    }

    @Test
    fun `leerer Topf fuehrt nicht zu Division durch Null`() {
        val summaries = FinanceCalculator.buildPotSummaries(pots, emptyList())

        assertEquals(3, summaries.size)
        summaries.forEach {
            assertEquals(0L, it.balanceCents)
            assertEquals(0, it.sharePercent)
        }
    }

    @Test
    fun `Filter auf einen Topf rechnet nur dessen Buchungen`() {
        val alle = listOf(
            booking(1, 1, 30_000), booking(1, 2, 30_000),
            booking(2, 1, 100_000)
        )
        val nurNotgroschen = alle.filter { it.potId == 1L }

        val state = FinanceCalculator.buildSavingsState(
            year = 2026,
            entries = nurNotgroschen,
            startBalanceCents = 0L,
            allEntries = nurNotgroschen,
            today = Period.of(2026, 8),
            pots = FinanceCalculator.buildPotSummaries(pots, alle),
            potFilter = 1L
        )

        assertEquals(60_000L, state.yearTotalCents)
        assertEquals(60_000L, state.endBalanceCents)
        assertEquals(1L, state.potFilter)
        assertEquals("Notgroschen", state.filteredPot?.pot?.name)
        // Die Topfuebersicht kennt weiterhin alle Toepfe
        assertEquals(3, state.pots.size)
        assertEquals(160_000L, state.potTotalCents)
    }

    @Test
    fun `Erspartes bleibt ohne Einfluss auf das Monatsergebnis`() {
        val bookings = listOf(booking(1, 8, 500_000))
        val summaries = FinanceCalculator.buildPotSummaries(pots, bookings)
        assertEquals(500_000L, summaries[0].balanceCents)

        val month = FinanceCalculator.buildMonthState(Period.of(2026, 8), emptyList(), emptyList())
        assertEquals(0L, month.incomeTotal)
        assertEquals(0L, month.expenseTotal)
        assertEquals(0L, month.balance)
    }
}
