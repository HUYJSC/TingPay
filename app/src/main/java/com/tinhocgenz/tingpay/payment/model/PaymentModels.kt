package com.tinhocgenz.tingpay.payment.model

import com.tinhocgenz.tingpay.domain.model.TransactionType

data class BankNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val bigText: String? = null,
    val postTime: Long = System.currentTimeMillis()
) {
    val fullContent: String
        get() = listOfNotNull(title, text, bigText).filter { it.isNotBlank() }.joinToString("\n")
}

data class ParsedTransaction(
    val bankCode: String,
    val accountNumber: String = "",
    val amount: Long,
    val type: TransactionType,
    val sender: String = "",
    val description: String = "",
    val transactionTime: Long = System.currentTimeMillis(),
    val rawMessage: String = ""
)
