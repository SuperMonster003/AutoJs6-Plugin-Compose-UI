package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.autojs.plugin.compose.api.ComposeUiThemeColors
import org.autojs.plugin.compose.api.model.ThemeSpec
import org.junit.Assert.*
import org.junit.Test

class ThemeMapperTest {
    @Test fun unspecifiedSettingsFollowSystemWhileExplicitNightWins() {
        assertEquals(roles(lightColorScheme()), roles(ThemeMapper.colorScheme(ThemeSpec(), systemDark = false)))
        assertEquals(roles(darkColorScheme()), roles(ThemeMapper.colorScheme(ThemeSpec(), systemDark = true)))
        assertEquals(roles(lightColorScheme()), roles(ThemeMapper.colorScheme(ThemeSpec(dark = false), systemDark = true)))
        assertEquals(roles(darkColorScheme()), roles(ThemeMapper.colorScheme(ThemeSpec(dark = true), systemDark = false)))
    }

    @Test fun everyFrozenColorRoleCanBeIndependentlyOverriddenIncludingAlpha() {
        val overrides = ComposeUiThemeColors.names.sorted().mapIndexed { index, role -> role to (0x12340000 + index) }.toMap()
        assertEquals(48, overrides.size)
        for (dark in listOf(false, true)) {
            val mapped = roles(ThemeMapper.colorScheme(ThemeSpec(seedArgb = 0xff0088aa.toInt(), colorOverrides = overrides, dark = dark)))
            assertEquals(overrides.keys, mapped.keys)
            overrides.forEach { (role, color) -> assertEquals(role, color, mapped.getValue(role).toArgb()) }
        }
        val without = ThemeMapper.colorScheme(ThemeSpec(seedArgb = 0xff0088aa.toInt()))
        val one = ThemeMapper.colorScheme(ThemeSpec(seedArgb = 0xff0088aa.toInt(), colorOverrides = mapOf("primary" to 0)))
        assertEquals(Color.Transparent, one.primary)
        assertEquals(without.onPrimary, one.onPrimary)
        assertEquals(without.surfaceTint, one.surfaceTint)
    }

    @Test fun dynamicSchemeIsOptInOverridesWinAndUnavailableDynamicFallsBackToSeed() {
        val seed = 0xff336699.toInt()
        val system = darkColorScheme(primary = Color.Green, surface = Color.Red)
        val seeded = ThemeMapper.colorScheme(ThemeSpec(seedArgb = seed))
        assertEquals(roles(seeded), roles(ThemeMapper.colorScheme(ThemeSpec(seedArgb = seed), dynamicScheme = system)))
        assertEquals(roles(seeded), roles(ThemeMapper.colorScheme(ThemeSpec(seedArgb = seed, dynamicColor = true))))
        val dynamic = ThemeMapper.colorScheme(ThemeSpec(seedArgb = seed, dynamicColor = true,
            colorOverrides = mapOf("primary" to 0x44112233)), dynamicScheme = system)
        assertEquals(0x44112233, dynamic.primary.toArgb())
        assertEquals(Color.Red, dynamic.surface)
        assertEquals(system.onPrimary, dynamic.onPrimary)
    }

    @Test fun seededPalettesChangeWithHueRetainReadableRolePairsAndKeepFixedRolesStable() {
        val seeds = listOf(0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt(), 0xffffff00.toInt(),
            0xff00ffff.toInt(), 0xffff00ff.toInt(), 0xff808080.toInt(), 0xff000000.toInt(), 0xffffffff.toInt())
        seeds.forEach { seed ->
            val light = ThemeMapper.colorScheme(ThemeSpec(seedArgb = seed, dark = false))
            val dark = ThemeMapper.colorScheme(ThemeSpec(seedArgb = seed, dark = true))
            for (scheme in listOf(light, dark)) {
                val pairs = listOf(
                    scheme.primary to scheme.onPrimary, scheme.primaryContainer to scheme.onPrimaryContainer,
                    scheme.secondary to scheme.onSecondary, scheme.secondaryContainer to scheme.onSecondaryContainer,
                    scheme.tertiary to scheme.onTertiary, scheme.tertiaryContainer to scheme.onTertiaryContainer,
                    scheme.surface to scheme.onSurface, scheme.surfaceVariant to scheme.onSurfaceVariant,
                    scheme.inverseSurface to scheme.inverseOnSurface,
                )
                pairs.forEach { (background, foreground) ->
                    assertTrue("Seed ${seed.toUInt().toString(16)} has unreadable role pair", contrast(background, foreground) >= 4.5)
                }
                assertTrue(roles(scheme).values.all { it.alpha == 1f })
            }
            val lightRoles = roles(light)
            val darkRoles = roles(dark)
            lightRoles.keys.filter { "Fixed" in it }.forEach { role -> assertEquals(role, lightRoles[role], darkRoles[role]) }
            assertNotEquals(light.surface, dark.surface)
        }
        val red = ThemeMapper.colorScheme(ThemeSpec(seedArgb = 0xffff0000.toInt()))
        val blue = ThemeMapper.colorScheme(ThemeSpec(seedArgb = 0xff0000ff.toInt()))
        assertNotEquals(red.primary, blue.primary)
        assertNotEquals(red.primaryContainer, blue.primaryContainer)
        assertEquals(red.error, blue.error)
        assertEquals(roles(red), roles(ThemeMapper.colorScheme(ThemeSpec(seedArgb = 0x12ff0000))))
    }

