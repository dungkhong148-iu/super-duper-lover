package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtsTest {
    private val day = LocalDate.of(2026, 11, 16)
    private val bank = Account(1, "Bank", AccountType.BANK, 1000)
    private val owe = Debt(1, DebtKind.OWE, "Card", "CTBC", total = 500, startDate = day, dueDate = day.plusDays(5))
    private val lent = Debt(2, DebtKind.LENT, "Minh", "Minh", total = 300, startDate = day)

    private fun pay(id: Long, debtId: Long, amount: Long) = DebtPayment(id, debtId, amount, day)

    @Test
    fun remainingProgressAndOverdue() {
        val payments = listOf(pay(1, 1, 200), pay(2, 1, 100), pay(3, 2, 999))
        assertEquals(300L, debtPaid(owe, payments))
        assertEquals(200L, debtRemaining(owe, payments))
        assertEquals(0.6f, debtProgress(owe, payments), 0.0001f)
        assertEquals(0L, debtRemaining(lent, payments))
        assertTrue(isDebtSettled(lent, payments))
        assertFalse(isDebtOverdue(owe, payments, day))
        assertTrue(isDebtOverdue(owe, payments, day.plusDays(6)))
        assertFalse(isDebtOverdue(lent, payments, day.plusDays(60)))
    }

    @Test
    fun debtTransactionKinds() {
        assertEquals(TxnKind.INCOME, debtTxnKind(DebtKind.OWE, isPayment = false))
        assertEquals(TxnKind.EXPENSE, debtTxnKind(DebtKind.OWE, isPayment = true))
        assertEquals(TxnKind.EXPENSE, debtTxnKind(DebtKind.LENT, isPayment = false))
        assertEquals(TxnKind.INCOME, debtTxnKind(DebtKind.LENT, isPayment = true))
    }

    @Test
    fun netWorthDoesNotMoveWhenBorrowingOrRepaying() {
        val before = netWorthByCurrency(listOf(bank), emptyList(), emptyList(), emptyList())
        assertEquals(1000L, before[Currency.TWD])
        // borrow 500 into the bank: balance +500, owed +500
        val borrow = Txn(1, TxnKind.INCOME, 500, 1, null, null, day, debtId = 1)
        val afterBorrow = netWorthByCurrency(listOf(bank), listOf(borrow), listOf(owe), emptyList())
        assertEquals(1000L, afterBorrow[Currency.TWD])
        // repay 200: balance -200, owed -200
        val repay = Txn(2, TxnKind.EXPENSE, 200, 1, null, null, day, debtId = 1)
        val afterRepay = netWorthByCurrency(listOf(bank), listOf(borrow, repay), listOf(owe), listOf(pay(1, 1, 200).copy(txnId = 2)))
        assertEquals(1000L, afterRepay[Currency.TWD])
    }

    @Test
    fun memoOnlyDebtsChangeNetWorth() {
        val worth = netWorthByCurrency(listOf(bank), emptyList(), listOf(owe, lent), emptyList())
        assertEquals(1000L - 500 + 300, worth[Currency.TWD])
        val totals = debtTotals(listOf(owe, lent), emptyList())
        assertEquals(DebtTotals(owe = 500, lent = 300), totals[Currency.TWD])
    }

    @Test
    fun debtCurrencyWithoutAccountStillAppears() {
        val usd = owe.copy(currency = Currency.USD, total = 70)
        val worth = netWorthByCurrency(listOf(bank), emptyList(), listOf(usd), emptyList())
        assertEquals(-70L, worth[Currency.USD])
        assertEquals(1000L, worth[Currency.TWD])
    }

    @Test
    fun debtTransactionsAreNotSpendingIncomeBudgetOrFlow() {
        val spend = Txn(1, TxnKind.EXPENSE, 40, 1, null, 1, day)
        val earn = Txn(2, TxnKind.INCOME, 90, 1, null, 2, day)
        val borrow = Txn(3, TxnKind.INCOME, 500, 1, null, null, day, debtId = 1)
        val repay = Txn(4, TxnKind.EXPENSE, 200, 1, null, null, day, debtId = 1)
        val all = listOf(spend, earn, borrow, repay)

        assertEquals(TxnTotals(income = 90, expense = 40), txnTotals(all, listOf(bank))[Currency.TWD])

        val budget = Budget(1, BudgetPeriod.MONTH, LocalDate.of(2026, 11, 1), null, null, 1000)
        assertEquals(40L, budgetUsage(budget, all, listOf(bank), day).spent)

        val flows = dailyFlows(all, listOf(bank), Currency.TWD, day, day)
        assertEquals(TxnTotals(income = 90, expense = 40), flows[day])

        // the account balance still counts every one of them
        assertEquals(1000L - 40 + 90 + 500 - 200, accountBalance(bank, all))
    }

    @Test
    fun repeatingJarStartsANewCycleAfterSettlement() {
        val plan = JarPlan(JarDuration(3, DurationUnit.MONTH), day.minusDays(90), baseSaved = 0, lastEvaluated = day)
        val jar = Goal(
            7, "Laptop", 900, null, completedAt = day, status = JarStatus.COMPLETED, sourceAccountId = 1, categoryId = 4,
            autoDeduct = true, plan = plan, repeat = true,
        )
        val next = nextJarCycle(jar, day)!!
        assertEquals(0L, next.id)
        assertEquals(JarStatus.ACTIVE, next.status)
        assertNull(next.completedAt)
        assertEquals(900L, next.target)
        assertEquals(JarDuration(3, DurationUnit.MONTH), next.plan!!.duration)
        assertEquals(day, next.plan!!.start)
        assertEquals(0L, next.plan!!.baseSaved)
        assertEquals(1L, next.sourceAccountId)
        assertTrue(next.repeat)
        assertNull(nextJarCycle(jar.copy(repeat = false), day))
        assertNull(nextJarCycle(jar.copy(plan = null), day))
    }
}
