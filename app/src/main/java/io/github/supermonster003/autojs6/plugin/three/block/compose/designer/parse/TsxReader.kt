package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HandlerSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostKind
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostOptions
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierArg
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.MountDecl
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Segment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.StateDecl
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.v2.ComposeUiV2

/**
 * Reads a TSX compose script into a [DesignDocument] (roadmap P9.2 / D42). The syntax tree comes from the
 * TypeScript Engine parser service (contract v1, [TsTree]); nothing is compiled or evaluated. The mapping
 * follows [ScriptReader]: top-level `compose.state` declarations and the single mount / floaty / dialog call
 * are interpreted, everything else is kept as opaque source text by range. Inside the mount, JSX elements
 * whose tag is `compose.X` or `compose:X` are nodes; attributes become keys, properties, slots, handlers and
 * modifier chains; text children become shorthand Text nodes with the value TypeScript's JSX transform gives
 * them; fragments are flattened; other tags and `{}` expressions stay opaque children. Anything that cannot be
 * kept is refused with a position.
 */
class TsxReader(private val catalog: ComponentCatalog = ComposeUiV2.interopCatalog) {

    /** Maps [tree], the parser's tree of exactly [source] (already passed through [normalize]), to a document. */
    fun read(source: String, tree: TsTree): DesignDocument = Session(source, tree, catalog).read()

    companion object {
        /** File name sent to the parser for documents; the extension selects TSX parsing. */
        const val FILE_NAME = "designer.tsx"

        /** The text the parser must see: line endings normalized the way the JavaScript reader does. */
        fun normalize(source: String): String = source.replace("\r\n", "\n").replace('\r', '\n')

        /** Source that wraps a handler the inspector edited, for a syntax check through the parser. */
        fun handlerProbe(params: List<String>, body: String): String =
            "(function (" + params.joinToString(", ") + ") {\n" + normalize(body) + "\n});\n"

        /** The first syntax error of a [handlerProbe] tree, positioned in the body text (line 1 is its first line). */
        fun handlerError(probe: String, tree: TsTree): DesignerParseError? {
            val diagnostic = tree.diagnostics.firstOrNull { it.category == "error" } ?: return null
            val offset = diagnostic.start.coerceIn(0, probe.length)
            return DesignerParseError((SourceText.lineOf(probe, offset) - 1).coerceAtLeast(1), SourceText.columnOf(probe, offset), diagnostic.message)
        }
    }

    private class Session(private val source: String, private val tree: TsTree, private val catalog: ComponentCatalog) {
        private var nextId = 1
        private val stateNames = HashSet<String>()

        fun read(): DesignDocument {
            tree.diagnostics.firstOrNull { it.category == "error" }?.let { fail(it.start, it.message) }
            if (tree.root.end != source.length) fail(0, "The syntax tree does not belong to this source")
            val statements = tree.root.nodes("statements")
            statements.forEach { statement -> stateOf(statement)?.let { stateNames += it.name } }
            val recognized = ArrayList<Triple<Int, Int, Segment>>()
            var mountSeen: TsNode? = null
            statements.forEach { statement ->
                val mountCall = mountOf(statement)
                when {
                    mountCall != null -> {
                        mountSeen?.let { first -> fail(statement, "The designer edits one mount, floaty or dialog per script; the first one is at line ${SourceText.lineOf(source, first.start)}") }
                        mountSeen = statement
                        recognized += Triple(statement.start, statement.end, Segment.Mount(mountCall))
                    }
                    else -> stateOf(statement)?.let { recognized += Triple(statement.start, statement.end, Segment.State(it)) }
                }
            }
            if (mountSeen == null) fail(0, "The script has no compose.mount, compose.floaty or compose.dialog call")
            val segments = ArrayList<Segment>()
            var cursor = 0
            recognized.sortedBy { it.first }.forEach { (start, end, segment) ->
                opaque(source.substring(cursor, start))?.let { segments += it }
                segments += segment
                cursor = end
            }
            opaque(source.substring(cursor))?.let { segments += it }
            return DesignDocument(segments, nextId, Dialect.TSX)
        }

        // ---------------------------------------------------------------- positions

        private fun fail(node: TsNode?, reason: String): Nothing = fail(node?.start ?: 0, reason)

        private fun fail(offset: Int, reason: String): Nothing {
            val bounded = offset.coerceIn(0, source.length)
            throw DesignerParseError(SourceText.lineOf(source, bounded), SourceText.columnOf(source, bounded), reason)
        }

