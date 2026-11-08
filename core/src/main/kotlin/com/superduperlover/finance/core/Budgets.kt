package com.superduperlover.finance.core

import java.time.YearMonth

/**
 * Spending against [budget]: only EXPENSE transactions of the budget's month, on the budget's
 * account (null = all accounts in the budget's currency) and category (null = all). Transfers and income
 * never count. [accounts] must hold every account, archived ones too.
 * Status is decided with Long math: OK below 80%, WARN from 80% up to and including 100%, OVER above.
 */
fun budgetUsage(budget: Budget, txns: List<Txn>, accounts: List<Account>): BudgetUsage {
    val currencyOfAccount = accounts.associate { it.id to it.currency }
    val spent = txns.asSequence()
        .filter { it.kind == TxnKind.EXPENSE }
        .filter { YearMonth.from(it.date) == budget.month }
        .filter {
            // one account: that account; "all accounts": only accounts in the budget's currency
            if (budget.accountId != null) it.accountId == budget.accountId
            else currencyOfAccount[it.accountId] == budget.currency
        }
        .filter { budget.categoryId == null || it.categoryId == budget.categoryId }
        .sumOf { it.amount }
    val limit = budget.limit
    val status = when {
        spent > limit -> BudgetStatus.OVER
        spent * 5 >= limit * 4 -> BudgetStatus.WARN
        else -> BudgetStatus.OK
    }
    val ratio = if (limit > 0) spent.toDouble() / limit.toDouble() else if (spent > 0) 1.0 else 0.0
    return BudgetUsage(spent = spent, limit = limit, remaining = limit - spent, ratio = ratio, status = status)
}
