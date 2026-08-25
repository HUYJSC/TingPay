package com.tinhocgenz.tingpay.domain.usecase

import com.tinhocgenz.tingpay.core.audio.AudioConfig
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
        val randomNum = Random().nextInt(9000) + 1000 // 4 chữ số 1000-9999
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
        // 1. Phân tích cú pháp thông báo ngân hàng
        val parsed = parserRegistry.parse(notification)
        if (parsed == null) {
            _events.tryEmit(PaymentEvent.NotificationIgnored("Không nhận diện được định dạng ngân hàng"))
            return
        }

        // 2. Chống giao dịch trùng lặp (SHA-256 Fingerprint)
        if (duplicateDetector.isDuplicate(parsed)) {
            _events.tryEmit(PaymentEvent.NotificationIgnored("Bỏ qua thông báo giao dịch bị trùng lặp"))
            return
        }

        // 3. Chuẩn hóa sang Transaction Model
        val transaction = normalizer.normalize(parsed, notification.postTime)

        // 4. Chỉ phát âm thanh và khớp lệnh đối với giao dịch TIỀN VÀO (CREDIT)
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

            // Lưu giao dịch vào SQLite DB
            try {
                transactionRepository.insertTransaction(finalTransaction)
            } catch (e: Exception) {
                return // Nếu bị lỗi trùng hash trong race condition -> dừng
            }

            // 5. Kích hoạt Loa Ting + Đọc tiếng Việt qua Audio Engine
            val audioModeStr = settingRepository.getSetting("audio_mode", AudioNotificationMode.TING_AND_AMOUNT.name)
            val audioMode = try {
                AudioNotificationMode.valueOf(audioModeStr)
            } catch (e: Exception) {
                AudioNotificationMode.TING_AND_AMOUNT
            }

            val speedRateStr = settingRepository.getSetting("speech_rate", "0.95")
            val speechRate = speedRateStr.toFloatOrNull() ?: 0.95f

            val audioConfig = AudioConfig(
                mode = audioMode,
                speechRate = speechRate
            )

            audioEngine.enqueuePaymentAudio(
                amount = finalTransaction.amount,
                bankName = finalTransaction.bankCode,
                config = audioConfig
            )
        } else {
            // Giao dịch DEBIT (trừ tiền) chỉ lưu lịch sử, TUYỆT ĐỐI KHÔNG PHÁT LOA
            try {
                transactionRepository.insertTransaction(transaction)
            } catch (e: Exception) {
                // Bỏ qua
            }
        }
    }
}
