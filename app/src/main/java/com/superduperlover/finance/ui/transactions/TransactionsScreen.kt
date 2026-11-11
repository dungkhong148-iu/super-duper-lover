package com.superduperlover.finance.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.txnTotals
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.color
import com.superduperlover.finance.ui.components.display
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.icon
import com.superduperlover.finance.ui.components.incomeColor
import com.superduperlover.finance.ui.components.label
import com.superduperlover.finance.ui.theme.ChiLiTheme
import com.superduperlover.finance.ui.theme.chiLiChipBorder
import com.superduperlover.finance.ui.theme.chiLiChipColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private const val MILLIS_PER_DAY = 86_400_000L

private fun formatDateRange(from: LocalDate, to: LocalDate): String {
    val dmf = DateTimeFormatter.ofPattern("dd/MM")
    val full = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return if (from == to) {
        from.format(full)
    } else if (from.year == to.year) {
        "${from.format(dmf)} - ${to.format(full)}"
    } else {
        "${from.format(full)} - ${to.format(full)}"
    }
}

/** Current month, newest first, grouped by day. Tap edits, long-press deletes (after a confirm). */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(onEdit: (Long) -> Unit, initialAccountId: Long? = null) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()

    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(monthStr) { YearMonth.parse(monthStr) }

    var customRangeStart by rememberSaveable { mutableStateOf<String?>(null) }
    var customRangeEnd by rememberSaveable { mutableStateOf<String?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val rangeFrom: LocalDate? = customRangeStart?.let(LocalDate::parse)
    val rangeTo: LocalDate? = customRangeEnd?.let(LocalDate::parse)
    val hasRange = rangeFrom != null && rangeTo != null

    var filterAccountId by rememberSaveable(initialAccountId) { mutableStateOf(initialAccountId) }
    var filterCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }

    val hasFilter = filterAccountId != null || filterCategoryId != null || searchText.isNotBlank() || hasRange

    val txnsFlow = remember(month, customRangeStart, customRangeEnd, filterAccountId, filterCategoryId) {
        if (rangeFrom != null && rangeTo != null) {
            repo.txns(
                from = rangeFrom,
                to = rangeTo,
                accountId = filterAccountId,
                categoryId = filterCategoryId,
            )
        } else {
            repo.txns(month = month, accountId = filterAccountId, categoryId = filterCategoryId)
        }
    }
    val rawTxns by txnsFlow.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val categories by remember(repo) { repo.categories(includeArchived = true) }.collectAsState(initial = emptyList())
    var toDelete by remember { mutableStateOf<Txn?>(null) }

    val txns = remember(rawTxns, searchText) {
        if (searchText.isBlank()) rawTxns
        else {
            val q = searchText.trim().lowercase()
            rawTxns.filter { it.note.lowercase().contains(q) }
        }
    }
    val totals = remember(txns, accounts) { txnTotals(txns, accounts) }
    val currencyOfAccount = remember(accounts) { accounts.associate { it.id to it.currency } }

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ChiLiTheme.colors.outline),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                customRangeStart = null
                                customRangeEnd = null
                                monthStr = month.minusMonths(1).toString()
                            },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Tháng trước")
                        }
                        if (rangeFrom != null && rangeTo != null) {
                            val rangeText = formatDateRange(rangeFrom, rangeTo)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDateRangePicker = true },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                val titleStyle = MaterialTheme.typography.titleMedium.let {
                                    if (it.fontSize.isSpecified && it.fontSize > 18.sp) it.copy(fontSize = 18.sp) else it
                                }
                                OneLineText(
                                    text = rangeText,
                                    style = titleStyle,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                IconButton(
                                    onClick = {
                                        customRangeStart = null
                                        customRangeEnd = null
                                    },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Quay lại xem theo tháng",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDateRangePicker = true },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                val titleStyle = MaterialTheme.typography.titleMedium.let {
                                    if (it.fontSize.isSpecified && it.fontSize > 18.sp) it.copy(fontSize = 18.sp) else it
                                }
                                OneLineText(
                                    text = "Tháng ${month.monthValue}/${month.year}",
                                    style = titleStyle,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                customRangeStart = null
                                customRangeEnd = null
                                monthStr = month.plusMonths(1).toString()
                            },
                            enabled = month.isBefore(YearMonth.now()),
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
                    if (isSearchOpen) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            label = { Text("Tìm trong ghi chú") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }
        }

        item(key = "filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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
                        label = {
                            OneLineText(
                                text = accountLabel,
                                modifier = Modifier.widthIn(max = 200.dp),
                            )
                        },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = filterAccountId != null),
                        modifier = Modifier.heightIn(min = 48.dp),
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
                        label = {
                            OneLineText(
                                text = catLabel,
                                modifier = Modifier.widthIn(max = 200.dp),
                            )
                        },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = filterCategoryId != null),
                        modifier = Modifier.heightIn(min = 48.dp),
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
                if (hasFilter) {
                    TextButton(
                        onClick = {
                            filterAccountId = null
                            filterCategoryId = null
                            searchText = ""
                            isSearchOpen = false
                            customRangeStart = null
                            customRangeEnd = null
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        OneLineText("Xóa bộ lọc", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item(key = "totals") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ChiLiTheme.colors.outline),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val valueStyle = MaterialTheme.typography.titleMedium.let {
                        if (it.fontSize.isSpecified && it.fontSize > 18.sp) it.copy(fontSize = 18.sp) else it
                    }
                    // Income tile
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = TxnKind.INCOME.icon(),
                                    contentDescription = null,
                                    tint = incomeColor(),
                                    modifier = Modifier.size(16.dp),
                                )
                                OneLineText(
                                    text = "Thu nhập",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    minFontSize = 10.sp,
                                )
                            }
                            totals.forEach { (currency, t) ->
                                OneLineText(
                                    text = "+${formatMoney(t.income, currency)}",
                                    style = valueStyle,
                                    fontWeight = FontWeight.Bold,
                                    color = incomeColor(),
                                )
                            }
                        }
                    }
                    // Expense tile
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = TxnKind.EXPENSE.icon(),
                                    contentDescription = null,
                                    tint = expenseColor(),
                                    modifier = Modifier.size(16.dp),
                                )
                                OneLineText(
                                    text = "Chi tiêu",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    minFontSize = 10.sp,
                                )
                            }
                            totals.forEach { (currency, t) ->
                                OneLineText(
                                    text = "-${formatMoney(t.expense, currency)}",
                                    style = valueStyle,
                                    fontWeight = FontWeight.Bold,
                                    color = expenseColor(),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (txns.isEmpty()) {
            item(key = "empty") {
                if (hasFilter) {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_wallet),
                        title = "Không có giao dịch phù hợp",
                        message = "Thử thay đổi hoặc xóa bộ lọc để xem các giao dịch khác.",
                        actionLabel = "Xóa bộ lọc",
                        onAction = {
                            filterAccountId = null
                            filterCategoryId = null
                            searchText = ""
                            isSearchOpen = false
                            customRangeStart = null
                            customRangeEnd = null
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_wallet),
                        title = "Chưa có giao dịch",
                        message = "Ghi lại khoản thu chi đầu tiên của tháng này.",
                        actionLabel = "Thêm giao dịch",
                        onAction = { onEdit(0L) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        } else {
            byDay.forEach { (date, dayTxns) ->
                item(key = "day-$date") {
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
                        ) {
                            OneLineText(
                                text = date.display(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            dayTxns.forEachIndexed { index, t ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    )
                                }
                                val category = t.categoryId?.let { categoryById[it] }
                                val title = if (t.kind == TxnKind.TRANSFER) t.kind.label() else category?.name ?: "Không rõ"
                                val account = accountNames[t.accountId] ?: "?"
                                val currency = currencyOfAccount[t.accountId] ?: Currency.VND
                                val where = if (t.kind == TxnKind.TRANSFER) {
                                    val received = t.toAmount?.let { amount ->
                                        currencyOfAccount[t.toAccountId]?.let { " (+${formatMoney(amount, it)})" }
                                    } ?: ""
                                    "$account → ${accountNames[t.toAccountId] ?: "?"}$received"
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .combinedClickable(onClick = { onEdit(t.id) }, onLongClick = { toDelete = t })
                                    .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (category != null) CategoryIcons.get(category.icon) else t.kind.icon(),
                                            contentDescription = null,
                                            tint = t.kind.color(),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        OneLineText(
                                            text = title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        OneLineText(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            minFontSize = 10.sp,
                                        )
                                    }
                                    OneLineText(
                                        text = sign + formatMoney(t.amount, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = t.kind.color(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Xóa giao dịch?") },
            text = { Text("${t.kind.label()} ${formatMoney(t.amount, currencyOfAccount[t.accountId] ?: Currency.VND)} sẽ bị xóa.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { repo.deleteTxn(t.id); toDelete = null } }) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Hủy") } },
        )
    }

    if (showDateRangePicker) {
        val today = remember { LocalDate.now() }
        val todayEpochMillis = remember(today) { today.toEpochDay() * MILLIS_PER_DAY }
        val todayYear = remember(today) { today.year }
        val selectableDates = remember(todayEpochMillis, todayYear) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayEpochMillis
                override fun isSelectableYear(year: Int): Boolean = year <= todayYear
            }
        }
        val initialStartMillis = remember(rangeFrom) {
            rangeFrom?.let { it.toEpochDay() * MILLIS_PER_DAY }
        }
        val initialEndMillis = remember(rangeTo) {
            rangeTo?.let { it.toEpochDay() * MILLIS_PER_DAY }
        }
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStartMillis,
            initialSelectedEndDateMillis = initialEndMillis,
            selectableDates = selectableDates,
        )

        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        if (startMillis != null) {
                            val endMillis = dateRangePickerState.selectedEndDateMillis ?: startMillis
                            val start = LocalDate.ofEpochDay(Math.floorDiv(startMillis, MILLIS_PER_DAY))
                            val end = LocalDate.ofEpochDay(Math.floorDiv(endMillis, MILLIS_PER_DAY))
                            customRangeStart = start.toString()
                            customRangeEnd = end.toString()
                        }
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null,
                ) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Hủy")
                }
            },
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f),
                title = {
                    Text(
                        "Chọn khoảng ngày",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    )
                },
                headline = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    val dmf = DateTimeFormatter.ofPattern("dd/MM")
                    val full = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val text = if (startMillis == null) {
                        "Từ ngày - Đến ngày"
                    } else if (endMillis == null) {
                        val start = LocalDate.ofEpochDay(Math.floorDiv(startMillis, MILLIS_PER_DAY))
                        "${start.format(dmf)} - Đến ngày"
                    } else {
                        val start = LocalDate.ofEpochDay(Math.floorDiv(startMillis, MILLIS_PER_DAY))
                        val end = LocalDate.ofEpochDay(Math.floorDiv(endMillis, MILLIS_PER_DAY))
                        "${start.format(dmf)} - ${end.format(full)}"
                    }
                    OneLineText(
                        text = text,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
            )
        }
    }
}
