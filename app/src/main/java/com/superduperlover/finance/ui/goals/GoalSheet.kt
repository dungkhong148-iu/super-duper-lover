package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.DurationUnit
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.JarDuration
import com.superduperlover.finance.core.JarPlanCalculator
import com.superduperlover.finance.core.SegmentedBarModel
import com.superduperlover.finance.core.accountBalance
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.durationOfPeriods
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.goalProgress
import com.superduperlover.finance.core.jarSchedule
import com.superduperlover.finance.core.maxDurationCount
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.add.AccountPickerSheet
import com.superduperlover.finance.ui.add.CategoryPickerSheet
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.CurrencyChips
import com.superduperlover.finance.ui.components.FormDialog
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionHeaderBand
import com.superduperlover.finance.ui.theme.ChiLiTheme
import com.superduperlover.finance.ui.theme.accountColor
import com.superduperlover.finance.ui.theme.chiLiChipBorder
import com.superduperlover.finance.ui.theme.chiLiChipColors
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private fun isQuickDuration(d: JarDuration): Boolean =
    d == JarDuration(7, DurationUnit.DAY) ||
    d == JarDuration(30, DurationUnit.DAY) ||
    d == JarDuration(3, DurationUnit.MONTH) ||
    d == JarDuration(6, DurationUnit.MONTH) ||
    d == JarDuration(1, DurationUnit.YEAR) ||
    (d.unit == DurationUnit.MONTH && d.count == 12)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSheet(
    goal: Goal,
    onDismiss: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val isNew = goal.id == 0L
    val today = remember { LocalDate.now() }

    val allAccounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val allCategories by remember(repo) { repo.categories(kind = CategoryKind.EXPENSE, includeArchived = true) }.collectAsState(initial = emptyList())
    val allTxns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    val jarSavings by remember(repo) { repo.jarSavings() }.collectAsState(initial = emptyList())
    val contributions by remember(repo, goal.id) {
        if (isNew) flowOf(emptyList()) else repo.contributions(goal.id)
    }.collectAsState(initial = emptyList())

    val progress = remember(goal, contributions, today) { goalProgress(goal, contributions, today) }
    val savedSoFar = progress.saved

    var name by remember { mutableStateOf(goal.name) }
    var kind by remember { mutableStateOf(goal.kind) }
    var currency by remember { mutableStateOf(goal.currency) }
    val initialTargetText = remember(goal) {
        if (isNew || goal.target == 0L) "" else amountToInput(goal.target, goal.currency)
    }
    var targetText by remember { mutableStateOf(initialTargetText) }
    var sourceAccountId by remember { mutableStateOf(goal.sourceAccountId) }
    var categoryId by remember { mutableStateOf(goal.categoryId) }
    var autoDeduct by remember { mutableStateOf(goal.autoDeduct) }

    // New jars always have a plan; existing jars without plan show a switch (default OFF).
    var hasPlan by remember { mutableStateOf(isNew || goal.plan != null) }
    var manualDeadline by remember { mutableStateOf(goal.deadline) }

    val initialDuration = remember(goal) {
        goal.plan?.duration ?: JarDuration(30, DurationUnit.DAY)
    }
    var duration by remember { mutableStateOf(initialDuration) }
    var customCountText by remember { mutableStateOf(initialDuration.count.toString()) }
    var isCustom by remember {
        mutableStateOf(goal.plan?.let { !isQuickDuration(it.duration) } ?: false)
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var targetError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val accounts = remember(allAccounts, sourceAccountId) {
        allAccounts.filter { !it.archived || it.id == sourceAccountId }
    }
    val categories = remember(allCategories, categoryId) {
        allCategories.filter { it.kind == CategoryKind.EXPENSE && (!it.archived || it.id == categoryId) }
    }

    val selectedAccount = accounts.firstOrNull { it.id == sourceAccountId }
    val balance = selectedAccount?.let { accountBalance(it, allTxns) } ?: 0L
    val parsedTarget = parseAmount(targetText, currency)

    val schedule = remember(duration, today) { jarSchedule(duration, today) }
    val bar = remember(balance, jarSavings, sourceAccountId, goal.id, parsedTarget) {
        SegmentedBarModel.of(balance, jarSavings, sourceAccountId ?: 0L, goal.id, parsedTarget ?: 0L)
    }

    val goalPlan = goal.plan
    val hasChanges = if (isNew) {
        name.isNotEmpty() || targetText.isNotEmpty() || sourceAccountId != null || categoryId != null
    } else {
        name != goal.name ||
            kind != goal.kind ||
            currency != goal.currency ||
            targetText != initialTargetText ||
            sourceAccountId != goal.sourceAccountId ||
            categoryId != goal.categoryId ||
            autoDeduct != goal.autoDeduct ||
            (hasPlan != (goalPlan != null)) ||
            (hasPlan && goalPlan != null && duration != goalPlan.duration) ||
            (!hasPlan && manualDeadline != goal.deadline)
    }

    val saveEnabled = name.isNotBlank() && targetText.isNotBlank()

    val onSave: () -> Unit = {
        val trimmed = name.trim()
        val parsed = parseAmount(targetText, currency)
        var hasError = false
        if (trimmed.isEmpty()) {
            nameError = "Nhập tên hũ"
            hasError = true
        }
        if (parsed == null || parsed <= 0L) {
            targetError = "Số tiền không hợp lệ"
            hasError = true
        }
        if (hasPlan) {
            if (sourceAccountId == null) {
                accountError = "Chọn tài khoản nguồn"
                hasError = true
            }
            if (categoryId == null) {
                categoryError = "Chọn danh mục"
                hasError = true
            }
        }
        if (!hasError && parsed != null) {
            scope.launch {
                val toSave = if (hasPlan) {
                    if (isNew) {
                        val plan = JarPlanCalculator.plan(duration, 0L, today)
                        val end = jarSchedule(duration, today).endDate
                        goal.copy(
                            name = trimmed,
                            target = parsed,
                            deadline = end,
                            archived = false,
                            currency = currency,
                            kind = kind,
                            sourceAccountId = sourceAccountId,
                            categoryId = categoryId,
                            autoDeduct = autoDeduct,
                            plan = plan,
                        )
                    } else {
                        val replanned = JarPlanCalculator.replan(goal, duration, savedSoFar, today)
                        replanned.copy(
                            name = trimmed,
                            target = parsed,
                            archived = false,
                            currency = currency,
                            kind = kind,
                            sourceAccountId = sourceAccountId,
                            categoryId = categoryId,
                            autoDeduct = autoDeduct,
                        )
                    }
                } else {
                    goal.copy(
                        name = trimmed,
                        target = parsed,
                        deadline = manualDeadline,
                        archived = false,
                        currency = currency,
                        kind = kind,
                        sourceAccountId = null,
                        categoryId = null,
                        autoDeduct = false,
                        plan = null,
                    )
                }
                repo.saveGoal(toSave)
                onDismiss()
            }
        }
    }

    FormDialog(
        title = if (isNew) "Tạo hũ" else "Sửa hũ",
        onClose = onDismiss,
        onSave = onSave,
        saveEnabled = saveEnabled,
        hasChanges = hasChanges,
    ) {
        SectionHeaderBand("Thông tin")
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
                label = { Text("Tên hũ") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            if (isNew && sourceAccountId == null) {
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
                label = { Text("Số tiền cần giữ (${currencySymbol(currency)})") },
                placeholder = { Text("0") },
                singleLine = true,
                isError = targetError != null,
                supportingText = targetError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!isNew && goal.plan == null) {
            SectionHeaderBand("Kế hoạch trích tiền")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OneLineText(
                        text = "Đặt kế hoạch trích tiền",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = hasPlan,
                        onCheckedChange = {
                            hasPlan = it
                            if (!it) {
                                accountError = null
                                categoryError = null
                            }
                        },
                    )
                }
            }
        }

        if (!hasPlan) {
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
                        OneLineText(manualDeadline?.let { formatGoalDate(it) } ?: "Chưa đặt hạn")
                    }
                    if (manualDeadline != null) {
                        TextButton(
                            onClick = { manualDeadline = null },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            OneLineText("Bỏ hạn")
                        }
                    }
                }
            }
        } else {
            SectionHeaderBand("Tài khoản & Danh mục")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    onClick = { showAccountPicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (accountError != null) MaterialTheme.colorScheme.error else ChiLiTheme.colors.outline,
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
                        if (selectedAccount != null) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(accountColor(selectedAccount.colorKey)),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                OneLineText(
                                    text = selectedAccount.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                OneLineText(
                                    text = "Số dư: ${formatMoney(accountBalance(selectedAccount, allTxns), selectedAccount.currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            OneLineText(
                                text = "Chọn tài khoản nguồn",
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
                if (accountError != null) {
                    Text(
                        text = accountError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                val selectedCategory = categories.firstOrNull { it.id == categoryId }
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
            }

            SectionHeaderBand("Thời gian gom tiền")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val quickSelected = !isCustom && isQuickDuration(duration)
                    FilterChip(
                        selected = quickSelected && duration == JarDuration(7, DurationUnit.DAY),
                        onClick = {
                            duration = JarDuration(7, DurationUnit.DAY)
                            customCountText = "7"
                            isCustom = false
                        },
                        label = { OneLineText("1 tuần") },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = quickSelected && duration == JarDuration(7, DurationUnit.DAY)),
                    )
                    FilterChip(
                        selected = quickSelected && duration == JarDuration(30, DurationUnit.DAY),
                        onClick = {
                            duration = JarDuration(30, DurationUnit.DAY)
                            customCountText = "30"
                            isCustom = false
                        },
                        label = { OneLineText("1 tháng") },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = quickSelected && duration == JarDuration(30, DurationUnit.DAY)),
                    )
                    FilterChip(
                        selected = quickSelected && duration == JarDuration(3, DurationUnit.MONTH),
                        onClick = {
                            duration = JarDuration(3, DurationUnit.MONTH)
                            customCountText = "3"
                            isCustom = false
                        },
                        label = { OneLineText("3 tháng") },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = quickSelected && duration == JarDuration(3, DurationUnit.MONTH)),
                    )
                    FilterChip(
                        selected = quickSelected && duration == JarDuration(6, DurationUnit.MONTH),
                        onClick = {
                            duration = JarDuration(6, DurationUnit.MONTH)
                            customCountText = "6"
                            isCustom = false
                        },
                        label = { OneLineText("6 tháng") },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = quickSelected && duration == JarDuration(6, DurationUnit.MONTH)),
                    )
                    FilterChip(
                        selected = quickSelected && (duration == JarDuration(1, DurationUnit.YEAR) || (duration.unit == DurationUnit.MONTH && duration.count == 12)),
                        onClick = {
                            duration = JarDuration(1, DurationUnit.YEAR)
                            customCountText = "1"
                            isCustom = false
                        },
                        label = { OneLineText("1 năm") },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = quickSelected && (duration == JarDuration(1, DurationUnit.YEAR) || (duration.unit == DurationUnit.MONTH && duration.count == 12))),
                    )
                    val customSelected = isCustom || !isQuickDuration(duration)
                    FilterChip(
                        selected = customSelected,
                        onClick = {
                            isCustom = true
                            customCountText = duration.count.toString()
                        },
                        label = { OneLineText("Tùy chỉnh") },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = customSelected),
                    )
                }

                if (isCustom || !isQuickDuration(duration)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = customCountText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(3)
                                customCountText = filtered
                                val countInt = filtered.toIntOrNull()
                                if (countInt != null) {
                                    val max = maxDurationCount(duration.unit)
                                    val clamped = countInt.coerceIn(1, max)
                                    duration = JarDuration(clamped, duration.unit)
                                }
                            },
                            label = { Text("Số lượng") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = duration.unit == DurationUnit.DAY,
                                onClick = {
                                    val max = maxDurationCount(DurationUnit.DAY)
                                    val newCount = duration.count.coerceIn(1, max)
                                    duration = JarDuration(newCount, DurationUnit.DAY)
                                    customCountText = newCount.toString()
                                },
                                label = { OneLineText("Ngày") },
                                colors = chiLiChipColors(),
                                border = chiLiChipBorder(selected = duration.unit == DurationUnit.DAY),
                            )
                            FilterChip(
                                selected = duration.unit == DurationUnit.MONTH,
                                onClick = {
                                    val max = maxDurationCount(DurationUnit.MONTH)
                                    val newCount = duration.count.coerceIn(1, max)
                                    duration = JarDuration(newCount, DurationUnit.MONTH)
                                    customCountText = newCount.toString()
                                },
                                label = { OneLineText("Tháng") },
                                colors = chiLiChipColors(),
                                border = chiLiChipBorder(selected = duration.unit == DurationUnit.MONTH),
                            )
                            FilterChip(
                                selected = duration.unit == DurationUnit.YEAR,
                                onClick = {
                                    val max = maxDurationCount(DurationUnit.YEAR)
                                    val newCount = duration.count.coerceIn(1, max)
                                    duration = JarDuration(newCount, DurationUnit.YEAR)
                                    customCountText = newCount.toString()
                                },
                                label = { OneLineText("Năm") },
                                colors = chiLiChipColors(),
                                border = chiLiChipBorder(selected = duration.unit == DurationUnit.YEAR),
                            )
                        }
                    }
                }

                SegmentedVolumeSlider(
                    schedule = schedule,
                    bar = bar,
                    currency = currency,
                    onPeriodsChange = { newPeriods ->
                        val newDuration = durationOfPeriods(schedule.cycle, newPeriods)
                        duration = newDuration
                        customCountText = newDuration.count.toString()
                        isCustom = !isQuickDuration(newDuration)
                    },
                    target = parsedTarget ?: 0L,
                    baseSaved = savedSoFar,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionHeaderBand("Cài đặt")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OneLineText(
                        text = "Tự động trừ tiền khi đủ",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                    )
                    Switch(
                        checked = autoDeduct,
                        onCheckedChange = { autoDeduct = it },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = accounts,
            selectedId = sourceAccountId,
            onSelect = { accId ->
                sourceAccountId = accId
                val acc = accounts.firstOrNull { it.id == accId }
                if (acc != null) {
                    currency = acc.currency
                }
                accountError = null
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = categories,
            selectedId = categoryId,
            onSelect = { catId ->
                categoryId = catId
                categoryError = null
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }

    if (showDatePicker) {
        val initialMillis = (manualDeadline ?: LocalDate.now()).toEpochDay() * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val epochDay = Math.floorDiv(it, 86_400_000L)
                        manualDeadline = LocalDate.ofEpochDay(epochDay)
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
