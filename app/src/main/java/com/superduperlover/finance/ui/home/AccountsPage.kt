package com.superduperlover.finance.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.BudgetStatus
import com.superduperlover.finance.core.BudgetUsage
import com.superduperlover.finance.core.accountBalance
import com.superduperlover.finance.core.budgetUsage
import com.superduperlover.finance.core.CURRENCY_DISPLAY_ORDER
import com.superduperlover.finance.core.balancesByCurrency
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.txnTotals
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.accounts.AccountSheet
import com.superduperlover.finance.ui.budgets.budgetStatusColor
import com.superduperlover.finance.ui.budgets.statusLabel
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.incomeColor
import java.time.YearMonth

private val FullWidth: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan) =
    { GridItemSpan(maxLineSpan) }

/**
 * "Tài khoản" page of the home tab (docs/ui-layout.md section 1): summary card, account tile grid,
 * "Ngân sách cần chú ý". Every number comes from `:core` (accountBalance, budgetUsage, txnTotals).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsPage(
    onOpenAccountTransactions: (Long) -> Unit,
    onOpenPlan: () -> Unit,
) {
    val repo = LocalRepo.current
    val month = remember { YearMonth.now() }
    val budgets by remember(repo, month) { repo.budgets(month) }.collectAsState(initial = emptyList())
    val monthTxns by remember(repo, month) { repo.txns(month = month) }.collectAsState(initial = emptyList())
    val allTxns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts() }.collectAsState(initial = emptyList())
    val allAccounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val categories by remember(repo) { repo.categories(includeArchived = true) }.collectAsState(initial = emptyList())
    // null = sheet closed; id 0 = new account
    var editing by remember { mutableStateOf<Account?>(null) }

    val usages = budgets.map { it to budgetUsage(it, monthTxns, allAccounts) }
    val totalBudgets = usages.filter { (b, _) -> b.categoryId == null }
    // Money left to spend, per currency (currencies are never added together).
    val leftByCurrency = CURRENCY_DISPLAY_ORDER.mapNotNull { currency ->
        val inCurrency = totalBudgets.filter { (b, _) -> b.currency == currency }
        if (inCurrency.isEmpty()) null else currency to inCurrency.sumOf { (_, u) -> u.remaining }
    }
    val attention = usages
        .filter { (_, u) -> u.status != BudgetStatus.OK }
        .sortedWith(
            compareByDescending<Pair<Budget, BudgetUsage>> { it.second.status.ordinal }
                .thenByDescending { it.second.ratio },
        )
    val totals = txnTotals(monthTxns, accounts)
    val balanceByCurrency = balancesByCurrency(accounts, allTxns)
    val balances = accounts.map { it to accountBalance(it, allTxns) }
    val accountNames = allAccounts.associate { it.id to it.name }
    val categoryNames = categories.associate { it.id to it.name }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "summary", span = FullWidth) {
            SectionCard {
                Text(
                    "Tổng số dư",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    balanceByCurrency.forEach { (currency, total) ->
                        OneLineText(
                            text = formatMoney(total, currency),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (total < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OneLineText(
                    text = "Còn được tiêu tháng này (${month.monthValue}/${month.year})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (leftByCurrency.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OneLineText(
                            "Chưa có ngân sách tổng",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onOpenPlan, modifier = Modifier.heightIn(min = 48.dp)) {
                            OneLineText("Tạo ngân sách", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        leftByCurrency.forEach { (currency, left) ->
                            OneLineText(
                                text = formatMoney(left, currency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (left < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (leftByCurrency.any { it.second < 0 }) {
                        Text("Đã vượt ngân sách", color = expenseColor(), style = MaterialTheme.typography.labelMedium)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        OneLineText("Thu nhập", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        totals.forEach { (currency, t) ->
                            OneLineText("+${formatMoney(t.income, currency)}", style = MaterialTheme.typography.titleSmall, color = incomeColor())
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        OneLineText("Chi tiêu", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        totals.forEach { (currency, t) ->
                            OneLineText("-${formatMoney(t.expense, currency)}", style = MaterialTheme.typography.titleSmall, color = expenseColor())
                        }
                    }
                }
            }
        }

        if (balances.isEmpty()) {
            item(key = "no-accounts", span = FullWidth) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_wallet),
                    title = "Chưa có tài khoản",
                    message = "Thêm ví, ngân hàng hoặc thẻ để bắt đầu ghi chép.",
                    actionLabel = "Thêm tài khoản",
                    onAction = { editing = Account(0, "", AccountType.CASH, 0) },
                )
            }
        } else {
            items(balances, key = { "account-${it.first.id}" }) { (account, balance) ->
                AccountTile(
                    account = account,
                    balance = balance,
                    onClick = { onOpenAccountTransactions(account.id) },
                    onLongClick = { editing = account },
                )
            }
            item(key = "add-account") {
                AddAccountTile(onClick = { editing = Account(0, "", AccountType.CASH, 0) })
            }
        }

        if (attention.isNotEmpty()) {
            item(key = "attention", span = FullWidth) {
                SectionCard(title = "Ngân sách cần chú ý", onAction = onOpenPlan) {
                    attention.forEach { (budget, usage) ->
                        val account = budget.accountId?.let { accountNames[it] ?: "?" } ?: "Tất cả tài khoản (${budget.currency.name})"
                        val category = budget.categoryId?.let { categoryNames[it] ?: "?" } ?: "Tất cả danh mục"
                        val color = budgetStatusColor(usage.status)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OneLineText(
                                    "$account · $category",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(usage.status.statusLabel(), color = color, style = MaterialTheme.typography.labelLarge)
                            }
                            LinearProgressIndicator(
                                progress = { usage.ratio.toFloat().coerceIn(0f, 1f) },
                                color = color,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 8.dp),
                            )
                            OneLineText(
                                text = if (usage.remaining >= 0) {
                                    "Đã chi ${formatMoney(usage.spent, budget.currency)} / ${formatMoney(usage.limit, budget.currency)}"
                                } else {
                                    "Vượt ${formatMoney(-usage.remaining, budget.currency)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { account ->
        AccountSheet(account = account, onDismiss = { editing = null })
    }
}
