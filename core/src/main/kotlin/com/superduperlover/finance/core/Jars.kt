package com.superduperlover.finance.core

import java.time.LocalDate

// Jars ("Kho hũ", phase H). A jar is a Goal: money set aside for a purpose. It is a virtual RESERVE: it never changes
// an account balance while saving; only settling it (a real EXPENSE) does. See docs/phase-h-plan.md.

/** SAVING = "Mục tiêu", SPENDING = "Chi tiêu" (a known big expense to save for). */
enum class JarKind { SAVING, SPENDING }

/** PENDING_SETTLEMENT = the jar is full and waits for the user to confirm the real expense. */
enum class JarStatus { ACTIVE, PENDING_SETTLEMENT, COMPLETED, CANCELLED }

enum class ContributionType { AUTO_SCHEDULED, MANUAL, SETTLEMENT }

enum class DurationUnit { DAY, MONTH, YEAR }

/** The reserve cycle is never chosen by the user: see [jarSchedule]. */
enum class ReserveCycle { DAILY, MONTHLY }

data class JarDuration(val count: Int, val unit: DurationUnit)

/**
 * The running plan of a jar. [start] = day the current schedule starts; [baseSaved] = what was already saved on
 * that day (the schedule splits target - baseSaved); [lastEvaluated] = last day the due periods were looked at.
 */
data class JarPlan(
    val duration: JarDuration,
    val start: LocalDate,
    val baseSaved: Long = 0,
    val lastEvaluated: LocalDate = start,
)

/** Biggest [JarDuration.count] the form offers per unit (the calculator itself accepts any count >= 1). */
fun maxDurationCount(unit: DurationUnit): Int = when (unit) {
    DurationUnit.DAY -> 30
    DurationUnit.MONTH -> 12
    DurationUnit.YEAR -> 3
}

private const val DAYS_PER_MONTH = 30
private const val MONTHS_PER_YEAR = 12

/** Length in "plan days": a month counts 30 days and a year 12 months, so 1 thang = 30 days, 1 nam = 12 months. */
fun JarDuration.totalDays(): Int = when (unit) {
    DurationUnit.DAY -> count
    DurationUnit.MONTH -> count * DAYS_PER_MONTH
    DurationUnit.YEAR -> count * MONTHS_PER_YEAR * DAYS_PER_MONTH
}

/**
 * When a plan reserves money. Fixed rule: total <= 30 days -> one reserve per day; longer -> one per month
 * (periods = totalDays / 30, [extraDays] = totalDays % 30, shown as "≈ 4 tháng 3 ngày" for 123 days).
 * Period i (1..[periods]) is due at its END: [dueDate]. The last one is [endDate], the expected finish day.
 */
data class JarSchedule(
    val cycle: ReserveCycle,
    val periods: Int,
    val extraDays: Int,
    val start: LocalDate,
    val endDate: LocalDate,
) {
    fun dueDate(index: Int): LocalDate {
        require(index in 1..periods) { "period $index not in 1..$periods" }
        return when {
            index == periods -> endDate
            cycle == ReserveCycle.DAILY -> start.plusDays(index.toLong())
            else -> start.plusMonths(index.toLong())
        }
    }
}

fun jarSchedule(duration: JarDuration, start: LocalDate): JarSchedule {
    require(duration.count >= 1) { "duration must be at least 1" }
    val total = duration.totalDays()
    return if (total <= DAYS_PER_MONTH) {
        JarSchedule(ReserveCycle.DAILY, total, 0, start, start.plusDays(total.toLong()))
    } else {
        val periods = total / DAYS_PER_MONTH
        val extra = total % DAYS_PER_MONTH
        JarSchedule(ReserveCycle.MONTHLY, periods, extra, start, start.plusMonths(periods.toLong()).plusDays(extra.toLong()))
    }
}

/**
 * Duration with exactly [periods] reserves: DAILY 1..30 -> days; MONTHLY (2 or more) -> months. One month is 30 days
 * and therefore daily, so a monthly cycle needs at least 2 periods. For the slider of the jar form.
 */
fun durationOfPeriods(cycle: ReserveCycle, periods: Int): JarDuration {
    require(periods >= 1) { "periods must be at least 1" }
    return when (cycle) {
        ReserveCycle.DAILY -> JarDuration(periods, DurationUnit.DAY)
        ReserveCycle.MONTHLY -> {
            require(periods >= 2) { "a monthly cycle has at least 2 periods" }
            JarDuration(periods, DurationUnit.MONTH)
        }
    }
}

/**
 * [total] split over [periods] in Long math: every period gets total / periods, the last one also gets the
 * remainder. The amounts always add up to [total].
 */
fun periodAmounts(total: Long, periods: Int): List<Long> {
    require(periods >= 1) { "periods must be at least 1" }
    val base = total / periods
    val rest = total % periods
    return List(periods) { i -> if (i == periods - 1) base + rest else base }
}

object JarPlanCalculator {
    /** The amount a plan still has to reserve: target minus what was saved when the schedule started. */
    fun planTotal(target: Long, baseSaved: Long): Long = (target - baseSaved).coerceAtLeast(0L)

