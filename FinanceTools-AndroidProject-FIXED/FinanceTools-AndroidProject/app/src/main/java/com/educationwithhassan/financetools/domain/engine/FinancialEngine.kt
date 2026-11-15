package com.educationwithhassan.financetools.domain.engine

import com.educationwithhassan.financetools.domain.model.CalculationOutput
import com.educationwithhassan.financetools.domain.model.DynamicRow

object FinancialEngine {
    fun calculate(
        toolId: String,
        inputs: Map<String, String>,
        rows: List<DynamicRow> = emptyList(),
        currency: String = "USD",
        fxRates: Map<String, Double> = CurrencyRatesProvider.baselineRatesUsd
    ): CalculationOutput {
        PersonalBankingEngine.calculate(toolId, inputs, rows, currency)?.let { return it }
        InvestmentBusinessEngine.calculate(toolId, inputs, rows, currency)?.let { return it }
        TaxCurrencyFreelanceEngine.calculate(toolId, inputs, rows, currency, fxRates)?.let { return it }
        IslamicCryptoAdvancedEngine.calculate(toolId, inputs, rows, currency)?.let { return it }
        return EngineMath.fail("Calculator '$toolId' is not registered in FinancialEngine.")
    }
}
