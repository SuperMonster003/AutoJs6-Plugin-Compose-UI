package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptText
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.ScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.StateDecl
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader

/**
 * The preview state panel (roadmap P9.3 step 1, D43): every top-level `compose.state` with its initial value
 * from the script and a preview value that only the canvas uses. Nothing here edits the document, so no undo
 * step, draft or save follows from it; preview values are dropped when the document is reloaded.
 */
@Composable
internal fun StatePanel(session: DesignerSession, palette: StandalonePalette, modifier: Modifier = Modifier) {
    val states = session.document.states
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = AddButtonClearance).testTag("designer-state")) {
        Text(stringResource(R.string.designer_state_hint), color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        if (states.isEmpty()) {
            Text(stringResource(R.string.designer_state_empty), color = Color(palette.muted), modifier = Modifier.padding(vertical = 8.dp))
            return@Column
        }
        states.forEach { state ->
            HorizontalDivider()
            StateRow(state, session.previewValues[state.name], palette, onChange = { session.setPreviewValue(state.name, it) })
        }
        if (session.previewValues.keys.any { name -> states.any { it.name == name } }) {
            TextButton(onClick = { session.resetPreviewValues() }, modifier = Modifier.testTag("designer-state-reset-all")) {
                Text(stringResource(R.string.designer_state_reset_all))
            }
        }
    }
}

@Composable
private fun StateRow(state: StateDecl, preview: JsLiteral?, palette: StandalonePalette, onChange: (JsLiteral?) -> Unit) {
    val initial = (state.initial as? PropValue.Literal)?.value
    val current = preview ?: initial
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("designer-state-${state.name}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.name, color = Color(palette.text), fontWeight = FontWeight.SemiBold, style = ScriptText.code, modifier = Modifier.weight(1f))
            if (preview != null) TextButton(onClick = { onChange(null) }, modifier = Modifier.testTag("designer-state-reset-${state.name}")) {
                Text(stringResource(R.string.designer_state_reset))
            }
        }
        val initialText = when (val value = state.initial) {
            is PropValue.Literal -> ScriptWriter.literal(value.value)
            is PropValue.Expression -> value.source
            is PropValue.StateRef -> value.name + ".value"
        }
        Text(stringResource(R.string.designer_state_initial, ScriptText.isolate(initialText)), color = Color(palette.muted), fontSize = 12.sp, style = ScriptText.output)
        val field = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("designer-state-value-${state.name}")
        when (current) {
            is JsLiteral.Bool -> Switch(checked = current.value, onCheckedChange = { onChange(JsLiteral.Bool(it)) }, modifier = Modifier.testTag("designer-state-value-${state.name}"))
            is JsLiteral.Str -> ValueField(current.value, palette, field, monospace = false) { onChange(JsLiteral.Str(it)) }
            is JsLiteral.Num -> ValueField(current.text, palette, field, monospace = true) { typed ->
                typed.trim().toDoubleOrNull()?.let { onChange(JsLiteral.Num(it, typed.trim())) }
            }
            // Lists, objects, null and values the script computes: typed as a JavaScript literal.
            else -> ValueField(current?.let { ScriptWriter.literal(it) } ?: "", palette, field, monospace = true) { typed -> parseLiteral(typed)?.let(onChange) }
        }
    }
}

@Composable
private fun ValueField(value: String, palette: StandalonePalette, modifier: Modifier, monospace: Boolean, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        label = { Text(stringResource(R.string.designer_state_value), fontSize = 12.sp) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 13.sp, color = Color(palette.text)).merge(if (monospace) ScriptText.Code else null),
        modifier = modifier,
    )
}

/** A JavaScript literal typed by the user, read with the designer's own parser; null while it is not one. */
private fun parseLiteral(text: String): JsLiteral? {
    if (text.isBlank()) return null
    return try {
        val document = ScriptReader().read("var value = compose.state($text);\ncompose.mount(compose.Column({}));\n")
        (document.states.singleOrNull()?.initial as? PropValue.Literal)?.value
    } catch (error: DesignerParseError) {
        null
    }
}
