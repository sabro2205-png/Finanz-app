package de.sabro.finanzapp.ui

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.sabro.finanzapp.data.EntryType
import de.sabro.finanzapp.data.FinanceEntry
import de.sabro.finanzapp.data.SavingsEntry
import de.sabro.finanzapp.ui.components.EntryEditorDialog
import de.sabro.finanzapp.ui.components.MonthPickerDialog
import de.sabro.finanzapp.ui.components.SavingsEditorDialog
import de.sabro.finanzapp.ui.month.MonthScreen
import de.sabro.finanzapp.ui.savings.SavingsScreen
import de.sabro.finanzapp.ui.theme.FinanzAppTheme
import de.sabro.finanzapp.ui.year.YearScreen
import de.sabro.finanzapp.util.Period

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FinanzAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FinanzApp()
                }
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    MONTH("Monat", Icons.Default.CalendarMonth),
    YEAR("Jahr", Icons.Default.StackedBarChart),
    SAVINGS("Erspartes", Icons.Default.Savings)
}

/** Welcher Dialog gerade offen ist. */
private sealed interface ActiveDialog {
    data class Entry(val existing: FinanceEntry?, val initialType: EntryType) : ActiveDialog
    data class Savings(val period: Int, val existing: SavingsEntry?) : ActiveDialog
    data object MonthPicker : ActiveDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanzApp() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: FinanceViewModel = viewModel(factory = FinanceViewModel.factory(application))

    val monthState by viewModel.monthState.collectAsStateWithLifecycle()
    val yearState by viewModel.yearState.collectAsStateWithLifecycle()
    val savingsState by viewModel.savingsState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.MONTH) }
    var dialog by remember { mutableStateOf<ActiveDialog?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = entry.label) },
                        label = { Text(entry.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            when (tab) {
                Tab.MONTH -> FloatingActionButton(onClick = {
                    dialog = ActiveDialog.Entry(existing = null, initialType = EntryType.EXPENSE)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Posten hinzufügen")
                }
                Tab.SAVINGS -> FloatingActionButton(onClick = {
                    // Standardmaessig fuer den aktuell gewaehlten Monat, sonst Januar des Jahres.
                    val period = if (Period.yearOf(selectedPeriod) == savingsState.year) {
                        selectedPeriod
                    } else {
                        Period.of(savingsState.year, 1)
                    }
                    dialog = ActiveDialog.Savings(period = period, existing = null)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Sparbetrag hinzufügen")
                }
                Tab.YEAR -> Unit
            }
        }
    ) { padding ->
        when (tab) {
            Tab.MONTH -> MonthScreen(
                state = monthState,
                onPreviousMonth = viewModel::showPreviousMonth,
                onNextMonth = viewModel::showNextMonth,
                onPickMonth = { dialog = ActiveDialog.MonthPicker },
                onEntryClick = { entry ->
                    dialog = ActiveDialog.Entry(existing = entry, initialType = entry.type)
                },
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            )

            Tab.YEAR -> YearScreen(
                state = yearState,
                onPreviousYear = viewModel::showPreviousYear,
                onNextYear = viewModel::showNextYear,
                onMonthClick = { period ->
                    viewModel.showPeriod(period)
                    tab = Tab.MONTH
                },
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            )

            Tab.SAVINGS -> SavingsScreen(
                state = savingsState,
                onPreviousYear = viewModel::showPreviousYear,
                onNextYear = viewModel::showNextYear,
                onAddForMonth = { period ->
                    dialog = ActiveDialog.Savings(period = period, existing = null)
                },
                onEntryClick = { entry ->
                    dialog = ActiveDialog.Savings(period = entry.period, existing = entry)
                },
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    when (val current = dialog) {
        is ActiveDialog.Entry -> EntryEditorDialog(
            period = selectedPeriod,
            existing = current.existing,
            initialType = current.initialType,
            onDismiss = { dialog = null },
            onSave = { result ->
                viewModel.saveEntry(
                    existing = current.existing,
                    type = result.type,
                    category = result.category,
                    title = result.title,
                    amountCents = result.amountCents,
                    recurring = result.recurring,
                    period = selectedPeriod
                )
                dialog = null
            },
            onDelete = current.existing?.let { entry ->
                {
                    viewModel.deleteEntry(entry)
                    dialog = null
                }
            },
            onStopFromHere = current.existing?.let { entry ->
                {
                    viewModel.stopEntryFrom(entry, selectedPeriod)
                    dialog = null
                }
            }
        )

        is ActiveDialog.Savings -> SavingsEditorDialog(
            period = current.period,
            existing = current.existing,
            onDismiss = { dialog = null },
            onSave = { title, amount ->
                viewModel.saveSaving(current.existing, current.period, title, amount)
                dialog = null
            },
            onDelete = current.existing?.let { entry ->
                {
                    viewModel.deleteSaving(entry)
                    dialog = null
                }
            }
        )

        ActiveDialog.MonthPicker -> MonthPickerDialog(
            period = selectedPeriod,
            onDismiss = { dialog = null },
            onSelect = { period ->
                viewModel.showPeriod(period)
                dialog = null
            }
        )

        null -> Unit
    }
}
