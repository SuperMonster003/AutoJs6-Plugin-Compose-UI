package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.FragmentUsage
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Roadmap P10.1: scripts the ui reader cannot map are rejected with a line and column; the rest is accepted. */
class UiScriptReaderErrorTest {
    private val reader = UiScriptReader()

    private fun rejected(script: String): DesignerParseError =
        try { reader.read(script); fail("expected a rejection"); throw IllegalStateException() } catch (error: DesignerParseError) { error }

    @Test
    fun `scripts without the directive or without ui layout are rejected`() {
        assertTrue(rejected("ui.layout(<vertical/>);\n").reason.contains("directive"))
        assertTrue(rejected("\"ui\";\nvar x = <vertical/>;\n").reason.contains("no ui.layout"))
        assertTrue(rejected("var a = 1;\n\"ui\";\nui.layout(<vertical/>);\n").reason.contains("directive"))
    }

    @Test
    fun `comments may precede the directive`() {
        val document = reader.read("// header\n/* block */\n'ui';\nui.layout(<vertical/>);\n")
        assertEquals("vertical", document.root!!.tag)
        assertTrue(UiScriptReader.looksLikeUiScript("// header\n'ui';\nui.layout(<a/>);"))
        assertTrue(!UiScriptReader.looksLikeUiScript("'ui';\ncompose.mount(function () { return compose.Text('a'); });"))
    }

    @Test
    fun `ui layout arguments that are not literals are rejected with their position`() {
        val call = rejected("\"ui\";\nui.layout(build());\n")
        assertEquals(2, call.line)
        assertEquals(11, call.column)
        val string = rejected("\"ui\";\nui.layout('<vertical/>');\n")
        assertTrue(string.reason.contains("XML literal"))
        val two = rejected("\"ui\";\nui.layout(<a/>, 1);\n")
        assertTrue(two.reason.contains("exactly one"))
    }

    @Test
    fun `a variable must hold exactly one literal and never be reassigned`() {
        val missing = rejected("\"ui\";\nvar v = build();\nui.layout(v);\n")
        assertEquals(3, missing.line)
        assertTrue(missing.reason.contains("not initialized"))
        val twice = rejected("\"ui\";\nvar v = <a/>;\nif (x) { var v = <b/>; }\nui.layout(v);\n")
        assertTrue(twice.reason.contains("more than one"))
        val reassigned = rejected("\"ui\";\nvar v = <a/>;\nv = <b/>;\nui.layout(v);\n")
        assertTrue(reassigned.reason.contains("reassigned"))
        val document = reader.read("\"ui\";\nconst layout = <vertical><text text=\"hi\"/></vertical>;\nui.layout(layout);\n")
        assertEquals("layout", document.fragments[0].boundTo)
        assertEquals(FragmentUsage.LAYOUT, document.fragments[0].usage)
    }

    @Test
    fun `literal problems are positioned in the script`() {
        val mismatch = rejected("\"ui\";\nui.layout(\n    <vertical>\n        <text/>\n    </horizontal>\n);\n")
        assertEquals(5, mismatch.line)
        assertEquals(5, mismatch.column)
        val list = rejected("\"ui\";\nui.layout(<><a/></>);\n")
        assertTrue(list.reason.contains("XML lists"))
        val nested = rejected("\"ui\";\nui.layout(<a>{<b/>}</a>);\n")
        assertEquals(2, nested.line) // Rhino itself refuses a literal inside another literal's expression
    }

    @Test
    fun `syntax errors in the script are positioned`() {
        val error = rejected("\"ui\";\nui.layout(<a/>);\nvar = ;\n")
        assertEquals(3, error.line)
    }

    @Test
    fun `other literals become fragments and the first layout call wins`() {
        val document = reader.read("\"ui\";\nvar item = <text text=\"{{name}}\"/>;\nfunction show() {\n    ui.layout(<frame><list id=\"l\">{item}</list></frame>);\n}\nvar other = ui.inflate(<button text=\"b\"/>);\nshow();\n")
        assertEquals(3, document.fragments.size)
        assertEquals(1, document.rootFragment)
        assertEquals(listOf(FragmentUsage.OTHER, FragmentUsage.LAYOUT, FragmentUsage.OTHER), document.fragments.map { it.usage })
        assertEquals("item", document.fragments[0].boundTo)
        assertEquals("{item}", (document.root!!.elementChildren[0].children[0] as io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode.Expr).source)
    }
}
