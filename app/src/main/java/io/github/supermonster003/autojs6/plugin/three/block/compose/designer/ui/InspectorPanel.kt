package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.ScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HandlerSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierArg
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import org.autojs.plugin.compose.api.catalog.ModifierSpec
import org.autojs.plugin.compose.api.catalog.PropSpec
import org.autojs.plugin.compose.api.catalog.ValueType
import org.autojs.plugin.compose.api.model.ShapeKind
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.model.ValueKind

private val typographyNames = listOf("displayLarge", "displayMedium", "displaySmall", "headlineLarge", "headlineMedium", "headlineSmall",
    "titleLarge", "titleMedium", "titleSmall", "bodyLarge", "bodyMedium", "bodySmall", "labelLarge", "labelMedium", "labelSmall")

/** Parses what the user typed into a number-or-dimension field. */
private fun numberLiteral(text: String): JsLiteral? {
    val trimmed = text.trim()
    val value = trimmed.toDoubleOrNull() ?: return if (Regex("""-?\d+(\.\d+)?(dp|sp)""").matches(trimmed)) JsLiteral.Str(trimmed) else null
    return JsLiteral.Num(value, trimmed)
}

private fun defaultLiteral(type: ValueType, spec: PropSpec?): JsLiteral {
    spec?.defaultValue?.let { uiLiteral(it) }?.let { return it }
    val kind = type.kinds.firstOrNull { it != ValueKind.NULL } ?: ValueKind.STRING
    return when (kind) {
        ValueKind.BOOLEAN -> JsLiteral.Bool(false)
        ValueKind.NUMBER, ValueKind.DP, ValueKind.SP -> JsLiteral.Num(type.minimum?.takeIf { it > 0 } ?: 0.0)
        ValueKind.ENUM -> JsLiteral.Str(type.enumValues.firstOrNull() ?: "")
        ValueKind.COLOR -> JsLiteral.Str("#6750A4")
        ValueKind.SHAPE -> JsLiteral.Str("rounded")
        ValueKind.TEXT_STYLE -> JsLiteral.Str("bodyMedium")
        ValueKind.ICON_NAME -> JsLiteral.Str("Filled.Star")
        ValueKind.LIST -> JsLiteral.Arr(List(type.listMinimum.coerceAtLeast(1)) { JsLiteral.Num(0.0) })
        else -> JsLiteral.Str("")
    }
}

private fun uiLiteral(value: UiValue): JsLiteral? = when (value) {
    is UiValue.Str -> JsLiteral.Str(value.value)
    is UiValue.Num -> JsLiteral.Num(value.value)
    is UiValue.Bool -> JsLiteral.Bool(value.value)
    is UiValue.Dp -> JsLiteral.Num(value.value)
    is UiValue.Sp -> JsLiteral.Num(value.value)
    is UiValue.Enum -> JsLiteral.Str(value.value)
    is UiValue.IconName -> JsLiteral.Str(value.value)
    is UiValue.Color -> JsLiteral.Str("#%08X".format(value.argb))
    is UiValue.Shape -> if (value.shape == ShapeKind.ROUNDED && value.radius != 0.0) JsLiteral.Obj(listOf("shape" to JsLiteral.Str("rounded"), "radius" to JsLiteral.Num(value.radius))) else JsLiteral.Str(value.shape.name.lowercase())
    is UiValue.ListOf -> JsLiteral.Arr(value.values.mapNotNull { uiLiteral(it) })
    UiValue.Null -> JsLiteral.Null
    else -> null
}

