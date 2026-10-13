package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.DocumentWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Segment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LiveTree
import org.autojs.plugin.compose.api.ComposeUiContract
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.v2.ComposeUiV2

/**
 * The designer's side of the sandbox (roadmap P9.3 step 2): runs the document's script while the interactive
 * preview is on and holds what the views show, all on the main thread: the sandbox's current tree, the console and
 * the failure that stopped it. The script is the document as it would be saved, with the preview values of the
 * state panel as the states' initial values; nothing here changes the document.
 */
internal class SandboxPreview(
    private val catalog: ComponentCatalog = ComposeUiV2.interopCatalog,
    private val limits: SandboxLimits = SandboxLimits(),
) {
    /** The sandbox's tree; null before the script mounted one, or when no sandbox runs. */
    var tree: LiveTree? by mutableStateOf(null)
        private set
    val console = mutableStateListOf<ConsoleLine>()
    /** The last failure: a script error, or the limit that stopped the sandbox ([SandboxFailure.stops]). */
    var failure: SandboxFailure? by mutableStateOf(null)
        private set
    var running: Boolean by mutableStateOf(false)
        private set

    private var sandbox: PreviewSandbox? = null
    private val main = Handler(Looper.getMainLooper())
    private val catalogJson by lazy {
        SandboxCatalog.json(catalog, mapOf("plugin" to BuildConfig.VERSION_NAME, "contract" to ComposeUiContract.CONTRACT_VERSION, "compose" to BuildConfig.COMPOSE_VERSION))
    }

    /** Starts the script of [document] afresh, replacing a running one. */
    fun start(document: DesignDocument, values: Map<String, JsLiteral>, name: String) {
        stop()
        console.clear()
        failure = null
        running = true
        lateinit var created: PreviewSandbox
        created = PreviewSandbox(catalogJson, limits) { output -> main.post { if (sandbox === created) apply(output) } }
        sandbox = created
        created.start(DocumentWriter.write(withInitials(document, values)), name)
    }

    fun stop() {
        sandbox?.close()
        sandbox = null
        running = false
        tree = null
    }

    /**
     * An event of the canvas: [callbackId] is the node's own id for a listener event, or the token of a modifier
     * callback (`clickable`); the payload becomes the handler's arguments as in the host.
     */
    fun dispatch(nodeId: Int, event: String, callbackId: Int, payload: Map<String, Any?>) {
        val current = sandbox ?: return
        val type = tree?.document?.node(nodeId)?.type ?: return
        current.dispatch(if (callbackId >= CALLBACK_BASE) callbackId else nodeId, event, SandboxEvents.arguments(event, type, payload))
    }

    fun clearConsole() = console.clear()

    private fun apply(output: SandboxOutput) {
        output.snapshot?.let { tree = SandboxTrees.parse(it) }
        console.addAll(output.console)
        output.failure?.let {
            failure = it
            console += ConsoleLine(ERROR, if (it.line > 0) "${it.message} (line ${it.line})" else it.message)
            if (it.stops) running = false
        }
        if (console.size > MAX_LINES) console.removeRange(0, console.size - MAX_LINES)
    }

    companion object {
        /**
         * Rhino's Date is built on java.time, which Android has from 8.0 (API 26). Desugaring java.time would also
         * rewrite the Compose classes the renderer loads inside AutoJs6, so older devices keep the step 1 preview.
         */
        val SUPPORTED: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        /** Modifier callback tokens of the preview runtime start here, above node ids and placeholder ids. */
        const val CALLBACK_BASE = 2_000_000
        private const val MAX_LINES = 1_000
        const val ERROR = "error"

        /** [document] with the preview values of the state panel as the initial values of their states. */
        fun withInitials(document: DesignDocument, values: Map<String, JsLiteral>): DesignDocument {
            if (values.isEmpty()) return document
            return document.copy(segments = document.segments.map { segment ->
                val state = segment as? Segment.State
                val value = state?.let { values[it.decl.name] }
                if (state == null || value == null) segment else Segment.State(state.decl.copy(initial = PropValue.Literal(value)))
            })
        }
    }
}
