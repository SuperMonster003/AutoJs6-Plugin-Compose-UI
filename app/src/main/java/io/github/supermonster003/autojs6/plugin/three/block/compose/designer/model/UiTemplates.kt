package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

/**
 * The component palette of a ui (XML) project (roadmap P10.4): the catalog's tags by the documentation's
 * categories, each with a small default element. `compose` (a Compose host inside a ui layout) and class-name
 * tags are not offered; a script that has them keeps them.
 */
object UiTemplates {
    data class Category(val id: String, val tags: List<String>)

    /** The documentation's category order; the catalog's own order inside each. */
    val CATEGORY_ORDER = listOf("layout", "text", "button", "input", "choice", "progress", "image", "media", "list", "other")
    private val EXCLUDED = setOf("compose")

    fun categories(catalog: UiCatalog): List<Category> =
        CATEGORY_ORDER.map { id -> Category(id, catalog.tags.filter { it.category == id && it.tag !in EXCLUDED }.map { it.tag }) }.filter { it.tags.isNotEmpty() }

    fun insertable(catalog: UiCatalog): List<String> = categories(catalog).flatMap { it.tags }

    /** A fresh element of [tag] with its default attributes; ids are assigned when it is inserted. */
    fun create(tag: String): XmlNode.Element = when (tag) {
        "text" -> element(tag, "text" to "Text")
        "button" -> element(tag, "text" to "Button")
        "input" -> element(tag, "w" to "*", "hint" to "Input")
        "search" -> element(tag, "w" to "*")
        "checkbox" -> element(tag, "text" to "Checkbox")
        "radio" -> element(tag, "text" to "Radio")
        "radiogroup" -> XmlNode.Element(0, tag, children = listOf(element("radio", "text" to "Option 1", "checked" to "true"), element("radio", "text" to "Option 2")))
        "switch" -> element(tag, "text" to "Switch")
        "togglebutton" -> element(tag, "text" to "Toggle")
        "checkedtext" -> element(tag, "text" to "Checked text")
        "spinner" -> element(tag, "entries" to "Option 1|Option 2")
        "seekbar" -> element(tag, "w" to "*", "max" to "100", "progress" to "50")
        "progressbar" -> element(tag)
        "ratingbar" -> element(tag, "numStars" to "5", "rating" to "3")
        "image", "imagebutton" -> element(tag, "w" to "48", "h" to "48")
        "video", "webview", "canvas" -> element(tag, "w" to "*", "h" to "200")
        "list", "grid" -> XmlNode.Element(0, tag, attrs = listOf(XmlAttr.literal("w", "*"), XmlAttr.literal("h", "auto")), children = listOf(element("text", "text" to "{{this}}", "padding" to "8")))
        "toolbar" -> element(tag, "title" to "Title")
        "card" -> element(tag, "w" to "*", "h" to "auto", "margin" to "8")
        "space" -> element(tag, "h" to "8")
        "view" -> element(tag, "w" to "*", "h" to "1")
        "console", "globalconsole" -> element(tag, "w" to "*", "h" to "200")
        "linear", "vertical", "frame", "relative", "scroll", "drawer", "appbar", "actionmenu", "viewpager", "tabs", "viewflipper", "viewswitcher", "textswitcher" -> element(tag, "w" to "*")
        else -> element(tag)
    }

    private fun element(tag: String, vararg attrs: Pair<String, String>): XmlNode.Element =
        XmlNode.Element(0, tag, attrs = attrs.map { (name, value) -> XmlAttr.literal(name, value) })
}
