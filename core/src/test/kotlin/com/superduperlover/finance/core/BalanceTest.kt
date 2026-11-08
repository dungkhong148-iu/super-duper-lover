package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceTest {
    private val a = Account(1, "A", AccountType.BANK, openingBalance = 1_000_000)
    private val b = Account(2, "B", AccountType.CASH, openingBalance = 0)
    private val day = LocalDate.of(2026, 11, 8)

    private val txns = listOf(
        Txn(1, TxnKind.EXPENSE, 200_000, accountId = 1, toAccountId = null, categoryId = 10, date = day),
        Txn(2, TxnKind.INCOME, 500_000, accountId = 1, toAccountId = null, categoryId = 20, date = day),
        Txn(3, TxnKind.TRANSFER, 300_000, accountId = 1, toAccountId = 2, categoryId = null, date = day),
    )

    @Test
    fun sourceAccountBalanceFollowsExpenseIncomeAndTransferOut() {
        assertEquals(1_000_000L, accountBalance(a, txns))
    }

    @Test
    fun destinationAccountReceivesTransfer() {
        assertEquals(300_000L, accountBalance(b, txns))
    }

    @Test
    fun noTransactionsGivesOpeningBalance() {
        assertEquals(1_000_000L, accountBalance(a, emptyList()))
    }
}
