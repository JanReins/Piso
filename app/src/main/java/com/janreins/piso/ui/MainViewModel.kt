package com.janreins.piso.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.janreins.piso.data.local.AppDatabase
import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.Budget
import com.janreins.piso.data.models.Debt
import com.janreins.piso.data.models.Goal
import com.janreins.piso.data.models.Investment
import com.janreins.piso.data.models.Transaction
import com.janreins.piso.data.repository.FinanceRepository
import com.janreins.piso.util.DateUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    HOME,
    ACTIVITY,
    ACCOUNTS,
    GOALS,
    MORE
}

enum class MoreSubScreen {
    BUDGETS,
    DEBTS,
    INVEST,
    SETTINGS
}

data class MonthlySummary(
    val income: Double = 0.0,
    val spent: Double = 0.0,
    val net: Double = 0.0,
    val goalMoves: Double = 0.0
)

data class NetWorthSummary(
    val netWorth: Double = 0.0,
    val accountsTotal: Double = 0.0,
    val investmentsTotal: Double = 0.0,
    val debtsTotal: Double = 0.0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db)
    }

    // --- Navigation State ---
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _moreSubScreen = MutableStateFlow<MoreSubScreen?>(null)
    val moreSubScreen: StateFlow<MoreSubScreen?> = _moreSubScreen.asStateFlow()

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
        _moreSubScreen.value = null
    }

    fun openMoreSubScreen(subScreen: MoreSubScreen) {
        _moreSubScreen.value = subScreen
    }

    fun closeMoreSubScreen() {
        _moreSubScreen.value = null
    }

    // --- User Feedback Messages ---
    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()

    fun showMessage(message: String) {
        viewModelScope.launch {
            _messageEvent.emit(message)
        }
    }

    // --- Base Flows ---
    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = repository.getRecentTransactions(6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<Goal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoals: StateFlow<List<Goal>> = repository.activeGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<Debt>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openDebts: StateFlow<List<Debt>> = repository.openDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val investments: StateFlow<List<Investment>> = repository.allInvestments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Activity Screen Filters ---
    private val _selectedMonthKey = MutableStateFlow(DateUtil.getCurrentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _activityFilter = MutableStateFlow("ALL") // ALL, INCOME, EXPENSE, TRANSFER
    val activityFilter: StateFlow<String> = _activityFilter.asStateFlow()

    fun setSelectedMonthKey(key: String) {
        _selectedMonthKey.value = key
    }

    fun setActivityFilter(filter: String) {
        _activityFilter.value = filter
    }

    // --- Net Worth Summary ---
    val netWorthSummary: StateFlow<NetWorthSummary> = combine(
        accounts,
        investments,
        debts
    ) { accList, invList, debtList ->
        val accTotal = accList.sumOf { it.balance }
        val invTotal = invList.sumOf { it.currentValue }
        val debtTotal = debtList.sumOf { it.remainingAmount }
        val totalNetWorth = accTotal + invTotal - debtTotal
        NetWorthSummary(
            netWorth = totalNetWorth,
            accountsTotal = accTotal,
            investmentsTotal = invTotal,
            debtsTotal = debtTotal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetWorthSummary())

    // --- This Month Summary ---
    val currentMonthSummary: StateFlow<MonthlySummary> = combine(
        transactions,
        _selectedMonthKey
    ) { txList, _ ->
        val currentKey = DateUtil.getCurrentMonthKey()
        var incomeSum = 0.0
        var spentSum = 0.0
        var goalMovesSum = 0.0

        for (tx in txList) {
            val txMonth = DateUtil.getMonthKey(tx.dateMillis)
            if (txMonth == currentKey) {
                val isGoalMove = tx.goalId != null || tx.goalFlow != null
                if (isGoalMove) {
                    goalMovesSum += tx.amount
                } else if (tx.type == "INCOME") {
                    incomeSum += tx.amount
                } else if (tx.type == "EXPENSE") {
                    spentSum += tx.amount
                }
            }
        }

        MonthlySummary(
            income = incomeSum,
            spent = spentSum,
            net = incomeSum - spentSum,
            goalMoves = goalMovesSum
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary())

    // --- Category Spending for Current Month ---
    val currentMonthCategorySpending: StateFlow<Map<String, Double>> = transactions.combine(_selectedMonthKey) { txList, _ ->
        val currentKey = DateUtil.getCurrentMonthKey()
        val spending = mutableMapOf<String, Double>()
        for (tx in txList) {
            if (DateUtil.getMonthKey(tx.dateMillis) == currentKey && tx.type == "EXPENSE" && tx.goalFlow == null) {
                spending[tx.category] = (spending[tx.category] ?: 0.0) + tx.amount
            }
        }
        spending
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Account CRUD ---
    fun addAccount(account: Account) {
        viewModelScope.launch {
            repository.insertAccount(account)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    // --- Transaction CRUD ---
    fun addTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.addTransaction(tx)
        }
    }

    fun updateTransaction(oldTx: Transaction, newTx: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(oldTx, newTx)
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    // --- Goal CRUD & Actions ---
    fun addGoal(goal: Goal) {
        viewModelScope.launch {
            repository.insertGoal(goal)
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun addMoneyToGoal(goalId: Long, amount: Double, fromAccountId: Long?) {
        viewModelScope.launch {
            val completed = repository.addMoneyToGoal(goalId, amount, fromAccountId)
            if (completed) {
                showMessage("Nice work – goal reached!")
            } else {
                showMessage("Added money to goal")
            }
        }
    }

    // --- Debt CRUD & Actions ---
    fun addDebt(debt: Debt) {
        viewModelScope.launch {
            repository.insertDebt(debt)
        }
    }

    fun updateDebt(debt: Debt) {
        viewModelScope.launch {
            repository.updateDebt(debt)
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    fun recordDebtPayment(debtId: Long, amount: Double, fromAccountId: Long?) {
        viewModelScope.launch {
            repository.recordDebtPayment(debtId, amount, fromAccountId)
            showMessage("Debt payment recorded")
        }
    }

    // --- Budget CRUD ---
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

    // --- Investment CRUD ---
    fun addInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.insertInvestment(investment)
        }
    }

    fun updateInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.updateInvestment(investment)
        }
    }

    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
        }
    }

    // --- Backup & Restore & Clear ---
    fun exportBackup(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onSuccess(json)
        }
    }

    fun importBackup(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importBackupJson(jsonString)
            if (success) {
                showMessage("Data restored successfully")
            } else {
                showMessage("Invalid backup format")
            }
            onResult(success)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            showMessage("All data cleared")
        }
    }
}
