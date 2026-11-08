package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetsTest {
    private val vnd1 = Account(1, "A", AccountType.BANK, 0, currency = Currency.VND)
    private val vnd2 = Account(2, "B", AccountType.CASH, 0, currency = Currency.VND)
    private val twd3 = Account(3, "C", AccountType.CASH, 0, currency = Currency.TWD)
    private val accounts = listOf(vnd1, vnd2, twd3)
    private val start = LocalDate.of(2026, 11, 1)
    private val today = LocalDate.of(2026, 11, 20)
    private val budget = Budget(
        1, BudgetPeriod.MONTH, start, accountId = 1, categoryId = null, limit = 1_000_000, currency = Currency.VND,
    )

    private fun expense(
        amount: Long,
        accountId: Long = 1,
        categoryId: Long? = 5,
        date: LocalDate = LocalDate.of(2026, 11, 10),
    ) = Txn(0, TxnKind.EXPENSE, amount, accountId, null, categoryId, date)

    private fun usage(b: Budget, txns: List<Txn>, now: LocalDate = today) = budgetUsage(b, txns, accounts, now)

    @Test
    fun halfSpentIsOk() {
        val u = usage(budget, listOf(expense(500_000)))
        assertEquals(500_000L, u.spent)
        assertEquals(1_000_000L, u.limit)
        assertEquals(500_000L, u.remaining)
        assertEquals(0.5, u.ratio, 0.0)
        assertEquals(BudgetStatus.OK, u.status)
        assertEquals(PeriodRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)), u.range)
        assertEquals(0L, u.carried)
    }

    @Test
    fun eightyPercentAndExactLimitAreWarn() {
        assertEquals(BudgetStatus.WARN, usage(budget, listOf(expense(800_000))).status)
        assertEquals(BudgetStatus.WARN, usage(budget, listOf(expense(1_000_000))).status)
        assertEquals(BudgetStatus.OK, usage(budget, listOf(expense(799_999))).status)
    }

    @Test
    fun overLimitIsOverWithNegativeRemaining() {
        val u = usage(budget, listOf(expense(1_000_001)))
        assertEquals(BudgetStatus.OVER, u.status)
        assertEquals(-1L, u.remaining)
    }

    @Test
    fun backdatedExpenseCountsOnlyInItsOwnPeriod() {
        val lastMonth = expense(900_000, date = LocalDate.of(2026, 10, 31))
        val oct = budget.copy(startDate = LocalDate.of(2026, 10, 1))
        assertEquals(0L, usage(budget, listOf(lastMonth)).spent)
        assertEquals(900_000L, usage(oct, listOf(lastMonth), now = LocalDate.of(2026, 10, 31)).spent)
    }

    @Test
    fun transfersAndIncomeAreNotSpending() {
        val date = LocalDate.of(2026, 11, 10)
        val transfer = Txn(0, TxnKind.TRANSFER, 400_000, 1, 2, null, date)
        val income = Txn(0, TxnKind.INCOME, 400_000, 1, null, 20, date)
        assertEquals(0L, usage(budget, listOf(transfer, income)).spent)
    }

    @Test
    fun otherAccountIsNotCountedUnlessBudgetCoversAllAccounts() {
        val other = expense(300_000, accountId = 2)
        assertEquals(0L, usage(budget, listOf(other)).spent)
        assertEquals(300_000L, usage(budget.copy(accountId = null), listOf(other)).spent)
    }

    @Test
    fun categoryBudgetCountsOnlyThatCategory() {
        val catBudget = budget.copy(categoryId = 5)
        val txns = listOf(expense(100_000, categoryId = 5), expense(200_000, categoryId = 6))
        assertEquals(100_000L, usage(catBudget, txns).spent)
    }

    @Test
    fun budgetForAllAccountsCountsOnlyAccountsOfItsCurrency() {
        val allTwd = budget.copy(accountId = null, currency = Currency.TWD, limit = 10_000)
        val onVnd = expense(300_000, accountId = 2)
        val onTwd = expense(2_500, accountId = 3)
        assertEquals(2_500L, usage(allTwd, listOf(onVnd, onTwd)).spent)
        assertEquals(300_000L, usage(budget.copy(accountId = null), listOf(onVnd, onTwd)).spent)
    }

    @Test
    fun weeklyBudgetUsesTheCurrentWeekOnly() {
        val weekly = budget.copy(period = BudgetPeriod.WEEK, startDate = LocalDate.of(2026, 11, 5), limit = 100)
        // today 2026-11-20 is in week index 2 = 19/11-25/11
        val txns = listOf(
            expense(30, date = LocalDate.of(2026, 11, 19)),
            expense(40, date = LocalDate.of(2026, 11, 25)),
            expense(500, date = LocalDate.of(2026, 11, 18)),
            expense(500, date = LocalDate.of(2026, 11, 26)),
        )
        val u = usage(weekly, txns)
        assertEquals(70L, u.spent)
        assertEquals(PeriodRange(LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 25)), u.range)
    }

    @Test
    fun todayBeforeStartShowsTheFirstPeriod() {
        val future = budget.copy(startDate = LocalDate.of(2026, 12, 1))
        val u = usage(future, listOf(expense(5)), now = LocalDate.of(2026, 11, 20))
        assertEquals(PeriodRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)), u.range)
        assertEquals(0L, u.spent)
    }

    // ---- rollover (MONTH, limit 1000, starting 2026-11-01)
    private val rolling = Budget(
        1, BudgetPeriod.MONTH, LocalDate.of(2026, 11, 1), accountId = 1, categoryId = null, limit = 1000,
        currency = Currency.VND, rollover = true,
    )

    private fun inMonth(year: Int, month: Int, amount: Long) = expense(amount, date = LocalDate.of(year, month, 10))

    @Test
    fun leftoverCarriesForwardAndChains() {
        val txns = listOf(inMonth(2026, 11, 700), inMonth(2026, 12, 1200))
        val period1 = usage(rolling, txns, now = LocalDate.of(2026, 12, 15))
        assertEquals(300L, period1.carried)
        assertEquals(1300L, period1.effectiveLimit)
        assertEquals(100L, period1.remaining)
        val period2 = usage(rolling, txns, now = LocalDate.of(2027, 1, 15))
        assertEquals(100L, period2.carried)
        assertEquals(1100L, period2.effectiveLimit)
        assertEquals(1100L, period2.remaining)
    }

    @Test
    fun overspendCarriesAsNegative() {
        val u = usage(rolling, listOf(inMonth(2026, 11, 1500)), now = LocalDate.of(2026, 12, 15))
        assertEquals(-500L, u.carried)
        assertEquals(500L, u.effectiveLimit)
    }

    @Test
    fun rolloverOffNeverCarries() {
        val txns = listOf(inMonth(2026, 11, 700), inMonth(2026, 12, 1200))
        val u = usage(rolling.copy(rollover = false), txns, now = LocalDate.of(2027, 1, 15))
        assertEquals(0L, u.carried)
        assertEquals(1000L, u.effectiveLimit)
    }

    @Test
    fun emptyPeriodsInTheMiddleStillCarry() {
        val u = usage(rolling, emptyList(), now = LocalDate.of(2027, 1, 15))
        assertEquals(2000L, u.carried)
        assertEquals(3000L, u.effectiveLimit)
    }

    @Test
    fun statusUsesTheEffectiveLimit() {
        // 900 spent of 1000 + 300 carried = 1300 -> 69% -> OK; without carry it would be WARN
        val txns = listOf(inMonth(2026, 11, 700), inMonth(2026, 12, 900))
        val december = LocalDate.of(2026, 12, 15)
        assertEquals(BudgetStatus.OK, usage(rolling, txns, now = december).status)
        assertEquals(BudgetStatus.WARN, usage(rolling.copy(rollover = false), txns, now = december).status)
        assertEquals(
            BudgetStatus.OVER,
            usage(rolling.copy(rollover = false), listOf(inMonth(2026, 12, 1100)), now = december).status,
        )
    }

    @Test
    fun noEffectiveLimitLeftIsOver() {
        val deep = listOf(inMonth(2026, 11, 2500)) // carries -1500 -> effective -500
        val u = usage(rolling, deep, now = LocalDate.of(2026, 12, 15))
        assertEquals(-500L, u.effectiveLimit)
        assertEquals(BudgetStatus.OVER, u.status)
        assertEquals(1.0, u.ratio, 0.0)
        val zero = rolling.copy(limit = 0, rollover = false)
        assertEquals(BudgetStatus.OVER, usage(zero, listOf(inMonth(2026, 11, 1))).status)
        assertEquals(BudgetStatus.WARN, usage(zero, emptyList()).status)
    }
}
