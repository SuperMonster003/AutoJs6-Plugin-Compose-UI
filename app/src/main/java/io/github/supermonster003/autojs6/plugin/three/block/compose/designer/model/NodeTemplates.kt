package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

import org.autojs.plugin.compose.api.catalog.ChildrenPolicy
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.catalog.ComponentInvocation
import org.autojs.plugin.compose.api.v2.ComposeUiV2

/**
 * Starter nodes for the component palette (roadmap D36): every catalog node component gets a small valid
 * default (required props and slots filled, scoped items wrapped in their container) so that inserting
 * it never produces a validation error on its own. Identities are 0 and are assigned on insertion.
 */
internal object NodeTemplates {
    private fun str(value: String) = PropValue.str(value)
    private fun num(value: Double) = PropValue.num(value)
    private fun bool(value: Boolean) = PropValue.bool(value)

    private fun node(type: String, vararg props: Pair<String, PropValue>, slots: List<Pair<String, DesignChild>> = emptyList(), children: List<DesignChild> = emptyList()): DesignNode =
        DesignNode(0, type, props.map { NodeEntry.Prop(it.first, it.second) } + slots.map { NodeEntry.Slot(it.first, it.second) }, children)

    private fun text(value: String): DesignChild = DesignChild.Node(DesignNode.text(0, value))
    private fun icon(name: String): DesignChild = DesignChild.Node(node("Icon", "name" to str(name)))
    private fun child(node: DesignNode): DesignChild = DesignChild.Node(node)