        private fun slice(node: TsNode): String = source.substring(node.start.coerceIn(0, source.length), node.end.coerceIn(node.start, source.length)).trim()

        private fun opaque(text: String): Segment.Opaque? {
            val trimmed = text.trim('\n', ' ', '\t')
            return trimmed.takeIf { it.isNotEmpty() }?.let { Segment.Opaque(it) }
        }

        private fun unwrap(node: TsNode): TsNode = if (node.kind == "ParenthesizedExpression") unwrap(node.node("expression") ?: node) else node

        // ---------------------------------------------------------------- top level

        private fun stateOf(statement: TsNode): StateDecl? {
            if (statement.kind != "VariableStatement" || statement.has("modifiers")) return null
            val list = statement.node("declarationList") ?: return null
            val keyword = list.declarationKind?.takeIf { it in DECLARATION_KEYWORDS } ?: return null
            val declaration = list.nodes("declarations").singleOrNull() ?: return null
            // An annotated variable (`const n: State<number> = ...`) cannot be written back from the model: opaque.
            if (declaration.has("type") || declaration.has("exclamationToken")) return null
            val name = declaration.node("name")?.takeIf { it.kind == "Identifier" }?.text ?: return null
            val call = declaration.node("initializer")?.takeIf { it.kind == "CallExpression" } ?: return null
            if (composeMethod(call) != "state") return null
            val arguments = call.nodes("arguments")
            if (arguments.size > 1) fail(arguments[1], "compose.state takes one initial value")
            val typeArguments = call.nodes("typeArguments").takeIf { it.isNotEmpty() }?.joinToString(", ") { slice(it) }
            val initial = arguments.firstOrNull()?.let { propValue(it) } ?: PropValue.Expression("undefined")
            return StateDecl(keyword, name, initial, typeArguments)
        }

        private fun mountOf(statement: TsNode): MountDecl? {
            when (statement.kind) {
                "VariableStatement" -> {
                    val list = statement.node("declarationList") ?: return null
                    val declaration = list.nodes("declarations").singleOrNull() ?: return null
                    val call = declaration.node("initializer")?.takeIf { it.kind == "CallExpression" } ?: return null
                    val kind = HostKind.byMethod(composeMethod(call) ?: return null) ?: return null
                    if (statement.has("modifiers")) fail(statement, "The designer cannot keep modifiers such as export on the mount declaration")
                    val keyword = list.declarationKind?.takeIf { it in DECLARATION_KEYWORDS } ?: fail(statement, "The mount result must be declared with var, let or const")
                    val target = declaration.node("name")
                    val name = target?.takeIf { it.kind == "Identifier" }?.text ?: fail(target ?: declaration, "The mount result must be bound to a plain variable")
                    declaration.node("type")?.let { fail(it, "The designer cannot keep a type annotation on the mount variable") }
                    return mount(call, kind, keyword, name)
                }
                "ExpressionStatement" -> {
                    val expression = statement.node("expression") ?: return null
                    if (expression.kind == "CallExpression") {
                        val kind = HostKind.byMethod(composeMethod(expression) ?: return null) ?: return null
                        return mount(expression, kind, null, null)
                    }
                    if (expression.kind == "BinaryExpression" && expression.node("operatorToken")?.kind == "EqualsToken") {
                        val call = expression.node("right")?.takeIf { it.kind == "CallExpression" } ?: return null
                        val kind = HostKind.byMethod(composeMethod(call) ?: return null) ?: return null
                        val left = expression.node("left")
                        val name = left?.takeIf { it.kind == "Identifier" }?.text ?: fail(left ?: expression, "The mount result must be assigned to a plain variable")
                        return mount(call, kind, "", name)
                    }
                    return null
                }
                else -> return null
            }
        }

