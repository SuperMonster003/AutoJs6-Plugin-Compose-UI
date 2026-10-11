package io.github.supermonster003.autojs6.plugin.compose.ui.app

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.compose.ui.ComposeUiPlugin
import io.github.supermonster003.autojs6.plugin.compose.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The gallery runs in the plugin's own process: every catalog entry composes, and scripts leave through the clipboard or
 * the host. No test leaves another app's window in front: the host is never started, and the settings screen is closed.
 */
@RunWith(AndroidJUnit4::class)
internal class GalleryDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<GalleryActivity>()
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everyCatalogEntryOpensWithALivePreviewAndItsScript() {
        rule.onNodeWithTag("gallery-list").assertIsDisplayed()
        GalleryCatalog.entries.forEach { entry ->
            rule.onNodeWithTag("gallery-list").performScrollToNode(hasTestTag("gallery-${entry.component}"))
            rule.onNodeWithTag("gallery-${entry.component}").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("preview-${entry.component}").assertExists()
            rule.onNodeWithTag("gallery-code", useUnmergedTree = true).assertTextContains(
                if (entry.component == "Snackbar") "session.showSnackbar(" else "compose.${entry.component}(", substring = true)
            rule.onNodeWithTag("gallery-back").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("gallery-list").assertIsDisplayed()
        }
    }

    @Test
    fun copyPutsTheScriptOnTheClipboard() {
        openEntry("Button")
        // API 29+ lets only the focused app read the clipboard, and a CI emulator grants focus later than a local
        // one. Earlier releases need no focus here, so the test does not depend on it there.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) rule.waitUntil(timeoutMillis = 10_000) { rule.activity.hasWindowFocus() }
        rule.onNodeWithTag("gallery-copy").performScrollTo().performClick()
        val expected = GalleryCatalog.entry("Button")!!.script()
        rule.waitUntil(timeoutMillis = 10_000) { clipboardText() == expected }
        assertEquals(expected, clipboardText())
        assertTrue(expected.startsWith("\"ui\";"))
    }

    @Test
    fun runTargetsTheInstalledHostAndOtherwiseOnlyReportsItMissing() {
        openEntry("Button")
        val script = GalleryCatalog.entry("Button")!!.script()
        val intent = ScriptLauncher.intent(script)
        assertEquals(ComposeUiPlugin.HOST_PACKAGE_NAME, intent.component?.packageName)
        assertEquals(ScriptLauncher.HOST_RUN_ACTIVITY, intent.component?.className)
        assertEquals(script, intent.getStringExtra(ScriptLauncher.EXTRA_SCRIPT))
        rule.onNodeWithTag("gallery-run").performScrollTo().assertIsDisplayed()
        if (ScriptLauncher.isHostInstalled(context)) {
            // The host's exported run entry is visible through the manifest <queries> declaration. It is not
            // started here: a host window in front would take focus from every later test.
            assertNotNull("the host run Activity resolves", context.packageManager.resolveActivity(intent, 0))
        } else {
            // Without the host (as on the CI emulators) running only explains why; nothing else is started.
            assertEquals(ScriptLauncher.Result.HOST_MISSING, ScriptLauncher.run(context, script))
            rule.onNodeWithTag("gallery-run").performClick()
            val missing = rule.activity.getString(R.string.gallery_run_missing_host)
            rule.waitUntil(timeoutMillis = 10_000) { rule.onAllNodesWithText(missing).fetchSemanticsNodes().isNotEmpty() }
            rule.waitForIdle()
            assertEquals(listOf<Activity>(rule.activity), resumedActivities())
        }
    }

    @Test
    fun theSettingsActionOpensTheSettingsScreen() {
        rule.onNodeWithTag("gallery-settings").performClick()
        rule.waitUntil(timeoutMillis = 10_000) { resumedSettings().isNotEmpty() }
        // Leave no second Activity behind for the following tests.
        val open = resumedSettings()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { open.forEach { it.finish() } }
        rule.waitUntil(timeoutMillis = 10_000) { resumedSettings().isEmpty() }
    }

    private fun openEntry(component: String) {
        rule.onNodeWithTag("gallery-list").performScrollToNode(hasTestTag("gallery-$component"))
        rule.onNodeWithTag("gallery-$component").performClick()
        rule.waitForIdle()
    }

    private fun resumedActivities(): List<Activity> {
        var resumed: List<Activity> = emptyList()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toList()
        }
        return resumed
    }

    private fun resumedSettings(): List<SettingsActivity> {
        var resumed: List<SettingsActivity> = emptyList()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<SettingsActivity>()
        }
        return resumed
    }

    private fun clipboardText(): String? {
        var clip: CharSequence? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(0)?.text
        }
        return clip?.toString()
    }
}
