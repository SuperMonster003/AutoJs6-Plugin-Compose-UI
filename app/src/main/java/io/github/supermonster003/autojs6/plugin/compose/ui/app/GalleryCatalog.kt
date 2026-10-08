package io.github.supermonster003.autojs6.plugin.compose.ui.app

import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.compose.ui.R

/**
 * A script node of a gallery example. Properties are JavaScript expressions; children are nodes or
 * JavaScript expressions (string literals become Text shorthands in the host). The JVM catalog test
 * validates every property, event, slot and child policy against the real contract catalog.
 */
internal class GalleryNode(
    val component: String,
    val props: List<Pair<String, String>> = emptyList(),
    val children: List<Any> = emptyList(),
) {
    fun render(indent: String = "    "): String {
        val propertyText = props.joinToString(", ") { (name, value) -> "$name: $value" }
        if (children.isEmpty()) return if (propertyText.isEmpty()) "compose.$component()" else "compose.$component({$propertyText})"
        val inner = "$indent    "
        val renderedChildren = children.joinToString(",\n") { child ->
            inner + when (child) {
                is GalleryNode -> child.render(inner)
                else -> child.toString()
            }
        }
        return "compose.$component({$propertyText},\n$renderedChildren)"
    }
}

/** One gallery example: the component it documents, optional script prelude lines and the mounted root. */
internal class GalleryEntry(
    val component: String,
    val root: GalleryNode,
    val prelude: List<String> = emptyList(),
    /** When set, the mount result is bound to this variable so callbacks can issue session commands. */
    val bind: String? = null,
) {
    fun script(): String = buildString {
        append("\"ui\";\n\n")
        if (prelude.isNotEmpty()) {
            prelude.forEach { append(it).append('\n') }
            append('\n')
        }
        if (bind != null) append("let $bind = ")
        append("compose.mount(function () {\n    return ")
        append(root.render("    "))
        append(";\n});\n")
    }
}

internal class GalleryCategory(val id: String, @StringRes val title: Int, val entries: List<GalleryEntry>)

/** Every node component of the contract catalog plus the Snackbar command, grouped for the gallery list. */
internal object GalleryCatalog {
    private fun node(component: String, vararg props: Pair<String, String>, children: List<Any> = emptyList()) = GalleryNode(component, props.toList(), children)
    private fun text(key: String, text: String, vararg more: Pair<String, String>) = node("Text", "key" to "'$key'", "text" to "'$text'", *more)
    private fun icon(name: String) = node("Icon", "name" to "'$name'")
    private fun pad(root: GalleryNode) = node("Column", "padding" to "16", "spacing" to "12", children = listOf(root))
    private fun state(name: String, initial: String) = "let $name = compose.state($initial);"
    private fun handler(body: String) = "function () { $body }"
    private fun handler(param: String, body: String) = "function ($param) { $body }"

