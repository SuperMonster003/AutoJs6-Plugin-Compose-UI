package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.TopLevel
import org.mozilla.javascript.Undefined

/**
 * The limits of one entry into the sandbox: loading the script, delivering one event, or running the timers that
 * are due. Instructions are counted by the interpreter; the wall clock is checked at the same points.
 */
internal data class SandboxLimits(
    val instructions: Long = 200_000_000L,
    val millis: Long = 3_000L,
    /** Interpreter frames (nested JavaScript calls). */
    val stackDepth: Int = 1_000,
)

/** A console line of the preview: `log`, `info`, `warn`, `error`, `toast` or `snackbar`. */
internal data class ConsoleLine(val level: String, val text: String)

/**
 * Why an entry failed. A [Kind.SCRIPT] error is the script's own (an uncaught exception, a compose error, a
 * syntax error) and the sandbox carries on; the other kinds stop it, because the interpreter was cut off in the
 * middle of the runtime's bookkeeping.
 */
internal data class SandboxFailure(val kind: Kind, val message: String, val line: Int = -1) {
    enum class Kind { SCRIPT, TIME_LIMIT, INSTRUCTION_LIMIT, STACK_LIMIT, MEMORY_LIMIT, INTERNAL }

    val stops: Boolean get() = kind != Kind.SCRIPT
}

/** The result of one entry: the new tree (null when nothing changed), the console lines and a failure. */
internal class SandboxOutput(val snapshot: String?, val console: List<ConsoleLine>, val failure: SandboxFailure?) {
    companion object {
        val NOTHING = SandboxOutput(null, emptyList(), null)
    }
}

/**
 * Executes a script of the designer in Rhino's interpreter (roadmap P9.3 step 2, D43; D8 / D35 revised
 * 2026-10-13): only safe standard objects, a class shutter that hides every Java class, an instruction and
 * wall-clock budget per entry and a bounded interpreter stack. Before the script, the preview runtime
 * (`/designer-sandbox/preview-runtime.js`) defines `compose` and the console; the bridge carries strings only,
 * so the script never sees a Java object. Not thread-safe: one engine is used from one thread, as Rhino
 * requires; [PreviewSandbox] gives it that thread.
 */
internal class SandboxEngine(private val catalogJson: String, private val limits: SandboxLimits = SandboxLimits()) {
    private val factory = Factory(limits)
    private var scope: TopLevel? = null

    /** True once a limit cut the interpreter off; every later call does nothing. */
    var stopped: Boolean = false
        private set

    /** Runs the preview runtime and then [source]; the output holds the first tree when the script mounted one. */
    fun load(source: String, name: String): SandboxOutput = enter { cx, scope ->
        cx.evaluateString(scope, "var __catalogJson = ${JsonText.quote(catalogJson)};", "<catalog>", 1, null)
        cx.evaluateString(scope, PRELUDE, "preview-runtime.js", 1, null)
        cx.evaluateString(scope, source, name, 1, null)
    }

    /** Delivers an event of the canvas: [target] is a node id, or the token of a modifier callback. */
    fun dispatch(target: Int, event: String, args: List<Any?>): SandboxOutput = enter { cx, scope ->
        evaluate(cx, scope, "__sandbox.dispatch($target, ${JsonText.quote(event)}, ${JsonText.quote(JsonText.write(args))})")
    }

    /** Runs the timers (`setTimeout`, `setInterval`, `compose.post`) that are due. */
    fun runTimers(): SandboxOutput = enter { cx, scope -> evaluate(cx, scope, "__sandbox.runTimers()") }

    /** Milliseconds until the next timer is due, or -1 when none is pending (or the sandbox stopped). */
    fun nextTimer(): Long {
        if (stopped || scope == null) return -1
        return try {
            inContext { cx, scope -> (evaluate(cx, scope, "__sandbox.nextTimer()") as? Number)?.toLong() ?: -1 }
        } catch (error: RhinoException) {
            -1
        }
    }

