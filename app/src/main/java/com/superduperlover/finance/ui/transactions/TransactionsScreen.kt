package com.superduperlover.finance.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.formatVnd
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.color
import com.superduperlover.finance.ui.components.display
import com.superduperlover.finance.ui.components.icon
import com.superduperlover.finance.ui.components.label
import java.time.YearMonth
import kotlinx.coroutines.launch

/** Current month, newest first, grouped by day. Tap edits, long-press deletes (after a confirm). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(onEdit: (Long) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val month = remember { YearMonth.now() }
    val txns by repo.txns(month = month).collectAsState(initial = emptyList())
    val accounts by repo.accounts(includeArchived = true).collectAsState(initial = emptyList())
    val categories by repo.categories(includeArchived = true).collectAsState(initial = emptyList())
    var toDelete by remember { mutableStateOf<Txn?>(null) }

    val accountNames = accounts.associate { it.id to it.name }
    val categoryById = categories.associateBy { it.id }
    val byDay = txns.groupBy { it.date }

    if (txns.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Chưa có giao dịch trong tháng ${month.monthValue}/${month.year}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        ) {
            item(key = "month") {
                Text(
                    "Tháng ${month.monthValue}/${month.year}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            byDay.forEach { (date, dayTxns) ->
                item(key = "day-$date") {
                    Text(
                        date.display(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                items(dayTxns, key = { it.id }) { t ->
                    val category = t.categoryId?.let { categoryById[it] }
                    val title = if (t.kind == TxnKind.TRANSFER) t.kind.label() else category?.name ?: "Không rõ"
                    val account = accountNames[t.accountId] ?: "?"
                    val where = if (t.kind == TxnKind.TRANSFER) {
                        "$account → ${accountNames[t.toAccountId] ?: "?"}"
                    } else {
                        account
                    }
                    val subtitle = if (t.note.isBlank()) where else "$where · ${t.note}"
                    val sign = when (t.kind) {
                        TxnKind.EXPENSE -> "-"
                        TxnKind.INCOME -> "+"
                        TxnKind.TRANSFER -> ""
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .combinedClickable(onClick = { onEdit(t.id) }, onLongClick = { toDelete = t })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(40.dp).background(t.kind.color().copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (category != null) CategoryIcons.get(category.icon) else t.kind.icon(),
                                contentDescription = null,
                                tint = t.kind.color(),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            sign + formatVnd(t.amount),
                            style = MaterialTheme.typography.titleMedium,
                            color = t.kind.color(),
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Xóa giao dịch?") },
            text = { Text("${t.kind.label()} ${formatVnd(t.amount)} sẽ bị xóa.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { repo.deleteTxn(t.id); toDelete = null } }) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Hủy") } },
        )
    }
}
