package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxPreview
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap P9.3 step 2 (D43): the interactive preview runs a JavaScript project's script in the sandbox. A click on
 * the counter example's button updates the canvas through the script's state, the console shows what the script
 * writes and its errors, an endless loop stops the preview without blocking the designer, and leaving the preview
 * leaves the document and its file as they were.
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerSandboxDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val onCanvas = hasAnyAncestor(hasTestTag("designer-canvas-content"))

    @Before fun needsTheSandbox() = assumeTrue("the sandbox needs Android 8.0 (java.time)", SandboxPreview.SUPPORTED)

    @Before @After fun clean() { store.list().filter { it.name.startsWith("sandbox-test") }.forEach { store.delete(it.name) } }

    private fun canvasText(text: String) = rule.onAllNodes(hasText(text) and onCanvas and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()

    private fun clickOnCanvas(label: String) = rule.onNode(hasText(label) and hasClickAction() and onCanvas).performClick()

    private fun consoleHas(text: String) = rule.onAllNodes(hasTestTag("designer-console-line") and hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private fun enterPreview() {
        rule.onNodeWithTag("designer-interact").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("designer-sandbox-live", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun theCounterButtonUpdatesTheCanvasAndTheDocumentStaysAsItWas() {
        val script = context.assets.open("examples/counter.js").use { it.readBytes().toString(Charsets.UTF_8) }
        val name = store.create("sandbox-test-counter", script).name
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use {
            // In edit mode the canvas shows the document, where the count is an expression.
            rule.waitUntil(5_000) { canvasText("Compose counter") && !canvasText("Count: 0") }
            enterPreview()
            assertTrue(canvasText("Count: 0"))
            clickOnCanvas("Increment")
            rule.waitUntil(5_000) { canvasText("Count: 1") }
            clickOnCanvas("Increment")
            rule.waitUntil(5_000) { canvasText("Count: 2") }
            clickOnCanvas("Reset")
            rule.waitUntil(5_000) { canvasText("Count: 0") }
            clickOnCanvas("Increment")
            rule.waitUntil(5_000) { canvasText("Count: 1") }

            // Leaving the preview shows the document again.
            rule.onNodeWithTag("designer-interact").performClick()
            rule.waitUntil(5_000) { !canvasText("Count: 1") }
            assertEquals(0, rule.onAllNodesWithTag("designer-sandbox-live", useUnmergedTree = true).fetchSemanticsNodes().size)
            // Edit mode again: a tap selects the button instead of running its handler.
            rule.onNode(hasText("Increment") and onCanvas, useUnmergedTree = true).performClick()
            rule.waitForIdle()
            assertFalse(canvasText("Count: 1"))
        }
        assertEquals("running the script never changes the file", script, store.read(name))
        assertFalse("nothing to recover: the document was never edited", store.draftFile(name).exists())
    }

    @Test
    fun theConsoleShowsOutputAndErrorsAndTheScriptGoesOn() {
        val script = "var n = compose.state(0);\nconsole.log('started');\n\n" +
            "compose.mount(function () {\n" +
            "    return compose.Column(\n" +
            "        compose.Button({key: 'log', onClick: function () {\n" +
            "            n.value = n.value + 1;\n" +
            "            toast('clicked ' + n.value);\n" +
            "        }}, 'Log'),\n" +
            "        compose.Button({key: 'fail', onClick: function () {\n" +
            "            missing();\n" +
            "        }}, 'Fail'),\n" +
            "        compose.Text({key: 'count', text: 'n = ' + n.value}));\n" +
            "});\n"
        val name = store.create("sandbox-test-console", script).name
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use {
            rule.waitUntil(5_000) { canvasText("Log") }
            enterPreview()
            assertTrue(canvasText("n = 0"))
            rule.onNodeWithTag("designer-tab-console").performClick()
            rule.waitUntil(5_000) { consoleHas("started") }
            clickOnCanvas("Log")
            rule.waitUntil(5_000) { canvasText("n = 1") && consoleHas("[toast] clicked 1") }
            clickOnCanvas("Fail")
            rule.waitUntil(5_000) { consoleHas("missing") && rule.onAllNodesWithTag("designer-sandbox-problem").fetchSemanticsNodes().isNotEmpty() }
            // A script error does not stop the preview.
            clickOnCanvas("Log")
            rule.waitUntil(5_000) { canvasText("n = 2") }
            rule.onNodeWithTag("designer-console-clear").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-console-line").fetchSemanticsNodes().isEmpty() }
        }
        assertEquals(script, store.read(name))
    }

    @Test
    fun aFieldTheScriptAddsAndRemovesIsDrawnRightAway() {
        // The script's tree changes inside a frame of the canvas: a field that joins it must be bound before it is
        // drawn, and one that leaves it must not be looked up afterwards (1.5.0 crashed with NODE_DETACHED).
        val script = "var open = compose.state(false);\n\n" +
            "compose.mount(function () {\n" +
            "    return compose.Column(\n" +
            "        compose.Button({key: 'toggle', onClick: function () {\n" +
            "            open.value = !open.value;\n" +
            "        }}, 'Toggle'),\n" +
            "        open.value ? compose.TextField({key: 'name', text: 'Ada'}) : compose.Text({key: 'closed', text: 'Closed'}),\n" +
            "        compose.Text({key: 'state', text: open.value ? 'Open' : 'Shut'}));\n" +
            "});\n"
        val name = store.create("sandbox-test-field", script).name
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use {
            rule.waitUntil(5_000) { canvasText("Toggle") }
            enterPreview()
            repeat(3) {
                clickOnCanvas("Toggle")
                rule.waitUntil(5_000) { canvasText("Open") && rule.onAllNodes(hasSetTextAction() and onCanvas and hasText("Ada")).fetchSemanticsNodes().size == 1 }
                clickOnCanvas("Toggle")
                rule.waitUntil(5_000) { canvasText("Shut") && rule.onAllNodes(hasSetTextAction() and onCanvas).fetchSemanticsNodes().isEmpty() }
            }
        }
        assertEquals(script, store.read(name))
    }

    @Test
    fun anEndlessLoopStopsThePreviewAndTheDesignerStaysResponsive() {
        val script = "compose.mount(compose.Column(\n" +
            "    compose.Button({key: 'hang', onClick: function () {\n" +
            "        while (true) {}\n" +
            "    }}, 'Hang'),\n" +
            "    compose.Text({key: 'label', text: 'Waiting'})));\n"
        val name = store.create("sandbox-test-loop", script).name
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use {
            rule.waitUntil(5_000) { canvasText("Waiting") }
            enterPreview()
            clickOnCanvas("Hang")
            // The loop runs on the sandbox thread: the designer keeps answering while the limit is reached.
            rule.onNodeWithTag("designer-tab-console").performClick()
            rule.waitUntil(10_000) { rule.onAllNodesWithTag("designer-sandbox-problem").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("designer-interact").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-sandbox-problem").fetchSemanticsNodes().isEmpty() }
            // Entering the preview again starts the script afresh.
            enterPreview()
            assertTrue(canvasText("Waiting"))
        }
        assertEquals(script, store.read(name))
    }
}