@Composable
private fun SectionTitle(text: String, palette: StandalonePalette) {
    Text(text, color = Color(palette.muted), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
}

/**
 * The property drawer (roadmap D36 / D37): catalog-driven editors for the selected node's key, properties,
 * shortcut properties, slots, events and modifier chain. Opaque expressions stay editable as code and can
 * be switched back to a literal or bound to a declared state.
 */
@Composable
internal fun InspectorPanel(session: DesignerSession, palette: StandalonePalette, onPickSlotComponent: (String) -> Unit, modifier: Modifier = Modifier) {
    val node = session.selected
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).testTag("designer-inspector")) {
        if (node == null) {
            Text(stringResource(R.string.designer_select_node_hint), color = Color(palette.muted), modifier = Modifier.padding(vertical = 16.dp))
            return@Column
        }
        val catalog = session.catalog
        val spec = catalog.component(node.type)
        val states = session.document.states.map { it.name }
        val parentType = session.document.parentOf(node.id)?.let { session.document.node(it.parentId)?.type }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            Text(node.type, color = Color(palette.text), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("#${node.id}", color = Color(palette.muted), fontSize = 12.sp)
        }
        if (spec == null) {
            Text(stringResource(R.string.designer_unknown_component), color = Color(palette.danger), fontSize = 13.sp)
            return@Column
        }
        // Key
        SectionTitle(stringResource(R.string.designer_section_key), palette)
        val keyText = (node.key as? PropValue.Literal)?.value?.let { (it as? JsLiteral.Str)?.value ?: (it as? JsLiteral.Num)?.text } ?: (node.key as? PropValue.Expression)?.source ?: ""
        InspectorTextField(keyText, "key", palette, Modifier.testTag("designer-key"), onSeal = { session.seal() }) { text ->
            session.execute(EditCommand.SetKey(node.id, text.takeIf { it.isNotBlank() }?.let { PropValue.str(it) }))
        }
        // Properties
        SectionTitle(stringResource(R.string.designer_section_props), palette)
        spec.props.forEach { prop ->
            val entry = node.props.firstOrNull { it.name == prop.name || it.name in prop.aliases }
            ValueRow(
                label = prop.name, type = prop.type, value = entry?.value, states = states, palette = palette, defaultLiteral = { defaultLiteral(prop.type, prop) },
                onSeal = { session.seal() },
            ) { value ->
                val name = entry?.name ?: prop.name
                session.execute(EditCommand.SetProp(node.id, name, value))
            }
        }
        // Shortcuts
        SectionTitle(stringResource(R.string.designer_section_shortcuts), palette)
        catalog.shortcuts.forEach { shortcut ->
            val modifierSpec = catalog.modifier(shortcut.modifierName) ?: return@forEach
            val type = modifierSpec.signatures.first().args.first().type
            val entry = node.props.firstOrNull { it.name == shortcut.name }
            val disabled = modifierSpec.allowedParents.isNotEmpty() && parentType !in modifierSpec.allowedParents
            ValueRow(
                label = shortcut.name + if (disabled) " (${modifierSpec.allowedParents.joinToString("/")})" else "",
                type = type, value = entry?.value, states = states, palette = palette, listShortcut = shortcut.expandList,
                defaultLiteral = { defaultLiteral(type, null) }, onSeal = { session.seal() },
            ) { value -> session.execute(EditCommand.SetProp(node.id, shortcut.name, value)) }
        }
        // Slots
        if (spec.slots.isNotEmpty()) {
            SectionTitle(stringResource(R.string.designer_section_slots), palette)
            spec.slots.forEach { slot ->
                val current = node.slot(slot.name)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(slot.name + if (slot.required) " *" else "", color = Color(palette.text), fontSize = 14.sp)
                        val description = when (current) {
                            null -> "-"
                            is DesignChild.Node -> if (current.node.isPlainText) "\"${(current.node.prop("text") as PropValue.Literal).value.let { (it as JsLiteral.Str).value }}\"" else current.node.type
                            is DesignChild.Expression -> "{${current.source}}"
                        }
                        Text(description, color = Color(palette.muted), fontSize = 12.sp)
                    }
                    TextButton(onClick = { session.execute(EditCommand.SetSlot(node.id, slot.name, DesignChild.Node(DesignNode.text(0, "Text"))), merge = false) }) { Text(stringResource(R.string.designer_slot_set_text)) }
                    TextButton(onClick = { onPickSlotComponent(slot.name) }) { Text(stringResource(R.string.designer_slot_set_component)) }
                    if (current != null) TextButton(onClick = { session.execute(EditCommand.SetSlot(node.id, slot.name, null), merge = false) }) { Text(stringResource(R.string.designer_slot_clear)) }
                }
            }
        }
        // Events
        if (spec.events.isNotEmpty()) {
            SectionTitle(stringResource(R.string.designer_section_events), palette)
            spec.events.forEach { event ->
                val name = "on" + event.name.replaceFirstChar(Char::uppercaseChar)
                HandlerEditor(name, node.handler(name), palette, onSeal = { session.seal() }) { handler ->
                    session.execute(EditCommand.SetHandler(node.id, name, handler), merge = handler != null)
                }
            }
        }
        // Modifier chain
        SectionTitle(stringResource(R.string.designer_section_modifiers), palette)
        val chain = node.modifiers
        val opaqueModifier = node.entries.filterIsInstance<NodeEntry.ModifierExpression>().firstOrNull()
        if (opaqueModifier != null) {
            Text("modifier: ${opaqueModifier.source}", color = Color(palette.muted), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            TextButton(onClick = { session.execute(EditCommand.SetModifiers(node.id, emptyList()), merge = false) }) { Text(stringResource(R.string.designer_value_literal)) }
        } else {
            ModifierChainEditor(chain ?: emptyList(), catalog.modifiers, parentType, palette, onSeal = { session.seal() }) { updated ->
                session.execute(EditCommand.SetModifiers(node.id, updated.takeIf { it.isNotEmpty() }))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InspectorTextField(value: String, label: String, palette: StandalonePalette, modifier: Modifier = Modifier, monospace: Boolean = false, singleLine: Boolean = true, onSeal: () -> Unit, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        label = { Text(label, fontSize = 12.sp) },
        singleLine = singleLine,
        textStyle = TextStyle(fontSize = 13.sp, fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default, color = Color(palette.text)),
        modifier = modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) onSeal() },
    )
}

