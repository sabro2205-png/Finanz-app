package de.sabro.finanzapp.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val dao: FinanceDao) {

    fun entriesForPeriod(period: Int): Flow<List<FinanceEntry>> = dao.entriesForPeriod(period)

    fun entriesForRange(from: Int, to: Int): Flow<List<FinanceEntry>> = dao.entriesForRange(from, to)

    suspend fun addEntry(entry: FinanceEntry) { dao.insertEntry(entry) }

    suspend fun updateEntry(entry: FinanceEntry) { dao.updateEntry(entry) }

    suspend fun deleteEntry(entry: FinanceEntry) { dao.deleteEntry(entry) }

    /**
     * Beendet einen laufenden Posten zum Ende des Vormonats von [period],
     * sodass frühere Monate unveraendert bleiben.
     */
    suspend fun endEntryBefore(entry: FinanceEntry, period: Int) {
        val newEnd = period - 1
        if (newEnd < entry.startPeriod) {
            dao.deleteEntry(entry)
        } else {
            dao.updateEntry(entry.copy(endPeriod = newEnd, recurring = true))
        }
    }

    fun savingsForRange(from: Int, to: Int): Flow<List<SavingsEntry>> = dao.savingsForRange(from, to)

    fun savingsBalanceBefore(period: Int): Flow<Long> = dao.savingsBalanceBefore(period)

    fun allSavings(): Flow<List<SavingsEntry>> = dao.allSavings()

    fun pots(): Flow<List<SavingsPot>> = dao.pots()

    /** Legt beim ersten Start einen Topf an, damit immer eingezahlt werden kann. */
    suspend fun ensureAtLeastOnePot(): Long? =
        if (dao.potCount() > 0) null
        else dao.insertPot(SavingsPot(name = "Erspartes", colorIndex = 0, position = 0))

    suspend fun addPot(name: String, colorIndex: Int, targetCents: Long?): Long =
        dao.insertPot(
            SavingsPot(
                name = name,
                colorIndex = colorIndex,
                targetCents = targetCents,
                position = dao.nextPotPosition()
            )
        )

    suspend fun updatePot(pot: SavingsPot) { dao.updatePot(pot) }

    /**
     * Loescht einen Topf. Ist [moveToPotId] gesetzt, wandern seine Buchungen
     * dorthin, sonst werden sie mitgeloescht.
     */
    suspend fun deletePot(pot: SavingsPot, moveToPotId: Long?) {
        if (moveToPotId != null) dao.moveSavings(pot.id, moveToPotId)
        else dao.deleteSavingsOfPot(pot.id)
        dao.deletePot(pot)
    }

    suspend fun addSaving(entry: SavingsEntry) { dao.insertSaving(entry) }

    suspend fun updateSaving(entry: SavingsEntry) { dao.updateSaving(entry) }

    suspend fun deleteSaving(entry: SavingsEntry) { dao.deleteSaving(entry) }
}
