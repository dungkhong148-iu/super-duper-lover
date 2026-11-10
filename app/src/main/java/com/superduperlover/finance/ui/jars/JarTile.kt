package com.superduperlover.finance.ui.jars

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.core.JarSaving
import com.superduperlover.finance.core.JarStatus
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.theme.ChiLiTheme

/**
 * Tile representing a jar (docs/ui-layout.md section 2, specs/H3.txt step 4).
 * Used in home accounts page and top row of JarsScreen.
 */
@Composable
fun JarTile(
    jar: JarSaving,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (statusLabel, statusColor) = when (jar.goal.status) {
        JarStatus.ACTIVE -> "Đang chạy" to MaterialTheme.colorScheme.onSurfaceVariant
        JarStatus.PENDING_SETTLEMENT -> "Chờ chi" to ChiLiTheme.colors.warn
        JarStatus.COMPLETED -> "Xong" to ChiLiTheme.colors.income
        JarStatus.CANCELLED -> "Đã hủy" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val progress = (jar.saved.toFloat() / jar.goal.target.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val progressColor = when (jar.goal.status) {
        JarStatus.COMPLETED -> ChiLiTheme.colors.income
        JarStatus.PENDING_SETTLEMENT -> ChiLiTheme.colors.warn
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, ChiLiTheme.colors.outline)
        },
        modifier = modifier
            .width(160.dp)
            .heightIn(min = 104.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OneLineText(
                    text = jar.goal.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                OneLineText(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = statusColor,
                    minFontSize = 9.sp,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
            )

            OneLineText(
                text = "${formatMoney(jar.saved, jar.goal.currency)} / ${formatMoney(jar.goal.target, jar.goal.currency)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minFontSize = 9.sp,
            )
        }
    }
}
