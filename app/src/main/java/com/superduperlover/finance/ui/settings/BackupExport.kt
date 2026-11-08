package com.superduperlover.finance.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.superduperlover.finance.core.backupFileName
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The export flow shared by Settings and the Home reminder card: returns a function that opens the system "save
 * file" dialog (default name `chili_backup_<date>.json`), writes the backup, and on success resets the backup
 * reminder counters. [onWorking] tells when the write starts / ends; [onMessage] gets the result text.
 */
@Composable
fun rememberBackupExport(
    onWorking: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
): () -> Unit {
    val repo = LocalRepo.current
    val prefs = LocalAppPrefs.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            onWorking(true)
            scope.launch(Dispatchers.IO) {
                try {
                    val json = repo.exportBackupJson()
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    prefs.markBackedUp()
                    onMessage("Đã xuất dữ liệu")
                } catch (e: Exception) {
                    onMessage("Xuất thất bại: ${e.message}")
                } finally {
                    onWorking(false)
                }
            }
        }
    }
    return { launcher.launch(backupFileName(LocalDate.now())) }
}
