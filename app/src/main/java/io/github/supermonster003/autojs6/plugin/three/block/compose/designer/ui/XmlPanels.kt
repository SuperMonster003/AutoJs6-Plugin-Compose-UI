package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.FragmentUsage
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifference
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifferenceKind
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifferences
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.UiPreviewClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.elementLabel

/** One row of the XML outline: an element with its depth, label and a short summary of its text. */
internal data class XmlOutlineRow(val id: Int, val depth: Int, val label: String, val detail: String, val hasChildren: Boolean)

internal fun xmlOutlineRows(root: XmlNode.Element?, collapsed: Set<Int> = emptySet()): List<XmlOutlineRow> {
    val rows = ArrayList<XmlOutlineRow>()
    fun visit(element: XmlNode.Element, depth: Int) {
        val text = element.attr("text")?.value ?: element.textContent?.trim()?.takeIf { it.isNotEmpty() }
        val children = element.elementChildren
        rows += XmlOutlineRow(element.id, depth, elementLabel(element), text?.let { "\"$it\"" } ?: "", children.isNotEmpty())
        if (element.id in collapsed) return
        children.forEach { visit(it, depth + 1) }
    }
    root?.let { visit(it, 0) }
    return rows
}

private enum class XmlDropZone { BEFORE, INTO, AFTER }

/**
 * The element tree of the fragment on the canvas (roadmap P10.3 / P10.4): a fragment switcher when the script has
 * several XML literals, rows in document order with collapse toggles, tap selection, up / down / delete on the
 * selected row, and long-press drag onto another row to reorder (top or bottom quarter) or reparent (middle, when
 * the target takes children).
 */
