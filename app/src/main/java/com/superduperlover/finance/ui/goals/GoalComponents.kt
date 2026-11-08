package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.GoalProgress
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.theme.ChiLiTheme
import java.time.LocalDate

fun formatGoalDate(date: LocalDate): String =
    "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)

@Composable
fun GoalProgressContent(goal: Goal, progress: GoalProgress) {
    val completedAt = goal.completedAt
    LinearProgressIndicator(
        progress = { progress.ratio.toFloat().coerceIn(0f, 1f) },
        color = if (completedAt != null) ChiLiTheme.colors.income else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    )
    OneLineText(
        text = "Đã để dành ${formatMoney(progress.saved, goal.currency)} / ${formatMoney(goal.target, goal.currency)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    if (completedAt != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = ChiLiTheme.colors.income,
                modifier = Modifier.size(16.dp),
            )
            OneLineText(
                text = "Hoàn thành ngày ${formatGoalDate(completedAt)}",
                color = ChiLiTheme.colors.income,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    } else if (progress.done) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            OneLineText(
                text = "Đã đạt mục tiêu",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    } else if (progress.perMonth != null) {
        OneLineText(
            text = "Cần để dành ${formatMoney(progress.perMonth!!, goal.currency)}/tháng",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val deadline = goal.deadline
    if (deadline != null) {
        OneLineText(
            text = "Hạn: ${formatGoalDate(deadline)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
