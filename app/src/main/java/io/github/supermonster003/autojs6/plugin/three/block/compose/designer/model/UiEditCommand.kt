package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

/**
 * Edits of a [UiDocument] (roadmap P10.1 / D49 / D40): pure functions from one immutable document to the next.
 * An edit touches only the start tag, text or position it is about: the start tag of an edited element loses its
 * source and is regenerated, untouched attributes keep theirs, inserted elements take the indentation of their
 * neighbours and removed elements take the whitespace before them along.
 */
sealed class UiEditCommand {
    /** Steps with the same non-null token merge while they follow each other directly. */
    open val mergeToken: Any? get() = null

    abstract fun apply(document: UiDocument): UiDocument

    /**
     * Sets attribute [name] to the raw value [value] (entities already encoded); null removes it, [bare] writes a
     * bare attribute (`<console global>`). A new attribute goes last (a new `id` first, where the host's samples
     * write it); an existing one keeps its place and quote.
     */
    data class SetAttr(val id: Int, val name: String, val value: String?, val bare: Boolean = false) : UiEditCommand() {
        override val mergeToken get() = Triple("attr", id, name)
        override fun apply(document: UiDocument) = document.mapElement(id) { element ->
            val attrs = element.attrs.toMutableList()
            val index = attrs.indexOfFirst { it.name == name }
            val raw = value
            fun add(attr: XmlAttr) = if (name == "id") attrs.add(0, attr) else attrs.add(attr)
            when {
                raw == null && !bare -> if (index >= 0) attrs.removeAt(index) else return@mapElement element
                bare -> if (index >= 0) attrs[index] = XmlAttr(name, null, null) else add(XmlAttr(name, null, null))
                raw == null -> return@mapElement element
                index >= 0 -> attrs[index] = XmlAttr(name, XmlAttr.partsOf(raw), quoteFor(raw, attrs[index].quote))
                else -> add(XmlAttr(name, XmlAttr.partsOf(raw), quoteFor(raw, null)))
            }
            element.copy(attrs = attrs, startTagSource = null)
        }

        private fun quoteFor(value: String, preferred: Char?): Char = when {
            preferred != null && preferred !in value -> preferred
            '"' !in value -> '"'
            '\'' !in value -> '\''
            else -> '"'
        }
    }

    /** Renames the element's tag; attributes and children stay. */
    data class SetTag(val id: Int, val tag: String) : UiEditCommand() {
        override fun apply(document: UiDocument) = document.mapElement(id) { element ->
            if (element.tag == tag) element else element.copy(tag = tag, startTagSource = null, endTagSource = null)
        }
    }

    /**
     * Replaces the text content of an element that has no element children (`<text>Hello</text>`); [text] is raw
     * (entities already encoded). An element with element children is left alone.
     */
    data class SetText(val id: Int, val text: String) : UiEditCommand() {
        override val mergeToken get() = Pair("text", id)
        override fun apply(document: UiDocument) = document.mapElement(id) { element ->
            if (element.children.any { it is XmlNode.Element || it is XmlNode.Expr }) return@mapElement element
            val kept = element.children.filterNot { it is XmlNode.Text }
            val children = if (text.isEmpty()) kept else listOf(XmlNode.Text(text)) + kept
            element.copy(children = children, selfClosing = false, startTagSource = if (element.selfClosing) null else element.startTagSource, startTagTail = if (element.selfClosing) "" else element.startTagTail)
        }
    }

    /**
     * Inserts [element] (renumbered from the document's next id) as the [index]-th element child of [parentId];
     * [index] counts element children only. A subtree without sources is laid out canonically with 4-space steps.
     */
    data class InsertElement(val parentId: Int, val index: Int, val element: XmlNode.Element) : UiEditCommand() {
        override fun apply(document: UiDocument): UiDocument {
            val (allocated, next) = document.allocate(element)
            return UiLayoutEdits.insert(document, parentId, index, allocated).copy(nextId = next)
        }
    }

    /** Removes element [id] with the whitespace before it; a fragment root cannot be removed. */
    data class RemoveElement(val id: Int) : UiEditCommand() {
        override fun apply(document: UiDocument): UiDocument = UiLayoutEdits.remove(document, id).first
    }

    /** Moves element [id] to be the [toIndex]-th element child of [toParentId] (counted after the removal). */
    data class MoveElement(val id: Int, val toParentId: Int, val toIndex: Int) : UiEditCommand() {
        override fun apply(document: UiDocument): UiDocument {
            val moving = document.element(id) ?: return document
            if (moving.descendants().any { it.id == toParentId }) return document
            val (removed, element) = UiLayoutEdits.remove(document, id)
            if (element == null) return document
            return UiLayoutEdits.insert(removed, toParentId, toIndex, element)
        }
    }

