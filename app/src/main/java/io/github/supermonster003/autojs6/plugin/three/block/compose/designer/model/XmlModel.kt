package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

/**
 * The designer's model of a `"ui";` mode script (roadmap P10.1, decisions D46 / D49): the script file split into
 * opaque source text and the XML (E4X) literals it contains, each literal kept as a concrete syntax tree that
 * remembers its original text. Untouched nodes are written back byte for byte; only edited start tags, text
 * nodes and inserted elements are regenerated. Elements carry document-local ids like [DesignNode]s do.
 */

/** A piece of an attribute value, in source order; [Text] is raw (entities are not decoded). */
sealed class AttrPart {
    data class Text(val text: String) : AttrPart()
    /** `{{ expression }}`: the host evaluates it when the attribute is applied; [expression] is the text between the braces. */
    data class Binding(val expression: String) : AttrPart()
    /** An E4X `{expr}` fragment the Rhino parser reported inside the value; [source] is the whole `{...}` text. */
    data class Expr(val source: String) : AttrPart()

    val raw: String
        get() = when (this) {
            is Text -> text
            is Binding -> "{{$expression}}"
            is Expr -> source
        }
}

/**
 * One attribute of a start tag. [source] is the original text including the whitespace that precedes the name
 * (` id="txt"`), null once the attribute is edited or created; [parts] is null for a bare attribute (`<console global>`).
 */
data class XmlAttr(
    val name: String,
    val parts: List<AttrPart>?,
    val quote: Char? = '"',
    val source: String? = null,
) {
    /** The raw value text (entities kept), empty for a bare attribute. */
    val value: String get() = parts?.joinToString("") { it.raw }.orEmpty()
    val isBare: Boolean get() = parts == null
    val hasBinding: Boolean get() = parts?.any { it is AttrPart.Binding } == true
    val hasExpr: Boolean get() = parts?.any { it is AttrPart.Expr } == true

    companion object {
        /** Splits a raw attribute value into text and `{{...}}` binding parts the way the host's inflater finds them. */
        fun partsOf(raw: String): List<AttrPart> {
            val parts = ArrayList<AttrPart>()
            var cursor = 0
            while (cursor < raw.length) {
                val open = raw.indexOf("{{", cursor)
                if (open < 0) break
                val close = raw.indexOf("}}", open + 2)
                if (close < 0) break
                if (open > cursor) parts += AttrPart.Text(raw.substring(cursor, open))
                parts += AttrPart.Binding(raw.substring(open + 2, close))
                cursor = close + 2
            }
            if (cursor < raw.length) parts += AttrPart.Text(raw.substring(cursor))
            return parts
        }

        fun literal(name: String, value: String, quote: Char = '"') = XmlAttr(name, partsOf(value), quote)
        fun bare(name: String) = XmlAttr(name, null, null)
    }
}

sealed class XmlNode {
    /** The original text of this node, null when it was generated or edited (elements keep tag sources separately). */
    abstract val source: String?

    data class Element(
        val id: Int,
        val tag: String,
        val attrs: List<XmlAttr> = emptyList(),
        val children: List<XmlNode> = emptyList(),
        val selfClosing: Boolean = false,
        /** Text between the last attribute (or the tag name) and `>`, with the `/` of a self-closing tag (`""`, `" "`, `" /"`, `"/"`). */
        val startTagTail: String = if (selfClosing) "/" else "",
        /** Original start tag text (`<vertical padding="8">`), null when any part of the start tag changed. */
        val startTagSource: String? = null,
        /** Original end tag text (`</vertical>`), null for a self-closing or regenerated element. */
        val endTagSource: String? = null,
    ) : XmlNode() {
        override val source: String? get() = null

        fun attr(name: String): XmlAttr? = attrs.firstOrNull { it.name == name }
        val elementChildren: List<Element> get() = children.filterIsInstance<Element>()
        /** The concatenated text content when the element has no element children (`<text>Hello</text>`), else null. */
        val textContent: String?
            get() = if (children.any { it is Element || it is Expr }) null else children.filterIsInstance<Text>().joinToString("") { it.text }

        /** Every element of this subtree in preorder, which is also the order the host's converter walks the DOM. */
        fun descendants(): Sequence<Element> = sequence {
            yield(this@Element)
            for (child in children) if (child is Element) yieldAll(child.descendants())
        }
    }

    data class Text(val text: String) : XmlNode() {
        override val source: String get() = text
        val isBlank: Boolean get() = text.isBlank()
    }

    /** `<!-- ... -->`, kept whole. */
    data class Comment(override val source: String) : XmlNode()

    /** `<![CDATA[ ... ]]>`, kept whole. */
    data class CData(override val source: String) : XmlNode()

    /** `<? ... ?>`, kept whole. */
    data class Instruction(override val source: String) : XmlNode()

