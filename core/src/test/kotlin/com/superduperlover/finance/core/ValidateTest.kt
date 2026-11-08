package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTest {
    private val accounts = listOf(
        Account(1, "Ví", AccountType.CASH, 0),
        Account(2, "Bank", AccountType.BANK, 0),
        Account(3, "Cũ", AccountType.BANK, 0, archived = true),
    )
    private val categories = listOf(
        Category(10, "Ăn uống", CategoryKind.EXPENSE, "food"),
        Category(20, "Lương", CategoryKind.INCOME, "salary"),
        Category(11, "Cũ", CategoryKind.EXPENSE, "old", archived = true),
    )
    private val day = LocalDate.of(2026, 11, 8)

    private val validExpense = Txn(1, TxnKind.EXPENSE, 50_000, 1, null, 10, day)
    private val validTransfer = Txn(2, TxnKind.TRANSFER, 50_000, 1, 2, null, day)

    private fun errors(t: Txn) = validateTxn(t, accounts, categories)

    @Test
    fun validTxnsHaveNoErrors() {
        assertTrue(errors(validExpense).isEmpty())
        assertTrue(errors(validTransfer).isEmpty())
        assertTrue(errors(Txn(3, TxnKind.INCOME, 1, 2, null, 20, day)).isEmpty())
    }

    @Test
    fun amountMustBePositive() {
        assertTrue(errors(validExpense.copy(amount = 0)).isNotEmpty())
        assertTrue(errors(validExpense.copy(amount = -1)).isNotEmpty())
    }

    @Test
    fun accountMustExistAndBeActive() {
        assertTrue(errors(validExpense.copy(accountId = 99)).isNotEmpty())
        assertTrue(errors(validExpense.copy(accountId = 3)).isNotEmpty())
    }

    @Test
    fun transferNeedsDifferentActiveTargetAndNoCategory() {
        assertTrue(errors(validTransfer.copy(toAccountId = null)).isNotEmpty())
        assertTrue(errors(validTransfer.copy(toAccountId = 1)).isNotEmpty())
        assertTrue(errors(validTransfer.copy(toAccountId = 3)).isNotEmpty())
        assertTrue(errors(validTransfer.copy(categoryId = 10)).isNotEmpty())
    }

    @Test
    fun expenseAndIncomeNeedMatchingActiveCategory() {
        assertTrue(errors(validExpense.copy(categoryId = null)).isNotEmpty())
        assertTrue(errors(validExpense.copy(categoryId = 99)).isNotEmpty())
        assertTrue(errors(validExpense.copy(categoryId = 20)).isNotEmpty())
        assertTrue(errors(validExpense.copy(kind = TxnKind.INCOME, categoryId = 10)).isNotEmpty())
        assertTrue(errors(validExpense.copy(categoryId = 11)).isNotEmpty())
    }
}