    /**
     * Appends a `ui.<id>.on('<event>', ...)` stub after the `ui.layout` statement of the element's fragment and the
     * handlers already written there (roadmap P10.4 / D52, option (b)); [assignId] first gives the element that id
     * when it has none. The stub is opaque script text afterwards: the designer neither shows nor edits it, and a
     * handler the script already registers for the same id and event is not added twice.
     */
    data class AddHandlerStub(val id: Int, val event: UiEvents.Event, val assignId: String? = null) : UiEditCommand() {
        override fun apply(document: UiDocument): UiDocument {
            val fragmentIndex = document.fragmentOf(id)
            if (document.fragments.getOrNull(fragmentIndex)?.stubOffset?.let { it < 0 } != false) return document
            val current = when {
                assignId == null -> document
                document.isNative(id) -> SetAttr(id, "android:id", "@+id/$assignId").apply(document)
                else -> SetAttr(id, "id", assignId).apply(document)
            }
            val element = current.element(id) ?: return document
            val elementId = element.idName()?.takeIf { IDENTIFIER.matches(it) } ?: return document
            val handler = UiHandler(elementId, event.name)
            if (handler in current.handlers) return document
            val fragment = current.fragments[fragmentIndex]
            // A literal's stubs go into the opaque text right after it; a layout file's into the text recorded with it (P11.3).
            val segmentIndex = if (fragment.file != null) {
                fragment.stubSegment
            } else {
                current.segments.indexOfFirst { it is UiSegment.Layout && it.fragment.root.id == fragment.root.id }.takeIf { it >= 0 }?.plus(1) ?: return document
            }
            val opaque = current.segments.getOrNull(segmentIndex) as? UiSegment.Opaque ?: return document
            val offset = fragment.stubOffset
            if (offset !in 0..opaque.source.length) return document
            val stub = UiEvents.stub(elementId, event, fragment.stubIndent)
            return insertStub(current, segmentIndex, offset, stub).copy(handlers = current.handlers + handler)
        }

        /** Inserts [stub] into the opaque segment [segmentIndex] at [offset]; every stub point there at or after it moves along. */
        private fun insertStub(document: UiDocument, segmentIndex: Int, offset: Int, stub: String): UiDocument {
            fun shifted(fragment: XmlFragment) = if (fragment.stubOffset >= offset) fragment.copy(stubOffset = fragment.stubOffset + stub.length) else fragment
            val segments = document.segments.mapIndexed { index, segment ->
                when {
                    index == segmentIndex && segment is UiSegment.Opaque ->
                        UiSegment.Opaque(segment.source.substring(0, offset) + stub + segment.source.substring(offset))
                    index == segmentIndex - 1 && segment is UiSegment.Layout && segment.fragment.stubOffset >= 0 -> segment.copy(fragment = shifted(segment.fragment))
                    else -> segment
                }
            }
            val files = document.files.map { if (it.stubSegment == segmentIndex && it.stubOffset >= 0) shifted(it) else it }
            return document.copy(segments = segments, files = files)
        }

        private companion object {
            val IDENTIFIER = Regex("""[A-Za-z_$][A-Za-z0-9_$]*""")
        }
    }

    /**
     * Replaces the whole document with [document], read back from a rewritten script (roadmap P11.3 / D66: moving a
     * literal into a layout file, adding a layout file); undo restores the previous document as it was.
     */
    data class ReplaceDocument(val document: UiDocument) : UiEditCommand() {
        override fun apply(document: UiDocument): UiDocument = this.document
    }
}

/** The whitespace bookkeeping behind insert, remove and move. */
internal object UiLayoutEdits {
    private const val STEP = "    "

    /** The indentation of element [id]: the text after the last newline in the whitespace before it. */
    fun indentOf(document: UiDocument, id: Int): String {
        val placement = document.parentOf(id)
        val before = if (placement != null) {
            val parent = document.element(placement.parentId) ?: return ""
            (parent.children.getOrNull(placement.childIndex - 1) as? XmlNode.Text)?.text
        } else {
            // A layout file's root follows the file's prefix (P11.3); a literal's root follows the script text before it.
            document.files.firstOrNull { it.root.id == id }?.file?.prefix ?: run {
                val index = document.segments.indexOfFirst { it is UiSegment.Layout && it.fragment.root.id == id }
                (document.segments.getOrNull(index - 1) as? UiSegment.Opaque)?.source
            }
        } ?: return ""
        val tail = before.substring(before.lastIndexOf('\n') + 1)
        return if (tail.isBlank()) tail else ""
    }

