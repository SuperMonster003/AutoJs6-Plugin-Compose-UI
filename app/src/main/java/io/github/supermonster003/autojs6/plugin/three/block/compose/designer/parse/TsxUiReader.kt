package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.AttrPart
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.FragmentUsage
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.TsxFragmentSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEvents
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiHandler
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiSegment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlEntities
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlFragment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import java.util.IdentityHashMap

/**
 * Reads a TSX `"ui"` script into a [UiDocument] (roadmap P11.7 / D57), from the TypeScript Engine parser's tree
 * ([TsTree]); nothing is compiled or evaluated. The JSX handed to `ui.layout(...)` / `ui.inflate(...)` (directly or
 * through one `const` / `let` variable) becomes layout fragments; the first one `ui.layout` lays out is the canvas
 * root; everything else is opaque text in place. Elements map the way the host's TSX runtime turns JSX into layout
 * XML: a quoted attribute is a string, `{expr}` an expression (literals are noted for the preview), a bare one
 * `true`; text children stay text (the runtime joins them into `text`); fragments are flattened. The model keeps
 * values XML-encoded like the XML reader does. Handlers are `ui.<id>.on(...)`, `(ui as any).<id>.on(...)`, the
 * `click` / `longClick` shortcuts and `ui.findView('<id>')?.click(...)`; an inline `onClick` is an attribute.
 */
class TsxUiReader {

    /** Maps [tree], the parser's tree of exactly [source] (already passed through [TsxReader.normalize]), to a document. */
    fun read(source: String, tree: TsTree): UiDocument = Session(source, tree).read()

    private class Session(private val source: String, private val tree: TsTree) {
        private var nextId = 1
        private val parents = IdentityHashMap<TsNode, TsNode>()
        private val literalValues = LinkedHashMap<String, String>()

        /** A JSX layout candidate: the JSX node and the variable it initializes, if any. */
        private class Root(val node: TsNode, val boundTo: String?) {
            var usage = FragmentUsage.OTHER
            var layoutCallOffset = -1
            var stubOffset = -1
            var stubIndent = ""
        }

