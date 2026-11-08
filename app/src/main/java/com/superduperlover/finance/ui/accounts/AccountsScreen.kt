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

@Composable
fun AccountsScreen(onOpenCategories: () -> Unit, onOpenSettings: () -> Unit) {
    val repo = LocalRepo.current
    val accounts by remember(repo) { repo.accounts() }.collectAsState(initial = emptyList())
    val txns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    // null = sheet closed; id 0 = new account
    var editing by remember { mutableStateOf<Account?>(null) }

    val balances = accounts.associate { it.id to accountBalance(it, txns) }
    val total = balances.values.sum()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "total") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tổng số dư", style = MaterialTheme.typography.labelLarge)
                    Text(formatVnd(total), style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        items(accounts, key = { it.id }) { account ->
            val balance = balances[account.id] ?: 0L
            Card(Modifier.fillMaxWidth().clickable { editing = account }) {
                Row(
                    Modifier.padding(16.dp).heightIn(min = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            account.type.label(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatVnd(balance),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (balance < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        item(key = "categories") {
            OutlinedButton(
                onClick = onOpenCategories,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Danh mục") }
        }
        item(key = "settings") {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Cài đặt và sao lưu") }
        }
        item(key = "add") {
            OutlinedButton(
                onClick = { editing = Account(0, "", AccountType.CASH, 0) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Thêm tài khoản") }
        }
    }

    editing?.let { account ->
        AccountSheet(account = account, onDismiss = { editing = null })
    }
}
