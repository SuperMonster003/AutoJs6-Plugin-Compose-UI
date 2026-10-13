package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.PreviewControls
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.PreviewFrames
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerSession
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P9.3 step 1: preview values of top-level states and the controls that change them. */
class PreviewStateTest {
    private val catalog = ComposeUiV2.interopCatalog
    private val script = """
        var on = compose.state(false);
        var name = compose.state('Ada');
        var level = compose.state(0.25);
        var page = compose.state(0);
        compose.mount(compose.Column({},
            compose.Switch({key: 'bound', checked: on.value}),
            compose.Switch({key: 'literal', checked: true}),
            compose.TextField({key: 'field', text: name.value}),
            compose.Slider({key: 'slider', value: level.value}),
            compose.Text({key: 'label', text: name.value})));
    """.trimIndent() + "\n"

    private val document = ScriptReader().read(script)
    private fun node(key: String) = document.nodes().single { (it.key as? io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue.Literal)?.value == JsLiteral.Str(key) }

    @Test
    fun `an event whose payload names a state-bound property controls that state`() {
        fun controls(key: String) = node(key).let { PreviewControls.controls(it, catalog.component(it.type)!!) }
            .mapValues { (_, list) -> list.map { it.field.name to it.state } }
        assertEquals(mapOf("checkedChange" to listOf("checked" to "on")), controls("bound"))
        assertEquals(emptyMap<String, Any>(), controls("literal"))
        assertEquals(mapOf("valueChange" to listOf("text" to "name")), controls("field"))
        assertEquals(mapOf("valueChange" to listOf("value" to "level")), controls("slider"))
        assertEquals(emptyMap<String, Any>(), controls("label"))
    }

    @Test
    fun `the catalog's value events name the properties they control`() {
        // PreviewControls relies on this naming; a catalog change shows up here instead of as a silent gap.
        val controlled = catalog.components.flatMap { spec ->
            spec.events.flatMap { event ->
                event.payloadFields.filter { field -> spec.props.any { it.name == field.name } }.map { "${spec.name}.${event.name}.${it.name}" }
            }
        }.toSortedSet()
        assertEquals(CONTROLLED, controlled)
    }

    @Test
    fun `payload values become literals of the bound states`() {
        val field = node("field")
        val payload = mapOf<String, Any?>("text" to "Grace", "selectionStart" to 5, "selectionEnd" to 5, "editSeq" to 3L)
        assertEquals(mapOf("name" to JsLiteral.Str("Grace")), PreviewControls.values(field, catalog.component(field.type)!!, "valueChange", payload::get))
        val slider = node("slider")
        assertEquals(mapOf("level" to JsLiteral.Num(0.5)), PreviewControls.values(slider, catalog.component(slider.type)!!, "valueChange") { if (it == "value") 0.5 else null })
        // A payload without the field (or another event) changes nothing.
        assertEquals(emptyMap<String, JsLiteral>(), PreviewControls.values(slider, catalog.component(slider.type)!!, "valueChange") { null })
        assertEquals(emptyMap<String, JsLiteral>(), PreviewControls.values(slider, catalog.component(slider.type)!!, "valueChangeFinished") { 1.0 })
    }

    @Test
    fun `preview values replace initial values and bound properties get callbacks`() {
        val initial = PreviewFrames.build(document, 1).frame!!
        assertEquals(UiValue.Bool(false), initial.nodes.getValue(node("bound").id).props["checked"])
        assertEquals(mapOf("checkedChange" to node("bound").id), initial.nodes.getValue(node("bound").id).callbacks)
        assertEquals(emptyMap<String, Int>(), initial.nodes.getValue(node("literal").id).callbacks)

        val preview = PreviewFrames.build(document, 2, values = mapOf("on" to JsLiteral.Bool(true), "name" to JsLiteral.Str("Grace"), "unknown" to JsLiteral.Num(1.0)))
        assertTrue(preview.problems.toString(), preview.problems.isEmpty())
        val frame = preview.frame!!
        assertEquals(UiValue.Bool(true), frame.nodes.getValue(node("bound").id).props["checked"])
        assertEquals(UiValue.Str("Grace"), frame.nodes.getValue(node("label").id).props["text"])
        assertEquals(UiValue.Str("Grace"), frame.nodes.getValue(node("field").id).props["text"])
    }

    @Test
    fun `a canvas event sets the preview value and leaves the document alone`() {
        val session = DesignerSession(document)
        val revision = session.revision
        session.previewEvent(node("bound").id, "checkedChange") { if (it == "checked") true else null }
        assertEquals(mapOf("on" to JsLiteral.Bool(true)), session.previewValues)
        session.previewEvent(node("literal").id, "checkedChange") { false }
        assertEquals(mapOf("on" to JsLiteral.Bool(true)), session.previewValues)
        assertEquals(revision, session.revision)
        assertFalse(session.dirty)
        assertEquals(document, session.document)

        session.setPreviewValue("on", null)
        assertEquals(emptyMap<String, JsLiteral>(), session.previewValues)
        session.setPreviewValue("name", JsLiteral.Str("Grace"))
        session.replace(ScriptReader().read(script))
        assertEquals("a reloaded document starts from its initial values", emptyMap<String, JsLiteral>(), session.previewValues)
        assertTrue((document.root!!.children.first() as DesignChild.Node).node.type == "Switch")
    }

    private companion object {
        /** Component.event.field triples whose payload field is also a property of the component. */
        val CONTROLLED = sortedSetOf(
            "Checkbox.checkedChange.checked",
            "DatePicker.dateChange.selectedDateMillis",
            "DatePicker.displayModeChange.displayMode",
            "FilterChip.selectedChange.selected",
            "HorizontalPager.pageChange.page",
            "InputChip.selectedChange.selected",
            "NavigationDrawer.openChange.open",
            "OutlinedTextField.valueChange.text",
            "RadioButton.checkedChange.checked",
            "SearchBar.expandedChange.expanded",
            "SegmentedButtonItem.selectedChange.selected",
            "Slider.valueChange.value",
            "Switch.checkedChange.checked",
            "TextField.valueChange.text",
            "TimePicker.timeChange.hour",
            "TimePicker.timeChange.minute",
            "Tooltip.openChange.open",
        )
    }
}