        fun read(): UiDocument {
            tree.diagnostics.firstOrNull { it.category == "error" }?.let { fail(it.start, it.message) }
            if (tree.root.end != source.length) fail(0, "The syntax tree does not belong to this source")
            val statements = tree.root.nodes("statements")
            val first = statements.firstOrNull()
            val directive = first?.takeIf { it.kind == "ExpressionStatement" }?.node("expression")?.takeIf { it.kind == "StringLiteral" }
            if (directive?.text != "ui") fail(first?.start ?: 0, "The script does not start with the \"ui\" directive")

            val calls = ArrayList<TsNode>()
            val initializers = HashMap<String, MutableList<Root>>()
            val roots = ArrayList<Root>()
            val assigned = HashSet<String>()
            val widgets = LinkedHashSet<String>()
            val handlers = LinkedHashSet<UiHandler>()
            var androidLayoutCall: TsNode? = null
            visit(tree.root, null) { node ->
                when (node.kind) {
                    "CallExpression" -> when (uiMethod(node)) {
                        "layout", "inflate" -> calls += node
                        "useAndroidLayout" -> if (androidLayoutCall.let { it == null || node.start < it.start }) androidLayoutCall = node
                        "registerWidget" -> node.nodes("arguments").firstOrNull()?.takeIf { it.kind == "StringLiteral" }?.text?.let { widgets += it }
                        "layoutFile" -> fail(node, "Layout files are not supported in TSX scripts yet; use ui.layout(<...>)")
                        else -> handlerOf(node)?.let { handlers += it }
                    }
                    "VariableDeclaration" -> {
                        val name = node.node("name")?.takeIf { it.kind == "Identifier" }?.text
                        val initializer = node.node("initializer")?.let(::unwrap)
                        if (name != null && initializer != null && isLayoutJsx(initializer)) {
                            val root = Root(initializer, name)
                            roots += root
                            initializers.getOrPut(name) { ArrayList() } += root
                        }
                    }
                    "BinaryExpression" -> if (node.node("operatorToken")?.kind == "EqualsToken") node.node("left")?.takeIf { it.kind == "Identifier" }?.text?.let { assigned += it }
                }
            }

            // The JSX of every ui.layout / ui.inflate call, and the variable a ui.layout lays out.
            calls.sortBy { it.start }
            var rootLayout: Root? = null
            for (call in calls) {
                val method = uiMethod(call)
                val argument = call.nodes("arguments").firstOrNull()?.let(::unwrap)
                    ?: fail(call, "ui.$method needs a layout")
                if (method == "layout" && call.nodes("arguments").size != 1) fail(call, "ui.layout takes exactly one argument")
                val root = when {
                    isLayoutJsx(argument) -> roots.firstOrNull { it.node === argument } ?: Root(argument, null).also { roots += it }
                    argument.kind == "Identifier" && method == "layout" -> {
                        val name = argument.text!!
                        val candidates = initializers[name].orEmpty()
                        when {
                            candidates.isEmpty() -> fail(argument, "ui.layout($name): the variable is not initialized with a JSX layout")
                            candidates.size > 1 -> fail(argument, "ui.layout($name): the variable is initialized with more than one JSX layout")
                            name in assigned -> fail(argument, "ui.layout($name): the variable is reassigned after its JSX layout")
                            else -> candidates[0]
                        }
                    }
                    // Compose JSX handed to ui.layout fails when it runs (the host's TSX runtime refuses it); it stays opaque here.
                    argument.kind.startsWith("Jsx") -> continue
                    method == "layout" -> fail(argument, "ui.layout needs a JSX layout or a variable holding one")
                    else -> continue
                }
                if (method == "layout" && root.usage == FragmentUsage.OTHER) {
                    root.usage = FragmentUsage.LAYOUT
                    root.layoutCallOffset = call.start
                    // Roadmap P10.4 / D52, D68: handler stubs go after this statement and the handlers already behind it.
                    statementOf(call)?.let { statement ->
                        val end = handlersEndAfter(statement)
                        if (end >= root.node.end && roots.none { it !== root && it.node.start >= root.node.end && it.node.start < end }) {
                            root.stubOffset = end - root.node.end
                            root.stubIndent = indentOf(statement.start)
                        }
                    }
                }
                if (method == "layout" && rootLayout == null) rootLayout = root
            }

            // Layouts nested in another one (JSX inside an attribute expression) stay part of that one's expression text.
            val outer = roots.sortedBy { it.node.start }.fold(ArrayList<Root>()) { kept, root ->
                if (kept.none { root.node.start >= it.node.start && root.node.end <= it.node.end }) kept += root
                kept
            }
            val segments = ArrayList<UiSegment>()
            var cursor = 0
            var rootIndex = -1
            outer.forEachIndexed { index, root ->
                if (root.node.start > cursor) segments += UiSegment.Opaque(source.substring(cursor, root.node.start))
                val element = layoutRoot(root.node)
                val fragment = XmlFragment(
                    element, root.usage, root.boundTo, root.layoutCallOffset, root.stubOffset, root.stubIndent,
                    tsx = TsxFragmentSource(source.substring(root.node.start, root.node.end), element),
                )
                segments += UiSegment.Layout(fragment)
                if (root === rootLayout) rootIndex = index
                cursor = root.node.end
            }
            if (cursor < source.length) segments += UiSegment.Opaque(source.substring(cursor))
            if (rootIndex < 0) fail(0, "The script has no ui.layout(...) call with a JSX layout")
            return UiDocument(
                segments, rootIndex, nextId, widgets, handlers, androidLayout = androidLayoutOf(androidLayoutCall),
                dialect = Dialect.TSX, literalValues = literalValues,
            )
        }

        // ---------------------------------------------------------------- tree helpers

        private fun visit(node: TsNode, parent: TsNode?, action: (TsNode) -> Unit) {
            if (parent != null) parents[node] = parent
            action(node)
            node.children.forEach { visit(it, node, action) }
        }

        private fun unwrap(node: TsNode): TsNode = if (node.kind == "ParenthesizedExpression") unwrap(node.node("expression") ?: node) else node

        /** The method of a `ui.<method>(...)` call, or null. */
        private fun uiMethod(call: TsNode): String? {
            val target = call.node("expression")?.takeIf { it.kind == "PropertyAccessExpression" } ?: return null
            if (target.node("expression")?.takeIf { it.kind == "Identifier" }?.text != "ui") return null
            return target.node("name")?.text
        }

