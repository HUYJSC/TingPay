package com.tinhocgenz.tingpay.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class TingPlayer(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e("TingPlayer", "Failed to initialize ToneGenerator", e)
        }
    }

    /**
     * Phát tiếng chuông Ting thanh thoát (Nốt A6/High chime)
     */
    fun playTing() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
        } catch (e: Exception) {
            Log.e("TingPlayer", "Error playing ting chime", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e("TingPlayer", "Error releasing player", e)
        }
    }
}
