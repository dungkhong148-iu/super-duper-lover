package com.superduperlover.finance.ui.home

import androidx.compose.animation.core.animate
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.ui.theme.ChiLiV3
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val SheetShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)

/**
 * v3 Home page: [carousel] sits fixed on the header gradient; the white sheet (a pager of pages, [pageContent] gets
 * the page and its scroll state) is one plate drawn on top, shared by every page. Swipe up slides the plate up and covers the carousel, then the list
 * scrolls; swipe down scrolls the list back to its top, then moves the plate down. The handle drags / taps it.
 * The sticky bar above the pager is never touched.
 */
@Composable
fun CollapsingSheetPage(
    carousel: @Composable () -> Unit,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageContent: @Composable (page: Int, scroll: ScrollState) -> Unit,
) {
    val c = ChiLiV3.colors
    // Carousel height (measured: it grows with the font scale) = how far the plate can slide up. offset: 0 .. -carouselPx.
    var carouselPx by remember { mutableIntStateOf(0) }
    var offset by remember { mutableFloatStateOf(0f) }
    // One list per page; the plate position is shared, only the visible page's list is scrolled back first.
    val scrolls = List(pagerState.pageCount) { rememberScrollState() }
    val scroll = scrolls[pagerState.currentPage]
    val scope = rememberCoroutineScope()

    suspend fun settle(target: Float) = animate(offset, target) { v, _ -> offset = v }
    suspend fun snap() = settle(if (offset < -carouselPx / 2f) -carouselPx.toFloat() else 0f)

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val max = carouselPx.toFloat()
                if (available.y < 0f && offset > -max) {
                    val new = (offset + available.y).coerceAtLeast(-max)
                    return Offset(0f, new - offset).also { offset = new }
                }
                // Swipe down while the plate covers the carousel: list back to its top first, then the plate.
                // Done here (not left to the list) so it works with short content and ignores overscroll.
                if (available.y > 0f && offset < 0f) {
                    val back = -scroll.dispatchRawDelta(-available.y.coerceAtMost(scroll.value.toFloat()))
                    val new = (offset + available.y - back).coerceAtMost(0f)
                    return Offset(0f, back + (new - offset)).also { offset = new }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                snap()
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier.fillMaxSize().clipToBounds()
            .background(Brush.verticalGradient(listOf(c.headerTop, c.headerBottom)))
            .nestedScroll(connection),
    ) {
        Box(Modifier.fillMaxWidth().onSizeChanged { carouselPx = it.height }) { carousel() }

        Surface(
            Modifier.layout { m, cons ->
                val top = (carouselPx + offset).roundToInt().coerceIn(0, cons.maxHeight)
                val p = m.measure(Constraints.fixed(cons.maxWidth, cons.maxHeight - top))
                layout(cons.maxWidth, cons.maxHeight) { p.place(0, top) }
            },
            shape = SheetShape, color = c.sheet, shadowElevation = c.shadowSheet.elevation,
        ) {
            Column {
                Box(
                    Modifier.fillMaxWidth()
                        .draggable(
                            rememberDraggableState { d -> offset = (offset + d).coerceIn(-carouselPx.toFloat(), 0f) },
                            Orientation.Vertical,
                            onDragStopped = { snap() },
                        )
                        .clickable { scope.launch { settle(if (offset < 0f) 0f else -carouselPx.toFloat()) } }
                        .padding(top = 10.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.width(40.dp).height(6.dp).clip(CircleShape).background(c.onSheetMuted.copy(alpha = 0.35f)))
                }
                HorizontalPager(pagerState, Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) { page ->
                    pageContent(page, scrolls[page])
                }
            }
        }
    }
}
