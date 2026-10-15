package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.elementLabel

/** One row of the XML outline: an element with its depth, label and a short summary of its text. */
internal data class XmlOutlineRow(val id: Int, val depth: Int, val label: String, val detail: String)

internal fun xmlOutlineRows(root: XmlNode.Element?): List<XmlOutlineRow> {
    val rows = ArrayList<XmlOutlineRow>()
    fun visit(element: XmlNode.Element, depth: Int) {
        val text = element.attr("text")?.value ?: element.textContent?.trim()?.takeIf { it.isNotEmpty() }
        rows += XmlOutlineRow(element.id, depth, elementLabel(element), text?.let { "\"$it\"" } ?: "")
        element.elementChildren.forEach { visit(it, depth + 1) }
    }
    root?.let { visit(it, 0) }
    return rows
}

/**
 * The element tree of the fragment on the canvas (roadmap P10.3): rows in document order, tap selection, and
 * up / down / delete on the selected row. Collapsing, drag reordering and the fragment switcher follow in P10.4.
 */
@Composable
internal fun XmlOutlinePanel(session: UiDesignerSession, palette: StandalonePalette, modifier: Modifier = Modifier) {
    val rows = remember(session.document, session.fragment) { xmlOutlineRows(session.root) }
    val accent = Color(palette.accent)
    LazyColumn(modifier = modifier.fillMaxSize().testTag("designer-xml-outline"), contentPadding = PaddingValues(bottom = AddButtonClearance)) {
        items(rows, key = { it.id }) { row ->
            val selected = row.id == session.selectedId
            val problem = row.id == session.problemElementId
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent)
                    .testTag("designer-xml-outline-${row.id}")
                    .clickable { session.selectedId = row.id }
                    .padding(start = (12 + row.depth * 16).dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Text(row.label, color = Color(if (problem) palette.danger else palette.text), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                if (row.detail.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(row.detail, color = Color(palette.muted), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
                Spacer(Modifier.weight(1f))
                if (selected && row.depth > 0) {
                    IconButton(onClick = { session.moveSelected(-1) }, modifier = Modifier.testTag("designer-xml-move-up")) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.designer_move_up), tint = Color(palette.muted))
                    }
                    IconButton(onClick = { session.moveSelected(1) }, modifier = Modifier.testTag("designer-xml-move-down")) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.designer_move_down), tint = Color(palette.muted))
                    }
                    IconButton(onClick = { session.deleteSelected() }, modifier = Modifier.testTag("designer-xml-delete")) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.designer_delete_node), tint = Color(palette.danger))
                    }
                }
            }
        }
    }
}

/** The validator's diagnostics and the preview's problems; a tap on an element's entry selects it. */
@Composable
internal fun XmlProblemsPanel(session: UiDesignerSession, palette: StandalonePalette, modifier: Modifier = Modifier) {
    val diagnostics = session.diagnostics
    val problems = session.previewProblems
    LazyColumn(modifier = modifier.fillMaxSize().testTag("designer-xml-problems"), contentPadding = PaddingValues(bottom = AddButtonClearance)) {
        if (diagnostics.isEmpty() && problems.isEmpty()) item {
            Text(stringResource(R.string.designer_no_issues), color = Color(palette.muted), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }
        items(diagnostics) { diagnostic ->
            val label = diagnostic.nodeId?.let { id -> session.document.element(id)?.let(::elementLabel) }
            Column(Modifier.fillMaxWidth().clickable(enabled = diagnostic.nodeId != null) { diagnostic.nodeId?.let { session.selectedId = it } }.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    (if (diagnostic.severity == Severity.ERROR) "! " else "? ") + (label?.let { "<$it> " } ?: "") + (diagnostic.target?.let { "$it: " } ?: "") + diagnostic.message,
                    color = Color(if (diagnostic.severity == Severity.ERROR) palette.danger else palette.text),
                    fontSize = 13.sp,
                )
            }
        }
        if (problems.isNotEmpty()) {
            item { Text(stringResource(R.string.designer_preview_problems), color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp)) }
            items(problems) { problem ->
                Text(
                    problem,
                    color = Color(palette.muted),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = session.problemElementId != null) { session.problemElementId?.let { session.selectedId = it } }.padding(horizontal = 16.dp, vertical = 4.dp).testTag("designer-xml-problem"),
                )
            }
        }
    }
}
