package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import android.view.Choreographer
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiEventFields as F
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.model.UiCommand
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiTree
import org.autojs.plugin.compose.api.model.UiValue

/**
 * Main-thread native editor ownership independent of composition and script response times.
 * Lazy items retain editor state while offscreen; only removal of the node releases it.
 */
internal class TextFieldController(
    private val emit: (generation: Long, nodeId: Int, type: String, callbackId: Int, payload: Bundle) -> Unit,
    private val reportError: (Throwable) -> Unit,
    private val scope: () -> CoroutineScope,
) {
    private class Binding(val node: UiNode, val generation: Long)
    private class Editor(val state: TextFieldState, val revision: TextFieldRevision) {
        var observer: Job? = null
        lateinit var input: InputTransformation
    }

    private val bindings = HashMap<Int, Binding>()
    private val editors = HashMap<Int, Editor>()
    private val changes = EditorFrameChanges()
    private val errors = LinkedHashMap<Int, ComposeUiContractException>()
    private var framePosted = false
    private var closed = false
    private val frameCallback = Choreographer.FrameCallback { flushFrame() }

    /** Called by NodeStore on the candidate tree, before publishing any part of a batch. */
    fun validateNode(node: UiNode) {
        if (!isTextField(node)) return
        val before = bindings[node.nodeId]?.node ?: return
        if (before.type == node.type && declaredText(before) != declaredText(node)) {
            throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = node.nodeId, prop = P.TEXT)
        }
    }

    /** Publish current callbacks and generation together; queued native edits follow this binding. */
    fun acceptFrame(tree: UiTree?, generation: Long) {
        if (closed) return
        val next = tree?.nodes?.filter(::isTextField)?.associateBy { it.nodeId }.orEmpty()
        for ((id, editor) in editors.toMap()) {
            if (next[id]?.type != bindings[id]?.node?.type) {
                editor.observer?.cancel()
                editors.remove(id)
                changes.remove(id)
                errors.remove(id)
            } else {
                synchronize(id, editor)
            }
        }
        bindings.clear()
        next.forEach { (id, node) -> bindings[id] = Binding(node, generation) }
        changes.retain(next.keys)
        errors.keys.retainAll(next.keys)
        if (changes.isEmpty && errors.isEmpty()) cancelFrame()
    }

    fun editor(nodeId: Int): TextFieldState = entry(nodeId).state

    fun inputTransformation(nodeId: Int): InputTransformation = entry(nodeId).input

    /** This method is synchronous, including reading native edits not yet observed by snapshotFlow. */
    fun execute(command: UiCommand.Edit) {
        val editor = entry(command.nodeId)
        synchronize(command.nodeId, editor)
        val target = editor.revision.resolve(command)
        val current = editor.revision.value
        if (current.text == target.text && current.start == target.start && current.end == target.end) return
        editor.state.edit {
            if (asCharSequence().toString() != target.text) replace(0, length, target.text)
            selection = TextRange(target.start, target.end)
        }
        synchronize(command.nodeId, editor)
    }

    fun dispose() {
        if (closed) return
        closed = true
        cancelFrame()
        editors.values.forEach { it.observer?.cancel() }
        editors.clear(); bindings.clear(); changes.clear(); errors.clear()
    }

    private fun entry(nodeId: Int): Editor {
        if (closed) throw ComposeUiContractException(ComposeUiErrorCodes.SESSION_CLOSED)
        val binding = bindings[nodeId] ?: throw ComposeUiContractException(ComposeUiErrorCodes.NODE_DETACHED, nodeId = nodeId)
        return editors.getOrPut(nodeId) {
            val initial = declaredText(binding.node)
            val state = TextFieldState(initial)
            val editor = Editor(state, TextFieldRevision(nodeId, snapshot(state)))
            editor.input = object : InputTransformation {
                override fun SemanticsPropertyReceiver.applySemantics() {
                    maxTextLength = ComposeUiLimits.MAX_STRING_CHARS
                    // Foundation installs these native actions before calling InputTransformation.
                    // Preserve caret/keyboard/toolbar behavior, then notify the current callback.
                    val config = this as? SemanticsConfiguration ?: return
                    config.getOrElseNullable(SemanticsActions.OnClick) { null }?.action?.let { native ->
                        onClick { commonAction(nodeId, editor, E.CLICK, native) }
                    }
                    config.getOrElseNullable(SemanticsActions.OnLongClick) { null }?.action?.let { native ->
                        onLongClick { commonAction(nodeId, editor, E.LONG_CLICK, native) }
                    }
                }
                override fun TextFieldBuffer.transformInput() {
                    if (closed || bindings[nodeId] == null || editors[nodeId] !== editor) { revertAllChanges(); return }
                    // Read-only/disabled nodes may still have an old InputConnection for one frame.
                    val node = bindings.getValue(nodeId).node
                    val disabled = !(node.props[P.ENABLED] as? UiValue.Bool).enabledByDefault()
                    val writesReadOnly = (node.props[P.READ_ONLY] as? UiValue.Bool)?.value == true && asCharSequence().toString() != state.text.toString()
                    if (disabled || writesReadOnly) {
                        revertAllChanges(); return
                    }
                    if (length > ComposeUiLimits.MAX_STRING_CHARS) {
                        revertAllChanges()
                        errors[nodeId] = ComposeUiContractException(ComposeUiErrorCodes.LIMIT_EXCEEDED, nodeId = nodeId, prop = P.TEXT)
                        postFrame()
                        return
                    }
                    // Capturing the proposed text/selection here also notices A -> B -> A edits
                    // that a conflating snapshotFlow can otherwise miss between two frames.
                    synchronize(nodeId, editor)
                    val prior = editor.revision.value
                    if (editor.revision.observe(EditorSnapshot(asCharSequence().toString(), selection.start, selection.end, prior.composingStart, prior.composingEnd))) {
                        changed(nodeId)
                    }
                }
            }
            // A lazy field may be created during composition; start observation after that
            // composition's snapshot is applied, rather than reading a not-yet-published state.
            editor.observer = scope().launch {
                try {
                    snapshotFlow { snapshot(state) }.collect {
                        // Do not use a possibly queued snapshot after execute() advanced the state.
                        synchronize(nodeId, editor)
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { reportError(failure) }
                catch (failure: LinkageError) { reportError(failure) }
            }
            editor
        }
    }

    private fun synchronize(nodeId: Int, editor: Editor) {
        if (editor.revision.observe(snapshot(editor.state))) changed(nodeId)
    }

    private fun commonAction(nodeId: Int, editor: Editor, type: String, native: () -> Boolean): Boolean {
        val binding = bindings[nodeId] ?: return false
        if (closed || editors[nodeId] !== editor || !(binding.node.props[P.ENABLED] as? UiValue.Bool).enabledByDefault()) return false
        val accepted = native()
        if (accepted) binding.node.callbacks[type]?.let { emit(binding.generation, nodeId, type, it, Bundle()) }
        return accepted
    }

    private fun changed(nodeId: Int) { changes.mark(nodeId); postFrame() }
    private fun postFrame() {
        if (!closed && !framePosted) {
            framePosted = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }
    private fun cancelFrame() {
        if (framePosted) Choreographer.getInstance().removeFrameCallback(frameCallback)
        framePosted = false
    }
    private fun flushFrame() {
        framePosted = false
        if (closed) return
        // Composition-only edits also need an up-to-date sequence before an event is delivered.
        val pending = changes.drain()
        for (id in pending) {
            val editor = editors[id] ?: continue
            val binding = bindings[id] ?: continue
            try {
                editor.revision.observe(snapshot(editor.state))
                val callback = binding.node.callbacks[E.VALUE_CHANGE] ?: continue
                val value = editor.revision.value
                emit(binding.generation, id, E.VALUE_CHANGE, callback, Bundle().apply {
                    putString(F.TEXT, value.text); putInt(F.SELECTION_START, value.start); putInt(F.SELECTION_END, value.end)
                    putLong(F.EDIT_SEQ, editor.revision.sequence)
                })
            } catch (failure: Exception) { reportError(failure) }
            catch (failure: LinkageError) { reportError(failure) }
        }
        val rejected = errors.values.toList()
        errors.clear()
        rejected.forEach(reportError)
    }

    private fun UiValue.Bool?.enabledByDefault() = this?.value ?: true
    private fun snapshot(state: TextFieldState): EditorSnapshot {
        val selection = state.selection
        val composition = state.composition
        return EditorSnapshot(state.text.toString(), selection.start, selection.end, composition?.start, composition?.end)
    }

    companion object {
        fun isTextField(node: UiNode) = node.type == C.TEXT_FIELD || node.type == C.OUTLINED_TEXT_FIELD
        private fun declaredText(node: UiNode) = (node.props[P.TEXT] as? UiValue.Str)?.value.orEmpty()
    }
}
