package com.superduperlover.finance.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

// All colours of ChiLi are defined here once (docs/ui-layout.md section 3). Screens use tokens, never raw hex.

/** Roles that Material 3 has no slot for. Read with [ChiLiTheme.colors]. */
@Immutable
class ChiLiColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val warn: Color,
    /** Budget over the limit: same as [expense]. */
    val over: Color,
    /** Card / panel outline. */
    val outline: Color,
    /** Selected "Thu nhập" tab / tinted tile (content on it = [income]). */
    val incomeContainer: Color,
    /** Selected "Chi tiêu" tab / tinted tile; content on it = [onExpenseContainer]. */
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    /** Selected "Chuyển khoản" tab / tinted tile (content on it = [transfer]). */
    val transferContainer: Color,
)

// Roles that Material 3 has no slot for, on the v3 palette (V3Theme.kt). Table: docs/ui-layout.md section 3.
private val DarkTokens = ChiLiColors(
    income = V3Dark.income,
    expense = V3Dark.expense,
    transfer = Color(0xFF93C5FD),
    warn = Color(0xFFFBBF24),
    over = V3Dark.expense,
    outline = V3Dark.cardBorder,
    incomeContainer = V3Dark.income.copy(alpha = 0.16f),
    expenseContainer = V3Dark.expense.copy(alpha = 0.16f),
    onExpenseContainer = V3Dark.expense,
    transferContainer = Color(0xFF93C5FD).copy(alpha = 0.16f),
)

private val LightTokens = ChiLiColors(
    income = V3Light.income,
    expense = V3Light.expense,
    transfer = Color(0xFF475569),
    warn = Color(0xFFB45309),
    over = V3Light.expense,
    outline = Color(0xFFE2E8F0),
    incomeContainer = V3Light.income.copy(alpha = 0.14f),
    expenseContainer = V3Light.expense.copy(alpha = 0.14f),
    onExpenseContainer = V3Light.expense,
    transferContainer = Color(0xFF475569).copy(alpha = 0.14f),
)

private val LocalChiLiColors = staticCompositionLocalOf { DarkTokens }

object ChiLiTheme {
    val colors: ChiLiColors
        @Composable @ReadOnlyComposable get() = LocalChiLiColors.current
}

/** The 8 account colours (docs/ui-layout.md section 3). Keys are stored in the database. */
private val AccountPalette = mapOf(
    "emerald" to Color(0xFF10B981),
    "teal" to Color(0xFF14B8A6),
    "sky" to Color(0xFF0EA5E9),
    "indigo" to Color(0xFF6366F1),
    "violet" to Color(0xFF8B5CF6),
    "rose" to Color(0xFFF43F5E),
    "amber" to Color(0xFFF59E0B),
    "slate" to Color(0xFF64748B),
)

/** Colour shown in pickers and chips; unknown key -> emerald. */
fun accountColor(key: String): Color = AccountPalette[key] ?: AccountPalette.getValue("emerald")

/** Tile background: the account colour darkened 40% so white text reaches at least 5:1 contrast on every colour. */
fun accountTileColor(key: String): Color = lerp(accountColor(key), Color.Black, 0.4f)

/** Text and icons on an account tile. */
val OnAccountTile = Color.White

/** Chip colours for every FilterChip: selected = solid accent fill + white text, idle = accent tint (no outline). */
@Composable
fun chiLiChipColors(): SelectableChipColors {
    val accent = formAccent()
    val on = onFormAccent()
    return FilterChipDefaults.filterChipColors(
        containerColor = accent.copy(alpha = 0.12f),
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedContainerColor = accent,
        selectedLabelColor = on,
        selectedLeadingIconColor = on,
        selectedTrailingIconColor = on,
    )
}

/** Chips have no border any more: the fill carries the state. */
@Composable
fun chiLiChipBorder(selected: Boolean, enabled: Boolean = true): BorderStroke =
    FilterChipDefaults.filterChipBorder(
        enabled = enabled,
        selected = selected,
        borderColor = Color.Transparent,
        selectedBorderColor = Color.Transparent,
        borderWidth = 0.dp,
        selectedBorderWidth = 0.dp,
    )

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    val dark = LocalAppPrefs.current.dark
    CompositionLocalProvider(LocalChiLiColors provides if (dark) DarkTokens else LightTokens) {
        V3Theme(dark = dark, content = content)
    }
}
