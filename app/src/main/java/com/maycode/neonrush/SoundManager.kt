package com.maycode.neonrush

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Sonidos simples sin necesidad de archivos externos.
 * Más adelante se pueden reemplazar por archivos .ogg en res/raw.
 */
class SoundManager(context: Context) {

    private var toneGen: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var enabled = true

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Exception) {
            toneGen = null
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun playJump() {
        if (!enabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (_: Exception) {}
    }

    fun playCoin() {
        if (!enabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 100)
        } catch (_: Exception) {}
    }

    fun playDeath() {
        if (!enabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            handler.postDelayed({
                try {
                    toneGen?.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 150)
                } catch (_: Exception) {}
            }, 180)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGen?.release()
        } catch (_: Exception) {}
        toneGen = null
    }
}
