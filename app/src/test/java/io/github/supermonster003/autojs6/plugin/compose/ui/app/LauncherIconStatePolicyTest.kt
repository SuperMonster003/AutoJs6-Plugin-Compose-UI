package io.github.supermonster003.autojs6.plugin.compose.ui.app

import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Component enabled state is the only persisted launcher choice; the automatic alias is the manifest default. */
class LauncherIconStatePolicyTest {
    private fun states(vararg explicit: LauncherIconMode) = LauncherIconMode.entries.associateWith {
        if (it in explicit) COMPONENT_ENABLED_STATE_ENABLED else if (it == LauncherIconMode.AUTO) COMPONENT_ENABLED_STATE_DEFAULT else COMPONENT_ENABLED_STATE_DISABLED
    }

    @Test
    fun `a fresh install resolves to the automatic alias`() {
        assertEquals(LauncherIconMode.AUTO, LauncherIconStatePolicy.resolve(states()))
        assertTrue(LauncherIconStatePolicy.enabled(LauncherIconMode.AUTO, COMPONENT_ENABLED_STATE_DEFAULT))
        assertFalse(LauncherIconStatePolicy.enabled(LauncherIconMode.LIGHT, COMPONENT_ENABLED_STATE_DEFAULT))
        assertTrue(LauncherIconStatePolicy.enabled(LauncherIconMode.LIGHT, COMPONENT_ENABLED_STATE_ENABLED))
    }

    @Test
    fun `an explicit choice wins and mixed states keep the automatic choice`() {
        assertEquals(LauncherIconMode.TRANSPARENT, LauncherIconStatePolicy.resolve(states(LauncherIconMode.TRANSPARENT)))
        assertEquals(LauncherIconMode.DARK, LauncherIconStatePolicy.resolve(states(LauncherIconMode.DARK)))
        assertEquals("explicit automatic beats another explicit entry", LauncherIconMode.AUTO, LauncherIconStatePolicy.resolve(states(LauncherIconMode.LIGHT, LauncherIconMode.AUTO)))
        assertEquals("two explicit non-automatic entries keep the first declared", LauncherIconMode.LIGHT, LauncherIconStatePolicy.resolve(states(LauncherIconMode.LIGHT, LauncherIconMode.DARK)))
    }

    @Test
    fun `alias names match the manifest components`() {
        assertEquals(listOf("AdaptiveLightIconAlias", "AdaptiveDarkIconAlias", "AdaptiveAutoIconAlias", "TransparentIconAlias"), LauncherIconMode.entries.map { it.alias })
    }
}
