package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

/**
 * The designer's immutable document model (roadmap P8.2 / D35 / D36 / D37).
 *
 * A document is the script file itself: the recognized parts (state declarations and the single
 * mount / floaty / dialog host declaration with its node tree) are structured, everything else is kept
 * as opaque source text in its original order. Property values keep three shapes: JavaScript literals
 * the inspector can edit, bare `name.value` reads of a declared state (bindings) and opaque expressions
 * written back verbatim. Node identities are document-local integers used by edit commands and by the
 * design-time preview.
 */
sealed class JsLiteral {
    data class Str(val value: String) : JsLiteral()
    /** [text] is the source spelling so that `0.6`, `1e3` or `-2` survive a round trip unchanged. */
    data class Num(val value: Double, val text: String) : JsLiteral() {
        constructor(value: Double) : this(value, numberText(value))
    }
    data class Bool(val value: Boolean) : JsLiteral()
    object Null : JsLiteral() { override fun toString() = "Null" }
    data class Arr(val items: List<JsLiteral>) : JsLiteral()
    data class Obj(val fields: List<Pair<String, JsLiteral>>) : JsLiteral()

    companion object {
        fun numberText(value: Double): String =
            if (value == Math.floor(value) && !value.isInfinite() && Math.abs(value) < 1e15) value.toLong().toString() else value.toString()
    }
}

sealed class PropValue {
    data class Literal(val value: JsLiteral) : PropValue()
    /** A bare `name.value` read of a state declared in the same document (D37 binding). */
    data class StateRef(val name: String) : PropValue()
    /** Any other expression, preserved verbatim. */
    data class Expression(val source: String) : PropValue()

    companion object {
        fun str(value: String) = Literal(JsLiteral.Str(value))
        fun num(value: Double) = Literal(JsLiteral.Num(value))
        fun bool(value: Boolean) = Literal(JsLiteral.Bool(value))
    }
}

/** A handler's source: parameters as written and the body text with the common indentation removed. */
data class HandlerSource(
    val params: List<String>,
    val body: String,
    val arrow: Boolean = false,
    /** `(x) => expr` arrows keep the expression form; [body] is then the expression. */
    val expressionBody: Boolean = false,
) {
    val isSingleLine: Boolean get() = '\n' !in body
}

sealed class ModifierArg {
    data class Value(val value: PropValue) : ModifierArg()
    data class Callback(val handler: HandlerSource) : ModifierArg()
}

/** One `.name(args)` operation of a `compose.modifier()` chain, in chain order. */
data class ModifierEntry(val name: String, val args: List<ModifierArg> = emptyList())

/** The entries of a node's property object, in source order. */
sealed class NodeEntry {
    abstract val name: String

    data class Key(val value: PropValue) : NodeEntry() { override val name get() = KEY }
    /** A catalog property, alias, shortcut, `ref`, or an unknown name the validator reports. */
    data class Prop(override val name: String, val value: PropValue) : NodeEntry()
    /** A slot of the component (catalog slots only); a string child is a shorthand Text node. */
    data class Slot(override val name: String, val child: DesignChild) : NodeEntry()
    /** An `onXxx` property whose value is a function. */
    data class Handler(override val name: String, val handler: HandlerSource) : NodeEntry()
    /** `modifier: compose.modifier().a().b()`. */
    data class Modifier(val chain: List<ModifierEntry>) : NodeEntry() { override val name get() = MODIFIER }
    /** `modifier: <anything else>`, preserved verbatim. */
    data class ModifierExpression(val source: String) : NodeEntry() { override val name get() = MODIFIER }

    companion object {
        const val KEY = "key"
        const val MODIFIER = "modifier"
    }
}

sealed class DesignChild {
    data class Node(val node: DesignNode) : DesignChild()
    /** A child the designer cannot edit (a variable, a conditional, a loop result), written back verbatim. */
    data class Expression(val source: String) : DesignChild()
}

data class DesignNode(
    val id: Int,
    val type: String,
    val entries: List<NodeEntry> = emptyList(),
    val children: List<DesignChild> = emptyList(),
    /** A Text node that was written as a bare string child or slot value; written back the same way while it stays plain. */
    val shorthand: Boolean = false,
) {
    val key: PropValue? get() = entries.filterIsInstance<NodeEntry.Key>().firstOrNull()?.value
    val props: List<NodeEntry.Prop> get() = entries.filterIsInstance<NodeEntry.Prop>()
    val slots: List<NodeEntry.Slot> get() = entries.filterIsInstance<NodeEntry.Slot>()
    val handlers: List<NodeEntry.Handler> get() = entries.filterIsInstance<NodeEntry.Handler>()
    val modifiers: List<ModifierEntry>? get() = entries.filterIsInstance<NodeEntry.Modifier>().firstOrNull()?.chain
    fun prop(name: String): PropValue? = props.firstOrNull { it.name == name }?.value
    fun slot(name: String): DesignChild? = slots.firstOrNull { it.name == name }?.child
    fun handler(name: String): HandlerSource? = handlers.firstOrNull { it.name == name }?.handler

    /** True while a shorthand Text node still has nothing but a literal `text`, so it can be written as a string again. */
    val isPlainText: Boolean
        get() = type == TEXT && entries.size == 1 && (entries[0] as? NodeEntry.Prop)?.let { it.name == "text" && it.value is PropValue.Literal && (it.value as PropValue.Literal).value is JsLiteral.Str } == true && children.isEmpty()

    /** Every node of this subtree in preorder. */
    fun descendants(): Sequence<DesignNode> = sequence {
        yield(this@DesignNode)
        for (slot in slots) (slot.child as? DesignChild.Node)?.let { yieldAll(it.node.descendants()) }
        for (child in children) (child as? DesignChild.Node)?.let { yieldAll(it.node.descendants()) }
    }

    companion object {
        const val TEXT = "Text"
        fun text(id: Int, value: String, shorthand: Boolean = true) =
            DesignNode(id, TEXT, listOf(NodeEntry.Prop("text", PropValue.str(value))), shorthand = shorthand)
    }
}

