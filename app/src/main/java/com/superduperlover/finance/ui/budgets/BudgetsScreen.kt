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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.BudgetPeriod
import com.superduperlover.finance.core.BudgetStatus
import com.superduperlover.finance.core.BudgetUsage
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
import java.time.LocalDate
import kotlin.math.abs

@Composable
internal fun budgetStatusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.OK -> ChiLiTheme.colors.income
    BudgetStatus.WARN -> ChiLiTheme.colors.warn
    BudgetStatus.OVER -> ChiLiTheme.colors.over
}

internal fun BudgetStatus.statusLabel(): String = when (this) {
    BudgetStatus.OK -> "Ổn"
    BudgetStatus.WARN -> "Sắp hết"
    BudgetStatus.OVER -> "Vượt mức"
}

internal fun BudgetPeriod.label(): String = when (this) {
    BudgetPeriod.DAY -> "Ngày"
    BudgetPeriod.WEEK -> "Tuần"
    BudgetPeriod.MONTH -> "Tháng"
    BudgetPeriod.QUARTER -> "Quý"
    BudgetPeriod.YEAR -> "Năm"
}

internal fun BudgetPeriod.recurringLabel(): String = when (this) {
    BudgetPeriod.DAY -> "Hằng ngày"
    BudgetPeriod.WEEK -> "Hằng tuần"
    BudgetPeriod.MONTH -> "Hằng tháng"
    BudgetPeriod.QUARTER -> "Hằng quý"
    BudgetPeriod.YEAR -> "Hằng năm"
}

internal fun BudgetPeriod.adverbLabel(): String = when (this) {
    BudgetPeriod.DAY -> "hằng ngày"
    BudgetPeriod.WEEK -> "hằng tuần"
    BudgetPeriod.MONTH -> "hằng tháng"
    BudgetPeriod.QUARTER -> "hằng quý"
    BudgetPeriod.YEAR -> "hằng năm"
}

@Composable
fun BudgetsScreen() {
    val repo = LocalRepo.current

    val today = remember { LocalDate.now() }
    val budgets by remember(repo) { repo.budgets() }.collectAsState(initial = emptyList())
    // Repeating budgets need every expense since their start date (periods roll over from the start).
    val txns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val categories by remember(repo) { repo.categories(CategoryKind.EXPENSE, includeArchived = true) }.collectAsState(initial = emptyList())

    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }

    var editingBudget by remember { mutableStateOf<Budget?>(null) }

    val sortedBudgetsWithUsage = remember(budgets, txns, accounts, today) {
        budgets.map { budget ->
            budget to budgetUsage(budget, txns, accounts, today)
        }.sortedWith(
            compareBy<Pair<Budget, BudgetUsage>> { (_, usage) ->
                when (usage.status) {
                    BudgetStatus.OVER -> 0
                    BudgetStatus.WARN -> 1
                    BudgetStatus.OK -> 2
                }
            }.thenBy { (budget, _) -> budget.id }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                    message = "Đặt hạn mức theo ngày, tuần, tháng, quý hoặc năm để biết còn được tiêu bao nhiêu.",
                    actionLabel = "Tạo ngân sách",
                    onAction = {
                        editingBudget = newBudget(today)
                    },
                    emphasized = true,
                )
            } else {
                for ((budget, usage) in sortedBudgetsWithUsage) {
                    val statusColor = budgetStatusColor(usage.status)
                    val accountName = budget.accountId?.let { accountsById[it]?.name ?: "?" } ?: "Tất cả tài khoản (${budget.currency.name})"
                    val categoryName = budget.categoryId?.let { categoriesById[it]?.name ?: "?" } ?: "Tất cả danh mục"

                    val range = usage.range
                    val startStr = "%02d/%02d".format(range.start.dayOfMonth, range.start.monthValue)
                    val endStr = "%02d/%02d".format(range.endInclusive.dayOfMonth, range.endInclusive.monthValue)

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
                        OneLineText(
                            text = "${budget.period.recurringLabel()} · $startStr – $endStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                            text = "Đã chi ${formatMoney(usage.spent, budget.currency)} / ${formatMoney(usage.effectiveLimit, budget.currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (usage.carried != 0L) {
                            val sign = if (usage.carried > 0) "+" else "-"
                            val carriedStr = "$sign${formatMoney(abs(usage.carried), budget.currency)}"
                            OneLineText(
                                text = "Hạn mức ${formatMoney(budget.limit, budget.currency)} · chuyển từ kỳ trước $carriedStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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

                FilledTonalButton(
                    onClick = {
                        editingBudget = newBudget(today)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    OneLineText("Thêm ngân sách", style = MaterialTheme.typography.labelLarge)
                }
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

/** A new budget: repeats monthly from the 1st of the current month, no rollover (the V2 form lets the user change this). */
internal fun newBudget(today: LocalDate) = Budget(
    id = 0L,
    period = BudgetPeriod.MONTH,
    startDate = today.withDayOfMonth(1),
    accountId = null,
    categoryId = null,
    limit = 0L,
)
