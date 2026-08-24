package com.tinhocgenz.tingpay.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tinhocgenz.tingpay.domain.model.OrderStatus
import com.tinhocgenz.tingpay.domain.model.TransactionType

@Entity(
    tableName = "bank_accounts",
    indices = [Index(value = ["account_number", "bank_code"], unique = true)]
)
data class BankAccountEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "bank_code") val bankCode: String,
    @ColumnInfo(name = "bank_name") val bankName: String,
    val bin: String,
    @ColumnInfo(name = "account_number") val accountNumber: String,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    val enabled: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["order_code"], unique = true),
        Index(value = ["status"]),
        Index(value = ["amount", "status"])
    ]
)
data class OrderEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "order_code") val orderCode: String,
    val amount: Long,
    val description: String,
    @ColumnInfo(name = "bank_account_id") val bankAccountId: String,
    val status: OrderStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "paid_at") val paidAt: Long?,
    @ColumnInfo(name = "expired_at") val expiredAt: Long
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transaction_hash"], unique = true),
        Index(value = ["transaction_time"]),
        Index(value = ["matched_order_id"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "bank_code") val bankCode: String,
    @ColumnInfo(name = "account_number") val accountNumber: String,
    val amount: Long,
    val type: TransactionType,
    val sender: String,
    val description: String,
    @ColumnInfo(name = "transaction_time") val transactionTime: Long,
    @ColumnInfo(name = "notification_time") val notificationTime: Long,
    @ColumnInfo(name = "raw_message") val rawMessage: String,
    @ColumnInfo(name = "transaction_hash") val transactionHash: String,
    @ColumnInfo(name = "matched_order_id") val matchedOrderId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
