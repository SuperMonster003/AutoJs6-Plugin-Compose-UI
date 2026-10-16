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

    /** The palette of a native Android XML fragment (roadmap P11.3 / D67): common view classes, by the same categories. */
    val NATIVE_CATEGORIES = listOf(
        Category("layout", listOf("LinearLayout", "FrameLayout", "RelativeLayout", "ScrollView", "HorizontalScrollView", "androidx.constraintlayout.widget.ConstraintLayout", "androidx.cardview.widget.CardView")),
        Category("text", listOf("TextView")),
        Category("button", listOf("Button", "ImageButton", "com.google.android.material.button.MaterialButton")),
        Category("input", listOf("EditText")),
        Category("choice", listOf("CheckBox", "RadioGroup", "RadioButton", "Switch", "ToggleButton", "Spinner")),
        Category("progress", listOf("ProgressBar", "SeekBar", "RatingBar")),
        Category("image", listOf("ImageView")),
        Category("list", listOf("ListView", "androidx.recyclerview.widget.RecyclerView")),
        Category("other", listOf("Space", "View", "WebView")),
    )

    /** A fresh native element of [tag] (a view class) with its layout size and a few defaults. */
    fun createNative(tag: String): XmlNode.Element {
        fun native(vararg extra: Pair<String, String>, width: String = WRAP, height: String = WRAP) =
            element(tag, "android:layout_width" to width, "android:layout_height" to height, *extra)
        return when (tag) {
            "LinearLayout" -> native("android:orientation" to "vertical", width = MATCH)
            "FrameLayout", "RelativeLayout", "ScrollView", "HorizontalScrollView", "androidx.constraintlayout.widget.ConstraintLayout", "androidx.cardview.widget.CardView" -> native(width = MATCH)
            "TextView" -> native("android:text" to "Text")
            "Button", "com.google.android.material.button.MaterialButton" -> native("android:text" to "Button")
            "EditText" -> native("android:hint" to "Input", width = MATCH)
            "CheckBox" -> native("android:text" to "Checkbox")
            "RadioButton" -> native("android:text" to "Radio")
            "RadioGroup" -> native().copy(children = listOf(
                element("RadioButton", "android:layout_width" to WRAP, "android:layout_height" to WRAP, "android:text" to "Option 1", "android:checked" to "true"),
                element("RadioButton", "android:layout_width" to WRAP, "android:layout_height" to WRAP, "android:text" to "Option 2"),
            ))
            "Switch" -> native("android:text" to "Switch")
            "SeekBar" -> native("android:max" to "100", "android:progress" to "50", width = MATCH)
            "RatingBar" -> native("android:numStars" to "5", "android:rating" to "3")
            "ImageView", "ImageButton" -> native(width = "48dp", height = "48dp")
            "ListView", "androidx.recyclerview.widget.RecyclerView", "WebView" -> native(width = MATCH, height = "200dp")
            "Space" -> native(height = "8dp")
            "View" -> native(width = MATCH, height = "1dp")
            else -> native()
        }
    }

    private const val MATCH = "match_parent"
    private const val WRAP = "wrap_content"

    private fun element(tag: String, vararg attrs: Pair<String, String>): XmlNode.Element =
        XmlNode.Element(0, tag, attrs = attrs.map { (name, value) -> XmlAttr.literal(name, value) })
}
