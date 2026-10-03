package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.graphics.Typeface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.model.ShapeKind
import org.autojs.plugin.compose.api.model.UiValue

/** Converts already validated V1 values. Missing and explicit null values use the caller's default. */
internal object ValueMapper {
    fun color(value: UiValue?, fallback: Color = Color.Unspecified): Color = when (value) {
        null, UiValue.Null -> fallback
        is UiValue.Color -> Color(value.argb)
        else -> invalid("Expected a color")
    }

    fun dp(value: UiValue?, fallback: Dp = Dp.Unspecified): Dp = when (value) {
        null, UiValue.Null -> fallback
        is UiValue.Dp -> value.value.toFloat().dp
        else -> invalid("Expected a dp dimension")
    }

    fun sp(value: UiValue?, fallback: TextUnit = TextUnit.Unspecified): TextUnit = when (value) {
        null, UiValue.Null -> fallback
        is UiValue.Sp -> value.value.toFloat().sp
        else -> invalid("Expected an sp dimension")
    }

    fun shape(value: UiValue?, fallback: Shape = RectangleShape): Shape = when (value) {
        null, UiValue.Null -> fallback
        is UiValue.Shape -> when (value.shape) {
            ShapeKind.RECTANGLE -> RectangleShape
            ShapeKind.ROUNDED -> RoundedCornerShape(value.radius.toFloat().dp)
            ShapeKind.CIRCLE -> CircleShape
        }
        else -> invalid("Expected a shape")
    }

    fun textStyle(value: UiValue?, typography: Typography, fallback: TextStyle = typography.bodyLarge): TextStyle = when (value) {
        null, UiValue.Null -> fallback
        is UiValue.Enum -> when (value.value) {
            "displayLarge" -> typography.displayLarge
            "displayMedium" -> typography.displayMedium
            "displaySmall" -> typography.displaySmall
            "headlineLarge" -> typography.headlineLarge
            "headlineMedium" -> typography.headlineMedium
            "headlineSmall" -> typography.headlineSmall
            "titleLarge" -> typography.titleLarge
            "titleMedium" -> typography.titleMedium
            "titleSmall" -> typography.titleSmall
            "bodyLarge" -> typography.bodyLarge
            "bodyMedium" -> typography.bodyMedium
            "bodySmall" -> typography.bodySmall
            "labelLarge" -> typography.labelLarge
            "labelMedium" -> typography.labelMedium
            "labelSmall" -> typography.labelSmall
            else -> invalid("Unknown typography role")
        }
        is UiValue.TextStyle -> fallback.merge(TextStyle(
            fontFamily = fontFamily(value.fontFamily),
            fontSize = value.fontSize?.toFloat()?.sp ?: TextUnit.Unspecified,
            fontWeight = value.fontWeight?.let(::FontWeight),
            fontStyle = value.italic?.let { if (it) FontStyle.Italic else FontStyle.Normal },
            lineHeight = value.lineHeight?.toFloat()?.sp ?: TextUnit.Unspecified,
            letterSpacing = value.letterSpacing?.toFloat()?.sp ?: TextUnit.Unspecified,
        ))
        else -> invalid("Expected a typography role or text style")
    }

    /** Other names select installed Android font families; unknown system names use Android's fallback. */
    fun fontFamily(name: String?): FontFamily? = when (name?.lowercase(Locale.ROOT)) {
        null -> null
        "default", "system" -> FontFamily.Default
        "sans-serif", "sansserif" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        "" -> invalid("Empty font family")
        else -> FontFamily(Typeface.create(name, Typeface.NORMAL))
    }

    fun boxAlignment(value: UiValue?): Alignment = when (enum(value, "topStart")) {
        "topStart" -> Alignment.TopStart
        "topCenter" -> Alignment.TopCenter
        "topEnd" -> Alignment.TopEnd
        "centerStart" -> Alignment.CenterStart
        "center" -> Alignment.Center
        "centerEnd" -> Alignment.CenterEnd
        "bottomStart" -> Alignment.BottomStart
        "bottomCenter" -> Alignment.BottomCenter
        "bottomEnd" -> Alignment.BottomEnd
        else -> invalid("Unknown box alignment")
    }

