package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditHistory
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEvents
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiHandler
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiSegment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Roadmap P10.4 / D52 (option (b)): the reader finds the handlers a ui script registers and where a generated
 * `ui.<id>.on(...)` stub goes (after the `ui.layout` statement and the handlers already behind it); the command
 * inserts the stub there, keeps every other byte, reads back as a handler, and is a no-op where nothing can be added.
 */
class UiHandlerStubTest {
    private val reader = UiScriptReader()
    private val root: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }
    private val click = UiEvents.common.first { it.name == "click" }
    private val longClick = UiEvents.common.first { it.name == "long_click" }

    private fun sample(name: String): String = Files.readString(root.resolve("app/src/test/resources/ui-samples/$name")).replace("\r\n", "\n")

    private fun UiDocument.opaqueAfter(fragment: Int): String {
        val index = segments.indexOfFirst { it is UiSegment.Layout && it.fragment === fragments[fragment] }
        return (segments[index + 1] as UiSegment.Opaque).source
    }

    private fun UiDocument.byId(id: String) = elements().first { it.attr("id")?.value == id }

    @Test
    fun `the login sample lists its handlers and places stubs after the last handler of each layout`() {
        val document = reader.read(sample("login.js"))
        assertEquals(setOf(UiHandler("login", "click"), UiHandler("register", "click"), UiHandler("cancel", "click")), document.handlers)
        val first = document.fragments[0]
        val second = document.fragments[1]
        assertEquals("    ", first.stubIndent)
        assertTrue(document.opaqueAfter(0).substring(0, first.stubOffset).endsWith("ui.register.on('click', () => showRegisterUI());"))
        assertTrue(document.opaqueAfter(0).substring(first.stubOffset).startsWith("\n}"))
        assertTrue(document.opaqueAfter(1).substring(0, second.stubOffset).endsWith("ui.cancel.on('click', () => showLoginUI());"))
    }

    @Test
    fun `a stub is appended after the handlers and reads back as one`() {
        val source = sample("login.js")
        val document = reader.read(source)
        val history = UiEditHistory(document)
        assertTrue(history.execute(UiEditCommand.AddHandlerStub(document.byId("name").id, longClick), merge = false))
        val written = UiScriptWriter.write(history.document)
        val expected = source.replace(
            "    ui.register.on('click', () => showRegisterUI());\n",
            "    ui.register.on('click', () => showRegisterUI());\n\n    ui.name.on('long_click', (event, view) => {\n        // TODO\n    });\n",
        )
        assertEquals(expected, written)
        val reread = reader.read(written)
        assertTrue(UiHandler("name", "long_click") in reread.handlers)
        assertEquals(written, UiScriptWriter.write(reread))
        // The next stub of the same layout follows the first; a layout's own handler offset moved past it.
        assertTrue(history.execute(UiEditCommand.AddHandlerStub(document.byId("password").id, click), merge = false))
        val twice = UiScriptWriter.write(history.document)
        assertTrue(twice.indexOf("ui.name.on('long_click'") < twice.indexOf("ui.password.on('click', () => {"))
        assertTrue(twice.indexOf("ui.password.on('click'") < twice.indexOf("// 显示注册界面"))
        assertEquals(reread.fragments[0].stubOffset + UiEvents.stub("password", click, "    ").length, reader.read(twice).fragments[0].stubOffset)
        // Undo is byte exact.
        history.undo()
        history.undo()
        assertEquals(source, UiScriptWriter.write(history.document))
    }

    @Test
    fun `an element without an id gets one in the same step and existing handlers are not added twice`() {
        val source = "\"ui\";\nui.layout(\n    <vertical>\n        <button/>\n    </vertical>\n);\n"
        val document = reader.read(source)
        assertEquals("", document.fragments[0].stubIndent)
        val button = document.elements().first { it.tag == "button" }
        val added = UiEditCommand.AddHandlerStub(button.id, click, assignId = "button1").apply(document)
        assertEquals(
            "\"ui\";\nui.layout(\n    <vertical>\n        <button id=\"button1\"/>\n    </vertical>\n);\n\nui.button1.on('click', () => {\n    // TODO\n});\n",
            UiScriptWriter.write(added),
        )
        assertTrue(UiHandler("button1", "click") in added.handlers)
        assertSame(added, UiEditCommand.AddHandlerStub(button.id, click).apply(added))
        // Without an id and without one to assign nothing happens.
        assertSame(document, UiEditCommand.AddHandlerStub(button.id, click).apply(document))
    }

    @Test
    fun `the ui project template takes its stub after its click handler and a new id goes first`() {
        val document = reader.read(ProjectStore.UI_TEMPLATE)
        assertEquals(setOf(UiHandler("ok", "click")), document.handlers)
        assertEquals("", document.fragments[0].stubIndent)
        // Everything after the literal but the final newline: `);`, the blank line and the click handler.
        assertEquals(document.opaqueAfter(0).length - 1, document.fragments[0].stubOffset)
        val ok = document.byId("ok")
        val added = UiEditCommand.AddHandlerStub(ok.id, longClick).apply(document)
        assertEquals(ProjectStore.UI_TEMPLATE.trimEnd('\n') + "\n\nui.ok.on('long_click', (event, view) => {\n    // TODO\n});\n", UiScriptWriter.write(added))
        // A palette element gets its id in front of the template's attributes (roadmap P10.4).
        val inserted = UiEditCommand.InsertElement(document.root!!.id, 2, UiTemplates.create("button")).apply(document)
        val button = inserted.elements().last()
        val named = UiEditCommand.SetAttr(button.id, "id", "go").apply(inserted)
        assertTrue(UiScriptWriter.write(named).contains("        <button id=\"go\" text=\"Button\"/>\n"))
    }

    @Test
    fun `a layout held in a variable takes its stub after the ui layout call`() {
        val source = "\"ui\";\nvar layout = <vertical><button id=\"ok\"/></vertical>;\nui.layout(layout);\nui.ok.click(() => {});\nconsole.log(1);\n"
        val document = reader.read(source)
        assertEquals(setOf(UiHandler("ok", "click")), document.handlers)
        val added = UiEditCommand.AddHandlerStub(document.byId("ok").id, longClick).apply(document)
        assertEquals(
            "\"ui\";\nvar layout = <vertical><button id=\"ok\"/></vertical>;\nui.layout(layout);\nui.ok.click(() => {});\n\nui.ok.on('long_click', (event, view) => {\n    // TODO\n});\nconsole.log(1);\n",
            UiScriptWriter.write(added),
        )
    }

    @Test
    fun `layouts that are not passed to ui layout, returned, or in an expression arrow get no stubs`() {
        val other = reader.read("\"ui\";\nvar item = <text id=\"t\"/>;\nui.layout(<vertical id=\"v\"/>);\n")
        assertEquals(-1, other.fragments[0].stubOffset)
        assertTrue(other.fragments[1].stubOffset >= 0)
        assertSame(other, UiEditCommand.AddHandlerStub(other.byId("t").id, click).apply(other))
        val returned = reader.read("\"ui\";\nfunction show() {\n    return ui.layout(<vertical id=\"v\"/>);\n}\nshow();\n")
        assertEquals(-1, returned.fragments[0].stubOffset)
        val arrow = reader.read("\"ui\";\nconst show = () => ui.layout(<vertical id=\"v\"/>);\nshow();\n")
        assertEquals(-1, arrow.fragments[0].stubOffset)
    }

    @Test
    fun `every bundled sample places a stub for its root layout that reads back`() {
        Files.list(root.resolve("app/src/test/resources/ui-samples")).use { stream ->
            stream.filter { it.toString().endsWith(".js") }.toList().forEach { path ->
                val source = Files.readString(path).replace("\r\n", "\n")
                val document = reader.read(source)
                val fragment = document.fragments[document.rootFragment]
                assertTrue("${path.fileName}: root layout has a stub position", fragment.stubOffset >= 0)
                val target = fragment.root.descendants().firstOrNull { it.attr("id") != null && !it.attr("id")!!.hasBinding } ?: fragment.root
                val assign = if (target.attr("id") == null) "stub_target" else null
                val added = UiEditCommand.AddHandlerStub(target.id, UiEvents.common.last(), assign).apply(document)
                val written = UiScriptWriter.write(added)
                val id = assign ?: target.attr("id")!!.value
                assertTrue("${path.fileName}: stub written", written.contains("ui.$id.on('key', (keyCode, event, view) => {"))
                val reread = reader.read(written)
                assertTrue("${path.fileName}: stub reads back as a handler", UiHandler(id, "key") in reread.handlers)
                assertEquals("${path.fileName}: stable", written, UiScriptWriter.write(reread))
            }
        }
    }
}
