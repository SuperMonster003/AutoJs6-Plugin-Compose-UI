package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.loading.ComposeUiHostEnvironment
import org.autojs.plugin.compose.api.loading.ComposeUiRenderer
import org.autojs.plugin.compose.api.model.*

/** Minimal V1 provider for P1 host acceptance. Additional properties/components are P2 work. */
class ComposeUiRendererImpl(private val environment: ComposeUiHostEnvironment) : ComposeUiRenderer {
    private val store = NodeStore(environment.sessionId, ::validatePreviewNode)
    private data class Frame(val nodes: Map<Int, UiNode>, val root: Int, val generation: Long)
    private val frame = mutableStateOf<Frame?>(null)
    private val theme = mutableStateOf(environment.initialTheme)
    private var view: ComposeView? = null
    private var recomposer: Recomposer? = null
    private var scope: CoroutineScope? = null
    private var closed = false

    init { main() }
    override fun view(): View {
        usable()
        return view ?: ComposeView(environment.hostContext).also { output ->
            val handler = CoroutineExceptionHandler { _, _ -> error(ComposeUiErrorCodes.RENDER_FAILED) }
            val owner = CoroutineScope(AndroidUiDispatcher.Main + SupervisorJob() + handler).also { scope = it }
            val composer = Recomposer(owner.coroutineContext).also { recomposer = it }
            owner.launch { composer.runRecomposeAndApplyChanges() }
            output.setParentCompositionContext(composer)
            output.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            output.setContent {
                val spec = theme.value
                val base = if (spec.dark == true) darkColorScheme() else lightColorScheme()
                val colors = spec.seedArgb?.let { base.copy(primary = Color(it)) } ?: base
                MaterialTheme(colorScheme = colors) { frame.value?.let { Render(it, it.root) } }
            }
            view = output
        }
    }
    override fun apply(batch: UiPatchBatch) {
        usable()
        val tree = store.apply(batch)
        frame.value = Frame(tree.nodes.associateBy { it.nodeId }, tree.rootId, store.generation)
    }
    override fun execute(command: UiCommand) {
        usable()
        throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, "Command is not supported by this renderer preview")
    }
    override fun setTheme(theme: ThemeSpec) { usable(); this.theme.value = theme }
    override fun dispose() {
        main()
        if (closed) return
        closed = true
        try { view?.disposeComposition() } finally {
            recomposer?.cancel(); scope?.cancel(); view = null; recomposer = null; scope = null; frame.value = null; store.clear()
        }
    }
    private fun main() { check(Looper.myLooper() == Looper.getMainLooper()) { "Renderer methods require the main thread" } }
    private fun usable() { main(); if (closed) throw ComposeUiContractException(ComposeUiErrorCodes.SESSION_CLOSED) }
    private fun error(code: String) {
        if (!closed) environment.eventSink.enqueue(UiEvent(environment.sessionId, store.generation, ComposeUiContract.NO_NODE_ID,
            ComposeUiEvents.ERROR, ComposeUiContract.SYSTEM_CALLBACK_ID, Bundle().apply {
                putString(ComposeUiEventFields.CODE, code); putString(ComposeUiEventFields.MESSAGE, code)
            }))
    }
    @Composable private fun Render(current: Frame, id: Int) {
        val node = current.nodes.getValue(id)
        key(id) {
            val modifier = Modifier.previewNode(node)
            val children = node.slots[ComposeUiSlots.CONTENT]?.let(::listOf) ?: node.children
            when (node.type) {
                ComposeUiComponents.COLUMN -> Column(modifier) { children.forEach { Render(current, it) } }
                ComposeUiComponents.TEXT -> Text((node.props[ComposeUiProps.TEXT] as? UiValue.Str)?.value.orEmpty(), modifier)
                ComposeUiComponents.BUTTON -> Button(onClick = {
                    node.callbacks[ComposeUiEvents.CLICK]?.let { callback ->
                        if (!closed) environment.eventSink.enqueue(UiEvent(environment.sessionId, current.generation, id, ComposeUiEvents.CLICK, callback))
                    }
                }, modifier = modifier, enabled = (node.props[ComposeUiProps.ENABLED] as? UiValue.Bool)?.value ?: true) {
                    children.forEach { Render(current, it) }
                }
            }
        }
    }
    private fun Modifier.previewNode(node: UiNode): Modifier = node.modifier.fold(this) { result, op ->
        when (op.name) {
            ComposeUiModifiers.PADDING -> {
                val a = op.args.map { (it as UiValue.Dp).value.toFloat().dp }
                when (a.size) { 1 -> result.padding(a[0]); 2 -> result.padding(a[0], a[1]); else -> result.padding(a[0], a[1], a[2], a[3]) }
            }
            ComposeUiModifiers.FILL_MAX_WIDTH -> result.fillMaxWidth((op.args.firstOrNull() as? UiValue.Num)?.value?.toFloat() ?: 1f)
            ComposeUiModifiers.TEST_TAG -> result.testTag((op.args[0] as UiValue.Str).value)
            ComposeUiModifiers.SEMANTICS -> result.semantics { contentDescription = (op.args[0] as UiValue.Str).value }
            else -> result
        }
    }
    private fun validatePreviewNode(node: UiNode) {
        val allowed = when (node.type) {
            ComposeUiComponents.COLUMN -> emptySet()
            ComposeUiComponents.TEXT -> setOf(ComposeUiProps.TEXT)
            ComposeUiComponents.BUTTON -> setOf(ComposeUiProps.ENABLED)
            else -> throw ComposeUiContractException(ComposeUiErrorCodes.UNKNOWN_COMPONENT, nodeId = node.nodeId)
        }
        node.props.keys.firstOrNull { it !in allowed }?.let { throw ComposeUiContractException(ComposeUiErrorCodes.UNKNOWN_PROP, nodeId = node.nodeId, prop = it) }
        if (node.modifier.any { it.name !in setOf(ComposeUiModifiers.PADDING, ComposeUiModifiers.FILL_MAX_WIDTH, ComposeUiModifiers.TEST_TAG, ComposeUiModifiers.SEMANTICS) }) {
            throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_MODIFIER, nodeId = node.nodeId)
        }
        if (node.callbacks.keys.any { node.type != ComposeUiComponents.BUTTON || it != ComposeUiEvents.CLICK }) {
            throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = node.nodeId)
        }
    }
}
