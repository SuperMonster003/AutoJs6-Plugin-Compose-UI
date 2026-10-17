package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptText
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.AttrPart
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiAndroidCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEvents
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlEntities
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.idName
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.elementLabel

/** One attribute group of the inspector: the attributes of one class of the element's chain, nearest class first. */
internal data class UiAttrGroup(val owner: String, val attrs: List<UiCatalog.Attr>)

/**
 * The catalog attributes of [element] that are not set on it, grouped by the class that declares them (nearest
 * first); set attributes are listed separately in document order. Registered widgets and unknown tags have no groups.
 */
internal fun uiAttrGroups(catalog: UiCatalog, viewClass: String?, element: XmlNode.Element): List<UiAttrGroup> {
    if (viewClass == null) return emptyList()
    val set = element.attrs.map { catalog.shorthands[it.name] ?: it.name }.toSet()
    return catalog.attrsFor(viewClass).filter { it.name !in set && it.aliases.none { alias -> alias in set } }
        .groupBy { it.ownerClass }.map { (owner, attrs) -> UiAttrGroup(owner, attrs) }
        .sortedBy { group -> catalog.chain(viewClass).indexOf(group.owner).let { if (it < 0) Int.MAX_VALUE else it } }
}

/**
 * The attributes the native catalog has for [element] (roadmap P11.3) that are not set on it and that the host sets:
 * those of its view class chain by class (nearest first), then its parent's layout parameters as one group.
 */
internal fun nativeAttrGroups(session: UiDesignerSession, element: XmlNode.Element): List<UiAttrGroup> {
    val android = session.android
    val set = element.attrs.map { it.name }.toSet() + "android:id"
    val className = session.nativeClassOf(element)
    val own = className?.let(android::attrsFor).orEmpty().filter { it.state == UiAndroidCatalog.State.SET && it.name !in set }
        .groupBy { it.ownerClass }.map { (owner, attrs) -> UiAttrGroup(UiAndroidCatalog.shortName(owner), attrs.map(android::toUiAttr)) }
    val parent = session.nativeParentClassOf(element)
    val params = android.layoutParamsFor(parent).filter { it.state == UiAndroidCatalog.State.SET && it.name !in set }
    val layout = if (params.isEmpty()) emptyList() else listOf(UiAttrGroup(UiAndroidCatalog.shortName(parent ?: "android.view.ViewGroup") + " layout", params.map(android::toUiAttr)))
    return layout + own
}

/**
 * The property drawer of a ui (XML) element (roadmap P10.4): tag (any palette tag), `id`, text content, the
 * attributes already set, the catalog's other attributes by class, a free attribute name, and the events the
 * script can handle (roadmap D52: a stub is appended to the script, not edited here). Values have three states:
 * literal text, a `{{ }}` binding (a declared name or any expression), or an E4X `{expr}` the script computes,
 * which is shown as source and only replaced as a whole.
 */
