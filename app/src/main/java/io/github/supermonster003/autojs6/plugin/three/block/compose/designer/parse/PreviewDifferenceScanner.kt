package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifference
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifferenceKind
import org.mozilla.javascript.CompilerEnvirons
import org.mozilla.javascript.Context
import org.mozilla.javascript.Parser
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.AstRoot
import org.mozilla.javascript.ast.BreakStatement
import org.mozilla.javascript.ast.DoLoop
import org.mozilla.javascript.ast.EmptyExpression
import org.mozilla.javascript.ast.ErrorCollector
import org.mozilla.javascript.ast.ForLoop
import org.mozilla.javascript.ast.FunctionCall
import org.mozilla.javascript.ast.FunctionNode
import org.mozilla.javascript.ast.KeywordLiteral
import org.mozilla.javascript.ast.Loop
import org.mozilla.javascript.ast.Name
import org.mozilla.javascript.ast.NumberLiteral
import org.mozilla.javascript.ast.ParenthesizedExpression
import org.mozilla.javascript.ast.ParseProblem
import org.mozilla.javascript.ast.PropertyGet
import org.mozilla.javascript.ast.ReturnStatement
import org.mozilla.javascript.ast.StringLiteral
import org.mozilla.javascript.ast.SwitchStatement
import org.mozilla.javascript.ast.ThrowStatement
import org.mozilla.javascript.ast.VariableInitializer
import org.mozilla.javascript.ast.WhileLoop

/**
 * Finds the lines of a `"ui"` mode script that behave differently in the designer's preview than in AutoJs6 (roadmap
 * P11.2 / D55): member access on `activity`, `ui.emitter`, the window-level `ui` functions, dialogs, endless loops on
 * the main thread, and threads and event listeners. The script is parsed with Rhino and never evaluated. A global
 * that the script declares itself (a variable, function or parameter of that name anywhere) is not reported. One
 * difference per kind and line, in source order; a script that does not parse has none (the reader reports it).
 */
internal object PreviewDifferenceScanner {
    /** Window-level `ui` functions that color the window; the preview ignores them (host `UI.ensureWindowActivity`). */
    private val WINDOW_COLORS = setOf("statusBarColor", "navigationBarColor", "backgroundColor")
    /** The other window-level `ui` functions the preview ignores. */
    private val WINDOW_NO_OPS = setOf("finish", "keepScreenOn", "statusBarIconLight", "statusBarIconLightBy", "navigationBarIconLight", "navigationBarIconLightBy")
    /** Global dialog functions (the host's `dialogs` module exports them as globals). */
    private val GLOBAL_DIALOGS = setOf("alert", "confirm", "prompt", "rawInput")
    private val LISTEN_METHODS = setOf("on", "once", "addListener", "prependListener", "prependOnceListener")
    private val LIFECYCLE_EVENTS = setOf("resume", "pause", "back_pressed")

    fun scan(source: String): List<PreviewDifference> {
        val text = source.replace("\r\n", "\n").replace('\r', '\n')
        val root = parse(text) ?: return emptyList()
        return Scan(text, root).run()
    }

    private class Scan(private val source: String, private val root: AstRoot) {
        private val declared = HashSet<String>()
        /** Names of functions handed to `threads.start(name)`: their bodies run off the main thread. */
        private val threadFunctions = HashSet<String>()
        private val found = LinkedHashMap<Pair<PreviewDifferenceKind, Int>, PreviewDifference>()

        fun run(): List<PreviewDifference> {
            root.visitAll { node ->
                when (node) {
                    is VariableInitializer -> (node.target as? Name)?.let { declared += it.identifier }
                    is FunctionNode -> {
                        node.functionName?.let { declared += it.identifier }
                        node.params.forEach { param -> (param as? Name)?.let { declared += it.identifier } }
                    }
                    is FunctionCall -> if (isMember(node.target, "threads", "start")) node.arguments.forEach { argument -> (argument as? Name)?.let { threadFunctions += it.identifier } }
                }
                true
            }
            root.visitAll { node ->
                when (node) {
                    is PropertyGet -> member(node)
                    is FunctionCall -> (node.target as? Name)?.identifier?.takeIf { it in GLOBAL_DIALOGS && it !in declared }?.let { add(PreviewDifferenceKind.DIALOGS, node, it) }
                    is Loop -> loop(node)
                }
                true
            }
            return found.values.sortedWith(compareBy({ it.line }, { it.column }))
        }

        private fun member(node: PropertyGet) {
            val receiver = (node.target as? Name)?.identifier ?: return
            if (receiver in declared) return
            val name = node.property.identifier
            when (receiver) {
                "activity" -> add(PreviewDifferenceKind.ACTIVITY, node, "activity.$name")
                "ui" -> when (name) {
                    in WINDOW_COLORS -> add(PreviewDifferenceKind.WINDOW_COLOR, node, "ui.$name")
                    in WINDOW_NO_OPS -> add(PreviewDifferenceKind.WINDOW_NO_OP, node, "ui.$name")
                    "emitter" -> emitter(node)
                }
                "dialogs" -> add(PreviewDifferenceKind.DIALOGS, node, "dialogs.$name")
                "threads", "events" -> add(PreviewDifferenceKind.THREADS, node, "$receiver.$name")
            }
        }

