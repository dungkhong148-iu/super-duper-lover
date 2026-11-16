package com.superduperlover.finance.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.theme.ChiLiV3
import kotlinx.coroutines.launch

private val PAGE_TITLES = listOf("Tài khoản", "Lịch")

/**
 * "Trang chủ" tab (docs/ui-layout.md section 1): green sticky bar (filter label + Tài khoản | Lịch pill) over a
 * swipeable pager. Page 0 = [AccountsPage], page 1 = Lịch (placeholder until N4).
 */
@Composable
fun HomeScreen(
    onOpenTransactions: (Long?) -> Unit,
    onEditTxn: (Long) -> Unit,
    onOpenGoal: (Long) -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenJars: () -> Unit = {},
) {
    val c = ChiLiV3.colors
    val repo = LocalRepo.current
    val accounts by remember(repo) { repo.accounts() }.collectAsState(initial = emptyList())
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val label = accounts.firstOrNull { it.id == selectedId }?.name ?: "Tất cả"
    val pagerState = rememberPagerState { PAGE_TITLES.size }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(c.headerTop)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFBEF264)))
            Text(label, Modifier.weight(1f), color = c.onHeader, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.clip(CircleShape).background(Color(0x40000000)).padding(4.dp)) {
                PAGE_TITLES.forEachIndexed { index, title ->
                    val on = pagerState.currentPage == index
                    Box(
                        Modifier.clip(CircleShape).background(if (on) c.cardActive else Color.Transparent)
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text(title, color = if (on) c.onCard else c.onHeader, style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
        HorizontalPager(pagerState, Modifier.weight(1f), verticalAlignment = Alignment.Top) { page ->
            when (page) {
                0 -> AccountsPage(
                    selectedId = selectedId,
                    onSelectedChange = { selectedId = it },
                    onOpenTransactions = onOpenTransactions,
                    onEditTxn = onEditTxn,
                    onOpenPlan = onOpenBudgets,
                    onOpenGoal = onOpenGoal,
                    onOpenJars = onOpenJars,
                )
                else -> CalendarPage()
            }
        }
    }
}
