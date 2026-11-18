package com.superduperlover.finance.ui.add

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

// Same geometry as Gboard on the user's Redmi (1080x2400, 450 dpi), measured from device screenshots
// (q2 compare, 2026-11-18): the keyboard is 332dp tall and anchored at the bottom; the key rows start
// 52dp below its top (Gboard's toolbar slot) and the 10-unit grid spans the full width.
private val KEYBOARD_HEIGHT = 332.dp
private val KEY_INSET_X = 2.5.dp
// Per row: key height and the gap below it (the gap is not tappable).
private val KEY_HEIGHTS = listOf(35.9.dp, 38.dp, 38.dp, 38.4.dp, 41.6.dp)
private val ROW_GAPS = listOf(6.8.dp, 7.8.dp, 7.8.dp, 7.8.dp, 0.dp)
private val TOP_SPACE = 52.3.dp

@Composable
fun NoteKeyboard(
    note: String,
    onNoteChange: (String) -> Unit,
    onDone: () -> Unit,
    onSwitchToSystemKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val key = ChiLiV3.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(KEYBOARD_HEIGHT)
            .padding(horizontal = 2.1.dp),
    ) {
        Spacer(Modifier.height(TOP_SPACE))

        KeyRow(0) {
            "1234567890".forEach { ch -> NoteKey(0, 1f, ch.toString(), { onNoteChange(note + ch) }) }
        }
        KeyRow(1) {
            "qwertyuiop".forEach { ch -> NoteKey(1, 1f, ch.toString(), { onNoteChange(note + ch) }) }
        }
        KeyRow(2) {
            Spacer(Modifier.weight(0.5f))
            "asdfghjkl".forEach { ch -> NoteKey(2, 1f, ch.toString(), { onNoteChange(note + ch) }) }
            Spacer(Modifier.weight(0.5f))
        }
        KeyRow(3) {
            Spacer(Modifier.weight(1.5f)) // Gboard's shift slot
            "zxcvbnm".forEach { ch -> NoteKey(3, 1f, ch.toString(), { onNoteChange(note + ch) }) }
            NoteKey(
                index = 3,
                units = 1.5f,
                text = "⌫",
                onClick = { if (note.isNotEmpty()) onNoteChange(note.dropLast(1)) },
                onLongClick = { onNoteChange("") },
                containerColor = key.card,
                contentDescription = "Xóa ký tự cuối",
            )
        }
        KeyRow(4) {
            NoteKey(
                index = 4,
                units = 1.5f,
                text = "⌨",
                onClick = onSwitchToSystemKeyboard,
                containerColor = key.card,
                contentDescription = "Bàn phím hệ thống",
            )
            Spacer(Modifier.weight(2f)) // Gboard's emoji and globe slots
            NoteKey(
                index = 4,
                units = 4f,
                text = "Cách",
                onClick = { onNoteChange(note + " ") },
                style = MaterialTheme.typography.titleMedium,
                containerColor = key.card,
                contentDescription = "Dấu cách",
            )
            Spacer(Modifier.weight(1f)) // Gboard's "." slot
            NoteKey(
                index = 4,
                units = 1.5f,
                text = "Xong",
                onClick = onDone,
                style = MaterialTheme.typography.titleSmall,
                containerColor = key.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentDescription = "Xong",
            )
        }
    }
}

/** One row: the cell is the key plus the gap below it; only the visible key is tappable. */
@Composable
private fun KeyRow(index: Int, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(KEY_HEIGHTS[index] + ROW_GAPS[index]),
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.NoteKey(
    index: Int,
    units: Float,
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    containerColor: Color = ChiLiV3.colors.cardActive,
    contentColor: Color = ChiLiV3.colors.onCard,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // Pressed: the key turns clearly accent-tinted (the accent key itself goes darker).
    val pressedOverlay = if (containerColor == ChiLiV3.colors.accent) Color.Black.copy(alpha = 0.3f)
    else ChiLiV3.colors.accent.copy(alpha = 0.45f)
    Box(
        modifier = Modifier
            .weight(units)
            .fillMaxHeight()
            .padding(start = KEY_INSET_X, end = KEY_INSET_X, bottom = ROW_GAPS[index])
            .clip(RoundedCornerShape(6.dp))
            .background(if (pressed) pressedOverlay.compositeOver(containerColor) else containerColor)
            // after the padding: a tap in the gap between keys presses nothing
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
            modifier = Modifier.padding(bottom = 4.dp), // the font sits ~2dp low in a short key
            style = style,
            fontWeight = FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
        )
    }
}
