package com.superduperlover.finance.core

import java.time.LocalDate
import java.time.YearMonth

enum class AccountType { CASH, BANK, CARD, EWALLET }

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val openingBalance: Long,
    val archived: Boolean = false,
)

enum class CategoryKind { EXPENSE, INCOME }

data class Category(
    val id: Long,
    val name: String,
    val kind: CategoryKind,
    val icon: String,
    val archived: Boolean = false,
)

enum class TxnKind { EXPENSE, INCOME, TRANSFER }

data class Txn(
    val id: Long,
    val kind: TxnKind,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val date: LocalDate,
    val note: String = "",
)

data class Budget(
    val id: Long,
    val month: YearMonth,
    val accountId: Long?,
    val categoryId: Long?,
    val limit: Long,
)

data class Goal(
    val id: Long,
    val name: String,
    val target: Long,
    val deadline: LocalDate?,
    val archived: Boolean = false,
)

data class GoalContribution(
    val id: Long,
    val goalId: Long,
    val amount: Long,
    val date: LocalDate,
)

enum class BudgetStatus { OK, WARN, OVER }

/** [ratio] is a display value only; it is never stored or added up. */
data class BudgetUsage(
    val spent: Long,
    val limit: Long,
    val remaining: Long,
    val ratio: Double,
    val status: BudgetStatus,
)

/** [ratio] is a display value only; it is never stored or added up. */
data class GoalProgress(
    val saved: Long,
    val remaining: Long,
    val ratio: Double,
    val done: Boolean,
    val perMonth: Long?,
)
