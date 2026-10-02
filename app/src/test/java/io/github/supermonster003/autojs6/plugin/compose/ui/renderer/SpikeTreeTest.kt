package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.spike.ComposeUiContractException
import org.autojs.plugin.compose.api.spike.SpikeContract
import org.autojs.plugin.compose.api.spike.UiNode
import org.junit.Assert.*
import org.junit.Test

class SpikeTreeTest {
    private fun text(value: String = "count") = UiNode(SpikeContract.TEXT, text = value)
    private fun invalid(root: UiNode) {
        try { SpikeTree.snapshot(root); fail("Invalid tree accepted") }
        catch (failure: ComposeUiContractException) { assertEquals(SpikeContract.INVALID_TREE, failure.code) }
    }

    @Test fun `snapshot detaches mutable children before the host changes its tree`() {
        val children = mutableListOf(text())
        val snapshot = SpikeTree.snapshot(UiNode(SpikeContract.COLUMN, children = children))
        children.clear()
        assertEquals(1, snapshot.children.size)
    }

    @Test fun `reject unknown components and children in leaf components`() {
        invalid(UiNode("Unknown"))
        invalid(text().copy(children = listOf(text())))
        invalid(UiNode(SpikeContract.BUTTON, callbackId = -1))
    }

    @Test fun `enforce node count at the boundary`() {
        val valid = UiNode(SpikeContract.COLUMN, children = List(SpikeContract.MAX_NODES - 1) { text() })
        assertEquals(SpikeContract.MAX_NODES - 1, SpikeTree.snapshot(valid).children.size)
        invalid(valid.copy(children = valid.children + text()))
    }

    @Test fun `enforce depth before recursing further`() {
        var root = text()
        repeat(SpikeContract.MAX_DEPTH - 1) { root = UiNode(SpikeContract.COLUMN, children = listOf(root)) }
        SpikeTree.snapshot(root)
        invalid(UiNode(SpikeContract.COLUMN, children = listOf(root)))
    }

    @Test fun `enforce text length before publishing state`() {
        SpikeTree.snapshot(text("a".repeat(SpikeContract.MAX_TEXT)))
        invalid(text("a".repeat(SpikeContract.MAX_TEXT + 1)))
    }
}
