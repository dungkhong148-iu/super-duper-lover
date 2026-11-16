package com.superduperlover.finance.core

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStatsTest {
    private val wed = LocalDate.of(2026, 11, 18)
    private val twd = Account(1, "TWD", AccountType.CASH, 0)
    private val vnd = Account(2, "VND", AccountType.BANK, 0, currency = Currency.VND)

    private fun txn(kind: TxnKind, amount: Long, date: LocalDate, accountId: Long = 1) =
        Txn(0, kind, amount, accountId, null, 1, date)

    @Test
    fun rangeBoundsAndStepper() {
        assertEquals(PeriodRange(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22)), txRangeBounds(TxRange.WEEK, wed))
        assertEquals(PeriodRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)), txRangeBounds(TxRange.MONTH, wed))
        assertNull(txRangeBounds(TxRange.ALL, wed))
        assertEquals(LocalDate.of(2026, 10, 18), shiftTxAnchor(TxRange.MONTH, wed, -1))
        assertEquals(LocalDate.of(2026, 11, 11), shiftTxAnchor(TxRange.WEEK, wed, -1))
        assertTrue(inTxRange(LocalDate.of(2026, 11, 22), TxRange.WEEK, wed))
        assertFalse(inTxRange(LocalDate.of(2026, 11, 23), TxRange.WEEK, wed))
        assertTrue(inTxRange(LocalDate.of(2000, 1, 1), TxRange.ALL, wed))
    }

    @Test
    fun dailyFlowsCoverEveryDayAndSkipOtherCurrencyAndTransfers() {
        val txns = listOf(
            txn(TxnKind.INCOME, 100, wed),
            txn(TxnKind.EXPENSE, 30, wed),
            txn(TxnKind.EXPENSE, 5, wed.plusDays(1)),
            txn(TxnKind.EXPENSE, 999, wed, accountId = 2),
            Txn(0, TxnKind.TRANSFER, 50, 1, 2, null, wed),
            txn(TxnKind.EXPENSE, 7, wed.plusDays(10)),
        )
        val flows = dailyFlows(txns, listOf(twd, vnd), Currency.TWD, wed, wed.plusDays(2))
        assertEquals(3, flows.size)
        assertEquals(TxnTotals(100, 30), flows[wed])
        assertEquals(TxnTotals(0, 5), flows[wed.plusDays(1)])
        assertEquals(TxnTotals(0, 0), flows[wed.plusDays(2)])
    }

    @Test
    fun weeklyFlowsBucketByDayOfMonth() {
        val txns = listOf(
            txn(TxnKind.EXPENSE, 10, LocalDate.of(2026, 11, 7)),
            txn(TxnKind.EXPENSE, 20, LocalDate.of(2026, 11, 8)),
            txn(TxnKind.INCOME, 90, LocalDate.of(2026, 11, 30)),
        )
        val weeks = weeklyFlows(txns, listOf(twd), Currency.TWD, YearMonth.of(2026, 11))
        assertEquals(5, weeks.size)
        assertEquals(TxnTotals(0, 10), weeks[0])
        assertEquals(TxnTotals(0, 20), weeks[1])
        assertEquals(TxnTotals(90, 0), weeks[4])
    }

    @Test
    fun streakCountsBackAndSurvivesAnEmptyToday() {
        val days = setOf(wed.minusDays(1), wed.minusDays(2), wed.minusDays(3), wed.minusDays(5))
        assertEquals(3, loggingStreak(days, wed))
        assertEquals(4, loggingStreak(days + wed, wed))
        assertEquals(0, loggingStreak(setOf(wed.minusDays(2)), wed))
    }

    @Test
    fun healthScoreIsClampedSavingsRate() {
        val h = healthStats(TxnTotals(income = 1000, expense = 250), daysElapsed = 10, streakDays = 4)
        assertEquals(75, h.score)
        assertEquals(75.0, h.savingsRate!!, 0.001)
        assertEquals(25L, h.avgSpendPerDay)
        assertEquals(0, healthStats(TxnTotals(100, 300), 5, 0).score)
        val none = healthStats(TxnTotals(0, 40), 0, 0)
        assertNull(none.savingsRate)
        assertEquals(0, none.score)
        assertEquals(40L, none.avgSpendPerDay)
    }

    @Test
    fun budgetPaceDividesWhatIsLeftByDaysLeft() {
        val range = PeriodRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
        val usage = BudgetUsage(spent = 4000, limit = 10_000, remaining = 6000, ratio = 0.4, status = BudgetStatus.OK, range = range)
        val p = budgetPace(usage, LocalDate.of(2026, 11, 21))
        assertEquals(10, p.daysLeft)
        assertEquals(600L, p.safePerDay)
        assertEquals(20 / 30.0, p.timeRatio, 0.0001)
        assertTrue(p.onPace)
        val over = usage.copy(spent = 12_000, remaining = -2000, ratio = 1.2, status = BudgetStatus.OVER)
        val q = budgetPace(over, LocalDate.of(2026, 11, 30))
        assertEquals(1, q.daysLeft)
        assertEquals(0L, q.safePerDay)
        assertFalse(q.onPace)
    }
}
