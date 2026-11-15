package com.educationwithhassan.financetools.domain.engine

import com.educationwithhassan.financetools.domain.engine.EngineMath.fail
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatMoney
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatNumber
import com.educationwithhassan.financetools.domain.engine.EngineMath.formatPercent
import com.educationwithhassan.financetools.domain.engine.EngineMath.parseDouble
import com.educationwithhassan.financetools.domain.engine.EngineMath.round
import com.educationwithhassan.financetools.domain.model.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object PersonalBankingEngine {

    internal fun computeCompound(
        principal: Double,
        annualRate: Double,
        years: Double,
        compPerYear: Int,
        monthlyContrib: Double = 0.0
    ): Pair<Double, Double> {
        val n = max(1, compPerYear)
        val r = annualRate / 100.0
        if (monthlyContrib == 0.0) {
            val fv = principal * (1.0 + r / n).pow(n * years)
            return fv to principal
        }
        val totalMonths = (years * 12).toInt().coerceAtLeast(1)
        val effMo = if (r == 0.0) 0.0 else (1.0 + r / n).pow(n / 12.0) - 1.0
        var bal = principal
        var contrib = principal
        for (m in 1..totalMonths) {
            bal = bal * (1.0 + effMo) + monthlyContrib
            contrib += monthlyContrib
        }
        return bal to contrib
    }

    internal fun computeLoan(
        principal: Double,
        annualRate: Double,
        months: Int,
        currency: String
    ): Triple<Double, Double, List<List<String>>> {
        val r = annualRate / 100.0 / 12.0
        val emi = when {
            months <= 0 -> principal
            r == 0.0 -> principal / months
            else -> {
                val pow = (1.0 + r).pow(months)
                (principal * r * pow) / (pow - 1.0)
            }
        }
        val totalPayment = emi * months
        val totalInterest = max(0.0, totalPayment - principal)
        val rows = mutableListOf<List<String>>()
        var bal = principal
        val step = if (months > 24) ceil(months / 12.0).toInt() else 1
        for (m in 1..months) {
            val interest = bal * r
            val prinPart = min(bal, emi - interest)
            bal = max(0.0, bal - prinPart)
            if (m <= 12 || m % step == 0 || m == months) {
                rows.add(listOf("Month $m", formatMoney(emi, currency), formatMoney(prinPart, currency), formatMoney(interest, currency), formatMoney(bal, currency)))
            }
        }
        return Triple(emi, totalInterest, rows)
    }

    fun calculate(toolId: String, inputs: Map<String, String>, rows: List<DynamicRow>, currency: String): CalculationOutput? {
        return when (toolId) {
            "personal-budget-planner" -> {
                val inc = parseDouble(inputs["income"], "Income", allowZero = false).getOrElse { return fail(it.message!!) }
                val n = parseDouble(inputs["needsPct"], "Needs %", max = 100.0).getOrElse { return fail(it.message!!) }
                val w = parseDouble(inputs["wantsPct"], "Wants %", max = 100.0).getOrElse { return fail(it.message!!) }
                val s = parseDouble(inputs["savingsPct"], "Savings %", max = 100.0).getOrElse { return fail(it.message!!) }
                if (abs(n + w + s - 100.0) > 0.01) return fail("Percentages must sum to 100%.")
                val needs = inc * n / 100.0
                val wants = inc * w / 100.0
                val savings = inc * s / 100.0
                CalculationOutput(true, primaryLabel = "Monthly Savings Allocation", primaryValue = savings, primaryFormatted = formatMoney(savings, currency),
                    metrics = listOf(MetricCardData("Needs", needs, formatMoney(needs, currency)), MetricCardData("Wants", wants, formatMoney(wants, currency))),
                    breakdown = listOf(BreakdownItem("Annual Savings", formatMoney(savings * 12, currency))),
                    chartSlices = listOf(ChartSlice("Needs", needs), ChartSlice("Wants", wants), ChartSlice("Savings", savings)))
            }
            "monthly-budget-calculator" -> {
                val inc = parseDouble(inputs["income"], "Income").getOrElse { return fail(it.message!!) }
                val keys = listOf("housing" to "Housing", "utilities" to "Utilities", "food" to "Food", "transport" to "Transport", "debt" to "Debt", "other" to "Other")
                var exp = 0.0
                val slices = mutableListOf<ChartSlice>()
                for ((k, lbl) in keys) {
                    val v = parseDouble(inputs[k], lbl, defaultValue = 0.0).getOrElse { return fail(it.message!!) }
                    exp += v
                    slices.add(ChartSlice(lbl, v))
                }
                val surplus = inc - exp
                CalculationOutput(true, primaryLabel = if (surplus >= 0) "Monthly Surplus" else "Monthly Deficit", primaryValue = surplus, primaryFormatted = formatMoney(surplus, currency),
                    metrics = listOf(MetricCardData("Income", inc, formatMoney(inc, currency)), MetricCardData("Expenses", exp, formatMoney(exp, currency))),
                    chartSlices = slices)
            }
            "daily-expense-tracker" -> {
                val limit = parseDouble(inputs["dailyLimit"], "Daily Limit").getOrElse { return fail(it.message!!) }
                val days = parseDouble(inputs["daysInPeriod"], "Days", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                if (rows.isEmpty()) return fail("Add at least one expense entry.")
                var total = 0.0
                val byCat = linkedMapOf<String, Double>()
                for (r in rows) {
                    val v = parseDouble(r.amount, r.label).getOrElse { return fail(it.message!!) }
                    total += v
                    byCat[r.category.ifBlank { "General" }] = (byCat[r.category.ifBlank { "General" }] ?: 0.0) + v
                }
                val rem = limit * days - total
                CalculationOutput(true, primaryLabel = "Total Expenses Logged", primaryValue = total, primaryFormatted = formatMoney(total, currency),
                    metrics = listOf(MetricCardData("Remaining Allowance", rem, formatMoney(rem, currency)), MetricCardData("Daily Average", total / days, formatMoney(total / days, currency))),
                    chartSlices = byCat.map { ChartSlice(it.key, it.value) })
            }
            "income-tracker" -> {
                val taxPct = parseDouble(inputs["taxReservePct"], "Tax %", max = 80.0).getOrElse { return fail(it.message!!) }
                val months = parseDouble(inputs["periodMonths"], "Months", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                if (rows.isEmpty()) return fail("Add at least one income source.")
                var gross = 0.0
                for (r in rows) gross += parseDouble(r.amount, r.label).getOrElse { return fail(it.message!!) }
                val net = gross * (1.0 - taxPct / 100.0)
                CalculationOutput(true, primaryLabel = "Net Income After Tax Reserve", primaryValue = net, primaryFormatted = formatMoney(net, currency),
                    metrics = listOf(MetricCardData("Gross Income", gross, formatMoney(gross, currency)), MetricCardData("Annualized Gross", (gross / months) * 12.0, formatMoney((gross / months) * 12.0, currency))))
            }
            "savings-calculator" -> {
                val p = parseDouble(inputs["initial"], "Initial Deposit").getOrElse { return fail(it.message!!) }
                val m = parseDouble(inputs["monthly"], "Monthly Contribution").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 50.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val (fv, contrib) = computeCompound(p, r, y, 12, m)
                CalculationOutput(true, primaryLabel = "Projected Total Savings", primaryValue = fv, primaryFormatted = formatMoney(fv, currency),
                    metrics = listOf(MetricCardData("Total Contributed", contrib, formatMoney(contrib, currency)), MetricCardData("Interest Earned", fv - contrib, formatMoney(fv - contrib, currency))))
            }
            "emergency-fund-calculator" -> {
                val ess = parseDouble(inputs["essentials"], "Monthly Essentials", allowZero = false).getOrElse { return fail(it.message!!) }
                val mo = parseDouble(inputs["months"], "Months", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val cur = parseDouble(inputs["current"], "Current Savings").getOrElse { return fail(it.message!!) }
                val add = parseDouble(inputs["monthlyAdd"], "Monthly Addition").getOrElse { return fail(it.message!!) }
                val target = ess * mo
                val gap = max(0.0, target - cur)
                val monthsToGoal = if (gap == 0.0) 0 else if (add > 0.0) ceil(gap / add).toInt() else -1
                CalculationOutput(true, primaryLabel = "Target Emergency Fund", primaryValue = target, primaryFormatted = formatMoney(target, currency),
                    metrics = listOf(MetricCardData("Remaining Gap", gap, formatMoney(gap, currency)), MetricCardData("Months to Goal", monthsToGoal.toDouble(), if (monthsToGoal == 0) "Funded" else if (monthsToGoal > 0) "$monthsToGoal mo" else "Add contribution")))
            }
            "net-worth-calculator" -> {
                val cash = parseDouble(inputs["cash"], "Cash").getOrElse { return fail(it.message!!) }
                val inv = parseDouble(inputs["investments"], "Investments").getOrElse { return fail(it.message!!) }
                val prop = parseDouble(inputs["property"], "Property").getOrElse { return fail(it.message!!) }
                val oth = parseDouble(inputs["otherAssets"], "Other Assets").getOrElse { return fail(it.message!!) }
                val mort = parseDouble(inputs["mortgage"], "Mortgage").getOrElse { return fail(it.message!!) }
                val loans = parseDouble(inputs["loans"], "Loans").getOrElse { return fail(it.message!!) }
                val cards = parseDouble(inputs["cards"], "Cards").getOrElse { return fail(it.message!!) }
                val assets = cash + inv + prop + oth
                val liab = mort + loans + cards
                val nw = assets - liab
                CalculationOutput(true, primaryLabel = "Total Net Worth", primaryValue = nw, primaryFormatted = formatMoney(nw, currency),
                    metrics = listOf(MetricCardData("Total Assets", assets, formatMoney(assets, currency)), MetricCardData("Total Liabilities", liab, formatMoney(liab, currency))))
            }
            "cash-flow-calculator" -> {
                val s = parseDouble(inputs["salaryInflow"], "Salary").getOrElse { return fail(it.message!!) }
                val o = parseDouble(inputs["otherInflow"], "Other Inflow").getOrElse { return fail(it.message!!) }
                val f = parseDouble(inputs["fixedOutflow"], "Fixed").getOrElse { return fail(it.message!!) }
                val v = parseDouble(inputs["variableOutflow"], "Variable").getOrElse { return fail(it.message!!) }
                val d = parseDouble(inputs["debtOutflow"], "Debt").getOrElse { return fail(it.message!!) }
                val inflow = s + o
                val outflow = f + v + d
                val net = inflow - outflow
                CalculationOutput(true, primaryLabel = "Monthly Net Cash Flow", primaryValue = net, primaryFormatted = formatMoney(net, currency),
                    metrics = listOf(MetricCardData("Inflow", inflow, formatMoney(inflow, currency)), MetricCardData("Outflow", outflow, formatMoney(outflow, currency))))
            }
            "spending-analyzer" -> {
                val inc = parseDouble(inputs["monthlyIncome"], "Monthly Income", allowZero = false).getOrElse { return fail(it.message!!) }
                if (rows.isEmpty()) return fail("Add at least one spending item.")
                var total = 0.0
                val cats = linkedMapOf<String, Double>()
                for (r in rows) {
                    val v = parseDouble(r.amount, r.label).getOrElse { return fail(it.message!!) }
                    total += v
                    val c = r.category.ifBlank { "Other" }
                    cats[c] = (cats[c] ?: 0.0) + v
                }
                CalculationOutput(true, primaryLabel = "Total Analyzed Spending", primaryValue = total, primaryFormatted = formatMoney(total, currency),
                    metrics = listOf(MetricCardData("Income Used", (total / inc) * 100.0, formatPercent((total / inc) * 100.0)), MetricCardData("Remaining", inc - total, formatMoney(inc - total, currency))),
                    chartSlices = cats.map { ChartSlice(it.key, it.value) })
            }
            "financial-goal-planner" -> {
                val target = parseDouble(inputs["goalAmount"], "Goal Amount", allowZero = false).getOrElse { return fail(it.message!!) }
                val pv = parseDouble(inputs["currentSaved"], "Current Saved").getOrElse { return fail(it.message!!) }
                val yrs = parseDouble(inputs["years"], "Years", allowZero = false, min = 0.1).getOrElse { return fail(it.message!!) }
                val rate = parseDouble(inputs["annualReturn"], "Return %", max = 40.0).getOrElse { return fail(it.message!!) }
                val n = max(1, (yrs * 12).toInt())
                val r = rate / 100.0 / 12.0
                val rem = max(0.0, target - pv * (1.0 + r).pow(n))
                val pmt = if (rem == 0.0) 0.0 else if (r == 0.0) rem / n else (rem * r) / ((1.0 + r).pow(n) - 1.0)
                CalculationOutput(true, primaryLabel = "Required Monthly Contribution", primaryValue = pmt, primaryFormatted = formatMoney(pmt, currency),
                    metrics = listOf(MetricCardData("Goal Target", target, formatMoney(target, currency)), MetricCardData("Months", n.toDouble(), "$n months")))
            }
            "monthly-savings-planner" -> {
                val inc = parseDouble(inputs["netIncome"], "Net Income", allowZero = false).getOrElse { return fail(it.message!!) }
                val e = parseDouble(inputs["emergencySave"], "Emergency").getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["retirementSave"], "Retirement").getOrElse { return fail(it.message!!) }
                val g = parseDouble(inputs["goalSave"], "Goals").getOrElse { return fail(it.message!!) }
                val tot = e + r + g
                CalculationOutput(true, primaryLabel = "Total Monthly Savings", primaryValue = tot, primaryFormatted = formatMoney(tot, currency),
                    metrics = listOf(MetricCardData("Savings Rate", (tot / inc) * 100.0, formatPercent((tot / inc) * 100.0)), MetricCardData("Annual Savings", tot * 12.0, formatMoney(tot * 12.0, currency))))
            }
            "household-budget-calculator" -> {
                val e1 = parseDouble(inputs["earner1"], "Earner 1").getOrElse { return fail(it.message!!) }
                val e2 = parseDouble(inputs["earner2"], "Earner 2").getOrElse { return fail(it.message!!) }
                val h = parseDouble(inputs["housing"], "Housing").getOrElse { return fail(it.message!!) }
                val g = parseDouble(inputs["groceries"], "Groceries").getOrElse { return fail(it.message!!) }
                val c = parseDouble(inputs["childcare"], "Childcare").getOrElse { return fail(it.message!!) }
                val b = parseDouble(inputs["bills"], "Bills").getOrElse { return fail(it.message!!) }
                val s = parseDouble(inputs["savings"], "Savings").getOrElse { return fail(it.message!!) }
                val totInc = e1 + e2
                val totExp = h + g + c + b
                val unalloc = totInc - totExp - s
                CalculationOutput(true, primaryLabel = "Unallocated Household Cash", primaryValue = unalloc, primaryFormatted = formatMoney(unalloc, currency),
                    metrics = listOf(MetricCardData("Combined Income", totInc, formatMoney(totInc, currency)), MetricCardData("Living Expenses", totExp, formatMoney(totExp, currency))))
            }
            "family-expense-splitter" -> {
                val tip = parseDouble(inputs["extraTip"], "Extra Fee").getOrElse { return fail(it.message!!) }
                if (rows.size < 2) return fail("Add at least 2 participants.")
                var total = tip
                val paidList = mutableListOf<Pair<String, Double>>()
                for (r in rows) {
                    val amt = parseDouble(r.amount, r.label).getOrElse { return fail(it.message!!) }
                    total += amt
                    paidList.add(r.label to amt)
                }
                val share = total / paidList.size
                val tableRows = paidList.map { (name, paid) ->
                    val diff = paid - share
                    listOf(name, formatMoney(paid, currency), formatMoney(share, currency), if (diff >= 0) "Receives ${formatMoney(diff, currency)}" else "Owes ${formatMoney(abs(diff), currency)}")
                }
                CalculationOutput(true, primaryLabel = "Equal Share Per Person", primaryValue = share, primaryFormatted = formatMoney(share, currency),
                    metrics = listOf(MetricCardData("Total Spend", total, formatMoney(total, currency))),
                    tableHeaders = listOf("Member", "Paid", "Share", "Settlement"), tableRows = tableRows)
            }
            "debt-to-income-calculator" -> {
                val inc = parseDouble(inputs["grossIncome"], "Gross Income", allowZero = false).getOrElse { return fail(it.message!!) }
                val h = parseDouble(inputs["housingDebt"], "Housing").getOrElse { return fail(it.message!!) }
                val a = parseDouble(inputs["autoDebt"], "Auto/Student").getOrElse { return fail(it.message!!) }
                val c = parseDouble(inputs["cardMinimums"], "Card Minimums").getOrElse { return fail(it.message!!) }
                val totDebt = h + a + c
                val dti = (totDebt / inc) * 100.0
                CalculationOutput(true, primaryLabel = "Total Debt-to-Income (DTI)", primaryValue = dti, primaryFormatted = formatPercent(dti),
                    metrics = listOf(MetricCardData("Front-End DTI", (h / inc) * 100.0, formatPercent((h / inc) * 100.0)), MetricCardData("Total Debt", totDebt, formatMoney(totDebt, currency))))
            }
            "financial-health-calculator" -> {
                val inc = parseDouble(inputs["monthlyIncome"], "Income", allowZero = false).getOrElse { return fail(it.message!!) }
                val exp = parseDouble(inputs["monthlyExpenses"], "Expenses", allowZero = false).getOrElse { return fail(it.message!!) }
                val mDebt = parseDouble(inputs["monthlyDebt"], "Monthly Debt").getOrElse { return fail(it.message!!) }
                val liq = parseDouble(inputs["liquidSavings"], "Liquid Savings").getOrElse { return fail(it.message!!) }
                val tDebt = parseDouble(inputs["totalDebt"], "Total Debt").getOrElse { return fail(it.message!!) }
                val sr = ((inc - exp - mDebt) / inc) * 100.0
                val runway = liq / exp
                val dti = (mDebt / inc) * 100.0
                val idx = round(min(30.0, max(0.0, (sr / 20.0) * 30.0)) + min(30.0, max(0.0, (runway / 6.0) * 30.0)) + (if (dti <= 20) 25.0 else if (dti <= 36) 18.0 else 10.0) + (if (tDebt <= liq) 15.0 else 8.0), 0)
                CalculationOutput(true, primaryLabel = "Financial Wellness Index", primaryValue = idx, primaryFormatted = "${idx.toInt()} / 100",
                    metrics = listOf(MetricCardData("Savings Rate", sr, formatPercent(sr)), MetricCardData("Runway", runway, "${formatNumber(runway, 1)} mo")))
            }
            "loan-calculator", "emi-calculator" -> {
                val p = parseDouble(inputs["principal"], "Principal", allowZero = false).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 100.0).getOrElse { return fail(it.message!!) }
                val mKey = if (toolId == "emi-calculator") "tenureMonths" else "months"
                val m = parseDouble(inputs[mKey], "Months", allowZero = false, min = 1.0, max = 600.0).getOrElse { return fail(it.message!!) }
                val (emi, totalInt, sched) = computeLoan(p, r, m.toInt(), currency)
                CalculationOutput(true, primaryLabel = if (toolId == "emi-calculator") "Monthly EMI" else "Monthly Payment", primaryValue = emi, primaryFormatted = formatMoney(emi, currency),
                    metrics = listOf(MetricCardData("Total Interest", totalInt, formatMoney(totalInt, currency)), MetricCardData("Total Repayment", p + totalInt, formatMoney(p + totalInt, currency))),
                    tableHeaders = listOf("Period", "Payment", "Principal", "Interest", "Balance"), tableRows = sched,
                    chartSlices = listOf(ChartSlice("Principal", p), ChartSlice("Interest", totalInt)))
            }
            "personal-loan-calculator" -> {
                val p = parseDouble(inputs["amount"], "Loan Amount", allowZero = false).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 100.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Tenure", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val f = parseDouble(inputs["feePct"], "Fee %", max = 20.0).getOrElse { return fail(it.message!!) }
                val (emi, totalInt, sched) = computeLoan(p, r, (y * 12).toInt(), currency)
                val fee = p * f / 100.0
                CalculationOutput(true, primaryLabel = "Monthly Installment", primaryValue = emi, primaryFormatted = formatMoney(emi, currency),
                    metrics = listOf(MetricCardData("Net Disbursed", p - fee, formatMoney(p - fee, currency)), MetricCardData("Total Cost", totalInt + fee, formatMoney(totalInt + fee, currency))),
                    tableHeaders = listOf("Period", "Payment", "Principal", "Interest", "Balance"), tableRows = sched)
            }
            "home-loan-calculator" -> {
                val price = parseDouble(inputs["propertyPrice"], "Property Price", allowZero = false).getOrElse { return fail(it.message!!) }
                val dp = parseDouble(inputs["downPayment"], "Down Payment").getOrElse { return fail(it.message!!) }
                if (dp >= price) return fail("Down payment must be less than property price.")
                val r = parseDouble(inputs["rate"], "Rate %", max = 40.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Tenure", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val loan = price - dp
                val (emi, totalInt, sched) = computeLoan(loan, r, (y * 12).toInt(), currency)
                CalculationOutput(true, primaryLabel = "Monthly Home Loan EMI", primaryValue = emi, primaryFormatted = formatMoney(emi, currency),
                    metrics = listOf(MetricCardData("Financed", loan, formatMoney(loan, currency)), MetricCardData("Total Interest", totalInt, formatMoney(totalInt, currency))),
                    tableHeaders = listOf("Period", "EMI", "Principal", "Interest", "Balance"), tableRows = sched)
            }
            "car-loan-calculator" -> {
                val p = parseDouble(inputs["price"], "Vehicle Price", allowZero = false).getOrElse { return fail(it.message!!) }
                val d = parseDouble(inputs["down"], "Down Payment").getOrElse { return fail(it.message!!) }
                val t = parseDouble(inputs["tradeIn"], "Trade-In").getOrElse { return fail(it.message!!) }
                val tx = parseDouble(inputs["taxPct"], "Sales Tax %", max = 30.0).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 40.0).getOrElse { return fail(it.message!!) }
                val m = parseDouble(inputs["months"], "Months", allowZero = false, min = 6.0).getOrElse { return fail(it.message!!) }
                val financed = p + max(0.0, p - t) * (tx / 100.0) - d - t
                if (financed <= 0.0) return fail("Down payment and trade-in exceed vehicle cost.")
                val (emi, totalInt, sched) = computeLoan(financed, r, m.toInt(), currency)
                CalculationOutput(true, primaryLabel = "Monthly Car Payment", primaryValue = emi, primaryFormatted = formatMoney(emi, currency),
                    metrics = listOf(MetricCardData("Financed", financed, formatMoney(financed, currency)), MetricCardData("Total Interest", totalInt, formatMoney(totalInt, currency))),
                    tableHeaders = listOf("Period", "Payment", "Principal", "Interest", "Balance"), tableRows = sched)
            }
            "mortgage-calculator" -> {
                val hp = parseDouble(inputs["homePrice"], "Home Price", allowZero = false).getOrElse { return fail(it.message!!) }
                val dp = parseDouble(inputs["downPct"], "Down %", max = 95.0).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "APR %", max = 30.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Term", allowZero = false, min = 5.0).getOrElse { return fail(it.message!!) }
                val pt = parseDouble(inputs["propTax"], "Property Tax").getOrElse { return fail(it.message!!) }
                val ins = parseDouble(inputs["insurance"], "Insurance").getOrElse { return fail(it.message!!) }
                val hoa = parseDouble(inputs["hoa"], "HOA").getOrElse { return fail(it.message!!) }
                val loan = hp * (1.0 - dp / 100.0)
                val (emi, totalInt, sched) = computeLoan(loan, r, (y * 12).toInt(), currency)
                val piti = emi + pt / 12.0 + ins / 12.0 + hoa
                CalculationOutput(true, primaryLabel = "Total Monthly Mortgage (PITI)", primaryValue = piti, primaryFormatted = formatMoney(piti, currency),
                    metrics = listOf(MetricCardData("P&I EMI", emi, formatMoney(emi, currency)), MetricCardData("Total Interest", totalInt, formatMoney(totalInt, currency))),
                    chartSlices = listOf(ChartSlice("P&I", emi), ChartSlice("Tax", pt / 12.0), ChartSlice("Insurance", ins / 12.0), ChartSlice("HOA", hoa)),
                    tableHeaders = listOf("Period", "P&I", "Principal", "Interest", "Balance"), tableRows = sched)
            }
            "interest-calculator" -> {
                val p = parseDouble(inputs["principal"], "Principal", allowZero = false).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 100.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 0.05).getOrElse { return fail(it.message!!) }
                val mode = inputs["mode"] ?: "compound_monthly"
                val totalInt = when (mode) {
                    "simple" -> p * (r / 100.0) * y
                    "compound_quarterly" -> p * (1.0 + r / 100.0 / 4.0).pow(4.0 * y) - p
                    "compound_annual" -> p * (1.0 + r / 100.0).pow(y) - p
                    else -> p * (1.0 + r / 100.0 / 12.0).pow(12.0 * y) - p
                }
                CalculationOutput(true, primaryLabel = "Total Interest", primaryValue = totalInt, primaryFormatted = formatMoney(totalInt, currency),
                    metrics = listOf(MetricCardData("Final Amount", p + totalInt, formatMoney(p + totalInt, currency))))
            }
            "compound-interest-calculator" -> {
                val p = parseDouble(inputs["principal"], "Principal").getOrElse { return fail(it.message!!) }
                val m = parseDouble(inputs["monthly"], "Monthly Addition", defaultValue = 0.0).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 100.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val n = inputs["freq"]?.toIntOrNull() ?: 12
                val (fv, contrib) = computeCompound(p, r, y, n, m)
                CalculationOutput(true, primaryLabel = "Future Compound Value", primaryValue = fv, primaryFormatted = formatMoney(fv, currency),
                    metrics = listOf(MetricCardData("Contributed", contrib, formatMoney(contrib, currency)), MetricCardData("Interest Earned", fv - contrib, formatMoney(fv - contrib, currency))))
            }
            "simple-interest-calculator" -> {
                val p = parseDouble(inputs["principal"], "Principal", allowZero = false).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 100.0).getOrElse { return fail(it.message!!) }
                val t = parseDouble(inputs["time"], "Time", allowZero = false, min = 0.01).getOrElse { return fail(it.message!!) }
                val yrs = when (inputs["unit"]) { "months" -> t / 12.0; "days" -> t / 365.0; else -> t }
                val si = (p * r * yrs) / 100.0
                CalculationOutput(true, primaryLabel = "Simple Interest (SI)", primaryValue = si, primaryFormatted = formatMoney(si, currency),
                    metrics = listOf(MetricCardData("Maturity Value", p + si, formatMoney(p + si, currency))))
            }
            "loan-repayment-calculator" -> {
                val bal = parseDouble(inputs["balance"], "Loan Balance", allowZero = false).getOrElse { return fail(it.message!!) }
                val rPct = parseDouble(inputs["rate"], "Rate %", max = 50.0).getOrElse { return fail(it.message!!) }
                val m = parseDouble(inputs["months"], "Months", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }.toInt()
                val ex = parseDouble(inputs["extraMonthly"], "Extra Payment").getOrElse { return fail(it.message!!) }
                val (emi, baseInt, _) = computeLoan(bal, rPct, m, currency)
                val r = rPct / 100.0 / 12.0
                var cur = bal
                var accInt = 0.0
                var mo = 0
                while (cur > 0.01 && mo < 600) {
                    mo++
                    val i = cur * r
                    val pay = min(cur + i, emi + ex)
                    accInt += i
                    cur = max(0.0, cur + i - pay)
                }
                val saved = max(0.0, baseInt - accInt)
                CalculationOutput(true, primaryLabel = "Total Interest Saved", primaryValue = saved, primaryFormatted = formatMoney(saved, currency),
                    metrics = listOf(MetricCardData("New Payoff Time", mo.toDouble(), "$mo months"), MetricCardData("Months Saved", (m - mo).toDouble(), "${max(0, m - mo)} months")))
            }
            "loan-eligibility-calculator" -> {
                val inc = parseDouble(inputs["income"], "Income", allowZero = false).getOrElse { return fail(it.message!!) }
                val ex = parseDouble(inputs["existingEmi"], "Existing EMIs").getOrElse { return fail(it.message!!) }
                val ratio = parseDouble(inputs["maxRatio"], "Max Ratio %", min = 10.0, max = 75.0).getOrElse { return fail(it.message!!) }
                val rate = parseDouble(inputs["rate"], "Rate %", max = 40.0).getOrElse { return fail(it.message!!) }
                val yrs = parseDouble(inputs["years"], "Tenure", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val maxEmi = max(0.0, inc * (ratio / 100.0) - ex)
                val n = (yrs * 12).toInt()
                val r = rate / 100.0 / 12.0
                val maxLoan = if (maxEmi <= 0.0) 0.0 else if (r == 0.0) maxEmi * n else maxEmi * (((1.0 + r).pow(n) - 1.0) / (r * (1.0 + r).pow(n)))
                CalculationOutput(true, primaryLabel = "Max Eligible Loan", primaryValue = maxLoan, primaryFormatted = formatMoney(maxLoan, currency),
                    metrics = listOf(MetricCardData("Available Monthly EMI", maxEmi, formatMoney(maxEmi, currency))))
            }
            "credit-card-payoff-calculator" -> {
                val bal = parseDouble(inputs["balance"], "Balance", allowZero = false).getOrElse { return fail(it.message!!) }
                val apr = parseDouble(inputs["apr"], "APR %", max = 60.0).getOrElse { return fail(it.message!!) }
                val pay = parseDouble(inputs["monthlyPay"], "Monthly Payment", allowZero = false).getOrElse { return fail(it.message!!) }
                val r = apr / 100.0 / 12.0
                if (pay <= bal * r) return fail("Monthly payment must exceed first month's interest.")
                var cur = bal
                var totInt = 0.0
                var months = 0
                while (cur > 0.01 && months < 600) {
                    months++
                    val i = cur * r
                    val actual = min(cur + i, pay)
                    totInt += i
                    cur = max(0.0, cur + i - actual)
                }
                CalculationOutput(true, primaryLabel = "Months to Debt-Free", primaryValue = months.toDouble(), primaryFormatted = "$months months",
                    metrics = listOf(MetricCardData("Total Interest", totInt, formatMoney(totInt, currency)), MetricCardData("Total Paid", bal + totInt, formatMoney(bal + totInt, currency))))
            }
            "credit-card-interest-calculator" -> {
                val bal = parseDouble(inputs["avgBalance"], "Balance").getOrElse { return fail(it.message!!) }
                val apr = parseDouble(inputs["apr"], "APR %", max = 60.0).getOrElse { return fail(it.message!!) }
                val days = parseDouble(inputs["days"], "Days", allowZero = false, min = 1.0, max = 60.0).getOrElse { return fail(it.message!!) }
                val daily = bal * (apr / 100.0 / 365.0)
                val cycle = daily * days
                CalculationOutput(true, primaryLabel = "Billing Cycle Interest", primaryValue = cycle, primaryFormatted = formatMoney(cycle, currency),
                    metrics = listOf(MetricCardData("Daily Interest", daily, formatMoney(daily, currency)), MetricCardData("Annualized Cost", daily * 365.0, formatMoney(daily * 365.0, currency))))
            }
            "bank-deposit-calculator" -> {
                val d = parseDouble(inputs["deposit"], "Deposit", allowZero = false).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 50.0).getOrElse { return fail(it.message!!) }
                val m = parseDouble(inputs["months"], "Months", allowZero = false, min = 1.0).getOrElse { return fail(it.message!!) }
                val tx = parseDouble(inputs["taxPct"], "Tax %", max = 50.0).getOrElse { return fail(it.message!!) }
                val gross = d * (1.0 + r / 100.0 / 12.0).pow(m)
                val netInt = (gross - d) * (1.0 - tx / 100.0)
                val netMat = d + netInt
                CalculationOutput(true, primaryLabel = "Net Maturity Value", primaryValue = netMat, primaryFormatted = formatMoney(netMat, currency),
                    metrics = listOf(MetricCardData("Net Profit", netInt, formatMoney(netInt, currency))))
            }
            "fixed-deposit-calculator" -> {
                val p = parseDouble(inputs["principal"], "Principal", allowZero = false).getOrElse { return fail(it.message!!) }
                val r = parseDouble(inputs["rate"], "Rate %", max = 50.0).getOrElse { return fail(it.message!!) }
                val y = parseDouble(inputs["years"], "Years", allowZero = false, min = 0.25).getOrElse { return fail(it.message!!) }
                val mode = inputs["payout"] ?: "cumulative"
                val totInt = when (mode) {
                    "quarterly", "monthly" -> p * (r / 100.0) * y
                    else -> p * (1.0 + r / 100.0 / 4.0).pow(4.0 * y) - p
                }
                CalculationOutput(true, primaryLabel = "FD Maturity Value", primaryValue = p + totInt, primaryFormatted = formatMoney(p + totInt, currency),
                    metrics = listOf(MetricCardData("Total Interest", totInt, formatMoney(totInt, currency))))
            }
            else -> null
        }
    }
}
