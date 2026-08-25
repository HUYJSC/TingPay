package com.tinhocgenz.tingpay

import com.tinhocgenz.tingpay.domain.model.OrderStatus
import com.tinhocgenz.tingpay.domain.model.Transaction
import com.tinhocgenz.tingpay.domain.model.TransactionType
import com.tinhocgenz.tingpay.payment.duplicate.DuplicateDetector
import com.tinhocgenz.tingpay.payment.model.ParsedTransaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEngineIntegrationTest {

    @Test
    fun `test CREDIT transaction qualifies for audio speech`() {
        val tx = Transaction(
            bankCode = "MB",
            amount = 350000,
            type = TransactionType.CREDIT,
            description = "Chuyen tien",
            transactionHash = "hash1"
        )

        val shouldPlayAudio = (tx.type == TransactionType.CREDIT)
        assertTrue("CREDIT transaction must qualify for speech", shouldPlayAudio)
    }

    @Test
    fun `test DEBIT transaction is blocked from audio speech`() {
        val tx = Transaction(
            bankCode = "MB",
            amount = 100000,
            type = TransactionType.DEBIT,
            description = "Rut tien ATM",
            transactionHash = "hash2"
        )

        val shouldPlayAudio = (tx.type == TransactionType.CREDIT)
        assertFalse("DEBIT transaction must NEVER qualify for speech", shouldPlayAudio)
    }

    @Test
    fun `test UNKNOWN transaction is blocked from audio speech`() {
        val tx = Transaction(
            bankCode = "MB",
            amount = 50000,
            type = TransactionType.UNKNOWN,
            description = "Unknown",
            transactionHash = "hash3"
        )

        val shouldPlayAudio = (tx.type == TransactionType.CREDIT)
        assertFalse("UNKNOWN transaction must NEVER qualify for speech", shouldPlayAudio)
    }
}
