package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap P8.3 device coverage: a new project gets components from the palette, property edits reach
 * the real-renderer canvas, the script is saved and reopened unchanged, undo / redo and outline moves
 * work, drafts are offered after an interrupted session, host options switch the mount kind, and every
 * gallery example opens in the designer.
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val created = ArrayList<String>()

    @Before fun clean() { store.list().filter { it.name.startsWith("device-test-") }.forEach { store.delete(it.name) } }
    @After fun remove() { created.forEach { store.delete(it) } }

    private fun project(stem: String, script: String = store.template()): String {
        val project = store.create("device-test-$stem", script)
        created += project.name
        return project.name
    }

    private fun launch(name: String): ActivityScenario<DesignerActivity> = ActivityScenario.launch(DesignerActivity.intent(context, name))

    @Test
    fun paletteInsertsPropertyEditsRenderAndSaveRoundTrips() {
        val name = project("palette")
        launch(name).use {
            rule.onNodeWithTag("designer-canvas-content").assertIsDisplayed()
            rule.onNodeWithTag("designer-outline").assertIsDisplayed()
            rule.onNodeWithTag("designer-add").performClick()
            rule.onNodeWithTag("designer-palette").performScrollToNode(hasTestTag("designer-palette-Button"))
            rule.onNodeWithTag("designer-palette-Button").performClick()
            rule.waitForIdle()
            // The Column root keeps its Text and gains the Button with its shorthand label: four outline rows.
            rule.onAllNodesWithTag("designer-outline-3").fetchSemanticsNodes().let { assertEquals(1, it.size) }
            rule.onNodeWithTag("designer-tab-inspector").performClick()
            rule.onNodeWithTag("designer-key").performTextClearance()
            rule.onNodeWithTag("designer-key").performTextInput("primary")
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val saved = store.read(name)
        assertTrue(saved, saved.contains("compose.Button({key: 'primary'},\n            'Button')"))
        assertTrue(saved, saved.contains("compose.Text({key: 'title', text: 'Hello', style: 'headlineSmall'})"))
        assertFalse(store.draftFile(name).exists())
        launch(name).use {
            rule.onNodeWithTag("designer-outline").assertIsDisplayed()
            rule.onNodeWithTag("designer-outline-3").assertIsDisplayed()
        }
    }

    @Test
    fun undoRedoAndOutlineMovesEditTheTree() {
        val name = project("moves", "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'a', text: 'A'}),\n    compose.Text({key: 'b', text: 'B'})));\n")
        launch(name).use {
            rule.onNodeWithTag("designer-outline-3").performClick()
            rule.onNodeWithTag("designer-move-up").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-undo").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-redo").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val saved = store.read(name)
        assertTrue(saved, saved.indexOf("key: 'b'") < saved.indexOf("key: 'a'"))
        launch(name).use {
            rule.onNodeWithTag("designer-outline-2").performClick()
            rule.onNodeWithTag("designer-delete-node").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        assertFalse(store.read(name).contains("key: 'b'"))
    }

    @Test
    fun canvasDragDropsTheNodeWhereThePointerIs() {
        val name = project("canvas-drop", "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'a', text: 'Alpha'}),\n    compose.Text({key: 'b', text: 'Beta'}),\n    compose.Text({key: 'c', text: 'Gamma'})));\n")
        launch(name).use {
            val onCanvas = hasAnyAncestor(hasTestTag("designer-canvas-content"))
            fun boundsOf(text: String) = rule.onNode(hasText(text) and onCanvas).fetchSemanticsNode().boundsInRoot
            val overlay = rule.onNodeWithTag("designer-canvas-overlay").fetchSemanticsNode().boundsInRoot.topLeft
            val gamma = boundsOf("Gamma")
            val alpha = boundsOf("Alpha")
            // Long-press Gamma, then drag it just above Alpha: it becomes the Column's first child, not its last.
            rule.onNodeWithTag("designer-canvas-overlay").performTouchInput {
                down(gamma.center - overlay)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
                moveTo(Offset(gamma.center.x, (gamma.center.y + alpha.top) / 2) - overlay, delayMillis = 100)
                moveTo(Offset(alpha.center.x, alpha.top + 1f) - overlay, delayMillis = 100)
                up()
            }
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val saved = store.read(name)
        assertTrue(saved, saved.indexOf("key: 'c'") < saved.indexOf("key: 'a'") && saved.indexOf("key: 'a'") < saved.indexOf("key: 'b'"))
    }

    @Test
    fun modifierChainDragMovesAnOperationWhereItIsReleased() {
        val name = project("modifier-drag", "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'x', text: 'X', modifier: compose.modifier().padding(4).width(40).height(20)})));\n")
        launch(name).use {
            rule.onNodeWithTag("designer-outline-2").performClick()
            rule.onNodeWithTag("designer-tab-inspector").performClick()
            rule.onNodeWithTag("designer-modifier-handle-2").performScrollTo()
            val first = rule.onNodeWithTag("designer-modifier-0").fetchSemanticsNode().boundsInRoot
            val handle = rule.onNodeWithTag("designer-modifier-handle-2").fetchSemanticsNode().boundsInRoot
            // Drag the third operation (height) by its handle to just above the first one (padding).
            rule.onNodeWithTag("designer-modifier-handle-2").performTouchInput {
                down(center)
                moveBy(Offset(0f, -viewConfiguration.touchSlop * 2), delayMillis = 50)
                moveTo(Offset(center.x, center.y + (first.top + 2f - handle.center.y)), delayMillis = 100)
                up()
            }
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
            val saved = store.read(name)
            assertTrue(saved, saved.contains(".height(20).padding(4).width(40)"))
            // The whole drag is one step: a single undo restores the original chain.
            rule.onNodeWithTag("designer-undo").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val undone = store.read(name)
        assertTrue(undone, undone.contains(".padding(4).width(40).height(20)"))
    }

    @Test
    fun aTextFieldOnTheCanvasFollowsItsTextProperty() {
        val name = project("field-text", "compose.mount(compose.Column({padding: 16},\n    compose.TextField({key: 'field', text: 'One'})));\n")
        launch(name).use {
            val onCanvas = hasAnyAncestor(hasTestTag("designer-canvas-content"))
            rule.waitUntil(5_000) { rule.onAllNodes(hasSetTextAction() and onCanvas and hasText("One")).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("designer-outline-2").performClick()
            rule.onNodeWithTag("designer-tab-inspector").performClick()
            // The renderer treats a field's text as its initial text; the canvas still has to show an edited one.
            val property = hasSetTextAction() and hasAnyAncestor(hasTestTag("designer-inspector")) and hasText("One")
            rule.onNode(property).performScrollTo().performTextReplacement("Two")
            rule.waitUntil(5_000) { rule.onAllNodes(hasSetTextAction() and onCanvas and hasText("Two")).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("designer-undo").performClick()
            rule.waitUntil(5_000) { rule.onAllNodes(hasSetTextAction() and onCanvas and hasText("One")).fetchSemanticsNodes().isNotEmpty() }
        }
    }

    @Test
    fun draftIsOfferedAndRestoredAfterAnInterruptedSession() {
        val name = project("draft")
        store.writeDraft(name, "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'draft', text: 'Draft'})));\n")
        launch(name).use {
            rule.onNodeWithTag("designer-draft-restore").assertIsDisplayed()
            rule.onNodeWithTag("designer-draft-restore").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-outline-2").assertIsDisplayed()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        assertTrue(store.read(name).contains("key: 'draft'"))
        store.writeDraft(name, "compose.mount(compose.Text({text: 'other'}));\n")
        launch(name).use {
            rule.onNodeWithTag("designer-draft-discard").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-outline-1").assertIsDisplayed()
        }
        assertFalse(store.draftFile(name).exists())
    }

    @Test
    fun discardingOnExitLeavesNoDraftBehind() {
        val name = project("discard")
        val before = store.read(name)
        launch(name).use { scenario ->
            rule.onNodeWithTag("designer-outline-2").performClick()
            rule.onNodeWithTag("designer-delete-node").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-back").performClick()
            rule.onNodeWithTag("designer-exit-discard").performClick()
            // finish() runs onStop, which used to write the discarded edits back as a draft. API 24 may hold back
            // onStop / onDestroy of a finished activity for up to 10 s while the next one is not idle.
            rule.waitUntil(15_000) { scenario.state == Lifecycle.State.DESTROYED }
        }
        assertFalse("discarded edits are not kept as a draft", store.draftFile(name).exists())
        assertEquals(before, store.read(name))
    }

    @Test
    fun hostOptionsSwitchTheMountKind() {
        val name = project("host")
        launch(name).use {
            rule.onNodeWithTag("designer-menu").performClick()
            rule.onNodeWithTag("designer-menu-host").performClick()
            rule.onNodeWithTag("designer-host-floaty").performClick()
            rule.onNodeWithTag("designer-host-apply").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        assertTrue(store.read(name).contains("compose.floaty(function () {"))
    }

    @Test
    fun everyGalleryExampleOpensInTheDesigner() {
        GalleryCatalog.entries.forEach { entry ->
            val name = project("gallery-" + entry.component.lowercase(), entry.script())
            launch(name).use {
                rule.onNodeWithTag("designer-canvas-content").assertIsDisplayed()
                rule.onNodeWithTag("designer-outline-1").assertIsDisplayed()
                rule.onAllNodesWithTag("designer-parse-error").fetchSemanticsNodes().let { assertEquals(entry.component, 0, it.size) }
            }
            store.delete(name)
            created -= name
        }
    }

    @Test
    fun unparseableScriptsShowThePosition() {
        val name = project("broken", "let page = compose.Column({});\ncompose.mount(page);\n")
        launch(name).use {
            rule.onNodeWithTag("designer-parse-error").assertIsDisplayed()
        }
    }
}
