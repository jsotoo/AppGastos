package com.minimalgastos.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minimalgastos.app.GastosApp
import com.minimalgastos.app.data.Category
import com.minimalgastos.app.data.Categorizer
import com.minimalgastos.app.data.Expense
import com.minimalgastos.app.data.Settings
import com.minimalgastos.app.notifications.Notifier
import com.minimalgastos.app.notifications.ReminderScheduler
import com.minimalgastos.app.util.CsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class Period(val label: String) {
    WEEK("Semana"), MONTH("Mes"), YEAR("Año"), ALL("Todo")
}

data class CategoryTotal(val category: Category, val total: Double, val fraction: Float)

data class DashboardState(
    val total: Double = 0.0,
    val budget: Double = 0.0,
    val byCategory: List<CategoryTotal> = emptyList()
) {
    val hasBudget: Boolean get() = budget > 0
    val remaining: Double get() = budget - total
    val ratio: Float get() = if (hasBudget) (total / budget).toFloat() else 0f

    /** 0 = normal, 80 = aviso, 100 = presupuesto superado. */
    val level: Int get() = budgetLevel(ratio)
}

fun budgetLevel(ratio: Float): Int = when {
    ratio >= 1f -> 100
    ratio >= 0.8f -> 80
    else -> 0
}

private fun LocalDate.startMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun monthRange(): Pair<Long, Long> {
    val first = LocalDate.now().withDayOfMonth(1)
    return first.startMillis() to first.plusMonths(1).startMillis()
}

private fun periodStart(period: Period): Long {
    val today = LocalDate.now()
    return when (period) {
        Period.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).startMillis()
        Period.MONTH -> today.withDayOfMonth(1).startMillis()
        Period.YEAR -> today.withDayOfYear(1).startMillis()
        Period.ALL -> Long.MIN_VALUE
    }
}

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as GastosApp).repository
    private val settingsRepo = (app as GastosApp).settingsRepository

    // ---------- Datos observables ----------

    val settings: StateFlow<Settings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val allExpenses: StateFlow<List<Expense>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dashboard: StateFlow<DashboardState> = combine(allExpenses, settings) { list, s ->
        val (from, to) = monthRange()
        val month = list.filter { it.date >= from && it.date < to }
        val total = month.sumOf { it.amount }
        val byCategory = month
            .groupBy { Category.fromName(it.category) }
            .map { (category, items) -> category to items.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .map { (category, sum) ->
                CategoryTotal(category, sum, if (total > 0) (sum / total).toFloat() else 0f)
            }
        DashboardState(total, s.budget, byCategory)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardState())

    // ---------- Historial con filtros ----------

    private val _period = MutableStateFlow(Period.MONTH)
    val period: StateFlow<Period> = _period

    private val _categoryFilter = MutableStateFlow<Category?>(null)
    val categoryFilter: StateFlow<Category?> = _categoryFilter

    val history: StateFlow<List<Expense>> =
        combine(allExpenses, _period, _categoryFilter) { list, period, category ->
            val from = periodStart(period)
            list.filter { it.date >= from && (category == null || it.category == category.name) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setPeriod(period: Period) {
        _period.value = period
    }

    fun setCategoryFilter(category: Category?) {
        _categoryFilter.value = category
    }

    // ---------- Entrada rápida ----------

    var amount by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    private var manualCategory by mutableStateOf<Category?>(null)
    private var suggestion by mutableStateOf<Category?>(null)
    private var lookupJob: Job? = null

    /** Categoría elegida a mano, o la sugerida automáticamente. */
    val selectedCategory: Category? get() = manualCategory ?: suggestion
    val isAutoCategory: Boolean get() = manualCategory == null && suggestion != null
    val canAdd: Boolean get() = (amount.toDoubleOrNull() ?: 0.0) > 0.0

    fun onAmountChange(text: String) {
        amount = text.filter { it in '0'..'9' }.take(12)
    }

    fun onDescriptionChange(text: String) {
        description = text
        suggestion = Categorizer.suggest(text)
        lookupJob?.cancel()
        if (text.trim().length >= 2) {
            // Si el usuario ya categorizó esta misma descripción antes, se respeta su elección.
            lookupJob = viewModelScope.launch {
                repo.learnedCategory(text)?.let { suggestion = it }
            }
        }
    }

    fun selectCategory(category: Category) {
        manualCategory = category
    }

    fun addExpense() {
        val value = amount.toDoubleOrNull() ?: return
        if (value <= 0) return
        val category = selectedCategory ?: Category.OTHER
        val text = description.trim().ifEmpty { category.label }
        viewModelScope.launch {
            repo.add(
                Expense(
                    amount = value,
                    description = text,
                    category = category.name,
                    date = System.currentTimeMillis()
                )
            )
            clearEntry()
            checkBudget(notify = true)
        }
    }

    private fun clearEntry() {
        lookupJob?.cancel()
        amount = ""
        description = ""
        manualCategory = null
        suggestion = null
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repo.delete(expense)
            checkBudget(notify = false)
        }
    }

    // ---------- Ajustes ----------

    fun saveBudget(text: String) {
        val value = text.filter { it in '0'..'9' }.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            settingsRepo.setBudget(value)
            checkBudget(notify = false)
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepo.setReminder(enabled, hour, minute)
            val context = getApplication<Application>()
            if (enabled) ReminderScheduler.schedule(context, hour, minute)
            else ReminderScheduler.cancel(context)
        }
    }

    // ---------- Exportación ----------

    fun exportCsv(onlyFiltered: Boolean, onReady: (Uri) -> Unit) {
        val data = if (onlyFiltered) history.value else allExpenses.value
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                CsvExporter.write(getApplication<Application>(), data)
            }
            onReady(uri)
        }
    }

    // ---------- Alertas de presupuesto ----------

    /** Notifica una sola vez al cruzar el 80% y el 100% de cada mes. */
    private suspend fun checkBudget(notify: Boolean) {
        val budget = settingsRepo.settings.first().budget
        if (budget <= 0) return
        val (from, to) = monthRange()
        val spent = repo.totalBetween(from, to)
        val level = budgetLevel((spent / budget).toFloat())

        val month = YearMonth.now().toString()
        val (savedMonth, savedLevel) = settingsRepo.alertState()
        val previous = if (savedMonth == month) savedLevel else 0

        if (notify && level > previous && level > 0) {
            Notifier.showBudgetAlert(getApplication<Application>(), level, spent, budget)
        }
        settingsRepo.setAlertState(month, level)
    }
}
