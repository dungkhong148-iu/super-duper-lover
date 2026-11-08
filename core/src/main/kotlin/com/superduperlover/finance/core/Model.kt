package com.superduperlover.finance.core

import java.time.LocalDate
import java.time.YearMonth

/** Currencies ChiLi knows. [decimals] = digits after the decimal point; amounts are stored as Long minor units. */
enum class Currency(val decimals: Int) { VND(0), TWD(0), USD(2) }

/** The default currency of new accounts, budgets and goals. */
val MAIN_CURRENCY = Currency.TWD

/** Order in which per-currency totals are shown: main currency first. */
val CURRENCY_DISPLAY_ORDER = listOf(Currency.TWD, Currency.VND, Currency.USD)

enum class AccountType { CASH, BANK, CARD, EWALLET }

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val openingBalance: Long,
    val archived: Boolean = false,
    val colorKey: String = defaultAccountColorKey(type),
    val currency: Currency = MAIN_CURRENCY,
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
    /** TRANSFER only: amount received, in the target account's currency. null = same as [amount]. */
    val toAmount: Long? = null,
)

data class Budget(
    val id: Long,
    val month: YearMonth,
    val accountId: Long?,
    val categoryId: Long?,
    val limit: Long,
    val currency: Currency = MAIN_CURRENCY,
)

data class Goal(
    val id: Long,
    val name: String,
    val target: Long,
    val deadline: LocalDate?,
    val archived: Boolean = false,
    val currency: Currency = MAIN_CURRENCY,
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
