package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.PreviewFrames
import org.autojs.plugin.compose.api.model.UiValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap D34: every gallery document derives a renderable contract tree; the palette templates validate. */
class PreviewFramesTest {
    private val reader = ScriptReader()

    @Test
    fun `every gallery document becomes a closed contract tree`() {
        GalleryCatalog.entries.forEach { entry ->
            val document = reader.read(entry.script())
            val preview = PreviewFrames.build(document, 1)
            val frame = requireNotNull(preview.frame) { "${entry.component}: ${preview.problems}" }
            assertEquals(entry.component, document.root!!.id, frame.rootId)
            assertTrue("${entry.component} keeps every design node: ${preview.problems}", document.nodes().all { it.id in frame.nodes || it.type == "AndroidView" || it.id in preview.placeholders.values })
            assertEquals("${entry.component}: unexpected preview problems ${preview.problems}", 0, preview.problems.count { !it.contains("AndroidView") && it != "Image: src not previewable" })
        }
    }

    @Test
    fun `bindings use the initial state value and expressions become placeholders or text`() {
        val pager = PreviewFrames.build(reader.read(GalleryCatalog.entry("HorizontalPager")!!.script()), 1)
        val pagerNode = pager.frame!!.nodes.values.first { it.type == "HorizontalPager" }
        assertEquals(0.0, (pagerNode.props["page"] as UiValue.Num).value, 0.0)
        val status = pager.frame.nodes.values.first { it.type == "Text" && it.key == "status" }
        assertEquals("'Page ' + (page.value + 1) + ' of 3'", (status.props["text"] as UiValue.Str).value)

        val lazy = PreviewFrames.build(reader.read(GalleryCatalog.entry("LazyColumn")!!.script()), 2)
        val root = lazy.frame!!.nodes.getValue(lazy.frame.rootId)
        assertEquals(1, root.children.size)
        val placeholder = lazy.frame.nodes.getValue(root.children.single())
        assertEquals("Text", placeholder.type)
        assertEquals("{rows}", (placeholder.props["text"] as UiValue.Str).value)
        assertEquals(root.nodeId, lazy.placeholders[placeholder.nodeId])
    }

    @Test
    fun `unrenderable components and duplicate keys degrade instead of failing`() {
        val unknown = PreviewFrames.build(reader.read("compose.mount(compose.Column({}, compose.Widget({text: 'x'}), compose.Text({key: 'a', text: 'a'}), compose.Text({key: 'a', text: 'b'})));\n"), 1)
        assertNotNull(unknown.frame)
        assertTrue(unknown.problems.toString(), unknown.problems.any { it.contains("Widget") })
        assertTrue(unknown.problems.toString(), unknown.problems.any { it.contains("Duplicate") })
        assertEquals(3, unknown.frame!!.nodes.getValue(unknown.frame.rootId).children.size)
    }

    @Test
    fun `every palette template validates inside its natural parent`() {
        val validator = DesignValidator()
        NodeTemplates.insertable().forEach { type ->
            val template = NodeTemplates.create(type)
            val parentType = DesignValidator.EXPECTED_PARENT[type]
            val root = if (parentType != null) NodeTemplates.create(parentType).let { parent -> parent.copy(children = listOf(DesignChild.Node(template))) }
            else DesignNode(0, "Column", children = listOf(DesignChild.Node(template)))
            val base = reader.read("compose.mount(compose.Column({}));\n")
            val document = EditCommand.ReplaceNode(base.root!!.id, root).apply(base).let { doc ->
                // ReplaceNode keeps the root id; renumber the inserted subtree through allocate for unique ids.
                val (child, next) = doc.allocate(DesignChild.Node(root))
                doc.withMount(doc.mount!!.copy(root = (child as DesignChild.Node).node)).copy(nextId = next)
            }
            val errors = validator.validate(document).filter { it.severity == Severity.ERROR }
            assertEquals("$type: $errors", 0, errors.size)
            val preview = PreviewFrames.build(document, 1)
            assertNotNull("$type: ${preview.problems}", preview.frame)
            assertEquals("$type: ${preview.problems}", 0, preview.problems.count { it != "Image: src not previewable" && !it.contains("AndroidView") })
        }
    }

    @Test
    fun `shortcut lists and chains reach the frame as modifier operations`() {
        val document: DesignDocument = reader.read("compose.mount(compose.Column({padding: [8, 16], modifier: compose.modifier().fillMaxWidth().background('#FF0000')}, compose.Text({text: 'x', weight: 1})));\n")
        val preview = PreviewFrames.build(document, 1)
        val root = preview.frame!!.nodes.getValue(preview.frame.rootId)
        assertEquals(listOf("padding", "fillMaxWidth", "background"), root.modifier.map { it.name })
        assertEquals(2, root.modifier[0].args.size)
        val text = preview.frame.nodes.getValue(root.children.single())
        assertEquals(listOf("weight"), text.modifier.map { it.name })
        assertEquals(PropValue.str("x"), document.root!!.let { (it.children.single() as DesignChild.Node).node.prop("text") })
    }
}
