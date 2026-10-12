package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsValueConverter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import org.autojs.plugin.compose.api.catalog.ValueType
import org.autojs.plugin.compose.api.model.ShapeKind
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.model.ValueKind
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap D36: the designer validates against the real contract catalog and reports, never blocks. */
class DesignValidatorTest {
    private val reader = ScriptReader()
    private val validator = DesignValidator()

    private fun errors(script: String) = validator.validate(reader.read(script)).filter { it.severity == Severity.ERROR }

    @Test
    fun `gallery scripts produce no errors`() {
        GalleryCatalog.entries.forEach { entry ->
            val diagnostics = validator.validate(reader.read(entry.script()))
            assertEquals("${entry.component}: ${diagnostics.filter { it.severity == Severity.ERROR }}", 0, diagnostics.count { it.severity == Severity.ERROR })
        }
    }

    @Test
    fun `unknown components, properties and events are reported with their target`() {
        val diagnostics = errors("compose.mount(compose.Column({padding: 16, colour: '#fff', onTap: function () {}}, compose.Txt({text: 'x'})));\n")
        assertTrue(diagnostics.toString(), diagnostics.any { it.target == "colour" && it.message.contains("no property") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.target == "onTap" && it.message.contains("no event") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.message.contains("Unknown component Txt") })
    }

    @Test
    fun `values are checked against the catalog types`() {
        val diagnostics = errors("compose.mount(compose.Column({spacing: -4, arrangement: 'middle'}, compose.Text({text: 'x', maxLines: 0, color: true}), compose.Icon({tint: '#000'})));\n")
        assertTrue(diagnostics.toString(), diagnostics.any { it.target == "spacing" })
        assertTrue(diagnostics.toString(), diagnostics.any { it.target == "arrangement" && it.message.contains("Expected one of") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.target == "maxLines" })
        assertTrue(diagnostics.toString(), diagnostics.any { it.target == "color" })
        assertTrue(diagnostics.toString(), diagnostics.any { it.target == "name" && it.message.contains("requires") })
    }

    @Test
    fun `scopes, children policies and item parents follow the contract`() {
        val diagnostics = errors("""
            compose.mount(compose.Box({},
                compose.Text({text: 'a', weight: 1}),
                compose.Spacer({}, compose.Text({text: 'child'})),
                compose.Tab({text: 'loose'}),
                compose.NavigationBar({key: 'bar'}, compose.Text({text: 'not an item'})),
                compose.Surface({}, compose.Text({text: 'one'}), compose.Text({text: 'two'})),
                compose.Row({}, compose.Text({key: 'dup', text: 'a'}), compose.Text({key: 'dup', text: 'b'}))));
        """.trimIndent() + "\n")
        assertTrue(diagnostics.toString(), diagnostics.any { it.target == "weight" && it.message.contains("Column / Row") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.message.contains("Spacer takes no children") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.message.contains("Tab must be a direct child of TabRow") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.message.contains("NavigationBar accepts only NavigationBarItem") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.message.contains("Surface takes a single child") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.message.contains("share the key 'dup'") })
    }

    @Test
    fun `modifier chains are checked by signature and scope`() {
        val diagnostics = errors("compose.mount(compose.Column({modifier: compose.modifier().padding(1, 2, 3).weight(1).fillMaxWidth(2).shadow(4)}, compose.Text({text: 'x', modifier: compose.modifier().weight(1)})));\n")
        assertTrue(diagnostics.toString(), diagnostics.any { it.message.contains("padding accepts") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.message.contains("weight only applies inside Column / Row") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.target == "modifier fillMaxWidth" && it.message.contains("range") })
        assertTrue(diagnostics.toString(), diagnostics.any { it.message.contains("Unknown modifier shadow") })
        val insideColumn = errors("compose.mount(compose.Column({}, compose.Text({text: 'x', weight: 1, modifier: compose.modifier().weight(2, false)})));\n")
        assertEquals("weight inside a Column is fine: $insideColumn", 0, insideColumn.size)
    }

    @Test
    fun `named colors and images are unverifiable warnings, hex colors convert`() {
        assertEquals(JsValueConverter.Conversion.Ok(UiValue.Color(0xFF112233.toInt())), JsValueConverter.convert(JsLiteral.Str("#112233"), ValueType(setOf(ValueKind.COLOR))))
        assertEquals(JsValueConverter.Conversion.Ok(UiValue.Color(0xFFFF0000.toInt())), JsValueConverter.convert(JsLiteral.Str("#f00"), ValueType(setOf(ValueKind.COLOR))))
        assertTrue(JsValueConverter.convert(JsLiteral.Str("red"), ValueType(setOf(ValueKind.COLOR))) is JsValueConverter.Conversion.Unverifiable)
        assertTrue(JsValueConverter.convert(JsLiteral.Str("./a.png"), ValueType(setOf(ValueKind.BITMAP))) is JsValueConverter.Conversion.Unverifiable)
        assertEquals(JsValueConverter.Conversion.Ok(UiValue.Shape(ShapeKind.ROUNDED, 12.0)), JsValueConverter.convert(JsLiteral.Obj(listOf("shape" to JsLiteral.Str("rounded"), "radius" to JsLiteral.Num(12.0))), ValueType(setOf(ValueKind.SHAPE))))
        assertEquals(JsValueConverter.Conversion.Ok(UiValue.Sp(18.0)), JsValueConverter.convert(JsLiteral.Str("18sp"), ValueType(setOf(ValueKind.SP), minimum = 0.0, minimumExclusive = true)))
        assertTrue(JsValueConverter.convert(JsLiteral.Str("18dp"), ValueType(setOf(ValueKind.SP))) is JsValueConverter.Conversion.Invalid)
        val warnings = validator.validate(reader.read("compose.mount(compose.Text({text: 'x', color: 'red'}));\n"))
        assertTrue(warnings.toString(), warnings.any { it.severity == Severity.WARNING && it.target == "color" })
        assertEquals(ComposeUiV2.interopCatalog.component("Text")!!.props.first { it.name == "color" }.type.kinds, setOf(ValueKind.COLOR, ValueKind.NULL))
    }
}