    fun insert(document: UiDocument, parentId: Int, index: Int, element: XmlNode.Element): UiDocument {
        val parent = document.element(parentId) ?: return document
        val elementChildren = parent.children.withIndex().filter { it.value is XmlNode.Element }
        val position = index.coerceIn(0, elementChildren.size)
        val indent = when {
            elementChildren.isNotEmpty() -> indentOf(document, (elementChildren[minOf(position, elementChildren.size - 1)].value as XmlNode.Element).id)
            else -> indentOf(document, parentId) + STEP
        }
        val laidOut = reindent(canonical(element, ""), "", indent)
        val children = parent.children.toMutableList()
        val insertion = listOf(XmlNode.Text("\n$indent"), laidOut)
        when {
            elementChildren.isEmpty() -> {
                val parentIndent = indentOf(document, parentId)
                // Only whitespace inside (`<vertical>\n</vertical>`): the new child replaces it rather than leaving a blank line.
                if (children.all { it is XmlNode.Text && it.isBlank }) children.clear()
                children += insertion
                children += XmlNode.Text("\n$parentIndent")
            }
            position < elementChildren.size -> {
                val childIndex = elementChildren[position].index
                val whitespaceBefore = (children.getOrNull(childIndex - 1) as? XmlNode.Text)?.isBlank == true
                children.addAll(if (whitespaceBefore) childIndex - 1 else childIndex, insertion)
            }
            else -> children.addAll(elementChildren.last().index + 1, insertion)
        }
        val reopened = parent.selfClosing
        return document.mapElement(parentId) {
            it.copy(
                children = children,
                selfClosing = false,
                startTagTail = if (reopened) "" else it.startTagTail,
                startTagSource = if (reopened) null else it.startTagSource,
                endTagSource = if (reopened) null else it.endTagSource,
            )
        }
    }

    /** Removes [id] and the blank text before it; returns the document and the removed element (with its old indentation applied). */
    fun remove(document: UiDocument, id: Int): Pair<UiDocument, XmlNode.Element?> {
        val placement = document.parentOf(id) ?: return document to null
        val parent = document.element(placement.parentId) ?: return document to null
        val element = parent.children[placement.childIndex] as XmlNode.Element
        val oldIndent = indentOf(document, id)
        val children = parent.children.toMutableList()
        children.removeAt(placement.childIndex)
        val before = children.getOrNull(placement.childIndex - 1)
        if (before is XmlNode.Text && before.isBlank && '\n' in before.text) children.removeAt(placement.childIndex - 1)
        val updated = document.mapElement(placement.parentId) { it.copy(children = children) }
        return updated to reindent(element, oldIndent, "")
    }

    /** Gives a source-less subtree the canonical shape: children on their own lines, leaf elements self-closing. */
    fun canonical(element: XmlNode.Element, indent: String): XmlNode.Element {
        if (element.startTagSource != null) return element
        val elements = element.children.filterIsInstance<XmlNode.Element>()
        val hasOnlyText = element.children.isNotEmpty() && elements.isEmpty()
        return when {
            element.children.isEmpty() -> element.copy(selfClosing = true, startTagTail = "/")
            hasOnlyText -> element.copy(selfClosing = false, startTagTail = "")
            else -> {
                val inner = "$indent$STEP"
                val children = ArrayList<XmlNode>()
                for (child in element.children) {
                    if (child is XmlNode.Text && child.isBlank) continue
                    children += XmlNode.Text("\n$inner")
                    children += if (child is XmlNode.Element) canonical(child, inner) else child
                }
                children += XmlNode.Text("\n$indent")
                element.copy(children = children, selfClosing = false, startTagTail = "")
            }
        }
    }

    /** Rewrites the indentation after every newline inside [node] from [old] to [new]; sources are kept otherwise. */
    fun reindent(node: XmlNode.Element, old: String, new: String): XmlNode.Element {
        if (old == new) return node
        fun text(value: String): String = value.replace("\n$old", "\n$new")
        fun visit(current: XmlNode): XmlNode = when (current) {
            is XmlNode.Element -> current.copy(
                attrs = current.attrs.map { attr -> attr.copy(source = attr.source?.let(::text)) },
                children = current.children.map(::visit),
                startTagTail = text(current.startTagTail),
                startTagSource = current.startTagSource?.let(::text),
            )
            is XmlNode.Text -> XmlNode.Text(text(current.text))
            is XmlNode.Comment -> XmlNode.Comment(text(current.source))
            is XmlNode.CData -> XmlNode.CData(text(current.source))
            is XmlNode.Instruction -> XmlNode.Instruction(text(current.source))
            is XmlNode.Expr -> XmlNode.Expr(text(current.source))
        }
        return visit(node) as XmlNode.Element
    }
}

/** Undo / redo over immutable [UiDocument]s, with the merge semantics of [EditHistory]. */
class UiEditHistory(initial: UiDocument, private val limit: Int = 200) {
    private class Step(val token: Any?, val before: UiDocument, var after: UiDocument, var commands: List<UiEditCommand>)

    private val undoStack = ArrayDeque<Step>()
    private val redoStack = ArrayDeque<Step>()
    var document: UiDocument = initial
        private set
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size

    fun execute(command: UiEditCommand, merge: Boolean = true): Boolean {
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

    fun reset(initial: UiDocument) {
        undoStack.clear()
        redoStack.clear()
        document = initial
    }
}
