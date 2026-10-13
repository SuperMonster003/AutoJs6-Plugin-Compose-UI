package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.DocumentWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.TsxWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostKind
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostOptions
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierArg
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Segment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsxReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Roadmap P9.2 / D42: TSX documents read from the TypeScript Engine parser's trees (snapshots, see
 * [TsxSnapshots]) map to the same documents as their JavaScript forms, the TSX writer's output reads back into
 * an equal document, and writing it again is byte-identical.
 */
class TsxRoundTripTest {
    private val snapshots = TsxSnapshots()
    private val reader = TsxReader()

    private fun read(name: String): DesignDocument = reader.read(snapshots.source(name), snapshots.tree(name))

    /** Reads a hand-written source, checks its canonical snapshot is the writer's output, and that it round trips. */
    private fun assertCanonicalRoundTrip(name: String): DesignDocument {
        val document = read(name)
        assertEquals(Dialect.TSX, document.dialect)
        val written = TsxWriter.write(document)
        val canonical = "canonical/$name"
        assertEquals("$name: the canonical snapshot is stale; run .python/generate_tsx_snapshots.py", snapshots.source(canonical), written)
        val reread = read(canonical)
        assertEquals("$name: the written TSX must read into the same document", document, reread)
        assertEquals("$name: writing again must be a fixed point", written, TsxWriter.write(reread))
        assertEquals(written, DocumentWriter.write(reread))
        return document
    }

    @Test
    fun `the trees come from the TypeScript Engine parser`() {
        assertTrue(snapshots.producer, snapshots.producer.startsWith("typescript 6.0.3; TypeScript Engine 0.7.0"))
        assertTrue("every source has a current tree", snapshots.entries.keys.all { snapshots.isCurrent(it) })
    }

    @Test
    fun `the TSX forms of all 56 gallery scripts map to their JavaScript documents`() {
        val gallery = TsxSnapshots.gallery()
        assertEquals(56, gallery.size)
        assertEquals(gallery.keys.map { "gallery/$it.tsx" }.sorted(), snapshots.sources("gallery"))
        gallery.forEach { (component, script) ->
            val name = "gallery/$component.tsx"
            val expected = ScriptReader().read(script).copy(dialect = Dialect.TSX)
            assertEquals("$name is stale; run .python/generate_tsx_snapshots.py", TsxWriter.write(expected), snapshots.source(name))
            val document = read(name)
            assertEquals("$name maps to the document of the JavaScript script", expected, document)
            assertEquals("$name is a fixed point", snapshots.source(name), TsxWriter.write(document))
        }
    }

    @Test
    fun `the TSX forms of the five bundled examples map to their JavaScript documents`() {
        val examples = TsxSnapshots.examples()
        assertEquals(5, examples.size)
        examples.forEach { (file, script) ->
            val name = "examples/" + file.removeSuffix(".js") + ".tsx"
            val expected = ScriptReader().read(script).copy(dialect = Dialect.TSX)
            assertEquals("$name is stale", TsxWriter.write(expected), snapshots.source(name))
            assertEquals(name, expected, read(name))
        }
    }

    @Test
    fun `the new project template is canonical TSX`() {
        val name = TsxSnapshotRecorder.TEMPLATE
        assertEquals(ProjectStore.TSX_TEMPLATE, snapshots.source(name))
        val document = read(name)
        assertEquals(ProjectStore.TSX_TEMPLATE, TsxWriter.write(document))
        val js = ScriptReader().read("\"ui\";\n\ncompose.mount(function () {\n    return compose.Column({padding: 16, spacing: 12},\n        compose.Text({key: 'title', text: 'Hello', style: 'headlineSmall'}));\n});\n")
        assertEquals("the TSX template shows the JavaScript template's interface", js.root, document.root)
        assertTrue(document.mount!!.renderArrow)
    }