        /** `ui.emitter`: listening to a lifecycle event the host can deliver, or any other Activity event. */
        private fun emitter(node: PropertyGet) {
            val method = (node.parent as? PropertyGet)?.takeIf { it.target === node }
            val call = (method?.parent as? FunctionCall)?.takeIf { it.target === method }
            val event = (call?.arguments?.firstOrNull() as? StringLiteral)?.value
            val listener = method?.property?.identifier?.takeIf { it in LISTEN_METHODS }
            val kind = if (listener != null && event in LIFECYCLE_EVENTS) PreviewDifferenceKind.LIFECYCLE else PreviewDifferenceKind.ACTIVITY_EVENTS
            add(kind, node, if (listener != null && event != null) "ui.emitter.$listener('$event')" else "ui.emitter")
        }

        /** A loop that cannot end (constant true condition, no exit in its body) on the main thread. */
        private fun loop(loop: Loop) {
            val label = when (loop) {
                is WhileLoop -> "while (${loop.condition.toSource()})".takeIf { isTrue(loop.condition) }
                is DoLoop -> "do ... while (${loop.condition.toSource()})".takeIf { isTrue(loop.condition) }
                is ForLoop -> when {
                    loop.condition == null || loop.condition is EmptyExpression -> "for (;;)"
                    isTrue(loop.condition) -> "for (; ${loop.condition.toSource()};)"
                    else -> null
                }
                else -> null
            } ?: return
            if (exits(loop) || offMainThread(loop)) return
            add(PreviewDifferenceKind.BUSY_WAIT, loop, label)
        }

        private fun isTrue(node: AstNode?): Boolean = when (node) {
            is ParenthesizedExpression -> isTrue(node.expression)
            is KeywordLiteral -> node.type == Token.TRUE
            is NumberLiteral -> node.number != 0.0 && !node.number.isNaN()
            else -> false
        }

        /** Whether [loop]'s body can leave it: a break that is not taken by a loop or switch inside it, a return or a throw. */
        private fun exits(loop: Loop): Boolean {
            var exits = false
            loop.body?.visit { node ->
                when {
                    exits || node is FunctionNode -> false
                    node is ReturnStatement || node is ThrowStatement -> { exits = true; false }
                    node is BreakStatement -> { if (node.breakLabel != null || !takenInside(node, loop)) exits = true; false }
                    else -> true
                }
            }
            return exits
        }

        private fun takenInside(node: AstNode, loop: Loop): Boolean {
            var current = node.parent
            while (current != null && current !== loop) {
                if (current is Loop || current is SwitchStatement) return true
                current = current.parent
            }
            return false
        }

        /** Whether [node] runs in a function given to `threads.start`, directly or by name. */
        private fun offMainThread(node: AstNode): Boolean {
            var current: AstNode? = node.parent
            while (current != null && current !is AstRoot) {
                if (current is FunctionNode && (isThreadArgument(current) || nameOf(current) in threadFunctions)) return true
                current = current.parent
            }
            return false
        }

        private fun isThreadArgument(function: FunctionNode): Boolean {
            val call = function.parent as? FunctionCall ?: return false
            return isMember(call.target, "threads", "start") && call.arguments.any { it === function }
        }

        private fun nameOf(function: FunctionNode): String? =
            function.functionName?.identifier ?: ((function.parent as? VariableInitializer)?.takeIf { it.initializer === function }?.target as? Name)?.identifier

        private fun add(kind: PreviewDifferenceKind, node: AstNode, api: String) {
            val offset = node.absolutePosition
            val line = SourceText.lineOf(source, offset)
            found.putIfAbsent(kind to line, PreviewDifference(kind, line, SourceText.columnOf(source, offset), api))
        }
    }

    private fun isMember(target: AstNode?, receiver: String, name: String): Boolean {
        val get = target as? PropertyGet ?: return false
        return (get.target as? Name)?.identifier == receiver && get.property.identifier == name
    }

    private fun parse(source: String): AstRoot? {
        val environment = CompilerEnvirons().apply {
            languageVersion = Context.VERSION_ES6
            isIdeMode = true
            setRecordingComments(false)
            setRecordingLocalJsDocComments(false)
        }
        val collector = ErrorCollector()
        environment.errorReporter = collector
        val root = try {
            Parser(environment, collector).parse(source, "designer.js", 1)
        } catch (error: RuntimeException) {
            return null
        }
        return root.takeIf { collector.errors.none { it.type == ParseProblem.Type.Error } }
    }
}
