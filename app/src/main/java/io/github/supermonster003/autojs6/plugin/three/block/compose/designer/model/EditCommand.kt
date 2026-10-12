package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

/**
 * Every edit of a [DesignDocument] goes through a command (roadmap D40). Commands are pure functions from
 * one immutable document to the next; [EditHistory] keeps the documents before and after each step, so
 * undo and redo never depend on a command computing its own inverse, and consecutive edits of the same
 * target (typing into one property) merge into a single step.
 */
sealed class EditCommand {
    /** Steps with the same non-null token merge while they follow each other directly. */
    open val mergeToken: Any? get() = null

    abstract fun apply(document: DesignDocument): DesignDocument

    /** Replaces the whole subtree whose root has [id]; the replacement keeps that id. */
    data class ReplaceNode(val id: Int, val node: DesignNode) : EditCommand() {
        override fun apply(document: DesignDocument) = document.mapNode(id) { node.copy(id = id) }
    }

    data class SetEntries(val id: Int, val entries: List<NodeEntry>) : EditCommand() {
        override fun apply(document: DesignDocument) = document.mapNode(id) { it.copy(entries = entries) }
    }

    /** Adds, replaces (in place) or removes (value null) a property. Shorthand Text nodes lose the shorthand on any other change. */
    data class SetProp(val id: Int, val name: String, val value: PropValue?) : EditCommand() {
        override val mergeToken get() = Triple("prop", id, name)
        override fun apply(document: DesignDocument) = document.mapNode(id) { node ->
            val entries = node.entries.toMutableList()
            val index = entries.indexOfFirst { it is NodeEntry.Prop && it.name == name }
            when {
                value == null -> if (index >= 0) entries.removeAt(index)
                index >= 0 -> entries[index] = NodeEntry.Prop(name, value)
                else -> entries += NodeEntry.Prop(name, value)
            }
            node.copy(entries = entries, shorthand = node.shorthand && name == "text" && value is PropValue.Literal && value.value is JsLiteral.Str)
        }
    }

    data class SetKey(val id: Int, val key: PropValue?) : EditCommand() {
        override val mergeToken get() = Pair("key", id)
        override fun apply(document: DesignDocument) = document.mapNode(id) { node ->
            val entries = node.entries.filterNot { it is NodeEntry.Key }.toMutableList()
            if (key != null) entries.add(0, NodeEntry.Key(key))
            node.copy(entries = entries, shorthand = false)
        }
    }

    data class SetSlot(val id: Int, val name: String, val child: DesignChild?) : EditCommand() {
        override fun apply(document: DesignDocument): DesignDocument {
            val (allocated, next) = child?.let { document.allocate(it) } ?: (null to document.nextId)
            return document.mapNode(id) { node ->
                val entries = node.entries.toMutableList()
                val index = entries.indexOfFirst { it is NodeEntry.Slot && it.name == name }
                when {
                    allocated == null -> if (index >= 0) entries.removeAt(index)
                    index >= 0 -> entries[index] = NodeEntry.Slot(name, allocated)
                    else -> entries += NodeEntry.Slot(name, allocated)
                }
                node.copy(entries = entries, shorthand = false)
            }.copy(nextId = next)
        }
    }

    data class SetHandler(val id: Int, val name: String, val handler: HandlerSource?) : EditCommand() {
        override val mergeToken get() = Triple("handler", id, name)
        override fun apply(document: DesignDocument) = document.mapNode(id) { node ->
            val entries = node.entries.toMutableList()
            val index = entries.indexOfFirst { it is NodeEntry.Handler && it.name == name }
            when {
                handler == null -> if (index >= 0) entries.removeAt(index)
                index >= 0 -> entries[index] = NodeEntry.Handler(name, handler)
                else -> entries += NodeEntry.Handler(name, handler)
            }
            node.copy(entries = entries, shorthand = false)
        }
    }

    /** Replaces the explicit modifier chain; null removes it. An opaque `modifier:` expression is replaced as well. */
    data class SetModifiers(val id: Int, val chain: List<ModifierEntry>?) : EditCommand() {
        override val mergeToken get() = Pair("modifier", id)
        override fun apply(document: DesignDocument) = document.mapNode(id) { node ->
            val entries = node.entries.toMutableList()
            val index = entries.indexOfFirst { it is NodeEntry.Modifier || it is NodeEntry.ModifierExpression }
            when {
                chain == null -> if (index >= 0) entries.removeAt(index)
                index >= 0 -> entries[index] = NodeEntry.Modifier(chain)
                else -> entries += NodeEntry.Modifier(chain)
            }
            node.copy(entries = entries, shorthand = false)
        }
    }

    /** Inserts [child] (re-numbered from the document's next id) at [index] of [parentId]'s primary children. */
    data class InsertChild(val parentId: Int, val index: Int, val child: DesignChild) : EditCommand() {
        override fun apply(document: DesignDocument): DesignDocument {
            val (allocated, next) = document.allocate(child)
            return document.mapNode(parentId) { parent ->
                val children = parent.children.toMutableList()
                children.add(index.coerceIn(0, children.size), allocated)
                parent.copy(children = children, shorthand = false)
            }.copy(nextId = next)
        }
    }

