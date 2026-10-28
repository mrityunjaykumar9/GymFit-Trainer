package com.freegymtrainer.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
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
 *   opened, so the ad never intercepts someone who just launched the app. This is also the #1
 *   reason it looks "broken" while testing - finishing a quick workout within 2 minutes of
 *   opening the app is a silent, intentional skip, not a bug. See [GymFitAds] logs below.
 * - [MIN_INTERVAL_BETWEEN_ADS_MS]: a cooldown after each shown ad, so someone finishing several
 *   short workouts back to back doesn't get one every single time.
 * Both are silent skips - [showIfReady] still calls [onDismissed] immediately, never blocking
 * navigation waiting on an ad that isn't going to show.
 */
object InterstitialAdManager {
    private const val TAG = "GymFitAds"
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
                    Log.d(TAG, "Interstitial loaded (${BuildConfig.ADMOB_INTERSTITIAL_ID})")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    Log.d(TAG, "Interstitial no-fill/error (${BuildConfig.ADMOB_INTERSTITIAL_ID}): ${error.code} ${error.message}")
                }
            }
        )
    }

    /** Null = not within cooldown. Non-null names which guard is currently blocking a show. */
    private fun cooldownReason(): String? {
        val now = System.currentTimeMillis()
        val sessionStart = sessionStartMillis ?: now
        val sinceStart = now - sessionStart
        if (sinceStart < MIN_SESSION_ENGAGEMENT_MS) {
            return "session engagement guard (${sinceStart / 1000}s into session, needs ${MIN_SESSION_ENGAGEMENT_MS / 1000}s)"
        }
        val lastShown = lastShownAtMillis ?: return null
        val sinceLastShown = now - lastShown
        if (sinceLastShown < MIN_INTERVAL_BETWEEN_ADS_MS) {
            return "post-ad cooldown (${sinceLastShown / 1000}s since last ad, needs ${MIN_INTERVAL_BETWEEN_ADS_MS / 1000}s)"
        }
        return null
    }

    /**
     * Shows the preloaded ad if one is ready and the timing guards above allow it.
     * [onDismissed] always runs exactly once either way - immediately if no ad is shown (never
     * blocks the user waiting on an ad), or once a shown ad is closed - and a fresh ad is
     * preloaded for next time.
     */
    fun showIfReady(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        val cooldown = cooldownReason()
        if (ad == null || cooldown != null) {
            Log.d(TAG, "Interstitial not shown: " + (cooldown ?: "no ad preloaded yet (still loading, or last load had no fill)"))
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
                Log.d(TAG, "Interstitial failed to show: ${error.code} ${error.message}")
                interstitialAd = null
                preload(activity)
                onDismissed()
            }
        }
        Log.d(TAG, "Showing interstitial")
        ad.show(activity)
    }
}
