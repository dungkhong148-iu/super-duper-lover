package com.superduperlover.finance.data

import androidx.room.withTransaction
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.GoalContribution
import com.superduperlover.finance.core.Txn
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DeleteResult { DELETED, HAS_TRANSACTIONS }

/**
 * The only door to storage. Reads return `:core` types; `id = 0` in `save*` means insert.
 * Items that have transactions are archived, never deleted.
 */
class Repo(private val db: AppDatabase) {
    private val dao = db.dao()

    // Upsert returns the new row id on insert and -1 on update.
    private fun idOf(requested: Long, result: Long) = if (requested == 0L) result else requested

    // accounts
    fun accounts(includeArchived: Boolean = false): Flow<List<Account>> =
        dao.accounts(includeArchived).map { rows -> rows.map { it.toCore() } }

    suspend fun saveAccount(a: Account): Long = idOf(a.id, dao.upsertAccount(a.toEntity()))

    suspend fun archiveAccount(id: Long) = dao.archiveAccount(id)

    /** Refuses (and deletes nothing) when any transaction uses the account. Budgets of the account go with it. */
    suspend fun deleteAccount(id: Long): DeleteResult = db.withTransaction {
        if (dao.txnCountForAccount(id) > 0) {
            DeleteResult.HAS_TRANSACTIONS
        } else {
            dao.deleteBudgetsForAccount(id)
            dao.deleteAccountRow(id)
            DeleteResult.DELETED
        }
    }

    // categories
    fun categories(kind: CategoryKind? = null, includeArchived: Boolean = false): Flow<List<Category>> =
        dao.categories(kind?.name, includeArchived).map { rows -> rows.map { it.toCore() } }

    suspend fun saveCategory(c: Category): Long = idOf(c.id, dao.upsertCategory(c.toEntity()))

    suspend fun archiveCategory(id: Long) = dao.archiveCategory(id)

    /** Refuses (and deletes nothing) when any transaction uses the category. Budgets of the category go with it. */
    suspend fun deleteCategory(id: Long): DeleteResult = db.withTransaction {
        if (dao.txnCountForCategory(id) > 0) {
            DeleteResult.HAS_TRANSACTIONS
        } else {
            dao.deleteBudgetsForCategory(id)
            dao.deleteCategoryRow(id)
            DeleteResult.DELETED
        }
    }

    /** category id -> number of transactions of [kind] that use it. Missing = never used. */
    suspend fun categoryUsage(kind: CategoryKind): Map<Long, Int> =
        dao.categoryUsage(kind.name).associate { it.categoryId to it.uses }

    // transactions
    /** Newest first. [month] filters by the transaction's own date; [accountId] also matches transfers into it. */
    fun txns(
        month: YearMonth? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
    ): Flow<List<Txn>> =
        dao.txns(month?.atDay(1)?.toString(), month?.atEndOfMonth()?.toString(), accountId, categoryId)
            .map { rows -> rows.map { it.toCore() } }

    suspend fun saveTxn(t: Txn): Long = idOf(t.id, dao.upsertTxn(t.toEntity()))

    suspend fun deleteTxn(id: Long) = dao.deleteTxn(id)

    // budgets
    fun budgets(month: YearMonth): Flow<List<Budget>> =
        dao.budgets(month.toString()).map { rows -> rows.map { it.toCore() } }

    suspend fun saveBudget(b: Budget): Long = idOf(b.id, dao.upsertBudget(b.toEntity()))

    suspend fun deleteBudget(id: Long) = dao.deleteBudget(id)

    // goals
    /** Active (not archived) goals. */
    fun goals(): Flow<List<Goal>> = dao.goals().map { rows -> rows.map { it.toCore() } }

    suspend fun saveGoal(g: Goal): Long = idOf(g.id, dao.upsertGoal(g.toEntity()))

    suspend fun archiveGoal(id: Long) = dao.archiveGoal(id)

    fun contributions(goalId: Long): Flow<List<GoalContribution>> =
        dao.contributions(goalId).map { rows -> rows.map { it.toCore() } }

    suspend fun addContribution(c: GoalContribution): Long = idOf(c.id, dao.upsertContribution(c.toEntity()))

    suspend fun deleteContribution(id: Long) = dao.deleteContribution(id)
}
