package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.FragmentUsage
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEvents
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiLayoutFiles
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P11.3 / D66, D67: an XML literal written directly in `ui.layout(...)` / `ui.inflate(...)` moves into a
 * layout file as it is, and a new layout file is added with one empty root and a reference at the end of the script.
 */
class UiLayoutFilesTest {
    private val reader = UiScriptReader()

    private fun read(source: String, files: Map<String, String> = emptyMap()): UiDocument = reader.read(source) { files[it] }

    @Test
    fun `a layout literal moves into a file and the script lays the file out`() {
        val document = read(ProjectStore.UI_TEMPLATE)
        assertNull(UiLayoutFiles.refusalOf(document, 0))
        assertEquals("layout.xml", UiLayoutFiles.suggestedPath(document, 0))

        val extracted = UiLayoutFiles.extract(document, 0, "layout.xml")

        assertEquals(
            """
            |"ui";
            |
            |ui.layoutFile("layout.xml");
            |
            |ui.ok.on('click', () => {
            |    toast('OK');
            |});
            |""".trimMargin(),
            UiScriptWriter.write(extracted),
        )
        assertEquals(
            mapOf(
                "layout.xml" to """
                |<vertical padding="16">
                |    <text id="title" text="Hello" textSize="24sp"/>
                |    <button id="ok" text="OK"/>
                |</vertical>
                |""".trimMargin(),
            ),
            UiScriptWriter.writeFiles(extracted),
        )
        val file = extracted.fragments[extracted.rootFragment]
        assertEquals("layout.xml", file.file?.path)
        assertEquals(FragmentUsage.LAYOUT, file.usage)
        assertFalse(extracted.isNative(file))
        // The existing handler is still known, and a new stub goes after the statement that loads the file and its handlers.
        assertTrue(extracted.handlers.any { it.id == "ok" && it.event == "click" })
        val title = extracted.elements().first { it.attr("id")?.value == "title" }
        val stubbed = UiScriptWriter.write(UiEditCommand.AddHandlerStub(title.id, UiEvents.common.first()).apply(extracted))
        assertTrue(stubbed, stubbed.indexOf("ui.title.on(") > stubbed.indexOf("ui.ok.on("))
    }

    @Test
    fun `an inflated literal keeps its other arguments`() {
        val source = """
            |'ui';
            |
            |ui.layout(<frame id="host"/>);
            |let row = ui.inflate(
            |    <horizontal>
            |        <text text="{{name}}"/>
            |    </horizontal>, ui.host, true);
            |""".trimMargin()
        val document = read(source)
        assertEquals("item.xml", UiLayoutFiles.suggestedPath(document, 1))
        val extracted = UiLayoutFiles.extract(document, 1, "rows/row.xml")
        assertEquals(
            """
            |'ui';
            |
            |ui.layout(<frame id="host"/>);
            |let row = ui.inflate(
            |    files.read('rows/row.xml'), ui.host, true);
            |""".trimMargin(),
            UiScriptWriter.write(extracted),
        )
        assertEquals(mapOf("rows/row.xml" to "<horizontal>\n    <text text=\"{{name}}\"/>\n</horizontal>\n"), UiScriptWriter.writeFiles(extracted))
        assertEquals(FragmentUsage.OTHER, extracted.files.single().usage)
    }

    @Test
    fun `literals the designer cannot move are refused`() {
        val source = """
            |"ui";
            |
            |var layout = <vertical><text id="a"/></vertical>;
            |ui.layout(layout);
            |let item = ui.inflate(<text>{label}</text>);
            |""".trimMargin()
        val document = read(source)
        assertEquals(UiLayoutFiles.Refusal.NOT_INLINE, UiLayoutFiles.refusalOf(document, 0))
        assertEquals(UiLayoutFiles.Refusal.EXPRESSIONS, UiLayoutFiles.refusalOf(document, 1))
        assertThrows(IllegalArgumentException::class.java) { UiLayoutFiles.extract(document, 0, "layout.xml") }

        val withFile = read("\"ui\";\n\nui.layoutFile(\"layout.xml\");\n", mapOf("layout.xml" to "<frame/>\n"))
        assertEquals(UiLayoutFiles.Refusal.NOT_A_LITERAL, UiLayoutFiles.refusalOf(withFile, 0))
    }

