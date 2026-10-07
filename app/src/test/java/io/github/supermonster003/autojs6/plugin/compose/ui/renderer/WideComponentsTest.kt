package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.ComposeUiSlots as S
import org.autojs.plugin.compose.api.model.*
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Components as V
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Props as VP
import org.junit.Assert.*
import org.junit.Test

/** Renderer transaction guards supplement the shared catalog's scalar and transport tests. */
class WideComponentsTest {
    private fun store() = NodeStore(19, RendererCatalog::validate, catalog = ComposeUiV2.catalog)
    private fun batch(generation: Long, vararg changes: UiPatch) = UiPatchBatch(19, generation, changes.toList())
    private fun text(id: Int) = UiNode(id, C.TEXT, props = mapOf(P.TEXT to UiValue.Str("item")))

    @Test fun invalidSingleSelectionCannotPublishAnyPartOfTheBatch() {
        val store = store()
        val tree = UiTree(1, listOf(UiNode(1, V.SEGMENTED_BUTTON, children = listOf(2, 3)),
            UiNode(2, V.SEGMENTED_BUTTON_ITEM, props = mapOf(VP.SELECTED to UiValue.Bool(true)), slots = mapOf(S.LABEL to 4)),
            UiNode(3, V.SEGMENTED_BUTTON_ITEM, slots = mapOf(S.LABEL to 5)), text(4), text(5)))
        store.apply(batch(1, UiPatch.SetRoot(tree)))
        reject(ComposeUiErrorCodes.INVALID_ARGUMENT) {
            store.apply(batch(2, UiPatch.SetProps(4, mapOf(P.TEXT to UiValue.Str("unaccepted"))),
                UiPatch.SetProps(3, mapOf(VP.SELECTED to UiValue.Bool(true)))))
        }
        assertEquals(tree.nodes, store.tree!!.nodes)
        assertEquals(1L, store.generation)
        store.apply(batch(2, UiPatch.SetProps(2, mapOf(VP.SELECTED to UiValue.Bool(false))),
            UiPatch.SetProps(3, mapOf(VP.SELECTED to UiValue.Bool(true)))))
        assertEquals(UiValue.Bool(true), store.tree!!.nodes.single { it.nodeId == 3 }.props[VP.SELECTED])
    }

    @Test fun movingScopedItemsOrShrinkingSelectedPageIsRejectedBeforeCommit() {
        val store = store()
        val tree = UiTree(1, listOf(UiNode(1, C.COLUMN, children = listOf(2, 6)),
            UiNode(2, V.NAVIGATION_BAR, children = listOf(3)),
            UiNode(3, V.NAVIGATION_BAR_ITEM, slots = mapOf(S.ICON to 4)), text(4),
            UiNode(6, V.HORIZONTAL_PAGER, props = mapOf(VP.PAGE to UiValue.Num(1.0)), children = listOf(7, 8)), text(7), text(8)))
        store.apply(batch(1, UiPatch.SetRoot(tree)))
        reject(ComposeUiErrorCodes.INVALID_ARGUMENT) { store.apply(batch(2, UiPatch.Remove(6, 8))) }
        reject(ComposeUiErrorCodes.INVALID_ARGUMENT) { store.apply(batch(2, UiPatch.Move(1, 3, 2))) }
        assertEquals(1L, store.generation)
        assertEquals(tree.nodes, store.tree!!.nodes)
        store.apply(batch(2, UiPatch.SetProps(6, mapOf(VP.PAGE to UiValue.Num(0.0))), UiPatch.Remove(6, 8)))
        assertEquals(listOf(7), store.tree!!.nodes.single { it.nodeId == 6 }.children)
    }

    @Test fun optionalInteropAndAllV1EntriesRemainInTheV2Catalog() {
        val legacy = org.autojs.plugin.compose.api.catalog.ComponentCatalog.V1.components
        legacy.forEach { assertSame(it, ComposeUiV2.catalog.component(it.name)) }
        assertEquals(25, ComposeUiV2.componentNames.size)
        assertEquals(ComposeUiV2.catalog.components.size + 1, ComposeUiV2.interopCatalog.components.size)
        assertNotNull(ComposeUiV2.interopCatalog.component("AndroidView"))
        ComposeUiV2.componentNames.forEach { assertNotNull(RendererCatalog.dispatch[it]) }
    }

    private fun reject(code: String, action: () -> Unit) {
        try { action(); fail("Expected $code") }
        catch (failure: ComposeUiContractException) { assertEquals(code, failure.code) }
    }
}
