package com.superduperlover.finance.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.ThemeMode

/** "Giao diện" screen: select system / light / dark mode (docs/ui-layout.md section 1). */
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val prefs = LocalAppPrefs.current

    Column(Modifier.fillMaxSize()) {
        ChiLiTopBar("Giao diện", onBack = onBack)
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard {
                ThemeOptionRow(
                    title = "Theo hệ thống",
                    subtitle = "Sáng hoặc tối theo cài đặt điện thoại",
                    selected = prefs.themeMode == ThemeMode.SYSTEM,
                    onSelect = { prefs.themeMode = ThemeMode.SYSTEM },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ThemeOptionRow(
                    title = "Sáng",
                    subtitle = null,
                    selected = prefs.themeMode == ThemeMode.LIGHT,
                    onSelect = { prefs.themeMode = ThemeMode.LIGHT },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ThemeOptionRow(
                    title = "Tối",
                    subtitle = null,
                    selected = prefs.themeMode == ThemeMode.DARK,
                    onSelect = { prefs.themeMode = ThemeMode.DARK },
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            OneLineText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
