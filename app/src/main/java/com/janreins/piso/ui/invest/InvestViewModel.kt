package com.janreins.piso.ui.invest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.janreins.piso.data.local.AppDatabase
import com.janreins.piso.data.models.Investment
import com.janreins.piso.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class InvestUiState(
    val isLoading: Boolean = true,
    val investments: List<Investment> = emptyList(),
    val isEmpty: Boolean = false
)

class InvestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db)
    }

    val investments: StateFlow<List<Investment>> = repository.allInvestments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<InvestUiState> = investments.map { invs ->
        InvestUiState(
            isLoading = false,
            investments = invs,
            isEmpty = invs.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InvestUiState())

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

    fun showMessage(message: String) {
        viewModelScope.launch {
            _messageEvent.emit(message)
        }
    }
}
