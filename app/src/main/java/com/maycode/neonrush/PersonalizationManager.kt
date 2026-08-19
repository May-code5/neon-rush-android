package com.maycode.neonrush

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Maneja fondos y música personalizados del usuario.
 */
class PersonalizationManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("neon_personalization", Context.MODE_PRIVATE)

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val KEY_BG_URI = "bg_uri"
        private const val KEY_MUSIC_URI = "music_uri"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_BG_PRESET = "bg_preset"

        // Fondos predefinidos (colores)
        val PRESET_BACKGROUNDS = listOf(
            "Oscuro Clásico" to 0xFF05050A.toInt(),
            "Azul Profundo" to 0xFF0A1628.toInt(),
            "Púrpura Neon" to 0xFF1A0A2E.toInt(),
            "Rojo Oscuro" to 0xFF1A0505.toInt(),
            "Verde Matrix" to 0xFF051A0A.toInt()
        )
    }

    fun isMusicEnabled(): Boolean = prefs.getBoolean(KEY_MUSIC_ENABLED, true)

    fun setMusicEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
        if (!enabled) stopMusic()
        else startMusic()
    }

    fun getSelectedPresetIndex(): Int = prefs.getInt(KEY_BG_PRESET, 0)

    fun setPresetBackground(index: Int) {
        prefs.edit()
            .putInt(KEY_BG_PRESET, index)
            .remove(KEY_BG_URI)
            .apply()
    }

    fun setCustomBackground(uri: Uri) {
        prefs.edit()
            .putString(KEY_BG_URI, uri.toString())
            .apply()
    }

    fun setCustomMusic(uri: Uri) {
        prefs.edit().putString(KEY_MUSIC_URI, uri.toString()).apply()
        stopMusic()
        startMusic()
    }

    fun getBackgroundDrawable(): Drawable {
        val customUri = prefs.getString(KEY_BG_URI, null)
        if (customUri != null) {
            try {
                val input = context.contentResolver.openInputStream(Uri.parse(customUri))
                val bitmap = BitmapFactory.decodeStream(input)
                input?.close()
                if (bitmap != null) {
                    return BitmapDrawable(context.resources, bitmap)
                }
            } catch (_: Exception) {
                // fallback
            }
        }
        val index = getSelectedPresetIndex().coerceIn(0, PRESET_BACKGROUNDS.lastIndex)
        return ColorDrawable(PRESET_BACKGROUNDS[index].second)
    }

    fun startMusic() {
        if (!isMusicEnabled()) return
        stopMusic()

        val musicUri = prefs.getString(KEY_MUSIC_URI, null)
        try {
            mediaPlayer = if (musicUri != null) {
                MediaPlayer.create(context, Uri.parse(musicUri))
            } else {
                // Sin música propia todavía (se puede añadir un raw después)
                null
            }
            mediaPlayer?.apply {
                isLooping = true
                setVolume(0.4f, 0.4f)
                start()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo reproducir la música", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        stopMusic()
    }
}
