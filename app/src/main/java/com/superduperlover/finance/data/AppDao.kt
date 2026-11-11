package com.superduperlover.finance.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class UsageRow(val categoryId: Long, val uses: Int)

data class SavedRow(val goalId: Long, val saved: Long)

@Dao
interface AppDao {
    // accounts
    @Query("SELECT * FROM accounts WHERE :includeArchived = 1 OR archived = 0 ORDER BY id")
    fun accounts(includeArchived: Boolean): Flow<List<AccountEntity>>

    @Upsert
    suspend fun upsertAccount(entity: AccountEntity): Long

    @Query("UPDATE accounts SET archived = 1 WHERE id = :id")
    suspend fun archiveAccount(id: Long)

    @Query("SELECT COUNT(*) FROM txns WHERE accountId = :id OR toAccountId = :id")
    suspend fun txnCountForAccount(id: Long): Int

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountRow(id: Long)

    @Query("DELETE FROM budgets WHERE accountId = :id")
    suspend fun deleteBudgetsForAccount(id: Long)

    @Query("UPDATE accounts SET archived = 0 WHERE id = :id")
    suspend fun restoreAccount(id: Long)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun accountById(id: Long): AccountEntity?

    @Query("SELECT * FROM txns WHERE accountId = :id OR toAccountId = :id")
    suspend fun txnsOfAccount(id: Long): List<TxnEntity>

    // categories
    @Query(
        "SELECT * FROM categories WHERE (:kind IS NULL OR kind = :kind) " +
            "AND (:includeArchived = 1 OR archived = 0) ORDER BY id",
    )
    fun categories(kind: String?, includeArchived: Boolean): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsertCategory(entity: CategoryEntity): Long

    @Query("UPDATE categories SET archived = 1 WHERE id = :id")
    suspend fun archiveCategory(id: Long)

    @Query("SELECT COUNT(*) FROM txns WHERE categoryId = :id")
    suspend fun txnCountForCategory(id: Long): Int

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryRow(id: Long)

    @Query("DELETE FROM budgets WHERE categoryId = :id")
    suspend fun deleteBudgetsForCategory(id: Long)

    @Query("UPDATE categories SET archived = 0 WHERE id = :id")
    suspend fun restoreCategory(id: Long)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun categoryById(id: Long): CategoryEntity?

