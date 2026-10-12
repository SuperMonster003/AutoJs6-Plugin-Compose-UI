package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
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
import org.mozilla.javascript.CompilerEnvirons
import org.mozilla.javascript.Context
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.Parser
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast.ArrayLiteral
import org.mozilla.javascript.ast.Assignment
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.ErrorCollector
import org.mozilla.javascript.ast.ExpressionStatement
import org.mozilla.javascript.ast.FunctionCall
import org.mozilla.javascript.ast.FunctionNode
import org.mozilla.javascript.ast.KeywordLiteral
import org.mozilla.javascript.ast.Name
import org.mozilla.javascript.ast.NumberLiteral
import org.mozilla.javascript.ast.ObjectLiteral
import org.mozilla.javascript.ast.ObjectProperty
import org.mozilla.javascript.ast.ParseProblem
import org.mozilla.javascript.ast.PropertyGet
import org.mozilla.javascript.ast.ReturnStatement
import org.mozilla.javascript.ast.StringLiteral
import org.mozilla.javascript.ast.UnaryExpression
import org.mozilla.javascript.ast.VariableDeclaration

/** A script the designer refuses to open; [line] and [column] are 1-based positions in the given source. */
class DesignerParseError(val line: Int, val column: Int, val reason: String) : RuntimeException("$line:$column: $reason")

/**
 * Reads a compose script into a [DesignDocument] (roadmap P8.2 / D35). The script is parsed with the host's
 * own Rhino `Parser` into an AST and never evaluated: no Context is entered, nothing is compiled. Only the
 * top-level state declarations and the single mount / floaty / dialog call are interpreted; every other
 * statement is kept as opaque source text in place, and property values the designer cannot represent as
 * literals stay opaque expressions. Anything the tree cannot express structurally is rejected with a
 * position instead of being silently dropped.
 */
class ScriptReader(private val catalog: ComponentCatalog = ComposeUiV2.interopCatalog) {

    fun read(source: String): DesignDocument = Session(source.replace("\r\n", "\n").replace('\r', '\n'), catalog).read()

    private class Session(private val source: String, private val catalog: ComponentCatalog) {
        private var nextId = 1
        private val stateNames = HashSet<String>()

        fun read(): DesignDocument {
            val root = parseAst()
            val statements = root.statements
            statements.forEach { statement -> stateOf(statement)?.let { stateNames += it.name } }
            val recognized = ArrayList<Triple<Int, Int, Segment>>()
            var mountSeen: AstNode? = null
            statements.forEach { statement ->
                val mountCall = mountOf(statement)
                when {
                    mountCall != null -> {
                        if (mountSeen != null) fail(statement, "The designer edits one mount, floaty or dialog per script; the first one is at line ${lineOf(mountSeen!!.absolutePosition)}")
                        mountSeen = statement
                        recognized += Triple(statement.absolutePosition, statementEnd(statement), Segment.Mount(mountCall))
                    }
                    else -> stateOf(statement)?.let { recognized += Triple(statement.absolutePosition, statementEnd(statement), Segment.State(it)) }
                }
            }
            if (mountSeen == null) fail(1, 1, "The script has no compose.mount, compose.floaty or compose.dialog call")
            val segments = ArrayList<Segment>()
            var cursor = 0
            recognized.sortedBy { it.first }.forEach { (start, end, segment) ->
                opaque(source.substring(cursor, start))?.let { segments += it }
                segments += segment
                cursor = end
            }
            opaque(source.substring(cursor))?.let { segments += it }
            return DesignDocument(segments, nextId)
        }

        // ---------------------------------------------------------------- Rhino

        private fun parseAst(): org.mozilla.javascript.ast.AstRoot {
            val environment = CompilerEnvirons().apply {
                languageVersion = Context.VERSION_ES6
                isIdeMode = true
                setRecordingComments(false)
                setRecordingLocalJsDocComments(false)
            }
            val collector = ErrorCollector()
            environment.errorReporter = collector
            fun collected(): DesignerParseError? = collector.errors.firstOrNull { it.type == ParseProblem.Type.Error }?.let { problem ->
                val offset = problem.fileOffset.coerceIn(0, source.length)
                DesignerParseError(lineOf(offset), columnOf(offset), problem.message ?: "Syntax error")
            }
            val root = try {
                Parser(environment, collector).parse(source, "designer.js", 1)
            } catch (error: EvaluatorException) {
                throw collected() ?: DesignerParseError(error.lineNumber().coerceAtLeast(1), error.columnNumber().coerceAtLeast(1), error.details() ?: "Syntax error")
            } catch (error: RuntimeException) {
                throw collected() ?: DesignerParseError(1, 1, error.message ?: "Syntax error")
            }
            collected()?.let { throw it }
            return root
        }

