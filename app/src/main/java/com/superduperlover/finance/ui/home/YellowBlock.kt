package com.superduperlover.finance.ui.home

import com.superduperlover.finance.ui.theme.tapScale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.PeriodRange
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.TxRange
import com.superduperlover.finance.core.txRangeBounds
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.dayMonth
import com.superduperlover.finance.ui.components.dayMonthYear
import com.superduperlover.finance.ui.components.icon
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.v3Shadow
import java.time.LocalDate

private const val MAX_ROWS = 30

private fun TxRange.label() = when (this) {
    TxRange.DAY -> "Ngày"
    TxRange.WEEK -> "Tuần"
    TxRange.MONTH -> "Tháng"
    TxRange.ALL -> "Tất cả"
}

private fun rangeText(range: TxRange, anchor: LocalDate): String {
    val b: PeriodRange = txRangeBounds(range, anchor) ?: return "Tất cả"
    return when (range) {
        TxRange.DAY -> b.start.dayMonthYear()
        TxRange.WEEK -> "${b.start.dayMonth()} - ${b.endInclusive.dayMonth()}"
        else -> "%02d/%d".format(b.start.monthValue, b.start.year)
    }
}

/**
 * v3 yellow block (docs/ui-layout.md section 1): accordion with the count, range selector with the "<" ">" stepper,
 * type-filter banner, the transactions of the range (tap = edit) and "Xem chi tiết" (full list).
 * [txns] are already filtered by account, type and range, newest first.
 */
@Composable
fun YellowBlock(
    txns: List<Txn>,
    accounts: Map<Long, Account>,
    categories: Map<Long, Category>,
    range: TxRange,
    anchor: LocalDate,
    expanded: Boolean,
    typeFilter: TxnKind?,
    hidden: Boolean,
    onToggle: () -> Unit,
    onRange: (TxRange) -> Unit,
    onShift: (Int) -> Unit,
    onClearFilter: () -> Unit,
    onOpenTxn: (Long) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ChiLiV3.colors
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.v3Shadow(c.shadow2, shape).clip(shape).background(c.yellow).border(1.dp, c.yellowBorder, shape)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).clip(CircleShape).clickable(onClick = onToggle).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = c.onYellow)
                Text("Giao dịch", color = c.onYellow, style = MaterialTheme.typography.titleSmall)
                Box(Modifier.clip(CircleShape).background(c.yellowChip).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("${txns.size}", color = c.onYellow, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (range != TxRange.ALL) {
                Icon(Icons.Filled.KeyboardArrowLeft, "Kỳ trước", Modifier.size(32.dp).clip(CircleShape).clickable { onShift(-1) }.padding(4.dp), tint = c.onYellow)
                Text(rangeText(range, anchor), color = c.onYellow, style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Filled.KeyboardArrowRight, "Kỳ sau", Modifier.size(32.dp).clip(CircleShape).clickable { onShift(1) }.padding(4.dp), tint = c.onYellow)
            }
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.yellowChip).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TxRange.entries.forEach { r ->
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (r == range) c.yellowChipSelected else Color.Transparent)
                                .clickable { onRange(r) }.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(r.label(), color = c.onYellow, style = MaterialTheme.typography.labelMedium) }
                    }
                }
                if (typeFilter != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.yellowChipSelected.copy(alpha = 0.8f)).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (typeFilter == TxnKind.INCOME) "Đang lọc: Thu nhập" else "Đang lọc: Chi tiêu",
                            Modifier.weight(1f), color = c.onYellow, style = MaterialTheme.typography.labelMedium,
                        )
                        Text("Xóa lọc", color = c.accent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(onClick = onClearFilter).padding(8.dp))
                    }
                }
                if (txns.isEmpty()) {
                    Text("Không có giao dịch trong khoảng này", color = c.onYellow, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
                }
                txns.take(MAX_ROWS).forEach { t -> TxnRow(t, accounts, categories, hidden, onClick = { onOpenTxn(t.id) }) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (txns.size > MAX_ROWS) "Hiện $MAX_ROWS / ${txns.size} giao dịch" else "${txns.size} giao dịch",
                        Modifier.weight(1f), color = c.onYellow, style = MaterialTheme.typography.labelMedium,
                    )
                    Box(Modifier.tapScale(onOpenAll).clip(CircleShape).background(Color(0xE60F172A)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text("Xem chi tiết", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TxnRow(t: Txn, accounts: Map<Long, Account>, categories: Map<Long, Category>, hidden: Boolean, onClick: () -> Unit) {
    val c = ChiLiV3.colors
    val category = t.categoryId?.let { categories[it] }
    val title = when {
        t.note.isNotBlank() -> t.note
        t.kind == TxnKind.TRANSFER -> "Chuyển khoản"
        else -> category?.name ?: "Giao dịch"
    }
    val from = accounts[t.accountId]
    val subtitle = buildString {
        append(t.date.dayMonth())
        from?.let { append(" · ").append(it.name) }
        if (t.kind == TxnKind.TRANSFER) accounts[t.toAccountId]?.let { append(" → ").append(it.name) }
    }
    val color = when (t.kind) {
        TxnKind.INCOME -> c.income
        TxnKind.EXPENSE -> c.expense
        TxnKind.TRANSFER -> c.onTxRow
    }
    val sign = when (t.kind) { TxnKind.INCOME -> "+"; TxnKind.EXPENSE -> "-"; TxnKind.TRANSFER -> "" }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().tapScale(onClick).v3Shadow(c.shadow1, shape).clip(shape).background(c.txRow).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(if (category != null) CategoryIcons.get(category.icon) else t.kind.icon(), null, Modifier.size(18.dp), tint = color)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = c.onTxRow, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = c.onSheetMuted, style = MaterialTheme.typography.labelSmall)
        }
        Text(sign + moneyText(t.amount, from?.currency ?: com.superduperlover.finance.core.MAIN_CURRENCY, hidden), color = color, style = MaterialTheme.typography.labelLarge)
    }
}
