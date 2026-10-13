package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.ScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.PreviewFrames
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Designer performance baseline (opt-in: `-e designerPerformance true`; skipped otherwise, so CI and the
 * regular suite do not pay for it). Three measurements, each logged as one `DESIGNER_PERF {json}` line:
 *
 * - pipeline: read, validate, preview-frame and write for the 56 gallery examples (summed per round) and for
 *   a synthetic document of about 250 nodes, 3 warm-up and 10 measured rounds;
 * - open: launching the editor until its canvas and outline are composed and idle, for the largest gallery
 *   example and the synthetic document, 2 warm-up and 8 measured launches (the test process is already
 *   running, so this is a warm Activity start, not a cold process start);
 * - edit: undo / redo of a row deletion until the canvas shows or drops the row's text, 10 of each.
 *
 * Times come from SystemClock and include the compose test's idle synchronisation and node queries, so they
 * are visibility-confirmation times, not pure rendering times (the same caveat as the P5 baseline).
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerPerformanceDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val created = ArrayList<String>()

    @Before fun optIn() {
        assumeTrue("designer performance runs only with -e designerPerformance true", InstrumentationRegistry.getArguments().getString("designerPerformance") == "true")
        store.list().filter { it.name.startsWith("perf-test-") }.forEach { store.delete(it.name) }
    }

    @After fun remove() { created.forEach { store.delete(it) } }

    private fun project(stem: String, script: String): String = store.create("perf-test-$stem", script).name.also { created += it }

    @Test
    fun pipelineOverGalleryExamplesAndALargeDocument() {
        val reader = ScriptReader()
        val validator = DesignValidator()
        val scripts = GalleryCatalog.entries.map { it.script() }
        val stages = listOf("read", "validate", "frame", "write")
        val gallery = stages.associateWith { ArrayList<Double>() }
        val large = stages.associateWith { ArrayList<Double>() }
        repeat(WARM_UP + ROUNDS) { round ->
            val sums = DoubleArray(stages.size)
            scripts.forEach { script -> measureStages(reader, validator, script).forEachIndexed { index, value -> sums[index] += value } }
            val single = measureStages(reader, validator, LARGE_SCRIPT)
            if (round >= WARM_UP) stages.forEachIndexed { index, stage -> gallery.getValue(stage) += sums[index]; large.getValue(stage) += single[index] }
        }
        report("pipeline", mapOf(
            "gallery_examples" to scripts.size.toString(),
            "large_nodes" to reader.read(LARGE_SCRIPT).root!!.let(::countNodes).toString(),
        ) + stages.flatMap { stage -> listOf("gallery_${stage}" to summary(gallery.getValue(stage)), "large_${stage}" to summary(large.getValue(stage))) })
    }

    @Test
    fun editorOpensTheLargestExampleAndTheLargeDocument() {
        val largest = GalleryCatalog.entries.maxBy { it.script().length }
        val cases = listOf(largest.component to largest.script(), "Large" to LARGE_SCRIPT)
        val results = cases.associate { (label, script) ->
            val name = project("open-" + label.lowercase(), script)
            val samples = ArrayList<Double>()
            repeat(OPEN_WARM_UP + OPEN_ROUNDS) { round ->
                val start = SystemClock.elapsedRealtimeNanos()
                ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use {
                    rule.waitUntil(30_000) {
                        rule.onAllNodesWithTag("designer-canvas-content").fetchSemanticsNodes().isNotEmpty() &&
                            rule.onAllNodesWithTag("designer-outline-1").fetchSemanticsNodes().isNotEmpty()
                    }
                    val elapsed = (SystemClock.elapsedRealtimeNanos() - start) / 1e6
                    if (round >= OPEN_WARM_UP) samples += elapsed
                }
            }
            "open_${label}" to summary(samples)
        }
        report("open", mapOf("largest_example" to largest.component, "largest_example_chars" to largest.script().length.toString()) + results)
    }

    @Test
    fun undoAndRedoOfARowDeletionReachTheCanvas() {
        val name = project("edit", LARGE_SCRIPT)
        val undo = ArrayList<Double>()
        val redo = ArrayList<Double>()
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use {
            rule.waitUntil(30_000) { rule.onAllNodesWithText(FIRST_CELL).fetchSemanticsNodes().isNotEmpty() }
            // Outline ids follow document order: 1 is the Column, 2 the first Row.
            rule.onNodeWithTag("designer-outline-2").performClick()
            rule.onNodeWithTag("designer-delete-node").performClick()
            rule.waitUntil(10_000) { rule.onAllNodesWithText(FIRST_CELL).fetchSemanticsNodes().isEmpty() }
            repeat(EDIT_ROUNDS) {
                var start = SystemClock.elapsedRealtimeNanos()
                rule.onNodeWithTag("designer-undo").performClick()
                rule.waitUntil(10_000) { rule.onAllNodesWithText(FIRST_CELL).fetchSemanticsNodes().isNotEmpty() }
                undo += (SystemClock.elapsedRealtimeNanos() - start) / 1e6
                start = SystemClock.elapsedRealtimeNanos()
                rule.onNodeWithTag("designer-redo").performClick()
                rule.waitUntil(10_000) { rule.onAllNodesWithText(FIRST_CELL).fetchSemanticsNodes().isEmpty() }
                redo += (SystemClock.elapsedRealtimeNanos() - start) / 1e6
            }
        }
        report("edit", mapOf("undo_restores_row" to summary(undo), "redo_removes_row" to summary(redo)))
    }

    private fun measureStages(reader: ScriptReader, validator: DesignValidator, script: String): DoubleArray {
        var start = System.nanoTime()
        val document = reader.read(script)
        val read = System.nanoTime() - start
        start = System.nanoTime()
        validator.validate(document)
        val validate = System.nanoTime() - start
        start = System.nanoTime()
        PreviewFrames.build(document, 1)
        val frame = System.nanoTime() - start
        start = System.nanoTime()
        ScriptWriter.write(document)
        val write = System.nanoTime() - start
        return doubleArrayOf(read / 1e6, validate / 1e6, frame / 1e6, write / 1e6)
    }

    private fun countNodes(node: DesignNode): Int =
        1 + node.children.sumOf { child -> (child as? DesignChild.Node)?.node?.let(::countNodes) ?: 0 }

    /** p50 / p90 / max in milliseconds with one decimal, over the measured samples. */
    private fun summary(samples: List<Double>): String {
        val sorted = samples.sorted()
        fun at(fraction: Double) = sorted[((sorted.size - 1) * fraction).toInt()]
        return "p50=%.1f p90=%.1f max=%.1f n=%d".format(Locale.ROOT, at(0.5), at(0.9), sorted.last(), sorted.size)
    }

    private fun report(kind: String, values: Map<String, String>) {
        val fields = (mapOf("kind" to kind, "device" to "${Build.MANUFACTURER} ${Build.MODEL}", "api" to Build.VERSION.SDK_INT.toString()) + values)
            .entries.joinToString(", ", "{", "}") { (key, value) -> "\"$key\": \"$value\"" }
        Log.i(TAG, "DESIGNER_PERF $fields")
    }

    private companion object {
        const val TAG = "DesignerPerformance"
        const val WARM_UP = 3
        const val ROUNDS = 10
        const val OPEN_WARM_UP = 2
        const val OPEN_ROUNDS = 8
        const val EDIT_ROUNDS = 10
        const val FIRST_CELL = "Cell 0 a"

        /** A Column of 50 rows, each with two texts and a labelled button: about 250 nodes, 5 % of the session limit. */
        val LARGE_SCRIPT: String = buildString {
            append("compose.mount(compose.Column({padding: 8},\n")
            append((0 until 50).joinToString(",\n") { row ->
                "    compose.Row({key: 'r$row'},\n" +
                    "        compose.Text({key: 't${row}a', text: 'Cell $row a'}),\n" +
                    "        compose.Text({key: 't${row}b', text: 'Cell $row b'}),\n" +
                    "        compose.Button({key: 'b$row'},\n            'Go $row'))"
            })
            append("));\n")
        }
    }
}
