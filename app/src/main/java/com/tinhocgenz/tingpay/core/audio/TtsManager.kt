package com.tinhocgenz.tingpay.core.audio

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Trình quản lý giọng đọc tiếng Việt duy nhất cho TingPay.
 * BẮT BUỘC SỬ DỤNG Locale("vi", "VN").
 * TUYỆT ĐỐI KHÔNG fallback sang Locale.US, English hoặc Locale.getDefault().
 */
class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        val VIETNAMESE_LOCALE = Locale("vi", "VN")
    }

    private var tts: TextToSpeech? = null

    var isInitialized: Boolean = false
        private set

    var isVietnameseSupported: Boolean = false
        private set

    private val _voiceMissingEvent = MutableStateFlow(false)
    val voiceMissingEvent: StateFlow<Boolean> = _voiceMissingEvent.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // BẮT BUỘC thiết lập Locale("vi", "VN")
            val langResult = tts?.setLanguage(VIETNAMESE_LOCALE)

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsManager", "Hệ thống thiếu gói giọng đọc tiếng Việt (vi-VN). KHÔNG fallback sang tiếng Anh!")
                isVietnameseSupported = false
                _voiceMissingEvent.value = true
                showVietnameseVoiceInstallGuidance()
            } else {
                isVietnameseSupported = true
                _voiceMissingEvent.value = false
                findAndSetBestVietnameseVoice()
            }

            tts?.setSpeechRate(0.95f) // Tốc độ đọc tiếng Việt tự nhiên
            tts?.setPitch(1.0f)
            isInitialized = true
        } else {
            Log.e("TtsManager", "Khởi tạo TTS Engine thất bại. Mã lỗi: $status")
            isInitialized = false
            isVietnameseSupported = false
        }
    }

    /**
     * Quét và chọn Voice tiếng Việt chuẩn nhất trong danh sách engine
     */
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
                Log.i("TtsManager", "Đã chọn giọng tiếng Việt: ${viVoice.name} (${viVoice.locale})")
            }
        } catch (e: Exception) {
            Log.w("TtsManager", "Không thể duyệt voice list trên engine này: ${e.message}")
        }
    }

    /**
     * Hướng dẫn người dùng mở cài đặt để tải gói giọng nói tiếng Việt
     */
    fun showVietnameseVoiceInstallGuidance() {
        try {
            Toast.makeText(
                context,
                "Thiết bị chưa có giọng đọc Tiếng Việt (vi-VN). Vui lòng cài đặt giọng tiếng Việt trong Cài đặt Trợ năng / TTS.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e("TtsManager", "Lỗi hiển thị Toast hướng dẫn: ${e.message}")
        }
    }

    /**
     * Mở màn hình cài đặt TTS của Android để người dùng tải gói tiếng Việt
     */
    fun openTtsSettingsIntent() {
        try {
            val installIntent = Intent().apply {
                action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            try {
                val settingsIntent = Intent("com.android.settings.TTS_SETTINGS").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
            } catch (ex: Exception) {
                Log.e("TtsManager", "Không thể mở trang cài đặt TTS: ${ex.message}")
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    /**
     * Đọc văn bản tiếng Việt.
     * TUYỆT ĐỐI KHÔNG ĐỌC nếu không có gói tiếng Việt vi-VN.
     */
    fun speak(
        text: String,
        utteranceId: String = "TingPay_${System.currentTimeMillis()}",
        onDone: (() -> Unit)? = null
    ) {
        // Nếu không có hỗ trợ tiếng Việt hoặc TTS chưa sẵn sàng -> Tuyệt đối không đọc tiếng Anh bồi
        if (!isInitialized || !isVietnameseSupported || tts == null) {
            Log.w("TtsManager", "Bỏ qua đọc văn bản vì không có gói tiếng Việt (vi-VN). Không phát tiếng Anh.")
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
            Log.e("TtsManager", "Lỗi dừng TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            isVietnameseSupported = false
        } catch (e: Exception) {
            Log.e("TtsManager", "Lỗi giải phóng TTS", e)
        }
    }
}