@Composable
private fun EnumDropdown(value: String, options: List<String>, palette: StandalonePalette, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { open = true }) { Text(value.ifEmpty { "-" }, color = Color(palette.text)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onPick(option) }) }
        }
    }
}

@Composable
private fun ValueRow(
    label: String,
    type: ValueType,
    value: PropValue?,
    states: List<String>,
    palette: StandalonePalette,
    listShortcut: Boolean = false,
    defaultLiteral: () -> JsLiteral,
    onSeal: () -> Unit,
    onChange: (PropValue?) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val literalLabel = stringResource(R.string.designer_value_literal)
    val stateLabel = stringResource(R.string.designer_value_state)
    val expressionLabel = stringResource(R.string.designer_value_expression)
    val clearLabel = stringResource(R.string.designer_value_none)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("designer-prop-$label")) {
        Text(label, color = Color(palette.text), fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Box(Modifier.weight(1f)) {
            when (value) {
                null -> Text("-", color = Color(palette.muted), modifier = Modifier.padding(8.dp))
                is PropValue.Literal -> LiteralEditor(type, value.value, palette, listShortcut, onSeal) { onChange(PropValue.Literal(it)) }
                is PropValue.StateRef -> EnumDropdown(value.name + ".value", states, palette) { onChange(PropValue.StateRef(it)) }
                is PropValue.Expression -> InspectorTextField(value.source, expressionLabel, palette, monospace = true, onSeal = onSeal) { onChange(PropValue.Expression(it)) }
            }
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.MoreVert, contentDescription = label, tint = Color(palette.muted)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(literalLabel) }, onClick = { menu = false; onChange(PropValue.Literal(defaultLiteral())) })
                if (states.isNotEmpty()) DropdownMenuItem(text = { Text(stateLabel) }, onClick = { menu = false; onChange(PropValue.StateRef(states.first())) })
                DropdownMenuItem(text = { Text(expressionLabel) }, onClick = { menu = false; onChange(PropValue.Expression(value?.let { ScriptWriter.value(it, "") } ?: "")) })
                if (value != null) DropdownMenuItem(text = { Text(clearLabel) }, onClick = { menu = false; onChange(null) })
            }
        }
    }
}

