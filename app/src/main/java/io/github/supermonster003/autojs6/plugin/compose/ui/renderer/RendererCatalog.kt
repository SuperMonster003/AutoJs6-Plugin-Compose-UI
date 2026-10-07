package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Components as V

/** The same dispatch table drives capabilities and RenderNode; unsupported catalog types stay absent. */
internal enum class RenderKind(val component: String) {
    COLUMN(C.COLUMN), ROW(C.ROW), BOX(C.BOX), SPACER(C.SPACER), SURFACE(C.SURFACE), CARD(C.CARD),
    DIVIDER(C.HORIZONTAL_DIVIDER), TEXT(C.TEXT), ICON(C.ICON), IMAGE(C.IMAGE), BUTTON(C.BUTTON),
    ELEVATED_BUTTON(C.ELEVATED_BUTTON), TONAL_BUTTON(C.FILLED_TONAL_BUTTON), OUTLINED_BUTTON(C.OUTLINED_BUTTON),
    TEXT_BUTTON(C.TEXT_BUTTON), ICON_BUTTON(C.ICON_BUTTON), SWITCH(C.SWITCH), CHECKBOX(C.CHECKBOX),
    RADIO_BUTTON(C.RADIO_BUTTON), SLIDER(C.SLIDER),
    TEXT_FIELD(C.TEXT_FIELD), OUTLINED_TEXT_FIELD(C.OUTLINED_TEXT_FIELD),
    LAZY_COLUMN(C.LAZY_COLUMN), LAZY_ROW(C.LAZY_ROW), SCAFFOLD(C.SCAFFOLD), TOP_APP_BAR(C.TOP_APP_BAR),
    ALERT_DIALOG(C.ALERT_DIALOG), CIRCULAR_PROGRESS(C.CIRCULAR_PROGRESS_INDICATOR), LINEAR_PROGRESS(C.LINEAR_PROGRESS_INDICATOR),
    NAVIGATION_BAR(V.NAVIGATION_BAR), NAVIGATION_BAR_ITEM(V.NAVIGATION_BAR_ITEM),
    NAVIGATION_RAIL(V.NAVIGATION_RAIL), NAVIGATION_RAIL_ITEM(V.NAVIGATION_RAIL_ITEM),
    NAVIGATION_DRAWER(V.NAVIGATION_DRAWER), NAVIGATION_DRAWER_ITEM(V.NAVIGATION_DRAWER_ITEM),
    TAB_ROW(V.TAB_ROW), TAB(V.TAB), MODAL_BOTTOM_SHEET(V.MODAL_BOTTOM_SHEET),
    DROPDOWN_MENU(V.DROPDOWN_MENU), DROPDOWN_MENU_ITEM(V.DROPDOWN_MENU_ITEM),
    DATE_PICKER(V.DATE_PICKER), TIME_PICKER(V.TIME_PICKER), HORIZONTAL_PAGER(V.HORIZONTAL_PAGER),
    LAZY_VERTICAL_GRID(V.LAZY_VERTICAL_GRID), ASSIST_CHIP(V.ASSIST_CHIP), FILTER_CHIP(V.FILTER_CHIP),
    INPUT_CHIP(V.INPUT_CHIP), BADGE(V.BADGE), SEGMENTED_BUTTON(V.SEGMENTED_BUTTON),
    SEGMENTED_BUTTON_ITEM(V.SEGMENTED_BUTTON_ITEM), FLOATING_ACTION_BUTTON(V.FLOATING_ACTION_BUTTON),
    SEARCH_BAR(V.SEARCH_BAR), TOOLTIP(V.TOOLTIP), PULL_TO_REFRESH(V.PULL_TO_REFRESH),
}

internal object RendererCatalog {
    val dispatch = RenderKind.entries.associateBy { it.component }
    val components: Set<String> = java.util.Collections.unmodifiableSet(dispatch.keys + C.SNACKBAR)
    val catalog get() = ComposeUiV2.catalog
    val indexedComponents = setOf(C.LAZY_COLUMN, C.LAZY_ROW, V.LAZY_VERTICAL_GRID, V.HORIZONTAL_PAGER)

    fun validate(node: UiNode) {
        if (node.type !in dispatch) throw ComposeUiContractException(ComposeUiErrorCodes.UNKNOWN_COMPONENT, nodeId = node.nodeId)
        if (node.type == C.ICON) IconCatalog.canonicalName((node.props.getValue(P.NAME) as UiValue.IconName).value)
        (node.props[P.STYLE] as? UiValue.TextStyle)?.fontFamily?.let(ValueMapper::fontFamily)
        if (node.type == C.SLIDER) {
            // Material creates steps + 2 tick values. Bound the derived collection before allocation.
            val steps = (node.props[P.STEPS] as? UiValue.Num)?.value ?: 0.0
            if (steps > ComposeUiLimits.MAX_VALUE_ITEMS - 2) throw ComposeUiContractException(
                ComposeUiErrorCodes.LIMIT_EXCEEDED, nodeId = node.nodeId, prop = P.STEPS)
            val range = node.props[P.RANGE] as? UiValue.ListOf
            val start = (range?.values?.get(0) as? UiValue.Num)?.value?.toFloat() ?: 0f
            val end = (range?.values?.get(1) as? UiValue.Num)?.value?.toFloat() ?: 1f
            if (start >= end || !(end - start).isFinite()) throw ComposeUiContractException(
                ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = node.nodeId, prop = P.RANGE)
        }
        // NodeStore validates all catalog properties, children, slots and modifier scopes first.
    }

}
