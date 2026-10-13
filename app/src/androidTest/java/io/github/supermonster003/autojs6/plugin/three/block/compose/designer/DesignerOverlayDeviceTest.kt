package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap P9.1: open window components are drawn over the page inside the canvas, with their slots and
 * children from the real renderer; no dialog or popup window opens, and a tap on the drawn popup selects
 * inside it.
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerOverlayDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val onCanvas = hasAnyAncestor(hasTestTag("designer-canvas-content"))

    @Before @After fun clean() { store.list().filter { it.name.startsWith("overlay-test-") }.forEach { store.delete(it.name) } }

    private fun launch(stem: String, script: String): ActivityScenario<DesignerActivity> =
        ActivityScenario.launch(DesignerActivity.intent(context, store.create("overlay-test-$stem", script).name))

    private fun assertNoWindow() {
        assertEquals(0, rule.onAllNodes(isDialog()).fetchSemanticsNodes().size)
        assertEquals(0, rule.onAllNodes(isPopup()).fetchSemanticsNodes().size)
    }

    /** Taps the canvas where [point] (root coordinates) is, then shows the inspector. */
    private fun tapAndInspect(point: Offset) {
        val overlay = rule.onNodeWithTag("designer-canvas-overlay").fetchSemanticsNode().boundsInRoot.topLeft
        rule.onNodeWithTag("designer-canvas-overlay").performTouchInput { click(point - overlay) }
        rule.waitForIdle()
        rule.onNodeWithTag("designer-tab-inspector").performClick()
        rule.waitForIdle()
    }

    /** The inspector's header names the selected node's type before anything else. */
    private fun assertInspecting(type: String) {
        val texts = rule.onAllNodes(hasAnyAncestor(hasTestTag("designer-inspector")) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .fetchSemanticsNodes().flatMap { node -> node.config[SemanticsProperties.Text].map { it.text } }
        assertEquals(texts.take(4).toString(), type, texts.firstOrNull())
    }

    @Test
    fun anOpenDialogIsDrawnOnTheCanvasAndItsButtonsSelect() {
        launch("dialog", "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'under', text: 'Under the dialog'}),\n    compose.AlertDialog({key: 'confirm', open: true, title: 'Delete file?', text: 'This cannot be undone.',\n        confirm: compose.TextButton({}, 'Delete'),\n        dismiss: compose.TextButton({}, 'Cancel')})));\n").use {
            rule.waitUntil(5_000) { rule.onAllNodes(hasText("Delete file?") and onCanvas).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("designer-overlay-dialog").assertIsDisplayed()
            rule.onNode(hasText("This cannot be undone.") and onCanvas).assertIsDisplayed()
            rule.onNode(hasText("Under the dialog") and onCanvas).assertIsDisplayed()
            assertNoWindow()
            val cancel = rule.onNode(hasText("Cancel") and onCanvas).fetchSemanticsNode().boundsInRoot
            val delete = rule.onNode(hasText("Delete") and onCanvas).fetchSemanticsNode().boundsInRoot
            assertTrue("dismiss comes before confirm: $cancel / $delete", cancel.right <= delete.left)
            // The merged button bounds: its label in the middle is a shorthand Text node of its own, the padding
            // beside the label is the button.
            tapAndInspect(delete.center)
            assertInspecting("Text")
            tapAndInspect(Offset(delete.left + 4f, delete.center.y))
            assertInspecting("TextButton")
        }
    }

    @Test
    fun anOpenSheetSitsAtTheBottomAndATapOnItSelectsTheSheet() {
        launch("sheet", "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'under', text: 'Under the sheet'}),\n    compose.ModalBottomSheet({key: 'sheet', open: true},\n        compose.Text({key: 'inside', text: 'In the sheet', padding: 24}))));\n").use {
            rule.waitUntil(5_000) { rule.onAllNodes(hasText("In the sheet") and onCanvas).fetchSemanticsNodes().isNotEmpty() }
            assertNoWindow()
            val canvas = rule.onNodeWithTag("designer-canvas-content").fetchSemanticsNode().boundsInRoot
            val inside = rule.onNode(hasText("In the sheet") and onCanvas).fetchSemanticsNode().boundsInRoot
            assertTrue("the sheet content is in the lower half of the canvas: $inside in $canvas", inside.top > canvas.center.y)
            // Above the content is the sheet's drag handle area: it belongs to the sheet, not to what lies under it.
            tapAndInspect(Offset(canvas.center.x, inside.top - 12f))
            assertInspecting("ModalBottomSheet")
        }
    }

    @Test
    fun anOpenMenuOpensBelowItsAnchor() {
        launch("menu", "compose.mount(compose.Column({padding: 16},\n    compose.DropdownMenu({key: 'menu', open: true,\n        anchor: compose.Button({}, 'Open menu')},\n        compose.DropdownMenuItem({key: 'edit', text: 'Edit'}),\n        compose.DropdownMenuItem({key: 'share', text: 'Share'}))));\n").use {
            rule.waitUntil(5_000) { rule.onAllNodes(hasText("Share") and onCanvas).fetchSemanticsNodes().isNotEmpty() }
            assertNoWindow()
            val anchor = rule.onNode(hasText("Open menu") and onCanvas).fetchSemanticsNode().boundsInRoot
            val menu = rule.onNodeWithTag("designer-overlay-menu").fetchSemanticsNode().boundsInRoot
            assertTrue("menu $menu below anchor $anchor", menu.top >= anchor.bottom && menu.left <= anchor.left + 1f)
            tapAndInspect(rule.onNode(hasText("Share") and onCanvas).fetchSemanticsNode().boundsInRoot.center)
            assertInspecting("DropdownMenuItem")
        }
    }

    @Test
    fun anOpenTooltipShowsAboveItsAnchorOrBelowWhenThereIsNoRoom() {
        launch("tooltip", "compose.mount(compose.Column({padding: 16, spacing: 120},\n    compose.Tooltip({key: 'top', tooltip: 'Top tip', open: true},\n        compose.Button({}, 'First')),\n    compose.Tooltip({key: 'lower', tooltip: 'Lower tip', open: true},\n        compose.Button({}, 'Second'))));\n").use {
            rule.waitUntil(5_000) { rule.onAllNodes(hasText("Lower tip") and onCanvas).fetchSemanticsNodes().isNotEmpty() }
            assertNoWindow()
            fun boundsOf(text: String) = rule.onNode(hasText(text) and onCanvas).fetchSemanticsNode().boundsInRoot
            assertTrue("tip below its anchor at the top edge", boundsOf("Top tip").top >= boundsOf("First").bottom)
            assertTrue("tip above its anchor", boundsOf("Lower tip").bottom <= boundsOf("Second").top)
        }
    }

    @Test
    fun aMenuOpenedInsideASheetIsDrawnAboveTheSheet() {
        launch("nested", "compose.mount(compose.Column({padding: 16},\n    compose.ModalBottomSheet({key: 'sheet', open: true},\n        compose.Column({padding: 24},\n            compose.DropdownMenu({key: 'menu', open: true,\n                anchor: compose.Button({}, 'More')},\n                compose.DropdownMenuItem({key: 'edit', text: 'Edit'}))))));\n").use {
            rule.waitUntil(5_000) { rule.onAllNodes(hasText("Edit") and onCanvas).fetchSemanticsNodes().isNotEmpty() }
            assertNoWindow()
            val anchor = rule.onNode(hasText("More") and onCanvas).fetchSemanticsNode().boundsInRoot
            val menu = rule.onNodeWithTag("designer-overlay-menu").fetchSemanticsNode().boundsInRoot
            // No room below the anchor at the bottom of the sheet: the menu opens above it instead of covering it.
            assertTrue("menu $menu above anchor $anchor", menu.bottom <= anchor.top && menu.left <= anchor.left + 1f)
            // The menu is over the sheet, so its item takes the tap rather than the sheet underneath.
            tapAndInspect(rule.onNode(hasText("Edit") and onCanvas).fetchSemanticsNode().boundsInRoot.center)
            assertInspecting("DropdownMenuItem")
        }
    }
}
