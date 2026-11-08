package com.superduperlover.finance.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.ui.budgets.BudgetsScreen
import com.superduperlover.finance.ui.goals.GoalsScreen

/** "Ngân sách & Mục tiêu" page: month switcher + budgets, then goals, in one scroll (docs/ui-layout.md section 1). */
@Composable
fun PlanPage(onOpenGoal: (Long) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        BudgetsScreen()
        Text(
            "Mục tiêu",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
        )
        GoalsScreen(onOpenGoal = onOpenGoal)
    }
}
