package com.superduperlover.finance.core

/**
 * Opening balance plus every transaction that touches [account], in the account's own currency (minor units).
 * A transfer takes `amount` out of the source and puts `toAmount ?: amount` into the target.
 */
fun accountBalance(account: Account, txns: List<Txn>): Long {
    var balance = account.openingBalance
    for (t in txns) {
        when (t.kind) {
            TxnKind.INCOME -> if (t.accountId == account.id) balance += t.amount
            TxnKind.EXPENSE -> if (t.accountId == account.id) balance -= t.amount
            TxnKind.TRANSFER -> {
                if (t.accountId == account.id) balance -= t.amount
                if (t.toAccountId == account.id) balance += t.toAmount ?: t.amount
            }
        }
    }
    return balance
}

/**
 * Sum of the balances of [accounts], per currency (never converted). One entry per currency that has an
 * account in [accounts]; the main currency comes first (see [CURRENCY_DISPLAY_ORDER]).
 */
fun balancesByCurrency(accounts: List<Account>, txns: List<Txn>): Map<Currency, Long> {
    val result = LinkedHashMap<Currency, Long>()
    for (currency in CURRENCY_DISPLAY_ORDER) {
        val inCurrency = accounts.filter { it.currency == currency }
        if (inCurrency.isNotEmpty()) result[currency] = inCurrency.sumOf { accountBalance(it, txns) }
    }
    return result
}
