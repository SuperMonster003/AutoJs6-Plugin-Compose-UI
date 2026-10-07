package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.flow.first
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Props as V

/**
 * M3 1.4.0 ModalBottomSheet cannot set an overlay window type before show. Its public sheet
 * scaffold/state combined with Compose Dialog supply the same drag, scrim and modal behavior.
 * The application/overlay choice is inherited from the real root window through public APIs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WideModalBottomSheet(node: UiNode, modifier: Modifier, content: @Composable () -> Unit,
    emit: (String, Bundle) -> Unit) {
    val currentNode by rememberUpdatedState(node)
    val currentEmit by rememberUpdatedState(emit)
    val cancelable = node.wideBoolean(V.CANCELABLE, true)
    val dismiss = { if (currentNode.wideBoolean(V.CANCELABLE, true)) currentEmit(E.DISMISS_REQUEST, Bundle()); Unit }
    var entered by remember { mutableStateOf(false) }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { target ->
        when {
            target == SheetValue.PartiallyExpanded -> false
            target == SheetValue.Hidden && entered -> { dismiss(); false }
            else -> true
        }
    })
    LaunchedEffect(state) {
        snapshotFlow { state.hasExpandedState }.first { it }
        state.show()
        entered = true
    }
    val rootParameters = LocalView.current.rootView.layoutParams as? WindowManager.LayoutParams
    val inheritedType = LocalRendererWindowType.current ?: rootParameters?.type?.takeIf { it >= WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW }
        ?: WindowManager.LayoutParams.TYPE_APPLICATION
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(
        dismissOnBackPress = cancelable, dismissOnClickOutside = false,
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
        windowType = inheritedType, windowToken = null)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) }
        val label = stringResource(android.R.string.cancel)
        BottomSheetScaffold(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = state),
            sheetPeekHeight = 0.dp, sheetSwipeEnabled = cancelable && entered,
            sheetDragHandle = if (cancelable) ({ BottomSheetDefaults.DragHandle() }) else null,
            sheetShape = (node.props[P.SHAPE] as? UiValue.Shape)?.let(ValueMapper::shape) ?: BottomSheetDefaults.ExpandedShape,
            sheetContainerColor = ValueMapper.color(node.props[P.CONTAINER_COLOR], BottomSheetDefaults.ContainerColor),
            sheetContentColor = ValueMapper.color(node.props[P.CONTENT_COLOR], contentColorFor(
                ValueMapper.color(node.props[P.CONTAINER_COLOR], BottomSheetDefaults.ContainerColor))),
            sheetContent = { Box(modifier.fillMaxWidth()) { content() } },
            snackbarHost = {}, containerColor = Color.Transparent) {
            var scrim = Modifier.fillMaxSize().background(BottomSheetDefaults.ScrimColor)
                .pointerInput(cancelable) { detectTapGestures { dismiss() } }
            if (cancelable) scrim = scrim.semantics { contentDescription = label; onClick(label) { dismiss(); true } }
            Box(scrim)
        }
    }
}
