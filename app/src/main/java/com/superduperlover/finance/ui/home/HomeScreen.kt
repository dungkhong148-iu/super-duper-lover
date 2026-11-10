package com.superduperlover.finance.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.theme.ChiLiTheme
import kotlinx.coroutines.launch

private val PAGE_TITLES = listOf("Tài khoản", "Ngân sách")

/**
 * "Trang chủ" tab: top bar, segmented card with a swipeable pager (docs/ui-layout.md section 1 & 2b).
 * Page 0 = [AccountsPage], page 1 = [PlanPage].
 */
@Composable
fun HomeScreen(
    onOpenAccountTransactions: (Long) -> Unit,
    onOpenGoal: (Long) -> Unit,
    onOpenJars: () -> Unit = {},
) {
    val pagerState = rememberPagerState { PAGE_TITLES.size }
    val scope = rememberCoroutineScope()
    fun showPage(index: Int) {
        scope.launch { pagerState.animateScrollToPage(index) }
    }

    Column(Modifier.fillMaxSize()) {
        ChiLiTopBar("Trang chủ")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ChiLiTheme.colors.outline),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
            ) {
                PAGE_TITLES.forEachIndexed { index, title ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .clickable { showPage(index) }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        OneLineText(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            minFontSize = 10.sp,
                        )
                    }
                }
            }
        }
        // Pages start at the top (the pager centres a short page vertically by default).
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
        ) { page ->
            when (page) {
                0 -> AccountsPage(
                    onOpenAccountTransactions = onOpenAccountTransactions,
                    onOpenPlan = { showPage(1) },
                    onOpenGoal = onOpenGoal,
                    onOpenJars = onOpenJars,
                )
                else -> PlanPage(onOpenGoal = onOpenGoal)
            }
        }
    }
}
