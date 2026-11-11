package com.superduperlover.finance.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.Goal
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.otherCategoryName
import com.superduperlover.finance.data.PurgePreview
import com.superduperlover.finance.data.PurgeResult
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.incomeColor
import com.superduperlover.finance.ui.components.label
import com.superduperlover.finance.ui.theme.accountColor
import kotlinx.coroutines.launch

@Composable
private fun CategoryKind.color(): Color = when (this) {
    CategoryKind.EXPENSE -> expenseColor()
    CategoryKind.INCOME -> incomeColor()
}

private fun accountIconRes(type: AccountType): Int = when (type) {
    AccountType.CASH -> R.drawable.ic_account_cash
    AccountType.BANK -> R.drawable.ic_account_bank
    AccountType.CARD -> R.drawable.ic_account_card
    AccountType.EWALLET -> R.drawable.ic_account_ewallet
}

@Composable
fun ArchiveScreen(onBack: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()

    val archivedAccounts by repo.archivedAccounts().collectAsState(initial = emptyList())
    val archivedCategories by repo.archivedCategories().collectAsState(initial = emptyList())
    val archivedGoals by repo.archivedGoals().collectAsState(initial = emptyList())

    var pendingAccount by remember { mutableStateOf<Account?>(null) }
    var previewAccount by remember { mutableStateOf<PurgePreview?>(null) }

    var pendingCategory by remember { mutableStateOf<Category?>(null) }
    var previewCategory by remember { mutableStateOf<PurgePreview?>(null) }
    var fallbackError by remember { mutableStateOf<String?>(null) }

    var pendingGoal by remember { mutableStateOf<Goal?>(null) }
    var contributionCount by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ChiLiTopBar(
            title = "Lưu trữ",
            onBack = onBack,
        )

        if (archivedAccounts.isEmpty() && archivedCategories.isEmpty() && archivedGoals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_wallet),
                    title = "Chưa có mục nào được lưu trữ",
                    message = "Các tài khoản, danh mục hoặc hũ đã lưu trữ sẽ xuất hiện tại đây.",
                    actionLabel = "Quay lại",
                    onAction = onBack,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "archived_accounts") {
                    SectionCard(title = "Tài khoản") {
                        if (archivedAccounts.isEmpty()) {
                            Text(
                                text = "Chưa có tài khoản lưu trữ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            archivedAccounts.forEachIndexed { index, account ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(accountColor(account.colorKey), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(accountIconRes(account.type)),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        OneLineText(
                                            text = account.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        OneLineText(
                                            text = account.type.label(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch { repo.restoreAccount(account.id) }
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        OneLineText("Khôi phục", style = MaterialTheme.typography.labelLarge)
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                previewAccount = repo.previewPurgeAccount(account.id)
                                                pendingAccount = account
                                            }
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                    ) {
                                        OneLineText("Xóa", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "archived_categories") {
                    SectionCard(title = "Danh mục") {
                        if (archivedCategories.isEmpty()) {
                            Text(
                                text = "Chưa có danh mục lưu trữ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            archivedCategories.forEachIndexed { index, category ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(category.kind.color().copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = CategoryIcons.get(category.icon),
                                            contentDescription = null,
                                            tint = category.kind.color(),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        OneLineText(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        OneLineText(
                                            text = if (category.kind == CategoryKind.EXPENSE) "Chi tiêu" else "Thu nhập",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch { repo.restoreCategory(category.id) }
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        OneLineText("Khôi phục", style = MaterialTheme.typography.labelLarge)
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                previewCategory = repo.previewPurgeCategory(category.id)
                                                pendingCategory = category
                                            }
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                    ) {
                                        OneLineText("Xóa", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "archived_goals") {
                    SectionCard(title = "Hũ") {
                        if (archivedGoals.isEmpty()) {
                            Text(
                                text = "Chưa có hũ lưu trữ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            archivedGoals.forEachIndexed { index, goal ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_savings),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        OneLineText(
                                            text = goal.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        OneLineText(
                                            text = formatMoney(goal.target, goal.currency),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch { repo.restoreGoal(goal.id) }
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        OneLineText("Khôi phục", style = MaterialTheme.typography.labelLarge)
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                contributionCount = repo.contributionCount(goal.id)
                                                pendingGoal = goal
                                            }
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                    ) {
                                        OneLineText("Xóa", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Account Xóa confirm dialog
    val currentPendingAccount = pendingAccount
    val currentPreviewAccount = previewAccount
    if (currentPendingAccount != null && currentPreviewAccount != null) {
        val message = buildString {
            if (currentPreviewAccount.deletedTxns > 0) {
                append("${currentPreviewAccount.deletedTxns} giao dịch bị xóa\n")
            }
            if (currentPreviewAccount.keptTransfers > 0) {
                append("${currentPreviewAccount.keptTransfers} chuyển khoản được giữ\n")
            }
            if (currentPreviewAccount.deletedTxns == 0 && currentPreviewAccount.keptTransfers == 0) {
                append("Tài khoản không có giao dịch nào.\n")
            }
            append("Hành động này không thể hoàn tác.")
        }
        AlertDialog(
            onDismissRequest = {
                pendingAccount = null
                previewAccount = null
            },
            title = { OneLineText("Xóa tài khoản?") },
            text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toPurge = currentPendingAccount
                        pendingAccount = null
                        previewAccount = null
                        scope.launch {
                            repo.purgeAccount(toPurge.id)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    OneLineText("Xóa", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingAccount = null
                        previewAccount = null
                    },
                ) {
                    OneLineText("Hủy", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }

    // Category Xóa confirm dialog
    val currentPendingCategory = pendingCategory
    val currentPreviewCategory = previewCategory
    if (currentPendingCategory != null && currentPreviewCategory != null) {
        val message = buildString {
            if (currentPreviewCategory.movedTxns > 0) {
                append("${currentPreviewCategory.movedTxns} giao dịch chuyển sang '${otherCategoryName(currentPendingCategory.kind)}'.\n")
            } else {
                append("Danh mục không có giao dịch nào.\n")
            }
            append("Hành động này không thể hoàn tác.")
        }
        AlertDialog(
            onDismissRequest = {
                pendingCategory = null
                previewCategory = null
            },
            title = { OneLineText("Xóa danh mục?") },
            text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toPurge = currentPendingCategory
                        pendingCategory = null
                        previewCategory = null
                        scope.launch {
                            val res = repo.purgeCategory(toPurge.id)
                            if (res == PurgeResult.IS_FALLBACK) {
                                fallbackError = "Không thể xóa danh mục '${toPurge.name}' khi vẫn còn giao dịch vì đây là danh mục mặc định chuyển tiếp."
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    OneLineText("Xóa", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingCategory = null
                        previewCategory = null
                    },
                ) {
                    OneLineText("Hủy", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }

    // Category fallback error dialog
    fallbackError?.let { err ->
        AlertDialog(
            onDismissRequest = { fallbackError = null },
            title = { OneLineText("Không thể xóa") },
            text = { Text(err, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { fallbackError = null }) {
                    OneLineText("Đóng", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }

    // Goal Xóa confirm dialog
    val currentPendingGoal = pendingGoal
    val currentContributionCount = contributionCount
    if (currentPendingGoal != null && currentContributionCount != null) {
        val message = buildString {
            if (currentContributionCount > 0) {
                append("$currentContributionCount khoản góp bị xóa.\n")
            } else {
                append("Hũ không có khoản góp nào.\n")
            }
            append("Hành động này không thể hoàn tác.")
        }
        AlertDialog(
            onDismissRequest = {
                pendingGoal = null
                contributionCount = null
            },
            title = { OneLineText("Xóa hũ?") },
            text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toPurge = currentPendingGoal
                        pendingGoal = null
                        contributionCount = null
                        scope.launch {
                            repo.deleteGoal(toPurge.id)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    OneLineText("Xóa", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingGoal = null
                        contributionCount = null
                    },
                ) {
                    OneLineText("Hủy", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}
