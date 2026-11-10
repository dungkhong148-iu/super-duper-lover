package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarsTest {
    private val start = LocalDate.of(2026, 11, 10)

    @Test
    fun upTo30DaysIsDailyLongerIsMonthly() {
        val week = jarSchedule(JarDuration(7, DurationUnit.DAY), start)
        assertEquals(ReserveCycle.DAILY, week.cycle)
        assertEquals(7, week.periods)
        assertEquals(start.plusDays(7), week.endDate)
        val month = jarSchedule(JarDuration(1, DurationUnit.MONTH), start)
        assertEquals(ReserveCycle.DAILY, month.cycle)
        assertEquals(30, month.periods)
        val sixMonths = jarSchedule(JarDuration(6, DurationUnit.MONTH), start)
        assertEquals(ReserveCycle.MONTHLY, sixMonths.cycle)
        assertEquals(6, sixMonths.periods)
        assertEquals(start.plusMonths(6), sixMonths.endDate)
        assertEquals(12, jarSchedule(JarDuration(1, DurationUnit.YEAR), start).periods)
    }

    @Test
    fun days123BecomeFourMonthlyPeriodsAndThreeExtraDays() {
        val s = jarSchedule(JarDuration(123, DurationUnit.DAY), start)
        assertEquals(ReserveCycle.MONTHLY, s.cycle)
        assertEquals(4, s.periods)
        assertEquals(3, s.extraDays)
        assertEquals(start.plusMonths(4).plusDays(3), s.endDate)
        assertEquals(start.plusMonths(3), s.dueDate(3))
        assertEquals(s.endDate, s.dueDate(4))
    }

    @Test
    fun amountsSplitWithRemainderInTheLastPeriod() {
        assertEquals(listOf(333L, 333L, 334L), periodAmounts(1000, 3))
        assertEquals(listOf(0L, 0L, 2L), periodAmounts(2, 3))
        assertEquals(1_000_000L, periodAmounts(1_000_000, 7).sum())
        assertEquals(listOf(5L), periodAmounts(5, 1))
    }

    @Test
    fun durationOfPeriodsRoundTrips() {
        assertEquals(JarDuration(12, DurationUnit.DAY), durationOfPeriods(ReserveCycle.DAILY, 12))
        val monthly = durationOfPeriods(ReserveCycle.MONTHLY, 18)
        assertEquals(18, jarSchedule(monthly, start).periods)
    }

    @Test
    fun replanSplitsWhatIsLeftFromTheEditDay() {
        val goal = Goal(1, "Laptop", 1200, null, plan = JarPlan(JarDuration(6, DurationUnit.MONTH), start, 0, start))
        val edit = LocalDate.of(2027, 1, 10)
        val replanned = JarPlanCalculator.replan(goal, JarDuration(3, DurationUnit.MONTH), 400, edit)
        assertEquals(JarPlan(JarDuration(3, DurationUnit.MONTH), edit, 400, edit), replanned.plan)
        assertEquals(edit.plusMonths(3), replanned.deadline)
        val s = jarSchedule(replanned.plan!!.duration, edit)
        // (1200 - 400) over 3 periods
        assertEquals(listOf(266L, 266L, 268L), JarPlanCalculator.amounts(1200, 400, s))
    }

    @Test
    fun dueEntriesCatchUpBetweenLastEvaluatedAndToday() {
        val plan = JarPlan(JarDuration(6, DurationUnit.MONTH), start, 0, start)
        // 3 months and 5 days later: periods 1..3 are due
        val today = start.plusMonths(3).plusDays(5)
        val due = ScheduleEngine.dueEntries(600, 0, plan, today)
        assertEquals(listOf(1, 2, 3), due.map { it.periodIndex })
        assertEquals(listOf(start.plusMonths(1), start.plusMonths(2), start.plusMonths(3)), due.map { it.date })
        assertEquals(listOf(100L, 100L, 100L), due.map { it.amount })
        // already evaluated up to month 2: only the third is left
        val later = plan.copy(lastEvaluated = start.plusMonths(2))
        assertEquals(listOf(3), ScheduleEngine.dueEntries(600, 200, later, today).map { it.periodIndex })
        assertTrue(ScheduleEngine.dueEntries(600, 0, plan, start).isEmpty())
    }

    @Test
    fun dueEntriesStopAtTheTarget() {
        val plan = JarPlan(JarDuration(4, DurationUnit.DAY), start, 0, start)
        // 100 over 4 days = 25 each; 70 already saved by hand -> only 30 more fit
        val due = ScheduleEngine.dueEntries(100, 70, plan, start.plusDays(10))
        assertEquals(listOf(25L, 5L), due.map { it.amount })
        assertTrue(ScheduleEngine.dueEntries(100, 100, plan, start.plusDays(10)).isEmpty())
        assertTrue(ScheduleEngine.dueEntries(100, 130, plan, start.plusDays(10)).isEmpty())
    }

    @Test
    fun lastPeriodCarriesTheRemainder() {
        val plan = JarPlan(JarDuration(3, DurationUnit.DAY), start, 0, start)
        assertEquals(listOf(333L, 333L, 334L), ScheduleEngine.dueEntries(1000, 0, plan, start.plusDays(3)).map { it.amount })
    }

    private fun jar(id: Long, account: Long, currency: Currency, status: JarStatus = JarStatus.ACTIVE, archived: Boolean = false) =
        Goal(id, "J$id", 1000, null, archived = archived, currency = currency, status = status, sourceAccountId = account)

    @Test
    fun reservedAndFreeByCurrencyCountOnlyReservingJars() {
        val jars = listOf(
            JarSaving(jar(1, 1, Currency.TWD), 300),
            JarSaving(jar(2, 1, Currency.TWD, JarStatus.PENDING_SETTLEMENT), 200),
            JarSaving(jar(3, 1, Currency.TWD, JarStatus.COMPLETED), 999),
            JarSaving(jar(4, 1, Currency.TWD, JarStatus.CANCELLED), 999),
            JarSaving(jar(5, 1, Currency.TWD, archived = true), 999),
            JarSaving(jar(6, 2, Currency.VND), 50),
        )
        val reserved = reservedByCurrency(jars)
        assertEquals(mapOf(Currency.TWD to 500L, Currency.VND to 50L), reserved)
        val free = freeByCurrency(mapOf(Currency.TWD to 450L, Currency.VND to 100L), reserved)
        assertEquals(-50L, free[Currency.TWD])
        assertEquals(50L, free[Currency.VND])
        assertEquals(listOf(Currency.TWD, Currency.VND), free.keys.toList())
    }

    @Test
    fun segmentedBarSplitsTheSourceAccountBalance() {
        val jars = listOf(
            JarSaving(jar(1, 1, Currency.TWD), 300),
            JarSaving(jar(2, 7, Currency.TWD), 900), // other account
            JarSaving(jar(9, 1, Currency.TWD), 100), // the jar being edited: left out
        )
        val bar = SegmentedBarModel.of(balance = 1000, jars = jars, accountId = 1, thisGoalId = 9, thisTarget = 500)
        assertEquals(300L, bar.others)
        assertEquals(500L, bar.thisJar)
        assertEquals(200L, bar.free)
        assertTrue(!bar.negative)
        assertEquals(0.3f, bar.othersFraction, 0.0001f)
        assertEquals(0.5f, bar.thisJarFraction, 0.0001f)
        assertEquals(0.2f, bar.freeFraction, 0.0001f)
    }

    @Test
    fun segmentedBarFreeCanBeNegative() {
        val bar = SegmentedBar(balance = 400, others = 300, thisJar = 500)
        assertEquals(-400L, bar.free)
        assertTrue(bar.negative)
        assertEquals(0f, bar.freeFraction, 0f)
        assertEquals(800L, bar.scale)
    }

    @Test
    fun settlementIsNotSavings() {
        val goal = Goal(1, "Laptop", 1000, null)
        val list = listOf(
            GoalContribution(1, 1, 1000, start),
            GoalContribution(2, 1, 1000, start, type = ContributionType.SETTLEMENT, txnId = 5),
        )
        assertEquals(1000L, jarSaved(list))
        assertEquals(1000L, goalProgress(goal, list, start).saved)
    }
}
