package com.superduperlover.finance.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.BackupReminder
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.BudgetStatus
import com.superduperlover.finance.core.BudgetUsage
import com.superduperlover.finance.core.CURRENCY_DISPLAY_ORDER
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.TxnTotals
import com.superduperlover.finance.core.accountBalance
import com.superduperlover.finance.core.backupReminder
import com.superduperlover.finance.core.balancesByCurrency
import com.superduperlover.finance.core.budgetUsage
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
import com.superduperlover.finance.ui.components.icon
import com.superduperlover.finance.ui.components.incomeColor
import com.superduperlover.finance.ui.components.thisLabel
import com.superduperlover.finance.ui.settings.rememberBackupExport
import com.superduperlover.finance.ui.theme.ChiLiTheme
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.chiLiChipBorder
import com.superduperlover.finance.ui.theme.chiLiChipColors
import java.time.LocalDate
import java.time.YearMonth

private val FullWidth: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan) =
    { GridItemSpan(maxLineSpan) }

/**
 * "Tài khoản" page of the home tab (docs/ui-layout.md section 1 & 2b): summary card, account tile grid,
 * "Ngân sách cần chú ý", and goal card. Every number comes from `:core` (accountBalance, budgetUsage, txnTotals).
 */
@Composable
fun AccountsPage(
    onOpenAccountTransactions: (Long) -> Unit,
    onOpenPlan: () -> Unit,
) {
    val repo = LocalRepo.current
    val prefs = LocalAppPrefs.current
    val month = remember { YearMonth.now() }
    val today = remember { LocalDate.now() }
    val budgets by remember(repo) { repo.budgets() }.collectAsState(initial = emptyList())
    val goals by remember(repo) { repo.goals() }.collectAsState(initial = emptyList())
    val monthTxns by remember(repo, month) { repo.txns(month = month) }.collectAsState(initial = emptyList())
    val allTxns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts() }.collectAsState(initial = emptyList())
    val allAccounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val categories by remember(repo) { repo.categories(includeArchived = true) }.collectAsState(initial = emptyList())
    // null = sheet closed; id 0 = new account
    var editing by remember { mutableStateOf<Account?>(null) }

    val availableCurrencies = CURRENCY_DISPLAY_ORDER.filter { currency ->
        accounts.any { it.currency == currency }
    }
    val selectedCurrency = when {
        prefs.summaryCurrency in availableCurrencies -> prefs.summaryCurrency!!
        MAIN_CURRENCY in availableCurrencies -> MAIN_CURRENCY
        availableCurrencies.isNotEmpty() -> availableCurrencies.first()
        else -> MAIN_CURRENCY
    }

    // budgets repeat every period: their usage needs every expense since the start date
    val usages = budgets.map { it to budgetUsage(it, allTxns, allAccounts, today) }
    val totalBudgets = usages.filter { (b, _) -> b.categoryId == null }
    val currencyTotalBudgets = totalBudgets.filter { (b, _) -> b.currency == selectedCurrency }
    val periodLabel = currencyTotalBudgets.map { (b, _) -> b.period }.distinct().singleOrNull()?.thisLabel() ?: "kỳ này"

    val attention = usages
        .filter { (_, u) -> u.status != BudgetStatus.OK }
        .sortedWith(
            compareByDescending<Pair<Budget, BudgetUsage>> { it.second.status.ordinal }
                .thenByDescending { it.second.ratio },
        )
    val totals = txnTotals(monthTxns, accounts)
    val selectedTotals = totals[selectedCurrency] ?: TxnTotals(0L, 0L)
    val balanceByCurrency = balancesByCurrency(accounts, allTxns)
    val selectedBalance = balanceByCurrency[selectedCurrency] ?: 0L
    val balances = accounts.map { it to accountBalance(it, allTxns) }
    val accountNames = allAccounts.associate { it.id to it.name }
    val categoryNames = categories.associate { it.id to it.name }

    var backupWorking by remember { mutableStateOf(false) }
    var backupError by remember { mutableStateOf<String?>(null) }
    val startBackup = rememberBackupExport(
        onWorking = { backupWorking = it },
        onMessage = { backupError = it.takeIf { m -> m.startsWith("Xuất thất bại") } },
    )
    val reminder = backupReminder(today, prefs.lastBackupAt, prefs.txnsSinceBackup)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (reminder != null) {
            item(key = "backup-reminder", span = FullWidth) {
                BackupReminderCard(reminder, enabled = !backupWorking, error = backupError, onClick = startBackup)
            }
        }
        item(key = "summary", span = FullWidth) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ChiLiTheme.colors.outline),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Row 1: "Tổng số dư" left, currency chips right (when >1 currency)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OneLineText(
                            text = "Tổng số dư",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (availableCurrencies.size > 1) {
                            Row(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(start = 8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                availableCurrencies.forEach { currency ->
                                    FilterChip(
                                        selected = currency == selectedCurrency,
                                        onClick = { prefs.summaryCurrency = currency },
                                        label = {
                                            OneLineText(
                                                text = currency.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.widthIn(max = 200.dp),
                                            )
                                        },
                                        colors = chiLiChipColors(),
                                        border = chiLiChipBorder(selected = currency == selectedCurrency),
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: big balance (OneLineText, 28sp max / 20sp min, Bold, onSurface or expense if negative)
                    OneLineText(
                        text = formatMoney(selectedBalance, selectedCurrency),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                        minFontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBalance < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                    )

                    // Nested box (colorScheme.surfaceContainerLowest well, 16dp corner, 12dp padding, full width)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            OneLineText(
                                text = "Còn được tiêu $periodLabel",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                minFontSize = 10.sp,
                            )
                            if (currencyTotalBudgets.isEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OneLineText(
                                        text = "Chưa có ngân sách tổng",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(
                                        onClick = onOpenPlan,
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        OneLineText("Tạo ngân sách", style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            } else {
                                val left = currencyTotalBudgets.sumOf { (_, u) -> u.remaining }
                                val valueStyle = MaterialTheme.typography.titleLarge.let {
                                    if (it.fontSize.isSpecified && it.fontSize > 18.sp) it.copy(fontSize = 18.sp) else it
                                }
                                OneLineText(
                                    text = formatMoney(left, selectedCurrency),
                                    style = valueStyle,
                                    fontWeight = FontWeight.Bold,
                                    color = if (left < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                                )
                                if (left < 0) {
                                    OneLineText(
                                        text = "Đã vượt ngân sách",
                                        color = expenseColor(),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }

                    // Row 3: two tiles side by side (weight 1f, 8dp gap), each a nested tile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Thu nhập tile
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(ChiLiTheme.colors.incomeContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = TxnKind.INCOME.icon(),
                                        contentDescription = null,
                                        tint = ChiLiTheme.colors.income,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    OneLineText(
                                        text = "Thu nhập",
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        minFontSize = 10.sp,
                                    )
                                    OneLineText(
                                        text = "+${formatMoney(selectedTotals.income, selectedCurrency)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = incomeColor(),
                                        minFontSize = 10.sp,
                                    )
                                }
                            }
                        }

                        // Chi tiêu tile
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(ChiLiTheme.colors.expenseContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = TxnKind.EXPENSE.icon(),
                                        contentDescription = null,
                                        tint = ChiLiTheme.colors.onExpenseContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    OneLineText(
                                        text = "Chi tiêu",
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        minFontSize = 10.sp,
                                    )
                                    OneLineText(
                                        text = "-${formatMoney(selectedTotals.expense, selectedCurrency)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = expenseColor(),
                                        minFontSize = 10.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "accounts-title", span = FullWidth) {
            val titleStyle = MaterialTheme.typography.titleMedium.let {
                if (it.fontSize.isSpecified && it.fontSize > 18.sp) it.copy(fontSize = 18.sp) else it
            }
            OneLineText(
                text = "Ví & Tài khoản",
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
            )
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
                                OneLineText(
                                    usage.status.statusLabel(),
                                    color = color,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { usage.ratio.toFloat().coerceIn(0f, 1f) },
                                color = color,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 8.dp),
                            )
                            OneLineText(
                                text = if (usage.remaining >= 0) {
                                    "Đã chi ${formatMoney(usage.spent, budget.currency)} / ${formatMoney(usage.effectiveLimit, budget.currency)}"
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

        item(key = "goals-card", span = FullWidth) {
            val activeGoals = goals.count { it.completedAt == null }
            val subtitle = if (activeGoals == 0) "Thiết lập mục tiêu đầu tiên của bạn" else "$activeGoals mục tiêu đang chạy"
            val titleStyle = MaterialTheme.typography.titleMedium.let {
                if (it.fontSize.isSpecified && it.fontSize > 18.sp) it.copy(fontSize = 18.sp) else it
            }
            Surface(
                onClick = onOpenPlan,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ChiLiTheme.colors.outline),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_flag),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        OneLineText(
                            text = "Mục tiêu tiết kiệm",
                            style = titleStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        OneLineText(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            minFontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }

    editing?.let { account ->
        AccountSheet(account = account, onDismiss = { editing = null })
    }
}

/** Passive reminder (docs/ui-layout.md section 2, cards): tap to export a backup file. */
@Composable
private fun BackupReminderCard(
    reminder: BackupReminder,
    enabled: Boolean,
    error: String?,
    onClick: () -> Unit,
) {
    val since = reminder.daysSince?.let { "Đã $it ngày chưa sao lưu" } ?: "Chưa sao lưu lần nào"
    val text = "$since (${reminder.newTxns} giao dịch mới) — chạm để xuất file"
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ChiLiTheme.colors.warn),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
