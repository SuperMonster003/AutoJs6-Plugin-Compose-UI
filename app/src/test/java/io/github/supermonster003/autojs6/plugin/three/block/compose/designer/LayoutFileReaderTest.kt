package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.FragmentUsage
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditHistory
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEvents
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.XmlCst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P11.3 / D56: layout files a `"ui"` script refers to by a literal path are read as fragments of the same
 * document (`ui.layoutFile` lays one out, `ui.inflate(files.read(...))` inflates one), kept byte for byte around
 * their root element, edited with the same commands and written back to their own text; handler stubs for their
 * elements go after the statement that loads them. The script's `ui.useAndroidLayout` mode is recorded.
 */
class LayoutFileReaderTest {
    private val reader = UiScriptReader()

    private val layout = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<!-- The main screen. -->
        |<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        |    android:layout_width="match_parent"
        |    android:layout_height="match_parent"
        |    android:orientation="vertical">
        |
        |    <TextView android:id="@+id/title" android:text="Hello" />
        |
        |    <Button android:id="@+id/ok" android:text="OK" />
        |</LinearLayout>
        |<!-- end -->
        |""".trimMargin()

    private val item = "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/name\" />\n"

    private val script = """
        |"ui";
        |
        |ui.useAndroidLayout(true);
        |ui.layoutFile("layout.xml");
        |ui.ok.on('click', () => toast('ok'));
        |
        |let row = ui.inflate(files.read("item.xml"), ui.title.getParent(), false);
        |""".trimMargin()

    private fun read(source: String = script, files: Map<String, String> = mapOf("layout.xml" to layout, "item.xml" to item)): UiDocument =
        reader.read(source) { files[it] }

    private fun UiDocument.byId(id: String) = elements().first { it.attr("android:id")?.value == "@+id/$id" }

    @Test
    fun `a layout file is the canvas root and reads back byte for byte`() {
        val document = read()
        assertEquals(2, document.files.size)
        val main = document.files[0]
        assertEquals("layout.xml", main.file!!.path)
        assertEquals(FragmentUsage.LAYOUT, main.usage)
        assertEquals("LinearLayout", document.root!!.tag)
        assertEquals(document.fragments.indexOf(main), document.rootFragment)
        assertEquals(layout, UiScriptWriter.writeFile(main))
        assertEquals(item, UiScriptWriter.writeFile(document.files[1]))
        assertEquals(FragmentUsage.OTHER, document.files[1].usage)
        // The script itself has no literal and is written unchanged.
        assertEquals(script, UiScriptWriter.write(document))
        assertEquals(mapOf("layout.xml" to layout, "item.xml" to item), UiScriptWriter.writeFiles(document))
        assertTrue(document.androidLayout!!)
        assertTrue(document.missingFiles.isEmpty())
        assertTrue(UiScriptReader.looksLikeUiScript(script))
    }

    @Test
    fun `CRLF files keep their line breaks and an edit changes only its start tag`() {
        val crlf = layout.replace("\n", "\r\n")
        val document = read(files = mapOf("layout.xml" to crlf, "item.xml" to item))
        assertEquals(crlf, UiScriptWriter.writeFile(document.files[0]))
        val history = UiEditHistory(document)
        assertTrue(history.execute(UiEditCommand.SetAttr(document.byId("title").id, "android:text", "Hi"), merge = false))
        val written = UiScriptWriter.writeFiles(history.document)
        assertEquals(crlf.replace("android:text=\"Hello\"", "android:text=\"Hi\""), written["layout.xml"])
        assertEquals(item, written["item.xml"])
        assertEquals(script, UiScriptWriter.write(history.document))
        assertTrue(history.undo())
        assertEquals(crlf, UiScriptWriter.writeFiles(history.document)["layout.xml"])
    }

    @Test
    fun `inserting into a file root keeps the file's indentation`() {
        val document = read()
        val history = UiEditHistory(document)
        val element = XmlCst.parse("<Switch android:text=\"On\"/>") { 0 }
        assertTrue(history.execute(UiEditCommand.InsertElement(document.root!!.id, 2, element), merge = false))
        val written = UiScriptWriter.writeFiles(history.document)["layout.xml"]!!
        assertTrue(written, written.contains("<Button android:id=\"@+id/ok\" android:text=\"OK\" />\n    <Switch android:text=\"On\"/>\n</LinearLayout>"))
    }

    @Test
    fun `handler stubs for file elements go after the statement that loads the file`() {
        val document = read()
        val history = UiEditHistory(document)
        val click = UiEvents.common.first { it.name == "click" }
        val longClick = UiEvents.common.first { it.name == "long_click" }
        // ui.ok already has a click handler in the script: no second one.
        assertFalse(history.execute(UiEditCommand.AddHandlerStub(document.byId("ok").id, click), merge = false))
        assertTrue(history.execute(UiEditCommand.AddHandlerStub(document.byId("title").id, click), merge = false))
        assertTrue(history.execute(UiEditCommand.AddHandlerStub(document.byId("title").id, longClick), merge = false))
        val expected = script.replace(
            "ui.ok.on('click', () => toast('ok'));",
            "ui.ok.on('click', () => toast('ok'));" + UiEvents.stub("title", click, "") + UiEvents.stub("title", longClick, ""),
        )
        assertEquals(expected, UiScriptWriter.write(history.document))
        // The stubs read back as handlers of a document that still loads the same files.
        assertEquals(setOf("ok", "title"), reader.read(expected) { mapOf("layout.xml" to layout, "item.xml" to item)[it] }.handlers.map { it.id }.toSet())
    }

    @Test
    fun `the first layout call decides the root between a literal and a file`() {
        val literalFirst = "\"ui\";\nui.layout(<vertical><text id=\"a\"/></vertical>);\nui.layoutFile(\"layout.xml\");\n"
        val first = read(literalFirst)
        assertEquals("vertical", first.root!!.tag)
        assertEquals(1, first.files.size)
        val fileFirst = "\"ui\";\nui.layoutFile(\"layout.xml\");\nlet later = () => ui.layout(<vertical><text id=\"a\"/></vertical>);\n"
        val second = read(fileFirst)
        assertEquals("LinearLayout", second.root!!.tag)
        // Element ids are unique across literals and files.
        assertEquals(second.elements().count(), second.elements().map { it.id }.toSet().size)
    }

    @Test
    fun `a file referred to twice is one fragment and laying it out wins`() {
        val source = "\"ui\";\nlet a = ui.inflate(files.read(\"layout.xml\"));\nui.layoutFile(\"layout.xml\");\n"
        val document = read(source)
        assertEquals(1, document.files.size)
        assertEquals(FragmentUsage.LAYOUT, document.files[0].usage)
        assertEquals(source.indexOf("ui.layoutFile"), document.files[0].layoutCallOffset)
    }

    @Test
    fun `missing and unreadable files`() {
        // A missing file next to a literal root is listed, and the document still opens.
        val withLiteral = read("\"ui\";\nui.layout(<vertical/>);\nlet row = ui.inflate(files.read(\"missing.xml\"));\n")
        assertEquals(listOf("missing.xml"), withLiteral.missingFiles)
        // Without anything else to lay out, the call is the error.
        val missing = assertThrows(DesignerParseError::class.java) { read("\"ui\";\nui.layoutFile(\"missing.xml\");\n") }
        assertEquals(2, missing.line)
        assertTrue(missing.message!!, missing.message!!.contains("missing.xml cannot be read"))
        val dynamic = assertThrows(DesignerParseError::class.java) { read("\"ui\";\nlet name = 'layout.xml';\nui.layoutFile(name);\n") }
        assertTrue(dynamic.message!!, dynamic.message!!.contains("string literal path"))
        // A malformed file is reported with its own line and column at the call.
        val broken = assertThrows(DesignerParseError::class.java) {
            read(files = mapOf("layout.xml" to "<?xml version=\"1.0\"?>\n<LinearLayout>\n  <TextView>\n</LinearLayout>\n", "item.xml" to item))
        }
        assertEquals(4, broken.line)
        assertTrue(broken.message!!, broken.message!!.contains("layout.xml, line 4, column 1"))
        val doctype = assertThrows(DesignerParseError::class.java) { read(files = mapOf("layout.xml" to "<!DOCTYPE x>\n<View/>", "item.xml" to item)) }
        assertTrue(doctype.message!!, doctype.message!!.contains("Document type declarations"))
    }

    @Test
    fun `the useAndroidLayout mode`() {
        fun mode(call: String): Boolean? = read("\"ui\";\n$call\nui.layoutFile(\"layout.xml\");\n").androidLayout
        assertEquals(true, mode("ui.useAndroidLayout();"))
        assertEquals(true, mode("ui.useAndroidLayout(true);"))
        assertEquals(false, mode("ui.useAndroidLayout(false);"))
        assertNull(mode("ui.useAndroidLayout(null);"))
        assertNull(mode("ui.useAndroidLayout(isNative());"))
        assertNull(mode(""))
        // The first call counts.
        assertEquals(false, mode("ui.useAndroidLayout(false);\nui.useAndroidLayout(true);"))
    }
}
