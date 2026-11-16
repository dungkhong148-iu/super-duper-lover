package com.superduperlover.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.formAccent
import com.superduperlover.finance.ui.theme.onFormAccent
import com.superduperlover.finance.ui.theme.v3Shadow

/** Which feature a form belongs to: it colours the top bar, the chips and the save button. */
enum class FormFeature { NEUTRAL, JAR, BUDGET, DEBT }

/**
 * Scaffold of a full-screen form (docs/ui-layout.md section 2): top bar in the feature colour (it also covers the
 * status bar) with X (discard), title, check (save, disabled while the form is invalid), then scrolling [content].
 * Group fields with [FormSection]. The caller asks for confirmation in [onClose] when the user changed something.
 */
@Composable
fun FullScreenForm(
    title: String,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accent = formAccent()
    val onAccent = onFormAccent()
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(accent)
                .statusBarsPadding()
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Đóng", tint = onAccent)
            }
            OneLineText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = onAccent,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            FilledIconButton(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = onAccent,
                    contentColor = accent,
                    disabledContainerColor = onAccent.copy(alpha = 0.35f),
                    disabledContentColor = accent.copy(alpha = 0.6f),
                ),
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Lưu")
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

/**
 * One group of fields of a form: its own tinted card (feature colour at 8% over the card colour, 1dp feature border,
 * soft shadow) with the title in the feature colour. Fields inside keep their own 16dp side padding.
 */
@Composable
fun ColumnScope.FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val accent = formAccent()
    val c = ChiLiV3.colors
    val shape = RoundedCornerShape(20.dp)
    val tint: Color = accent.copy(alpha = 0.08f).compositeOver(c.cardActive)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .v3Shadow(c.shadow1, shape)
            .clip(shape)
            .background(tint)
            .border(1.dp, accent.copy(alpha = 0.2f), shape),
    ) {
        OneLineText(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 0.dp),
        )
        content()
    }
}
