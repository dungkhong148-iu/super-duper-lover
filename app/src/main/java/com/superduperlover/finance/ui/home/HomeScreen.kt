package com.superduperlover.finance.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.OneLineText
import kotlinx.coroutines.launch

private val PAGE_TITLES = listOf("Tài khoản", "Ngân sách & Mục tiêu")

/**
 * "Trang chủ" tab: top bar, TabRow with a swipeable pager (docs/ui-layout.md section 1).
 * Page 0 = [AccountsPage], page 1 = [PlanPage].
 */
@Composable
fun HomeScreen(
    onOpenAccountTransactions: (Long) -> Unit,
    onOpenGoal: (Long) -> Unit,
) {
    val pagerState = rememberPagerState { PAGE_TITLES.size }
    val scope = rememberCoroutineScope()
    fun showPage(index: Int) {
        scope.launch { pagerState.animateScrollToPage(index) }
    }

    Column(Modifier.fillMaxSize()) {
        ChiLiTopBar("Trang chủ")
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            PAGE_TITLES.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { showPage(index) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    text = { OneLineText(title, style = MaterialTheme.typography.titleSmall) },
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> AccountsPage(
                    onOpenAccountTransactions = onOpenAccountTransactions,
                    onOpenPlan = { showPage(1) },
                )
                else -> PlanPage(onOpenGoal = onOpenGoal)
            }
        }
    }
}
