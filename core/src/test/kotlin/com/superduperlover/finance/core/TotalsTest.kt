package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TotalsTest {
    private val day = LocalDate.of(2026, 11, 8)

    @Test
    fun sumsIncomeAndExpense() {
        val txns = listOf(
            Txn(1, TxnKind.INCOME, 500_000, accountId = 1, toAccountId = null, categoryId = 1, date = day),
            Txn(2, TxnKind.INCOME, 300_000, accountId = 1, toAccountId = null, categoryId = 2, date = day),
            Txn(3, TxnKind.EXPENSE, 200_000, accountId = 1, toAccountId = null, categoryId = 3, date = day),
        )
        assertEquals(TxnTotals(income = 800_000L, expense = 200_000L), txnTotals(txns))
    }

    @Test
    fun transferChangesNothing() {
        val txns = listOf(
            Txn(1, TxnKind.INCOME, 500_000, accountId = 1, toAccountId = null, categoryId = 1, date = day),
            Txn(2, TxnKind.INCOME, 300_000, accountId = 1, toAccountId = null, categoryId = 2, date = day),
            Txn(3, TxnKind.EXPENSE, 200_000, accountId = 1, toAccountId = null, categoryId = 3, date = day),
            Txn(4, TxnKind.TRANSFER, 1_000_000, accountId = 1, toAccountId = 2, categoryId = null, date = day),
        )
        assertEquals(TxnTotals(income = 800_000L, expense = 200_000L), txnTotals(txns))
    }

    @Test
    fun emptyListGivesZero() {
        assertEquals(TxnTotals(income = 0L, expense = 0L), txnTotals(emptyList()))
    }
}
