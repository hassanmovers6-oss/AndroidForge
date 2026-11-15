package com.educationwithhassan.financetools.domain.engine

import com.educationwithhassan.financetools.domain.engine.EngineMath.fail
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatMoney
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatNumber
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatPercent
import com.educationwithhassan.financetools.domain.engine.EngineMath.parseDouble
import com.educationwithhassan.financetools.domain.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

object InvestmentBusinessEngine {
    fun calculate(toolId: String, inputs: Map<String, String>, rows: List<DynamicRow>, currency: String): CalculationOutput? {
        return when (toolId) {
            "investment-calculator" -> {
                val p = parseDouble(inputs["initial"], "Initial").getOrElse { return fail(it.message!!) }
                val m = parseDouble(inputs["monthly"], "Monthly").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["returnRate"], "Return %", max = 50.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val (fv, contrib) = PersonalBankingEngine.computeCompound(p, r, y, 12, m)
                CalculationOutput(true, primaryLabel = "Projected Ending Value", primaryValue = fv, primaryFormatted = formatMoney(fv, currency),
                    metrics = listOf(MetricCardData("Invested", contrib, formatMoney(contrib, currency)), MetricCardData("Gains", fv - contrib, formatMoney(fv - contrib, currency))))
            }
            "roi-calculator" -> {
                val init = parseDouble(inputs["initial"], "Initial", allowZero = false).getOrElse { return fail(it.message!!) }
                val fin = parseDouble(inputs["finalValue"], "Final Value").getOrElse { return fail(it.message!!) }
                val yrs = parseDouble(inputs["years"], "Years", allowZero = false, min = 0.05).getOrElse { return fail(it.message!!) }
                val gain = fin - init
                val roi = (gain / init) * 100.0
                val ann = ((fin / init).pow(1.0 / yrs) - 1.0) * 100.0
                CalculationOutput(true, primaryLabel = "ROI", primaryValue = roi, primaryFormatted = formatPercent(roi),
                    metrics = listOf(MetricCardData("Net Gain", gain, formatMoney(gain, currency)), MetricCardData("Annualized ROI", ann, formatPercent(ann))))
            }
            "stock-profit-calculator" -> {
                val sh = parseDouble(inputs["shares"], "Shares", allowZero = false).getOrElse { return fail(it.message!!) }
                val bp = parseDouble(inputs["buyPrice"], "Buy Price", allowZero = false).getOrElse { return fail(it.message!!) }
                val sp = parseDouble(inputs["sellPrice"], "Sell Price").getOrElse { return fail(it.message!!) }
                val bc = parseDouble(inputs["buyComm"], "Buy Comm").getOrElse { return fail(it.message!!) }
                val sc = parseDouble(inputs["sellComm"], "Sell Comm").getOrElse { return fail(it.message!!) }
                val cost = sh * bp + bc
                val profit = (sh * sp - sc) - cost
                val roi = if (cost > 0) (profit / cost) * 100.0 else 0.0
                CalculationOutput(true, primaryLabel = "Net Stock Profit", primaryValue = profit, primaryFormatted = formatMoney(profit, currency),
                    metrics = listOf(MetricCardData("Return", roi, formatPercent(roi)), MetricCardData("Break-Even Price", (cost + sc) / sh, formatMoney((cost + sc) / sh, currency))))
            }
            "stock-average-calculator" -> {
                val s1 = parseDouble(inputs["shares1"], "Shares 1", allowZero = false).getOrElse { return fail(it.message!!) }
                val p1 = parseDouble(inputs["price1"], "Price 1").getOrElse { return fail(it.message!!) }
                val s2 = parseDouble(inputs["shares2"], "Shares 2", allowZero = false).getOrElse { return fail(it.message!!) }
                val p2 = parseDouble(inputs["price2"], "Price 2").getOrElse { return fail(it.message!!) }
                val totS = s1 + s2
                val totC = s1 * p1 + s2 * p2
                val avg = totC / totS
                CalculationOutput(true, primaryLabel = "New Average Price", primaryValue = avg, primaryFormatted = formatMoney(avg, currency),
                    metrics = listOf(MetricCardData("Total Shares", totS, formatNumber(totS, 2)), MetricCardData("Total Cost", totC, formatMoney(totC, currency))))
            }
            "dividend-calculator" -> {
                val sh = parseDouble(inputs["shares"], "Shares", allowZero = false).getOrElse { return fail(it.message!!) }
                val sp = parseDouble(inputs["sharePrice"], "Share Price", allowZero = false).getOrElse { return fail(it.message!!) }
                val dps = parseDouble(inputs["divPerShare"], "Dividend/Share").getOrElse { return fail(it.message!!) }
                val freq = inputs["freq"]?.toIntOrNull() ?: 4
                val tx = parseDouble(inputs["taxPct"], "Tax %", max = 60.0).getOrElse { return fail(it.message!!) }
                val gross = sh * dps * freq
                val net = gross * (1.0 - tx / 100.0)
                val yld = (gross / (sh * sp)) * 100.0
                CalculationOutput(true, primaryLabel = "Net Annual Dividend", primaryValue = net, primaryFormatted = formatMoney(net, currency),
                    metrics = listOf(MetricCardData("Yield", yld, formatPercent(yld)), MetricCardData("Monthly Net", net / 12.0, formatMoney(net / 12.0, currency))))
            }
            "sip-calculator" -> {
                val m = parseDouble(inputs["monthly"], "Monthly SIP", allowZero = false).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 50.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val months = (y * 12).toInt()
                val i = r / 100.0 / 12.0
                val inv = m * months
                val fv = if (i == 0.0) inv else m * (((1.0 + i).pow(months) - 1.0) / i) * (1.0 + i)
                CalculationOutput(true, primaryLabel = "SIP Maturity Value", primaryValue = fv, primaryFormatted = formatMoney(fv, currency),
                    metrics = listOf(MetricCardData("Invested", inv, formatMoney(inv, currency)), MetricCardData("Wealth Gain", fv - inv, formatMoney(fv - inv, currency))))
            }
            "mutual-fund-calculator" -> {
                val p = parseDouble(inputs["lumpSum"], "Lump Sum", allowZero = false).getOrElse { return fail(it.message!!) }
                val gr = parseDouble(inputs["grossReturn"], "Gross Return %", max = 50.0).getOrElse { return fail(it.message!!) }
                val er = parseDouble(inputs["expenseRatio"], "Expense Ratio %", max = 10.0).getOrElse { return fail(it.message!!) }
                val el = parseDouble(inputs["entryLoad"], "Entry Fee %", max = 10.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val netFv = p * (1.0 - el / 100.0) * (1.0 + (gr - er) / 100.0).pow(y)
                val grossFv = p * (1.0 + gr / 100.0).pow(y)
                CalculationOutput(true, primaryLabel = "Net Mutual Fund Value", primaryValue = netFv, primaryFormatted = formatMoney(netFv, currency),
                    metrics = listOf(MetricCardData("Fee Drag", grossFv - netFv, formatMoney(grossFv - netFv, currency))))
            }
            "compound-growth-calculator" -> {
                val start = parseDouble(inputs["startValue"], "Start Value").getOrElse { return fail(it.message!!) }
                var add = parseDouble(inputs["annualContrib"], "Annual Addition").getOrElse { return fail(it.message!!) }
                val step = parseDouble(inputs["stepUpPct"], "Step-Up %", max = 30.0).getOrElse { return fail(it.message!!) }
                val rate = parseDouble(inputs["growthRate"], "Growth Rate %", max = 50.0).getOrElse { return fail(it.message!!) }
                val yrs = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }.toInt()
                var bal = start
                var totIn = start
                for (t in 1..yrs) {
                    bal = (bal + add) * (1.0 + rate / 100.0)
                    totIn += add
                    add *= (1.0 + step / 100.0)
                }
                CalculationOutput(true, primaryLabel = "Final Compounded Value", primaryValue = bal, primaryFormatted = formatMoney(bal, currency),
                    metrics = listOf(MetricCardData("Capital Added", totIn, formatMoney(totIn, currency)), MetricCardData("Gains", bal - totIn, formatMoney(bal - totIn, currency))))
            }
            "portfolio-tracker" -> {
                val cash = parseDouble(inputs["cashReserve"], "Cash").getOrElse { return fail(it.message!!) }
                if (rows.isEmpty()) return fail("Add at least one holding.")
                var totMkt = cash
                var totCost = cash
                val slices = mutableListOf<ChartSlice>()
                for (r in rows) {
                    val mkt = parseDouble(r.amount, r.label).getOrElse { return fail(it.message!!) }
                    val cost = parseDouble(r.extra.ifBlank { r.amount }, "Cost Basis").getOrElse { return fail(it.message!!) }
                    totMkt += mkt
                    totCost += cost
                    slices.add(ChartSlice(r.label, mkt))
                }
                CalculationOutput(true, primaryLabel = "Total Portfolio Value", primaryValue = totMkt, primaryFormatted = formatMoney(totMkt, currency),
                    metrics = listOf(MetricCardData("Unrealized P&L", totMkt - totCost, formatMoney(totMkt - totCost, currency))), chartSlices = slices)
            }
            "asset-allocation-calculator" -> {
                val cs = parseDouble(inputs["curStocks"], "Equities").getOrElse { return fail(it.message!!) }
                val cb = parseDouble(inputs["curBonds"], "Bonds").getOrElse { return fail(it.message!!) }
                val cc = parseDouble(inputs["curCash"], "Cash").getOrElse { return fail(it.message!!) }
                val ts = parseDouble(inputs["tgtStocks"], "Target Equities %", max = 100.0).getOrElse { return fail(it.message!!) }
                val tb = parseDouble(inputs["tgtBonds"], "Target Bonds %", max = 100.0).getOrElse { return fail(it.message!!) }
                val tc = parseDouble(inputs["tgtCash"], "Target Cash %", max = 100.0).getOrElse { return fail(it.message!!) }
                if (abs(ts + tb + tc - 100.0) > 0.01) return fail("Targets must sum to 100%.")
                val tot = cs + cb + cc
                if (tot <= 0) return fail("Enter non-zero portfolio values.")
                CalculationOutput(true, primaryLabel = "Total Portfolio Value", primaryValue = tot, primaryFormatted = formatMoney(tot, currency),
                    breakdown = listOf(
                        BreakdownItem("Equities Action", formatMoney(tot * ts / 100.0 - cs, currency)),
                        BreakdownItem("Bonds Action", formatMoney(tot * tb / 100.0 - cb, currency)),
                        BreakdownItem("Cash Action", formatMoney(tot * tc / 100.0 - cc, currency))
                    ))
            }
            "risk-return-calculator" -> {
                val er = parseDouble(inputs["expectedReturn"], "Expected Return %", allowNegative = true, min = -100.0, max = 200.0).getOrElse { return fail(it.message!!) }
                val rf = parseDouble(inputs["riskFreeRate"], "Risk-Free %", max = 50.0).getOrElse { return fail(it.message!!) }
                val vol = parseDouble(inputs["volatility"], "Volatility %", allowZero = false, min = 0.01, max = 200.0).getOrElse { return fail(it.message!!) }
                val sharpe = (er - rf) / vol
                CalculationOutput(true, primaryLabel = "Sharpe Ratio", primaryValue = sharpe, primaryFormatted = formatNumber(sharpe, 3),
                    metrics = listOf(MetricCardData("Excess Return", er - rf, formatPercent(er - rf))))
            }
            "capital-gains-calculator" -> {
                val bc = parseDouble(inputs["buyCost"], "Buy Cost").getOrElse { return fail(it.message!!) }
                val bf = parseDouble(inputs["buyFees"], "Buy Fees").getOrElse { return fail(it.message!!) }
                val sp = parseDouble(inputs["salePrice"], "Sale Price").getOrElse { return fail(it.message!!) }
                val sf = parseDouble(inputs["sellFees"], "Sell Fees").getOrElse { return fail(it.message!!) }
                val tx = parseDouble(inputs["taxPct"], "Tax %", max = 60.0).getOrElse { return fail(it.message!!) }
                val gain = (sp - sf) - (bc + bf)
                val tax = if (gain > 0) gain * tx / 100.0 else 0.0
                CalculationOutput(true, primaryLabel = "Realized Capital Gain", primaryValue = gain, primaryFormatted = formatMoney(gain, currency),
                    metrics = listOf(MetricCardData("Tax Due", tax, formatMoney(tax, currency)), MetricCardData("Net Profit", gain - tax, formatMoney(gain - tax, currency))))
            }
            "break-even-calculator" -> {
                val fc = parseDouble(inputs["fixedCosts"], "Fixed Costs").getOrElse { return fail(it.message!!) }
                val p = parseDouble(inputs["pricePerUnit"], "Price", allowZero = false).getOrElse { return fail(it.message!!) }
                val v = parseDouble(inputs["varCostPerUnit"], "Variable Cost").getOrElse { return fail(it.message!!) }
                val cm = p - v
                if (cm <= 0) return fail("Selling price must exceed variable cost.")
                val units = fc / cm
                CalculationOutput(true, primaryLabel = "Break-Even Units", primaryValue = ceil(units), primaryFormatted = "${formatNumber(units, 1)} units",
                    metrics = listOf(MetricCardData("Break-Even Revenue", units * p, formatMoney(units * p, currency))))
            }
            "cagr-calculator" -> {
                val b = parseDouble(inputs["beginValue"], "Beginning Value", allowZero = false).getOrElse { return fail(it.message!!) }
                val e = parseDouble(inputs["endValue"], "Ending Value", allowZero = false).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 0.1).getOrElse { return fail(it.message!!) }
                val cagr = ((e / b).pow(1.0 / y) - 1.0) * 100.0
                CalculationOutput(true, primaryLabel = "CAGR", primaryValue = cagr, primaryFormatted = formatPercent(cagr),
                    metrics = listOf(MetricCardData("Total Gain", e - b, formatMoney(e - b, currency))))
            }
            "xirr-calculator" -> {
                if (rows.size < 2) return fail("Add at least 2 dated cash flows.")
                val flows = mutableListOf<Pair<Double, LocalDate>>()
                var hasPos = false; var hasNeg = false; var net = 0.0
                for (r in rows) {
                    val amt = parseDouble(r.amount, r.label, allowNegative = true).getOrElse { return fail(it.message!!) }
                    val dt = runCatching { LocalDate.parse(r.extra) }.getOrElse { return fail("Invalid date for ${r.label} (use YYYY-MM-DD).") }
                    if (amt > 0) hasPos = true
                    if (amt < 0) hasNeg = true
                    net += amt
                    flows.add(amt to dt)
                }
                if (!hasPos || !hasNeg) return fail("XIRR needs at least one negative outflow and one positive inflow.")
                flows.sortBy { it.second }
                val d0 = flows.first().second
                val yrs = flows.map { ChronoUnit.DAYS.between(d0, it.second) / 365.0 }
                var rate = 0.1
                for (iter in 0 until 100) {
                    var npv = 0.0; var dNpv = 0.0
                    for (i in flows.indices) {
                        val t = yrs[i]; val base = 1.0 + rate
                        if (base <= 0.0001) break
                        val denom = base.pow(t)
                        npv += flows[i].first / denom
                        dNpv -= (t * flows[i].first) / (denom * base)
                    }
                    if (abs(npv) < 1e-6 || abs(dNpv) < 1e-10) break
                    val next = rate - npv / dNpv
                    rate = if (!next.isFinite() || next <= -0.999) rate * 0.5 else next
                }
                CalculationOutput(true, primaryLabel = "Annualized XIRR", primaryValue = rate * 100.0, primaryFormatted = formatPercent(rate * 100.0),
                    metrics = listOf(MetricCardData("Net Cash Profit", net, formatMoney(net, currency))))
            }
            "profit-calculator" -> {
                val c = parseDouble(inputs["cost"], "Cost").getOrElse { return fail(it.message!!) }
                val s = parseDouble(inputs["selling"], "Selling").getOrElse { return fail(it.message!!) }
                val u = parseDouble(inputs["units"], "Units", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val profit = (s - c) * u
                val margin = if (s > 0) ((s - c) / s) * 100.0 else 0.0
                CalculationOutput(true, primaryLabel = "Total Profit", primaryValue = profit, primaryFormatted = formatMoney(profit, currency),
                    metrics = listOf(MetricCardData("Margin", margin, formatPercent(margin))))
            }
            "loss-calculator" -> {
                val c = parseDouble(inputs["cost"], "Cost", allowZero = false).getOrElse { return fail(it.message!!) }
                val s = parseDouble(inputs["selling"], "Selling").getOrElse { return fail(it.message!!) }
                val loss = c - s
                CalculationOutput(true, primaryLabel = if (loss > 0) "Total Loss" else "Net Gain", primaryValue = abs(loss), primaryFormatted = formatMoney(abs(loss), currency),
                    metrics = listOf(MetricCardData("Loss %", max(0.0, (loss / c) * 100.0), formatPercent(max(0.0, (loss / c) * 100.0)))))
            }
            "profit-margin-calculator" -> {
                val c = parseDouble(inputs["cost"], "Cost").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["revenue"], "Revenue", allowZero = false).getOrElse { return fail(it.message!!) }
                val tm = parseDouble(inputs["targetMargin"], "Target Margin %", max = 99.9).getOrElse { return fail(it.message!!) }
                val margin = ((r - c) / r) * 100.0
                CalculationOutput(true, primaryLabel = "Profit Margin", primaryValue = margin, primaryFormatted = formatPercent(margin),
                    metrics = listOf(MetricCardData("Net Profit", r - c, formatMoney(r - c, currency)), MetricCardData("Target Price", c / (1.0 - tm / 100.0), formatMoney(c / (1.0 - tm / 100.0), currency))))
            }
            "gross-profit-calculator" -> {
                val rev = parseDouble(inputs["revenue"], "Revenue", allowZero = false).getOrElse { return fail(it.message!!) }
                val cogs = parseDouble(inputs["cogs"], "COGS").getOrElse { return fail(it.message!!) }
                val gp = rev - cogs
                CalculationOutput(true, primaryLabel = "Gross Profit", primaryValue = gp, primaryFormatted = formatMoney(gp, currency),
                    metrics = listOf(MetricCardData("Gross Margin", (gp / rev) * 100.0, formatPercent((gp / rev) * 100.0))))
            }
            "net-profit-calculator" -> {
                val rev = parseDouble(inputs["revenue"], "Revenue", allowZero = false).getOrElse { return fail(it.message!!) }
                val cogs = parseDouble(inputs["cogs"], "COGS").getOrElse { return fail(it.message!!) }
                val opex = parseDouble(inputs["opex"], "OpEx").getOrElse { return fail(it.message!!) }
                val int = parseDouble(inputs["interest"], "Interest").getOrElse { return fail(it.message!!) }
                val tx = parseDouble(inputs["taxes"], "Taxes").getOrElse { return fail(it.message!!) }
                val np = rev - cogs - opex - int - tx
                CalculationOutput(true, primaryLabel = "Net Profit", primaryValue = np, primaryFormatted = formatMoney(np, currency),
                    metrics = listOf(MetricCardData("Net Margin", (np / rev) * 100.0, formatPercent((np / rev) * 100.0))))
            }
            "markup-calculator" -> {
                val c = parseDouble(inputs["cost"], "Cost", allowZero = false).getOrElse { return fail(it.message!!) }
                val mk = parseDouble(inputs["markupPct"], "Markup %", max = 1000.0).getOrElse { return fail(it.message!!) }
                val price = c * (1.0 + mk / 100.0)
                CalculationOutput(true, primaryLabel = "Selling Price", primaryValue = price, primaryFormatted = formatMoney(price, currency),
                    metrics = listOf(MetricCardData("Profit / Unit", price - c, formatMoney(price - c, currency)), MetricCardData("Margin %", ((price - c) / price) * 100.0, formatPercent(((price - c) / price) * 100.0))))
            }
            "business-vat-calculator" -> {
                val s = parseDouble(inputs["salesNet"], "Sales").getOrElse { return fail(it.message!!) }
                val p = parseDouble(inputs["purchasesNet"], "Purchases").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["vatRate"], "VAT %", max = 50.0).getOrElse { return fail(it.message!!) }
                val outVat = s * r / 100.0
                val inVat = p * r / 100.0
                val net = outVat - inVat
                CalculationOutput(true, primaryLabel = if (net >= 0) "Net VAT Payable" else "Net VAT Refund", primaryValue = abs(net), primaryFormatted = formatMoney(abs(net), currency),
                    metrics = listOf(MetricCardData("Output VAT", outVat, formatMoney(outVat, currency)), MetricCardData("Input VAT Credit", inVat, formatMoney(inVat, currency))))
            }
            "business-sales-tax-calculator" -> {
                val up = parseDouble(inputs["unitPrice"], "Unit Price").getOrElse { return fail(it.message!!) }
                val qty = parseDouble(inputs["qty"], "Quantity", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val disc = parseDouble(inputs["discount"], "Discount").getOrElse { return fail(it.message!!) }
                val tr = parseDouble(inputs["taxRate"], "Tax %", max = 40.0).getOrElse { return fail(it.message!!) }
                val sub = max(0.0, up * qty - disc)
                val tax = sub * tr / 100.0
                CalculationOutput(true, primaryLabel = "Grand Total", primaryValue = sub + tax, primaryFormatted = formatMoney(sub + tax, currency),
                    metrics = listOf(MetricCardData("Subtotal", sub, formatMoney(sub, currency)), MetricCardData("Tax", tax, formatMoney(tax, currency))))
            }
            "invoice-generator", "receipt-generator" -> {
                val tx = parseDouble(inputs["taxPct"], "Tax %", max = 50.0).getOrElse { return fail(it.message!!) }
                val disc = parseDouble(inputs["discount"], "Discount", defaultValue = 0.0).getOrElse { return fail(it.message!!) }
                if (rows.isEmpty()) return fail("Add at least one line item.")
                var sub = 0.0
                val tRows = mutableListOf<List<String>>()
                for (r in rows) {
                    val pr = parseDouble(r.amount, r.label).getOrElse { return fail(it.message!!) }
                    val qt = parseDouble(r.quantity, "Qty", allowZero = false).getOrElse { return fail(it.message!!) }
                    val tot = pr * qt
                    sub += tot
                    tRows.add(listOf(r.label, formatNumber(qt, 2), formatMoney(pr, currency), formatMoney(tot, currency)))
                }
                val afterDisc = max(0.0, sub - disc)
                val taxAmt = afterDisc * tx / 100.0
                val grand = afterDisc + taxAmt
                CalculationOutput(true, primaryLabel = if (toolId == "invoice-generator") "Invoice Total Due" else "Receipt Total Paid",
                    primaryValue = grand, primaryFormatted = formatMoney(grand, currency),
                    metrics = listOf(MetricCardData("Subtotal", sub, formatMoney(sub, currency)), MetricCardData("Tax", taxAmt, formatMoney(taxAmt, currency))),
                    tableHeaders = listOf("Item", "Qty", "Price", "Total"), tableRows = tRows)
            }
            "business-expense-tracker" -> {
                val b = parseDouble(inputs["budget"], "Budget").getOrElse { return fail(it.message!!) }
                val tx = parseDouble(inputs["corpTaxPct"], "Tax %", max = 60.0).getOrElse { return fail(it.message!!) }
                if (rows.isEmpty()) return fail("Add at least one expense.")
                var total = 0.0
                for (r in rows) total += parseDouble(r.amount, r.label).getOrElse { return fail(it.message!!) }
                CalculationOutput(true, primaryLabel = "Total Business Expenses", primaryValue = total, primaryFormatted = formatMoney(total, currency),
                    metrics = listOf(MetricCardData("Budget Variance", b - total, formatMoney(b - total, currency)), MetricCardData("Tax Shield", total * tx / 100.0, formatMoney(total * tx / 100.0, currency))))
            }
            "revenue-calculator" -> {
                val u = parseDouble(inputs["units"], "Units").getOrElse { return fail(it.message!!) }
                val p = parseDouble(inputs["price"], "Price").getOrElse { return fail(it.message!!) }
                val ret = parseDouble(inputs["returnsPct"], "Returns %", max = 100.0).getOrElse { return fail(it.message!!) }
                val disc = parseDouble(inputs["discountPct"], "Discount %", max = 100.0).getOrElse { return fail(it.message!!) }
                val gross = u * p
                val net = gross * (1.0 - disc / 100.0) * (1.0 - ret / 100.0)
                CalculationOutput(true, primaryLabel = "Net Revenue", primaryValue = net, primaryFormatted = formatMoney(net, currency),
                    metrics = listOf(MetricCardData("Gross Revenue", gross, formatMoney(gross, currency))))
            }
            "cash-flow-forecast" -> {
                val open = parseDouble(inputs["openingCash"], "Starting Cash").getOrElse { return fail(it.message!!) }
                var inf = parseDouble(inputs["monthlyInflow"], "Inflow").getOrElse { return fail(it.message!!) }
                val g = parseDouble(inputs["inflowGrowth"], "Growth %", allowNegative = true, min = -50.0, max = 100.0).getOrElse { return fail(it.message!!) }
                val out = parseDouble(inputs["monthlyOutflow"], "Outflow").getOrElse { return fail(it.message!!) }
                val mo = parseDouble(inputs["months"], "Months", allowZero = false, min = 1.0, max = 36.0).getOrElse { return fail(it.message!!) }.toInt()
                var bal = open
                val tRows = mutableListOf<List<String>>()
                for (m in 1..mo) {
                    val net = inf - out
                    bal += net
                    tRows.add(listOf("Month $m", formatMoney(inf, currency), formatMoney(out, currency), formatMoney(bal, currency)))
                    inf *= (1.0 + g / 100.0)
                }
                CalculationOutput(true, primaryLabel = "Ending Cash (Month $mo)", primaryValue = bal, primaryFormatted = formatMoney(bal, currency),
                    tableHeaders = listOf("Month", "Inflow", "Outflow", "Ending Cash"), tableRows = tRows)
            }
            "payroll-calculator" -> {
                val g = parseDouble(inputs["grossPay"], "Gross Pay", allowZero = false).getOrElse { return fail(it.message!!) }
                val et = parseDouble(inputs["empTaxPct"], "EE Tax %", max = 60.0).getOrElse { return fail(it.message!!) }
                val ed = parseDouble(inputs["empBenefits"], "EE Deductions").getOrElse { return fail(it.message!!) }
                val rt = parseDouble(inputs["employerTaxPct"], "ER Tax %", max = 40.0).getOrElse { return fail(it.message!!) }
                val ec = parseDouble(inputs["employerContrib"], "ER Benefits").getOrElse { return fail(it.message!!) }
                val netPay = max(0.0, g * (1.0 - et / 100.0) - ed)
                val erCost = g * (1.0 + rt / 100.0) + ec
                CalculationOutput(true, primaryLabel = "Total Employer Cost", primaryValue = erCost, primaryFormatted = formatMoney(erCost, currency),
                    metrics = listOf(MetricCardData("Employee Net Pay", netPay, formatMoney(netPay, currency))))
            }
            "depreciation-calculator" -> {
                val c = parseDouble(inputs["cost"], "Cost", allowZero = false).getOrElse { return fail(it.message!!) }
                val s = parseDouble(inputs["salvage"], "Salvage").getOrElse { return fail(it.message!!) }
                val l = parseDouble(inputs["life"], "Useful Life", allowZero = false, min = 1.0, max = 50.0).getOrElse { return fail(it.message!!) }.toInt()
                if (s >= c) return fail("Salvage must be less than cost.")
                val method = inputs["method"] ?: "straight"
                var book = c; var accum = 0.0
                val tRows = mutableListOf<List<String>>()
                var yr1Dep = 0.0
                for (yr in 1..l) {
                    val dep = if (method == "straight") (c - s) / l else if (yr == l || book - book * (2.0 / l) < s) max(0.0, book - s) else book * (2.0 / l)
                    if (yr == 1) yr1Dep = dep
                    accum += dep
                    book = max(s, book - dep)
                    tRows.add(listOf("Year $yr", formatMoney(dep, currency), formatMoney(accum, currency), formatMoney(book, currency)))
                }
                CalculationOutput(true, primaryLabel = "Year 1 Depreciation", primaryValue = yr1Dep, primaryFormatted = formatMoney(yr1Dep, currency),
                    tableHeaders = listOf("Year", "Depreciation", "Accumulated", "Book Value"), tableRows = tRows)
            }
            else -> null
        }
    }
}
