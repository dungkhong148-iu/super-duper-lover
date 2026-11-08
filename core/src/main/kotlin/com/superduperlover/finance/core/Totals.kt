package com.superduperlover.finance.core

data class TxnTotals(val income: Long, val expense: Long)

/**
 * Sum of INCOME and of EXPENSE amounts per currency of the transaction's account. TRANSFER is ignored.
 * One entry per currency that has an account in [accounts] (zeros when it has no transactions);
 * the main currency comes first (see [CURRENCY_DISPLAY_ORDER]). Amounts of different currencies are never added.
 */
fun txnTotals(txns: List<Txn>, accounts: List<Account>): Map<Currency, TxnTotals> {
    val currencyOfAccount = accounts.associate { it.id to it.currency }
    val income = HashMap<Currency, Long>()
    val expense = HashMap<Currency, Long>()
    for (t in txns) {
        val currency = currencyOfAccount[t.accountId] ?: continue
        when (t.kind) {
            TxnKind.INCOME -> income[currency] = (income[currency] ?: 0L) + t.amount
            TxnKind.EXPENSE -> expense[currency] = (expense[currency] ?: 0L) + t.amount
            TxnKind.TRANSFER -> Unit
        }
    }
    val result = LinkedHashMap<Currency, TxnTotals>()
    for (currency in CURRENCY_DISPLAY_ORDER) {
        if (accounts.any { it.currency == currency }) {
            result[currency] = TxnTotals(income = income[currency] ?: 0L, expense = expense[currency] ?: 0L)
        }
    }
    return result
}
