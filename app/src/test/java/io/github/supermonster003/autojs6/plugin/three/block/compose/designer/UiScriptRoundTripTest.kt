package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.FragmentUsage
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditHistory
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Roadmap P10.1 / D49: ui-mode scripts read into a [UiDocument] and written back untouched are byte-identical,
 * and every single edit changes only the text it is about. The corpus is the twelve host samples copied under
 * `src/test/resources/ui-samples/`; with `AUTOJS6_HOST_DIR` set, every `"ui";` sample of that checkout that
 * calls `ui.layout` with a literal is read as well.
 */
class UiScriptRoundTripTest {
    private companion object {
        /** `ui.layout(` followed by a literal, possibly on the next line. */
        val LITERAL_CALL = Regex("""ui\.layout\(\s*<""")
    }

    private val reader = UiScriptReader()
    private val root: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }

    private fun samples(): Map<String, String> = Files.list(root.resolve("app/src/test/resources/ui-samples")).use { stream ->
        stream.filter { it.toString().endsWith(".js") }.toList().associate { it.fileName.toString() to Files.readString(it) }
    }

    private fun normalized(text: String) = text.replace("\r\n", "\n")

    @Test
    fun `the bundled samples read and write back byte for byte`() {
        val samples = samples()
        assertEquals(12, samples.size)
        samples.forEach { (name, script) ->
            val source = normalized(script)
            val document = reader.read(source)
            assertEquals("$name: untouched documents are written byte for byte", source, UiScriptWriter.write(document))
            assertEquals("$name: reading the written text gives the same document", document, reader.read(UiScriptWriter.write(document)))
            assertNotNull("$name has a canvas root", document.root)
            assertTrue("$name: the canvas root is a layout fragment", document.fragments[document.rootFragment].usage == FragmentUsage.LAYOUT)
            assertTrue("$name: ids are unique", document.elements().map { it.id }.toList().let { it.size == it.toSet().size })
        }
    }

    @Test
    fun `registered widgets are collected`() {
        val document = reader.read(normalized(samples().getValue("widget-template.js")))
        assertEquals(setOf("input-layout"), document.widgets)
    }

    @Test
    fun `the login sample has two layout fragments and the first is the root`() {
        val document = reader.read(normalized(samples().getValue("login.js")))
        assertEquals(2, document.fragments.size)
        assertEquals(0, document.rootFragment)
        assertTrue(document.fragments.all { it.usage == FragmentUsage.LAYOUT })
        assertEquals("frame", document.root!!.tag)
    }

    @Test
    fun `setting an attribute rewrites only that start tag`() {
        samples().forEach { (name, script) ->
            val source = normalized(script)
            val document = reader.read(source)
            document.elements().toList().forEach { element ->
                val edited = UiEditCommand.SetAttr(element.id, "id", "edited_${element.id}").apply(document)
                val written = UiScriptWriter.write(edited)
                val newTag = UiScriptWriter.startTag(edited.element(element.id)!!)
                val oldTag = element.startTagSource!!
                val prefix = source.commonPrefixWith(written).length.coerceAtMost(minOf(source.length, written.length))
                val suffix = source.commonSuffixWith(written).length.coerceAtMost(minOf(source.length, written.length) - prefix)
                val sourceMid = source.substring(prefix, source.length - suffix)
                val writtenMid = written.substring(prefix, written.length - suffix)
                assertEquals("$name <${element.tag}#${element.id}>: the length changes by the start tag only", newTag.length - oldTag.length, written.length - source.length)
                assertTrue("$name <${element.tag}#${element.id}>: the changed text [$sourceMid] lies inside the old start tag", oldTag.contains(sourceMid))
                assertTrue("$name <${element.tag}#${element.id}>: the new text [$writtenMid] lies inside the new start tag", newTag.contains(writtenMid))
                val reread = reader.read(written)
                assertEquals("$name: the edited attribute reads back", "edited_${element.id}", reread.element(element.id)!!.attr("id")!!.value)
            }
        }
    }

    @Test
    fun `removing an element takes the blank line before it and nothing else`() {
        val source = normalized(samples().getValue("card.js"))
        val document = reader.read(source)
        val removable = document.elements().filter { document.parentOf(it.id) != null }.toList()
        assertTrue(removable.isNotEmpty())
        removable.forEach { element ->
            val removed = UiEditCommand.RemoveElement(element.id).apply(document)
            val written = UiScriptWriter.write(removed)
            val elementText = UiScriptWriter.write(element)
            assertTrue("<${element.tag}#${element.id}> is gone", elementText !in written || source.indexOf(elementText) != source.lastIndexOf(elementText))
            assertTrue("the rest of the script is kept", written.length < source.length && written.lines().size < source.lines().size)
            reader.read(written)
        }
    }

    @Test
    fun `inserting and removing through the history restores the original bytes`() {
        samples().forEach { (name, script) ->
            val source = normalized(script)
            val history = UiEditHistory(reader.read(source))
            val target = history.document.root!!
            val button = XmlNode.Element(0, "button", listOf(io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr.literal("id", "added"), io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr.literal("text", "Added")))
            assertTrue(history.execute(UiEditCommand.InsertElement(target.id, 0, button)))
            val inserted = UiScriptWriter.write(history.document)
            assertNotEquals(source, inserted)
            val rereadInserted = reader.read(inserted)
            assertEquals("$name: the inserted element is the first child", "button", rereadInserted.root!!.elementChildren.first().tag)
            assertEquals("$name: the insertion keeps the following lines", source.lines().size + (if (target.selfClosing) 2 else 1), inserted.lines().size)
            assertTrue(history.undo())
            assertEquals("$name: undo restores the bytes", source, UiScriptWriter.write(history.document))
            assertTrue(history.redo())
            assertEquals(inserted, UiScriptWriter.write(history.document))
        }
    }

    @Test
    fun `moving an element keeps its text and re-indents it`() {
        val source = normalized(samples().getValue("login.js"))
        val document = reader.read(source)
        val root = document.root!!
        val vertical = root.elementChildren.first()
        val lastRow = vertical.elementChildren.last()
        val moved = UiEditCommand.MoveElement(lastRow.id, root.id, 0).apply(document)
        assertEquals("the moved row leads the frame", lastRow.id, moved.root!!.elementChildren.first().id)
        val written = UiScriptWriter.write(moved)
        val reread = reader.read(written)
        assertEquals("the moved row reads back first", lastRow.tag, reread.root!!.elementChildren.first().tag)
        assertEquals("line count is unchanged", source.lines().size, written.lines().size)
        val movedText = UiScriptWriter.write(moved.element(lastRow.id)!!)
        assertTrue("inner lines follow the new indentation", movedText.lines().drop(1).all { it.startsWith("            ") || it.isBlank() })
    }

    @Test
    fun `host samples read when AUTOJS6_HOST_DIR points at a checkout`() {
        val host = System.getenv("AUTOJS6_HOST_DIR") ?: return
        val sampleRoot = Paths.get(host, "app/src/main/assets-app/sample")
        if (!Files.isDirectory(sampleRoot)) return
        val directive = Regex("""^\s*(?:(?://[^\n]*\n|/\*.*?\*/)\s*)*(['"])ui\1\s*;""", RegexOption.DOT_MATCHES_ALL)
        var read = 0
        Files.walk(sampleRoot).use { stream ->
            stream.filter { it.toString().endsWith(".js") }.forEach { path ->
                val source = normalized(Files.readString(path))
                if (!directive.containsMatchIn(source) || !LITERAL_CALL.containsMatchIn(source)) return@forEach
                val document = reader.read(source)
                assertEquals("$path is written byte for byte", source, UiScriptWriter.write(document))
                read++
            }
        }
        assertTrue("expected at least 40 host samples, read $read", read >= 40)
    }
}
