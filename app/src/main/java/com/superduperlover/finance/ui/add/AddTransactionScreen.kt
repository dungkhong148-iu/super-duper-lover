package com.superduperlover.finance.ui.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.validateTxn
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.color
import com.superduperlover.finance.ui.components.display
import com.superduperlover.finance.ui.components.icon
import com.superduperlover.finance.ui.components.label
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MAX_DIGITS = 12
private const val DELETE_KEY = "Xóa"
private const val MILLIS_PER_DAY = 86_400_000L

/**
 * One screen: type -> amount (number pad) -> account -> category -> save.
 * [txnId] 0 = new transaction; otherwise edits that transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(txnId: Long, onClose: () -> Unit, onGoAccounts: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val allAccounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val allCategories by remember(repo) { repo.categories(includeArchived = true) }.collectAsState(initial = emptyList())

    var kindName by rememberSaveable { mutableStateOf(TxnKind.EXPENSE.name) }
    var digits by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var epochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var note by rememberSaveable { mutableStateOf("") }
    var receivedText by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(txnId == 0L) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    var showDate by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf(emptyMap<Long, Int>()) }

    val kind = TxnKind.valueOf(kindName)
    val date = LocalDate.ofEpochDay(epochDay)

    LaunchedEffect(txnId) {
        if (!loaded) {
            repo.txns().first().firstOrNull { it.id == txnId }?.let { t ->
                val accounts = repo.accounts(includeArchived = true).first().associateBy { it.id }
                kindName = t.kind.name
                digits = amountToInput(t.amount, accounts[t.accountId]?.currency ?: MAIN_CURRENCY)
                receivedText = t.toAmount?.let { amountToInput(it, accounts[t.toAccountId]?.currency ?: MAIN_CURRENCY) } ?: ""
                accountId = t.accountId
                toAccountId = t.toAccountId
                categoryId = t.categoryId
                epochDay = t.date.toEpochDay()
                note = t.note
            }
            loaded = true
        }
    }
    // New transaction: start on the first active account.
    LaunchedEffect(allAccounts) {
        if (txnId == 0L && accountId == null) {
            allAccounts.firstOrNull { !it.archived }?.let { accountId = it.id }
        }
    }
    LaunchedEffect(kind) {
        if (kind != TxnKind.TRANSFER) usage = repo.categoryUsage(CategoryKind.valueOf(kind.name))
    }

    val pickAccounts = allAccounts.filter { !it.archived || it.id == accountId || it.id == toAccountId }
    val pickCategories = allCategories
        .filter { it.kind.name == kind.name && (!it.archived || it.id == categoryId) }
        .sortedWith(compareByDescending<Category> { usage[it.id] ?: 0 }.thenBy { it.id })

    // The amount is in the currency of the (source) account; a transfer between two currencies also takes
    // the amount received, in the target account's currency.
    val sourceAccount = allAccounts.firstOrNull { it.id == accountId }
    val targetAccount = allAccounts.firstOrNull { it.id == toAccountId }
    val currency = sourceAccount?.currency ?: MAIN_CURRENCY
    val crossCurrency = kind == TxnKind.TRANSFER && sourceAccount != null && targetAccount != null &&
        sourceAccount.currency != targetAccount.currency
    val amount = parseAmount(digits, currency)
    val kindColor = kind.color()

    // validateTxn returns plain messages; show each one next to the field it is about.
    fun errorsAbout(vararg words: String) =
        errors.filter { e -> words.any { e.lowercase().contains(it) } }
    val amountErrors = errorsAbout("số tiền")
    val accountErrors = errorsAbout("tài khoản")
    val categoryErrors = errorsAbout("danh mục")

    fun typeDigits(d: String) {
        if (d == ".") {
            // decimal point: only for currencies with decimals (USD), once
            if (currency.decimals == 0 || digits.contains('.')) return
            digits = if (digits.isEmpty()) "0." else "$digits."
            errors = emptyList()
            return
        }
        if (digits.isEmpty() && d.startsWith("0")) return
        val fraction = digits.substringAfter('.', "")
        if (digits.contains('.') && fraction.length + d.length > currency.decimals) return
        if (digits.length + d.length <= MAX_DIGITS) {
            digits += d
            errors = emptyList()
        }
    }

    fun save() {
        val txn = Txn(
            id = txnId,
            kind = kind,
            amount = amount ?: 0L,
            accountId = accountId ?: -1L,
            toAccountId = if (kind == TxnKind.TRANSFER) toAccountId else null,
            categoryId = if (kind == TxnKind.TRANSFER) null else categoryId,
            date = date,
            note = note.trim(),
            toAmount = if (crossCurrency) targetAccount?.let { parseAmount(receivedText, it.currency) } else null,
        )
        val found = validateTxn(txn, allAccounts, allCategories)
        if (found.isEmpty()) {
            scope.launch {
                repo.saveTxn(txn)
                onClose()
            }
        } else {
            errors = found
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                if (txnId == 0L) "Thêm giao dịch" else "Sửa giao dịch",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        if (loaded && allAccounts.none { !it.archived } && txnId == 0L) {
            NoAccounts(onGoAccounts)
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KindSwitch(kind) { picked ->
                kindName = picked.name
                if (picked == TxnKind.TRANSFER) categoryId = null
                else if (allCategories.firstOrNull { it.id == categoryId }?.kind?.name != picked.name) categoryId = null
                errors = emptyList()
            }

            Column {
                Text(
                    formatMoney(amount ?: 0L, currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (amount != null) kindColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                amountErrors.forEach { ErrorText(it) }
            }

            AccountPicker(
                label = if (kind == TxnKind.TRANSFER) "Từ tài khoản" else "Tài khoản",
                accounts = pickAccounts,
                selectedId = accountId,
                onPick = { accountId = it; errors = emptyList() },
            )
            if (kind == TxnKind.TRANSFER) {
                AccountPicker(
                    label = "Đến tài khoản",
                    accounts = pickAccounts,
                    selectedId = toAccountId,
                    onPick = { toAccountId = it; errors = emptyList() },
                )
            }
            accountErrors.forEach { ErrorText(it) }
            if (crossCurrency && targetAccount != null) {
                OutlinedTextField(
                    value = receivedText,
                    onValueChange = { receivedText = it; errors = emptyList() },
                    label = { Text("Số tiền nhận (${currencySymbol(targetAccount.currency)})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (kind != TxnKind.TRANSFER) {
                Text("Danh mục", style = MaterialTheme.typography.labelLarge)
                CategoryGrid(pickCategories, categoryId) { categoryId = it; errors = emptyList() }
                categoryErrors.forEach { ErrorText(it) }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDate = true },
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (date == LocalDate.now()) "Hôm nay" else date.display())
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NumberPad(decimalPoint = currency.decimals > 0, onDigits = ::typeDigits, onBackspace = { digits = digits.dropLast(1); errors = emptyList() })
            Button(
                onClick = ::save,
                colors = ButtonDefaults.buttonColors(containerColor = kindColor, contentColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Lưu ${kind.label().lowercase()}", style = MaterialTheme.typography.titleMedium) }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { epochDay = Math.floorDiv(it, MILLIS_PER_DAY) }
                    showDate = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Hủy") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun NoAccounts(onGoAccounts: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Chưa có tài khoản. Hãy thêm một tài khoản trước khi ghi giao dịch.")
            Button(onClick = onGoAccounts, modifier = Modifier.heightIn(min = 48.dp)) { Text("Thêm tài khoản") }
        }
    }
}

/** Three full-label buttons. Colour is backed by an icon and the label, never the only cue. */
@Composable
private fun KindSwitch(selected: TxnKind, onPick: (TxnKind) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TxnKind.entries.forEach { k ->
            val on = k == selected
            val c = k.color()
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onPick(k) }),
                shape = RoundedCornerShape(12.dp),
                color = if (on) c.copy(alpha = 0.16f) else Color.Transparent,
                border = BorderStroke(if (on) 2.dp else 1.dp, if (on) c else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(k.icon(), contentDescription = null, tint = c)
                    Text(
                        k.label(),
                        color = if (on) c else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountPicker(label: String, accounts: List<Account>, selectedId: Long?, onPick: (Long) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            accounts.forEach { a ->
                FilterChip(
                    selected = a.id == selectedId,
                    onClick = { onPick(a.id) },
                    label = { Text("${a.name} · ${a.currency.name}") },
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(categories: List<Category>, selectedId: Long?, onPick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(4).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { c ->
                    val on = c.id == selectedId
                    Surface(
                        onClick = { onPick(c.id) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 72.dp)
                            .semantics { this.selected = on },
                        shape = RoundedCornerShape(12.dp),
                        color = if (on) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (on) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    ) {
                        Column(
                            Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(CategoryIcons.get(c.icon), contentDescription = null)
                            Text(
                                c.name,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
                repeat(4 - rowItems.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun NumberPad(decimalPoint: Boolean, onDigits: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf(if (decimalPoint) "." else "000", "0", DELETE_KEY),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    Surface(
                        onClick = { if (key == DELETE_KEY) onBackspace() else onDigits(key) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .semantics { if (key == DELETE_KEY) contentDescription = "Xóa số cuối" },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(key, fontSize = if (key == DELETE_KEY) 16.sp else 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
