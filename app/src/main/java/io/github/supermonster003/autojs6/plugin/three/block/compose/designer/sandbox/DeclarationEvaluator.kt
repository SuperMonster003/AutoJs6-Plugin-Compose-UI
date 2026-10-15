package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText
import org.mozilla.javascript.CompilerEnvirons
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Parser
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.ast.ArrayLiteral
import org.mozilla.javascript.ast.Assignment
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.AstRoot
import org.mozilla.javascript.ast.ConditionalExpression
import org.mozilla.javascript.ast.ElementGet
import org.mozilla.javascript.ast.ErrorCollector
import org.mozilla.javascript.ast.FunctionCall
import org.mozilla.javascript.ast.InfixExpression
import org.mozilla.javascript.ast.KeywordLiteral
import org.mozilla.javascript.ast.Name
import org.mozilla.javascript.ast.NumberLiteral
import org.mozilla.javascript.ast.ObjectLiteral
import org.mozilla.javascript.ast.ObjectProperty
import org.mozilla.javascript.ast.ParenthesizedExpression
import org.mozilla.javascript.ast.ParseProblem
import org.mozilla.javascript.ast.PropertyGet
import org.mozilla.javascript.ast.StringLiteral
import org.mozilla.javascript.ast.TemplateCharacters
import org.mozilla.javascript.ast.TemplateLiteral
import org.mozilla.javascript.ast.UnaryExpression
import org.mozilla.javascript.ast.UpdateExpression
import org.mozilla.javascript.ast.VariableDeclaration

/**
 * Evaluates the value-like top-level declarations of a `"ui"` mode script for the layout preview (roadmap P10.3 /
 * D47 level 1). Only `var` / `let` / `const` declarations whose initializer is built from literals, array and object
 * literals, template strings, unary / binary / conditional operators, property and index access on those, and
 * `JSON.*` / `Math.*` calls are run, in source order, so a later declaration may use an earlier one; functions,
 * calls into the AutoJs6 modules and everything else are skipped. The E4X `{expr}` texts of the literals are then
 * evaluated in the same scope. Everything runs in Rhino's interpreter with safe standard objects, no Java classes
 * and a small budget, on the calling thread; nothing of the script's other statements executes.
 */
internal class DeclarationEvaluator(private val limits: SandboxLimits = SandboxLimits(instructions = 20_000_000L, millis = 1_000L, stackDepth = 200)) {

    /** A top-level declaration the evaluator may run: the variable and its initializer source. */
    data class Declaration(val name: String, val initializer: String)

    /**
     * [declarations] holds the JSON text of each evaluated declaration (functions and undefined are dropped),
     * [expressions] the string value of each requested expression, [failures] the error of what could not be
     * evaluated (by declaration name or expression).
     */
    data class Result(
        val declarations: Map<String, String>,
        val expressions: Map<String, String>,
        val failures: Map<String, String>,
    ) {
        companion object {
            val EMPTY = Result(emptyMap(), emptyMap(), emptyMap())
        }
    }

    /** Evaluates the safe declarations of [source] and then [expressions]; a syntax error gives [Result.EMPTY]. */
    fun evaluate(source: String, expressions: Collection<String> = emptyList()): Result {
        val declarations = try { declarationsOf(source) } catch (error: RuntimeException) { return Result.EMPTY }
        if (declarations.isEmpty() && expressions.isEmpty()) return Result.EMPTY
        val script = buildString {
            append("var __d = {}, __x = {}, __e = {};\n")
            declarations.forEach { (name, initializer) ->
                append("try { var ").append(name).append(" = (").append(initializer).append("\n); __d[")
                    .append(JsonText.quote(name)).append("] = ").append(name)
                    .append("; } catch (e) { __e[").append(JsonText.quote(name)).append("] = String(e); }\n")
            }
            expressions.distinct().forEach { expression ->
                val key = JsonText.quote(expression)
                append("try { __x[").append(key).append("] = String((").append(expression).append("\n)); } catch (e) { __e[")
                    .append(key).append("] = String(e); }\n")
            }
            append("JSON.stringify({ d: __d, x: __x, e: __e }, function (k, v) { return typeof v === 'function' ? undefined : v; });")
        }
        val json = try {
            run(script)
        } catch (error: RhinoException) {
            return Result(emptyMap(), emptyMap(), mapOf("" to error.details()))
        } catch (limit: LimitReached) {
            return Result(emptyMap(), emptyMap(), mapOf("" to (limit.message ?: "limit")))
        } catch (error: StackOverflowError) {
            return Result(emptyMap(), emptyMap(), mapOf("" to "The declarations nested too deeply"))
        }
        val parsed = JsonText.parse(json) as? Map<*, *> ?: return Result.EMPTY
        val values = (parsed["d"] as? Map<*, *>).orEmpty().entries.associate { (key, value) -> key as String to JsonText.write(value) }
        val texts = (parsed["x"] as? Map<*, *>).orEmpty().entries.associate { (key, value) -> key as String to (value as? String ?: JsonText.write(value)) }
        val failures = (parsed["e"] as? Map<*, *>).orEmpty().entries.associate { (key, value) -> key as String to (value as? String ?: "") }
        return Result(values, texts, failures)
    }

