package com.superduperlover.finance.ui.budgets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CurrencyChips
import com.superduperlover.finance.ui.components.FormDialog
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionHeaderBand
import kotlinx.coroutines.launch

@Composable
internal fun BudgetSheet(
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
    var pickedCurrency by remember { mutableStateOf(budget.currency) }

    // One account: its currency. "Tất cả": the currency chosen below.
    val currency = accounts.firstOrNull { it.id == selectedAccountId }?.currency ?: pickedCurrency

    val initialLimitInput = remember(budget) {
        if (isNew) "" else amountToInput(budget.limit, budget.currency)
    }
    var limitInput by remember { mutableStateOf(initialLimitInput) }
    var limitError by remember { mutableStateOf<String?>(null) }
    var duplicateError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val hasChanges = selectedAccountId != budget.accountId ||
        selectedCategoryId != budget.categoryId ||
        pickedCurrency != budget.currency ||
        limitInput != initialLimitInput

    val saveEnabled = limitInput.isNotBlank()

    val onSave: () -> Unit = {
        val parsedLimit = parseAmount(limitInput, currency)
        if (parsedLimit == null) {
            limitError = "Số tiền không hợp lệ"
        }
        val isDuplicate = existingBudgets.any {
            it.id != budget.id && it.accountId == selectedAccountId && it.categoryId == selectedCategoryId &&
                (selectedAccountId != null || it.currency == currency)
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
                        currency = currency,
                    )
                )
                onDismiss()
            }
        }
    }

    FormDialog(
        title = if (isNew) "Thêm ngân sách" else "Sửa ngân sách",
        onClose = onDismiss,
        onSave = onSave,
        saveEnabled = saveEnabled,
        hasChanges = hasChanges,
    ) {
        SectionHeaderBand("Phạm vi")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OneLineText("Tài khoản", style = MaterialTheme.typography.labelLarge)
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
                        label = { Text("${acc.name} · ${acc.currency.name}") },
                    )
                }
            }

            OneLineText("Danh mục", style = MaterialTheme.typography.labelLarge)
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
        }

        SectionHeaderBand("Hạn mức")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selectedAccountId == null) {
                CurrencyChips(
                    selected = pickedCurrency,
                    onSelect = {
                        pickedCurrency = it
                        duplicateError = null
                    },
                    label = "Loại tiền (các tài khoản cùng loại)",
                )
            }
            OutlinedTextField(
                value = limitInput,
                onValueChange = {
                    limitInput = it
                    limitError = null
                    duplicateError = null
                },
                label = { Text("Hạn mức (${currencySymbol(currency)})") },
                singleLine = true,
                isError = limitError != null || duplicateError != null,
                supportingText = (limitError ?: duplicateError)?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!isNew) {
            SectionHeaderBand("Khác")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText("Xóa ngân sách", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
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
