package com.tinhocgenz.tingpay.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set

    var isVietnameseSupported: Boolean = false
        private set

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val viLocale = Locale("vi", "VN")
            val langResult = tts?.setLanguage(viLocale)

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "Vietnamese language pack missing in default engine")
                isVietnameseSupported = false
            } else {
                isVietnameseSupported = true
                findAndSetBestVietnameseVoice()
            }

            tts?.setSpeechRate(0.95f) // Tốc độ tự nhiên, rõ ràng
            tts?.setPitch(1.0f)
            isInitialized = true
        } else {
            Log.e("TtsManager", "TTS initialization failed: status $status")
            isInitialized = false
        }
    }

    private fun findAndSetBestVietnameseVoice() {
        try {
            val voices = tts?.voices ?: return
            val viVoice = voices.firstOrNull { voice ->
                voice.locale.language.equals("vi", ignoreCase = true) && !voice.isNetworkConnectionRequired
            } ?: voices.firstOrNull { it.locale.language.equals("vi", ignoreCase = true) }

            if (viVoice != null) {
                tts?.voice = viVoice
            }
        } catch (e: Exception) {
            Log.w("TtsManager", "Voice inspection not supported on this engine", e)
        }
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    /**
     * Đọc văn bản bằng tiếng Việt với Audio Focus và lắng nghe kết thúc
     */
    fun speak(
        text: String,
        utteranceId: String = System.currentTimeMillis().toString(),
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            onDone?.invoke()
            return
        }

        // Đăng ký listener nhận sự kiện đọc xong
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
            Log.e("TtsManager", "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e("TtsManager", "Error shutting down TTS", e)
        }
    }
}
