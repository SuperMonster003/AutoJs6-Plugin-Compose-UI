package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.model.*
import org.junit.Assert.*
import org.junit.Test

class NodeStoreTest {
    private fun text(id: Int, value: String = "", key: String? = null) = UiNode(id, ComposeUiComponents.TEXT, key, mapOf(ComposeUiProps.TEXT to UiValue.Str(value)))
    private fun batch(generation: Long, vararg patches: UiPatch) = UiPatchBatch(1, generation, patches.toList())
    private fun initial() = UiTree(1, listOf(UiNode(1, ComposeUiComponents.COLUMN, children = listOf(2, 3)), text(2, "a"), text(3, "b")))

    @Test fun firstGenerationZeroIsAcceptedButCannotBeReplayed() {
        val store = NodeStore(1)
        val first = store.apply(batch(0, UiPatch.SetRoot(initial())))
        assertEquals(0L, store.generation)
        try { store.apply(batch(0, UiPatch.SetProps(2, emptyMap()))); fail() }
        catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.INVALID_ARGUMENT, e.code) }
        assertSame(first, store.tree)
    }

    @Test fun emptyInitialTransactionKeepsTheViewEmptyAndAdvancesGeneration() {
        val store = NodeStore(1)
        assertNull(store.apply(batch(0)))
        assertNull(store.tree)
        assertEquals(0L, store.generation)
        assertNotNull(store.apply(batch(1, UiPatch.SetRoot(initial()))))
    }

    @Test fun insertRemoveMoveAndFullPropsReplacementProduceClosedTrees() {
        val store = NodeStore(1)
        store.apply(batch(1, UiPatch.SetRoot(initial())))
        val tree = store.apply(batch(2, UiPatch.Move(1, 3, 0), UiPatch.Insert(1, 1, UiTree(4, listOf(text(4)))), UiPatch.Remove(1, 2), UiPatch.SetProps(3, emptyMap())))
        assertEquals(listOf(3, 4), tree!!.nodes.single { it.nodeId == 1 }.children)
        assertEquals(setOf(1, 3, 4), tree!!.nodes.map { it.nodeId }.toSet())
        assertTrue(tree!!.nodes.single { it.nodeId == 3 }.props.isEmpty())
    }
    @Test fun slotReplacementDeletesTheOldSubtreeAndNullClearsTheSlot() {
        val store = NodeStore(1)
        store.apply(batch(1, UiPatch.SetRoot(UiTree(1, listOf(UiNode(1, ComposeUiComponents.BUTTON, slots = mapOf(ComposeUiSlots.CONTENT to 2)), text(2))))))
        val result = store.apply(batch(2, UiPatch.ReplaceSlot(1, ComposeUiSlots.CONTENT, UiTree(3, listOf(text(3))))))
        assertEquals(setOf(1, 3), result!!.nodes.map { it.nodeId }.toSet())
        assertEquals(1, store.apply(batch(3, UiPatch.ReplaceSlot(1, ComposeUiSlots.CONTENT, null)))!!.nodes.size)
    }
    @Test fun failedLatePatchDoesNotPublishEarlierMutationsOrAdvanceGeneration() {
        val store = NodeStore(1)
        val before = store.apply(batch(1, UiPatch.SetRoot(initial())))
        try { store.apply(batch(2, UiPatch.SetProps(2, emptyMap()), UiPatch.Remove(1, 99))); fail() }
        catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.INVALID_ARGUMENT, e.code) }
        assertSame(before, store.tree); assertEquals(1L, store.generation)
        assertEquals(2L, store.apply(batch(2, UiPatch.SetProps(2, emptyMap()))).let { store.generation })
    }
    @Test fun cyclesDuplicateIdsUnknownPropsAndWrongSessionAreRejectedAtomically() {
        val store = NodeStore(1)
        val before = store.apply(batch(1, UiPatch.SetRoot(UiTree(1, listOf(UiNode(1, ComposeUiComponents.COLUMN, children = listOf(2)), UiNode(2, ComposeUiComponents.COLUMN, children = listOf(3)), text(3))))))
        val bad = listOf(batch(2, UiPatch.Move(2, 2, 0), UiPatch.Remove(2, 2)), batch(2, UiPatch.Insert(1, 0, UiTree(3, listOf(text(3))))),
            batch(2, UiPatch.SetProps(3, mapOf("unknown" to UiValue.Bool(true)))), UiPatchBatch(99, 2, listOf(UiPatch.SetRoot(initial()))), batch(1, UiPatch.SetRoot(initial())))
        bad.forEach { transaction ->
            try { store.apply(transaction); fail() } catch (_: ComposeUiContractException) { }
            assertSame(before, store.tree); assertEquals(1L, store.generation)
        }
    }
    @Test fun crossParentMoveHonorsPostRemovalIndexAndCatalogScopes() {
        val store = NodeStore(1)
        store.apply(batch(1, UiPatch.SetRoot(UiTree(1, listOf(UiNode(1, ComposeUiComponents.COLUMN, children = listOf(2, 3)),
            UiNode(2, ComposeUiComponents.COLUMN, children = listOf(4)), UiNode(3, ComposeUiComponents.COLUMN), text(4))))))
        val moved = store.apply(batch(2, UiPatch.Move(3, 4, 0)))
        assertTrue(moved!!.nodes.single { it.nodeId == 2 }.children.isEmpty())
        assertEquals(listOf(4), moved!!.nodes.single { it.nodeId == 3 }.children)
        try { store.apply(batch(3, UiPatch.SetProps(4, emptyMap(), listOf(ModifierOp(ComposeUiModifiers.ALIGN, listOf(UiValue.Enum("center")), ScopeKind.BOX))))); fail() }
        catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.SCOPE_MISMATCH, e.code) }
        assertSame(moved, store.tree)
    }
    @Test fun deepIntermediateMovesCanBeRemovedWithoutRecursingBeforeFinalValidation() {
        val store = NodeStore(1)
        val children = (2..2001).toList()
        store.apply(batch(1, UiPatch.SetRoot(UiTree(1, listOf(UiNode(1, ComposeUiComponents.COLUMN, children = children)) +
            children.map { UiNode(it, ComposeUiComponents.COLUMN) }))))
        val operations = (3..2001).map { UiPatch.Move(it - 1, it, 0) } + UiPatch.Remove(1, 2)
        val result = store.apply(UiPatchBatch(1, 2, operations))
        assertEquals(1, result!!.nodes.size)
        assertTrue(result!!.nodes.single().children.isEmpty())
    }
}
