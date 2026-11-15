package com.educationwithhassan.financetools.domain.model

enum class FinanceCategory(
    val id: String,
    val title: String,
    val shortTitle: String,
    val description: String,
    val expectedCount: Int
) {
    PERSONAL("personal", "Personal Finance Tools", "Personal", "Budgets, expenses, savings goals & net worth", 15),
    BANKING("banking", "Banking & Loan Tools", "Banking & Loans", "Loans, mortgages, EMI, interest & deposits", 15),
    INVESTMENT("investment", "Investment Tools", "Investments", "ROI, stocks, SIP, mutual funds, CAGR & XIRR", 15),
    BUSINESS("business", "Business & Accounting Tools", "Business", "Margins, markup, B2B tax, invoices & payroll", 15),
    TAX("tax", "Tax Tools", "Taxes", "Income, salary, brackets, GST/VAT & capital gains", 10),
    CURRENCY("currency", "Currency & International Finance", "Currency & FX", "FX conversion, forex profit, transfers & inflation", 8),
    FREELANCE("freelance", "Freelancing & Salary Tools", "Freelance & Pay", "Hourly rate, paycheck, overtime & project quotes", 10),
    ISLAMIC("islamic", "Islamic Finance Tools", "Islamic Finance", "Zakat, Mudarabah, Mahr & inheritance estimator", 10),
    CRYPTO("crypto", "Cryptocurrency Finance Tools", "Crypto", "Hypothetical P&L, DCA average, mining & ROI", 8),
    ADVANCED("advanced", "Advanced Financial Tools", "Advanced", "Retirement, FIRE, annuities, Rule of 72 & TVM", 14)
}

enum class InputKind {
    CURRENCY, NUMBER, PERCENT, SELECT, TEXT, DATE
}

data class SelectOption(
    val label: String,
    val value: String
)

data class ToolField(
    val key: String,
    val label: String,
    val kind: InputKind,
    val defaultValue: String,
    val options: List<SelectOption> = emptyList(),
    val optional: Boolean = false
)

enum class DynamicListKind {
    NONE, EXPENSES, INCOMES, MEMBERS, HOLDINGS, INVOICE_ITEMS, RECEIPT_ITEMS, CASHFLOWS, TAX_BRACKETS, CRYPTO_BUYS, CRYPTO_HOLDINGS
}

data class DynamicRow(
    val id: String,
    val label: String,
    val amount: String,
    val quantity: String = "1",
    val category: String = "General",
    val extra: String = ""
)

data class FinanceTool(
    val number: Int,
    val id: String,
    val name: String,
    val category: FinanceCategory,
    val shortDescription: String,
    val keywords: List<String>,
    val popular: Boolean = false,
    val supportsCurrency: Boolean = true,
    val supportsPdfExport: Boolean = false,
    val listKind: DynamicListKind = DynamicListKind.NONE,
    val defaultRows: List<DynamicRow> = emptyList(),
    val formulaExplanation: String,
    val fields: List<ToolField>
)

data class MetricCardData(
    val label: String,
    val numericValue: Double,
    val formattedValue: String
)

data class BreakdownItem(
    val label: String,
    val value: String,
    val detail: String = ""
)

data class ChartSlice(
    val label: String,
    val value: Double
)

data class ChartSeries(
    val name: String,
    val points: List<Pair<String, Double>>
)

data class CalculationOutput(
    val valid: Boolean,
    val errorMessage: String? = null,
    val primaryLabel: String = "",
    val primaryValue: Double = 0.0,
    val primaryFormatted: String = "—",
    val metrics: List<MetricCardData> = emptyList(),
    val breakdown: List<BreakdownItem> = emptyList(),
    val tableHeaders: List<String> = emptyList(),
    val tableRows: List<List<String>> = emptyList(),
    val chartSlices: List<ChartSlice> = emptyList(),
    val chartSeries: List<ChartSeries> = emptyList(),
    val notes: List<String> = emptyList()
)
