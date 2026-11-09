package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CurrencyChips
import com.superduperlover.finance.ui.components.FormDialog
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionHeaderBand
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSheet(
    goal: Goal,
    onDismiss: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val isNew = goal.id == 0L

    var name by remember { mutableStateOf(goal.name) }
    var currency by remember { mutableStateOf(goal.currency) }
    val initialTargetText = remember(goal) {
        if (isNew || goal.target == 0L) "" else amountToInput(goal.target, goal.currency)
    }
    var targetText by remember { mutableStateOf(initialTargetText) }
    var deadline by remember { mutableStateOf(goal.deadline) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var targetError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val hasChanges = name != goal.name ||
        currency != goal.currency ||
        targetText != initialTargetText ||
        deadline != goal.deadline

    val saveEnabled = name.isNotBlank() && targetText.isNotBlank()

    val onSave: () -> Unit = {
        val trimmed = name.trim()
        val parsedTarget = parseAmount(targetText, currency)
        if (trimmed.isEmpty()) nameError = "Nhập tên mục tiêu"
        if (parsedTarget == null) targetError = "Số tiền không hợp lệ"
        if (trimmed.isNotEmpty() && parsedTarget != null) {
            scope.launch {
                repo.saveGoal(
                    // copy: keeps completedAt (editing a goal must not reopen it)
                    goal.copy(
                        name = trimmed,
                        target = parsedTarget,
                        deadline = deadline,
                        archived = false,
                        currency = currency,
                    )
                )
                onDismiss()
            }
        }
    }

    FormDialog(
        title = if (isNew) "Thêm mục tiêu" else "Sửa mục tiêu",
        onClose = onDismiss,
        onSave = onSave,
        saveEnabled = saveEnabled,
        hasChanges = hasChanges,
    ) {
        SectionHeaderBand("Mục tiêu")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text("Tên mục tiêu") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            if (isNew) {
                CurrencyChips(
                    selected = currency,
                    onSelect = {
                        currency = it
                        targetError = null
                    },
                )
            }

            OutlinedTextField(
                value = targetText,
                onValueChange = {
                    targetText = it
                    targetError = null
                },
                label = { Text("Số tiền mục tiêu (${currencySymbol(currency)})") },
                placeholder = { Text("0") },
                singleLine = true,
                isError = targetError != null,
                supportingText = targetError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeaderBand("Thời hạn")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OneLineText("Hạn hoàn thành (không bắt buộc)", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText(deadline?.let { formatGoalDate(it) } ?: "Chưa đặt hạn")
                }
                if (deadline != null) {
                    TextButton(
                        onClick = { deadline = null },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        OneLineText("Bỏ hạn")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        val initialMillis = (deadline ?: LocalDate.now()).toEpochDay() * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val epochDay = Math.floorDiv(it, 86_400_000L)
                        deadline = LocalDate.ofEpochDay(epochDay)
                    }
                    showDatePicker = false
                }) {
                    OneLineText("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    OneLineText("Hủy")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