    val categories: List<GalleryCategory> = listOf(
        GalleryCategory("layout", R.string.gallery_category_layout, listOf(
            GalleryEntry("Column", node("Column", "padding" to "16", "spacing" to "8", "horizontalAlignment" to "'center'",
                children = listOf(text("first", "First"), text("second", "Second"), text("third", "Third")))),
            GalleryEntry("Row", node("Row", "padding" to "16", "spacing" to "8", "verticalAlignment" to "'center'",
                children = listOf(icon("Filled.Star"), text("label", "Starred"), node("Spacer", "weight" to "1"), text("trailing", "End")))),
            GalleryEntry("Box", node("Box", "w" to "160", "h" to "120", "bg" to "'#E3F2FD'", "contentAlignment" to "'center'",
                children = listOf(text("inner", "Centered")))),
            GalleryEntry("Spacer", node("Column", "padding" to "16",
                children = listOf(text("above", "Above the spacer"), node("Spacer", "h" to "24"), text("below", "Below the spacer")))),
            GalleryEntry("LazyColumn", node("LazyColumn", "contentPadding" to "16", "spacing" to "8",
                children = listOf("rows")), prelude = listOf(
                "let rows = [];",
                "for (let i = 0; i < 100; i++) rows.push(compose.Text({key: 'row-' + i, text: 'Row ' + i}));",
            )),
            GalleryEntry("LazyRow", node("LazyRow", "contentPadding" to "16", "spacing" to "8",
                children = listOf("cards")), prelude = listOf(
                "let cards = [];",
                "for (let i = 0; i < 20; i++) cards.push(compose.Card({key: 'card-' + i, padding: 16}, compose.Text({text: 'Card ' + i})));",
            )),
            GalleryEntry("LazyVerticalGrid", node("LazyVerticalGrid", "columns" to "3", "spacing" to "8", "contentPadding" to "16",
                children = listOf("cells")), prelude = listOf(
                "let cells = [];",
                "for (let i = 0; i < 30; i++) cells.push(compose.Card({key: 'cell-' + i, padding: 16}, compose.Text({text: String(i)})));",
            )),
            GalleryEntry("HorizontalPager", node("Column", "padding" to "16", "spacing" to "8", children = listOf(
                node("Text", "key" to "'status'", "text" to "'Page ' + (page.value + 1) + ' of 3'"),
                node("HorizontalPager", "key" to "'pager'", "h" to "160", "page" to "page.value",
                    "onPageChange" to handler("index", "page.value = index;"),
                    children = listOf(
                        node("Card", "key" to "'one'", "padding" to "24", children = listOf("'First page'")),
                        node("Card", "key" to "'two'", "padding" to "24", children = listOf("'Second page'")),
                        node("Card", "key" to "'three'", "padding" to "24", children = listOf("'Third page'")),
                    )),
            )), prelude = listOf(state("page", "0"))),
        )),
        GalleryCategory("container", R.string.gallery_category_container, listOf(
            GalleryEntry("Surface", pad(node("Surface", "color" to "'#FFF8E1'", "shape" to "'rounded'", "tonalElevation" to "2",
                children = listOf(node("Text", "padding" to "16", "text" to "'A tinted surface'"))))),
            GalleryEntry("Card", pad(node("Card", "elevation" to "2",
                children = listOf(node("Column", "padding" to "16", "spacing" to "8",
                    children = listOf(text("title", "Card title", "style" to "'titleMedium'"), text("body", "Supporting text inside the card"))))))),
            GalleryEntry("HorizontalDivider", node("Column", "padding" to "16", "spacing" to "12",
                children = listOf(text("above", "Above"), node("HorizontalDivider", "thickness" to "1"), text("below", "Below")))),
            GalleryEntry("Scaffold", node("Scaffold", "topBar" to "compose.TopAppBar({title: 'Scaffold'})",
                children = listOf(node("Column", "padding" to "16", children = listOf(text("body", "Content below the app bar")))))),
            GalleryEntry("TopAppBar", node("Scaffold", "topBar" to node("TopAppBar", "title" to "'Title'",
                "navigationIcon" to node("IconButton", "onClick" to handler("toast('Navigation');"), children = listOf(icon("Filled.Menu"))).render("        "),
                "actions" to node("IconButton", "onClick" to handler("toast('Action');"), children = listOf(icon("Filled.MoreVert"))).render("        ")).render("        "),
                children = listOf(node("Column", "padding" to "16", children = listOf(text("body", "Page content")))))),
            GalleryEntry("AndroidView", node("Column", "padding" to "16", "spacing" to "12", children = listOf(
                text("label", "A classic View inside Compose"),
                node("AndroidView", "key" to "'native'", "view" to "ui.inflate('<text text=\"Hello from XML\" textSize=\"18sp\" />')"),
            ))),
        )),
        GalleryCategory("text", R.string.gallery_category_text, listOf(
            GalleryEntry("Text", node("Column", "padding" to "16", "spacing" to "8", children = listOf(
                text("headline", "Headline", "style" to "'headlineSmall'"),
                text("body", "Body text with a custom color", "color" to "'#1565C0'"),
                text("clipped", "A very long single line that is clipped with an ellipsis at the end", "maxLines" to "1", "overflow" to "'ellipsis'"),
            ))),
            GalleryEntry("Icon", node("Row", "padding" to "16", "spacing" to "16", children = listOf(
                node("Icon", "name" to "'Filled.Home'"), node("Icon", "name" to "'Filled.Favorite'", "tint" to "'#E53935'"), node("Icon", "name" to "'Outlined.Settings'"),
            ))),
            GalleryEntry("Image", node("Column", "padding" to "16", children = listOf(
                node("Image", "key" to "'picture'", "src" to "picture", "w" to "160", "h" to "120", "contentScale" to "'fit'", "contentDescription" to "'Sample image'"),
            )), prelude = listOf(
                "let picture = images.read(files.path('./sample.png'));",
            )),
            GalleryEntry("Badge", node("Row", "padding" to "16", "spacing" to "24", children = listOf(
                node("Badge", "text" to "'3'", "visible" to "true", children = listOf(icon("Filled.Email"))),
                node("Badge", "visible" to "true", children = listOf(icon("Filled.Notifications"))),
            ))),
            GalleryEntry("Tooltip", node("Column", "padding" to "16", children = listOf(
                node("Tooltip", "key" to "'tip'", "tooltip" to "'Long-press to show this tooltip'", "open" to "open.value",
                    "onOpenChange" to handler("value", "open.value = value;"),
                    children = listOf(node("Button", "onClick" to handler("open.value = !open.value;"), children = listOf("'Hover me'")))),
            )), prelude = listOf(state("open", "false"))),
        )),
        GalleryCategory("button", R.string.gallery_category_button, listOf(
            GalleryEntry("Button", pad(node("Button", "key" to "'tap'", "onClick" to handler("count.value++;"),
                children = listOf("'Clicked ' + count.value + ' times'"))), prelude = listOf(state("count", "0"))),
            GalleryEntry("ElevatedButton", pad(node("ElevatedButton", "onClick" to handler("toast('Elevated');"), children = listOf("'Elevated'")))),
            GalleryEntry("FilledTonalButton", pad(node("FilledTonalButton", "onClick" to handler("toast('Tonal');"), children = listOf("'Filled tonal'")))),
            GalleryEntry("OutlinedButton", pad(node("OutlinedButton", "onClick" to handler("toast('Outlined');"), children = listOf("'Outlined'")))),
            GalleryEntry("TextButton", pad(node("TextButton", "onClick" to handler("toast('Text');"), children = listOf("'Text button'")))),
            GalleryEntry("IconButton", pad(node("IconButton", "onClick" to handler("toast('Icon');"), children = listOf(icon("Filled.Favorite"))))),
            GalleryEntry("FloatingActionButton", pad(node("FloatingActionButton", "onClick" to handler("toast('Add');"), children = listOf(icon("Filled.Add"))))),
            GalleryEntry("SegmentedButton", pad(node("SegmentedButton", "key" to "'choice'", "mode" to "'single'", children = listOf(
                node("SegmentedButtonItem", "key" to "'day'", "label" to "'Day'", "selected" to "choice.value === 'day'", "onSelectedChange" to handler("choice.value = 'day';")),
                node("SegmentedButtonItem", "key" to "'week'", "label" to "'Week'", "selected" to "choice.value === 'week'", "onSelectedChange" to handler("choice.value = 'week';")),
                node("SegmentedButtonItem", "key" to "'month'", "label" to "'Month'", "selected" to "choice.value === 'month'", "onSelectedChange" to handler("choice.value = 'month';")),
            ))), prelude = listOf(state("choice", "'day'"))),
            GalleryEntry("SegmentedButtonItem", pad(node("SegmentedButton", "key" to "'filters'", "mode" to "'multi'", children = listOf(
                node("SegmentedButtonItem", "key" to "'bold'", "label" to "'Bold'", "selected" to "bold.value", "onSelectedChange" to handler("value", "bold.value = value;")),
                node("SegmentedButtonItem", "key" to "'italic'", "label" to "'Italic'", "selected" to "italic.value", "onSelectedChange" to handler("value", "italic.value = value;")),
            ))), prelude = listOf(state("bold", "true"), state("italic", "false"))),
        )),
        GalleryCategory("selection", R.string.gallery_category_selection, listOf(
            GalleryEntry("Switch", node("Row", "padding" to "16", "spacing" to "12", "verticalAlignment" to "'center'", children = listOf(
                node("Text", "key" to "'label'", "text" to "enabled.value ? 'Enabled' : 'Disabled'", "weight" to "1"),
                node("Switch", "key" to "'toggle'", "checked" to "enabled.value", "onCheckedChange" to handler("value", "enabled.value = value;")),
            )), prelude = listOf(state("enabled", "true"))),
            GalleryEntry("Checkbox", node("Row", "padding" to "16", "spacing" to "12", "verticalAlignment" to "'center'", children = listOf(
                node("Checkbox", "key" to "'agree'", "checked" to "agreed.value", "onCheckedChange" to handler("value", "agreed.value = value;")),
                text("label", "I agree to the terms"),
            )), prelude = listOf(state("agreed", "false"))),
            GalleryEntry("RadioButton", node("Column", "padding" to "16", "spacing" to "8", children = listOf(
                node("Row", "key" to "'small'", "spacing" to "12", "verticalAlignment" to "'center'", children = listOf(
                    node("RadioButton", "selected" to "size.value === 'small'", "onClick" to handler("size.value = 'small';")), text("small-label", "Small"))),
                node("Row", "key" to "'large'", "spacing" to "12", "verticalAlignment" to "'center'", children = listOf(
                    node("RadioButton", "selected" to "size.value === 'large'", "onClick" to handler("size.value = 'large';")), text("large-label", "Large"))),
            )), prelude = listOf(state("size", "'small'"))),
            GalleryEntry("Slider", node("Column", "padding" to "16", "spacing" to "8", children = listOf(
                node("Text", "key" to "'value'", "text" to "'Volume: ' + Math.round(volume.value)"),
                node("Slider", "key" to "'volume'", "value" to "volume.value", "range" to "[0, 100]", "steps" to "9",
                    "onValueChange" to handler("value", "volume.value = value;")),
            )), prelude = listOf(state("volume", "40"))),
            GalleryEntry("AssistChip", pad(node("AssistChip", "label" to "'Set a reminder'", "leadingIcon" to "compose.Icon({name: 'Filled.DateRange'})",
                "onClick" to handler("toast('Assist');")))),
            GalleryEntry("FilterChip", node("Row", "padding" to "16", "spacing" to "8", children = listOf(
                node("FilterChip", "key" to "'done'", "label" to "'Done'", "selected" to "showDone.value", "onSelectedChange" to handler("value", "showDone.value = value;")),
                node("FilterChip", "key" to "'open'", "label" to "'Open'", "selected" to "showOpen.value", "onSelectedChange" to handler("value", "showOpen.value = value;")),
            )), prelude = listOf(state("showDone", "true"), state("showOpen", "false"))),
            GalleryEntry("InputChip", pad(node("InputChip", "key" to "'tag'", "label" to "'Android'", "selected" to "selected.value",
                "leadingIcon" to "compose.Icon({name: 'Filled.Check'})", "onSelectedChange" to handler("value", "selected.value = value;"))),
                prelude = listOf(state("selected", "false"))),
        )),
        GalleryCategory("input", R.string.gallery_category_input, listOf(
            GalleryEntry("TextField", node("Column", "padding" to "16", "spacing" to "8", children = listOf(
                node("TextField", "key" to "'name'", "text" to "name.value", "label" to "'Name'", "placeholder" to "'Your name'", "singleLine" to "true",
                    "onValueChange" to handler("text", "name.value = text;")),
                node("Text", "key" to "'echo'", "text" to "'Hello, ' + (name.value || 'stranger')"),
            )), prelude = listOf(state("name", "''"))),
            GalleryEntry("OutlinedTextField", pad(node("OutlinedTextField", "key" to "'email'", "text" to "email.value", "label" to "'Email'",
                "keyboardType" to "'email'", "isError" to "email.value.length > 0 && email.value.indexOf('@') < 0",
                "supportingText" to "'Enter a valid address'", "onValueChange" to handler("text", "email.value = text;"))),
                prelude = listOf(state("email", "''"))),
            GalleryEntry("SearchBar", node("Column", "padding" to "16", children = listOf(
                node("SearchBar", "key" to "'search'", "query" to "query.value", "expanded" to "expanded.value", "placeholder" to "'Search'",
                    "onQueryChange" to handler("text", "query.value = text;"),
                    "onSearch" to handler("text", "toast('Searching ' + text); expanded.value = false;"),
                    "onExpandedChange" to handler("value", "expanded.value = value;"),
                    children = listOf(text("hint", "Suggestions appear here"))),
            )), prelude = listOf(state("query", "''"), state("expanded", "false"))),
            GalleryEntry("DatePicker", node("Column", "padding" to "16", children = listOf(
                node("DatePicker", "key" to "'date'", "selectedDateMillis" to "date.value", "displayMode" to "'picker'",
                    "onDateChange" to handler("millis", "date.value = millis;")),
            )), prelude = listOf(state("date", "null"))),
            GalleryEntry("TimePicker", node("Column", "padding" to "16", "spacing" to "8", children = listOf(
                node("Text", "key" to "'value'", "text" to "hour.value + ':' + (minute.value < 10 ? '0' : '') + minute.value"),
                node("TimePicker", "key" to "'time'", "hour" to "hour.value", "minute" to "minute.value", "is24Hour" to "true",
                    "onTimeChange" to handler("h, m", "hour.value = h; minute.value = m;")),
            )), prelude = listOf(state("hour", "9"), state("minute", "30"))),
        )),
        GalleryCategory("progress", R.string.gallery_category_progress, listOf(
            GalleryEntry("CircularProgressIndicator", node("Row", "padding" to "16", "spacing" to "24", children = listOf(
                node("CircularProgressIndicator", "key" to "'determinate'", "progress" to "0.6"),
                node("CircularProgressIndicator", "key" to "'indeterminate'"),
            ))),
            GalleryEntry("LinearProgressIndicator", node("Column", "padding" to "16", "spacing" to "16", children = listOf(
                node("LinearProgressIndicator", "key" to "'determinate'", "progress" to "0.4"),
                node("LinearProgressIndicator", "key" to "'indeterminate'"),
            ))),
            GalleryEntry("PullToRefresh", node("PullToRefresh", "key" to "'refresh'", "refreshing" to "refreshing.value",
                "onRefresh" to handler("refreshing.value = true; setTimeout(function () { refreshing.value = false; }, 1500);"),
                children = listOf(node("LazyColumn", "contentPadding" to "16", "spacing" to "8", children = listOf("rows")))),
                prelude = listOf(state("refreshing", "false"), "let rows = [];", "for (let i = 0; i < 30; i++) rows.push(compose.Text({key: 'row-' + i, text: 'Pull down to refresh ' + i}));")),
        )),
        GalleryCategory("navigation", R.string.gallery_category_navigation, listOf(
            GalleryEntry("NavigationBar", node("Column", "spacing" to "0", children = listOf(
                node("Text", "key" to "'page'", "padding" to "16", "weight" to "1", "text" to "'Selected: ' + tab.value"),
                node("NavigationBar", "key" to "'bar'", children = listOf(
                    node("NavigationBarItem", "key" to "'home'", "icon" to "compose.Icon({name: 'Filled.Home'})", "label" to "'Home'", "selected" to "tab.value === 'home'", "onClick" to handler("tab.value = 'home';")),
                    node("NavigationBarItem", "key" to "'search'", "icon" to "compose.Icon({name: 'Filled.Search'})", "label" to "'Search'", "selected" to "tab.value === 'search'", "onClick" to handler("tab.value = 'search';")),
                    node("NavigationBarItem", "key" to "'profile'", "icon" to "compose.Icon({name: 'Filled.Person'})", "label" to "'Profile'", "selected" to "tab.value === 'profile'", "onClick" to handler("tab.value = 'profile';")),
                )),
            )), prelude = listOf(state("tab", "'home'"))),
            GalleryEntry("NavigationBarItem", node("NavigationBar", "key" to "'bar'", children = listOf(
                node("NavigationBarItem", "key" to "'inbox'", "icon" to "compose.Icon({name: 'Filled.Email'})", "label" to "'Inbox'", "selected" to "true", "alwaysShowLabel" to "true", "onClick" to handler("toast('Inbox');")),
                node("NavigationBarItem", "key" to "'starred'", "icon" to "compose.Icon({name: 'Filled.Star'})", "label" to "'Starred'", "selected" to "false", "onClick" to handler("toast('Starred');")),
            ))),
            GalleryEntry("NavigationRail", node("Row", children = listOf(
                node("NavigationRail", "key" to "'rail'", "header" to "compose.Icon({name: 'Filled.Menu'})", children = listOf(
                    node("NavigationRailItem", "key" to "'home'", "icon" to "compose.Icon({name: 'Filled.Home'})", "label" to "'Home'", "selected" to "page.value === 'home'", "onClick" to handler("page.value = 'home';")),
                    node("NavigationRailItem", "key" to "'settings'", "icon" to "compose.Icon({name: 'Filled.Settings'})", "label" to "'Settings'", "selected" to "page.value === 'settings'", "onClick" to handler("page.value = 'settings';")),
                )),
                node("Text", "key" to "'page'", "padding" to "16", "text" to "'Page: ' + page.value"),
            )), prelude = listOf(state("page", "'home'"))),
            GalleryEntry("NavigationRailItem", node("NavigationRail", "key" to "'rail'", children = listOf(
                node("NavigationRailItem", "key" to "'one'", "icon" to "compose.Icon({name: 'Filled.Favorite'})", "label" to "'Favorites'", "selected" to "true", "onClick" to handler("toast('Favorites');")),
                node("NavigationRailItem", "key" to "'two'", "icon" to "compose.Icon({name: 'Filled.Info'})", "label" to "'Info'", "selected" to "false", "onClick" to handler("toast('Info');")),
            ))),
            GalleryEntry("NavigationDrawer", node("NavigationDrawer", "key" to "'drawer'", "open" to "open.value",
                "onOpenChange" to handler("value", "open.value = value;"),
                "drawerContent" to node("Column", "padding" to "16", "spacing" to "8", children = listOf(
                    node("NavigationDrawerItem", "key" to "'home'", "label" to "'Home'", "icon" to "compose.Icon({name: 'Filled.Home'})", "selected" to "true", "onClick" to handler("open.value = false;")),
                    node("NavigationDrawerItem", "key" to "'settings'", "label" to "'Settings'", "icon" to "compose.Icon({name: 'Filled.Settings'})", "selected" to "false", "onClick" to handler("open.value = false;")),
                )).render("        "),
                children = listOf(node("Column", "padding" to "16", children = listOf(
                    node("Button", "onClick" to handler("open.value = true;"), children = listOf("'Open drawer'")))))),
                prelude = listOf(state("open", "false"))),
            GalleryEntry("NavigationDrawerItem", node("Column", "padding" to "16", "spacing" to "8", children = listOf(
                node("NavigationDrawerItem", "key" to "'inbox'", "label" to "'Inbox'", "icon" to "compose.Icon({name: 'Filled.Email'})", "badge" to "'12'", "selected" to "item.value === 'inbox'", "onClick" to handler("item.value = 'inbox';")),
                node("NavigationDrawerItem", "key" to "'sent'", "label" to "'Sent'", "icon" to "compose.Icon({name: 'Filled.Send'})", "selected" to "item.value === 'sent'", "onClick" to handler("item.value = 'sent';")),
            )), prelude = listOf(state("item", "'inbox'"))),
            GalleryEntry("TabRow", node("Column", children = listOf(
                node("TabRow", "key" to "'tabs'", "selectedIndex" to "index.value", children = listOf(
                    node("Tab", "key" to "'overview'", "text" to "'Overview'", "onClick" to handler("index.value = 0;")),
                    node("Tab", "key" to "'details'", "text" to "'Details'", "onClick" to handler("index.value = 1;")),
                    node("Tab", "key" to "'history'", "text" to "'History'", "onClick" to handler("index.value = 2;")),
                )),
                node("Text", "key" to "'page'", "padding" to "16", "text" to "'Tab ' + (index.value + 1)"),
            )), prelude = listOf(state("index", "0"))),
            GalleryEntry("Tab", node("TabRow", "key" to "'tabs'", "selectedIndex" to "0", "scrollable" to "true", children = listOf(
                node("Tab", "key" to "'text'", "text" to "'Text only'", "onClick" to handler("toast('Text');")),
                node("Tab", "key" to "'icon'", "text" to "'With icon'", "icon" to "compose.Icon({name: 'Filled.Star'})", "onClick" to handler("toast('Icon');")),
                node("Tab", "key" to "'disabled'", "text" to "'Disabled'", "enabled" to "false"),
            ))),
        )),
        GalleryCategory("dialog", R.string.gallery_category_dialog, listOf(
            GalleryEntry("AlertDialog", node("Column", "padding" to "16", children = listOf(
                node("Button", "onClick" to handler("open.value = true;"), children = listOf("'Show dialog'")),
                node("AlertDialog", "key" to "'confirm'", "open" to "open.value", "title" to "'Delete file?'", "text" to "'This cannot be undone.'",
                    "confirm" to "compose.TextButton({onClick: function () { open.value = false; toast('Deleted'); }}, 'Delete')",
                    "dismiss" to "compose.TextButton({onClick: function () { open.value = false; }}, 'Cancel')",
                    "onDismissRequest" to handler("open.value = false;")),
            )), prelude = listOf(state("open", "false"))),
            GalleryEntry("ModalBottomSheet", node("Column", "padding" to "16", children = listOf(
                node("Button", "onClick" to handler("open.value = true;"), children = listOf("'Show bottom sheet'")),
                node("ModalBottomSheet", "key" to "'sheet'", "open" to "open.value", "onDismissRequest" to handler("open.value = false;"),
                    children = listOf(node("Column", "padding" to "24", "spacing" to "12", children = listOf(
                        text("title", "Sheet title", "style" to "'titleMedium'"),
                        node("Button", "onClick" to handler("open.value = false;"), children = listOf("'Close'")))))),
            )), prelude = listOf(state("open", "false"))),
            GalleryEntry("DropdownMenu", node("Column", "padding" to "16", children = listOf(
                node("DropdownMenu", "key" to "'menu'", "open" to "open.value", "onDismissRequest" to handler("open.value = false;"),
                    "anchor" to "compose.Button({onClick: function () { open.value = true; }}, 'Open menu')",
                    children = listOf(
                        node("DropdownMenuItem", "key" to "'edit'", "text" to "'Edit'", "onClick" to handler("open.value = false; toast('Edit');")),
                        node("DropdownMenuItem", "key" to "'share'", "text" to "'Share'", "onClick" to handler("open.value = false; toast('Share');")),
                    )),
            )), prelude = listOf(state("open", "false"))),
            GalleryEntry("DropdownMenuItem", node("Column", "padding" to "16", children = listOf(
                node("DropdownMenu", "key" to "'menu'", "open" to "open.value", "onDismissRequest" to handler("open.value = false;"),
                    "anchor" to "compose.IconButton({onClick: function () { open.value = true; }}, compose.Icon({name: 'Filled.MoreVert'}))",
                    children = listOf(
                        node("DropdownMenuItem", "key" to "'copy'", "text" to "'Copy'", "leadingIcon" to "compose.Icon({name: 'Filled.Share'})", "onClick" to handler("open.value = false;")),
                        node("DropdownMenuItem", "key" to "'delete'", "text" to "'Delete'", "leadingIcon" to "compose.Icon({name: 'Filled.Delete'})", "enabled" to "false"),
                    )),
            )), prelude = listOf(state("open", "false"))),
            GalleryEntry("Snackbar", node("Scaffold", children = listOf(node("Column", "padding" to "16", children = listOf(
                node("Button", "key" to "'show'", "onClick" to handler("session.showSnackbar('Saved', {actionLabel: 'Undo', onAction: function () { toast('Undone'); }});"),
                    children = listOf("'Show snackbar'")))))), bind = "session"),
        )),
    )

    val entries: List<GalleryEntry> = categories.flatMap { it.entries }

    fun entry(component: String): GalleryEntry? = entries.firstOrNull { it.component == component }
}
