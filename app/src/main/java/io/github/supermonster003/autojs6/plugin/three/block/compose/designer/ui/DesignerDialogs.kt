package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptText
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Diagnostic
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostKind
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostOptions
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity

/** The component palette (roadmap D36): the gallery's nine categories, every insertable node component. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComponentPalette(palette: StandalonePalette, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val insertable = remember { NodeTemplates.insertable().toSet() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(palette.surface)) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 16.dp).testTag("designer-palette")) {
            GalleryCatalog.categories.forEach { category ->
                val names = category.entries.map { it.component }.filter { it in insertable }
                if (names.isEmpty()) return@forEach
                item(key = "category-" + category.id) {
                    Text(stringResource(category.title), color = Color(palette.muted), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(names, key = { it }) { name ->
                    Text(
                        name,
                        color = Color(palette.text),
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(name) }.padding(vertical = 10.dp).testTag("designer-palette-$name"),
                    )
                    HorizontalDivider(color = Color(palette.divider))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Host kind and options (mount / floaty / dialog) as an editable key-value list. */
@Composable
internal fun HostOptionsDialog(session: DesignerSession, palette: StandalonePalette, onDismiss: () -> Unit) {
    val mount = session.document.mount ?: return
    var kind by remember { mutableStateOf(mount.kind) }
    val initial = (mount.options as? HostOptions.Fields)?.fields ?: emptyList()
    val opaque = mount.options as? HostOptions.Expression
    val fields = remember { initial.map { (name, value) -> name to valueText(value) }.toMutableList().let { androidx.compose.runtime.mutableStateListOf(*it.toTypedArray()) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.designer_host_options)) },
        text = {
            Column {
                Text(stringResource(R.string.designer_host_kind), color = Color(palette.muted), fontSize = 12.sp)
                Row {
                    HostKind.entries.forEach { candidate ->
                        FilterChip(selected = kind == candidate, onClick = { kind = candidate }, label = { Text("compose." + candidate.method) }, modifier = Modifier.padding(end = 6.dp).testTag("designer-host-" + candidate.method))
                    }
                }
                if (opaque != null) Text("{" + opaque.source + "}", fontSize = 12.sp, color = Color(palette.muted), style = ScriptText.code)
                fields.forEachIndexed { index, (name, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = name, onValueChange = { fields[index] = it to value }, label = { Text(stringResource(R.string.designer_host_option_name), fontSize = 11.sp) }, singleLine = true, textStyle = ScriptText.inline, modifier = Modifier.width(120.dp))
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(value = value, onValueChange = { fields[index] = name to it }, label = { Text(stringResource(R.string.designer_host_option_value), fontSize = 11.sp) }, singleLine = true, textStyle = ScriptText.inline, modifier = Modifier.weight(1f))
                        TextButton(onClick = { fields.removeAt(index) }) { Text(stringResource(R.string.designer_remove)) }
                    }
                }
                TextButton(onClick = { fields += "" to "" }) { Text(stringResource(R.string.designer_add_option)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val options = if (opaque != null && fields.isEmpty()) opaque else fields.filter { it.first.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { list ->
                    HostOptions.Fields(list.map { (name, text) -> name.trim() to parseValue(text) })
                }
                session.execute(EditCommand.SetHost(kind, options), merge = false)
                onDismiss()
            }, modifier = Modifier.testTag("designer-host-apply")) { Text(stringResource(R.string.designer_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.designer_cancel)) } },
    )
}

internal fun valueText(value: PropValue): String = io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.ScriptWriter.value(value, "")

/** Turns typed text into a literal when it is one (number, boolean, null, quoted string), otherwise an expression. */
internal fun parseValue(text: String): PropValue {
    val trimmed = text.trim()
    trimmed.toDoubleOrNull()?.let { return PropValue.Literal(JsLiteral.Num(it, trimmed)) }
    return when {
        trimmed == "true" -> PropValue.bool(true)
        trimmed == "false" -> PropValue.bool(false)
        trimmed == "null" -> PropValue.Literal(JsLiteral.Null)
        trimmed.length >= 2 && (trimmed.first() == '\'' && trimmed.last() == '\'' || trimmed.first() == '"' && trimmed.last() == '"') -> PropValue.str(trimmed.substring(1, trimmed.length - 1))
        else -> PropValue.Expression(trimmed)
    }
}

/** The validation report; tapping a row selects the node it is about. */
@Composable
internal fun IssuesDialog(diagnostics: List<Diagnostic>, problems: List<String>, palette: StandalonePalette, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.designer_issues)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp).testTag("designer-issues")) {
                if (diagnostics.isEmpty() && problems.isEmpty()) item { Text(stringResource(R.string.designer_no_issues), color = Color(palette.muted)) }
                items(diagnostics) { diagnostic ->
                    Column(Modifier.fillMaxWidth().clickable(enabled = diagnostic.nodeId != null) { diagnostic.nodeId?.let { onSelect(it); onDismiss() } }.padding(vertical = 6.dp)) {
                        Text(
                            (if (diagnostic.severity == Severity.ERROR) "! " else "? ") + (diagnostic.nodeId?.let { "#$it " } ?: "") + (diagnostic.target?.let { "$it: " } ?: "") + diagnostic.message,
                            color = Color(if (diagnostic.severity == Severity.ERROR) palette.danger else palette.text),
                            fontSize = 13.sp,
                            style = ScriptText.inline,
                        )
                    }
                }
                if (problems.isNotEmpty()) {
                    item { Text(stringResource(R.string.designer_preview_problems), color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                    items(problems) { problem -> Text(problem, color = Color(palette.muted), fontSize = 12.sp, style = ScriptText.inline) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.designer_apply)) } },
    )
}
