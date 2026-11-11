package com.superduperlover.finance.core

// Rules for deleting archived things for good ("Lưu trữ" screen). Pure: the data layer applies the result.

/** Transactions of a purged category move to the "other" category of the same kind (created when missing). */
fun otherCategoryName(kind: CategoryKind) = if (kind == CategoryKind.EXPENSE) "Khác" else "Thu nhập khác"

/** Icon key of the "other" category of [kind] (same keys as the seed data). */
fun otherCategoryIcon(kind: CategoryKind) = if (kind == CategoryKind.EXPENSE) "other" else "income_other"

/**
 * The existing category the transactions of [purged] move to: the "other" category of the same kind, never
 * [purged] itself. null = none exists (the data layer creates it, unless [purged] is that category).
 */
fun fallbackCategoryFor(purged: Category, all: List<Category>): Category? =
    all.firstOrNull { it.id != purged.id && it.kind == purged.kind && it.name == otherCategoryName(purged.kind) }

/**
 * What happens to the transactions when [account] is deleted for good.
 * [deleted]: its expenses and incomes. [converted]: transfers with another account, kept so that account's balance
 * does not change: a transfer in becomes an Expense of the sender, a transfer out becomes an Income of the receiver
 * (in the receiver's currency). The converted transactions have `categoryId = null`: the data layer sets the
 * "other" category of their kind ([otherCategoryName]).
 */
data class AccountPurgePlan(val deleted: List<Txn>, val converted: List<Txn>)

fun planAccountPurge(account: Account, txns: List<Txn>): AccountPurgePlan {
    val deleted = mutableListOf<Txn>()
    val converted = mutableListOf<Txn>()
    for (t in txns) {
        val touches = t.accountId == account.id || t.toAccountId == account.id
        if (!touches) continue
        val other = if (t.accountId == account.id) t.toAccountId else t.accountId
        if (t.kind != TxnKind.TRANSFER || other == null || other == account.id) {
            deleted += t
        } else if (t.accountId == account.id) {
            val note = t.note.ifBlank { "Chuyển từ ${account.name}" }
            converted += Txn(t.id, TxnKind.INCOME, t.toAmount ?: t.amount, other, null, null, t.date, note)
        } else {
            val note = t.note.ifBlank { "Chuyển đến ${account.name}" }
            converted += Txn(t.id, TxnKind.EXPENSE, t.amount, other, null, null, t.date, note)
        }
    }
    return AccountPurgePlan(deleted, converted)
}

/** A jar can be deleted when it has no contribution, or when it is archived (its contributions go with it). */
fun canDeleteGoal(goal: Goal, contributionCount: Int): Boolean = goal.archived || contributionCount == 0
