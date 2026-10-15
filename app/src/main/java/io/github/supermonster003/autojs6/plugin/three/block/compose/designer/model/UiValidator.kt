package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

/**
 * Checks a [UiDocument] against the [UiCatalog] (roadmap P10.1): unknown tags, unknown or unsupported attributes,
 * value formats, RelativeLayout rules outside a relative parent, list templates, duplicate ids and empty bindings.
 * Values with `{{ }}` bindings or `{expr}` parts are not type checked. Elements with namespaced attributes
 * (`xmlns:android`, `android:text`) are Android layouts the host inflates as written and are left alone.
 */
class UiValidator(private val catalog: UiCatalog = UiCatalog.load()) {

    /** The catalog tag or the view class a class-name tag maps to; [known] is false when the class is a guess. */
    data class Resolved(val viewClass: String, val tag: UiCatalog.Tag?, val known: Boolean)

    fun resolve(tag: String): Resolved? {
        catalog.tag(tag)?.let { return Resolved(it.viewClass, it, true) }
        if (!tag.contains('.') && tag.first().isLowerCase()) return null
        val simple = tag.substringAfterLast('.')
        return if (catalog.knowsClass(simple)) Resolved(simple, null, true) else Resolved(if (simple.endsWith("Layout") || simple.endsWith("Group")) "ViewGroup" else "View", null, false)
    }

    fun validate(document: UiDocument): List<Diagnostic> {
        val diagnostics = ArrayList<Diagnostic>()
        val ids = HashMap<String, MutableList<Int>>()
        for (fragment in document.fragments) validate(fragment.root, null, diagnostics, ids, document.widgets)
        ids.forEach { (id, owners) -> if (owners.size > 1) owners.forEach { diagnostics += Diagnostic(it, "id", Severity.ERROR, "Duplicate id \"$id\"") } }
        return diagnostics
    }

    private fun validate(element: XmlNode.Element, parent: XmlNode.Element?, out: MutableList<Diagnostic>, ids: MutableMap<String, MutableList<Int>>, widgets: Set<String>) {
        val namespaced = element.attrs.any { it.name.startsWith("xmlns") || ':' in it.name }
        val widget = element.tag in widgets
        val resolved = if (widget) null else resolve(element.tag)
        when {
            widget -> Unit
            resolved == null -> out += Diagnostic(element.id, null, Severity.ERROR, "Unknown tag <${element.tag}>")
            !resolved.known -> out += Diagnostic(element.id, null, Severity.WARNING, "<${element.tag}> is checked with ${resolved.viewClass} attributes only")
        }
        element.attr("id")?.takeUnless { it.hasBinding || it.hasExpr }?.let { attr ->
            val id = attr.value.removePrefix("@+id/").removePrefix("@id/")
            ids.getOrPut(id) { ArrayList() } += element.id
        }
        if (resolved != null && !namespaced) {
            val unsupported = catalog.unsupported(resolved.viewClass)
            for (attr in element.attrs) {
                val canonical = catalog.shorthands[attr.name] ?: attr.name
                if (canonical in unsupported) {
                    out += Diagnostic(element.id, attr.name, Severity.ERROR, "${attr.name} is rejected by the host for <${element.tag}>")
                    continue
                }
                val entry = catalog.attr(resolved.viewClass, attr.name)
                val creation = entry == null && catalog.creationOnly(element.tag).any { attr.name in it.attrs }
                if (entry == null && !creation) {
                    out += Diagnostic(element.id, attr.name, Severity.WARNING, "Unknown attribute ${attr.name} on <${element.tag}>; the host ignores it")
                    continue
                }
                if (entry != null && entry.group == RELATIVE_GROUP && parent != null && resolve(parent.tag)?.viewClass?.let { it != "RelativeLayout" && !catalog.chain(it).contains("RelativeLayout") } == true) {
                    out += Diagnostic(element.id, attr.name, Severity.WARNING, "${attr.name} only applies inside <relative>")
                }
                validateValue(element, attr, entry, out)
            }
        }
        val tag = resolved?.tag
        val elementChildren = element.elementChildren
        if (tag != null) {
            if (tag.tag == "list" || tag.tag == "grid") {
                if (elementChildren.size != 1) out += Diagnostic(element.id, "children", Severity.ERROR, "<${tag.tag}> needs exactly one element child as its item template")
            } else if (!tag.container && elementChildren.isNotEmpty()) {
                out += Diagnostic(element.id, "children", Severity.WARNING, "<${element.tag}> does not take element children")
            }
        }
        for (child in elementChildren) validate(child, element, out, ids, widgets)
    }

