package com.educationwithhassan.financetools.ads

import com.educationwithhassan.financetools.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Centralized Google AdMob configuration.
 * Separates official Google Test Ad Unit IDs (debug builds / USE_TEST_ADS = true)
 * from the Finance Tools Production AdMob credentials (release builds / USE_TEST_ADS = false).
 */
object AdMobConfig {
    // Official Google AdMob Test Credentials (used automatically when BuildConfig.USE_TEST_ADS == true)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Production AdMob Credentials (Finance Tools – All-in-One Calculator)
    const val PROD_APP_ID = "ca-app-pub-3133971041724338~5131467706"
    private const val PROD_APP_OPEN_ID = "ca-app-pub-3133971041724338/1974162184"
    private const val PROD_INTERSTITIAL_ID = "ca-app-pub-3133971041724338/2097130312"
    private const val PROD_BANNER_ID = "ca-app-pub-3133971041724338/8340259981"
    private const val PROD_NATIVE_ID = "ca-app-pub-3133971041724338/5682307056"
    private const val PROD_REWARDED_ID = "ca-app-pub-3133971041724338/1687221725"

    val isTestEnvironment: Boolean
        get() = BuildConfig.USE_TEST_ADS

    val environmentLabel: String
        get() = if (isTestEnvironment) "Test" else "Production"

    val activeAppId: String
        get() = if (isTestEnvironment) TEST_APP_ID else PROD_APP_ID

    val appOpenAdUnitId: String
        get() = if (isTestEnvironment) TEST_APP_OPEN_ID else PROD_APP_OPEN_ID

    val interstitialAdUnitId: String
        get() = if (isTestEnvironment) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID

    val bannerAdUnitId: String
        get() = if (isTestEnvironment) TEST_BANNER_ID else PROD_BANNER_ID

    val nativeAdUnitId: String
        get() = if (isTestEnvironment) TEST_NATIVE_ID else PROD_NATIVE_ID

    val rewardedAdUnitId: String
        get() = if (isTestEnvironment) TEST_REWARDED_ID else PROD_REWARDED_ID

    /**
     * Configures Google Mobile Ads RequestConfiguration.
     * In Test mode, registers emulator and optional developer test device IDs so production ads
     * are never accidentally requested during QA.
     */
    fun applyRequestConfiguration(extraTestDeviceIds: List<String> = emptyList()) {
        val builder = RequestConfiguration.Builder()
        if (isTestEnvironment) {
            val devices = (listOf(AdRequest.DEVICE_ID_EMULATOR) + extraTestDeviceIds).distinct()
            builder.setTestDeviceIds(devices)
        }
        MobileAds.setRequestConfiguration(builder.build())
    }
}