        private fun mount(call: TsNode, kind: HostKind, bindKeyword: String?, bindName: String?): MountDecl {
            call.nodes("typeArguments").firstOrNull()?.let { fail(it, "The designer cannot keep type arguments on compose.${kind.method}") }
            val arguments = call.nodes("arguments")
            if (arguments.isEmpty()) fail(call, "compose.${kind.method} needs the interface to show")
            if (arguments.size > 2) fail(arguments[2], "compose.${kind.method} takes the interface and an optional options object")
            val spec = unwrap(arguments[0])
            var render = false
            var arrow = false
            var prelude: String? = null
            val root = when (spec.kind) {
                "ArrowFunction", "FunctionExpression" -> {
                    render = true
                    arrow = spec.kind == "ArrowFunction"
                    spec.nodes("parameters").firstOrNull()?.let { fail(it, "A render function takes no parameters") }
                    unkeptFunctionPart(spec)?.let { (node, what) -> fail(node, "The designer cannot keep $what on the render function") }
                    val body = spec.node("body") ?: fail(spec, "The render function has no body")
                    if (body.kind != "Block") {
                        rootNode(body)
                    } else {
                        val statements = body.nodes("statements")
                        val last = statements.lastOrNull()?.takeIf { it.kind == "ReturnStatement" }
                            ?: fail(spec, "The render function must end with return <element>")
                        val returned = last.node("expression") ?: fail(last, "return must produce the root element")
                        if (statements.size > 1) {
                            var start = statements.first().start.coerceIn(0, source.length)
                            val lineStart = source.lastIndexOf('\n', start - 1) + 1
                            if (source.substring(lineStart, start).isBlank()) start = lineStart
                            prelude = SourceText.dedent(source.substring(start, statements[statements.size - 2].end.coerceAtLeast(start)))
                                .takeIf { it.isNotBlank() }
                        }
                        rootNode(returned)
                    }
                }
                in JSX_KINDS -> rootNode(spec)
                else -> fail(spec, "The interface must be a render function or a JSX element such as <compose.Column>")
            }
            val options = arguments.getOrNull(1)?.let { option ->
                if (option.kind == "ObjectLiteralExpression") HostOptions.Fields(option.nodes("properties").map { property ->
                    when (property.kind) {
                        "PropertyAssignment" -> {
                            val key = property.node("name")
                            (key?.let { keyName(it) } ?: fail(key ?: property, "Option keys must be plain names")) to propValue(property.node("initializer")!!)
                        }
                        "ShorthandPropertyAssignment" -> {
                            val key = property.node("name")!!
                            key.text!! to PropValue.Expression(key.text)
                        }
                        "SpreadAssignment" -> fail(property, "Options cannot use spread syntax")
                        else -> fail(property, "Options must be plain name: value pairs")
                    }
                }) else HostOptions.Expression(slice(option))
            }
            return MountDecl(kind, root, bindKeyword, bindName, render, arrow, prelude, options)
        }

        /** A part of a function the model has no place for, with its description; null when there is none. */
        private fun unkeptFunctionPart(function: TsNode): Pair<TsNode, String>? {
            function.nodes("modifiers").firstOrNull()?.let { return it to "the ${slice(it)} modifier" }
            function.node("asteriskToken")?.let { return it to "a generator" }
            function.nodes("typeParameters").firstOrNull()?.let { return it to "type parameters" }
            function.node("type")?.let { return it to "a return type" }
            return null
        }

        private fun rootNode(expression: TsNode): DesignNode {
            val value = unwrap(expression)
            val element = if (value.kind == "JsxFragment") {
                val children = value.nodes("children").filterNot { isEmptyChild(it) }
                children.singleOrNull() ?: fail(value, "The root fragment must hold exactly one compose element")
            } else value
            if (element.kind != "JsxElement" && element.kind != "JsxSelfClosingElement" || componentName(element) == null) {
                fail(element, "The root must be a compose element such as <compose.Column>")
            }
            return node(element)
        }

        // ---------------------------------------------------------------- elements

        private fun composeMethod(call: TsNode): String? {
            if (call.has("questionDotToken")) return null
            val target = call.node("expression")?.takeIf { it.kind == "PropertyAccessExpression" && !it.has("questionDotToken") } ?: return null
            val receiver = target.node("expression")?.takeIf { it.kind == "Identifier" }?.text ?: return null
            if (receiver != "compose" && receiver != "\$compose") return null
            return target.node("name")?.text
        }

        private fun opening(element: TsNode): TsNode = if (element.kind == "JsxElement") element.node("openingElement")!! else element

        /** The component name of a `compose.X` / `compose:X` element, null for any other tag. */
        private fun componentName(element: TsNode): String? {
            val tag = opening(element).node("tagName") ?: return null
            val name = when (tag.kind) {
                "PropertyAccessExpression" -> {
                    val receiver = tag.node("expression")?.takeIf { it.kind == "Identifier" }?.text
                    if (receiver == "compose" || receiver == "\$compose") tag.node("name")?.text else null
                }
                "JsxNamespacedName" -> if (tag.node("namespace")?.text == "compose") tag.node("name")?.text else null
                else -> null
            } ?: return null
            if (name in RESERVED) fail(tag, "compose.$name is not a component")
            return name
        }