    /** Amount of each period of a new plan (index 0 = first). */
    fun amounts(target: Long, baseSaved: Long, schedule: JarSchedule): List<Long> =
        periodAmounts(planTotal(target, baseSaved), schedule.periods)

    /** Plan of a new jar, or of a running one after an edit: re-splits (target - [saved]) from [today]. */
    fun plan(duration: JarDuration, saved: Long, today: LocalDate): JarPlan = JarPlan(duration, today, saved, today)

    /** [goal] with a new plan from [today]; [deadline] follows the end of the plan. */
    fun replan(goal: Goal, duration: JarDuration, saved: Long, today: LocalDate): Goal =
        goal.copy(plan = plan(duration, saved, today), deadline = jarSchedule(duration, today).endDate)
}

/** One reserve that has come due. [date] is the day it was due (earlier than today when the app was closed). */
data class DueEntry(val periodIndex: Int, val date: LocalDate, val amount: Long)

object ScheduleEngine {
    /**
     * Periods of [plan] due after [LocalDate] plan.lastEvaluated up to and including [today], in order. Stops at the
     * target: nothing is reserved once [saved] + the entries reach [target], and the last entry is cut to fit.
     */
    fun dueEntries(target: Long, saved: Long, plan: JarPlan, today: LocalDate): List<DueEntry> {
        val schedule = jarSchedule(plan.duration, plan.start)
        val amounts = JarPlanCalculator.amounts(target, plan.baseSaved, schedule)
        var room = (target - saved).coerceAtLeast(0L)
        val result = mutableListOf<DueEntry>()
        for (index in 1..schedule.periods) {
            if (room <= 0) break
            val due = schedule.dueDate(index)
            if (due.isAfter(today)) break
            if (!due.isAfter(plan.lastEvaluated)) continue
            val amount = minOf(amounts[index - 1], room)
            if (amount <= 0) continue
            result.add(DueEntry(index, due, amount))
            room -= amount
        }
        return result
    }
}

/** A jar and what it has saved (the sum of its contributions, see [jarSaved]). */
data class JarSaving(val goal: Goal, val saved: Long)

/** Jars that hold a reserve: not archived, and still saving or waiting to be settled. */
fun Goal.isReserving(): Boolean =
    !archived && (status == JarStatus.ACTIVE || status == JarStatus.PENDING_SETTLEMENT)

/** Saved amount of a jar: every contribution except the SETTLEMENT record (that one is the money spent). */
fun jarSaved(contributions: List<GoalContribution>): Long =
    contributions.filter { it.type != ContributionType.SETTLEMENT }.sumOf { it.amount }

/** Reserve of all reserving jars, per currency (never converted), in [CURRENCY_DISPLAY_ORDER]. */
fun reservedByCurrency(jars: List<JarSaving>): Map<Currency, Long> {
    val result = LinkedHashMap<Currency, Long>()
    for (currency in CURRENCY_DISPLAY_ORDER) {
        val inCurrency = jars.filter { it.goal.isReserving() && it.goal.currency == currency }
        if (inCurrency.isNotEmpty()) result[currency] = inCurrency.sumOf { it.saved }
    }
    return result
}

/** "Tự do" per currency: balance minus reserve. Negative is allowed (a reserve may be above the real balance). */
fun freeByCurrency(balances: Map<Currency, Long>, reserved: Map<Currency, Long>): Map<Currency, Long> {
    val result = LinkedHashMap<Currency, Long>()
    for (currency in CURRENCY_DISPLAY_ORDER) {
        if (currency in balances || currency in reserved) {
            result[currency] = (balances[currency] ?: 0L) - (reserved[currency] ?: 0L)
        }
    }
    return result
}

/** Reserve of the jars whose source account is [accountId], leaving out [excludeGoalId] (the jar being edited). */
fun reserveOnAccount(jars: List<JarSaving>, accountId: Long, excludeGoalId: Long = 0L): Long =
    jars.filter { it.goal.isReserving() && it.goal.sourceAccountId == accountId && it.goal.id != excludeGoalId }
        .sumOf { it.saved }

/**
 * The bar of the jar form. Bar length = [balance] of the source account. Segments: [others] = reserve of the other
 * jars on this account now; [thisJar] = this jar when full (its whole target; what it already saved is part of it);
 * [free] = what is left after both, negative when they do not fit (the third segment turns coral).
 */
data class SegmentedBar(val balance: Long, val others: Long, val thisJar: Long) {
    val free: Long get() = balance - others - thisJar
    val negative: Boolean get() = free < 0

    /** Length the segments are drawn against: the balance, or the two reserves when they are bigger. */
    val scale: Long get() = maxOf(balance, others + thisJar, 1L)

    val othersFraction: Float get() = fraction(others)
    val thisJarFraction: Float get() = fraction(thisJar)
    val freeFraction: Float get() = fraction(free.coerceAtLeast(0L))

    private fun fraction(part: Long): Float = (part.coerceAtLeast(0L).toDouble() / scale.toDouble()).toFloat()
}

object SegmentedBarModel {
    fun of(balance: Long, jars: List<JarSaving>, accountId: Long, thisGoalId: Long, thisTarget: Long): SegmentedBar =
        SegmentedBar(balance, reserveOnAccount(jars, accountId, thisGoalId), thisTarget.coerceAtLeast(0L))
}
