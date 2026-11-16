package com.superduperlover.finance.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.superduperlover.finance.R

// ChiLi v3 redesign tokens (raw/redesign mock v3): light by default + a soft dark version of the same roles, and the
// UVN Ai Cap Nhe font. FinanceTheme (Theme.kt) wraps V3Theme; the debug "ChiLi v3" demo uses it too.

/** Every colour role of the v3 mock. Read with [ChiLiV3.colors]; screens never use raw hex. */
@Immutable
class V3Colors(
    val headerTop: Color,
    val headerBottom: Color,
    val onHeader: Color,
    /** The big rounded sheet that carries the page content. */
    val sheet: Color,
    val onSheet: Color,
    val onSheetMuted: Color,
    val card: Color,
    val cardActive: Color,
    val onCard: Color,
    val cardBorder: Color,
    val heroOuter: Color,
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val onHero: Color,
    val heroBalance: Color,
    /** Translucent well on the hero card. */
    val heroWell: Color,
    val yellow: Color,
    val yellowBorder: Color,
    val onYellow: Color,
    /** Chips, count badge and range selector track on the yellow block. */
    val yellowChip: Color,
    val yellowChipSelected: Color,
    val txRow: Color,
    val onTxRow: Color,
    val income: Color,
    val expense: Color,
    val incomePill: Color,
    val expensePill: Color,
    val onPill: Color,
    val accent: Color,
    val fab: Color,
    val onFab: Color,
    val nav: Color,
    val navBorder: Color,
    /** Depth, from the mock's compose-elevation-1/2/3/fab, hero card outer and the sheet's upward shadow. */
    val shadow1: V3Shadow,
    val shadow2: V3Shadow,
    val shadow3: V3Shadow,
    val shadowFab: V3Shadow,
    val shadowHero: V3Shadow,
    val shadowSheet: V3Shadow,
)

/** One Modifier.shadow: elevation + ambient / spot colour (Android 9+). */
@Immutable
class V3Shadow(val elevation: Dp, val ambient: Color, val spot: Color)

/** Shadow of a [shape] (no clipping: draw the background after it). */
fun Modifier.v3Shadow(s: V3Shadow, shape: Shape): Modifier =
    shadow(s.elevation, shape, clip = false, ambientColor = s.ambient, spotColor = s.spot)

val V3Light = V3Colors(
    headerTop = Color(0xFF00B159),
    headerBottom = Color(0xFF009E5B),
    onHeader = Color.White,
    sheet = Color(0xFFF8FAFC),
    onSheet = Color(0xFF0F172A),
    onSheetMuted = Color(0xFF64748B),
    card = Color(0xFFE2E8F0),
    cardActive = Color(0xFFFFFFFF),
    onCard = Color(0xFF1E293B),
    cardBorder = Color(0xFFF1F5F9),
    heroOuter = Color(0xFFE8ECEF),
    heroStart = Color(0xFF98F53E),
    heroMid = Color(0xFF83E82B),
    heroEnd = Color(0xFF75DC21),
    onHero = Color(0xFF0F172A),
    heroBalance = Color(0xFF1D4ED8),
    heroWell = Color(0x66FFFFFF),
    yellow = Color(0xFFFFD95A),
    yellowBorder = Color(0xD9FCD34D),
    onYellow = Color(0xFF0F172A),
    yellowChip = Color(0x1A000000),
    yellowChipSelected = Color(0xFFFFFFFF),
    txRow = Color(0xE6FFFFFF),
    onTxRow = Color(0xFF0F172A),
    income = Color(0xFF047857),
    expense = Color(0xFFE11D48),
    incomePill = Color(0xFF00A859),
    expensePill = Color(0xFFFF4D4D),
    onPill = Color.White,
    accent = Color(0xFF047857),
    fab = Color(0xFF33999E),
    onFab = Color(0xFFF6E8CE),
    nav = Color(0xFFE5E7EB),
    navBorder = Color(0xCCCBD5E1),
    shadow1 = V3Shadow(2.dp, Color(0x0D000000), Color(0x0F000000)),
    shadow2 = V3Shadow(4.dp, Color(0x14002312), Color(0x1F002312)),
    shadow3 = V3Shadow(6.dp, Color(0x1A002312), Color(0x24002312)),
    shadowFab = V3Shadow(8.dp, Color(0x3D145358), Color(0x29000000)),
    shadowHero = V3Shadow(5.dp, Color(0x0F0F172A), Color(0x140F172A)),
    shadowSheet = V3Shadow(12.dp, Color(0x24002814), Color(0x24002814)),
)

