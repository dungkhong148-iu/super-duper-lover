package com.superduperlover.finance.ui.budgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.budgetPace
import com.superduperlover.finance.core.budgetUsage
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.dayMonth
import com.superduperlover.finance.ui.home.moneyText
import com.superduperlover.finance.ui.theme.ChiLiTheme
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.v3Shadow
import java.time.LocalDate
import kotlin.math.abs

@Composable
internal fun budgetStatusColor(status: BudgetStatus): Color {
    val c = ChiLiV3.colors
    return when (status) {
        BudgetStatus.OK -> c.income
        BudgetStatus.WARN -> ChiLiTheme.colors.warn
        BudgetStatus.OVER -> c.expense
    }
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

@Composable
fun BudgetsScreen() {
    val repo = LocalRepo.current
    val prefs = LocalAppPrefs.current
    val c = ChiLiV3.colors

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

    val totalEntries = sortedBudgetsWithUsage.filter { (b, _) -> b.categoryId == null && b.currency == MAIN_CURRENCY }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val totalShape = RoundedCornerShape(26.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .v3Shadow(c.shadow3, totalShape)
                    .clip(totalShape)
                    .background(Brush.linearGradient(listOf(c.headerTop, c.headerBottom)))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (totalEntries.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Tổng ngân sách",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.onHeader,
                        )
                    }
                    Text(
                        text = "Chưa có ngân sách tổng",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onHeader,
                    )
                } else {
                    val totalSpent = totalEntries.sumOf { it.second.spent }
                    val totalLimit = totalEntries.sumOf { it.second.limit }
                    val totalRemaining = totalEntries.sumOf { it.second.remaining }
                    val totalCarried = totalEntries.sumOf { it.second.carried }
                    val effectiveLimit = totalLimit + totalCarried
                    val ratio = if (effectiveLimit > 0) {
                        totalSpent.toDouble() / effectiveLimit.toDouble()
                    } else if (totalSpent == 0L) {
                        0.0
                    } else {
                        1.0
                    }
                    val statuses = totalEntries.map { it.second.status }
                    val worstStatus = when {
                        BudgetStatus.OVER in statuses -> BudgetStatus.OVER
                        BudgetStatus.WARN in statuses -> BudgetStatus.WARN
                        else -> BudgetStatus.OK
                    }
                    val totalUsage = BudgetUsage(
                        spent = totalSpent,
                        limit = totalLimit,
                        remaining = totalRemaining,
                        ratio = ratio,
                        status = worstStatus,
                        range = totalEntries.first().second.range,
                        carried = totalCarried,
                    )
                    val pace = budgetPace(totalUsage, today)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Tổng ngân sách",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.onHeader,
                        )
                        val paceShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .clip(paceShape)
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (pace.onPace) "Đúng tiến độ" else "Chi tiêu nhanh",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.onHeader,
                            )
                        }
                    }

                    val remainingPrefix = if (totalUsage.remaining >= 0) "Còn lại " else "Vượt "
                    val displayRemaining = if (totalUsage.remaining >= 0) totalUsage.remaining else -totalUsage.remaining
                    Text(
                        text = "$remainingPrefix${moneyText(displayRemaining, MAIN_CURRENCY, prefs.hideBalance)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.onHeader,
                    )

                    Text(
                        text = "Chi tiêu an toàn: ${moneyText(pace.safePerDay, MAIN_CURRENCY, prefs.hideBalance)} / ngày (còn ${pace.daysLeft} ngày)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onHeader,
                    )

                    val progressVal = totalUsage.ratio.toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressVal },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = c.onHeader,
                        trackColor = Color(0x40FFFFFF),
                        strokeCap = StrokeCap.Round,
                    )

                    val percentInt = (progressVal * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${moneyText(totalUsage.spent, MAIN_CURRENCY, prefs.hideBalance)} / ${moneyText(totalUsage.effectiveLimit, MAIN_CURRENCY, prefs.hideBalance)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.onHeader,
                        )
                        Text(
                            text = "$percentInt%",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.onHeader,
                        )
                    }
                }
            }

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
                    val startStr = range.start.dayMonth()
                    val endStr = range.endInclusive.dayMonth()

                    val cardShape = RoundedCornerShape(24.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .v3Shadow(c.shadow2, cardShape)
                            .clip(cardShape)
                            .background(c.cardActive)
                            .border(1.dp, c.cardBorder, cardShape)
                            .clickable { editingBudget = budget }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            OneLineText(
                                text = "$accountName · $categoryName",
                                style = MaterialTheme.typography.titleMedium,
                                color = c.onCard,
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
                            color = c.onSheetMuted,
                        )
                        LinearProgressIndicator(
                            progress = { usage.ratio.toFloat().coerceIn(0f, 1f) },
                            color = statusColor,
                            trackColor = c.card,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        OneLineText(
                            text = "Đã chi ${moneyText(usage.spent, budget.currency, prefs.hideBalance)} / ${moneyText(usage.effectiveLimit, budget.currency, prefs.hideBalance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.onCard,
                        )
                        if (usage.carried != 0L) {
                            val sign = if (usage.carried > 0) "+" else "-"
                            val carriedStr = "$sign${moneyText(abs(usage.carried), budget.currency, prefs.hideBalance)}"
                            OneLineText(
                                text = "Hạn mức ${moneyText(budget.limit, budget.currency, prefs.hideBalance)} · chuyển từ kỳ trước $carriedStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.onSheetMuted,
                            )
                        }
                        OneLineText(
                            text = if (usage.remaining >= 0) {
                                "Còn lại ${moneyText(usage.remaining, budget.currency, prefs.hideBalance)}"
                            } else {
                                "Vượt ${moneyText(-usage.remaining, budget.currency, prefs.hideBalance)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (usage.remaining >= 0) c.onSheetMuted else c.expense,
                        )
                    }
                }

                Button(
                    onClick = {
                        editingBudget = newBudget(today)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.cardActive,
                        contentColor = c.accent,
                    ),
                    border = BorderStroke(1.dp, c.cardBorder),
                ) {
                    Text(
                        text = "Thêm ngân sách",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.accent,
                    )
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
