package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsxUiReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Roadmap P11.7: TSX ui scripts the designer cannot map are refused with the line and column of the offending node. */
class TsxUiReaderErrorTest {
    private val snapshots = TsxSnapshots()

    private fun refuse(case: String): DesignerParseError {
        val name = "ui-errors/$case.tsx"
        try {
            TsxUiReader().read(snapshots.source(name), snapshots.tree(name))
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
            listOf("compose", "duplicate", "layout-file", "no-directive", "not-jsx", "reassigned", "spread", "syntax", "two-roots").map { "ui-errors/$it.tsx" },
            snapshots.sources("ui-errors"),
        )
    }

    @Test
    fun `syntax errors come from the parser diagnostics`() {
        val error = refuse("syntax")
        assertEquals(3, error.line)
        assertTrue(error.column > 1)
        assertTrue(error.reason, error.reason.isNotBlank())
    }

    @Test
    fun `the script needs the directive and a JSX layout`() {
        assertRefused("no-directive", 1, 1, "does not start with the \"ui\" directive")
        assertRefused("not-jsx", 4, 11, "needs a JSX layout or a variable holding one")
        assertRefused("reassigned", 5, 11, "reassigned after its JSX layout")
        assertRefused("layout-file", 3, 1, "Layout files are not supported in TSX scripts")
    }

    @Test
    fun `elements the host's TSX runtime refuses are refused`() {
        assertRefused("spread", 4, 18, "Spread attributes")
        assertRefused("compose", 5, 9, "Compose elements cannot appear in a ui layout")
        assertRefused("two-roots", 4, 5, "exactly one root element")
        assertRefused("duplicate", 3, 25, "id is given twice")
    }
}
