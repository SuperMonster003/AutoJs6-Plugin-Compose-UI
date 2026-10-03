package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import org.autojs.plugin.compose.api.ComposeUiThemeColors as T
import org.autojs.plugin.compose.api.model.ThemeSpec

/** Material roles are derived from a seed or the public Android dynamic-color API, then overridden. */
internal object ThemeMapper {
    @Composable
    fun Content(theme: ThemeSpec, content: @Composable () -> Unit) {
        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        val systemDark = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val dark = theme.dark ?: systemDark
        // Configuration participates in this key: night mode and platform resource changes refresh dynamic colors.
        val colors = remember(theme, context, configuration) {
            val dynamic = if (theme.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else null
            colorScheme(theme, systemDark, dynamic)
        }
        val typography = remember(theme.fontFamily) { typography(theme.fontFamily) }
        val systemDensity = LocalDensity.current
        val density = remember(systemDensity, theme.fontScale) { density(theme, systemDensity) }
        // Keep the host context/resources/owners. Only text scaling changes within this composition.
        CompositionLocalProvider(LocalDensity provides density) {
            MaterialTheme(colorScheme = colors, typography = typography, content = content)
        }
    }

    /** A missing dynamic scheme (including API < 31) falls back to the supplied seed. Explicit roles win last. */
    fun colorScheme(theme: ThemeSpec, systemDark: Boolean = false, dynamicScheme: ColorScheme? = null): ColorScheme {
        val dark = theme.dark ?: systemDark
        val base = when {
            theme.dynamicColor && dynamicScheme != null -> dynamicScheme
            theme.seedArgb != null -> seededColorScheme(theme.seedArgb!!, dark)
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
        return overrides(base, theme.colorOverrides)
    }

    fun density(theme: ThemeSpec, system: Density): Density = theme.fontScale?.let {
        Density(system.density, it.toFloat().coerceAtLeast(Float.MIN_VALUE))
    } ?: system

    fun typography(fontFamily: String?, base: Typography = Typography()): Typography {
        val family = ValueMapper.fontFamily(fontFamily) ?: return base
        return base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = family),
            displayMedium = base.displayMedium.copy(fontFamily = family),
            displaySmall = base.displaySmall.copy(fontFamily = family),
            headlineLarge = base.headlineLarge.copy(fontFamily = family),
            headlineMedium = base.headlineMedium.copy(fontFamily = family),
            headlineSmall = base.headlineSmall.copy(fontFamily = family),
            titleLarge = base.titleLarge.copy(fontFamily = family),
            titleMedium = base.titleMedium.copy(fontFamily = family),
            titleSmall = base.titleSmall.copy(fontFamily = family),
            bodyLarge = base.bodyLarge.copy(fontFamily = family),
            bodyMedium = base.bodyMedium.copy(fontFamily = family),
            bodySmall = base.bodySmall.copy(fontFamily = family),
            labelLarge = base.labelLarge.copy(fontFamily = family),
            labelMedium = base.labelMedium.copy(fontFamily = family),
            labelSmall = base.labelSmall.copy(fontFamily = family),
        )
    }

