package com.superduperlover.finance.ui.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.data.DeleteResult
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.incomeColor
import com.superduperlover.finance.ui.components.label
import kotlinx.coroutines.launch

@Composable
private fun CategoryKind.color(): Color = when (this) {
    CategoryKind.EXPENSE -> expenseColor()
    CategoryKind.INCOME -> incomeColor()
}

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val allCategories by remember(repo) { repo.categories(includeArchived = true) }.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<Category?>(null) }

    val expenseCategories = remember(allCategories) {
        allCategories.filter { it.kind == CategoryKind.EXPENSE && !it.archived }
    }
    val incomeCategories = remember(allCategories) {
        allCategories.filter { it.kind == CategoryKind.INCOME && !it.archived }
    }
    val archivedCategories = remember(allCategories) {
        allCategories.filter { it.archived }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                text = "Danh mục",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header_expense") {
                Text(
                    text = TxnKind.EXPENSE.label(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(expenseCategories, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onClick = { editing = category },
                )
            }
            item(key = "add_expense") {
                OutlinedButton(
                    onClick = {
                        editing = Category(
                            id = 0L,
                            name = "",
                            kind = CategoryKind.EXPENSE,
                            icon = "food",
                            archived = false,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text("Thêm danh mục")
                }
            }

            item(key = "header_income") {
                Text(
                    text = TxnKind.INCOME.label(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            }
            items(incomeCategories, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onClick = { editing = category },
                )
            }
            item(key = "add_income") {
                OutlinedButton(
                    onClick = {
                        editing = Category(
                            id = 0L,
                            name = "",
                            kind = CategoryKind.INCOME,
                            icon = "salary",
                            archived = false,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text("Thêm danh mục")
                }
            }

            if (archivedCategories.isNotEmpty()) {
                item(key = "header_archived") {
                    Text(
                        text = "Đã lưu trữ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(archivedCategories, key = { it.id }) { category ->
                    ArchivedCategoryRow(
                        category = category,
                        onRestore = {
                            scope.launch {
                                repo.saveCategory(category.copy(archived = false))
                            }
                        },
                    )
                }
            }
        }
    }

    editing?.let { category ->
        CategorySheet(
            category = category,
            allCategories = allCategories,
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ArchivedCategoryRow(
    category: Category,
    onRestore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CategoryIcons.get(category.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onRestore,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Khôi phục")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(
    category: Category,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val isNew = category.id == 0L

    var name by remember { mutableStateOf(category.name) }
    var kind by remember { mutableStateOf(category.kind) }
    var selectedIcon by remember {
        mutableStateOf(
            if (category.icon.isNotBlank()) category.icon
            else if (category.kind == CategoryKind.EXPENSE) "food"
            else "salary"
        )
    }
    var nameError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isNew) "Thêm danh mục" else "Sửa danh mục",
                style = MaterialTheme.typography.titleLarge,
            )

            if (isNew) {
                Text("Loại", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == CategoryKind.EXPENSE,
                        onClick = {
                            kind = CategoryKind.EXPENSE
                            nameError = null
                        },
                        label = { Text(TxnKind.EXPENSE.label()) },
                    )
                    FilterChip(
                        selected = kind == CategoryKind.INCOME,
                        onClick = {
                            kind = CategoryKind.INCOME
                            nameError = null
                        },
                        label = { Text(TxnKind.INCOME.label()) },
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Loại:", style = MaterialTheme.typography.labelLarge)
                    Text(
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

            Text("Biểu tượng", style = MaterialTheme.typography.labelLarge)
            val iconEntries = remember { CategoryIcons.all.entries.toList() }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                color = if (on) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (on) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = vector,
                                        contentDescription = key,
                                        tint = if (on) MaterialTheme.colorScheme.onPrimaryContainer
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

            Button(
                onClick = {
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Lưu")
            }

            if (!isNew) {
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
                    Text("Lưu trữ")
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
                    Text("Xóa danh mục", color = MaterialTheme.colorScheme.error)
                }
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
}
