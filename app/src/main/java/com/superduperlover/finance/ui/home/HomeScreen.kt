package com.superduperlover.finance.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.BudgetStatus
import com.superduperlover.finance.core.BudgetUsage
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.accountBalance
import com.superduperlover.finance.core.budgetUsage
import com.superduperlover.finance.core.formatVnd
import com.superduperlover.finance.core.goalProgress
import com.superduperlover.finance.core.txnTotals
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.budgets.budgetStatusColor
import com.superduperlover.finance.ui.budgets.statusLabel
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.incomeColor
import com.superduperlover.finance.ui.goals.GoalProgressContent
import java.time.LocalDate
import java.time.YearMonth

private const val NEAREST_GOALS = 3

/**
 * Home dashboard. Every number comes from `:core` (budgetUsage, accountBalance, goalProgress, txnTotals).
 * "Còn được tiêu" = sum of `remaining` of this month's budgets that have no category.
 */
@Composable
fun HomeScreen(
    onOpenBudgets: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenGoal: (Long) -> Unit,
) {
    val repo = LocalRepo.current
    val month = remember { YearMonth.now() }
    val budgets by remember(repo, month) { repo.budgets(month) }.collectAsState(initial = emptyList())
    val monthTxns by remember(repo, month) { repo.txns(month = month) }.collectAsState(initial = emptyList())
    val allTxns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts() }.collectAsState(initial = emptyList())
    val allAccounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val categories by remember(repo) { repo.categories(includeArchived = true) }.collectAsState(initial = emptyList())
    val goals by remember(repo) { repo.goals() }.collectAsState(initial = emptyList())

    val usages = budgets.map { it to budgetUsage(it, monthTxns) }
    val totalBudgets = usages.filter { (b, _) -> b.categoryId == null }
    val left = totalBudgets.sumOf { (_, u) -> u.remaining }
    val attention = usages
        .filter { (_, u) -> u.status != BudgetStatus.OK }
        .sortedWith(compareByDescending<Pair<Budget, BudgetUsage>> { it.second.status.ordinal }.thenByDescending { it.second.ratio })
    val totals = txnTotals(monthTxns)
    val balances = accounts.map { it to accountBalance(it, allTxns) }
    val accountNames = allAccounts.associate { it.id to it.name }
    val categoryNames = categories.associate { it.id to it.name }
    val nearestGoals = goals
        .sortedWith(compareBy<Goal> { it.deadline == null }.thenBy { it.deadline }.thenBy { it.id })
        .take(NEAREST_GOALS)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "left") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Còn được tiêu tháng này (${month.monthValue}/${month.year})",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (totalBudgets.isEmpty()) {
                        Text(
                            "Chưa có ngân sách tổng cho tháng này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onOpenBudgets, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Tạo ngân sách")
                        }
                    } else {
                        Text(
                            formatVnd(left),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (left < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                        )
                        if (left < 0) {
                            Text("Đã vượt ngân sách", color = expenseColor(), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Thu nhập +${formatVnd(totals.income)}", color = incomeColor(), style = MaterialTheme.typography.bodyMedium)
                        Text("Chi tiêu -${formatVnd(totals.expense)}", color = expenseColor(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (attention.isNotEmpty()) {
            item(key = "attention-title") { SectionTitle("Ngân sách cần chú ý") }
            items(attention, key = { "attention-${it.first.id}" }) { (budget, usage) ->
                val account = budget.accountId?.let { accountNames[it] ?: "?" } ?: "Tất cả tài khoản"
                val category = budget.categoryId?.let { categoryNames[it] ?: "?" } ?: "Tất cả danh mục"
                val color = budgetStatusColor(usage.status)
                Card(Modifier.fillMaxWidth().clickable(onClick = onOpenBudgets)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$account · $category", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(usage.status.statusLabel(), color = color, style = MaterialTheme.typography.labelLarge)
                        }
                        LinearProgressIndicator(
                            progress = { usage.ratio.toFloat().coerceIn(0f, 1f) },
                            color = color,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 8.dp),
                        )
                        Text(
                            "Đã chi ${formatVnd(usage.spent)} / ${formatVnd(usage.limit)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            if (usage.remaining >= 0) "Còn lại ${formatVnd(usage.remaining)}"
                            else "Vượt ${formatVnd(-usage.remaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item(key = "accounts-title") { SectionTitle("Tài khoản") }
        item(key = "accounts") {
            Card(Modifier.fillMaxWidth().clickable(onClick = onOpenAccounts)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (balances.isEmpty()) {
                        Text("Chưa có tài khoản", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = onOpenAccounts, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Thêm tài khoản")
                        }
                    } else {
                        balances.forEach { (account, balance) ->
                            Row(Modifier.heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(account.name, modifier = Modifier.weight(1f))
                                Text(
                                    formatVnd(balance),
                                    color = if (balance < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tổng", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(formatVnd(balances.sumOf { it.second }), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }

        if (nearestGoals.isNotEmpty()) {
            item(key = "goals-title") { SectionTitle("Mục tiêu gần nhất") }
            items(nearestGoals, key = { "goal-${it.id}" }) { goal ->
                HomeGoalCard(goal = goal, onOpen = { onOpenGoal(goal.id) })
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun HomeGoalCard(goal: Goal, onOpen: () -> Unit) {
    val repo = LocalRepo.current
    val contributions by remember(repo, goal.id) { repo.contributions(goal.id) }.collectAsState(initial = emptyList())
    val progress = goalProgress(goal, contributions, LocalDate.now())
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(goal.name, style = MaterialTheme.typography.titleMedium)
            GoalProgressContent(goal = goal, progress = progress)
        }
    }
}