    @Test
    fun `paths stay relative xml files the script does not use yet`() {
        val document = read("\"ui\";\n\nui.layoutFile(\"layout.xml\");\nui.inflate(files.read(\"gone.xml\"));\n", mapOf("layout.xml" to "<frame/>\n"))
        assertEquals(listOf("gone.xml"), document.missingFiles)
        assertNull(UiLayoutFiles.pathProblem(document, "res/layout/main.xml"))
        assertEquals(UiLayoutFiles.PathProblem.TAKEN, UiLayoutFiles.pathProblem(document, "layout.xml"))
        assertEquals(UiLayoutFiles.PathProblem.TAKEN, UiLayoutFiles.pathProblem(document, "gone.xml"))
        for (path in listOf("", ".xml", "layout", "layout.txt", "/sdcard/a.xml", "../a.xml", "a/../b.xml", "./a.xml", "a//b.xml", "C:/a.xml", "a\\b.xml", "a'b.xml", "a\"b.xml")) {
            assertEquals(path, UiLayoutFiles.PathProblem.INVALID, UiLayoutFiles.pathProblem(document, path))
        }
        assertEquals("layout-2.xml", UiLayoutFiles.freePath(document, "layout.xml"))
        assertEquals("item.xml", UiLayoutFiles.suggestedNewPath(document, native = false))
        assertEquals("res/layout/item.xml", UiLayoutFiles.suggestedNewPath(document, native = true))
    }

    @Test
    fun `a new file gets an empty root and a reference at the end of the script`() {
        val document = read(ProjectStore.UI_TEMPLATE)
        val added = UiLayoutFiles.addFile(document, "res/layout/item_row.xml", native = true)
        assertEquals(
            ProjectStore.UI_TEMPLATE + "\nlet itemRowView = ui.inflate(files.read(\"res/layout/item_row.xml\"));\n",
            UiScriptWriter.write(added),
        )
        assertEquals(UiLayoutFiles.blankFile(native = true), UiScriptWriter.writeFiles(added)["res/layout/item_row.xml"])
        val file = added.files.single()
        assertTrue(added.isNative(file))
        assertEquals("LinearLayout", file.root.tag)

        val again = UiLayoutFiles.addFile(added, "item-row.xml", native = false)
        assertTrue(UiScriptWriter.write(again).endsWith("let itemRowView2 = ui.inflate(files.read(\"item-row.xml\"));\n"))
        assertEquals("vertical", again.files.last().root.tag)
        assertFalse(again.isNative(again.files.last()))
    }

    @Test
    fun `the script's android layout mode decides whether a fragment is native`() {
        val files = mapOf("layout.xml" to "<frame/>\n")
        assertFalse(read("\"ui\";\nui.layoutFile(\"layout.xml\");\n", files).let { it.isNative(it.files.single()) })
        assertTrue(read("\"ui\";\nui.useAndroidLayout();\nui.layoutFile(\"layout.xml\");\n", files).let { it.isNative(it.files.single()) })
        assertNull(UiLayoutFiles.fixedDialect(read("\"ui\";\nui.layoutFile(\"layout.xml\");\n", files)))
        assertEquals(false, UiLayoutFiles.fixedDialect(read("\"ui\";\nui.useAndroidLayout(false);\nui.layoutFile(\"layout.xml\");\n", files)))
    }

    @Test
    fun `dedent keeps relative indentation`() {
        assertEquals("<a>\n    <b/>\n\n</a>", UiLayoutFiles.dedent("<a>\n        <b/>\n\n    </a>"))
        assertEquals("<a/>", UiLayoutFiles.dedent("<a/>"))
    }
}
