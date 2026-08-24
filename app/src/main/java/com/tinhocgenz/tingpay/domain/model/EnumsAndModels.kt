package com.tinhocgenz.tingpay.domain.model

enum class OrderStatus {
    CREATED,
    WAITING,
    PAID,
    REVIEW,
    EXPIRED,
    CANCELLED
}

enum class TransactionType {
    CREDIT,  // Tiền vào (+)
    DEBIT,   // Tiền ra (-)
    UNKNOWN  // Không xác định
}

data class BankAccount(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bankCode: String,      // e.g. "MB", "VCB", "TCB", "ACB", "BIDV", "CTG"
    val bankName: String,      // e.g. "Ngân hàng Quân Đội (MBBank)"
    val bin: String,           // e.g. "970422"
    val accountNumber: String,
    val accountName: String,
    val isDefault: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class Order(
    val id: String = java.util.UUID.randomUUID().toString(),
    val orderCode: String,      // e.g. "TP8291"
    val amount: Long,
    val description: String = "",
    val bankAccountId: String,
    val status: OrderStatus = OrderStatus.WAITING,
    val createdAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val expiredAt: Long = System.currentTimeMillis() + 15 * 60 * 1000 // 15 mins default
)

data class Transaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bankCode: String,
    val accountNumber: String = "",
    val amount: Long,
    val type: TransactionType,
    val sender: String = "",
    val description: String = "",
    val transactionTime: Long = System.currentTimeMillis(),
    val notificationTime: Long = System.currentTimeMillis(),
    val rawMessage: String = "",
    val transactionHash: String,
    val matchedOrderId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppSetting(
    val key: String,
    val value: String
)

data class BankInfo(
    val code: String,
    val shortName: String,
    val name: String,
    val bin: String,
    val packageName: String = ""
)

data class DashboardStatistics(
    val todayRevenue: Long = 0,
    val todayTransactionCount: Int = 0,
    val averageTransactionValue: Long = 0,
    val successOrdersCount: Int = 0,
    val pendingOrdersCount: Int = 0,
    val reviewOrdersCount: Int = 0,
    val hourlyRevenue: Map<Int, Long> = emptyMap(),
    val bankRevenue: Map<String, Long> = emptyMap()
)