val V3Dark = V3Colors(
    headerTop = Color(0xFF0A291C),
    headerBottom = Color(0xFF0E3524),
    onHeader = Color(0xFFE8F5EE),
    sheet = Color(0xFF111916),
    onSheet = Color(0xFFE2EBE6),
    onSheetMuted = Color(0xFF94A3A0),
    card = Color(0xFF182420),
    cardActive = Color(0xFF20312B),
    onCard = Color(0xFFE2EBE6),
    cardBorder = Color(0xFF24342E),
    heroOuter = Color(0xFF1F332B),
    heroStart = Color(0xFF143324),
    heroMid = Color(0xFF173B2A),
    heroEnd = Color(0xFF1B4332),
    onHero = Color(0xFFE2EBE6),
    heroBalance = Color(0xFF4ADE80),
    heroWell = Color(0x40000000),
    yellow = Color(0xFF261F10),
    yellowBorder = Color(0x52F59E0B),
    onYellow = Color(0xFFFDE68A),
    yellowChip = Color(0x4D000000),
    yellowChipSelected = Color(0x40F59E0B),
    txRow = Color(0xE61F2925),
    onTxRow = Color(0xFFE2EBE6),
    income = Color(0xFF4ADE80),
    expense = Color(0xFFFB7185),
    incomePill = Color(0x4D059669),
    expensePill = Color(0x40E11D48),
    onPill = Color(0xFFE2EBE6),
    accent = Color(0xFF34D399),
    fab = Color(0xFF059669),
    onFab = Color.White,
    nav = Color(0xFF0E1513),
    navBorder = Color(0xFF1E2C27),
    shadow1 = V3Shadow(2.dp, Color(0x59000000), Color(0x73000000)),
    shadow2 = V3Shadow(4.dp, Color(0x73000000), Color(0x8C000000)),
    shadow3 = V3Shadow(6.dp, Color(0x8C000000), Color(0xA6000000)),
    shadowFab = V3Shadow(8.dp, Color(0x99000000), Color(0x4010B981)),
    shadowHero = V3Shadow(6.dp, Color(0x73000000), Color(0x73000000)),
    shadowSheet = V3Shadow(12.dp, Color(0xA6000000), Color(0xA6000000)),
)

private val LocalV3Colors = staticCompositionLocalOf { V3Light }

object ChiLiV3 {
    val colors: V3Colors
        @Composable @ReadOnlyComposable get() = LocalV3Colors.current
}

/** Font of v3: one weight only, full Vietnamese. (c) Tom Truong, all rights reserved: personal use, never publish the APK or the font. */
val UvnFontFamily = FontFamily(Font(R.font.uvn_ai_cap_nhe, FontWeight.Normal))

/**
 * Size multiplier of the whole UVN type scale (user pick N2: option A 1.15 x 0.85 = about 0.98 of the sp used today).
 * The ONE place to calibrate. UVN draws small: x-height 0.43 em vs Roboto 0.53, cap 0.61 vs 0.71.
 */
const val V3_FONT_CALIBRATION = 0.98f

/** Material 3 type scale in UVN. Every size = Material default x [calibration]; no fake bold (the font has one weight). */
fun v3Typography(calibration: Float = V3_FONT_CALIBRATION): Typography {
    // Fixed 1.4 line height with no trimming: stacked Vietnamese marks on capitals ("Ệ") are never cut off.
    fun t(size: Float) = TextStyle(
        fontFamily = UvnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (size * calibration).sp,
        lineHeight = (size * calibration * 1.4f).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
    return Typography(
        displayLarge = t(57f), displayMedium = t(45f), displaySmall = t(36f),
        headlineLarge = t(32f), headlineMedium = t(28f), headlineSmall = t(24f),
        titleLarge = t(22f), titleMedium = t(16f), titleSmall = t(14f),
        bodyLarge = t(16f), bodyMedium = t(14f), bodySmall = t(12f),
        labelLarge = t(14f), labelMedium = t(12f), labelSmall = t(11f),
    )
}

private fun v3ColorScheme(c: V3Colors, dark: Boolean): ColorScheme {
    val container = if (dark) Color(0xFF14532D) else Color(0xFFD1FAE5)
    val onContainer = if (dark) Color(0xFFBBF7D0) else Color(0xFF064E3B)
    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.accent,
        onPrimary = if (dark) Color(0xFF003828) else Color.White,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = c.accent,
        onSecondary = if (dark) Color(0xFF003828) else Color.White,
        secondaryContainer = container,
        onSecondaryContainer = onContainer,
        background = c.sheet,
        onBackground = c.onSheet,
        // surface = card colour (screens draw cards with `surface`).
        surface = c.cardActive,
        onSurface = c.onSheet,
        surfaceVariant = c.card,
        onSurfaceVariant = c.onSheetMuted,
        surfaceTint = c.accent,
        outline = c.onSheetMuted,
        outlineVariant = c.cardBorder,
        error = c.expense,
        surfaceContainerLowest = c.sheet,
        surfaceContainerLow = c.cardActive,
        surfaceContainer = c.cardActive,
        surfaceContainerHigh = c.card,
        surfaceContainerHighest = c.card,
    )
}

/** v3 theme: colour tokens, Material scheme and the UVN type scale. */
@Composable
fun V3Theme(dark: Boolean, calibration: Float = V3_FONT_CALIBRATION, content: @Composable () -> Unit) {
    val colors = if (dark) V3Dark else V3Light
    CompositionLocalProvider(LocalV3Colors provides colors) {
        MaterialTheme(colorScheme = v3ColorScheme(colors, dark), typography = v3Typography(calibration), content = content)
    }
}