    fun horizontalAlignment(value: UiValue?): Alignment.Horizontal = when (enum(value, "start")) {
        "start" -> Alignment.Start
        "center" -> Alignment.CenterHorizontally
        "end" -> Alignment.End
        else -> invalid("Unknown horizontal alignment")
    }

    fun verticalAlignment(value: UiValue?): Alignment.Vertical = when (enum(value, "top")) {
        "top" -> Alignment.Top
        "center" -> Alignment.CenterVertically
        "bottom" -> Alignment.Bottom
        else -> invalid("Unknown vertical alignment")
    }

    fun textAlign(value: UiValue?): TextAlign = when (enum(value, "start")) {
        "start" -> TextAlign.Start
        "end" -> TextAlign.End
        "left" -> TextAlign.Left
        "right" -> TextAlign.Right
        "center" -> TextAlign.Center
        "justify" -> TextAlign.Justify
        else -> invalid("Unknown text alignment")
    }

    fun textOverflow(value: UiValue?): TextOverflow = when (enum(value, "clip")) {
        "clip" -> TextOverflow.Clip
        "ellipsis" -> TextOverflow.Ellipsis
        "visible" -> TextOverflow.Visible
        else -> invalid("Unknown text overflow")
    }

    fun contentScale(value: UiValue?): ContentScale = when (enum(value, "fit")) {
        "fit" -> ContentScale.Fit
        "crop" -> ContentScale.Crop
        "fillBounds" -> ContentScale.FillBounds
        "fillWidth" -> ContentScale.FillWidth
        "fillHeight" -> ContentScale.FillHeight
        "inside" -> ContentScale.Inside
        "none" -> ContentScale.None
        else -> invalid("Unknown content scale")
    }

    /** Android Color.parseColor names and its two accepted hex forms, without Android stub calls. */
    fun parseColor(value: String): Int {
        if (value.startsWith('#')) {
            val digits = value.substring(1)
            if (digits.length != 6 && digits.length != 8 || digits.any { it.digitToIntOrNull(16) == null }) {
                invalid("Expected #RRGGBB or #AARRGGBB")
            }
            val parsed = digits.toLong(16)
            return (if (digits.length == 6) parsed or 0xff000000L else parsed).toInt()
        }
        return namedColors[value.lowercase(Locale.ROOT)] ?: invalid("Unknown color name")
    }

    /** Signed dimensions stay signed here: the property/Modifier catalog decides whether negatives are legal. */
    fun parseDimension(value: String): UiValue {
        val match = dimensionPattern.matchEntire(value) ?: invalid("Expected a dp or sp dimension")
        val number = match.groupValues[1].toDoubleOrNull() ?: invalid("Invalid dimension")
        return if (match.groupValues[2] == "dp") UiValue.Dp(number) else UiValue.Sp(number)
    }

    fun parseDimension(value: Number): UiValue.Dp = UiValue.Dp(value.toDouble())

    private fun enum(value: UiValue?, fallback: String) = when (value) {
        null, UiValue.Null -> fallback
        is UiValue.Enum -> value.value
        else -> invalid("Expected an enum")
    }

    private fun invalid(message: String): Nothing = throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, message)

    private val dimensionPattern = Regex("([+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)(dp|sp)")
    private val namedColors = mapOf(
        "black" to 0xff000000.toInt(), "darkgray" to 0xff444444.toInt(), "gray" to 0xff888888.toInt(),
        "lightgray" to 0xffcccccc.toInt(), "white" to 0xffffffff.toInt(), "red" to 0xffff0000.toInt(),
        "green" to 0xff00ff00.toInt(), "blue" to 0xff0000ff.toInt(), "yellow" to 0xffffff00.toInt(),
        "cyan" to 0xff00ffff.toInt(), "magenta" to 0xffff00ff.toInt(), "aqua" to 0xff00ffff.toInt(),
        "fuchsia" to 0xffff00ff.toInt(), "darkgrey" to 0xff444444.toInt(), "grey" to 0xff888888.toInt(),
        "lightgrey" to 0xffcccccc.toInt(), "lime" to 0xff00ff00.toInt(), "maroon" to 0xff800000.toInt(),
        "navy" to 0xff000080.toInt(), "olive" to 0xff808000.toInt(), "purple" to 0xff800080.toInt(),
        "silver" to 0xffc0c0c0.toInt(), "teal" to 0xff008080.toInt(),
    )
}
