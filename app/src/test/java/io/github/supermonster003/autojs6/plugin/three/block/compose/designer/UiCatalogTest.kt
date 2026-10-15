package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P10.1 / appendix C: the generated ui catalog is complete and consistent. */
class UiCatalogTest {
    private val catalog = UiCatalog.load()

    @Test
    fun `every tag has a view class whose chain reaches View`() {
        assertTrue("expected the documented tag table, got ${catalog.tags.size}", catalog.tags.size >= 45)
        catalog.tags.forEach { tag ->
            assertTrue("${tag.tag} class ${tag.viewClass} is known", catalog.knowsClass(tag.viewClass))
            assertEquals("${tag.tag} chain ends at View", "View", catalog.chain(tag.viewClass).last())
            assertTrue("${tag.tag} lists itself as an alias", tag.tag in tag.aliases)
            assertTrue("${tag.tag} has a category", tag.category.isNotBlank())
        }
    }

    @Test
    fun `aliases are unique across tags and attribute names are unique per class`() {
        val aliases = catalog.tags.flatMap { it.aliases }
        assertEquals(aliases.toSet().size, aliases.size)
        catalog.tags.map { it.viewClass }.distinct().forEach { viewClass ->
            val names = catalog.attrsFor(viewClass).map { it.name }
            assertEquals("$viewClass attribute names are unique", names.toSet().size, names.size)
        }
    }

    @Test
    fun `shorthands point at known attributes and common lookups work`() {
        catalog.shorthands.forEach { (short, full) ->
            assertNotNull("shorthand $short -> $full exists on TextView", catalog.attr("TextView", full))
        }
        val text = catalog.tag("text")!!
        assertEquals("JsTextView", text.viewClass)
        assertNotNull(catalog.attr(text.viewClass, "textSize"))
        assertNotNull("shorthand size", catalog.attr(text.viewClass, "size"))
        assertNotNull("alias w", catalog.attr(text.viewClass, "w"))
        assertNotNull("view attribute on a button", catalog.attr(catalog.tag("btn")!!.viewClass, "background"))
        assertNotNull("input attribute", catalog.attr(catalog.tag("input")!!.viewClass, "hint"))
        assertNotNull("list attribute", catalog.attr(catalog.tag("list")!!.viewClass, "orientation"))
        assertNull("grid attribute is not on list", catalog.attr(catalog.tag("list")!!.viewClass, "spanCount"))
        assertNotNull(catalog.attr(catalog.tag("grid")!!.viewClass, "spanCount"))
        assertNotNull("tabs attribute", catalog.attr(catalog.tag("tabs")!!.viewClass, "tabMode"))
        assertEquals(listOf("fixed", "scrollable"), catalog.attr(catalog.tag("tabs")!!.viewClass, "tabMode")!!.values)
        assertNotNull("video attribute", catalog.attr(catalog.tag("video")!!.viewClass, "videoPath"))
        assertNotNull("seekbar inherits progress attributes", catalog.attr(catalog.tag("seekbar")!!.viewClass, "max"))
    }

    @Test
    fun `unsupported and creation-only attributes are listed`() {
        assertTrue("onClick" in catalog.unsupported("View"))
        assertTrue("onClick" in catalog.unsupported(catalog.tag("button")!!.viewClass))
        assertTrue(catalog.creationOnly("button").any { "isBorderless" in it.attrs })
        assertTrue(catalog.creationOnly("spinner").any { "spinnerMode" in it.attrs })
        assertTrue("style applies to every tag", catalog.creationOnly("text").any { "style" in it.attrs })
    }

    @Test
    fun `the catalog records its documentation commit and value vocabularies`() {
        assertTrue(catalog.docsCommit.isNotBlank())
        assertTrue("center" in catalog.gravity)
        assertTrue("sp" in catalog.dimensionUnits)
    }
}
