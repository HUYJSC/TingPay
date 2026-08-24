package com.tinhocgenz.tingpay.payment.parser

import com.tinhocgenz.tingpay.domain.model.TransactionType
import com.tinhocgenz.tingpay.payment.model.BankNotification
import com.tinhocgenz.tingpay.payment.model.ParsedTransaction
import java.util.regex.Pattern

interface BankParser {
    val bankCode: String
    val supportedPackages: Set<String>

    fun supports(packageName: String, text: String): Boolean {
        return supportedPackages.contains(packageName)
    }

    fun parse(notification: BankNotification): ParsedTransaction?
}

// -------------------------------------------------------------
// MBBank Parser (com.mbmobile)
// -------------------------------------------------------------
class MBBankParser : BankParser {
    override val bankCode: String = "MB"
    override val supportedPackages: Set<String> = setOf("com.mbmobile", "com.mb.mbbank")

    private val amountPattern = Pattern.compile("(?:GD:|Bien dong:?)\\s*([+-]?\\s*[\\d.,]+)\\s*(?:VND|VND|d|D)?", Pattern.CASE_INSENSITIVE)
    private val accountPattern = Pattern.compile("(?:TK|Tai khoan)\\s*([\\w\\d]+)", Pattern.CASE_INSENSITIVE)
    private val descPattern = Pattern.compile("(?:ND|Noi dung|ND:)\\s*(.*)", Pattern.CASE_INSENSITIVE)

    override fun parse(notification: BankNotification): ParsedTransaction? {
        val content = notification.fullContent

        // Extract Amount
        val (amount, type) = parseAmountAndType(content) ?: return null

        // Extract Account
        val accountMatcher = accountPattern.matcher(content)
        val account = if (accountMatcher.find()) accountMatcher.group(1) ?: "" else ""

        // Extract Description
        val descMatcher = descPattern.matcher(content)
        val desc = if (descMatcher.find()) descMatcher.group(1)?.trim() ?: "" else content

        return ParsedTransaction(
            bankCode = bankCode,
            accountNumber = account,
            amount = amount,
            type = type,
            description = desc,
            transactionTime = notification.postTime,
            rawMessage = content
        )
    }
}

// -------------------------------------------------------------
// Vietcombank Parser (com.VCB)
// -------------------------------------------------------------
class VietcombankParser : BankParser {
    override val bankCode: String = "VCB"
    override val supportedPackages: Set<String> = setOf("com.VCB", "com.vcb.digibank")

    override fun parse(notification: BankNotification): ParsedTransaction? {
        val content = notification.fullContent
        val (amount, type) = parseAmountAndType(content) ?: return null

        return ParsedTransaction(
            bankCode = bankCode,
            amount = amount,
            type = type,
            description = content,
            transactionTime = notification.postTime,
            rawMessage = content
        )
    }
}

// -------------------------------------------------------------
// Techcombank Parser (vn.com.techcombank.bb.app)
// -------------------------------------------------------------
class TechcombankParser : BankParser {
    override val bankCode: String = "TCB"
    override val supportedPackages: Set<String> = setOf(
        "vn.com.techcombank.bb.app",
        "techcombank.justpay"
    )

    override fun parse(notification: BankNotification): ParsedTransaction? {
        val content = notification.fullContent
        val (amount, type) = parseAmountAndType(content) ?: return null

        return ParsedTransaction(
            bankCode = bankCode,
            amount = amount,
            type = type,
            description = content,
            transactionTime = notification.postTime,
            rawMessage = content
        )
    }
}

// -------------------------------------------------------------
// ACB Parser (mobile.acb.com.vn)
// -------------------------------------------------------------
class ACBParser : BankParser {
    override val bankCode: String = "ACB"
    override val supportedPackages: Set<String> = setOf("mobile.acb.com.vn", "com.acb.one")

    override fun parse(notification: BankNotification): ParsedTransaction? {
        val content = notification.fullContent
        val (amount, type) = parseAmountAndType(content) ?: return null

        return ParsedTransaction(
            bankCode = bankCode,
            amount = amount,
            type = type,
            description = content,
            transactionTime = notification.postTime,
            rawMessage = content
        )
    }
}

// -------------------------------------------------------------
// BIDV Parser (com.vnpay.bidv)
// -------------------------------------------------------------
class BIDVParser : BankParser {
    override val bankCode: String = "BIDV"
    override val supportedPackages: Set<String> = setOf("com.vnpay.bidv")

    override fun parse(notification: BankNotification): ParsedTransaction? {
        val content = notification.fullContent
        val (amount, type) = parseAmountAndType(content) ?: return null

        return ParsedTransaction(
            bankCode = bankCode,
            amount = amount,
            type = type,
            description = content,
            transactionTime = notification.postTime,
            rawMessage = content
        )
    }
}

// -------------------------------------------------------------
// VietinBank Parser (com.vietinbank.ipay)
// -------------------------------------------------------------
class VietinBankParser : BankParser {
    override val bankCode: String = "CTG"
    override val supportedPackages: Set<String> = setOf("com.vietinbank.ipay")