    @Test fun fontOverridesPreserveTypographyMetricsAndSystemDensity() {
        val base = Typography()
        assertSame(base, ThemeMapper.typography(null, base))
        val mapped = ThemeMapper.typography("monospace", base)
        val originals = listOf(base.displayLarge, base.displayMedium, base.displaySmall, base.headlineLarge,
            base.headlineMedium, base.headlineSmall, base.titleLarge, base.titleMedium, base.titleSmall,
            base.bodyLarge, base.bodyMedium, base.bodySmall, base.labelLarge, base.labelMedium, base.labelSmall)
        val changed = listOf(mapped.displayLarge, mapped.displayMedium, mapped.displaySmall, mapped.headlineLarge,
            mapped.headlineMedium, mapped.headlineSmall, mapped.titleLarge, mapped.titleMedium, mapped.titleSmall,
            mapped.bodyLarge, mapped.bodyMedium, mapped.bodySmall, mapped.labelLarge, mapped.labelMedium, mapped.labelSmall)
        originals.zip(changed).forEach { (original, replacement) ->
            assertEquals(original.copy(fontFamily = FontFamily.Monospace), replacement)
        }
        val systemDensity = Density(2.75f, 1.3f)
        assertSame(systemDensity, ThemeMapper.density(ThemeSpec(), systemDensity))
        val scaled = ThemeMapper.density(ThemeSpec(fontScale = 1.7), systemDensity)
        assertEquals(2.75f, scaled.density, 0f)
        assertEquals(1.7f, scaled.fontScale, 0f)
        assertEquals(1.3f, systemDensity.fontScale, 0f)
        assertTrue(ThemeMapper.density(ThemeSpec(fontScale = Double.MIN_VALUE), systemDensity).fontScale > 0f)
    }

    private fun contrast(first: Color, second: Color): Double {
        fun luminance(color: Color): Double {
            fun linear(channel: Float) = if (channel <= 0.04045f) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
            return linear(color.red) * 0.2126 + linear(color.green) * 0.7152 + linear(color.blue) * 0.0722
        }
        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    /** Independently enumerate the public ColorScheme fields, guarding the whole frozen crosswalk. */
    private fun roles(colors: ColorScheme) = mapOf(
        "primary" to colors.primary, "onPrimary" to colors.onPrimary,
        "primaryContainer" to colors.primaryContainer, "onPrimaryContainer" to colors.onPrimaryContainer,
        "inversePrimary" to colors.inversePrimary, "secondary" to colors.secondary,
        "onSecondary" to colors.onSecondary, "secondaryContainer" to colors.secondaryContainer,
        "onSecondaryContainer" to colors.onSecondaryContainer, "tertiary" to colors.tertiary,
        "onTertiary" to colors.onTertiary, "tertiaryContainer" to colors.tertiaryContainer,
        "onTertiaryContainer" to colors.onTertiaryContainer, "background" to colors.background,
        "onBackground" to colors.onBackground, "surface" to colors.surface,
        "onSurface" to colors.onSurface, "surfaceVariant" to colors.surfaceVariant,
        "onSurfaceVariant" to colors.onSurfaceVariant, "surfaceTint" to colors.surfaceTint,
        "inverseSurface" to colors.inverseSurface, "inverseOnSurface" to colors.inverseOnSurface,
        "error" to colors.error, "onError" to colors.onError,
        "errorContainer" to colors.errorContainer, "onErrorContainer" to colors.onErrorContainer,
        "outline" to colors.outline, "outlineVariant" to colors.outlineVariant, "scrim" to colors.scrim,
        "surfaceBright" to colors.surfaceBright, "surfaceDim" to colors.surfaceDim,
        "surfaceContainer" to colors.surfaceContainer, "surfaceContainerHigh" to colors.surfaceContainerHigh,
        "surfaceContainerHighest" to colors.surfaceContainerHighest, "surfaceContainerLow" to colors.surfaceContainerLow,
        "surfaceContainerLowest" to colors.surfaceContainerLowest,
        "primaryFixed" to colors.primaryFixed, "primaryFixedDim" to colors.primaryFixedDim,
        "onPrimaryFixed" to colors.onPrimaryFixed, "onPrimaryFixedVariant" to colors.onPrimaryFixedVariant,
        "secondaryFixed" to colors.secondaryFixed, "secondaryFixedDim" to colors.secondaryFixedDim,
        "onSecondaryFixed" to colors.onSecondaryFixed, "onSecondaryFixedVariant" to colors.onSecondaryFixedVariant,
        "tertiaryFixed" to colors.tertiaryFixed, "tertiaryFixedDim" to colors.tertiaryFixedDim,
        "onTertiaryFixed" to colors.onTertiaryFixed, "onTertiaryFixedVariant" to colors.onTertiaryFixedVariant,
    )
}
