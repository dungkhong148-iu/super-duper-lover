package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.GoalProgress
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.ui.LocalRepo
import java.time.LocalDate
import kotlinx.coroutines.launch

fun formatGoalDate(date: LocalDate): String =
    "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)

@Composable
fun GoalProgressContent(goal: Goal, progress: GoalProgress) {
    LinearProgressIndicator(
        progress = { progress.ratio.toFloat().coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 8.dp),
    )
    Text(
        text = "Đã để dành ${formatMoney(progress.saved, goal.currency)} / ${formatMoney(goal.target, goal.currency)}",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (progress.done) {
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
            Text(
                text = "Đã đạt mục tiêu",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    } else if (progress.perMonth != null) {
        Text(
            text = "Cần để dành ${formatMoney(progress.perMonth!!, goal.currency)}/tháng",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val deadline = goal.deadline
    if (deadline != null) {
        Text(
            text = "Hạn: ${formatGoalDate(deadline)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
