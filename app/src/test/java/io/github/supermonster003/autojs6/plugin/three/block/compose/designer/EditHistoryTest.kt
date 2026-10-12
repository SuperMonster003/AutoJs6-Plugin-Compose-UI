package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.ScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditHistory
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HandlerSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostKind
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostOptions
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierArg
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Segment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.StateDecl
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap D40: every edit is a command, undo restores the exact previous document, merged edits form one step. */
class EditHistoryTest {
    private val reader = ScriptReader()
    private val script = """
        "ui";

        let count = compose.state(0);

        compose.mount(function () {
            return compose.Column({padding: 16, spacing: 8},
                compose.Text({key: 'title', text: 'Hello'}),
                compose.Button({key: 'add', onClick: function () { count.value++; }},
                    'Add'),
                compose.Row({key: 'row'},
                    compose.Text({key: 'a', text: 'A'}),
                    compose.Text({key: 'b', text: 'B'})));
        });
    """.trimIndent() + "\n"

    private fun history() = EditHistory(reader.read(script))
    private fun node(history: EditHistory, type: String, key: String) = history.document.nodes().first { it.type == type && it.key == PropValue.Literal(JsLiteral.Str(key)) }

    @Test
    fun `undo restores the previous document field by field and redo reapplies`() {
        val history = history()
        val initial = history.document
        val title = node(history, "Text", "title")
        assertTrue(history.execute(EditCommand.SetProp(title.id, "text", PropValue.str("Changed"))))
        assertNotEquals(initial, history.document)
        assertEquals(PropValue.str("Changed"), history.document.node(title.id)!!.prop("text"))
        assertTrue(history.undo())
        assertEquals(initial, history.document)
        assertFalse(history.canUndo)
        assertTrue(history.redo())
        assertEquals(PropValue.str("Changed"), history.document.node(title.id)!!.prop("text"))
        assertFalse(history.canRedo)
    }

    @Test
    fun `consecutive edits of one property merge into a single step`() {
        val history = history()
        val title = node(history, "Text", "title")
        history.execute(EditCommand.SetProp(title.id, "text", PropValue.str("H")))
        history.execute(EditCommand.SetProp(title.id, "text", PropValue.str("He")))
        history.execute(EditCommand.SetProp(title.id, "text", PropValue.str("Hel")))
        assertEquals(1, history.undoCount)
        history.execute(EditCommand.SetProp(title.id, "style", PropValue.str("titleLarge")))
        assertEquals(2, history.undoCount)
        history.seal()
        history.execute(EditCommand.SetProp(title.id, "style", PropValue.str("bodyLarge")))
        assertEquals("a sealed run starts a new step", 3, history.undoCount)
        history.undo(); history.undo(); history.undo()
        assertEquals(PropValue.str("Hello"), history.document.node(title.id)!!.prop("text"))
    }

    @Test
    fun `no-op commands record nothing`() {
        val history = history()
        val title = node(history, "Text", "title")
        assertFalse(history.execute(EditCommand.SetProp(title.id, "text", PropValue.str("Hello"))))
        assertFalse(history.execute(EditCommand.RemoveNode(history.document.root!!.id)))
        assertFalse(history.execute(EditCommand.SetProp(999, "text", PropValue.str("x"))))
        assertEquals(0, history.undoCount)
    }

    @Test
    fun `inserted subtrees receive fresh identities and the document stays consistent`() {
        val history = history()
        val root = history.document.root!!
        val before = history.document.nextId
        val subtree = DesignNode(0, "Card", children = listOf(DesignChild.Node(DesignNode.text(0, "Inside"))))
        assertTrue(history.execute(EditCommand.InsertChild(root.id, 1, DesignChild.Node(subtree))))
        val document = history.document
        val ids = document.nodes().map { it.id }.toList()
        assertEquals(ids.toSet().size, ids.size)
        assertEquals(before + 2, document.nextId)
        val card = (document.root!!.children[1] as DesignChild.Node).node
        assertEquals("Card", card.type)
        assertEquals(before, card.id)
        assertEquals(before + 1, (card.children.single() as DesignChild.Node).node.id)
        assertEquals(root.id, document.parentOf(card.id)!!.parentId)
        assertEquals(1, document.parentOf(card.id)!!.index)
        val written = ScriptWriter.write(document)
        assertTrue(written, written.contains("compose.Card({},\n            'Inside')"))
        val reread = reader.read(written)
        assertEquals("identities are renumbered on read, structure is kept", document.nodes().map { it.type }.toList(), reread.nodes().map { it.type }.toList())
        assertEquals(written, ScriptWriter.write(reread))
    }

