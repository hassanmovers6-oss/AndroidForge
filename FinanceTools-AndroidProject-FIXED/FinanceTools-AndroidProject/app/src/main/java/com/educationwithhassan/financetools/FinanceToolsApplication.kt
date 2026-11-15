package com.educationwithhassan.financetools

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.educationwithhassan.financetools.ads.AdDiagnostics
import com.educationwithhassan.financetools.ads.AdMobConfig
import com.educationwithhassan.financetools.ads.AppOpenAdManager
import com.educationwithhassan.financetools.data.local.FinanceDatabase
import com.educationwithhassan.financetools.data.local.UserPreferencesRepository
import com.google.android.gms.ads.MobileAds
import java.lang.ref.WeakReference

class FinanceToolsApplication : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    val database: FinanceDatabase by lazy { FinanceDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    private var currentActivityRef: WeakReference<Activity>? = null

    override fun onCreate() {
        super<Application>.onCreate()

        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        appOpenAdManager = AppOpenAdManager(this)

        // Apply Test/Production RequestConfiguration before MobileAds initialization
        AdMobConfig.applyRequestConfiguration()

        MobileAds.initialize(this) { initStatus ->
            val adapterStates = initStatus.adapterStatusMap.entries.joinToString("; ") { (adapter, status) ->
                "${adapter.substringAfterLast('.')}=${status.initializationState.name}"
            }
            AdDiagnostics.onSdkInitialized(adapterStates.ifEmpty { "Ready" })
            appOpenAdManager.loadAd()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        // Show App Open Ad when returning to foreground (never blocks startup, skips if full-screen ad active)
        val activity = currentActivityRef?.get() ?: return
        appOpenAdManager.showAdIfAvailable(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        if (!AdDiagnostics.isAnyFullScreenAdActive()) {
            currentActivityRef = WeakReference(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!AdDiagnostics.isAnyFullScreenAdActive()) {
            currentActivityRef = WeakReference(activity)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef?.clear()
        }
    }
}
