package com.superduperlover.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
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
)

@Entity(tableName = "budgets", indices = [Index("month")])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val month: String,
    val accountId: Long?,
    val categoryId: Long?,
    // "limit" is an SQL keyword, so the column is named differently.
    val limitAmount: Long,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val target: Long,
    val deadline: String?,
    val archived: Boolean,
)

@Entity(tableName = "goal_contributions", indices = [Index("goalId")])
data class ContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val goalId: Long,
    val amount: Long,
    val date: String,
)

fun AccountEntity.toCore() = Account(id, name, AccountType.valueOf(type), openingBalance, archived)
fun Account.toEntity() = AccountEntity(id, name, type.name, openingBalance, archived)

fun CategoryEntity.toCore() = Category(id, name, CategoryKind.valueOf(kind), icon, archived)
fun Category.toEntity() = CategoryEntity(id, name, kind.name, icon, archived)

fun TxnEntity.toCore() =
    Txn(id, TxnKind.valueOf(kind), amount, accountId, toAccountId, categoryId, LocalDate.parse(date), note)

fun Txn.toEntity() = TxnEntity(id, kind.name, amount, accountId, toAccountId, categoryId, date.toString(), note)

fun BudgetEntity.toCore() = Budget(id, YearMonth.parse(month), accountId, categoryId, limitAmount)
fun Budget.toEntity() = BudgetEntity(id, month.toString(), accountId, categoryId, limit)

fun GoalEntity.toCore() = Goal(id, name, target, deadline?.let(LocalDate::parse), archived)
fun Goal.toEntity() = GoalEntity(id, name, target, deadline?.toString(), archived)

fun ContributionEntity.toCore() = GoalContribution(id, goalId, amount, LocalDate.parse(date))
fun GoalContribution.toEntity() = ContributionEntity(id, goalId, amount, date.toString())
