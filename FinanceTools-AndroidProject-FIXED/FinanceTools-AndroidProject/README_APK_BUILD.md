# Finance Tools – All-in-One Calculator (Android Studio Project)

Package: `com.educationwithhassan.financetools`  
Min SDK: Android 8.0 (API 26)  
Target SDK: Android 15 (API 35)  
Architecture: Kotlin · Jetpack Compose · Material 3 · MVVM · Room · DataStore · Navigation Compose · Google Mobile Ads SDK (`23.6.0`)

## Prerequisites
1. **Android Studio Ladybug (2024.2.1)** or newer
2. **JDK 17** (bundled with Android Studio)
3. **Android SDK Platform 35** & Build-Tools 35.0.0

## Google AdMob Configuration (Test vs Production)
- **Debug Builds (`assembleDebug`)**:
  - Automatically sets `BuildConfig.USE_TEST_ADS = true` and manifest placeholder `admobAppId = ca-app-pub-3940256099942544~3347511713`.
  - Requests Google's official test ad units across all 5 formats (`AdMobConfig.kt`) and configures `RequestConfiguration.setTestDeviceIds(...)`.
- **Release Builds (`assembleRelease` / `bundleRelease`)**:
  - Automatically sets `BuildConfig.USE_TEST_ADS = false` and manifest placeholder `admobAppId = ca-app-pub-3133971041724338~5131467706`.
  - Uses the Finance Tools production AdMob Ad Unit IDs centrally in `ads/AdMobConfig.kt`.

### Integrated Ad Formats
1. **App Open (`AppOpenAdManager.kt`)**: Preloads at startup, respects 4-hour freshness, 60s cooldown, 30s grace window after Interstitial/Rewarded ads, and never blocks app startup.
2. **Interstitial (`InterstitialAdManager.kt`)**: Preloads ahead of time, enforces a configurable `>= 120s` cooldown, triggers only on eligible forward tool navigation transitions, never interrupts typing/results or Back navigation.
3. **Adaptive Banner (`BannerAdView.kt`)**: Uses `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize`, docked cleanly in `Scaffold.bottomBar` on Home, Directory, and Calculator screens without covering inputs/results, and collapses gracefully on failure/offline.
4. **Native Advanced (`NativeAdCard.kt`)**: Placed between card groups on `HomeDashboardScreen` (~every 6–8 cards), labeled `Advertisement` with `AdChoicesView`, `MediaView`, icon, headline, advertiser, body, and CTA registration.
5. **Rewarded (`RewardedAdManager.kt`)**: Optional user-initiated dialogs on Calculator/History/Settings to unlock 24h PDF Financial Reports or +15 bonus saved slots; reward is granted strictly once via `onUserEarnedReward`.

### Logcat Diagnostic Tags
- `FinanceTools_AdMob`
- `FinanceTools_AppOpen`
- `FinanceTools_Interstitial`
- `FinanceTools_Banner`
- `FinanceTools_Native`
- `FinanceTools_Rewarded`

Open **Settings -> Open AdMob Diagnostics** inside the app to inspect SDK initialization, Test/Production environment, loaded status, and last error per format.

## Build Debug APK (Command Line)
```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## Build Signed Release APK / AAB for Google Play
1. Configure your signing keystore in Android Studio (`Build -> Generate Signed Bundle / APK`).
2. Run `./gradlew bundleRelease` or `./gradlew assembleRelease`.
