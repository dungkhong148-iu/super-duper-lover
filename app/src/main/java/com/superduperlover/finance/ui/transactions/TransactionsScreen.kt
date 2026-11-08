package com.superduperlover.finance.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.formatVnd
import com.superduperlover.finance.core.txnTotals
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.color
import com.superduperlover.finance.ui.components.display
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.icon
import com.superduperlover.finance.ui.components.incomeColor
import com.superduperlover.finance.ui.components.label
import java.time.YearMonth
import kotlinx.coroutines.launch

/** Current month, newest first, grouped by day. Tap edits, long-press deletes (after a confirm). */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(onEdit: (Long) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()

    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(monthStr) { YearMonth.parse(monthStr) }

    var filterAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var filterCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }

    val txnsFlow = remember(month, filterAccountId, filterCategoryId) {
        repo.txns(month = month, accountId = filterAccountId, categoryId = filterCategoryId)
    }
    val rawTxns by txnsFlow.collectAsState(initial = emptyList())
    val accounts by repo.accounts(includeArchived = true).collectAsState(initial = emptyList())
    val categories by repo.categories(includeArchived = true).collectAsState(initial = emptyList())
    var toDelete by remember { mutableStateOf<Txn?>(null) }

    val txns = remember(rawTxns, searchText) {
        if (searchText.isBlank()) rawTxns
        else {
            val q = searchText.trim().lowercase()
            rawTxns.filter { it.note.lowercase().contains(q) }
        }
    }
    val totals = remember(txns) { txnTotals(txns) }

    val accountNames = accounts.associate { it.id to it.name }
    val categoryById = categories.associateBy { it.id }
    val byDay = txns.groupBy { it.date }

    val duplicateNames = remember(categories) {
        categories.groupBy { it.name }.filter { it.value.size > 1 }.keys
    }
    fun categoryDisplayName(c: Category): String {
        val isDuplicate = duplicateNames.contains(c.name)
        val kindLabel = if (c.kind == CategoryKind.EXPENSE) "Chi tiêu" else "Thu nhập"
        return if (isDuplicate) "${c.name} ($kindLabel)" else c.name
    }

    var accountMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { monthStr = month.minusMonths(1).toString() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Tháng trước")
                }
                Text(
                    "Tháng ${month.monthValue}/${month.year}",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { monthStr = month.plusMonths(1).toString() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Tháng sau")
                }
                IconButton(
                    onClick = { isSearchOpen = !isSearchOpen },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Tìm kiếm")
                }
            }
        }

        item(key = "filters") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Account filter chip
                    Box {
                        val selectedAccount = accounts.firstOrNull { it.id == filterAccountId }
                        val accountLabel = if (selectedAccount != null) "Tài khoản: ${selectedAccount.name}" else "Tài khoản: Tất cả"
                        FilterChip(
                            selected = filterAccountId != null,
                            onClick = { accountMenuExpanded = true },
                            label = { Text(accountLabel) },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        )
                        DropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tất cả") },
                                onClick = {
                                    filterAccountId = null
                                    accountMenuExpanded = false
                                },
                            )
                            val availableAccounts = accounts.filter { !it.archived || it.id == filterAccountId }
                            availableAccounts.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text(a.name) },
                                    onClick = {
                                        filterAccountId = a.id
                                        accountMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Category filter chip
                    Box {
                        val selectedCategory = categories.firstOrNull { it.id == filterCategoryId }
                        val catLabel = if (selectedCategory != null) {
                            "Danh mục: ${categoryDisplayName(selectedCategory)}"
                        } else {
                            "Danh mục: Tất cả"
                        }
                        FilterChip(
                            selected = filterCategoryId != null,
                            onClick = { categoryMenuExpanded = true },
                            label = { Text(catLabel) },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        )
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tất cả") },
                                onClick = {
                                    filterCategoryId = null
                                    categoryMenuExpanded = false
                                },
                            )
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(categoryDisplayName(c)) },
                                    onClick = {
                                        filterCategoryId = c.id
                                        categoryMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Reset button
                    if (filterAccountId != null || filterCategoryId != null || searchText.isNotBlank()) {
                        TextButton(
                            onClick = {
                                filterAccountId = null
                                filterCategoryId = null
                                searchText = ""
                                isSearchOpen = false
                            },
                        ) {
                            Text("Xóa bộ lọc")
                        }
                    }
                }

                if (isSearchOpen) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Tìm trong ghi chú") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }

        item(key = "totals") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Thu nhập",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "+${formatVnd(totals.income)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = incomeColor(),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Chi tiêu",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "-${formatVnd(totals.expense)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = expenseColor(),
                        )
                    }
                }
            }
        }

        if (txns.isEmpty()) {
            item(key = "empty") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (filterAccountId != null || filterCategoryId != null || searchText.isNotBlank()) {
                            "Không có giao dịch phù hợp"
                        } else {
                            "Chưa có giao dịch trong tháng ${month.monthValue}/${month.year}"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
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
                            Modifier
                                .size(40.dp)
                                .background(t.kind.color().copy(alpha = 0.15f), CircleShape),
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
