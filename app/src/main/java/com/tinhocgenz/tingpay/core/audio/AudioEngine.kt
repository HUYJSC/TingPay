package com.tinhocgenz.tingpay.core.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class AudioNotificationMode {
    TING_ONLY,
    TING_AND_AMOUNT,
    BANK_AND_AMOUNT,
    FULL_SENTENCE
}

data class AudioRequest(
    val amount: Long,
    val bankName: String = "",
    val customText: String? = null,
    val mode: AudioNotificationMode = AudioNotificationMode.TING_AND_AMOUNT
)

class AudioEngine(private val context: Context) {

    private val ttsManager = TtsManager(context)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val audioQueue = Channel<AudioRequest>(capacity = Channel.UNLIMITED)
    private val playbackMutex = Mutex()

    init {
        startQueueConsumer()
    }

    private fun startQueueConsumer() {
        scope.launch {
            for (request in audioQueue) {
                playbackMutex.withLock {
                    processAudioRequest(request)
                }
            }
        }
    }

    private suspend fun processAudioRequest(request: AudioRequest) {
        // 1. Play Ding / Ting Sound
        playTingSound()
        delay(400) // Brief pause after Ding sound

        // 2. Play Speech according to mode
        val speechText = when (request.mode) {
            AudioNotificationMode.TING_ONLY -> null
            AudioNotificationMode.TING_AND_AMOUNT -> {
                val words = VietnameseNumberToWords.convert(request.amount)
                "Đã nhận $words"
            }
            AudioNotificationMode.BANK_AND_AMOUNT -> {
                val words = VietnameseNumberToWords.convert(request.amount)
                val bankPart = if (request.bankName.isNotBlank()) "${request.bankName}, " else ""
                "${bankPart}nhận $words"
            }
            AudioNotificationMode.FULL_SENTENCE -> {
                request.customText ?: "Đã thanh toán thành công ${VietnameseNumberToWords.convert(request.amount)}"
            }
        }

        if (speechText != null) {
            val completionChannel = Channel<Unit>(capacity = 1)
            ttsManager.speak(speechText, System.currentTimeMillis().toString()) {
                scope.launch { completionChannel.send(Unit) }
            }
            // Wait until speech is finished or max 8 seconds timeout
            kotlinx.coroutines.withTimeoutOrNull(8000) {
                completionChannel.receive()
            }
            delay(200)
        }
    }

    private fun playTingSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
            scope.launch {
                delay(300)
                toneGen.release()
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error playing ting sound", e)
        }
    }

    fun notifyPaymentReceived(
        amount: Long,
        bankName: String = "",
        mode: AudioNotificationMode = AudioNotificationMode.TING_AND_AMOUNT
    ) {
        scope.launch {
            audioQueue.send(AudioRequest(amount = amount, bankName = bankName, mode = mode))
        }
    }

    fun shutdown() {
        ttsManager.shutdown()
    }
}