    @Test
    fun `move, remove and slot commands keep the tree closed`() {
        val history = history()
        val root = history.document.root!!
        val row = node(history, "Row", "row")
        assertTrue(history.execute(EditCommand.MoveChild(root.id, 1, row.id, 0)))
        var current = history.document
        assertEquals(listOf("Text", "Row"), current.root!!.children.map { (it as DesignChild.Node).node.type })
        assertEquals(listOf("Button", "Text", "Text"), current.node(row.id)!!.children.map { (it as DesignChild.Node).node.type })
        assertFalse("a parent cannot move into its own subtree", history.execute(EditCommand.MoveChild(root.id, 1, node(history, "Text", "a").id, 0)))
        val button = node(history, "Button", "add")
        assertTrue(history.execute(EditCommand.RemoveNode(button.id)))
        current = history.document
        assertEquals(null, current.node(button.id))
        assertEquals(2, current.node(row.id)!!.children.size)
        assertTrue(history.execute(EditCommand.SetSlot(root.id, "content", DesignChild.Node(DesignNode.text(0, "Slot")))))
        current = history.document
        val slotNode = (current.root!!.slot("content") as DesignChild.Node).node
        assertEquals(current.nextId - 1, slotNode.id)
        assertEquals("content", current.parentOf(slotNode.id)!!.slotName)
        assertTrue(history.execute(EditCommand.SetSlot(root.id, "content", null)))
        assertEquals(null, history.document.root!!.slot("content"))
        while (history.undo()) Unit
        assertEquals(reader.read(script), history.document)
    }

    @Test
    fun `handlers, modifiers, keys, host options and states are editable`() {
        val history = history()
        val title = node(history, "Text", "title")
        assertTrue(history.execute(EditCommand.SetHandler(title.id, "onClick", HandlerSource(emptyList(), "toast('hi');"))))
        assertTrue(history.execute(EditCommand.SetModifiers(title.id, listOf(ModifierEntry("padding", listOf(ModifierArg.Value(PropValue.num(8.0)))), ModifierEntry("fillMaxWidth")))))
        assertTrue(history.execute(EditCommand.SetKey(title.id, PropValue.str("heading"))))
        assertTrue(history.execute(EditCommand.SetHost(HostKind.FLOATY, HostOptions.Fields(listOf("raw" to PropValue.bool(true))))))
        assertTrue(history.execute(EditCommand.SetStates(listOf(StateDecl("let", "count", PropValue.num(0.0)), StateDecl("let", "open", PropValue.bool(false))))))
        assertTrue(history.execute(EditCommand.SetRenderPrelude("let rows = [];")))
        val document = history.document
        val written = ScriptWriter.write(document)
        assertTrue(written, written.contains("let count = compose.state(0);\nlet open = compose.state(false);"))
        assertTrue(written, written.contains("compose.floaty(function () {\n    let rows = [];\n    return compose.Column("))
        assertTrue(written, written.contains("}, {raw: true});"))
        assertTrue(written, written.contains("compose.Text({key: 'heading', text: 'Hello', onClick: function () { toast('hi'); }, modifier: compose.modifier().padding(8).fillMaxWidth()})"))
        assertEquals(document, reader.read(written))
        assertEquals(listOf(Segment.Opaque::class, Segment.State::class, Segment.State::class, Segment.Mount::class), document.segments.map { it::class })
    }
}
