package com.educationwithhassan.financetools.domain.engine

import com.educationwithhassan.financetools.domain.engine.EngineMath.fail
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatMoney
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatNumber
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatPercent
import com.educationwithhassan.financetools.domain.engine.EngineMath.parseDouble
import com.educationwithhassan.financetools.domain.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

object TaxCurrencyFreelanceEngine {
    fun calculate(
        toolId: String,
        inputs: Map<String, String>,
        rows: List<DynamicRow>,
        currency: String,
        fxRates: Map<String, Double>
    ): CalculationOutput? {
        return when (toolId) {
            "income-tax-calculator" -> {
                val g = parseDouble(inputs["grossIncome"], "Gross Income").getOrElse { return fail(it.message!!) }
                val d = parseDouble(inputs["deductions"], "Deductions").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["taxRate"], "Tax Rate %", max = 75.0).getOrElse { return fail(it.message!!) }
                val c = parseDouble(inputs["credits"], "Credits").getOrElse { return fail(it.message!!) }
                val taxable = max(0.0, g - d)
                val tax = max(0.0, taxable * r / 100.0 - c)
                CalculationOutput(true, primaryLabel = "Estimated Income Tax", primaryValue = tax, primaryFormatted = formatMoney(tax, currency),
                    metrics = listOf(MetricCardData("After-Tax Income", g - tax, formatMoney(g - tax, currency)), MetricCardData("Effective Rate", if (g > 0) (tax / g) * 100.0 else 0.0, formatPercent(if (g > 0) (tax / g) * 100.0 else 0.0))))
            }
            "salary-tax-calculator" -> {
                val ms = parseDouble(inputs["monthlySalary"], "Monthly Salary").getOrElse { return fail(it.message!!) }
                val ex = parseDouble(inputs["exemptAnnual"], "Annual Exempt").getOrElse { return fail(it.message!!) }
                val it = parseDouble(inputs["incomeTaxPct"], "Income Tax %", max = 65.0).getOrElse { return fail(it.message!!) }
                val ss = parseDouble(inputs["socialPct"], "Social %", max = 30.0).getOrElse { return fail(it.message!!) }
                val ann = ms * 12.0
                val totTax = max(0.0, ann - ex) * (it / 100.0) + ann * (ss / 100.0)
                CalculationOutput(true, primaryLabel = "Monthly Salary Tax", primaryValue = totTax / 12.0, primaryFormatted = formatMoney(totTax / 12.0, currency),
                    metrics = listOf(MetricCardData("Monthly Take-Home", ms - totTax / 12.0, formatMoney(ms - totTax / 12.0, currency)), MetricCardData("Annual Tax", totTax, formatMoney(totTax, currency))))
            }
            "tax-sales-tax-calculator" -> {
                val amt = parseDouble(inputs["amount"], "Amount").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 50.0).getOrElse { return fail(it.message!!) }
                val isAdd = (inputs["mode"] ?: "add") == "add"
                val pre = if (isAdd) amt else amt / (1.0 + r / 100.0)
                val tax = if (isAdd) amt * r / 100.0 else amt - pre
                val tot = pre + tax
                CalculationOutput(true, primaryLabel = if (isAdd) "Total Including Tax" else "Pre-Tax Base", primaryValue = if (isAdd) tot else pre, primaryFormatted = formatMoney(if (isAdd) tot else pre, currency),
                    metrics = listOf(MetricCardData("Sales Tax", tax, formatMoney(tax, currency))))
            }
            "tax-vat-calculator" -> {
                val a = parseDouble(inputs["amount"], "Amount").getOrElse { return fail(it.message!!) }
                val v = parseDouble(inputs["vatRate"], "VAT %", max = 50.0).getOrElse { return fail(it.message!!) }
                val add = (inputs["direction"] ?: "exclude_to_include") == "exclude_to_include"
                val net = if (add) a else a / (1.0 + v / 100.0)
                val vat = if (add) net * v / 100.0 else a - net
                CalculationOutput(true, primaryLabel = if (add) "Gross (Incl. VAT)" else "Net (Excl. VAT)", primaryValue = if (add) net + vat else net, primaryFormatted = formatMoney(if (add) net + vat else net, currency),
                    metrics = listOf(MetricCardData("VAT Amount", vat, formatMoney(vat, currency))))
            }
            "tax-refund-calculator" -> {
                val inc = parseDouble(inputs["income"], "Income").getOrElse { return fail(it.message!!) }
                val ded = parseDouble(inputs["deductions"], "Deductions").getOrElse { return fail(it.message!!) }
                val rate = parseDouble(inputs["effRate"], "Rate %", max = 70.0).getOrElse { return fail(it.message!!) }
                val cred = parseDouble(inputs["credits"], "Credits").getOrElse { return fail(it.message!!) }
                val wh = parseDouble(inputs["withheld"], "Withheld").getOrElse { return fail(it.message!!) }
                val liability = max(0.0, max(0.0, inc - ded) * rate / 100.0 - cred)
                val diff = wh - liability
                CalculationOutput(true, primaryLabel = if (diff >= 0) "Estimated Refund" else "Balance Owed", primaryValue = abs(diff), primaryFormatted = formatMoney(abs(diff), currency),
                    metrics = listOf(MetricCardData("Tax Liability", liability, formatMoney(liability, currency))))
            }
            "capital-gains-tax-calculator" -> {
                val bp = parseDouble(inputs["buyPrice"], "Cost Basis").getOrElse { return fail(it.message!!) }
                val sp = parseDouble(inputs["sellPrice"], "Sale Proceeds").getOrElse { return fail(it.message!!) }
                val lo = parseDouble(inputs["lossOffset"], "Loss Offset").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["taxRate"], "CGT Rate %", max = 60.0).getOrElse { return fail(it.message!!) }
                val gain = sp - bp
                val tax = max(0.0, gain - lo) * r / 100.0
                CalculationOutput(true, primaryLabel = "Capital Gains Tax Due", primaryValue = tax, primaryFormatted = formatMoney(tax, currency),
                    metrics = listOf(MetricCardData("Net Gain After Tax", gain - tax, formatMoney(gain - tax, currency))))
            }
            "tax-bracket-calculator" -> {
                val inc = parseDouble(inputs["taxableIncome"], "Taxable Income").getOrElse { return fail(it.message!!) }
                if (rows.isEmpty()) return fail("Add at least one tax bracket.")
                val slabs = rows.map {
                    val cap = parseDouble(it.amount, it.label, allowZero = false).getOrElse { e -> return fail(e.message!!) }
                    val rate = parseDouble(it.extra.ifBlank { "0" }, "Rate %", max = 90.0).getOrElse { e -> return fail(e.message!!) }
                    cap to rate
                }.sortedBy { it.first }
                var prev = 0.0; var totTax = 0.0
                for ((cap, rate) in slabs) {
                    val span = max(0.0, minOf(inc, cap) - prev)
                    totTax += span * rate / 100.0
                    prev = cap
                }
                CalculationOutput(true, primaryLabel = "Total Progressive Tax", primaryValue = totTax, primaryFormatted = formatMoney(totTax, currency),
                    metrics = listOf(MetricCardData("Effective Rate", if (inc > 0) (totTax / inc) * 100.0 else 0.0, formatPercent(if (inc > 0) (totTax / inc) * 100.0 else 0.0))))
            }
            "gst-calculator" -> {
                val a = parseDouble(inputs["amount"], "Amount").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["gstRate"], "GST %", max = 50.0).getOrElse { return fail(it.message!!) }
                val ex = (inputs["type"] ?: "exclusive") == "exclusive"
                val net = if (ex) a else a / (1.0 + r / 100.0)
                val gst = if (ex) net * r / 100.0 else a - net
                CalculationOutput(true, primaryLabel = if (ex) "Total (Incl. GST)" else "Pre-GST Net", primaryValue = if (ex) net + gst else net, primaryFormatted = formatMoney(if (ex) net + gst else net, currency),
                    metrics = listOf(MetricCardData("Total GST", gst, formatMoney(gst, currency)), MetricCardData("CGST / SGST Split", gst / 2.0, formatMoney(gst / 2.0, currency))))
            }
            "tax-zakat-calculator" -> {
                val c = parseDouble(inputs["cash"], "Cash").getOrElse { return fail(it.message!!) }
                val inv = parseDouble(inputs["investments"], "Investments").getOrElse { return fail(it.message!!) }
                val rec = parseDouble(inputs["receivables"], "Receivables").getOrElse { return fail(it.message!!) }
                val lib = parseDouble(inputs["liabilities"], "Liabilities").getOrElse { return fail(it.message!!) }
                val nis = parseDouble(inputs["nisab"], "Nisab").getOrElse { return fail(it.message!!) }
                val net = max(0.0, c + inv + rec - lib)
                val zakat = if (net >= nis && net > 0) net * 0.025 else 0.0
                CalculationOutput(true, primaryLabel = "Annual Zakat Payable", primaryValue = zakat, primaryFormatted = formatMoney(zakat, currency),
                    metrics = listOf(MetricCardData("Net Zakatable Wealth", net, formatMoney(net, currency))))
            }
            "withholding-tax-calculator" -> {
                val gp = parseDouble(inputs["grossPayment"], "Payment").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["whtRate"], "WHT %", max = 60.0).getOrElse { return fail(it.message!!) }
                val grossUp = inputs["grossUp"] == "yes"
                val gross = if (grossUp) gp / (1.0 - r / 100.0) else gp
                val wht = gross * r / 100.0
                val net = gross - wht
                CalculationOutput(true, primaryLabel = if (grossUp) "Required Gross Invoice" else "Net Payment Received", primaryValue = if (grossUp) gross else net, primaryFormatted = formatMoney(if (grossUp) gross else net, currency),
                    metrics = listOf(MetricCardData("WHT Deducted", wht, formatMoney(wht, currency))))
            }
            "currency-converter" -> {
                val amt = parseDouble(inputs["amount"], "Amount").getOrElse { return fail(it.message!!) }
                val from = inputs["fromCurrency"] ?: "USD"
                val to = inputs["toCurrency"] ?: "PKR"
                val customRaw = inputs["customRate"]?.trim().orEmpty()
                val rate = if (customRaw.isNotEmpty()) {
                    parseDouble(customRaw, "Manual Rate", allowZero = false).getOrElse { return fail(it.message!!) }
                } else {
                    (fxRates[to] ?: 1.0) / (fxRates[from] ?: 1.0)
                }
                val out = amt * rate
                CalculationOutput(true, primaryLabel = "Converted ($to)", primaryValue = out, primaryFormatted = formatMoney(out, to),
                    metrics = listOf(MetricCardData("Exchange Rate", rate, "1 $from = ${formatNumber(rate, 4)} $to")))
            }
            "exchange-rate-calculator" -> {
                val amt = parseDouble(inputs["amount"], "Amount", allowZero = false).getOrElse { return fail(it.message!!) }
                val to = inputs["toCurrency"] ?: "EUR"
                val mid = parseDouble(inputs["midRate"], "Mid Rate", allowZero = false).getOrElse { return fail(it.message!!) }
                val off = parseDouble(inputs["offeredRate"], "Offered Rate", allowZero = false).getOrElse { return fail(it.message!!) }
                val spread = ((mid - off) / mid) * 100.0
                CalculationOutput(true, primaryLabel = "Received ($to)", primaryValue = amt * off, primaryFormatted = formatMoney(amt * off, to),
                    metrics = listOf(MetricCardData("FX Markup Loss", amt * (mid - off), formatMoney(amt * (mid - off), to)), MetricCardData("Spread", spread, formatPercent(spread))))
            }
            "forex-profit-calculator" -> {
                val lots = parseDouble(inputs["lots"], "Lots", allowZero = false).getOrElse { return fail(it.message!!) }
                val en = parseDouble(inputs["entry"], "Entry", allowZero = false).getOrElse { return fail(it.message!!) }
                val ex = parseDouble(inputs["exit"], "Exit", allowZero = false).getOrElse { return fail(it.message!!) }
                val comm = parseDouble(inputs["commission"], "Commission").getOrElse { return fail(it.message!!) }
                val diff = if ((inputs["direction"] ?: "buy") == "buy") ex - en else en - ex
                val net = diff * lots * 100000.0 - comm
                CalculationOutput(true, primaryLabel = "Net Forex P&L", primaryValue = net, primaryFormatted = formatMoney(net, currency),
                    metrics = listOf(MetricCardData("Pips", diff * 10000.0, "${formatNumber(diff * 10000.0, 1)} pips")))
            }
            "international-money-transfer-calculator" -> {
                val send = parseDouble(inputs["sendAmount"], "Send Amount", allowZero = false).getOrElse { return fail(it.message!!) }
                val fee = parseDouble(inputs["transferFee"], "Fee").getOrElse { return fail(it.message!!) }
                if (fee >= send) return fail("Fee cannot exceed send amount.")
                val markup = parseDouble(inputs["fxMarkupPct"], "FX Margin %", max = 20.0).getOrElse { return fail(it.message!!) }
                val from = inputs["fromCurrency"] ?: "USD"
                val to = inputs["toCurrency"] ?: "PKR"
                val mid = (fxRates[to] ?: 1.0) / (fxRates[from] ?: 1.0)
                val eff = mid * (1.0 - markup / 100.0)
                val rec = (send - fee) * eff
                CalculationOutput(true, primaryLabel = "Recipient Receives ($to)", primaryValue = rec, primaryFormatted = formatMoney(rec, to),
                    metrics = listOf(MetricCardData("Effective Rate", eff, "1 $from = ${formatNumber(eff, 4)} $to")))
            }
            "purchasing-power-calculator" -> {
                val a = parseDouble(inputs["amount"], "Amount", allowZero = false).getOrElse { return fail(it.message!!) }
                val inf = parseDouble(inputs["inflation"], "Inflation %", max = 100.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val real = a / (1.0 + inf / 100.0).pow(y)
                CalculationOutput(true, primaryLabel = "Real Purchasing Power", primaryValue = real, primaryFormatted = formatMoney(real, currency),
                    metrics = listOf(MetricCardData("Power Lost", a - real, formatMoney(a - real, currency))))
            }
            "inflation-calculator" -> {
                val p = parseDouble(inputs["presentCost"], "Present Cost", allowZero = false).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["inflationRate"], "Inflation %", max = 100.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val fv = p * (1.0 + r / 100.0).pow(y)
                CalculationOutput(true, primaryLabel = "Future Cost", primaryValue = fv, primaryFormatted = formatMoney(fv, currency),
                    metrics = listOf(MetricCardData("Price Increase", fv - p, formatMoney(fv - p, currency))))
            }
            "currency-appreciation-calculator" -> {
                val o = parseDouble(inputs["oldRate"], "Old Rate", allowZero = false).getOrElse { return fail(it.message!!) }
                val n = parseDouble(inputs["newRate"], "New Rate", allowZero = false).getOrElse { return fail(it.message!!) }
                val a = parseDouble(inputs["baseAmount"], "Base Holding").getOrElse { return fail(it.message!!) }
                val pct = ((n - o) / o) * 100.0
                CalculationOutput(true, primaryLabel = "Currency Appreciation", primaryValue = pct, primaryFormatted = formatPercent(pct),
                    metrics = listOf(MetricCardData("Value Change", a * (n - o), formatNumber(a * (n - o), 2))))
            }
            "currency-depreciation-calculator" -> {
                val o = parseDouble(inputs["oldRate"], "Old Rate", allowZero = false).getOrElse { return fail(it.message!!) }
                val n = parseDouble(inputs["newRate"], "New Rate", allowZero = false).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 0.1).getOrElse { return fail(it.message!!) }
                val dep = ((o - n) / o) * 100.0
                val ann = (1.0 - (n / o).pow(1.0 / y)) * 100.0
                CalculationOutput(true, primaryLabel = "Cumulative Depreciation", primaryValue = abs(dep), primaryFormatted = formatPercent(dep),
                    metrics = listOf(MetricCardData("Annualized Rate", ann, formatPercent(ann))))
            }
            "hourly-rate-calculator" -> {
                val net = parseDouble(inputs["targetNet"], "Target Net", allowZero = false).getOrElse { return fail(it.message!!) }
                val ov = parseDouble(inputs["overhead"], "Overhead").getOrElse { return fail(it.message!!) }
                val tx = parseDouble(inputs["taxPct"], "Tax %", max = 65.0).getOrElse { return fail(it.message!!) }
                val hrs = parseDouble(inputs["billableHoursWeek"], "Hours/Wk", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val wks = parseDouble(inputs["workingWeeks"], "Weeks/Yr", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val gross = net / (1.0 - tx / 100.0) + ov
                val rate = gross / (hrs * wks)
                CalculationOutput(true, primaryLabel = "Minimum Hourly Rate", primaryValue = rate, primaryFormatted = "${formatMoney(rate, currency)} / hr",
                    metrics = listOf(MetricCardData("Required Annual Gross", gross, formatMoney(gross, currency))))
            }
            "freelance-income-calculator" -> {
                val h = parseDouble(inputs["hoursMonth"], "Hours").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["hourlyRate"], "Rate").getOrElse { return fail(it.message!!) }
                val ret = parseDouble(inputs["retainers"], "Retainers").getOrElse { return fail(it.message!!) }
                val pf = parseDouble(inputs["platformFeePct"], "Platform Fee %", max = 40.0).getOrElse { return fail(it.message!!) }
                val ex = parseDouble(inputs["expenses"], "Expenses").getOrElse { return fail(it.message!!) }
                val gross = h * r + ret
                val net = gross * (1.0 - pf / 100.0) - ex
                CalculationOutput(true, primaryLabel = "Net Monthly Income", primaryValue = net, primaryFormatted = formatMoney(net, currency),
                    metrics = listOf(MetricCardData("Annualized Net", net * 12.0, formatMoney(net * 12.0, currency))))
            }
            "salary-calculator" -> {
                val amt = parseDouble(inputs["amount"], "Amount").getOrElse { return fail(it.message!!) }
                val hpw = parseDouble(inputs["hoursPerWeek"], "Hours/Wk", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val wpy = parseDouble(inputs["weeksPerYear"], "Weeks/Yr", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val annual = when (inputs["period"]) {
                    "monthly" -> amt * 12.0; "biweekly" -> amt * 26.0; "weekly" -> amt * wpy; "hourly" -> amt * hpw * wpy; else -> amt
                }
                CalculationOutput(true, primaryLabel = "Annual Gross Salary", primaryValue = annual, primaryFormatted = formatMoney(annual, currency),
                    metrics = listOf(MetricCardData("Monthly", annual / 12.0, formatMoney(annual / 12.0, currency)), MetricCardData("Hourly", annual / (hpw * wpy), formatMoney(annual / (hpw * wpy), currency))))
            }
            "annual-salary-calculator" -> {
                val b = parseDouble(inputs["baseSalary"], "Base Salary").getOrElse { return fail(it.message!!) }
                val bp = parseDouble(inputs["bonusPct"], "Bonus %", max = 200.0).getOrElse { return fail(it.message!!) }
                val ben = parseDouble(inputs["benefitsValue"], "Benefits").getOrElse { return fail(it.message!!) }
                val tx = parseDouble(inputs["taxPct"], "Tax %", max = 65.0).getOrElse { return fail(it.message!!) }
                val cash = b * (1.0 + bp / 100.0)
                val comp = cash + ben
                CalculationOutput(true, primaryLabel = "Total Compensation", primaryValue = comp, primaryFormatted = formatMoney(comp, currency),
                    metrics = listOf(MetricCardData("Net Annual Cash", cash * (1.0 - tx / 100.0), formatMoney(cash * (1.0 - tx / 100.0), currency))))
            }
            "overtime-pay-calculator" -> {
                val r = parseDouble(inputs["hourlyRate"], "Hourly Wage", allowZero = false).getOrElse { return fail(it.message!!) }
                val reg = parseDouble(inputs["regularHours"], "Regular Hrs").getOrElse { return fail(it.message!!) }
                val ot = parseDouble(inputs["overtimeHours"], "OT Hrs").getOrElse { return fail(it.message!!) }
                val mult = parseDouble(inputs["otMultiplier"], "Multiplier", min = 1.0, max = 5.0).getOrElse { return fail(it.message!!) }
                val dbl = parseDouble(inputs["doubleHours"], "Double-Time Hrs").getOrElse { return fail(it.message!!) }
                val tot = reg * r + ot * r * mult + dbl * r * 2.0
                CalculationOutput(true, primaryLabel = "Total Gross Pay", primaryValue = tot, primaryFormatted = formatMoney(tot, currency),
                    metrics = listOf(MetricCardData("Overtime Pay", ot * r * mult + dbl * r * 2.0, formatMoney(ot * r * mult + dbl * r * 2.0, currency))))
            }
            "paycheck-calculator" -> {
                val g = parseDouble(inputs["grossPay"], "Gross Pay", allowZero = false).getOrElse { return fail(it.message!!) }
                val pre = parseDouble(inputs["preTaxDed"], "Pre-Tax").getOrElse { return fail(it.message!!) }
                val it = parseDouble(inputs["incomeTaxPct"], "Income Tax %", max = 60.0).getOrElse { return fail(it.message!!) }
                val pt = parseDouble(inputs["payrollTaxPct"], "Payroll Tax %", max = 30.0).getOrElse { return fail(it.message!!) }
                val post = parseDouble(inputs["postTaxDed"], "Post-Tax").getOrElse { return fail(it.message!!) }
                val taxable = max(0.0, g - pre)
                val net = max(0.0, taxable - taxable * it / 100.0 - g * pt / 100.0 - post)
                CalculationOutput(true, primaryLabel = "Net Take-Home Paycheck", primaryValue = net, primaryFormatted = formatMoney(net, currency))
            }
            "hourly-to-monthly-salary-converter" -> {
                val hw = parseDouble(inputs["hourlyWage"], "Hourly Wage", allowZero = false).getOrElse { return fail(it.message!!) }
                val hpw = parseDouble(inputs["hoursWeek"], "Hours/Wk", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val pw = parseDouble(inputs["paidWeeks"], "Paid Weeks", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val annual = hw * hpw * pw
                CalculationOutput(true, primaryLabel = "Monthly Gross Salary", primaryValue = annual / 12.0, primaryFormatted = formatMoney(annual / 12.0, currency),
                    metrics = listOf(MetricCardData("Annual Salary", annual, formatMoney(annual, currency))))
            }
            "freelancer-tax-calculator" -> {
                val g = parseDouble(inputs["grossReceipts"], "Gross Revenue").getOrElse { return fail(it.message!!) }
                val ex = parseDouble(inputs["businessExpenses"], "Expenses").getOrElse { return fail(it.message!!) }
                val se = parseDouble(inputs["seTaxPct"], "SE Tax %", max = 40.0).getOrElse { return fail(it.message!!) }
                val it = parseDouble(inputs["incomeTaxPct"], "Income Tax %", max = 60.0).getOrElse { return fail(it.message!!) }
                val profit = max(0.0, g - ex)
                val seTax = profit * 0.9235 * (se / 100.0)
                val incTax = max(0.0, profit - seTax * 0.5) * (it / 100.0)
                val totTax = seTax + incTax
                CalculationOutput(true, primaryLabel = "Total Annual Tax", primaryValue = totTax, primaryFormatted = formatMoney(totTax, currency),
                    metrics = listOf(MetricCardData("Quarterly Estimate", totTax / 4.0, formatMoney(totTax / 4.0, currency))))
            }
            "project-cost-calculator" -> {
                val hrs = parseDouble(inputs["estHours"], "Hours", allowZero = false).getOrElse { return fail(it.message!!) }
                val rate = parseDouble(inputs["hourlyRate"], "Rate").getOrElse { return fail(it.message!!) }
                val dir = parseDouble(inputs["directCosts"], "Direct Costs").getOrElse { return fail(it.message!!) }
                val buf = parseDouble(inputs["bufferPct"], "Buffer %", max = 100.0).getOrElse { return fail(it.message!!) }
                val prof = parseDouble(inputs["profitPct"], "Profit %", max = 200.0).getOrElse { return fail(it.message!!) }
                val quote = ((hrs * rate + dir) * (1.0 + buf / 100.0)) * (1.0 + prof / 100.0)
                CalculationOutput(true, primaryLabel = "Recommended Client Quote", primaryValue = quote, primaryFormatted = formatMoney(quote, currency),
                    metrics = listOf(MetricCardData("Effective Hourly", quote / hrs, formatMoney(quote / hrs, currency))))
            }
            "freelance-profit-calculator" -> {
                val g = parseDouble(inputs["grossRevenue"], "Revenue", allowZero = false).getOrElse { return fail(it.message!!) }
                val pf = parseDouble(inputs["platformFees"], "Platform Fees").getOrElse { return fail(it.message!!) }
                val ov = parseDouble(inputs["overhead"], "Overhead").getOrElse { return fail(it.message!!) }
                val tx = parseDouble(inputs["taxReserve"], "Tax Reserve").getOrElse { return fail(it.message!!) }
                val hrs = parseDouble(inputs["totalHours"], "Hours", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val net = g - pf - ov - tx
                CalculationOutput(true, primaryLabel = "Net Freelance Profit", primaryValue = net, primaryFormatted = formatMoney(net, currency),
                    metrics = listOf(MetricCardData("Profit Margin", (net / g) * 100.0, formatPercent((net / g) * 100.0)), MetricCardData("Net Hourly", net / hrs, formatMoney(net / hrs, currency))))
            }
            else -> null
        }
    }
}
