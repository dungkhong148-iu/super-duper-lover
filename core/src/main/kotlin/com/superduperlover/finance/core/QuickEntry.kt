package com.superduperlover.finance.core

import java.time.LocalDate
import kotlin.math.roundToLong

/** Name of the category that holds balance corrections ("Điều chỉnh số dư"). One per kind, created on demand. */
const val RECONCILE_CATEGORY_NAME = "Chênh lệch"

/**
 * Exchange rate (target minor units per source minor unit) of the latest cross-currency transfer between the two
 * accounts, in either direction (a reverse transfer gives the inverted rate). null = none yet.
 * Latest = newest date, then highest id. Used to prefill "Số tiền nhận".
 */
fun suggestRate(fromAccountId: Long, toAccountId: Long, txns: List<Txn>): Double? {
    val latest = txns
        .filter { t ->
            t.kind == TxnKind.TRANSFER && t.toAmount != null && t.amount > 0 && t.toAmount > 0 &&
                ((t.accountId == fromAccountId && t.toAccountId == toAccountId) ||
                    (t.accountId == toAccountId && t.toAccountId == fromAccountId))
        }
        .maxWithOrNull(compareBy<Txn> { it.date }.thenBy { it.id })
        ?: return null
    val rate = latest.toAmount!!.toDouble() / latest.amount.toDouble()
    return if (latest.accountId == fromAccountId) rate else 1.0 / rate
}

/** [amount] times [rate], rounded to the nearest minor unit. */
fun applyRate(amount: Long, rate: Double): Long = (amount * rate).roundToLong()

/**
 * The transaction (id = 0) that makes [account]'s computed balance equal [realBalance]: an Expense when the real
 * balance is lower, an Income when higher, null when equal. [categoryId] must be the "Chênh lệch" category of
 * that kind (see [RECONCILE_CATEGORY_NAME]); call [reconcileKind] first to know which kind to look up.
 */
fun reconcileTxn(account: Account, txns: List<Txn>, realBalance: Long, categoryId: Long, date: LocalDate): Txn? {
    val diff = realBalance - accountBalance(account, txns)
    if (diff == 0L) return null
    return Txn(
        id = 0,
        kind = if (diff > 0) TxnKind.INCOME else TxnKind.EXPENSE,
        amount = kotlin.math.abs(diff),
        accountId = account.id,
        toAccountId = null,
        categoryId = categoryId,
        date = date,
        note = RECONCILE_CATEGORY_NAME,
    )
}

/** Which kind [reconcileTxn] will produce: EXPENSE (real lower), INCOME (real higher), null (equal). */
fun reconcileKind(account: Account, txns: List<Txn>, realBalance: Long): TxnKind? {
    val diff = realBalance - accountBalance(account, txns)
    return when {
        diff > 0 -> TxnKind.INCOME
        diff < 0 -> TxnKind.EXPENSE
        else -> null
    }
}
