package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import androidx.compose.runtime.Composable
import org.autojs.plugin.compose.api.model.UiCommand

/** Private presentation seam: base/F2 loading never resolves the optional dialog contract. */
internal interface RendererPresentation {
    @Composable fun Content(generation: Long, content: @Composable () -> Unit)
    fun onFrameAccepted(generation: Long)
    fun deferCommand(command: UiCommand, action: () -> Unit): Boolean
    /** Initial presentation and unhandled recomposer failures require lifecycle cleanup. */
    fun handleFailure(failure: Throwable): Boolean
    fun dispose()
}
