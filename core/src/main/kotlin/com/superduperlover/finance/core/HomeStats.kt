package com.superduperlover.finance.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// Pure helpers of the v3 Home: yellow-block range, in/out per day and week, health card, budget pace.

/** Range selector of the yellow transaction block. */
enum class TxRange { DAY, WEEK, MONTH, ALL }

/** First and last day (both included) of [range] around [anchor]; null for [TxRange.ALL]. Weeks run Monday to Sunday. */
fun txRangeBounds(range: TxRange, anchor: LocalDate): PeriodRange? = when (range) {
    TxRange.DAY -> PeriodRange(anchor, anchor)
    TxRange.WEEK -> PeriodRange(
        anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
        anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
    )
    TxRange.MONTH -> PeriodRange(anchor.withDayOfMonth(1), anchor.withDayOfMonth(anchor.lengthOfMonth()))
    TxRange.ALL -> null
}

/** The "<" ">" stepper: moves [anchor] by [steps] ranges (negative = back). ALL does not move. */
fun shiftTxAnchor(range: TxRange, anchor: LocalDate, steps: Int): LocalDate = when (range) {
    TxRange.DAY -> anchor.plusDays(steps.toLong())
    TxRange.WEEK -> anchor.plusWeeks(steps.toLong())
    TxRange.MONTH -> anchor.plusMonths(steps.toLong())
    TxRange.ALL -> anchor
}

fun inTxRange(date: LocalDate, range: TxRange, anchor: LocalDate): Boolean {
    val b = txRangeBounds(range, anchor) ?: return true
    return !date.isBefore(b.start) && !date.isAfter(b.endInclusive)
}

/**
 * Income and expense per day of [from]..[to] (both included), in [currency] only (the account's currency decides).
 * Every day of the range has an entry; transfers are ignored.
 */
fun dailyFlows(
    txns: List<Txn>,
    accounts: List<Account>,
    currency: Currency,
    from: LocalDate,
    to: LocalDate,
): Map<LocalDate, TxnTotals> {
    val ids = accounts.filter { it.currency == currency }.map { it.id }.toSet()
    val income = HashMap<LocalDate, Long>()
    val expense = HashMap<LocalDate, Long>()
    for (t in txns) {
        if (t.debtId != null || t.accountId !in ids || t.date.isBefore(from) || t.date.isAfter(to)) continue
        when (t.kind) {
            TxnKind.INCOME -> income[t.date] = (income[t.date] ?: 0L) + t.amount
            TxnKind.EXPENSE -> expense[t.date] = (expense[t.date] ?: 0L) + t.amount
            TxnKind.TRANSFER -> Unit
        }
    }
    val result = LinkedHashMap<LocalDate, TxnTotals>()
    var d = from
    while (!d.isAfter(to)) {
        result[d] = TxnTotals(income = income[d] ?: 0L, expense = expense[d] ?: 0L)
        d = d.plusDays(1)
    }
    return result
}

/** Week 1 = days 1-7 of the month, week 2 = 8-14, ...: 4 to 5 buckets (the chart of the Lịch page). */
fun weeklyFlows(txns: List<Txn>, accounts: List<Account>, currency: Currency, month: YearMonth): List<TxnTotals> {
    val daily = dailyFlows(txns, accounts, currency, month.atDay(1), month.atEndOfMonth())
    val weeks = (month.lengthOfMonth() + 6) / 7
    return List(weeks) { w ->
        val days = daily.filterKeys { (it.dayOfMonth - 1) / 7 == w }.values
        TxnTotals(income = days.sumOf { it.income }, expense = days.sumOf { it.expense })
    }
}

/**
 * Consecutive days with at least one transaction, counted back from [today]. A day without a transaction yet today
 * does not break the streak (it counts from yesterday); 0 when neither today nor yesterday has one.
 */
fun loggingStreak(txnDates: Set<LocalDate>, today: LocalDate): Int {
    var d = if (today in txnDates) today else today.minusDays(1)
    var n = 0
    while (d in txnDates) {
        n++
        d = d.minusDays(1)
    }
    return n
}

/**
 * [score] 0-100 = savings rate clamped; [savingsRate] is (income - expense) / income in percent, null without income
 * (score 0). [avgSpendPerDay] = expense / [daysElapsed] (at least 1 day).
 */
data class HealthStats(val score: Int, val savingsRate: Double?, val avgSpendPerDay: Long, val streakDays: Int)

fun healthStats(totals: TxnTotals, daysElapsed: Int, streakDays: Int): HealthStats {
    val rate = if (totals.income > 0) (totals.income - totals.expense) * 100.0 / totals.income else null
    return HealthStats(
        score = (rate ?: 0.0).coerceIn(0.0, 100.0).toInt(),
        savingsRate = rate,
        avgSpendPerDay = totals.expense / daysElapsed.coerceAtLeast(1),
        streakDays = streakDays,
    )
}

/**
 * [safePerDay] = what is left (never below 0) divided by the days left in the period including [today] (at least 1).
 * [timeRatio] = share of the period already passed; [onPace] = spending ratio not above the time ratio.
 */
data class BudgetPace(val safePerDay: Long, val daysLeft: Int, val timeRatio: Double, val onPace: Boolean)

fun budgetPace(usage: BudgetUsage, today: LocalDate): BudgetPace {
    val total = ChronoUnit.DAYS.between(usage.range.start, usage.range.endInclusive).toInt() + 1
    val left = (ChronoUnit.DAYS.between(today, usage.range.endInclusive).toInt() + 1).coerceIn(1, total)
    val timeRatio = (total - left).toDouble() / total
    return BudgetPace(
        safePerDay = usage.remaining.coerceAtLeast(0L) / left,
        daysLeft = left,
        timeRatio = timeRatio,
        onPace = usage.ratio <= timeRatio,
    )
}