    @Test
    fun `seven host TSX samples map and round trip, the three without a top-level mount are refused`() {
        val mapped = listOf("counter", "dialog", "floaty", "fragment-boundaries", "memo", "mixed-render", "wide-components")
        val refused = listOf("interop", "legacy-xml", "non-ui")
        assertEquals((mapped + refused).map { "host/$it.tsx" }.sorted(), snapshots.sources("host"))
        mapped.forEach { assertCanonicalRoundTrip("host/$it.tsx") }
        refused.forEach { sample ->
            try {
                read("host/$sample.tsx")
                fail("host/$sample.tsx should be refused")
            } catch (error: DesignerParseError) {
                assertTrue(error.reason, error.reason.contains("no compose.mount"))
            }
        }

        val counter = read("host/counter.tsx")
        assertEquals(listOf("count"), counter.states.map { it.name })
        val column = counter.root!!
        assertEquals("Column", column.type)
        // <><compose:Column>...</compose:Column></> is the root; the nested fragment of two Texts is flattened.
        assertEquals(listOf("TopAppBar", "Text", "Text"), column.children.mapNotNull { (it as? DesignChild.Node)?.node?.type })
        val title = (column.children[0] as DesignChild.Node).node.slot("title") as DesignChild.Node
        assertEquals(PropValue.str("TSX slot title"), title.node.prop("text"))
        assertEquals(PropValue.Expression("'Count: ' + count.value"), (column.children[1] as DesignChild.Node).node.prop("text"))
        // The alias tag <Increment> is kept as written.
        val alias = column.children[3] as DesignChild.Expression
        assertTrue(alias.source.startsWith("<Increment key=\"button\"") && alias.source.endsWith("</Increment>"))

        val dialog = read("host/dialog.tsx")
        val mount = dialog.mount!!
        assertEquals(HostKind.DIALOG, mount.kind)
        assertEquals("const" to "session", mount.bindKeyword to mount.bindName)
        assertTrue("an expression-bodied arrow is a render function", mount.render && mount.renderArrow && mount.renderPrelude == null)
        assertEquals(listOf("type", "cancelable", "theme"), (mount.options as HostOptions.Fields).fields.map { it.first })
        val fragment = read("host/fragment-boundaries.tsx").mount!!
        assertEquals("compose.mount(<>...</>) passes the element itself", false, fragment.render)
        assertEquals(listOf("Text", "OutlinedTextField", "OutlinedTextField"), fragment.root.children.map { (it as DesignChild.Node).node.type })
        val close = dialog.nodes().first { it.prop("testTag") == PropValue.str("tsx-dialog-close") }
        assertEquals("session.close()", close.handler("onClick")!!.body)
        assertTrue(close.handler("onClick")!!.expressionBody)

        val memo = read("host/memo.tsx")
        assertTrue(memo.mount!!.renderPrelude!!.startsWith("const children = items.value.map(item => compose.memo(() => {"))
        assertEquals(listOf(DesignChild.Expression("children")), memo.nodes().first { it.type == "LazyColumn" }.children)
    }

    @Test
    fun `JSX text takes the value the TypeScript JSX transform gives it`() {
        val document = assertCanonicalRoundTrip("cases/text.tsx")
        val rows = document.root!!.children.map { (it as DesignChild.Node).node }
        fun texts(node: DesignNode): List<String> = node.children.map { child ->
            when (child) {
                is DesignChild.Node -> ((child.node.prop("text") as PropValue.Literal).value as JsLiteral.Str).value.also { assertTrue(child.node.shorthand) }
                is DesignChild.Expression -> "{" + child.source + "}"
            }
        }
        assertEquals(listOf("Save"), texts(rows[0]))
        assertEquals(listOf("Two words on two lines"), texts(rows[1]))
        assertEquals(listOf("Fish & chips \u00A9 \u00A9 A &unknown;"), texts(rows[2]))
        assertEquals(listOf(" padded "), texts(rows[3]))
        assertEquals(listOf("Count: ", "{count}"), texts(rows[4]))
        assertEquals(listOf("quoted", "double"), texts(rows[5]))
        assertEquals(listOf("it's {braced} <tagged>"), texts(rows[6]))
    }

