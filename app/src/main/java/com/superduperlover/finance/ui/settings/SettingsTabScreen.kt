package com.superduperlover.finance.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.ThemeMode

/** "Cài đặt" tab: a list of settings entries (docs/ui-layout.md section 1). */
@Composable
fun SettingsTabScreen(
    onOpenCategories: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenArchive: () -> Unit,
) {
    val prefs = LocalAppPrefs.current
    val themeSubtitle = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> "Theo hệ thống"
        ThemeMode.LIGHT -> "Sáng"
        ThemeMode.DARK -> "Tối"
    }

    Column(Modifier.fillMaxSize()) {
        ChiLiTopBar("Cài đặt")
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard {
                SettingsRow("Giao diện", themeSubtitle, onOpenAppearance)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow("Danh mục", "Thêm, đổi tên danh mục chi tiêu và thu nhập", onOpenCategories)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow("Lưu trữ", "Khôi phục hoặc xóa hẳn tài khoản, danh mục, hũ", onOpenArchive)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow("Sao lưu và khôi phục", "Xuất hoặc nhập toàn bộ dữ liệu ra tệp", onOpenBackup)
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            OneLineText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
