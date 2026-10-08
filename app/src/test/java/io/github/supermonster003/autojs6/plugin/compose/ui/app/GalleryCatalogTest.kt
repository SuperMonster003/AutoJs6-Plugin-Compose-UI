package io.github.supermonster003.autojs6.plugin.compose.ui.app

import org.autojs.plugin.compose.api.catalog.ChildrenPolicy
import org.autojs.plugin.compose.api.catalog.ComponentInvocation
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gallery documents every catalog component with a script that the real host accepts: each node uses
 * only properties, aliases, shortcuts, slots and events the contract catalog declares for that component,
 * respects its children policy and supplies required slots.
 */
class GalleryCatalogTest {
    private val catalog = ComposeUiV2.interopCatalog
    private val universal = setOf("key", "testTag", "contentDescription", "modifier", "ref")
    private val shortcuts = catalog.shortcuts.map { it.name }.toSet()

    @Test
    fun `every node component and the snackbar command have exactly one gallery entry`() {
        val expected = catalog.components.filter { it.invocation == ComponentInvocation.NODE }.map { it.name }.toSet() + "Snackbar"
        val actual = GalleryCatalog.entries.map { it.component }
        assertEquals(actual.toSet(), actual.toSet().also { assertEquals("no duplicate entries", actual.size, it.size) })
        assertEquals(expected, actual.toSet())
        assertTrue("categories are non-empty", GalleryCatalog.categories.all { it.entries.isNotEmpty() })
        assertEquals(catalog.components.size, GalleryCatalog.entries.size)
    }

    @Test
    fun `scripts are ui mode mounts that mention the documented factory`() {
        GalleryCatalog.entries.forEach { entry ->
            val script = entry.script()
            assertTrue(entry.component, script.startsWith("\"ui\";\n"))
            assertTrue(entry.component, script.contains("compose.mount(function () {"))
            val expectedMention = if (entry.component == "Snackbar") "showSnackbar(" else "compose.${entry.component}("
            assertTrue("${entry.component} script must use $expectedMention", script.contains(expectedMention))
            assertTrue("${entry.component} script keeps ASCII punctuation", script.all { it.code < 128 })
            assertEquals(entry.component, GalleryCatalog.entry(entry.component)?.component)
        }
    }

    @Test
    fun `every node uses declared properties, events, slots and children`() {
        GalleryCatalog.entries.forEach { entry -> validate(entry.component, entry.root, root = true) }
    }

    private fun validate(owner: String, node: GalleryNode, root: Boolean) {
        val spec = catalog.component(node.component) ?: error("$owner: unknown component ${node.component}")
        assertEquals("$owner: ${node.component} is a node factory", ComponentInvocation.NODE, spec.invocation)
        val properties = spec.props.flatMap { listOf(it.name) + it.aliases }.toSet()
        val slots = spec.slots.map { it.name }.toSet()
        val events = spec.events.map { "on" + it.name.replaceFirstChar(Char::uppercaseChar) }.toSet()
        val allowed = properties + slots + events + universal + shortcuts
        node.props.forEach { (name, value) ->
            assertTrue("$owner: ${node.component} has no property, slot or event '$name'", name in allowed)
            assertTrue("$owner: ${node.component}.$name has an empty value", value.isNotBlank())
        }
        assertTrue("$owner: ${node.component} has duplicate properties", node.props.map { it.first }.distinct().size == node.props.size)
        spec.slots.filter { it.required }.forEach { slot ->
            assertTrue("$owner: ${node.component} requires slot ${slot.name}", node.props.any { it.first == slot.name })
        }
        when (spec.childrenPolicy) {
            ChildrenPolicy.NONE -> assertTrue("$owner: ${node.component} accepts no children", node.children.isEmpty())
            ChildrenPolicy.SINGLE -> assertTrue("$owner: ${node.component} accepts one child", node.children.size <= 1)
            else -> Unit
        }
        node.children.filterIsInstance<GalleryNode>().forEach { validate(owner, it, root = false) }
        if (!root) assertTrue("$owner: nested ${node.component} renders", node.render().startsWith("compose.${node.component}("))
    }
}
