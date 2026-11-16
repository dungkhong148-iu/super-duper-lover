package com.superduperlover.finance.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.LocalFormAccent
import com.superduperlover.finance.ui.theme.LocalOnFormAccent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Full-screen dialog host for forms (docs/ui-layout.md section 2 and specs/U3.txt).
 * Wraps [FullScreenForm] in a full-screen [Dialog].
 * When closing (via ✕ button, system back, or dismiss):
 * - If [hasChanges] is true, prompts with an AlertDialog to confirm discarding changes.
 * - Otherwise calls [onClose] immediately.
 */
@Composable
fun FormDialog(
    title: String,
    onClose: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    hasChanges: Boolean,
    feature: FormFeature = FormFeature.NEUTRAL,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = ChiLiV3.colors
    val white = Color.White
    val accent: Color? = when (feature) {
        FormFeature.NEUTRAL -> null
        FormFeature.JAR -> c.jarAccent
        FormFeature.BUDGET -> c.budgetAccent
        FormFeature.DEBT -> c.debtAccent
    }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val handleClose = {
        if (hasChanges) {
            showDiscardConfirm = true
        } else {
            onClose()
        }
    }

    CompositionLocalProvider(
        LocalFormAccent provides accent,
        LocalOnFormAccent provides accent?.let { white },
    ) {
    Dialog(
        onDismissRequest = handleClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // No systemBarsPadding here: the form's top bar paints the status bar area itself.
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            FullScreenForm(
                title = title,
                onClose = handleClose,
                onSave = onSave,
                saveEnabled = saveEnabled,
                content = content,
            )
        }
    }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Bỏ thay đổi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onClose()
                    },
                ) {
                    Text("Bỏ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Tiếp tục sửa")
                }
            },
        )
    }
}
