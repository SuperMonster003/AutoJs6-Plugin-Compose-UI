package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.UiPreviewClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.autojs.plugin.ui.preview.UiPreviewContract
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
 * Roadmap P10.6: the interactive preview of a `"ui";` project runs the whole script in AutoJs6 (preview contract
 * v2, API 33+). With such a host installed: the switch asks once per project, then the host runs the script in the
 * canvas, a tap on the embedded button reaches the script and its console output shows in the console tab, a script
 * error shows above the canvas, hiding the designer stops the script and showing it runs it again, and turning the
 * switch off stops it; the document, the file and the draft are untouched throughout. Skipped below API 33 and on a
 * host without `runScript`.
 */
@RunWith(AndroidJUnit4::class)
internal class UiInteractiveDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val store = ProjectStore(context)
    private val created = ArrayList<String>()

    @Before fun clean() {
        assumeTrue("the interactive preview needs API 33", Build.VERSION.SDK_INT >= UiPreviewContract.MIN_API_INTERACTIVE)
        assumeTrue("the host with the preview service is not installed", UiPreviewClient.availability(context) == null)
        store.list().filter { it.name.startsWith("ui-interactive-test-") }.forEach { store.delete(it.name) }
        context.getSharedPreferences("designer", android.content.Context.MODE_PRIVATE).edit().remove("ui-interactive-confirmed").apply()
    }

    @After fun remove() { created.forEach { store.delete(it) } }

    private val counter = "\"ui\";\nui.layout(\n    <vertical>\n        <button id=\"b\" w=\"*\" h=\"*\" text=\"count 0\"/>\n    </vertical>\n);\nvar n = 0;\nconsole.log(\"started\");\nui.b.on(\"click\", () => {\n    n++;\n    ui.b.setText(\"count \" + n);\n    console.log(\"clicked \" + n);\n});\n"

    private fun project(stem: String, script: String): String {
        val project = store.create("ui-interactive-test-$stem", script)
        created += project.name
        return project.name
    }

    private fun launch(name: String): ActivityScenario<DesignerActivity> = ActivityScenario.launch(DesignerActivity.intent(context, name))

    private fun ActivityScenario<DesignerActivity>.activity(): DesignerActivity {
        lateinit var activity: DesignerActivity
        onActivity { activity = it }
        return activity
    }

    /** Waits for the handshake and skips the test when the host has no `runScript` (a v1 host). */
    private fun ActivityScenario<DesignerActivity>.v2Client(): UiPreviewClient {
        val client = requireNotNull(activity().currentPreviewClient)
        rule.waitUntil(30_000) { client.status is UiPreviewClient.Status.Ready || client.status is UiPreviewClient.Status.Unavailable }
        assertTrue(client.status.toString(), client.status is UiPreviewClient.Status.Ready)
        assumeTrue("the installed AutoJs6 does not offer runScript (preview contract v2)", client.scriptRunSupported)
        return client
    }

    /** A tap injected through the system: the interactive surface sits on top of the window, out of Compose's reach. */
    private fun tapOnScreen(x: Float, y: Float) {
        val automation = instrumentation.uiAutomation
        val now = SystemClock.uptimeMillis()
        MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }.let { automation.injectInputEvent(it, true); it.recycle() }
        MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, x, y, 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }.let { automation.injectInputEvent(it, true); it.recycle() }
    }

    private fun textOf(tag: String): String =
        rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text } ?: ""

    private fun assertTagContains(tag: String, expected: String) {
        val actual = textOf(tag)
        assertTrue("$tag shows '$actual'", actual.contains(expected))
    }

    /** The console tab's lines (a lazy list: the texts composed right now). */
    private fun assertConsoleShows(expected: String) {
        val lines = rule.onAllNodesWithTag("designer-ui-console-line", useUnmergedTree = true).fetchSemanticsNodes()
            .map { it.config.getOrNull(SemanticsProperties.Text)?.joinToString { text -> text.text } ?: "" }
        assertTrue("console lines: $lines", lines.any { it.contains(expected) })
    }

    private fun ActivityScenario<DesignerActivity>.tapLiveSurfaceCenter() {
        val bounds = rule.onNodeWithTag("designer-xml-surface-live").fetchSemanticsNode().boundsInWindow
        val location = IntArray(2)
        onActivity { it.window.decorView.getLocationOnScreen(location) }
        tapOnScreen(location[0] + bounds.center.x, location[1] + bounds.center.y)
    }

    @Test
    fun theSwitchRunsTheScriptInTheHostAndATapReachesIt() {
        val name = project("counter", counter)
        launch(name).use { scenario ->
            val client = scenario.v2Client()
            val session = requireNotNull(scenario.activity().currentUiSession)
            // The first time, the switch asks; cancelling keeps editing.
            rule.onNodeWithTag("designer-interact").performClick()
            rule.onNodeWithTag("designer-ui-interact-cancel").performClick()
            rule.waitForIdle()
            assertFalse(session.interactive)
            assertTrue(rule.onAllNodesWithTag("designer-xml-surface-live").fetchSemanticsNodes().isEmpty())
            rule.onNodeWithTag("designer-interact").performClick()
            rule.onNodeWithTag("designer-ui-interact-confirm").performClick()
            rule.waitUntil(30_000) { rule.onAllNodesWithTag("designer-ui-live", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            assertTrue(session.interactive)
            assertTrue(client.running)
            rule.onNodeWithTag("designer-xml-surface-live").assertIsDisplayed()
            rule.waitUntil(15_000) { client.consoleLines.any { it.text.contains("started") } }
            rule.onNodeWithTag("designer-tab-console").performClick()
            rule.onNodeWithTag("designer-ui-console-status").assertIsDisplayed()
            assertConsoleShows("started")
            // The embedded button fills the layout: a tap on the surface's centre reaches the script's click handler.
            rule.waitUntil(15_000) {
                if (client.consoleLines.none { it.text.contains("clicked") }) scenario.tapLiveSurfaceCenter()
                SystemClock.sleep(500)
                client.consoleLines.any { it.text.contains("clicked 1") }
            }
            assertConsoleShows("clicked 1")
            // Off: the script stops and the canvas returns to the layout preview of the document.
            rule.onNodeWithTag("designer-interact").performClick()
            rule.waitUntil(15_000) { !client.running && client.lastExit == UiPreviewContract.EXIT_STOPPED }
            rule.waitUntil(15_000) { rule.onAllNodesWithTag("designer-xml-surface").fetchSemanticsNodes().isNotEmpty() }
            assertFalse(session.interactive)
            assertFalse(session.dirty)
            assertEquals(counter, session.script())
        }
        assertEquals(counter, store.read(name))
        assertFalse(store.draftFile(name).exists())
        // The confirmation is remembered per project: the switch goes straight on.
        launch(name).use { scenario ->
            val client = scenario.v2Client()
            rule.onNodeWithTag("designer-interact").performClick()
            assertTrue(rule.onAllNodesWithTag("designer-ui-interact-confirm").fetchSemanticsNodes().isEmpty())
            rule.waitUntil(30_000) { client.running && client.runId != null }
        }
    }

    @Test
    fun hidingTheDesignerStopsTheScriptAndShowingItRunsAgain() {
        val name = project("lifecycle", counter)
        launch(name).use { scenario ->
            val client = scenario.v2Client()
            rule.onNodeWithTag("designer-interact").performClick()
            rule.onNodeWithTag("designer-ui-interact-confirm").performClick()
            rule.waitUntil(30_000) { client.running && client.runId != null }
            val firstRun = client.runRevision
            scenario.moveToState(Lifecycle.State.CREATED)
            rule.waitUntil(15_000) { !client.running && client.lastExit == UiPreviewContract.EXIT_STOPPED }
            scenario.moveToState(Lifecycle.State.RESUMED)
            rule.waitUntil(30_000) { client.runRevision > firstRun && client.running && client.runId != null }
            assertTrue(requireNotNull(scenario.activity().currentUiSession).interactive)
        }
        assertEquals(counter, store.read(name))
        assertFalse(store.draftFile(name).exists())
    }

    @Test
    fun aScriptErrorShowsAboveTheCanvasAndInTheConsole() {
        val name = project("error", "\"ui\";\nui.layout(\n    <frame>\n        <text text=\"boom\"/>\n    </frame>\n);\nthrow new Error(\"boom at start\");\n")
        launch(name).use { scenario ->
            val client = scenario.v2Client()
            rule.onNodeWithTag("designer-interact").performClick()
            rule.onNodeWithTag("designer-ui-interact-confirm").performClick()
            rule.waitUntil(30_000) { client.lastExit == UiPreviewContract.EXIT_ERROR }
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-ui-script-problem").fetchSemanticsNodes().isNotEmpty() }
            assertTagContains("designer-ui-script-problem", "boom at start")
            rule.onNodeWithTag("designer-ui-script-problem").performClick()
            assertTagContains("designer-ui-console-status", "boom at start")
            assertFalse(client.running)
        }
        assertFalse(store.draftFile(name).exists())
    }
}
