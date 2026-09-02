package com.janreins.piso.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.janreins.piso.data.local.AppDatabase
import com.janreins.piso.data.local.ThemeMode
import com.janreins.piso.data.local.UserProfile
import com.janreins.piso.data.local.UserProfileManager
import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.Budget
import com.janreins.piso.data.models.Debt
import com.janreins.piso.data.models.Goal
import com.janreins.piso.data.models.Investment
import com.janreins.piso.data.models.Transaction
import com.janreins.piso.data.models.UserCategory
import com.janreins.piso.data.models.UserSubcategory
import com.janreins.piso.data.repository.FinanceRepository
import com.janreins.piso.util.DateUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    SETTINGS,
    CATEGORIES
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

data class SubcategorySplit(
    val name: String,
    val amount: Double
)

data class CategorySpendingBreakdown(
    val category: String,
    val totalAmount: Double,
    val subcategories: List<SubcategorySplit>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    private val profileManager = UserProfileManager(application.applicationContext)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db)
        viewModelScope.launch {
            repository.seedDefaultCategoriesIfEmpty()
        }
    }

    // --- Profile & Authentication State ---
    val userProfile: StateFlow<UserProfile> = profileManager.userProfile
    val isAppLocked: StateFlow<Boolean> = profileManager.isLocked

    private var _skipLockOnce = false
    val skipLockOnce: Boolean
        get() = _skipLockOnce

    fun setSkipLockOnce(value: Boolean = true) {
        _skipLockOnce = value
    }

    fun createProfile(name: String, pin: String?) {
        profileManager.createProfile(name, pin)
        showMessage("Welcome to Piso, ${name.trim()}!")
    }

    fun updateDisplayName(name: String) {
        profileManager.updateDisplayName(name)
        showMessage("Profile name updated")
    }

    fun getLockoutRemainingSeconds(): Int {
        return profileManager.getLockoutRemainingSeconds()
    }

    fun verifyPin(pin: String): Boolean {
        return profileManager.verifyPin(pin)
    }

    fun unlockApp(pin: String): Boolean {
        return profileManager.unlock(pin)
    }

    fun lockApp() {
        _skipLockOnce = false
        if (profileManager.hasPin()) {
            profileManager.lock()
            showMessage("Piso locked")
        } else {
            showMessage("Set a PIN first in Settings to enable lock.")
        }
    }

    fun onAppBackgrounded() {
        if (_skipLockOnce) {
            _skipLockOnce = false
            return
        }
        if (profileManager.hasPin()) {
            profileManager.lock()
        }
    }

    fun setPin(pin: String) {
        profileManager.setPin(pin)
        showMessage("PIN set successfully")
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        val success = profileManager.changePin(oldPin, newPin)
        if (success) {
            showMessage("PIN changed successfully")
        }
        return success
    }

    fun removePin(oldPin: String): Boolean {
        val success = profileManager.removePin(oldPin)
        if (success) {
            showMessage("PIN removed")
        }
        return success
    }

    fun setThemeMode(mode: ThemeMode) {
        profileManager.setThemeMode(mode)
    }

    fun clearAllDataAndReset() {
        viewModelScope.launch {
            repository.clearAllData()
            profileManager.clearProfile()
            _currentTab.value = MainTab.HOME
            _moreSubScreen.value = null
            showMessage("Piso has been completely reset")
        }
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

    val categories: StateFlow<List<UserCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subcategories: StateFlow<List<UserSubcategory>> = repository.allSubcategories
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
    val currentMonthSummary: StateFlow<MonthlySummary> = transactions.map { txList ->
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

    // --- Category Spending for Current Month (Totals Map) ---
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

    // --- Detailed Spending Breakdown (Parent Category + Indented Subcategories) ---
    val currentMonthSpendingBreakdown: StateFlow<List<CategorySpendingBreakdown>> = transactions.map { txList ->
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

            // Check if any transactions in this category have a subcategory assigned
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

    // Map for fast breakdown lookup on Budgets screen
    val currentMonthBreakdownMap: StateFlow<Map<String, CategorySpendingBreakdown>> = currentMonthSpendingBreakdown.map { list ->
        list.associateBy { it.category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Category CRUD Operations ---
    fun addCategory(name: String, kind: String, onResult: (Boolean, String?) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            onResult(false, "Category name cannot be empty")
            return
        }
        viewModelScope.launch {
            val existing = repository.allCategories.first()
            if (existing.any { it.kind.equals(kind, ignoreCase = true) && it.name.equals(trimmed, ignoreCase = true) }) {
                onResult(false, "A $kind category named '$trimmed' already exists")
                return@launch
            }
            val newCat = UserCategory(name = trimmed, kind = kind, isArchived = false)
            repository.insertCategory(newCat)
            showMessage("Category '$trimmed' added")
            onResult(true, null)
        }
    }

    fun updateCategoryName(category: UserCategory, newName: String, onResult: (Boolean, String?) -> Unit) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            onResult(false, "Category name cannot be empty")
            return
        }
        if (trimmed.equals(category.name, ignoreCase = true)) {
            onResult(true, null)
            return
        }
        viewModelScope.launch {
            val existing = repository.allCategories.first()
            if (existing.any { it.id != category.id && it.kind.equals(category.kind, ignoreCase = true) && it.name.equals(trimmed, ignoreCase = true) }) {
                onResult(false, "A ${category.kind} category named '$trimmed' already exists")
                return@launch
            }
            repository.updateCategoryName(category, trimmed)
            showMessage("Category updated")
            onResult(true, null)
        }
    }

    fun toggleCategoryArchived(category: UserCategory) {
        viewModelScope.launch {
            val nextArchived = !category.isArchived
            repository.setCategoryArchived(category, nextArchived)
            showMessage(if (nextArchived) "${category.name} archived" else "${category.name} unarchived")
        }
    }

    fun deleteCategory(category: UserCategory) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showMessage("Category removed")
        }
    }

    // --- Subcategory CRUD Operations ---
    fun addSubcategory(parentCategoryName: String, name: String, onResult: (Boolean, String?) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            onResult(false, "Subcategory name cannot be empty")
            return
        }
        viewModelScope.launch {
            val existing = repository.allSubcategories.first()
            if (existing.any { it.parentCategoryName.equals(parentCategoryName, ignoreCase = true) && it.name.equals(trimmed, ignoreCase = true) }) {
                onResult(false, "Subcategory '$trimmed' already exists under $parentCategoryName")
                return@launch
            }
            val newSub = UserSubcategory(parentCategoryName = parentCategoryName, name = trimmed, isArchived = false)
            repository.insertSubcategory(newSub)
            showMessage("Subcategory '$trimmed' added")
            onResult(true, null)
        }
    }

    fun updateSubcategoryName(subcategory: UserSubcategory, newName: String, onResult: (Boolean, String?) -> Unit) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            onResult(false, "Subcategory name cannot be empty")
            return
        }
        if (trimmed.equals(subcategory.name, ignoreCase = true)) {
            onResult(true, null)
            return
        }
        viewModelScope.launch {
            val existing = repository.allSubcategories.first()
            if (existing.any { it.id != subcategory.id && it.parentCategoryName.equals(subcategory.parentCategoryName, ignoreCase = true) && it.name.equals(trimmed, ignoreCase = true) }) {
                onResult(false, "Subcategory '$trimmed' already exists under ${subcategory.parentCategoryName}")
                return@launch
            }
            repository.updateSubcategoryName(subcategory, trimmed)
            showMessage("Subcategory updated")
            onResult(true, null)
        }
    }

    fun toggleSubcategoryArchived(subcategory: UserSubcategory) {
        viewModelScope.launch {
            val nextArchived = !subcategory.isArchived
            repository.setSubcategoryArchived(subcategory, nextArchived)
            showMessage(if (nextArchived) "${subcategory.name} archived" else "${subcategory.name} unarchived")
        }
    }

    fun deleteSubcategory(subcategory: UserSubcategory) {
        viewModelScope.launch {
            repository.deleteSubcategory(subcategory)
            showMessage("Subcategory removed")
        }
    }

    // --- Account CRUD ---
    fun addAccount(account: Account) {
        viewModelScope.launch {
            repository.insertAccount(account)
            showMessage("Account created")
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
            showMessage("Account updated")
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            val hasTx = repository.hasTransactionsForAccount(account.id)
            if (hasTx) {
                showMessage("Move or delete this account’s activity first.")
                return@launch
            }
            repository.deleteAccount(account)
            showMessage("Account deleted")
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
