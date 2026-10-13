package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox

import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * One run of a script in the sandbox on its own thread (roadmap P9.3 step 2): the engine is created, loaded, sent
 * events and woken for its timers on that thread only, as Rhino requires, and every output that carries something
 * goes to [deliver] (still on that thread). The thread has a large stack because callbacks that Java built-ins
 * call (`forEach`, `sort`, `JSON.stringify` replacers) nest Java frames as well. After [close] nothing more is
 * delivered; an entry that is running finishes within its limits and is dropped.
 */
internal class PreviewSandbox(
    private val catalogJson: String,
    private val limits: SandboxLimits = SandboxLimits(),
    private val deliver: (SandboxOutput) -> Unit,
) : AutoCloseable {
    private val executor = ScheduledThreadPoolExecutor(1) { task -> Thread(null, task, "designer-sandbox", STACK_BYTES).apply { isDaemon = true } }
    /** Touched on the sandbox thread only. */
    private var engine: SandboxEngine? = null
    private var timer: ScheduledFuture<*>? = null
    @Volatile private var closed = false

    fun start(source: String, name: String) = post { SandboxEngine(catalogJson, limits).also { engine = it }.load(source, name) }

    /** [target] is a node id or a modifier callback token; [args] are the handler's arguments ([SandboxEvents]). */
    fun dispatch(target: Int, event: String, args: List<Any?>) = post { engine?.dispatch(target, event, args) }

    override fun close() {
        closed = true
        executor.shutdownNow()
    }

    private fun post(task: () -> SandboxOutput?) {
        if (closed) return
        try { executor.execute { run(task) } } catch (rejected: RejectedExecutionException) { /* closed meanwhile */ }
    }

    private fun run(task: () -> SandboxOutput?) {
        if (closed) return
        val output = try {
            task()
        } catch (failure: Throwable) {
            // A fault of the sandbox itself, not of the script: an executor would swallow it silently.
            SandboxOutput(null, emptyList(), SandboxFailure(SandboxFailure.Kind.INTERNAL, failure.toString()))
        }
        if (!closed && output != null && (output.snapshot != null || output.console.isNotEmpty() || output.failure != null)) deliver(output)
        schedule()
    }

    private fun schedule() {
        timer?.cancel(false)
        timer = null
        val current = engine ?: return
        val delay = current.nextTimer()
        if (delay < 0 || closed) return
        timer = try {
            executor.schedule({ run { current.runTimers() } }, delay, TimeUnit.MILLISECONDS)
        } catch (rejected: RejectedExecutionException) { null }
    }

    companion object {
        private const val STACK_BYTES = 8L * 1024 * 1024
    }
}
