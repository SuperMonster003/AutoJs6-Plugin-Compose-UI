package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptText
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
import org.autojs.plugin.compose.api.model.UiCommand
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiTree
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.model.ValueKind
import org.autojs.plugin.compose.api.v2.ComposeUiV2

/**
 * A design-time frame: the contract tree derived from a [DesignDocument] for the real renderer (roadmap
 * D34). Literal properties convert like the host's script bridge; state bindings show their initial
 * value; opaque expressions become their source text where the property is text-like (isolated left to right, so it
 * keeps its order on a right-to-left canvas) and are skipped elsewhere; opaque children, slots and unrenderable
 * components become labeled placeholder texts whose identities map back to the design node that owns them. Window-producing components (AlertDialog,
 * ModalBottomSheet, DropdownMenu, Tooltip) whose `open` resolves to true reach the renderer closed, so no
 * system window opens and only what sits in place is drawn (a menu's anchor, a tooltip's anchor); their
 * window content is listed as an [Overlay] the canvas draws over the page (roadmap P9.1). A catalog component the
 * designer cannot draw (AndroidView, whose view only exists in a running script) stands in as a tinted
 * Surface that keeps the node's size modifiers and shows its view expression (roadmap P9.1, D44). With an
 * [ImageSource], a literal Image `src` string becomes the bitmap it names (P9.1, D44).
 */
internal class PreviewFrame(
    val frame: RenderFrame?,
    val tree: UiTree?,
    val generation: Long,
    /** Problems that kept parts of the tree from being previewed, in node order. */
    val problems: List<String>,
    /** Placeholder UiNode ids -> the design node id they stand in for (the owner for expression children). */
    val placeholders: Map<Int, Int>,
    /** Open window components in tree preorder (a popup before those opened inside it), to draw over the page. */
    val overlays: List<Overlay> = emptyList(),
    /** The sandbox's hooks when the frame shows its tree (P9.3 step 2). */
    val live: LiveHooks? = null,
)

/**
 * The interactive preview's script hooks (roadmap P9.3 step 2): the events of each node that have listeners in
 * the sandbox, and the callback tokens of each node's modifier chain in chain order. With them the frame carries
 * the callbacks the script declared instead of the state-bound controls of [PreviewControls].
 */
internal class LiveHooks(
    val events: Map<Int, List<String>>,
    val tokens: Map<Int, List<Int>>,
    /**
     * The edit sequence each text field's text in the tree goes with: the canvas sets a field to that text through
     * an edit command, which the field refuses when the user has typed since, as the host's renderer does.
     */
    val fieldSequences: Map<Int, Long> = emptyMap(),
    /** Handle commands (focus, blur, scrollTo) issued with this tree, carried out once it is drawn. */
    val commands: List<UiCommand> = emptyList(),
)

/** The sandbox's current tree as a throwaway document whose node ids are the runtime's, with its hooks. */
internal class LiveTree(val document: DesignDocument, val hooks: LiveHooks)

/** How the canvas draws an open window component (roadmap P9.1). */
internal enum class OverlayKind { DIALOG, SHEET, MENU, TOOLTIP }

/**
 * An open window component drawn inside the canvas instead of a window: [slots] and [children] are UiNode ids
 * of the frame (the closed owner still holds them, so the contract tree stays closed) that the overlay draws.
 */
internal class Overlay(val ownerId: Int, val kind: OverlayKind, val slots: Map<String, Int>, val children: List<Int>)

internal object PreviewFrames {
    private const val PLACEHOLDER_BASE = 1_000_000
    private const val PLACEHOLDER_COLOR = 0xFF8A8A8A.toInt()
    private const val STAND_IN_COLOR = 0x1F8A8A8A
    private const val STAND_IN_SOURCE_LIMIT = 60

    /** Components the renderer presents in a separate window (dialog or popup) when `open` is true. */
    internal val WINDOWED = mapOf(
        ComposeUiComponents.ALERT_DIALOG to OverlayKind.DIALOG,
        "ModalBottomSheet" to OverlayKind.SHEET,
        "DropdownMenu" to OverlayKind.MENU,
        "Tooltip" to OverlayKind.TOOLTIP,
    )

    /**
     * [values] are preview values of top-level states (roadmap P9.3): they replace the initial values in
     * bindings without touching the document. Properties bound to a state get callbacks for the events that
     * change them ([PreviewControls]), so an interactive canvas can report those changes. With [live] the
     * document is the sandbox's tree (P9.3 step 2) and the callbacks are the script's own.
     */
    fun build(
        document: DesignDocument,
        generation: Long,
        catalog: ComponentCatalog = ComposeUiV2.interopCatalog,
        images: ImageSource? = null,
        values: Map<String, JsLiteral> = emptyMap(),
        live: LiveHooks? = null,
    ): PreviewFrame {
        // A sandbox whose script closed its session (or never mounted) shows an empty canvas, which is no problem.
        val root = document.root ?: return PreviewFrame(null, null, generation, if (live == null) listOf("The document has no mount") else emptyList(), emptyMap())
        val states = document.states.associate { it.name to (values[it.name]?.let { value -> PropValue.Literal(value) } ?: it.initial) }
        val builder = Builder(catalog, states, images, live)
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
        return PreviewFrame(frame, tree, generation, builder.problems, builder.placeholders, if (frame == null) emptyList() else builder.overlays.filterNotNull(), live)
    }

