package com.tinhocgenz.tingpay

import com.tinhocgenz.tingpay.core.audio.VietnameseMoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class VietnameseMoneyFormatterTest {

    @Test
    fun `test exact Vietnamese money words formatting for all specified test vectors`() {
        assertEquals("không đồng", VietnameseMoneyFormatter.formatToWords(0L))
        assertEquals("một đồng", VietnameseMoneyFormatter.formatToWords(1L))
        assertEquals("năm đồng", VietnameseMoneyFormatter.formatToWords(5L))
        assertEquals("mười đồng", VietnameseMoneyFormatter.formatToWords(10L))
        assertEquals("mười lăm đồng", VietnameseMoneyFormatter.formatToWords(15L))
        assertEquals("hai mươi mốt đồng", VietnameseMoneyFormatter.formatToWords(21L))
        assertEquals("hai mươi tư đồng", VietnameseMoneyFormatter.formatToWords(24L))
        assertEquals("hai mươi lăm đồng", VietnameseMoneyFormatter.formatToWords(25L))
        assertEquals("một trăm đồng", VietnameseMoneyFormatter.formatToWords(100L))
        assertEquals("một trăm lẻ một đồng", VietnameseMoneyFormatter.formatToWords(101L))
        assertEquals("một trăm lẻ năm đồng", VietnameseMoneyFormatter.formatToWords(105L))
        assertEquals("một trăm mười đồng", VietnameseMoneyFormatter.formatToWords(110L))
        assertEquals("một trăm mười lăm đồng", VietnameseMoneyFormatter.formatToWords(115L))
        assertEquals("một nghìn đồng", VietnameseMoneyFormatter.formatToWords(1000L))
        assertEquals("một nghìn không trăm lẻ năm đồng", VietnameseMoneyFormatter.formatToWords(1005L))
        assertEquals("một nghìn không trăm năm mươi đồng", VietnameseMoneyFormatter.formatToWords(1050L))
        assertEquals("mười lăm nghìn đồng", VietnameseMoneyFormatter.formatToWords(15000L))
        assertEquals("hai mươi mốt nghìn đồng", VietnameseMoneyFormatter.formatToWords(21000L))
        assertEquals("hai mươi lăm nghìn đồng", VietnameseMoneyFormatter.formatToWords(25000L))
        assertEquals("một trăm nghìn đồng", VietnameseMoneyFormatter.formatToWords(100000L))
        assertEquals("một trăm lẻ năm nghìn đồng", VietnameseMoneyFormatter.formatToWords(105000L))
        assertEquals("chín trăm chín mươi chín nghìn chín trăm chín mươi chín đồng", VietnameseMoneyFormatter.formatToWords(999999L))
        assertEquals("một triệu đồng", VietnameseMoneyFormatter.formatToWords(1000000L))
        assertEquals("một triệu hai trăm năm mươi nghìn đồng", VietnameseMoneyFormatter.formatToWords(1250000L))
        assertEquals("mười triệu đồng", VietnameseMoneyFormatter.formatToWords(10000000L))
        assertEquals("một trăm triệu đồng", VietnameseMoneyFormatter.formatToWords(100000000L))
        assertEquals("một tỷ đồng", VietnameseMoneyFormatter.formatToWords(1000000000L))
    }
}