    @Test
    fun `attributes become keys, literals, bindings, slots, handlers and modifier chains`() {
        val document = assertCanonicalRoundTrip("cases/attributes.tsx")
        assertEquals(listOf("number", "string | null", null), document.states.map { it.typeArguments })
        assertTrue("an annotated variable stays opaque", document.segments.any { it is Segment.Opaque && it.source == "const total: number = 3;" })
        val column = document.root!!
        assertEquals("compose:Column is a Column", "Column", column.type)
        assertEquals(PropValue.Literal(JsLiteral.Num(-2.0, "-2")), column.prop("spacing"))
        assertEquals(PropValue.Literal(JsLiteral.Num(1000.0, "1e3")), column.prop("weight"))
        assertEquals(PropValue.Literal(JsLiteral.Num(16.0, "0x10")), column.prop("w"))
        assertEquals("a bare attribute is true", PropValue.bool(true), column.prop("enabled"))
        val nodes = column.children.map { (it as DesignChild.Node).node }
        assertEquals(PropValue.Literal(JsLiteral.Str("first")), nodes[0].key)
        assertEquals("entities in a quoted attribute are decoded", PropValue.str("double & quoted"), nodes[0].prop("text"))
        assertEquals(PropValue.str("titleMedium"), nodes[0].prop("style"))
        assertEquals("a quoted attribute keeps its backslash", PropValue.str("back\\slash"), nodes[1].prop("text"))
        assertEquals(PropValue.Literal(JsLiteral.Arr(listOf(JsLiteral.Num(1.0, "1"), JsLiteral.Num(2.0, "2")))), nodes[1].prop("color"))
        assertEquals(PropValue.StateRef("page"), nodes[2].prop("maxLines"))
        val titles = nodes.subList(3, 6).map { it.slot("title") as DesignChild.Node }
        assertTrue(titles[0].node.shorthand)
        assertEquals(listOf("Plain title", "Element title", "Fragment title"), titles.map { (it.node.prop("text") as PropValue.Literal).value.let { value -> (value as JsLiteral.Str).value } })
        assertEquals(DesignChild.Expression("<></>"), nodes[6].slot("label"))
        assertEquals("TopAppBar", (nodes[7].slot("topBar") as DesignChild.Node).node.type)
        assertEquals(3, nodes[8].children.size)
        assertEquals(DesignChild.Expression("label.value"), nodes[8].children[2])
        val handler = nodes[9].handler("onCheckedChange")!!
        assertEquals(listOf("value: boolean"), handler.params)
        assertEquals("checked.value = value;", handler.body)
        val chain = nodes[10].modifiers!!
        assertEquals(listOf("fillMaxWidth", "clickable", "padding"), chain.map { it.name })
        assertTrue(chain[1].args.single() is ModifierArg.Callback)
        assertEquals(NodeEntry.ModifierExpression("base.then(compose.modifier().padding(4))"), nodes[11].entries.single())
        assertEquals(HostOptions.Fields(listOf("theme" to PropValue.Literal(JsLiteral.Obj(listOf("dark" to JsLiteral.Bool(false)))))), document.mount!!.options)
    }

    @Test
    fun `expressions, aliases and handlers the model cannot keep stay verbatim`() {
        val document = assertCanonicalRoundTrip("cases/expressions.tsx")
        val mount = document.mount!!
        assertEquals(false, mount.renderArrow)
        assertEquals("const visible = items.value.length > 0;", mount.renderPrelude)
        val children = document.root!!.children
        assertEquals(DesignChild.Expression("visible && <compose.Text text='Visible' />"), children[0])
        assertEquals(DesignChild.Expression("items.value.map(item => <compose.Text key={item} text={item} />)"), children[1])
        assertEquals(DesignChild.Expression("/* A comment child stays where it is. */"), children[2])
        assertEquals(DesignChild.Expression("...items.value"), children[3])
        assertEquals(DesignChild.Expression("<Increment onClick={() => items.value = []}>Clear</Increment>"), children[4])
        val buttons = children.subList(5, 10).map { (it as DesignChild.Node).node }
        assertEquals(PropValue.Expression("<T,>(value: T) => value"), buttons[0].prop("onClick"))
        assertEquals(PropValue.Expression("async () => { await Promise.resolve(); }"), buttons[1].prop("onClick"))
        assertEquals(PropValue.Expression("function named() { return 1; }"), buttons[2].prop("onClick"))
        assertEquals(PropValue.Expression("(event: unknown): void => {}"), buttons[3].prop("onClick"))
        val typed = buttons[4].handler("onLongClick")!!
        assertEquals(listOf("page: number"), typed.params)
        assertEquals("items.value = items.value.slice(page);\nconsole.log(page as number);", typed.body)
        assertEquals("a braced element is an element", "Text", (children[10] as DesignChild.Node).node.type)
    }
}
