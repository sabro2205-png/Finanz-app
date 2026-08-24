package de.sabro.finanzapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FinanceEntry::class, SavingsEntry::class, SavingsPot::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao

    companion object {
        /**
         * Version 1 kannte keine Toepfe – dort war die Bezeichnung jeder Buchung
         * faktisch schon der Topfname. Daraus werden die Toepfe gebildet, sodass
         * nichts verloren geht. Bewusst keine zerstoerende Migration: bestehende
         * Sparbetraege muessen die Aktualisierung ueberleben.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pots (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        colorIndex INTEGER NOT NULL,
                        targetCents INTEGER,
                        position INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // Je vorhandener Bezeichnung ein Topf, in stabiler Reihenfolge.
                // Ohne Fensterfunktionen: die gibt es erst ab SQLite 3.25,
                // also erst ab Android 11 – die App laeuft ab Android 8.
                db.execSQL(
                    """
                    INSERT INTO pots (name, colorIndex, targetCents, position)
                    SELECT name, 0, NULL, 0 FROM (
                        SELECT CASE WHEN TRIM(title) = '' THEN 'Erspartes' ELSE TRIM(title) END AS name,
                               MIN(period) AS firstPeriod
                        FROM savings
                        GROUP BY CASE WHEN TRIM(title) = '' THEN 'Erspartes' ELSE TRIM(title) END
                        ORDER BY firstPeriod, name
                    )
                    """.trimIndent()
                )
                // AUTOINCREMENT vergibt die Ids in Einfuegereihenfolge, daraus
                // ergeben sich Position und Farbe.
                db.execSQL(
                    """
                    UPDATE pots SET
                        position = (SELECT COUNT(*) FROM pots older WHERE older.id < pots.id),
                        colorIndex = (SELECT COUNT(*) FROM pots older WHERE older.id < pots.id) % 6
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE savings_new (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        potId INTEGER NOT NULL,
                        period INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        amountCents INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO savings_new (id, potId, period, title, amountCents)
                    SELECT s.id,
                           (SELECT p.id FROM pots p
                             WHERE p.name = CASE WHEN TRIM(s.title) = '' THEN 'Erspartes' ELSE TRIM(s.title) END),
                           s.period, '', s.amountCents
                    FROM savings s
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE savings")
                db.execSQL("ALTER TABLE savings_new RENAME TO savings")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finanz-db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