    @Query("UPDATE txns SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun moveTxnsToCategory(fromId: Long, toId: Long)

    @Query(
        "SELECT categoryId, COUNT(*) AS uses FROM txns " +
            "WHERE categoryId IS NOT NULL AND kind = :kind GROUP BY categoryId",
    )
    suspend fun categoryUsage(kind: String): List<UsageRow>

    /** Like [categoryUsage] but only transactions dated [since] (ISO date) or later. */
    @Query(
        "SELECT categoryId, COUNT(*) AS uses FROM txns " +
            "WHERE categoryId IS NOT NULL AND kind = :kind AND date >= :since GROUP BY categoryId",
    )
    suspend fun categoryUsageSince(kind: String, since: String): List<UsageRow>

    @Query("SELECT * FROM categories WHERE kind = :kind AND name = :name ORDER BY id LIMIT 1")
    suspend fun categoryByName(kind: String, name: String): CategoryEntity?

    // transactions; from/to are ISO dates (inclusive), null = no limit
    @Query(
        "SELECT * FROM txns WHERE (:from IS NULL OR date >= :from) AND (:to IS NULL OR date <= :to) " +
            "AND (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId) " +
            "AND (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY date DESC, id DESC",
    )
    fun txns(from: String?, to: String?, accountId: Long?, categoryId: Long?): Flow<List<TxnEntity>>

    @Upsert
    suspend fun upsertTxn(entity: TxnEntity): Long

    @Query("SELECT * FROM txns WHERE id = :id")
    suspend fun txnById(id: Long): TxnEntity?

    /** A SETTLEMENT entry that points at a transaction about to be deleted keeps its amount but loses the link. */
    @Query("UPDATE goal_contributions SET txnId = NULL WHERE txnId = :txnId")
    suspend fun clearContributionTxn(txnId: Long)

    @Query("DELETE FROM txns WHERE id = :id")
    suspend fun deleteTxn(id: Long)

    // budgets
    @Query("SELECT * FROM budgets ORDER BY id")
    fun budgets(): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsertBudget(entity: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    // goals
    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY id")
    fun goals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE archived = 1 ORDER BY id")
    fun archivedGoals(): Flow<List<GoalEntity>>

    @Query("UPDATE goals SET archived = 0 WHERE id = :id")
    suspend fun restoreGoal(id: Long)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalRow(id: Long)

    @Query("SELECT COUNT(*) FROM goal_contributions WHERE goalId = :goalId")
    suspend fun contributionCount(goalId: Long): Int

    @Query("DELETE FROM goal_contributions WHERE goalId = :goalId")
    suspend fun deleteContributionsForGoal(goalId: Long)

    @Upsert
    suspend fun upsertGoal(entity: GoalEntity): Long

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun goalById(id: Long): GoalEntity?

    /** Completed on [date] (status COMPLETED), or back to in progress when null (status ACTIVE). */
    @Query(
        "UPDATE goals SET completedAt = :date, " +
            "status = CASE WHEN :date IS NULL THEN 'ACTIVE' ELSE 'COMPLETED' END WHERE id = :id",
    )
    suspend fun setGoalCompletedAt(id: Long, date: String?)

    @Query("UPDATE goals SET status = :status WHERE id = :id")
    suspend fun setGoalStatus(id: Long, status: String)

    @Query("UPDATE goals SET lastEvaluatedDate = :date WHERE id = :id")
    suspend fun setGoalLastEvaluated(id: Long, date: String)

    /** Jars with a running plan, the ones that get periods reserved when the app opens. */
    @Query("SELECT * FROM goals WHERE archived = 0 AND status = 'ACTIVE' AND planStart IS NOT NULL ORDER BY id")
    suspend fun activePlanGoals(): List<GoalEntity>

    @Query("UPDATE goals SET sourceAccountId = NULL WHERE sourceAccountId = :accountId")
    suspend fun clearGoalSourceAccount(accountId: Long)

    @Query("UPDATE goals SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearGoalCategory(categoryId: Long)

    /** Saved amount of a jar: every contribution except the SETTLEMENT record (the money spent). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE goalId = :goalId AND type != 'SETTLEMENT'")
    suspend fun contributionSum(goalId: Long): Long

    @Query("SELECT goalId, COALESCE(SUM(amount), 0) AS saved FROM goal_contributions WHERE type != 'SETTLEMENT' GROUP BY goalId")
    fun savedSums(): Flow<List<SavedRow>>

    @Query("UPDATE goals SET archived = 1 WHERE id = :id")
    suspend fun archiveGoal(id: Long)

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC, id DESC")
    fun contributions(goalId: Long): Flow<List<ContributionEntity>>

    @Upsert
    suspend fun upsertContribution(entity: ContributionEntity): Long

    @Query("DELETE FROM goal_contributions WHERE id = :id")
    suspend fun deleteContribution(id: Long)

    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun allAccounts(): List<AccountEntity>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun allCategories(): List<CategoryEntity>

    @Query("SELECT * FROM txns ORDER BY id")
    suspend fun allTxns(): List<TxnEntity>

    @Query("SELECT * FROM budgets ORDER BY id")
    suspend fun allBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM goals ORDER BY id")
    suspend fun allGoals(): List<GoalEntity>

    @Query("SELECT * FROM goal_contributions ORDER BY id")
    suspend fun allContributions(): List<ContributionEntity>

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM txns")
    suspend fun clearTxns()

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("DELETE FROM goal_contributions")
    suspend fun clearContributions()
}
