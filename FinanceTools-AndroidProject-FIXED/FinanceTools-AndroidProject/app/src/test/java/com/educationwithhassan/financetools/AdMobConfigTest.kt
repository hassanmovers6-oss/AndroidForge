package com.educationwithhassan.financetools

import com.educationwithhassan.financetools.ads.AdDiagnostics
import com.educationwithhassan.financetools.ads.AdFormat
import com.educationwithhassan.financetools.ads.AdMobConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdMobConfigTest {

    @Test
    fun productionAndTestCredentialsAreProperlyConfigured() {
        assertEquals("ca-app-pub-3133971041724338~5131467706", AdMobConfig.PROD_APP_ID)
        assertEquals("ca-app-pub-3940256099942544~3347511713", AdMobConfig.TEST_APP_ID)
        assertTrue(AdMobConfig.appOpenAdUnitId.startsWith("ca-app-pub-"))
        assertTrue(AdMobConfig.interstitialAdUnitId.startsWith("ca-app-pub-"))
        assertTrue(AdMobConfig.bannerAdUnitId.startsWith("ca-app-pub-"))
        assertTrue(AdMobConfig.nativeAdUnitId.startsWith("ca-app-pub-"))
        assertTrue(AdMobConfig.rewardedAdUnitId.startsWith("ca-app-pub-"))
    }

    @Test
    fun diagnosticsTracksAllFiveFormatsWithoutFakeImpressions() {
        val snapshot = AdDiagnostics.snapshot.value
        assertEquals(5, snapshot.formats.size)
        AdFormat.entries.forEach { format ->
            assertTrue(snapshot.formats.containsKey(format))
        }
        assertFalse(snapshot.isFullScreenAdActive)
    }
}