    private class Builder(private val catalog: ComponentCatalog, private val states: Map<String, PropValue>, private val images: ImageSource?, private val live: LiveHooks?) {
        val nodes = LinkedHashMap<Int, UiNode>()
        val problems = ArrayList<String>()
        val placeholders = HashMap<Int, Int>()
        /** Open window components in tree preorder; a slot stays null when its owner ends up a placeholder. */
        val overlays = ArrayList<Overlay?>()
        private var nextPlaceholder = PLACEHOLDER_BASE

        fun emit(node: DesignNode, parentType: String?): Int {
            val spec = catalog.component(node.type)
            if (spec == null || node.type !in RendererCatalog.dispatch) {
                problems += "${node.type} cannot be previewed"
                return if (spec != null) standIn(node, parentType) else placeholder(node.id, "[${node.type}]")
            }
            val raw = LinkedHashMap<String, UiValue>()
            var key: String? = null
            val operations = ArrayList<ModifierOp>()
            val tokens = live?.let { it.tokens[node.id].orEmpty().iterator() }
            node.entries.forEach { entry ->
                when (entry) {
                    is NodeEntry.Key -> key = when (val value = entry.value) {
                        is PropValue.Literal -> (value.value as? JsLiteral.Str)?.value ?: (value.value as? JsLiteral.Num)?.text
                        else -> null
                    }
                    is NodeEntry.Prop -> propValue(node, spec.props.firstOrNull { it.name == entry.name || entry.name in it.aliases }?.type, entry)?.let { raw[entry.name] = it }
                    is NodeEntry.Modifier -> entry.chain.forEach { operation -> modifierOp(operation, parentType, tokens)?.let { operations += it } }
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
            val overlay = WINDOWED[node.type]?.takeIf { (normalized?.props?.get(ComposeUiProps.OPEN) as? UiValue.Bool)?.value == true }
            // Taken before the content is emitted, so a popup opened inside another one is drawn above it.
            val overlaySlot = if (overlay != null) overlays.size.also { overlays += null } else -1
            val slots = LinkedHashMap<String, Int>()
            node.slots.forEach { slot ->
                if (spec.slots.none { it.name == slot.name }) return@forEach
                slots[slot.name] = when (val child = slot.child) {
                    is DesignChild.Node -> emit(child.node, node.type)
                    is DesignChild.Expression -> placeholder(node.id, ScriptText.isolate("{${child.source}}"))
                }
            }
            val children = node.children.map { child ->
                when (child) {
                    is DesignChild.Node -> emit(child.node, node.type)
                    is DesignChild.Expression -> placeholder(node.id, ScriptText.isolate("{${child.source}}"))
                }
            }
            val props = (normalized?.props ?: emptyMap()).let { if (overlay != null) it + (ComposeUiProps.OPEN to UiValue.Bool(false)) else it }
            val callbacks = if (live != null) {
                live.events[node.id].orEmpty().filter { event -> spec.events.any { it.name == event } }.associateWith { node.id }
            } else PreviewControls.controls(node, spec).keys.associateWith { node.id }
            val uiNode = try {
                UiNode(node.id, node.type, key, props, normalized?.modifier ?: emptyList(), children, slots, callbacks).also { RendererCatalog.validate(it) }
            } catch (error: ComposeUiContractException) {
                problems += "${node.type}: ${error.message ?: error.code}"
                try {
                    UiNode(node.id, node.type, key, emptyMap(), emptyList(), children, slots, callbacks).also { RendererCatalog.validate(it) }
                } catch (second: ComposeUiContractException) {
                    return placeholder(node.id, "[${node.type}]")
                }
            }
            nodes[uiNode.nodeId] = uiNode
            if (overlay != null) overlays[overlaySlot] = Overlay(node.id, overlay, slots, children)
            return uiNode.nodeId
        }

        private fun propValue(node: DesignNode, type: ValueType?, entry: NodeEntry.Prop): UiValue? {
            val shortcut = catalog.shortcuts.firstOrNull { it.name == entry.name }
            val modifier = shortcut?.let { catalog.modifier(it.modifierName) }
            val valueType = type ?: modifier?.signatures?.first()?.args?.first()?.type ?: return null
            val propValue = entry.value
            if (images != null && entry.name == ComposeUiProps.SRC && propValue is PropValue.Literal && propValue.value is JsLiteral.Str) {
                return when (val lookup = images.resolve((propValue.value as JsLiteral.Str).value)) {
                    is ImageLookup.Found -> lookup.value
                    is ImageLookup.Missing -> { problems += lookup.problem; null }
                }
            }
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
                is PropValue.Expression -> if (ValueKind.STRING in valueType.kinds && entry.name != ComposeUiProps.SRC) UiValue.Str(ScriptText.isolate(propValue.source)) else null
            }
            if (converted == null && propValue !is PropValue.Expression) problems += "${node.type}.${entry.name}: value not previewable"
            return converted
        }

        private fun literal(literal: JsLiteral, type: ValueType): UiValue? =
            (JsValueConverter.convert(literal, type) as? JsValueConverter.Conversion.Ok)?.value

        /**
         * A document's callback arguments are source text, so such operations are left out; the sandbox's carry
         * tokens ([tokens], taken in chain order even when the operation is left out, so later ones stay aligned).
         */
        private fun modifierOp(operation: ModifierEntry, parentType: String?, tokens: Iterator<Int>? = null): ModifierOp? {
            val callbackTokens = operation.args.filterIsInstance<ModifierArg.Callback>().map { if (tokens?.hasNext() == true) tokens.next() else null }
            val spec = catalog.modifier(operation.name) ?: return null
            val signature = spec.signatures.firstOrNull { operation.args.size in it.requiredCount..it.args.size } ?: return null
            if (tokens == null && (operation.args.any { it is ModifierArg.Callback } || signature.args.take(operation.args.size).any { it.callback })) return null
            var nextToken = 0
            val args = operation.args.mapIndexed { index, argument ->
                if (argument is ModifierArg.Callback) {
                    val token = callbackTokens[nextToken++] ?: return null
                    return@mapIndexed UiValue.Num(token.toDouble())
                }
                if (signature.args[index].callback) return null
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

        /**
         * A catalog component without a design-time rendering: a tinted Surface carrying the node's shortcut and
         * chained modifiers (so a sized AndroidView keeps its size and weight), labelled with its type and the
         * source of its first expression property (the `view` factory).
         */
        private fun standIn(node: DesignNode, parentType: String?): Int {
            val raw = LinkedHashMap<String, UiValue>()
            val operations = ArrayList<ModifierOp>()
            var source: String? = null
            node.entries.forEach { entry ->
                when (entry) {
                    is NodeEntry.Prop -> when {
                        catalog.shortcuts.any { it.name == entry.name } -> propValue(node, null, entry)?.let { raw[entry.name] = it }
                        source == null && entry.value is PropValue.Expression -> source = (entry.value as PropValue.Expression).source
                    }
                    is NodeEntry.Modifier -> entry.chain.forEach { operation -> modifierOp(operation, parentType)?.let { operations += it } }
                    else -> Unit
                }
            }
            val shown = source?.replace(Regex("\\s+"), " ")?.let { if (it.length > STAND_IN_SOURCE_LIMIT) it.take(STAND_IN_SOURCE_LIMIT - 3) + "..." else it }
            val padding = try {
                // Box has no required property, so the catalog turns the shortcut into its modifier operation alone.
                catalog.normalizeProperties(ComposeUiComponents.BOX, mapOf("padding" to UiValue.Dp(8.0)), emptyList()).modifier
            } catch (error: ComposeUiContractException) { emptyList() }
            val label = placeholder(node.id, "[${node.type}]" + (shown?.let { " " + ScriptText.isolate(it) } ?: ""), padding)
            val props = raw + (ComposeUiProps.COLOR to UiValue.Color(STAND_IN_COLOR))
            val normalized = try {
                catalog.normalizeProperties(ComposeUiComponents.SURFACE, props, operations)
            } catch (error: ComposeUiContractException) {
                try { catalog.normalizeProperties(ComposeUiComponents.SURFACE, mapOf(ComposeUiProps.COLOR to UiValue.Color(STAND_IN_COLOR)), emptyList()) } catch (ignored: ComposeUiContractException) { null }
            }
            val id = nextPlaceholder++
            placeholders[id] = node.id
            nodes[id] = UiNode(id, ComposeUiComponents.SURFACE, null, normalized?.props ?: emptyMap(), normalized?.modifier ?: emptyList(), listOf(label))
            return id
        }

        private fun placeholder(ownerId: Int, text: String, modifier: List<ModifierOp> = emptyList()): Int {
            val id = nextPlaceholder++
            placeholders[id] = ownerId
            nodes[id] = UiNode(id, ComposeUiComponents.TEXT, null, mapOf(
                ComposeUiProps.TEXT to UiValue.Str(text),
                ComposeUiProps.STYLE to UiValue.Enum("bodySmall"),
                ComposeUiProps.COLOR to UiValue.Color(PLACEHOLDER_COLOR),
            ), modifier)
            return id
        }
    }
}
