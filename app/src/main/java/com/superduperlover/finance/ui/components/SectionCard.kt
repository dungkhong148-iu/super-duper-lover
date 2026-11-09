package com.superduperlover.finance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.ui.theme.ChiLiTheme

/**
 * Card of a screen section (docs/ui-layout.md section 2): surface colour, 16dp corners, 16dp padding,
 * thin outline, no shadow. [title] is optional; [onAction] adds a "›" button at the right of the title row.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    onAction: (() -> Unit)? = null,
    actionDescription: String = "Xem thêm",
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ChiLiTheme.colors.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val titleStyle = MaterialTheme.typography.titleMedium.let {
                        if (it.fontSize.isSpecified && it.fontSize > 18.sp) it.copy(fontSize = 18.sp) else it
                    }
                    OneLineText(
                        text = title,
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (onAction != null) {
                        IconButton(onClick = onAction, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = actionDescription)
                        }
                    }
                }
            }
            content()
        }
    }
}

/** Strip that groups the fields of a form (docs/ui-layout.md section 2, "Full-screen form"). */
@Composable
fun SectionHeaderBand(text: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier.fillMaxWidth()) {
        OneLineText(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