        /** True for `ui` and `(ui as any)` / `(<any>ui)`. */
        private fun isUi(node: TsNode?): Boolean {
            val inner = node?.let(::unwrap) ?: return false
            return when (inner.kind) {
                "Identifier" -> inner.text == "ui"
                "AsExpression", "TypeAssertionExpression", "SatisfiesExpression" -> isUi(inner.node("expression"))
                else -> false
            }
        }

        /** The handler [call] registers, or null (see the class comment for the forms). */
        private fun handlerOf(call: TsNode): UiHandler? {
            val target = call.node("expression")?.takeIf { it.kind == "PropertyAccessExpression" } ?: return null
            val method = target.node("name")?.text ?: return null
            val receiver = target.node("expression")?.let(::unwrap) ?: return null
            val id = when {
                receiver.kind == "PropertyAccessExpression" && isUi(receiver.node("expression")) -> receiver.node("name")?.text
                // ui.findView('<id>')?.click(...)
                receiver.kind == "CallExpression" -> receiver.takeIf { uiMethod(it) == "findView" }?.nodes("arguments")?.firstOrNull()
                    ?.takeIf { it.kind == "StringLiteral" }?.text
                else -> null
            } ?: return null
            val arguments = call.nodes("arguments")
            return when (method) {
                "on", "once", "addListener", "prependListener", "prependOnceListener" ->
                    arguments.firstOrNull()?.takeIf { it.kind == "StringLiteral" }?.text?.let { UiHandler(id, it) }
                else -> UiEvents.ofShortcut(method)?.takeIf { arguments.isNotEmpty() }?.let { UiHandler(id, it) }
            }
        }

        /** The statement holding [node] in a statement list; null inside an expression-bodied arrow function or after `return`. */
        private fun statementOf(node: TsNode): TsNode? {
            var current = node
            while (true) {
                val parent = parents[current] ?: return null
                if (parent.kind in STATEMENT_LISTS && parent.nodes("statements").any { it === current }) {
                    return current.takeUnless { it.kind == "ReturnStatement" }
                }
                if (parent.kind == "ArrowFunction" && parent.node("body") === current) return null
                current = parent
            }
        }

        /** The end of [statement], extended over the handler statements that directly follow it in the same list. */
        private fun handlersEndAfter(statement: TsNode): Int {
            var end = statement.end
            val siblings = parents[statement]?.nodes("statements") ?: return end
            val index = siblings.indexOfFirst { it === statement }
            if (index < 0) return end
            for (i in index + 1 until siblings.size) {
                val call = siblings[i].takeIf { it.kind == "ExpressionStatement" }?.node("expression")?.takeIf { it.kind == "CallExpression" } ?: break
                if (handlerOf(call) == null) break
                end = siblings[i].end
            }
            return end
        }

        /** The whitespace that starts the line of [offset], when nothing but whitespace precedes [offset] on it. */
        private fun indentOf(offset: Int): String {
            val lineStart = source.lastIndexOf('\n', offset - 1) + 1
            val prefix = source.substring(lineStart, offset)
            return if (prefix.isBlank()) prefix else ""
        }

        /** `ui.useAndroidLayout()` and `(true)` keep native XML, `(false)` converts it, anything else is automatic. */
        private fun androidLayoutOf(call: TsNode?): Boolean? {
            call ?: return null
            val argument = call.nodes("arguments").firstOrNull() ?: return true
            return when (argument.kind) {
                "TrueKeyword" -> true
                "FalseKeyword" -> false
                else -> null
            }
        }

        // ---------------------------------------------------------------- JSX

        private fun opening(element: TsNode): TsNode = if (element.kind == "JsxElement") element.node("openingElement")!! else element

        /** The tag of an element: a name, a dotted class name or `ns:name`; null when it is not one. */
        private fun tagOf(element: TsNode): String? {
            val tag = opening(element).node("tagName") ?: return null
            return when (tag.kind) {
                "Identifier" -> tag.text
                "JsxNamespacedName" -> tag.node("namespace")?.text?.let { ns -> tag.node("name")?.text?.let { "$ns:$it" } }
                "PropertyAccessExpression" -> dottedName(tag)
                else -> null
            }
        }