        private fun fail(node: AstNode?, reason: String): Nothing {
            val offset = (node?.absolutePosition ?: 0).coerceIn(0, source.length)
            throw DesignerParseError(lineOf(offset), columnOf(offset), reason)
        }

        private fun fail(line: Int, column: Int, reason: String): Nothing = throw DesignerParseError(line, column, reason)

        private fun lineOf(offset: Int): Int = 1 + source.substring(0, offset.coerceIn(0, source.length)).count { it == '\n' }

        private fun columnOf(offset: Int): Int {
            val bounded = offset.coerceIn(0, source.length)
            return bounded - source.lastIndexOf('\n', bounded - 1)
        }

        private fun slice(node: AstNode): String {
            val start = node.absolutePosition.coerceIn(0, source.length)
            val end = (start + node.length).coerceIn(start, source.length)
            return source.substring(start, end).trim()
        }

        private fun statementEnd(node: AstNode): Int {
            var end = (node.absolutePosition + node.length).coerceIn(0, source.length)
            var index = end
            while (index < source.length && (source[index] == ' ' || source[index] == '\t')) index++
            if (index < source.length && source[index] == ';') end = index + 1
            return end
        }

        private fun opaque(text: String): Segment.Opaque? {
            val trimmed = text.trim('\n', ' ', '\t')
            return trimmed.takeIf { it.isNotEmpty() }?.let { Segment.Opaque(it) }
        }

        // ---------------------------------------------------------------- top level

        private fun stateOf(statement: AstNode): StateDecl? {
            val declaration = statement as? VariableDeclaration ?: return null
            val initializer = declaration.variables.singleOrNull() ?: return null
            val name = (initializer.target as? Name)?.identifier ?: return null
            val call = initializer.initializer as? FunctionCall ?: return null
            if (composeMethod(call) != "state") return null
            if (call.arguments.size > 1) fail(call.arguments[1], "compose.state takes one initial value")
            val initial = call.arguments.firstOrNull()?.let { propValue(it) } ?: PropValue.Expression("undefined")
            return StateDecl(declarationKeyword(declaration), name, initial)
        }

        private fun declarationKeyword(declaration: VariableDeclaration): String = when {
            declaration.isConst -> "const"
            declaration.isLet -> "let"
            else -> "var"
        }

        private fun mountOf(statement: AstNode): MountDecl? {
            when (statement) {
                is VariableDeclaration -> {
                    val initializer = statement.variables.singleOrNull() ?: return null
                    val call = initializer.initializer as? FunctionCall ?: return null
                    val kind = HostKind.byMethod(composeMethod(call) ?: return null) ?: return null
                    val name = (initializer.target as? Name)?.identifier ?: fail(initializer.target, "The mount result must be bound to a plain variable")
                    return mount(call, kind, declarationKeyword(statement), name)
                }
                is ExpressionStatement -> {
                    val expression = statement.expression
                    if (expression is FunctionCall) {
                        val kind = HostKind.byMethod(composeMethod(expression) ?: return null) ?: return null
                        return mount(expression, kind, null, null)
                    }
                    if (expression is Assignment) {
                        val call = expression.right as? FunctionCall ?: return null
                        val kind = HostKind.byMethod(composeMethod(call) ?: return null) ?: return null
                        val name = (expression.left as? Name)?.identifier ?: fail(expression.left, "The mount result must be assigned to a plain variable")
                        return mount(call, kind, "", name)
                    }
                    return null
                }
                else -> return null
            }
        }

