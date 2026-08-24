package de.sabro.finanzapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    /** Alle Posten, die im angegebenen Monat gelten (einmalig oder laufend). */
    @Query(
        """
        SELECT * FROM entries
        WHERE startPeriod <= :period AND (endPeriod IS NULL OR endPeriod >= :period)
        ORDER BY amountCents DESC, title ASC
        """
    )
    fun entriesForPeriod(period: Int): Flow<List<FinanceEntry>>

    /** Alle Posten, die sich mit dem Zeitraum ueberschneiden (fuer die Jahresansicht). */
    @Query(
        """
        SELECT * FROM entries
        WHERE startPeriod <= :toPeriod AND (endPeriod IS NULL OR endPeriod >= :fromPeriod)
        ORDER BY startPeriod ASC
        """
    )
    fun entriesForRange(fromPeriod: Int, toPeriod: Int): Flow<List<FinanceEntry>>

    @Insert
    suspend fun insertEntry(entry: FinanceEntry): Long

    @Update
    suspend fun updateEntry(entry: FinanceEntry)

    @Delete
    suspend fun deleteEntry(entry: FinanceEntry)

    @Query("SELECT * FROM savings WHERE period BETWEEN :fromPeriod AND :toPeriod ORDER BY period ASC, id ASC")
    fun savingsForRange(fromPeriod: Int, toPeriod: Int): Flow<List<SavingsEntry>>

    @Query("SELECT * FROM savings ORDER BY period ASC, id ASC")
    fun allSavings(): Flow<List<SavingsEntry>>

    /** Sparstand vor dem Jahresbeginn – Basis fuer die kumulierte Kurve. */
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM savings WHERE period < :period")
    fun savingsBalanceBefore(period: Int): Flow<Long>

    @Query("SELECT * FROM pots ORDER BY position ASC, id ASC")
    fun pots(): Flow<List<SavingsPot>>

    @Query("SELECT COUNT(*) FROM pots")
    suspend fun potCount(): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM pots")
    suspend fun nextPotPosition(): Int

    @Insert
    suspend fun insertPot(pot: SavingsPot): Long

    @Update
    suspend fun updatePot(pot: SavingsPot)

    @Delete
    suspend fun deletePot(pot: SavingsPot)

    @Query("UPDATE savings SET potId = :toPotId WHERE potId = :fromPotId")
    suspend fun moveSavings(fromPotId: Long, toPotId: Long)

    @Query("DELETE FROM savings WHERE potId = :potId")
    suspend fun deleteSavingsOfPot(potId: Long)

    @Insert
    suspend fun insertSaving(entry: SavingsEntry): Long

    @Update
    suspend fun updateSaving(entry: SavingsEntry)

    @Delete
    suspend fun deleteSaving(entry: SavingsEntry)
}
