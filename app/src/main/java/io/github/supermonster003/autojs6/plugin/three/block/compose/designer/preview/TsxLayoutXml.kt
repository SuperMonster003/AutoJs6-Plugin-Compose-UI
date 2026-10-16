package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlEntities
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode

/**
 * A TSX layout as the layout XML the host's TSX runtime makes of it (roadmap P11.7; host `TypeScriptTsxRuntime`),
 * for the preview request: event attributes (`onClick`) are bound by the runtime and dropped here, a bare attribute is
 * `"true"`, the text children of the text-like tags become their `text` attribute, children that hold only a comment
 * vanish, and attributes are sorted by name as the runtime writes them. Element ids and the preorder stay the same,
 * so bounds map back to the model. `{expr}` values stay expressions; the request resolves the literal ones from
 * [literalValues] (by the expression text without braces) and notes the rest.
 */
internal object TsxLayoutXml {
    private val EVENT = Regex("""on[A-Z][A-Za-z0-9]*""")
    private val TEXT_TAGS = setOf("button", "checkbox", "input", "radio", "switch", "text")

    fun lower(element: XmlNode.Element, literalValues: Map<String, String>): XmlNode.Element {
        val attrs = element.attrs
            .filterNot { EVENT.matches(it.name) }
            .map { if (it.isBare) XmlAttr.literal(it.name, "true") else it.copy(source = null) }
            .sortedBy { it.name }
            .toMutableList()
        val children = element.children.filterNot { it is XmlNode.Expr && isComment(it.source) }
        val elements = children.filterIsInstance<XmlNode.Element>()
        val texts = children.filter { it is XmlNode.Text || it is XmlNode.Expr }
        if (element.tag in TEXT_TAGS && elements.isEmpty() && texts.isNotEmpty() && attrs.none { it.name == "text" }) {
            val values = texts.map { child ->
                when (child) {
                    is XmlNode.Text -> XmlEntities.decode(child.text)
                    else -> literalValues[XmlPreviewFrames.expressionOf(child.source!!)]
                }
            }
            if (values.all { it != null }) {
                val text = values.joinToString("").trim()
                if (text.isNotEmpty()) attrs += XmlAttr.literal("text", XmlEntities.encodeAttribute(text, '"'))
                attrs.sortBy { it.name }
                return element.copy(attrs = attrs, children = emptyList(), selfClosing = true, startTagTail = "/", startTagSource = null, endTagSource = null)
            }
        }
        val lowered = children.mapNotNull { child ->
            when (child) {
                is XmlNode.Element -> lower(child, literalValues)
                // Blank text between elements means nothing to the runtime.
                is XmlNode.Text -> child.takeUnless { it.isBlank && elements.isNotEmpty() }
                else -> child
            }
        }
        return element.copy(
            attrs = attrs, children = lowered, selfClosing = lowered.isEmpty(), startTagTail = if (lowered.isEmpty()) "/" else "",
            startTagSource = null, endTagSource = null,
        )
    }

    private fun isComment(source: String?): Boolean =
        source.orEmpty().trim().removePrefix("{").removeSuffix("}").trim().let { it.isEmpty() || (it.startsWith("/*") && it.endsWith("*/")) }
}