        private fun mount(call: FunctionCall, kind: HostKind, bindKeyword: String?, bindName: String?): MountDecl {
            val arguments = call.arguments
            if (arguments.isEmpty()) fail(call, "compose.${kind.method} needs the interface to show")
            if (arguments.size > 2) fail(arguments[2], "compose.${kind.method} takes the interface and an optional options object")
            val spec = arguments[0]
            var render = false
            var arrow = false
            var prelude: String? = null
            val root = when (spec) {
                is FunctionNode -> {
                    render = true
                    arrow = spec.functionType == FunctionNode.ARROW_FUNCTION
                    if (spec.params.isNotEmpty()) fail(spec.params[0], "A render function takes no parameters")
                    val statements = bodyStatements(spec)
                    val last = statements.lastOrNull() as? ReturnStatement
                        ?: fail(spec, "The render function must end with return <node>")
                    val returned = last.returnValue ?: fail(last, "return must produce the root node")
                    if (statements.size > 1) {
                        val first = statements.first()
                        // Start at the beginning of the first statement's line so that every line carries its
                        // own indentation and dedent() removes the common prefix uniformly.
                        var start = first.absolutePosition.coerceIn(0, source.length)
                        val lineStart = source.lastIndexOf('\n', start - 1) + 1
                        if (source.substring(lineStart, start).isBlank()) start = lineStart
                        prelude = dedent(source.substring(start, statementEnd(statements[statements.size - 2]).coerceAtLeast(start)))
                            .takeIf { it.isNotBlank() }
                    }
                    rootNode(returned)
                }
                is FunctionCall -> rootNode(spec)
                else -> fail(spec, "The interface must be a render function or a compose component factory call such as compose.Column(...)")
            }
            val options = arguments.getOrNull(1)?.let { option ->
                if (option is ObjectLiteral) HostOptions.Fields(option.elements.map { element ->
                    val property = element as? ObjectProperty ?: fail(element, "Options cannot use spread syntax")
                    (keyName(property.key) ?: fail(property.key, "Option keys must be plain names")) to propValue(property.value)
                }) else HostOptions.Expression(slice(option))
            }
            return MountDecl(kind, root, bindKeyword, bindName, render, arrow, prelude, options)
        }

        private fun bodyStatements(function: FunctionNode): List<AstNode> {
            val body = function.body
            if (body is ReturnStatement) return listOf(body)
            return body.filterIsInstance<AstNode>()
        }

        private fun rootNode(expression: AstNode): DesignNode {
            val call = expression as? FunctionCall
            if (call == null || componentName(call) == null) fail(expression, "The root must be a compose component factory call such as compose.Column(...)")
            return node(call)
        }

        // ---------------------------------------------------------------- nodes

        private fun composeMethod(call: FunctionCall): String? {
            val target = call.target as? PropertyGet ?: return null
            val receiver = (target.target as? Name)?.identifier ?: return null
            if (receiver != "compose" && receiver != "\$compose") return null
            return target.property.identifier
        }

        private fun componentName(call: FunctionCall): String? = composeMethod(call)?.takeUnless { it in RESERVED }

        private fun node(call: FunctionCall): DesignNode {
            val type = componentName(call) ?: fail(call, "Not a compose component factory")
            val id = nextId++
            val arguments = call.arguments
            val first = arguments.firstOrNull()
            val hasProps = first != null && (first is ObjectLiteral || isNullish(first))
            val entries = ArrayList<NodeEntry>()
            var childrenProp: List<DesignChild>? = null
            val slotNames = catalog.component(type)?.slots?.map { it.name }?.toSet() ?: emptySet()
            if (first is ObjectLiteral) {
                first.elements.forEach { element ->
                    val property = element as? ObjectProperty ?: fail(element, "A property object cannot use spread syntax")
                    val name = keyName(property.key) ?: fail(property.key, "Property keys must be plain names")
                    val value = property.value
                    when {
                        name == NodeEntry.KEY -> entries += NodeEntry.Key(propValue(value))
                        name == NodeEntry.MODIFIER -> entries += modifierEntry(value)
                        name == "children" -> {
                            if (childrenProp != null) fail(property, "children is given twice")
                            childrenProp = children(value)
                        }
                        name == "slots" -> {
                            val slots = value as? ObjectLiteral ?: fail(value, "slots must be an object")
                            slots.elements.forEach { slotElement ->
                                val slot = slotElement as? ObjectProperty ?: fail(slotElement, "slots cannot use spread syntax")
                                val slotName = keyName(slot.key) ?: fail(slot.key, "Slot names must be plain names")
                                entries += NodeEntry.Slot(slotName, child(slot.value))
                            }
                        }
                        value is FunctionNode && isEventName(name) -> entries += NodeEntry.Handler(name, handler(value))
                        name in slotNames -> entries += NodeEntry.Slot(name, child(value))
                        else -> entries += NodeEntry.Prop(name, propValue(value))
                    }
                }
            }
            val positional = if (hasProps) arguments.drop(1) else arguments
            if (childrenProp != null && positional.isNotEmpty()) fail(positional.first(), "children cannot be both a property and positional arguments")
            val children = childrenProp ?: positional.flatMap { children(it) }
            return DesignNode(id, type, entries, children)
        }

        private fun children(argument: AstNode): List<DesignChild> = when {
            argument is FunctionCall && componentName(argument) != null -> listOf(DesignChild.Node(node(argument)))
            argument is StringLiteral -> listOf(DesignChild.Node(DesignNode.text(nextId++, argument.value)))
            argument is ArrayLiteral -> argument.elements.flatMap { children(it) }
            else -> listOf(DesignChild.Expression(slice(argument)))
        }