    data class RemoveChild(val parentId: Int, val index: Int) : EditCommand() {
        override fun apply(document: DesignDocument) = document.mapNode(parentId) { parent ->
            if (index !in parent.children.indices) parent else parent.copy(children = parent.children.filterIndexed { i, _ -> i != index })
        }
    }

    /** Removes the node [id] wherever it sits (primary child or slot); the root cannot be removed. */
    data class RemoveNode(val id: Int) : EditCommand() {
        override fun apply(document: DesignDocument): DesignDocument {
            val placement = document.parentOf(id) ?: return document
            return if (placement.slotName != null) SetSlot(placement.parentId, placement.slotName, null).apply(document)
            else RemoveChild(placement.parentId, placement.index).apply(document)
        }
    }

    /** Moves a primary child between parents or within one parent; [toIndex] refers to the target list after removal. */
    data class MoveChild(val fromParentId: Int, val fromIndex: Int, val toParentId: Int, val toIndex: Int) : EditCommand() {
        override fun apply(document: DesignDocument): DesignDocument {
            val from = document.node(fromParentId) ?: return document
            val child = from.children.getOrNull(fromIndex) ?: return document
            if (child is DesignChild.Node && (child.node.id == toParentId || child.node.descendants().any { it.id == toParentId })) return document
            val removed = RemoveChild(fromParentId, fromIndex).apply(document)
            return removed.mapNode(toParentId) { parent ->
                val children = parent.children.toMutableList()
                children.add(toIndex.coerceIn(0, children.size), child)
                parent.copy(children = children, shorthand = false)
            }
        }
    }

    data class SetHost(val kind: HostKind, val options: HostOptions?) : EditCommand() {
        override val mergeToken get() = "host"
        override fun apply(document: DesignDocument): DesignDocument {
            val mount = document.mount ?: return document
            return document.withMount(mount.copy(kind = kind, options = options))
        }
    }

    data class SetRenderPrelude(val prelude: String?) : EditCommand() {
        override val mergeToken get() = "renderPrelude"
        override fun apply(document: DesignDocument): DesignDocument {
            val mount = document.mount ?: return document
            return document.withMount(mount.copy(renderPrelude = prelude?.takeIf { it.isNotBlank() }))
        }
    }

    /** Replaces the state declarations; new declarations go where the first one was (or before the mount). */
    data class SetStates(val decls: List<StateDecl>) : EditCommand() {
        override fun apply(document: DesignDocument): DesignDocument {
            val remaining = document.segments.filterNot { it is Segment.State }.toMutableList()
            val firstState = document.segments.indexOfFirst { it is Segment.State }
            val insertAt = if (firstState >= 0) {
                document.segments.subList(0, firstState).count { it !is Segment.State }
            } else remaining.indexOfFirst { it is Segment.Mount }.takeIf { it >= 0 } ?: remaining.size
            remaining.addAll(insertAt, decls.map { Segment.State(it) })
            return document.copy(segments = remaining)
        }
    }
}

/** Undo / redo over immutable documents; merges consecutive steps that share a merge token (roadmap D40). */
class EditHistory(initial: DesignDocument, private val limit: Int = 200) {
    private class Step(val token: Any?, val before: DesignDocument, var after: DesignDocument, var commands: List<EditCommand>)

    private val undoStack = ArrayDeque<Step>()
    private val redoStack = ArrayDeque<Step>()
    var document: DesignDocument = initial
        private set
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size

    /** Applies [command]; returns true when the document changed. A no-op command records nothing. */
    fun execute(command: EditCommand, merge: Boolean = true): Boolean {
        val after = command.apply(document)
        if (after == document) return false
        redoStack.clear()
        val last = undoStack.lastOrNull()
        val token = command.mergeToken
        if (merge && token != null && last != null && last.token == token) {
            last.after = after
            last.commands = last.commands + command
        } else {
            undoStack.addLast(Step(token, document, after, listOf(command)))
            while (undoStack.size > limit) undoStack.removeFirst()
        }
        document = after
        return true
    }

    /** Ends the current merge run so that the next edit of the same target becomes its own step. */
    fun seal() {
        undoStack.lastOrNull()?.let { last -> if (last.token != null) undoStack[undoStack.lastIndex] = Step(null, last.before, last.after, last.commands) }
    }

    fun undo(): Boolean {
        val step = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(step)
        document = step.before
        return true
    }

    fun redo(): Boolean {
        val step = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(Step(null, step.before, step.after, step.commands))
        document = step.after
        return true
    }

    /** Replaces the current document without a history step (loading a file or a recovered draft). */
    fun reset(initial: DesignDocument) {
        undoStack.clear()
        redoStack.clear()
        document = initial
    }
}
