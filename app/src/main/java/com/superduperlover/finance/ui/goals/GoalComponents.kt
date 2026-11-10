package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.GoalProgress
import com.superduperlover.finance.core.JarPlanCalculator
import com.superduperlover.finance.core.JarSchedule
import com.superduperlover.finance.core.ReserveCycle
import com.superduperlover.finance.core.SegmentedBar
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.theme.ChiLiTheme
import java.time.LocalDate
import kotlin.math.roundToInt

fun formatGoalDate(date: LocalDate): String =
    "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)

@Composable
fun SegmentedBarVisual(
    bar: SegmentedBar,
    currency: Currency,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
        ) {
            if (bar.othersFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(bar.othersFraction)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline),
                )
            }
            if (bar.thisJarFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(bar.thisJarFraction)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            if (bar.freeFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(bar.freeFraction)
                        .fillMaxHeight()
                        .background(
                            if (bar.negative) ChiLiTheme.colors.expense
                            else MaterialTheme.colorScheme.secondaryContainer,
                        ),
                )
            }
        }
    }
}

@Composable
fun SegmentedVolumeSlider(
    schedule: JarSchedule,
    bar: SegmentedBar,
    currency: Currency,
    onPeriodsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    target: Long = bar.thisJar,
    baseSaved: Long = 0L,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp)) {
            SegmentedBarVisual(bar = bar, currency = currency)
        }
        val valueRange = if (schedule.cycle == ReserveCycle.DAILY) 1f..30f else 2f..36f
        val steps = if (schedule.cycle == ReserveCycle.DAILY) 28 else 33
        Slider(
            value = schedule.periods.toFloat().coerceIn(valueRange),
            onValueChange = { onPeriodsChange(it.roundToInt()) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
        )
        val perPeriodAmount = JarPlanCalculator.amounts(target, baseSaved, schedule).firstOrNull() ?: 0L
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val rateScheduleText = buildString {
                append("Trích ")
                append(formatMoney(perPeriodAmount, currency))
                append("/")
                append(if (schedule.cycle == ReserveCycle.DAILY) "ngày" else "tháng")
                append(" · Xong trong ")
                append(schedule.periods)
                append(" kỳ (Dự kiến: ")
                append(formatGoalDate(schedule.endDate))
                append(")")
                if (schedule.extraDays > 0) {
                    append(" ≈ ${schedule.periods} tháng ${schedule.extraDays} ngày")
                }
            }
            Text(
                text = rateScheduleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Tự do còn lại trên tài khoản này: ${formatMoney(bar.free, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (bar.negative) ChiLiTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
                text = "Đã hoàn thành",
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
