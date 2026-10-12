package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.ScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostKind
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostOptions
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Segment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Roadmap P8.2: every gallery example and every bundled example script parses, the written script parses
 * back into an equal document, and writing that document again is byte-identical (the designer's own
 * output is a fixed point of parse / write).
 */
class ScriptRoundTripTest {
    private val reader = ScriptReader()
    private val root: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }

    private fun examples(): Map<String, String> = Files.list(root.resolve("app/src/main/assets/examples")).use { stream ->
        stream.filter { it.toString().endsWith(".js") }.toList().associate { it.fileName.toString() to Files.readString(it) }
    }

    private fun assertRoundTrip(name: String, script: String): DesignDocument {
        val document = reader.read(script)
        val written = ScriptWriter.write(document)
        val reread = reader.read(written)
        assertEquals("$name: the written script must parse into the same document", document, reread)
        assertEquals("$name: writing again must be a fixed point", written, ScriptWriter.write(reread))
        assertTrue("$name keeps ASCII punctuation", written.all { it.code < 128 || Character.isLetterOrDigit(it) })
        return document
    }

    @Test
    fun `all 56 gallery scripts round trip`() {
        val entries = GalleryCatalog.entries
        assertEquals(56, entries.size)
        entries.forEach { entry ->
            val document = assertRoundTrip(entry.component, entry.script())
            val mount = requireNotNull(document.mount)
            assertEquals(entry.component, HostKind.MOUNT, mount.kind)
            assertTrue(entry.component, mount.render)
            if (entry.component == "Snackbar") assertEquals("session", mount.bindName) else assertEquals(null, mount.bindName)
            if (entry.component != "Snackbar") assertTrue("${entry.component} mentions its factory", document.nodes().any { it.type == entry.component })
        }
    }

    @Test
    fun `the five bundled examples round trip`() {
        val examples = examples()
        assertEquals(setOf("counter.js", "form.js", "list.js", "floaty-hud.js", "theme.js"), examples.keys)
        examples.forEach { (name, script) -> assertRoundTrip(name, script) }
    }

    @Test
    fun `gallery scripts written by the designer are already canonical`() {
        // The gallery renders with the same conventions, so most of its scripts are fixed points on the first write.
        val stable = GalleryCatalog.entries.count { entry -> ScriptWriter.write(reader.read(entry.script())) == entry.script() }
        assertTrue("expected most gallery scripts to be canonical, got $stable of 56", stable >= 40)
    }

    @Test
    fun `state reads become bindings and the rest stays literal or opaque`() {
        val pager = reader.read(GalleryCatalog.entry("HorizontalPager")!!.script())
        assertEquals(listOf("page"), pager.states.map { it.name })
        assertEquals("let", pager.states.single().keyword)
        assertEquals(PropValue.Literal(JsLiteral.Num(0.0, "0")), pager.states.single().initial)
        val pagerNode = pager.nodes().first { it.type == "HorizontalPager" }
        assertEquals(PropValue.StateRef("page"), pagerNode.prop("page"))
        assertEquals(PropValue.Literal(JsLiteral.Num(160.0, "160")), pagerNode.prop("h"))
        val handler = requireNotNull(pagerNode.handler("onPageChange"))
        assertEquals(listOf("index"), handler.params)
        assertEquals("page.value = index;", handler.body)
        val status = pager.nodes().first { it.type == "Text" && it.key == PropValue.Literal(JsLiteral.Str("status")) }
        assertEquals(PropValue.Expression("'Page ' + (page.value + 1) + ' of 3'"), status.prop("text"))
        assertEquals(3, pagerNode.children.size)
        val card = (pagerNode.children[0] as DesignChild.Node).node
        assertEquals("Card", card.type)
        val shorthand = (card.children.single() as DesignChild.Node).node
        assertTrue(shorthand.shorthand && shorthand.isPlainText)
        assertEquals(PropValue.Literal(JsLiteral.Str("First page")), shorthand.prop("text"))
    }

    @Test
    fun `slots, lists, shapes, modifier chains and helper statements are represented`() {
        val scaffold = reader.read(GalleryCatalog.entry("Scaffold")!!.script())
        val topBar = scaffold.root!!.slot("topBar") as DesignChild.Node
        assertEquals("TopAppBar", topBar.node.type)
        assertEquals("Text", (topBar.node.slot("title") as DesignChild.Node).node.type)

        val slider = reader.read(GalleryCatalog.entry("Slider")!!.script()).nodes().first { it.type == "Slider" }
        assertEquals(PropValue.Literal(JsLiteral.Arr(listOf(JsLiteral.Num(0.0, "0"), JsLiteral.Num(100.0, "100")))), slider.prop("range"))

        val lazy = reader.read(GalleryCatalog.entry("LazyColumn")!!.script())
        assertTrue("the helper loop stays opaque", lazy.segments.filterIsInstance<Segment.Opaque>().any { it.source.contains("rows.push(") })
        assertEquals(listOf(DesignChild.Expression("rows")), lazy.root!!.children)

        val form = reader.read(examples().getValue("form.js"))
        val column = form.nodes().first { it.type == "Column" }
        val chain = requireNotNull(column.modifiers)
        assertEquals(listOf("fillMaxSize", "verticalScroll", "padding"), chain.map { it.name })
        assertEquals(PropValue.Literal(JsLiteral.Num(20.0, "20")), (chain[2].args.single() as io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierArg.Value).value)
        assertTrue("node handles referenced by name stay opaque children", column.children.any { it == DesignChild.Expression("nameField") })
        assertEquals(false, form.mount!!.render)

        val hud = reader.read(examples().getValue("floaty-hud.js"))
        val mount = hud.mount!!
        assertEquals(HostKind.FLOATY, mount.kind)
        assertEquals("var" to "hud", mount.bindKeyword to mount.bindName)
        val options = mount.options as HostOptions.Fields
        assertEquals(listOf("raw", "x", "y", "width", "height"), options.fields.map { it.first })
        assertEquals(PropValue.Literal(JsLiteral.Bool(false)), options.fields[0].second)
        assertEquals(PropValue.Expression("margin"), options.fields[1].second)
        assertEquals(PropValue.Literal(JsLiteral.Num(-2.0, "-2")), options.fields[4].second)
        val surface = hud.root!!
        assertEquals(PropValue.Literal(JsLiteral.Obj(listOf("shape" to JsLiteral.Str("rounded"), "radius" to JsLiteral.Num(12.0, "12")))), surface.prop("shape"))

        val list = reader.read(examples().getValue("list.js"))
        assertTrue("render statements before return are kept", list.mount!!.renderPrelude!!.startsWith("var rows = [];"))
        assertTrue(list.mount!!.renderPrelude!!.contains("rows.push("))
        assertEquals(PropValue.Expression("listRef"), list.nodes().first { it.type == "LazyColumn" }.prop("ref"))
    }

    @Test
    fun `node identities are unique and allocated in document order`() {
        val document = reader.read(GalleryCatalog.entry("TopAppBar")!!.script())
        val ids = document.nodes().map { it.id }.toList()
        assertEquals(ids.sorted(), ids)
        assertEquals(ids.toSet().size, ids.size)
        assertEquals(ids.max() + 1, document.nextId)
        ids.forEach { id -> assertEquals(id, document.node(id)!!.id) }
        val entries = document.root!!.entries
        assertTrue(entries.any { it is NodeEntry.Slot && it.name == "topBar" })
    }
}
