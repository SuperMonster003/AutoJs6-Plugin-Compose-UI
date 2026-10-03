package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.model.ShapeKind
import org.autojs.plugin.compose.api.model.UiValue
import org.junit.Assert.*
import org.junit.Test

class ValueMapperTest {
    @Test fun colorsPreserveArgbAndMatchAndroidNamedColorSemantics() {
        assertEquals(0xff0099ee.toInt(), ValueMapper.parseColor("#0099eE"))
        assertEquals(0x01020304, ValueMapper.parseColor("#01020304"))
        assertEquals(0, ValueMapper.parseColor("#00000000"))
        assertEquals(0xffffffff.toInt(), ValueMapper.parseColor("#FFFFFFFF"))
        val cases = mapOf(
            "ReD" to 0xffff0000.toInt(), "GREEN" to 0xff00ff00.toInt(),
            "blue" to 0xff0000ff.toInt(), "gray" to 0xff888888.toInt(),
            "GREY" to 0xff888888.toInt(), "darkgray" to 0xff444444.toInt(),
            "darkgrey" to 0xff444444.toInt(), "lightgray" to 0xffcccccc.toInt(),
            "LIGHTGREY" to 0xffcccccc.toInt(), "aqua" to 0xff00ffff.toInt(),
            "fuchsia" to 0xffff00ff.toInt(), "lime" to 0xff00ff00.toInt(),
            "maroon" to 0xff800000.toInt(), "navy" to 0xff000080.toInt(),
            "olive" to 0xff808000.toInt(), "purple" to 0xff800080.toInt(),
            "silver" to 0xffc0c0c0.toInt(), "teal" to 0xff008080.toInt(),
        )
        cases.forEach { (name, argb) -> assertEquals(name, argb, ValueMapper.parseColor(name)) }
        assertEquals(0x12345678, ValueMapper.color(UiValue.Color(0x12345678)).toArgb())
        assertEquals(Color.Unspecified, ValueMapper.color(UiValue.Null))
        assertEquals(Color.Blue, ValueMapper.color(null, Color.Blue))
        listOf("#fff", "#12345", "#1234567", "#123456789", "#gg1122", "#-12345", "", "unknown", " red").forEach {
            invalid { ValueMapper.parseColor(it) }
        }
        invalid { ValueMapper.color(UiValue.Str("red")) }
    }

    @Test fun dimensionsKeepTheirUnitAndSignedOffsetWithoutSilentlyCoercingKinds() {
        assertEquals(UiValue.Dp(8.0), ValueMapper.parseDimension("8dp"))
        assertEquals(UiValue.Sp(16.5), ValueMapper.parseDimension("16.5sp"))
        assertEquals(UiValue.Dp(-2.25), ValueMapper.parseDimension("-2.25dp"))
        assertEquals(UiValue.Sp(0.5), ValueMapper.parseDimension("+.5sp"))
        assertEquals(UiValue.Dp(100.0), ValueMapper.parseDimension("1e2dp"))
        assertEquals(UiValue.Dp(12.0), ValueMapper.parseDimension(12))
        assertEquals(8.dp, ValueMapper.dp(ValueMapper.parseDimension("8dp")))
        assertEquals((-0.5).sp, ValueMapper.sp(ValueMapper.parseDimension("-.5sp")))
        assertEquals(10.dp, ValueMapper.dp(null, 10.dp))
        assertEquals(12.sp, ValueMapper.sp(UiValue.Null, 12.sp))
        listOf("16", "1px", "8DP", "1 dp", "NaNdp", "Infinitysp", "1e999dp", "3.5e38dp", "--2dp", "dp").forEach {
            invalid { ValueMapper.parseDimension(it) }
        }
        invalid { ValueMapper.parseDimension(Double.NaN) }
        invalid { ValueMapper.dp(UiValue.Sp(8.0)) }
        invalid { ValueMapper.sp(UiValue.Num(8.0)) }
    }

