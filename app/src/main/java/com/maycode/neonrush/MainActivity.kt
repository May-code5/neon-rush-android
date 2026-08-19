package com.maycode.neonrush

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.maycode.neonrush.ads.AdManager
import com.maycode.neonrush.billing.BillingManager
import com.maycode.neonrush.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var adManager: AdManager
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("neon_rush_prefs", MODE_PRIVATE)

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

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onDestroy() {
        adManager.destroy()
        billingManager.endConnection()
        super.onDestroy()
    }
}
