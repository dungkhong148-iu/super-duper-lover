package com.superduperlover.finance.core

import java.time.LocalDate

/** OWE = money the user borrowed and must pay back; LENT = money the user lent and waits to get back. */
enum class DebtKind { OWE, LENT }

/**
 * A debt. [total] is the full amount in [currency]. [accountId] / [originTxnId] = the account and the real
 * transaction created with the debt (null for a "memo only" old debt that touches no account).
 */
data class Debt(
    val id: Long,
    val kind: DebtKind,
    val title: String,
    val person: String,
    val total: Long,
    val currency: Currency = MAIN_CURRENCY,
    val accountId: Long? = null,
    val originTxnId: Long? = null,
    val startDate: LocalDate,
    val dueDate: LocalDate? = null,
    val note: String = "",
)

/** One repayment (OWE) or collection (LENT). [txnId] = its real transaction, null for a memo-only payment. */
data class DebtPayment(
    val id: Long,
    val debtId: Long,
    val amount: Long,
    val date: LocalDate,
    val accountId: Long? = null,
    val txnId: Long? = null,
)

fun debtPaid(debt: Debt, payments: List<DebtPayment>): Long =
    payments.filter { it.debtId == debt.id }.sumOf { it.amount }

/** What is still open; never below 0. */
fun debtRemaining(debt: Debt, payments: List<DebtPayment>): Long =
    (debt.total - debtPaid(debt, payments)).coerceAtLeast(0L)

fun isDebtSettled(debt: Debt, payments: List<DebtPayment>): Boolean = debtRemaining(debt, payments) == 0L

/** Paid share 0..1 for a progress bar (1 when the total is 0). */
fun debtProgress(debt: Debt, payments: List<DebtPayment>): Float =
    if (debt.total <= 0) 1f else (debtPaid(debt, payments).toFloat() / debt.total).coerceIn(0f, 1f)

/** Open and past its due date. */
fun isDebtOverdue(debt: Debt, payments: List<DebtPayment>, today: LocalDate): Boolean =
    debt.dueDate != null && debt.dueDate.isBefore(today) && !isDebtSettled(debt, payments)

/** Kind of the real transaction of a debt: borrow = INCOME, lend = EXPENSE, repay = EXPENSE, collect = INCOME. */
fun debtTxnKind(kind: DebtKind, isPayment: Boolean): TxnKind = when {
    kind == DebtKind.OWE && !isPayment -> TxnKind.INCOME
    kind == DebtKind.OWE -> TxnKind.EXPENSE
    !isPayment -> TxnKind.EXPENSE
    else -> TxnKind.INCOME
}

/** What is still open in one currency: [owe] = to pay back, [lent] = to get back. */
data class DebtTotals(val owe: Long, val lent: Long)

/** Open debts per currency (never converted), in [CURRENCY_DISPLAY_ORDER]; only currencies that have a debt. */
fun debtTotals(debts: List<Debt>, payments: List<DebtPayment>): Map<Currency, DebtTotals> {
    val result = LinkedHashMap<Currency, DebtTotals>()
    for (currency in CURRENCY_DISPLAY_ORDER) {
        val inCurrency = debts.filter { it.currency == currency }
        if (inCurrency.isEmpty()) continue
        result[currency] = DebtTotals(
            owe = inCurrency.filter { it.kind == DebtKind.OWE }.sumOf { debtRemaining(it, payments) },
            lent = inCurrency.filter { it.kind == DebtKind.LENT }.sumOf { debtRemaining(it, payments) },
        )
    }
    return result
}

/**
 * Net worth per currency: balances of the accounts + what is still lent - what is still owed. Borrowing 1000 into
 * account A raises A by 1000 and the debt by 1000, so the total does not move; a memo-only debt lowers it.
 * Currencies that have an account or a debt, in [CURRENCY_DISPLAY_ORDER].
 */
fun netWorthByCurrency(
    accounts: List<Account>,
    txns: List<Txn>,
    debts: List<Debt>,
    payments: List<DebtPayment>,
): Map<Currency, Long> {
    val balances = balancesByCurrency(accounts, txns)
    val open = debtTotals(debts, payments)
    val result = LinkedHashMap<Currency, Long>()
    for (currency in CURRENCY_DISPLAY_ORDER) {
        if (currency !in balances && currency !in open) continue
        val d = open[currency]
        result[currency] = (balances[currency] ?: 0L) + (d?.lent ?: 0L) - (d?.owe ?: 0L)
    }
    return result
}
