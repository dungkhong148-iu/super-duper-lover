package com.superduperlover.finance.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.goalProgress
import com.superduperlover.finance.ui.LocalRepo
import java.time.LocalDate

@Composable
fun GoalsScreen(onOpenGoal: (Long) -> Unit) {
    val repo = LocalRepo.current
    val goals by repo.goals().collectAsState(initial = emptyList())
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (goals.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Chưa có mục tiêu nào",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalCard(goal = goal, onOpen = { onOpenGoal(goal.id) })
            }
        }
        item(key = "add_goal") {
            OutlinedButton(
                onClick = { editingGoal = Goal(0L, "", 0L, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Thêm mục tiêu")
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
    val contributions by repo.contributions(goal.id).collectAsState(initial = emptyList())
    val progress = goalProgress(goal, contributions, LocalDate.now())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(goal.name, style = MaterialTheme.typography.titleMedium)
            GoalProgressContent(goal = goal, progress = progress)
        }
    }
}
