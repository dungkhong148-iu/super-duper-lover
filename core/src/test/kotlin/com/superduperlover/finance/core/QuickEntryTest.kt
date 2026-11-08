package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QuickEntryTest {
    private val day = LocalDate.of(2026, 11, 8)

    private fun transfer(id: Long, from: Long, to: Long, amount: Long, toAmount: Long?, date: LocalDate = day) =
        Txn(id, TxnKind.TRANSFER, amount, accountId = from, toAccountId = to, categoryId = null, date = date, toAmount = toAmount)

    // suggestRate

    @Test
    fun rateOfLatestTransferBetweenSameAccounts() {
        val txns = listOf(
            transfer(1, 1, 2, 1000, 28_000, day.minusDays(10)),
            transfer(2, 1, 2, 1000, 30_000, day),
        )
        assertEquals(30.0, suggestRate(1, 2, txns)!!, 1e-9)
    }

    @Test
    fun sameDayHigherIdWins() {
        val txns = listOf(transfer(2, 1, 2, 100, 3_000), transfer(1, 1, 2, 100, 2_000))
        assertEquals(30.0, suggestRate(1, 2, txns)!!, 1e-9)
    }

    @Test
    fun reverseTransferGivesInvertedRate() {
        val txns = listOf(transfer(1, 2, 1, 30_000, 1000))
        assertEquals(30.0, suggestRate(1, 2, txns)!!, 1e-9)
    }

    @Test
    fun otherAccountsAndSameCurrencyTransfersAreIgnored() {
        val txns = listOf(
            transfer(1, 1, 3, 100, 3_000),
            transfer(2, 1, 2, 500, null),
        )
        assertNull(suggestRate(1, 2, txns))
    }

    @Test
    fun applyRateRoundsToMinorUnit() {
        assertEquals(30_000L, applyRate(1000, 30.0))
        assertEquals(2L, applyRate(3, 0.5))
        assertEquals(0L, applyRate(0, 30.0))
    }

    // reconcileTxn

    private val acc = Account(1, "A", AccountType.BANK, openingBalance = 1_000)
    private val spent = Txn(1, TxnKind.EXPENSE, 300, 1, null, 10, day)

    @Test
    fun realBelowComputedGivesExpense() {
        val t = reconcileTxn(acc, listOf(spent), realBalance = 600, categoryId = 99, date = day)!!
        assertEquals(TxnKind.EXPENSE, t.kind)
        assertEquals(100L, t.amount)
        assertEquals(1L, t.accountId)
        assertEquals(99L, t.categoryId)
        assertEquals(0L, t.id)
    }

    @Test
    fun realAboveComputedGivesIncome() {
        val t = reconcileTxn(acc, listOf(spent), realBalance = 750, categoryId = 98, date = day)!!
        assertEquals(TxnKind.INCOME, t.kind)
        assertEquals(50L, t.amount)
        assertEquals(98L, t.categoryId)
    }

    @Test
    fun noDifferenceGivesNull() {
        assertNull(reconcileTxn(acc, listOf(spent), realBalance = 700, categoryId = 1, date = day))
    }

    @Test
    fun resultPassesValidation() {
        val cats = listOf(Category(99, RECONCILE_CATEGORY_NAME, CategoryKind.EXPENSE, "tool"))
        val t = reconcileTxn(acc, emptyList(), realBalance = 400, categoryId = 99, date = day)
        assertNotNull(t)
        assertEquals(emptyList<String>(), validateTxn(t!!, listOf(acc), cats))
    }
}
