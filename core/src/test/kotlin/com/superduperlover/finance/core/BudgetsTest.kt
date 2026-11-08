package com.superduperlover.finance.core

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetsTest {
    private val nov = YearMonth.of(2026, 11)
    private val vnd1 = Account(1, "A", AccountType.BANK, 0, currency = Currency.VND)
    private val vnd2 = Account(2, "B", AccountType.CASH, 0, currency = Currency.VND)
    private val twd3 = Account(3, "C", AccountType.CASH, 0, currency = Currency.TWD)
    private val accounts = listOf(vnd1, vnd2, twd3)
    private val budget = Budget(1, nov, accountId = 1, categoryId = null, limit = 1_000_000, currency = Currency.VND)

    private fun expense(
        amount: Long,
        accountId: Long = 1,
        categoryId: Long? = 5,
        date: LocalDate = LocalDate.of(2026, 11, 10),
    ) = Txn(0, TxnKind.EXPENSE, amount, accountId, null, categoryId, date)

    @Test
    fun halfSpentIsOk() {
        val u = budgetUsage(budget, listOf(expense(500_000)), accounts)
        assertEquals(BudgetUsage(500_000, 1_000_000, 500_000, 0.5, BudgetStatus.OK), u)
    }

    @Test
    fun eightyPercentAndExactLimitAreWarn() {
        assertEquals(BudgetStatus.WARN, budgetUsage(budget, listOf(expense(800_000)), accounts).status)
        assertEquals(BudgetStatus.WARN, budgetUsage(budget, listOf(expense(1_000_000)), accounts).status)
        assertEquals(BudgetStatus.OK, budgetUsage(budget, listOf(expense(799_999)), accounts).status)
    }

    @Test
    fun overLimitIsOverWithNegativeRemaining() {
        val u = budgetUsage(budget, listOf(expense(1_000_001)), accounts)
        assertEquals(BudgetStatus.OVER, u.status)
        assertEquals(-1L, u.remaining)
    }

    @Test
    fun backdatedExpenseCountsOnlyInItsOwnMonth() {
        val lastMonth = expense(900_000, date = LocalDate.of(2026, 10, 31))
        assertEquals(0L, budgetUsage(budget, listOf(lastMonth), accounts).spent)
        val octBudget = budget.copy(month = YearMonth.of(2026, 10))
        assertEquals(900_000L, budgetUsage(octBudget, listOf(lastMonth), accounts).spent)
    }

    @Test
    fun transfersAndIncomeAreNotSpending() {
        val date = LocalDate.of(2026, 11, 10)
        val transfer = Txn(0, TxnKind.TRANSFER, 400_000, 1, 2, null, date)
        val income = Txn(0, TxnKind.INCOME, 400_000, 1, null, 20, date)
        assertEquals(0L, budgetUsage(budget, listOf(transfer, income), accounts).spent)
    }

    @Test
    fun otherAccountIsNotCountedUnlessBudgetCoversAllAccounts() {
        val other = expense(300_000, accountId = 2)
        assertEquals(0L, budgetUsage(budget, listOf(other), accounts).spent)
        val allAccounts = budget.copy(accountId = null)
        assertEquals(300_000L, budgetUsage(allAccounts, listOf(other), accounts).spent)
    }

    @Test
    fun categoryBudgetCountsOnlyThatCategory() {
        val catBudget = budget.copy(categoryId = 5)
        val txns = listOf(expense(100_000, categoryId = 5), expense(200_000, categoryId = 6))
        assertEquals(100_000L, budgetUsage(catBudget, txns, accounts).spent)
    }

    @Test
    fun budgetForAllAccountsCountsOnlyAccountsOfItsCurrency() {
        val allTwd = budget.copy(accountId = null, currency = Currency.TWD, limit = 10_000)
        val onVnd = expense(300_000, accountId = 2)
        val onTwd = expense(2_500, accountId = 3)
        assertEquals(2_500L, budgetUsage(allTwd, listOf(onVnd, onTwd), accounts).spent)
        val allVnd = budget.copy(accountId = null)
        assertEquals(300_000L, budgetUsage(allVnd, listOf(onVnd, onTwd), accounts).spent)
    }
}
