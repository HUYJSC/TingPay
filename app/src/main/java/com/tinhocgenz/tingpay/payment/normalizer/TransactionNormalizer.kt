package com.tinhocgenz.tingpay.payment.normalizer

import com.tinhocgenz.tingpay.core.security.HashUtils
import com.tinhocgenz.tingpay.domain.model.Transaction
import com.tinhocgenz.tingpay.payment.model.ParsedTransaction

class TransactionNormalizer {

    fun normalize(parsed: ParsedTransaction, notificationTime: Long): Transaction {
        val fingerprint = HashUtils.createTransactionFingerprint(
            bankCode = parsed.bankCode,
            accountNumber = parsed.accountNumber,
            amount = parsed.amount,
            transactionTime = parsed.transactionTime,
            description = parsed.description
        )

        return Transaction(
            bankCode = parsed.bankCode,
            accountNumber = parsed.accountNumber,
            amount = parsed.amount,
            type = parsed.type,
            sender = parsed.sender,
            description = parsed.description,
            transactionTime = parsed.transactionTime,
            notificationTime = notificationTime,
            rawMessage = parsed.rawMessage,
            transactionHash = fingerprint
        )
    }
}
