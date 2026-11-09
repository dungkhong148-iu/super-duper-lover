package com.superduperlover.finance.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.ACCOUNT_COLOR_KEYS
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.accountBalance
import com.superduperlover.finance.core.amountToInput
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.core.defaultAccountColorKey
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.parseAmount
import com.superduperlover.finance.core.reconcileKind
import kotlin.math.abs
import com.superduperlover.finance.data.DeleteResult
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CurrencyChips
import com.superduperlover.finance.ui.components.FormDialog
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionHeaderBand
import com.superduperlover.finance.ui.components.label
import com.superduperlover.finance.ui.theme.OnAccountTile
import com.superduperlover.finance.ui.theme.accountColor
import com.superduperlover.finance.ui.theme.accountTileColor
import com.superduperlover.finance.ui.theme.chiLiChipBorder
import com.superduperlover.finance.ui.theme.chiLiChipColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val ColorDescriptions = mapOf(
    "emerald" to "Xanh lục",
    "teal" to "Xanh ngọc",
    "sky" to "Xanh da trời",
    "indigo" to "Chàm",
    "violet" to "Tím",
    "rose" to "Hồng",
    "amber" to "Cam vàng",
    "slate" to "Xám",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountSheet(account: Account, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val isNew = account.id == 0L

    var name by remember { mutableStateOf(account.name) }
    var type by remember { mutableStateOf(account.type) }
    var currency by remember { mutableStateOf(account.currency) }
    var userPickedColor by remember { mutableStateOf(!isNew) }
    var selectedColorKey by remember { mutableStateOf(account.colorKey) }

    // The currency cannot change once the account has transactions.
    var hasTxns by remember { mutableStateOf(false) }
    LaunchedEffect(account.id) {
        if (!isNew) hasTxns = repo.txns(accountId = account.id).first().isNotEmpty()
    }

    val initialOpening = remember(account) {
        if (isNew || account.openingBalance == 0L) "" else amountToInput(account.openingBalance, account.currency)
    }
    var opening by remember { mutableStateOf(initialOpening) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var openingError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var showReconcileDialog by remember { mutableStateOf(false) }
    var reconciled by remember { mutableStateOf(false) }

    val hasChanges = name != account.name ||
        type != account.type ||
        currency != account.currency ||
        opening != initialOpening ||
        selectedColorKey != account.colorKey

    val saveEnabled = name.isNotBlank()

    val onSave: () -> Unit = {
        val trimmed = name.trim()
        val openingValue = if (opening.isBlank() || opening.trim() == "0") 0L else parseAmount(opening, currency)
        if (trimmed.isEmpty()) nameError = "Nhập tên tài khoản"
        if (openingValue == null) openingError = "Số tiền không hợp lệ"
        if (trimmed.isNotEmpty() && openingValue != null) {
            scope.launch {
                repo.saveAccount(
                    account.copy(
                        name = trimmed,
                        type = type,
                        openingBalance = openingValue,
                        currency = currency,
                        colorKey = selectedColorKey,
                    ),
                )
                onDismiss()
            }
        }
    }

    FormDialog(
        title = if (isNew) "Thêm tài khoản" else "Sửa tài khoản",
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
                label = { Text("Tên tài khoản") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
            OneLineText("Loại", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AccountType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = {
                            type = t
                            if (!userPickedColor) {
                                selectedColorKey = defaultAccountColorKey(t)
                            }
                        },
                        label = { OneLineText(t.label()) },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = type == t),
                    )
                }
            }
        }

        SectionHeaderBand("Tiền tệ và số dư")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CurrencyChips(
                selected = currency,
                onSelect = {
                    currency = it
                    openingError = null
                },
                enabled = isNew || !hasTxns,
            )
            if (!isNew && hasTxns) {
                Text(
                    "Không đổi được loại tiền vì tài khoản đã có giao dịch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = opening,
                onValueChange = {
                    opening = it
                    openingError = null
                },
                label = { Text("Số dư ban đầu (${currencySymbol(currency)})") },
                placeholder = { Text("0") },
                singleLine = true,
                isError = openingError != null,
                supportingText = openingError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeaderBand("Màu sắc")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
                color = accountTileColor(selectedColorKey),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OneLineText(
                        text = name.ifBlank { "Tên tài khoản" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnAccountTile,
                        modifier = Modifier.weight(1f),
                    )
                    OneLineText(
                        text = type.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnAccountTile.copy(alpha = 0.8f),
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ACCOUNT_COLOR_KEYS.forEach { key ->
                    val isSelected = key == selectedColorKey
                    val desc = ColorDescriptions[key] ?: key
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClickLabel = desc) {
                                userPickedColor = true
                                selectedColorKey = key
                            }
                            .semantics {
                                selected = isSelected
                                contentDescription = desc
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(accountColor(key), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = OnAccountTile,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
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
                    onClick = { showReconcileDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText("Điều chỉnh số dư")
                }
                if (reconciled) {
                    Text(
                        text = "Đã điều chỉnh số dư",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repo.archiveAccount(account.id)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText("Lưu trữ")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            when (repo.deleteAccount(account.id)) {
                                DeleteResult.DELETED -> onDismiss()
                                DeleteResult.HAS_TRANSACTIONS ->
                                    deleteError = "Tài khoản đã có giao dịch, chỉ có thể lưu trữ"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText("Xóa tài khoản", color = MaterialTheme.colorScheme.error)
                }
                deleteError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showReconcileDialog) {
        ReconcileDialog(
            account = account,
            onDismiss = { showReconcileDialog = false },
            onSuccess = { reconciled = true },
        )
    }
}

@Composable
private fun ReconcileDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var currentBalance by remember { mutableStateOf<Long?>(null) }
    var txns by remember { mutableStateOf<List<Txn>?>(null) }
    var realText by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(account.id) {
        val loadedTxns = repo.txns(accountId = account.id).first()
        txns = loadedTxns
        currentBalance = accountBalance(account, loadedTxns)
    }

    val real = parseAmount(realText, account.currency)
    val isError = realText.isNotEmpty() && real == null
    val current = currentBalance
    val loadedTxns = txns
    val kind = if (real != null && loadedTxns != null) reconcileKind(account, loadedTxns, real) else null
    val confirmEnabled = real != null && kind != null && !submitting

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Điều chỉnh số dư") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OneLineText(
                    text = "Số dư trong ChiLi: " + (if (current != null) formatMoney(current, account.currency) else ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = realText,
                    onValueChange = { realText = it },
                    label = { Text("Số dư thực tế (${currencySymbol(account.currency)})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Số tiền không hợp lệ") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (real != null && current != null) {
                    val diff = abs(real - current)
                    val message = when (kind) {
                        null -> "Không có chênh lệch"
                        TxnKind.EXPENSE -> "Sẽ ghi một khoản chi ${formatMoney(diff, account.currency)} (danh mục Chênh lệch)"
                        TxnKind.INCOME -> "Sẽ ghi một khoản thu ${formatMoney(diff, account.currency)} (danh mục Chênh lệch)"
                        else -> null
                    }
                    if (message != null) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (real != null && !submitting) {
                        submitting = true
                        scope.launch {
                            val id = repo.reconcileAccount(account.id, real)
                            if (id != null) {
                                onSuccess()
                            }
                            onDismiss()
                        }
                    }
                },
                enabled = confirmEnabled,
            ) {
                OneLineText("Điều chỉnh")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                OneLineText("Hủy")
            }
        },
    )
}
