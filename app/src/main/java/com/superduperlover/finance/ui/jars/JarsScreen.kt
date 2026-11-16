package com.superduperlover.finance.ui.jars

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.JarSaving
import com.superduperlover.finance.core.JarStatus
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.isReserving
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.dayMonthYear
import com.superduperlover.finance.ui.goals.GoalSheet
import com.superduperlover.finance.ui.home.moneyText
import com.superduperlover.finance.ui.theme.ChiLiTheme
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.v3Shadow

enum class JarTab { SAVINGS, RECURRING, DEBTS }

/**
 * Kho hũ bottom tab screen (docs/ui-layout.md, specs/H3.txt step 5, specs/H5.txt, specs/N5.txt).
 */
@Composable
fun JarsScreen(
    onOpenJar: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val repo = LocalRepo.current
    val prefs = LocalAppPrefs.current
    val c = ChiLiV3.colors

    val jarSavings by remember(repo) { repo.jarSavings() }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val totalReserved = remember(jarSavings) { jarSavings.filter { it.goal.isReserving() }.sumOf { it.saved } }
    var selectedTab by remember { mutableStateOf(JarTab.SAVINGS) }

    var selectedJarId by remember { mutableStateOf<Long?>(null) }
    var showCreateGoalSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ChiLiTopBar(
            title = "Kho hũ",
            onBack = onBack,
            actions = {
                TextButton(onClick = { showCreateGoalSheet = true }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    OneLineText("Tạo hũ")
                }
            },
        )

        val summaryShape = RoundedCornerShape(26.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .v3Shadow(c.shadow3, summaryShape)
                .clip(summaryShape)
                .background(Brush.linearGradient(listOf(c.headerTop, c.headerBottom)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Tổng tiền trong hũ",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onHeader,
                )
                val activeCount = jarSavings.count { it.goal.isReserving() }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "$activeCount hũ",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onHeader,
                    )
                }
            }
            Text(
                text = moneyText(totalReserved, MAIN_CURRENCY, prefs.hideBalance),
                style = MaterialTheme.typography.headlineLarge,
                color = c.onHeader,
            )
            Text(
                text = "Tiền giữ chỗ cho các mục tiêu và chi phí sắp tới",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onHeader,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            JarTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val tabShape = RoundedCornerShape(12.dp)
                val label = when (tab) {
                    JarTab.SAVINGS -> "Tiết kiệm"
                    JarTab.RECURRING -> "Thường kỳ"
                    JarTab.DEBTS -> "Sổ nợ"
                }
                val tabModifier = if (selected) {
                    Modifier
                        .weight(1f)
                        .v3Shadow(c.shadow1, tabShape)
                        .clip(tabShape)
                        .background(c.cardActive)
                        .padding(vertical = 10.dp)
                } else {
                    Modifier
                        .weight(1f)
                        .clip(tabShape)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 10.dp)
                }
                Box(
                    modifier = tabModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) c.onCard else c.onSheetMuted,
                    )
                }
            }
        }

        when (selectedTab) {
            JarTab.SAVINGS -> {
                if (jarSavings.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(jarSavings, key = { it.goal.id }) { jar ->
                            JarTile(
                                jar = jar,
                                selected = selectedJarId == jar.goal.id,
                                onClick = {
                                    selectedJarId = if (selectedJarId == jar.goal.id) null else jar.goal.id
                                },
                            )
                        }
                    }
                }

                if (selectedJarId != null) {
                    val selectedJar = jarSavings.firstOrNull { it.goal.id == selectedJarId }
                    if (selectedJar != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            OneLineText(
                                text = "Đang lọc: ${selectedJar.goal.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.onSheetMuted,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { selectedJarId = null },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                OneLineText(
                                    text = "Xóa bộ lọc",
                                    color = c.accent,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }

                val displayedJars = if (selectedJarId == null) {
                    jarSavings
                } else {
                    jarSavings.filter { it.goal.id == selectedJarId }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (jarSavings.isEmpty()) {
                        item(key = "empty_jars") {
                            EmptyState(
                                icon = painterResource(R.drawable.ic_savings),
                                title = "Chưa có hũ nào",
                                message = "Hũ giữ chỗ tiền cho một mục đích: tiết kiệm để mua sắm, hoặc chi phí lớn biết trước như tiền nhà, học phí, bảo hiểm. Tiền vẫn nằm trong tài khoản cho đến khi bạn chi.",
                                actionLabel = "Tạo hũ",
                                onAction = { showCreateGoalSheet = true },
                            )
                        }
                    } else {
                        item(key = "jars_info_note") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp),
                                    tint = c.onSheetMuted,
                                )
                                Text(
                                    text = "Hũ giữ chỗ tiền cho một mục đích: tiết kiệm để mua sắm, hoặc chi phí lớn biết trước như tiền nhà, học phí, bảo hiểm. Tiền vẫn nằm trong tài khoản cho đến khi bạn chi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.onSheetMuted,
                                )
                            }
                        }

                        if (displayedJars.isEmpty()) {
                            item(key = "empty_filter") {
                                EmptyState(
                                    icon = painterResource(R.drawable.ic_savings),
                                    title = "Không có hũ phù hợp",
                                    message = "Không tìm thấy hũ với bộ lọc đã chọn.",
                                    actionLabel = "Xóa bộ lọc",
                                    onAction = { selectedJarId = null },
                                )
                            }
                        } else {
                            items(displayedJars, key = { it.goal.id }) { jar ->
                                JarDetailCard(
                                    jar = jar,
                                    accountName = jar.goal.sourceAccountId?.let { accountMap[it]?.name },
                                    onClick = { onOpenJar(jar.goal.id) },
                                )
                            }
                        }
                    }
                }
            }
            JarTab.RECURRING -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                ) {
                    val lockShape = RoundedCornerShape(24.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .v3Shadow(c.shadow2, lockShape)
                            .clip(lockShape)
                            .background(c.cardActive)
                            .border(1.dp, c.cardBorder, lockShape)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(c.card),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_savings),
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Hũ thường kỳ",
                                style = MaterialTheme.typography.titleMedium,
                                color = c.onCard,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(c.card)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "Sắp có",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.onSheetMuted,
                                )
                            }
                        }
                        Text(
                            text = "Tự động phân bổ tiền theo chu kỳ cho hóa đơn, tiền nhà, học phí và các chi phí định kỳ.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.onSheetMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            JarTab.DEBTS -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                ) {
                    val lockShape = RoundedCornerShape(24.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .v3Shadow(c.shadow2, lockShape)
                            .clip(lockShape)
                            .background(c.cardActive)
                            .border(1.dp, c.cardBorder, lockShape)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(c.card),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_wallet),
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Sổ nợ",
                                style = MaterialTheme.typography.titleMedium,
                                color = c.onCard,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(c.card)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "Sắp có",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.onSheetMuted,
                                )
                            }
                        }
                        Text(
                            text = "Theo dõi các khoản vay, nợ phải trả và cho vay, nhắc lịch thanh toán.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.onSheetMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    if (showCreateGoalSheet) {
        GoalSheet(
            goal = Goal(0L, "", 0L, null),
            onDismiss = { showCreateGoalSheet = false },
        )
    }
}

