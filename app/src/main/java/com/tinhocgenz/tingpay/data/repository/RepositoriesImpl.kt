package com.tinhocgenz.tingpay.data.repository

import com.tinhocgenz.tingpay.data.database.TingPayDatabase
import com.tinhocgenz.tingpay.data.database.entity.AppSettingEntity
import com.tinhocgenz.tingpay.data.database.entity.BankAccountEntity
import com.tinhocgenz.tingpay.data.database.entity.OrderEntity
import com.tinhocgenz.tingpay.data.database.entity.TransactionEntity
import com.tinhocgenz.tingpay.domain.model.AppSetting
import com.tinhocgenz.tingpay.domain.model.BankAccount
import com.tinhocgenz.tingpay.domain.model.DashboardStatistics
import com.tinhocgenz.tingpay.domain.model.Order
import com.tinhocgenz.tingpay.domain.model.OrderStatus
import com.tinhocgenz.tingpay.domain.model.Transaction
import com.tinhocgenz.tingpay.domain.repository.BankAccountRepository
import com.tinhocgenz.tingpay.domain.repository.OrderRepository
import com.tinhocgenz.tingpay.domain.repository.SettingRepository
import com.tinhocgenz.tingpay.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class BankAccountRepositoryImpl(
    private val db: TingPayDatabase
) : BankAccountRepository {

    override fun getAllAccounts(): Flow<List<BankAccount>> {
        return db.bankAccountDao().getAllAccounts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getDefaultAccount(): BankAccount? {
        return db.bankAccountDao().getDefaultAccount()?.toDomain()
    }

    override suspend fun getAccountById(id: String): BankAccount? {
        return db.bankAccountDao().getAccountById(id)?.toDomain()
    }

    override suspend fun insertAccount(account: BankAccount) {
        if (account.isDefault) {
            db.bankAccountDao().clearDefaultFlag()
        }
        db.bankAccountDao().insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: BankAccount) {
        if (account.isDefault) {
            db.bankAccountDao().clearDefaultFlag()
        }
        db.bankAccountDao().updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(id: String) {
        db.bankAccountDao().deleteAccount(id)
    }

    override suspend fun setDefaultAccount(id: String) {
        db.bankAccountDao().clearDefaultFlag()
        db.bankAccountDao().setDefaultFlag(id)
    }

    private fun BankAccountEntity.toDomain() = BankAccount(
        id = id,
        bankCode = bankCode,
        bankName = bankName,
        bin = bin,
        accountNumber = accountNumber,
        accountName = accountName,
        isDefault = isDefault,
        enabled = enabled,
        createdAt = createdAt
    )

    private fun BankAccount.toEntity() = BankAccountEntity(
        id = id,
        bankCode = bankCode,
        bankName = bankName,
        bin = bin,
        accountNumber = accountNumber,
        accountName = accountName,
        isDefault = isDefault,
        enabled = enabled,
        createdAt = createdAt
    )
}

class OrderRepositoryImpl(
    private val db: TingPayDatabase
) : OrderRepository {

    override fun getActiveOrders(): Flow<List<Order>> {
        return db.orderDao().getActiveOrders().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentOrders(limit: Int): Flow<List<Order>> {
        return db.orderDao().getRecentOrders(limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getOrderById(id: String): Order? {
        return db.orderDao().getOrderById(id)?.toDomain()
    }

    override suspend fun getOrderByCode(code: String): Order? {
        return db.orderDao().getOrderByCode(code)?.toDomain()
    }

    override suspend fun getWaitingOrdersByAmount(amount: Long, fromTime: Long): List<Order> {
        return db.orderDao().getWaitingOrdersByAmount(amount, fromTime).map { it.toDomain() }
    }

    override suspend fun insertOrder(order: Order) {
        db.orderDao().insertOrder(order.toEntity())
    }

    override suspend fun updateOrderStatus(id: String, status: OrderStatus, paidAt: Long?) {
        db.orderDao().updateOrderStatus(id, status, paidAt)
    }

    override suspend fun cancelOrder(id: String) {
        db.orderDao().cancelOrder(id)
    }

    private fun OrderEntity.toDomain() = Order(
        id = id,
        orderCode = orderCode,
        amount = amount,
        description = description,
        bankAccountId = bankAccountId,
        status = status,
        createdAt = createdAt,
        paidAt = paidAt,
        expiredAt = expiredAt
    )

    private fun Order.toEntity() = OrderEntity(
        id = id,
        orderCode = orderCode,
        amount = amount,
        description = description,
        bankAccountId = bankAccountId,
        status = status,
        createdAt = createdAt,
        paidAt = paidAt,
        expiredAt = expiredAt
    )
}

class TransactionRepositoryImpl(
    private val db: TingPayDatabase
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return db.transactionDao().getAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return db.transactionDao().getRecentTransactions(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return db.transactionDao().getTransactionsByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun isFingerprintExists(hash: String): Boolean {
        return db.transactionDao().countFingerprint(hash) > 0
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        db.transactionDao().insertTransaction(transaction.toEntity())
    }

    override suspend fun getStatistics(startTime: Long, endTime: Long): DashboardStatistics {
        val txs = db.transactionDao().getCreditTransactionsBetween(startTime, endTime)
        val todayRevenue = txs.sumOf { it.amount }
        val count = txs.size
        val avg = if (count > 0) todayRevenue / count else 0

        val hourlyMap = mutableMapOf<Int, Long>()
        val bankMap = mutableMapOf<String, Long>()

        val calendar = Calendar.getInstance()
        for (tx in txs) {
            calendar.timeInMillis = tx.transactionTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyMap[hour] = (hourlyMap[hour] ?: 0L) + tx.amount
            bankMap[tx.bankCode] = (bankMap[tx.bankCode] ?: 0L) + tx.amount
        }

        return DashboardStatistics(
            todayRevenue = todayRevenue,
            todayTransactionCount = count,
            averageTransactionValue = avg,
            hourlyRevenue = hourlyMap,
            bankRevenue = bankMap
        )
    }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        bankCode = bankCode,
        accountNumber = accountNumber,
        amount = amount,
        type = type,
        sender = sender,
        description = description,
        transactionTime = transactionTime,
        notificationTime = notificationTime,
        rawMessage = rawMessage,
        transactionHash = transactionHash,
        matchedOrderId = matchedOrderId,
        createdAt = createdAt
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        bankCode = bankCode,
        accountNumber = accountNumber,
        amount = amount,
        type = type,
        sender = sender,
        description = description,
        transactionTime = transactionTime,
        notificationTime = notificationTime,
        rawMessage = rawMessage,
        transactionHash = transactionHash,
        matchedOrderId = matchedOrderId,
        createdAt = createdAt
    )
}

class SettingRepositoryImpl(
    private val db: TingPayDatabase
) : SettingRepository {

    override suspend fun getSetting(key: String, defaultValue: String): String {
        return db.appSettingDao().getSetting(key) ?: defaultValue
    }

    override suspend fun saveSetting(key: String, value: String) {
        db.appSettingDao().saveSetting(AppSettingEntity(key, value))
    }

    override fun observeSetting(key: String, defaultValue: String): Flow<String> {
        return db.appSettingDao().observeSetting(key).map { it ?: defaultValue }
    }
}
