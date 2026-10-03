package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiEventFields as F
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.ComposeUiSlots as S
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue

/** Slot children do not inherit Material's implementation-only Row/Column/Box scopes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdvancedComponents(
    node: UiNode,
    modifier: Modifier,
    commands: NodeCommandRegistry,
    snackbar: SnackbarController,
    childKey: (Int) -> Any,
    childType: (Int) -> String? = { null },
    renderChild: @Composable (Int) -> Unit,
    emit: (type: String, callbackId: Int, payload: Bundle) -> Unit,
) {
    when (node.type) {
        C.LAZY_COLUMN, C.LAZY_ROW -> {
            val state = remember(node.nodeId) { LazyListState() }
            val currentNode by rememberUpdatedState(node)
            val currentEmit by rememberUpdatedState(emit)
            DisposableEffect(commands, node.nodeId, state) {
                val unregister = commands.registerComponent(node.nodeId, NodeCommandRegistry.Handle(
                    scroll = { index, offset ->
                        // The renderer validates the latest accepted tree before running this
                        // action. Composed children can still belong to the previous frame here.
                        // Request the next remeasure so its new item provider receives the target,
                        // instead of clamping against old items or preserving their first-item key.
                        // Without an index, the contract specifies an absolute pixel offset.
                        state.requestScrollToItem(index ?: 0, offset ?: 0)
                    },
                    indexedScroll = true,
                ))
                onDispose { unregister() }
            }
            LaunchedEffect(state) {
                var last = state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
                // A requested scroll changes position before layout. Observe completed measures
                // so accepting a command alone does not masquerade as a rendered scroll event.
                snapshotFlow { state.layoutInfo }
                    .drop(1).conflate().collect {
                        // Report the most recent position at most once per frame. A fling never
                        // creates an unbounded coroutine/event backlog or emits old queued offsets.
                        withFrameNanos { }
                        val position = state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
                        if (position != last) {
                            last = position
                            currentNode.callbacks[E.SCROLL]?.let { callback ->
                                currentEmit(E.SCROLL, callback, Bundle().apply {
                                    putInt(F.FIRST_VISIBLE_INDEX, position.first)
                                    putInt(F.OFFSET, position.second)
                                })
                            }
                        }
                    }
            }
            val reverseLayout = node.boolean(P.REVERSE_LAYOUT)
            val userScrollEnabled = node.boolean(P.USER_SCROLL_ENABLED, true)
            if (node.type == C.LAZY_COLUMN) LazyColumn(
                modifier = modifier, state = state, contentPadding = lazyContentPadding(node),
                reverseLayout = reverseLayout, userScrollEnabled = userScrollEnabled,
                verticalArrangement = verticalArrangement(node),
                horizontalAlignment = horizontalAlignment(node.enumeration(P.ALIGNMENT, "start")),
            ) {
                items(node.children, key = childKey, contentType = childType) { renderChild(it) }
            } else LazyRow(
                modifier = modifier, state = state, contentPadding = lazyContentPadding(node),
                reverseLayout = reverseLayout, userScrollEnabled = userScrollEnabled,
                horizontalArrangement = horizontalArrangement(node),
                verticalAlignment = verticalAlignment(node.enumeration(P.ALIGNMENT, "top")),
            ) {
                items(node.children, key = childKey, contentType = childType) { renderChild(it) }
            }
        }
        C.SCAFFOLD -> {
            val color = ValueMapper.color(node.props[P.CONTAINER_COLOR], MaterialTheme.colorScheme.background)
            Scaffold(
                modifier = modifier,
                topBar = { node.slots[S.TOP_BAR]?.let { renderChild(it) } },
                snackbarHost = {
                    val custom = node.slots[S.SNACKBAR_HOST]
                    if (custom != null) renderChild(custom)
                    else snackbar.hostState(node.nodeId)?.let { SnackbarHost(it) }
                },
                containerColor = color,
                contentColor = ValueMapper.color(node.props[P.CONTENT_COLOR], contentColorFor(color)),
                // The host owns system-bar and IME insets. Only topBar's measured height is applied.
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { padding ->
                Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                    val child = node.slots[S.CONTENT] ?: node.children.firstOrNull()
                    child?.let { renderChild(it) }
                }
            }
        }
        C.TOP_APP_BAR -> {
            val content = ValueMapper.color(node.props[P.CONTENT_COLOR])
            TopAppBar(
                title = { node.slots[S.TITLE]?.let { renderChild(it) } },
                modifier = modifier,
                navigationIcon = { node.slots[S.NAVIGATION_ICON]?.let { renderChild(it) } },
                actions = { node.slots[S.ACTIONS]?.let { renderChild(it) } },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ValueMapper.color(node.props[P.CONTAINER_COLOR]),
                    navigationIconContentColor = content, titleContentColor = content,
                    actionIconContentColor = content, subtitleContentColor = content,
                ),
            )
        }
        C.ALERT_DIALOG -> if (node.boolean(P.OPEN)) {
            val currentNode by rememberUpdatedState(node)
            val currentEmit by rememberUpdatedState(emit)
            AlertDialog(
                onDismissRequest = {
                    currentNode.callbacks[E.DISMISS_REQUEST]?.let { currentEmit(E.DISMISS_REQUEST, it, Bundle()) }
                    // The dialog stays open until the host accepts an open=false tree update.
                },
                confirmButton = { renderChild(node.slots.getValue(S.CONFIRM)) },
                modifier = modifier,
                dismissButton = optionalSlot(node, S.DISMISS, renderChild),
                icon = optionalSlot(node, S.ICON, renderChild),
                title = optionalSlot(node, S.TITLE, renderChild),
                text = optionalSlot(node, S.TEXT, renderChild),
                shape = (node.props[P.SHAPE] as? UiValue.Shape)?.let(ValueMapper::shape) ?: AlertDialogDefaults.shape,
                containerColor = ValueMapper.color(node.props[P.CONTAINER_COLOR], AlertDialogDefaults.containerColor),
            )
        }
        C.CIRCULAR_PROGRESS_INDICATOR -> {
            val progress = (node.props[P.PROGRESS] as? UiValue.Num)?.value?.toFloat()
            val color = ValueMapper.color(node.props[P.COLOR], ProgressIndicatorDefaults.circularColor)
            if (progress == null) CircularProgressIndicator(
                modifier = modifier, color = color,
                trackColor = ValueMapper.color(node.props[P.TRACK_COLOR], ProgressIndicatorDefaults.circularIndeterminateTrackColor),
            ) else CircularProgressIndicator(
                progress = { progress }, modifier = modifier, color = color,
                trackColor = ValueMapper.color(node.props[P.TRACK_COLOR], ProgressIndicatorDefaults.circularDeterminateTrackColor),
            )
        }
        C.LINEAR_PROGRESS_INDICATOR -> {
            val progress = (node.props[P.PROGRESS] as? UiValue.Num)?.value?.toFloat()
            val color = ValueMapper.color(node.props[P.COLOR], ProgressIndicatorDefaults.linearColor)
            val track = ValueMapper.color(node.props[P.TRACK_COLOR], ProgressIndicatorDefaults.linearTrackColor)
            if (progress == null) LinearProgressIndicator(modifier = modifier, color = color, trackColor = track)
            else LinearProgressIndicator(progress = { progress }, modifier = modifier, color = color, trackColor = track)
        }
        else -> throw ComposeUiContractException(ComposeUiErrorCodes.UNKNOWN_COMPONENT, nodeId = node.nodeId)
    }
}

/** A nullable slot preserves Material's own spacing when the optional slot is absent. */
private fun optionalSlot(node: UiNode, name: String, render: @Composable (Int) -> Unit): (@Composable () -> Unit)? =
    node.slots[name]?.let { id -> @Composable { render(id) } }

internal fun lazyContentPadding(node: UiNode): PaddingValues {
    val value = node.props[P.CONTENT_PADDING]
    val values = when (value) {
        null -> listOf(0f)
        is UiValue.Dp -> listOf(value.value.toFloat())
        is UiValue.ListOf -> value.values.map { (it as UiValue.Dp).value.toFloat() }
        else -> throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = node.nodeId, prop = P.CONTENT_PADDING)
    }
    return when (values.size) {
        1 -> PaddingValues(values[0].dp)
        2 -> PaddingValues(horizontal = values[0].dp, vertical = values[1].dp)
        4 -> PaddingValues(start = values[0].dp, top = values[1].dp, end = values[2].dp, bottom = values[3].dp)
        else -> throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = node.nodeId, prop = P.CONTENT_PADDING)
    }
}

private fun UiNode.boolean(name: String, default: Boolean = false) = (props[name] as? UiValue.Bool)?.value ?: default
private fun UiNode.enumeration(name: String, default: String) = (props[name] as? UiValue.Enum)?.value ?: default