@Composable
private fun JarDetailCard(
    jar: JarSaving,
    accountName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = LocalAppPrefs.current
    val c = ChiLiV3.colors
    val (statusLabel, statusColor) = when (jar.goal.status) {
        JarStatus.ACTIVE -> "Đang chạy" to c.onSheetMuted
        JarStatus.PENDING_SETTLEMENT -> "Chờ chi" to ChiLiTheme.colors.warn
        JarStatus.COMPLETED -> "Xong" to c.income
        JarStatus.CANCELLED -> "Đã hủy" to c.onSheetMuted
    }

    val progress = (jar.saved.toFloat() / jar.goal.target.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val progressColor = when (jar.goal.status) {
        JarStatus.COMPLETED -> c.income
        JarStatus.PENDING_SETTLEMENT -> ChiLiTheme.colors.warn
        else -> c.accent
    }

    val deadlineStr = jar.goal.deadline?.dayMonthYear()
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .v3Shadow(c.shadow2, shape)
            .clip(shape)
            .background(c.cardActive)
            .border(1.dp, c.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OneLineText(
                text = jar.goal.name,
                style = MaterialTheme.typography.titleMedium,
                color = c.onCard,
                modifier = Modifier.weight(1f),
            )

            OneLineText(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = progressColor,
            trackColor = c.card,
            strokeCap = StrokeCap.Round,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OneLineText(
                text = "${moneyText(jar.saved, jar.goal.currency, prefs.hideBalance)} / ${moneyText(jar.goal.target, jar.goal.currency, prefs.hideBalance)}",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onCard,
            )
            val percent = (progress * 100).toInt()
            OneLineText(
                text = "$percent%",
                style = MaterialTheme.typography.labelMedium,
                color = c.onSheetMuted,
            )
        }

        if (accountName != null || deadlineStr != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (accountName != null) {
                    OneLineText(
                        text = "Tài khoản: $accountName",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSheetMuted,
                        modifier = if (deadlineStr != null) Modifier.weight(1f, fill = false) else Modifier,
                    )
                }
                if (deadlineStr != null) {
                    OneLineText(
                        text = "Hạn: $deadlineStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSheetMuted,
                    )
                }
            }
        }

        if (jar.goal.status == JarStatus.PENDING_SETTLEMENT) {
            val bannerShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(bannerShape)
                    .background(c.card)
                    .border(1.dp, ChiLiTheme.colors.warn, bannerShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_savings),
                    contentDescription = null,
                    tint = ChiLiTheme.colors.warn,
                    modifier = Modifier.size(18.dp),
                )
                OneLineText(
                    text = "Chờ xác nhận chi tiêu",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChiLiTheme.colors.warn,
                )
            }
        }
    }
}
