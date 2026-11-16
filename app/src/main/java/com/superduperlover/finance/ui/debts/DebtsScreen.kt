package com.superduperlover.finance.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Debt
import com.superduperlover.finance.core.DebtKind
import com.superduperlover.finance.core.DebtPayment
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.debtPaid
import com.superduperlover.finance.core.debtProgress
import com.superduperlover.finance.core.debtRemaining
import com.superduperlover.finance.core.debtTotals
import com.superduperlover.finance.core.isDebtOverdue
import com.superduperlover.finance.core.isDebtSettled
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.dayMonthYear
import com.superduperlover.finance.ui.home.moneyText
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.tapScale
import com.superduperlover.finance.ui.theme.v3Shadow
import java.time.LocalDate

@Composable
fun DebtsTab(modifier: Modifier = Modifier) {
    val repo = LocalRepo.current
    val prefs = LocalAppPrefs.current
    val c = ChiLiV3.colors

    val debts by remember(repo) { repo.debts() }.collectAsState(initial = emptyList())
    val payments by remember(repo) { repo.debtPayments() }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val today = remember { LocalDate.now() }

    var showCreateDebt by remember { mutableStateOf(false) }
    var selectedDebt by remember { mutableStateOf<Debt?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "summary") {
            DebtSummaryCard(
                debts = debts,
                payments = payments,
                hideBalance = prefs.hideBalance,
            )
        }

        if (debts.isNotEmpty()) {
            item(key = "add_btn") {
                val btnShape = RoundedCornerShape(16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .v3Shadow(c.shadow1, btnShape)
                        .clip(btnShape)
                        .background(c.cardActive)
                        .border(1.dp, c.cardBorder, btnShape)
                        .tapScale(onClick = { showCreateDebt = true })
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = c.accent,
                    )
                    Spacer(Modifier.width(8.dp))
                    OneLineText(
                        text = "Thêm khoản nợ",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.accent,
                    )
                }
            }

            items(debts, key = { it.id }) { debt ->
                DebtCard(
                    debt = debt,
                    payments = payments,
                    today = today,
                    hideBalance = prefs.hideBalance,
                    onClick = { selectedDebt = debt },
                )
            }
        } else {
            item(key = "empty") {
                EmptyState(
                    icon = painterResource(R.drawable.ic_wallet),
                    title = "Chưa có khoản nợ nào",
                    message = "Ghi chép các khoản đang nợ hoặc cho mượn để theo dõi tiến độ trả và hạn trả dễ dàng.",
                    actionLabel = "Thêm khoản nợ",
                    onAction = { showCreateDebt = true },
                )
            }
        }
    }

    if (showCreateDebt) {
        CreateDebtDialog(
            onDismiss = { showCreateDebt = false },
            accounts = accounts,
        )
    }

    val activeSelectedDebt = selectedDebt?.let { cur -> debts.firstOrNull { it.id == cur.id } ?: cur }
    if (activeSelectedDebt != null) {
        DebtDetailDialog(
            debt = activeSelectedDebt,
            payments = payments,
            accounts = accounts,
            accountMap = accountMap,
            onDismiss = { selectedDebt = null },
        )
    }
}

@Composable
private fun DebtSummaryCard(
    debts: List<Debt>,
    payments: List<DebtPayment>,
    hideBalance: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = ChiLiV3.colors
    val totalsMap = remember(debts, payments) { debtTotals(debts, payments) }
    val summaryShape = RoundedCornerShape(26.dp)
    val openCount = debts.count { !isDebtSettled(it, payments) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .v3Shadow(c.shadow3, summaryShape)
            .clip(summaryShape)
            .background(Brush.linearGradient(listOf(c.headerTop, c.headerBottom)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Sổ nợ",
                style = MaterialTheme.typography.labelLarge,
                color = c.onHeader,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "$openCount khoản mở",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.onHeader,
                )
            }
        }

        if (totalsMap.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Đang nợ",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onHeader,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = moneyText(0L, MAIN_CURRENCY, hideBalance),
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.onHeader,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cho mượn",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onHeader,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = moneyText(0L, MAIN_CURRENCY, hideBalance),
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.onHeader,
                    )
                }
            }
        } else {
            val showCurrencyLabel = totalsMap.size > 1
            totalsMap.forEach { (currency, totals) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (showCurrencyLabel) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x26FFFFFF))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = currency.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.onHeader,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Đang nợ",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.onHeader,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = moneyText(totals.owe, currency, hideBalance),
                                style = MaterialTheme.typography.headlineSmall,
                                color = c.onHeader,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cho mượn",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.onHeader,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = moneyText(totals.lent, currency, hideBalance),
                                style = MaterialTheme.typography.headlineSmall,
                                color = c.onHeader,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtCard(
    debt: Debt,
    payments: List<DebtPayment>,
    today: LocalDate,
    hideBalance: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ChiLiV3.colors
    val paid = debtPaid(debt, payments)
    val remaining = debtRemaining(debt, payments)
    val progress = debtProgress(debt, payments)
    val settled = isDebtSettled(debt, payments)
    val overdue = isDebtOverdue(debt, payments, today)

    val cardShape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .v3Shadow(c.shadow2, cardShape)
            .clip(cardShape)
            .background(c.cardActive)
            .border(1.dp, c.cardBorder, cardShape)
            .tapScale(onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.onCard,
                )
                Text(
                    text = debt.person,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSheetMuted,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (kindText, kindBg, kindColor) = if (debt.kind == DebtKind.OWE) {
                    Triple("Mình nợ", c.expense.copy(alpha = 0.15f), c.expense)
                } else {
                    Triple("Cho mượn", c.income.copy(alpha = 0.15f), c.income)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(kindBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = kindText,
                        style = MaterialTheme.typography.labelSmall,
                        color = kindColor,
                    )
                }
                if (overdue) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.expense)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Quá hạn",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
                if (settled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.accent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Đã xong",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.accent,
                        )
                    }
                }
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (debt.kind == DebtKind.OWE) c.expense else c.accent,
            trackColor = c.card,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val paidLabel = if (debt.kind == DebtKind.OWE) "Đã trả: " else "Đã thu: "
            val paidText = paidLabel + moneyText(paid, debt.currency, hideBalance) + " / " + moneyText(debt.total, debt.currency, hideBalance)
            Text(
                text = paidText,
                style = MaterialTheme.typography.bodySmall,
                color = c.onSheetMuted,
            )
            val remText = "Còn: " + moneyText(remaining, debt.currency, hideBalance)
            Text(
                text = remText,
                style = MaterialTheme.typography.labelMedium,
                color = if (settled) c.accent else c.onCard,
            )
        }

        val dueDate = debt.dueDate
        if (dueDate != null) {
            Text(
                text = "Hạn trả: ${dueDate.dayMonthYear()}",
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) c.expense else c.onSheetMuted,
            )
        }
        if (debt.note.isNotBlank()) {
            Text(
                text = debt.note,
                style = MaterialTheme.typography.bodySmall,
                color = c.onSheetMuted,
                maxLines = 1,
            )
        }
    }
}
