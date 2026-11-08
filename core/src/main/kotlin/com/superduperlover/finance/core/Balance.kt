package com.superduperlover.finance.core

/** Opening balance plus every transaction that touches [account]. Transfers move money between accounts. */
fun accountBalance(account: Account, txns: List<Txn>): Long {
    var balance = account.openingBalance
    for (t in txns) {
        when (t.kind) {
            TxnKind.INCOME -> if (t.accountId == account.id) balance += t.amount
            TxnKind.EXPENSE -> if (t.accountId == account.id) balance -= t.amount
            TxnKind.TRANSFER -> {
                if (t.accountId == account.id) balance -= t.amount
                if (t.toAccountId == account.id) balance += t.amount
            }
        }
    }
    return balance
}
