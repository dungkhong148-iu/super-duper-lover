package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.goalProgress
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import java.time.LocalDate

@Composable
fun GoalsScreen(onOpenGoal: (Long) -> Unit) {
    val repo = LocalRepo.current
    val goals by remember(repo) { repo.goals() }.collectAsState(initial = emptyList())
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    val inProgress = remember(goals) {
        goals.filter { it.completedAt == null }
            .sortedWith(
                compareBy<Goal, LocalDate?>(nullsLast(naturalOrder())) { it.deadline }
                    .thenBy { it.id }
            )
    }

    val completed = remember(goals) {
        goals.filter { it.completedAt != null }
            .sortedWith(
                compareByDescending<Goal> { it.completedAt }
                    .thenByDescending { it.id }
            )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (goals.isEmpty()) {
            EmptyState(
                icon = painterResource(R.drawable.ic_flag),
                title = "Hãy tạo hũ đầu tiên",
                message = "Chọn một khoản cần để dành và theo dõi tiến độ mỗi tháng.",
                actionLabel = "Tạo hũ",
                onAction = { editingGoal = Goal(0L, "", 0L, null) },
                emphasized = true,
            )
        } else {
            val subTitleStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )

            OneLineText(
                text = "Đang thực hiện",
                style = subTitleStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (inProgress.isEmpty()) {
                OneLineText(
                    text = "Chưa có hũ đang thực hiện",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                for (goal in inProgress) {
                    GoalCard(goal = goal, onOpen = { onOpenGoal(goal.id) })
                }
            }

            if (completed.isNotEmpty()) {
                OneLineText(
                    text = "Đã hoàn thành (${completed.size})",
                    style = subTitleStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                for (goal in completed) {
                    GoalCard(goal = goal, onOpen = { onOpenGoal(goal.id) })
                }
            }

            FilledTonalButton(
                onClick = { editingGoal = Goal(0L, "", 0L, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                OneLineText("Thêm hũ", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    editingGoal?.let { goal ->
        GoalSheet(goal = goal, onDismiss = { editingGoal = null })
    }
}

@Composable
private fun GoalCard(goal: Goal, onOpen: () -> Unit) {
    val repo = LocalRepo.current
    val contributions by remember(repo, goal.id) { repo.contributions(goal.id) }.collectAsState(initial = emptyList())
    val progress = goalProgress(goal, contributions, LocalDate.now())

    SectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        OneLineText(goal.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        GoalProgressContent(goal = goal, progress = progress)
    }
}
