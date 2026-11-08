package com.superduperlover.finance.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.accountBalance
import com.superduperlover.finance.core.formatVnd
import com.superduperlover.finance.core.parseVndInput
import com.superduperlover.finance.data.DeleteResult
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.label
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSheet(account: Account, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val isNew = account.id == 0L

    var name by remember { mutableStateOf(account.name) }
    var type by remember { mutableStateOf(account.type) }
    var opening by remember {
        mutableStateOf(if (isNew || account.openingBalance == 0L) "" else account.openingBalance.toString())
    }
    var nameError by remember { mutableStateOf<String?>(null) }
    var openingError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (isNew) "Thêm tài khoản" else "Sửa tài khoản",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text("Tên tài khoản") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Loại", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AccountType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.label()) },
                    )
                }
            }
            OutlinedTextField(
                value = opening,
                onValueChange = { opening = it; openingError = null },
                label = { Text("Số dư ban đầu") },
                placeholder = { Text("0") },
                singleLine = true,
                isError = openingError != null,
                supportingText = openingError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            deleteError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {
                    val trimmed = name.trim()
                    // Empty or "0" opening balance means 0; anything else must parse.
                    val openingValue = if (opening.isBlank() || opening.trim() == "0") 0L else parseVndInput(opening)
                    if (trimmed.isEmpty()) nameError = "Nhập tên tài khoản"
                    if (openingValue == null) openingError = "Số tiền không hợp lệ"
                    if (trimmed.isNotEmpty() && openingValue != null) {
                        scope.launch {
                            repo.saveAccount(account.copy(name = trimmed, type = type, openingBalance = openingValue))
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Lưu") }
            if (!isNew) {
                OutlinedButton(
                    onClick = { scope.launch { repo.archiveAccount(account.id); onDismiss() } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Lưu trữ") }
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Xóa tài khoản", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