@Composable
internal fun UiInspectorPanel(session: UiDesignerSession, palette: StandalonePalette, modifier: Modifier = Modifier) {
    val element = session.selected
    val catalog = session.catalog
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = AddButtonClearance).testTag("designer-ui-inspector")) {
        if (element == null) {
            Text(stringResource(R.string.designer_select_node_hint), color = Color(palette.muted), modifier = Modifier.padding(vertical = 16.dp))
            return@Column
        }
        val tag = catalog.tag(element.tag)
        val widget = element.tag in session.document.widgets
        val viewClass = tag?.viewClass ?: session.viewClassOf(element.tag)
        val namespaced = element.attrs.any { it.name.startsWith("xmlns") || ':' in it.name }
        // Roadmap P11.3: an element of a native Android XML fragment is edited with the native catalog.
        val native = session.document.isNative(element.id)
        // Tag
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            if (native) {
                Text("<${element.tag}>", color = Color(palette.text), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, style = ScriptText.code, modifier = Modifier.weight(1f).padding(vertical = 8.dp).testTag("designer-ui-tag"))
            } else {
                TagPicker(element.tag, catalog, palette, Modifier.weight(1f)) { picked -> session.execute(UiEditCommand.SetTag(element.id, picked), merge = false) }
            }
            Text(elementLabel(element), color = Color(palette.muted), fontSize = 12.sp, style = ScriptText.code)
        }
        if (!native && tag == null && !widget && viewClass == null) {
            Text(stringResource(R.string.designer_ui_unknown_tag), color = Color(palette.danger), fontSize = 13.sp)
        }
        // id: AutoJs6's id="x", or android:id="@+id/x" in native XML
        UiSectionTitle(if (native) "android:id" else "id", palette)
        val idText = if (native) element.idName().orEmpty() else element.attr("id")?.let { XmlEntities.decode(it.value) } ?: ""
        UiTextField(idText, "id", palette, Modifier.testTag("designer-ui-id"), monospace = true, onSeal = { session.seal() }) { text ->
            val id = text.trim().takeIf { it.isNotEmpty() }
            if (native) {
                session.execute(UiEditCommand.SetAttr(element.id, "android:id", id?.let { XmlEntities.encodeAttribute("@+id/" + it.removePrefix("@+id/").removePrefix("@id/"), '"') }))
            } else {
                session.execute(UiEditCommand.SetAttr(element.id, "id", id?.let { XmlEntities.encodeAttribute(it, '"') }))
            }
        }
        // Text content: elements without element children (`<text>Hello</text>`), unless they are empty containers.
        val textContent = element.textContent
        if (textContent != null && (tag?.container != true || textContent.isNotBlank())) {
            UiSectionTitle(stringResource(R.string.designer_ui_text_content), palette)
            UiTextField(XmlEntities.decode(textContent), stringResource(R.string.designer_ui_value_text), palette, Modifier.testTag("designer-ui-text"), singleLine = false, onSeal = { session.seal() }) { text ->
                session.execute(UiEditCommand.SetText(element.id, XmlEntities.encodeText(text)))
            }
        }
        // Set attributes
        UiSectionTitle(stringResource(R.string.designer_ui_attr_set), palette)
        val setAttrs = element.attrs.filter { it.name != "id" && !(native && it.name == "android:id") }
        if (setAttrs.isEmpty()) Text(stringResource(R.string.designer_ui_attr_none), color = Color(palette.muted), fontSize = 12.sp)
        setAttrs.forEach { attr ->
            val spec = if (native) session.nativeAttr(element, attr.name) else viewClass?.let { catalog.attr(it, attr.name) }
            AttrRow(session, element, attr.name, attr, spec, catalog, palette)
        }
        // Catalog attributes by class
        if (native || (!namespaced && !widget)) {
            val expanded = remember(element.id, element.tag) { mutableStateMapOf<String, Boolean>() }
            val groups = if (native) nativeAttrGroups(session, element) else uiAttrGroups(catalog, viewClass, element)
            groups.forEachIndexed { index, group ->
                val open = expanded[group.owner] ?: (index == 0)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { expanded[group.owner] = !open }.padding(top = 14.dp, bottom = 4.dp).testTag("designer-ui-group-${group.owner}"),
                ) {
                    Text(if (open) "-" else "+", color = Color(palette.muted), style = ScriptText.code, modifier = Modifier.width(18.dp))
                    Text(group.owner, color = Color(palette.muted), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(" (${group.attrs.size})", color = Color(palette.muted), fontSize = 12.sp)
                }
                if (open) group.attrs.forEach { spec -> AttrRow(session, element, spec.name, null, spec, catalog, palette) }
            }
        }
        // Any attribute by name (the validator warns about unknown ones)
        UiSectionTitle(stringResource(R.string.designer_ui_attr_add), palette)
        var newName by remember(element.id) { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName, onValueChange = { newName = it }, label = { Text(stringResource(R.string.designer_ui_attr_name), fontSize = 12.sp) }, singleLine = true,
                textStyle = ScriptText.Code.merge(TextStyle(fontSize = 13.sp, color = Color(palette.text))), modifier = Modifier.weight(1f).testTag("designer-ui-attr-name"),
            )
            TextButton(onClick = {
                val name = newName.trim()
                if (name.isEmpty() || element.attr(name) != null) return@TextButton
                val spec = if (native) session.nativeAttr(element, name) else viewClass?.let { catalog.attr(it, name) }
                session.execute(UiEditCommand.SetAttr(element.id, name, defaultValue(spec)), merge = false)
                newName = ""
            }, modifier = Modifier.testTag("designer-ui-attr-add")) { Text(stringResource(R.string.designer_add)) }
        }
        // Events (roadmap D52, option (b))
        UiSectionTitle(stringResource(R.string.designer_section_events), palette)
        val canAdd = session.canAddHandlers(element)
        Text(stringResource(if (canAdd) R.string.designer_ui_event_hint else R.string.designer_ui_event_no_layout), color = Color(palette.muted), fontSize = 12.sp)
        val handled = session.handledEvents(element)
        UiEvents.forTag(catalog, tag).forEach { event ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("designer-ui-event-${event.name}")) {
                Text(event.name, color = Color(palette.text), fontSize = 13.sp, style = ScriptText.code, modifier = Modifier.weight(1f))
                if (event.name in handled) Text(stringResource(R.string.designer_ui_event_exists), color = Color(palette.muted), fontSize = 12.sp)
                else TextButton(onClick = { session.addHandler(element, event) }, enabled = canAdd || (session.document.dialect == Dialect.TSX && event.name == "click"), modifier = Modifier.testTag("designer-ui-event-add-${event.name}")) { Text(stringResource(R.string.designer_add)) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun defaultValue(spec: UiCatalog.Attr?): String = when (spec?.type) {
    "boolean" -> "true"
    "enum" -> spec.values.firstOrNull().orEmpty()
    "int", "float" -> "0"
    "dimension" -> "16"
    "color" -> "#6750A4"
    "gravity" -> "center"
    else -> ""
}

@Composable
private fun UiSectionTitle(text: String, palette: StandalonePalette) {
    Text(text, color = Color(palette.muted), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
}

@Composable
private fun TagPicker(tag: String, catalog: UiCatalog, palette: StandalonePalette, modifier: Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val tags = remember(catalog) { UiTemplates.insertable(catalog).sorted() }
    Box(modifier) {
        TextButton(onClick = { open = true }, modifier = Modifier.testTag("designer-ui-tag")) {
            Text("<$tag>", color = Color(palette.text), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, style = ScriptText.code)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            tags.forEach { candidate ->
                DropdownMenuItem(text = { Text(candidate, style = ScriptText.code) }, onClick = { open = false; if (candidate != tag) onPick(candidate) }, modifier = Modifier.testTag("designer-ui-tag-$candidate"))
            }
        }
    }
}

@Composable
internal fun UiTextField(
    value: String,
    label: String,
    palette: StandalonePalette,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    singleLine: Boolean = true,
    onSeal: () -> Unit,
    onChange: (String) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        label = { Text(label, fontSize = 12.sp) },
        singleLine = singleLine,
        textStyle = TextStyle(fontSize = 13.sp, color = Color(palette.text)).merge(if (monospace) ScriptText.Code else null),
        modifier = modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) onSeal() },
    )
}

