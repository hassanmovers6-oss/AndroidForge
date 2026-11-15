package com.educationwithhassan.financetools.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finance_user_prefs")

data class UserPreferences(
    val darkMode: Boolean = false,
    val defaultCurrency: String = "USD",
    val liveFxEnabled: Boolean = true,
    val lastFxSyncText: String = "Offline baseline active",
    val advancedPdfUnlockedUntilMs: Long = 0L,
    val bonusSavedSlots: Int = 0
) {
    val isAdvancedPdfUnlocked: Boolean
        get() = System.currentTimeMillis() < advancedPdfUnlockedUntilMs

    val totalSavedSlotsLimit: Int
        get() = 25 + bonusSavedSlots
}

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val LIVE_FX_ENABLED = booleanPreferencesKey("live_fx_enabled")
        val LAST_FX_SYNC = stringPreferencesKey("last_fx_sync")
        val ADVANCED_PDF_UNLOCK_UNTIL = longPreferencesKey("advanced_pdf_unlock_until_ms")
        val BONUS_SAVED_SLOTS = intPreferencesKey("bonus_saved_slots")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "USD",
            liveFxEnabled = prefs[Keys.LIVE_FX_ENABLED] ?: true,
            lastFxSyncText = prefs[Keys.LAST_FX_SYNC] ?: "Offline baseline active",
            advancedPdfUnlockedUntilMs = prefs[Keys.ADVANCED_PDF_UNLOCK_UNTIL] ?: 0L,
            bonusSavedSlots = prefs[Keys.BONUS_SAVED_SLOTS] ?: 0
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setDefaultCurrency(code: String) {
        context.dataStore.edit { it[Keys.DEFAULT_CURRENCY] = code }
    }

    suspend fun setLiveFxEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LIVE_FX_ENABLED] = enabled }
    }

    suspend fun setLastFxSync(text: String) {
        context.dataStore.edit { it[Keys.LAST_FX_SYNC] = text }
    }

    suspend fun grantAdvancedPdfUnlock(durationHours: Int = 24) {
        val unlockUntil = System.currentTimeMillis() + durationHours * 3_600_000L
        context.dataStore.edit { it[Keys.ADVANCED_PDF_UNLOCK_UNTIL] = unlockUntil }
    }

    suspend fun grantBonusSavedSlots(extraSlots: Int = 15) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BONUS_SAVED_SLOTS] ?: 0
            prefs[Keys.BONUS_SAVED_SLOTS] = current + extraSlots
        }
    }
}
