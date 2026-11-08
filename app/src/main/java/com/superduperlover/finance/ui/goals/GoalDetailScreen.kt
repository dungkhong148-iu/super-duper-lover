package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.superduperlover.finance.core.GoalContribution
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.goalProgress
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.incomeColor
import java.time.LocalDate
import kotlinx.coroutines.launch

/** [onBack] pops the screen. */
@Composable
fun GoalDetailScreen(goalId: Long, onBack: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val goalsNullable by remember(repo) { repo.goals() }.collectAsState(initial = null)

    if (goalsNullable == null) {
        return
    }

    val goal = goalsNullable?.firstOrNull { it.id == goalId }
    if (goal == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val contributions by remember(repo, goal.id) { repo.contributions(goal.id) }.collectAsState(initial = emptyList())
    val progress = goalProgress(goal, contributions, LocalDate.now())

    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var editingContribution by remember { mutableStateOf<GoalContribution?>(null) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "top_bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                }
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "summary_card") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GoalProgressContent(goal = goal, progress = progress)
                }
            }
        }

        item(key = "action_buttons") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        editingContribution = GoalContribution(
                            id = 0L,
                            goalId = goal.id,
                            amount = 0L,
                            date = LocalDate.now(),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text("Thêm khoản để dành")
                }
                OutlinedButton(
                    onClick = { editingGoal = goal },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text("Sửa mục tiêu")
                }
                OutlinedButton(
                    onClick = { showArchiveConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) {
                    Text("Lưu trữ mục tiêu")
                }
            }
        }

        item(key = "contributions_header") {
            Text(
                text = "Các khoản đã để dành",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (contributions.isEmpty()) {
            item(key = "empty_contributions") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Chưa có khoản nào",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(contributions, key = { it.id }) { contribution ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingContribution = contribution },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatGoalDate(contribution.date),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formatMoney(contribution.amount, goal.currency),
                            style = MaterialTheme.typography.titleMedium,
                            color = incomeColor(),
                        )
                    }
                }
            }
        }
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Lưu trữ mục tiêu này?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveConfirm = false
                        scope.launch {
                            repo.archiveGoal(goal.id)
                            onBack()
                        }
                    },
                ) {
                    Text("Lưu trữ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text("Hủy")
                }
            },
        )
    }

    editingGoal?.let { g ->
        GoalSheet(goal = g, onDismiss = { editingGoal = null })
    }

    editingContribution?.let { c ->
        ContributionSheet(
            goalId = goal.id,
            currency = goal.currency,
            contribution = c,
            onDismiss = { editingContribution = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContributionSheet(
    goalId: Long,
    currency: Currency,
    contribution: GoalContribution,
    onDismiss: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val isNew = contribution.id == 0L

    var amountText by remember {
        mutableStateOf(if (isNew || contribution.amount == 0L) "" else amountToInput(contribution.amount, currency))
    }
    var selectedDate by remember { mutableStateOf(contribution.date) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                text = if (isNew) "Thêm khoản để dành" else "Sửa khoản để dành",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                label = { Text("Số tiền (${currencySymbol(currency)})") },
                placeholder = { Text("0") },
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Ngày", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(formatGoalDate(selectedDate))
            }
            Button(
                onClick = {
                    val parsedAmount = parseAmount(amountText, currency)
                    if (parsedAmount == null) {
                        amountError = "Số tiền không hợp lệ"
                    } else {
                        scope.launch {
                            repo.addContribution(
                                GoalContribution(
                                    id = contribution.id,
                                    goalId = goalId,
                                    amount = parsedAmount,
                                    date = selectedDate,
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
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedDate.toEpochDay() * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val epochDay = Math.floorDiv(it, 86_400_000L)
                        selectedDate = LocalDate.ofEpochDay(epochDay)
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
            title = { Text("Xóa khoản này?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            repo.deleteContribution(contribution.id)
                            onDismiss()
                        }
                    },
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
