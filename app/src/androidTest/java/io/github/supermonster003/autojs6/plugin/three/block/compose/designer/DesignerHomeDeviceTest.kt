package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerHomeActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Roadmap D38: the launcher target lists projects, creates them with the starter script and deletes them. */
@RunWith(AndroidJUnit4::class)
internal class DesignerHomeDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<DesignerHomeActivity>()

    private val store = ProjectStore(InstrumentationRegistry.getInstrumentation().targetContext)

    @Before fun clean() { store.list().filter { it.name.startsWith("home-test-") }.forEach { store.delete(it.name) } }
    @After fun remove() { store.list().filter { it.name.startsWith("home-test-") }.forEach { store.delete(it.name) } }

    @Test
    fun homeListsCreatesAndDeletesProjects() {
        rule.onNodeWithTag("home-gallery").assertIsDisplayed()
        rule.onNodeWithTag("home-settings").assertIsDisplayed()
        rule.onNodeWithTag("home-new").performClick()
        rule.onNodeWithTag("home-project-name").performTextInput("home-test-one")
        rule.onNodeWithTag("home-create").performClick()
        rule.waitForIdle()
        val project = store.list().first { it.name == "home-test-one.js" }
        assertEquals(store.template(), store.read(project.name))
        assertTrue(store.file(project.name).isFile)
        // The editor opened on top; return to the home, which refreshes its list on resume. The first
        // back press may only close the keyboard that the name field opened.
        val scenario = rule.activityRule.scenario
        var presses = 0
        while (scenario.state != Lifecycle.State.RESUMED && presses < 4) {
            presses++
            InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            try { rule.waitUntil(3_000) { scenario.state == Lifecycle.State.RESUMED } } catch (timeout: ComposeTimeoutException) { }
        }
        assertEquals("home resumed after $presses back presses", Lifecycle.State.RESUMED, scenario.state)
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("home-project-home-test-one.js").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("home-project-home-test-one.js").assertIsDisplayed()
        rule.onNodeWithTag("home-delete-home-test-one.js").performClick()
        rule.onNodeWithTag("home-delete-confirm").performClick()
        rule.waitForIdle()
        assertFalse(store.file("home-test-one.js").exists())
    }
}
