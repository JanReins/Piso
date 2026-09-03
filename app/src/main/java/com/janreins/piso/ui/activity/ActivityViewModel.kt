package com.janreins.piso.ui.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.janreins.piso.data.local.AppDatabase
import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.Transaction
import com.janreins.piso.data.models.UserCategory
import com.janreins.piso.data.models.UserSubcategory
import com.janreins.piso.data.repository.FinanceRepository
import com.janreins.piso.util.DateUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class ActivityUiState(
    val isLoading: Boolean = true,
    val selectedMonthKey: String = DateUtil.getCurrentMonthKey(),
    val activityFilter: String = "ALL", // ALL, INCOME, EXPENSE, TRANSFER
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<UserCategory> = emptyList(),
    val subcategories: List<UserSubcategory> = emptyList(),
    val isEmpty: Boolean = false
)

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    private val _selectedMonthKey = MutableStateFlow(DateUtil.getCurrentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _activityFilter = MutableStateFlow("ALL")
    val activityFilter: StateFlow<String> = _activityFilter.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db)
    }

    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<UserCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subcategories: StateFlow<List<UserSubcategory>> = repository.allSubcategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterState = combine(_selectedMonthKey, _activityFilter) { monthKey, filter ->
        monthKey to filter
    }

    val uiState: StateFlow<ActivityUiState> = combine(
        filterState,
        transactions,
        accounts,
        categories,
        subcategories
    ) { (monthKey, filter), txs, accs, cats, subs ->
        ActivityUiState(
            isLoading = false,
            selectedMonthKey = monthKey,
            activityFilter = filter,
            transactions = txs,
            accounts = accs,
            categories = cats,
            subcategories = subs,
            isEmpty = txs.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActivityUiState())

    fun setSelectedMonthKey(key: String) {
        _selectedMonthKey.value = key
    }

    fun setActivityFilter(filter: String) {
        _activityFilter.value = filter
    }

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

    fun showMessage(message: String) {
        viewModelScope.launch {
            _messageEvent.emit(message)
        }
    }
}
