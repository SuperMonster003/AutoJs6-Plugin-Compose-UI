package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptText
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.ImageLookup
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.ImageSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.OverlayKind
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
        assertEquals(ScriptText.isolate("'Page ' + (page.value + 1) + ' of 3'"), (status.props["text"] as UiValue.Str).value)

        val lazy = PreviewFrames.build(reader.read(GalleryCatalog.entry("LazyColumn")!!.script()), 2)
        val root = lazy.frame!!.nodes.getValue(lazy.frame.rootId)
        assertEquals(1, root.children.size)
        val placeholder = lazy.frame.nodes.getValue(root.children.single())
        assertEquals("Text", placeholder.type)
        assertEquals(ScriptText.isolate("{rows}"), (placeholder.props["text"] as UiValue.Str).value)
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

    @Test
    fun `an open window component reaches the renderer closed and is listed as an overlay`() {
        val document = reader.read("compose.mount(compose.Column({},\n    compose.ModalBottomSheet({key: 'sheet', open: true},\n        compose.Text({text: 'In the sheet'})),\n    compose.ModalBottomSheet({key: 'closed', open: false},\n        compose.Text({text: 'Not shown'}))));\n")
        val preview = PreviewFrames.build(document, 1)
        val frame = preview.frame
        assertNotNull(preview.problems.toString(), frame)
        frame!!
        val sheets = document.root!!.children.map { (it as DesignChild.Node).node }
        // Both sheets stay in the closed contract tree, so the renderer opens no window...
        sheets.forEach { sheet -> assertEquals(UiValue.Bool(false), frame.nodes.getValue(sheet.id).props["open"]) }
        // ...and only the open one is drawn over the page, with its content node.
        val overlay = preview.overlays.single()
        assertEquals(sheets[0].id, overlay.ownerId)
        assertEquals(OverlayKind.SHEET, overlay.kind)
        assertEquals("In the sheet", (frame.nodes.getValue(overlay.children.single()).props["text"] as UiValue.Str).value)
        assertEquals(0, frame.nodes.values.count { it.type == "Card" })
    }

    @Test
    fun `a popup opened inside another one is drawn above it`() {
        val document = reader.read("compose.mount(compose.Column({},\n    compose.ModalBottomSheet({key: 'sheet', open: true},\n        compose.DropdownMenu({key: 'menu', open: true, anchor: compose.Button({}, 'More')},\n            compose.DropdownMenuItem({key: 'edit', text: 'Edit'}))),\n    compose.AlertDialog({key: 'later', open: true, title: 'Later'})));\n")
        val preview = PreviewFrames.build(document, 1)
        assertNotNull(preview.problems.toString(), preview.frame)
        assertEquals(listOf(OverlayKind.SHEET, OverlayKind.MENU, OverlayKind.DIALOG), preview.overlays.map { it.kind })
    }

    @Test
    fun `an AndroidView stands in with its size and its view expression`() {
        val document = reader.read("compose.mount(compose.Column({},\n    compose.AndroidView({key: 'native', h: 120, view: ui.inflate('<text text=\"x\"/>')})));\n")
        val androidView = (document.root!!.children.single() as DesignChild.Node).node
        val preview = PreviewFrames.build(document, 1)
        val frame = preview.frame
        assertNotNull(preview.problems.toString(), frame)
        frame!!
        val standIn = frame.nodes.values.single { it.type == "Surface" }
        assertEquals(androidView.id, preview.placeholders[standIn.nodeId])
        assertTrue(standIn.modifier.map { it.name }.toString(), standIn.modifier.any { it.name == "height" })
        val label = frame.nodes.getValue(standIn.children.single())
        assertEquals("[AndroidView] " + ScriptText.isolate("ui.inflate('<text text=\"x\"/>')"), (label.props["text"] as UiValue.Str).value)
        assertEquals("the label keeps off the stand-in's edges", listOf("padding"), label.modifier.map { it.name })
        assertTrue(preview.problems.toString(), preview.problems.any { it.contains("AndroidView") })
    }

    @Test
    fun `literal image sources go to the image source and its reason becomes a problem`() {
        val asked = ArrayList<String>()
        val images = ImageSource { src -> asked += src; ImageLookup.Missing("Image: $src is missing here") }
        val document = reader.read("compose.mount(compose.Column({},\n    compose.Image({src: 'pics/logo.png', w: 48, h: 48}),\n    compose.Image({src: picture})));\n")
        val preview = PreviewFrames.build(document, 1, images = images)
        // Only the literal source is looked up; the expression stays a placeholder as before.
        assertEquals(listOf("pics/logo.png"), asked)
        assertTrue(preview.problems.toString(), "Image: pics/logo.png is missing here" in preview.problems)
        assertEquals(0, preview.frame!!.nodes.values.count { it.type == "Image" })
    }
}