        /** True for a lowercase or namespaced tag other than compose: an XML layout element, which Compose rejects. */
        private fun isXmlTag(element: TsNode): Boolean {
            val tag = opening(element).node("tagName") ?: return false
            return when (tag.kind) {
                "Identifier" -> tag.text?.firstOrNull()?.isLowerCase() == true || tag.text?.contains('-') == true
                "JsxNamespacedName" -> true
                else -> false
            }
        }

        private fun node(element: TsNode): DesignNode {
            val type = componentName(element) ?: fail(element, "Not a compose element")
            val opening = opening(element)
            opening.nodes("typeArguments").firstOrNull()?.let { fail(it, "The designer cannot keep type arguments on an element") }
            val id = nextId++
            val entries = ArrayList<NodeEntry>()
            var childrenProp: List<DesignChild>? = null
            var childrenAttribute: TsNode? = null
            val slotNames = catalog.component(type)?.slots?.map { it.name }?.toSet() ?: emptySet()
            opening.node("attributes")?.nodes("properties")?.forEach { attribute ->
                if (attribute.kind == "JsxSpreadAttribute") fail(attribute, "Spread attributes cannot be represented in the designer")
                val nameNode = attribute.node("name")!!
                if (nameNode.kind != "Identifier") fail(nameNode, "Namespaced attribute names are not supported")
                val name = nameNode.text!!
                val initializer = attribute.node("initializer")
                val value = attributeValue(attribute, initializer)
                when {
                    name == NodeEntry.KEY -> entries += NodeEntry.Key(value.prop())
                    name == NodeEntry.MODIFIER -> entries += (value.expression?.let { modifierEntry(it) } ?: fail(attribute, "modifier must be a compose.modifier() chain or an expression"))
                    name == "children" -> {
                        if (childrenProp != null) fail(attribute, "children is given twice")
                        childrenAttribute = attribute
                        childrenProp = value.expression?.let { children(it) }
                            ?: value.string?.let { listOf(DesignChild.Node(DesignNode.text(nextId++, it))) }
                            ?: listOf(DesignChild.Expression("true"))
                    }
                    name == "slots" -> {
                        val slots = value.expression?.takeIf { it.kind == "ObjectLiteralExpression" } ?: fail(attribute, "slots must be an object")
                        slots.nodes("properties").forEach { property ->
                            if (property.kind != "PropertyAssignment") fail(property, "slots must hold plain name: value pairs")
                            val key = property.node("name")
                            val slotName = key?.let { keyName(it) } ?: fail(key ?: property, "Slot names must be plain names")
                            entries += NodeEntry.Slot(slotName, child(AttributeValue(expression = property.node("initializer")!!)))
                        }
                    }
                    value.expression != null && isEventName(name) && isFunction(value.expression) ->
                        entries += handler(value.expression)?.let { NodeEntry.Handler(name, it) } ?: NodeEntry.Prop(name, PropValue.Expression(slice(value.expression)))
                    name in slotNames -> entries += NodeEntry.Slot(name, child(value))
                    else -> entries += NodeEntry.Prop(name, value.prop())
                }
            }
            val nested = if (element.kind == "JsxElement") jsxChildren(element.nodes("children")) else emptyList()
            if (childrenProp != null && nested.isNotEmpty()) fail(childrenAttribute, "children cannot be both an attribute and nested elements")
            return DesignNode(id, type, entries, childrenProp ?: nested)
        }

        /** A JSX attribute value: a quoted string, an expression (from `{}` or a bare element), or nothing (`true`). */
        private inner class AttributeValue(val string: String? = null, val expression: TsNode? = null) {
            fun prop(): PropValue = when {
                string != null -> PropValue.str(string)
                expression != null -> propValue(expression)
                else -> PropValue.bool(true)
            }
        }

        private fun attributeValue(attribute: TsNode, initializer: TsNode?): AttributeValue = when {
            initializer == null -> AttributeValue()
            initializer.kind == "StringLiteral" -> AttributeValue(string = JsxText.attributeValue(rawString(initializer)) { entity -> fail(initializer, "$entity is not a valid character") })
            initializer.kind == "JsxExpression" -> AttributeValue(expression = initializer.node("expression") ?: fail(attribute, "An attribute expression cannot be empty"))
            else -> AttributeValue(expression = initializer)
        }

