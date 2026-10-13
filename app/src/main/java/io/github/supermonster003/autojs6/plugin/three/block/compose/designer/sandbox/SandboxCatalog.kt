package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText
import org.autojs.plugin.compose.api.ComposeUiComponents
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.catalog.ComponentInvocation
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.v2.ComposeUiV2

/**
 * The part of the component catalog the preview runtime needs (roadmap P9.3 step 2), as the JSON text it parses:
 * which components are node factories, their properties with aliases, `onXxx` callback properties, slots, events,
 * commands and simple default values, the modifiers with their callback arguments and commands, the shortcuts, and
 * what `compose.version` reports. The runtime checks names against it the way the host checks them.
 */
internal object SandboxCatalog {
    /** Components whose `scrollTo` takes an item index, as in the host. */
    private val INDEXED = listOf(
        ComposeUiComponents.LAZY_COLUMN, ComposeUiComponents.LAZY_ROW,
        ComposeUiV2.Components.HORIZONTAL_PAGER, ComposeUiV2.Components.LAZY_VERTICAL_GRID,
    )
    private val FIELDS = setOf(ComposeUiComponents.TEXT_FIELD, ComposeUiComponents.OUTLINED_TEXT_FIELD)

    fun json(catalog: ComponentCatalog, version: Map<String, Any?>): String {
        val components = LinkedHashMap<String, Any?>()
        catalog.components.forEach { spec ->
            val canonical = LinkedHashMap<String, String>()
            val defaults = LinkedHashMap<String, Any?>()
            spec.props.forEach { prop ->
                canonical[prop.name] = prop.name
                prop.aliases.forEach { canonical[it] = prop.name }
                prop.defaultValue?.let(::plain)?.let { defaults[prop.name] = it }
            }
            components[spec.name] = mapOf(
                "node" to (spec.invocation == ComponentInvocation.NODE),
                "properties" to canonical.keys.toList(),
                "canonical" to canonical,
                "defaults" to defaults,
                "callbacks" to spec.events.associate { callbackProperty(it.name) to it.name },
                "slots" to spec.slots.map { it.name },
                "events" to spec.events.map { it.name },
                "commands" to spec.commands,
                "field" to (spec.name in FIELDS),
            )
        }
        val modifiers = catalog.modifiers.associate { spec ->
            spec.name to mapOf(
                "signatures" to spec.signatures.map { signature ->
                    mapOf("required" to signature.requiredCount, "args" to signature.args.map { mapOf("callback" to it.callback) })
                },
                "commands" to spec.commands,
            )
        }
        return JsonText.write(mapOf(
            "components" to components,
            "modifiers" to modifiers,
            "shortcuts" to catalog.shortcuts.map { it.name },
            "maxModifierOps" to ComposeUiLimits.MAX_MODIFIER_OPS,
            "indexed" to INDEXED,
            "version" to version,
        ))
    }

    /** The script-side value of a default the runtime can return from a handle; others stay undefined. */
    private fun plain(value: UiValue): Any? = when (value) {
        is UiValue.Str -> value.value
        is UiValue.Num -> value.value
        is UiValue.Bool -> value.value
        is UiValue.Enum -> value.value
        is UiValue.IconName -> value.value
        is UiValue.Dp -> value.value
        is UiValue.Sp -> value.value
        else -> null
    }

    fun callbackProperty(event: String): String = "on" + event.replaceFirstChar { it.uppercaseChar() }
}
