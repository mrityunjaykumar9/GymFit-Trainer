package com.freegymtrainer.app.ads

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.freegymtrainer.app.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * How many rows apart to insert a banner inside a long scrollable list (exercise library, food
 * database, workout history). A bottom-only banner on a 50-item list is only ever seen by
 * whoever scrolls all the way down - most users don't. Spacing one in roughly every screenful
 * instead means most scroll depths pass at least one, without stacking them close enough to read
 * as spammy ad density. Not tied to any specific ad unit - every in-feed spot reuses the same
 * [BuildConfig.ADMOB_BANNER_ID], same as every other banner placement in the app.
 */
const val AD_FEED_INTERVAL = 8

/**
 * Whether a list of [itemCount] rows should still get a trailing ad after its last row, on top
 * of the periodic in-feed ones every [AD_FEED_INTERVAL] rows. Needed so a short/filtered list
 * (fewer than [AD_FEED_INTERVAL] rows - no periodic ad ever fires) still gets one ad rather than
 * none - but skipped when a periodic ad already landed within the last couple of rows, so two
 * banners don't end up sitting almost back-to-back right at the end of the list. That kind of
 * tight ad density is exactly what Google Play/AdMob's ad-placement policies flag.
 */
fun shouldShowTrailingAd(itemCount: Int): Boolean {
    if (itemCount < AD_FEED_INTERVAL) return true
    return itemCount % AD_FEED_INTERVAL > 2
}

/**
 * Reusable AdMob banner, shown on every main content screen *except* Onboarding (Google's AdMob
 * policy explicitly bans ads on loading/setup/onboarding screens - not a style choice, a policy
 * red line that risks account-level enforcement) and the active workout-execution timer (ads
 * placed where a user is rapidly tapping "next set"/"done" risk accidental taps counting as
 * invalid clicks, which is its own AdMob policy risk - see WorkoutExecutionScreen for the
 * interstitial shown there instead, between sets rather than overlapping them).
 *
 * Uses an *adaptive* banner (full device width, height chosen by Google per device/screen size)
 * rather than a fixed 320x50 size - adaptive banners get noticeably better fill rate and eCPM
 * because more ad formats can compete for the slot in Google's auction. This is the single
 * biggest banner-revenue lever available without adding a new ad format.
 *
 * Waits on [ConsentManager.adsReady] before creating the AdView or requesting an ad - AdMob's EU
 * User Consent Policy requires the EEA/UK consent decision to be known before any ad request.
 * Renders an empty space of banner height until then, so layout doesn't jump once it appears.
 *
 * It's completely normal to sometimes see a real ad here and sometimes see nothing ("no fill") -
 * no ad network wins an auction for every single request, especially on an emulator or a very
 * new AdMob account/app not yet linked to a live Play Store listing. [AdListener] below logs
 * load success/failure under tag "GymFitAds" so a no-fill (expected, not a bug) can be told apart
 * from a real wiring error (wrong ad unit ID, missing consent, etc.) while testing.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val ready by ConsentManager.adsReady.collectAsState()
    if (!ready) {
        Spacer(modifier.fillMaxWidth().height(50.dp))
        return
    }

    val context = LocalContext.current
    val adWidthDp = LocalConfiguration.current.screenWidthDp

    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp))
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d("GymFitAds", "Banner no-fill/error (${BuildConfig.ADMOB_BANNER_ID}): ${error.code} ${error.message}")
                }
                override fun onAdLoaded() {
                    Log.d("GymFitAds", "Banner loaded (${BuildConfig.ADMOB_BANNER_ID})")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    // AdView holds native/platform resources (WebView-backed) - never destroying it previously
    // meant every screen visit leaked one. Now cleaned up the moment this composable leaves
    // composition (user navigates away), which matters a lot more now that a banner sits on
    // nearly every screen instead of just three.
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    AndroidView(modifier = modifier.fillMaxWidth(), factory = { adView })
}
