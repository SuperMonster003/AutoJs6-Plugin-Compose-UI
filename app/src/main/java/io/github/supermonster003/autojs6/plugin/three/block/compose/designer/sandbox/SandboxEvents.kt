package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox

import org.autojs.plugin.compose.api.ComposeUiComponents
import org.autojs.plugin.compose.api.ComposeUiEventFields
import org.autojs.plugin.compose.api.ComposeUiEvents
import org.autojs.plugin.compose.api.v2.ComposeUiV2

/**
 * The arguments a script's event handler receives for a renderer event, decoded from its payload the way the host
 * does (AutoJs6 `Compose.invokeCallback` and `ComposeV2Callbacks`): a field's `valueChange` gets the text, the
 * selection and the edit sequence, a checked or focus change its boolean, a scroll the first visible index and the
 * offset, the V2 events their scalar fields, and every other event (a click) nothing.
 */
internal object SandboxEvents {
    private val F = ComposeUiEventFields
    private val V = ComposeUiV2.Fields
    private val E = ComposeUiV2.Events

    fun arguments(event: String, componentType: String, payload: Map<String, Any?>): List<Any?> = when (event) {
        ComposeUiEvents.CHECKED_CHANGE -> listOf(payload[F.CHECKED] as? Boolean ?: false)
        ComposeUiEvents.FOCUS_CHANGE -> listOf(payload[F.FOCUSED] as? Boolean ?: false)
        ComposeUiEvents.VALUE_CHANGE -> if (componentType == ComposeUiComponents.TEXT_FIELD || componentType == ComposeUiComponents.OUTLINED_TEXT_FIELD) {
            listOf(
                payload[F.TEXT] as? String ?: "",
                mapOf("start" to number(payload[F.SELECTION_START]), "end" to number(payload[F.SELECTION_END])),
                number(payload[F.EDIT_SEQ]),
            )
        } else listOf(number(payload[F.VALUE]))
        ComposeUiEvents.SCROLL -> listOf(payload[F.FIRST_VISIBLE_INDEX]?.let(::number), number(payload[F.OFFSET]))
        E.OPEN_CHANGE -> listOf(payload[V.OPEN] as? Boolean ?: false)
        E.SELECTED_CHANGE -> listOf(payload[V.SELECTED] as? Boolean ?: false)
        E.EXPANDED_CHANGE -> listOf(payload[V.EXPANDED] as? Boolean ?: false)
        E.QUERY_CHANGE, E.SEARCH -> listOf(payload[F.TEXT] as? String ?: "")
        E.PAGE_CHANGE -> listOf(number(payload[V.PAGE]))
        E.TIME_CHANGE -> listOf(number(payload[V.HOUR]), number(payload[V.MINUTE]))
        E.DATE_CHANGE -> listOf(payload[V.SELECTED_DATE_MILLIS]?.let(::number))
        E.DISPLAY_MODE_CHANGE -> listOf(payload[V.DISPLAY_MODE] as? String ?: "picker")
        else -> emptyList()
    }

    private fun number(value: Any?): Number = when (value) {
        is Int, is Long -> value as Number
        is Number -> value.toDouble()
        else -> 0
    }
}
