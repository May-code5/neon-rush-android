package com.maycode.neonrush.ads

import android.app.Activity
import android.util.Log
import android.widget.FrameLayout
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

/**
 * Gestor de anuncios con Unity Ads.
 * Game ID: 800359215
 */
class AdManager(private val activity: Activity) {

    companion object {
        private const val GAME_ID = "800359215"
        private const val BANNER_PLACEMENT = "Banner_Android"
        private const val INTERSTITIAL_PLACEMENT = "Interstitial_Android"
        private const val REWARDED_PLACEMENT = "Rewarded_Android"

        private const val TEST_MODE = true   // Cambiar a false cuando publiques
        private const val TAG = "UnityAds"
    }

    private var bannerView: BannerView? = null
    private var interstitialReady = false
    private var rewardedReady = false

    init {
        UnityAds.initialize(activity, GAME_ID, TEST_MODE, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d(TAG, "Unity Ads initialized")
                loadInterstitial()
                loadRewarded()
            }

            override fun onInitializationFailed(
                error: UnityAds.UnityAdsInitializationError?,
                message: String?
            ) {
                Log.e(TAG, "Init failed: $message")
            }
        })
    }

    fun loadBanner(container: FrameLayout) {
        bannerView = BannerView(activity, BANNER_PLACEMENT, UnityBannerSize.BANNER).apply {
            listener = object : BannerView.IListener {
                override fun onBannerLoaded(banner: BannerView?) {
                    Log.d(TAG, "Banner loaded")
                }

                override fun onBannerClick(banner: BannerView?) {}
                override fun onBannerFailedToLoad(banner: BannerView?, error: BannerErrorInfo?) {
                    Log.e(TAG, "Banner failed: ${error?.errorMessage}")
                }
                override fun onBannerLeftApplication(banner: BannerView?) {}
                override fun onBannerShown(banner: BannerView?) {}
            }
            container.addView(this)
            load()
        }
    }

    fun loadInterstitial() {
        UnityAds.load(INTERSTITIAL_PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                interstitialReady = true
                Log.d(TAG, "Interstitial loaded")
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                interstitialReady = false
                Log.e(TAG, "Interstitial failed: $message")
            }
        })
    }

    fun showInterstitial(onDismissed: () -> Unit = {}) {
        if (!interstitialReady) {
            onDismissed()
            loadInterstitial()
            return
        }

        UnityAds.show(activity, INTERSTITIAL_PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                placementId: String?,
                error: UnityAds.UnityAdsShowError?,
                message: String?
            ) {
                interstitialReady = false
                onDismissed()
                loadInterstitial()
            }

            override fun onUnityAdsShowStart(placementId: String?) {}
            override fun onUnityAdsShowClick(placementId: String?) {}

            override fun onUnityAdsShowComplete(
                placementId: String?,
                state: UnityAds.UnityAdsShowCompletionState?
            ) {
                interstitialReady = false
                onDismissed()
                loadInterstitial()
            }
        })
    }

    fun loadRewarded() {
        UnityAds.load(REWARDED_PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                rewardedReady = true
                Log.d(TAG, "Rewarded loaded")
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                rewardedReady = false
                Log.e(TAG, "Rewarded failed: $message")
            }
        })
    }

    fun showRewarded(onRewarded: () -> Unit, onFailed: () -> Unit = {}) {
        if (!rewardedReady) {
            onFailed()
            loadRewarded()
            return
        }

        UnityAds.show(activity, REWARDED_PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                placementId: String?,
                error: UnityAds.UnityAdsShowError?,
                message: String?
            ) {
                rewardedReady = false
                onFailed()
                loadRewarded()
            }

            override fun onUnityAdsShowStart(placementId: String?) {}
            override fun onUnityAdsShowClick(placementId: String?) {}

            override fun onUnityAdsShowComplete(
                placementId: String?,
                state: UnityAds.UnityAdsShowCompletionState?
            ) {
                rewardedReady = false
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    onRewarded()
                } else {
                    onFailed()
                }
                loadRewarded()
            }
        })
    }

    fun destroy() {
        bannerView?.destroy()
        bannerView = null
    }
}
