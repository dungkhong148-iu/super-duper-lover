package com.superduperlover.finance.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.data.validateBackupJson
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingJson by remember { mutableStateOf("") }

    val startExport = rememberBackupExport(
        onWorking = { working ->
            isWorking = working
            if (working) {
                statusMessage = null
                errorMessages = emptyList()
            }
        },
        onMessage = { statusMessage = it },
    )

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isWorking = true
            statusMessage = null
            errorMessages = emptyList()
            scope.launch(Dispatchers.IO) {
                try {
                    val text = context.contentResolver.openInputStream(uri)!!.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                    val errors = validateBackupJson(text)
                    if (errors.isNotEmpty()) {
                        errorMessages = errors.take(5)
                    } else {
                        pendingJson = text
                        showConfirmDialog = true
                    }
                } catch (e: Exception) {
                    errorMessages = listOf("Không thể đọc tệp: ${e.message}")
                } finally {
                    isWorking = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ChiLiTopBar(
            title = "Sao lưu và khôi phục",
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "Xuất dữ liệu") {
                Text(
                    text = "Lưu toàn bộ tài khoản, danh mục, giao dịch, ngân sách và mục tiêu vào một tệp JSON.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = startExport,
                    enabled = !isWorking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText("Xuất dữ liệu", style = MaterialTheme.typography.labelLarge)
                }
            }

            SectionCard(title = "Nhập dữ liệu") {
                Text(
                    text = "Khôi phục dữ liệu từ tệp JSON đã sao lưu trước đó. Dữ liệu hiện tại sẽ bị thay thế.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    enabled = !isWorking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText("Nhập dữ liệu", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (statusMessage != null || errorMessages.isNotEmpty()) {
                SectionCard {
                    statusMessage?.let { msg ->
                        OneLineText(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    for (err in errorMessages) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Thay thế toàn bộ dữ liệu?") },
            text = { Text("Dữ liệu hiện tại sẽ bị xóa và thay bằng nội dung tệp. Không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        isWorking = true
                        statusMessage = null
                        errorMessages = emptyList()
                        scope.launch(Dispatchers.IO) {
                            try {
                                val errors = repo.importBackupJson(pendingJson)
                                if (errors.isEmpty()) {
                                    statusMessage = "Đã nhập dữ liệu"
                                } else {
                                    errorMessages = errors.take(5)
                                }
                            } catch (e: Exception) {
                                errorMessages = listOf("Nhập thất bại: ${e.message}")
                            } finally {
                                isWorking = false
                            }
                        }
                    }
                ) {
                    Text("Thay thế")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Hủy")
                }
            },
        )
    }
}
