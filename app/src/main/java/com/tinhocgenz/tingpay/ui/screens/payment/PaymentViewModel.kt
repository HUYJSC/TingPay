package com.tinhocgenz.tingpay.ui.screens.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinhocgenz.tingpay.TingPayApp
import com.tinhocgenz.tingpay.domain.model.BankAccount
import com.tinhocgenz.tingpay.domain.model.Order
import com.tinhocgenz.tingpay.domain.model.OrderStatus
import com.tinhocgenz.tingpay.domain.usecase.PaymentEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentUiState(
    val amountString: String = "",
    val description: String = "",
    val currentOrder: Order? = null,
    val defaultAccount: BankAccount? = null,
    val isPaidSuccess: Boolean = false,
    val paidAmount: Long = 0,
    val errorMessage: String? = null
)

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TingPayApp
    private val createOrderUseCase = app.createOrderUseCase
    private val orderRepo = app.orderRepository
    private val bankRepo = app.bankAccountRepository
    private val processNotificationUseCase = app.processNotificationUseCase

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        loadDefaultAccount()
        listenPaymentEvents()
    }

    private fun loadDefaultAccount() {
        viewModelScope.launch {
            val account = bankRepo.getDefaultAccount()
            _uiState.value = _uiState.value.copy(defaultAccount = account)
        }
    }

    private fun listenPaymentEvents() {
        viewModelScope.launch {
            processNotificationUseCase.events.collect { event ->
                if (event is PaymentEvent.PaymentReceived) {
                    val currentOrd = _uiState.value.currentOrder
                    if (currentOrd != null && (event.order?.id == currentOrd.id || event.transaction.amount == currentOrd.amount)) {
                        _uiState.value = _uiState.value.copy(
                            isPaidSuccess = true,
                            paidAmount = event.transaction.amount
                        )
                    }
                }
            }
        }
    }

    fun onDigit(digit: String) {
        val cur = _uiState.value.amountString
        if (cur.length < 10) { // Max 10 digits
            _uiState.value = _uiState.value.copy(amountString = cur + digit)
        }
    }

    fun onBackspace() {
        val cur = _uiState.value.amountString
        if (cur.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(amountString = cur.dropLast(1))
        }
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(amountString = "")
    }

    fun onDescriptionChange(desc: String) {
        _uiState.value = _uiState.value.copy(description = desc)
    }

    fun createOrder(onSuccess: (Order) -> Unit) {
        val amount = _uiState.value.amountString.toLongOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Vui lòng nhập số tiền hợp lệ")
            return
        }

        viewModelScope.launch {
            val order = createOrderUseCase(amount, _uiState.value.description)
            if (order != null) {
                _uiState.value = _uiState.value.copy(currentOrder = order, errorMessage = null)
                onSuccess(order)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Chưa thiết lập tài khoản nhận tiền")
            }
        }
    }

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            val order = orderRepo.getOrderById(orderId)
            val account = order?.let { bankRepo.getAccountById(it.bankAccountId) }
            _uiState.value = _uiState.value.copy(
                currentOrder = order,
                defaultAccount = account
            )
        }
    }
}
