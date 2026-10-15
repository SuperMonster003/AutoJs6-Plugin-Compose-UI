package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.XmlPreviewFrames
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.XmlPreviewValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Roadmap P10.3: every bundled ui sample yields a preview request whose XML is the fragment as written (no E4X
 * expressions remain), whose element id table follows the preorder the host numbers elements in, and whose binding
 * context carries only the declarations the `{{ }}` expressions name; `{expr}` fragments are replaced by their
 * values or noted.
 */
class XmlPreviewFramesTest {
    private val reader = UiScriptReader()
    private val root: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }

    private fun samples(): Map<String, String> = Files.list(root.resolve("app/src/test/resources/ui-samples")).use { stream ->
        stream.filter { it.toString().endsWith(".js") }.toList().associate { it.fileName.toString() to Files.readString(it).replace("\r\n", "\n") }
    }

    @Test
    fun `every bundled sample gives a request in the host's element order`() {
        samples().forEach { (name, script) ->
            val document = reader.read(script)
            val request = XmlPreviewFrames.build(document)
            val fragment = document.fragments[document.rootFragment]
            assertEquals("$name: ids follow preorder", fragment.root.descendants().map { it.id }.toList(), request.elementIds)
            assertEquals("$name: the root is element 0", fragment.root.id, request.elementIdAt(0))
            assertEquals("$name: every element maps back", request.elementIds.size, request.elementIds.map { request.indexOf(it) }.toSet().size)
            assertFalse("$name: no E4X expression remains", request.xml.contains(Regex("""(?<!\{)\{(?!\{)[^{}]*\}(?!\})""")))
            assertTrue("$name: the XML starts with the root tag", request.xml.startsWith("<" + fragment.root.tag))
            assertTrue("$name: the binding context is a JSON object", JsonText.parse(request.bindingsJson) is Map<*, *>)
            val hasExpressions = fragment.root.descendants().any { element -> element.attrs.any { it.hasExpr } || element.children.any { it is XmlNode.Expr } }
            if (!hasExpressions) {
                assertEquals("$name: without expressions the XML is the fragment as written", UiScriptWriter.write(fragment.root), request.xml)
            }
        }
    }

    @Test
    fun `binding identifiers come from the expressions and resolve against declarations and overrides`() {
        val document = reader.read(
            "\"ui\";\nvar title = 'Hi';\nvar count = 2;\nui.layout(<vertical><text text=\"{{title}} x {{count + 1}}\"/><text text=\"{{other.name}} {{String(count)}}\"/></vertical>);\n",
        )
        val request = XmlPreviewFrames.build(document, values = XmlPreviewValues(declarations = mapOf("title" to "\"Hi\"", "count" to "2", "unused" to "1"), overrides = mapOf("count" to "7")))
        assertEquals(listOf("title", "count", "other", "String"), request.bindingNames)
        assertEquals("{\"title\":\"Hi\",\"count\":7}", request.bindingsJson)
        assertEquals(listOf("title", "count"), XmlPreviewFrames.identifiersOf("title + count.length"))
        assertEquals(listOf("a"), XmlPreviewFrames.identifiersOf("typeof a === 'undefined' ? null : a"))
    }

    @Test
    fun `expressions are replaced by their values or kept with a note`() {
        // This Rhino branch reports `{expr}` only in content position (P10.1); the attribute path shares the code.
        val document = reader.read(
            "\"ui\";\nvar msg = 'a & b';\nui.layout(<vertical><text text=\"{{msg}}\"/><text>{msg}</text><text>{unknown}</text></vertical>);\n",
        )
        val values = XmlPreviewValues(declarations = mapOf("msg" to "\"a & b\""), expressions = mapOf("msg" to "a & b"))
        val request = XmlPreviewFrames.build(document, values = values)
        assertTrue(request.xml, request.xml.contains("<text>a &amp; b</text>"))
        assertTrue(request.xml, request.xml.contains("<text><!-- {unknown} --></text>"))
        assertEquals(1, request.notes.size)
        assertEquals(4, request.notes[0].elementId)
        assertEquals(null, request.notes[0].attrName)
        assertEquals(4, request.elementIds.size)
        assertEquals("{\"msg\":\"a & b\"}", request.bindingsJson)

        val bare = XmlPreviewFrames.build(document)
        assertTrue(bare.xml, bare.xml.contains("<text><!-- {msg} --></text>"))
        assertEquals(listOf(3, 4), bare.notes.map { it.elementId })
        assertEquals("{}", bare.bindingsJson)
    }

    @Test
    fun `the second fragment of a script can be requested on its own`() {
        val document = reader.read(samples().getValue("login.js"))
        val second = XmlPreviewFrames.build(document, fragment = 1)
        assertEquals(document.fragments[1].root.descendants().map { it.id }.toList(), second.elementIds)
        assertTrue(second.elementIds.none { it in XmlPreviewFrames.build(document).elementIds })
    }
}
