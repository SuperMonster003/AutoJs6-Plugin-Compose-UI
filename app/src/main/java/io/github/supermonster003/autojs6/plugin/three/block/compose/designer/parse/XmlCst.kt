package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.AttrPart
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode

/** A literal the designer cannot map; [offset] is relative to the literal text given to [XmlCst.parse]. */
internal class XmlCstError(val offset: Int, val reason: String) : RuntimeException(reason)

/**
 * A tolerant, position-keeping parser for one E4X XML literal (roadmap P10.1 / D49). It only needs to accept what
 * the Rhino lexer already accepted and to remember every byte: whitespace, comments, attribute order and quotes
 * stay in the tree as node and attribute sources. `{expr}` fragments are not scanned; their ranges come from the
 * Rhino AST ([ExprRange]) and become [XmlNode.Expr] / [AttrPart.Expr]. `{{ }}` bindings are plain text to this
 * parser and are split off by [XmlAttr.partsOf]. No entity decoding and no namespace semantics.
 */
internal object XmlCst {

    /** An E4X expression inside the literal: [start] at `{`, [end] after `}`, offsets relative to the literal text. */
    data class ExprRange(val start: Int, val end: Int, val inAttribute: Boolean)

    /** Parses [text] (the whole literal, from `<` to the last `>`); element ids come from [nextId] in preorder. */
    fun parse(text: String, exprRanges: List<ExprRange> = emptyList(), nextId: () -> Int): XmlNode.Element =
        Scanner(text, exprRanges.sortedBy { it.start }, nextId).parseDocument()

    /** A layout file split around its root element: [prefix] and [suffix] are kept byte for byte. */
    data class FileParts(val prefix: String, val root: XmlNode.Element, val suffix: String)

    /**
     * Parses a whole layout file (roadmap P11.3): an optional byte order mark, XML declaration, comments and whitespace
     * before one root element, comments and whitespace after it. A layout file has no E4X expressions, so `{` is text.
     */
    fun parseFile(text: String, nextId: () -> Int): FileParts = Scanner(text, emptyList(), nextId).parseFile()

    private class Scanner(private val text: String, private val exprs: List<ExprRange>, private val nextId: () -> Int) {
        private var pos = 0
        private var exprCursor = 0

        fun parseDocument(): XmlNode.Element {
            if (text.startsWith("<>")) fail(0, "XML lists (<>...</>) are not supported")
            if (!text.startsWith("<")) fail(0, "An XML literal starts with <")
            val root = parseElement()
            val restStart = pos
            if (text.substring(pos).isNotBlank()) fail(restStart, "Only one root element is allowed in a layout literal")
            return root
        }

        fun parseFile(): FileParts {
            if (text.firstOrNull()?.code == BYTE_ORDER_MARK) pos = 1
            while (true) {
                skipWhitespace()
                when {
                    pos >= text.length -> fail(pos, "The layout file has no root element")
                    at("<?") -> readUntil("?>", "Unterminated XML declaration")
                    at("<!--") -> readUntil("-->", "Unterminated comment")
                    at("<!") -> fail(pos, "Document type declarations are not supported in a layout file")
                    at("<") -> break
                    else -> fail(pos, "Text before the root element")
                }
            }
            val prefix = text.substring(0, pos)
            val root = parseElement()
            val suffixStart = pos
            while (true) {
                skipWhitespace()
                when {
                    pos >= text.length -> break
                    at("<!--") -> readUntil("-->", "Unterminated comment")
                    at("<?") -> readUntil("?>", "Unterminated processing instruction")
                    else -> fail(pos, "Only one root element is allowed in a layout file")
                }
            }
            return FileParts(prefix, root, text.substring(suffixStart))
        }

        private fun exprAt(offset: Int): ExprRange? {
            while (exprCursor < exprs.size && exprs[exprCursor].end <= offset) exprCursor++
            val range = exprs.getOrNull(exprCursor) ?: return null
            return if (range.start == offset) range else null
        }

        private fun nextExprStart(from: Int): Int {
            var i = exprCursor
            while (i < exprs.size && exprs[i].start < from) i++
            return exprs.getOrNull(i)?.start ?: text.length
        }

