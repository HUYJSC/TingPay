package com.tinhocgenz.tingpay.core.audio

/**
 * Converts numbers into standard Vietnamese spoken words for Text-To-Speech (TTS)
 */
object VietnameseNumberToWords {

    private val DIGITS = arrayOf(
        "không", "một", "hai", "ba", "bốn",
        "năm", "sáu", "bảy", "tám", "chín"
    )

    private val UNITS = arrayOf("", "nghìn", "triệu", "tỷ")

    fun convert(amount: Long): String {
        if (amount == 0L) return "không đồng"
        if (amount < 0) return "âm " + convert(-amount)

        var num = amount
        val groups = mutableListOf<Int>()

        while (num > 0) {
            groups.add((num % 1000).toInt())
            num /= 1000
        }

        val words = mutableListOf<String>()

        for (i in groups.indices.reversed()) {
            val groupValue = groups[i]
            if (groupValue == 0) continue

            val isFirstGroup = (i == groups.size - 1)
            val groupText = readThreeDigits(groupValue, !isFirstGroup)
            val unit = UNITS[i % UNITS.size]

            val part = if (unit.isNotEmpty()) "$groupText $unit" else groupText
            words.add(part.trim())
        }

        return words.joinToString(" ").trim() + " đồng"
    }

    private fun readThreeDigits(number: Int, readZeroHundred: Boolean): String {
        val hundred = number / 100
        val ten = (number % 100) / 10
        val unit = number % 10

        val sb = StringBuilder()

        if (hundred > 0 || readZeroHundred) {
            sb.append(DIGITS[hundred]).append(" trăm ")
        }

        if (ten > 1) {
            sb.append(DIGITS[ten]).append(" mươi ")
            if (unit == 1) {
                sb.append("mốt")
            } else if (unit == 5) {
                sb.append("lăm")
            } else if (unit > 0) {
                sb.append(DIGITS[unit])
            }
        } else if (ten == 1) {
            sb.append("mười ")
            if (unit == 5) {
                sb.append("lăm")
            } else if (unit > 0) {
                sb.append(DIGITS[unit])
            }
        } else if (ten == 0 && unit > 0) {
            if (hundred > 0 || readZeroHundred) {
                sb.append("linh ")
            }
            sb.append(DIGITS[unit])
        }

        return sb.toString().trim()
    }
}
