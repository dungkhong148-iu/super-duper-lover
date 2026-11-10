package com.superduperlover.finance.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.ui.budgets.BudgetsScreen

/** "Ngân sách" page: month switcher + budgets (docs/ui-layout.md section 1). */
@Composable
fun PlanPage(onOpenGoal: ((Long) -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        BudgetsScreen()
    }
}
