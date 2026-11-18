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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.layout
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
import com.superduperlover.finance.core.JarSaving
import com.superduperlover.finance.core.JarStatus
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.TxRange
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.TxnTotals
import com.superduperlover.finance.core.accountBalance
import com.superduperlover.finance.core.backupReminder
import com.superduperlover.finance.core.balancesByCurrency
import com.superduperlover.finance.core.debtTotals
import com.superduperlover.finance.core.netWorthByCurrency
import com.superduperlover.finance.core.budgetUsage
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.freeByCurrency
import com.superduperlover.finance.core.inTxRange
import com.superduperlover.finance.core.shiftTxAnchor
import com.superduperlover.finance.core.reservedByCurrency
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

/**
 * "Tài khoản" page of the home tab (docs/ui-layout.md section 1 & 2b): summary card, account tile grid,
 * and "Ngân sách cần chú ý". Every number comes from `:core` (accountBalance, budgetUsage, txnTotals).
 */
@Composable
fun HomeBody(
    pagerState: PagerState,
    selectedId: Long?,
    onSelectedChange: (Long?) -> Unit,
    /** Full list; the id (when an account is selected) opens it filtered to that account. */
    onOpenTransactions: (Long?) -> Unit,
    onEditTxn: (Long) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenGoal: (Long) -> Unit = {},
    onOpenJars: () -> Unit = {},
) {
    val repo = LocalRepo.current
    val prefs = LocalAppPrefs.current
    val month = remember { YearMonth.now() }
    val today = remember { LocalDate.now() }
    val budgets by remember(repo) { repo.budgets() }.collectAsState(initial = emptyList())
    val jarSavings by remember(repo) { repo.jarSavings() }.collectAsState(initial = emptyList())
    val monthTxns by remember(repo, month) { repo.txns(month = month) }.collectAsState(initial = emptyList())
    val allTxns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts() }.collectAsState(initial = emptyList())
    val allAccounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val debts by remember(repo) { repo.debts() }.collectAsState(initial = emptyList())
    val debtPayments by remember(repo) { repo.debtPayments() }.collectAsState(initial = emptyList())
    val categories by remember(repo) { repo.categories(includeArchived = true) }.collectAsState(initial = emptyList())
    // null = sheet closed; id 0 = new account
    var editing by remember { mutableStateOf<Account?>(null) }

    // Home filter: [selectedId] (null = all, held by HomeScreen for the header label). A card that vanished (archived / deleted) means "all".
    val selected = accounts.firstOrNull { it.id == selectedId }
    // v3: Hero in the main currency; a selected account of another currency shows its own.
    val selectedCurrency = selected?.currency ?: MAIN_CURRENCY
    val hidden = prefs.hideBalance
    // Yellow block state; the Thu nhập / Chi tiêu pills of the Hero set typeName.
    var typeName by rememberSaveable { mutableStateOf<String?>(null) }
    var rangeName by rememberSaveable { mutableStateOf(TxRange.MONTH.name) }
    var anchorDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var expanded by rememberSaveable { mutableStateOf(true) }
    val typeFilter = TxnKind.entries.firstOrNull { it.name == typeName }
    val range = TxRange.valueOf(rangeName)
    val anchor = LocalDate.ofEpochDay(anchorDay)

    // budgets repeat every period: their usage needs every expense since the start date
    val usages = budgets.map { it to budgetUsage(it, allTxns, allAccounts, today) }
    // Only the one budget for all accounts + all categories (summing every all-category budget double counts).
    val currencyTotalBudgets = usages
        .filter { (b, _) -> b.accountId == null && b.categoryId == null && b.currency == selectedCurrency }
        .take(1)
    val periodLabel = currencyTotalBudgets.map { (b, _) -> b.period }.distinct().singleOrNull()?.thisLabel() ?: "kỳ này"

    val attention = usages
        .filter { (_, u) -> u.status != BudgetStatus.OK }
        .sortedWith(
            compareByDescending<Pair<Budget, BudgetUsage>> { it.second.status.ordinal }
                .thenByDescending { it.second.ratio },
        )
    val scopeAccounts = if (selected != null) listOf(selected) else accounts
    val totals = txnTotals(if (selected != null) monthTxns.filter { it.accountId == selected.id } else monthTxns, accounts)
    val selectedTotals = totals[selectedCurrency] ?: TxnTotals(0L, 0L)
    val balanceByCurrency = balancesByCurrency(scopeAccounts, allTxns)
    // All accounts: the Hero total is the net worth (balances + lent - owed); one account: just its balance.
    val openDebts = debtTotals(debts, debtPayments)[selectedCurrency]
    val selectedBalance = if (selected == null) {
        netWorthByCurrency(accounts, allTxns, debts, debtPayments)[selectedCurrency] ?: 0L
    } else {
        balanceByCurrency[selectedCurrency] ?: 0L
    }
    val cashBalance = balanceByCurrency[selectedCurrency] ?: 0L
    val reservedMap = reservedByCurrency(jarSavings)
    val freeMap = freeByCurrency(balanceByCurrency, reservedMap)
    val selectedReserved = reservedMap[selectedCurrency] ?: 0L
    val selectedFree = freeMap[selectedCurrency] ?: cashBalance
    val pendingJars = jarSavings.filter { it.goal.status == JarStatus.PENDING_SETTLEMENT && !it.goal.archived }
    val balances = accounts.map { it to accountBalance(it, allTxns) }
    val accountNames = allAccounts.associate { it.id to it.name }
    val categoryNames = categories.associate { it.id to it.name }

    CollapsingSheetPage(
        carousel = {
            AccountCarousel(
                accounts = balances,
                selectedId = selected?.id,
                hidden = hidden,
                onSelect = { onSelectedChange(if (selectedId == it) null else it) },
                onEdit = { editing = it },
                onAdd = { editing = Account(0, "", AccountType.CASH, 0) },
            )
        },
        pagerState = pagerState,
    ) { page, scroll ->
        if (page == 1) {
            CalendarPage(accountId = selected?.id, scroll = scroll)
            return@CollapsingSheetPage
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (balances.isEmpty()) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_wallet),
                    title = "Chưa có tài khoản",
                    message = "Thêm ví, ngân hàng hoặc thẻ để bắt đầu ghi chép.",
                    actionLabel = "Thêm tài khoản",
                    onAction = { editing = Account(0, "", AccountType.CASH, 0) },
                )
            }
            for (jar in pendingJars) {
                PendingJarBanner(jar = jar, hidden = hidden, onClick = { onOpenGoal(jar.goal.id) })
            }
            HeroCard(
                balance = selectedBalance,
                free = selectedFree,
                reserved = selectedReserved,
                budgetLeft = currencyTotalBudgets.takeIf { it.isNotEmpty() }?.sumOf { (_, u) -> u.remaining },
                title = selected?.name ?: "Tổng số dư",
                showFree = selected == null,
                debtOwe = if (selected == null) openDebts?.owe ?: 0L else 0L,
                debtLent = if (selected == null) openDebts?.lent ?: 0L else 0L,
                periodLabel = periodLabel,
                income = selectedTotals.income,
                expense = selectedTotals.expense,
                currency = selectedCurrency,
                typeFilter = typeFilter,
                onTypeFilter = { typeName = if (typeFilter == it) null else it.name },
                hidden = hidden,
                onToggleHidden = { prefs.hideBalance = !prefs.hideBalance },
                onOpenBudgets = onOpenPlan,
                onOpenJars = onOpenJars,
            )

            val listed = allTxns.filter {
                (selected == null || it.accountId == selected.id || it.toAccountId == selected.id) &&
                    (typeFilter == null || it.kind == typeFilter) && inTxRange(it.date, range, anchor)
            }
            YellowBlock(
                txns = listed,
                accounts = allAccounts.associateBy { it.id },
                categories = categories.associateBy { it.id },
                range = range,
                anchor = anchor,
                expanded = expanded,
                typeFilter = typeFilter,
                hidden = hidden,
                onToggle = { expanded = !expanded },
                onRange = { rangeName = it.name },
                onShift = { anchorDay = shiftTxAnchor(range, anchor, it).toEpochDay() },
                onClearFilter = { typeName = null },
                onOpenTxn = onEditTxn,
                onOpenAll = { onOpenTransactions(selected?.id) },
            )

            if (attention.isNotEmpty()) {
                SectionCard(title = "Ngân sách cần chú ý", onAction = onOpenPlan) {
                    attention.forEach { (budget, usage) ->
                        val account = budget.accountId?.let { accountNames[it] ?: "?" } ?: "Tất cả tài khoản (${budget.currency.name})"
                        val category = budget.name ?: budget.categoryId?.let { categoryNames[it] ?: "?" } ?: "Tất cả danh mục"
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
                                    "Đã chi ${moneyText(usage.spent, budget.currency, hidden)} / ${moneyText(usage.effectiveLimit, budget.currency, hidden)}"
                                } else {
                                    "Vượt ${moneyText(-usage.remaining, budget.currency, hidden)}"
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

@Composable
private fun PendingJarBanner(
    jar: JarSaving,
    hidden: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ChiLiTheme.colors.warn),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ChiLiTheme.colors.warn.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_savings),
                    contentDescription = null,
                    tint = ChiLiTheme.colors.warn,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                OneLineText(
                    text = "Hũ đã đủ tiền: ${jar.goal.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Đã tích lũy đủ ${moneyText(jar.saved, jar.goal.currency, hidden)} / ${moneyText(jar.goal.target, jar.goal.currency, hidden)} — chạm để xác nhận chi tiêu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