    private fun overrides(base: ColorScheme, overrides: Map<String, Int>): ColorScheme {
        if (overrides.isEmpty()) return base
        fun role(name: String, fallback: Color) = overrides[name]?.let(::Color) ?: fallback
        return base.copy(
            primary = role(T.PRIMARY, base.primary),
            onPrimary = role(T.ON_PRIMARY, base.onPrimary),
            primaryContainer = role(T.PRIMARY_CONTAINER, base.primaryContainer),
            onPrimaryContainer = role(T.ON_PRIMARY_CONTAINER, base.onPrimaryContainer),
            inversePrimary = role(T.INVERSE_PRIMARY, base.inversePrimary),
            secondary = role(T.SECONDARY, base.secondary),
            onSecondary = role(T.ON_SECONDARY, base.onSecondary),
            secondaryContainer = role(T.SECONDARY_CONTAINER, base.secondaryContainer),
            onSecondaryContainer = role(T.ON_SECONDARY_CONTAINER, base.onSecondaryContainer),
            tertiary = role(T.TERTIARY, base.tertiary),
            onTertiary = role(T.ON_TERTIARY, base.onTertiary),
            tertiaryContainer = role(T.TERTIARY_CONTAINER, base.tertiaryContainer),
            onTertiaryContainer = role(T.ON_TERTIARY_CONTAINER, base.onTertiaryContainer),
            background = role(T.BACKGROUND, base.background),
            onBackground = role(T.ON_BACKGROUND, base.onBackground),
            surface = role(T.SURFACE, base.surface),
            onSurface = role(T.ON_SURFACE, base.onSurface),
            surfaceVariant = role(T.SURFACE_VARIANT, base.surfaceVariant),
            onSurfaceVariant = role(T.ON_SURFACE_VARIANT, base.onSurfaceVariant),
            surfaceTint = role(T.SURFACE_TINT, base.surfaceTint),
            inverseSurface = role(T.INVERSE_SURFACE, base.inverseSurface),
            inverseOnSurface = role(T.INVERSE_ON_SURFACE, base.inverseOnSurface),
            error = role(T.ERROR, base.error),
            onError = role(T.ON_ERROR, base.onError),
            errorContainer = role(T.ERROR_CONTAINER, base.errorContainer),
            onErrorContainer = role(T.ON_ERROR_CONTAINER, base.onErrorContainer),
            outline = role(T.OUTLINE, base.outline),
            outlineVariant = role(T.OUTLINE_VARIANT, base.outlineVariant),
            scrim = role(T.SCRIM, base.scrim),
            surfaceBright = role(T.SURFACE_BRIGHT, base.surfaceBright),
            surfaceDim = role(T.SURFACE_DIM, base.surfaceDim),
            surfaceContainer = role(T.SURFACE_CONTAINER, base.surfaceContainer),
            surfaceContainerHigh = role(T.SURFACE_CONTAINER_HIGH, base.surfaceContainerHigh),
            surfaceContainerHighest = role(T.SURFACE_CONTAINER_HIGHEST, base.surfaceContainerHighest),
            surfaceContainerLow = role(T.SURFACE_CONTAINER_LOW, base.surfaceContainerLow),
            surfaceContainerLowest = role(T.SURFACE_CONTAINER_LOWEST, base.surfaceContainerLowest),
            primaryFixed = role(T.PRIMARY_FIXED, base.primaryFixed),
            primaryFixedDim = role(T.PRIMARY_FIXED_DIM, base.primaryFixedDim),
            onPrimaryFixed = role(T.ON_PRIMARY_FIXED, base.onPrimaryFixed),
            onPrimaryFixedVariant = role(T.ON_PRIMARY_FIXED_VARIANT, base.onPrimaryFixedVariant),
            secondaryFixed = role(T.SECONDARY_FIXED, base.secondaryFixed),
            secondaryFixedDim = role(T.SECONDARY_FIXED_DIM, base.secondaryFixedDim),
            onSecondaryFixed = role(T.ON_SECONDARY_FIXED, base.onSecondaryFixed),
            onSecondaryFixedVariant = role(T.ON_SECONDARY_FIXED_VARIANT, base.onSecondaryFixedVariant),
            tertiaryFixed = role(T.TERTIARY_FIXED, base.tertiaryFixed),
            tertiaryFixedDim = role(T.TERTIARY_FIXED_DIM, base.tertiaryFixedDim),
            onTertiaryFixed = role(T.ON_TERTIARY_FIXED, base.onTertiaryFixed),
            onTertiaryFixedVariant = role(T.ON_TERTIARY_FIXED_VARIANT, base.onTertiaryFixedVariant),
        )
    }

