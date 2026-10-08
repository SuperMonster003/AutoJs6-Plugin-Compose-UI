package io.github.supermonster003.autojs6.plugin.compose.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure resolution rules of the standalone appearance (specification section 7). */
class AppearancePreferencesTest {
    private val host = HostAppearance("ja", dark = true, primary = 0xff2196f3.toInt(), accent = 0xff009688.toInt())

    @Test
    fun `defaults follow the host and fall back to the system without a host`() {
        val defaults = AppearancePreferences()
        assertEquals(HostAppearance("ja", true, 0xff2196f3.toInt(), 0xff009688.toInt()), defaults.resolve(host, "en-US", false))
        val fallback = defaults.resolve(null, "en-US", false)
        assertEquals("en-US", fallback.language)
        assertFalse(fallback.dark)
        assertEquals(AppearancePreferences.DEFAULT_COLOR, fallback.primary)
        assertEquals(AppearancePreferences.DEFAULT_COLOR, fallback.accent)
        assertEquals(0xffffdead.toInt(), AppearancePreferences.DEFAULT_COLOR)
    }

    @Test
    fun `explicit choices override the host and a local color replaces both roles`() {
        val chosen = AppearancePreferences(language = "zh-Hant-TW", darkMode = AppearancePreferences.LIGHT, color = 0x00ff5722)
        val resolved = chosen.resolve(host, "en-US", true)
        assertEquals("zh-Hant-TW", resolved.language)
        assertFalse(resolved.dark)
        assertEquals("a saved color is always opaque", 0xffff5722.toInt(), resolved.primary)
        assertEquals(0xffff5722.toInt(), resolved.accent)
        val system = AppearancePreferences(language = AppearancePreferences.FOLLOW_SYSTEM, darkMode = AppearancePreferences.FOLLOW_SYSTEM)
        assertEquals("en-US", system.resolve(host, "en-US", true).language)
        assertTrue(system.resolve(host, "en-US", true).dark)
        assertTrue(AppearancePreferences(darkMode = AppearancePreferences.DARK).resolve(null, "en", false).dark)
    }

    @Test
    fun `unknown stored values fall back to following the host`() {
        val stale = AppearancePreferences(language = "tlh", darkMode = "sepia")
        val resolved = stale.resolve(host, "en-US", false)
        assertEquals("ja", resolved.language)
        assertTrue(resolved.dark)
        assertNull(stale.color)
        assertNotEquals(AppearancePreferences.LANGUAGES, AppearancePreferences.DARK_MODES)
        assertEquals(12, AppearancePreferences.LANGUAGES.size)
        assertEquals(listOf("host", "system", "light", "dark"), AppearancePreferences.DARK_MODES)
    }
}
