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
 * explicitly loaded ahead of time and shown at a moment the app chooses.
 *
 * Two timing guards keep this from feeling naggy on top of only ever being called from natural
 * break points (see call site), never mid-workout:
 * - [MIN_SESSION_ENGAGEMENT_MS]: nothing shows in the first couple minutes after the app is
 *   opened, so the ad never intercepts someone who just launched the app.
 * - [MIN_INTERVAL_BETWEEN_ADS_MS]: a cooldown after each shown ad, so someone finishing several
 *   short workouts back to back doesn't get one every single time.
 * Both are silent skips - [showIfReady] still calls [onDismissed] immediately, never blocking
 * navigation waiting on an ad that isn't going to show.
 */
object InterstitialAdManager {
    private const val MIN_SESSION_ENGAGEMENT_MS = 2 * 60 * 1000L
    private const val MIN_INTERVAL_BETWEEN_ADS_MS = 5 * 60 * 1000L

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var sessionStartMillis: Long? = null
    private var lastShownAtMillis: Long? = null

    /** Marks when this app process started - call once, e.g. from Application.onCreate. */
    fun markSessionStart() {
        if (sessionStartMillis == null) sessionStartMillis = System.currentTimeMillis()
    }

    fun preload(context: Context) {
        markSessionStart()
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

    private fun withinCooldown(): Boolean {
        val now = System.currentTimeMillis()
        val sessionStart = sessionStartMillis ?: now
        if (now - sessionStart < MIN_SESSION_ENGAGEMENT_MS) return true
        val lastShown = lastShownAtMillis ?: return false
        return now - lastShown < MIN_INTERVAL_BETWEEN_ADS_MS
    }

    /**
     * Shows the preloaded ad if one is ready and the timing guards above allow it.
     * [onDismissed] always runs exactly once either way - immediately if no ad is shown (never
     * blocks the user waiting on an ad), or once a shown ad is closed - and a fresh ad is
     * preloaded for next time.
     */
    fun showIfReady(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null || withinCooldown()) {
            onDismissed()
            preload(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastShownAtMillis = System.currentTimeMillis()
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
