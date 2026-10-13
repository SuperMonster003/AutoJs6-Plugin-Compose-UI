package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsTreeFormatException
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsTreeJson
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsxReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Roadmap D42: TSX the designer cannot map is refused with a position (line and column of the offending node). */
class TsxReaderErrorTest {
    private val snapshots = TsxSnapshots()

    private fun refuse(case: String): DesignerParseError {
        val name = "errors/$case.tsx"
        try {
            TsxReader().read(snapshots.source(name), snapshots.tree(name))
        } catch (error: DesignerParseError) {
            return error
        }
        fail("$name should be refused")
        throw IllegalStateException()
    }

    private fun assertRefused(case: String, line: Int, column: Int, reason: String) {
        val error = refuse(case)
        assertEquals("$case: ${error.reason}", line to column, error.line to error.column)
        assertTrue("$case: ${error.reason}", error.reason.contains(reason))
    }

    @Test
    fun `every error case is covered`() {
        assertEquals(
            listOf(
                "children-twice", "generic-render", "invalid-entity", "namespaced-attribute", "no-mount", "no-return", "render-parameters",
                "reserved-tag", "root-not-compose", "root-two-elements", "spread-attribute", "spread-options", "syntax-error", "two-mounts",
                "typed-mount-variable", "xml-tag",
            ).map { "errors/$it.tsx" },
            snapshots.sources("errors"),
        )
    }

    @Test
    fun `syntax errors come from the parser diagnostics`() {
        val error = refuse("syntax-error")
        assertEquals(3, error.line)
        assertTrue(error.column > 1)
        assertTrue(error.reason, error.reason.isNotBlank())
    }

    @Test
    fun `the top level must hold exactly one mount with a render function or an element`() {
        assertRefused("no-mount", 1, 1, "no compose.mount")
        assertRefused("two-mounts", 2, 1, "one mount, floaty or dialog")
        assertRefused("no-return", 1, 15, "must end with return")
        assertRefused("render-parameters", 1, 16, "takes no parameters")
        assertRefused("generic-render", 1, 16, "type parameters")
        assertRefused("typed-mount-variable", 1, 16, "type annotation")
        assertRefused("spread-options", 1, 43, "spread")
    }

    @Test
    fun `the root and every node must be compose elements`() {
        assertRefused("root-not-compose", 2, 21, "compose element")
        assertRefused("root-two-elements", 1, 15, "exactly one compose element")
        assertRefused("xml-tag", 3, 9, "XML layout element")
        assertRefused("reserved-tag", 1, 32, "not a component")
    }

    @Test
    fun `attributes the model cannot keep are refused`() {
        assertRefused("spread-attribute", 2, 29, "Spread attributes")
        assertRefused("namespaced-attribute", 1, 29, "Namespaced attribute")
        assertRefused("children-twice", 1, 31, "children cannot be both")
        assertRefused("invalid-entity", 1, 28, "is not a valid character")
    }

    @Test
    fun `a tree of another schema or of other text is not read`() {
        try {
            TsTreeJson.parse("{\"schemaRevision\":2,\"root\":{\"kind\":\"SourceFile\",\"start\":0,\"end\":0}}")
            fail("schema revision 2 must be refused")
        } catch (error: TsTreeFormatException) {
            assertTrue(error.message!!.contains("revision 2"))
        }
        val name = "errors/no-mount.tsx"
        try {
            TsxReader().read(snapshots.source(name) + "\n", snapshots.tree(name))
            fail("a tree of a different text must be refused")
        } catch (error: DesignerParseError) {
            assertTrue(error.reason.contains("does not belong"))
        }
    }

    @Test
    fun `a handler probe reports positions in the body`() {
        val probe = TsxReader.handlerProbe(listOf("value: number"), "count.value = value;\nif (value > ) {}")
        assertTrue(probe.startsWith("(function (value: number) {\n"))
        // The probe itself is parsed by the service at run time; here only its framing is fixed.
        assertEquals(2, probe.lines().indexOf("if (value > ) {}"))
    }
}
