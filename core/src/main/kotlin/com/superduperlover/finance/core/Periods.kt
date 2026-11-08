package com.superduperlover.finance.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

private fun monthsPerPeriod(period: BudgetPeriod): Long = when (period) {
    BudgetPeriod.MONTH -> 1
    BudgetPeriod.QUARTER -> 3
    BudgetPeriod.YEAR -> 12
    else -> error("not a month based period")
}

/** First day of period [index] (0 = the first period, starting on [startDate]). */
private fun periodStart(period: BudgetPeriod, startDate: LocalDate, index: Int): LocalDate = when (period) {
    BudgetPeriod.DAY -> startDate.plusDays(index.toLong())
    BudgetPeriod.WEEK -> startDate.plusDays(7L * index)
    // plusMonths clamps the day to the end of a shorter month (31 Jan + 1 month = 28 Feb), always counted from startDate
    else -> startDate.plusMonths(monthsPerPeriod(period) * index)
}

/**
 * Dates of period [index] of a budget that starts on [startDate] (index 0 = first period).
 * DAY = 1 day, WEEK = 7 days from the start date, MONTH / QUARTER / YEAR = from the start day of the month to the day
 * before the same day 1 / 3 / 12 months later (start 25 Nov -> 25/11 to 24/12). A start day 29-31 falls on the last
 * day of a shorter month. Periods follow each other without gaps.
 */
fun periodRange(period: BudgetPeriod, startDate: LocalDate, index: Int): PeriodRange {
    val start = periodStart(period, startDate, index)
    val end = periodStart(period, startDate, index + 1).minusDays(1)
    return PeriodRange(start, end)
}

/** Index of the period that contains [day]; null when [day] is before [startDate]. */
fun periodIndexAt(period: BudgetPeriod, startDate: LocalDate, day: LocalDate): Int? {
    if (day.isBefore(startDate)) return null
    var index = when (period) {
        BudgetPeriod.DAY -> ChronoUnit.DAYS.between(startDate, day)
        BudgetPeriod.WEEK -> ChronoUnit.DAYS.between(startDate, day) / 7
        else -> ChronoUnit.MONTHS.between(startDate, day) / monthsPerPeriod(period)
    }.toInt()
    // month based guess can be off by one around clamped short months: settle it against the real ranges
    while (index > 0 && periodStart(period, startDate, index).isAfter(day)) index--
    while (!periodStart(period, startDate, index + 1).isAfter(day)) index++
    return index
}
