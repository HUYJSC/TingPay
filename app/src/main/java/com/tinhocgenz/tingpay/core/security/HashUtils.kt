package com.tinhocgenz.tingpay.core.security

import java.security.MessageDigest

object HashUtils {

    /**
     * Compute SHA-256 Hex string for transaction deduplication fingerprint
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate unique transaction fingerprint
     */
    fun createTransactionFingerprint(
        bankCode: String,
        accountNumber: String,
        amount: Long,
        transactionTime: Long,
        description: String
    ): String {
        val raw = "$bankCode|$accountNumber|$amount|$transactionTime|${description.trim()}"
        return sha256(raw)
    }
}
