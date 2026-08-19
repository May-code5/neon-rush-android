package com.maycode.neonrush

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.maycode.neonrush.ads.AdManager
import com.maycode.neonrush.billing.BillingManager
import com.maycode.neonrush.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var adManager: AdManager
    private lateinit var billingManager: BillingManager
    private lateinit var personalization: PersonalizationManager

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            personalization.setCustomBackground(it)
            applyBackground()
            Toast.makeText(this, "Fondo personalizado aplicado", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickMusicLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            personalization.setCustomMusic(it)
            Toast.makeText(this, "Música personalizada aplicada", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (!granted) {
            Toast.makeText(this, "Se necesita permiso para acceder a tus archivos", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("neon_rush_prefs", MODE_PRIVATE)
        personalization = PersonalizationManager(this)

        adManager = AdManager(this)
        adManager.loadBanner(binding.adBannerContainer)
        adManager.loadInterstitial()
        adManager.loadRewarded()

        billingManager = BillingManager(this) { productId, success ->
            if (success) {
                when (productId) {
                    "coins_1000" -> addCoins(1000)
                    "coins_5000" -> addCoins(5000)
                    "gems_100" -> addGems(100)
                    "remove_ads" -> {
                        prefs.edit().putBoolean("ads_removed", true).apply()
                        Toast.makeText(this, "¡Anuncios eliminados!", Toast.LENGTH_SHORT).show()
                        binding.adBannerContainer.removeAllViews()
                    }
                    "premium_monthly" -> {
                        prefs.edit().putBoolean("premium", true).apply()
                        Toast.makeText(this, "¡Premium activado!", Toast.LENGTH_SHORT).show()
                    }
                }
                updateUI()
            }
        }
        billingManager.startConnection()

        applyBackground()
        updateUI()

        binding.btnPlay.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        binding.btnShop.setOnClickListener {
            showShopDialog()
        }

        binding.btnSkins.setOnClickListener {
            showSkinsDialog()
        }

        // Long press en el título o en el personaje abre personalización
        binding.tvTitle.setOnLongClickListener {
            showPersonalizationDialog()
            true
        }
        binding.characterPreview.setOnClickListener {
            showPersonalizationDialog()
        }
    }

    private fun applyBackground() {
        binding.root.background = personalization.getBackgroundDrawable()
    }

    private fun updateUI() {
        val highScore = prefs.getInt("high_score", 0)
        val coins = prefs.getInt("coins", 0)
        binding.tvHighScore.text = "Récord: $highScore"
        binding.tvCoins.text = "$coins 🪙"

        if (prefs.getBoolean("ads_removed", false) || prefs.getBoolean("premium", false)) {
            binding.adBannerContainer.removeAllViews()
        }
    }

    private fun addCoins(amount: Int) {
        val current = prefs.getInt("coins", 0)
        prefs.edit().putInt("coins", current + amount).apply()
        Toast.makeText(this, "+$amount monedas", Toast.LENGTH_SHORT).show()
    }

    private fun addGems(amount: Int) {
        val current = prefs.getInt("gems", 0)
        prefs.edit().putInt("gems", current + amount).apply()
        Toast.makeText(this, "+$amount gemas", Toast.LENGTH_SHORT).show()
    }

    private fun showShopDialog() {
        val options = arrayOf(
            "1.000 Monedas",
            "5.000 Monedas (mejor oferta)",
            "100 Gemas",
            "Eliminar Anuncios (para siempre)",
            "Premium Mensual (sin ads + bonus diario)"
        )
        AlertDialog.Builder(this)
            .setTitle("🛒 Tienda")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> billingManager.launchPurchase("coins_1000")
                    1 -> billingManager.launchPurchase("coins_5000")
                    2 -> billingManager.launchPurchase("gems_100")
                    3 -> billingManager.launchPurchase("remove_ads")
                    4 -> billingManager.launchPurchase("premium_monthly")
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showSkinsDialog() {
        val options = arrayOf(
            "Cyan Clásico (gratis)",
            "Magenta Neon - 500 monedas",
            "Verde Lima - 800 monedas",
            "Dorado Legendario - 2000 monedas",
            "Arcoíris (Premium)"
        )
        AlertDialog.Builder(this)
            .setTitle("🎨 Skins")
            .setItems(options) { _, which ->
                Toast.makeText(this, "Skin seleccionada (próximamente se aplicará en juego)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showPersonalizationDialog() {
        val options = arrayOf(
            "Cambiar fondo (predefinidos)",
            "Elegir mi propia imagen de fondo",
            "Elegir mi propia música",
            if (personalization.isMusicEnabled()) "Desactivar música" else "Activar música"
        )
        AlertDialog.Builder(this)
            .setTitle("🎨 Personalizar Lobby")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPresetBackgrounds()
                    1 -> pickCustomBackground()
                    2 -> pickCustomMusic()
                    3 -> {
                        val newState = !personalization.isMusicEnabled()
                        personalization.setMusicEnabled(newState)
                        Toast.makeText(this, if (newState) "Música activada" else "Música desactivada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showPresetBackgrounds() {
        val names = PersonalizationManager.PRESET_BACKGROUNDS.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Elegir fondo")
            .setItems(names) { _, which ->
                personalization.setPresetBackground(which)
                applyBackground()
                Toast.makeText(this, "Fondo cambiado", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun pickCustomBackground() {
        if (checkAndRequestPermissions()) {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun pickCustomMusic() {
        if (checkAndRequestPermissions()) {
            pickMusicLauncher.launch("audio/*")
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        return if (permissions.isEmpty()) {
            true
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
            false
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        personalization.startMusic()
    }

    override fun onPause() {
        super.onPause()
        personalization.stopMusic()
    }

    override fun onDestroy() {
        personalization.release()
        adManager.destroy()
        billingManager.endConnection()
        super.onDestroy()
    }
}
