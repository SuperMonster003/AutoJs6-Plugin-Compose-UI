package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.FragmentUsage
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEvents
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiHandler
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiSegment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlFragment
import org.mozilla.javascript.CompilerEnvirons
import org.mozilla.javascript.Context
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.Parser
import org.mozilla.javascript.ast.Assignment
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.AstRoot
import org.mozilla.javascript.ast.Block
import org.mozilla.javascript.ast.ErrorCollector
import org.mozilla.javascript.ast.ExpressionStatement
import org.mozilla.javascript.ast.FunctionCall
import org.mozilla.javascript.ast.Name
import org.mozilla.javascript.ast.ParseProblem
import org.mozilla.javascript.ast.PropertyGet
import org.mozilla.javascript.ast.ReturnStatement
import org.mozilla.javascript.ast.Scope
import org.mozilla.javascript.ast.ScriptNode
import org.mozilla.javascript.ast.StringLiteral
import org.mozilla.javascript.ast.VariableInitializer
import org.mozilla.javascript.ast.XmlExpression
import org.mozilla.javascript.ast.XmlLiteral
import org.mozilla.javascript.ast.XmlString

/**
 * Reads a `"ui";` mode script into a [UiDocument] (roadmap P10.1 / D46 / D49). The script is parsed with the host's
 * Rhino `Parser` and never evaluated. Every XML literal becomes a layout fragment kept as a concrete syntax tree;
 * the literal handed to the first `ui.layout(...)` call (directly or through one variable) is the canvas root;
 * everything else stays opaque source text in place. What cannot be mapped is rejected with a position.
 */
class UiScriptReader {

    fun read(source: String): UiDocument = Session(source.replace("\r\n", "\n").replace('\r', '\n')).read()

    companion object {
        private val DIRECTIVE = Regex("""^\s*(?:(?://[^\n]*\n|/\*.*?\*/)\s*)*(['"])ui\1\s*;""", RegexOption.DOT_MATCHES_ALL)

        /** A cheap test on the text: the `"ui"` directive first, a `ui.layout(` call and no compose mount. */
        fun looksLikeUiScript(source: String): Boolean =
            DIRECTIVE.containsMatchIn(source) && source.contains("ui.layout(") && !Regex("""compose\.(mount|floaty|dialog)\(""").containsMatchIn(source)
    }

    private class Session(private val source: String) {
        private var nextId = 1

        fun read(): UiDocument {
            val root = parseAst()
            val first = root.statements.firstOrNull()
            val directive = (first as? ExpressionStatement)?.expression as? StringLiteral
            if (directive == null || directive.value != "ui") fail(first?.absolutePosition ?: 0, "The script does not start with the \"ui\" directive")

            val literals = ArrayList<Literal>()
            val initializers = HashMap<String, MutableList<Literal>>()
            val assigned = HashSet<String>()
            val layoutCalls = ArrayList<FunctionCall>()
            val widgets = LinkedHashSet<String>()
            val handlers = LinkedHashSet<UiHandler>()
            root.visitAll { node ->
                when (node) {
                    is XmlLiteral -> literals += literalOf(node)
                    is Assignment -> (node.left as? Name)?.let { assigned += it.identifier }
                    is FunctionCall -> when {
                        isUiCall(node, "layout") -> layoutCalls += node
                        isUiCall(node, "registerWidget") -> (node.arguments.firstOrNull() as? StringLiteral)?.let { widgets += it.value }
                        else -> handlerOf(node)?.let { handlers += it }
                    }
                }
                true
            }
            literals.sortBy { it.start }
            literals.forEach { literal -> literal.boundTo?.let { initializers.getOrPut(it) { ArrayList() } += literal } }
            if (layoutCalls.isEmpty()) fail(0, "The script has no ui.layout(...) call")

            layoutCalls.sortBy { it.absolutePosition }
            var rootLiteral: Literal? = null
            for (call in layoutCalls) {
                val argument = call.arguments.singleOrNull() ?: fail(call.absolutePosition, "ui.layout takes exactly one argument")
                val literal = when (argument) {
                    is XmlLiteral -> literals.first { it.start == argument.absolutePosition }
                    is Name -> {
                        val candidates = initializers[argument.identifier].orEmpty()
                        when {
                            candidates.isEmpty() -> fail(argument.absolutePosition, "ui.layout(${argument.identifier}): the variable is not initialized with an XML literal")
                            candidates.size > 1 -> fail(argument.absolutePosition, "ui.layout(${argument.identifier}): the variable is initialized with more than one XML literal")
                            argument.identifier in assigned -> fail(argument.absolutePosition, "ui.layout(${argument.identifier}): the variable is reassigned after its XML literal")
                            else -> candidates[0]
                        }
                    }
                    else -> fail(argument.absolutePosition, "ui.layout needs an XML literal or a variable holding one")
                }
                if (literal.usage == FragmentUsage.OTHER) {
                    literal.usage = FragmentUsage.LAYOUT
                    literal.layoutCallOffset = call.absolutePosition
                    // Roadmap P10.4 / D52: handler stubs go after this statement and the handlers already behind it.
                    statementOf(call)?.let { statement ->
                        val end = handlersEndAfter(statement)
                        if (end >= literal.end && literals.none { it !== literal && it.start >= literal.end && it.start < end }) {
                            literal.stubOffset = end - literal.end
                            literal.stubIndent = indentOf(statement.absolutePosition)
                        }
                    }
                }
                if (rootLiteral == null) rootLiteral = literal
            }

            val segments = ArrayList<UiSegment>()
            var cursor = 0
            var rootIndex = -1
            literals.forEachIndexed { index, literal ->
                if (literal.start < cursor) fail(literal.start, "Nested XML literals are not supported")
                if (literal.start > cursor) segments += UiSegment.Opaque(source.substring(cursor, literal.start))
                val element = try {
                    XmlCst.parse(literal.text, literal.exprs) { nextId++ }
                } catch (error: XmlCstError) {
                    fail(literal.start + error.offset, error.reason)
                }
                segments += UiSegment.Layout(XmlFragment(element, literal.usage, literal.boundTo, literal.layoutCallOffset, literal.stubOffset, literal.stubIndent))
                if (literal === rootLiteral) rootIndex = index
                cursor = literal.end
            }
            if (cursor < source.length) segments += UiSegment.Opaque(source.substring(cursor))
            return UiDocument(segments, rootIndex, nextId, widgets, handlers)
        }

