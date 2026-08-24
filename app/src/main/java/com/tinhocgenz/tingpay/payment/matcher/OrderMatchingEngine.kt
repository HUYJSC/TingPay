package com.tinhocgenz.tingpay.payment.matcher

import com.tinhocgenz.tingpay.domain.model.Order
import com.tinhocgenz.tingpay.domain.model.OrderStatus
import com.tinhocgenz.tingpay.domain.model.Transaction
import com.tinhocgenz.tingpay.domain.model.TransactionType
import com.tinhocgenz.tingpay.domain.repository.OrderRepository

sealed interface MatchResult {
    data class AutoPaid(val order: Order, val score: Int, val reason: String) : MatchResult
    data class NeedsReview(val candidateOrders: List<Order>, val score: Int, val reason: String) : MatchResult
    data class NoMatch(val reason: String) : MatchResult
}

class OrderMatchingEngine(
    private val orderRepository: OrderRepository
) {

    suspend fun match(transaction: Transaction): MatchResult {
        // 1. Only CREDIT transactions can be matched with orders
        if (transaction.type != TransactionType.CREDIT) {
            return MatchResult.NoMatch("Giao dịch không phải tiền vào (CREDIT)")
        }

        val descNormalized = transaction.description.uppercase()

        // 2. Exact OrderCode Matching (Deterministic matching)
        // Search if description contains any existing OrderCode
        val waitingOrders = orderRepository.getWaitingOrdersByAmount(
            amount = transaction.amount,
            fromTime = transaction.transactionTime - 30 * 60 * 1000 // Last 30 mins window
        )

        // Try exact match by OrderCode first
        val exactMatchOrder = waitingOrders.firstOrNull { order ->
            val code = order.orderCode.uppercase()
            descNormalized.contains(code) || descNormalized.contains(code.replace("TP", ""))
        }

        if (exactMatchOrder != null) {
            return MatchResult.AutoPaid(
                order = exactMatchOrder,
                score = 100,
                reason = "Khớp chính xác mã đơn hàng ${exactMatchOrder.orderCode}"
            )
        }

        // 3. Amount-based Probabilistic Matching
        if (waitingOrders.isEmpty()) {
            return MatchResult.NoMatch("Không tìm thấy đơn hàng chờ nào có số tiền ${transaction.amount}đ")
        }

        if (waitingOrders.size == 1) {
            // Exactly one single order waiting with this amount
            val singleOrder = waitingOrders.first()
            return MatchResult.AutoPaid(
                order = singleOrder,
                score = 90,
                reason = "Khớp đơn duy nhất theo số tiền ${transaction.amount}đ (Khách không ghi mã đơn)"
            )
        }

        // 4. Multiple conflicting orders with the exact same amount
        // Example: 2 customers paying 500,000 VND at the same time without OrderCode
        return MatchResult.NeedsReview(
            candidateOrders = waitingOrders,
            score = 75,
            reason = "Phát hiện ${waitingOrders.size} đơn hàng trùng số tiền ${transaction.amount}đ. Cần thu ngân xác nhận thủ công."
        )
    }
}
