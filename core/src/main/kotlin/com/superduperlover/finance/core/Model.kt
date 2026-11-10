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

/** How often a budget repeats. Periods are counted from [Budget.startDate] (see [periodRange]). */
enum class BudgetPeriod { DAY, WEEK, MONTH, QUARTER, YEAR }

/**
 * A budget repeats every [period], counted from [startDate]. With [rollover] what is left at the end of a period
 * (or the overspend, as a negative amount) is added to the limit of the next one.
 */
data class Budget(
    val id: Long,
    val period: BudgetPeriod,
    val startDate: LocalDate,
    val accountId: Long?,
    val categoryId: Long?,
    val limit: Long,
    val currency: Currency = MAIN_CURRENCY,
    val rollover: Boolean = false,
)

/** First and last day (both included) of one budget period. */
data class PeriodRange(val start: LocalDate, val endInclusive: LocalDate)

/**
 * A jar ("hũ"; the table is still called goals). Without a [plan] it is a plain saving goal filled by hand.
 * With a plan the app reserves money on a schedule (see Jars.kt). [deadline] = end of the plan when there is one.
 */
data class Goal(
    val id: Long,
    val name: String,
    val target: Long,
    val deadline: LocalDate?,
    val archived: Boolean = false,
    val currency: Currency = MAIN_CURRENCY,
    /** Day the goal was completed (automatically on reaching the target, or by hand); null = in progress. */
    val completedAt: LocalDate? = null,
    val kind: JarKind = JarKind.SAVING,
    val status: JarStatus = JarStatus.ACTIVE,
    /** Account the money is really spent from when the jar is settled. */
    val sourceAccountId: Long? = null,
    /** Expense category of the settlement transaction. */
    val categoryId: Long? = null,
    /** true: the app settles a full jar by itself; false: it waits for the user (PENDING_SETTLEMENT). */
    val autoDeduct: Boolean = false,
    val plan: JarPlan? = null,
)

data class GoalContribution(
    val id: Long,
    val goalId: Long,
    val amount: Long,
    val date: LocalDate,
    val type: ContributionType = ContributionType.MANUAL,
    /** SETTLEMENT only: the real expense transaction. */
    val txnId: Long? = null,
    /** AUTO_SCHEDULED only: which period of the plan (1 = first). */
    val periodIndex: Int? = null,
)

enum class BudgetStatus { OK, WARN, OVER }

/** [ratio] is a display value only; it is never stored or added up. */
data class BudgetUsage(
    val spent: Long,
    val limit: Long,
    val remaining: Long,
    val ratio: Double,
    val status: BudgetStatus,
    /** The current period of the budget. */
    val range: PeriodRange,
    /** Amount carried in from earlier periods (rollover); 0 when rollover is off. Negative after an overspend. */
    val carried: Long = 0,
) {
    /** The limit that counts in this period: [limit] plus [carried]. */
    val effectiveLimit: Long get() = limit + carried
}

/** [ratio] is a display value only; it is never stored or added up. */
data class GoalProgress(
    val saved: Long,
    val remaining: Long,
    val ratio: Double,
    val done: Boolean,
    val perMonth: Long?,
)
