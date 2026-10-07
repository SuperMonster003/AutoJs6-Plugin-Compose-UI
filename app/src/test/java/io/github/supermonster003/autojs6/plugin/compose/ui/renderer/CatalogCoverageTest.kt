package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import org.junit.Assert.*
import org.junit.Test

class CatalogCoverageTest {
    @Test fun implementedComponentsRetainV1AndCoverEveryV2Entry() {
        val expected = setOf(C.COLUMN, C.ROW, C.BOX, C.SPACER, C.SURFACE, C.CARD, C.HORIZONTAL_DIVIDER,
            C.TEXT, C.ICON, C.IMAGE, C.BUTTON, C.ELEVATED_BUTTON, C.FILLED_TONAL_BUTTON, C.OUTLINED_BUTTON,
            C.TEXT_BUTTON, C.ICON_BUTTON, C.SWITCH, C.CHECKBOX, C.RADIO_BUTTON, C.SLIDER,
            C.TEXT_FIELD, C.OUTLINED_TEXT_FIELD, C.LAZY_COLUMN, C.LAZY_ROW, C.SCAFFOLD, C.TOP_APP_BAR,
            C.ALERT_DIALOG, C.CIRCULAR_PROGRESS_INDICATOR, C.LINEAR_PROGRESS_INDICATOR, C.SNACKBAR)
        assertEquals(expected + ComposeUiV2.componentNames, RendererCatalog.components)
        assertEquals(RenderKind.entries.size, RendererCatalog.dispatch.size)
        RendererCatalog.components.forEach { assertNotNull(ComposeUiV2.catalog.component(it)) }
        assertEquals(55, RendererCatalog.components.size)
        assertEquals(54, RendererCatalog.dispatch.size)
        try { RendererCatalog.validate(UiNode(1, C.SNACKBAR)); fail() }
        catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.UNKNOWN_COMPONENT, e.code) }
    }

    @Test fun coreIconNamesAreExplicitBoundedAndCaseInsensitive() {
        assertEquals(280, IconCatalog.names.size)
        assertEquals("Filled.Home", IconCatalog.canonicalName("home"))
        assertEquals("Outlined.Home", IconCatalog.canonicalName("outlined.HOME"))
        assertEquals("AutoMirrored.Filled.ArrowBack", IconCatalog.canonicalName("AutoMirrored.Filled.ArrowBack"))
        assertEquals(24f, IconCatalog.vector("Home").viewportWidth, 0f)
        assertEquals(24f, IconCatalog.vector("Outlined.Home").viewportHeight, 0f)
        try { IconCatalog.vector("Unknown"); fail() }
        catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.INVALID_ARGUMENT, e.code) }
    }
    @Test fun derivedSliderTicksAndFloatRangeAreBoundedBeforeComposition() {
        RendererCatalog.validate(UiNode(1, C.SLIDER, props = mapOf(P.STEPS to UiValue.Num((ComposeUiLimits.MAX_VALUE_ITEMS - 2).toDouble()))))
        try { RendererCatalog.validate(UiNode(1, C.SLIDER, props = mapOf(P.STEPS to UiValue.Num(Int.MAX_VALUE.toDouble())))); fail() }
        catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.LIMIT_EXCEEDED, e.code); assertEquals(P.STEPS, e.prop) }
        for ((start, end) in listOf(-Float.MAX_VALUE.toDouble() to Float.MAX_VALUE.toDouble(), 1.0 to 1.00000000001)) {
            val values = UiValue.ListOf(listOf(UiValue.Num(start), UiValue.Num(end)))
            try { RendererCatalog.validate(UiNode(1, C.SLIDER, props = mapOf(P.RANGE to values))); fail() }
            catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.INVALID_ARGUMENT, e.code); assertEquals(P.RANGE, e.prop) }
        }
    }
}
