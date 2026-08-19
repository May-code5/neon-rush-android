package com.maycode.neonrush.ads

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Gestor de anuncios AdMob.
 *
 * IMPORTANTE: Los IDs actuales son de PRUEBA de Google.
 * Antes de publicar reemplázalos por tus IDs reales de AdMob.
 */
class AdManager(private val activity: Activity) {

    companion object {
        // === IDs DE PRUEBA (reemplazar en producción) ===
        private const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

        private const val TAG = "AdManager"
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var bannerAdView: AdView? = null

    init {
        MobileAds.initialize(activity) {}
    }

    fun loadBanner(container: FrameLayout) {
        bannerAdView = AdView(activity).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_ID
            container.addView(this)
            loadAd(AdRequest.Builder().build())
        }
    }

    fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, INTERSTITIAL_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed: ${error.message}")
                }
            })
    }

    fun showInterstitial(onDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial() // precargar el siguiente
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            onDismissed()
            loadInterstitial()
        }
    }

    fun loadRewarded() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, REWARDED_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded failed: ${error.message}")
                }
            })
    }

    fun showRewarded(onRewarded: () -> Unit, onFailed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    onFailed()
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount}")
                onRewarded()
            }
        } else {
            onFailed()
            loadRewarded()
        }
    }

    fun destroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        interstitialAd = null
        rewardedAd = null
    }
}
