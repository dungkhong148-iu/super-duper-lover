package com.superduperlover.finance.ui.jars

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.JarKind
import com.superduperlover.finance.core.JarSaving
import com.superduperlover.finance.core.JarStatus
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.goals.GoalSheet
import com.superduperlover.finance.ui.theme.ChiLiTheme

/**
 * Kho hũ bottom tab screen (docs/ui-layout.md, specs/H3.txt step 5, specs/H5.txt).
 */
@Composable
fun JarsScreen(
    onOpenJar: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val repo = LocalRepo.current
    val jarSavings by remember(repo) { repo.jarSavings() }.collectAsState(initial = emptyList())
    val accounts by remember(repo) { repo.accounts(includeArchived = true) }.collectAsState(initial = emptyList())
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { selectedJarId = null },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        OneLineText(
                            text = "Xóa bộ lọc",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
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
                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Hũ giữ chỗ tiền cho một mục đích: tiết kiệm để mua sắm, hoặc chi phí lớn biết trước như tiền nhà, học phí, bảo hiểm. Tiền vẫn nằm trong tài khoản cho đến khi bạn chi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val (statusLabel, statusColor) = when (jar.goal.status) {
        JarStatus.ACTIVE -> "Đang chạy" to MaterialTheme.colorScheme.onSurfaceVariant
        JarStatus.PENDING_SETTLEMENT -> "Chờ chi" to ChiLiTheme.colors.warn
        JarStatus.COMPLETED -> "Xong" to ChiLiTheme.colors.income
        JarStatus.CANCELLED -> "Đã hủy" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val progress = (jar.saved.toFloat() / jar.goal.target.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val progressColor = when (jar.goal.status) {
        JarStatus.COMPLETED -> ChiLiTheme.colors.income
        JarStatus.PENDING_SETTLEMENT -> ChiLiTheme.colors.warn
        else -> MaterialTheme.colorScheme.primary
    }

    val deadlineStr = jar.goal.deadline?.let {
        "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ChiLiTheme.colors.outline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    OneLineText(
                        text = if (jar.goal.kind == JarKind.SPENDING) "Chi phí lớn" else "Tiết kiệm",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OneLineText(
                    text = jar.goal.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                OneLineText(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OneLineText(
                    text = "${formatMoney(jar.saved, jar.goal.currency)} / ${formatMoney(jar.goal.target, jar.goal.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val percent = (progress * 100).toInt()
                OneLineText(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = if (deadlineStr != null) Modifier.weight(1f, fill = false) else Modifier,
                        )
                    }
                    if (deadlineStr != null) {
                        OneLineText(
                            text = "Hạn: $deadlineStr",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (jar.goal.status == JarStatus.PENDING_SETTLEMENT) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, ChiLiTheme.colors.warn),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            fontWeight = FontWeight.SemiBold,
                            color = ChiLiTheme.colors.warn,
                        )
                    }
                }
            }
        }
    }
}