@Composable
private fun LiteralEditor(type: ValueType, literal: JsLiteral, palette: StandalonePalette, listShortcut: Boolean, onSeal: () -> Unit, onChange: (JsLiteral) -> Unit) {
    val kinds = type.kinds
    when {
        literal is JsLiteral.Bool || (ValueKind.BOOLEAN in kinds && literal is JsLiteral.Null) ->
            Switch(checked = (literal as? JsLiteral.Bool)?.value == true, onCheckedChange = { onChange(JsLiteral.Bool(it)) })
        ValueKind.ENUM in kinds && literal is JsLiteral.Str && type.enumValues.isNotEmpty() ->
            EnumDropdown(literal.value, type.enumValues, palette) { onChange(JsLiteral.Str(it)) }
        ValueKind.TEXT_STYLE in kinds && literal is JsLiteral.Str ->
            EnumDropdown(literal.value, typographyNames, palette) { onChange(JsLiteral.Str(it)) }
        ValueKind.SHAPE in kinds -> {
            val fields = (literal as? JsLiteral.Obj)?.fields?.toMap() ?: mapOf("shape" to literal)
            val shape = (fields["shape"] as? JsLiteral.Str)?.value ?: "rectangle"
            val radius = (fields["radius"] as? JsLiteral.Num)?.text ?: ""
            Row(verticalAlignment = Alignment.CenterVertically) {
                EnumDropdown(shape, listOf("rectangle", "rounded", "circle"), palette) { picked ->
                    onChange(if (picked == "rounded" && radius.isNotEmpty()) JsLiteral.Obj(listOf("shape" to JsLiteral.Str(picked), "radius" to JsLiteral.Num(radius.toDouble(), radius))) else JsLiteral.Str(picked))
                }
                if (shape == "rounded") InspectorTextField(radius, "radius", palette, Modifier.width(96.dp), onSeal = onSeal) { text ->
                    val number = text.trim().toDoubleOrNull()
                    onChange(if (number != null) JsLiteral.Obj(listOf("shape" to JsLiteral.Str("rounded"), "radius" to JsLiteral.Num(number, text.trim()))) else JsLiteral.Str("rounded"))
                }
            }
        }
        (ValueKind.LIST in kinds && (literal is JsLiteral.Arr || listShortcut)) -> {
            val text = (literal as? JsLiteral.Arr)?.items?.joinToString(", ") { ScriptWriter.literal(it) } ?: ScriptWriter.literal(literal)
            InspectorTextField(text, "list", palette, monospace = true, onSeal = onSeal) { typed ->
                val items = typed.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { numberLiteral(it) ?: JsLiteral.Str(it.trim('\'', '"')) }
                onChange(if (items.size == 1 && listShortcut) items[0] else JsLiteral.Arr(items))
            }
        }
        ValueKind.COLOR in kinds -> {
            val text = (literal as? JsLiteral.Str)?.value ?: (literal as? JsLiteral.Num)?.text ?: ""
            Row(verticalAlignment = Alignment.CenterVertically) {
                val parsed = Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$").matchEntire(text)?.let { java.lang.Long.parseLong(it.groupValues[1].padStart(8, 'F'), 16).toInt() }
                Box(Modifier.size(20.dp).background(parsed?.let { Color(it) } ?: Color.Transparent, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(8.dp))
                InspectorTextField(text, "color", palette, monospace = true, onSeal = onSeal) { onChange(JsLiteral.Str(it.trim())) }
            }
        }
        kinds.any { it == ValueKind.NUMBER || it == ValueKind.DP || it == ValueKind.SP } && literal !is JsLiteral.Str ->
            InspectorTextField((literal as? JsLiteral.Num)?.text ?: "", "number", palette, monospace = true, onSeal = onSeal) { typed -> numberLiteral(typed)?.let(onChange) }
        kinds.any { it == ValueKind.DP || it == ValueKind.SP || it == ValueKind.NUMBER } && literal is JsLiteral.Str ->
            InspectorTextField(literal.value, "value", palette, monospace = true, onSeal = onSeal) { typed -> onChange(numberLiteral(typed) ?: JsLiteral.Str(typed)) }
        else -> InspectorTextField((literal as? JsLiteral.Str)?.value ?: ScriptWriter.literal(literal), "text", palette, onSeal = onSeal) { onChange(JsLiteral.Str(it)) }
    }
}

@Composable
private fun HandlerEditor(name: String, handler: HandlerSource?, palette: StandalonePalette, onSeal: () -> Unit, onChange: (HandlerSource?) -> Unit) {
    var params by remember(name, handler == null) { mutableStateOf(handler?.params?.joinToString(", ") ?: "") }
    var body by remember(name, handler == null) { mutableStateOf(handler?.body ?: "") }
    var error by remember(name) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("designer-event-$name")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = Color(palette.text), fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (handler == null) TextButton(onClick = { onChange(HandlerSource(emptyList(), "// TODO")) }) { Text(stringResource(R.string.designer_add)) }
            else TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.designer_remove)) }
        }
        if (handler != null) {
            OutlinedTextField(value = params, onValueChange = { params = it }, label = { Text(stringResource(R.string.designer_handler_params), fontSize = 12.sp) }, singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color(palette.text)), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(stringResource(R.string.designer_handler_body), fontSize = 12.sp) }, minLines = 3,
                textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color(palette.text)), modifier = Modifier.fillMaxWidth().testTag("designer-handler-body"))
            error?.let { Text(it, color = Color(palette.danger), fontSize = 12.sp) }
            Row {
                TextButton(onClick = {
                    val parameterList = params.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    val problem = ScriptReader.checkHandler(parameterList, body)
                    error = problem?.let { "${it.line}:${it.column} ${it.reason}" }
                    if (problem == null) { onChange(HandlerSource(parameterList, body.trim(), handler.arrow, false)); onSeal() }
                }, modifier = Modifier.testTag("designer-handler-apply")) { Text(stringResource(R.string.designer_apply)) }
            }
        }
    }
}

