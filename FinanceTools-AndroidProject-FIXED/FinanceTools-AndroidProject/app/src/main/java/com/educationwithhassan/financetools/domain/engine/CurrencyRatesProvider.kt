package com.educationwithhassan.financetools.domain.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

object CurrencyRatesProvider {
    val supportedCurrencies = listOf(
        "USD" to "$",
        "PKR" to "Rs ",
        "GBP" to "£",
        "EUR" to "€",
        "SAR" to "SAR ",
        "AED" to "AED ",
        "INR" to "₹",
        "CAD" to "C$",
        "AUD" to "A$",
        "JPY" to "¥",
        "CHF" to "CHF ",
        "CNY" to "¥",
        "TRY" to "₺",
        "MYR" to "RM ",
        "SGD" to "S$"
    )

    val baselineRatesUsd: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "PKR" to 278.5,
        "GBP" to 0.79,
        "EUR" to 0.92,
        "SAR" to 3.75,
        "AED" to 3.6725,
        "INR" to 84.4,
        "CAD" to 1.39,
        "AUD" to 1.53,
        "JPY" to 153.2,
        "CHF" to 0.88,
        "CNY" to 7.22,
        "TRY" to 34.4,
        "MYR" to 4.44,
        "SGD" to 1.34
    )

    fun symbolFor(code: String): String =
        supportedCurrencies.firstOrNull { it.first == code }?.second ?: "$code "

    suspend fun fetchLiveRates(): Result<Pair<Map<String, Double>, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://open.er-api.com/v6/latest/USD")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            require(json.optString("result") == "success") { "FX API returned non-success result" }
            val ratesJson = json.getJSONObject("rates")
            val updated = baselineRatesUsd.toMutableMap()
            for (code in baselineRatesUsd.keys) {
                if (ratesJson.has(code)) {
                    val v = ratesJson.getDouble(code)
                    if (v.isFinite() && v > 0.0) updated[code] = v
                }
            }
            val timestamp = json.optString("time_last_update_utc", Instant.now().toString())
            updated.toMap() to timestamp
        }
    }
}