        private fun dottedName(node: TsNode): String? = when (node.kind) {
            "Identifier" -> node.text
            "PropertyAccessExpression" -> dottedName(node.node("expression") ?: return null)?.let { left -> node.node("name")?.text?.let { "$left.$it" } }
            else -> null
        }

        /** A compose element (`compose.X`, `$compose.X`, `compose:X`), which a ui layout cannot hold. */
        private fun isCompose(tag: String?): Boolean = tag != null && (tag.startsWith("compose.") || tag.startsWith("\$compose.") || tag.startsWith("compose:"))

        /** A JSX layout: an element with a ui tag, or a fragment whose first element has one. */
        private fun isLayoutJsx(node: TsNode): Boolean = when (node.kind) {
            "JsxElement", "JsxSelfClosingElement" -> tagOf(node).let { it != null && !isCompose(it) }
            "JsxFragment" -> node.nodes("children").firstOrNull { it.kind == "JsxElement" || it.kind == "JsxSelfClosingElement" || it.kind == "JsxFragment" }?.let(::isLayoutJsx) == true
            else -> false
        }

        /** The root element of a layout: the element itself, or the only element of a fragment (as the host requires). */
        private fun layoutRoot(node: TsNode): XmlNode.Element {
            if (node.kind != "JsxFragment") return element(node)
            val children = children(node.nodes("children"))
            val elements = children.filterIsInstance<XmlNode.Element>()
            if (elements.size != 1 || children.any { it is XmlNode.Text }) fail(node, "A ui layout needs exactly one root element")
            return elements[0]
        }

        private fun element(node: TsNode): XmlNode.Element {
            val tag = tagOf(node) ?: fail(node, "This element tag cannot be used in a ui layout")
            if (isCompose(tag)) fail(node, "Compose elements cannot appear in a ui layout; use compose.attach for them")
            val opening = opening(node)
            opening.nodes("typeArguments").firstOrNull()?.let { fail(it, "The designer cannot keep type arguments on an element") }
            val id = nextId++
            val attrs = ArrayList<XmlAttr>()
            opening.node("attributes")?.nodes("properties")?.forEach { attribute ->
                if (attribute.kind == "JsxSpreadAttribute") fail(attribute, "Spread attributes cannot be represented in the designer")
                val nameNode = attribute.node("name")!!
                val name = when (nameNode.kind) {
                    "JsxNamespacedName" -> nameNode.node("namespace")?.text + ":" + nameNode.node("name")?.text
                    else -> nameNode.text!!
                }
                if (attrs.any { it.name == name }) fail(attribute, "$name is given twice")
                attrs += attribute(attribute, name)
            }
            val children = if (node.kind == "JsxElement") children(node.nodes("children")) else emptyList()
            return XmlNode.Element(id, tag, attrs, children, selfClosing = node.kind == "JsxSelfClosingElement")
        }

        private fun attribute(attribute: TsNode, name: String): XmlAttr {
            val initializer = attribute.node("initializer") ?: return XmlAttr.bare(name)
            return when (initializer.kind) {
                "StringLiteral" -> {
                    val value = JsxText.attributeValue(source.substring(initializer.start + 1, initializer.end - 1)) { entity -> fail(initializer, "$entity is not a valid character") }
                    XmlAttr.literal(name, XmlEntities.encodeAttribute(value, '"'))
                }
                "JsxExpression" -> {
                    val expression = initializer.node("expression") ?: fail(attribute, "An attribute expression cannot be empty")
                    if (initializer.has("dotDotDotToken")) fail(attribute, "Spread attributes cannot be represented in the designer")
                    // A string expression is a string, as the writer puts one a quoted attribute cannot hold.
                    stringOf(expression)?.let { return XmlAttr.literal(name, XmlEntities.encodeAttribute(it, '"')) }
                    val container = source.substring(initializer.start, initializer.end)
                    literalOf(expression)?.let { literalValues[container.removePrefix("{").removeSuffix("}").trim()] = it }
                    XmlAttr(name, listOf(AttrPart.Expr(container)), '"')
                }
                // An element as the value (<a b=<c/>/>) is an expression too.
                else -> XmlAttr(name, listOf(AttrPart.Expr("{" + source.substring(initializer.start, initializer.end) + "}")), '"')
            }
        }

