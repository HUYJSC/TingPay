package com.tinhocgenz.tingpay.ui.screens.cashier

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinhocgenz.tingpay.TingPayApp
import com.tinhocgenz.tingpay.core.security.PinSecurityManager
import com.tinhocgenz.tingpay.domain.model.BankAccount
import com.tinhocgenz.tingpay.domain.model.Order
import com.tinhocgenz.tingpay.domain.usecase.PaymentEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CashierUiState(
    val amountString: String = "",
    val currentOrder: Order? = null,
    val defaultAccount: BankAccount? = null,
    val isLocked: Boolean = false,
    val pinPromptVisible: Boolean = false,
    val enteredPin: String = "",
    val pinError: String? = null,
    val lastSuccessAmount: Long? = null
)

class CashierViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TingPayApp
    private val bankRepo = app.bankAccountRepository
    private val createOrderUseCase = app.createOrderUseCase
    private val processNotificationUseCase = app.processNotificationUseCase
    private val settingRepo = app.settingRepository
    private val pinSecurityManager = PinSecurityManager()

    private val _uiState = MutableStateFlow(CashierUiState())
    val uiState: StateFlow<CashierUiState> = _uiState.asStateFlow()

    init {
        loadData()
        listenPaymentEvents()
    }

    private fun loadData() {
        viewModelScope.launch {
            val acc = bankRepo.getDefaultAccount()
            val hasPin = settingRepo.getSetting("cashier_pin").isNotBlank()
            _uiState.value = _uiState.value.copy(
                defaultAccount = acc,
                isLocked = hasPin
            )
        }
    }

    private fun listenPaymentEvents() {
        viewModelScope.launch {
            processNotificationUseCase.events.collect { event ->
                if (event is PaymentEvent.PaymentReceived) {
                    val current = _uiState.value.currentOrder
                    if (current != null && (event.order?.id == current.id || event.transaction.amount == current.amount)) {
                        _uiState.value = _uiState.value.copy(
                            lastSuccessAmount = event.transaction.amount,
                            currentOrder = null,
                            amountString = ""
                        )
                    }
                }
            }
        }
    }

    fun onDigit(d: String) {
        val cur = _uiState.value.amountString
        if (cur.length < 10) {
            val newAmount = cur + d
            _uiState.value = _uiState.value.copy(amountString = newAmount)
            createQuickOrder(newAmount.toLongOrNull() ?: 0)
        }
    }

    fun onBackspace() {
        val cur = _uiState.value.amountString
        if (cur.isNotEmpty()) {
            val newAmount = cur.dropLast(1)
            _uiState.value = _uiState.value.copy(amountString = newAmount)
            if (newAmount.isNotEmpty()) {
                createQuickOrder(newAmount.toLongOrNull() ?: 0)
            } else {
                _uiState.value = _uiState.value.copy(currentOrder = null)
            }
        }
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(amountString = "", currentOrder = null)
    }

    private fun createQuickOrder(amount: Long) {
        if (amount <= 0) return
        viewModelScope.launch {
            val order = createOrderUseCase(amount, "Thu ngân POS")
            _uiState.value = _uiState.value.copy(currentOrder = order)
        }
    }

    fun showPinPrompt() {
        _uiState.value = _uiState.value.copy(pinPromptVisible = true, enteredPin = "", pinError = null)
    }

    fun hidePinPrompt() {
        _uiState.value = _uiState.value.copy(pinPromptVisible = false)
    }

    fun onPinEntered(pin: String, onExitGranted: () -> Unit) {
        viewModelScope.launch {
            val storedHash = settingRepo.getSetting("cashier_pin")
            if (storedHash.isBlank() || pinSecurityManager.verifyPin(pin, storedHash)) {
                _uiState.value = _uiState.value.copy(pinPromptVisible = false)
                onExitGranted()
            } else {
                _uiState.value = _uiState.value.copy(pinError = "Mã PIN không chính xác!")
            }
        }
    }
}
