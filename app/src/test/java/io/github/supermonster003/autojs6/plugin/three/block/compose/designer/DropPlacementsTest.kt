package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.DropPlacements
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LayoutAxes
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LayoutAxis
import org.autojs.plugin.compose.api.catalog.ChildrenPolicy
import org.autojs.plugin.compose.api.catalog.ComponentInvocation
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P9.1: a canvas drop lands at the position under the pointer, along the container's layout axis. */
class DropPlacementsTest {
    private val column = Rect(0f, 0f, 100f, 300f)
    private val rows = listOf(Rect(0f, 0f, 100f, 50f), Rect(0f, 60f, 100f, 110f), Rect(0f, 120f, 100f, 170f))

    @Test
    fun `every catalog container with several children has a layout axis`() {
        val containers = ComposeUiV2.interopCatalog.components
            .filter { it.invocation == ComponentInvocation.NODE && (it.childrenPolicy == ChildrenPolicy.MANY || it.childrenPolicy == ChildrenPolicy.LAZY_ITEMS) }
            .map { it.name }
        val missing = containers.filter { it !in LayoutAxes.explicit }
        assertTrue("containers without a layout axis: $missing", missing.isEmpty())
    }

    @Test
    fun `a vertical drop goes before the first child below the pointer`() {
        assertEquals(0, DropPlacements.place(LayoutAxis.VERTICAL, Offset(50f, 10f), column, rows).index)
        val middle = DropPlacements.place(LayoutAxis.VERTICAL, Offset(50f, 70f), column, rows)
        assertEquals(1, middle.index)
        // The line sits halfway between the first and the second child, across the container.
        assertEquals(Offset(0f, 55f) to Offset(100f, 55f), middle.line)
        assertEquals(3, DropPlacements.place(LayoutAxis.VERTICAL, Offset(50f, 250f), column, rows).index)
    }

    @Test
    fun `children without bounds keep their place in the index`() {
        val withExpression = listOf(rows[0], null, rows[2])
        assertEquals(2, DropPlacements.place(LayoutAxis.VERTICAL, Offset(50f, 100f), column, withExpression).index)
        // The unknown first child cannot be compared, so the drop goes before the first known child below.
        assertEquals(1, DropPlacements.place(LayoutAxis.VERTICAL, Offset(50f, 5f), column, listOf(null, rows[1])).index)
        assertEquals(3, DropPlacements.place(LayoutAxis.VERTICAL, Offset(50f, 200f), column, listOf(null, null, null)).index)
    }

    @Test
    fun `a horizontal drop follows the reading direction of the row`() {
        val row = Rect(0f, 0f, 300f, 40f)
        val ltr = listOf(Rect(0f, 0f, 90f, 40f), Rect(100f, 0f, 190f, 40f), Rect(200f, 0f, 290f, 40f))
        val between = DropPlacements.place(LayoutAxis.HORIZONTAL, Offset(120f, 20f), row, ltr)
        assertEquals(1, between.index)
        assertEquals(Offset(95f, 0f) to Offset(95f, 40f), between.line)
        assertEquals(2, DropPlacements.place(LayoutAxis.HORIZONTAL, Offset(160f, 20f), row, ltr).index)
        assertEquals(Offset(195f, 0f) to Offset(195f, 40f), DropPlacements.place(LayoutAxis.HORIZONTAL, Offset(160f, 20f), row, ltr).line)
        // Right to left: document order starts at the right edge.
        val rtl = ltr.reversed()
        assertEquals(0, DropPlacements.place(LayoutAxis.HORIZONTAL, Offset(295f, 20f), row, rtl).index)
        assertEquals(1, DropPlacements.place(LayoutAxis.HORIZONTAL, Offset(160f, 20f), row, rtl).index)
        assertEquals(3, DropPlacements.place(LayoutAxis.HORIZONTAL, Offset(10f, 20f), row, rtl).index)
    }

    @Test
    fun `a grid drop is row major`() {
        val cells = listOf(
            Rect(0f, 0f, 50f, 50f), Rect(60f, 0f, 110f, 50f),
            Rect(0f, 60f, 50f, 110f), Rect(60f, 60f, 110f, 110f),
        )
        assertEquals(1, DropPlacements.place(LayoutAxis.GRID, Offset(70f, 20f), null, cells).index)
        assertEquals(2, DropPlacements.place(LayoutAxis.GRID, Offset(100f, 20f), null, cells).index)
        assertEquals(3, DropPlacements.place(LayoutAxis.GRID, Offset(70f, 80f), null, cells).index)
        assertEquals(4, DropPlacements.place(LayoutAxis.GRID, Offset(100f, 100f), null, cells).index)
    }

    @Test
    fun `a stack appends without a line and an empty container shows its edge`() {
        val stacked = DropPlacements.place(LayoutAxis.STACK, Offset(50f, 10f), column, rows)
        assertEquals(3, stacked.index)
        assertNull(stacked.line)
        val empty = DropPlacements.place(LayoutAxis.VERTICAL, Offset(50f, 10f), column, emptyList())
        assertEquals(0, empty.index)
        assertEquals(Offset(0f, 2f) to Offset(100f, 2f), empty.line)
    }
}