        private class Literal(val start: Int, val end: Int, val text: String, val exprs: List<XmlCst.ExprRange>, val boundTo: String?) {
            var usage = FragmentUsage.OTHER
            var layoutCallOffset = -1
            var stubOffset = -1
            var stubIndent = ""
        }

        /** The handler [call] registers (`ui.<id>.on('<event>', ...)` and the `click` / `longClick` shortcuts), or null. */
        private fun handlerOf(call: FunctionCall): UiHandler? {
            val target = call.target as? PropertyGet ?: return null
            val receiver = target.target as? PropertyGet ?: return null
            if ((receiver.target as? Name)?.identifier != "ui") return null
            val id = receiver.property.identifier
            return when (val method = target.property.identifier) {
                "on", "once", "addListener", "prependListener", "prependOnceListener" -> (call.arguments.firstOrNull() as? StringLiteral)?.let { UiHandler(id, it.value) }
                else -> UiEvents.ofShortcut(method)?.takeIf { call.arguments.isNotEmpty() }?.let { UiHandler(id, it) }
            }
        }

        /**
         * The statement holding [node]: the ancestor whose parent is the script, a function body or a block. An
         * expression-bodied arrow function has no statement to append after, so null.
         */
        private fun statementOf(node: AstNode): AstNode? {
            var current: AstNode = node
            while (true) {
                val parent = current.parent ?: return null
                if (parent is AstRoot || parent is Block || (parent is Scope && parent !is ScriptNode)) {
                    // After `return ui.layout(...)` nothing runs; such a layout gets no stubs.
                    return current.takeUnless { it is ReturnStatement }
                }
                if (parent is ScriptNode) return null
                current = parent
            }
        }

        /** The end of [statement], extended over the handler statements that directly follow it in the same block. */
        private fun handlersEndAfter(statement: AstNode): Int {
            var end = statement.absolutePosition + statement.length
            val parent = statement.parent ?: return end
            val siblings = parent.toList()
            val index = siblings.indexOf(statement)
            if (index < 0) return end
            for (i in index + 1 until siblings.size) {
                val next = siblings[i] as? ExpressionStatement ?: break
                val call = next.expression as? FunctionCall ?: break
                if (handlerOf(call) == null) break
                end = next.absolutePosition + next.length
            }
            return end
        }

        /** The whitespace that starts the line of [offset], when nothing but whitespace precedes [offset] on it. */
        private fun indentOf(offset: Int): String {
            val lineStart = source.lastIndexOf('\n', offset - 1) + 1
            val prefix = source.substring(lineStart, offset)
            return if (prefix.isBlank()) prefix else ""
        }

        private fun literalOf(node: XmlLiteral): Literal {
            val fragments = node.fragments
            if (fragments.isEmpty()) fail(node.absolutePosition, "Empty XML literal")
            val start = fragments.first().absolutePosition
            var end = start
            val exprs = ArrayList<XmlCst.ExprRange>()
            for (fragment in fragments) {
                val position = fragment.absolutePosition
                when (fragment) {
                    is XmlString -> end = position + fragment.xml.length
                    is XmlExpression -> {
                        end = position + fragment.length
                        exprs += XmlCst.ExprRange(position - start, end - start, fragment.isXmlAttribute)
                    }
                }
            }
            val boundTo = (node.parent as? VariableInitializer)?.let { init -> (init.target as? Name)?.takeIf { init.initializer === node }?.identifier }
            return Literal(start, end, source.substring(start, end), exprs, boundTo)
        }

        private fun isUiCall(call: FunctionCall, method: String): Boolean {
            val target = call.target as? PropertyGet ?: return false
            return (target.target as? Name)?.identifier == "ui" && target.property.identifier == method
        }

        private fun parseAst(): AstRoot {
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

        private fun lineOf(offset: Int) = SourceText.lineOf(source, offset)
        private fun columnOf(offset: Int) = SourceText.columnOf(source, offset)

        private fun fail(offset: Int, reason: String): Nothing = throw DesignerParseError(lineOf(offset), columnOf(offset), reason)

        @Suppress("unused")
        private fun fail(node: AstNode, reason: String): Nothing = fail(node.absolutePosition, reason)
    }
}