    private fun run(script: String): String {
        val factory = Factory(limits)
        val cx = factory.enterContext()
        try {
            factory.arm()
            val scope = cx.initSafeStandardObjects()
            return Context.toString(cx.evaluateString(scope, script, "<declarations>", 1, null))
        } finally {
            Context.exit()
        }
    }

    /** Thrown from the instruction observer; an Error, so no `catch` of the evaluated text runs. */
    private class LimitReached(message: String) : Error(message, null, false, false)

    private class Factory(private val limits: SandboxLimits) : ContextFactory() {
        private var used = 0L
        private var deadline = 0L

        fun arm() {
            used = 0L
            deadline = System.nanoTime() + limits.millis * 1_000_000L
        }

        override fun makeContext(): Context = super.makeContext().apply {
            setInterpretedMode(true)
            setLanguageVersion(Context.VERSION_ES6)
            setClassShutter { false }
            setInstructionObserverThreshold(10_000)
            setMaximumInterpreterStackDepth(limits.stackDepth)
        }

        override fun observeInstructionCount(cx: Context, instructionCount: Int) {
            used += instructionCount
            if (used > limits.instructions) throw LimitReached("The declarations ran more than ${limits.instructions} instructions")
            if (System.nanoTime() > deadline) throw LimitReached("The declarations ran longer than ${limits.millis} ms")
        }
    }

    companion object {
        /** The sandbox interpreter needs java.time (roadmap P9.3); below it the preview shows bindings as source. */
        val SUPPORTED: Boolean get() = SandboxPreview.SUPPORTED

        private val GLOBAL_OBJECTS = setOf("Math", "JSON")
        private val GLOBAL_NAMES = setOf("undefined", "NaN", "Infinity")

        /** The value-like top-level declarations of [source], in order; throws on a syntax error. */
        fun declarationsOf(source: String): List<Declaration> {
            val text = source.replace("\r\n", "\n").replace('\r', '\n')
            val root = parse(text)
            val known = HashSet<String>()
            val result = ArrayList<Declaration>()
            for (statement in root.statements) {
                val declaration = statement as? VariableDeclaration ?: continue
                for (variable in declaration.variables) {
                    val name = (variable.target as? Name)?.identifier ?: continue
                    val initializer = variable.initializer ?: continue
                    if (isValueLike(initializer, known)) {
                        result += Declaration(name, text.substring(initializer.absolutePosition, initializer.absolutePosition + initializer.length))
                        known += name
                    }
                }
            }
            return result
        }

        private fun isValueLike(node: AstNode, known: Set<String>): Boolean = when (node) {
            is StringLiteral, is NumberLiteral, is KeywordLiteral -> true
            is TemplateLiteral -> node.elements.all { it is TemplateCharacters || isValueLike(it, known) }
            is ArrayLiteral -> node.elements.all { isValueLike(it, known) }
            is ObjectLiteral -> node.elements.all { property -> isValueLike(property, known) }
            is ObjectProperty -> !node.isGetterMethod && !node.isSetterMethod && !node.isNormalMethod && node.value != null && isValueLike(node.value, known)
            is Name -> node.identifier in known || node.identifier in GLOBAL_NAMES
            is ParenthesizedExpression -> isValueLike(node.expression, known)
            is UnaryExpression -> isValueLike(node.operand, known)
            is UpdateExpression -> false
            is ConditionalExpression -> isValueLike(node.testExpression, known) && isValueLike(node.trueExpression, known) && isValueLike(node.falseExpression, known)
            is PropertyGet -> isReadable(node, known)
            is ElementGet -> isValueLike(node.target, known) && isValueLike(node.element, known)
            is FunctionCall -> isLibraryCall(node) && node.arguments.all { isValueLike(it, known) }
            is Assignment -> false
            is InfixExpression -> isValueLike(node.left, known) && isValueLike(node.right, known)
            else -> false
        }

        /** `a.b.c` on a known declaration, or a property of `Math` / `JSON` (`Math.PI`). */
        private fun isReadable(node: PropertyGet, known: Set<String>): Boolean {
            val target = node.target
            return when (target) {
                is Name -> target.identifier in known || target.identifier in GLOBAL_OBJECTS
                is PropertyGet -> isReadable(target, known)
                is ElementGet -> isValueLike(target, known)
                else -> isValueLike(target, known)
            }
        }

        /** `JSON.parse(...)`, `Math.max(...)` and the like: a call on `Math` or `JSON` only. */
        private fun isLibraryCall(call: FunctionCall): Boolean {
            val target = call.target as? PropertyGet ?: return false
            return (target.target as? Name)?.identifier in GLOBAL_OBJECTS
        }

        private fun parse(source: String): AstRoot {
            val environment = CompilerEnvirons().apply {
                languageVersion = Context.VERSION_ES6
                isIdeMode = true
                setRecordingComments(false)
                setRecordingLocalJsDocComments(false)
            }
            val collector = ErrorCollector()
            environment.errorReporter = collector
            val root = Parser(environment, collector).parse(source, "declarations.js", 1)
            // In IDE mode the parser recovers from errors instead of throwing; a script with one is not evaluated.
            require(collector.errors.none { it.type == ParseProblem.Type.Error }) { "The script has a syntax error" }
            return root
        }
    }
}