    private fun enter(action: (Context, TopLevel) -> Unit): SandboxOutput {
        if (stopped) return SandboxOutput.NOTHING
        val failure = try {
            return inContext { cx, scope ->
                var failure = try { action(cx, scope); null } catch (error: RhinoException) { scriptFailure(error) }
                // A script error left the runtime consistent (its finally blocks ran), so what changed before it
                // still renders: a mount followed by a failing statement shows the mounted tree.
                val snapshot = try {
                    evaluate(cx, scope, "__sandbox.flush()")?.let(Context::toString)
                } catch (error: RhinoException) {
                    failure = failure ?: scriptFailure(error)
                    null
                }
                SandboxOutput(snapshot, drain(cx, scope), failure)
            }
        } catch (limit: LimitReached) {
            SandboxFailure(limit.kind, limit.message ?: limit.kind.name)
        } catch (error: StackOverflowError) {
            SandboxFailure(SandboxFailure.Kind.STACK_LIMIT, "The script nested too deeply")
        } catch (error: OutOfMemoryError) {
            SandboxFailure(SandboxFailure.Kind.MEMORY_LIMIT, "The script ran out of memory")
        }
        stopped = true
        // The lines written before the cut still help to find the loop; reading them is cheap and bounded.
        val console = try { inContext { cx, scope -> drain(cx, scope) } } catch (error: Throwable) { emptyList() }
        return SandboxOutput(null, console, failure)
    }

    private fun <T> inContext(action: (Context, TopLevel) -> T): T {
        val cx = factory.enterContext()
        try {
            factory.arm()
            val current = scope ?: cx.initSafeStandardObjects().also { scope = it }
            return action(cx, current)
        } finally {
            Context.exit()
        }
    }

    private fun evaluate(cx: Context, scope: TopLevel, source: String): Any? =
        cx.evaluateString(scope, source, "<designer>", 1, null).takeUnless { it == null || it is Undefined }

    private fun drain(cx: Context, scope: TopLevel): List<ConsoleLine> {
        val text = try { evaluate(cx, scope, "typeof __sandbox === 'undefined' ? '[]' : __sandbox.drain()") } catch (error: RhinoException) { null }
        val lines = (text?.let { JsonText.parse(Context.toString(it)) } as? List<*>).orEmpty()
        return lines.mapNotNull { line -> (line as? Map<*, *>)?.let { ConsoleLine(it["level"] as? String ?: "log", it["text"] as? String ?: "") } }
    }

    private fun scriptFailure(error: RhinoException): SandboxFailure {
        // An error the runtime throws (a compose check, an unavailable module) points into the runtime; the user
        // needs the line of their own script that called it, the innermost one on the script stack.
        val line = if (error.sourceName() !in INTERNAL) error.lineNumber()
        else error.scriptStack.firstOrNull { it.fileName !in INTERNAL }?.lineNumber ?: -1
        return SandboxFailure(SandboxFailure.Kind.SCRIPT, error.details(), line)
    }

    /** Thrown from the instruction observer; an Error, so no `catch` or `finally` of the script runs. */
    private class LimitReached(val kind: SandboxFailure.Kind, message: String) : Error(message, null, false, false)

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
            // Safe standard objects define no Java packages; the shutter also hides any class that could be reached.
            setClassShutter { false }
            setInstructionObserverThreshold(OBSERVER_THRESHOLD)
            setMaximumInterpreterStackDepth(limits.stackDepth)
        }

        override fun observeInstructionCount(cx: Context, instructionCount: Int) {
            used += instructionCount
            if (used > limits.instructions) throw LimitReached(SandboxFailure.Kind.INSTRUCTION_LIMIT, "The script ran more than ${limits.instructions} instructions")
            if (System.nanoTime() > deadline) throw LimitReached(SandboxFailure.Kind.TIME_LIMIT, "The script ran longer than ${limits.millis} ms")
        }
    }

    companion object {
        private const val OBSERVER_THRESHOLD = 10_000
        private val INTERNAL = setOf(null, "preview-runtime.js", "<designer>", "<catalog>")

        /** The preview runtime, a resource of the plugin's own class loader. */
        val PRELUDE: String by lazy {
            val stream = requireNotNull(SandboxEngine::class.java.getResourceAsStream("/designer-sandbox/preview-runtime.js")) { "Missing preview runtime" }
            stream.use { it.readBytes().toString(Charsets.UTF_8) }
        }
    }
}
