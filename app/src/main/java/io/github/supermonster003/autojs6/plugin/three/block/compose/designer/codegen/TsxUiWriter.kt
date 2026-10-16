package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.AttrPart
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiSegment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlEntities
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlFragment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsxText

/**
 * Writes a TSX `"ui"` script back (roadmap P11.7 / D57). Opaque text is kept; a JSX layout that was not edited is
 * written as it was read, byte for byte; an edited one is written whole in the format of [TsxWriter]: four-space
 * indentation from the line the layout starts on, single-quoted strings, `{}` for expressions, each child on its
 * own line, a lone plain text child inline, empty elements self-closing. The model keeps attribute and text values
 * XML-encoded (as the XML reader does), so they are decoded here; a value a quoted JSX attribute or bare JSX text
 * could not hold is written as a string expression.
 */
object TsxUiWriter {
    private const val INDENT = "    "

    fun write(document: UiDocument): String = buildString {
        for (segment in document.segments) when (segment) {
            is UiSegment.Opaque -> append(segment.source)
            is UiSegment.Layout -> append(fragment(segment.fragment, indentBefore(this)))
        }
    }

    /** A layout: its original text while unedited, else the root written from [indent] (the indentation of its line). */
    fun fragment(fragment: XmlFragment, indent: String): String {
        val tsx = fragment.tsx
        if (tsx != null && tsx.root == fragment.root) return tsx.original
        return element(fragment.root, indent)
    }

    /** The element for [element], starting at the current position; nested lines are indented from [indent]. */
    fun element(element: XmlNode.Element, indent: String): String {
        val attributes = element.attrs.joinToString("") { " " + attribute(it, indent) }
        val children = element.children.filterNot { it is XmlNode.Text && XmlEntities.decode(it.text).isBlank() }
        if (children.isEmpty()) return "<${element.tag}$attributes />"
        val inline = (children.singleOrNull() as? XmlNode.Text)?.let { XmlEntities.decode(it.text) }?.takeIf { JsxText.isSafeChild(it) }
        if (inline != null) return "<${element.tag}$attributes>$inline</${element.tag}>"
        val inner = indent + INDENT
        val lines = ArrayList<String>()
        var previousBare = false
        children.forEach { child ->
            when (child) {
                is XmlNode.Element -> { lines += inner + element(child, inner); previousBare = false }
                is XmlNode.Text -> {
                    val text = XmlEntities.decode(child.text)
                    // Two bare text lines in a row would read back as one text joined by a space.
                    val bare = !previousBare && JsxText.isSafeChild(text)
                    lines += inner + if (bare) text else "{" + ScriptWriter.quote(text) + "}"
                    previousBare = bare
                }
                is XmlNode.Expr -> { lines += inner + reindent(child.source, inner); previousBare = false }
                else -> { lines += inner + "{/* " + child.source.orEmpty().replace("*/", "* /") + " */}"; previousBare = false }
            }
        }
        return "<${element.tag}$attributes>\n" + lines.joinToString("\n") + "\n" + indent + "</${element.tag}>"
    }

    /** `name`, `name='value'`, `name={'value'}` or `name={expression}`. */
    fun attribute(attr: XmlAttr, indent: String): String {
        val parts = attr.parts ?: return attr.name
        val expression = parts.singleOrNull() as? AttrPart.Expr
        if (expression != null) return attr.name + "=" + reindent(expression.source, indent)
        val value = XmlEntities.decode(attr.value)
        return attr.name + "=" + if (JsxText.isSafeAttribute(value)) "'$value'" else "{" + ScriptWriter.quote(value) + "}"
    }

    /**
     * [source] (a `{...}` container, maybe over several lines) with its continuation lines moved so that their common
     * indentation becomes [indent]: an expression keeps its own shape wherever the element is written.
     */
    fun reindent(source: String, indent: String): String {
        val lines = source.split('\n')
        if (lines.size == 1) return source
        val rest = lines.drop(1)
        val common = rest.filter { it.isNotBlank() }.minOfOrNull { line -> line.takeWhile { it == ' ' || it == '\t' }.length } ?: 0
        return lines.first() + rest.joinToString("") { line -> "\n" + if (line.isBlank()) "" else indent + line.drop(common) }
    }

    /** The indentation of the line [text] currently ends on, as far as it is only spaces and tabs. */
    private fun indentBefore(text: CharSequence): String {
        val lineStart = text.lastIndexOf('\n') + 1
        val line = text.substring(lineStart)
        return line.takeWhile { it == ' ' || it == '\t' }
    }

    private fun CharSequence.lastIndexOf(char: Char): Int {
        for (i in length - 1 downTo 0) if (this[i] == char) return i
        return -1
    }
}
