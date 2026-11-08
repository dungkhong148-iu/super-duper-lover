package com.superduperlover.finance.core

import java.time.LocalDate

/**
 * Spending of the CURRENT period of [budget] (the period that contains [today]; before the start date: the first one).
 * Counts only EXPENSE transactions dated inside the period, on the budget's account (null = all accounts in the
 * budget's currency) and category (null = all). Transfers and income never count.
 * [accounts] must hold every account, archived ones too.
 *
 * With `rollover`, what was left in every earlier period (or the overspend, negative) is added to this period's limit:
 * `carried = sum over earlier periods of (limit - spent)`, chained from the start date.
 * Status is decided with Long math against the effective limit (limit + carried): OK below 80%, WARN from 80% up to
 * and including 100%, OVER above (an effective limit of 0 or less with any spend, or below 0, is OVER).
 */
fun budgetUsage(budget: Budget, txns: List<Txn>, accounts: List<Account>, today: LocalDate): BudgetUsage {
    val currencyOfAccount = accounts.associate { it.id to it.currency }
    val index = periodIndexAt(budget.period, budget.startDate, today) ?: 0

    val spentByPeriod = HashMap<Int, Long>()
    for (t in txns) {
        if (t.kind != TxnKind.EXPENSE) continue
        val onAccount = if (budget.accountId != null) {
            t.accountId == budget.accountId
        } else {
            currencyOfAccount[t.accountId] == budget.currency
        }
        if (!onAccount) continue
        if (budget.categoryId != null && t.categoryId != budget.categoryId) continue
        val i = periodIndexAt(budget.period, budget.startDate, t.date) ?: continue
        if (i <= index) spentByPeriod[i] = (spentByPeriod[i] ?: 0L) + t.amount
    }

    var carried = 0L
    if (budget.rollover) {
        for (k in 0 until index) carried += budget.limit - (spentByPeriod[k] ?: 0L)
    }

    val spent = spentByPeriod[index] ?: 0L
    val effective = budget.limit + carried
    val status = when {
        spent > effective -> BudgetStatus.OVER
        spent * 5 >= effective * 4 -> BudgetStatus.WARN
        else -> BudgetStatus.OK
    }
    val ratio = if (effective > 0) spent.toDouble() / effective.toDouble() else if (spent > 0 || effective < 0) 1.0 else 0.0
    return BudgetUsage(
        spent = spent,
        limit = budget.limit,
        remaining = effective - spent,
        ratio = ratio,
        status = status,
        range = periodRange(budget.period, budget.startDate, index),
        carried = carried,
    )
}
