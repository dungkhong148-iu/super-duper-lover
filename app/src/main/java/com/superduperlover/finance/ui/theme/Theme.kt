package com.superduperlover.finance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

// Dark tokens follow the Stitch reference (raw/stitch_reference DESIGN.md); table in docs/ui-layout.md section 3.
private val DarkTokens = ChiLiColors(
    income = Color(0xFF44EDB7),
    expense = Color(0xFFFFB3B0),
    transfer = Color(0xFF9EDBFF),
    warn = Color(0xFFFBBF24),
    over = Color(0xFFFFB3B0),
    outline = Color(0x993C4A43),
    incomeContainer = Color(0x2644EDB7),
    expenseContainer = Color(0xFF881D24),
    onExpenseContainer = Color(0xFFFFDAD8),
    transferContainer = Color(0x269EDBFF),
)

private val LightTokens = ChiLiColors(
    income = Color(0xFF059669),
    expense = Color(0xFFDC2626),
    transfer = Color(0xFF475569),
    warn = Color(0xFFB45309),
    over = Color(0xFFDC2626),
    outline = Color(0xFFD5E0D9),
    incomeContainer = Color(0x29059669),
    expenseContainer = Color(0x29DC2626),
    onExpenseContainer = Color(0xFFDC2626),
    transferContainer = Color(0x29475569),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF44EDB7),
    onPrimary = Color(0xFF003828),
    // FAB / filled brand container. On-colour is darker than Stitch's #00533C: 6.6:1 instead of 4.56:1.
    primaryContainer = Color(0xFF00D09C),
    onPrimaryContainer = Color(0xFF003828),
    // Selected chips, nav indicator, tinted icon circles: 15% mint fill + mint content.
    secondary = Color(0xFF44EDB7),
    onSecondary = Color(0xFF003828),
    secondaryContainer = Color(0x2644EDB7),
    onSecondaryContainer = Color(0xFF44EDB7),
    tertiary = Color(0xFF9EDBFF),
    onTertiary = Color(0xFF00354A),
    tertiaryContainer = Color(0x269EDBFF),
    onTertiaryContainer = Color(0xFF9EDBFF),
    background = Color(0xFF10141A),
    onBackground = Color(0xFFDFE2EB),
    // surface = card colour (screens draw cards with `surface`).
    surface = Color(0xFF1C2026),
    onSurface = Color(0xFFDFE2EB),
    surfaceVariant = Color(0xFF262A31),
    onSurfaceVariant = Color(0xFFBACAC1),
    surfaceTint = Color(0xFF44EDB7),
    outline = Color(0xFF85948C),
    outlineVariant = Color(0xFF3C4A43),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0A0E14),
    surfaceContainerLow = Color(0xFF181C22),
    surfaceContainer = Color(0xFF1C2026),
    surfaceContainerHigh = Color(0xFF262A31),
    surfaceContainerHighest = Color(0xFF31353C),
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF047857),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    background = Color(0xFFF6FAF7),
    onBackground = Color(0xFF0F1F17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F1F17),
    surfaceVariant = Color(0xFFE7EFEA),
    onSurfaceVariant = Color(0xFF4B5B53),
    surfaceTint = Color(0xFF047857),
    outline = Color(0xFF8FA398),
    outlineVariant = Color(0xFFD5E0D9),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE7EFEA),
    surfaceContainerHighest = Color(0xFFE7EFEA),
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

/**
 * Chip colours for every FilterChip: selected = 15% mint fill + mint label (Stitch "active chip").
 * Light mode keeps its old container/label roles.
 */
@Composable
fun chiLiChipColors(): SelectableChipColors {
    val c = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = c.secondaryContainer,
        selectedLabelColor = c.onSecondaryContainer,
        selectedLeadingIconColor = c.onSecondaryContainer,
        selectedTrailingIconColor = c.onSecondaryContainer,
    )
}

/** Chip border for every FilterChip: 1dp outline when idle, 1dp mint outline when selected. */
@Composable
fun chiLiChipBorder(selected: Boolean, enabled: Boolean = true): BorderStroke {
    val c = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipBorder(
        enabled = enabled,
        selected = selected,
        borderColor = c.outlineVariant,
        selectedBorderColor = c.primary,
        borderWidth = 1.dp,
        selectedBorderWidth = 1.dp,
    )
}

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    val dark = when (LocalAppPrefs.current.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalChiLiColors provides if (dark) DarkTokens else LightTokens) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
    }
}
