package com.superduperlover.finance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

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
)

private val DarkTokens = ChiLiColors(
    income = Color(0xFF34D399),
    expense = Color(0xFFF87171),
    transfer = Color(0xFF94A3B8),
    warn = Color(0xFFFBBF24),
    over = Color(0xFFF87171),
    outline = Color(0x14FFFFFF),
)

private val LightTokens = ChiLiColors(
    income = Color(0xFF059669),
    expense = Color(0xFFDC2626),
    transfer = Color(0xFF475569),
    warn = Color(0xFFB45309),
    over = Color(0xFFDC2626),
    outline = Color(0xFFD5E0D9),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF04241A),
    primaryContainer = Color(0xFF0B3D2E),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFF04241A),
    secondaryContainer = Color(0xFF0B3D2E),
    onSecondaryContainer = Color(0xFFD1FAE5),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFE8F0EC),
    surface = Color(0xFF18211C),
    onSurface = Color(0xFFE8F0EC),
    surfaceVariant = Color(0xFF212C26),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceTint = Color(0xFF10B981),
    outline = Color(0xFF3A4A41),
    outlineVariant = Color(0xFF2A3731),
    error = Color(0xFFF87171),
    onError = Color(0xFF3B0A0A),
    surfaceContainerLowest = Color(0xFF0F1512),
    surfaceContainerLow = Color(0xFF18211C),
    surfaceContainer = Color(0xFF18211C),
    surfaceContainerHigh = Color(0xFF212C26),
    surfaceContainerHighest = Color(0xFF212C26),
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
