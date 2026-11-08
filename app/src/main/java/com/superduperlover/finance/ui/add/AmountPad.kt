package com.superduperlover.finance.ui.add

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * 4-column calculator pad:
 * Columns 1-3 digits: 7 8 9 / 4 5 6 / 1 2 3 / (000 or .) 0 ⌫.
 * Column 4 operators: ÷ × − + =.
 * Keys >= 56dp tall and >= 48dp wide, 6dp gaps.
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
) {
    val decimalPoint = currency.decimals > 0
    val digitRows = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf(if (decimalPoint) "." else "000", "0", "⌫"),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(306.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Left 3 columns: Digits (4 rows)
        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            digitRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                            "000" -> {
                                PadKey(
                                    text = "000",
                                    onClick = { onDigit("000") },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
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
                                    fontWeight = FontWeight.Medium,
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
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PadKey(
                text = "÷",
                onClick = { onOperator('/') },
                contentDescription = "Chia",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            PadKey(
                text = "=",
                onClick = onEquals,
                contentDescription = "Bằng",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
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
    fontWeight: FontWeight = FontWeight.Normal,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
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
        shape = RoundedCornerShape(12.dp),
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
