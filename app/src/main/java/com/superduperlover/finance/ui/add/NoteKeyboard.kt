package com.superduperlover.finance.ui.add

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.ui.theme.ChiLiV3

// Gboard-like: keys 48dp tall (touch target), 4dp apart, the rows use the full width.
private val KEY_HEIGHT = 48.dp
private val KEY_GAP = 4.dp

@Composable
fun NoteKeyboard(
    note: String,
    onNoteChange: (String) -> Unit,
    onDone: () -> Unit,
    onSwitchToSystemKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KEY_GAP),
    ) {
        // Row 1 (Digits, 10 keys, each Modifier.weight(1f))
        Row(
            modifier = Modifier
                .height(KEY_HEIGHT)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { digit ->
                NoteKey(
                    text = digit,
                    onClick = { onNoteChange(note + digit) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        // Row 2 (QWERTY top row, 10 keys, each Modifier.weight(1f))
        Row(
            modifier = Modifier
                .height(KEY_HEIGHT)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").forEach { letter ->
                NoteKey(
                    text = letter,
                    onClick = { onNoteChange(note + letter) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        // Row 3 (QWERTY home row, 9 keys, total weight 10f)
        Row(
            modifier = Modifier
                .height(KEY_HEIGHT)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").forEach { letter ->
                NoteKey(
                    text = letter,
                    onClick = { onNoteChange(note + letter) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }

        // Row 4 (QWERTY bottom row + Backspace, total weight 10f)
        Row(
            modifier = Modifier
                .height(KEY_HEIGHT)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            listOf("z", "x", "c", "v", "b", "n", "m").forEach { letter ->
                NoteKey(
                    text = letter,
                    onClick = { onNoteChange(note + letter) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
            NoteKey(
                text = "⌫",
                onClick = { if (note.isNotEmpty()) onNoteChange(note.dropLast(1)) },
                onLongClick = { onNoteChange("") },
                containerColor = ChiLiV3.colors.card,
                contentDescription = "Xóa ký tự cuối",
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
            )
        }

        // Row 5 (Bottom action row, total weight 10f)
        Row(
            modifier = Modifier
                .height(KEY_HEIGHT)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoteKey(
                text = "⌨",
                onClick = onSwitchToSystemKeyboard,
                containerColor = ChiLiV3.colors.card,
                contentDescription = "Bàn phím hệ thống",
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
            )
            NoteKey(
                text = "Cách",
                onClick = { onNoteChange(note + " ") },
                style = MaterialTheme.typography.titleMedium,
                containerColor = ChiLiV3.colors.card,
                contentDescription = "Dấu cách",
                modifier = Modifier
                    .weight(5.5f)
                    .fillMaxHeight(),
            )
            NoteKey(
                text = "Xong",
                onClick = onDone,
                containerColor = ChiLiV3.colors.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                contentDescription = "Xong",
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    fontWeight: FontWeight = FontWeight.Normal,
    containerColor: Color = ChiLiV3.colors.cardActive,
    contentColor: Color = ChiLiV3.colors.onCard,
) {
    val shape = RoundedCornerShape(8.dp)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // Pressed: the key turns clearly accent-tinted (the accent key itself goes darker).
    val pressedOverlay = if (containerColor == ChiLiV3.colors.accent) Color.Black.copy(alpha = 0.3f)
    else ChiLiV3.colors.accent.copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (pressed) pressedOverlay.compositeOver(containerColor) else containerColor)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            color = contentColor,
            maxLines = 1,
        )
    }
}