@Composable
internal fun XmlOutlinePanel(session: UiDesignerSession, palette: StandalonePalette, modifier: Modifier = Modifier) {
    val collapsed = remember { mutableStateMapOf<Int, Boolean>() }
    val rows = remember(session.document, session.fragment, collapsed.size) { xmlOutlineRows(session.root, collapsed.keys) }
    val rowBounds = remember { mutableStateMapOf<Int, Rect>() }
    var dropIndicator by remember { mutableStateOf<Pair<Int, XmlDropZone>?>(null) }
    val accent = Color(palette.accent)
    val rootId = session.root?.id
    Column(modifier.fillMaxSize()) {
        val fragments = session.document.fragments
        if (fragments.size > 1) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp).testTag("designer-xml-fragments")) {
                fragments.forEachIndexed { index, fragment ->
                    val label = if (fragment.usage == FragmentUsage.LAYOUT) stringResource(R.string.designer_ui_fragment_root, index + 1) else stringResource(R.string.designer_ui_fragment, index + 1)
                    FilterChip(
                        selected = session.fragment == index,
                        onClick = { session.fragment = index; session.selectedId = fragment.root.id },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 6.dp).testTag("designer-xml-fragment-$index"),
                    )
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize().testTag("designer-xml-outline"), contentPadding = PaddingValues(bottom = AddButtonClearance)) {
            items(rows, key = { it.id }) { row ->
                val selected = row.id == session.selectedId
                val problem = row.id == session.problemElementId
                val indicator = dropIndicator?.takeIf { it.first == row.id }
                val background = when {
                    indicator?.second == XmlDropZone.INTO -> accent.copy(alpha = 0.22f)
                    selected -> accent.copy(alpha = 0.12f)
                    else -> Color.Transparent
                }
                Box(Modifier.fillMaxWidth().onGloballyPositioned { coords -> rowBounds[row.id] = coords.boundsInRoot() }) {
                    if (indicator?.second == XmlDropZone.BEFORE) Box(Modifier.fillMaxWidth().height(2.dp).background(accent).align(Alignment.TopCenter))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(background)
                            .testTag("designer-xml-outline-${row.id}")
                            .clickable { session.selectedId = row.id }
                            .pointerInput(row.id, session.revision) {
                                if (row.id == rootId) return@pointerInput
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { session.draggingId = row.id; session.selectedId = row.id },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val origin = rowBounds[row.id]?.topLeft ?: Offset.Zero
                                        val point = origin + change.position
                                        val target = rowBounds.entries.firstOrNull { (targetId, rect) -> targetId != row.id && rect.contains(point) }
                                        dropIndicator = target?.let { (targetId, rect) ->
                                            val zone = when {
                                                point.y < rect.top + rect.height * 0.25f -> XmlDropZone.BEFORE
                                                point.y > rect.bottom - rect.height * 0.25f -> XmlDropZone.AFTER
                                                else -> XmlDropZone.INTO
                                            }
                                            val resolved = if (zone == XmlDropZone.INTO && !session.canDropInto(row.id, targetId)) XmlDropZone.AFTER else zone
                                            targetId to resolved
                                        }
                                    },
                                    onDragEnd = {
                                        val target = dropIndicator
                                        dropIndicator = null
                                        session.draggingId = null
                                        if (target != null) applyXmlDrop(session, row.id, target.first, target.second)
                                    },
                                    onDragCancel = { dropIndicator = null; session.draggingId = null },
                                )
                            }
                            .padding(start = (8 + row.depth * 16).dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    ) {
                        if (row.hasChildren) {
                            val isCollapsed = row.id in collapsed
                            Text(
                                if (isCollapsed) "+" else "-",
                                color = Color(palette.muted),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(18.dp).clickable { if (isCollapsed) collapsed.remove(row.id) else collapsed[row.id] = true }.testTag("designer-xml-collapse-${row.id}"),
                            )
                        } else Spacer(Modifier.width(18.dp))
                        Text(
                            row.label,
                            color = Color(if (problem) palette.danger else palette.text),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (row.detail.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Text(row.detail, color = Color(palette.muted), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        }
                        Spacer(Modifier.weight(1f))
                        if (selected && row.id != rootId) {
                            IconButton(onClick = { session.moveSelected(-1) }, modifier = Modifier.size(32.dp).testTag("designer-xml-move-up")) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.designer_move_up), tint = Color(palette.muted))
                            }
                            IconButton(onClick = { session.moveSelected(1) }, modifier = Modifier.size(32.dp).testTag("designer-xml-move-down")) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.designer_move_down), tint = Color(palette.muted))
                            }
                            IconButton(onClick = { session.deleteSelected() }, modifier = Modifier.size(32.dp).testTag("designer-xml-delete")) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.designer_delete_node), tint = Color(palette.danger))
                            }
                        }
                    }
                    if (indicator?.second == XmlDropZone.AFTER) Box(Modifier.fillMaxWidth().height(2.dp).background(accent).align(Alignment.BottomCenter))
                }
            }
        }
    }
}

private fun applyXmlDrop(session: UiDesignerSession, dragged: Int, targetId: Int, zone: XmlDropZone) {
    val moved = when (zone) {
        XmlDropZone.INTO -> session.moveInto(dragged, targetId)
        XmlDropZone.BEFORE, XmlDropZone.AFTER -> {
            val placement = session.document.parentOf(targetId) ?: return
            val parent = session.document.element(placement.parentId) ?: return
            val siblings = parent.elementChildren
            var index = siblings.indexOfFirst { it.id == targetId } + if (zone == XmlDropZone.AFTER) 1 else 0
            val draggedIndex = siblings.indexOfFirst { it.id == dragged }
            if (draggedIndex in 0 until index) index--
            session.moveTo(dragged, parent.id, index)
        }
    }
    if (moved) session.selectedId = dragged
}

/**
 * The validator's diagnostics and the preview's problems, then the script's preview differences (roadmap P11.2 /
 * D55: informational, by line, not counted as problems); a tap on an element's entry selects it, a tap on a
 * difference opens the documentation. [scriptRunning]: the interactive preview runs the script now.
 */
