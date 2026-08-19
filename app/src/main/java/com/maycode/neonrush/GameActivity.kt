package com.maycode.neonrush

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.maycode.neonrush.ads.AdManager
import com.maycode.neonrush.databinding.ActivityGameBinding
import com.maycode.neonrush.game.GameView

class GameActivity : AppCompatActivity(), GameView.GameListener {

    private lateinit var binding: ActivityGameBinding
    private lateinit var gameView: GameView
    private lateinit var prefs: SharedPreferences
    private lateinit var adManager: AdManager

    private var canRevive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("neon_rush_prefs", MODE_PRIVATE)
        adManager = AdManager(this)
        adManager.loadInterstitial()
        adManager.loadRewarded()

        gameView = GameView(this)
        gameView.listener = this

        val container = binding.root as FrameLayout
        container.addView(
            gameView,
            0,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        binding.btnRevive.setOnClickListener {
            if (canRevive) {
                adManager.showRewarded(
                    onRewarded = {
                        if (isFinishing) return@showRewarded
                        canRevive = false
                        binding.gameOverOverlay.visibility = View.GONE
                        gameView.revive()
                    },
                    onFailed = {
                        if (!isFinishing) finish()
                    }
                )
            }
        }

        binding.btnMenu.setOnClickListener {
            adManager.showInterstitial {
                if (!isFinishing) finish()
            }
        }

        gameView.startGame()
    }

    override fun onScoreChanged(score: Int, coins: Int) {
        if (isFinishing) return
        runOnUiThread {
            if (isFinishing) return@runOnUiThread
            binding.tvScore.text = getString(R.string.score, score)
            binding.tvGameCoins.text = "$coins 🪙"
        }
    }

    override fun onGameOver(finalScore: Int, coinsCollected: Int) {
        if (isFinishing) return

        val high = prefs.getInt("high_score", 0)
        if (finalScore > high) {
            prefs.edit().putInt("high_score", finalScore).apply()
        }
        val totalCoins = prefs.getInt("coins", 0) + coinsCollected
        prefs.edit().putInt("coins", totalCoins).apply()

        runOnUiThread {
            if (isFinishing) return@runOnUiThread
            binding.tvFinalScore.text = "Puntos: $finalScore  |  +$coinsCollected monedas"
            binding.gameOverOverlay.visibility = View.VISIBLE
            binding.btnRevive.visibility = if (canRevive) View.VISIBLE else View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        if (::gameView.isInitialized) gameView.pause()
    }

    override fun onResume() {
        super.onResume()
        if (::gameView.isInitialized) gameView.resume()
    }

    override fun onDestroy() {
        if (::gameView.isInitialized) {
            gameView.listener = null
            gameView.stop()
        }
        if (::adManager.isInitialized) adManager.destroy()
        super.onDestroy()
    }
}
