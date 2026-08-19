package com.maycode.neonrush

import android.content.Context
import android.content.SharedPreferences

data class Skin(
    val id: String,
    val name: String,
    val color: Int,
    val glowColor: Int,
    val price: Int,          // 0 = gratis
    val requiresPremium: Boolean = false
)

object SkinManager {

    val SKINS = listOf(
        Skin("cyan", "Cyan Clásico", 0xFF00F5FF.toInt(), 0x5500F5FF.toInt(), 0),
        Skin("magenta", "Magenta Neon", 0xFFFF00E5.toInt(), 0x55FF00E5.toInt(), 500),
        Skin("lime", "Verde Lima", 0xFF39FF14.toInt(), 0x5539FF14.toInt(), 800),
        Skin("gold", "Dorado Legendario", 0xFFFFD700.toInt(), 0x55FFD700.toInt(), 2000),
        Skin("rainbow", "Arcoíris", 0xFFFF6B35.toInt(), 0x55FF6B35.toInt(), 0, requiresPremium = true)
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("neon_skins", Context.MODE_PRIVATE)

    fun getSelectedSkinId(context: Context): String =
        prefs(context).getString("selected_skin", "cyan") ?: "cyan"

    fun setSelectedSkin(context: Context, skinId: String) {
        prefs(context).edit().putString("selected_skin", skinId).apply()
    }

    fun isSkinUnlocked(context: Context, skin: Skin): Boolean {
        if (skin.price == 0 && !skin.requiresPremium) return true
        if (skin.requiresPremium) {
            val mainPrefs = context.getSharedPreferences("neon_rush_prefs", Context.MODE_PRIVATE)
            return mainPrefs.getBoolean("premium", false)
        }
        return prefs(context).getBoolean("unlocked_${skin.id}", false)
    }

    fun unlockSkin(context: Context, skinId: String) {
        prefs(context).edit().putBoolean("unlocked_$skinId", true).apply()
    }

    fun getSelectedSkin(context: Context): Skin {
        val id = getSelectedSkinId(context)
        return SKINS.find { it.id == id } ?: SKINS[0]
    }
}
