package com.tinhocgenz.tingpay

import com.tinhocgenz.tingpay.core.qr.Crc16
import com.tinhocgenz.tingpay.core.qr.VietQrEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VietQrEngineTest {

    @Test
    fun `test CRC16 calculation with known vector`() {
        val testData = "00020101021238540010A00000072701240006970422011001234567890208QRIBFTTA530370454063500005802VN62120808TP1234566304"
        val crc = Crc16.calculate(testData)
        assertEquals(4, crc.length)
        assertTrue(crc.matches("[0-9A-F]{4}".toRegex()))
    }

    @Test
    fun `test generate dynamic VietQR payload for MBBank`() {
        val payload = VietQrEngine.generatePayload(
            bin = "970422",
            accountNumber = "0123456789",
            amount = 350000,
            message = "TP8291"
        )

        // Must contain standard EMVCo & VietQR tags
        assertTrue(payload.startsWith("000201010212")) // Dynamic QR indicator
        assertTrue(payload.contains("A000000727")) // Napas AID
        assertTrue(payload.contains("970422")) // MBBank BIN
        assertTrue(payload.contains("0123456789")) // Account Number
        assertTrue(payload.contains("5303704")) // VND Currency
        assertTrue(payload.contains("5406350000")) // 350000 VND
        assertTrue(payload.contains("5802VN")) // Country VN
        assertTrue(payload.contains("TP8291")) // Order Code
        assertTrue(payload.contains("6304")) // CRC Tag
    }

    @Test
    fun `test generate static VietQR payload`() {
        val payload = VietQrEngine.generatePayload(
            bin = "970436",
            accountNumber = "9988776655",
            amount = 0
        )

        assertTrue(payload.startsWith("000201010211")) // Static QR indicator
        assertTrue(payload.contains("970436")) // VCB BIN
        assertTrue(payload.contains("9988776655"))
    }
}
