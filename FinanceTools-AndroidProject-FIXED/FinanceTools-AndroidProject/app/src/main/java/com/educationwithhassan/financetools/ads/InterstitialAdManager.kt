package com.educationwithhassan.financetools.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlin.math.max

/**
 * Manages Google AdMob Interstitial Ads with strict UX & policy safeguards:
 * - Preloads in advance on main thread
 * - Enforces a configurable cooldown of at least 120 seconds between displays
 * - Skips first calculator open and never fires on every click
 * - Never interrupts typing, active calculations, or calculation results
 * - Never fires on back navigation
 * - Prevents duplicate concurrent displays and always resumes navigation on dismiss/error
 */
class InterstitialAdManager(
    context: Context,
    cooldownSeconds: Long = 120L
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cooldownMs: Long = max(120L, cooldownSeconds) * 1000L
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var isShowing = false
    private var lastShownMs = 0L
    private var navigationTransitionCount = 0
    private var lastOpenedToolId: String? = null

    fun setCooldownSeconds(seconds: Long) {
        cooldownMs = max(120L, seconds) * 1000L
    }

    fun preload() {
        mainHandler.post {
            if (isLoading || interstitialAd != null) return@post
            isLoading = true
            AdDiagnostics.onRequestStarted(AdFormat.INTERSTITIAL)
            InterstitialAd.load(
                appContext,
                AdMobConfig.interstitialAdUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoading = false
                        AdDiagnostics.onAdLoaded(AdFormat.INTERSTITIAL)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isLoading = false
                        AdDiagnostics.onAdFailedToLoad(AdFormat.INTERSTITIAL, error)
                    }
                }
            )
        }
    }

    /**
     * Checks eligibility when navigating forward to a different calculator tool.
     * Always invokes [onProceed] even if the ad is skipped, unavailable, or fails to show.
     */
    fun showIfEligibleOnToolNavigation(
        activity: Activity,
        targetToolId: String,
        isUserEnteringInputs: Boolean = false,
        onProceed: () -> Unit
    ) {
        mainHandler.post {
            val isSameTool = targetToolId == lastOpenedToolId
            lastOpenedToolId = targetToolId
            navigationTransitionCount++

            // Never interrupt active inputs/calculations, first tool open, or duplicate tool re-open
            if (isUserEnteringInputs || navigationTransitionCount < 2 || isSameTool) {
                onProceed()
                return@post
            }

            if (activity.isFinishing || activity.isDestroyed) {
                onProceed()
                return@post
            }

            if (isShowing || AdDiagnostics.isAnyFullScreenAdActive()) {
                Log.d(AdFormat.INTERSTITIAL.logTag, "Skipping interstitial: full-screen ad already active")
                onProceed()
                return@post
            }

            val now = System.currentTimeMillis()
            if (lastShownMs != 0L && now - lastShownMs < cooldownMs) {
                val remainingSec = (cooldownMs - (now - lastShownMs)) / 1000L
                Log.d(AdFormat.INTERSTITIAL.logTag, "Skipping interstitial: ${remainingSec}s cooldown remaining")
                onProceed()
                return@post
            }

            val ad = interstitialAd
            if (ad == null) {
                preload()
                onProceed()
                return@post
            }

            isShowing = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdDiagnostics.onAdDisplayed(AdFormat.INTERSTITIAL, isFullScreen = true)
                }

                override fun onAdImpression() {
                    AdDiagnostics.onAdImpression(AdFormat.INTERSTITIAL)
                }

                override fun onAdClicked() {
                    AdDiagnostics.onAdClicked(AdFormat.INTERSTITIAL)
                }

                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    isShowing = false
                    lastShownMs = System.currentTimeMillis()
                    AdDiagnostics.onAdDismissed(AdFormat.INTERSTITIAL, isFullScreen = true)
                    preload()
                    onProceed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    isShowing = false
                    AdDiagnostics.onAdFailedToShow(AdFormat.INTERSTITIAL, adError, isFullScreen = true)
                    preload()
                    onProceed()
                }
            }
            ad.show(activity)
        }
    }

    fun destroy() {
        mainHandler.post {
            interstitialAd?.fullScreenContentCallback = null
            interstitialAd = null
            isLoading = false
            isShowing = false
            AdDiagnostics.onAdDisposed(AdFormat.INTERSTITIAL)
        }
    }
}
