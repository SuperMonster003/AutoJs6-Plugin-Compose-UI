package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue

/** One visible line of the outline: a node, a slot header or an opaque child. */
internal data class OutlineRow(val id: Int?, val depth: Int, val label: String, val detail: String, val slot: String?, val expression: Boolean, val hasChildren: Boolean, val node: DesignNode?)

internal fun outlineRows(root: DesignNode?, collapsed: Set<Int>): List<OutlineRow> {
    val rows = ArrayList<OutlineRow>()
    fun visit(node: DesignNode, depth: Int, slot: String?) {
        val hasChildren = node.children.isNotEmpty() || node.slots.isNotEmpty()
        rows += OutlineRow(node.id, depth, node.type, summary(node), slot, false, hasChildren, node)
        if (node.id in collapsed) return
        node.slots.forEach { entry ->
            when (val child = entry.child) {
                is DesignChild.Node -> visit(child.node, depth + 1, entry.name)
                is DesignChild.Expression -> rows += OutlineRow(null, depth + 1, "{${child.source}}", "", entry.name, true, false, null)
            }
        }
        node.children.forEach { child ->
            when (child) {
                is DesignChild.Node -> visit(child.node, depth + 1, null)
                is DesignChild.Expression -> rows += OutlineRow(null, depth + 1, "{${child.source}}", "", null, true, false, null)
            }
        }
    }
    root?.let { visit(it, 0, null) }
    return rows
}

private fun summary(node: DesignNode): String {
    val key = (node.key as? PropValue.Literal)?.value?.let { (it as? JsLiteral.Str)?.value ?: (it as? JsLiteral.Num)?.text }
    val text = (node.prop("text") as? PropValue.Literal)?.value?.let { (it as? JsLiteral.Str)?.value }
    return listOfNotNull(key?.let { "#$it" }, text?.let { "\"$it\"" }).joinToString(" ")
}

private enum class DropZone { BEFORE, INTO, AFTER }

/**
 * The outline tree (roadmap D36): rows in document order with collapse toggles, tap selection, up / down /
 * delete actions on the selected row, and long-press drag onto another row to reorder (top or bottom
 * quarter) or reparent (middle, when the target accepts children).
 */
@Composable
internal fun OutlinePanel(session: DesignerSession, palette: StandalonePalette, modifier: Modifier = Modifier) {
    val collapsed = remember { mutableStateMapOf<Int, Boolean>() }
    val rows = remember(session.document, collapsed.size) { outlineRows(session.document.root, collapsed.keys) }
    val rowBounds = remember { mutableStateMapOf<Int, Rect>() }
    var dropIndicator by remember { mutableStateOf<Pair<Int, DropZone>?>(null) }
    val accent = Color(palette.accent)
    LazyColumn(modifier = modifier.fillMaxSize().testTag("designer-outline")) {
        items(rows, key = { row -> row.id?.toString() ?: "expr-${rows.indexOf(row)}" }) { row ->
            val selected = row.id != null && row.id == session.selectedId
            val indicator = dropIndicator?.takeIf { it.first == row.id }
            val background = when {
                indicator?.second == DropZone.INTO -> accent.copy(alpha = 0.22f)
                selected -> accent.copy(alpha = 0.12f)
                else -> Color.Transparent
            }
            Box(Modifier.fillMaxWidth().onGloballyPositioned { coords -> row.id?.let { rowBounds[it] = coords.boundsInRoot() } }) {
                if (indicator?.second == DropZone.BEFORE) Box(Modifier.fillMaxWidth().height(2.dp).background(accent).align(Alignment.TopCenter))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(background)
                        .then(if (row.id != null) Modifier.testTag("designer-outline-${row.id}") else Modifier)
                        .clickable(enabled = row.id != null) { session.selectedId = row.id }
                        .pointerInput(row.id, session.revision) {
                            val id = row.id ?: return@pointerInput
                            if (id == session.document.root?.id) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { session.draggingId = id; session.selectedId = id },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val origin = rowBounds[id]?.topLeft ?: Offset.Zero
                                    val point = origin + change.position
                                    val target = rowBounds.entries.firstOrNull { (targetId, rect) -> targetId != id && rect.contains(point) }
                                    dropIndicator = target?.let { (targetId, rect) ->
                                        val zone = when {
                                            point.y < rect.top + rect.height * 0.25f -> DropZone.BEFORE
                                            point.y > rect.bottom - rect.height * 0.25f -> DropZone.AFTER
                                            else -> DropZone.INTO
                                        }
                                        val resolved = if (zone == DropZone.INTO && !session.canDropInto(id, targetId)) DropZone.AFTER else zone
                                        targetId to resolved
                                    }
                                },
                                onDragEnd = {
                                    val target = dropIndicator
                                    dropIndicator = null
                                    session.draggingId = null
                                    if (target != null) applyDrop(session, id, target.first, target.second)
                                },
                                onDragCancel = { dropIndicator = null; session.draggingId = null },
                            )
                        }
                        .padding(start = (8 + row.depth * 16).dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    if (row.hasChildren && row.id != null) {
                        val isCollapsed = row.id in collapsed
                        Text(
                            if (isCollapsed) "+" else "-",
                            color = Color(palette.muted),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(18.dp).clickable { if (isCollapsed) collapsed.remove(row.id) else collapsed[row.id] = true },
                        )
                    } else Spacer(Modifier.width(18.dp))
                    row.slot?.let { Text("$it:", color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp)) }
                    Text(
                        row.label,
                        color = Color(if (row.expression) palette.muted else palette.text),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = if (row.expression) FontFamily.Monospace else FontFamily.Default,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (row.detail.isNotEmpty()) Text(" " + row.detail, color = Color(palette.muted), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.weight(1f))
                    if (selected && row.id != session.document.root?.id) {
                        IconButton(onClick = { session.moveSelected(-1) }, modifier = Modifier.size(32.dp).testTag("designer-move-up")) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.designer_move_up), tint = Color(palette.muted))
                        }
                        IconButton(onClick = { session.moveSelected(1) }, modifier = Modifier.size(32.dp).testTag("designer-move-down")) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.designer_move_down), tint = Color(palette.muted))
                        }
                        IconButton(onClick = { session.deleteSelected() }, modifier = Modifier.size(32.dp).testTag("designer-delete-node")) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.designer_delete_node), tint = Color(palette.danger))
                        }
                    }
                }
                if (indicator?.second == DropZone.AFTER) Box(Modifier.fillMaxWidth().height(2.dp).background(accent).align(Alignment.BottomCenter))
            }
        }
    }
}

private fun applyDrop(session: DesignerSession, dragged: Int, targetId: Int, zone: DropZone) {
    when (zone) {
        DropZone.INTO -> session.moveInto(dragged, targetId)
        DropZone.BEFORE, DropZone.AFTER -> {
            val placement = session.document.parentOf(targetId) ?: return
            if (placement.slotName != null) return
            val draggedPlacement = session.document.parentOf(dragged) ?: return
            var index = placement.index + if (zone == DropZone.AFTER) 1 else 0
            if (draggedPlacement.parentId == placement.parentId && draggedPlacement.index < index) index--
            session.moveTo(dragged, placement.parentId, index)
        }
    }
    session.selectedId = dragged
}
