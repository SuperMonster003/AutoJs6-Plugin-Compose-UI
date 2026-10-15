package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEvents
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P10.4: the palette covers every catalog tag but `compose`, and each template inserts, validates and previews. */
class UiTemplatesTest {
    private val catalog = UiCatalog.load()
    private val validator = UiValidator(catalog)
    private val reader = UiScriptReader()

    @Test
    fun `the palette lists every tag once by category and leaves compose out`() {
        val categories = UiTemplates.categories(catalog)
        assertEquals(UiTemplates.CATEGORY_ORDER, categories.map { it.id })
        val tags = categories.flatMap { it.tags }
        assertEquals(tags.toSet().size, tags.size)
        assertEquals(catalog.tags.map { it.tag }.toSet() - "compose", tags.toSet())
        assertTrue("compose" !in tags)
    }

    @Test
    fun `every template inserts into a vertical layout without errors or unknown attributes`() {
        val base = reader.read("\"ui\";\nui.layout(\n    <vertical id=\"root\">\n        <text id=\"title\" text=\"Hello\"/>\n    </vertical>\n);\n")
        val rootId = base.root!!.id
        UiTemplates.insertable(catalog).forEach { tag ->
            val template = UiTemplates.create(tag)
            assertEquals(tag, template.tag)
            val inserted = UiEditCommand.InsertElement(rootId, 1, template).apply(base)
            val written = UiScriptWriter.write(inserted)
            assertTrue("$tag writes a start tag", written.contains("<$tag"))
            val diagnostics = validator.validate(inserted)
            assertTrue("$tag: ${diagnostics.filter { it.severity == Severity.ERROR }}", diagnostics.none { it.severity == Severity.ERROR })
            assertTrue("$tag: ${diagnostics.filter { "Unknown" in it.message }}", diagnostics.none { "Unknown" in it.message })
            // The inserted subtree has no sources and the reread one has; the text is what must be stable.
            assertEquals("$tag reads back", written, UiScriptWriter.write(reader.read(written)))
            assertEquals("$tag keeps its element count", inserted.elements().count(), reader.read(written).elements().count())
        }
    }

    @Test
    fun `events follow the catalog chains`() {
        fun names(tag: String) = UiEvents.forTag(catalog, catalog.tag(tag)).map { it.name }
        assertEquals(listOf("click", "long_click", "touch", "key"), names("text"))
        assertEquals(listOf("click", "long_click", "touch", "key", "check"), names("checkbox"))
        assertEquals(listOf("click", "long_click", "touch", "key", "check"), names("switch"))
        assertEquals(listOf("click", "long_click", "touch", "key", "item_click", "item_long_click", "item_bind"), names("list"))
        assertEquals(listOf("click", "long_click", "touch", "key", "draw"), names("canvas"))
        assertEquals(listOf("click", "long_click", "touch", "key"), UiEvents.forTag(catalog, null).map { it.name })
        assertEquals("\n\n    ui.ok.on('click', () => {\n        // TODO\n    });", UiEvents.stub("ok", UiEvents.common[0], "    "))
    }
}
