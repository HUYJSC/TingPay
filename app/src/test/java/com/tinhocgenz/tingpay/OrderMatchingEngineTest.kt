package com.tinhocgenz.tingpay

import com.tinhocgenz.tingpay.domain.model.Order
import com.tinhocgenz.tingpay.domain.model.OrderStatus
import com.tinhocgenz.tingpay.domain.model.Transaction
import com.tinhocgenz.tingpay.domain.model.TransactionType
import com.tinhocgenz.tingpay.domain.repository.OrderRepository
import com.tinhocgenz.tingpay.payment.matcher.MatchResult
import com.tinhocgenz.tingpay.payment.matcher.OrderMatchingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderMatchingEngineTest {

    private class FakeOrderRepository(private val waitingOrders: List<Order>) : OrderRepository {
        override fun getActiveOrders(): Flow<List<Order>> = emptyFlow()
        override fun getRecentOrders(limit: Int): Flow<List<Order>> = emptyFlow()
        override suspend fun getOrderById(id: String): Order? = waitingOrders.firstOrNull { it.id == id }
        override suspend fun getOrderByCode(code: String): Order? = waitingOrders.firstOrNull { it.orderCode == code }
        override suspend fun getWaitingOrdersByAmount(amount: Long, fromTime: Long): List<Order> {
            return waitingOrders.filter { it.amount == amount && it.status == OrderStatus.WAITING }
        }
        override suspend fun insertOrder(order: Order) {}
        override suspend fun updateOrderStatus(id: String, status: OrderStatus, paidAt: Long?) {}
        override suspend fun cancelOrder(id: String) {}
    }

    @Test
    fun `test exact OrderCode match gives AutoPaid score 100`() = runBlocking {
        val order1 = Order(id = "1", orderCode = "TP8899", amount = 500000, bankAccountId = "acc1")
        val engine = OrderMatchingEngine(FakeOrderRepository(listOf(order1)))

        val tx = Transaction(
            bankCode = "MB",
            amount = 500000,
            type = TransactionType.CREDIT,
            description = "Nguyen Van B chuyen tien TP8899",
            transactionHash = "hash1"
        )

        val result = engine.match(tx)
        assertTrue(result is MatchResult.AutoPaid)
        val autoPaid = result as MatchResult.AutoPaid
        assertEquals("1", autoPaid.order.id)
        assertEquals(100, autoPaid.score)
    }

    @Test
    fun `test single amount match when customer forgot OrderCode gives AutoPaid score 90`() = runBlocking {
        val order1 = Order(id = "1", orderCode = "TP1111", amount = 250000, bankAccountId = "acc1")
        val engine = OrderMatchingEngine(FakeOrderRepository(listOf(order1)))

        val tx = Transaction(
            bankCode = "VCB",
            amount = 250000,
            type = TransactionType.CREDIT,
            description = "Chuyen tien mua hang",
            transactionHash = "hash2"
        )

        val result = engine.match(tx)
        assertTrue(result is MatchResult.AutoPaid)
        val autoPaid = result as MatchResult.AutoPaid
        assertEquals("1", autoPaid.order.id)
        assertEquals(90, autoPaid.score)
    }

    @Test
    fun `test conflict when 2 customers pay 500k at the same time without OrderCode gives NeedsReview`() = runBlocking {
        val order1 = Order(id = "1", orderCode = "TP1111", amount = 500000, bankAccountId = "acc1")
        val order2 = Order(id = "2", orderCode = "TP2222", amount = 500000, bankAccountId = "acc1")
        val engine = OrderMatchingEngine(FakeOrderRepository(listOf(order1, order2)))

        val tx = Transaction(
            bankCode = "TCB",
            amount = 500000,
            type = TransactionType.CREDIT,
            description = "Chuyen tien", // No order code
            transactionHash = "hash3"
        )

        val result = engine.match(tx)
        assertTrue(result is MatchResult.NeedsReview)
        val review = result as MatchResult.NeedsReview
        assertEquals(2, review.candidateOrders.size)
    }
}
