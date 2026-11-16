package com.superduperlover.finance.ui.theme

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.composed
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
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
    /** Negative amounts on the Hero, and the Thu nhập / Chi tiêu pills on it. */
    val heroNegative: Color,
    val heroPillIncome: Color,
    val heroPillExpense: Color,
    /** Icon well on account cards. */
    val iconWell: Color,
    /** Round button on the Hero (eye). */
    val heroButton: Color,
    /** "Còn được tiêu" chip on the Hero and its text. */
    val heroChip: Color,
    val onHeroChip: Color,
    /** "Xem chi tiết" button on the yellow block and its text. */
    val yellowAction: Color,
    val onYellowAction: Color,
    /** Embossed text shadow of the big Hero balance (light only). */
    val heroBalanceShadow: Shadow,
    val income: Color,
    val expense: Color,
    val incomePill: Color,
    val expensePill: Color,
    val onPill: Color,
    /** Circle behind the icon on the Hero Thu nhập / Chi tiêu pills. */
    val pillIconWell: Color,
    /** Expense dot on the selected (accent) calendar day. */
    val onAccentExpense: Color,
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
    /** Form top bar / chip / save colours per feature (white text on them). */
    val jarAccent: Color,
    val budgetAccent: Color,
    val debtAccent: Color,
)

/** One Modifier.shadow: elevation + ambient / spot colour (Android 9+). */
@Immutable
class V3Shadow(val elevation: Dp, val ambient: Color, val spot: Color)

/**
 * Press effect of the mock (.tap-scale): scales to 0.96 for 140 ms while pressed, no ripple. Put it FIRST in the chain
 * (before shadow / clip / background) so the whole item scales; it also makes the item clickable.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tapScale(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(140, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "tapScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(interactionSource = source, indication = null, onLongClick = onLongClick, onClick = onClick)
}

/** Shadow of a [shape] (no clipping: draw the background after it). */
fun Modifier.v3Shadow(s: V3Shadow, shape: Shape): Modifier =
    shadow(s.elevation, shape, clip = false, ambientColor = s.ambient, spotColor = s.spot)

val V3Light = V3Colors(
    headerTop = Color(0xFF00B159),
    headerBottom = Color(0xFF009E5B),
    onHeader = Color.White,
    sheet = Color(0xFFF8FAFC),
    onSheet = Color(0xFF020617),
    onSheetMuted = Color(0xFF475569),
    card = Color(0xFFE2E8F0),
    cardActive = Color(0xFFFFFFFF),
    onCard = Color(0xFF0F172A),
    cardBorder = Color(0xFFF1F5F9),
    heroOuter = Color(0xFFE8ECEF),
    // Same deep emerald as the Lịch health card (headerTop -> headerBottom).
    heroStart = Color(0xFF00B159),
    heroMid = Color(0xFF00A65A),
    heroEnd = Color(0xFF009E5B),
    onHero = Color.White,
    heroBalance = Color.White,
    heroWell = Color(0x26000000),
    yellow = Color(0xFFF6B01E),
    yellowBorder = Color(0xD9D97706),
    onYellow = Color(0xFF020617),
    yellowChip = Color(0x26000000),
    yellowChipSelected = Color(0xFFFFFFFF),
    txRow = Color(0xE6FFFFFF),
    onTxRow = Color(0xFF020617),
    iconWell = Color(0xCCFFFFFF),
    heroNegative = Color(0xFFFECDD3),
    heroPillIncome = Color(0xFF065F46),
    heroPillExpense = Color(0xFFE11D48),
    heroButton = Color(0x33FFFFFF),
    heroChip = Color(0x40FFFFFF),
    onHeroChip = Color.White,
    yellowAction = Color(0xE60F172A),
    onYellowAction = Color.White,
    heroBalanceShadow = Shadow(Color(0x73FFFFFF), Offset(0f, 2f), 0f),
    income = Color(0xFF047857),
    expense = Color(0xFFE11D48),
    incomePill = Color(0xFF00A859),
    expensePill = Color(0xFFFF4D4D),
    onPill = Color.White,
    pillIconWell = Color(0x59CBD5E1),
    onAccentExpense = Color(0xFFFFB3B0),
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
    jarAccent = Color(0xFF00854A),
    budgetAccent = Color(0xFFC26A00),
    debtAccent = Color(0xFFE11D48),
)