        private fun children(children: List<TsNode>): List<XmlNode> {
            val out = ArrayList<XmlNode>()
            children.forEach { child ->
                when (child.kind) {
                    "JsxText" -> {
                        if (child.triviaOnly) return@forEach
                        val value = JsxText.childValue(child.text ?: "") { entity -> fail(child, "$entity is not a valid character") } ?: return@forEach
                        out += XmlNode.Text(XmlEntities.encodeText(value))
                    }
                    "JsxExpression" -> {
                        val container = source.substring(child.start, child.end)
                        val expression = child.node("expression")
                        // A string expression is text, as the writer puts text that bare JSX text cannot hold.
                        val string = expression?.takeUnless { child.has("dotDotDotToken") || source.substring(child.start + 1, child.end - 1).trim() != slice(it) }?.let(::stringOf)
                        if (string != null) {
                            out += XmlNode.Text(XmlEntities.encodeText(string))
                            return@forEach
                        }
                        expression?.let { literalOf(it)?.let { value -> literalValues[container.removePrefix("{").removeSuffix("}").trim()] = value } }
                        out += XmlNode.Expr(container)
                    }
                    "JsxFragment" -> out += children(child.nodes("children"))
                    "JsxElement", "JsxSelfClosingElement" -> out += element(child)
                    else -> fail(child, "Unexpected JSX child")
                }
            }
            return out
        }

        /** The value of a string literal expression (`'a'`, `"a"`, `` `a` `` without substitutions), else null. */
        private fun stringOf(expression: TsNode): String? = unwrap(expression).takeIf { it.kind == "StringLiteral" || it.kind == "NoSubstitutionTemplateLiteral" }?.text

        private fun slice(node: TsNode): String = source.substring(node.start, node.end).trim()

        /** The string the host's TSX runtime makes of a literal expression (a string, a finite number, a boolean), else null. */
        private fun literalOf(expression: TsNode): String? {
            val node = unwrap(expression)
            return when (node.kind) {
                "StringLiteral", "NoSubstitutionTemplateLiteral" -> node.text
                "NumericLiteral" -> node.text?.let(::numberText)
                "TrueKeyword" -> "true"
                "FalseKeyword" -> "false"
                "PrefixUnaryExpression" -> if (node.operator == "-" || node.operator == "MinusToken") node.node("operand")?.takeIf { it.kind == "NumericLiteral" }?.text?.let(::numberText)?.let { "-$it" } else null
                else -> null
            }
        }

        /** JavaScript's ToString of a numeric literal's value for the plain cases (integers and short decimals). */
        private fun numberText(text: String): String? {
            val value = text.replace("_", "").let { literal ->
                when {
                    literal.startsWith("0x", true) -> literal.substring(2).toLongOrNull(16)?.toDouble()
                    literal.startsWith("0o", true) -> literal.substring(2).toLongOrNull(8)?.toDouble()
                    literal.startsWith("0b", true) -> literal.substring(2).toLongOrNull(2)?.toDouble()
                    else -> literal.toDoubleOrNull()
                }
            } ?: return null
            if (!value.isFinite()) return null
            if (value == Math.rint(value) && kotlin.math.abs(value) < 1e15) return value.toLong().toString()
            val plain = value.toBigDecimal().stripTrailingZeros().toPlainString()
            return plain.takeIf { it.length <= 21 }
        }

        // ---------------------------------------------------------------- positions

        private fun fail(node: TsNode?, reason: String): Nothing = fail(node?.start ?: 0, reason)

        private fun fail(offset: Int, reason: String): Nothing {
            val bounded = offset.coerceIn(0, source.length)
            throw DesignerParseError(SourceText.lineOf(source, bounded), SourceText.columnOf(source, bounded), reason)
        }

        companion object {
            /** Nodes whose `statements` a stub can follow: the script, blocks and module / case bodies. */
            val STATEMENT_LISTS = setOf("SourceFile", "Block", "ModuleBlock", "CaseClause", "DefaultClause")
        }
    }
}
