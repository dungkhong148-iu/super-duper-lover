package com.superduperlover.finance.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurgeTest {
    private val day = LocalDate.of(2026, 11, 8)
    private val a = Account(1, "Ví", AccountType.CASH, 0, archived = true)

    private fun txn(id: Long, kind: TxnKind, accountId: Long, toAccountId: Long? = null, amount: Long = 100, toAmount: Long? = null, note: String = "") =
        Txn(id, kind, amount, accountId, toAccountId, if (kind == TxnKind.TRANSFER) null else 10, day, note, toAmount)

    @Test
    fun purgeAccountDeletesItsExpensesAndIncomes() {
        val txns = listOf(
            txn(1, TxnKind.EXPENSE, 1),
            txn(2, TxnKind.INCOME, 1),
            txn(3, TxnKind.EXPENSE, 2),
        )
        val plan = planAccountPurge(a, txns)
        assertEquals(listOf(1L, 2L), plan.deleted.map { it.id })
        assertTrue(plan.converted.isEmpty())
    }

    @Test
    fun transferIntoPurgedAccountBecomesExpenseOfTheOtherAccount() {
        val plan = planAccountPurge(a, listOf(txn(1, TxnKind.TRANSFER, accountId = 2, toAccountId = 1, amount = 500)))
        assertTrue(plan.deleted.isEmpty())
        val t = plan.converted.single()
        assertEquals(TxnKind.EXPENSE, t.kind)
        assertEquals(2L, t.accountId)
        assertEquals(500L, t.amount)
        assertNull(t.toAccountId)
        assertNull(t.toAmount)
        assertNull(t.categoryId)
        assertEquals(1L, t.id)
        assertEquals(day, t.date)
    }

    @Test
    fun transferOutOfPurgedAccountBecomesIncomeOfTheOtherAccountInItsCurrency() {
        // 1_000_000 VND sent from the purged account arrived as 1_250 in the other account
        val plan = planAccountPurge(
            a,
            listOf(txn(1, TxnKind.TRANSFER, accountId = 1, toAccountId = 2, amount = 1_000_000, toAmount = 1_250)),
        )
        val t = plan.converted.single()
        assertEquals(TxnKind.INCOME, t.kind)
        assertEquals(2L, t.accountId)
        assertEquals(1_250L, t.amount)
        assertNull(t.toAmount)
    }

    @Test
    fun sameCurrencyTransferKeepsAmountWhenToAmountIsNull() {
        val t = planAccountPurge(a, listOf(txn(1, TxnKind.TRANSFER, accountId = 1, toAccountId = 2, amount = 70))).converted.single()
        assertEquals(70L, t.amount)
    }

    @Test
    fun convertedTransferKeepsNoteOrGetsOneNamingTheDeletedAccount() {
        val plan = planAccountPurge(
            a,
            listOf(
                txn(1, TxnKind.TRANSFER, accountId = 1, toAccountId = 2, note = "tiền nhà"),
                txn(2, TxnKind.TRANSFER, accountId = 1, toAccountId = 2),
                txn(3, TxnKind.TRANSFER, accountId = 2, toAccountId = 1),
            ),
        )
        assertEquals(listOf("tiền nhà", "Chuyển từ Ví", "Chuyển đến Ví"), plan.converted.map { it.note })
    }

    @Test
    fun transfersNotTouchingTheAccountAreIgnored() {
        val plan = planAccountPurge(a, listOf(txn(1, TxnKind.TRANSFER, accountId = 2, toAccountId = 3)))
        assertTrue(plan.deleted.isEmpty())
        assertTrue(plan.converted.isEmpty())
    }

    @Test
    fun balanceOfTheOtherAccountDoesNotChangeAfterPurge() {
        val b = Account(2, "Bank", AccountType.BANK, 1_000)
        val before = listOf(
            txn(1, TxnKind.TRANSFER, accountId = 1, toAccountId = 2, amount = 300),
            txn(2, TxnKind.TRANSFER, accountId = 2, toAccountId = 1, amount = 120),
            txn(3, TxnKind.EXPENSE, accountId = 1, amount = 50),
        )
        val plan = planAccountPurge(a, before)
        val after = plan.converted.map { it.copy(categoryId = 99) }
        assertEquals(
            accountBalance(b, before),
            accountBalance(b, after),
        )
    }

    @Test
    fun fallbackIsTheOtherCategoryOfTheSameKindExceptThePurgedOne() {
        val other = Category(5, "Khác", CategoryKind.EXPENSE, "other")
        val incomeOther = Category(6, "Thu nhập khác", CategoryKind.INCOME, "income_other")
        val purged = Category(7, "Cũ", CategoryKind.EXPENSE, "old", archived = true)
        val all = listOf(other, incomeOther, purged)
        assertEquals(other, fallbackCategoryFor(purged, all))
        assertEquals(incomeOther, fallbackCategoryFor(Category(8, "Xưa", CategoryKind.INCOME, "x"), all))
        assertNull(fallbackCategoryFor(purged, listOf(purged)))
        assertNull(fallbackCategoryFor(other, all))
    }

    @Test
    fun jarWithContributionsCanBeDeletedOnlyWhenArchived() {
        val jar = Goal(1, "Du lịch", 1_000, null)
        assertTrue(canDeleteGoal(jar, contributionCount = 0))
        assertFalse(canDeleteGoal(jar, contributionCount = 3))
        assertTrue(canDeleteGoal(jar.copy(archived = true), contributionCount = 3))
    }
}