    private fun validateValue(element: XmlNode.Element, attr: XmlAttr, entry: UiCatalog.Attr?, out: MutableList<Diagnostic>) {
        if (attr.isBare) {
            if (entry != null && entry.type != "boolean") out += Diagnostic(element.id, attr.name, Severity.WARNING, "${attr.name} is written without a value")
            return
        }
        val parts = attr.parts.orEmpty()
        if (parts.any { it is AttrPart.Binding && it.expression.isBlank() }) {
            out += Diagnostic(element.id, attr.name, Severity.ERROR, "Empty {{ }} binding in ${attr.name}")
            return
        }
        if (attr.hasBinding || attr.hasExpr || entry == null) return
        val value = XmlEntities.decode(attr.value).trim()
        val problem: String? = when (entry.type) {
            "boolean" -> if (value == "true" || value == "false") null else "expects true or false"
            "int" -> if (INT.matches(value)) null else "expects an integer"
            "float" -> if (NUMBER.matches(value)) null else "expects a number"
            "dimension" -> if (value.split(WHITESPACE).all { isDimension(it, entry.name) }) null else "expects a dimension such as 16, 8dp or 14sp"
            "color" -> when {
                COLOR_HEX.matches(value) || value.startsWith("@color/") || value.startsWith("?") -> null
                NAMED.matches(value) -> { out += Diagnostic(element.id, attr.name, Severity.WARNING, "Named color \"$value\" is not checked"); null }
                else -> "expects a color such as #RRGGBB"
            }
            "gravity" -> if (value.split('|').all { it.trim() in catalog.gravity }) null else "expects gravity words joined with |"
            "enum" -> if (value in entry.values) null else "expects one of ${entry.values.joinToString(", ")}"
            "id" -> if (ID.matches(value)) null else "expects an identifier"
            else -> null
        }
        if (problem != null) out += Diagnostic(element.id, attr.name, Severity.ERROR, "${attr.name}=\"$value\" $problem")
    }

    private fun isDimension(token: String, attrName: String): Boolean {
        if (token.isEmpty()) return false
        if (DIMENSION.matches(token) || token.startsWith("?")) return true
        if (attrName == "width" || attrName == "height" || attrName == "minWidth" || attrName == "minHeight") {
            return token in SIZE_WORDS || PERCENT.matches(token)
        }
        return false
    }

    private companion object {
        const val RELATIVE_GROUP = "RelativeLayout 子视图规则"
        val INT = Regex("""-?\d+""")
        val NUMBER = Regex("""-?\d+(\.\d+)?""")
        val DIMENSION = Regex("""-?\d+(\.\d+)?(px|dp|dip|sp|pt|in|mm)?""")
        val PERCENT = Regex("""\d+(\.\d+)?%""")
        val COLOR_HEX = Regex("""#[0-9a-fA-F]{3,8}""")
        val NAMED = Regex("""[A-Za-z][A-Za-z0-9_-]*""")
        val ID = Regex("""(@\+?id/)?[A-Za-z_][A-Za-z0-9_]*""")
        val WHITESPACE = Regex("""\s+""")
        val SIZE_WORDS = setOf("wrap_content", "match_parent", "fill_parent", "auto", "*")
    }
}
