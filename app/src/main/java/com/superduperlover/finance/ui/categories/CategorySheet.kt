package com.superduperlover.finance.ui.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.data.DeleteResult
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.FormDialog
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionHeaderBand
import com.superduperlover.finance.ui.components.label
import com.superduperlover.finance.ui.theme.chiLiChipBorder
import com.superduperlover.finance.ui.theme.chiLiChipColors
import kotlinx.coroutines.launch

@Composable
internal fun CategorySheet(
    category: Category,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val isNew = category.id == 0L

    var name by remember { mutableStateOf(category.name) }
    var kind by remember { mutableStateOf(category.kind) }
    val initialIcon = remember(category) {
        if (category.icon.isNotBlank()) category.icon
        else if (category.kind == CategoryKind.EXPENSE) "food"
        else "salary"
    }
    var selectedIcon by remember { mutableStateOf(initialIcon) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    val hasChanges = name != category.name ||
        (isNew && kind != category.kind) ||
        selectedIcon != initialIcon

    val saveEnabled = name.isNotBlank()

    val onSave: () -> Unit = {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            nameError = "Nhập tên danh mục"
        } else if (allCategories.any { other ->
                other.id != category.id &&
                    other.kind == kind &&
                    other.name.trim().equals(trimmed, ignoreCase = true)
            }
        ) {
            nameError = "Tên danh mục đã tồn tại"
        } else {
            scope.launch {
                repo.saveCategory(
                    category.copy(
                        name = trimmed,
                        kind = kind,
                        icon = selectedIcon,
                    )
                )
                onDismiss()
            }
        }
    }

    FormDialog(
        title = if (isNew) "Thêm danh mục" else "Sửa danh mục",
        onClose = onDismiss,
        onSave = onSave,
        saveEnabled = saveEnabled,
        hasChanges = hasChanges,
    ) {
        SectionHeaderBand("Thông tin")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isNew) {
                OneLineText("Loại", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == CategoryKind.EXPENSE,
                        onClick = {
                            kind = CategoryKind.EXPENSE
                            nameError = null
                        },
                        label = { OneLineText(TxnKind.EXPENSE.label()) },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = kind == CategoryKind.EXPENSE),
                    )
                    FilterChip(
                        selected = kind == CategoryKind.INCOME,
                        onClick = {
                            kind = CategoryKind.INCOME
                            nameError = null
                        },
                        label = { OneLineText(TxnKind.INCOME.label()) },
                        colors = chiLiChipColors(),
                        border = chiLiChipBorder(selected = kind == CategoryKind.INCOME),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OneLineText("Loại:", style = MaterialTheme.typography.labelLarge)
                    OneLineText(
                        text = if (category.kind == CategoryKind.EXPENSE) TxnKind.EXPENSE.label() else TxnKind.INCOME.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text("Tên danh mục") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeaderBand("Biểu tượng")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val iconEntries = remember { CategoryIcons.all.entries.toList() }
            iconEntries.chunked(5).forEach { rowEntries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowEntries.forEach { (key, vector) ->
                        val on = key == selectedIcon
                        Surface(
                            onClick = { selectedIcon = key },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .semantics { this.selected = on },
                            shape = RoundedCornerShape(8.dp),
                            color = if (on) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (on) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        ) {
                            Box(
                                modifier = Modifier.padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = key,
                                    tint = if (on) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    repeat(5 - rowEntries.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (!isNew) {
            SectionHeaderBand("Khác")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repo.archiveCategory(category.id)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText("Lưu trữ")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            when (repo.deleteCategory(category.id)) {
                                DeleteResult.DELETED -> onDismiss()
                                DeleteResult.HAS_TRANSACTIONS ->
                                    deleteError = "Danh mục đã có giao dịch, chỉ có thể lưu trữ"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    OneLineText("Xóa danh mục", color = MaterialTheme.colorScheme.error)
                }

                deleteError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
