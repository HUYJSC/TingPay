package com.tinhocgenz.tingpay.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tinhocgenz.tingpay.data.database.entity.AppSettingEntity
import com.tinhocgenz.tingpay.data.database.entity.BankAccountEntity
import com.tinhocgenz.tingpay.data.database.entity.OrderEntity
import com.tinhocgenz.tingpay.data.database.entity.TransactionEntity
import com.tinhocgenz.tingpay.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY is_default DESC, created_at DESC")
    fun getAllAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultAccount(): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BankAccountEntity)

    @Update
    suspend fun updateAccount(account: BankAccountEntity)

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("UPDATE bank_accounts SET is_default = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE bank_accounts SET is_default = 1 WHERE id = :id")
    suspend fun setDefaultFlag(id: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE status = 'WAITING' ORDER BY created_at DESC")
    fun getActiveOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY created_at DESC LIMIT :limit")
    fun getRecentOrders(limit: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE order_code = :code LIMIT 1")
    suspend fun getOrderByCode(code: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE amount = :amount AND status = 'WAITING' AND created_at >= :fromTime")
    suspend fun getWaitingOrdersByAmount(amount: Long, fromTime: Long): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status, paid_at = :paidAt WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: OrderStatus, paidAt: Long?)

    @Query("UPDATE orders SET status = 'CANCELLED' WHERE id = :id")
    suspend fun cancelOrder(id: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY transaction_time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY transaction_time DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transaction_time BETWEEN :startTime AND :endTime ORDER BY transaction_time DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE transaction_hash = :hash")
    suspend fun countFingerprint(hash: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE transaction_time BETWEEN :startTime AND :endTime AND type = 'CREDIT'")
    suspend fun getCreditTransactionsBetween(startTime: Long, endTime: Long): List<TransactionEntity>
}

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun observeSetting(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSettingEntity)
}