        private fun parseElement(): XmlNode.Element {
            val start = pos
            if (!at("<")) fail(pos, "Expected <")
            pos++
            val tag = readName() ?: fail(pos, "Expected a tag name")
            val attrs = ArrayList<XmlAttr>()
            var tail: String
            var selfClosing: Boolean
            while (true) {
                val wsStart = pos
                skipWhitespace()
                val ws = text.substring(wsStart, pos)
                when {
                    pos >= text.length -> fail(start, "Unterminated start tag <$tag>")
                    at("/>") -> { tail = "$ws/"; selfClosing = true; pos += 2; break }
                    at(">") -> { tail = ws; selfClosing = false; pos++; break }
                    exprAt(pos) != null -> fail(pos, "Expressions are not supported in a start tag")
                    else -> {
                        val name = readName() ?: fail(pos, "Expected an attribute name in <$tag>")
                        skipWhitespace()
                        val attr = if (at("=")) {
                            pos++
                            skipWhitespace()
                            val quote = text.getOrNull(pos)
                            if (quote != '"' && quote != '\'') fail(pos, "Expected a quoted value for $name")
                            pos++
                            val parts = readQuotedValue(quote, name)
                            XmlAttr(name, parts, quote, text.substring(wsStart, pos))
                        } else {
                            XmlAttr(name, null, null, text.substring(wsStart, pos))
                        }
                        if (attrs.any { it.name == name }) fail(wsStart, "Duplicate attribute $name in <$tag>")
                        attrs += attr
                    }
                }
            }
            val startTagSource = text.substring(start, pos)
            val id = nextId()
            if (selfClosing) {
                return XmlNode.Element(id, tag, attrs, emptyList(), true, tail, startTagSource, null)
            }
            val children = ArrayList<XmlNode>()
            while (true) {
                if (pos >= text.length) fail(start, "Missing end tag </$tag>")
                val expr = exprAt(pos)
                when {
                    expr != null -> { children += XmlNode.Expr(text.substring(expr.start, expr.end)); pos = expr.end }
                    at("</") -> break
                    at("<!--") -> children += XmlNode.Comment(readUntil("-->", "Unterminated comment"))
                    at("<![CDATA[") -> children += XmlNode.CData(readUntil("]]>", "Unterminated CDATA section"))
                    at("<?") -> children += XmlNode.Instruction(readUntil("?>", "Unterminated processing instruction"))
                    at("<") -> children += parseElement()
                    else -> {
                        val textStart = pos
                        val limit = minOf(text.indexOf('<', pos).let { if (it < 0) text.length else it }, nextExprStart(pos))
                        pos = limit
                        children += XmlNode.Text(text.substring(textStart, pos))
                    }
                }
            }
            val endStart = pos
            pos += 2
            val endName = readName() ?: fail(endStart, "Expected a tag name in the end tag")
            skipWhitespace()
            if (!at(">")) fail(pos, "Expected > after </$endName")
            pos++
            if (endName != tag) fail(endStart, "End tag </$endName> does not match <$tag>")
            return XmlNode.Element(id, tag, attrs, children, false, tail, startTagSource, text.substring(endStart, pos))
        }

        private fun readQuotedValue(quote: Char, attrName: String): List<AttrPart> {
            val parts = ArrayList<AttrPart>()
            val buffer = StringBuilder()
            while (true) {
                if (pos >= text.length) fail(pos, "Unterminated value of $attrName")
                val expr = exprAt(pos)
                if (expr != null) {
                    if (buffer.isNotEmpty()) { parts += XmlAttr.partsOf(buffer.toString()); buffer.setLength(0) }
                    parts += AttrPart.Expr(text.substring(expr.start, expr.end))
                    pos = expr.end
                    continue
                }
                val c = text[pos]
                if (c == quote) break
                buffer.append(c)
                pos++
            }
            if (buffer.isNotEmpty()) parts += XmlAttr.partsOf(buffer.toString())
            pos++
            return parts
        }

        private fun readUntil(terminator: String, reason: String): String {
            val start = pos
            val end = text.indexOf(terminator, pos)
            if (end < 0) fail(start, reason)
            pos = end + terminator.length
            return text.substring(start, pos)
        }

        private fun readName(): String? {
            val start = pos
            while (pos < text.length && isNameChar(text[pos], pos == start)) pos++
            return if (pos > start) text.substring(start, pos) else null
        }

        private fun isNameChar(c: Char, first: Boolean): Boolean =
            c.isLetter() || c == '_' || c == ':' || (!first && (c.isDigit() || c == '-' || c == '.'))

        private fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        private fun at(token: String): Boolean = text.startsWith(token, pos)

        private fun fail(offset: Int, reason: String): Nothing = throw XmlCstError(offset, reason)
    }

    private const val BYTE_ORDER_MARK = 0xFEFF
}
