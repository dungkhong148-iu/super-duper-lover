package com.superduperlover.finance.core

data class TxnTotals(val income: Long, val expense: Long)

/** Sum of INCOME and of EXPENSE amounts. TRANSFER is ignored. */
fun txnTotals(txns: List<Txn>): TxnTotals {
    var income = 0L
    var expense = 0L
    for (t in txns) {
        when (t.kind) {
            TxnKind.INCOME -> income += t.amount
            TxnKind.EXPENSE -> expense += t.amount
            TxnKind.TRANSFER -> Unit
        }
    }
    return TxnTotals(income = income, expense = expense)
}
