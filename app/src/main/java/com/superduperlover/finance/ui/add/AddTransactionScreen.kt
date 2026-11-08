package com.superduperlover.finance.ui.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.applyRate
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.evalAmountExpr
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.core.suggestRate
import com.superduperlover.finance.core.validateTxn
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import com.superduperlover.finance.ui.components.color
import com.superduperlover.finance.ui.components.display
import com.superduperlover.finance.ui.components.icon
import com.superduperlover.finance.ui.components.label
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MILLIS_PER_DAY = 86_400_000L

private enum class PickerSheetType {
    SOURCE_ACCOUNT,
    TARGET_ACCOUNT,
    CATEGORY,
}

private fun hasOperator(text: String): Boolean = text.any { it in "+-*/" }

private fun prettyExpr(text: String): String =
    text.replace('*', '×').replace('/', '÷').replace('-', '−')

private fun currentOperand(text: String): String {
    val lastOpIdx = text.indexOfLast { it in "+-*/" }
    return if (lastOpIdx < 0) text else text.substring(lastOpIdx + 1)
}

/**
 * Redesigned Add / Edit transaction screen (docs/ui-layout.md section 2):
 * 1. Top bar: ✕ · Title · ✓
 * 2. Type tabs: Chi tiêu · Thu nhập · Chuyển khoản
 * 3. Amount header: tinted rounded box with live evaluation and expression
 * 4. Pickers row: Tài khoản | Danh mục (or Từ tài khoản | Đến tài khoản)
 * 5. Top categories chip row + Tất cả chip
 * 6. Compact row: Date button + Note field
 * 7. Cross-currency field (when transferring between different currencies)
 * 8. Fixed calculator pad with evalAmountExpr support
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    txnId: Long,
    onClose: () -> Unit,
    onGoAccounts: () -> Unit,
    initialKind: TxnKind = TxnKind.EXPENSE,
) {
    val repo = LocalRepo.current
    val prefs = LocalAppPrefs.current
    val scope = rememberCoroutineScope()
    val allAccounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val allCategories by remember(repo) { repo.categories(includeArchived = true) }.collectAsState(initial = emptyList())

    var kindName by rememberSaveable { mutableStateOf(initialKind.name) }
    var expr by rememberSaveable { mutableStateOf("") }
    var exprError by rememberSaveable { mutableStateOf<String?>(null) }
    var accountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var epochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var note by rememberSaveable { mutableStateOf("") }
    var receivedText by rememberSaveable { mutableStateOf("") }
    var receivedEdited by rememberSaveable { mutableStateOf(false) }
    var rate by remember { mutableStateOf<Double?>(null) }
    var loaded by rememberSaveable { mutableStateOf(txnId == 0L) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    var showDate by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf(emptyMap<Long, Int>()) }
    var recentUsage by remember { mutableStateOf(emptyMap<Long, Int>()) }
    var pickerSheet by remember { mutableStateOf<PickerSheetType?>(null) }

    val kind = TxnKind.valueOf(kindName)
    val date = LocalDate.ofEpochDay(epochDay)

    LaunchedEffect(txnId) {
        if (!loaded) {
            repo.txns().first().firstOrNull { it.id == txnId }?.let { t ->
                val accounts = repo.accounts(includeArchived = true).first().associateBy { it.id }
                kindName = t.kind.name
                expr = amountToInput(t.amount, accounts[t.accountId]?.currency ?: MAIN_CURRENCY)
                val toInput = t.toAmount?.let { amountToInput(it, accounts[t.toAccountId]?.currency ?: MAIN_CURRENCY) } ?: ""
                receivedText = toInput
                if (toInput.isNotEmpty()) {
                    receivedEdited = true
                }
                accountId = t.accountId
                toAccountId = t.toAccountId
                categoryId = t.categoryId
                epochDay = t.date.toEpochDay()
                note = t.note
            }
            loaded = true
        }
    }

    // New transaction: start on the account used last for this type, else on the first active account.
    fun applyRememberedAccounts(k: TxnKind) {
        val active = allAccounts.filter { !it.archived }
        val from = active.firstOrNull { it.id == prefs.lastAccount(k) } ?: active.firstOrNull()
        accountId = from?.id
        if (k == TxnKind.TRANSFER) {
            toAccountId = active.firstOrNull { it.id == prefs.lastToAccount() && it.id != from?.id }?.id
        }
    }

    LaunchedEffect(allAccounts) {
        if (txnId == 0L && accountId == null) applyRememberedAccounts(kind)
    }

    LaunchedEffect(kind) {
        if (kind != TxnKind.TRANSFER) {
            val catKind = CategoryKind.valueOf(kind.name)
            usage = repo.categoryUsage(catKind)
            recentUsage = repo.categoryUsageRecent(catKind)
        }
    }

    val sourceAccount = allAccounts.firstOrNull { it.id == accountId }
    val targetAccount = allAccounts.firstOrNull { it.id == toAccountId }
    val selectedCategory = allCategories.firstOrNull { it.id == categoryId }
    val currency = sourceAccount?.currency ?: MAIN_CURRENCY
    val crossCurrency = kind == TxnKind.TRANSFER && sourceAccount != null && targetAccount != null &&
        sourceAccount.currency != targetAccount.currency

    LaunchedEffect(accountId, toAccountId, crossCurrency) {
        rate = if (crossCurrency && accountId != null && toAccountId != null) {
            suggestRate(accountId!!, toAccountId!!, repo.txns().first())
        } else {
            null
        }
    }

    LaunchedEffect(expr, rate, crossCurrency, receivedEdited) {
        if (crossCurrency && !receivedEdited && rate != null) {
            val amount = evalAmountExpr(expr, currency)
            receivedText = if (amount != null && amount > 0) {
                amountToInput(applyRate(amount, rate!!), targetAccount.currency)
            } else {
                ""
            }
        }
    }

    val pickAccounts = allAccounts.filter { !it.archived || it.id == accountId || it.id == toAccountId }
    val pickCategories = allCategories
        .filter { it.kind.name == kind.name && (!it.archived || it.id == categoryId) }
        .sortedWith(compareByDescending<Category> { usage[it.id] ?: 0 }.thenBy { it.id })

    val baseCategoryIds = remember(kind, recentUsage, allCategories) {
        if (kind == TxnKind.TRANSFER) emptyList()
        else {
            allCategories
                .filter { it.kind.name == kind.name && !it.archived }
                .sortedWith(compareByDescending<Category> { recentUsage[it.id] ?: 0 }.thenBy { it.id })
                .take(7)
                .map { it.id }
        }
    }

    val displayedCategories = remember(baseCategoryIds, categoryId, allCategories) {
        val baseList = baseCategoryIds.mapNotNull { id -> allCategories.firstOrNull { it.id == id } }
        if (categoryId != null && categoryId !in baseCategoryIds) {
            val selected = allCategories.firstOrNull { it.id == categoryId }
            if (selected != null && selected.kind.name == kind.name) {
                if (baseList.size >= 7) {
                    baseList.take(6) + selected
                } else {
                    baseList + selected
                }
            } else {
                baseList
            }
        } else {
            baseList
        }
    }

    fun errorsAbout(vararg words: String) =
        errors.filter { e -> words.any { e.lowercase().contains(it) } }

    val amountErrors = (exprError?.let { listOf(it) } ?: emptyList()) +
        errors.filter { e -> e.lowercase().contains("số tiền") && !e.lowercase().contains("số tiền nhận") }
    val accountErrors = errorsAbout("tài khoản")
    val categoryErrors = errorsAbout("danh mục")
    val receivedErrors = errors.filter { it.lowercase().contains("số tiền nhận") }

    fun onDigit(d: String) {
        exprError = null
        errors = emptyList()
        val operand = currentOperand(expr)
        if (operand.isEmpty() && d.startsWith("0")) return
        if (currency.decimals > 0 && operand.contains('.')) {
            val fraction = operand.substringAfter('.')
            if (fraction.length + d.length > currency.decimals) return
        }
        if (expr.length + d.length <= 14) {
            expr += d
        }
    }

    fun onDot() {
        if (currency.decimals == 0) return
        exprError = null
        errors = emptyList()
        val operand = currentOperand(expr)
        if (operand.contains('.')) return
        if (operand.isEmpty()) {
            if (expr.length + 2 <= 14) {
                expr += "0."
            }
        } else {
            if (expr.length + 1 <= 14) {
                expr += "."
            }
        }
    }

    fun onOperator(op: Char) {
        if (expr.isEmpty()) return
        exprError = null
        errors = emptyList()
        val lastChar = expr.last()
        if (lastChar in "+-*/") {
            expr = expr.dropLast(1) + op
        } else {
            val clean = if (expr.endsWith('.')) expr.dropLast(1) else expr
            if (clean.isNotEmpty() && clean.length + 1 <= 14) {
                expr = clean + op
            }
        }
    }

    fun onBackspace() {
        exprError = null
        errors = emptyList()
        if (expr.isNotEmpty()) {
            expr = expr.dropLast(1)
        }
    }

    fun onClear() {
        exprError = null
        errors = emptyList()
        expr = ""
    }

    fun onEquals() {
        errors = emptyList()
        val result = evalAmountExpr(expr, currency)
        if (result != null) {
            expr = amountToInput(result, currency)
            exprError = null
        } else {
            exprError = "Biểu thức không hợp lệ"
        }
    }

    fun save() {
        val evaluatedAmount = evalAmountExpr(expr, currency)
        val finalAmount = evaluatedAmount ?: 0L
        if (evaluatedAmount != null) {
            expr = amountToInput(evaluatedAmount, currency)
            exprError = null
        } else if (hasOperator(expr)) {
            exprError = "Biểu thức không hợp lệ"
        }

        val txn = Txn(
            id = txnId,
            kind = kind,
            amount = finalAmount,
            accountId = accountId ?: -1L,
            toAccountId = if (kind == TxnKind.TRANSFER) toAccountId else null,
            categoryId = if (kind == TxnKind.TRANSFER) null else categoryId,
            date = date,
            note = note.trim(),
            toAmount = if (crossCurrency) {
                parseAmount(receivedText, targetAccount.currency)
            } else null,
        )
        val found = validateTxn(txn, allAccounts, allCategories)
        if (found.isEmpty() && evaluatedAmount != null) {
            scope.launch {
                repo.saveTxn(txn)
                prefs.noteTxnSaved()
                if (txnId == 0L) prefs.rememberAccount(txn.kind, txn.accountId, txn.toAccountId)
                onClose()
            }
        } else {
            errors = found
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Đóng")
            }
            OneLineText(
                text = if (txnId == 0L) "Thêm giao dịch" else "Sửa giao dịch",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )
            if (txnId != 0L || WindowInsets.isImeVisible) {
                IconButton(
                    onClick = ::save,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Lưu")
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        if (loaded && allAccounts.none { !it.archived } && txnId == 0L) {
            NoAccounts(onGoAccounts)
            return@Column
        }

        // Middle scrolling content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 2. Type tabs
            TypeTabs(
                selected = kind,
                onPick = { picked ->
                    kindName = picked.name
                    if (txnId == 0L) {
                        applyRememberedAccounts(picked)
                        if (picked == TxnKind.TRANSFER) {
                            receivedEdited = false
                            receivedText = ""
                        }
                    }
                    if (picked == TxnKind.TRANSFER) {
                        categoryId = null
                    } else if (allCategories.firstOrNull { it.id == categoryId }?.kind?.name != picked.name) {
                        categoryId = null
                    }
                    errors = emptyList()
                    exprError = null
                },
            )

            // 3. Amount header
            AmountHeader(
                kind = kind,
                expr = expr,
                currency = currency,
                errors = amountErrors.distinct(),
            )

            // 4. Pickers row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PickerButton(
                    label = if (kind == TxnKind.TRANSFER) "Từ tài khoản" else "Tài khoản",
                    value = sourceAccount?.let { "${it.name} (${it.currency.name})" } ?: "Chọn tài khoản",
                    onClick = { pickerSheet = PickerSheetType.SOURCE_ACCOUNT },
                    modifier = Modifier.weight(1f),
                )
                PickerButton(
                    label = if (kind == TxnKind.TRANSFER) "Đến tài khoản" else "Danh mục",
                    value = if (kind == TxnKind.TRANSFER) {
                        targetAccount?.let { "${it.name} (${it.currency.name})" } ?: "Chọn tài khoản"
                    } else {
                        selectedCategory?.name ?: "Chọn danh mục"
                    },
                    onClick = {
                        pickerSheet = if (kind == TxnKind.TRANSFER) {
                            PickerSheetType.TARGET_ACCOUNT
                        } else {
                            PickerSheetType.CATEGORY
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            if (accountErrors.isNotEmpty() || (kind != TxnKind.TRANSFER && categoryErrors.isNotEmpty())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        accountErrors.forEach { ErrorText(it) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (kind != TxnKind.TRANSFER) {
                            categoryErrors.forEach { ErrorText(it) }
                        }
                    }
                }
            }

            // 5. Fixed category grid (hidden for transfer)
            if (kind != TxnKind.TRANSFER) {
                CategoryGrid(
                    categories = displayedCategories,
                    selectedId = categoryId,
                    kindColor = kind.color(),
                    onSelect = { id ->
                        categoryId = id
                        errors = errors.filterNot { it.lowercase().contains("danh mục") }
                    },
                    onMore = { pickerSheet = PickerSheetType.CATEGORY },
                )
            }

            // 6. Compact row: Date button + Note field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showDate = true },
                    modifier = Modifier.heightIn(min = 56.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    OneLineText(
                        text = if (date == LocalDate.now()) "Hôm nay" else date.display(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
            }

            // 7. Cross-currency field (transfer only)
            if (crossCurrency) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = receivedText,
                        onValueChange = {
                            receivedEdited = true
                            receivedText = it
                            errors = emptyList()
                        },
                        label = { Text("Số tiền nhận (${currencySymbol(targetAccount.currency)})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (rate != null && !receivedEdited) {
                        Text(
                            text = "Tự điền theo lần chuyển gần nhất, bạn có thể sửa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    }
                    receivedErrors.forEach { ErrorText(it) }
                }
            }
        }

        // 8. Fixed Calculator Pad (hidden when soft keyboard is open)
        if (!WindowInsets.isImeVisible) {
            AmountPad(
                currency = currency,
                onDigit = ::onDigit,
                onDot = ::onDot,
                onOperator = ::onOperator,
                onBackspace = ::onBackspace,
                onClear = ::onClear,
                onEquals = ::onEquals,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                onSave = if (txnId == 0L) ::save else null,
            )
        }
    }

    // ModalBottomSheets for pickers
    when (pickerSheet) {
        PickerSheetType.SOURCE_ACCOUNT -> {
            AccountPickerSheet(
                accounts = pickAccounts,
                selectedId = accountId,
                onSelect = {
                    if (accountId != it) {
                        accountId = it
                        if (txnId == 0L) {
                            receivedEdited = false
                            receivedText = ""
                        }
                    }
                    errors = emptyList()
                },
                onDismiss = { pickerSheet = null },
            )
        }
        PickerSheetType.TARGET_ACCOUNT -> {
            AccountPickerSheet(
                accounts = pickAccounts,
                selectedId = toAccountId,
                onSelect = {
                    if (toAccountId != it) {
                        toAccountId = it
                        if (txnId == 0L) {
                            receivedEdited = false
                            receivedText = ""
                        }
                    }
                    errors = emptyList()
                },
                onDismiss = { pickerSheet = null },
            )
        }
        PickerSheetType.CATEGORY -> {
            CategoryPickerSheet(
                categories = pickCategories,
                selectedId = categoryId,
                onSelect = {
                    categoryId = it
                    errors = emptyList()
                },
                onDismiss = { pickerSheet = null },
            )
        }
        null -> Unit
    }

    // Date picker dialog
    if (showDate) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = epochDay * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        epochDay = Math.floorDiv(it, MILLIS_PER_DAY)
                    }
                    showDate = false
                }) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) {
                    Text("Hủy")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TypeTabs(
    selected: TxnKind,
    onPick: (TxnKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TxnKind.entries.forEach { k ->
            val on = k == selected
            val c = k.color()
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = on,
                        role = Role.Tab,
                        onClick = { onPick(k) },
                    ),
                shape = RoundedCornerShape(12.dp),
                color = if (on) c.copy(alpha = 0.16f) else Color.Transparent,
                border = BorderStroke(
                    width = if (on) 2.dp else 1.dp,
                    color = if (on) c else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = k.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (on) c else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    OneLineText(
                        text = k.label(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) c else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountHeader(
    kind: TxnKind,
    expr: String,
    currency: Currency,
    errors: List<String>,
    modifier: Modifier = Modifier,
) {
    val kindColor = kind.color()
    val hasOp = hasOperator(expr)
    val shownAmount: Long = if (!hasOp) {
        parseAmount(expr, currency) ?: 0L
    } else {
        evalAmountExpr(expr, currency) ?: 0L
    }
    val formatted = formatMoney(shownAmount, currency)
    val sign = when (kind) {
        TxnKind.EXPENSE -> "-"
        TxnKind.INCOME -> "+"
        TxnKind.TRANSFER -> ""
    }
    val displayText = if (sign.isNotEmpty()) "$sign $formatted" else formatted

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(kindColor.copy(alpha = 0.16f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                OneLineText(
                    text = displayText,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = kindColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hasOp) {
                    Spacer(Modifier.height(4.dp))
                    OneLineText(
                        text = prettyExpr(expr),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        errors.forEach { err ->
            ErrorText(err)
        }
    }
}

@Composable
private fun PickerButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            OneLineText(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun NoAccounts(onGoAccounts: () -> Unit) {
    SectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Chưa có tài khoản. Hãy thêm một tài khoản trước khi ghi giao dịch.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(
                onClick = onGoAccounts,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Thêm tài khoản")
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedId: Long?,
    kindColor: Color,
    onSelect: (Long) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moreIndex = categories.size
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (i in 0 until 4) {
                when {
                    i < moreIndex -> {
                        val c = categories[i]
                        CategoryGridCell(
                            category = c,
                            selected = c.id == selectedId,
                            kindColor = kindColor,
                            onSelect = { onSelect(c.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    i == moreIndex -> {
                        CategoryGridMoreCell(
                            onClick = onMore,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    else -> {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (i in 4 until 8) {
                when {
                    i < moreIndex -> {
                        val c = categories[i]
                        CategoryGridCell(
                            category = c,
                            selected = c.id == selectedId,
                            kindColor = kindColor,
                            onSelect = { onSelect(c.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    i == moreIndex -> {
                        CategoryGridMoreCell(
                            onClick = onMore,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    else -> {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGridCell(
    category: Category,
    selected: Boolean,
    kindColor: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .semantics {
                contentDescription = category.name
            },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) kindColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) kindColor else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = CategoryIcons.get(category.icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) kindColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            OneLineText(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) kindColor else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CategoryGridMoreCell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 56.dp)
            .semantics {
                contentDescription = "Khác"
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            OneLineText(
                text = "Khác...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
