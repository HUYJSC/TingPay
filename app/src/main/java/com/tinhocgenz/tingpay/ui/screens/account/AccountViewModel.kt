package com.tinhocgenz.tingpay.ui.screens.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinhocgenz.tingpay.TingPayApp
import com.tinhocgenz.tingpay.data.database.BankListProvider
import com.tinhocgenz.tingpay.domain.model.BankAccount
import com.tinhocgenz.tingpay.domain.model.BankInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUiState(
    val accounts: List<BankAccount> = emptyList(),
    val availableBanks: List<BankInfo> = BankListProvider.supportedBanks,
    val selectedBank: BankInfo? = null,
    val accountNumber: String = "",
    val accountName: String = "",
    val isDefault: Boolean = true,
    val isSavedSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TingPayApp
    private val repo = app.bankAccountRepository

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    val accounts = repo.getAllAccounts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onBankSelected(bank: BankInfo) {
        _uiState.value = _uiState.value.copy(selectedBank = bank)
    }

    fun onAccountNumberChanged(number: String) {
        _uiState.value = _uiState.value.copy(accountNumber = number)
    }

    fun onAccountNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(accountName = name)
    }

    fun onDefaultChanged(isDefault: Boolean) {
        _uiState.value = _uiState.value.copy(isDefault = isDefault)
    }

    fun saveBankAccount() {
        val state = _uiState.value
        val bank = state.selectedBank
        if (bank == null) {
            _uiState.value = state.copy(errorMessage = "Vui lòng chọn ngân hàng")
            return
        }
        if (state.accountNumber.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Vui lòng nhập số tài khoản")
            return
        }
        if (state.accountName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Vui lòng nhập tên chủ tài khoản")
            return
        }

        viewModelScope.launch {
            val account = BankAccount(
                bankCode = bank.code,
                bankName = bank.shortName,
                bin = bank.bin,
                accountNumber = state.accountNumber.trim(),
                accountName = state.accountName.trim().uppercase(),
                isDefault = state.isDefault
            )
            repo.insertAccount(account)
            _uiState.value = state.copy(isSavedSuccess = true, errorMessage = null)
        }
    }

    fun setDefault(id: String) {
        viewModelScope.launch {
            repo.setDefaultAccount(id)
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            repo.deleteAccount(id)
        }
    }
}