    @Test fun typographyRolesAndPartialStylesRetainInheritedAttributes() {
        val typography = Typography()
        val roles = mapOf(
            "displayLarge" to typography.displayLarge, "displayMedium" to typography.displayMedium,
            "displaySmall" to typography.displaySmall, "headlineLarge" to typography.headlineLarge,
            "headlineMedium" to typography.headlineMedium, "headlineSmall" to typography.headlineSmall,
            "titleLarge" to typography.titleLarge, "titleMedium" to typography.titleMedium,
            "titleSmall" to typography.titleSmall, "bodyLarge" to typography.bodyLarge,
            "bodyMedium" to typography.bodyMedium, "bodySmall" to typography.bodySmall,
            "labelLarge" to typography.labelLarge, "labelMedium" to typography.labelMedium,
            "labelSmall" to typography.labelSmall,
        )
        roles.forEach { (name, expected) -> assertEquals(name, expected, ValueMapper.textStyle(UiValue.Enum(name), typography)) }
        val base = TextStyle(color = Color.Blue, fontSize = 11.sp, fontStyle = FontStyle.Italic, lineHeight = 18.sp)
        val mapped = ValueMapper.textStyle(UiValue.TextStyle(fontFamily = "monospace", fontWeight = 675,
            italic = false, letterSpacing = -0.5), typography, base)
        assertEquals(Color.Blue, mapped.color)
        assertEquals(11.sp, mapped.fontSize)
        assertEquals(18.sp, mapped.lineHeight)
        assertEquals(FontWeight(675), mapped.fontWeight)
        assertEquals(FontStyle.Normal, mapped.fontStyle)
        assertEquals(FontFamily.Monospace, mapped.fontFamily)
        assertEquals((-0.5).sp, mapped.letterSpacing)
        assertEquals(typography.bodyLarge, ValueMapper.textStyle(null, typography))
        assertEquals(base, ValueMapper.textStyle(UiValue.Null, typography, base))
        invalid { ValueMapper.textStyle(UiValue.Enum("BodyLarge"), typography) }
        invalid { ValueMapper.textStyle(UiValue.Str("bodyLarge"), typography) }
    }

    @Test fun shapesAndGenericFontsUseComposeTypes() {
        assertSame(RectangleShape, ValueMapper.shape(UiValue.Shape(ShapeKind.RECTANGLE)))
        assertSame(CircleShape, ValueMapper.shape(UiValue.Shape(ShapeKind.CIRCLE)))
        assertSame(CircleShape, ValueMapper.shape(null, CircleShape))
        val rounded = ValueMapper.shape(UiValue.Shape(ShapeKind.ROUNDED, 7.5)) as RoundedCornerShape
        assertEquals(15f, rounded.topStart.toPx(Size(100f, 100f), Density(2f)), 0f)
        assertEquals(15f, rounded.bottomEnd.toPx(Size(100f, 100f), Density(2f)), 0f)
        assertNull(ValueMapper.fontFamily(null))
        assertSame(FontFamily.Default, ValueMapper.fontFamily("default"))
        assertSame(FontFamily.SansSerif, ValueMapper.fontFamily("Sans-Serif"))
        assertSame(FontFamily.Serif, ValueMapper.fontFamily("serif"))
        assertSame(FontFamily.Cursive, ValueMapper.fontFamily("cursive"))
        invalid { ValueMapper.shape(UiValue.Enum("circle")) }
    }

    @Test fun enumMappingsDistinguishLogicalFromPhysicalDirections() {
        assertEquals(Alignment.TopStart, ValueMapper.boxAlignment(null))
        assertEquals(Alignment.BottomEnd, ValueMapper.boxAlignment(UiValue.Enum("bottomEnd")))
        assertEquals(Alignment.End, ValueMapper.horizontalAlignment(UiValue.Enum("end")))
        assertEquals(Alignment.Bottom, ValueMapper.verticalAlignment(UiValue.Enum("bottom")))
        assertEquals(TextAlign.Start, ValueMapper.textAlign(null))
        assertEquals(TextAlign.Left, ValueMapper.textAlign(UiValue.Enum("left")))
        assertEquals(TextAlign.End, ValueMapper.textAlign(UiValue.Enum("end")))
        assertEquals(TextOverflow.Ellipsis, ValueMapper.textOverflow(UiValue.Enum("ellipsis")))
        assertSame(ContentScale.Crop, ValueMapper.contentScale(UiValue.Enum("crop")))
        invalid { ValueMapper.boxAlignment(UiValue.Enum("left")) }
        invalid { ValueMapper.textAlign(UiValue.Enum("middle")) }
        invalid { ValueMapper.contentScale(UiValue.Str("crop")) }
    }

    private fun invalid(block: () -> Unit) {
        val error = assertThrows(ComposeUiContractException::class.java, block)
        assertEquals(ComposeUiErrorCodes.INVALID_ARGUMENT, error.code)
    }
}
