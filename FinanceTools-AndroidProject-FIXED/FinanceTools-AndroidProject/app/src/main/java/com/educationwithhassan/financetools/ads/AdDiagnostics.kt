package com.educationwithhassan.financetools.ads

import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

enum class AdFormat(val displayName: String, val logTag: String) {
    APP_OPEN("App Open", "FinanceTools_AppOpen"),
    INTERSTITIAL("Interstitial", "FinanceTools_Interstitial"),
    BANNER("Adaptive Banner", "FinanceTools_Banner"),
    NATIVE("Native Advanced", "FinanceTools_Native"),
    REWARDED("Rewarded", "FinanceTools_Rewarded")
}

data class AdFormatDiagnostic(
    val format: AdFormat,
    val isLoaded: Boolean = false,
    val lastStatus: String = "Not requested yet",
    val lastErrorCode: Int? = null,
    val lastErrorMessage: String? = null,
    val lastUpdatedMs: Long = 0L
)

data class AdDiagnosticsSnapshot(
    val sdkInitialized: Boolean = false,
    val sdkStatusMessage: String = "Initializing...",
    val environment: String = AdMobConfig.environmentLabel,
    val isFullScreenAdActive: Boolean = false,
    val formats: Map<AdFormat, AdFormatDiagnostic> = AdFormat.entries.associateWith {
        AdFormatDiagnostic(format = it)
    }
)

/**
 * Centralized, privacy-safe AdMob diagnostics & full-screen overlap coordinator.
 * Never logs sensitive user data and never fabricates impression counts.
 */
object AdDiagnostics {
    const val TAG_ADMOB = "FinanceTools_AdMob"

    private val fullScreenActive = AtomicBoolean(false)
    private val lastFullScreenDismissedMs = AtomicLong(0L)

    private val _snapshot = MutableStateFlow(AdDiagnosticsSnapshot())
    val snapshot: StateFlow<AdDiagnosticsSnapshot> = _snapshot.asStateFlow()

    fun isAnyFullScreenAdActive(): Boolean = fullScreenActive.get()

    fun msSinceLastFullScreenAdDismissed(): Long {
        val last = lastFullScreenDismissedMs.get()
        return if (last == 0L) Long.MAX_VALUE else System.currentTimeMillis() - last
    }

    fun onSdkInitialized(summary: String) {
        Log.i(TAG_ADMOB, "SDK initialized [env=${AdMobConfig.environmentLabel}]: $summary")
        _snapshot.update {
            it.copy(
                sdkInitialized = true,
                sdkStatusMessage = summary,
                environment = AdMobConfig.environmentLabel
            )
        }
    }

    fun onRequestStarted(format: AdFormat) {
        Log.d(format.logTag, "[${format.displayName}] Ad request started (env=${AdMobConfig.environmentLabel})")
        updateFormat(format) { current ->
            current.copy(
                lastStatus = "Loading...",
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
    }

    fun onAdLoaded(format: AdFormat) {
        Log.i(format.logTag, "[${format.displayName}] Ad loaded successfully")
        updateFormat(format) { current ->
            current.copy(
                isLoaded = true,
                lastStatus = "Loaded",
                lastErrorCode = null,
                lastErrorMessage = null,
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
    }

    fun onAdFailedToLoad(format: AdFormat, error: LoadAdError) {
        Log.w(
            format.logTag,
            "[${format.displayName}] Failed to load: code=${error.code}, message=${error.message}, domain=${error.domain}"
        )
        updateFormat(format) { current ->
            current.copy(
                isLoaded = false,
                lastStatus = "Load failed",
                lastErrorCode = error.code,
                lastErrorMessage = error.message,
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
    }

    fun onAdDisplayed(format: AdFormat, isFullScreen: Boolean) {
        Log.i(format.logTag, "[${format.displayName}] Ad displayed")
        if (isFullScreen) {
            fullScreenActive.set(true)
        }
        updateFormat(format) { current ->
            current.copy(
                isLoaded = if (isFullScreen) false else current.isLoaded,
                lastStatus = "Displayed",
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
        _snapshot.update { it.copy(isFullScreenAdActive = fullScreenActive.get()) }
    }

    fun onAdFailedToShow(format: AdFormat, error: AdError, isFullScreen: Boolean) {
        Log.w(
            format.logTag,
            "[${format.displayName}] Failed to show: code=${error.code}, message=${error.message}"
        )
        if (isFullScreen) {
            fullScreenActive.set(false)
        }
        updateFormat(format) { current ->
            current.copy(
                isLoaded = false,
                lastStatus = "Show failed",
                lastErrorCode = error.code,
                lastErrorMessage = error.message,
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
        _snapshot.update { it.copy(isFullScreenAdActive = fullScreenActive.get()) }
    }

    fun onAdImpression(format: AdFormat) {
        Log.i(format.logTag, "[${format.displayName}] SDK impression callback received")
    }

    fun onAdClicked(format: AdFormat) {
        Log.i(format.logTag, "[${format.displayName}] SDK click callback received")
    }

    fun onAdDismissed(format: AdFormat, isFullScreen: Boolean) {
        Log.i(format.logTag, "[${format.displayName}] Ad dismissed")
        if (isFullScreen) {
            fullScreenActive.set(false)
            lastFullScreenDismissedMs.set(System.currentTimeMillis())
        }
        updateFormat(format) { current ->
            current.copy(
                isLoaded = if (isFullScreen) false else current.isLoaded,
                lastStatus = "Dismissed",
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
        _snapshot.update { it.copy(isFullScreenAdActive = fullScreenActive.get()) }
    }

    fun onRewardGranted(type: String, amount: Int) {
        Log.i(AdFormat.REWARDED.logTag, "[Rewarded] Reward granted: type=$type, amount=$amount")
        updateFormat(AdFormat.REWARDED) { current ->
            current.copy(
                lastStatus = "Reward granted ($amount $type)",
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
    }

    fun onAdDisposed(format: AdFormat) {
        Log.d(format.logTag, "[${format.displayName}] Ad resource disposed")
        updateFormat(format) { current ->
            current.copy(
                isLoaded = false,
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
    }

    private fun updateFormat(
        format: AdFormat,
        transform: (AdFormatDiagnostic) -> AdFormatDiagnostic
    ) {
        _snapshot.update { current ->
            val existing = current.formats[format] ?: AdFormatDiagnostic(format)
            current.copy(formats = current.formats + (format to transform(existing)))
        }
    }
}
