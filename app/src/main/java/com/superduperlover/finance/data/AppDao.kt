package com.superduperlover.finance.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class UsageRow(val categoryId: Long, val uses: Int)

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

    @Query(
        "SELECT categoryId, COUNT(*) AS uses FROM txns " +
            "WHERE categoryId IS NOT NULL AND kind = :kind GROUP BY categoryId",
    )
    suspend fun categoryUsage(kind: String): List<UsageRow>

    // transactions; from/to are ISO dates (inclusive), null = no limit
    @Query(
        "SELECT * FROM txns WHERE (:from IS NULL OR date >= :from) AND (:to IS NULL OR date <= :to) " +
            "AND (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId) " +
            "AND (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY date DESC, id DESC",
    )
    fun txns(from: String?, to: String?, accountId: Long?, categoryId: Long?): Flow<List<TxnEntity>>

    @Upsert
    suspend fun upsertTxn(entity: TxnEntity): Long

    @Query("DELETE FROM txns WHERE id = :id")
    suspend fun deleteTxn(id: Long)

    // budgets
    @Query("SELECT * FROM budgets WHERE month = :month ORDER BY id")
    fun budgets(month: String): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsertBudget(entity: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    // goals
    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY id")
    fun goals(): Flow<List<GoalEntity>>

    @Upsert
    suspend fun upsertGoal(entity: GoalEntity): Long

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
