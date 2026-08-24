package de.sabro.finanzapp.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val germany = Locale.GERMANY

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(germany)

private val plainFormat: NumberFormat = NumberFormat.getNumberInstance(germany).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

/** 123456 -> "1.234,56 €" */
fun formatMoney(cents: Long): String = currencyFormat.format(cents / 100.0)

/** 123456 -> "+1.234,56 €", -500 -> "-5,00 €" */
fun formatMoneySigned(cents: Long): String =
    if (cents > 0) "+" + formatMoney(cents) else formatMoney(cents)

/** Kompakt fuer Diagramm-Achsen: 1234 € -> "1,2 T€" */
fun formatMoneyCompact(cents: Long): String {
    val euro = cents / 100.0
    return when {
        abs(euro) >= 1_000_000 -> String.format(germany, "%.1f Mio", euro / 1_000_000)
        abs(euro) >= 1_000 -> String.format(germany, "%.1fT", euro / 1_000)
        else -> String.format(germany, "%.0f", euro)
    }
}

/** Wert ohne Waehrungssymbol, wie er im Eingabefeld erscheinen soll. */
fun formatAmountForInput(cents: Long): String = plainFormat.format(cents / 100.0)

/**
 * Akzeptiert deutsche ("1.234,56") und englische ("1234.56") Schreibweise
 * sowie einfache Eingaben wie "50" oder "-12,3".
 */
fun parseAmountToCents(input: String): Long? {
    var text = input.trim()
        .replace("€", "")
        .replace(" ", "")
        .replace(" ", "")
    if (text.isEmpty()) return null

    val hasComma = text.contains(',')
    val dots = text.count { it == '.' }

    text = when {
        // 1.234,56 -> Punkte sind Tausendertrenner
        hasComma -> text.replace(".", "").replace(',', '.')
        // 12.50 -> Punkt ist Dezimaltrenner
        dots == 1 && text.substringAfterLast('.').length in 1..2 -> text
        // 1.234 -> Punkt ist Tausendertrenner
        dots >= 1 -> text.replace(".", "")
        else -> text
    }

    val value = text.toDoubleOrNull() ?: return null
    if (value.isNaN() || value.isInfinite()) return null
    return (value * 100.0).roundToLong()
}

/** Prozentanteil als ganze Zahl, sicher gegen Division durch 0. */
fun percentOf(part: Long, total: Long): Int =
    if (total <= 0L) 0 else ((part.toDouble() / total.toDouble()) * 100).roundToLong().toInt()