@Composable
internal fun XmlProblemsPanel(session: UiDesignerSession, palette: StandalonePalette, scriptRunning: Boolean, modifier: Modifier = Modifier) {
    val diagnostics = session.diagnostics
    val problems = session.previewProblems
    val differences = session.visibleDifferences
    val context = LocalContext.current
    val docsUnavailable = stringResource(R.string.designer_ui_diff_docs_unavailable)
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
        if (differences.isNotEmpty()) {
            item {
                Text(
                    stringResource(if (scriptRunning) R.string.designer_ui_diff_header_running else R.string.designer_ui_diff_header_layout),
                    color = Color(palette.muted),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).testTag("designer-ui-diff-header"),
                )
            }
            items(differences) { difference ->
                Column(
                    Modifier.fillMaxWidth().clickable {
                        val opened = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PreviewDifferences.DOCS_URL)).addCategory(Intent.CATEGORY_BROWSABLE)) }.isSuccess
                        if (!opened) Toast.makeText(context, docsUnavailable, Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 16.dp, vertical = 6.dp).testTag("designer-ui-diff"),
                ) {
                    Text(
                        "i " + stringResource(R.string.designer_ui_diff_line, difference.line) + "  " + difference.api,
                        color = Color(palette.muted),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(differenceText(difference), color = Color(palette.text), fontSize = 13.sp)
                }
            }
        }
    }
}

/** The sentence that explains [difference] (one per kind; the window functions name themselves). */
@Composable
private fun differenceText(difference: PreviewDifference): String = when (difference.kind) {
    PreviewDifferenceKind.ACTIVITY -> stringResource(R.string.designer_ui_diff_activity)
    PreviewDifferenceKind.LIFECYCLE -> stringResource(R.string.designer_ui_diff_lifecycle)
    PreviewDifferenceKind.ACTIVITY_EVENTS -> stringResource(R.string.designer_ui_diff_activity_events)
    PreviewDifferenceKind.WINDOW_COLOR -> stringResource(R.string.designer_ui_diff_window_color, difference.api)
    PreviewDifferenceKind.WINDOW_NO_OP -> stringResource(R.string.designer_ui_diff_window_no_op, difference.api)
    PreviewDifferenceKind.DIALOGS -> stringResource(R.string.designer_ui_diff_dialogs)
    PreviewDifferenceKind.BUSY_WAIT -> stringResource(R.string.designer_ui_diff_busy_wait)
    PreviewDifferenceKind.THREADS -> stringResource(R.string.designer_ui_diff_threads)
}

/**
 * The bindings panel (roadmap P10.4, the ui counterpart of the state panel): every `{{ }}` name the fragment on the
 * canvas uses, the value the preview evaluated for it from the script's top-level declarations, and an override
 * (JSON) that only the canvas sees. Nothing here edits the document.
 */
@Composable
internal fun XmlBindingsPanel(session: UiDesignerSession, palette: StandalonePalette, modifier: Modifier = Modifier) {
    val names = session.bindingNames
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = AddButtonClearance).testTag("designer-ui-bindings")) {
        Text(stringResource(R.string.designer_ui_bindings_hint), color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        if (names.isEmpty()) {
            Text(stringResource(R.string.designer_ui_bindings_empty), color = Color(palette.muted), modifier = Modifier.padding(vertical = 8.dp).testTag("designer-ui-bindings-empty"))
            return@Column
        }
        names.forEach { name ->
            HorizontalDivider()
            val declared = session.previewDeclarations[name]
            val failure = session.declarationFailures[name]
            val override = session.bindingOverrides[name]
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("designer-ui-binding-$name")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = Color(palette.text), fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    if (override != null) TextButton(onClick = { session.setBindingOverride(name, null) }, modifier = Modifier.testTag("designer-ui-binding-reset-$name")) {
                        Text(stringResource(R.string.designer_state_reset))
                    }
                }
                val status = when {
                    declared != null -> stringResource(R.string.designer_state_initial, declared)
                    failure != null -> failure
                    else -> stringResource(R.string.designer_ui_binding_undeclared)
                }
                Text(status, color = Color(palette.muted), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                var text by remember(name, override == null) { mutableStateOf(override ?: declared ?: "") }
                var invalid by remember(name) { mutableStateOf(false) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { typed ->
                        text = typed
                        val trimmed = typed.trim()
                        val valid = trimmed.isNotEmpty() && runCatching { JsonText.parse(trimmed) }.isSuccess
                        invalid = trimmed.isNotEmpty() && !valid
                        if (valid) session.setBindingOverride(name, trimmed)
                    },
                    label = { Text(stringResource(R.string.designer_ui_binding_value), fontSize = 12.sp) },
                    isError = invalid,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color(palette.text)),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("designer-ui-binding-value-$name"),
                )
                if (invalid) Text(stringResource(R.string.designer_ui_binding_invalid), color = Color(palette.danger), fontSize = 12.sp, modifier = Modifier.testTag("designer-ui-binding-invalid-$name"))
            }
        }
        if (session.bindingOverrides.keys.any { it in names }) {
            TextButton(onClick = { session.bindingOverrides.keys.toList().forEach { session.setBindingOverride(it, null) } }, modifier = Modifier.testTag("designer-ui-bindings-reset-all")) {
                Text(stringResource(R.string.designer_state_reset_all))
            }
        }
    }
}

