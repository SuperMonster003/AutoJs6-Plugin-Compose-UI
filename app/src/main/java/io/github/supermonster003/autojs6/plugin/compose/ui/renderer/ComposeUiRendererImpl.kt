package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.*
import io.github.supermonster003.autojs6.plugin.compose.ui.BuildConfig
import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.loading.ComposeUiHostEnvironment
import org.autojs.plugin.compose.api.loading.ComposeUiRenderer
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.model.*

/** V1 renderer: publish immutable validated frames and queue every event through the host sink. */
class ComposeUiRendererImpl(private val environment: ComposeUiHostEnvironment) : ComposeUiRenderer {
    private val fields = TextFieldController(::emit, ::error, ::ensureScope)
    private val snackbar = SnackbarController(::ensureScope, ::emit)
    private val store = NodeStore(environment.sessionId, { node ->
        RendererCatalog.validate(node)
        fields.validateNode(node)
    }, allowUnknownDebug = BuildConfig.DEBUG)
    private val frame = mutableStateOf<RenderFrame?>(null)
    private val theme = mutableStateOf(environment.initialTheme)
    private val commands = NodeCommandRegistry()
    private var view: ComposeView? = null
    private var container: GuardedComposeContainer? = null
    private var recomposer: Recomposer? = null
    private var scope: CoroutineScope? = null
    private var closed = false

    init { main() }
    override fun view(): View {
        usable()
        return container ?: GuardedComposeContainer(environment.hostContext, ::error).also { root ->
            val output = ComposeView(environment.hostContext)
            val owner = ensureScope()
            val composer = Recomposer(owner.coroutineContext).also { recomposer = it }
            owner.launch { composer.runRecomposeAndApplyChanges() }
            output.setParentCompositionContext(composer)
            output.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            output.setContent {
                ThemeMapper.Content(theme.value) {
                    Box(Modifier.semantics { testTagsAsResourceId = true }) {
                        frame.value?.let { RenderNode(it, it.rootId, commands, fields, snackbar, ::emit) }
                    }
                }
            }
            view = output
            root.mount(output)
            container = root
        }
    }
    override fun apply(batch: UiPatchBatch) {
        usable()
        val tree = store.apply(batch)
        fields.acceptFrame(tree, store.generation)
        snackbar.acceptFrame(tree, store.generation)
        frame.value = tree?.let { RenderFrame(it.rootId, it.nodes.associateBy { node -> node.nodeId }, store.generation) }
        container?.recover()
    }
    override fun execute(command: UiCommand) {
        usable()
        fun validateLiveNode(): UiNode? {
            val id = when (command) {
                is UiCommand.Focus -> command.nodeId
                is UiCommand.Blur -> command.nodeId
                is UiCommand.ScrollTo -> command.nodeId
                is UiCommand.Edit -> command.nodeId
                is UiCommand.ShowSnackbar -> null
            }
            val node = id?.let { target -> store.tree?.nodes?.firstOrNull { it.nodeId == target } }
            if (id != null && node == null) throw ComposeUiContractException(ComposeUiErrorCodes.NODE_DETACHED, nodeId = id)
            val name = when (command) {
                is UiCommand.Focus -> ComposeUiCommands.FOCUS
                is UiCommand.Blur -> ComposeUiCommands.BLUR
                is UiCommand.ScrollTo -> ComposeUiCommands.SCROLL_TO
                is UiCommand.Edit -> ComposeUiCommands.EDIT
                is UiCommand.ShowSnackbar -> null
            }
            if (node != null && name != null) {
                val catalog = ComponentCatalog.V1
                if (name !in catalog.requireComponent(node.type).commands && node.modifier.none {
                    name in catalog.modifier(it.name)!!.commands
                }) throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = id)
            }
            if (command is UiCommand.Edit && !TextFieldController.isTextField(node!!)) {
                throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = id)
            }
            if (command is UiCommand.ScrollTo && node?.type in setOf(ComposeUiComponents.LAZY_COLUMN, ComposeUiComponents.LAZY_ROW)) {
                val index = command.index
                if (index != null && index >= node!!.children.size) throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = id)
            }
            if (command is UiCommand.ScrollTo && command.index != null && node?.type !in setOf(ComposeUiComponents.LAZY_COLUMN, ComposeUiComponents.LAZY_ROW)) {
                throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = id)
            }
            return node
        }
        val acceptedType = validateLiveNode()?.type
        when (command) {
            is UiCommand.Edit -> { fields.execute(command); return }
            is UiCommand.ShowSnackbar -> { snackbar.execute(command); return }
            else -> Unit
        }
        val action = commands.resolve(command)
        ensureScope().launch { try {
            val current = validateLiveNode()
            if (current?.type != acceptedType) throw ComposeUiContractException(ComposeUiErrorCodes.NODE_DETACHED, nodeId = current?.nodeId)
            action()
        } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error(failure) } catch (failure: LinkageError) { error(failure) } }
    }
    override fun setTheme(theme: ThemeSpec) {
        usable()
        theme.fontFamily?.let(ValueMapper::fontFamily)
        this.theme.value = theme
        container?.recover()
    }
    override fun dispose() {
        main()
        if (closed) return
        closed = true
        try { view?.disposeComposition() } finally {
            fields.dispose(); snackbar.close(); commands.clear(); recomposer?.cancel(); scope?.cancel()
            container?.removeAllViews(); container = null
            view = null; recomposer = null; scope = null; frame.value = null; store.clear()
        }
    }
    private fun ensureScope(): CoroutineScope = scope ?: CoroutineScope(
        AndroidUiDispatcher.Main + SupervisorJob() + CoroutineExceptionHandler { _, failure -> error(failure) }
    ).also { scope = it }
    private fun main() { check(Looper.myLooper() == Looper.getMainLooper()) { "Renderer methods require the main thread" } }
    private fun usable() { main(); if (closed) throw ComposeUiContractException(ComposeUiErrorCodes.SESSION_CLOSED) }
    private fun emit(generation: Long, nodeId: Int, type: String, callbackId: Int, payload: Bundle) {
        if (!closed) environment.eventSink.enqueue(UiEvent(environment.sessionId, generation, nodeId, type, callbackId, payload))
    }
    private fun error(failure: Throwable) {
        val contract = failure as? ComposeUiContractException
        val code = contract?.code ?: ComposeUiErrorCodes.RENDER_FAILED
        emit(store.generation, contract?.nodeId ?: ComposeUiContract.NO_NODE_ID, ComposeUiEvents.ERROR, ComposeUiContract.SYSTEM_CALLBACK_ID,
            Bundle().apply {
                putString(ComposeUiEventFields.CODE, code); putString(ComposeUiEventFields.MESSAGE, code)
                contract?.prop?.let { putString(ComposeUiEventFields.PROP, it) }
            })
    }
}
