package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes as Error
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.ComposeUiSlots as S
import org.autojs.plugin.compose.api.model.UiCommand
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiTree
import org.junit.Assert.*
import org.junit.Test

class SnackbarQueueTest {
    @Test fun targetSelectionUsesTopologyInsteadOfStorageOrSlotMapOrder() {
        val outer = UiTree(1, listOf(UiNode(3, C.SCAFFOLD), UiNode(2, C.SCAFFOLD, children = listOf(3)),
            UiNode(1, C.COLUMN, children = listOf(2))))
        assertEquals(SnackbarTarget(2, null), snackbarTarget(outer))
        val siblings = UiTree(1, listOf(UiNode(1, C.COLUMN, children = listOf(3, 2)), UiNode(2, C.SCAFFOLD), UiNode(3, C.SCAFFOLD)))
        assertEquals(SnackbarTarget(3, null), snackbarTarget(siblings))
        val slots = linkedMapOf(S.TITLE to 2, S.ACTIONS to 3)
        for (map in listOf(slots, slots.entries.reversed().associate { it.toPair() })) {
            val slotted = UiTree(1, listOf(UiNode(1, C.TOP_APP_BAR, slots = map), UiNode(2, C.SCAFFOLD), UiNode(3, C.SCAFFOLD)))
            assertEquals(SnackbarTarget(3, null), snackbarTarget(slotted))
        }
        val custom = UiTree(1, listOf(UiNode(1, C.SCAFFOLD, slots = mapOf(S.SNACKBAR_HOST to 2)), UiNode(2, C.SCAFFOLD)))
        assertEquals(SnackbarTarget(1, 2), snackbarTarget(custom))
        assertNull(snackbarTarget(null))
        assertNull(snackbarTarget(UiTree(1, listOf(UiNode(1, C.TEXT)))))
    }

    @Test fun targetValidationIsSynchronousAndCannotSilentlyUseANestedCustomHost() {
        val queue = SnackbarQueue()
        failure(Error.INVALID_ARGUMENT) { queue.enqueue(UiCommand.ShowSnackbar("missing")) }
        queue.acceptFrame(SnackbarTarget(4, 5), 2)
        failure(Error.INVALID_ARGUMENT, 4) { queue.enqueue(UiCommand.ShowSnackbar("custom")) }
        assertNull(queue.first())
        queue.acceptFrame(SnackbarTarget(4, null), 3)
        val request = queue.enqueue(UiCommand.ShowSnackbar("visible", callbackId = 8))
        assertSame(request, queue.first())
        assertEquals(3L, request.generation)
    }

    @Test fun unrelatedFramesKeepFifoAndCapturedGenerationWithExactlyOneTerminalResult() {
        val queue = SnackbarQueue()
        val target = SnackbarTarget(1, null)
        queue.acceptFrame(target, 7)
        val first = queue.enqueue(UiCommand.ShowSnackbar("first", callbackId = 21))
        assertTrue(queue.acceptFrame(target, 8).isEmpty())
        val second = queue.enqueue(UiCommand.ShowSnackbar("second", callbackId = 22))
        assertEquals(7L, first.generation); assertEquals(8L, second.generation)
        val completion = queue.complete(first, true)!!
        assertTrue(completion.action); assertSame(first, completion.request)
        assertNull(queue.complete(first, false))
        assertSame(second, queue.first())
        assertFalse(queue.complete(second, false)!!.action)
        assertNull(queue.first())
    }

    @Test fun hostRemovalDismissesActiveAndPendingOnceButClosingNeverEmitsCallbacks() {
        val queue = SnackbarQueue()
        queue.acceptFrame(SnackbarTarget(1, null), 5)
        val first = queue.enqueue(UiCommand.ShowSnackbar("first"))
        val second = queue.enqueue(UiCommand.ShowSnackbar("second"))
        val removed = queue.acceptFrame(SnackbarTarget(2, null), 6)
        assertEquals(listOf(first, second), removed.map { it.request })
        assertTrue(removed.all { !it.action })
        assertNull(queue.complete(first, true)); assertNull(queue.complete(second, false))
        val next = queue.enqueue(UiCommand.ShowSnackbar("replacement"))
        queue.close(); queue.close()
        assertNull(queue.complete(next, false)); assertNull(queue.first()); assertNull(queue.target)
        failure(Error.SESSION_CLOSED) { queue.enqueue(UiCommand.ShowSnackbar("closed")) }
    }

    @Test fun pendingQueueUsesSharedEventBudgetAndFreesCapacityOnlyAfterCompletion() {
        val queue = SnackbarQueue()
        queue.acceptFrame(SnackbarTarget(1, null), 0)
        repeat(ComposeUiLimits.EVENT_QUEUE_CAPACITY) { queue.enqueue(UiCommand.ShowSnackbar("item")) }
        failure(Error.LIMIT_EXCEEDED) { queue.enqueue(UiCommand.ShowSnackbar("overflow")) }
        queue.complete(queue.first()!!, false)
        queue.enqueue(UiCommand.ShowSnackbar("replacement"))
        failure(Error.LIMIT_EXCEEDED) { queue.validate() }
    }

    @Test fun workerFailureDismissesEveryOutstandingRequestAndLeavesTheHostReusable() {
        val queue = SnackbarQueue()
        val target = SnackbarTarget(1, null)
        queue.acceptFrame(target, 3)
        val first = queue.enqueue(UiCommand.ShowSnackbar("first", callbackId = 6))
        val second = queue.enqueue(UiCommand.ShowSnackbar("second", callbackId = 7))
        assertEquals(listOf(first, second), queue.dismissAll().map { it.request })
        assertNull(queue.complete(first, true)); assertNull(queue.complete(second, false))
        assertTrue(queue.dismissAll().isEmpty()); assertEquals(target, queue.target)
        assertNotNull(queue.enqueue(UiCommand.ShowSnackbar("retry")))
    }

    private fun failure(code: String, nodeId: Int? = null, action: () -> Unit) {
        try { action(); fail("Expected $code") }
        catch (failure: ComposeUiContractException) {
            assertEquals(code, failure.code)
            assertEquals(nodeId, failure.nodeId)
        }
    }
}
