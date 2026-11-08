package com.superduperlover.finance.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.BudgetStatus
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.budgetUsage
import com.superduperlover.finance.core.formatVnd
import com.superduperlover.finance.core.parseVndInput
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.incomeColor
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val AmberLight = Color(0xFFB26A00)
private val AmberDark = Color(0xFFFFB74D)

@Composable
internal fun amberColor(): Color = if (isSystemInDarkTheme()) AmberDark else AmberLight

@Composable
internal fun budgetStatusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.OK -> incomeColor()
    BudgetStatus.WARN -> amberColor()
    BudgetStatus.OVER -> expenseColor()
}

internal fun BudgetStatus.statusLabel(): String = when (this) {
    BudgetStatus.OK -> "Ổn"
    BudgetStatus.WARN -> "Sắp hết"
    BudgetStatus.OVER -> "Vượt mức"
}

@Composable
fun BudgetsScreen() {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()

    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(monthStr) { YearMonth.parse(monthStr) }

    val budgets by remember(repo, month) { repo.budgets(month) }.collectAsState(initial = emptyList())
    val txns by remember(repo, month) { repo.txns(month = month) }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val categories by remember(repo) { repo.categories(CategoryKind.EXPENSE, includeArchived = true) }.collectAsState(initial = emptyList())

    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }

    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    var copyMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(monthStr) {
        copyMessage = null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { monthStr = month.minusMonths(1).toString() },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Tháng trước",
                )
            }
            Text(
                text = "Tháng ${month.monthValue}/${month.year}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { monthStr = month.plusMonths(1).toString() },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Tháng sau",
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (budgets.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Chưa có ngân sách cho tháng này",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(budgets, key = { it.id }) { budget ->
                    val usage = budgetUsage(budget, txns)
                    val statusColor = budgetStatusColor(usage.status)
                    val accountName = budget.accountId?.let { accountsById[it]?.name ?: "?" } ?: "Tất cả tài khoản"
                    val categoryName = budget.categoryId?.let { categoriesById[it]?.name ?: "?" } ?: "Tất cả danh mục"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingBudget = budget },
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "$accountName · $categoryName",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                LinearProgressIndicator(
                                    progress = { usage.ratio.toFloat().coerceIn(0f, 1f) },
                                    color = statusColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 8.dp),
                                )
                                Text(
                                    text = usage.status.statusLabel(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = statusColor,
                                )
                            }
                            Text(
                                text = "Đã chi ${formatVnd(usage.spent)} / ${formatVnd(usage.limit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (usage.remaining >= 0) {
                                    "Còn lại ${formatVnd(usage.remaining)}"
                                } else {
                                    "Vượt ${formatVnd(-usage.remaining)}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (usage.remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else expenseColor(),
                            )
                        }
                    }
                }
            }

            item(key = "add_budget") {
                Button(
                    onClick = {
                        editingBudget = Budget(
                            id = 0L,
                            month = month,
                            accountId = null,
                            categoryId = null,
                            limit = 0L,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text("Thêm ngân sách")
                }
            }

            item(key = "copy_previous") {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val lastMonth = month.minusMonths(1)
                            val lastMonthBudgets = repo.budgets(lastMonth).first()
                            if (lastMonthBudgets.isEmpty()) {
                                copyMessage = "Tháng trước chưa có ngân sách"
                                return@launch
                            }
                            val currentBudgets = repo.budgets(month).first()
                            val toCopy = lastMonthBudgets.filter { last ->
                                currentBudgets.none { curr ->
                                    curr.accountId == last.accountId && curr.categoryId == last.categoryId
                                }
                            }
                            if (toCopy.isEmpty()) {
                                copyMessage = "Đã có đủ ngân sách"
                            } else {
                                copyMessage = null
                                for (b in toCopy) {
                                    repo.saveBudget(b.copy(id = 0L, month = month))
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text("Sao chép từ tháng trước")
                }
            }

            copyMessage?.let { msg ->
                item(key = "copy_message") {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSheet(
    budget: Budget,
    existingBudgets: List<Budget>,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val isNew = budget.id == 0L

    var selectedAccountId by remember { mutableStateOf(budget.accountId) }
    var selectedCategoryId by remember { mutableStateOf(budget.categoryId) }
    var limitInput by remember {
        mutableStateOf(if (isNew) "" else budget.limit.toString())
    }
    var limitError by remember { mutableStateOf<String?>(null) }
    var duplicateError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isNew) "Thêm ngân sách" else "Sửa ngân sách",
                style = MaterialTheme.typography.titleLarge,
            )

            Text("Tài khoản", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedAccountId == null,
                    onClick = {
                        selectedAccountId = null
                        duplicateError = null
                    },
                    label = { Text("Tất cả") },
                )
                val displayedAccounts = remember(accounts, budget.accountId) {
                    accounts.filter { !it.archived || it.id == budget.accountId }
                }
                displayedAccounts.forEach { acc ->
                    FilterChip(
                        selected = selectedAccountId == acc.id,
                        onClick = {
                            selectedAccountId = acc.id
                            duplicateError = null
                        },
                        label = { Text(acc.name) },
                    )
                }
            }

            Text("Danh mục", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = {
                        selectedCategoryId = null
                        duplicateError = null
                    },
                    label = { Text("Tất cả") },
                )
                val displayedCategories = remember(categories, budget.categoryId) {
                    categories.filter { it.kind == CategoryKind.EXPENSE && (!it.archived || it.id == budget.categoryId) }
                }
                displayedCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = {
                            selectedCategoryId = cat.id
                            duplicateError = null
                        },
                        label = { Text(cat.name) },
                    )
                }
            }

            OutlinedTextField(
                value = limitInput,
                onValueChange = {
                    limitInput = it
                    limitError = null
                    duplicateError = null
                },
                label = { Text("Hạn mức (₫)") },
                singleLine = true,
                isError = limitError != null,
                supportingText = limitError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            duplicateError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = {
                    val parsedLimit = parseVndInput(limitInput)
                    if (parsedLimit == null) {
                        limitError = "Số tiền không hợp lệ"
                    }
                    val isDuplicate = existingBudgets.any {
                        it.id != budget.id && it.accountId == selectedAccountId && it.categoryId == selectedCategoryId
                    }
                    if (isDuplicate) {
                        duplicateError = "Ngân sách này đã tồn tại"
                    }
                    if (parsedLimit != null && !isDuplicate) {
                        scope.launch {
                            repo.saveBudget(
                                Budget(
                                    id = budget.id,
                                    month = budget.month,
                                    accountId = selectedAccountId,
                                    categoryId = selectedCategoryId,
                                    limit = parsedLimit,
                                )
                            )
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Lưu")
            }

            if (!isNew) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text("Xóa ngân sách", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa ngân sách này?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            repo.deleteBudget(budget.id)
                            onDismiss()
                        }
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Hủy")
                }
            },
        )
    }
}