@Composable
private fun ModifierChainEditor(chain: List<ModifierEntry>, specs: List<ModifierSpec>, parentType: String?, palette: StandalonePalette, onSeal: () -> Unit, onChange: (List<ModifierEntry>) -> Unit) {
    chain.forEachIndexed { index, operation ->
        val spec = specs.firstOrNull { it.name == operation.name }
        Column(Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("designer-modifier-$index")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(".${operation.name}", color = Color(palette.text), fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                TextButton(enabled = index > 0, onClick = { onChange(chain.toMutableList().apply { add(index - 1, removeAt(index)) }) }) { Text(stringResource(R.string.designer_move_up)) }
                TextButton(enabled = index < chain.lastIndex, onClick = { onChange(chain.toMutableList().apply { add(index + 1, removeAt(index)) }) }) { Text(stringResource(R.string.designer_move_down)) }
                TextButton(onClick = { onChange(chain.filterIndexed { i, _ -> i != index }) }) { Text(stringResource(R.string.designer_remove)) }
            }
            val signature = spec?.signatures?.firstOrNull { operation.args.size in it.requiredCount..it.args.size } ?: spec?.signatures?.firstOrNull()
            operation.args.forEachIndexed { argIndex, argument ->
                val argSpec = signature?.args?.getOrNull(argIndex)
                val text = when (argument) {
                    is ModifierArg.Value -> ScriptWriter.value(argument.value, "")
                    is ModifierArg.Callback -> "function"
                }
                InspectorTextField(text, argSpec?.name ?: "arg${argIndex + 1}", palette, monospace = true, onSeal = onSeal) { typed ->
                    val value = numberLiteral(typed)?.let { PropValue.Literal(it) }
                        ?: when (typed.trim()) { "true" -> PropValue.bool(true); "false" -> PropValue.bool(false); else -> null }
                        ?: if (typed.trim().startsWith("'") || typed.trim().startsWith("\"")) PropValue.str(typed.trim().trim('\'', '"')) else PropValue.Expression(typed.trim())
                    onChange(chain.toMutableList().apply { this[index] = operation.copy(args = operation.args.toMutableList().apply { this[argIndex] = ModifierArg.Value(value) }) })
                }
            }
        }
        HorizontalDivider(color = Color(palette.divider))
    }
    var adding by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { adding = true }, modifier = Modifier.testTag("designer-add-modifier")) { Text(stringResource(R.string.designer_add_modifier)) }
        DropdownMenu(expanded = adding, onDismissRequest = { adding = false }) {
            specs.filter { it.allowedParents.isEmpty() || parentType in it.allowedParents }.forEach { spec ->
                DropdownMenuItem(text = { Text(spec.name) }, onClick = {
                    adding = false
                    val signature = spec.signatures.first()
                    val args = signature.args.filter { it.defaultValue == null && !it.callback }.map { arg ->
                        ModifierArg.Value(PropValue.Literal(arg.defaultValue?.let(::uiLiteral) ?: defaultLiteral(arg.type, null)))
                    }
                    if (signature.args.any { it.callback && it.defaultValue == null }) return@DropdownMenuItem
                    onChange(chain + ModifierEntry(spec.name, args))
                })
            }
        }
    }
}
