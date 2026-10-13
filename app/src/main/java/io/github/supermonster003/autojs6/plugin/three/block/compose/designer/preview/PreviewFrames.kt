package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsValueConverter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierArg
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.RenderFrame
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.RendererCatalog
import org.autojs.plugin.compose.api.ComposeUiComponents
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiProps
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.catalog.ValueType
import org.autojs.plugin.compose.api.model.ModifierOp
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiTree
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.model.ValueKind
import org.autojs.plugin.compose.api.v2.ComposeUiV2

/**
 * A design-time frame: the contract tree derived from a [DesignDocument] for the real renderer (roadmap
 * D34). Literal properties convert like the host's script bridge; state bindings show their initial
 * value; opaque expressions become their source text where the property is text-like and are skipped
 * elsewhere; opaque children, slots and unrenderable components become labeled placeholder texts whose
 * identities map back to the design node that owns them. Window-producing components (AlertDialog,
 * ModalBottomSheet, DropdownMenu, Tooltip) whose `open` resolves to true are shown as an inline card
 * holding their label, slots and children instead of a real window over the designer.
 */
internal class PreviewFrame(
    val frame: RenderFrame?,
    val tree: UiTree?,
    val generation: Long,
    /** Problems that kept parts of the tree from being previewed, in node order. */
    val problems: List<String>,
    /** Placeholder UiNode ids -> the design node id they stand in for (the owner for expression children). */
    val placeholders: Map<Int, Int>,
)

internal object PreviewFrames {
    private const val PLACEHOLDER_BASE = 1_000_000
    private const val PLACEHOLDER_COLOR = 0xFF8A8A8A.toInt()

    /** Components the renderer presents in a separate window (dialog or popup) when `open` is true. */
    internal val WINDOWED = setOf(ComposeUiComponents.ALERT_DIALOG, "ModalBottomSheet", "DropdownMenu", "Tooltip")

    fun build(document: DesignDocument, generation: Long, catalog: ComponentCatalog = ComposeUiV2.interopCatalog): PreviewFrame {
        val root = document.root ?: return PreviewFrame(null, null, generation, listOf("The document has no mount"), emptyMap())
        val states = document.states.associate { it.name to it.initial }
        val builder = Builder(catalog, states)
        val rootId = builder.emit(root, null)
        var nodes: Map<Int, UiNode> = builder.nodes
        val tree = try {
            UiTree(rootId, nodes.values.toList())
        } catch (error: ComposeUiContractException) {
            if (error.code == ComposeUiErrorCodes.DUPLICATE_KEY) {
                builder.problems += "Duplicate sibling keys are ignored in the preview"
                nodes = nodes.mapValues { (_, node) -> if (node.key == null) node else UiNode(node.nodeId, node.type, null, node.props, node.modifier, node.children, node.slots, node.callbacks) }
                try { UiTree(rootId, nodes.values.toList()) } catch (second: ComposeUiContractException) { builder.problems += (second.message ?: second.code); null }
            } else {
                builder.problems += (error.message ?: error.code)
                null
            }
        }
        val frame = tree?.let { RenderFrame(it.rootId, it.nodes.associateBy { node -> node.nodeId }, generation) }
        return PreviewFrame(frame, tree, generation, builder.problems, builder.placeholders)
    }

    private class Builder(private val catalog: ComponentCatalog, private val states: Map<String, PropValue>) {
        val nodes = LinkedHashMap<Int, UiNode>()
        val problems = ArrayList<String>()
        val placeholders = HashMap<Int, Int>()
        private var nextPlaceholder = PLACEHOLDER_BASE

        fun emit(node: DesignNode, parentType: String?): Int {
            val spec = catalog.component(node.type)
            if (spec == null || node.type !in RendererCatalog.dispatch) {
                problems += "${node.type} cannot be previewed"
                return placeholder(node.id, "[${node.type}]")
            }
            val raw = LinkedHashMap<String, UiValue>()
            var key: String? = null
            val operations = ArrayList<ModifierOp>()
            node.entries.forEach { entry ->
                when (entry) {
                    is NodeEntry.Key -> key = when (val value = entry.value) {
                        is PropValue.Literal -> (value.value as? JsLiteral.Str)?.value ?: (value.value as? JsLiteral.Num)?.text
                        else -> null
                    }
                    is NodeEntry.Prop -> propValue(node, spec.props.firstOrNull { it.name == entry.name || entry.name in it.aliases }?.type, entry)?.let { raw[entry.name] = it }
                    is NodeEntry.Modifier -> entry.chain.forEach { operation -> modifierOp(operation, parentType)?.let { operations += it } }
                    is NodeEntry.Slot, is NodeEntry.Handler, is NodeEntry.ModifierExpression -> Unit
                }
            }
            val normalized = try {
                catalog.normalizeProperties(node.type, raw, operations)
            } catch (error: ComposeUiContractException) {
                problems += "${node.type}: ${error.message ?: error.code}"
                try { catalog.normalizeProperties(node.type, emptyMap(), emptyList()) } catch (ignored: ComposeUiContractException) { null }
            }
            // A required property whose value cannot be previewed (an expression such as a bitmap variable)
            // leaves nothing the renderer could draw: stand in for the whole node instead of crashing it.
            val missing = spec.props.filter { it.required && it.defaultValue == null && (normalized?.props?.get(it.name) ?: raw[it.name]) == null }
            if (missing.isNotEmpty()) {
                problems += "${node.type}: ${missing.joinToString { it.name }} not previewable"
                return placeholder(node.id, "[${node.type}]")
            }
            val slots = LinkedHashMap<String, Int>()
            node.slots.forEach { slot ->
                if (spec.slots.none { it.name == slot.name }) return@forEach
                slots[slot.name] = when (val child = slot.child) {
                    is DesignChild.Node -> emit(child.node, node.type)
                    is DesignChild.Expression -> placeholder(node.id, "{${child.source}}")
                }
            }
            val children = node.children.map { child ->
                when (child) {
                    is DesignChild.Node -> emit(child.node, node.type)
                    is DesignChild.Expression -> placeholder(node.id, "{${child.source}}")
                }
            }
            if (node.type in WINDOWED && (normalized?.props?.get(ComposeUiProps.OPEN) as? UiValue.Bool)?.value == true) {
                return expanded(node, slots.values + children)
            }
            val uiNode = try {
                UiNode(node.id, node.type, key, normalized?.props ?: emptyMap(), normalized?.modifier ?: emptyList(), children, slots).also { RendererCatalog.validate(it) }
            } catch (error: ComposeUiContractException) {
                problems += "${node.type}: ${error.message ?: error.code}"
                try {
                    UiNode(node.id, node.type, key, emptyMap(), emptyList(), children, slots).also { RendererCatalog.validate(it) }
                } catch (second: ComposeUiContractException) {
                    return placeholder(node.id, "[${node.type}]")
                }
            }
            nodes[uiNode.nodeId] = uiNode
            return uiNode.nodeId
        }

