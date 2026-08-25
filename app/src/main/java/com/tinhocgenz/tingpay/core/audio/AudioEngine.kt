package com.tinhocgenz.tingpay.core.audio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class AudioEngine(private val context: Context) {

    private val tingPlayer = TingPlayer(context)
    val ttsManager = TtsManager(context)

    private val scope = CoroutineScope(Dispatchers.Default)
    private val audioQueue = Channel<AudioPlaybackRequest>(capacity = Channel.UNLIMITED)
    private val playbackMutex = Mutex()

    init {
        startQueueProcessor()
    }

    private fun startQueueProcessor() {
        scope.launch {
            for (request in audioQueue) {
                playbackMutex.withLock {
                    processPlayback(request)
                }
            }
        }
    }

    private suspend fun processPlayback(request: AudioPlaybackRequest) {
        // 1. Phát chuông Ting trước
        tingPlayer.playTing()
        delay(380) // Khoảng nghỉ tự nhiên sau tiếng chuông

        // Nếu chế độ là TING_ONLY hoặc thiết bị chưa có tiếng Việt -> Dừng sau tiếng Ting
        if (request.config.mode == AudioNotificationMode.TING_ONLY || !ttsManager.isVietnameseSupported) {
            delay(200)
            return
        }

        // 2. Chuyển đổi số tiền sang chữ tiếng Việt tự nhiên
        val amountWords = VietnameseMoneyFormatter.formatToWords(request.amount)

        // 3. Xây dựng câu đọc tiếng Việt ngắn gọn, dễ nghe
        val textToSpeak = when (request.config.mode) {
            AudioNotificationMode.TING_ONLY -> null
            AudioNotificationMode.TING_AND_AMOUNT -> {
                "Đã nhận $amountWords"
            }
            AudioNotificationMode.BANK_AND_AMOUNT -> {
                val bankPrefix = if (request.bankName.isNotBlank()) "${request.bankName}, " else ""
                "${bankPrefix}nhận $amountWords"
            }
            AudioNotificationMode.FULL_SENTENCE -> {
                request.customSentence ?: "Đã thanh toán thành công số tiền $amountWords"
            }
        }

        if (textToSpeak != null) {
            val completionSignal = Channel<Unit>(capacity = 1)
            val utteranceId = "TingPay_${System.currentTimeMillis()}"

            ttsManager.setSpeechRate(request.config.speechRate)
            ttsManager.setPitch(request.config.pitch)

            ttsManager.speak(textToSpeak, utteranceId) {
                scope.launch { completionSignal.send(Unit) }
            }

            // Chờ đọc xong hoặc tối đa 8 giây (timeout an toàn)
            withTimeoutOrNull(8000) {
                completionSignal.receive()
            }

            // Khoảng nghỉ giữa 2 giao dịch liên tiếp (300-500ms)
            delay(350)
        }
    }

    /**
     * Gửi yêu cầu phát âm thanh nhận tiền vào hàng đợi (Thread-safe & Non-blocking)
     */
    fun enqueuePaymentAudio(
        amount: Long,
        bankName: String = "",
        config: AudioConfig = AudioConfig()
    ) {
        scope.launch {
            audioQueue.send(
                AudioPlaybackRequest(
                    amount = amount,
                    bankName = bankName,
                    config = config
                )
            )
        }
    }

    /**
     * Thử âm thanh mẫu trong Settings
     */
    fun testVoice(sampleAmount: Long = 350000, bankName: String = "MBBank", config: AudioConfig = AudioConfig()) {
        enqueuePaymentAudio(sampleAmount, bankName, config)
    }

    fun shutdown() {
        tingPlayer.release()
        ttsManager.shutdown()
    }
}
