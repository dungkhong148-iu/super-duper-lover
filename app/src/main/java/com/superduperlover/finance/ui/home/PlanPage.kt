package com.superduperlover.finance.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.budgets.BudgetsScreen
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.goals.GoalsScreen

/** "Ngân sách & Mục tiêu" page: month switcher + budgets, then goals, in one scroll (docs/ui-layout.md section 1). */
@Composable
fun PlanPage(onOpenGoal: (Long) -> Unit) {
    val repo = LocalRepo.current
    val goals by remember(repo) { repo.goals() }.collectAsState(initial = emptyList())
    val runningCount = remember(goals) { goals.count { it.completedAt == null } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        BudgetsScreen()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp),
        ) {
            val titleStyle = MaterialTheme.typography.titleMedium.let {
                if (it.fontSize.isSpecified && it.fontSize > 18.sp) it.copy(fontSize = 18.sp) else it
            }.copy(fontWeight = FontWeight.SemiBold)
            OneLineText(
                text = "Mục tiêu",
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (goals.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    OneLineText(
                        text = "${runningCount} đang chạy",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        GoalsScreen(onOpenGoal = onOpenGoal)
    }
}
