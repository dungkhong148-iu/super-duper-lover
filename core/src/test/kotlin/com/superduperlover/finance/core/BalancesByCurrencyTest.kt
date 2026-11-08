package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BalancesByCurrencyTest {
    private val day = LocalDate.of(2026, 11, 8)
    private val a = Account(1, "A", AccountType.BANK, openingBalance = 5_000_000, currency = Currency.VND)
    private val b = Account(2, "B", AccountType.CASH, openingBalance = 1_000, currency = Currency.TWD)
    private val c = Account(3, "C", AccountType.CARD, openingBalance = 250, currency = Currency.USD)
    private val d = Account(4, "D", AccountType.CASH, openingBalance = 2_000_000, currency = Currency.VND)

    @Test
    fun sumsAccountsOfTheSameCurrency() {
        val result = balancesByCurrency(listOf(a, b, d), emptyList())
        assertEquals(7_000_000L, result[Currency.VND])
        assertEquals(1_000L, result[Currency.TWD])
        assertEquals(setOf(Currency.VND, Currency.TWD), result.keys)
    }

    @Test
    fun crossCurrencyTransferMovesBothAmounts() {
        val transfer = Txn(1, TxnKind.TRANSFER, 1_000_000, 1, 2, null, day, toAmount = 1_250)
        assertEquals(4_000_000L, accountBalance(a, listOf(transfer)))
        assertEquals(2_250L, accountBalance(b, listOf(transfer)))
        val result = balancesByCurrency(listOf(a, b), listOf(transfer))
        assertEquals(4_000_000L, result[Currency.VND])
        assertEquals(2_250L, result[Currency.TWD])
    }

    @Test
    fun sameCurrencyTransferKeepsTotalUnchanged() {
        val transfer = Txn(1, TxnKind.TRANSFER, 300_000, 1, 4, null, day)
        val result = balancesByCurrency(listOf(a, d), listOf(transfer))
        assertEquals(7_000_000L, result[Currency.VND])
    }

    @Test
    fun usdBalanceIsInCents() {
        val spend = Txn(1, TxnKind.EXPENSE, 50, 3, null, 1, day)
        assertEquals(200L, balancesByCurrency(listOf(c), listOf(spend))[Currency.USD])
    }

    @Test
    fun mainCurrencyComesFirst() {
        assertEquals(listOf(Currency.TWD, Currency.VND, Currency.USD), balancesByCurrency(listOf(a, b, c), emptyList()).keys.toList())
    }
}
