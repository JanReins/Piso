package com.janreins.piso.ui.budgets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.janreins.piso.data.local.AppDatabase
import com.janreins.piso.data.models.Budget
import com.janreins.piso.data.models.UserCategory
import com.janreins.piso.data.repository.FinanceRepository
import com.janreins.piso.ui.state.CategorySpendingBreakdown
import com.janreins.piso.ui.state.SubcategorySplit
import com.janreins.piso.util.DateUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class BudgetsUiState(
    val isLoading: Boolean = true,
    val budgets: List<Budget> = emptyList(),
    val currentMonthCategorySpending: Map<String, Double> = emptyMap(),
    val currentMonthBreakdownMap: Map<String, CategorySpendingBreakdown> = emptyMap(),
    val categories: List<UserCategory> = emptyList(),
    val isEmpty: Boolean = false
)

class BudgetsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db)
    }

    val budgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<UserCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val transactions = repository.allTransactions

    val currentMonthCategorySpending: StateFlow<Map<String, Double>> = transactions.map { txList ->
        val currentKey = DateUtil.getCurrentMonthKey()
        val spending = mutableMapOf<String, Double>()
        for (tx in txList) {
            val isGoalMove = tx.goalId != null || tx.goalFlow != null
            if (DateUtil.getMonthKey(tx.dateMillis) == currentKey && tx.type == "EXPENSE" && !isGoalMove) {
                val cat = tx.category.ifBlank { "Other" }
                spending[cat] = (spending[cat] ?: 0.0) + tx.amount
            }
        }
        spending
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val currentMonthSpendingBreakdown: StateFlow<List<CategorySpendingBreakdown>> = transactions.map { txList ->
        val currentKey = DateUtil.getCurrentMonthKey()
        val expenseTxs = txList.filter {
            DateUtil.getMonthKey(it.dateMillis) == currentKey &&
                it.type == "EXPENSE" &&
                it.goalId == null &&
                it.goalFlow == null
        }

        val grouped = expenseTxs.groupBy { it.category.ifBlank { "Other" } }

        grouped.map { (catName, txs) ->
            val total = txs.sumOf { it.amount }

            val hasSubcategories = txs.any { it.subcategory.isNotBlank() }
            val splits = if (hasSubcategories) {
                val subMap = mutableMapOf<String, Double>()
                var unassigned = 0.0
                for (tx in txs) {
                    val sub = tx.subcategory.trim()
                    if (sub.isNotBlank()) {
                        subMap[sub] = (subMap[sub] ?: 0.0) + tx.amount
                    } else {
                        unassigned += tx.amount
                    }
                }
                val list = subMap.entries
                    .sortedByDescending { it.value }
                    .map { SubcategorySplit(it.key, it.value) }
                    .toMutableList()

                if (unassigned > 0.0) {
                    list.add(SubcategorySplit("Other", unassigned))
                }
                list
            } else {
                emptyList()
            }

            CategorySpendingBreakdown(
                category = catName,
                totalAmount = total,
                subcategories = splits
            )
        }.sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthBreakdownMap: StateFlow<Map<String, CategorySpendingBreakdown>> = currentMonthSpendingBreakdown.map { list ->
        list.associateBy { it.category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgets,
        currentMonthCategorySpending,
        currentMonthBreakdownMap,
        categories
    ) { bdgts, spending, breakdown, cats ->
        BudgetsUiState(
            isLoading = false,
            budgets = bdgts,
            currentMonthCategorySpending = spending,
            currentMonthBreakdownMap = breakdown,
            categories = cats,
            isEmpty = bdgts.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetsUiState())

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            repository.insertBudget(budget)
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _messageEvent.emit(message)
        }
    }
}