@Composable
private fun EnumPicker(value: String, options: List<String>, palette: StandalonePalette, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { open = true }) { Text(value.ifEmpty { "-" }, color = Color(palette.text), fontSize = 13.sp, style = ScriptText.code) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onPick(option) }) }
        }
    }
}

/**
 * One attribute: its name (with the full name of a shorthand), the editor for its current state, and a menu to
 * switch state (text / `{{ }}` binding / bare) or remove it.
 */
@Composable
private fun AttrRow(session: UiDesignerSession, element: XmlNode.Element, name: String, attr: XmlAttr?, spec: UiCatalog.Attr?, catalog: UiCatalog, palette: StandalonePalette) {
    var menu by remember { mutableStateOf(false) }
    val full = catalog.shorthands[name]
    fun set(raw: String?, bare: Boolean = false, merge: Boolean = true) = session.execute(UiEditCommand.SetAttr(element.id, name, raw, bare), merge)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("designer-ui-attr-$name")) {
        Column(Modifier.width(110.dp)) {
            Text(name, color = Color(palette.text), fontSize = 13.sp, style = ScriptText.code)
            val hint = full ?: spec?.typeText?.takeIf { it.isNotBlank() }
            if (hint != null) Text(hint, color = Color(palette.muted), fontSize = 10.sp, maxLines = 1)
        }
        Box(Modifier.weight(1f)) {
            when {
                attr == null -> Text("-", color = Color(palette.muted), modifier = Modifier.padding(8.dp))
                attr.isBare -> Text(stringResource(R.string.designer_ui_value_bare), color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                attr.hasExpr -> Column {
                    Text(attr.value, color = Color(palette.muted), fontSize = 12.sp, style = ScriptText.code, modifier = Modifier.testTag("designer-ui-attr-expr-$name"))
                    Text(stringResource(R.string.designer_ui_value_expr), color = Color(palette.muted), fontSize = 10.sp)
                }
                attr.hasBinding -> BindingEditor(attr, session.declaredNames, palette, onSeal = { session.seal() }) { set(it) }
                spec?.type == "boolean" -> Switch(checked = XmlEntities.decode(attr.value).trim() == "true", onCheckedChange = { set(if (it) "true" else "false", merge = false) }, modifier = Modifier.testTag("designer-ui-attr-switch-$name"))
                spec?.type == "enum" && spec.values.isNotEmpty() -> EnumPicker(XmlEntities.decode(attr.value), spec.values, palette) { set(XmlEntities.encodeAttribute(it, '"'), merge = false) }
                else -> UiTextField(XmlEntities.decode(attr.value), stringResource(R.string.designer_ui_value_text), palette, Modifier.testTag("designer-ui-attr-value-$name"), monospace = spec?.type != "string", onSeal = { session.seal() }) { text ->
                    set(XmlEntities.encodeAttribute(text, '"'))
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp).testTag("designer-ui-attr-menu-$name")) { Icon(Icons.Filled.MoreVert, contentDescription = name, tint = Color(palette.muted)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.designer_ui_value_text)) }, onClick = {
                    menu = false
                    set(if (attr != null && !attr.isBare) attr.value else defaultValue(spec), merge = false)
                }, modifier = Modifier.testTag("designer-ui-attr-literal-$name"))
                DropdownMenuItem(text = { Text(stringResource(R.string.designer_ui_value_binding)) }, onClick = {
                    menu = false
                    val expression = attr?.parts?.filterIsInstance<AttrPart.Binding>()?.firstOrNull()?.expression ?: session.declaredNames.firstOrNull() ?: ""
                    set("{{$expression}}", merge = false)
                }, modifier = Modifier.testTag("designer-ui-attr-bind-$name"))
                if (spec?.type == "boolean" || attr?.isBare == true) DropdownMenuItem(text = { Text(stringResource(R.string.designer_ui_value_bare)) }, onClick = { menu = false; set(null, bare = true, merge = false) })
                if (attr != null) DropdownMenuItem(text = { Text(stringResource(R.string.designer_value_none)) }, onClick = { menu = false; set(null, merge = false) }, modifier = Modifier.testTag("designer-ui-attr-clear-$name"))
            }
        }
    }
    HorizontalDivider(color = Color(palette.divider))
}

