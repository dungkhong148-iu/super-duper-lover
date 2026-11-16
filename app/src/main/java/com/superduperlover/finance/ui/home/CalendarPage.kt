package com.superduperlover.finance.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.TxnTotals
import com.superduperlover.finance.core.dailyFlows
import com.superduperlover.finance.core.healthStats
import com.superduperlover.finance.core.loggingStreak
import com.superduperlover.finance.core.txnTotals
import com.superduperlover.finance.core.weeklyFlows
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.v3Shadow
import java.time.LocalDate
import java.time.YearMonth

/**
 * "Lịch" page of Home (page 1 of the pager inside the white plate): health card, month calendar grid and weekly flow
 * bars. [accountId] = the account selected in the carousel (null = all accounts); [scroll] is the plate's list state.
 */
@Composable
fun CalendarPage(accountId: Long?, scroll: ScrollState) {
    val c = ChiLiV3.colors
    val repo = LocalRepo.current
    val prefs = LocalAppPrefs.current
    val today = remember { LocalDate.now() }
    val month = remember { YearMonth.now() }
    val monthTxns by remember(repo, month) { repo.txns(month = month) }.collectAsState(initial = emptyList())
    val allTxns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    val allAccounts by remember(repo) { repo.accounts() }.collectAsState(initial = emptyList())
    // Only the selected account (or all) counts: every core function below skips transactions of other accounts.
    val accounts = remember(allAccounts, accountId) { allAccounts.filter { accountId == null || it.id == accountId } }
    val currency = if (accountId == null) MAIN_CURRENCY else accounts.firstOrNull()?.currency ?: MAIN_CURRENCY
    val hidden = prefs.hideBalance
    val monthTotals = txnTotals(monthTxns, accounts)[currency] ?: TxnTotals(0L, 0L)
    val streakDays = remember(allTxns, accounts, today) {
        val ids = accounts.map { it.id }.toSet()
        loggingStreak(allTxns.filter { it.accountId in ids }.map { it.date }.toSet(), today)
    }
    val health = remember(monthTotals, today, streakDays) { healthStats(monthTotals, today.dayOfMonth, streakDays) }
    val daily = remember(monthTxns, accounts, month) { dailyFlows(monthTxns, accounts, currency, month.atDay(1), month.atEndOfMonth()) }
    val weekly = remember(monthTxns, accounts, month) { weeklyFlows(monthTxns, accounts, currency, month) }
    var selectedDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedDay)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Health Card
        val healthCardShape = RoundedCornerShape(26.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .v3Shadow(c.shadow3, healthCardShape)
                .clip(healthCardShape)
                .background(Brush.linearGradient(listOf(c.headerTop, c.headerBottom)))
                .padding(16.dp),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "SỨC KHỎE TÀI CHÍNH",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.onHeader.copy(alpha = 0.85f),
                        )
                        Text(
                            "Tháng %d, %d".format(month.monthValue, month.year),
                            style = MaterialTheme.typography.titleMedium,
                            color = c.onHeader,
                        )
                    }
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "${health.score} / 100 điểm",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.onHeader,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x26000000))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Tỷ lệ tiết kiệm",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.onHeader.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (health.savingsRate != null) "%.1f%%".format(health.savingsRate) else "—",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFBEF264),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Chi trung bình",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.onHeader.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            moneyText(health.avgSpendPerDay, currency, hidden) + "/ngày",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.onHeader,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Chuỗi ghi chép",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.onHeader.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${health.streakDays} ngày",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFCD34D),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // 2. Month Calendar Card
        val calCardShape = RoundedCornerShape(24.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .v3Shadow(c.shadow2, calCardShape)
                .clip(calCardShape)
                .background(c.cardActive)
                .border(BorderStroke(1.dp, c.cardBorder), calCardShape)
                .padding(16.dp),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Lịch biến động số dư",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.onCard,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(c.incomePill))
                            Text("Thu", style = MaterialTheme.typography.labelSmall, color = c.onSheetMuted)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(c.expensePill))
                            Text("Chi", style = MaterialTheme.typography.labelSmall, color = c.onSheetMuted)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { dayLabel ->
                        Text(
                            dayLabel,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.onSheetMuted,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
                val daysInMonth = month.lengthOfMonth()
                val totalCells = firstDayOffset + daysInMonth
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (col in 0 until 7) {
                                val dayNum = r * 7 + col - firstDayOffset + 1
                                if (dayNum < 1 || dayNum > daysInMonth) {
                                    Spacer(Modifier.weight(1f))
                                } else {
                                    val date = month.atDay(dayNum)
                                    val isSelected = date == selectedDate
                                    val dayTotals = daily[date]
                                    val hasIncome = (dayTotals?.income ?: 0L) > 0L
                                    val hasExpense = (dayTotals?.expense ?: 0L) > 0L
                                    val cellShape = RoundedCornerShape(12.dp)
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .heightIn(min = 40.dp)
                                            .clip(cellShape)
                                            .then(
                                                if (isSelected) Modifier.v3Shadow(c.shadow1, cellShape).background(c.accent)
                                                else Modifier.clickable { selectedDay = date.toEpochDay() },
                                            )
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Text(
                                                "$dayNum",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) Color.White else c.onCard,
                                            )
                                            Row(
                                                Modifier.height(5.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                if (hasIncome) {
                                                    Box(
                                                        Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else c.incomePill),
                                                    )
                                                }
                                                if (hasExpense) {
                                                    Box(
                                                        Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color(0xFFFFB3B0) else c.expensePill),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = c.cardBorder, thickness = 1.dp)
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Ngày %02d/%02d/%d:".format(selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.onCard,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val selTotals = daily[selectedDate] ?: TxnTotals(0L, 0L)
                        Text(
                            "+" + moneyText(selTotals.income, currency, hidden),
                            color = c.income,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            "−" + moneyText(selTotals.expense, currency, hidden),
                            color = c.expense,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        // 3. Weekly Flow Bars Card
        val weeklyCardShape = RoundedCornerShape(24.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .v3Shadow(c.shadow2, weeklyCardShape)
                .clip(weeklyCardShape)
                .background(c.cardActive)
                .border(BorderStroke(1.dp, c.cardBorder), weeklyCardShape)
                .padding(16.dp),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Biểu đồ Thu và Chi theo tuần",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.onCard,
                        )
                        Text(
                            "So sánh dòng tiền vào và ra",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.onSheetMuted,
                        )
                    }
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(c.accent.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Tháng %d".format(month.monthValue),
                            style = MaterialTheme.typography.labelSmall,
                            color = c.accent,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                val maxFlow = weekly.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    for (w in weekly.indices) {
                        val wTotals = weekly[w]
                        val inFrac = (wTotals.income.toFloat() / maxFlow).coerceIn(0f, 1f)
                        val outFrac = (wTotals.expense.toFloat() / maxFlow).coerceIn(0f, 1f)
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Row(
                                Modifier.height(90.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Box(
                                    Modifier
                                        .width(10.dp)
                                        .fillMaxHeight(fraction = inFrac.coerceAtLeast(if (wTotals.income > 0) 0.08f else 0.03f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (wTotals.income > 0) c.incomePill else c.incomePill.copy(alpha = 0.25f)),
                                )
                                Box(
                                    Modifier
                                        .width(10.dp)
                                        .fillMaxHeight(fraction = outFrac.coerceAtLeast(if (wTotals.expense > 0) 0.08f else 0.03f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (wTotals.expense > 0) c.expensePill else c.expensePill.copy(alpha = 0.25f)),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Tuần ${w + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.onSheetMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
