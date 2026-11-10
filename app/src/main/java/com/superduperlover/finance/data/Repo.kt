package com.superduperlover.finance.data

import androidx.room.withTransaction
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.ContributionType
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.GoalContribution
import com.superduperlover.finance.core.JarSaving
import com.superduperlover.finance.core.JarStatus
import com.superduperlover.finance.core.ScheduleEngine
import com.superduperlover.finance.core.RECONCILE_CATEGORY_NAME
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.reconcileKind
import com.superduperlover.finance.core.reconcileTxn
import com.superduperlover.finance.core.shouldAutoComplete
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
            dao.clearGoalSourceAccount(id)
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
            dao.clearGoalCategory(id)
            dao.deleteCategoryRow(id)
            DeleteResult.DELETED
        }
    }

    /** category id -> number of transactions of [kind] that use it. Missing = never used. */
    suspend fun categoryUsage(kind: CategoryKind): Map<Long, Int> =
        dao.categoryUsage(kind.name).associate { it.categoryId to it.uses }

    /** Like [categoryUsage] but counts only transactions of the last [days] days up to [today]. */
    suspend fun categoryUsageRecent(kind: CategoryKind, days: Int = 60, today: LocalDate = LocalDate.now()): Map<Long, Int> =
        dao.categoryUsageSince(kind.name, today.minusDays(days.toLong()).toString()).associate { it.categoryId to it.uses }

    /**
     * Id of the "Chênh lệch" category of [kind] (used for balance corrections). Created on first use, no schema
     * change; an archived one is reused as is.
     */
    suspend fun reconcileCategoryId(kind: CategoryKind): Long = db.withTransaction {
        dao.categoryByName(kind.name, RECONCILE_CATEGORY_NAME)?.id
            ?: dao.upsertCategory(
                CategoryEntity(0, RECONCILE_CATEGORY_NAME, kind.name, "tool", archived = false),
            )
    }

    /**
     * Makes the computed balance of the account equal [realBalance] by saving one Expense or Income (category
     * "Chênh lệch") dated [today]. Returns the new transaction id, or null when there is no difference (or the
     * account does not exist).
     */
    suspend fun reconcileAccount(accountId: Long, realBalance: Long, today: LocalDate = LocalDate.now()): Long? =
        db.withTransaction {
            val account = dao.accounts(true).first().firstOrNull { it.id == accountId }?.toCore() ?: return@withTransaction null
            val txns = dao.txns(null, null, accountId, null).first().map { it.toCore() }
            val kind = reconcileKind(account, txns, realBalance) ?: return@withTransaction null
            val categoryId = reconcileCategoryId(if (kind == TxnKind.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE)
            val txn = reconcileTxn(account, txns, realBalance, categoryId, today) ?: return@withTransaction null
            dao.upsertTxn(txn.toEntity())
        }

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
    /** All budgets. Each repeats every period from its start date; use `budgetUsage(..., today)` for the current one. */
    fun budgets(): Flow<List<Budget>> = dao.budgets().map { rows -> rows.map { it.toCore() } }

    suspend fun saveBudget(b: Budget): Long = idOf(b.id, dao.upsertBudget(b.toEntity()))

    suspend fun deleteBudget(id: Long) = dao.deleteBudget(id)

    // goals
    /** Active (not archived) goals. */
    fun goals(): Flow<List<Goal>> = dao.goals().map { rows -> rows.map { it.toCore() } }

    suspend fun saveGoal(g: Goal): Long = idOf(g.id, dao.upsertGoal(g.toEntity()))

    suspend fun archiveGoal(id: Long) = dao.archiveGoal(id)

    /** Marks the goal completed on [date] (by hand, before the target is reached). */
    suspend fun completeGoal(id: Long, date: LocalDate = LocalDate.now()) = dao.setGoalCompletedAt(id, date.toString())

    /** Back to "in progress". It is not completed again until a contribution crosses the target (see shouldAutoComplete). */
    suspend fun reopenGoal(id: Long) = dao.setGoalCompletedAt(id, null)

    fun contributions(goalId: Long): Flow<List<GoalContribution>> =
        dao.contributions(goalId).map { rows -> rows.map { it.toCore() } }

    /** Every active jar with what it has saved: the input of reservedByCurrency / freeByCurrency / SegmentedBarModel. */
    fun jarSavings(): Flow<List<JarSaving>> = combine(goals(), dao.savedSums()) { goals, sums ->
        val saved = sums.associate { it.goalId to it.saved }
        goals.map { JarSaving(it, saved[it.id] ?: 0L) }
    }

    /**
     * Adds (id = 0) or edits a contribution. A goal without a plan is completed on [today] when the contribution brings
     * the saved amount to the target (only if it is not completed yet and was below the target before). A jar with a
     * plan that gets full is settled (auto-deduct on) or waits as PENDING_SETTLEMENT.
     */
    suspend fun addContribution(c: GoalContribution, today: LocalDate = LocalDate.now()): Long = db.withTransaction {
        val goal = dao.goalById(c.goalId)?.toCore()
        val savedBefore = dao.contributionSum(c.goalId)
        val id = idOf(c.id, dao.upsertContribution(c.toEntity()))
        if (goal != null) {
            if (goal.plan == null) {
                if (shouldAutoComplete(goal, savedBefore, dao.contributionSum(c.goalId))) {
                    dao.setGoalCompletedAt(goal.id, today.toString())
                }
            } else {
                settleIfFull(goal.id, today)
            }
        }
        id
    }

    suspend fun deleteContribution(id: Long) = dao.deleteContribution(id)

    /**
     * Reserves the periods of every running plan that came due since it was last looked at (the app has no
     * background job; call it when MainActivity starts). Returns how many entries were added.
     */
    suspend fun materializeDueJarEntries(today: LocalDate = LocalDate.now()): Int = db.withTransaction {
        var added = 0
        for (row in dao.activePlanGoals()) {
            val goal = row.toCore()
            val plan = goal.plan ?: continue
            if (!plan.lastEvaluated.isBefore(today)) continue
            val due = ScheduleEngine.dueEntries(goal.target, dao.contributionSum(goal.id), plan, today)
            for (e in due) {
                dao.upsertContribution(
                    ContributionEntity(0, goal.id, e.amount, e.date.toString(), ContributionType.AUTO_SCHEDULED.name, null, e.periodIndex),
                )
                added++
            }
            dao.setGoalLastEvaluated(goal.id, today.toString())
            settleIfFull(goal.id, today)
        }
        added
    }

    /**
     * Spends the jar for real: one EXPENSE of [amount] (default: the target) from the source account, in
     * [categoryId] (default: the jar's category), dated [date]; the jar is COMPLETED and a SETTLEMENT entry records
     * the transaction. Returns the transaction id, or null when the jar cannot be settled (no source account or
     * category, account of another currency, already completed or cancelled).
     */
    suspend fun settleJar(
        goalId: Long,
        categoryId: Long? = null,
        amount: Long? = null,
        date: LocalDate = LocalDate.now(),
    ): Long? = db.withTransaction {
        val goal = dao.goalById(goalId)?.toCore() ?: return@withTransaction null
        settle(goal, categoryId, amount, date)
    }

    /** The jar stops: its reserve is released and nothing is spent. */
    suspend fun cancelJar(id: Long) = dao.setGoalStatus(id, JarStatus.CANCELLED.name)

    // A plan jar that reached its target: settle it when auto-deduct is on and possible, else wait for the user.
    private suspend fun settleIfFull(goalId: Long, today: LocalDate) {
        val goal = dao.goalById(goalId)?.toCore() ?: return
        if (goal.plan == null || goal.status != JarStatus.ACTIVE) return
        if (dao.contributionSum(goalId) < goal.target) return
        val settled = goal.autoDeduct && settle(goal, null, null, today) != null
        if (!settled) dao.setGoalStatus(goalId, JarStatus.PENDING_SETTLEMENT.name)
    }

    private suspend fun settle(goal: Goal, categoryId: Long?, amount: Long?, date: LocalDate): Long? {
        if (goal.status == JarStatus.COMPLETED || goal.status == JarStatus.CANCELLED) return null
        val accountId = goal.sourceAccountId ?: return null
        val category = categoryId ?: goal.categoryId ?: return null
        val account = dao.allAccounts().firstOrNull { it.id == accountId }?.toCore() ?: return null
        if (account.currency != goal.currency) return null
        val spent = amount ?: goal.target
        val txnId = dao.upsertTxn(Txn(0, TxnKind.EXPENSE, spent, accountId, null, category, date, goal.name).toEntity())
        dao.upsertContribution(
            ContributionEntity(0, goal.id, spent, date.toString(), ContributionType.SETTLEMENT.name, txnId, null),
        )
        dao.setGoalCompletedAt(goal.id, date.toString())
        return txnId
    }

    suspend fun exportBackupJson(): String = db.withTransaction {
        val data = BackupData(
            accounts = dao.allAccounts(),
            categories = dao.allCategories(),
            txns = dao.allTxns(),
            budgets = dao.allBudgets(),
            goals = dao.allGoals(),
            contributions = dao.allContributions(),
        )
        data.toJson(exportedAt = java.time.LocalDateTime.now().withNano(0).toString())
    }

    suspend fun importBackupJson(json: String): List<String> {
        val errors = validateBackupJson(json)
        if (errors.isNotEmpty()) return errors
        val data = try {
            parseBackup(jsonToTree(json))
        } catch (e: Exception) {
            return listOf("Tệp sao lưu bị lỗi: ${e.message}")
        }
        db.withTransaction {
            dao.clearContributions()
            dao.clearGoals()
            dao.clearBudgets()
            dao.clearTxns()
            dao.clearCategories()
            dao.clearAccounts()

            for (a in data.accounts) dao.upsertAccount(a)
            for (c in data.categories) dao.upsertCategory(c)
            for (t in data.txns) dao.upsertTxn(t)
            for (b in data.budgets) dao.upsertBudget(b)
            for (g in data.goals) dao.upsertGoal(g)
            for (c in data.contributions) dao.upsertContribution(c)
        }
        return emptyList()
    }
}
