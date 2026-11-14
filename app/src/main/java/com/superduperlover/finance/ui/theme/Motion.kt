package com.superduperlover.finance.ui.theme

import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavBackStackEntry

// Pop-up motion and glow, ready for screens to adopt later. No screen uses this file yet (only the debug "ChiLi FX" demo).
// Everything is instant when the system animator scale is 0 ("remove animations").

/** False when the user turned system animations off (animator duration scale 0). */
@Composable
fun animationsOn(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
}

object Motion {
    const val ENTER_MS = 150
    const val EXIT_MS = 150

    /** Spring damping: lower = bigger overshoot. 0.5 overshoots ~5dp, 0.65 ~2dp on a 32dp slide. */
    const val BOUNCE_STRONG = 0.5f
    const val BOUNCE_MEDIUM = 0.65f

    /** Default until the user picks. */
    const val BOUNCE = BOUNCE_MEDIUM

    /** Enter starts this far below its place. */
    val SLIDE_IN = 32.dp

    /** Exit moves this far down. */
    val SLIDE_OUT = 16.dp

    /** Spring with a visible overshoot, about 0.3 s to settle. */
    fun <T> pop(bounce: Float = BOUNCE) = spring<T>(dampingRatio = bounce, stiffness = 700f)

    /** Slide up from [SLIDE_IN] below, overshoot a little, settle; plus fade. */
    fun popEnter(on: Boolean, density: Density, bounce: Float = BOUNCE): EnterTransition =
        if (on) {
            slideInVertically(animationSpec = pop(bounce), initialOffsetY = { with(density) { SLIDE_IN.roundToPx() } }) +
                fadeIn(tween(ENTER_MS))
        } else {
            EnterTransition.None
        }

    /** Short slide down plus fade, no bounce. */
    fun popExit(on: Boolean, density: Density): ExitTransition =
        if (on) {
            slideOutVertically(animationSpec = tween(EXIT_MS), targetOffsetY = { with(density) { SLIDE_OUT.roundToPx() } }) +
                fadeOut(tween(EXIT_MS))
        } else {
            ExitTransition.None
        }
}

/** Bounce up on first composition (same motion as [Motion.popEnter]). For cards, sheet content, a block that just appeared. */
@Composable
fun Modifier.popIn(bounce: Float = Motion.BOUNCE): Modifier {
    val on = animationsOn()
    val slidePx = with(LocalDensity.current) { Motion.SLIDE_IN.toPx() }
    val progress = remember { Animatable(if (on) 0f else 1f) }
    LaunchedEffect(Unit) { if (progress.value < 1f) progress.animateTo(1f, Motion.pop(bounce)) }
    return graphicsLayer {
        val p = progress.value
        alpha = (p * 3f).coerceIn(0f, 1f)
        translationY = (1f - p) * slidePx
    }
}

/**
 * Dialog that bounces up and slides down on exit. Keep it composed and drive [visible]: it removes itself once the
 * exit has played (`if (show) Dialog` would cut the exit). The window is [Motion.SLIDE_IN] taller on both sides so the slide is not clipped.
 */
@Composable
fun PopDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    bounce: Float = Motion.BOUNCE,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val on = animationsOn()
    val density = LocalDensity.current
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (state.currentState || state.targetState) {
        Dialog(onDismissRequest = onDismissRequest, properties = properties) {
            Box(Modifier.padding(vertical = Motion.SLIDE_IN)) {
                AnimatedVisibility(
                    visibleState = state,
                    enter = Motion.popEnter(on, density, bounce),
                    exit = Motion.popExit(on, density),
                ) { content() }
            }
        }
    }
}

/** Bottom sheet: the usual slide, and the content bounces up on top of it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopSheet(onDismissRequest: () -> Unit, bounce: Float = Motion.BOUNCE, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.popIn(bounce), content = content)
    }
}

/** Screen transitions for `NavHost(enterTransition = nav.enter, exitTransition = nav.exit, popEnterTransition = nav.popEnter, popExitTransition = nav.popExit)`. */
class PopNav(
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
)

@Composable
fun rememberPopNav(bounce: Float = Motion.BOUNCE): PopNav {
    val on = animationsOn()
    val density = LocalDensity.current
    return remember(on, bounce, density) {
        PopNav(
            enter = { Motion.popEnter(on, density, bounce) },
            exit = { if (on) fadeOut(tween(Motion.EXIT_MS)) else ExitTransition.None },
            popEnter = { if (on) fadeIn(tween(Motion.ENTER_MS)) else EnterTransition.None },
            popExit = { Motion.popExit(on, density) },
        )
    }
}

/** For `LazyColumn` rows (they need `key`): new rows fade in, rows below slide to the gap; removed rows fade out. */
@Composable
fun LazyItemScope.popItem(): Modifier =
    if (animationsOn()) {
        Modifier.animateItem(fadeInSpec = tween(Motion.ENTER_MS), fadeOutSpec = tween(Motion.EXIT_MS))
    } else {
        Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
    }

enum class GlowLevel(val spread: Dp, val peakAlpha: Float) {
    /** Selected item / block that was just shown. */
    SOFT(10.dp, 0.22f),

    /** Main buttons (Lưu, +). */
    STRONG(18.dp, 0.42f),
}

private const val GLOW_LAYERS = 8

/**
 * Soft halo that follows the shape, drawn outside the bounds: stacked outlines whose alpha falls off, no blur, so it
 * works on every API level. [color] defaults to the theme mint. [cornerRadius] should match the shape (it is clamped
 * to half the height, so a pill needs only a big value). Needs parents that do not clip it (leave [GlowLevel.spread] of room).
 */
@Composable
fun Modifier.glow(
    level: GlowLevel = GlowLevel.SOFT,
    color: Color = MaterialTheme.colorScheme.primary,
    cornerRadius: Dp = 24.dp,
): Modifier = drawBehind {
    val step = level.spread.toPx() / GLOW_LAYERS
    val r = minOf(cornerRadius.toPx(), minOf(size.width, size.height) / 2f)
    for (i in 0 until GLOW_LAYERS) {
        val fade = 1f - i / GLOW_LAYERS.toFloat()
        val grow = step * i + step / 2f
        drawRoundRect(
            color = color.copy(alpha = level.peakAlpha * fade * fade),
            topLeft = Offset(-grow, -grow),
            size = Size(size.width + 2 * grow, size.height + 2 * grow),
            cornerRadius = CornerRadius(r + grow),
            style = Stroke(width = step + 1f),
        )
    }
}