        /** The characters between the quotes of a JSX attribute string, which keeps backslashes as written. */
        private fun rawString(literal: TsNode): String = source.substring(literal.start + 1, literal.end - 1)

        private fun isFunction(node: TsNode): Boolean = node.kind == "ArrowFunction" || node.kind == "FunctionExpression"

        private fun isEmptyChild(child: TsNode): Boolean = when (child.kind) {
            "JsxText" -> child.triviaOnly || JsxText.childValue(child.text ?: "") { entity -> fail(child, "$entity is not a valid character") } == null
            "JsxExpression" -> child.node("expression") == null && innerText(child).isEmpty()
            else -> false
        }

        /** The text between the braces of a JSX expression container, trimmed. */
        private fun innerText(container: TsNode): String = source.substring(container.start + 1, container.end - 1).trim()

        private fun jsxChildren(children: List<TsNode>): List<DesignChild> {
            val out = ArrayList<DesignChild>()
            children.forEach { child ->
                when (child.kind) {
                    "JsxText" -> {
                        if (child.triviaOnly) return@forEach
                        val value = JsxText.childValue(child.text ?: "") { entity -> fail(child, "$entity is not a valid character") } ?: return@forEach
                        out += DesignChild.Node(DesignNode.text(nextId++, value))
                    }
                    "JsxExpression" -> {
                        val inner = innerText(child)
                        val expression = child.node("expression")
                        when {
                            inner.isEmpty() -> Unit
                            // Comments or a spread around the expression are kept by keeping the whole container text.
                            expression == null || child.has("dotDotDotToken") || inner != slice(expression) -> out += DesignChild.Expression(inner)
                            else -> out += expressionChildren(expression)
                        }
                    }
                    "JsxFragment" -> out += jsxChildren(child.nodes("children"))
                    "JsxElement", "JsxSelfClosingElement" -> out += elementChild(child)
                    else -> fail(child, "Unexpected JSX child")
                }
            }
            return out
        }

        private fun elementChild(element: TsNode): DesignChild = when {
            componentName(element) != null -> DesignChild.Node(node(element))
            isXmlTag(element) -> fail(element, "An XML layout element cannot appear inside a Compose interface")
            else -> DesignChild.Expression(slice(element))
        }

        /** The children an expression in a child position contributes, as in the JavaScript reader. */
        private fun expressionChildren(expression: TsNode): List<DesignChild> = when (expression.kind) {
            "JsxElement", "JsxSelfClosingElement" -> listOf(elementChild(expression))
            "JsxFragment" -> jsxChildren(expression.nodes("children"))
            "StringLiteral" -> listOf(DesignChild.Node(DesignNode.text(nextId++, expression.text ?: "")))
            else -> listOf(DesignChild.Expression(slice(expression)))
        }

        /** The value of the `children` attribute: elements, strings and arrays of them, or an opaque expression. */
        private fun children(value: TsNode): List<DesignChild> = when (value.kind) {
            "ArrayLiteralExpression" -> value.nodes("elements").flatMap { children(it) }
            else -> expressionChildren(value)
        }

        /** A slot value: one element (a single-element fragment counts), a string, or an opaque expression. */
        private fun child(value: AttributeValue): DesignChild {
            value.string?.let { return DesignChild.Node(DesignNode.text(nextId++, it)) }
            val expression = value.expression ?: return DesignChild.Expression("true")
            return when (expression.kind) {
                "JsxElement", "JsxSelfClosingElement" -> elementChild(expression)
                "JsxFragment" -> {
                    val inner = expression.nodes("children").filterNot { isEmptyChild(it) }
                    val only = inner.singleOrNull()
                    if (only != null && (only.kind == "JsxElement" || only.kind == "JsxSelfClosingElement") && componentName(only) != null) DesignChild.Node(node(only))
                    else DesignChild.Expression(slice(expression))
                }
                "StringLiteral" -> DesignChild.Node(DesignNode.text(nextId++, expression.text ?: ""))
                else -> DesignChild.Expression(slice(expression))
            }
        }