    fun create(type: String): DesignNode = when (type) {
        "Column", "Row" -> node(type, "padding" to num(16.0), "spacing" to num(8.0))
        "Box" -> node(type, "padding" to num(16.0))
        "Spacer" -> node(type, "h" to num(16.0))
        "LazyColumn", "LazyRow" -> node(type, "contentPadding" to num(16.0), "spacing" to num(8.0), children = listOf(text("Item")))
        "Scaffold" -> node(type, children = listOf(child(node("Column", "padding" to num(16.0), children = listOf(text("Content"))))))
        "Surface" -> node(type, "tonalElevation" to num(2.0), children = listOf(child(node("Text", "padding" to num(16.0), "text" to str("Surface")))))
        "Card" -> node(type, "elevation" to num(2.0), children = listOf(child(node("Text", "padding" to num(16.0), "text" to str("Card")))))
        "HorizontalDivider" -> node(type, "thickness" to num(1.0))
        "TopAppBar" -> node(type, slots = listOf("title" to text("Title")))
        "Text" -> node(type, "text" to str("Text"))
        "Icon" -> node(type, "name" to str("Filled.Star"))
        "Image" -> node(type, "src" to PropValue.Expression("images.read(files.path('./image.png'))"), "w" to num(120.0), "h" to num(120.0))
        "Button", "ElevatedButton", "FilledTonalButton", "OutlinedButton", "TextButton" -> node(type, children = listOf(text("Button")))
        "IconButton" -> node(type, children = listOf(icon("Filled.Favorite")))
        "Switch", "Checkbox" -> node(type, "checked" to bool(false))
        "RadioButton" -> node(type, "selected" to bool(false))
        "Slider" -> node(type, "value" to num(0.5))
        "TextField", "OutlinedTextField" -> node(type, "text" to str(""), slots = listOf("label" to text("Label")))
        "CircularProgressIndicator", "LinearProgressIndicator" -> node(type, "progress" to num(0.5))
        "AlertDialog" -> node(type, "open" to bool(false), slots = listOf("title" to text("Title"), "text" to text("Message"), "confirm" to child(node("TextButton", children = listOf(text("OK"))))))
        "NavigationBar" -> node(type, children = listOf(child(navigationItem("NavigationBarItem", "Home", "Filled.Home", true)), child(navigationItem("NavigationBarItem", "Search", "Filled.Search", false))))
        "NavigationBarItem" -> navigationItem(type, "Item", "Filled.Star", false)
        "NavigationRail" -> node(type, children = listOf(child(navigationItem("NavigationRailItem", "Home", "Filled.Home", true)), child(navigationItem("NavigationRailItem", "Settings", "Filled.Settings", false))))
        "NavigationRailItem" -> navigationItem(type, "Item", "Filled.Star", false)
        "NavigationDrawer" -> node(type, "open" to bool(false),
            slots = listOf("drawerContent" to child(node("Column", "padding" to num(16.0), children = listOf(child(node("NavigationDrawerItem", "selected" to bool(true), slots = listOf("label" to text("Home")))))))),
            children = listOf(child(node("Column", "padding" to num(16.0), children = listOf(text("Content"))))))
        "NavigationDrawerItem" -> node(type, "selected" to bool(false), slots = listOf("label" to text("Item")))
        "TabRow" -> node(type, "selectedIndex" to num(0.0), children = listOf(child(node("Tab", slots = listOf("text" to text("First")))), child(node("Tab", slots = listOf("text" to text("Second"))))))
        "Tab" -> node(type, slots = listOf("text" to text("Tab")))
        "ModalBottomSheet" -> node(type, "open" to bool(false), children = listOf(child(node("Column", "padding" to num(24.0), children = listOf(text("Sheet"))))))
        "DropdownMenu" -> node(type, "open" to bool(false), slots = listOf("anchor" to child(node("Button", children = listOf(text("Menu"))))),
            children = listOf(child(node("DropdownMenuItem", slots = listOf("text" to text("Item"))))))
        "DropdownMenuItem" -> node(type, slots = listOf("text" to text("Item")))
        "DatePicker" -> node(type)
        "TimePicker" -> node(type, "hour" to num(9.0), "minute" to num(30.0))
        "HorizontalPager" -> node(type, "h" to num(160.0), children = listOf(child(node("Card", "padding" to num(24.0), children = listOf(text("Page 1")))), child(node("Card", "padding" to num(24.0), children = listOf(text("Page 2"))))))
        "LazyVerticalGrid" -> node(type, "columns" to num(2.0), "spacing" to num(8.0), "contentPadding" to num(16.0), children = listOf(child(node("Card", "padding" to num(16.0), children = listOf(text("Cell"))))))
        "AssistChip" -> node(type, slots = listOf("label" to text("Chip")))
        "FilterChip", "InputChip" -> node(type, "selected" to bool(false), slots = listOf("label" to text("Chip")))
        "Badge" -> node(type, "text" to str("1"), children = listOf(icon("Filled.Email")))
        "SegmentedButton" -> node(type, children = listOf(child(node("SegmentedButtonItem", "selected" to bool(true), slots = listOf("label" to text("A")))), child(node("SegmentedButtonItem", "selected" to bool(false), slots = listOf("label" to text("B"))))))
        "SegmentedButtonItem" -> node(type, "selected" to bool(false), slots = listOf("label" to text("Item")))
        "FloatingActionButton" -> node(type, children = listOf(icon("Filled.Add")))
        "SearchBar" -> node(type, "query" to str(""), slots = listOf("placeholder" to text("Search")))
        "Tooltip" -> node(type, "open" to bool(false), slots = listOf("tooltip" to text("Tip")), children = listOf(child(node("Button", children = listOf(text("Hover"))))))
        "PullToRefresh" -> node(type, "refreshing" to bool(false), children = listOf(child(node("Column", "padding" to num(16.0), children = listOf(text("Pull to refresh"))))))
        else -> node(type)
    }

    private fun navigationItem(type: String, label: String, icon: String, selected: Boolean) =
        node(type, "selected" to bool(selected), slots = listOf("icon" to icon(icon), "label" to text(label)))

    /** Catalog node components the palette offers (the Snackbar command and the AndroidView interop entry are not insertable). */
    fun insertable(catalog: ComponentCatalog = ComposeUiV2.interopCatalog): List<String> =
        catalog.components.filter { it.invocation == ComponentInvocation.NODE && it.name != "AndroidView" }.map { it.name }

    /** Whether [parentType] can take one more primary child. */
    fun acceptsChild(catalog: ComponentCatalog, parentType: String, currentChildren: Int): Boolean {
        val spec = catalog.component(parentType) ?: return false
        return when (spec.childrenPolicy) {
            ChildrenPolicy.NONE -> false
            ChildrenPolicy.SINGLE -> currentChildren == 0
            ChildrenPolicy.MANY, ChildrenPolicy.LAZY_ITEMS -> true
        }
    }
}
