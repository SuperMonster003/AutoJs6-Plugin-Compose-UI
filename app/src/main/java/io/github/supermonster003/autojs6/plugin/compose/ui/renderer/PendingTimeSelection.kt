package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.ComposeUiLimits
import java.util.ArrayDeque

/** Preserve the other edited field while ordered timeChange events wait for accepted host frames. */
internal class PendingTimeSelection(hour: Int, minute: Int) {
    data class Value(val hour: Int, val minute: Int)
    private var accepted = Value(hour, minute)
    private val pending = ArrayDeque<Value>()

    fun accept(hour: Int, minute: Int) {
        val next = Value(hour, minute)
        if (next == accepted) return
        accepted = next
        if (next in pending) {
            while (pending.isNotEmpty() && pending.removeFirst() != next) { /* Drop acknowledged predecessors. */ }
        } else pending.clear() // A programmatic replacement supersedes any unacknowledged draft.
    }

    fun hour(value: Int): Value? = request((pending.peekLast() ?: accepted).copy(hour = value))
    fun minute(value: Int): Value? = request((pending.peekLast() ?: accepted).copy(minute = value))

    private fun request(value: Value): Value? {
        if (value == (pending.peekLast() ?: accepted)) return null
        // The host event queue is bounded too; a nonresponding script cannot retain unlimited drafts.
        if (pending.size >= ComposeUiLimits.EVENT_QUEUE_CAPACITY) pending.removeFirst()
        pending.addLast(value)
        return value
    }
}
