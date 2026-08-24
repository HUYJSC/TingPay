package com.tinhocgenz.tingpay

import com.tinhocgenz.tingpay.domain.model.TransactionType
import com.tinhocgenz.tingpay.payment.model.BankNotification
import com.tinhocgenz.tingpay.payment.parser.BankParserRegistry
import com.tinhocgenz.tingpay.payment.parser.MBBankParser
import com.tinhocgenz.tingpay.payment.parser.VietcombankParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BankParserTest {

    private val registry = BankParserRegistry()

    @Test
    fun `test MBBank notification format`() {
        val noti = BankNotification(
            packageName = "com.mbmobile",
            title = "Biến động số dư",
            text = "TK: 0123456789 | GD: +350,000VND 24/08/2026 20:30 | SD: 10,000,000VND | ND: TP8291 Nguyen Van A chuyen tien",
            postTime = System.currentTimeMillis()
        )

        val parsed = registry.parse(noti)
        assertNotNull(parsed)
        assertEquals("MB", parsed!!.bankCode)
        assertEquals(350000L, parsed.amount)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals("0123456789", parsed.accountNumber)
    }

    @Test
    fun `test Vietcombank notification format`() {
        val noti = BankNotification(
            packageName = "com.VCB",
            title = "VCB Digibank",
            text = "So du TK 0123456789 +500.000 VND luc 24-08-2026 19:45:00. Ref TP9911",
            postTime = System.currentTimeMillis()
        )

        val parsed = registry.parse(noti)
        assertNotNull(parsed)
        assertEquals("VCB", parsed!!.bankCode)
        assertEquals(500000L, parsed.amount)
        assertEquals(TransactionType.CREDIT, parsed.type)
    }

    @Test
    fun `test Techcombank notification format`() {
        val noti = BankNotification(
            packageName = "vn.com.techcombank.bb.app",
            title = "Biến động số dư",
            text = "Giao dich +1,250,000 VND tai TK 19033333333333. ND: TP4455 Thanh toan hoa don",
            postTime = System.currentTimeMillis()
        )

        val parsed = registry.parse(noti)
        assertNotNull(parsed)
        assertEquals("TCB", parsed!!.bankCode)
        assertEquals(1250000L, parsed.amount)
        assertEquals(TransactionType.CREDIT, parsed.type)
    }

    @Test
    fun `test Debit notification format`() {
        val noti = BankNotification(
            packageName = "com.mbmobile",
            title = "Biến động số dư",
            text = "TK: 0123456789 | GD: -100,000VND | ND: Rut tien ATM",
            postTime = System.currentTimeMillis()
        )

        val parsed = registry.parse(noti)
        assertNotNull(parsed)
        assertEquals(100000L, parsed!!.amount)
        assertEquals(TransactionType.DEBIT, parsed.type)
    }

    @Test
    fun `test Generic Bank fallback format`() {
        val noti = BankNotification(
            packageName = "com.unknown.bank",
            title = "Thông báo nhận tiền",
            text = "Tài khoản của bạn vừa ghi có +45,000 VND. Nội dung: Cafe TP1122",
            postTime = System.currentTimeMillis()
        )

        val parsed = registry.parse(noti)
        assertNotNull(parsed)
        assertEquals(45000L, parsed!!.amount)
        assertEquals(TransactionType.CREDIT, parsed.type)
    }
}
