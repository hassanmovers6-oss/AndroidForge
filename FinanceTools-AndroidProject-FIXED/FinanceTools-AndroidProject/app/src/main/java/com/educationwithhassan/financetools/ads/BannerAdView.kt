package com.educationwithhassan.financetools.ads

import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Policy-safe Google Adaptive Anchored Banner Ad container.
 * - Uses AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize for responsive visibility
 * - Respects system navigation bar insets and collapses cleanly to 0.dp if loading fails or offline
 * - Properly handles resume/pause/destroy lifecycle to prevent memory leaks
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.bannerAdUnitId,
    applyNavigationBarPadding: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var adLoaded by remember(adUnitId) { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(if (applyNavigationBarPadding && adLoaded) Modifier.navigationBarsPadding() else Modifier)
    ) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adaptiveSize = remember(widthDp, context) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        val adHeightDp = remember(adaptiveSize, context) {
            adaptiveSize.getHeightInPixels(context) / context.resources.displayMetrics.density
        }

        val adView = remember(adUnitId, adaptiveSize) {
            AdView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setAdSize(adaptiveSize)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        adLoaded = true
                        AdDiagnostics.onAdLoaded(AdFormat.BANNER)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adLoaded = false
                        AdDiagnostics.onAdFailedToLoad(AdFormat.BANNER, error)
                    }

                    override fun onAdImpression() {
                        AdDiagnostics.onAdImpression(AdFormat.BANNER)
                    }

                    override fun onAdClicked() {
                        AdDiagnostics.onAdClicked(AdFormat.BANNER)
                    }

                    override fun onAdOpened() {
                        AdDiagnostics.onAdDisplayed(AdFormat.BANNER, isFullScreen = false)
                    }

                    override fun onAdClosed() {
                        AdDiagnostics.onAdDismissed(AdFormat.BANNER, isFullScreen = false)
                    }
                }
                AdDiagnostics.onRequestStarted(AdFormat.BANNER)
                loadAd(AdRequest.Builder().build())
            }
        }

        DisposableEffect(lifecycleOwner, adView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
                AdDiagnostics.onAdDisposed(AdFormat.BANNER)
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (adLoaded) 2.dp else 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (adLoaded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                AndroidView(
                    factory = { adView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (adLoaded) adHeightDp.dp else 0.dp)
                        .padding(vertical = if (adLoaded) 2.dp else 0.dp)
                )
            }
        }
    }
}
