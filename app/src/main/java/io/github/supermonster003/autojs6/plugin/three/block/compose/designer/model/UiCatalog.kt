package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText

/**
 * The ui (XML) tag and attribute catalog (roadmap P10.1, appendix C), generated from the documentation site by
 * `.python/generate_ui_catalog.py` into `resources/designer-ui/catalog.json`. Attributes belong to Android view
 * classes and apply to every tag whose class chain contains them, the way the host's inflater picks the nearest
 * registered attribute handler.
 */
class UiCatalog private constructor(private val data: Map<String, Any?>) {

    data class Tag(
        val tag: String,
        val aliases: List<String>,
        val viewClass: String,
        val category: String,
        val container: Boolean,
        val axis: String?,
        val summary: String,
    )

    data class Attr(
        val name: String,
        val aliases: List<String>,
        val type: String,
        val values: List<String>,
        val typeText: String,
        val summary: String,
        val group: String,
        val ownerClass: String,
    )

    data class CreationAttr(val tags: List<String>, val attrs: List<String>, val type: String, val values: List<String>, val summary: String)

    val docsCommit: String = data["docsCommit"] as String
    val tags: List<Tag> = (data["tags"] as List<*>).map { entry ->
        val map = entry as Map<*, *>
        Tag(
            map["tag"] as String, strings(map["aliases"]), map["viewClass"] as String, map["category"] as String,
            map["container"] as Boolean, map["axis"] as String?, map["summary"] as String,
        )
    }
    val shorthands: Map<String, String> = (data["shorthands"] as Map<*, *>).entries.associate { it.key as String to it.value as String }
    val gravity: Set<String> = strings(data["gravity"]).toSet()
    val dimensionUnits: Set<String> = strings(data["dimensionUnits"]).toSet()
    val creationOnly: List<CreationAttr> = (data["creationOnly"] as List<*>).map { entry ->
        val map = entry as Map<*, *>
        CreationAttr(strings(map["tags"]), strings(map["attrs"]), map["type"] as String, strings(map["values"]), map["summary"] as String)
    }

    private val chains: Map<String, List<String>> = (data["chains"] as Map<*, *>).entries.associate { it.key as String to strings(it.value) }
    private val attrsByClass: Map<String, List<Attr>> = (data["classes"] as Map<*, *>).entries.associate { (name, value) ->
        val className = name as String
        className to ((value as Map<*, *>)["attrs"] as List<*>).map { entry ->
            val map = entry as Map<*, *>
            Attr(
                map["name"] as String, strings(map["aliases"]), map["type"] as String, strings(map["values"]),
                map["typeText"] as String, map["summary"] as String, (map["group"] as? String).orEmpty(), className,
            )
        }
    }
    private val unsupportedByClass: Map<String, Set<String>> = (data["unsupported"] as Map<*, *>).entries.associate { it.key as String to strings(it.value).toSet() }
    private val tagIndex: Map<String, Tag> = tags.flatMap { tag -> tag.aliases.map { it to tag } }.toMap()

    /** The tag entry for [name] (a tag or an alias), or null for class-name tags and unknown names. */
    fun tag(name: String): Tag? = tagIndex[name]

    /** Whether [viewClass] is a class the catalog knows a chain for. */
    fun knowsClass(viewClass: String): Boolean = viewClass in chains

    /** [viewClass] followed by its parents, nearest first. */
    fun chain(viewClass: String): List<String> = listOf(viewClass) + chains[viewClass].orEmpty()

    /** Every attribute a view of [viewClass] accepts, nearest class first, one entry per name. */
    fun attrsFor(viewClass: String): List<Attr> {
        val seen = HashSet<String>()
        return chain(viewClass).flatMap { attrsByClass[it].orEmpty() }.filter { seen.add(it.name) }
    }

    /** The attribute [name] (a name, alias or shorthand) of [viewClass], or null. */
    fun attr(viewClass: String, name: String): Attr? {
        val canonical = shorthands[name] ?: name
        return attrsFor(viewClass).firstOrNull { it.name == canonical || canonical in it.aliases || name in it.aliases }
    }

    /** Attributes the host throws on for [viewClass]. */
    fun unsupported(viewClass: String): Set<String> = chain(viewClass).flatMap { unsupportedByClass[it].orEmpty() }.toSet()

    /** Creation-time attributes of [tag] (`isBorderless` of a button, `spinnerMode` of a spinner), plus the ones for any tag. */
    fun creationOnly(tag: String): List<CreationAttr> = creationOnly.filter { tag in it.tags || "*" in it.tags }

    companion object {
        const val RESOURCE = "designer-ui/catalog.json"

        @Volatile
        private var loaded: UiCatalog? = null

        /** The packaged catalog, read once. */
        fun load(): UiCatalog = loaded ?: synchronized(this) {
            loaded ?: parse(requireNotNull(UiCatalog::class.java.classLoader?.getResourceAsStream(RESOURCE)) { "missing $RESOURCE" }.use { String(it.readBytes(), Charsets.UTF_8) }).also { loaded = it }
        }

        @Suppress("UNCHECKED_CAST")
        fun parse(json: String): UiCatalog = UiCatalog(JsonText.parse(json) as Map<String, Any?>)

        private fun strings(value: Any?): List<String> = (value as? List<*>)?.map { it as String }.orEmpty()
    }
}
