package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxPreview
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
 * Roadmap P9.3: the state panel changes preview values only (step 1), and the interactive preview of a JavaScript
 * project runs its script from those values (step 2, Android 8.0 or later), whose handlers change the canvas and
 * nothing else; below Android 8.0 state-bound controls set the preview values as in step 1. The script on disk
 * never changes and no draft is written.
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerPreviewStateDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val onCanvas = hasAnyAncestor(hasTestTag("designer-canvas-content"))
    private val script = "var on = compose.state(false);\nvar name = compose.state('Ada');\n\n" +
        "compose.mount(function () {\n" +
        "    return compose.Column({padding: 16},\n" +
        "        compose.Switch({key: 'toggle', checked: on.value, onCheckedChange: function (checked) {\n" +
        "            on.value = checked;\n" +
        "        }}),\n" +
        "        compose.Text({key: 'label', text: name.value}),\n" +
        "        compose.TextField({key: 'field', text: name.value, onValueChange: function (text) {\n" +
        "            name.value = text;\n" +
        "        }}));\n" +
        "});\n"

    @Before @After fun clean() { store.list().filter { it.name.startsWith("state-test") }.forEach { store.delete(it.name) } }

    private fun canvasText(text: String) = rule.onAllNodes(hasText(text) and onCanvas and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()

    private fun canvasSwitchIs(state: ToggleableState) =
        rule.onAllNodes(isToggleable() and onCanvas and SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theStatePanelAndTheInteractiveCanvasChangeOnlyThePreview() {
        val name = store.create("state-test", script).name
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use {
            rule.waitUntil(5_000) { canvasText("Ada") }
            val canvasSwitch = rule.onNode(isToggleable() and onCanvas)
            canvasSwitch.assertIsOff()
            rule.onNodeWithTag("designer-tab-state").performClick()
            rule.onNodeWithTag("designer-state-on").performScrollTo().assertIsDisplayed()

            // The panel drives the canvas, including a text field whose renderer keeps its own text. On a small screen
            // (the API 24 CI device is a Nexus 5) the hint pushes the first row below the visible part of the panel.
            rule.onNodeWithTag("designer-state-value-on").performScrollTo().performClick()
            rule.waitUntil(5_000) { canvasSwitchIs(ToggleableState.On) }
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

            if (SandboxPreview.SUPPORTED) {
                // Interactive preview: the script runs from the preview values, and its handlers change the canvas only.
                rule.onNodeWithTag("designer-state-value-on").performScrollTo().performClick()
                rule.waitUntil(5_000) { canvasSwitchIs(ToggleableState.On) }
                rule.onNodeWithTag("designer-interact").performClick()
                rule.waitUntil(10_000) { rule.onAllNodesWithTag("designer-sandbox-live", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
                canvasSwitch.assertIsOn()
                canvasSwitch.performClick()
                rule.waitUntil(5_000) { canvasSwitchIs(ToggleableState.Off) }
                rule.onNodeWithTag("designer-state-value-on").assertIsOn()
                rule.onNode(hasSetTextAction() and onCanvas).performTextInput(" Lovelace")
                rule.waitUntil(5_000) { canvasText("Ada Lovelace") }
                rule.onNodeWithTag("designer-state-value-name").assert(hasText("Ada"))

                // Leaving the preview shows the document with the preview values again.
                rule.onNodeWithTag("designer-interact").performClick()
                rule.waitUntil(5_000) { canvasText("Ada") && canvasSwitchIs(ToggleableState.On) }
                assertEquals(0, rule.onAllNodesWithTag("designer-sandbox-live", useUnmergedTree = true).fetchSemanticsNodes().size)
            } else {
                // Below Android 8.0 the script does not run (step 1): state-bound controls set the preview values.
                rule.onNodeWithTag("designer-interact").performClick()
                rule.waitForIdle()
                canvasSwitch.performClick()
                rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("designer-state-value-on") and SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On)).fetchSemanticsNodes().isNotEmpty() }
                canvasSwitch.assertIsOn()
                rule.onNode(hasSetTextAction() and onCanvas).performTextInput(" Lovelace")
                rule.waitUntil(5_000) { canvasText("Ada Lovelace") }
                rule.onNodeWithTag("designer-state-value-name").assert(hasText("Ada Lovelace"))
                rule.onNodeWithTag("designer-interact").performClick()
                rule.waitForIdle()
            }
        }
        assertEquals("preview values and the running script never reach the file", script, store.read(name))
        assertFalse("nothing to recover: the document was never edited", store.draftFile(name).exists())
    }
}
