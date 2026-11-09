package com.superduperlover.finance.ui.add

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.ui.components.OneLineText

/**
 * 4-column calculator pad:
 * Columns 1-3 digits: 7 8 9 / 4 5 6 / 1 2 3 / (000 or .) 0 ⌫.
 * Column 4 operators: ÷ × − + =.
 * Keys >= 48dp tall and 16dp corner, 8dp gaps.
 */
@Composable
fun AmountPad(
    currency: Currency,
    onDigit: (String) -> Unit,
    onDot: () -> Unit,
    onOperator: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier,
    onSave: (() -> Unit)? = null,
) {
    val keyK = when (currency) {
        Currency.USD -> "."
        Currency.VND -> "000"
        Currency.TWD -> "00"
    }
    val digitRows = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf(keyK, "0", "⌫"),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Left 3 columns: Digits (4 rows)
            Column(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                digitRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "⌫" -> {
                                    PadKey(
                                        text = "⌫",
                                        onClick = onBackspace,
                                        onLongClick = onClear,
                                        contentDescription = "Xóa số cuối",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                                "." -> {
                                    PadKey(
                                        text = ".",
                                        onClick = onDot,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                                "000", "00" -> {
                                    PadKey(
                                        text = key,
                                        onClick = { onDigit(key) },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                                else -> {
                                    PadKey(
                                        text = key,
                                        onClick = { onDigit(key) },
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right 1 column: Operators (5 keys)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PadKey(
                    text = "÷",
                    onClick = { onOperator('/') },
                    contentDescription = "Chia",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                PadKey(
                    text = "×",
                    onClick = { onOperator('*') },
                    contentDescription = "Nhân",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                PadKey(
                    text = "−",
                    onClick = { onOperator('-') },
                    contentDescription = "Trừ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                PadKey(
                    text = "+",
                    onClick = { onOperator('+') },
                    contentDescription = "Cộng",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                PadKey(
                    text = "=",
                    onClick = onEquals,
                    contentDescription = "Bằng",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        if (onSave != null) {
            Surface(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .semantics { contentDescription = "Lưu giao dịch" },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    OneLineText(
                        text = "Lưu",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PadKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    fontSize: TextUnit = 22.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            )
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = contentColor,
            )
        }
    }
}
