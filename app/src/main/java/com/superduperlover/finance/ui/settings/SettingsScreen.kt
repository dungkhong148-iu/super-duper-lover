package com.superduperlover.finance.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.data.validateBackupJson
import com.superduperlover.finance.ui.LocalRepo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isWorking = true
            statusMessage = null
            errorMessages = emptyList()
            scope.launch(Dispatchers.IO) {
                try {
                    val json = repo.exportBackupJson()
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    statusMessage = "Đã xuất dữ liệu"
                } catch (e: Exception) {
                    statusMessage = "Xuất thất bại: ${e.message}"
                } finally {
                    isWorking = false
                }
            }
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                text = "Cài đặt và sao lưu",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Text(
            text = "Sao lưu lưu toàn bộ tài khoản, danh mục, giao dịch, ngân sách và mục tiêu vào một tệp JSON.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                val defaultName = "chili-backup-%s.json".format(
                    LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                )
                exportLauncher.launch(defaultName)
            },
            enabled = !isWorking,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text("Xuất dữ liệu")
        }

        Button(
            onClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            enabled = !isWorking,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text("Nhập dữ liệu")
        }

        statusMessage?.let { msg ->
            Text(
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