        private fun propValue(node: DesignNode, type: ValueType?, entry: NodeEntry.Prop): UiValue? {
            val shortcut = catalog.shortcuts.firstOrNull { it.name == entry.name }
            val modifier = shortcut?.let { catalog.modifier(it.modifierName) }
            val valueType = type ?: modifier?.signatures?.first()?.args?.first()?.type ?: return null
            val propValue = entry.value
            val converted: UiValue? = when (propValue) {
                is PropValue.Literal -> {
                    val raw = propValue.value
                    if (shortcut != null && shortcut.expandList && raw is JsLiteral.Arr && modifier != null) {
                        val signature = modifier.signatures.firstOrNull { raw.items.size in it.requiredCount..it.args.size }
                        if (signature == null) null else {
                            val items = raw.items.mapIndexed { index, item -> literal(item, signature.args[index].type) }
                            if (items.any { it == null }) null else UiValue.ListOf(items.map { it!! })
                        }
                    } else literal(raw, valueType)
                }
                is PropValue.StateRef -> (states[propValue.name] as? PropValue.Literal)?.let { state -> literal(state.value, valueType) }
                is PropValue.Expression -> if (ValueKind.STRING in valueType.kinds && entry.name != ComposeUiProps.SRC) UiValue.Str(propValue.source) else null
            }
            if (converted == null && propValue !is PropValue.Expression) problems += "${node.type}.${entry.name}: value not previewable"
            return converted
        }

        private fun literal(literal: JsLiteral, type: ValueType): UiValue? =
            (JsValueConverter.convert(literal, type) as? JsValueConverter.Conversion.Ok)?.value

        private fun modifierOp(operation: ModifierEntry, parentType: String?): ModifierOp? {
            val spec = catalog.modifier(operation.name) ?: return null
            val signature = spec.signatures.firstOrNull { operation.args.size in it.requiredCount..it.args.size } ?: return null
            if (operation.args.any { it is ModifierArg.Callback } || signature.args.take(operation.args.size).any { it.callback }) return null
            val args = operation.args.mapIndexed { index, argument ->
                val value = (argument as ModifierArg.Value).value
                val literal = when (value) {
                    is PropValue.Literal -> value.value
                    is PropValue.StateRef -> (states[value.name] as? PropValue.Literal)?.value ?: return null
                    is PropValue.Expression -> return null
                }
                literal(literal, signature.args[index].type) ?: return null
            }
            val op = ModifierOp(operation.name, args, spec.scope)
            return try { catalog.validateModifier(op, parentType); op } catch (error: ComposeUiContractException) { null }
        }

        /** An open window component as an inline card: its label, then its slots and children in order. */
        private fun expanded(node: DesignNode, content: List<Int>): Int {
            val label = placeholder(node.id, "[${node.type}]")
            val column = synthetic(node.id, ComposeUiComponents.COLUMN, mapOf("padding" to UiValue.Num(12.0), ComposeUiProps.SPACING to UiValue.Num(8.0)), listOf(label) + content)
            return synthetic(node.id, ComposeUiComponents.CARD, emptyMap(), listOf(column))
        }

        private fun synthetic(ownerId: Int, type: String, raw: Map<String, UiValue>, children: List<Int>): Int {
            val id = nextPlaceholder++
            placeholders[id] = ownerId
            val normalized = try { catalog.normalizeProperties(type, raw, emptyList()) } catch (error: ComposeUiContractException) { null }
            nodes[id] = UiNode(id, type, null, normalized?.props ?: emptyMap(), normalized?.modifier ?: emptyList(), children)
            return id
        }

        private fun placeholder(ownerId: Int, text: String): Int {
            val id = nextPlaceholder++
            placeholders[id] = ownerId
            nodes[id] = UiNode(id, ComposeUiComponents.TEXT, null, mapOf(
                ComposeUiProps.TEXT to UiValue.Str(text),
                ComposeUiProps.STYLE to UiValue.Enum("bodySmall"),
                ComposeUiProps.COLOR to UiValue.Color(PLACEHOLDER_COLOR),
            ))
            return id
        }
    }
}
