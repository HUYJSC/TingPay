package com.tinhocgenz.tingpay.core.audio

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Trình phát giọng đọc tiếng Việt hoàn toàn tự động cho TingPay.
 * TỰ ĐỘNG NHẬN DIỆN VÀ PHÁT TIẾNG VIỆT 100% KHÔNG CẦN NGƯỜI DÙNG CÀI ĐẶT GÌ.
 */
class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        val VIETNAMESE_LOCALE = Locale("vi", "VN")
    }

    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set

    var isVietnameseSupported: Boolean = true
        private set

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Tự động gán Locale vi-VN
            val langResult = tts?.setLanguage(VIETNAMESE_LOCALE)

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Thử các biến thể locale tiếng Việt khác có trên máy (vi, vie)
                val fallbackVi = Locale("vi")
                val res = tts?.setLanguage(fallbackVi)
                isVietnameseSupported = (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED)
            } else {
                isVietnameseSupported = true
            }

            findAndSetBestVietnameseVoice()
            tts?.setSpeechRate(0.95f)
            tts?.setPitch(1.0f)
            isInitialized = true
        } else {
            isInitialized = false
        }
    }

    private fun findAndSetBestVietnameseVoice() {
        try {
            val voices = tts?.voices ?: return
            val viVoice = voices.firstOrNull { voice ->
                voice.locale.language.equals("vi", ignoreCase = true) && !voice.isNetworkConnectionRequired
            } ?: voices.firstOrNull {
                it.locale.language.equals("vi", ignoreCase = true)
            }

            if (viVoice != null) {
                tts?.voice = viVoice
            }
        } catch (e: Exception) {
            Log.w("TtsManager", "Không thể duyệt voice list: ${e.message}")
        }
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    /**
     * Tự động phát âm thanh tiếng Việt không làm phiền người dùng
     */
    fun speak(
        text: String,
        utteranceId: String = "TingPay_${System.currentTimeMillis()}",
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            onDone?.invoke()
            return
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) onDone?.invoke()
            }
            override fun onError(id: String?) {
                if (id == utteranceId) onDone?.invoke()
            }
        })

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TtsManager", "Lỗi dừng TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e("TtsManager", "Lỗi giải phóng TTS", e)
        }
    }
}
