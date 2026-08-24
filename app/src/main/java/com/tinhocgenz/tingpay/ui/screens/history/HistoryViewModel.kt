package com.tinhocgenz.tingpay.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinhocgenz.tingpay.TingPayApp
import com.tinhocgenz.tingpay.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val filterType: String = "ALL" // "ALL", "CREDIT", "DEBIT"
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TingPayApp
    private val repo = app.transactionRepository

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow("ALL")

    val uiState: StateFlow<HistoryUiState> = combine(
        repo.getAllTransactions(),
        _searchQuery,
        _filterType
    ) { txs, query, filter ->
        val filtered = txs.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.description.contains(query, ignoreCase = true) ||
                    tx.bankCode.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)

            val matchesType = when (filter) {
                "CREDIT" -> tx.type.name == "CREDIT"
                "DEBIT" -> tx.type.name == "DEBIT"
                else -> true
            }

            matchesQuery && matchesType
        }
        HistoryUiState(transactions = filtered, searchQuery = query, filterType = filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun onFilterChanged(filter: String) {
        _filterType.value = filter
    }
}
