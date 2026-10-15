package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.DeclarationEvaluator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.UiPreviewClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap P10.3: a `"ui";` project opens on the XML canvas; with an AutoJs6 that has the preview service the canvas
 * receives the host's bounds for every element, a tap selects by those bounds, an edit brings a new frame, a
 * failing frame points at its element, undo recovers and saving writes the edited script. Without the service the
 * canvas shows why (the host or its service is missing) and the rest is skipped.
 */
@RunWith(AndroidJUnit4::class)
internal class UiPreviewDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)

    @Before @After fun clean() { store.list().filter { it.name.startsWith("ui-preview-test") }.forEach { store.delete(it.name) } }

    /** A tap injected through the system (Compose's touch injection reflects on a hidden API that API 37 removed). */
    private fun tapOnScreen(x: Float, y: Float) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val now = SystemClock.uptimeMillis()
        MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }.let { automation.injectInputEvent(it, true); it.recycle() }
        MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, x, y, 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }.let { automation.injectInputEvent(it, true); it.recycle() }
    }

    private val script = """
        "ui";
        var title = "Hello preview";
        ui.layout(
            <vertical id="root" padding="16">
                <text id="title" text="{{title}}" textSize="24sp"/>
                <button id="ok" text="OK"/>
                <text id="note" text="A note"/>
            </vertical>
        );
    """.trimIndent() + "\n"

    @Test
    fun theCanvasShowsTheHostFrameSelectsByBoundsAndFollowsEdits() {
        val name = store.create("ui-preview-test-frame", script).name
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use { scenario ->
            rule.onNodeWithTag("designer-xml-canvas").assertIsDisplayed()
            rule.onNodeWithTag("designer-xml-outline").assertIsDisplayed()
            lateinit var activity: DesignerActivity
            scenario.onActivity { activity = it }
            fun client() = requireNotNull(activity.currentPreviewClient)
            fun session() = requireNotNull(activity.currentUiSession)
            val titleId = session().document.elements().first { it.attr("id")?.value == "title" }.id
            val okId = session().document.elements().first { it.attr("id")?.value == "ok" }.id
            val noteId = session().document.elements().first { it.attr("id")?.value == "note" }.id

            if (UiPreviewClient.availability(context) != null) {
                // No host, or a host without the service: the canvas says so and the outline still works.
                rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-xml-status").fetchSemanticsNodes().isNotEmpty() }
                rule.onNodeWithTag("designer-xml-outline-$okId").performClick()
                rule.waitUntil(5_000) { session().selectedId == okId }
                return
            }

            rule.waitUntil(30_000) { client().status is UiPreviewClient.Status.Ready || client().status is UiPreviewClient.Status.Unavailable }
            assertTrue(client().status.toString(), client().status is UiPreviewClient.Status.Ready)
            // The snapshot mode reports the bounds before the pixels arrive with the layout result.
            // A cold host starts its first script engine slowly (an x86 emulator took 18 s) and may need a second attempt.
            rule.waitUntil(90_000) { session().previewFrames > 0 && client().bounds.isNotEmpty() && (client().embedded || client().snapshot != null) }
            val bounds = client().bounds
            assertEquals(4, client().elementCount)
            assertEquals(listOf(0, 1, 2, 3), bounds.map { it.index }.sorted())
            assertEquals(listOf("root", "title", "ok", "note"), bounds.sortedBy { it.index }.map { it.id })
            if (!client().embedded) assertNotNull("the snapshot mode shows a bitmap", client().snapshot)
            // The problems follow the frame in a later composition. With the declaration evaluator (Android 8.0+) the
            // frame carries {{title}}'s value and has no problem; below, the host cannot know `title` and says so.
            rule.waitUntil(10_000) {
                if (DeclarationEvaluator.SUPPORTED) session().previewProblems.isEmpty() else session().previewProblems.any { "title" in it }
            }

            // A tap on the button's bounds selects the button (zoom 1: canvas pixels are root pixels).
            val button = bounds.first { it.index == 2 }
            val origin = rule.onNodeWithTag("designer-xml-overlay").fetchSemanticsNode().positionOnScreen
            tapOnScreen(origin.x + (button.left + button.right) / 2f, origin.y + (button.top + button.bottom) / 2f)
            rule.waitUntil(5_000) { session().selectedId == okId }
            rule.onNodeWithTag("designer-xml-outline-$okId").assertIsDisplayed()

            // An edit brings a new frame from the host.
            val before = session().previewFrames
            scenario.onActivity { it.currentUiSession!!.execute(UiEditCommand.SetAttr(titleId, "text", "Changed"), merge = false) }
            rule.waitUntil(30_000) { session().previewFrames > before }
            assertTrue(session().script().contains("text=\"Changed\""))

            // A failing frame points at its element; undo brings the previous frame back.
            scenario.onActivity { it.currentUiSession!!.execute(UiEditCommand.SetTag(noteId, "nope"), merge = false) }
            rule.waitUntil(30_000) { session().problemElementId == noteId }
            assertTrue(session().previewProblems.toString(), session().previewProblems.any { it.contains("nope") })
            assertTrue(rule.onAllNodesWithTag("designer-xml-status").fetchSemanticsNodes().isNotEmpty())
            val failed = session().previewFrames
            scenario.onActivity { it.currentUiSession!!.undo() }
            rule.waitUntil(30_000) { session().problemElementId == null && session().previewFrames > failed }

            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val saved = store.read(name)
        assertTrue(saved, saved.contains("<text id=\"title\" text=\"Changed\" textSize=\"24sp\"/>"))
        assertTrue(saved, saved.contains("<text id=\"note\" text=\"A note\"/>"))
        assertFalse(store.draftFile(name).exists())
    }
}
