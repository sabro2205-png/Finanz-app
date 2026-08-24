package de.sabro.finanzapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class EntryType {
    INCOME,
    EXPENSE;

    val label: String
        get() = if (this == INCOME) "Einnahme" else "Ausgabe"
}

/** Die vom Nutzer gewuenschte Staffelung der Ausgaben. */
enum class ExpenseCategory(val label: String) {
    SUBSCRIPTION("Abos"),
    FINANCING("Finanzierung"),
    RENT("Miete"),
    OTHER("Sonstige Ausgaben");

    companion object {
        /** Anzeigereihenfolge in der Monatsuebersicht. */
        val ordered: List<ExpenseCategory> = listOf(RENT, FINANCING, SUBSCRIPTION, OTHER)
    }
}

/**
 * Eine Einnahme oder Ausgabe.
 *
 * Gueltigkeit wird ueber [startPeriod] / [endPeriod] abgebildet:
 *  - einmalig:            startPeriod == endPeriod
 *  - monatlich laufend:   endPeriod == null (unbefristet) oder endPeriod > startPeriod (befristet)
 */
@Entity(tableName = "entries")
data class FinanceEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: EntryType,
    /** nur bei [EntryType.EXPENSE] gesetzt */
    val category: ExpenseCategory?,
    val title: String,
    val amountCents: Long,
    val startPeriod: Int,
    val endPeriod: Int?,
    val recurring: Boolean,
    val note: String? = null
)

/**
 * Ein Spartopf, z. B. Notgroschen oder Urlaubskasse.
 * [colorIndex] verweist auf die Farbreihe der Oberflaeche, [targetCents]
 * ist ein freiwilliges Sparziel.
 */
@Entity(tableName = "pots")
data class SavingsPot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorIndex: Int = 0,
    val targetCents: Long? = null,
    /** Anzeigereihenfolge, damit Toepfe nicht springen. */
    val position: Int = 0
)

/**
 * Eine Buchung auf einem Spartopf. Bewusst getrennt von [FinanceEntry] –
 * Erspartes fliesst nicht in Einnahmen, Ausgaben oder das Monatsergebnis ein.
 * Negative Betraege bilden Entnahmen ab.
 */
@Entity(tableName = "savings")
data class SavingsEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val potId: Long,
    val period: Int,
    /** Freiwillige Notiz – den Namen traegt der Topf. */
    val title: String = "",
    val amountCents: Long
)

class Converters {
    @TypeConverter
    fun entryTypeToString(value: EntryType): String = value.name

    @TypeConverter
    fun stringToEntryType(value: String): EntryType = EntryType.valueOf(value)

    @TypeConverter
    fun categoryToString(value: ExpenseCategory?): String? = value?.name

    @TypeConverter
    fun stringToCategory(value: String?): ExpenseCategory? = value?.let { ExpenseCategory.valueOf(it) }
}
