package com.tinhocgenz.tingpay.core.audio

/**
 * Bộ chuyển đổi số tiền tệ sang chữ tiếng Việt chuẩn xác cho Loa thông báo TingPay
 * Xử lý chính xác các trường hợp: lẻ/linh, mốt, tư, lăm, không trăm, nghìn, triệu, tỷ, nghìn tỷ.
 */
object VietnameseMoneyFormatter {

    private val DIGITS = arrayOf(
        "không", "một", "hai", "ba", "bốn",
        "năm", "sáu", "bảy", "tám", "chín"
    )

    private val UNITS = arrayOf("", "nghìn", "triệu", "tỷ")

    /**
     * Chuyển đổi số tiền thành câu đọc tiếng Việt tự nhiên
     * Ví dụ: 350000 -> "ba trăm năm mươi nghìn đồng"
     *        105000 -> "một trăm lẻ năm nghìn đồng"
     *        24000  -> "hai mươi tư nghìn đồng"
     */
    fun formatToWords(amount: Long): String {
        if (amount == 0L) return "không đồng"
        if (amount < 0) return "âm " + formatToWords(-amount)

        var num = amount
        val groups = mutableListOf<Int>()

        while (num > 0) {
            groups.add((num % 1000).toInt())
            num /= 1000
        }

        val groupCount = groups.size
        val words = mutableListOf<String>()

        for (i in groups.indices.reversed()) {
            val groupValue = groups[i]
            if (groupValue == 0) continue

            val isHighestGroup = (i == groupCount - 1)
            val readZeroHundred = !isHighestGroup

            val groupText = readThreeDigitsGroup(groupValue, readZeroHundred)
            val unitIndex = i % 4
            val unit = UNITS[unitIndex]

            // Xử lý các cấp độ nghìn tỷ (nếu số tiền >= 1.000 tỷ)
            val billionsMultiplier = i / 4
            val extraBillions = if (billionsMultiplier > 0 && unitIndex == 0) {
                "tỷ ".repeat(billionsMultiplier).trim()
            } else ""

            val part = when {
                extraBillions.isNotEmpty() -> "$groupText $extraBillions"
                unit.isNotEmpty() -> "$groupText $unit"
                else -> groupText
            }

            words.add(part.trim())
        }

        return words.joinToString(" ").trim() + " đồng"
    }

    /**
     * Đọc nhóm 3 chữ số (ví dụ: 105 -> "một trăm lẻ năm", 025 -> "không trăm hai mươi lăm")
     */
    private fun readThreeDigitsGroup(number: Int, readZeroHundred: Boolean): String {
        val hundred = number / 100
        val ten = (number % 100) / 10
        val unit = number % 10

        val sb = StringBuilder()

        // 1. Hàng trăm
        if (hundred > 0 || readZeroHundred) {
            sb.append(DIGITS[hundred]).append(" trăm ")
        }

        // 2. Hàng chục
        if (ten > 1) {
            sb.append(DIGITS[ten]).append(" mươi ")
        } else if (ten == 1) {
            sb.append("mười ")
        } else if (ten == 0 && unit > 0 && (hundred > 0 || readZeroHundred)) {
            sb.append("lẻ ")
        }

        // 3. Hàng đơn vị
        when {
            unit == 1 && ten >= 2 -> sb.append("mốt")
            unit == 4 && ten >= 2 -> sb.append("tư")
            unit == 5 && ten >= 1 -> sb.append("lăm")
            unit > 0 -> sb.append(DIGITS[unit])
        }

        return sb.toString().trim()
    }
}
