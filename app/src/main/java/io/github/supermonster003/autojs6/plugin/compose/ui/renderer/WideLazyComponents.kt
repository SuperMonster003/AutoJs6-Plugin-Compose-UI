package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import org.autojs.plugin.compose.api.ComposeUiEventFields as F
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Components as C
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Props as V
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Events as VE
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Fields as VF
import kotlin.math.roundToInt

@Composable
internal fun WideLazyComponents(node: UiNode, modifier: Modifier, commands: NodeCommandRegistry,
    child: (Int) -> UiNode, render: @Composable (Int) -> Unit, emit: (String, Bundle) -> Unit) {
    val currentNode by rememberUpdatedState(node)
    val currentEmit by rememberUpdatedState(emit)
    if (node.type == C.LAZY_VERTICAL_GRID) {
        val state = remember(node.nodeId) { LazyGridState() }
        DisposableEffect(commands, node.nodeId, state) {
            val unregister = commands.registerComponent(node.nodeId, NodeCommandRegistry.Handle(
                scroll = { index, offset -> state.requestScrollToItem(index ?: 0, offset ?: 0) }, indexedScroll = true))
            onDispose { unregister() }
        }
        LaunchedEffect(state) {
            var previous = state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
            snapshotFlow { state.layoutInfo }.drop(1).conflate().collect {
                withFrameNanos { }
                val next = state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
                if (next != previous) {
                    previous = next
                    currentEmit(E.SCROLL, Bundle().apply { putInt(F.FIRST_VISIBLE_INDEX, next.first); putInt(F.OFFSET, next.second) })
                }
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(node.wideInt(V.COLUMNS, 2)), modifier = modifier,
            state = state, contentPadding = lazyContentPadding(node),
            reverseLayout = node.wideBoolean(P.REVERSE_LAYOUT),
            userScrollEnabled = node.wideBoolean(P.USER_SCROLL_ENABLED, true),
            verticalArrangement = Arrangement.spacedBy(node.wideDp(P.SPACING)),
            horizontalArrangement = Arrangement.spacedBy(node.wideDp(P.SPACING))) {
            items(node.children, key = { child(it).key ?: it }, contentType = { child(it).type }) { render(it) }
        }
    } else {
        val page = node.wideInt(V.PAGE)
        val state = rememberPagerState(initialPage = page) { currentNode.children.size }
        var requestSerial by remember(node.nodeId) { mutableLongStateOf(0L) }
        // Only an explicit page change repositions. Unrelated frames preserve the current item key.
        LaunchedEffect(page) { if (state.currentPage != page) state.requestScrollToPage(page) }
        DisposableEffect(commands, node.nodeId, state) {
            val unregister = commands.registerComponent(node.nodeId, NodeCommandRegistry.Handle(
                scroll = scroll@{ index, offset ->
                    val ticket = ++requestSerial
                    var pageSize = state.layoutInfo.pageSize + state.layoutInfo.pageSpacing
                    if (pageSize == 0 && offset != null && offset != 0) {
                        // Keep the pixel request until a real measure can convert it. A newer
                        // request or disposal wakes and cancels this suspended predecessor.
                        val measured = snapshotFlow { (state.layoutInfo.pageSize + state.layoutInfo.pageSpacing) to requestSerial }
                            .first { it.first > 0 || it.second != ticket }
                        if (measured.second != ticket) return@scroll
                        pageSize = measured.first
                    }
                    // Frozen ScrollTo uses pixels, while Pager's public request uses fractions.
                    // Normalize whole pages before the fractional remainder; request remeasure so
                    // a same-turn accepted growth is not clamped against an old item provider.
                    val target = pagerPixelRequest(index, offset, pageSize)
                    state.requestScrollToPage(target.first, target.second)
                }, indexedScroll = true))
            onDispose { requestSerial++; unregister() }
        }
        LaunchedEffect(state) {
            var previous = state.settledPage
            var previousScroll = state.currentPage to (state.currentPageOffsetFraction * state.layoutInfo.pageSize).roundToInt()
            snapshotFlow { state.layoutInfo to state.settledPage }.drop(1).conflate().collect {
                withFrameNanos { }
                val next = state.settledPage
                if (next != previous) {
                    previous = next
                    currentEmit(VE.PAGE_CHANGE, Bundle().apply { putInt(VF.PAGE, next) })
                }
                val scroll = state.currentPage to (state.currentPageOffsetFraction * (state.layoutInfo.pageSize + state.layoutInfo.pageSpacing)).roundToInt()
                if (scroll != previousScroll) {
                    previousScroll = scroll
                    currentEmit(E.SCROLL, Bundle().apply { putInt(F.FIRST_VISIBLE_INDEX, scroll.first); putInt(F.OFFSET, scroll.second) })
                }
            }
        }
        HorizontalPager(state = state, modifier = modifier,
            userScrollEnabled = node.wideBoolean(P.USER_SCROLL_ENABLED, true), pageSpacing = node.wideDp(V.PAGE_SPACING),
            key = { index -> child(node.children[index]).key ?: node.children[index] }) { index -> render(node.children[index]) }
    }
}

/** Long arithmetic prevents signed offset overflow before converting to the public pager fraction. */
internal fun pagerPixelRequest(index: Int?, offset: Int?, pageSize: Int): Pair<Int, Float> {
    if (pageSize <= 0) return (index ?: 0) to 0f
    val size = pageSize.toLong()
    val absolute = ((index ?: 0).toLong() * size + (offset ?: 0)).coerceAtLeast(0L)
    var page = absolute / size
    var fraction = (absolute % size).toDouble() / size
    if (fraction > 0.5) { page++; fraction-- }
    if (page > Int.MAX_VALUE) return Int.MAX_VALUE to 0f
    return page.toInt() to fraction.toFloat()
}
