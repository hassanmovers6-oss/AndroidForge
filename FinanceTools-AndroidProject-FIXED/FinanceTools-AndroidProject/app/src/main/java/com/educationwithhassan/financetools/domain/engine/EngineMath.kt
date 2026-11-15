package com.educationwithhassan.financetools.domain.engine

import com.educationwithhassan.financetools.domain.model.CalculationOutput
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object EngineMath {
    private val symbols = DecimalFormatSymbols(Locale.US)

    fun round(value: Double, decimals: Int = 2): Double {
        if (!value.isFinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()
    }

    fun formatMoney(value: Double, currency: String = "USD", decimals: Int = 2): String {
        val safe = if (value.isFinite()) value else 0.0
        val sign = if (safe < 0) "-" else ""
        val rounded = round(abs(safe), decimals)
        val pattern = if (decimals > 0) "#,##0.${"0".repeat(decimals)}" else "#,##0"
        val df = DecimalFormat(pattern, symbols)
        return "$sign${CurrencyRatesProvider.symbolFor(currency)}${df.format(rounded)}"
    }

    fun formatNumber(value: Double, decimals: Int = 2): String {
        val safe = if (value.isFinite()) value else 0.0
        val rounded = round(safe, decimals)
        val pattern = if (decimals > 0) "#,##0.${"0".repeat(decimals)}" else "#,##0"
        return DecimalFormat(pattern, symbols).format(rounded)
    }

    fun formatPercent(value: Double, decimals: Int = 2): String {
        val safe = if (value.isFinite()) value else 0.0
        return "${String.format(Locale.US, "%.${decimals}f", round(safe, decimals))}%"
    }

    fun parseDouble(
        raw: String?,
        label: String,
        allowZero: Boolean = true,
        allowNegative: Boolean = false,
        min: Double? = null,
        max: Double? = null,
        defaultValue: Double? = null
    ): Result<Double> {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return if (defaultValue != null) Result.success(defaultValue)
            else Result.failure(IllegalArgumentException("Please enter $label."))
        }
        val num = trimmed.toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("$label must be a valid number."))
        if (!num.isFinite()) return Result.failure(IllegalArgumentException("$label must be finite."))
        if (!allowNegative && num < 0.0) return Result.failure(IllegalArgumentException("$label cannot be negative."))
        if (!allowZero && num == 0.0) return Result.failure(IllegalArgumentException("$label must be greater than zero."))
        if (min != null && num < min) return Result.failure(IllegalArgumentException("$label must be at least $min."))
        if (max != null && num > max) return Result.failure(IllegalArgumentException("$label cannot exceed $max."))
        return Result.success(num)
    }

    fun fail(msg: String): CalculationOutput = CalculationOutput(valid = false, errorMessage = msg)
}
