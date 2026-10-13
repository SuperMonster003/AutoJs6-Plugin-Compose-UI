package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap P9.3 step 1: the state panel changes preview values only, and in the interactive preview the
 * canvas's state-bound controls change them; the script on disk never changes and no draft is written.
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerPreviewStateDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val onCanvas = hasAnyAncestor(hasTestTag("designer-canvas-content"))
    private val script = "var on = compose.state(false);\nvar name = compose.state('Ada');\n\n" +
        "compose.mount(compose.Column({padding: 16},\n" +
        "    compose.Switch({key: 'toggle', checked: on.value}),\n" +
        "    compose.Text({key: 'label', text: name.value}),\n" +
        "    compose.TextField({key: 'field', text: name.value})));\n"

    @Before @After fun clean() { store.list().filter { it.name.startsWith("state-test") }.forEach { store.delete(it.name) } }

    private fun canvasText(text: String) = rule.onAllNodes(hasText(text) and onCanvas and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theStatePanelAndTheInteractiveCanvasChangeOnlyThePreview() {
        val name = store.create("state-test", script).name
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use {
            rule.waitUntil(5_000) { canvasText("Ada") }
            val canvasSwitch = rule.onNode(isToggleable() and onCanvas)
            canvasSwitch.assertIsOff()
            rule.onNodeWithTag("designer-tab-state").performClick()
            rule.onNodeWithTag("designer-state-on").assertIsDisplayed()

            // The panel drives the canvas, including a text field whose renderer keeps its own text.
            rule.onNodeWithTag("designer-state-value-on").performClick()
            rule.waitUntil(5_000) { rule.onAllNodes(isToggleable() and onCanvas and SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, androidx.compose.ui.state.ToggleableState.On)).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("designer-state-value-name").performScrollTo().performTextClearance()
            rule.onNodeWithTag("designer-state-value-name").performTextInput("Grace")
            rule.waitUntil(5_000) { canvasText("Grace") }
            rule.waitUntil(5_000) { rule.onAllNodes(hasSetTextAction() and onCanvas and hasText("Grace")).fetchSemanticsNodes().isNotEmpty() }

            // Edit mode: a tap on the canvas switch selects it and changes nothing.
            rule.onNodeWithTag("designer-state-reset-all").performScrollTo().performClick()
            rule.waitUntil(5_000) { canvasText("Ada") }
            canvasSwitch.performClick()
            rule.waitForIdle()
            canvasSwitch.assertIsOff()
            rule.onNodeWithTag("designer-state-value-on").assertIsOff()

            // Interactive preview: the canvas switch and text field set the preview values.
            rule.onNodeWithTag("designer-interact").performClick()
            rule.waitForIdle()
            canvasSwitch.performClick()
            rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("designer-state-value-on") and SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, androidx.compose.ui.state.ToggleableState.On)).fetchSemanticsNodes().isNotEmpty() }
            canvasSwitch.assertIsOn()
            rule.onNode(hasSetTextAction() and onCanvas).performTextInput(" Lovelace")
            rule.waitUntil(5_000) { canvasText("Ada Lovelace") }
            rule.onNodeWithTag("designer-state-value-name").assert(hasText("Ada Lovelace"))
            rule.onNodeWithTag("designer-interact").performClick()
            rule.waitForIdle()
        }
        assertEquals("preview values never reach the script", script, store.read(name))
        assertFalse("nothing to recover: the document was never edited", store.draftFile(name).exists())
    }
}
