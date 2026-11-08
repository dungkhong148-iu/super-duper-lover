package com.superduperlover.finance.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.BudgetStatus
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.budgetUsage
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.incomeColor
import com.superduperlover.finance.ui.theme.ChiLiTheme
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
internal fun amberColor(): Color = ChiLiTheme.colors.warn

@Composable
internal fun budgetStatusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.OK -> incomeColor()
    BudgetStatus.WARN -> amberColor()
    BudgetStatus.OVER -> expenseColor()
}

internal fun BudgetStatus.statusLabel(): String = when (this) {
    BudgetStatus.OK -> "Ổn"
    BudgetStatus.WARN -> "Sắp hết"
    BudgetStatus.OVER -> "Vượt mức"
}

@Composable
fun BudgetsScreen() {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()

    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(monthStr) { YearMonth.parse(monthStr) }

    val budgets by remember(repo, month) { repo.budgets(month) }.collectAsState(initial = emptyList())
    val txns by remember(repo, month) { repo.txns(month = month) }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val categories by remember(repo) { repo.categories(CategoryKind.EXPENSE, includeArchived = true) }.collectAsState(initial = emptyList())

    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }

    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    var copyMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(monthStr) {
        copyMessage = null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { monthStr = month.minusMonths(1).toString() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Tháng trước",
                )
            }
            OneLineText(
                text = "Tháng ${month.monthValue}/${month.year}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { monthStr = month.plusMonths(1).toString() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Tháng sau",
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (budgets.isEmpty()) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_savings),
                    title = "Hãy tạo ngân sách đầu tiên",
                    message = "Đặt hạn mức để biết mỗi tháng còn được tiêu bao nhiêu.",
                    actionLabel = "Tạo ngân sách",
                    onAction = {
                        editingBudget = Budget(id = 0L, month = month, accountId = null, categoryId = null, limit = 0L)
                    },
                )
            } else {
                for (budget in budgets) {
                    val usage = budgetUsage(budget, txns, accounts)
                    val statusColor = budgetStatusColor(usage.status)
                    val accountName = budget.accountId?.let { accountsById[it]?.name ?: "?" } ?: "Tất cả tài khoản (${budget.currency.name})"
                    val categoryName = budget.categoryId?.let { categoriesById[it]?.name ?: "?" } ?: "Tất cả danh mục"

                    SectionCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingBudget = budget },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            OneLineText(
                                text = "$accountName · $categoryName",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(8.dp))
                            OneLineText(
                                text = usage.status.statusLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { usage.ratio.toFloat().coerceIn(0f, 1f) },
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        OneLineText(
                            text = "Đã chi ${formatMoney(usage.spent, budget.currency)} / ${formatMoney(usage.limit, budget.currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OneLineText(
                            text = if (usage.remaining >= 0) {
                                "Còn lại ${formatMoney(usage.remaining, budget.currency)}"
                            } else {
                                "Vượt ${formatMoney(-usage.remaining, budget.currency)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (usage.remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else expenseColor(),
                        )
                    }
                }
            }

            Button(
                onClick = {
                    editingBudget = Budget(
                        id = 0L,
                        month = month,
                        accountId = null,
                        categoryId = null,
                        limit = 0L,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                OneLineText("Thêm ngân sách", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val lastMonth = month.minusMonths(1)
                        val lastMonthBudgets = repo.budgets(lastMonth).first()
                        if (lastMonthBudgets.isEmpty()) {
                            copyMessage = "Tháng trước chưa có ngân sách"
                            return@launch
                        }
                        val currentBudgets = repo.budgets(month).first()
                        val toCopy = lastMonthBudgets.filter { last ->
                            currentBudgets.none { curr ->
                                curr.accountId == last.accountId && curr.categoryId == last.categoryId
                            }
                        }
                        if (toCopy.isEmpty()) {
                            copyMessage = "Đã có đủ ngân sách"
                        } else {
                            copyMessage = null
                            for (b in toCopy) {
                                repo.saveBudget(b.copy(id = 0L, month = month))
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                OneLineText("Sao chép từ tháng trước", style = MaterialTheme.typography.labelLarge)
            }

            copyMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
    }

    editingBudget?.let { budget ->
        BudgetSheet(
            budget = budget,
            existingBudgets = budgets,
            accounts = accounts,
            categories = categories,
            onDismiss = { editingBudget = null },
        )
    }
}
