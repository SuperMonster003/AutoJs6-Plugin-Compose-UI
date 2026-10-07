package io.github.supermonster003.autojs6.plugin.compose.ui.renderer.dialog

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.OneShotPreDrawListener
import io.github.supermonster003.autojs6.plugin.compose.ui.renderer.RendererPresentation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiEventFields
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.dialog.DialogInteropV1
import org.autojs.plugin.compose.api.dialog.DialogOptionsV1
import org.autojs.plugin.compose.api.model.UiCommand
import java.util.ArrayDeque

/**
 * One real Compose Dialog for both presentations. M3 1.4.0 ModalBottomSheet creates a private
 * application window before its content runs and cannot set an overlay type through public API.
 * Its public scaffold and SheetState provide the sheet, anchors, drag and nested-scroll behavior
 * here; our enclosing Dialog supplies modality and the host-selected window type/token.
 */
internal class DialogPresentation(
    private val options: DialogOptionsV1,
    private val onDismiss: (Bundle) -> Unit,
    private val hasOverlayPermission: () -> Boolean,
    private val onFailure: (Throwable) -> Unit,
) : RendererPresentation {
    private var closed by mutableStateOf(false)
    private var dismissed by mutableStateOf(false)
    private var generation = 0L
    private var ready = false
    private var contentView: View? = null
    private var preDraw: OneShotPreDrawListener? = null
    private var focusObserver: ViewTreeObserver? = null
    private var focusListener: ViewTreeObserver.OnWindowFocusChangeListener? = null
    private var focusWaitSerial = 0L
    private data class PendingCommand(val needsWindowFocus: Boolean, val action: () -> Unit)
    private val pending = ArrayDeque<PendingCommand>()

    @Composable override fun Content(generation: Long, content: @Composable () -> Unit) {
        if (!closed && !dismissed) {
            if (options.type == DialogInteropV1.BOTTOM_SHEET) BottomSheet(generation, content)
            else Alert(generation, content)
        }
    }

    private fun properties(fullScreen: Boolean) = DialogProperties(
        dismissOnBackPress = options.cancelable,
        // The fullscreen sheet owns its scrim; a platform outside click has no separate bounds.
        dismissOnClickOutside = options.cancelable && !fullScreen,
        usePlatformDefaultWidth = !fullScreen,
        decorFitsSystemWindows = !fullScreen,
        windowType = options.windowType,
        windowToken = options.windowToken,
    )

    @Composable private fun Alert(generation: Long, content: @Composable () -> Unit) {
        Dialog(onDismissRequest = ::requestDismiss, properties = properties(fullScreen = false)) {
            ContentReady(generation)
            Surface(modifier = Modifier.widthIn(min = 280.dp, max = 560.dp).testTag(ALERT_TAG),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp) {
                Box(Modifier.padding(24.dp)) { content() }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun BottomSheet(generation: Long, content: @Composable () -> Unit) {
        // Skipping partial expansion gives distinct Expanded and Hidden anchors with peek=0.
        // confirmValueChange also guards the drag-handle accessibility action when not cancelable.
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.PartiallyExpanded && (it != SheetValue.Hidden || options.cancelable) })
        var opened by remember { mutableStateOf(false) }
        var hiding by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val dismiss: () -> Unit = {
            if (options.cancelable && !closed && !dismissed && !hiding) {
                hiding = true
                scope.launch {
                    try {
                        sheet.hide()
                        if (!sheet.isVisible) requestDismiss()
                    } finally { hiding = false }
                }
            }
        }
        // The initial Hidden value is preparation, never a user dismissal. Wait for real anchors.
        LaunchedEffect(sheet) {
            snapshotFlow { sheet.hasExpandedState }.first { it }
            if (!closed && !dismissed && !hiding) {
                sheet.show()
                opened = true
            }
        }
        LaunchedEffect(sheet, opened) {
            if (opened) {
                snapshotFlow { sheet.currentValue == SheetValue.Hidden && !sheet.isAnimationRunning }
                    .first { it }
                requestDismiss()
            }
        }
        Dialog(onDismissRequest = dismiss, properties = properties(fullScreen = true)) {
            ContentReady(generation)
            // Only the Compose scrim dims this window. Type/token were set by DialogProperties
            // before show; changing ordinary window flags after attachment is a public operation.
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect { window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) }
            val cancelLabel = stringResource(android.R.string.cancel)
            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
                scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheet),
                sheetPeekHeight = 0.dp,
                sheetSwipeEnabled = options.cancelable && opened && !hiding,
                sheetDragHandle = if (options.cancelable) ({ BottomSheetDefaults.DragHandle() }) else null,
                sheetContent = {
                    Box(Modifier.fillMaxWidth().testTag(SHEET_TAG).padding(horizontal = 24.dp, vertical = 16.dp)) { content() }
                },
                snackbarHost = {},
                containerColor = Color.Transparent,
            ) {
                var scrim = Modifier.fillMaxSize().testTag(SCRIM_TAG)
                    .background(BottomSheetDefaults.ScrimColor)
                    .pointerInput(options.cancelable) { detectTapGestures { dismiss() } }
                if (options.cancelable) scrim = scrim.semantics {
                    contentDescription = cancelLabel
                    onClick(label = cancelLabel) { dismiss(); true }
                }
                Box(scrim)
            }
        }
    }

    @Composable private fun ContentReady(frameGeneration: Long) {
        val view = LocalView.current
        SideEffect { awaitDraw(view, frameGeneration) }
    }

    private fun awaitDraw(view: View, frameGeneration: Long) {
        if (closed || dismissed || generation != frameGeneration) return
        if (contentView !== view) {
            clearReadiness()
            contentView = view
        }
        if (ready) return
        preDraw?.removeListener()
        preDraw = OneShotPreDrawListener.add(view) {
            preDraw = null
            view.post {
                if (!closed && !dismissed && generation == frameGeneration && contentView === view && view.isAttachedToWindow) {
                    ready = true
                    drainCommands()
                }
            }
        }
        view.invalidate()
    }

    override fun onFrameAccepted(generation: Long) {
        this.generation = generation
        clearReadiness()
    }

    override fun deferCommand(command: UiCommand, action: () -> Unit): Boolean {
        if (closed || dismissed) throw ComposeUiContractException(ComposeUiErrorCodes.SESSION_CLOSED)
        val needsWindowFocus = command is UiCommand.Focus
        if (ready && pending.isEmpty() && (!needsWindowFocus || contentView?.hasWindowFocus() == true)) return false
        if (pending.size >= ComposeUiLimits.MAX_PATCHES_PER_BATCH) {
            throw ComposeUiContractException(ComposeUiErrorCodes.LIMIT_EXCEEDED)
        }
        pending += PendingCommand(needsWindowFocus, action)
        drainCommands()
        return true
    }

    private fun drainCommands() {
        val view = contentView ?: return
        if (!view.isAttachedToWindow) return
        while (pending.isNotEmpty() && !closed && !dismissed && ready && contentView === view) {
            // API < 30's SoftwareKeyboardControllerCompat drops show() without window focus.
            // Do not issue the Focus command until the real modal can receive that request.
            // Other commands need no focus themselves, but never overtake an earlier command.
            if (pending.first.needsWindowFocus && !view.hasWindowFocus()) {
                awaitWindowFocus(view)
                return
            }
            clearFocusListener()
            try { pending.removeFirst().action.invoke() }
            catch (failure: Exception) { onFailure(failure) }
            catch (failure: LinkageError) { onFailure(failure) }
        }
    }

    private fun awaitWindowFocus(view: View) {
        if (focusListener != null) return
        val expectedGeneration = generation
        val serial = ++focusWaitSerial
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if (focused) view.post {
                if (!closed && !dismissed && generation == expectedGeneration && focusWaitSerial == serial &&
                    contentView === view && ready && view.isAttachedToWindow) {
                    clearFocusListener()
                    drainCommands()
                }
            }
        }
        focusObserver = view.viewTreeObserver.also { it.addOnWindowFocusChangeListener(listener) }
        focusListener = listener
        // Cover focus arriving between the readiness check and listener installation.
        if (view.hasWindowFocus()) listener.onWindowFocusChanged(true)
    }

    private fun clearFocusListener() {
        focusWaitSerial++
        val observer = focusObserver
        val listener = focusListener
        if (observer?.isAlive == true && listener != null) observer.removeOnWindowFocusChangeListener(listener)
        focusObserver = null; focusListener = null
    }

    private fun clearReadiness() {
        ready = false
        preDraw?.removeListener(); preDraw = null
        clearFocusListener()
    }

    private fun requestDismiss() {
        if (closed || dismissed || !options.cancelable) return
        dismissed = true
        clearCommands()
        onDismiss(Bundle())
    }

    override fun handleFailure(failure: Throwable): Boolean {
        if (closed || dismissed) return true
        // This hook handles posted initial composition and unhandled renderer/recomposer
        // coroutines. Synchronous apply failures and guarded native-node errors stay recoverable.
        // A failed Dialog.show cannot leave a transparent anchor keeping the script alive.
        dismissed = true
        clearCommands()
        val contract = failure as? ComposeUiContractException
        val denied = options.windowType != WindowManager.LayoutParams.TYPE_APPLICATION &&
            (failure is SecurityException || failure is WindowManager.BadTokenException &&
                !runCatching(hasOverlayPermission).getOrDefault(false))
        val code = if (denied) ComposeUiErrorCodes.PERMISSION_REQUIRED
            else contract?.code ?: ComposeUiErrorCodes.RENDER_FAILED
        onDismiss(Bundle().apply {
            putString(ComposeUiEventFields.CODE, code)
            putString(ComposeUiEventFields.MESSAGE, code)
            contract?.prop?.let { putString(ComposeUiEventFields.PROP, it) }
        })
        return true
    }

    override fun dispose() { closed = true; clearCommands() }

    private fun clearCommands() {
        pending.clear()
        clearReadiness()
        contentView = null
    }

    internal companion object {
        const val ALERT_TAG = "compose-dialog-alert"
        const val SHEET_TAG = "compose-dialog-sheet"
        const val SCRIM_TAG = "compose-dialog-scrim"
    }
}
