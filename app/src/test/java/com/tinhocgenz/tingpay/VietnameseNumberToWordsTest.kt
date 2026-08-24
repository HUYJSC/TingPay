package com.tinhocgenz.tingpay

import com.tinhocgenz.tingpay.core.audio.VietnameseNumberToWords
import org.junit.Assert.assertEquals
import org.junit.Test

class VietnameseNumberToWordsTest {

    @Test
    fun `test convert common amounts to spoken Vietnamese`() {
        assertEquals("ba mươi lăm nghìn đồng", VietnameseNumberToWords.convert(35000))
        assertEquals("ba trăm năm mươi nghìn đồng", VietnameseNumberToWords.convert(350000))
        assertEquals("năm trăm nghìn đồng", VietnameseNumberToWords.convert(500000))
        assertEquals("một triệu không trăm linh năm nghìn đồng", VietnameseNumberToWords.convert(1005000))
        assertEquals("hai triệu năm trăm nghìn đồng", VietnameseNumberToWords.convert(2500000))
        assertEquals("mười nghìn đồng", VietnameseNumberToWords.convert(10000))
        assertEquals("hai mươi nghìn đồng", VietnameseNumberToWords.convert(20000))
        assertEquals("hai mươi lăm nghìn đồng", VietnameseNumberToWords.convert(25000))
        assertEquals("hai mươi mốt nghìn đồng", VietnameseNumberToWords.convert(21000))
    }
}
