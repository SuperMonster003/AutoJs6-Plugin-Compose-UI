package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiEventFields
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.ComposeUiProps
import org.autojs.plugin.compose.api.model.UiCommand

/** Detached editor values. Deliberately omit text from diagnostics. Offsets are UTF-16. */
internal data class EditorSnapshot(
    val text: String,
    val start: Int,
    val end: Int,
    val composingStart: Int? = null,
    val composingEnd: Int? = null,
) {
    override fun toString() = "EditorSnapshot(length=${text.length}, selection=$start..$end)"
}

/** Pure edit arbitration. Composition-only changes also invalidate a script's older edit. */
internal class TextFieldRevision(private val nodeId: Int, initial: EditorSnapshot) {
    var value = initial
        private set
    var sequence = 0L
        private set

    init { validate(initial) }

    fun observe(next: EditorSnapshot): Boolean {
        validate(next)
        if (next == value) return false
        if (sequence == Long.MAX_VALUE) fail(ComposeUiErrorCodes.LIMIT_EXCEEDED, ComposeUiEventFields.EDIT_SEQ)
        value = next
        sequence++
        return true
    }

    /** Resolve every argument before touching the native buffer, preserving failed edits atomically. */
    fun resolve(command: UiCommand.Edit): EditorSnapshot {
        if (command.editSeq != sequence) fail(ComposeUiErrorCodes.INVALID_ARGUMENT, ComposeUiEventFields.EDIT_SEQ)
        val text = command.text ?: value.text
        val start = command.selection?.start ?: if (command.text != null && text != value.text) text.length else value.start
        val end = command.selection?.end ?: if (command.text != null && text != value.text) text.length else value.end
        return EditorSnapshot(text, start, end).also(::validate)
    }

    private fun validate(snapshot: EditorSnapshot) {
        if (snapshot.text.length > ComposeUiLimits.MAX_STRING_CHARS) fail(ComposeUiErrorCodes.LIMIT_EXCEEDED, ComposeUiProps.TEXT)
        if (snapshot.start !in 0..snapshot.text.length || snapshot.end !in 0..snapshot.text.length) {
            fail(ComposeUiErrorCodes.INVALID_ARGUMENT, "selection")
        }
    }

    private fun fail(code: String, prop: String): Nothing = throw ComposeUiContractException(code, nodeId = nodeId, prop = prop)
}

/** Keep only one pending notification per editor until the next display frame. */
internal class EditorFrameChanges {
    private val pending = LinkedHashSet<Int>()
    fun mark(nodeId: Int) { pending += nodeId }
    fun remove(nodeId: Int) { pending.remove(nodeId) }
    fun retain(nodeIds: Set<Int>) { pending.retainAll(nodeIds) }
    fun drain(): List<Int> = pending.toList().also { pending.clear() }
    fun clear() = pending.clear()
    val isEmpty get() = pending.isEmpty()
}
