package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TotalsTest {
    private val day = LocalDate.of(2026, 11, 8)
    private val vnd = Account(1, "VND bank", AccountType.BANK, 0, currency = Currency.VND)
    private val vnd2 = Account(2, "VND cash", AccountType.CASH, 0, currency = Currency.VND)
    private val twd = Account(3, "TWD", AccountType.CASH, 0, currency = Currency.TWD)
    private val accounts = listOf(vnd, vnd2, twd)

    private fun txn(id: Long, kind: TxnKind, amount: Long, accountId: Long, toAccountId: Long? = null) =
        Txn(id, kind, amount, accountId, toAccountId, if (kind == TxnKind.TRANSFER) null else 1, day)

    @Test
    fun sumsIncomeAndExpensePerCurrency() {
        val txns = listOf(
            txn(1, TxnKind.INCOME, 500_000, 1),
            txn(2, TxnKind.INCOME, 300_000, 2),
            txn(3, TxnKind.EXPENSE, 200_000, 1),
            txn(4, TxnKind.INCOME, 3_500, 3),
            txn(5, TxnKind.EXPENSE, 120, 3),
        )
        val totals = txnTotals(txns, accounts)
        assertEquals(TxnTotals(income = 800_000L, expense = 200_000L), totals[Currency.VND])
        assertEquals(TxnTotals(income = 3_500L, expense = 120L), totals[Currency.TWD])
    }

    @Test
    fun transferChangesNothing() {
        val txns = listOf(
            txn(1, TxnKind.INCOME, 500_000, 1),
            txn(2, TxnKind.TRANSFER, 1_000_000, 1, toAccountId = 3),
        )
        val totals = txnTotals(txns, accounts)
        assertEquals(TxnTotals(500_000L, 0L), totals[Currency.VND])
        assertEquals(TxnTotals(0L, 0L), totals[Currency.TWD])
    }

    @Test
    fun oneEntryPerCurrencyThatHasAnAccount() {
        val totals = txnTotals(emptyList(), accounts)
        assertEquals(setOf(Currency.VND, Currency.TWD), totals.keys)
        assertFalse(totals.containsKey(Currency.USD))
        assertEquals(TxnTotals(0L, 0L), totals[Currency.VND])
    }

    @Test
    fun mainCurrencyComesFirst() {
        assertEquals(listOf(Currency.TWD, Currency.VND), txnTotals(emptyList(), accounts).keys.toList())
    }
}
