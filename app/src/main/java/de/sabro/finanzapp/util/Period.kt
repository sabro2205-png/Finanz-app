package de.sabro.finanzapp.util

import java.util.Calendar

/**
 * Ein Monat wird als fortlaufender Index gespeichert:  jahr * 12 + (monat - 1).
 * Dadurch lassen sich Zeitraeume mit einfachen Zahlenvergleichen abfragen.
 */
object Period {

    val MONTH_NAMES = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    )

    val MONTH_SHORT = listOf(
        "Jan", "Feb", "Mär", "Apr", "Mai", "Jun",
        "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"
    )

    fun of(year: Int, month: Int): Int = year * 12 + (month - 1)

    fun yearOf(period: Int): Int = Math.floorDiv(period, 12)

    /** 1 = Januar ... 12 = Dezember */
    fun monthOf(period: Int): Int = Math.floorMod(period, 12) + 1

    fun firstOfYear(year: Int): Int = of(year, 1)

    fun lastOfYear(year: Int): Int = of(year, 12)

    fun current(): Int {
        val cal = Calendar.getInstance()
        return of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun label(period: Int): String = "${MONTH_NAMES[monthOf(period) - 1]} ${yearOf(period)}"

    fun shortLabel(period: Int): String = "${MONTH_SHORT[monthOf(period) - 1]} ${yearOf(period)}"

    fun monthName(period: Int): String = MONTH_NAMES[monthOf(period) - 1]
}
