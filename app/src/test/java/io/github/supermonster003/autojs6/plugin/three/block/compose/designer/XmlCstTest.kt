package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.AttrPart
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlEntities
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.XmlCst
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.XmlCstError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Roadmap P10.1: the literal parser keeps every byte and reports positions. */
class XmlCstTest {
    private fun parse(text: String, exprs: List<XmlCst.ExprRange> = emptyList()): XmlNode.Element {
        var next = 1
        return XmlCst.parse(text, exprs) { next++ }
    }

    private fun error(text: String, exprs: List<XmlCst.ExprRange> = emptyList()): XmlCstError =
        try { parse(text, exprs); fail("expected a parse error for $text"); throw IllegalStateException() } catch (error: XmlCstError) { error }

    @Test
    fun `attributes keep their whitespace, order and quotes`() {
        val text = "<text  id='a'\n    text=\"b\"  size=\"16sp\" />"
        val element = parse(text)
        assertEquals(listOf("id", "text", "size"), element.attrs.map { it.name })
        assertEquals("  id='a'", element.attrs[0].source)
        assertEquals('\'', element.attrs[0].quote)
        assertEquals("\n    text=\"b\"", element.attrs[1].source)
        assertEquals(" /", element.startTagTail)
        assertTrue(element.selfClosing)
        assertEquals(text, UiScriptWriter.write(element))
    }

    @Test
    fun `bare attributes, comments, cdata, instructions and entities are kept`() {
        val text = "<vertical><console global/><!-- c --><![CDATA[x<y]]><?pi hint?><text>a &amp; b</text></vertical>"
        val element = parse(text)
        val console = element.elementChildren[0]
        assertTrue(console.attr("global")!!.isBare)
        assertEquals(listOf(XmlNode.Element::class, XmlNode.Comment::class, XmlNode.CData::class, XmlNode.Instruction::class, XmlNode.Element::class), element.children.map { it::class })
        assertEquals("a &amp; b", element.elementChildren[1].textContent)
        assertEquals("a & b", XmlEntities.decode(element.elementChildren[1].textContent!!))
        assertEquals(text, UiScriptWriter.write(element))
    }

    @Test
    fun `bindings are split inside attribute values and expressions follow the rhino ranges`() {
        val text = "<v a=\"x {{m}} y {{n.z}}\">{msg}</v>"
        val exprStart = text.indexOf("{msg}")
        val element = parse(text, listOf(XmlCst.ExprRange(exprStart, exprStart + 5, false)))
        val parts = element.attr("a")!!.parts!!
        assertEquals(listOf(AttrPart.Text("x "), AttrPart.Binding("m"), AttrPart.Text(" y "), AttrPart.Binding("n.z")), parts)
        assertEquals(listOf(XmlNode.Expr("{msg}")), element.children)
        assertEquals(text, UiScriptWriter.write(element))
    }

    @Test
    fun `an expression inside an attribute value becomes an expr part`() {
        val text = "<v a=\"p {q} r\"/>"
        val start = text.indexOf("{q}")
        val element = parse(text, listOf(XmlCst.ExprRange(start, start + 3, true)))
        assertEquals(listOf(AttrPart.Text("p "), AttrPart.Expr("{q}"), AttrPart.Text(" r")), element.attr("a")!!.parts)
        assertEquals(text, UiScriptWriter.write(element))
    }

    @Test
    fun `ids follow preorder`() {
        val element = parse("<a><b><c/></b><d/></a>")
        assertEquals(listOf(1 to "a", 2 to "b", 3 to "c", 4 to "d"), element.descendants().map { it.id to it.tag }.toList())
    }

    @Test
    fun `errors carry the offset of the problem`() {
        assertEquals(0, error("<>a</>").offset)
        val mismatch = error("<a><b></a>")
        assertEquals(6, mismatch.offset)
        assertTrue(mismatch.reason.contains("</a>"))
        val unterminated = error("<a b=\"x")
        assertTrue(unterminated.reason.contains("Unterminated"))
        assertTrue(error("<a><b/>").reason.contains("Missing end tag"))
        assertEquals(5, error("<a b=x/>").offset)
        assertTrue(error("<a/><b/>").reason.contains("one root"))
        assertTrue(error("<a x=\"1\" x=\"2\"/>").reason.contains("Duplicate"))
    }

    @Test
    fun `generated attributes and tags render canonically`() {
        val element = XmlNode.Element(1, "button", listOf(XmlAttr.literal("id", "ok"), XmlAttr.literal("text", "say \"hi\"", '\''), XmlAttr.bare("global")))
        assertEquals("<button id=\"ok\" text='say \"hi\"' global/>", UiScriptWriter.write(element.copy(selfClosing = true, startTagTail = "/")))
        assertNull(element.startTagSource)
        assertEquals("a &amp; &lt;b&gt; &quot;c&quot;", XmlEntities.encodeAttribute("a & <b> \"c\"", '"'))
    }
}