        private fun child(value: AstNode): DesignChild = when {
            value is FunctionCall && componentName(value) != null -> DesignChild.Node(node(value))
            value is StringLiteral -> DesignChild.Node(DesignNode.text(nextId++, value.value))
            else -> DesignChild.Expression(slice(value))
        }

        private fun modifierEntry(value: AstNode): NodeEntry {
            val operations = ArrayList<ModifierEntry>()
            var current: AstNode = value
            while (true) {
                val call = current as? FunctionCall ?: return NodeEntry.ModifierExpression(slice(value))
                val target = call.target as? PropertyGet ?: return NodeEntry.ModifierExpression(slice(value))
                val operation = target.property.identifier
                val receiver = target.target
                if (receiver is Name && (receiver.identifier == "compose" || receiver.identifier == "\$compose")) {
                    if (operation != "modifier" || call.arguments.isNotEmpty()) return NodeEntry.ModifierExpression(slice(value))
                    break
                }
                operations += ModifierEntry(operation, call.arguments.map { argument ->
                    if (argument is FunctionNode) ModifierArg.Callback(handler(argument)) else ModifierArg.Value(propValue(argument))
                })
                current = receiver
            }
            return NodeEntry.Modifier(operations.reversed())
        }

        private fun handler(function: FunctionNode): HandlerSource {
            val params = function.params.map { slice(it) }
            val arrow = function.functionType == FunctionNode.ARROW_FUNCTION
            val body = function.body
            if (arrow && function.isExpressionClosure) {
                val returned = (body as? ReturnStatement) ?: body.filterIsInstance<ReturnStatement>().firstOrNull()
                val expression = returned?.returnValue
                if (expression != null) return HandlerSource(params, slice(expression), arrow = true, expressionBody = true)
            }
            val text = slice(body)
            val inner = if (text.startsWith("{") && text.endsWith("}")) text.substring(1, text.length - 1) else text
            return HandlerSource(params, dedent(inner), arrow)
        }

        private fun propValue(value: AstNode): PropValue {
            literal(value)?.let { return PropValue.Literal(it) }
            if (value is PropertyGet) {
                val receiver = value.target
                if (receiver is Name && receiver.identifier in stateNames && value.property.identifier == "value") return PropValue.StateRef(receiver.identifier)
            }
            return PropValue.Expression(slice(value))
        }

        private fun literal(value: AstNode): JsLiteral? = when (value) {
            is StringLiteral -> JsLiteral.Str(value.value)
            is NumberLiteral -> JsLiteral.Num(value.number, slice(value))
            is UnaryExpression -> {
                val operand = value.operand
                if (value.operator == Token.NEG && operand is NumberLiteral) JsLiteral.Num(-operand.number, "-" + slice(operand)) else null
            }
            is KeywordLiteral -> when (value.type) {
                Token.TRUE -> JsLiteral.Bool(true)
                Token.FALSE -> JsLiteral.Bool(false)
                Token.NULL -> JsLiteral.Null
                else -> null
            }
            is ArrayLiteral -> {
                val items = value.elements.map { literal(it) ?: return null }
                JsLiteral.Arr(items)
            }
            is ObjectLiteral -> {
                val fields = value.elements.map { element ->
                    val property = element as? ObjectProperty ?: return null
                    val name = keyName(property.key) ?: return null
                    name to (literal(property.value) ?: return null)
                }
                JsLiteral.Obj(fields)
            }
            else -> null
        }

        private fun keyName(key: AstNode): String? = when (key) {
            is Name -> key.identifier
            is StringLiteral -> key.value
            is NumberLiteral -> key.value
            else -> null
        }

        private fun isNullish(node: AstNode): Boolean =
            node is KeywordLiteral && node.type == Token.NULL || node is Name && node.identifier == "undefined"

        private fun isEventName(name: String): Boolean = name.length > 2 && name.startsWith("on") && name[2].isUpperCase()

        private fun dedent(text: String): String {
            val lines = text.split('\n').dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
            if (lines.isEmpty()) return ""
            val indent = lines.filter { it.isNotBlank() }.minOf { line -> line.takeWhile { it == ' ' || it == '\t' }.length }
            return lines.joinToString("\n") { line -> if (line.isBlank()) "" else line.substring(minOf(indent, line.takeWhile { it == ' ' || it == '\t' }.length)).trimEnd() }
        }

        companion object {
            /** compose members that are not component factories. */
            private val RESERVED = setOf(
                "modifier", "state", "ref", "mount", "floaty", "dialog", "attach", "memo", "batch", "post", "theme",
                "createElement", "version", "isAvailable", "sessions", "closeAll",
            )
        }
    }
}
