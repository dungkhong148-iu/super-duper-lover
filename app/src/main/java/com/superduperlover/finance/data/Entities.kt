package com.superduperlover.finance.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.GoalContribution
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.TxnKind
import java.time.LocalDate
import java.time.YearMonth

// Enums are stored by name, dates as ISO text (yyyy-MM-dd), months as yyyy-MM. Amounts are Long đồng.

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val type: String,
    val openingBalance: Long,
    val archived: Boolean,
    // Added in DB v2; existing rows are VND and get a colour by type in Migration1To2.
    @ColumnInfo(defaultValue = "emerald") val colorKey: String,
    @ColumnInfo(defaultValue = "VND") val currency: String,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val kind: String,
    val icon: String,
    val archived: Boolean,
)

@Entity(
    tableName = "txns",
    indices = [Index("date"), Index("accountId"), Index("toAccountId"), Index("categoryId")],
)
data class TxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val kind: String,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val date: String,
    val note: String,
    /** Transfers only: amount received in the target account's currency (null = same as amount). DB v2. */
    val toAmount: Long?,
)

@Entity(tableName = "budgets", indices = [Index("month")])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val month: String,
    val accountId: Long?,
    val categoryId: Long?,
    // "limit" is an SQL keyword, so the column is named differently.
    val limitAmount: Long,
    @ColumnInfo(defaultValue = "VND") val currency: String,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val target: Long,
    val deadline: String?,
    val archived: Boolean,
    @ColumnInfo(defaultValue = "VND") val currency: String,
)

@Entity(tableName = "goal_contributions", indices = [Index("goalId")])
data class ContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val goalId: Long,
    val amount: Long,
    val date: String,
)

fun AccountEntity.toCore() =
    Account(id, name, AccountType.valueOf(type), openingBalance, archived, colorKey, Currency.valueOf(currency))

fun Account.toEntity() = AccountEntity(id, name, type.name, openingBalance, archived, colorKey, currency.name)

fun CategoryEntity.toCore() = Category(id, name, CategoryKind.valueOf(kind), icon, archived)
fun Category.toEntity() = CategoryEntity(id, name, kind.name, icon, archived)

fun TxnEntity.toCore() =
    Txn(id, TxnKind.valueOf(kind), amount, accountId, toAccountId, categoryId, LocalDate.parse(date), note, toAmount)

fun Txn.toEntity() =
    TxnEntity(id, kind.name, amount, accountId, toAccountId, categoryId, date.toString(), note, toAmount)

fun BudgetEntity.toCore() =
    Budget(id, YearMonth.parse(month), accountId, categoryId, limitAmount, Currency.valueOf(currency))

fun Budget.toEntity() = BudgetEntity(id, month.toString(), accountId, categoryId, limit, currency.name)

fun GoalEntity.toCore() =
    Goal(id, name, target, deadline?.let(LocalDate::parse), archived, Currency.valueOf(currency))

fun Goal.toEntity() = GoalEntity(id, name, target, deadline?.toString(), archived, currency.name)

fun ContributionEntity.toCore() = GoalContribution(id, goalId, amount, LocalDate.parse(date))
fun GoalContribution.toEntity() = ContributionEntity(id, goalId, amount, date.toString())
