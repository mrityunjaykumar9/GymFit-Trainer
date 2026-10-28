package com.freegymtrainer.app.ads

import android.app.Activity
import android.content.Context
import com.freegymtrainer.app.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps Google's User Messaging Platform (UMP) SDK. AdMob's EU User Consent Policy requires
 * asking users in the EEA/UK for ad-personalization consent (and showing the DMA message) before
 * any ad request goes out - skipping this risks AdMob account enforcement, not just a policy
 * warning. MainActivity runs [gatherConsent] once at startup, before MobileAds.initialize(); every
 * ad surface (AdBanner, InterstitialAdManager) waits on [adsReady] instead of loading immediately,
 * since ads must never be requested ahead of a consent decision.
 *
 * Outside the EEA/UK, requestConsentInfoUpdate() resolves immediately with no form shown - this
 * flow is a no-op delay of a few hundred ms for the vast majority of users.
 */
object ConsentManager {
    // Flip to true only when you specifically want to test the EEA consent dialog itself (it was
    // verified working this way already). Leave false for everyday local runs - with it true,
    // EVERY debug run requires resolving a consent dialog before AdBanner shows anything, which
    // is exactly why ads can appear to "stop working" locally otherwise. Real users are never
    // affected either way: this constant only ever applies inside `if (BuildConfig.DEBUG)` below.
    private const val FORCE_EEA_FOR_TESTING = false

    private val _adsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = _adsReady

    fun gatherConsent(activity: Activity, onResolved: () -> Unit) {
        val paramsBuilder = ConsentRequestParameters.Builder()
            // Play Console's Target audience for this app is 13+, never directed at children -
            // required flag for the EU consent request.
            .setTagForUnderAgeOfConsent(false)

        if (BuildConfig.DEBUG && FORCE_EEA_FOR_TESTING) {
            // Forces the EEA consent flow so it can be exercised on a test device outside the EEA
            // (e.g. India). Never compiled into release builds, so it can never affect a real
            // user's consent flow. Add your device's hashed ID (logcat prints it on first run,
            // tag "Ads", once you hit this code path) to see the real form.
            paramsBuilder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build()
            )
        }

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            paramsBuilder.build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // Invoked whether or not a form was actually shown - canRequestAds() reflects
                    // the outcome either way (already-granted consent, just-granted, or declined).
                    onResolved()
                    _adsReady.value = consentInformation.canRequestAds()
                }
            },
            {
                // Couldn't fetch consent status (e.g. no network at startup) - fail open rather
                // than permanently blocking ads for a device that's simply offline right now.
                onResolved()
                _adsReady.value = true
            }
        )
    }

    fun canRequestAds(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).canRequestAds()
}
