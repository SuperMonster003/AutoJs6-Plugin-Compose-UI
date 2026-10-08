package io.github.supermonster003.autojs6.plugin.compose.ui.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.compose.ui.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pickers edit a draft; only confirm persists, and cancel or an outside click leaves the saved value untouched. */
@RunWith(AndroidJUnit4::class)
internal class SettingsDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<SettingsActivity>()
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun reset() { clear() }
    @After fun restore() { clear(); LauncherIcons.select(context, LauncherIconMode.AUTO) }

    private fun clear() {
        context.getSharedPreferences("app-appearance", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun darkModeChoiceSavesOnlyAfterConfirmation() {
        rule.onNodeWithTag("settings-page").assertIsDisplayed()
        rule.onNodeWithTag("settings-night").performClick()
        rule.onNodeWithTag("dialog-night").assertIsDisplayed()
        rule.onNodeWithTag("dialog-night-option-3").performClick()
        rule.onNodeWithTag("dialog-night-cancel").performClick()
        rule.waitForIdle()
        assertEquals("cancel keeps the saved value", AppearancePreferences.FOLLOW_HOST, AppearancePreferences.read(context).darkMode)

        rule.onNodeWithTag("settings-night").performClick()
        rule.onNodeWithTag("dialog-night-option-3").performClick()
        rule.onNodeWithTag("dialog-night-confirm").performClick()
        rule.waitForIdle()
        assertEquals(AppearancePreferences.DARK, AppearancePreferences.read(context).darkMode)
        rule.onNodeWithTag("settings-night-summary", useUnmergedTree = true).assertTextEquals(rule.activity.getString(R.string.app_settings_always_dark))
        assertTrue("the page redraws in dark mode", rule.activity.appearance.dark)
    }

    @Test
    fun themeColorAcceptsPresetsAndValidInputOnly() {
        rule.onNodeWithTag("settings-color").performClick()
        rule.onNodeWithTag("dialog-color").assertIsDisplayed()
        rule.onNodeWithTag("dialog-color-input").performTextInput("not a color")
        rule.onNodeWithTag("dialog-color-confirm").assertIsNotEnabled()
        rule.onNodeWithTag("dialog-color-input").performTextClearance()
        rule.onNodeWithTag("dialog-color-input").performTextInput("#123456")
        rule.onNodeWithTag("dialog-color-confirm").assertIsEnabled()
        rule.onNodeWithTag("dialog-color-preview", useUnmergedTree = true).assertTextEquals("#123456  rgb(18, 52, 86)")
        rule.onNodeWithTag("dialog-color-cancel").performClick()
        rule.waitForIdle()
        assertNull("cancel discards the draft", AppearancePreferences.read(context).color)

        rule.onNodeWithTag("settings-color").performClick()
        rule.onNodeWithTag("dialog-color-preset-#2196F3").performClick()
        rule.onNodeWithTag("dialog-color-confirm").performClick()
        rule.waitForIdle()
        assertEquals(0xff2196f3.toInt(), AppearancePreferences.read(context).color)
        rule.onNodeWithTag("settings-color-summary", useUnmergedTree = true).assertTextEquals("#2196F3")
        assertEquals(0xff2196f3.toInt(), rule.activity.appearance.accent)

        rule.onNodeWithTag("settings-color").performClick()
        rule.onNodeWithTag("dialog-color-follow").performClick()
        rule.onNodeWithTag("dialog-color-confirm").performClick()
        rule.waitForIdle()
        assertNull("following the host clears the local color", AppearancePreferences.read(context).color)
    }

    @Test
    fun launcherIconChoicePersistsAsComponentStateAndRestores() {
        rule.onNodeWithTag("settings-icon").performClick()
        rule.onNodeWithTag("dialog-icon").assertIsDisplayed()
        rule.onNodeWithTag("dialog-icon-option-3").performClick()
        rule.onNodeWithTag("dialog-icon-confirm").performClick()
        rule.waitForIdle()
        assertEquals(LauncherIconMode.TRANSPARENT, LauncherIcons.current(context))
        rule.onNodeWithTag("settings-icon-summary", useUnmergedTree = true).assertTextEquals(rule.activity.getString(R.string.launcher_icon_transparent))
        LauncherIcons.select(context, LauncherIconMode.AUTO)
        assertEquals(LauncherIconMode.AUTO, LauncherIcons.current(context))
    }

    @Test
    fun hostAppearanceIsOptionalAndResolvedValuesAreUsable() {
        val snapshot = HostAppearanceReader.read(context)
        val resolved = AppearancePreferences.resolve(context, snapshot)
        assertTrue(resolved.language.isNotEmpty())
        assertEquals(0xff000000.toInt(), resolved.primary and 0xff000000.toInt())
        assertEquals(0xff000000.toInt(), resolved.accent and 0xff000000.toInt())
        if (snapshot != null) {
            assertEquals("following the host copies its language", snapshot.language, resolved.language)
            assertEquals(snapshot.dark, resolved.dark)
        }
    }
}