    /** An E4X `{expr}` in content position; the Rhino parser decides where these are. */
    data class Expr(override val source: String) : XmlNode()
}

/** How a literal is used by the script; decides which one the canvas shows first. */
enum class FragmentUsage {
    /** Passed to `ui.layout(...)` (directly or through one variable). */
    LAYOUT,
    /** Any other literal (`ui.inflate`, a template kept in a variable, ...). */
    OTHER,
}

/**
 * One XML literal of the script. [boundTo] is the variable the literal is assigned to (`var layout = <...>`), null
 * when it is written inline; [layoutCallOffset] is the offset of the `ui.layout` call that uses it, for ordering.
 * [stubOffset] (roadmap P10.4 / D52) is where a generated `ui.<id>.on(...)` stub goes, as an offset into the opaque
 * segment that follows the literal: after the `ui.layout(...)` statement and the handler statements already written
 * directly after it; -1 when the designer cannot place one. [stubIndent] is that statement's indentation.
 */
data class XmlFragment(
    val root: XmlNode.Element,
    val usage: FragmentUsage = FragmentUsage.OTHER,
    val boundTo: String? = null,
    val layoutCallOffset: Int = -1,
    val stubOffset: Int = -1,
    val stubIndent: String = "",
    /**
     * Set for a layout file the script refers to (roadmap P11.3 / D56) instead of a literal in it. Its stubs go into
     * the opaque segment [stubSegment] at [stubOffset], after the statement that loads the file.
     */
    val file: LayoutFileSource? = null,
    val stubSegment: Int = -1,
)

/**
 * A layout XML file a script refers to with a literal path (roadmap P11.3 / D56): `ui.layoutFile("...")` lays it
 * out, `ui.inflate(files.read("..."))` inflates it. [prefix] (byte order mark, XML declaration, comments) and
 * [suffix] around the root element are kept byte for byte; the model holds LF line breaks and [crlf] restores
 * CRLF when the file is written.
 */
data class LayoutFileSource(
    /** The path as the script writes it, relative to the script's directory unless absolute. */
    val path: String,
    val prefix: String,
    val suffix: String,
    val crlf: Boolean = false,
)

/**
 * Whether this fragment is native Android XML (roadmap P11.3): its root declares `xmlns:android` or uses `android:`
 * attributes. Ids are then written `android:id="@+id/..."` rather than AutoJs6's `id="..."`.
 */
val XmlFragment.isNative: Boolean
    get() = file != null || root.attrs.any { it.name == "xmlns:android" || it.name.startsWith("android:") }

/** The element's id name: the plain-text value of `id` or `android:id` without its `@+id/` / `@id/` prefix, or null. */
fun XmlNode.Element.idName(): String? = (attr("id") ?: attr("android:id"))
    ?.takeUnless { it.hasBinding || it.hasExpr }?.value?.removePrefix("@+id/")?.removePrefix("@id/")?.takeIf { it.isNotEmpty() }

/** A `ui.<id>.on('<event>', ...)` (or `ui.<id>.click(...)`) registration the script already contains. */
data class UiHandler(val id: String, val event: String)

sealed class UiSegment {
    /** Source text the designer does not interpret, in place. */
    data class Opaque(val source: String) : UiSegment()
    data class Layout(val fragment: XmlFragment) : UiSegment()
}

/**
 * A `"ui";` mode script: segments in file order, the index (among layout segments) of the canvas root, and the
 * next free element id. Ids are unique across all fragments.
 */