    override fun parse(notification: BankNotification): ParsedTransaction? {
        val content = notification.fullContent
        val (amount, type) = parseAmountAndType(content) ?: return null

        return ParsedTransaction(
            bankCode = bankCode,
            amount = amount,
            type = type,
            description = content,
            transactionTime = notification.postTime,
            rawMessage = content
        )
    }
}

// -------------------------------------------------------------
// Generic Bank Parser (Fallback for any Banking App)
// -------------------------------------------------------------
class GenericBankParser : BankParser {
    override val bankCode: String = "GENERIC"
    override val supportedPackages: Set<String> = emptySet()

    override fun supports(packageName: String, text: String): Boolean {
        // Fallback parser accepts any notification that contains currency indicators or balance change signals
        val lower = text.lowercase()
        return lower.contains("vnd") || lower.contains("vnđ") || lower.contains("ghi có") ||
                lower.contains("nhận") || lower.contains("số dư") || lower.contains("biến động") ||
                lower.contains("+")
    }

    override fun parse(notification: BankNotification): ParsedTransaction? {
        val content = notification.fullContent
        val (amount, type) = parseAmountAndType(content) ?: return null

        return ParsedTransaction(
            bankCode = detectBankCodeFromPackage(notification.packageName),
            amount = amount,
            type = type,
            description = content,
            transactionTime = notification.postTime,
            rawMessage = content
        )
    }

    private fun detectBankCodeFromPackage(packageName: String): String {
        return when {
            packageName.contains("mb", ignoreCase = true) -> "MB"
            packageName.contains("vcb", ignoreCase = true) -> "VCB"
            packageName.contains("techcombank", ignoreCase = true) -> "TCB"
            packageName.contains("acb", ignoreCase = true) -> "ACB"
            packageName.contains("bidv", ignoreCase = true) -> "BIDV"
            packageName.contains("vietinbank", ignoreCase = true) -> "CTG"
            packageName.contains("tpbank", ignoreCase = true) -> "TPB"
            packageName.contains("vpbank", ignoreCase = true) -> "VPB"
            packageName.contains("momo", ignoreCase = true) -> "MOMO"
            packageName.contains("zalopay", ignoreCase = true) -> "ZALOPAY"
            else -> "BANK"
        }
    }
}

/**
 * Shared utility for extracting amount and CREDIT/DEBIT transaction type
 */
fun parseAmountAndType(content: String): Pair<Long, TransactionType>? {
    val clean = content.replace(",", "").replace(".", "")

    // Regex checking for +amount or credit indicators
    val creditPattern = Pattern.compile("(?:\\+|tang|ghi co|nhan|cong|\\bcredit\\b)\\s*([\\d]+)", Pattern.CASE_INSENSITIVE)
    val debitPattern = Pattern.compile("(?:\\-|giam|ghi no|tru|thanh toan|rut|\\bdebit\\b)\\s*([\\d]+)", Pattern.CASE_INSENSITIVE)

    // Check with standard dot/comma formats before clearing
    val formattedCreditPattern = Pattern.compile("(?:\\+|tang|ghi co|nhan|cong|\\bcredit\\b)\\s*([\\d.,]+)\\s*(?:VND|VNĐ|d|đ)?", Pattern.CASE_INSENSITIVE)
    val formattedDebitPattern = Pattern.compile("(?:\\-|giam|ghi no|tru|thanh toan|rut|\\bdebit\\b)\\s*([\\d.,]+)\\s*(?:VND|VNĐ|d|đ)?", Pattern.CASE_INSENSITIVE)

    val creditMatcher = formattedCreditPattern.matcher(content)
    if (creditMatcher.find()) {
        val numStr = creditMatcher.group(1)?.replace(".", "")?.replace(",", "")?.trim() ?: ""
        val amount = numStr.toLongOrNull()
        if (amount != null && amount > 0) {
            return Pair(amount, TransactionType.CREDIT)
        }
    }

    val debitMatcher = formattedDebitPattern.matcher(content)
    if (debitMatcher.find()) {
        val numStr = debitMatcher.group(1)?.replace(".", "")?.replace(",", "")?.trim() ?: ""
        val amount = numStr.toLongOrNull()
        if (amount != null && amount > 0) {
            return Pair(amount, TransactionType.DEBIT)
        }
    }

    // Fallback: search for numbers followed by VND/VNĐ/d
    val generalAmountPattern = Pattern.compile("([\\d.,]+)\\s*(?:VND|VNĐ|d|đ)", Pattern.CASE_INSENSITIVE)
    val genMatcher = generalAmountPattern.matcher(content)
    if (genMatcher.find()) {
        val numStr = genMatcher.group(1)?.replace(".", "")?.replace(",", "")?.trim() ?: ""
        val amount = numStr.toLongOrNull()
        if (amount != null && amount > 0) {
            val isDebit = content.contains("-") || content.lowercase().contains("ghi nợ") || content.lowercase().contains("trừ")
            val type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT
            return Pair(amount, type)
        }
    }

    return null
}
