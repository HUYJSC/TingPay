package com.tinhocgenz.tingpay.domain.usecase

import com.tinhocgenz.tingpay.core.audio.AudioEngine
import com.tinhocgenz.tingpay.core.audio.AudioNotificationMode
import com.tinhocgenz.tingpay.domain.model.Order
import com.tinhocgenz.tingpay.domain.model.OrderStatus
import com.tinhocgenz.tingpay.domain.model.Transaction
import com.tinhocgenz.tingpay.domain.model.TransactionType
import com.tinhocgenz.tingpay.domain.repository.BankAccountRepository
import com.tinhocgenz.tingpay.domain.repository.OrderRepository
import com.tinhocgenz.tingpay.domain.repository.SettingRepository
import com.tinhocgenz.tingpay.domain.repository.TransactionRepository
import com.tinhocgenz.tingpay.payment.duplicate.DuplicateDetector
import com.tinhocgenz.tingpay.payment.matcher.MatchResult
import com.tinhocgenz.tingpay.payment.matcher.OrderMatchingEngine
import com.tinhocgenz.tingpay.payment.model.BankNotification
import com.tinhocgenz.tingpay.payment.normalizer.TransactionNormalizer
import com.tinhocgenz.tingpay.payment.parser.BankParserRegistry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Random

class CreateOrderUseCase(
    private val orderRepository: OrderRepository,
    private val bankAccountRepository: BankAccountRepository
) {
    suspend operator fun invoke(amount: Long, description: String = "", customAccountId: String? = null): Order? {
        val account = if (customAccountId != null) {
            bankAccountRepository.getAccountById(customAccountId)
        } else {
            bankAccountRepository.getDefaultAccount()
        } ?: return null

        val orderCode = generateOrderCode()
        val order = Order(
            orderCode = orderCode,
            amount = amount,
            description = description,
            bankAccountId = account.id,
            status = OrderStatus.WAITING
        )

        orderRepository.insertOrder(order)
        return order
    }

    private fun generateOrderCode(): String {
        val randomNum = Random().nextInt(9000) + 1000 // 4 digits 1000-9999
        return "TP$randomNum"
    }
}

sealed interface PaymentEvent {
    data class PaymentReceived(val transaction: Transaction, val order: Order?) : PaymentEvent
    data class PaymentNeedsReview(val transaction: Transaction, val candidateOrders: List<Order>) : PaymentEvent
    data class NotificationIgnored(val reason: String) : PaymentEvent
}

class ProcessNotificationUseCase(
    private val parserRegistry: BankParserRegistry,
    private val duplicateDetector: DuplicateDetector,
    private val normalizer: TransactionNormalizer,
    private val matchingEngine: OrderMatchingEngine,
    private val transactionRepository: TransactionRepository,
    private val orderRepository: OrderRepository,
    private val settingRepository: SettingRepository,
    private val audioEngine: AudioEngine
) {
    private val _events = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PaymentEvent> = _events.asSharedFlow()

    suspend operator fun invoke(notification: BankNotification) {
        // 1. Parse Notification
        val parsed = parserRegistry.parse(notification)
        if (parsed == null) {
            _events.tryEmit(PaymentEvent.NotificationIgnored("Không thể nhận diện cú pháp ngân hàng"))
            return
        }

        // 2. Check for Duplicates (SHA-256 Fingerprint)
        if (duplicateDetector.isDuplicate(parsed)) {
            _events.tryEmit(PaymentEvent.NotificationIgnored("Bỏ qua thông báo giao dịch trùng"))
            return
        }

        // 3. Normalize into Transaction Model
        val transaction = normalizer.normalize(parsed, notification.postTime)

        // 4. If transaction is CREDIT (Money in), execute Matching & Audio
        if (transaction.type == TransactionType.CREDIT) {
            val matchResult = matchingEngine.match(transaction)

            var finalTransaction = transaction
            when (matchResult) {
                is MatchResult.AutoPaid -> {
                    finalTransaction = transaction.copy(matchedOrderId = matchResult.order.id)
                    orderRepository.updateOrderStatus(
                        id = matchResult.order.id,
                        status = OrderStatus.PAID,
                        paidAt = transaction.transactionTime
                    )
                    _events.tryEmit(PaymentEvent.PaymentReceived(finalTransaction, matchResult.order))
                }
                is MatchResult.NeedsReview -> {
                    _events.tryEmit(PaymentEvent.PaymentNeedsReview(finalTransaction, matchResult.candidateOrders))
                }
                is MatchResult.NoMatch -> {
                    _events.tryEmit(PaymentEvent.PaymentReceived(finalTransaction, null))
                }
            }

            // Save transaction to DB
            try {
                transactionRepository.insertTransaction(finalTransaction)
            } catch (e: Exception) {
                // If race condition hit unique hash, ignore
                return
            }

            // Trigger Ting Sound & Vietnamese Text-To-Speech
            val audioModeStr = settingRepository.getSetting("audio_mode", AudioNotificationMode.TING_AND_AMOUNT.name)
            val audioMode = try {
                AudioNotificationMode.valueOf(audioModeStr)
            } catch (e: Exception) {
                AudioNotificationMode.TING_AND_AMOUNT
            }

            audioEngine.notifyPaymentReceived(
                amount = finalTransaction.amount,
                bankName = finalTransaction.bankCode,
                mode = audioMode
            )
        } else {
            // Save DEBIT transaction for history without audio
            try {
                transactionRepository.insertTransaction(transaction)
            } catch (e: Exception) {
                // Ignore duplicate
            }
        }
    }
}
