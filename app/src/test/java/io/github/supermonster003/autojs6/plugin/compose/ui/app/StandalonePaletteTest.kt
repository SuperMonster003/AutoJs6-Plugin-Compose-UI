package io.github.supermonster003.autojs6.plugin.compose.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The accent roles must equal the ones the other standalone plugins derive with Material Components 1.13.0's
 * bundled Material Color Utilities. The fixture rows were produced by that library (seed, light primary,
 * light onPrimary, dark primary, dark onPrimary).
 */
class StandalonePaletteTest {
    private val reference = """
        ffffdead ff7e5700 ffffffff fffbbb49 ff432c00
        fff44336 ffbb1614 ffffffff ffffb4a9 ff690002
        ffe91e63 ffbc004b ffffffff ffffb2be ff660025
        ff9c27b0 ff9a25ae ffffffff fff9abff ff570066
        ff673ab7 ff6f43c0 ffffffff ffd3bbff ff3f008d
        ff3f51b5 ff4355b9 ffffffff ffbac3ff ff08218a
        ff2196f3 ff0061a4 ffffffff ff9ecaff ff003258
        ff03a9f4 ff006493 ffffffff ff8dcdff ff00344f
        ff00bcd4 ff006876 ffffffff ff44d8f1 ff00363e
        ff009688 ff006a60 ffffffff ff53dbc9 ff003731
        ff4caf50 ff006e1c ffffffff ff78dc77 ff00390a
        ff8bc34a ff3e6a00 ffffffff ff9ed75b ff1e3700
        ffff9800 ff8b5000 ffffffff ffffb870 ff4a2800
        ffff5722 ffb02f00 ffffffff ffffb5a0 ff5f1500
        ff795548 ff9a4522 ffffffff ffffb59a ff5b1b00
        ff607d8b ff006783 ffffffff ff65d3ff ff003546
        ff123456 ff0d61a4 ffffffff ffa0c9ff ff00325a
        ff808080 ff5e5e5e ffffffff ffc6c6c6 ff303030
        ffffffff ff5e5e5e ffffffff ffc6c6c6 ff303030
        ff000000 ff5e5e5e ffffffff ffc6c6c6 ff303030
        ff00ff00 ff026e00 ffffffff ff31e524 ff013a00
    """.trimIndent().lines().map { line -> line.trim().split(" ").map { it.toLong(16).toInt() } }

    @Test
    fun `tone 40 and 80 roles match Material Color Utilities for presets, greys and extremes`() {
        assertEquals(ThemeColorValue.presets, reference.take(16).map { it[0] })
        reference.forEach { (seed, light, onLight, dark, onDark) ->
            assertEquals("light primary of ${ThemeColorValue.hex(seed)}", ThemeColorValue.hex(light), ThemeColorValue.hex(ThemeAccentRoles.fromSeed(seed, false).primary))
            assertEquals("light onPrimary of ${ThemeColorValue.hex(seed)}", ThemeColorValue.hex(onLight), ThemeColorValue.hex(ThemeAccentRoles.fromSeed(seed, false).onPrimary))
            assertEquals("dark primary of ${ThemeColorValue.hex(seed)}", ThemeColorValue.hex(dark), ThemeColorValue.hex(ThemeAccentRoles.fromSeed(seed, true).primary))
            assertEquals("dark onPrimary of ${ThemeColorValue.hex(seed)}", ThemeColorValue.hex(onDark), ThemeColorValue.hex(ThemeAccentRoles.fromSeed(seed, true).onPrimary))
        }
    }

    @Test
    fun `the palette keeps neutral surfaces and a readable accent in both modes`() {
        listOf(false, true).forEach { dark ->
            val palette = StandalonePalette.resolve(HostAppearance("en", dark, 0xffffdead.toInt(), 0xfffff176.toInt()))
            assertEquals(if (dark) 0xff121212.toInt() else 0xfff3f4f5.toInt(), palette.background)
            assertEquals(if (dark) 0xff1e1e1e.toInt() else 0xffffffff.toInt(), palette.surface)
            assertTrue("accent readable on background", StandaloneColorPolicy.contrastRatio(palette.accent, palette.background) >= 4.5)
            assertTrue("accent readable on surface", StandaloneColorPolicy.contrastRatio(palette.accent, palette.surface) >= 4.5)
            assertTrue("text readable", StandaloneColorPolicy.contrastRatio(palette.text, palette.background) >= 4.5)
            assertEquals(dark, palette.isDark)
            val scheme = palette.colorScheme()
            assertEquals(palette.accent, argb(scheme.primary))
            assertEquals(palette.background, argb(scheme.background))
            assertEquals(palette.surface, argb(scheme.surface))
        }
    }

    @Test
    fun `color input accepts six digit hex and rgb triples only`() {
        assertEquals(0xff12ab34.toInt(), ThemeColorValue.parse(" #12AB34 "))
        assertEquals(0xff12ab34.toInt(), ThemeColorValue.parse("12ab34"))
        assertEquals(0xff0a1464.toInt(), ThemeColorValue.parse("RGB( 10 , 20,100 )"))
        listOf("#12AB34FF", "#12A", "rgb(256, 0, 0)", "rgb(-1, 0, 0)", "rgb(1, 2)", "#12AB34 extra", "", "red").forEach {
            assertNull("'$it' must be rejected", ThemeColorValue.parse(it))
        }
        assertEquals("#12AB34", ThemeColorValue.hex(0xff12ab34.toInt()))
        assertEquals("rgb(18, 171, 52)", ThemeColorValue.rgb(0xff12ab34.toInt()))
    }

    private fun argb(color: androidx.compose.ui.graphics.Color): Int {
        val a = (color.alpha * 255 + 0.5f).toInt(); val r = (color.red * 255 + 0.5f).toInt(); val g = (color.green * 255 + 0.5f).toInt(); val b = (color.blue * 255 + 0.5f).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
