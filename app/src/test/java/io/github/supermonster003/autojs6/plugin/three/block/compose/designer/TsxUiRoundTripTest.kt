package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.FragmentUsage
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEvents
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiHandler
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.idName
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsxUiReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.XmlPreviewFrames
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * Roadmap P11.7 / D57 / D68: TSX ui scripts. Every TSX ui source (hand-written cases, the TypeScript Engine's ui
 * template, the new-project template and the host's legacy XML sample) reads and writes back byte for byte while
 * unedited; its canonical form (every layout written whole) reads back to the same layouts and is a fixed point; an
 * edit rewrites only its own layout; handler stubs follow D68; the preview and the validator follow the host's TSX
 * runtime. Trees come from the TypeScript Engine parser through `.python/generate_tsx_snapshots.py`.
 */
class TsxUiRoundTripTest {
    private val snapshots = TsxSnapshots()

    private val sources: List<String>
        get() = snapshots.sources("ui") + snapshots.sources("ui-templates") + TsxSnapshotRecorder.UI_HOST

    private fun read(name: String): UiDocument = TsxUiReader().read(snapshots.source(name), snapshots.tree(name))

    /** The layouts without element ids or sources, and expressions without their indentation: what a re-read must reproduce. */
    private fun shape(document: UiDocument): List<String> =
        document.fragments.map { fragment -> (fragment.usage.name + " " + shape(fragment.root)).lines().joinToString("|") { it.trim() } }

    private fun shape(node: XmlNode): String = when (node) {
        is XmlNode.Element -> "<" + node.tag + node.attrs.joinToString("") { " " + it.name + (if (it.isBare) "" else "=" + it.value) } + ">" +
            node.children.joinToString("") { shape(it) } + "</" + node.tag + ">"
        is XmlNode.Text -> "[" + node.text + "]"
        else -> "{" + node.source + "}"
    }

    @Test
    fun `every TSX ui source is covered and has a canonical form`() {
        assertEquals(listOf("ui/cases.tsx", "ui/engine-template.tsx", "ui/variable.tsx"), snapshots.sources("ui"))
        assertEquals(ProjectStore.TSX_UI_TEMPLATE, snapshots.source(TsxSnapshotRecorder.UI_TEMPLATE))
        sources.forEach { name -> assertTrue("$name has no canonical form", Files.isRegularFile(snapshots.root.resolve("ui-canonical/$name"))) }
    }

    @Test
    fun `unedited layouts are written back byte for byte`() {
        sources.forEach { name ->
            val document = read(name)
            assertEquals(name, Dialect.TSX, document.dialect)
            assertEquals(name, snapshots.source(name), UiScriptWriter.write(document))
        }
    }

    @Test
    fun `the canonical form reads back to the same layouts and is a fixed point`() {
        sources.forEach { name ->
            val canonicalName = "ui-canonical/$name"
            val canonical = snapshots.source(canonicalName)
            val document = read(name)
            assertEquals(name, canonical, TsxSnapshots.tsxUiCanonical(document))
            val reread = TsxUiReader().read(canonical, snapshots.tree(canonicalName))
            assertEquals(name, shape(document), shape(reread))
            assertEquals(name, canonical, TsxSnapshots.tsxUiCanonical(reread))
        }
        // The new-project template is already in the canonical form.
        assertEquals(ProjectStore.TSX_UI_TEMPLATE, snapshots.source("ui-canonical/" + TsxSnapshotRecorder.UI_TEMPLATE))
    }

    @Test
    fun `the reader maps layouts, handlers, literals and the script's settings`() {
        val document = read("ui/cases.tsx")
        assertEquals(listOf(FragmentUsage.LAYOUT, FragmentUsage.OTHER), document.fragments.map { it.usage })
        assertEquals(0, document.rootFragment)
        assertEquals("row", document.fragments[1].boundTo)
        assertEquals(false, document.androidLayout)
        assertEquals(setOf("my-widget"), document.widgets)
        assertEquals(setOf(UiHandler("list", "long_click"), UiHandler("cancel", "click")), document.handlers)
        // Number, boolean and negative literals are known to the preview; a string expression is a plain string.
        assertEquals(mapOf("8" to "8", "-4" to "-4", "0.5" to "0.5", "true" to "true", "2" to "2"), document.literalValues)
        val list = document.elements().first { it.idName() == "list" }
        assertEquals("visible", list.attr("visibility")?.value)
        assertTrue(list.attr("clickable")!!.isBare)
        assertEquals("{title}", document.elements().first { it.idName() == "title" }.attr("text")?.value)
        assertEquals("Tom &amp; Jerry &lt;3", document.elements().first { it.idName() == "entities" }.textContent)
        assertEquals("Name: &apos;first&apos;".replace("&apos;", "'"), document.elements().first { it.idName() == "name" }.attr("hint")?.let { it.value })
        assertEquals("@+id/cancel", document.elements().first { it.attr("w") != null }.attr("android:id")?.value)
        // Handler stubs go after the ui.layout statement and the two handler statements behind it.
        val fragment = document.fragments[0]
        val after = (document.segments[2] as io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiSegment.Opaque).source
        assertTrue(after.substring(0, fragment.stubOffset).endsWith("toast('cancel');\n});"))
        assertEquals("", fragment.stubIndent)
        // A layout handed to ui.layout through a variable is the canvas root.
        val variable = read("ui/variable.tsx")
        assertEquals("layout", variable.fragments[variable.rootFragment].boundTo)
        assertEquals(setOf(UiHandler("message", "click")), variable.handlers)
    }

    @Test
    fun `an edit rewrites only its own layout`() {
        val name = "ui/cases.tsx"
        val source = snapshots.source(name)
        val document = read(name)
        val edited = UiEditCommand.SetAttr(document.root!!.id, "id", "page").apply(document)
        val text = UiScriptWriter.write(edited)
        val start = source.indexOf("<frame>")
        val end = source.indexOf("</frame>") + "</frame>".length
        assertEquals(source.substring(0, start), text.substring(0, start))
        assertEquals(source.substring(end), text.substring(text.length - (source.length - end)))
        val canonical = snapshots.source("ui-canonical/$name")
        assertEquals(canonical.replaceFirst("<frame>", "<frame id='page'>"), text)
        // Undoing the edit gives back the read tree, so the layout is written as it was read again.
        assertEquals(source, UiScriptWriter.write(edited.mapElement(document.root!!.id) { document.root!! }))
    }

    @Test
    fun `handler stubs follow D68`() {
        val document = read("ui/variable.tsx")
        val message = document.elements().first { it.idName() == "message" }
        // A click is an inline onClick: no id and no statement needed, the layout is rewritten.
        val clicked = UiEditCommand.AddHandlerStub(message.id, UiEvents.common.first { it.name == "click" }).apply(document)
        assertTrue(UiScriptWriter.write(clicked), UiScriptWriter.write(clicked).contains(
            "        <text id='message' text='Hello' onClick={() => {\n            // TODO\n        }} />",
        ))
        assertEquals(document, UiEditCommand.AddHandlerStub(message.id, UiEvents.common.first { it.name == "click" }).apply(clicked).let { document })
        // Other events are (ui as any).<id>.on(...) statements with typed, underscored parameters after the layout's.
        val longClick = UiEditCommand.AddHandlerStub(message.id, UiEvents.common.first { it.name == "long_click" }).apply(document)
        assertEquals(
            snapshots.source("ui/variable.tsx").replace(
                "ui.message.click(() => toast('hi'));",
                "ui.message.click(() => toast('hi'));\n\n(ui as any).message.on('long_click', (_event: any, _view: any) => {\n    // TODO\n});",
            ),
            UiScriptWriter.write(longClick),
        )
        assertTrue(UiHandler("message", "long_click") in longClick.handlers)
        assertEquals("_keyCode: any, _event: any, _view: any", UiEvents.tsxParams("keyCode, event, view"))
    }

    @Test
    fun `the preview gets the layout XML the host's TSX runtime makes`() {
        val template = XmlPreviewFrames.build(read("ui/engine-template.tsx"))
        assertEquals(
            "<vertical gravity=\"center\" padding=\"24\"><text id=\"status\" text=\"TypeScript TSX template\" textSize=\"20sp\"/><button text=\"Click me\"/></vertical>",
            template.xml,
        )
        assertTrue(template.notes.isEmpty())
        val cases = XmlPreviewFrames.build(read("ui/cases.tsx"))
        // Attributes sorted, the bare one true, literals resolved, onClick dropped, text joined into text; {title} is noted.
        assertTrue(cases.xml, cases.xml.contains("<vertical alpha=\"0.5\" clickable=\"true\" enabled=\"true\" id=\"list\" margin=\"-4\" padding=\"8\" visibility=\"visible\">"))
        assertTrue(cases.xml, cases.xml.contains("<text id=\"entities\" text=\"Tom &amp; Jerry &lt;3\"/>"))
        assertTrue(cases.xml, cases.xml.contains("<button id=\"ok\" text=\"OK\"/>"))
        assertTrue(cases.xml, cases.xml.contains("<button android:id=\"@+id/cancel\" id=\"cancel\" text=\"Cancel\" w=\"*\"/>"))
        assertEquals(listOf("text"), cases.notes.map { it.attrName })
        assertEquals(listOf("count"), cases.bindingNames)
    }

    @Test
    fun `the validator applies the TSX runtime's rules`() {
        val validator = UiValidator()
        sources.forEach { name -> assertEquals(name, emptyList<String>(), validator.validate(read(name)).filter { it.severity == Severity.ERROR }.map { it.message }) }
        val document = read("ui/variable.tsx")
        val message = document.elements().first { it.idName() == "message" }
        fun errors(edit: UiEditCommand) = validator.validate(edit.apply(document)).filter { it.severity == Severity.ERROR }.map { it.target to it.message }
        assertEquals(listOf("onLongClick" to "The TSX runtime binds only onClick; register this event with (ui as any).<id>.on(...)"), errors(UiEditCommand.SetAttr(message.id, "onLongClick", "x")))
        assertEquals(listOf("onClick" to "onClick needs a function: onClick={() => { ... }}"), errors(UiEditCommand.SetAttr(message.id, "onClick", "x")))
        // No unknown-attribute warning for the inline onClick of the template.
        val template = read("ui-templates/new-project.tsx")
        assertTrue(validator.validate(template).none { it.target == "onClick" })
    }
}
