package de.sabro.finanzapp.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.sabro.finanzapp.data.AppDatabase
import de.sabro.finanzapp.data.EntryType
import de.sabro.finanzapp.data.ExpenseCategory
import de.sabro.finanzapp.data.FinanceEntry
import de.sabro.finanzapp.data.FinanceRepository
import de.sabro.finanzapp.data.SavingsEntry
import de.sabro.finanzapp.domain.FinanceCalculator
import de.sabro.finanzapp.domain.MonthUiState
import de.sabro.finanzapp.domain.SavingsUiState
import de.sabro.finanzapp.domain.YearUiState
import de.sabro.finanzapp.util.Period
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(private val repo: FinanceRepository) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(Period.current())
    val selectedPeriod: StateFlow<Int> = _selectedPeriod.asStateFlow()

    private val _selectedYear = MutableStateFlow(Period.currentYear())
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    /** Der gewaehlte Monat inklusive Vormonat fuer den Vergleich. */
    val monthState: StateFlow<MonthUiState> = _selectedPeriod
        .flatMapLatest { period ->
            combine(
                repo.entriesForPeriod(period),
                repo.entriesForPeriod(period - 1)
            ) { current, previous ->
                FinanceCalculator.buildMonthState(period, current, previous)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthUiState())

    val yearState: StateFlow<YearUiState> = _selectedYear
        .flatMapLatest { year ->
            repo.entriesForRange(Period.firstOfYear(year), Period.lastOfYear(year))
                .map { entries -> FinanceCalculator.buildYearState(year, entries) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearUiState())

    val savingsState: StateFlow<SavingsUiState> = _selectedYear
        .flatMapLatest { year ->
            val from = Period.firstOfYear(year)
            val to = Period.lastOfYear(year)
            combine(
                repo.savingsForRange(from, to),
                repo.savingsBalanceBefore(from),
                repo.allSavings()
            ) { entries, startBalance, all ->
                // Die Prognose braucht die gesamte Historie, nicht nur das Jahr.
                FinanceCalculator.buildSavingsState(year, entries, startBalance, all)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsUiState())

    // ------------------------------------------------------------ Navigation

    fun showPreviousMonth() { _selectedPeriod.value -= 1 }

    fun showNextMonth() { _selectedPeriod.value += 1 }

    fun showPeriod(period: Int) { _selectedPeriod.value = period }

    fun showPreviousYear() { _selectedYear.value -= 1 }

    fun showNextYear() { _selectedYear.value += 1 }

    // ------------------------------------------------------------ Bearbeiten

    fun saveEntry(
        existing: FinanceEntry?,
        type: EntryType,
        category: ExpenseCategory?,
        title: String,
        amountCents: Long,
        recurring: Boolean,
        startPeriod: Int,
        endPeriod: Int?
    ) {
        viewModelScope.launch {
            // Bei einmaligen Posten faellt das Ende immer mit dem Start zusammen.
            val end = if (recurring) endPeriod else startPeriod
            if (existing == null) {
                repo.addEntry(
                    FinanceEntry(
                        type = type,
                        category = if (type == EntryType.EXPENSE) category else null,
                        title = title,
                        amountCents = amountCents,
                        startPeriod = startPeriod,
                        endPeriod = end,
                        recurring = recurring
                    )
                )
            } else {
                repo.updateEntry(
                    existing.copy(
                        type = type,
                        category = if (type == EntryType.EXPENSE) category else null,
                        title = title,
                        amountCents = amountCents,
                        recurring = recurring,
                        startPeriod = startPeriod,
                        endPeriod = end
                    )
                )
            }
            // Der Posten kann jetzt ausserhalb des angezeigten Monats liegen.
            val current = _selectedPeriod.value
            val active = startPeriod <= current && (end == null || end >= current)
            if (!active) _selectedPeriod.value = startPeriod
        }
    }

    fun deleteEntry(entry: FinanceEntry) {
        viewModelScope.launch { repo.deleteEntry(entry) }
    }

    /** Laufenden Posten ab [period] beenden – frühere Monate bleiben unveraendert. */
    fun stopEntryFrom(entry: FinanceEntry, period: Int) {
        viewModelScope.launch { repo.endEntryBefore(entry, period) }
    }

    fun saveSaving(existing: SavingsEntry?, period: Int, title: String, amountCents: Long) {
        viewModelScope.launch {
            if (existing == null) {
                repo.addSaving(SavingsEntry(period = period, title = title, amountCents = amountCents))
            } else {
                repo.updateSaving(existing.copy(period = period, title = title, amountCents = amountCents))
            }
        }
    }

    fun deleteSaving(entry: SavingsEntry) {
        viewModelScope.launch { repo.deleteSaving(entry) }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = AppDatabase.get(application).financeDao()
                    return FinanceViewModel(FinanceRepository(dao)) as T
                }
            }
    }
}
