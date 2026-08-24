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
            dao.updateEntry(entry.copy(endPeriod = newEnd))
        }
    }

    fun savingsForRange(from: Int, to: Int): Flow<List<SavingsEntry>> = dao.savingsForRange(from, to)

    fun savingsBalanceBefore(period: Int): Flow<Long> = dao.savingsBalanceBefore(period)

    suspend fun addSaving(entry: SavingsEntry) { dao.insertSaving(entry) }

    suspend fun updateSaving(entry: SavingsEntry) { dao.updateSaving(entry) }

    suspend fun deleteSaving(entry: SavingsEntry) { dao.deleteSaving(entry) }
}
