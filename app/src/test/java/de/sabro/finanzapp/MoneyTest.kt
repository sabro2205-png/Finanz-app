package de.sabro.finanzapp

import de.sabro.finanzapp.util.Period
import de.sabro.finanzapp.util.parseAmountToCents
import de.sabro.finanzapp.util.percentOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `deutsche Schreibweise wird erkannt`() {
        assertEquals(4_999L, parseAmountToCents("49,99"))
        assertEquals(123_456L, parseAmountToCents("1.234,56"))
        assertEquals(123_400L, parseAmountToCents("1.234"))
        assertEquals(1_999L, parseAmountToCents(" 19,99 €"))
    }

    @Test
    fun `englische Schreibweise wird erkannt`() {
        assertEquals(1_250L, parseAmountToCents("12.50"))
        assertEquals(1_250L, parseAmountToCents("12.5"))
    }

    @Test
    fun `negative Betraege sind erlaubt`() {
        assertEquals(-25_000L, parseAmountToCents("-250"))
    }

    @Test
    fun `ungueltige Eingaben liefern null`() {
        assertNull(parseAmountToCents(""))
        assertNull(parseAmountToCents("   "))
        assertNull(parseAmountToCents("abc"))
    }

    @Test
    fun `Prozentanteil ist sicher gegen Division durch Null`() {
        assertEquals(25, percentOf(2_500L, 10_000L))
        assertEquals(0, percentOf(2_500L, 0L))
    }

    @Test
    fun `Monatsindex rollt ueber Jahresgrenzen`() {
        assertEquals("Dezember 2025", Period.label(Period.of(2026, 1) - 1))
        assertEquals("Januar 2026", Period.label(Period.of(2025, 12) + 1))
        assertEquals(2026, Period.yearOf(Period.of(2026, 8)))
        assertEquals(8, Period.monthOf(Period.of(2026, 8)))
    }
}
