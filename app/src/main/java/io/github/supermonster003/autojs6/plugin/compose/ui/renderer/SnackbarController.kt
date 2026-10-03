package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiContract
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.ComposeUiSlots as S
import org.autojs.plugin.compose.api.model.SnackbarDuration
import org.autojs.plugin.compose.api.model.UiCommand
import org.autojs.plugin.compose.api.model.UiTree
import java.util.ArrayDeque
import androidx.compose.material3.SnackbarDuration as MaterialDuration

internal data class SnackbarTarget(val nodeId: Int, val customHostId: Int?)

/** Primary children precede slots; slot-name order is stable across Parcel/map implementations. */
internal fun snackbarTarget(tree: UiTree?): SnackbarTarget? {
    if (tree == null) return null
    val nodes = tree.nodes.associateBy { it.nodeId }
    val pending = ArrayDeque<Int>().apply { add(tree.rootId) }
    while (pending.isNotEmpty()) {
        val node = nodes.getValue(pending.removeLast())
        if (node.type == C.SCAFFOLD) return SnackbarTarget(node.nodeId, node.slots[S.SNACKBAR_HOST])
        val children = node.children + node.slots.toSortedMap().values
        for (index in children.indices.reversed()) pending.addLast(children[index])
    }
    return null
}

/** Pure bounded lifecycle state: one accepted request can produce at most one terminal event. */
internal class SnackbarQueue {
    class Request(val command: UiCommand.ShowSnackbar, val generation: Long)
    data class Completion(val request: Request, val action: Boolean)

    var target: SnackbarTarget? = null
        private set
    private var generation = 0L
    private var closed = false
    private val requests = ArrayDeque<Request>()

    fun acceptFrame(nextTarget: SnackbarTarget?, generation: Long): List<Completion> {
        if (closed) throw ComposeUiContractException(ComposeUiErrorCodes.SESSION_CLOSED)
        val removed = if (target == nextTarget) emptyList() else requests.map { Completion(it, false) }.also { requests.clear() }
        target = nextTarget
        this.generation = generation
        return removed
    }

    fun validate() {
        if (closed) throw ComposeUiContractException(ComposeUiErrorCodes.SESSION_CLOSED)
        val target = target ?: throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT)
        if (target.customHostId != null) throw ComposeUiContractException(
            ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = target.nodeId)
        // A request can enqueue one terminal event. Reuse the shared event budget rather than
        // introduce an undocumented limit or keep unbounded suspended Material coroutines.
        if (requests.size >= ComposeUiLimits.EVENT_QUEUE_CAPACITY) throw ComposeUiContractException(ComposeUiErrorCodes.LIMIT_EXCEEDED)
    }

    fun enqueue(command: UiCommand.ShowSnackbar): Request {
        validate()
        return Request(command, generation).also(requests::addLast)
    }

    fun first(): Request? = requests.peekFirst()

    fun complete(request: Request, action: Boolean): Completion? =
        if (requests.remove(request)) Completion(request, action) else null

    fun dismissAll(): List<Completion> = requests.map { Completion(it, false) }.also { requests.clear() }

    fun close() { closed = true; requests.clear(); target = null }
}

/** Main-thread session controller. Material's queue is fed by exactly one bounded worker. */
internal class SnackbarController(
    private val scope: () -> CoroutineScope,
    private val emit: (generation: Long, nodeId: Int, type: String, callbackId: Int, payload: Bundle) -> Unit,
) {
    private val queue = SnackbarQueue()
    private var state: SnackbarHostState? = null
    private var worker: Job? = null
    private var workerToken: Any? = null

    fun acceptFrame(tree: UiTree?, generation: Long) {
        val before = queue.target
        val removed = queue.acceptFrame(snackbarTarget(tree), generation)
        if (before != queue.target) {
            worker?.cancel(); worker = null; workerToken = null
            // A removed/replaced host never lends its visible SnackbarData to a new host.
            state = queue.target?.takeIf { it.customHostId == null }?.let { SnackbarHostState() }
        }
        removed.forEach(::publish)
    }

    fun hostState(nodeId: Int): SnackbarHostState? = if (queue.target?.nodeId == nodeId) state else null

    fun validate(@Suppress("UNUSED_PARAMETER") command: UiCommand.ShowSnackbar) = queue.validate()

    fun execute(command: UiCommand.ShowSnackbar) {
        queue.validate()
        val owner = scope()
        if (!owner.isActive) throw ComposeUiContractException(ComposeUiErrorCodes.SESSION_CLOSED)
        val host = state ?: throw ComposeUiContractException(ComposeUiErrorCodes.INTERNAL)
        queue.enqueue(command)
        if (worker?.isActive == true) return
        val token = Any().also { workerToken = it }
        val job = owner.launch(start = CoroutineStart.LAZY) {
            try {
                while (true) {
                    val request = queue.first() ?: break
                    val command = request.command
                    val result = host.showSnackbar(
                        message = command.message, actionLabel = command.actionLabel,
                        withDismissAction = command.duration == SnackbarDuration.INDEFINITE,
                        duration = when (command.duration) {
                            SnackbarDuration.SHORT -> MaterialDuration.Short
                            SnackbarDuration.LONG -> MaterialDuration.Long
                            SnackbarDuration.INDEFINITE -> MaterialDuration.Indefinite
                        },
                    )
                    queue.complete(request, result == SnackbarResult.ActionPerformed)?.let(::publish)
                }
            } catch (cancelled: CancellationException) {
                // Host replacement and close clear this token before canceling the old worker.
                if (workerToken === token) queue.dismissAll().forEach(::publish)
                throw cancelled
            } catch (failure: Exception) {
                if (workerToken === token) queue.dismissAll().forEach(::publish)
                throw failure
            } catch (failure: LinkageError) {
                if (workerToken === token) queue.dismissAll().forEach(::publish)
                throw failure
            } finally {
                if (workerToken === token) { worker = null; workerToken = null }
            }
        }
        worker = job
        job.start()
    }

    fun close() {
        queue.close()
        worker?.cancel(); worker = null; workerToken = null; state = null
    }

    private fun publish(completion: SnackbarQueue.Completion) {
        val request = completion.request
        request.command.callbackId?.let { callback ->
            emit(request.generation, ComposeUiContract.NO_NODE_ID, if (completion.action) E.ACTION else E.DISMISS, callback, Bundle())
        }
    }
}
