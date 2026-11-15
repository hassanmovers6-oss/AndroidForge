package com.educationwithhassan.financetools.domain.engine

import com.educationwithhassan.financetools.domain.engine.EngineMath.fail
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatMoney
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatNumber
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatPercent
import com.educationwithhassan.financetools.domain.engine.EngineMath.parseDouble
import com.educationwithhassan.financetools.domain.model.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object IslamicCryptoAdvancedEngine {
    fun calculate(toolId: String, inputs: Map<String, String>, rows: List<DynamicRow>, currency: String): CalculationOutput? {
        return when (toolId) {
            "islamic-zakat-calculator" -> {
                val c = parseDouble(inputs["cash"], "Cash").getOrElse { return fail(it.message!!) }
                val g = parseDouble(inputs["goldValue"], "Gold").getOrElse { return fail(it.message!!) }
                val s = parseDouble(inputs["silverValue"], "Silver").getOrElse { return fail(it.message!!) }
                val inv = parseDouble(inputs["investments"], "Investments").getOrElse { return fail(it.message!!) }
                val bs = parseDouble(inputs["businessStock"], "Stock").getOrElse { return fail(it.message!!) }
                val d = parseDouble(inputs["debts"], "Debts").getOrElse { return fail(it.message!!) }
                val nis = parseDouble(inputs["nisabValue"], "Nisab").getOrElse { return fail(it.message!!) }
                val net = max(0.0, c + g + s + inv + bs - d)
                val zakat = if (net >= nis && net > 0) net * 0.025 else 0.0
                CalculationOutput(true, primaryLabel = "Total Zakat Due (2.5%)", primaryValue = zakat, primaryFormatted = formatMoney(zakat, currency),
                    metrics = listOf(MetricCardData("Net Zakatable Wealth", net, formatMoney(net, currency))),
                    notes = listOf("Jurisprudence schools differ on Nisab benchmark (gold vs. silver) and personal jewelry exemptions."))
            }
            "zakat-on-gold-calculator" -> {
                val w = parseDouble(inputs["weight"], "Weight").getOrElse { return fail(it.message!!) }
                val p = parseDouble(inputs["pricePerGram24k"], "24K Price/g", allowZero = false).getOrElse { return fail(it.message!!) }
                val nis = parseDouble(inputs["nisabGrams"], "Nisab Grams", allowZero = false).getOrElse { return fail(it.message!!) }
                val unitFactor = when (inputs["unit"]) { "tola" -> 11.6638; "oz" -> 31.1035; else -> 1.0 }
                val karat = inputs["karat"]?.toDoubleOrNull() ?: 24.0
                val pureGrams = w * unitFactor * (karat / 24.0)
                val valTotal = pureGrams * p
                val zakat = if (pureGrams >= nis) valTotal * 0.025 else 0.0
                CalculationOutput(true, primaryLabel = "Zakat on Gold", primaryValue = zakat, primaryFormatted = formatMoney(zakat, currency),
                    metrics = listOf(MetricCardData("Pure 24K Grams", pureGrams, "${formatNumber(pureGrams, 2)} g"), MetricCardData("Gold Value", valTotal, formatMoney(valTotal, currency))))
            }
            "zakat-on-silver-calculator" -> {
                val w = parseDouble(inputs["weight"], "Weight").getOrElse { return fail(it.message!!) }
                val pur = parseDouble(inputs["purityPct"], "Purity %", min = 1.0, max = 100.0).getOrElse { return fail(it.message!!) }
                val p = parseDouble(inputs["pricePerGram"], "Price/g", allowZero = false).getOrElse { return fail(it.message!!) }
                val nis = parseDouble(inputs["nisabGrams"], "Nisab Grams", allowZero = false).getOrElse { return fail(it.message!!) }
                val pure = w * (pur / 100.0)
                val valTotal = pure * p
                val zakat = if (pure >= nis) valTotal * 0.025 else 0.0
                CalculationOutput(true, primaryLabel = "Zakat on Silver", primaryValue = zakat, primaryFormatted = formatMoney(zakat, currency),
                    metrics = listOf(MetricCardData("Pure Silver", pure, "${formatNumber(pure, 2)} g")))
            }
            "zakat-on-cash-calculator" -> {
                val ch = parseDouble(inputs["cashHand"], "Cash").getOrElse { return fail(it.message!!) }
                val bk = parseDouble(inputs["bankSavings"], "Bank").getOrElse { return fail(it.message!!) }
                val rec = parseDouble(inputs["receivables"], "Receivables").getOrElse { return fail(it.message!!) }
                val d = parseDouble(inputs["debtsDue"], "Debts").getOrElse { return fail(it.message!!) }
                val nis = parseDouble(inputs["nisab"], "Nisab").getOrElse { return fail(it.message!!) }
                val net = max(0.0, ch + bk + rec - d)
                val zakat = if (net >= nis && net > 0) net * 0.025 else 0.0
                CalculationOutput(true, primaryLabel = "Zakat on Cash", primaryValue = zakat, primaryFormatted = formatMoney(zakat, currency),
                    metrics = listOf(MetricCardData("Net Cash", net, formatMoney(net, currency))))
            }
            "zakat-on-business-assets-calculator" -> {
                val c = parseDouble(inputs["bizCash"], "Cash").getOrElse { return fail(it.message!!) }
                val inv = parseDouble(inputs["inventory"], "Inventory").getOrElse { return fail(it.message!!) }
                val rec = parseDouble(inputs["receivables"], "Receivables").getOrElse { return fail(it.message!!) }
                val pay = parseDouble(inputs["payables"], "Payables").getOrElse { return fail(it.message!!) }
                val nis = parseDouble(inputs["nisab"], "Nisab").getOrElse { return fail(it.message!!) }
                val net = max(0.0, c + inv + rec - pay)
                val zakat = if (net >= nis && net > 0) net * 0.025 else 0.0
                CalculationOutput(true, primaryLabel = "Business Zakat", primaryValue = zakat, primaryFormatted = formatMoney(zakat, currency),
                    metrics = listOf(MetricCardData("Working Capital", net, formatMoney(net, currency))))
            }
            "islamic-inheritance-calculator" -> {
                val ge = parseDouble(inputs["grossEstate"], "Gross Estate", allowZero = false).getOrElse { return fail(it.message!!) }
                val df = parseDouble(inputs["debtsFuneral"], "Debts & Funeral").getOrElse { return fail(it.message!!) }
                val was = parseDouble(inputs["wasiyyah"], "Wasiyyah").getOrElse { return fail(it.message!!) }
                val sons = parseDouble(inputs["sons"], "Sons", max = 20.0).getOrElse { return fail(it.message!!) }.toInt()
                val daughters = parseDouble(inputs["daughters"], "Daughters", max = 20.0).getOrElse { return fail(it.message!!) }.toInt()
                val afterDebts = max(0.0, ge - df)
                val netEstate = max(0.0, afterDebts - min(was, afterDebts / 3.0))
                val hasChildren = sons + daughters > 0
                val father = inputs["fatherAlive"] == "yes"
                val mother = inputs["motherAlive"] == "yes"
                val spouse = inputs["spouse"] ?: "none"

                val shares = mutableListOf<Triple<String, Double, String>>()
                var fixedSum = 0.0
                if (spouse == "wife") {
                    val f = if (hasChildren) 1.0 / 8.0 else 1.0 / 4.0
                    shares.add(Triple("Wife", f, if (hasChildren) "1/8" else "1/4"))
                    fixedSum += f
                } else if (spouse == "husband") {
                    val f = if (hasChildren) 1.0 / 4.0 else 1.0 / 2.0
                    shares.add(Triple("Husband", f, if (hasChildren) "1/4" else "1/2"))
                    fixedSum += f
                }
                if (mother) {
                    val f = if (hasChildren) 1.0 / 6.0 else 1.0 / 3.0
                    shares.add(Triple("Mother", f, if (hasChildren) "1/6" else "1/3"))
                    fixedSum += f
                }
                if (father && hasChildren) {
                    shares.add(Triple("Father", 1.0 / 6.0, "1/6"))
                    fixedSum += 1.0 / 6.0
                }
                if (sons == 0 && daughters > 0) {
                    val f = if (daughters == 1) 0.5 else 2.0 / 3.0
                    shares.add(Triple("Daughters ($daughters)", f, if (daughters == 1) "1/2" else "2/3"))
                    fixedSum += f
                }
                if (fixedSum > 1.0) {
                    for (i in shares.indices) shares[i] = shares[i].copy(second = shares[i].second / fixedSum)
                    fixedSum = 1.0
                }
                val residue = max(0.0, 1.0 - fixedSum)
                if (residue > 0.0001) {
                    if (sons > 0) {
                        val units = sons * 2 + daughters
                        shares.add(Triple("Sons ($sons)", residue * (sons * 2.0 / units), "Asabah 2:1"))
                        if (daughters > 0) shares.add(Triple("Daughters ($daughters)", residue * (daughters.toDouble() / units), "Asabah 2:1"))
                    } else if (father) {
                        shares.add(Triple("Father (Residue)", residue, "Asabah"))
                    }
                }
                val tRows = shares.map { (h, f, r) -> listOf(h, r, formatPercent(f * 100.0), formatMoney(netEstate * f, currency)) }
                CalculationOutput(true, primaryLabel = "Net Distributable Estate", primaryValue = netEstate, primaryFormatted = formatMoney(netEstate, currency),
                    tableHeaders = listOf("Heir", "Rule", "Share %", "Amount"), tableRows = tRows,
                    notes = listOf("Educational estimator for primary heirs. Complex cases and school-specific differences require a qualified Islamic inheritance scholar."))
            }
            "islamic-profit-sharing-calculator" -> {
                val capA = parseDouble(inputs["partnerACapital"], "Partner A Capital").getOrElse { return fail(it.message!!) }
                val capB = parseDouble(inputs["partnerBCapital"], "Partner B Capital").getOrElse { return fail(it.message!!) }
                val out = parseDouble(inputs["netOutcome"], "Net Outcome", allowNegative = true).getOrElse { return fail(it.message!!) }
                val pctA = parseDouble(inputs["partnerAProfitPct"], "Partner A Share %", max = 100.0).getOrElse { return fail(it.message!!) }
                val tot = capA + capB
                if (tot <= 0.0) return fail("Total capital must be positive.")
                val shareA = if (out >= 0) out * pctA / 100.0 else out * (capA / tot)
                val shareB = out - shareA
                CalculationOutput(true, primaryLabel = "Partner A Share", primaryValue = shareA, primaryFormatted = formatMoney(shareA, currency),
                    metrics = listOf(MetricCardData("Partner B Share", shareB, formatMoney(shareB, currency))))
            }
            "mahr-calculator" -> {
                val pr = parseDouble(inputs["promptMahr"], "Prompt Mahr").getOrElse { return fail(it.message!!) }
                val df = parseDouble(inputs["deferredMahr"], "Deferred Mahr").getOrElse { return fail(it.message!!) }
                val gv = parseDouble(inputs["goldValueIncluded"], "Gold Value").getOrElse { return fail(it.message!!) }
                val gp = parseDouble(inputs["goldPriceGram"], "Gold Price/g", allowZero = false).getOrElse { return fail(it.message!!) }
                val tot = pr + df + gv
                CalculationOutput(true, primaryLabel = "Total Agreed Mahr", primaryValue = tot, primaryFormatted = formatMoney(tot, currency),
                    metrics = listOf(MetricCardData("24K Gold Equivalent", tot / gp, "${formatNumber(tot / gp, 2)} g")))
            }
            "islamic-savings-planner" -> {
                val init = parseDouble(inputs["initial"], "Initial").getOrElse { return fail(it.message!!) }
                val mo = parseDouble(inputs["monthly"], "Monthly").getOrElse { return fail(it.message!!) }
                val pr = parseDouble(inputs["profitRate"], "Rate %", max = 40.0).getOrElse { return fail(it.message!!) }
                val yrs = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }.toInt()
                val nis = parseDouble(inputs["nisab"], "Nisab").getOrElse { return fail(it.message!!) }
                var bal = init; var zakatPaid = 0.0
                for (y in 1..yrs) {
                    for (m in 1..12) bal = bal * (1.0 + pr / 100.0 / 12.0) + mo
                    if (bal >= nis) { val z = bal * 0.025; zakatPaid += z; bal -= z }
                }
                CalculationOutput(true, primaryLabel = "Net Halal Savings", primaryValue = bal, primaryFormatted = formatMoney(bal, currency),
                    metrics = listOf(MetricCardData("Cumulative Zakat Paid", zakatPaid, formatMoney(zakatPaid, currency))))
            }
            "islamic-charity-calculator" -> {
                val ms = parseDouble(inputs["monthlySadaqah"], "Monthly Sadaqah").getOrElse { return fail(it.message!!) }
                val az = parseDouble(inputs["annualZakat"], "Annual Zakat").getOrElse { return fail(it.message!!) }
                val fm = parseDouble(inputs["familyMembers"], "Members").getOrElse { return fail(it.message!!) }
                val fr = parseDouble(inputs["fitrRate"], "Fitr Rate").getOrElse { return fail(it.message!!) }
                val wq = parseDouble(inputs["waqf"], "Waqf").getOrElse { return fail(it.message!!) }
                val tot = ms * 12.0 + az + fm.toInt() * fr + wq
                CalculationOutput(true, primaryLabel = "Total Annual Charity", primaryValue = tot, primaryFormatted = formatMoney(tot, currency),
                    metrics = listOf(MetricCardData("Monthly Set-Aside", tot / 12.0, formatMoney(tot / 12.0, currency))))
            }
            "crypto-profit-calculator" -> {
                val inv = parseDouble(inputs["investment"], "Investment", allowZero = false).getOrElse { return fail(it.message!!) }
                val bp = parseDouble(inputs["buyPrice"], "Entry Price", allowZero = false).getOrElse { return fail(it.message!!) }
                val sp = parseDouble(inputs["sellPrice"], "Exit Price").getOrElse { return fail(it.message!!) }
                val bf = parseDouble(inputs["buyFeePct"], "Buy Fee %", max = 10.0).getOrElse { return fail(it.message!!) }
                val sf = parseDouble(inputs["sellFeePct"], "Sell Fee %", max = 10.0).getOrElse { return fail(it.message!!) }
                val coins = inv * (1.0 - bf / 100.0) / bp
                val netExit = coins * sp * (1.0 - sf / 100.0)
                val profit = netExit - inv
                CalculationOutput(true, primaryLabel = if (profit >= 0) "Net Crypto Profit" else "Net Crypto Loss", primaryValue = profit, primaryFormatted = formatMoney(profit, currency),
                    metrics = listOf(MetricCardData("ROI", (profit / inv) * 100.0, formatPercent((profit / inv) * 100.0)), MetricCardData("Coins", coins, formatNumber(coins, 6))))
            }
            "crypto-investment-calculator" -> {
                val init = parseDouble(inputs["initial"], "Initial").getOrElse { return fail(it.message!!) }
                val dca = parseDouble(inputs["monthlyDca"], "Monthly DCA").getOrElse { return fail(it.message!!) }
                val rate = parseDouble(inputs["assumedAnnualPct"], "Growth %", allowNegative = true, min = -90.0, max = 200.0).getOrElse { return fail(it.message!!) }
                val mo = parseDouble(inputs["months"], "Months", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }.toInt()
                val mRate = (1.0 + rate / 100.0).pow(1.0 / 12.0) - 1.0
                var bal = init; var inv = init
                for (m in 1..mo) { bal = bal * (1.0 + mRate) + dca; inv += dca }
                CalculationOutput(true, primaryLabel = "Hypothetical Ending Value", primaryValue = bal, primaryFormatted = formatMoney(bal, currency),
                    metrics = listOf(MetricCardData("Invested", inv, formatMoney(inv, currency))))
            }
            "bitcoin-profit-calculator" -> {
                val inv = parseDouble(inputs["investment"], "Investment", allowZero = false).getOrElse { return fail(it.message!!) }
                val bp = parseDouble(inputs["btcBuyPrice"], "Buy Price", allowZero = false).getOrElse { return fail(it.message!!) }
                val sp = parseDouble(inputs["btcSellPrice"], "Sell Price").getOrElse { return fail(it.message!!) }
                val fees = parseDouble(inputs["totalFees"], "Fees").getOrElse { return fail(it.message!!) }
                val btc = inv / bp
                val profit = btc * sp - inv - fees
                CalculationOutput(true, primaryLabel = "Net Hypothetical BTC Profit", primaryValue = profit, primaryFormatted = formatMoney(profit, currency),
                    metrics = listOf(MetricCardData("BTC Acquired", btc, "${formatNumber(btc, 6)} BTC")))
            }
            "crypto-average-price-calculator" -> {
                if (rows.isEmpty()) return fail("Add at least one purchase lot.")
                var coins = 0.0; var spent = 0.0
                for (r in rows) {
                    val p = parseDouble(r.amount, r.label).getOrElse { return fail(it.message!!) }
                    val q = parseDouble(r.quantity, "Quantity", allowZero = false).getOrElse { return fail(it.message!!) }
                    coins += q; spent += p * q
                }
                val avg = spent / coins
                CalculationOutput(true, primaryLabel = "Weighted Average Price", primaryValue = avg, primaryFormatted = formatMoney(avg, currency),
                    metrics = listOf(MetricCardData("Total Coins", coins, formatNumber(coins, 6)), MetricCardData("Total Spent", spent, formatMoney(spent, currency))))
            }
            "crypto-converter" -> {
                val sym = inputs["coinSymbol"]?.trim()?.uppercase().orEmpty().ifEmpty { "COIN" }
                val price = parseDouble(inputs["coinPrice"], "Coin Price", allowZero = false).getOrElse { return fail(it.message!!) }
                val valIn = parseDouble(inputs["inputVal"], "Amount").getOrElse { return fail(it.message!!) }
                val f2c = (inputs["direction"] ?: "fiat_to_coin") == "fiat_to_coin"
                if (f2c) {
                    val c = valIn / price
                    CalculationOutput(true, primaryLabel = "Equivalent $sym", primaryValue = c, primaryFormatted = "${formatNumber(c, 6)} $sym")
                } else {
                    val f = valIn * price
                    CalculationOutput(true, primaryLabel = "Fiat Value ($currency)", primaryValue = f, primaryFormatted = formatMoney(f, currency))
                }
            }
            "crypto-portfolio-tracker" -> {
                if (rows.isEmpty()) return fail("Add at least one crypto holding.")
                var totVal = 0.0; var totCost = 0.0
                for (r in rows) {
                    val p = parseDouble(r.amount, r.label).getOrElse { return fail(it.message!!) }
                    val q = parseDouble(r.quantity, "Qty").getOrElse { return fail(it.message!!) }
                    val c = parseDouble(r.extra.ifBlank { "0" }, "Cost Basis").getOrElse { return fail(it.message!!) }
                    totVal += p * q; totCost += c
                }
                CalculationOutput(true, primaryLabel = "Crypto Portfolio Value", primaryValue = totVal, primaryFormatted = formatMoney(totVal, currency),
                    metrics = listOf(MetricCardData("Unrealized P&L", totVal - totCost, formatMoney(totVal - totCost, currency))))
            }
            "mining-profitability-calculator" -> {
                val rew = parseDouble(inputs["dailyGrossReward"], "Daily Reward").getOrElse { return fail(it.message!!) }
                val watts = parseDouble(inputs["powerWatts"], "Watts", allowZero = false).getOrElse { return fail(it.message!!) }
                val kwh = parseDouble(inputs["kwhCost"], "Electricity Cost").getOrElse { return fail(it.message!!) }
                val pool = parseDouble(inputs["poolFeePct"], "Pool Fee %", max = 20.0).getOrElse { return fail(it.message!!) }
                val dailyPower = (watts / 1000.0) * 24.0 * kwh
                val dailyNet = rew * (1.0 - pool / 100.0) - dailyPower
                CalculationOutput(true, primaryLabel = "Monthly Net Mining Profit", primaryValue = dailyNet * 30.0, primaryFormatted = formatMoney(dailyNet * 30.0, currency),
                    metrics = listOf(MetricCardData("Daily Net", dailyNet, formatMoney(dailyNet, currency))))
            }
            "crypto-roi-calculator" -> {
                val init = parseDouble(inputs["initialCost"], "Initial Cost", allowZero = false).getOrElse { return fail(it.message!!) }
                val cur = parseDouble(inputs["currentValue"], "Current Value").getOrElse { return fail(it.message!!) }
                val mo = parseDouble(inputs["monthsHeld"], "Months", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val roi = ((cur - init) / init) * 100.0
                val ann = if (cur > 0) ((cur / init).pow(12.0 / mo) - 1.0) * 100.0 else -100.0
                CalculationOutput(true, primaryLabel = "Crypto ROI", primaryValue = roi, primaryFormatted = formatPercent(roi),
                    metrics = listOf(MetricCardData("Multiplier", cur / init, "${formatNumber(cur / init, 2)}x"), MetricCardData("Annualized ROI", ann, formatPercent(ann))))
            }
            "retirement-calculator" -> {
                val ca = parseDouble(inputs["currentAge"], "Current Age", min = 18.0, max = 85.0).getOrElse { return fail(it.message!!) }
                val ra = parseDouble(inputs["retireAge"], "Retirement Age", min = 20.0, max = 95.0).getOrElse { return fail(it.message!!) }
                if (ra <= ca) return fail("Retirement age must exceed current age.")
                val cs = parseDouble(inputs["currentSavings"], "Current Savings").getOrElse { return fail(it.message!!) }
                val ms = parseDouble(inputs["monthlySave"], "Monthly Save").getOrElse { return fail(it.message!!) }
                val er = parseDouble(inputs["expectedReturn"], "Return %", max = 30.0).getOrElse { return fail(it.message!!) }
                val inf = parseDouble(inputs["inflation"], "Inflation %", max = 25.0).getOrElse { return fail(it.message!!) }
                val wr = parseDouble(inputs["withdrawalPct"], "Withdrawal %", allowZero = false, min = 1.0, max = 15.0).getOrElse { return fail(it.message!!) }
                val yrs = ra - ca
                val (bal, _) = PersonalBankingEngine.computeCompound(cs, er, yrs, 12, ms)
                val real = bal / (1.0 + inf / 100.0).pow(yrs)
                CalculationOutput(true, primaryLabel = "Projected Retirement Corpus", primaryValue = bal, primaryFormatted = formatMoney(bal, currency),
                    metrics = listOf(MetricCardData("Today's Purchasing Power", real, formatMoney(real, currency)), MetricCardData("Est. Monthly Income", (real * wr / 100.0) / 12.0, formatMoney((real * wr / 100.0) / 12.0, currency))))
            }
            "pension-calculator" -> {
                val sal = parseDouble(inputs["finalSalary"], "Final Salary", allowZero = false).getOrElse { return fail(it.message!!) }
                val sy = parseDouble(inputs["serviceYears"], "Service Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val mult = parseDouble(inputs["multiplierPct"], "Accrual %", allowZero = false, min = 0.1, max = 10.0).getOrElse { return fail(it.message!!) }
                val repl = min(100.0, sy * mult)
                val ann = sal * repl / 100.0
                CalculationOutput(true, primaryLabel = "Starting Monthly Pension", primaryValue = ann / 12.0, primaryFormatted = formatMoney(ann / 12.0, currency),
                    metrics = listOf(MetricCardData("Annual Pension", ann, formatMoney(ann, currency)), MetricCardData("Replacement", repl, formatPercent(repl))))
            }
            "inflation-adjusted-return-calculator" -> {
                val p = parseDouble(inputs["principal"], "Principal", allowZero = false).getOrElse { return fail(it.message!!) }
                val nom = parseDouble(inputs["nominalReturn"], "Nominal %", allowNegative = true, min = -50.0, max = 100.0).getOrElse { return fail(it.message!!) }
                val inf = parseDouble(inputs["inflationRate"], "Inflation %", max = 50.0).getOrElse { return fail(it.message!!) }
                val yrs = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val realRate = (((1.0 + nom / 100.0) / (1.0 + inf / 100.0)) - 1.0) * 100.0
                val realFv = p * (1.0 + realRate / 100.0).pow(yrs)
                CalculationOutput(true, primaryLabel = "Real Annual Return Rate", primaryValue = realRate, primaryFormatted = formatPercent(realRate),
                    metrics = listOf(MetricCardData("Real Ending Value", realFv, formatMoney(realFv, currency))))
            }
            "future-value-calculator" -> {
                val pv = parseDouble(inputs["pv"], "PV").getOrElse { return fail(it.message!!) }
                val pmt = parseDouble(inputs["pmt"], "PMT").getOrElse { return fail(it.message!!) }
                val rPct = parseDouble(inputs["ratePerPeriod"], "Rate %", max = 100.0).getOrElse { return fail(it.message!!) }
                val n = parseDouble(inputs["periods"], "Periods", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val r = rPct / 100.0
                val fv = pv * (1.0 + r).pow(n) + (if (r == 0.0) pmt * n else pmt * (((1.0 + r).pow(n) - 1.0) / r))
                CalculationOutput(true, primaryLabel = "Future Value (FV)", primaryValue = fv, primaryFormatted = formatMoney(fv, currency))
            }
            "present-value-calculator" -> {
                val fv = parseDouble(inputs["fv"], "FV").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["discountRate"], "Discount Rate %", max = 100.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 0.1).getOrElse { return fail(it.message!!) }
                val pv = fv / (1.0 + r / 100.0).pow(y)
                CalculationOutput(true, primaryLabel = "Present Value (PV)", primaryValue = pv, primaryFormatted = formatMoney(pv, currency))
            }
            "annuity-calculator" -> {
                val pv = parseDouble(inputs["corpus"], "Principal", allowZero = false).getOrElse { return fail(it.message!!) }
                val rPct = parseDouble(inputs["rate"], "Rate %", max = 40.0).getOrElse { return fail(it.message!!) }
                val yrs = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val n = (yrs * 12.0).toInt()
                val r = rPct / 100.0 / 12.0
                var pmt = if (r == 0.0) pv / n else (pv * r) / (1.0 - (1.0 + r).pow(-n))
                if (inputs["timing"] == "start" && r > 0) pmt /= (1.0 + r)
                CalculationOutput(true, primaryLabel = "Monthly Annuity Payout", primaryValue = pmt, primaryFormatted = formatMoney(pmt, currency),
                    metrics = listOf(MetricCardData("Total Payouts", pmt * n, formatMoney(pmt * n, currency))))
            }
            "rule-of-72-calculator" -> {
                val r = parseDouble(inputs["rate"], "Annual Rate %", allowZero = false, min = 0.1, max = 100.0).getOrElse { return fail(it.message!!) }
                val p = parseDouble(inputs["principal"], "Principal").getOrElse { return fail(it.message!!) }
                val yrs = 72.0 / r
                val exact = ln(2.0) / ln(1.0 + r / 100.0)
                CalculationOutput(true, primaryLabel = "Doubling Time (Rule of 72)", primaryValue = yrs, primaryFormatted = "${formatNumber(yrs, 2)} years",
                    metrics = listOf(MetricCardData("Exact Years", exact, "${formatNumber(exact, 2)} years"), MetricCardData("Doubled Value", p * 2.0, formatMoney(p * 2.0, currency))))
            }
            "financial-independence-calculator" -> {
                val exp = parseDouble(inputs["annualExpenses"], "Annual Expenses", allowZero = false).getOrElse { return fail(it.message!!) }
                val swr = parseDouble(inputs["swrPct"], "Withdrawal %", allowZero = false, min = 1.0, max = 10.0).getOrElse { return fail(it.message!!) }
                val cur = parseDouble(inputs["currentNetWorth"], "Current Net Worth").getOrElse { return fail(it.message!!) }
                val mo = parseDouble(inputs["monthlyInvest"], "Monthly Savings").getOrElse { return fail(it.message!!) }
                val ret = parseDouble(inputs["realReturnPct"], "Real Return %", max = 25.0).getOrElse { return fail(it.message!!) }
                val target = exp / (swr / 100.0)
                var bal = cur; var m = 0; val rMo = ret / 100.0 / 12.0
                while (bal < target && m < 960) { m++; bal = bal * (1.0 + rMo) + mo }
                CalculationOutput(true, primaryLabel = "FIRE Target Number", primaryValue = target, primaryFormatted = formatMoney(target, currency),
                    metrics = listOf(MetricCardData("Years to FI", m / 12.0, if (bal >= target) "${formatNumber(m / 12.0, 1)} yrs" else "> 80 yrs")))
            }
            "early-retirement-calculator" -> {
                val ca = parseDouble(inputs["currentAge"], "Current Age", min = 18.0, max = 75.0).getOrElse { return fail(it.message!!) }
                val ra = parseDouble(inputs["targetRetireAge"], "Target Age", min = 25.0, max = 80.0).getOrElse { return fail(it.message!!) }
                if (ra <= ca) return fail("Target age must exceed current age.")
                val cp = parseDouble(inputs["currentPortfolio"], "Current Portfolio").getOrElse { return fail(it.message!!) }
                val sp = parseDouble(inputs["desiredAnnualSpend"], "Desired Spend", allowZero = false).getOrElse { return fail(it.message!!) }
                val rPct = parseDouble(inputs["annualReturn"], "Return %", max = 25.0).getOrElse { return fail(it.message!!) }
                val target = sp / 0.035
                val n = ((ra - ca) * 12).toInt()
                val r = rPct / 100.0 / 12.0
                val gap = max(0.0, target - cp * (1.0 + r).pow(n))
                val reqMo = if (gap == 0.0) 0.0 else if (r == 0.0) gap / n else (gap * r) / ((1.0 + r).pow(n) - 1.0)
                CalculationOutput(true, primaryLabel = "Required Monthly Savings", primaryValue = reqMo, primaryFormatted = formatMoney(reqMo, currency),
                    metrics = listOf(MetricCardData("Target Corpus", target, formatMoney(target, currency))))
            }
            "opportunity-cost-calculator" -> {
                val ot = parseDouble(inputs["oneTimeSpend"], "One-Time Spend").getOrElse { return fail(it.message!!) }
                val mo = parseDouble(inputs["monthlySpend"], "Monthly Spend").getOrElse { return fail(it.message!!) }
                val ret = parseDouble(inputs["altReturn"], "Alternative Return %", max = 40.0).getOrElse { return fail(it.message!!) }
                val yrs = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val n = (yrs * 12).toInt(); val r = ret / 100.0 / 12.0
                val fv = ot * (1.0 + r).pow(n) + (if (r == 0.0) mo * n else mo * (((1.0 + r).pow(n) - 1.0) / r))
                CalculationOutput(true, primaryLabel = "Future Opportunity Cost", primaryValue = fv, primaryFormatted = formatMoney(fv, currency))
            }
            "real-interest-rate-calculator" -> {
                val n = parseDouble(inputs["nominalRate"], "Nominal %", allowNegative = true, min = -20.0, max = 100.0).getOrElse { return fail(it.message!!) }
                val i = parseDouble(inputs["inflationRate"], "Inflation %", allowNegative = true, min = -10.0, max = 100.0).getOrElse { return fail(it.message!!) }
                if (1.0 + i / 100.0 <= 0.0) return fail("Inflation cannot be -100% or lower.")
                val exact = (((1.0 + n / 100.0) / (1.0 + i / 100.0)) - 1.0) * 100.0
                CalculationOutput(true, primaryLabel = "Exact Real Interest Rate", primaryValue = exact, primaryFormatted = formatPercent(exact),
                    metrics = listOf(MetricCardData("Approx (Nom - Inf)", n - i, formatPercent(n - i))))
            }
            "loan-vs-investment-calculator" -> {
                val bal = parseDouble(inputs["loanBalance"], "Loan Balance", allowZero = false).getOrElse { return fail(it.message!!) }
                val lr = parseDouble(inputs["loanRate"], "Loan Rate %", max = 40.0).getOrElse { return fail(it.message!!) }
                val mo = parseDouble(inputs["remainingMonths"], "Months", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }.toInt()
                val ex = parseDouble(inputs["extraMonthly"], "Extra Monthly", allowZero = false).getOrElse { return fail(it.message!!) }
                val ir = parseDouble(inputs["investReturn"], "Invest Return %", max = 40.0).getOrElse { return fail(it.message!!) }
                val itx = parseDouble(inputs["investTaxPct"], "Invest Tax %", max = 60.0).getOrElse { return fail(it.message!!) }
                val (emi, baseInt, _) = PersonalBankingEngine.computeLoan(bal, lr, mo, currency)
                val rLoan = lr / 100.0 / 12.0
                val rInv = (ir * (1.0 - itx / 100.0)) / 100.0 / 12.0
                var b2 = bal; var accInt = 0.0; var postPayoff = 0.0; var investPath = 0.0
                for (m in 1..mo) {
                    if (b2 > 0.01) {
                        val i = b2 * rLoan; accInt += i
                        b2 = max(0.0, b2 + i - min(b2 + i, emi + ex))
                    } else postPayoff = postPayoff * (1.0 + rInv) + (emi + ex)
                    investPath = investPath * (1.0 + rInv) + ex
                }
                val adv = investPath - postPayoff
                CalculationOutput(true, primaryLabel = if (adv >= 0) "Investing Leads By" else "Early Loan Payoff Leads By", primaryValue = abs(adv), primaryFormatted = formatMoney(abs(adv), currency),
                    metrics = listOf(MetricCardData("Interest Saved", max(0.0, baseInt - accInt), formatMoney(max(0.0, baseInt - accInt), currency))))
            }
            "recurring-deposit-calculator" -> {
                val p = parseDouble(inputs["monthlyDeposit"], "Monthly Deposit", allowZero = false).getOrElse { return fail(it.message!!) }
                val rPct = parseDouble(inputs["rate"], "Rate %", max = 40.0).getOrElse { return fail(it.message!!) }
                val m = parseDouble(inputs["months"], "Months", allowZero = false, min = 3.0).getOrElse { return fail(it.message!!) }.toInt()
                val r = rPct / 100.0
                var mat = 0.0
                for (k in 0 until m) mat += p * (1.0 + r / 4.0).pow((m - k) / 3.0)
                CalculationOutput(true, primaryLabel = "RD Maturity Value", primaryValue = mat, primaryFormatted = formatMoney(mat, currency),
                    metrics = listOf(MetricCardData("Deposited", p * m, formatMoney(p * m, currency)), MetricCardData("Interest Earned", mat - p * m, formatMoney(mat - p * m, currency))))
            }
            "financial-forecast-calculator" -> {
                val nw0 = parseDouble(inputs["startNetWorth"], "Start Net Worth", allowNegative = true).getOrElse { return fail(it.message!!) }
                var inc = parseDouble(inputs["annualIncome"], "Annual Income").getOrElse { return fail(it.message!!) }
                val ig = parseDouble(inputs["incomeGrowth"], "Income Growth %", allowNegative = true, min = -20.0, max = 50.0).getOrElse { return fail(it.message!!) }
                var exp = parseDouble(inputs["annualExpense"], "Annual Expense").getOrElse { return fail(it.message!!) }
                val eg = parseDouble(inputs["expenseGrowth"], "Expense Growth %", allowNegative = true, min = -10.0, max = 50.0).getOrElse { return fail(it.message!!) }
                val ret = parseDouble(inputs["portfolioReturn"], "Return %", allowNegative = true, min = -20.0, max = 40.0).getOrElse { return fail(it.message!!) }
                val yrs = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0, max = 40.0).getOrElse { return fail(it.message!!) }.toInt()
                var nw = nw0
                val tRows = mutableListOf<List<String>>()
                for (y in 1..yrs) {
                    val sur = inc - exp
                    nw = nw * (1.0 + ret / 100.0) + sur
                    tRows.add(listOf("Year $y", formatMoney(inc, currency), formatMoney(exp, currency), formatMoney(nw, currency)))
                    inc *= (1.0 + ig / 100.0); exp *= (1.0 + eg / 100.0)
                }
                CalculationOutput(true, primaryLabel = "Projected Year $yrs Net Worth", primaryValue = nw, primaryFormatted = formatMoney(nw, currency),
                    tableHeaders = listOf("Year", "Income", "Expense", "Ending Net Worth"), tableRows = tRows)
            }
            else -> null
        }
    }
}
