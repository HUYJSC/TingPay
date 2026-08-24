package com.tinhocgenz.tingpay.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinhocgenz.tingpay.TingPayApp
import com.tinhocgenz.tingpay.core.notification.NotificationHelper
import com.tinhocgenz.tingpay.domain.model.BankAccount
import com.tinhocgenz.tingpay.domain.model.DashboardStatistics
import com.tinhocgenz.tingpay.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val defaultAccount: BankAccount? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val statistics: DashboardStatistics = DashboardStatistics(),
    val isServiceEnabled: Boolean = false,
    val isLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TingPayApp
    private val bankAccountRepo = app.bankAccountRepository
    private val transactionRepo = app.transactionRepository

    private val _isServiceEnabled = MutableStateFlow(NotificationHelper.isNotificationServiceEnabled(application))
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        bankAccountRepo.getAllAccounts(),
        transactionRepo.getRecentTransactions(limit = 10),
        _isServiceEnabled
    ) { accounts, transactions, serviceEnabled ->
        val defaultAcc = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
        val stats = calculateTodayStats(transactions)
        HomeUiState(
            defaultAccount = defaultAcc,
            recentTransactions = transactions,
            statistics = stats,
            isServiceEnabled = serviceEnabled,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun checkServiceStatus() {
        _isServiceEnabled.value = NotificationHelper.isNotificationServiceEnabled(getApplication())
    }

    private fun calculateTodayStats(transactions: List<Transaction>): DashboardStatistics {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfToday = calendar.timeInMillis

        val todayTxs = transactions.filter { it.transactionTime >= startOfToday && it.type.name == "CREDIT" }
        val revenue = todayTxs.sumOf { it.amount }
        val count = todayTxs.size
        val avg = if (count > 0) revenue / count else 0

        return DashboardStatistics(
            todayRevenue = revenue,
            todayTransactionCount = count,
            averageTransactionValue = avg
        )
    }
}
