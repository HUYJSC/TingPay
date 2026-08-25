package com.tinhocgenz.tingpay.core.audio

enum class AudioNotificationMode {
    TING_ONLY,
    TING_AND_AMOUNT,
    BANK_AND_AMOUNT,
    FULL_SENTENCE
}

data class AudioConfig(
    val mode: AudioNotificationMode = AudioNotificationMode.TING_AND_AMOUNT,
    val speechRate: Float = 0.95f,
    val pitch: Float = 1.0f,
    val includeBankName: Boolean = false,
    val customTemplate: String = "Đã nhận {amount} đồng."
)

data class AudioPlaybackRequest(
    val amount: Long,
    val bankName: String = "",
    val customSentence: String? = null,
    val config: AudioConfig = AudioConfig()
)