// Dark = neutral charcoal (user pick D1 option A): grey-black layers >= 1.3:1 apart, colour only on Hero, yellow block
// and pills, solid fills (no translucent pill over a shadow). Text >= 4.5:1, UI shapes >= 3:1 where it matters.
val V3Dark = V3Colors(
    headerTop = Color(0xFF0F3D27),
    headerBottom = Color(0xFF0C3320),
    onHeader = Color(0xFFF0FDF4),
    sheet = Color(0xFF121417),
    onSheet = Color(0xFFE8ECEF),
    onSheetMuted = Color(0xFFB4BCC6),
    card = Color(0xFF1F2329),
    cardActive = Color(0xFF2A3038),
    onCard = Color(0xFFE8ECEF),
    cardBorder = Color(0xFF38404A),
    heroOuter = Color(0xFF2A3038),
    heroStart = Color(0xFF1E6B45),
    heroMid = Color(0xFF1A5E3D),
    heroEnd = Color(0xFF165235),
    onHero = Color.White,
    heroBalance = Color(0xFFBEF264),
    heroWell = Color(0x33000000),
    yellow = Color(0xFF3A2F12),
    yellowBorder = Color(0x66F59E0B),
    onYellow = Color(0xFFFDE68A),
    yellowChip = Color(0x4D000000),
    yellowChipSelected = Color(0xFF5C4714),
    txRow = Color(0xFF15181C),
    onTxRow = Color(0xFFE8ECEF),
    iconWell = Color(0xFF343A43),
    heroNegative = Color(0xFFFDA4AF),
    heroPillIncome = Color(0xFF15803D),
    heroPillExpense = Color(0xFFBE123C),
    heroButton = Color(0x1AFFFFFF),
    heroChip = Color(0xFF0B2E1E),
    onHeroChip = Color(0xFF86EFAC),
    yellowAction = Color(0xFFFCD34D),
    onYellowAction = Color(0xFF1F1A0A),
    heroBalanceShadow = Shadow.None,
    income = Color(0xFF4ADE80),
    expense = Color(0xFFFDA4AF),
    incomePill = Color(0xFF15803D),
    expensePill = Color(0xFFBE123C),
    onPill = Color.White,
    pillIconWell = Color(0x33FFFFFF),
    onAccentExpense = Color(0xFF881337),
    accent = Color(0xFF34D399),
    fab = Color(0xFF047857),
    onFab = Color.White,
    nav = Color(0xFF1F2329),
    navBorder = Color(0xFF38404A),
    shadow1 = V3Shadow(2.dp, Color(0x59000000), Color(0x73000000)),
    shadow2 = V3Shadow(4.dp, Color(0x73000000), Color(0x8C000000)),
    shadow3 = V3Shadow(6.dp, Color(0x8C000000), Color(0xA6000000)),
    shadowFab = V3Shadow(8.dp, Color(0x99000000), Color(0x4010B981)),
    shadowHero = V3Shadow(6.dp, Color(0x73000000), Color(0x73000000)),
    shadowSheet = V3Shadow(12.dp, Color(0xA6000000), Color(0xA6000000)),
    jarAccent = Color(0xFF059669),
    budgetAccent = Color(0xFFB45309),
    debtAccent = Color(0xFFBE123C),
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

/** Colour of the form being shown (null = the neutral accent); set by FormDialog, read by chips, choice buttons, ticks. */
val LocalFormAccent = androidx.compose.runtime.compositionLocalOf<Color?> { null }
val LocalOnFormAccent = androidx.compose.runtime.compositionLocalOf<Color?> { null }

@Composable
fun formAccent(): Color = LocalFormAccent.current ?: MaterialTheme.colorScheme.primary

@Composable
fun onFormAccent(): Color = LocalOnFormAccent.current ?: MaterialTheme.colorScheme.onPrimary
