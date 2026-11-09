package com.superduperlover.finance.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.superduperlover.finance.core.Category
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.CategoryIcons
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.EmptyState
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.components.SectionCard
import com.superduperlover.finance.ui.components.expenseColor
import com.superduperlover.finance.ui.components.incomeColor
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
        ChiLiTopBar(
            title = "Danh mục",
            onBack = onBack,
        )

        if (allCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_wallet),
                    title = "Chưa có danh mục",
                    message = "Tạo danh mục đầu tiên để phân loại các khoản thu chi.",
                    actionLabel = "Thêm danh mục",
                    onAction = {
                        editing = Category(
                            id = 0L,
                            name = "",
                            kind = CategoryKind.EXPENSE,
                            icon = "food",
                            archived = false,
                        )
                    },
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
                item(key = "card_expense") {
                    SectionCard(title = "Chi tiêu") {
                        if (expenseCategories.isEmpty()) {
                            OneLineText(
                                text = "Chưa có danh mục chi tiêu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            expenseCategories.forEachIndexed { index, category ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                CategoryRow(
                                    category = category,
                                    onClick = { editing = category },
                                )
                            }
                        }
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
                            OneLineText("Thêm danh mục", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                item(key = "card_income") {
                    SectionCard(title = "Thu nhập") {
                        if (incomeCategories.isEmpty()) {
                            OneLineText(
                                text = "Chưa có danh mục thu nhập",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            incomeCategories.forEachIndexed { index, category ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                CategoryRow(
                                    category = category,
                                    onClick = { editing = category },
                                )
                            }
                        }
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
                            OneLineText("Thêm danh mục", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                if (archivedCategories.isNotEmpty()) {
                    item(key = "card_archived") {
                        SectionCard(title = "Đã lưu trữ") {
                            archivedCategories.forEachIndexed { index, category ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
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
        OneLineText(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
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
        OneLineText(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onRestore,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            OneLineText("Khôi phục", style = MaterialTheme.typography.labelLarge)
        }
    }
}
