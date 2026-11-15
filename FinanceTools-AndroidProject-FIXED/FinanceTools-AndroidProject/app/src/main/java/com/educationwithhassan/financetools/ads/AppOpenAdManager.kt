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
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * Policy-safe Google AdMob App Open Ad Manager:
 * - Preloads on main thread with 4-hour freshness validation
 * - Never blocks app startup indefinitely
 * - Enforces cooldown (min 60s between App Open ads) and a 30s grace window after Interstitial/Rewarded ads
 * - Never displays on top of another active full-screen ad
 */
class AppOpenAdManager(
    context: Context,
    private val appOpenCooldownMs: Long = 60_000L,
    private val postFullScreenGraceMs: Long = 30_000L
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var isShowing = false
    private var loadTimeMs = 0L
    private var lastShownMs = 0L

    fun loadAd() {
        mainHandler.post {
            if (isLoading || isAdAvailable()) return@post
            isLoading = true
            AdDiagnostics.onRequestStarted(AdFormat.APP_OPEN)
            AppOpenAd.load(
                appContext,
                AdMobConfig.appOpenAdUnitId,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        loadTimeMs = System.currentTimeMillis()
                        isLoading = false
                        AdDiagnostics.onAdLoaded(AdFormat.APP_OPEN)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        appOpenAd = null
                        isLoading = false
                        AdDiagnostics.onAdFailedToLoad(AdFormat.APP_OPEN, error)
                    }
                }
            )
        }
    }

    private fun isAdAvailable(): Boolean {
        val fourHoursMs = 4 * 60 * 60 * 1000L
        return appOpenAd != null && (System.currentTimeMillis() - loadTimeMs) < fourHoursMs
    }

    fun showAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        mainHandler.post {
            if (activity.isFinishing || activity.isDestroyed) {
                onComplete()
                return@post
            }
            if (isShowing || AdDiagnostics.isAnyFullScreenAdActive()) {
                Log.d(AdFormat.APP_OPEN.logTag, "Skipping App Open Ad: another full-screen ad is active")
                onComplete()
                return@post
            }
            val now = System.currentTimeMillis()
            if (lastShownMs != 0L && now - lastShownMs < appOpenCooldownMs) {
                Log.d(AdFormat.APP_OPEN.logTag, "Skipping App Open Ad: cooldown active")
                onComplete()
                return@post
            }
            if (AdDiagnostics.msSinceLastFullScreenAdDismissed() < postFullScreenGraceMs) {
                Log.d(AdFormat.APP_OPEN.logTag, "Skipping App Open Ad: recent interstitial/rewarded grace period")
                onComplete()
                return@post
            }
            val ad = appOpenAd
            if (ad == null || !isAdAvailable()) {
                appOpenAd = null
                loadAd()
                onComplete()
                return@post
            }

            isShowing = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdDiagnostics.onAdDisplayed(AdFormat.APP_OPEN, isFullScreen = true)
                }

                override fun onAdImpression() {
                    AdDiagnostics.onAdImpression(AdFormat.APP_OPEN)
                }

                override fun onAdClicked() {
                    AdDiagnostics.onAdClicked(AdFormat.APP_OPEN)
                }

                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowing = false
                    lastShownMs = System.currentTimeMillis()
                    AdDiagnostics.onAdDismissed(AdFormat.APP_OPEN, isFullScreen = true)
                    loadAd()
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowing = false
                    AdDiagnostics.onAdFailedToShow(AdFormat.APP_OPEN, adError, isFullScreen = true)
                    loadAd()
                    onComplete()
                }
            }
            ad.show(activity)
        }
    }

    fun dispose() {
        mainHandler.post {
            appOpenAd?.fullScreenContentCallback = null
            appOpenAd = null
            isLoading = false
            isShowing = false
            AdDiagnostics.onAdDisposed(AdFormat.APP_OPEN)
        }
    }
}
