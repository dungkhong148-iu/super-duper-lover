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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.BudgetPeriod
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.core.periodIndexAt
import com.superduperlover.finance.core.periodRange
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CurrencyChips
import com.superduperlover.finance.ui.components.FormDialog
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionHeaderBand
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val today = remember { LocalDate.now() }

    var selectedAccountId by remember { mutableStateOf(budget.accountId) }
    var selectedCategoryId by remember { mutableStateOf(budget.categoryId) }
    var pickedCurrency by remember { mutableStateOf(budget.currency) }

    var selectedPeriod by remember { mutableStateOf(budget.period) }
    var selectedStartDate by remember { mutableStateOf(budget.startDate) }
    var rollover by remember { mutableStateOf(budget.rollover) }

    var showDatePicker by remember { mutableStateOf(false) }

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
        limitInput != initialLimitInput ||
        selectedPeriod != budget.period ||
        selectedStartDate != budget.startDate ||
        rollover != budget.rollover

    val saveEnabled = limitInput.isNotBlank()

    val onSave: () -> Unit = {
        val parsedLimit = parseAmount(limitInput, currency)
        if (parsedLimit == null) {
            limitError = "Số tiền không hợp lệ"
        }
        val isDuplicate = existingBudgets.any {
            it.id != budget.id &&
                it.accountId == selectedAccountId &&
                it.categoryId == selectedCategoryId &&
                it.period == selectedPeriod &&
                (selectedAccountId != null || it.currency == currency)
        }
        if (isDuplicate) {
            duplicateError = "Ngân sách này đã tồn tại"
        }
        if (parsedLimit != null && !isDuplicate) {
            scope.launch {
                repo.saveBudget(
                    budget.copy(
                        accountId = selectedAccountId,
                        categoryId = selectedCategoryId,
                        limit = parsedLimit,
                        currency = currency,
                        period = selectedPeriod,
                        startDate = selectedStartDate,
                        rollover = rollover,
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

        SectionHeaderBand("Chu kỳ")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OneLineText("Chu kỳ", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BudgetPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = selectedPeriod == p,
                        onClick = {
                            selectedPeriod = p
                            duplicateError = null
                        },
                        label = { Text(p.label()) },
                    )
                }
            }

            OneLineText("Ngày bắt đầu", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                OneLineText("%02d/%02d/%04d".format(selectedStartDate.dayOfMonth, selectedStartDate.monthValue, selectedStartDate.year))
            }

            val periodIndex = remember(selectedPeriod, selectedStartDate, today) {
                periodIndexAt(selectedPeriod, selectedStartDate, today) ?: 0
            }
            val currentRange = remember(selectedPeriod, selectedStartDate, periodIndex) {
                periodRange(selectedPeriod, selectedStartDate, periodIndex)
            }
            val startStr = "%02d/%02d".format(currentRange.start.dayOfMonth, currentRange.start.monthValue)
            val endStr = "%02d/%02d".format(currentRange.endInclusive.dayOfMonth, currentRange.endInclusive.monthValue)
            val ruleExplain = if (selectedPeriod == BudgetPeriod.WEEK) "7 ngày từ ngày bắt đầu." else "Mỗi kỳ tính từ ngày bắt đầu."
            Text(
                text = "Kỳ hiện tại: $startStr – $endStr. $ruleExplain",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = rollover,
                        role = Role.Switch,
                        onValueChange = {
                            rollover = it
                            duplicateError = null
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                ) {
                    OneLineText(
                        text = "Chuyển phần còn lại sang kỳ sau",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Tiền còn thừa được cộng vào hạn mức kỳ sau; chi vượt sẽ bị trừ đi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = rollover,
                    onCheckedChange = null,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
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

    if (showDatePicker) {
        val initialMillis = selectedStartDate.toEpochDay() * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val epochDay = Math.floorDiv(it, 86_400_000L)
                        selectedStartDate = LocalDate.ofEpochDay(epochDay)
                        duplicateError = null
                    }
                    showDatePicker = false
                }) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            },
        ) {
            DatePicker(state = datePickerState)
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
