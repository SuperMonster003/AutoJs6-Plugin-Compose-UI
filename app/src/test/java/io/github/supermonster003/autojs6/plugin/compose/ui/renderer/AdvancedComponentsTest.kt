package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import androidx.compose.ui.unit.LayoutDirection
import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue
import org.junit.Assert.assertEquals
import org.junit.Test

class AdvancedComponentsTest {
    @Test fun lazyPaddingPreservesLogicalEdgesAcrossRtlAndEveryCatalogOverload() {
        fun edges(value: UiValue?, direction: LayoutDirection): List<Float> {
            val node = UiNode(1, C.LAZY_COLUMN, props = value?.let { mapOf(P.CONTENT_PADDING to it) } ?: emptyMap())
            ComponentCatalog.V1.validateNode(node)
            val padding = lazyContentPadding(node)
            return listOf(padding.calculateLeftPadding(direction).value, padding.calculateTopPadding().value,
                padding.calculateRightPadding(direction).value, padding.calculateBottomPadding().value)
        }
        fun list(vararg values: Int) = UiValue.ListOf(values.map { UiValue.Dp(it.toDouble()) })
        assertEquals(listOf(0f, 0f, 0f, 0f), edges(null, LayoutDirection.Ltr))
        assertEquals(listOf(3f, 3f, 3f, 3f), edges(UiValue.Dp(3.0), LayoutDirection.Rtl))
        assertEquals(listOf(5f, 5f, 5f, 5f), edges(list(5), LayoutDirection.Ltr))
        assertEquals(listOf(2f, 7f, 2f, 7f), edges(list(2, 7), LayoutDirection.Rtl))
        assertEquals(listOf(1f, 2f, 3f, 4f), edges(list(1, 2, 3, 4), LayoutDirection.Ltr))
        assertEquals(listOf(3f, 2f, 1f, 4f), edges(list(1, 2, 3, 4), LayoutDirection.Rtl))
    }
}