/**
 * The console tab of a ui project in 1.6.0: the layout preview runs no script, so this shows the preview's status
 * and what the host reported while drawing (failures with their codes and positions, diagnostics by attribute).
 */
@Composable
internal fun XmlConsolePanel(client: UiPreviewClient, statusText: String?, interactive: Boolean, palette: StandalonePalette, modifier: Modifier = Modifier) {
    val failure = client.failure
    val diagnostics = client.diagnostics
    // Roadmap P10.6: while the interactive preview runs the script, the tab is that script's console.
    val lines = client.consoleLines
    val list = rememberLazyListState()
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) list.scrollToItem(lines.size - 1) }
    Column(modifier.fillMaxSize().testTag("designer-ui-console")) {
        Text(stringResource(if (interactive) R.string.designer_ui_console_run_hint else R.string.designer_ui_console_hint), color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
        if (interactive) {
            val scriptError = client.scriptError
            val lastExit = client.lastExit
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val status = when {
                    scriptError != null -> stringResource(R.string.designer_sandbox_error, scriptError)
                    client.running && client.runId == null -> stringResource(R.string.designer_ui_interact_starting)
                    client.running -> stringResource(R.string.designer_ui_interact_running)
                    lastExit != null -> stringResource(R.string.designer_ui_interact_exited, lastExit)
                    else -> statusText ?: ""
                }
                Text(status, color = Color(if (scriptError != null) palette.danger else palette.muted), fontSize = 12.sp, modifier = Modifier.weight(1f).padding(vertical = 4.dp).testTag("designer-ui-console-status"))
                if (lines.isNotEmpty()) TextButton(onClick = { client.clearConsole() }, modifier = Modifier.testTag("designer-ui-console-clear")) {
                    Text(stringResource(R.string.designer_console_clear))
                }
            }
            SelectionContainer {
                LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = AddButtonClearance)) {
                    if (lines.isEmpty()) item {
                        Text(stringResource(R.string.designer_ui_console_run_empty), color = Color(palette.muted), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    items(lines) { line ->
                        val color = when {
                            line.level >= Log.ERROR -> palette.danger
                            line.level == Log.WARN -> palette.accent
                            else -> palette.text
                        }
                        Text(line.text, color = Color(color), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 2.dp).testTag("designer-ui-console-line"))
                    }
                }
            }
            return@Column
        }
        val status = when (val current = client.status) {
            is UiPreviewClient.Status.Ready -> "AutoJs6 " + (current.hostVersionName ?: current.hostVersionCode.toString()) + (statusText?.let { " - $it" } ?: "")
            else -> statusText ?: current.javaClass.simpleName
        }
        Text(status, color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("designer-ui-console-status"))
        SelectionContainer {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = AddButtonClearance)) {
                if (failure == null && diagnostics.isEmpty()) {
                    Text(stringResource(R.string.designer_ui_console_empty), color = Color(palette.muted), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                failure?.let {
                    val position = if (it.line > 0) " (${it.line}:${it.column})" else ""
                    Text("[${it.code}] ${it.message}$position", color = Color(palette.danger), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 2.dp).testTag("designer-ui-console-line"))
                }
                diagnostics.forEach { diagnostic ->
                    Text("[${diagnostic.code}] " + (diagnostic.attrName?.let { "$it: " } ?: "") + diagnostic.message, color = Color(palette.text), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 2.dp).testTag("designer-ui-console-line"))
                }
            }
        }
    }
}
