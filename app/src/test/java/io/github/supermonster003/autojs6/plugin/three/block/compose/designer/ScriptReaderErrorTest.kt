package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Roadmap D35: structures the designer cannot map are refused with a position, never dropped silently. */
class ScriptReaderErrorTest {
    private val reader = ScriptReader()

    private fun refuse(script: String): DesignerParseError = try {
        reader.read(script)
        fail("expected the script to be refused")
        throw IllegalStateException()
    } catch (error: DesignerParseError) {
        error
    }

    @Test
    fun `syntax errors carry the line`() {
        val error = refuse("\"ui\";\n\ncompose.mount(function () {\n    return compose.Column({padding: 16},\n        compose.Text({text: 'a'}\n});\n")
        assertTrue("line ${error.line}: ${error.reason}", error.line >= 5)
        assertTrue(error.column >= 1)
    }

    @Test
    fun `a script without a mount is refused`() {
        val error = refuse("\"ui\";\nlet count = compose.state(0);\n")
        assertEquals(1, error.line)
        assertTrue(error.reason.contains("compose.mount"))
    }

    @Test
    fun `two mounts are refused at the second one`() {
        val error = refuse("compose.mount(compose.Text({text: 'a'}));\ncompose.floaty(compose.Text({text: 'b'}));\n")
        assertEquals(2, error.line)
        assertTrue(error.reason, error.reason.contains("one mount"))
    }

    @Test
    fun `a root that is not a factory call is refused`() {
        val error = refuse("let page = compose.Column({});\ncompose.mount(page);\n")
        assertEquals(2, error.line)
        assertEquals(15, error.column)
        assertTrue(error.reason, error.reason.contains("compose.Column"))
    }

    @Test
    fun `a render function without a returned node is refused`() {
        val error = refuse("compose.mount(function () {\n    let node = compose.Text({text: 'x'});\n});\n")
        assertEquals(1, error.line)
        assertTrue(error.reason, error.reason.contains("return"))
    }

    @Test
    fun `spread and computed keys cannot be mapped`() {
        val spread = refuse("let base = {padding: 16};\ncompose.mount(compose.Column({...base}, compose.Text({text: 'x'})));\n")
        assertEquals(2, spread.line)
        assertTrue(spread.reason, spread.reason.contains("spread"))
        val computed = refuse("let name = 'text';\ncompose.mount(compose.Text({[name]: 'x'}));\n")
        assertEquals(2, computed.line)
        assertTrue(computed.reason, computed.reason.contains("plain names"))
    }

    @Test
    fun `children given twice are refused`() {
        val error = refuse("compose.mount(compose.Column({children: [compose.Text({text: 'a'})]}, compose.Text({text: 'b'})));\n")
        assertEquals(1, error.line)
        assertTrue(error.reason, error.reason.contains("children"))
    }

    @Test
    fun `opaque children and expressions are accepted rather than refused`() {
        val document = reader.read("let rows = [];\ncompose.mount(function () {\n    return compose.Column({padding: cond ? 8 : 16}, rows, cond && compose.Text({text: 'x'}), false);\n});\n")
        val column = document.root!!
        assertEquals(3, column.children.size)
        assertEquals("cond ? 8 : 16", (column.prop("padding") as io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue.Expression).source)
    }
}
