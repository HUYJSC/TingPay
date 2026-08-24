package com.tinhocgenz.tingpay.domain.repository

import com.tinhocgenz.tingpay.domain.model.BankAccount
import com.tinhocgenz.tingpay.domain.model.DashboardStatistics
import com.tinhocgenz.tingpay.domain.model.Order
import com.tinhocgenz.tingpay.domain.model.OrderStatus
import com.tinhocgenz.tingpay.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface BankAccountRepository {
    fun getAllAccounts(): Flow<List<BankAccount>>
    suspend fun getDefaultAccount(): BankAccount?
    suspend fun getAccountById(id: String): BankAccount?
    suspend fun insertAccount(account: BankAccount)
    suspend fun updateAccount(account: BankAccount)
    suspend fun deleteAccount(id: String)
    suspend fun setDefaultAccount(id: String)
}

interface OrderRepository {
    fun getActiveOrders(): Flow<List<Order>>
    fun getRecentOrders(limit: Int = 20): Flow<List<Order>>
    suspend fun getOrderById(id: String): Order?
    suspend fun getOrderByCode(code: String): Order?
    suspend fun getWaitingOrdersByAmount(amount: Long, fromTime: Long): List<Order>
    suspend fun insertOrder(order: Order)
    suspend fun updateOrderStatus(id: String, status: OrderStatus, paidAt: Long? = null)
    suspend fun cancelOrder(id: String)
}

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int = 30): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
    suspend fun isFingerprintExists(hash: String): Boolean
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun getStatistics(startTime: Long, endTime: Long): DashboardStatistics
}

interface SettingRepository {
    suspend fun getSetting(key: String, defaultValue: String = ""): String
    suspend fun saveSetting(key: String, value: String)
    fun observeSetting(key: String, defaultValue: String = ""): Flow<String>
}