        private fun modifierEntry(value: TsNode): NodeEntry {
            val operations = ArrayList<ModifierEntry>()
            var current: TsNode = value
            while (true) {
                if (current.kind != "CallExpression" || current.has("questionDotToken") || current.has("typeArguments")) return NodeEntry.ModifierExpression(slice(value))
                val target = current.node("expression")?.takeIf { it.kind == "PropertyAccessExpression" && !it.has("questionDotToken") }
                    ?: return NodeEntry.ModifierExpression(slice(value))
                val operation = target.node("name")?.takeIf { it.kind == "Identifier" }?.text ?: return NodeEntry.ModifierExpression(slice(value))
                val receiver = target.node("expression")!!
                if (receiver.kind == "Identifier" && (receiver.text == "compose" || receiver.text == "\$compose")) {
                    if (operation != "modifier" || current.nodes("arguments").isNotEmpty()) return NodeEntry.ModifierExpression(slice(value))
                    break
                }
                operations += ModifierEntry(operation, current.nodes("arguments").map { argument ->
                    val callback = if (isFunction(argument)) handler(argument) else null
                    if (callback != null) ModifierArg.Callback(callback) else ModifierArg.Value(propValue(argument))
                })
                current = receiver
            }
            return NodeEntry.Modifier(operations.reversed())
        }

        /** The handler form of a function, or null when it has parts the model cannot keep (it then stays an expression). */
        private fun handler(function: TsNode): HandlerSource? {
            if (unkeptFunctionPart(function) != null || function.has("name")) return null
            val params = function.nodes("parameters").map { slice(it) }
            val arrow = function.kind == "ArrowFunction"
            val body = function.node("body") ?: return null
            if (body.kind != "Block") return HandlerSource(params, slice(body), arrow = true, expressionBody = true)
            val inner = source.substring(body.start + 1, body.end - 1)
            return HandlerSource(params, SourceText.dedent(inner), arrow)
        }

        private fun propValue(value: TsNode): PropValue {
            literal(value)?.let { return PropValue.Literal(it) }
            if (value.kind == "PropertyAccessExpression" && !value.has("questionDotToken")) {
                val receiver = value.node("expression")
                if (receiver?.kind == "Identifier" && receiver.text in stateNames && value.node("name")?.text == "value") return PropValue.StateRef(receiver.text!!)
            }
            return PropValue.Expression(slice(value))
        }

        private fun literal(value: TsNode): JsLiteral? = when (value.kind) {
            "StringLiteral" -> JsLiteral.Str(value.text ?: "")
            "NumericLiteral" -> number(value)?.let { JsLiteral.Num(it, slice(value)) }
            "PrefixUnaryExpression" -> {
                val operand = value.node("operand")
                if (value.operator == "MinusToken" && operand?.kind == "NumericLiteral") number(operand)?.let { JsLiteral.Num(-it, "-" + slice(operand)) } else null
            }
            "TrueKeyword" -> JsLiteral.Bool(true)
            "FalseKeyword" -> JsLiteral.Bool(false)
            "NullKeyword" -> JsLiteral.Null
            "ArrayLiteralExpression" -> JsLiteral.Arr(value.nodes("elements").map { literal(it) ?: return null })
            "ObjectLiteralExpression" -> JsLiteral.Obj(value.nodes("properties").map { property ->
                if (property.kind != "PropertyAssignment") return null
                val name = property.node("name")?.let { keyName(it) } ?: return null
                name to (literal(property.node("initializer") ?: return null) ?: return null)
            })
            else -> null
        }

        /** TypeScript gives a numeric literal's value as decimal text ("1000" for `1e3`, "16" for `0x10`). */
        private fun number(literal: TsNode): Double? = literal.text?.toDoubleOrNull()

        private fun keyName(key: TsNode): String? = when (key.kind) {
            "Identifier", "StringLiteral" -> key.text
            "NumericLiteral" -> slice(key)
            else -> null
        }

        private fun isEventName(name: String): Boolean = name.length > 2 && name.startsWith("on") && name[2].isUpperCase()

        companion object {
            private val DECLARATION_KEYWORDS = setOf("var", "let", "const")
            private val JSX_KINDS = setOf("JsxElement", "JsxSelfClosingElement", "JsxFragment")

            /** compose members that are not component factories (the same set as the JavaScript reader). */
            private val RESERVED = setOf(
                "modifier", "state", "ref", "mount", "floaty", "dialog", "attach", "memo", "batch", "post", "theme",
                "createElement", "version", "isAvailable", "sessions", "closeAll",
            )
        }
    }
}
