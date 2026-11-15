package com.educationwithhassan.financetools.ads

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Policy-compliant Google Native Advanced Ad card for the Home screen:
 * - Clearly labeled "Advertisement" with explicit AdChoicesView attribution
 * - Registers headline, advertiser, icon, MediaView, body, and CTA assets on NativeAdView
 * - Styled distinctly from calculator cards so users never confuse ads with calculator buttons
 * - Collapses cleanly when offline or failed, and destroys NativeAd on dispose
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.nativeAdUnitId
) {
    val context = LocalContext.current
    var nativeAd by remember(adUnitId) { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adUnitId) {
        var active = true
        AdDiagnostics.onRequestStarted(AdFormat.NATIVE)
        val nativeOptions = NativeAdOptions.Builder()
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .build()

        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                if (!active) {
                    ad.destroy()
                    return@forNativeAd
                }
                nativeAd?.destroy()
                nativeAd = ad
                AdDiagnostics.onAdLoaded(AdFormat.NATIVE)
                AdDiagnostics.onAdDisplayed(AdFormat.NATIVE, isFullScreen = false)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd = null
                    AdDiagnostics.onAdFailedToLoad(AdFormat.NATIVE, error)
                }

                override fun onAdImpression() {
                    AdDiagnostics.onAdImpression(AdFormat.NATIVE)
                }

                override fun onAdClicked() {
                    AdDiagnostics.onAdClicked(AdFormat.NATIVE)
                }
            })
            .withNativeAdOptions(nativeOptions)
            .build()

        loader.loadAd(AdRequest.Builder().build())

        onDispose {
            active = false
            nativeAd?.destroy()
            nativeAd = null
            AdDiagnostics.onAdDisposed(AdFormat.NATIVE)
        }
    }

    val loadedAd = nativeAd ?: return

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val emeraldColor = MaterialTheme.colorScheme.secondary.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            factory = { ctx: Context -> createNativeAdView(ctx) },
            update = { adView ->
                populateNativeAdView(
                    adView = adView,
                    ad = loadedAd,
                    primaryColor = primaryColor,
                    emeraldColor = emeraldColor,
                    onSurfaceColor = onSurfaceColor,
                    mutedColor = mutedColor
                )
            }
        )
    }
}

private fun dp(context: Context, value: Int): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        context.resources.displayMetrics
    ).toInt()

private fun createNativeAdView(ctx: Context): NativeAdView {
    val adView = NativeAdView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val root = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Top row: explicit "Advertisement" label + AdChoices attribution
    val topBar = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val adBadge = TextView(ctx).apply {
        tag = "ad_badge"
        text = "Advertisement"
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        setPadding(dp(ctx, 8), dp(ctx, 2), dp(ctx, 8), dp(ctx, 2))
        background = GradientDrawable().apply {
            cornerRadius = dp(ctx, 6).toFloat()
            setColor(Color.parseColor("#0B1F3A"))
        }
    }

    val spacer = View(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
    }

    val adChoices = AdChoicesView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    topBar.addView(adBadge)
    topBar.addView(spacer)
    topBar.addView(adChoices)

    // Header row: App Icon + Headline + Advertiser
    val headerRow = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(ctx, 10) }
    }

    val iconView = ImageView(ctx).apply {
        tag = "ad_icon"
        scaleType = ImageView.ScaleType.CENTER_CROP
        layoutParams = LinearLayout.LayoutParams(dp(ctx, 44), dp(ctx, 44)).apply {
            marginEnd = dp(ctx, 12)
        }
    }

    val titleCol = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }

    val headlineView = TextView(ctx).apply {
        tag = "ad_headline"
        textSize = 15f
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 2
    }

    val advertiserView = TextView(ctx).apply {
        tag = "ad_advertiser"
        textSize = 12f
        maxLines = 1
    }

    titleCol.addView(headlineView)
    titleCol.addView(advertiserView)
    headerRow.addView(iconView)
    headerRow.addView(titleCol)

    // MediaView for Native Advanced rich media
    val mediaView = MediaView(ctx).apply {
        tag = "ad_media"
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(ctx, 140)
        ).apply { topMargin = dp(ctx, 10) }
    }

    // Body copy
    val bodyView = TextView(ctx).apply {
        tag = "ad_body"
        textSize = 13f
        maxLines = 3
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(ctx, 8) }
    }

    // Explicit Call To Action button
    val ctaButton = Button(ctx).apply {
        tag = "ad_cta"
        textSize = 13f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(ctx, 10) }
    }

    root.addView(topBar)
    root.addView(headerRow)
    root.addView(mediaView)
    root.addView(bodyView)
    root.addView(ctaButton)
    adView.addView(root)

    adView.adChoicesView = adChoices
    adView.iconView = iconView
    adView.headlineView = headlineView
    adView.advertiserView = advertiserView
    adView.mediaView = mediaView
    adView.bodyView = bodyView
    adView.callToActionView = ctaButton

    return adView
}

private fun populateNativeAdView(
    adView: NativeAdView,
    ad: NativeAd,
    primaryColor: Int,
    emeraldColor: Int,
    onSurfaceColor: Int,
    mutedColor: Int
) {
    val headlineView = adView.headlineView as TextView
    val advertiserView = adView.advertiserView as TextView
    val iconView = adView.iconView as ImageView
    val mediaView = adView.mediaView as MediaView
    val bodyView = adView.bodyView as TextView
    val ctaButton = adView.callToActionView as Button

    headlineView.text = ad.headline ?: ""
    headlineView.setTextColor(onSurfaceColor)

    val advertiserText = ad.advertiser ?: ad.store ?: "Sponsored Partner"
    advertiserView.text = advertiserText
    advertiserView.setTextColor(mutedColor)

    val iconDrawable = ad.icon?.drawable
    if (iconDrawable != null) {
        iconView.setImageDrawable(iconDrawable)
        iconView.visibility = View.VISIBLE
    } else {
        iconView.visibility = View.GONE
    }

    if (ad.mediaContent != null) {
        mediaView.mediaContent = ad.mediaContent
        mediaView.visibility = View.VISIBLE
    } else {
        mediaView.visibility = View.GONE
    }

    if (!ad.body.isNullOrBlank()) {
        bodyView.text = ad.body
        bodyView.setTextColor(mutedColor)
        bodyView.visibility = View.VISIBLE
    } else {
        bodyView.visibility = View.GONE
    }

    if (!ad.callToAction.isNullOrBlank()) {
        ctaButton.text = ad.callToAction
        ctaButton.background = GradientDrawable().apply {
            cornerRadius = dp(adView.context, 10).toFloat()
            setColor(emeraldColor)
        }
        ctaButton.visibility = View.VISIBLE
    } else {
        ctaButton.visibility = View.GONE
    }

    adView.findViewWithTag<TextView>("ad_badge")?.background = GradientDrawable().apply {
        cornerRadius = dp(adView.context, 6).toFloat()
        setColor(primaryColor)
    }

    adView.setNativeAd(ad)
}