enum class HostKind(val method: String) { MOUNT("mount"), FLOATY("floaty"), DIALOG("dialog");
    companion object { fun byMethod(method: String) = entries.firstOrNull { it.method == method } }
}

sealed class HostOptions {
    data class Fields(val fields: List<Pair<String, PropValue>>) : HostOptions()
    data class Expression(val source: String) : HostOptions()
}

/** `keyword name = compose.state(initial);` at the top level of the script. */
data class StateDecl(val keyword: String, val name: String, val initial: PropValue)

/** The single `compose.mount / floaty / dialog(...)` declaration of the script. */
data class MountDecl(
    val kind: HostKind,
    val root: DesignNode,
    /** `let session = compose.mount(...)`: declaration keyword (empty for a plain assignment) and name. */
    val bindKeyword: String? = null,
    val bindName: String? = null,
    /** True for `compose.mount(function () { ...; return tree; })`, false for a node handle passed directly. */
    val render: Boolean = true,
    val renderArrow: Boolean = false,
    /** Statements of the render function before its `return`, verbatim and dedented. */
    val renderPrelude: String? = null,
    val options: HostOptions? = null,
)

sealed class Segment {
    /** Source text the designer does not interpret: directives, comments, helper statements. */
    data class Opaque(val source: String) : Segment()
    data class State(val decl: StateDecl) : Segment()
    data class Mount(val decl: MountDecl) : Segment()
}

data class DesignDocument(val segments: List<Segment>, val nextId: Int) {
    val mount: MountDecl? get() = segments.filterIsInstance<Segment.Mount>().firstOrNull()?.decl
    val root: DesignNode? get() = mount?.root
    val states: List<StateDecl> get() = segments.filterIsInstance<Segment.State>().map { it.decl }

    fun nodes(): Sequence<DesignNode> = root?.descendants() ?: emptySequence()
    fun node(id: Int): DesignNode? = nodes().firstOrNull { it.id == id }

    /** The parent of [id] and the position of [id] inside it (a slot name or a child index). */
    fun parentOf(id: Int): Placement? {
        for (parent in nodes()) {
            parent.slots.forEach { slot -> if ((slot.child as? DesignChild.Node)?.node?.id == id) return Placement(parent.id, slotName = slot.name) }
            parent.children.forEachIndexed { index, child -> if ((child as? DesignChild.Node)?.node?.id == id) return Placement(parent.id, index = index) }
        }
        return null
    }

    data class Placement(val parentId: Int, val index: Int = -1, val slotName: String? = null)

    /** Returns a copy whose node [id] is replaced by [transform]'s result; unchanged when [id] is absent. */
    fun mapNode(id: Int, transform: (DesignNode) -> DesignNode): DesignDocument {
        val current = mount ?: return this
        val replaced = replaceIn(current.root, id, transform)
        if (replaced === current.root) return this
        return withMount(current.copy(root = replaced))
    }

    fun withMount(decl: MountDecl): DesignDocument = copy(segments = segments.map { if (it is Segment.Mount) Segment.Mount(decl) else it })

    /** Re-numbers every node of [child] from [nextId] so that inserted subtrees never collide with existing identities. */
    fun allocate(child: DesignChild): Pair<DesignChild, Int> {
        var next = nextId
        fun renumber(node: DesignNode): DesignNode {
            val id = next++
            val entries = node.entries.map { entry ->
                if (entry is NodeEntry.Slot && entry.child is DesignChild.Node) entry.copy(child = DesignChild.Node(renumber(entry.child.node))) else entry
            }
            val children = node.children.map { if (it is DesignChild.Node) DesignChild.Node(renumber(it.node)) else it }
            return node.copy(id = id, entries = entries, children = children)
        }
        return when (child) {
            is DesignChild.Node -> DesignChild.Node(renumber(child.node)) to next
            is DesignChild.Expression -> child to next
        }
    }

    companion object {
        fun replaceIn(node: DesignNode, id: Int, transform: (DesignNode) -> DesignNode): DesignNode {
            if (node.id == id) return transform(node)
            var changed = false
            val entries = node.entries.map { entry ->
                if (entry is NodeEntry.Slot && entry.child is DesignChild.Node) {
                    val inner = replaceIn(entry.child.node, id, transform)
                    if (inner !== entry.child.node) { changed = true; entry.copy(child = DesignChild.Node(inner)) } else entry
                } else entry
            }
            val children = node.children.map { child ->
                if (child is DesignChild.Node) {
                    val inner = replaceIn(child.node, id, transform)
                    if (inner !== child.node) { changed = true; DesignChild.Node(inner) } else child
                } else child
            }
            return if (changed) node.copy(entries = entries, children = children) else node
        }
    }
}
