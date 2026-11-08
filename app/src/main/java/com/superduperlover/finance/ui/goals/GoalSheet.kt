package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.superduperlover.finance.core.GoalProgress
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.ui.components.CurrencyChips
import com.superduperlover.finance.ui.LocalRepo
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
    var targetText by remember {
        mutableStateOf(if (isNew || goal.target == 0L) "" else amountToInput(goal.target, goal.currency))
    }
    var deadline by remember { mutableStateOf(goal.deadline) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var targetError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isNew) "Thêm mục tiêu" else "Sửa mục tiêu",
                style = MaterialTheme.typography.titleLarge,
            )
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
                CurrencyChips(selected = currency, onSelect = { currency = it; targetError = null })
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
            Text("Hạn hoàn thành (không bắt buộc)", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(deadline?.let { formatGoalDate(it) } ?: "Chưa đặt hạn")
                }
                if (deadline != null) {
                    TextButton(onClick = { deadline = null }) {
                        Text("Bỏ hạn")
                    }
                }
            }
            Button(
                onClick = {
                    val trimmed = name.trim()
                    val parsedTarget = parseAmount(targetText, currency)
                    if (trimmed.isEmpty()) nameError = "Nhập tên mục tiêu"
                    if (parsedTarget == null) targetError = "Số tiền không hợp lệ"
                    if (trimmed.isNotEmpty() && parsedTarget != null) {
                        scope.launch {
                            repo.saveGoal(
                                Goal(
                                    id = goal.id,
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
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text("Lưu")
            }
        }
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
}
