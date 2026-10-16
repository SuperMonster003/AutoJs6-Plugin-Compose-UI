package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

/**
 * The view events the inspector offers a handler stub for (roadmap P10.4 / D52, option (b)): the ones the host's
 * `NativeView` emits for every view, plus the events of compound buttons, adapter views and the canvas view, as
 * documented on `api/ui.md`. [params] is the documented listener parameter list; `click` takes the view only,
 * which stubs leave out the way the documentation's examples do.
 */
object UiEvents {
    data class Event(val name: String, val params: String)

    val common: List<Event> = listOf(
        Event("click", ""),
        Event("long_click", "event, view"),
        Event("touch", "event, view"),
        Event("key", "keyCode, event, view"),
    )
    private val compound = listOf(Event("check", "checked, view"))
    private val adapter = listOf(
        Event("item_click", "item, position, itemView, listView"),
        Event("item_long_click", "event, item, position, itemView, listView"),
        Event("item_bind", "itemView, holder"),
    )
    private val canvas = listOf(Event("draw", "canvas"))

    /** The events of [tag] (a catalog entry; null for unknown tags gives the common ones), common ones first. */
    fun forTag(catalog: UiCatalog, tag: UiCatalog.Tag?): List<Event> {
        if (tag == null) return common
        val chain = catalog.chain(tag.viewClass)
        return common +
            (if ("CompoundButton" in chain) compound else emptyList()) +
            (if (tag.tag == "list" || tag.tag == "grid") adapter else emptyList()) +
            (if (tag.tag == "canvas") canvas else emptyList())
    }

    /** The event an `ui.<id>.<method>(listener)` shortcut registers, or null when [method] is not one. */
    fun ofShortcut(method: String): String? = when (method) {
        "click" -> "click"
        "longClick" -> "long_click"
        else -> null
    }

    /**
     * The statement a stub adds after the layout: `ui.<id>.on('<event>', (<params>) => {`, a `// TODO` body line and
     * `});`, every line prefixed with [indent]; the text starts with a blank line so it stands apart from the statement
     * before it and ends without a newline (the text after the insertion point keeps its own).
     */
    fun stub(id: String, event: Event, indent: String): String =
        "\n\n" + indent + "ui." + id + ".on('" + event.name + "', (" + event.params + ") => {\n" +
            indent + "    // TODO\n" +
            indent + "});"

    /**
     * The TSX form of [stub] (roadmap D68): `ui` has no member per id in the TSX declarations and the project compiles
     * with `strict` / `noUnusedParameters`, so the receiver is `(ui as any)` and every parameter is `_name: any`.
     */
    fun tsxStub(id: String, event: Event, indent: String): String =
        "\n\n" + indent + "(ui as any)." + id + ".on('" + event.name + "', (" + tsxParams(event.params) + ") => {\n" +
            indent + "    // TODO\n" +
            indent + "});"

    /** `event, view` as `_event: any, _view: any`. */
    fun tsxParams(params: String): String =
        params.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ") { "_$it: any" }

    /** The inline click handler of a TSX element (roadmap D68): the value of its `onClick` attribute. */
    const val TSX_CLICK_ATTRIBUTE = "onClick"
    const val TSX_CLICK_STUB = "{() => {\n    // TODO\n}}"
}
