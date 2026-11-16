package com.superduperlover.finance.ui.debts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.Debt
import com.superduperlover.finance.core.DebtKind
import com.superduperlover.finance.core.DebtPayment
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.accountBalance
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.debtPaid
import com.superduperlover.finance.core.debtProgress
import com.superduperlover.finance.core.debtRemaining
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.isDebtSettled
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.add.AccountPickerSheet
import com.superduperlover.finance.ui.components.CheckRow
import com.superduperlover.finance.ui.components.ChoiceButtons
import com.superduperlover.finance.ui.components.CurrencyChips
import com.superduperlover.finance.ui.components.FormDialog
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionHeaderBand
import com.superduperlover.finance.ui.components.dayMonthYear
import com.superduperlover.finance.ui.home.moneyText
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.accountColor
import com.superduperlover.finance.ui.theme.v3Shadow
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDebtDialog(
    onDismiss: () -> Unit,
    accounts: List<Account>,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val c = ChiLiV3.colors
    val allTxns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())

    var kind by remember { mutableStateOf(DebtKind.OWE) }
    var title by remember { mutableStateOf("") }
    var person by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(MAIN_CURRENCY) }
    var memoOnly by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var hasDueDate by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var note by remember { mutableStateOf("") }

    var showAccountPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var personError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }

    val parsedTotal = parseAmount(totalText, currency)
    val saveEnabled = title.isNotBlank() && person.isNotBlank() && parsedTotal != null && (memoOnly || selectedAccountId != null)
    val hasChanges = title.isNotBlank() || person.isNotBlank() || totalText.isNotBlank() || note.isNotBlank() || selectedAccountId != null

    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }

    val onSave: () -> Unit = {
        if (title.isBlank()) titleError = "Nhập nội dung"
        if (person.isBlank()) personError = "Nhập tên người"
        if (parsedTotal == null || parsedTotal <= 0) totalError = "Số tiền không hợp lệ"
        if (!memoOnly && selectedAccountId == null) accountError = "Chọn tài khoản"

        val total = parsedTotal
        if (saveEnabled && total != null) {
            scope.launch {
                repo.createDebt(
                    Debt(
                        id = 0,
                        kind = kind,
                        title = title.trim(),
                        person = person.trim(),
                        total = total,
                        currency = currency,
                        startDate = startDate,
                        dueDate = if (hasDueDate) dueDate else null,
                        note = note.trim(),
                    ),
                    accountId = if (memoOnly) null else selectedAccountId,
                )
                onDismiss()
            }
        }
    }

    FormDialog(
        title = "Thêm khoản nợ",
        onClose = onDismiss,
        onSave = onSave,
        saveEnabled = saveEnabled,
        hasChanges = hasChanges,
    ) {
        SectionHeaderBand("Loại khoản nợ")
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ChoiceButtons(
                options = listOf("Mình nợ", "Cho mượn"),
                selectedIndex = if (kind == DebtKind.OWE) 0 else 1,
                onSelect = { kind = if (it == 0) DebtKind.OWE else DebtKind.LENT },
            )
        }

        SectionHeaderBand("Thông tin")
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = { Text("Nội dung") },
                placeholder = { Text("Ví dụ: Vay mua xe, Mượn tiền cơm...") },
                singleLine = true,
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = person,
                onValueChange = {
                    person = it
                    personError = null
                },
                label = { Text(if (kind == DebtKind.OWE) "Chủ nợ (người bạn nợ)" else "Người vay (người mượn bạn)") },
                placeholder = { Text("Tên người hoặc tổ chức") },
                singleLine = true,
                isError = personError != null,
                supportingText = personError?.let { { Text(it) } },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeaderBand("Số tiền")
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CurrencyChips(
                selected = currency,
                onSelect = {
                    currency = it
                    selectedAccountId = null
                },
                label = "Loại tiền",
            )
            OutlinedTextField(
                value = totalText,
                onValueChange = {
                    totalText = it
                    totalError = null
                },
                label = { Text("Số tiền (${currencySymbol(currency)})") },
                placeholder = { Text("0") },
                singleLine = true,
                isError = totalError != null,
                supportingText = totalError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeaderBand("Tài khoản ghi nhận")
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CheckRow(
                text = "Chỉ ghi nhớ (khoản cũ, không đụng tài khoản)",
                subtitle = "Khoản nợ từ trước, không tạo giao dịch thu/chi vào tài khoản",
                checked = memoOnly,
                onChange = {
                    memoOnly = it
                    if (it) {
                        selectedAccountId = null
                        accountError = null
                    }
                },
            )
            if (!memoOnly) {
                val accountBorderColor = if (accountError != null) MaterialTheme.colorScheme.error else c.cardBorder
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showAccountPicker = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accountBorderColor),
                    color = c.cardActive,
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
                                    color = c.onCard,
                                )
                                OneLineText(
                                    text = "Số dư: ${formatMoney(accountBalance(selectedAccount, allTxns), selectedAccount.currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.onSheetMuted,
                                )
                            }
                        } else {
                            OneLineText(
                                text = "Chọn tài khoản ghi nhận",
                                style = MaterialTheme.typography.bodyLarge,
                                color = c.onSheetMuted,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (accountError != null) {
                    Text(
                        text = accountError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }

        SectionHeaderBand("Thời gian")
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showStartDatePicker = true },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, c.cardBorder),
                color = c.cardActive,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OneLineText("Ngày bắt đầu", style = MaterialTheme.typography.bodyMedium, color = c.onSheetMuted)
                    OneLineText(startDate.dayMonthYear(), style = MaterialTheme.typography.bodyLarge, color = c.onCard)
                }
            }
            CheckRow(
                text = "Có ngày hẹn trả",
                checked = hasDueDate,
                onChange = {
                    hasDueDate = it
                    if (it && dueDate == null) dueDate = LocalDate.now().plusMonths(1)
                },
            )
            if (hasDueDate) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showDueDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, c.cardBorder),
                    color = c.cardActive,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OneLineText("Ngày hẹn trả", style = MaterialTheme.typography.bodyMedium, color = c.onSheetMuted)
                        OneLineText((dueDate ?: LocalDate.now()).dayMonthYear(), style = MaterialTheme.typography.bodyLarge, color = c.onCard)
                    }
                }
            }
        }

        SectionHeaderBand("Ghi chú")
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú thêm (tùy chọn)") },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = accounts.filter { !it.archived && it.currency == currency },
            selectedId = selectedAccountId,
            onSelect = {
                selectedAccountId = it
                accountError = null
            },
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showStartDatePicker) {
        val initialMillis = startDate.toEpochDay() * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val epochDay = Math.floorDiv(it, 86_400_000L)
                        startDate = LocalDate.ofEpochDay(epochDay)
                    }
                    showStartDatePicker = false
                }) {
                    OneLineText("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    OneLineText("Hủy")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDueDatePicker) {
        val initialMillis = (dueDate ?: LocalDate.now()).toEpochDay() * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val epochDay = Math.floorDiv(it, 86_400_000L)
                        dueDate = LocalDate.ofEpochDay(epochDay)
                    }
                    showDueDatePicker = false
                }) {
                    OneLineText("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    OneLineText("Hủy")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailDialog(
    debt: Debt,
    payments: List<DebtPayment>,
    accounts: List<Account>,
    accountMap: Map<Long, Account>,
    onDismiss: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val prefs = LocalAppPrefs.current
    val c = ChiLiV3.colors

    val thisPayments = remember(payments, debt.id) {
        payments.filter { it.debtId == debt.id }.sortedByDescending { it.date }
    }
    val paid = debtPaid(debt, thisPayments)
    val remaining = debtRemaining(debt, thisPayments)
    val progress = debtProgress(debt, thisPayments)
    val isSettled = isDebtSettled(debt, thisPayments)

    var title by remember(debt) { mutableStateOf(debt.title) }
    var person by remember(debt) { mutableStateOf(debt.person) }
    var totalText by remember(debt) { mutableStateOf(amountToInput(debt.total, debt.currency)) }
    var hasDueDate by remember(debt) { mutableStateOf(debt.dueDate != null) }
    var dueDate by remember(debt) { mutableStateOf(debt.dueDate) }
    var note by remember(debt) { mutableStateOf(debt.note) }
    var totalError by remember { mutableStateOf<String?>(null) }

    var showRecordPayment by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDeleteDebtConfirm by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<DebtPayment?>(null) }

    val parsedTotal = parseAmount(totalText, debt.currency)
    val hasChanges = title != debt.title || person != debt.person || parsedTotal != debt.total || (if (hasDueDate) dueDate else null) != debt.dueDate || note != debt.note
    val saveEnabled = title.isNotBlank() && person.isNotBlank() && parsedTotal != null && parsedTotal >= paid && parsedTotal > 0

    val onSave: () -> Unit = {
        if (parsedTotal == null || parsedTotal <= 0) {
            totalError = "Số tiền không hợp lệ"
        } else if (parsedTotal < paid) {
            totalError = "Tổng tiền không thể nhỏ hơn số tiền đã trả (${moneyText(paid, debt.currency, false)})"
        } else {
            scope.launch {
                repo.updateDebt(
                    debt.copy(
                        title = title.trim(),
                        person = person.trim(),
                        total = parsedTotal,
                        dueDate = if (hasDueDate) dueDate else null,
                        note = note.trim(),
                    ),
                )
                onDismiss()
            }
        }
    }

    FormDialog(
        title = if (debt.kind == DebtKind.OWE) "Khoản nợ" else "Khoản cho mượn",
        onClose = onDismiss,
        onSave = onSave,
        saveEnabled = saveEnabled,
        hasChanges = hasChanges,
    ) {
        SectionHeaderBand("Tiến độ")
        val progShape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .v3Shadow(c.shadow1, progShape)
                .clip(progShape)
                .background(c.cardActive)
                .border(1.dp, c.cardBorder, progShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val paidLabel = if (debt.kind == DebtKind.OWE) "Đã trả: " else "Đã thu: "
                Text(
                    text = paidLabel + moneyText(paid, debt.currency, prefs.hideBalance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSheetMuted,
                )
                Text(
                    text = "Tổng: " + moneyText(debt.total, debt.currency, prefs.hideBalance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSheetMuted,
                )
            }
            Text(
                text = "Còn lại: " + moneyText(remaining, debt.currency, prefs.hideBalance),
                style = MaterialTheme.typography.headlineSmall,
                color = if (isSettled) c.accent else c.onCard,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (debt.kind == DebtKind.OWE) c.expense else c.accent,
                trackColor = c.card,
            )
        }

        SectionHeaderBand("Lịch sử thanh toán")
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val actionText = if (debt.kind == DebtKind.OWE) "Ghi trả" else "Ghi thu"
            Button(
                onClick = { showRecordPayment = true },
                enabled = !isSettled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(actionText, style = MaterialTheme.typography.labelLarge)
            }
            if (isSettled) {
                Text(
                    text = "Khoản nợ đã được thanh toán xong",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.accent,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            if (thisPayments.isEmpty()) {
                Text(
                    text = "Chưa có lượt thanh toán nào.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSheetMuted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                thisPayments.forEach { payment ->
                    val payShape = RoundedCornerShape(12.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(payShape),
                        shape = payShape,
                        color = c.card,
                        border = BorderStroke(1.dp, c.cardBorder),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                OneLineText(
                                    text = payment.date.dayMonthYear(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = c.onSheetMuted,
                                )
                                val accName = payment.accountId?.let { accountMap[it]?.name } ?: "Chỉ ghi nhớ"
                                OneLineText(
                                    text = accName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.onCard,
                                )
                            }
                            Text(
                                text = moneyText(payment.amount, debt.currency, prefs.hideBalance),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (debt.kind == DebtKind.OWE) c.expense else c.income,
                            )
                            IconButton(
                                onClick = { paymentToDelete = payment },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Xóa",
                                    tint = c.onSheetMuted,
                                )
                            }
                        }
                    }
                }
            }
        }

        SectionHeaderBand("Thông tin khoản nợ")
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nội dung") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = person,
                onValueChange = { person = it },
                label = { Text(if (debt.kind == DebtKind.OWE) "Chủ nợ" else "Người vay") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = totalText,
                onValueChange = {
                    totalText = it
                    totalError = null
                },
                label = { Text("Tổng tiền (${currencySymbol(debt.currency)})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = totalError != null,
                supportingText = totalError?.let { { Text(it) } } ?: { Text("Tối thiểu: ${moneyText(paid, debt.currency, false)}") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            CheckRow(
                text = "Có ngày hẹn trả",
                checked = hasDueDate,
                onChange = {
                    hasDueDate = it
                    if (it && dueDate == null) dueDate = LocalDate.now().plusMonths(1)
                },
            )
            if (hasDueDate) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showDueDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, c.cardBorder),
                    color = c.cardActive,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OneLineText("Ngày hẹn trả", style = MaterialTheme.typography.bodyMedium, color = c.onSheetMuted)
                        OneLineText((dueDate ?: LocalDate.now()).dayMonthYear(), style = MaterialTheme.typography.bodyLarge, color = c.onCard)
                    }
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú") },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeaderBand("Xóa")
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            OutlinedButton(
                onClick = { showDeleteDebtConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text("Xóa khoản nợ", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (paymentToDelete != null) {
        val p = paymentToDelete!!
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("Xóa lượt thanh toán?") },
            text = { Text("Khoản tiền ${moneyText(p.amount, debt.currency, false)} sẽ được mở lại.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repo.deleteDebtPayment(p.id)
                        }
                        paymentToDelete = null
                    },
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text("Hủy")
                }
            },
        )
    }

    if (showDeleteDebtConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteDebtConfirm = false },
            title = { Text("Xóa khoản nợ?") },
            text = { Text("Toàn bộ các lượt ghi trả/thu và giao dịch liên quan sẽ bị xóa.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repo.deleteDebt(debt.id)
                            onDismiss()
                        }
                        showDeleteDebtConfirm = false
                    },
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDebtConfirm = false }) {
                    Text("Hủy")
                }
            },
        )
    }

    if (showDueDatePicker) {
        val initialMillis = (dueDate ?: LocalDate.now()).toEpochDay() * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val epochDay = Math.floorDiv(it, 86_400_000L)
                        dueDate = LocalDate.ofEpochDay(epochDay)
                    }
                    showDueDatePicker = false
                }) {
                    OneLineText("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    OneLineText("Hủy")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRecordPayment) {
        RecordPaymentSheet(
            debt = debt,
            remaining = remaining,
            accounts = accounts,
            onDismiss = { showRecordPayment = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentSheet(
    debt: Debt,
    remaining: Long,
    accounts: List<Account>,
    onDismiss: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val c = ChiLiV3.colors
    val allTxns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())

    var amountText by remember { mutableStateOf(amountToInput(remaining, debt.currency)) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var memoOnly by remember { mutableStateOf(debt.accountId == null) }
    var selectedAccountId by remember { mutableStateOf<Long?>(debt.accountId) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }

    val availableAccounts = remember(accounts, debt.currency) {
        accounts.filter { !it.archived && it.currency == debt.currency }
    }
    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OneLineText(
                text = if (debt.kind == DebtKind.OWE) "Ghi nhận trả nợ" else "Ghi nhận thu nợ",
                style = MaterialTheme.typography.titleLarge,
                color = c.onSheet,
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                label = { Text("Số tiền (${currencySymbol(debt.currency)})") },
                supportingText = amountError?.let { { Text(it) } } ?: { Text("Tối đa: ${moneyText(remaining, debt.currency, false)}") },
                isError = amountError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, c.cardBorder),
                color = c.cardActive,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OneLineText("Ngày ghi nhận", style = MaterialTheme.typography.bodyMedium, color = c.onSheetMuted)
                    OneLineText(date.dayMonthYear(), style = MaterialTheme.typography.bodyLarge, color = c.onCard)
                }
            }

            CheckRow(
                text = "Chỉ ghi nhớ (không đụng tài khoản)",
                subtitle = "Không tạo giao dịch thu/chi vào tài khoản",
                checked = memoOnly,
                onChange = {
                    memoOnly = it
                    if (it) {
                        selectedAccountId = null
                        accountError = null
                    }
                },
            )

            if (!memoOnly) {
                val accountBorderColor = if (accountError != null) MaterialTheme.colorScheme.error else c.cardBorder
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showAccountPicker = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accountBorderColor),
                    color = c.cardActive,
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
                                    color = c.onCard,
                                )
                                OneLineText(
                                    text = "Số dư: ${formatMoney(accountBalance(selectedAccount, allTxns), selectedAccount.currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.onSheetMuted,
                                )
                            }
                        } else {
                            OneLineText(
                                text = "Chọn tài khoản",
                                style = MaterialTheme.typography.bodyLarge,
                                color = c.onSheetMuted,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (accountError != null) {
                    Text(
                        text = accountError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }

            Button(
                onClick = {
                    val parsed = parseAmount(amountText, debt.currency)
                    if (parsed == null || parsed < 1 || parsed > remaining) {
                        amountError = "Số tiền phải từ 1 đến ${moneyText(remaining, debt.currency, false)}"
                    } else if (!memoOnly && selectedAccountId == null) {
                        accountError = "Chọn tài khoản"
                    } else {
                        scope.launch {
                            repo.addDebtPayment(debt.id, parsed, date, if (memoOnly) null else selectedAccountId)
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                OneLineText("Lưu", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val initialMillis = date.toEpochDay() * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val epochDay = Math.floorDiv(it, 86_400_000L)
                        date = LocalDate.ofEpochDay(epochDay)
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

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = availableAccounts,
            selectedId = selectedAccountId,
            onSelect = {
                selectedAccountId = it
                accountError = null
            },
            onDismiss = { showAccountPicker = false },
        )
    }
}
