package com.tinhocgenz.tingpay.core.qr

/**
 * CRC16 CCITT-FALSE Implementation
 * Polynomial: 0x1021, Initial value: 0xFFFF, No reflection, Final XOR: 0x0000
 * Compliant with EMVCo / Napas VietQR standard.
 */
object Crc16 {
    private const val POLYNOMIAL = 0x1021
    private const val INITIAL_VALUE = 0xFFFF

    fun calculate(data: String): String {
        val bytes = data.toByteArray(Charsets.UTF_8)
        var crc = INITIAL_VALUE

        for (b in bytes) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            for (i in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor POLYNOMIAL) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }

        return String.format("%04X", crc)
    }
}
