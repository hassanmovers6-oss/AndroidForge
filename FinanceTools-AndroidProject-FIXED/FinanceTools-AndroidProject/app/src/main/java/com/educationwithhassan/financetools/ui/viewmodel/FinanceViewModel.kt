package com.educationwithhassan.financetools.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.educationwithhassan.financetools.FinanceToolsApplication
import com.educationwithhassan.financetools.data.local.FavoriteToolEntity
import com.educationwithhassan.financetools.data.local.RecentToolEntity
import com.educationwithhassan.financetools.data.local.SavedCalculationEntity
import com.educationwithhassan.financetools.data.local.UserPreferences
import com.educationwithhassan.financetools.domain.engine.CurrencyRatesProvider
import com.educationwithhassan.financetools.domain.engine.FinancialEngine
import com.educationwithhassan.financetools.domain.model.CalculationOutput
import com.educationwithhassan.financetools.domain.model.DynamicRow
import com.educationwithhassan.financetools.domain.model.FinanceTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FinanceToolsApplication
    private val dao = app.database.savedCalculationDao()
    private val prefsRepo = app.preferencesRepository

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val history: StateFlow<List<SavedCalculationEntity>> = dao.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<List<String>> = dao.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentIds: StateFlow<List<String>> = dao.observeRecentIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _fxRates = MutableStateFlow(CurrencyRatesProvider.baselineRatesUsd)
    val fxRates: StateFlow<Map<String, Double>> = _fxRates.asStateFlow()

    private val _fxStatus = MutableStateFlow("Offline baseline rates active")
    val fxStatus: StateFlow<String> = _fxStatus.asStateFlow()

    fun syncLiveExchangeRates() {
        viewModelScope.launch {
            _fxStatus.value = "Fetching live rates..."
            val res = CurrencyRatesProvider.fetchLiveRates()
            res.onSuccess { (rates, timestamp) ->
                _fxRates.value = rates
                val msg = "Live FX updated ($timestamp)"
                _fxStatus.value = msg
                prefsRepo.setLastFxSync(msg)
            }.onFailure {
                _fxStatus.value = "Live rates unavailable (offline). Using local rates."
            }
        }
    }

    fun markToolOpened(toolId: String) {
        viewModelScope.launch {
            dao.upsertRecent(RecentToolEntity(toolId = toolId))
        }
    }

    fun toggleFavorite(toolId: String) {
        viewModelScope.launch {
            if (favoriteIds.value.contains(toolId)) dao.removeFavorite(toolId)
            else dao.addFavorite(FavoriteToolEntity(toolId = toolId))
        }
    }

    /**
     * Saves calculation to Room DB if within the user's slot capacity.
     * Returns true if queued for save, false if capacity reached (user can optionally unlock +15 bonus slots via Rewarded Ad).
     */
    fun saveCalculation(tool: FinanceTool, inputs: Map<String, String>, currency: String, output: CalculationOutput): Boolean {
        if (!output.valid) return false
        val limit = preferences.value.totalSavedSlotsLimit
        if (history.value.size >= limit) {
            return false
        }
        viewModelScope.launch {
            val json = JSONObject(inputs).toString()
            val summary = output.metrics.joinToString(" · ") { "${it.label}: ${it.formattedValue}" }
            dao.insertCalculation(
                SavedCalculationEntity(
                    toolId = tool.id,
                    toolName = tool.name,
                    categoryId = tool.category.id,
                    currencyCode = currency,
                    inputsJson = json,
                    primaryLabel = output.primaryLabel,
                    primaryResult = output.primaryFormatted,
                    summaryDetails = summary
                )
            )
        }
        return true
    }

    fun grantAdvancedPdfUnlock() {
        viewModelScope.launch { prefsRepo.grantAdvancedPdfUnlock(durationHours = 24) }
    }

    fun grantBonusSavedSlots() {
        viewModelScope.launch { prefsRepo.grantBonusSavedSlots(extraSlots = 15) }
    }

    fun deleteCalculation(id: Long) = viewModelScope.launch { dao.deleteCalculation(id) }
    fun clearHistory() = viewModelScope.launch { dao.clearAllHistory() }
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDarkMode(enabled) }
    fun setDefaultCurrency(code: String) = viewModelScope.launch { prefsRepo.setDefaultCurrency(code) }
    fun setLiveFxEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setLiveFxEnabled(enabled) }

    fun calculate(tool: FinanceTool, inputs: Map<String, String>, rows: List<DynamicRow>, currency: String): CalculationOutput {
        return FinancialEngine.calculate(tool.id, inputs, rows, currency, _fxRates.value)
    }
}
