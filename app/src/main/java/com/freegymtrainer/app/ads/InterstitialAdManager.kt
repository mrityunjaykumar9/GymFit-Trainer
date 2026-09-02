package com.freegymtrainer.app.ads

import android.app.Activity
import android.content.Context
import com.freegymtrainer.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Preloads and shows a full-screen interstitial ad. Unlike the banner (a passive view that
 * just sits in the layout), AdMob does not insert interstitials automatically - they must be
 * explicitly loaded ahead of time and shown at a moment the app chooses. This is called only
 * from natural break points (see call site) - never mid-workout, matching the same "don't
 * interrupt an active session" rule the banner placement already follows.
 */
object InterstitialAdManager {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Shows the preloaded ad if one is ready. [onDismissed] always runs exactly once either
     * way - immediately if no ad is ready yet (never blocks the user waiting on an ad), or
     * once the shown ad is closed - and a fresh ad is preloaded for next time.
     */
    fun showIfReady(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            preload(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preload(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                preload(activity)
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
