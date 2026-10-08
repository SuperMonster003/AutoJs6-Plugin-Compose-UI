package io.github.supermonster003.autojs6.plugin.compose.ui.app

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** The same fixed Material Color Utilities tone rule as the other standalone AutoJs6 plugins. */
internal object ThemeAccentRoles {
    data class Roles(val primary: Int, val onPrimary: Int)

    fun fromSeed(seed: Int, dark: Boolean): Roles {
        val source = Hct.fromInt(seed or -0x1000000)
        val chroma = if (source.chroma < 4.0) 0.0 else max(source.chroma, 48.0).coerceAtMost(96.0)
        val tones = TonalPalette.fromHueAndChroma(source.hue, chroma)
        return Roles(tones.tone(if (dark) 80 else 40), tones.tone(if (dark) 20 else 100))
    }
}

/** Pure input and value policy of the theme color picker. No preference changes while a draft is edited. */
internal object ThemeColorValue {
    val presets = listOf(0xffffdead.toInt(), 0xfff44336.toInt(), 0xffe91e63.toInt(),
        0xff9c27b0.toInt(), 0xff673ab7.toInt(), 0xff3f51b5.toInt(), 0xff2196f3.toInt(),
        0xff03a9f4.toInt(), 0xff00bcd4.toInt(), 0xff009688.toInt(), 0xff4caf50.toInt(),
        0xff8bc34a.toInt(), 0xffff9800.toInt(), 0xffff5722.toInt(), 0xff795548.toInt(), 0xff607d8b.toInt())
    private val hex = Regex("#?([0-9a-fA-F]{6})")
    private val rgb = Regex("rgb\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)", RegexOption.IGNORE_CASE)

    fun parse(input: String): Int? {
        val text = input.trim()
        hex.matchEntire(text)?.let { return it.groupValues[1].toInt(16) or -0x1000000 }
        val channels = rgb.matchEntire(text)?.groupValues?.drop(1)?.map(String::toInt) ?: return null
        if (channels.any { it !in 0..255 }) return null
        return -0x1000000 or (channels[0] shl 16) or (channels[1] shl 8) or channels[2]
    }

    fun hex(color: Int): String = String.format(Locale.ROOT, "#%06X", color and 0xffffff)
    fun rgb(color: Int): String = "rgb(${color ushr 16 and 255}, ${color ushr 8 and 255}, ${color and 255})"
}

/** Pure WCAG color math, matching the shared standalone palette's multi-surface fallback. */
internal object StandaloneColorPolicy {
    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val value = (color shr shift and 0xff) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun contrastRatio(first: Int, second: Int): Double =
        (max(luminance(first), luminance(second)) + 0.05) / (min(luminance(first), luminance(second)) + 0.05)

    fun onFilledColor(background: Int): Int =
        if (contrastRatio(-0x1000000, background) >= contrastRatio(-1, background)) -0x1000000 else -1

    fun readableAccent(color: Int, background: Int): Int {
        val opaque = color or -0x1000000
        if (contrastRatio(opaque, background) >= 4.5) return opaque
        fun toward(target: Int): Int? {
            if (contrastRatio(target, background) < 4.5) return null
            var low = 0.0
            var high = 1.0
            repeat(18) {
                val middle = (low + high) / 2.0
                if (contrastRatio(blend(opaque, target, middle), background) >= 4.5) high = middle else low = middle
            }
            return blend(opaque, target, high)
        }
        val source = luminance(opaque)
        return listOfNotNull(toward(-0x1000000), toward(-1)).minByOrNull { abs(luminance(it) - source) }
            ?: onFilledColor(background)
    }

    fun blend(first: Int, second: Int, ratio: Double): Int {
        fun channel(shift: Int): Int {
            val start = first shr shift and 0xff
            val end = second shr shift and 0xff
            return (start + (end - start) * ratio).toInt().coerceIn(0, 255)
        }
        return -0x1000000 or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}

/** Neutral surfaces never depend on a theme seed; primary and accent keep their separate host roles. */
internal data class StandalonePalette(
    val primary: Int,
    val onPrimary: Int,
    val accent: Int,
    val onAccent: Int,
    val background: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val text: Int,
    val muted: Int,
    val outline: Int,
    val divider: Int,
    val danger: Int,
    val isDark: Boolean,
) {
    /** Material 3 scheme for Compose: controls, selection and emphasis use the accent, surfaces stay neutral. */
    fun colorScheme(): ColorScheme {
        val base = if (isDark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = Color(accent), onPrimary = Color(onAccent),
            primaryContainer = Color(StandaloneColorPolicy.blend(surface, accent, 0.2)), onPrimaryContainer = Color(text),
            secondary = Color(accent), onSecondary = Color(onAccent),
            secondaryContainer = Color(StandaloneColorPolicy.blend(surface, accent, 0.16)), onSecondaryContainer = Color(text),
            tertiary = Color(primary), onTertiary = Color(onPrimary),
            background = Color(background), onBackground = Color(text),
            surface = Color(surface), onSurface = Color(text),
            surfaceVariant = Color(surfaceVariant), onSurfaceVariant = Color(muted),
            surfaceContainer = Color(surface), surfaceContainerLow = Color(background), surfaceContainerLowest = Color(background),
            surfaceContainerHigh = Color(surfaceVariant), surfaceContainerHighest = Color(surfaceVariant),
            outline = Color(outline), outlineVariant = Color(divider),
            error = Color(danger), onError = Color(StandaloneColorPolicy.onFilledColor(danger)),
        )
    }

    companion object {
        fun resolve(appearance: HostAppearance): StandalonePalette {
            val dark = appearance.dark
            val background = if (dark) 0xff121212.toInt() else 0xfff3f4f5.toInt()
            val surface = if (dark) 0xff1e1e1e.toInt() else 0xffffffff.toInt()
            val variant = if (dark) 0xff292a2d.toInt() else 0xffebedef.toInt()
            val primary = ThemeAccentRoles.fromSeed(appearance.primary, dark)
            val accentRoles = ThemeAccentRoles.fromSeed(appearance.accent, dark)
            var accent = StandaloneColorPolicy.readableAccent(accentRoles.primary, background)
            repeat(8) {
                for (reference in listOf(background, surface, variant,
                    StandaloneColorPolicy.blend(background, accent, 0x1c / 255.0),
                    StandaloneColorPolicy.blend(surface, accent, 0x1c / 255.0))) {
                    accent = StandaloneColorPolicy.readableAccent(accent, reference)
                }
            }
            return StandalonePalette(
                primary.primary, primary.onPrimary, accent, accentRoles.onPrimary,
                background, surface, variant,
                if (dark) 0xffe6e1e5.toInt() else 0xff1d1b20.toInt(),
                if (dark) 0xffb9bac0.toInt() else 0xff5f6368.toInt(),
                if (dark) 0xff777a82.toInt() else 0xffc5c8ce.toInt(),
                if (dark) 0xff34363a.toInt() else 0xffe0e3e7.toInt(),
                if (dark) 0xffffb4ab.toInt() else 0xffb3261e.toInt(),
                dark,
            )
        }
    }
}
