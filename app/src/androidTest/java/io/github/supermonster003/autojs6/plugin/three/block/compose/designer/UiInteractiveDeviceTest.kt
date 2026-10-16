package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.graphics.Rect
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
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.InteractivePreviewSettings
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.UiPreviewClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.autojs.plugin.ui.preview.UiPreviewContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
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
 *
 * Roadmap P11.6 (preview contract v4, skipped on older hosts): the script's dialog shows inside the canvas and its
 * button works, the back key button goes to the dialog first and otherwise ends the run with `exit`, hiding and
 * showing the designer send pause / resume while the run (and its counter) goes on, and a busy main thread ends the
 * run within the limit of the settings page while the designer stays usable. On a v4 host the v2 hide test is skipped.
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

    @After fun remove() {
        created.forEach { store.delete(it) }
        context.getSharedPreferences(InteractivePreviewSettings.PREFERENCES, android.content.Context.MODE_PRIVATE).edit().remove("ui-busy-wait-limit-seconds").commit()
    }

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
            assumeFalse("a v4 host pauses the run instead (see the lifecycle test)", client.lifecycleSupported)
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

    // ------------------------------------------------------------------------------------- contract v4 (P11.6)

    /** Waits for the v2 handshake, then skips the test when the host does not deliver lifecycle events (contract v4). */
    private fun ActivityScenario<DesignerActivity>.v4Client(): UiPreviewClient {
        val client = v2Client()
        assumeTrue("the installed AutoJs6 does not serve preview contract v4", client.lifecycleSupported)
        return client
    }

    private fun ActivityScenario<DesignerActivity>.startInteractive(client: UiPreviewClient) {
        rule.onNodeWithTag("designer-interact").performClick()
        rule.onNodeWithTag("designer-ui-interact-confirm").performClick()
        rule.waitUntil(30_000) { client.running && client.runId != null }
    }

    private fun waitForConsole(client: UiPreviewClient, text: String, timeoutMs: Long = 15_000) =
        rule.waitUntil(timeoutMs) { client.consoleLines.any { it.text.contains(text) } }

    private fun ActivityScenario<DesignerActivity>.surfaceOnScreen(): Rect {
        val bounds = rule.onNodeWithTag("designer-xml-surface-live").fetchSemanticsNode().boundsInWindow
        val location = IntArray(2)
        onActivity { it.window.decorView.getLocationOnScreen(location) }
        return Rect(location[0] + bounds.left.toInt(), location[1] + bounds.top.toInt(), location[0] + bounds.right.toInt(), location[1] + bounds.bottom.toInt())
    }

    /**
     * The first dialog reports where its button is in the host's root (the embedded tree is not part of this window's
     * accessibility tree), so the test can tap it through the canvas; the button then asks again with dialogs.confirm.
     */
    private val asking = "\"ui\";\nui.layout(\n    <vertical>\n        <button id=\"b\" w=\"*\" h=\"*\" text=\"ask\"/>\n    </vertical>\n);\n" +
        "function ask() {\n    dialogs.confirm(\"Confirm title\", \"Confirm body\", (ok) => console.log(\"confirmed \" + ok));\n}\n" +
        "console.log(\"started\");\nvar built = dialogs.build({ title: \"Built title\", content: \"Built body\", positive: \"Yes\" });\n" +
        "built.on(\"positive\", () => console.log(\"positive pressed\"));\nbuilt.show();\n" +
        "setTimeout(() => {\n    let button = built.getActionButton(Packages.com.afollestad.materialdialogs.DialogAction.POSITIVE);\n" +
        "    let at = java.lang.reflect.Array.newInstance(java.lang.Integer.TYPE, 2);\n    button.getLocationInWindow(at);\n" +
        "    console.log(\"positive at \" + [at[0], at[1], button.getWidth(), button.getHeight(), built.isShowing()].join(\",\"));\n}, 500);\n" +
        "ui.b.on(\"click\", ask);\n"

    @Test
    fun theScriptsDialogShowsInTheCanvasAndTakesTheBackKey() {
        val name = project("dialog", asking)
        launch(name).use { scenario ->
            val client = scenario.v4Client()
            scenario.startInteractive(client)
            waitForConsole(client, "started")
            // The dialog lies inside the canvas: its button has a place in the host's root, and a tap there reaches it.
            waitForConsole(client, "positive at ")
            val (x, y, width, height, showing) = client.consoleLines.first { it.text.contains("positive at ") }.text.substringAfter("positive at ").split(",")
            assertEquals("true", showing.trim())
            val surface = scenario.surfaceOnScreen()
            val button = Rect(surface.left + x.toInt(), surface.top + y.toInt(), surface.left + x.toInt() + width.toInt(), surface.top + y.toInt() + height.toInt())
            assertTrue("button $button is not inside the canvas $surface", width.toInt() > 0 && height.toInt() > 0 && surface.contains(button))
            rule.waitUntil(15_000) {
                if (client.consoleLines.none { it.text.contains("positive pressed") }) tapOnScreen(button.exactCenterX(), button.exactCenterY())
                SystemClock.sleep(500)
                client.consoleLines.any { it.text.contains("positive pressed") }
            }
            // The button asks with dialogs.confirm; that dialog takes the back key: it is cancelled, the key counts as
            // consumed and the run goes on.
            rule.waitUntil(15_000) {
                if (client.consoleLines.none { it.text.contains("confirmed") } && client.lastLifecycle == null) {
                    scenario.tapLiveSurfaceCenter()
                    SystemClock.sleep(800)
                    rule.onNodeWithTag("designer-ui-back").performClick()
                }
                SystemClock.sleep(500)
                client.lastLifecycle != null
            }
            assertEquals(UiPreviewClient.LifecycleHandled(UiPreviewContract.LIFECYCLE_BACK_PRESSED, true), client.lastLifecycle)
            waitForConsole(client, "confirmed false")
            SystemClock.sleep(500)
            assertTrue(client.running)
            assertEquals(null, client.lastExit)
        }
        assertEquals(asking, store.read(name))
        assertFalse(store.draftFile(name).exists())
    }

    @Test
    fun theBackKeyEndsARunWithoutAHandlerAndPausingKeepsTheRun() {
        val handled = counter + "ui.emitter.on(\"pause\", () => console.log(\"paused at \" + n));\n" +
            "ui.emitter.on(\"resume\", () => console.log(\"resumed at \" + n));\n"
        val name = project("pause", handled)
        launch(name).use { scenario ->
            val client = scenario.v4Client()
            scenario.startInteractive(client)
            waitForConsole(client, "started")
            val firstRun = client.runRevision
            rule.waitUntil(15_000) {
                if (client.consoleLines.none { it.text.contains("clicked") }) scenario.tapLiveSurfaceCenter()
                SystemClock.sleep(500)
                client.consoleLines.any { it.text.contains("clicked 1") }
            }
            // Hidden and shown again: the same run gets pause and resume, and its counter is still 1.
            scenario.moveToState(Lifecycle.State.CREATED)
            waitForConsole(client, "paused at 1")
            assertTrue(client.running)
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitForConsole(client, "resumed at 1")
            assertEquals(firstRun, client.runRevision)
            assertEquals(1, client.consoleLines.count { it.text.contains("started") })
            rule.waitUntil(15_000) {
                if (client.consoleLines.none { it.text.contains("clicked 2") }) scenario.tapLiveSurfaceCenter()
                SystemClock.sleep(500)
                client.consoleLines.any { it.text.contains("clicked 2") }
            }
            // No handler consumes the back key: the run ends with exit, as finishing the Activity would.
            rule.onNodeWithTag("designer-ui-back").performClick()
            rule.waitUntil(15_000) { !client.running && client.lastExit == UiPreviewContract.EXIT_SCRIPT }
            assertEquals(UiPreviewClient.LifecycleHandled(UiPreviewContract.LIFECYCLE_BACK_PRESSED, false), client.lastLifecycle)
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-ui-back").fetchSemanticsNodes().isEmpty() }
            assertTagContains("designer-ui-interact-status", UiPreviewContract.EXIT_SCRIPT)
        }
        assertEquals(handled, store.read(name))
        assertFalse(store.draftFile(name).exists())
    }

    @Test
    fun aBusyMainThreadEndsTheRunWithinTheSettingsLimit() {
        assertTrue(InteractivePreviewSettings.setBusyWaitLimitSeconds(context, 1))
        val busy = "\"ui\";\nui.layout(\n    <frame>\n        <text text=\"busy\"/>\n    </frame>\n);\nconsole.log(\"spinning\");\nwhile (true) {\n}\n"
        val name = project("busy", busy)
        launch(name).use { scenario ->
            val client = scenario.v4Client()
            val startedAt = SystemClock.uptimeMillis()
            scenario.startInteractive(client)
            rule.waitUntil(15_000) { client.lastExit == UiPreviewContract.EXIT_ERROR }
            assertTrue("the run took ${SystemClock.uptimeMillis() - startedAt} ms", SystemClock.uptimeMillis() - startedAt < 10_000)
            val wait = requireNotNull(client.busyWait)
            assertEquals(1_000L, wait.limitMs)
            assertTrue(wait.elapsedMs >= 1_000L)
            assertTrue("line ${wait.line}", wait.line in 8..9)
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-ui-script-problem").fetchSemanticsNodes().isNotEmpty() }
            val expected = scenario.activity().getString(R.string.designer_ui_interact_busy_wait_line, 1, wait.line)
            assertEquals(expected, textOf("designer-ui-script-problem"))
            // The designer stays usable: the switch goes off and the canvas shows the layout preview again.
            rule.onNodeWithTag("designer-interact").performClick()
            rule.waitUntil(15_000) { rule.onAllNodesWithTag("designer-xml-surface").fetchSemanticsNodes().isNotEmpty() }
            assertFalse(requireNotNull(scenario.activity().currentUiSession).interactive)
        }
        assertEquals(busy, store.read(name))
    }

    // ------------------------------------------------------------------------------------ contract v5 (P11.H3)

    /** The TypeScript Engine template's way: an inline onClick, and the ids through `as any` (roadmap D68). */
    private val tsxCounter = "\"ui\";\n\nlet n: number = 0;\nui.layout(\n    <vertical>\n" +
        "        <button id='b' w='*' h='*' text='count 0' onClick={() => {\n            n++;\n" +
        "            (ui as any).b.setText('count ' + n);\n            console.log('clicked ' + n);\n        }} />\n" +
        "    </vertical>\n);\nconsole.log('tsx started');\n"

    private fun tsxProject(stem: String, script: String): String {
        val project = store.create("ui-interactive-test-$stem", script, ProjectStore.TSX_EXTENSION)
        created += project.name
        return project.name
    }

    /** Waits for the v4 handshake and the parsed TSX document, then skips the test unless the host runs TSX (contract v5). */
    private fun ActivityScenario<DesignerActivity>.tsxClient(): UiPreviewClient {
        // TypeScript Engine parses the document first; the preview client comes with the opened document.
        rule.waitUntil(30_000) { activity().currentPreviewClient != null }
        val client = v4Client()
        assumeTrue("the installed AutoJs6 does not serve preview contract v5", client.tsxRunKnown)
        assumeTrue("TypeScript Engine is not installed, so AutoJs6 cannot compile TSX", client.tsxRunSupported)
        rule.waitUntil(30_000) { rule.onAllNodesWithTag("designer-interact").fetchSemanticsNodes().isNotEmpty() }
        return client
    }

    @Test
    fun aTsxScriptIsCompiledByTheHostAndATapReachesItsInlineOnClick() {
        val name = tsxProject("tsx", tsxCounter)
        launch(name).use { scenario ->
            val client = scenario.tsxClient()
            val startedAt = SystemClock.uptimeMillis()
            scenario.startInteractive(client)
            waitForConsole(client, "tsx started", 30_000)
            android.util.Log.i("UiInteractiveDeviceTest", "TSX run logged after ${SystemClock.uptimeMillis() - startedAt} ms")
            assertFalse(client.compiling)
            rule.waitUntil(15_000) {
                if (client.consoleLines.none { it.text.contains("clicked") }) scenario.tapLiveSurfaceCenter()
                SystemClock.sleep(500)
                client.consoleLines.any { it.text.contains("clicked 1") }
            }
            assertEquals(null, client.scriptError)
            rule.onNodeWithTag("designer-tab-console").performClick()
            assertConsoleShows("clicked 1")
            rule.onNodeWithTag("designer-interact").performClick()
            rule.waitUntil(15_000) { !client.running && client.lastExit == UiPreviewContract.EXIT_STOPPED }
        }
        assertEquals(tsxCounter, store.read(name))
        assertFalse(store.draftFile(name).exists())
    }

    @Test
    fun aTsxScriptThatDoesNotCompileShowsItsLineAboveTheCanvas() {
        val wrong = "\"ui\";\nui.layout(\n    <frame>\n        <text text='typed' />\n    </frame>\n);\nconst x: number = 'one';\nconsole.log('not compiled ' + x);\n"
        val name = tsxProject("tsx-wrong", wrong)
        launch(name).use { scenario ->
            val client = scenario.tsxClient()
            scenario.startInteractive(client)
            rule.waitUntil(30_000) { client.lastExit == UiPreviewContract.EXIT_ERROR }
            val failure = requireNotNull(client.compileFailure)
            assertEquals(7, failure.line)
            assertEquals(7, failure.column)
            assertTrue(failure.message, failure.message.startsWith("TS2322: "))
            assertFalse(client.compiling)
            assertTrue(client.consoleLines.none { it.text.contains("not compiled") })
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-ui-script-problem").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(scenario.activity().getString(R.string.designer_ui_interact_compile_failed_line, 7, failure.message), textOf("designer-ui-script-problem"))
            // Every error diagnostic is in the console too.
            rule.onNodeWithTag("designer-ui-script-problem").performClick()
            assertConsoleShows("TS2322")
        }
        assertEquals(wrong, store.read(name))
    }
}