    /**
     * Dependency-free CIELAB/LCh palette, not the HCT algorithm used by Material You.
     * Role lightness follows light/dark Material role relationships. Out-of-gamut colors reduce
     * chroma at constant hue and lightness rather than clipping channels and losing contrast.
     * Seed alpha has no meaning for a tonal palette; explicit color overrides preserve alpha.
     */
    private fun seededColorScheme(seed: Int, dark: Boolean): ColorScheme {
        val palette = SeedPalette(seed)
        fun p(tone: Int) = palette.color(tone, 1.0, 64.0)
        fun s(tone: Int) = palette.color(tone, 0.4, 16.0)
        fun t(tone: Int) = palette.color(tone, 0.6, 24.0, Math.PI / 3)
        fun n(tone: Int) = palette.color(tone, 0.08, 4.0)
        fun v(tone: Int) = palette.color(tone, 0.16, 8.0)
        val accent = if (dark) 80 else 40
        val onAccent = if (dark) 20 else 100
        val container = if (dark) 30 else 90
        val onContainer = if (dark) 90 else 10
        val base = if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = p(accent), onPrimary = p(onAccent),
            primaryContainer = p(container), onPrimaryContainer = p(onContainer),
            inversePrimary = p(if (dark) 40 else 80),
            secondary = s(accent), onSecondary = s(onAccent),
            secondaryContainer = s(container), onSecondaryContainer = s(onContainer),
            tertiary = t(accent), onTertiary = t(onAccent),
            tertiaryContainer = t(container), onTertiaryContainer = t(onContainer),
            background = n(if (dark) 6 else 98), onBackground = n(if (dark) 90 else 10),
            surface = n(if (dark) 6 else 98), onSurface = n(if (dark) 90 else 10),
            surfaceVariant = v(if (dark) 30 else 90), onSurfaceVariant = v(if (dark) 80 else 30),
            surfaceTint = p(accent), inverseSurface = n(if (dark) 90 else 20),
            inverseOnSurface = n(if (dark) 20 else 95),
            outline = v(if (dark) 60 else 50), outlineVariant = v(if (dark) 30 else 80),
            scrim = Color.Black, surfaceBright = n(if (dark) 24 else 98),
            surfaceDim = n(if (dark) 6 else 87), surfaceContainer = n(if (dark) 12 else 94),
            surfaceContainerHigh = n(if (dark) 17 else 92),
            surfaceContainerHighest = n(if (dark) 22 else 90),
            surfaceContainerLow = n(if (dark) 10 else 96),
            surfaceContainerLowest = n(if (dark) 4 else 100),
            primaryFixed = p(90), primaryFixedDim = p(80),
            onPrimaryFixed = p(10), onPrimaryFixedVariant = p(30),
            secondaryFixed = s(90), secondaryFixedDim = s(80),
            onSecondaryFixed = s(10), onSecondaryFixedVariant = s(30),
            tertiaryFixed = t(90), tertiaryFixedDim = t(80),
            onTertiaryFixed = t(10), onTertiaryFixedVariant = t(30),
        )
    }

    /** Standard sRGB <-> D65 CIELAB conversion with bounded chroma search, using only JVM math. */
    private class SeedPalette(seed: Int) {
        private val hue: Double
        private val chroma: Double

        init {
            fun linear(byte: Int): Double {
                val channel = byte / 255.0
                return if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
            }
            val r = linear(seed ushr 16 and 255)
            val g = linear(seed ushr 8 and 255)
            val b = linear(seed and 255)
            fun lab(value: Double) = if (value > 216.0 / 24389) value.pow(1.0 / 3) else (24389.0 / 27 * value + 16) / 116
            val x = lab((0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047)
            val y = lab(0.2126729 * r + 0.7151522 * g + 0.0721750 * b)
            val z = lab((0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883)
            val a = 500 * (x - y)
            val labB = 200 * (y - z)
            chroma = hypot(a, labB).let { if (it < 0.001) 0.0 else it }
            hue = if (chroma == 0.0) 0.0 else atan2(labB, a)
        }

        fun color(tone: Int, chromaScale: Double, chromaLimit: Double, hueShift: Double = 0.0): Color {
            if (tone == 0) return Color.Black
            if (tone == 100) return Color.White
            val angle = hue + hueShift
            fun rgb(c: Double): DoubleArray {
                val fy = (tone + 16.0) / 116
                val fx = fy + c * cos(angle) / 500
                val fz = fy - c * sin(angle) / 200
                fun xyz(value: Double): Double {
                    val cube = value * value * value
                    return if (cube > 216.0 / 24389) cube else (116 * value - 16) / (24389.0 / 27)
                }
                val x = xyz(fx) * 0.95047
                val y = xyz(fy)
                val z = xyz(fz) * 1.08883
                return doubleArrayOf(
                    3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
                    -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
                    0.0556434 * x - 0.2040259 * y + 1.0572252 * z,
                )
            }
            var upper = (chroma * chromaScale).coerceAtMost(chromaLimit)
            var lower = 0.0
            var result = rgb(upper)
            if (result.any { it < 0 || it > 1 }) {
                repeat(14) {
                    val middle = (lower + upper) / 2
                    if (rgb(middle).all { it in 0.0..1.0 }) lower = middle else upper = middle
                }
                result = rgb(lower)
            }
            fun byte(channel: Double): Int {
                val srgb = if (channel <= 0.0031308) channel * 12.92 else 1.055 * channel.pow(1.0 / 2.4) - 0.055
                return (srgb * 255).roundToInt().coerceIn(0, 255)
            }
            return Color(0xff000000.toInt() or (byte(result[0]) shl 16) or (byte(result[1]) shl 8) or byte(result[2]))
        }
    }
}