/** A `{{ expression }}` value: the expression as text, with the script's declared names one tap away. */
@Composable
private fun BindingEditor(attr: XmlAttr, declared: List<String>, palette: StandalonePalette, onSeal: () -> Unit, onChange: (String) -> Unit) {
    val parts = attr.parts.orEmpty()
    val single = parts.singleOrNull() as? AttrPart.Binding
    var pick by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (single != null) {
            UiTextField(single.expression, "{{ }}", palette, Modifier.weight(1f).testTag("designer-ui-attr-binding-${attr.name}"), monospace = true, onSeal = onSeal) { onChange("{{$it}}") }
        } else {
            // Text mixed with bindings (`Hello {{name}}`): edited as the raw value.
            UiTextField(attr.value, "{{ }}", palette, Modifier.weight(1f).testTag("designer-ui-attr-binding-${attr.name}"), monospace = true, onSeal = onSeal) { onChange(it) }
        }
        if (declared.isNotEmpty()) Box {
            TextButton(onClick = { pick = true }, modifier = Modifier.testTag("designer-ui-attr-declared-${attr.name}")) { Text("...", color = Color(palette.muted)) }
            DropdownMenu(expanded = pick, onDismissRequest = { pick = false }) {
                declared.forEach { name -> DropdownMenuItem(text = { Text(name, style = ScriptText.code) }, onClick = { pick = false; onChange("{{$name}}") }) }
            }
        }
    }
}
