package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditHistory
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P10.1 / D49: each command changes only what it is about, merges typing and undoes exactly. */
class UiEditHistoryTest {
    private val reader = UiScriptReader()
    private val script = "\"ui\";\nui.layout(\n    <vertical padding=\"16\">\n        <text id=\"title\" text=\"Hello\"  textSize='24sp'/>\n        <button id=\"ok\" text=\"OK\"/>\n    </vertical>\n);\nui.ok.on('click', () => toast('hi'));\n"

    private fun history() = UiEditHistory(reader.read(script))
    private fun text(history: UiEditHistory) = UiScriptWriter.write(history.document)

    @Test
    fun `setting an attribute keeps the others verbatim and merges consecutive edits`() {
        val history = history()
        val title = history.document.root!!.elementChildren[0]
        assertTrue(history.execute(UiEditCommand.SetAttr(title.id, "text", "H")))
        assertTrue(history.execute(UiEditCommand.SetAttr(title.id, "text", "Hi")))
        assertEquals(1, history.undoCount)
        assertEquals(script.replace("text=\"Hello\"", "text=\"Hi\""), text(history))
        assertTrue(history.execute(UiEditCommand.SetAttr(title.id, "textSize", "30sp")))
        assertEquals("the single quote is kept, the edited attribute is respaced", script.replace("text=\"Hello\"", "text=\"Hi\"").replace("  textSize='24sp'", " textSize='30sp'"), text(history))
        assertTrue(history.execute(UiEditCommand.SetAttr(title.id, "gravity", "center")))
        assertTrue(text(history).contains("textSize='30sp' gravity=\"center\"/>"))
        assertTrue(history.execute(UiEditCommand.SetAttr(title.id, "id", null)))
        assertTrue(text(history).contains("<text text=\"Hi\" textSize='30sp' gravity=\"center\"/>"))
        while (history.undo()) Unit
        assertEquals(script, text(history))
    }

    @Test
    fun `no-op commands record nothing`() {
        val history = history()
        val title = history.document.root!!.elementChildren[0]
        assertFalse(history.execute(UiEditCommand.SetAttr(title.id, "missing", null)))
        assertFalse(history.execute(UiEditCommand.SetTag(title.id, "text")))
        assertFalse(history.execute(UiEditCommand.RemoveElement(history.document.root!!.id)))
        assertFalse(history.canUndo)
    }

    @Test
    fun `bare attributes, tags and text content`() {
        val history = history()
        val root = history.document.root!!
        val button = root.elementChildren[1]
        assertTrue(history.execute(UiEditCommand.SetAttr(button.id, "enabled", null, bare = true)))
        assertTrue(text(history).contains("<button id=\"ok\" text=\"OK\" enabled/>"))
        assertTrue(history.execute(UiEditCommand.SetTag(button.id, "btn")))
        assertTrue(text(history).contains("<btn id=\"ok\" text=\"OK\" enabled/>"))
        assertTrue(history.execute(UiEditCommand.SetText(button.id, "Go")))
        assertTrue(text(history).contains("<btn id=\"ok\" text=\"OK\" enabled>Go</btn>"))
        assertTrue(history.execute(UiEditCommand.SetText(button.id, "")))
        assertTrue(text(history).contains("<btn id=\"ok\" text=\"OK\" enabled></btn>"))
        assertFalse("text content of a container is left alone", history.execute(UiEditCommand.SetText(root.id, "x")))
    }

    @Test
    fun `insert remove and move keep the indentation of the neighbours`() {
        val history = history()
        val root = history.document.root!!
        val card = XmlNode.Element(0, "card", listOf(XmlAttr.literal("w", "*")), listOf(XmlNode.Element(0, "text", listOf(XmlAttr.literal("text", "inner"))), XmlNode.Element(0, "button", listOf(XmlAttr.literal("text", "b")))))
        assertTrue(history.execute(UiEditCommand.InsertElement(root.id, 1, card)))
        val expectedCard = "        <card w=\"*\">\n            <text text=\"inner\"/>\n            <button text=\"b\"/>\n        </card>\n"
        assertEquals(script.replace("        <button id=\"ok\" text=\"OK\"/>\n", expectedCard + "        <button id=\"ok\" text=\"OK\"/>\n"), text(history))
        val cardId = history.document.root!!.elementChildren[1].id
        val ok = history.document.root!!.elementChildren[2]
        assertTrue(history.execute(UiEditCommand.MoveElement(ok.id, cardId, 0)))
        assertTrue(text(history).contains("        <card w=\"*\">\n            <button id=\"ok\" text=\"OK\"/>\n            <text text=\"inner\"/>\n"))
        assertTrue(history.execute(UiEditCommand.RemoveElement(cardId)))
        assertEquals(script.replace("        <button id=\"ok\" text=\"OK\"/>\n", ""), text(history))
        assertTrue(history.undo())
        assertTrue(history.undo())
        assertTrue(history.undo())
        assertEquals(script, text(history))
        assertTrue(history.redo())
        assertTrue(text(history).contains(expectedCard))
    }

    @Test
    fun `inserting into a self-closing element opens it`() {
        val history = UiEditHistory(reader.read("\"ui\";\nui.layout(<frame id=\"f\"/>);\n"))
        val root = history.document.root!!
        assertTrue(history.execute(UiEditCommand.InsertElement(root.id, 0, XmlNode.Element(0, "text", listOf(XmlAttr.literal("text", "a"))))))
        assertEquals("\"ui\";\nui.layout(<frame id=\"f\">\n    <text text=\"a\"/>\n</frame>);\n", text(history))
    }

    @Test
    fun `a move into its own subtree is refused`() {
        val history = history()
        val root = history.document.root!!
        val title = root.elementChildren[0]
        assertFalse(history.execute(UiEditCommand.MoveElement(root.id, title.id, 0)))
    }
}