data class UiDocument(
    val segments: List<UiSegment>,
    val rootFragment: Int,
    val nextId: Int,
    /** Tag names the script registers with `ui.registerWidget(name, ...)`; their attributes are the script's business. */
    val widgets: Set<String> = emptySet(),
    /** Event handlers the script registers on elements by id (roadmap P10.4 / D52), so the inspector offers no second stub. */
    val handlers: Set<UiHandler> = emptySet(),
    /** The layout files the script refers to (roadmap P11.3), in the order of their first call; [fragments] lists them after the literals. */
    val files: List<XmlFragment> = emptyList(),
    /** Paths of referenced layout files that could not be read. */
    val missingFiles: List<String> = emptyList(),
    /** The script's `ui.useAndroidLayout(...)` mode: true / false, or null for automatic (also when it never calls it). */
    val androidLayout: Boolean? = null,
) {
    /** The ids (`id` or `android:id` values written as plain text) of every element, for generating a fresh one. */
    fun elementIds(): Set<String> = elements().mapNotNull { it.idName() }.toSet()

    /** Whether the fragment holding element [id] is native Android XML (roadmap P11.3), where ids are `android:id="@+id/..."`. */
    fun isNative(id: Int): Boolean = fragments.getOrNull(fragmentOf(id))?.isNative == true

    /** The literal fragments in script order, then the layout files ([files]). */
    val fragments: List<XmlFragment> get() = segments.filterIsInstance<UiSegment.Layout>().map { it.fragment } + files
    val root: XmlNode.Element? get() = fragments.getOrNull(rootFragment)?.root

    fun elements(): Sequence<XmlNode.Element> = fragments.asSequence().flatMap { it.root.descendants() }
    fun element(id: Int): XmlNode.Element? = elements().firstOrNull { it.id == id }

    /** The fragment index holding [id], or -1. */
    fun fragmentOf(id: Int): Int = fragments.indexOfFirst { fragment -> fragment.root.descendants().any { it.id == id } }

    /** The preorder index of [id] among the elements of its fragment (the host's DOM order), or -1. */
    fun elementIndexOf(id: Int): Int {
        val fragment = fragments.getOrNull(fragmentOf(id)) ?: return -1
        return fragment.root.descendants().indexOfFirst { it.id == id }
    }

    /** The element at preorder [index] of [fragment], or null. */
    fun elementAt(fragment: Int, index: Int): XmlNode.Element? = fragments.getOrNull(fragment)?.root?.descendants()?.elementAtOrNull(index)

    /** The parent element of [id] and the index of [id] in the parent's full [XmlNode.Element.children] list, or null for roots. */
    fun parentOf(id: Int): Placement? {
        for (parent in elements()) {
            val index = parent.children.indexOfFirst { it is XmlNode.Element && it.id == id }
            if (index >= 0) return Placement(parent.id, index)
        }
        return null
    }

    data class Placement(val parentId: Int, val childIndex: Int)

    /** Returns a copy whose element [id] is replaced by [transform]'s result; unchanged when [id] is absent. */
    fun mapElement(id: Int, transform: (XmlNode.Element) -> XmlNode.Element): UiDocument {
        var changed = false
        val segments = segments.map { segment ->
            if (segment !is UiSegment.Layout || changed) return@map segment
            val replaced = replaceIn(segment.fragment.root, id, transform)
            if (replaced === segment.fragment.root) segment else { changed = true; segment.copy(fragment = segment.fragment.copy(root = replaced)) }
        }
        if (changed) return copy(segments = segments)
        val files = files.map { fragment ->
            if (changed) return@map fragment
            val replaced = replaceIn(fragment.root, id, transform)
            if (replaced === fragment.root) fragment else { changed = true; fragment.copy(root = replaced) }
        }
        return if (changed) copy(files = files) else this
    }

    /** Gives [element] and its descendants fresh ids from [nextId]; returns the element and the next free id. */
    fun allocate(element: XmlNode.Element): Pair<XmlNode.Element, Int> {
        var next = nextId
        fun renumber(node: XmlNode): XmlNode = when (node) {
            is XmlNode.Element -> {
                val id = next++
                node.copy(id = id, children = node.children.map(::renumber))
            }
            else -> node
        }
        return (renumber(element) as XmlNode.Element) to next
    }

    private fun replaceIn(node: XmlNode.Element, id: Int, transform: (XmlNode.Element) -> XmlNode.Element): XmlNode.Element {
        if (node.id == id) return transform(node)
        var changed = false
        val children = node.children.map { child ->
            if (child !is XmlNode.Element || changed) return@map child
            val replaced = replaceIn(child, id, transform)
            if (replaced !== child) changed = true
            replaced
        }
        return if (changed) node.copy(children = children) else node
    }
}

/** XML entity helpers for the inspector: the model keeps text raw, people edit decoded text. */
object XmlEntities {
    private val named = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'")

    fun decode(raw: String): String {
        if ('&' !in raw) return raw
        val out = StringBuilder(raw.length)
        var cursor = 0
        while (cursor < raw.length) {
            val amp = raw.indexOf('&', cursor)
            if (amp < 0) { out.append(raw, cursor, raw.length); break }
            val semicolon = raw.indexOf(';', amp + 1)
            val entity = if (semicolon > amp) raw.substring(amp + 1, semicolon) else null
            val replacement = when {
                entity == null -> null
                entity.startsWith("#x") || entity.startsWith("#X") -> entity.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
                entity.startsWith("#") -> entity.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) }
                else -> named[entity]
            }
            out.append(raw, cursor, amp)
            if (replacement != null) { out.append(replacement); cursor = semicolon + 1 } else { out.append('&'); cursor = amp + 1 }
        }
        return out.toString()
    }

    /** Encodes [text] for an attribute value quoted with [quote]; `&`, `<`, `>` and the quote itself become entities. */
    fun encodeAttribute(text: String, quote: Char): String = buildString(text.length) {
        for (c in text) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            quote -> append(if (quote == '"') "&quot;" else "&apos;")
            else -> append(c)
        }
    }

    /** Encodes [text] for element content; `&`, `<` and `>` become entities. */
    fun encodeText(text: String): String = buildString(text.length) {
        for (c in text) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            else -> append(c)
        }
    }
}
