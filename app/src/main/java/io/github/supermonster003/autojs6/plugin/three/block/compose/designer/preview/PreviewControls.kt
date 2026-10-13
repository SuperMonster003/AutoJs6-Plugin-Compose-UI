package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import org.autojs.plugin.compose.api.catalog.ComponentSpec
import org.autojs.plugin.compose.api.catalog.PayloadFieldSpec
import org.autojs.plugin.compose.api.catalog.PayloadType

/**
 * Controlled properties of the interactive preview (roadmap P9.3 step 1, D43). A property bound to a
 * top-level state (`checked: on.value`) and an event of the same component whose payload carries a field of
 * the same name (`checkedChange` with `checked`) make that event set the state's preview value. The catalog
 * follows this naming for every value-like event (checked, value and text, selected, page, open,
 * selectedDateMillis, hour and minute), so no table per component is needed. The script's handlers never run.
 */
internal object PreviewControls {
    class Control(val field: PayloadFieldSpec, val state: String)

    /** The events of [node] that set a state's preview value, by event name. */
    fun controls(node: DesignNode, spec: ComponentSpec): Map<String, List<Control>> {
        val bound = spec.props.mapNotNull { prop ->
            val entry = node.props.firstOrNull { it.name == prop.name || it.name in prop.aliases } ?: return@mapNotNull null
            (entry.value as? PropValue.StateRef)?.let { prop.name to it.name }
        }.toMap()
        if (bound.isEmpty()) return emptyMap()
        return spec.events.mapNotNull { event ->
            val controls = event.payloadFields.mapNotNull { field -> bound[field.name]?.let { Control(field, it) } }
            if (controls.isEmpty()) null else event.name to controls
        }.toMap()
    }

    /** The preview values that [event] of [node] sets; [read] returns a payload field's value as the event carries it. */
    fun values(node: DesignNode, spec: ComponentSpec, event: String, read: (String) -> Any?): Map<String, JsLiteral> =
        controls(node, spec)[event].orEmpty().mapNotNull { control -> literal(control.field.type, read(control.field.name))?.let { control.state to it } }.toMap()

    private fun literal(type: PayloadType, raw: Any?): JsLiteral? = when (type) {
        PayloadType.BOOLEAN -> (raw as? Boolean)?.let { JsLiteral.Bool(it) }
        PayloadType.STRING -> (raw as? String)?.let { JsLiteral.Str(it) }
        PayloadType.INTEGER, PayloadType.LONG, PayloadType.NUMBER -> (raw as? Number)?.let { JsLiteral.Num(it.toDouble()) }
    }
}
