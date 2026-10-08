package io.github.supermonster003.autojs6.plugin.compose.ui.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Each icon mode is a stable alias with its own resource; the automatic alias keeps an independent resource ID
 * (icon specification section 3) so launchers that honor night configuration can switch it.
 */
@RunWith(AndroidJUnit4::class)
class LauncherIconDeviceTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After fun restore() { LauncherIcons.select(context, LauncherIconMode.AUTO) }

    @Test
    fun everyModeBecomesTheOnlyLauncherEntryWithItsOwnIconResource() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val seen = HashMap<LauncherIconMode, Int>()
        LauncherIconMode.entries.forEach { mode ->
            LauncherIcons.select(context, mode)
            assertEquals(mode, LauncherIcons.current(context))
            @Suppress("DEPRECATION")
            val matches = context.packageManager.queryIntentActivities(launcher, 0)
            assertEquals("$mode leaves exactly one launcher entry: ${matches.map { it.activityInfo.name }}", 1, matches.size)
            assertEquals(mode.component(context).className, matches.single().activityInfo.name)
            seen[mode] = matches.single().activityInfo.icon
        }
        assertEquals("four distinct icon resources", 4, seen.values.toSet().size)
        assertEquals("ic_launcher_system_auto", context.resources.getResourceEntryName(seen.getValue(LauncherIconMode.AUTO)))
        assertEquals("ic_launcher", context.resources.getResourceEntryName(seen.getValue(LauncherIconMode.TRANSPARENT)))
        assertEquals("ic_launcher_system_light", context.resources.getResourceEntryName(seen.getValue(LauncherIconMode.LIGHT)))
        assertEquals("ic_launcher_system", context.resources.getResourceEntryName(seen.getValue(LauncherIconMode.DARK)))
    }

    @Test
    fun normalizeKeepsAnExplicitChoiceAndRepairsMixedStates() {
        LauncherIcons.select(context, LauncherIconMode.DARK)
        LauncherIcons.normalize(context)
        assertEquals(LauncherIconMode.DARK, LauncherIcons.current(context))
        // A second explicit alias (as an interrupted switch could leave) normalizes to one entry again.
        context.packageManager.setComponentEnabledSetting(LauncherIconMode.LIGHT.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        LauncherIcons.normalize(context)
        val enabled = LauncherIconMode.entries.filter { context.packageManager.getComponentEnabledSetting(it.component(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
        assertEquals(1, enabled.size)
        assertTrue(enabled.single() in setOf(LauncherIconMode.LIGHT, LauncherIconMode.DARK))
    }
}
