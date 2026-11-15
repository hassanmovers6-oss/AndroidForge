package com.educationwithhassan.financetools.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Policy-safe Google AdMob Rewarded Ad Manager:
 * - Preloads ads on main thread
 * - Only grants rewards inside onUserEarnedReward AND enforces single-fire AtomicBoolean per display
 * - Never grants rewards on ad load, failure, or early dismissal
 * - Prevents duplicate concurrent displays and logs all SDK callbacks
 */
class RewardedAdManager(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var isShowing = false

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun preload() {
        mainHandler.post {
            if (isLoading || rewardedAd != null) return@post
            isLoading = true
            AdDiagnostics.onRequestStarted(AdFormat.REWARDED)
            RewardedAd.load(
                appContext,
                AdMobConfig.rewardedAdUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isLoading = false
                        _isReady.value = true
                        AdDiagnostics.onAdLoaded(AdFormat.REWARDED)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        isLoading = false
                        _isReady.value = false
                        AdDiagnostics.onAdFailedToLoad(AdFormat.REWARDED, error)
                    }
                }
            )
        }
    }

    fun show(
        activity: Activity,
        onRewardEarned: (type: String, amount: Int) -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onUnavailableOrFailed: (String) -> Unit
    ) {
        mainHandler.post {
            if (activity.isFinishing || activity.isDestroyed) {
                onUnavailableOrFailed("Activity is no longer active.")
                return@post
            }
            if (isShowing || AdDiagnostics.isAnyFullScreenAdActive()) {
                onUnavailableOrFailed("Another full-screen ad is currently displayed.")
                return@post
            }
            val ad = rewardedAd
            if (ad == null) {
                _isReady.value = false
                preload()
                onUnavailableOrFailed("Rewarded video is loading or unavailable offline. Please try again in a moment.")
                return@post
            }

            isShowing = true
            _isReady.value = false
            val rewardHandledOnce = AtomicBoolean(false)

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdDiagnostics.onAdDisplayed(AdFormat.REWARDED, isFullScreen = true)
                }

                override fun onAdImpression() {
                    AdDiagnostics.onAdImpression(AdFormat.REWARDED)
                }

                override fun onAdClicked() {
                    AdDiagnostics.onAdClicked(AdFormat.REWARDED)
                }

                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    isShowing = false
                    AdDiagnostics.onAdDismissed(AdFormat.REWARDED, isFullScreen = true)
                    preload()
                    if (!rewardHandledOnce.get()) {
                        onDismissedWithoutReward()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    isShowing = false
                    AdDiagnostics.onAdFailedToShow(AdFormat.REWARDED, error, isFullScreen = true)
                    preload()
                    onUnavailableOrFailed("Unable to display ad (${error.message}).")
                }
            }

            ad.show(activity) { rewardItem ->
                // Guarantee reward callback executes exactly once per watched ad
                if (rewardHandledOnce.compareAndSet(false, true)) {
                    AdDiagnostics.onRewardGranted(rewardItem.type, rewardItem.amount)
                    onRewardEarned(rewardItem.type, rewardItem.amount)
                }
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            rewardedAd?.fullScreenContentCallback = null
            rewardedAd = null
            isLoading = false
            isShowing = false
            _isReady.value = false
            AdDiagnostics.onAdDisposed(AdFormat.REWARDED)
        }
    }
}
