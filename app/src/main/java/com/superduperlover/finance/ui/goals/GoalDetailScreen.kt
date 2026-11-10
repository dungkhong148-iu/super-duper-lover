package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.ContributionType
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.GoalContribution
import com.superduperlover.finance.core.JarStatus
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.goalProgress
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.add.CategoryPickerSheet
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import com.superduperlover.finance.ui.theme.ChiLiTheme
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
    val expenseCategories by remember(repo) {
        repo.categories(kind = CategoryKind.EXPENSE, includeArchived = true)
    }.collectAsState(initial = emptyList())

    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var editingContribution by remember { mutableStateOf<GoalContribution?>(null) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showCancelJarConfirm by remember { mutableStateOf(false) }
    var showSettlementSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ChiLiTopBar(
            title = goal.name,
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (goal.status == JarStatus.PENDING_SETTLEMENT) {
                item(key = "pending_settlement_card") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ChiLiTheme.colors.warn),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ChiLiTheme.colors.warn.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_savings),
                                        contentDescription = null,
                                        tint = ChiLiTheme.colors.warn,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Text(
                                    text = "Hũ đã gom đủ tiền, chờ xác nhận chi tiêu",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showSettlementSheet = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                ) {
                                    OneLineText("Xác nhận chi tiêu", style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = { showCancelJarConfirm = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                ) {
                                    OneLineText("Hủy hũ", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            if (goal.status == JarStatus.CANCELLED) {
                item(key = "cancelled_banner") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                text = "Hũ đã bị hủy (số tiền giữ đã được giải phóng)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item(key = "summary_card") {
                SectionCard {
                    GoalProgressContent(goal = goal, progress = progress)
                }
            }

            item(key = "action_buttons") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (goal.status == JarStatus.ACTIVE) {
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
                            OneLineText("Thêm khoản để dành", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (goal.status == JarStatus.ACTIVE && goal.plan == null) {
                        if (goal.completedAt == null) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repo.completeGoal(goal.id)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                            ) {
                                OneLineText("Đánh dấu hoàn thành", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repo.reopenGoal(goal.id)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                            ) {
                                OneLineText("Mở lại", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else if (goal.status == JarStatus.COMPLETED && goal.plan == null) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repo.reopenGoal(goal.id)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            OneLineText("Mở lại", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (goal.status == JarStatus.ACTIVE && goal.plan != null) {
                        OutlinedButton(
                            onClick = { showCancelJarConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        ) {
                            OneLineText("Hủy hũ", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (goal.status != JarStatus.CANCELLED) {
                        OutlinedButton(
                            onClick = { editingGoal = goal },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            OneLineText("Sửa mục tiêu", style = MaterialTheme.typography.labelLarge)
                        }
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
                        OneLineText("Lưu trữ mục tiêu", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item(key = "contributions_header") {
                OneLineText(
                    text = "Các khoản đã để dành",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (contributions.isEmpty()) {
                item(key = "empty_contributions") {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_savings),
                        title = "Hãy thêm khoản đầu tiên",
                        message = "Chưa có khoản nào",
                        actionLabel = "Thêm khoản để dành",
                        onAction = {
                            editingContribution = GoalContribution(
                                id = 0L,
                                goalId = goal.id,
                                amount = 0L,
                                date = LocalDate.now(),
                            )
                        },
                    )
                }
            } else {
                items(contributions, key = { it.id }) { contribution ->
                    val isManual = contribution.type == ContributionType.MANUAL
                    SectionCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isManual) { editingContribution = contribution },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                OneLineText(
                                    text = formatGoalDate(contribution.date),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val tagText = when (contribution.type) {
                                    ContributionType.AUTO_SCHEDULED -> {
                                        "Tự động trích" + if (contribution.periodIndex != null) " (Kỳ ${contribution.periodIndex})" else ""
                                    }
                                    ContributionType.MANUAL -> "Thủ công"
                                    ContributionType.SETTLEMENT -> "Thanh toán chi tiêu"
                                }
                                OneLineText(
                                    text = tagText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (contribution.type == ContributionType.SETTLEMENT) {
                                        ChiLiTheme.colors.expense
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            OneLineText(
                                text = formatMoney(contribution.amount, goal.currency),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (contribution.type == ContributionType.SETTLEMENT) {
                                    ChiLiTheme.colors.expense
                                } else {
                                    ChiLiTheme.colors.income
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCancelJarConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelJarConfirm = false },
            title = { Text("Hủy hũ này?") },
            text = {
                Text("Số tiền giữ sẽ được giải phóng và không có khoản chi nào được tạo.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelJarConfirm = false
                        scope.launch {
                            repo.cancelJar(goal.id)
                        }
                    },
                ) {
                    OneLineText("Hủy hũ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelJarConfirm = false }) {
                    OneLineText("Không")
                }
            },
        )
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
                    OneLineText("Lưu trữ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    OneLineText("Hủy")
                }
            },
        )
    }

    if (showSettlementSheet) {
        SettlementSheet(
            goal = goal,
            savedAmount = progress.saved,
            categories = expenseCategories,
            onDismiss = { showSettlementSheet = false },
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
private fun SettlementSheet(
    goal: Goal,
    savedAmount: Long,
    categories: List<Category>,
    onDismiss: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()

    val defaultAmount = if (goal.target > 0L) goal.target else savedAmount
    var amountText by remember {
        mutableStateOf(if (defaultAmount > 0L) amountToInput(defaultAmount, goal.currency) else "")
    }
    var selectedCategoryId by remember { mutableStateOf(goal.categoryId) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var settleError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val selectableCategories = remember(categories, selectedCategoryId) {
        categories.filter { !it.archived || it.id == selectedCategoryId }
    }
    val selectedCategory = remember(categories, selectedCategoryId) {
        categories.firstOrNull { it.id == selectedCategoryId }
    }

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
            OneLineText(
                text = "Xác nhận chi tiêu",
                style = MaterialTheme.typography.titleLarge,
            )

            OneLineText("Danh mục chi tiêu", style = MaterialTheme.typography.labelLarge)
            Surface(
                onClick = { showCategoryPicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (categoryError != null) MaterialTheme.colorScheme.error else ChiLiTheme.colors.outline,
                ),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (selectedCategory != null) {
                        Icon(
                            imageVector = CategoryIcons.get(selectedCategory.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        OneLineText(
                            text = selectedCategory.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        OneLineText(
                            text = "Chọn danh mục",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (categoryError != null) {
                Text(
                    text = categoryError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                    settleError = null
                },
                label = { Text("Số tiền (${currencySymbol(goal.currency)})") },
                placeholder = { Text("0") },
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OneLineText("Ngày chi tiêu", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                OneLineText(formatGoalDate(selectedDate), style = MaterialTheme.typography.labelLarge)
            }

            if (settleError != null) {
                Text(
                    text = settleError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Button(
                onClick = {
                    val parsedAmount = parseAmount(amountText, goal.currency)
                    if (parsedAmount == null || parsedAmount <= 0L) {
                        amountError = "Số tiền không hợp lệ"
                        return@Button
                    }
                    if (selectedCategoryId == null) {
                        categoryError = "Vui lòng chọn danh mục"
                        return@Button
                    }
                    scope.launch {
                        val txnId = repo.settleJar(
                            goalId = goal.id,
                            categoryId = selectedCategoryId,
                            amount = parsedAmount,
                            date = selectedDate,
                        )
                        if (txnId != null) {
                            onDismiss()
                        } else {
                            settleError = "Không thể thanh toán hũ (kiểm tra tài khoản nguồn hoặc danh mục)"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                OneLineText("Xác nhận chi tiêu", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = selectableCategories,
            selectedId = selectedCategoryId,
            onSelect = {
                selectedCategoryId = it
                categoryError = null
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
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
                    OneLineText("Chọn", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    OneLineText("Hủy", style = MaterialTheme.typography.labelLarge)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
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
            OneLineText(
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
            OneLineText("Ngày", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                OneLineText(formatGoalDate(selectedDate), style = MaterialTheme.typography.labelLarge)
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
                OneLineText("Lưu", style = MaterialTheme.typography.labelLarge)
            }
            if (!isNew) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText("Xóa", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
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
                    OneLineText("Chọn", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    OneLineText("Hủy", style = MaterialTheme.typography.labelLarge)
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
                    OneLineText("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    OneLineText("Hủy")
                }
            },
        )
    }
}
