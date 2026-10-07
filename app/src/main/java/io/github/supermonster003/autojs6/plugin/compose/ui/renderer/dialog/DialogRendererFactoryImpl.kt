package io.github.supermonster003.autojs6.plugin.compose.ui.renderer.dialog

import android.provider.Settings
import io.github.supermonster003.autojs6.plugin.compose.ui.renderer.interop.AndroidViewRenderer
import org.autojs.plugin.compose.api.dialog.DialogInteropV1
import org.autojs.plugin.compose.api.dialog.DialogOptionsV1
import org.autojs.plugin.compose.api.dialog.DialogRendererFactoryV1
import org.autojs.plugin.compose.api.interop.AndroidViewRendererV1
import org.autojs.plugin.compose.api.loading.ComposeUiHostEnvironment

/** Optional entry; V1/F2 entry points contain only its advertised name string. */
class DialogRendererFactoryImpl : DialogRendererFactoryV1 {
    override fun extensionVersion() = DialogInteropV1.VERSION

    override fun create(environment: ComposeUiHostEnvironment, options: DialogOptionsV1): AndroidViewRendererV1 {
        lateinit var renderer: AndroidViewRenderer
        val presentation = DialogPresentation(options,
            onDismiss = { renderer.enqueueSystemEvent(DialogInteropV1.DISMISS_REQUEST, it) },
            hasOverlayPermission = { Settings.canDrawOverlays(environment.hostContext) },
            onFailure = { renderer.reportFailure(it) })
        renderer = AndroidViewRenderer(environment, presentation)
        // Returning the actual interop renderer preserves the process-wide AndroidView claim owner.
        return renderer
    }
}
