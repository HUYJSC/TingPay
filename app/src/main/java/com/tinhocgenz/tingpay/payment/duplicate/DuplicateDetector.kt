package com.tinhocgenz.tingpay.payment.duplicate

import com.tinhocgenz.tingpay.core.security.HashUtils
import com.tinhocgenz.tingpay.domain.repository.TransactionRepository
import com.tinhocgenz.tingpay.payment.model.ParsedTransaction
import java.util.concurrent.ConcurrentHashMap

class DuplicateDetector(
    private val transactionRepository: TransactionRepository,
    private val timeWindowMillis: Long = 10 * 60 * 1000 // 10 minutes cache window
) {
    // In-memory LRU cache for lightning-fast checking
    private val memoryCache = ConcurrentHashMap<String, Long>()

    /**
     * Checks if transaction is duplicate. Returns true if duplicate (should be IGNORED).
     */
    suspend fun isDuplicate(parsed: ParsedTransaction): Boolean {
        val now = System.currentTimeMillis()
        val fingerprint = HashUtils.createTransactionFingerprint(
            bankCode = parsed.bankCode,
            accountNumber = parsed.accountNumber,
            amount = parsed.amount,
            transactionTime = parsed.transactionTime,
            description = parsed.description
        )

        // 1. Check in-memory cache
        val cachedTime = memoryCache[fingerprint]
        if (cachedTime != null && (now - cachedTime) < timeWindowMillis) {
            return true
        }

        // 2. Check SQLite Database
        val existsInDb = transactionRepository.isFingerprintExists(fingerprint)
        if (existsInDb) {
            memoryCache[fingerprint] = now
            return true
        }

        // Clean stale memory entries
        cleanStaleCache(now)

        return false
    }

    fun markSeen(fingerprint: String) {
        memoryCache[fingerprint] = System.currentTimeMillis()
    }

    private fun cleanStaleCache(currentTime: Long) {
        if (memoryCache.size > 200) {
            val iterator = memoryCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (currentTime - entry.value > timeWindowMillis) {
                    iterator.remove()
                }
            }
        }
    }
}
