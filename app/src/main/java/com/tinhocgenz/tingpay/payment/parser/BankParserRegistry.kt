package com.tinhocgenz.tingpay.payment.parser

import com.tinhocgenz.tingpay.payment.model.BankNotification
import com.tinhocgenz.tingpay.payment.model.ParsedTransaction

class BankParserRegistry(
    private val parsers: List<BankParser> = listOf(
        MBBankParser(),
        VietcombankParser(),
        TechcombankParser(),
        ACBParser(),
        BIDVParser(),
        VietinBankParser()
    ),
    private val genericParser: GenericBankParser = GenericBankParser()
) {

    fun parse(notification: BankNotification): ParsedTransaction? {
        val packageName = notification.packageName
        val content = notification.fullContent

        // 1. Try specific registered parser matching package name
        val matchedParser = parsers.firstOrNull { it.supports(packageName, content) }
        if (matchedParser != null) {
            val result = matchedParser.parse(notification)
            if (result != null) return result
        }

        // 2. Try Generic Parser fallback
        if (genericParser.supports(packageName, content)) {
            return genericParser.parse(notification)
        }

        return null
    }
}
