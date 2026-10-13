package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** How a container lays out its primary children, as far as choosing a drop position is concerned. */
internal enum class LayoutAxis { VERTICAL, HORIZONTAL, GRID, STACK }

/**
 * Layout axes of the containers the renderer draws (roadmap P9.1). The contract catalog says which
 * components take children but not how they arrange them, so the designer keeps this table next to the
 * renderer it mirrors; `LayoutAxesTest` fails when a catalog container is missing from it.
 */
internal object LayoutAxes {
    val explicit: Map<String, LayoutAxis> = buildMap {
        listOf("Column", "LazyColumn", "Card", "NavigationRail", "NavigationDrawer", "ModalBottomSheet", "DropdownMenu", "SearchBar")
            .forEach { put(it, LayoutAxis.VERTICAL) }
        listOf("Row", "LazyRow", "Button", "ElevatedButton", "FilledTonalButton", "OutlinedButton", "TextButton", "NavigationBar", "TabRow", "SegmentedButton", "HorizontalPager")
            .forEach { put(it, LayoutAxis.HORIZONTAL) }
        put("LazyVerticalGrid", LayoutAxis.GRID)
        listOf("Box", "Surface", "Scaffold", "FloatingActionButton", "IconButton", "PullToRefresh", "Tooltip")
            .forEach { put(it, LayoutAxis.STACK) }
    }

    fun of(type: String): LayoutAxis = explicit[type] ?: LayoutAxis.STACK
}

/** Where a dragged node lands: an index among the container's children without it, and the line to draw. */
internal data class DropPlacement(val index: Int, val line: Pair<Offset, Offset>?)

internal object DropPlacements {
    private const val LINE_INSET = 2f

    /**
     * Places a drop at [pointer] inside [container] whose primary children (the dragged node already left
     * out) have the bounds [children], in document order; null stands for a child without bounds (an
     * expression child or an item a lazy list has not composed), which keeps its position but is never
     * compared. The order on screen may run against document order (right-to-left rows, reversed
     * lists), so the direction is read from the first and last bounded children. A stack appends.
     */
    fun place(axis: LayoutAxis, pointer: Offset, container: Rect?, children: List<Rect?>): DropPlacement {
        val bounded = children.withIndex().filter { it.value != null }.map { IndexedValue(it.index, it.value!!) }
        if (axis == LayoutAxis.STACK || bounded.isEmpty()) {
            return DropPlacement(children.size, if (axis == LayoutAxis.STACK) null else container?.let { emptyLine(axis, it) })
        }
        return when (axis) {
            LayoutAxis.VERTICAL -> linear(pointer.y, bounded, children.size, container, vertical = true)
            LayoutAxis.HORIZONTAL -> linear(pointer.x, bounded, children.size, container, vertical = false)
            LayoutAxis.GRID -> grid(pointer, bounded, children.size)
            LayoutAxis.STACK -> error("handled above")
        }
    }

    private fun linear(position: Float, bounded: List<IndexedValue<Rect>>, count: Int, container: Rect?, vertical: Boolean): DropPlacement {
        fun center(rect: Rect) = if (vertical) rect.center.y else rect.center.x
        val reversed = bounded.size > 1 && center(bounded.first().value) > center(bounded.last().value)
        val next = bounded.firstOrNull { (_, rect) -> if (reversed) center(rect) < position else center(rect) > position }
        val previous = bounded.lastOrNull { next == null || it.index < next.index }
        val index = next?.index ?: count
        // The line sits halfway between the neighbours, or just outside the end child when there is one only.
        val start = previous?.value
        val end = next?.value
        val across = container ?: (end ?: start)!!
        return if (vertical) {
            val y = when {
                start != null && end != null -> if (reversed) (start.top + end.bottom) / 2 else (start.bottom + end.top) / 2
                end != null -> if (reversed) end.bottom + LINE_INSET else end.top - LINE_INSET
                else -> if (reversed) start!!.top - LINE_INSET else start!!.bottom + LINE_INSET
            }
            DropPlacement(index, Offset(across.left, y) to Offset(across.right, y))
        } else {
            val x = when {
                start != null && end != null -> if (reversed) (start.left + end.right) / 2 else (start.right + end.left) / 2
                end != null -> if (reversed) end.right + LINE_INSET else end.left - LINE_INSET
                else -> if (reversed) start!!.left - LINE_INSET else start!!.right + LINE_INSET
            }
            DropPlacement(index, Offset(x, across.top) to Offset(x, across.bottom))
        }
    }

    /** Row-major cells: a child follows the pointer when it lies in a lower row, or in its row past it. */
    private fun grid(pointer: Offset, bounded: List<IndexedValue<Rect>>, count: Int): DropPlacement {
        val reversed = bounded.size > 1 && bounded.first().value.center.x > bounded[1].value.center.x && bounded[1].value.top < bounded.first().value.bottom
        val next = bounded.firstOrNull { (_, rect) ->
            rect.top > pointer.y || (pointer.y in rect.top..rect.bottom && if (reversed) rect.center.x < pointer.x else rect.center.x > pointer.x)
        }
        val index = next?.index ?: count
        val cell = next?.value ?: bounded.last().value
        val x = when {
            next == null -> if (reversed) cell.left - LINE_INSET else cell.right + LINE_INSET
            reversed -> cell.right + LINE_INSET
            else -> cell.left - LINE_INSET
        }
        return DropPlacement(index, Offset(x, cell.top) to Offset(x, cell.bottom))
    }

    private fun emptyLine(axis: LayoutAxis, container: Rect): Pair<Offset, Offset> = when (axis) {
        LayoutAxis.HORIZONTAL -> Offset(container.left + LINE_INSET, container.top) to Offset(container.left + LINE_INSET, container.bottom)
        else -> Offset(container.left, container.top + LINE_INSET) to Offset(container.right, container.top + LINE_INSET)
    }
}
