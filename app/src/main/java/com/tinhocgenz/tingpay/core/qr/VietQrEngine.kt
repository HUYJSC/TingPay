package com.tinhocgenz.tingpay.core.qr

/**
 * VietQR / EMVCo Standard Generator
 * Generates dynamic/static QR payloads compatible with all Vietnamese Banking Apps.
 */
object VietQrEngine {

    private const val GUID_NAPAS = "A000000727"
    private const val SERVICE_FAST_TRANSFER = "QRIBFTTA" // Fast transfer to Account
    private const val CURRENCY_VND = "704"
    private const val COUNTRY_VN = "VN"

    /**
     * Formats a single EMVCo Tag-Length-Value (TLV) chunk
     */
    private fun tlv(tag: String, value: String): String {
        val len = String.format("%02d", value.length)
        return "$tag$len$value"
    }

    /**
     * Builds the complete VietQR Payload string
     * @param bin Bank BIN (6 digits, e.g. MB: 970422, VCB: 970436)
     * @param accountNumber Bank Account Number
     * @param amount Transaction amount in VND (0 for static QR)
     * @param message Order Code or payment description
     */
    fun generatePayload(
        bin: String,
        accountNumber: String,
        amount: Long = 0,
        message: String = ""
    ): String {
        val sb = StringBuilder()

        // 00: Payload Format Indicator
        sb.append(tlv("00", "01"))

        // 01: Point of Initiation Method (11: Static, 12: Dynamic)
        val pointOfInitiation = if (amount > 0) "12" else "11"
        sb.append(tlv("01", pointOfInitiation))

        // 38: Merchant Account Information
        val beneficiaryBuilder = StringBuilder()
        beneficiaryBuilder.append(tlv("00", bin))
        beneficiaryBuilder.append(tlv("01", accountNumber))

        val merchantInfoBuilder = StringBuilder()
        merchantInfoBuilder.append(tlv("00", GUID_NAPAS))
        merchantInfoBuilder.append(tlv("01", beneficiaryBuilder.toString()))
        merchantInfoBuilder.append(tlv("02", SERVICE_FAST_TRANSFER))

        sb.append(tlv("38", merchantInfoBuilder.toString()))

        // 53: Transaction Currency (704 = VND)
        sb.append(tlv("53", CURRENCY_VND))

        // 54: Transaction Amount
        if (amount > 0) {
            sb.append(tlv("54", amount.toString()))
        }

        // 58: Country Code
        sb.append(tlv("58", COUNTRY_VN))

        // 62: Additional Data Field (Purpose / Order Reference)
        if (message.isNotBlank()) {
            val normalizedMessage = normalizeMessage(message)
            val subField08 = tlv("08", normalizedMessage)
            sb.append(tlv("62", subField08))
        }

        // 63: CRC (Checksum over all previous data + "6304")
        val dataBeforeCrc = sb.toString() + "6304"
        val crc = Crc16.calculate(dataBeforeCrc)

        return "$dataBeforeCrc$crc"
    }

    /**
     * Remove Vietnamese diacritics and special characters for standard QR payload compatibility
     */
    private fun normalizeMessage(input: String): String {
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("đ", "d")
            .replace("Đ", "D")
            .replace("[^a-zA-Z0-9 ]".toRegex(), "")
            .trim()
        return if (normalized.length > 25) normalized.substring(0, 25) else normalized
    }
}
