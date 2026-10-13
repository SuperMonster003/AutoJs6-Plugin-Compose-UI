package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.DesignerPerformanceDeviceTest.Companion.LARGE_SCRIPT
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.DesignerPerformanceDeviceTest.Companion.report
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.DesignerPerformanceDeviceTest.Companion.summary
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerSession
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Real frame times of an edit on the 251-node document (roadmap P9.1, opt-in like
 * [DesignerPerformanceDeviceTest]). That class confirms visibility through the compose test rule, whose
 * idle synchronisation and node queries are part of its times; while such a rule is active, recomposition is
 * also driven by the test's clock. This class has no compose rule: undo and redo are called on the session on
 * the main thread, the editor composes with its real frame clock, and the window's [FrameMetrics] give the
 * frames drawn until it settles. One `DESIGNER_PERF {json}` line, kind `frames`:
 *
 * - `*_to_first_frame` / `*_to_last_frame`: from the call to the end of the first frame (the one that
 *   recomposes) and of the last frame after it (API 26 and later, which report intended vsync times);
 * - `*_call`: the undo / redo call itself; `*_main_cpu`: main thread CPU time from the call until the frames
 *   settled, which a busy host (such as a shared emulator machine) inflates less than wall time;
 * - `*_frames`: frames drawn after the call;
 * - `*_longest_*`: the longest of those frames, in total and by phase. Compose recomposition runs in a
 *   Choreographer frame callback and is counted as animation; render is sync, command issue and swap.
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerFramePerformanceDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)

    @Before fun optIn() {
        assumeTrue("designer performance runs only with -e designerPerformance true", InstrumentationRegistry.getArguments().getString("designerPerformance") == "true")
        clean()
    }

    @After fun clean() { store.list().filter { it.name.startsWith("perf-frames-") }.forEach { store.delete(it.name) } }

    @Test
    fun undoAndRedoFramesOfTheLargeDocument() {
        val name = store.create("perf-frames-edit", LARGE_SCRIPT).name
        val thread = HandlerThread("designer-frame-metrics").apply { start() }
        val frames = ConcurrentLinkedQueue<FrameSample>()
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ -> frames += FrameSample.of(metrics) }
        val edits = linkedMapOf("undo" to ArrayList<EditFrames>(), "redo" to ArrayList<EditFrames>())
        try {
            ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use { scenario ->
                var session: DesignerSession? = null
                scenario.onActivity { activity -> session = requireNotNull(activity.currentSession) }
                val editor = checkNotNull(session)
                val rows = { editor.document.root!!.children.size }
                scenario.onActivity { editor.execute(EditCommand.RemoveNode(editor.document.root!!.children.first().let { (it as DesignChild.Node).node.id })) }
                scenario.onActivity { it.window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper)) }
                repeat(WARM_UP + ROUNDS) { round ->
                    for (label in edits.keys) {
                        SystemClock.sleep(SETTLE_MILLIS)
                        frames.clear()
                        var start = 0L
                        var returned = 0L
                        var cpu = 0L
                        scenario.onActivity { cpu = Debug.threadCpuTimeNanos(); start = System.nanoTime(); if (label == "undo") editor.undo() else editor.redo(); returned = System.nanoTime() }
                        SystemClock.sleep(SETTLE_MILLIS)
                        scenario.onActivity { cpu = Debug.threadCpuTimeNanos() - cpu }
                        check(rows() == if (label == "undo") 50 else 49) { "$label did not apply" }
                        if (round >= WARM_UP) edits.getValue(label) += EditFrames(start, returned, cpu, frames.toList())
                    }
                }
                scenario.onActivity { it.window.removeOnFrameMetricsAvailableListener(listener) }
            }
        } finally {
            thread.quitSafely()
        }
        report("frames", edits.flatMap { (label, samples) ->
            val longest = samples.map { edit -> edit.frames.maxBy { it.total } }
            val timed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            listOfNotNull(
                if (timed) "${label}_to_first_frame" to summary(samples.map { it.firstFrameEnd() }) else null,
                if (timed) "${label}_to_last_frame" to summary(samples.map { it.lastFrameEnd() }) else null,
                "${label}_call" to summary(samples.map { it.call }),
                "${label}_main_cpu" to summary(samples.map { it.mainCpu }),
                "${label}_frames" to summary(samples.map { it.frames.size.toDouble() }),
                "${label}_longest_frame" to summary(longest.map { it.total }),
                "${label}_longest_delay" to summary(longest.map { it.delay }),
                "${label}_longest_animation" to summary(longest.map { it.animation }),
                "${label}_longest_layout" to summary(longest.map { it.layout }),
                "${label}_longest_draw" to summary(longest.map { it.draw }),
                "${label}_longest_render" to summary(longest.map { it.render }),
            )
        }.toMap())
    }

    /** One frame in milliseconds, with its intended vsync time (System.nanoTime clock, API 26 and later). */
    private class FrameSample(val intendedVsync: Long, val total: Double, val delay: Double, val animation: Double, val layout: Double, val draw: Double, val render: Double) {
        val end: Long get() = intendedVsync + (total * 1e6).toLong()

        companion object {
            fun of(metrics: FrameMetrics): FrameSample {
                fun ms(id: Int) = metrics.getMetric(id) / 1e6
                return FrameSample(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) metrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP) else 0L,
                    ms(FrameMetrics.TOTAL_DURATION), ms(FrameMetrics.UNKNOWN_DELAY_DURATION) + ms(FrameMetrics.INPUT_HANDLING_DURATION), ms(FrameMetrics.ANIMATION_DURATION), ms(FrameMetrics.LAYOUT_MEASURE_DURATION), ms(FrameMetrics.DRAW_DURATION),
                    ms(FrameMetrics.SYNC_DURATION) + ms(FrameMetrics.COMMAND_ISSUE_DURATION) + ms(FrameMetrics.SWAP_BUFFERS_DURATION),
                )
            }
        }
    }

    /** The frames scheduled after the edit called at [start]; before API 26 all frames since the edit. */
    private class EditFrames(private val start: Long, returned: Long, cpu: Long, all: List<FrameSample>) {
        /** Main thread CPU time from the call until the frames settled: the work itself, less exposed to a busy host than wall time. */
        val mainCpu = cpu / 1e6
        /** The undo / redo call itself (history step and validation), before any frame. */
        val call = (returned - start) / 1e6
        val frames = all.filter { Build.VERSION.SDK_INT < Build.VERSION_CODES.O || it.intendedVsync >= start }.ifEmpty { error("no frame after the edit") }
        fun firstFrameEnd(): Double = (frames.minOf { it.end } - start) / 1e6
        fun lastFrameEnd(): Double = (frames.maxOf { it.end } - start) / 1e6
    }

    private companion object {
        const val WARM_UP = 2
        const val ROUNDS = 10
        const val SETTLE_MILLIS = 1500L
    }
}
