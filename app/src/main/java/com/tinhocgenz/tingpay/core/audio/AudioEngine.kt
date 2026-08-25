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

/**
 * AudioEngine điều phối âm thanh thông báo nhận tiền TingPay.
 * Luôn phát âm thanh Ting trước, sau đó đọc số tiền bằng tiếng Việt (vi-VN).
 * Tuyệt đối không phát tiếng Anh khi thiếu gói tiếng Việt.
 */
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
        // 1. Luôn phát chuông Ting thanh thoát đầu tiên
        tingPlayer.playTing()
        delay(380)

        // Nếu chế độ là TING_ONLY hoặc thiết bị chưa có gói tiếng Việt (vi-VN)
        // -> Dừng ngay sau tiếng Ting, KHÔNG được đọc tiếng Anh hoặc ngôn ngữ khác
        if (request.config.mode == AudioNotificationMode.TING_ONLY || !ttsManager.isVietnameseSupported) {
            if (!ttsManager.isVietnameseSupported) {
                Log.w("AudioEngine", "Chỉ phát chuông Ting do thiết bị chưa cài đặt giọng nói tiếng Việt (vi-VN).")
            }
            delay(200)
            return
        }

        // 2. Chuyển đổi số tiền sang chuỗi tiếng Việt tự nhiên (100% tiếng Việt)
        val amountWords = VietnameseMoneyFormatter.formatToWords(request.amount)

        // 3. Xây dựng câu đọc tiếng Việt
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
                request.customSentence ?: "Bạn vừa nhận được $amountWords"
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

            // Chờ đọc xong hoặc timeout an toàn
            withTimeoutOrNull(8000) {
                completionSignal.receive()
            }

            // Khoảng nghỉ giữa 2 giao dịch liên tiếp để không chồng tiếng
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
     * Thử âm thanh mẫu trong Cài đặt
     */
    fun testVoice(sampleAmount: Long = 350000, bankName: String = "MBBank", config: AudioConfig = AudioConfig()) {
        enqueuePaymentAudio(sampleAmount, bankName, config)
    }

    fun shutdown() {
        tingPlayer.release()
        ttsManager.shutdown()
    }
}
