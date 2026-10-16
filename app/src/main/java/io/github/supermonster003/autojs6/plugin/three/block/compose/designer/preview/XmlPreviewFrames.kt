package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.AttrPart
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlEntities
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText

/** What the preview request could not carry faithfully, by element and attribute (null for content). */
internal data class XmlPreviewNote(val elementId: Int, val attrName: String?, val message: String)

/**
 * The values the request draws on (roadmap P10.3): the evaluated top-level declarations of the script by name
 * (JSON text each, from the sandbox's `DeclarationEvaluator`), overrides from the bindings panel (JSON text,
 * winning over the script), and the `{expr}` texts the script's E4X literals compute (the expression source,
 * without braces, to its string value; absent when it could not be evaluated).
 */
internal data class XmlPreviewValues(
    val declarations: Map<String, String> = emptyMap(),
    val overrides: Map<String, String> = emptyMap(),
    val expressions: Map<String, String> = emptyMap(),
) {
    companion object {
        val EMPTY = XmlPreviewValues()
    }
}

/**
 * One `setLayout` request for the host's preview service: the XML text of a fragment with every E4X `{expr}`
 * replaced, the `{{ }}` binding context as JSON, the element ids in the host's preorder, and notes.
 */
internal class XmlPreviewRequest(
    val xml: String,
    val bindingsJson: String,
    /** Element ids by preorder index: the host numbers the elements of its converted document the same way. */
    val elementIds: List<Int>,
    /** Identifiers the `{{ }}` expressions of the fragment use, in order of first use. */
    val bindingNames: List<String>,
    val notes: List<XmlPreviewNote>,
    /** The layout file the fragment comes from (roadmap P11.3), relative to the script, or null for a literal. */
    val layoutPath: String? = null,
    /** The script's `ui.useAndroidLayout` mode, null when automatic. */
    val androidLayout: Boolean? = null,
    /** The current text of every layout file of the document, by path: the files the host resolves sit beside them. */
    val files: Map<String, String> = emptyMap(),
) {
    fun elementIdAt(index: Int): Int? = elementIds.getOrNull(index)
    fun indexOf(elementId: Int): Int = elementIds.indexOf(elementId)
}

/**
 * Builds preview requests from a [UiDocument] (roadmap P10.3 / D47 level 1). The fragment's concrete syntax tree
 * is serialized as it would be written back, except that E4X `{expr}` fragments, which only a running script
 * could evaluate, are replaced: in an attribute by the expression's string value (or its source, with a note),
 * in content by the value as text (or a comment placeholder, with a note). Comments do not count as elements,
 * so the preorder of the host's document stays the preorder of this tree. The binding context holds the
 * declarations the `{{ }}` expressions name, when the script defined them with a value the sandbox could compute.
 */
internal object XmlPreviewFrames {

    private val IDENTIFIER = Regex("""(?<![\w$.])[A-Za-z_$][\w$]*""")
    private val RESERVED = setOf(
        "true", "false", "null", "undefined", "typeof", "instanceof", "new", "this", "void", "delete", "in", "of",
        "function", "return", "if", "else", "var", "let", "const", "NaN", "Infinity",
    )

    fun build(document: UiDocument, fragment: Int = document.rootFragment, values: XmlPreviewValues = XmlPreviewValues.EMPTY): XmlPreviewRequest {
        val written = requireNotNull(document.fragments.getOrNull(fragment)?.root) { "The document has no fragment $fragment" }
        // Roadmap P11.7: a TSX layout goes as the XML the host's TSX runtime makes of it, its literal {expr}s known.
        val tsx = document.dialect == Dialect.TSX
        val root = if (tsx) TsxLayoutXml.lower(written, document.literalValues) else written
        @Suppress("NAME_SHADOWING")
        val values = if (tsx) values.copy(expressions = document.literalValues + values.expressions) else values
        val notes = ArrayList<XmlPreviewNote>()
        val names = LinkedHashSet<String>()
        val xml = buildString { writeElement(root, values, notes, names, this) }
        val bindings = buildString {
            append('{')
            var first = true
            for (name in names) {
                val value = values.overrides[name] ?: values.declarations[name] ?: continue
                if (!first) append(',')
                first = false
                append(JsonText.quote(name)).append(':').append(value)
            }
            append('}')
        }
        return XmlPreviewRequest(
            xml, bindings, root.descendants().map { it.id }.toList(), names.toList(), notes,
            document.fragments[fragment].file?.path, document.androidLayout, UiScriptWriter.writeFiles(document),
        )
    }

    /** The identifiers a `{{ }}` expression may resolve on the binding context, in order of first use. */
    fun identifiersOf(expression: String): List<String> =
        IDENTIFIER.findAll(expression).map { it.value }.filter { it !in RESERVED }.distinct().toList()

    private fun writeElement(element: XmlNode.Element, values: XmlPreviewValues, notes: MutableList<XmlPreviewNote>, names: MutableSet<String>, out: StringBuilder) {
        element.attrs.forEach { attr -> attr.parts?.forEach { part -> if (part is AttrPart.Binding) names += identifiersOf(part.expression) } }
        val needsRewrite = element.attrs.any { it.hasExpr }
        if (!needsRewrite && element.startTagSource != null) {
            out.append(element.startTagSource)
        } else {
            out.append('<').append(element.tag)
            element.attrs.forEach { attr ->
                if (!attr.hasExpr && attr.source != null) out.append(attr.source)
                else out.append(' ').append(attributeText(element, attr, values, notes))
            }
            out.append(element.startTagTail).append('>')
        }
        if (element.selfClosing) return
        element.children.forEach { child ->
            when (child) {
                is XmlNode.Element -> writeElement(child, values, notes, names, out)
                is XmlNode.Text -> out.append(child.text)
                is XmlNode.Expr -> {
                    val value = values.expressions[expressionOf(child.source)]
                    if (value != null) {
                        out.append(XmlEntities.encodeText(value))
                    } else {
                        out.append("<!-- ").append(child.source.replace("--", "- -")).append(" -->")
                        notes += XmlPreviewNote(element.id, null, "${child.source} is not evaluated in the preview")
                    }
                }
                else -> out.append(child.source.orEmpty())
            }
        }
        out.append(element.endTagSource ?: "</${element.tag}>")
    }

    private fun attributeText(element: XmlNode.Element, attr: XmlAttr, values: XmlPreviewValues, notes: MutableList<XmlPreviewNote>): String {
        if (attr.isBare) return attr.name
        val quote = attr.quote ?: '"'
        val value = buildString {
            attr.parts?.forEach { part ->
                when (part) {
                    is AttrPart.Expr -> {
                        val evaluated = values.expressions[expressionOf(part.source)]
                        if (evaluated != null) {
                            append(XmlEntities.encodeAttribute(evaluated, quote))
                        } else {
                            append(part.source)
                            notes += XmlPreviewNote(element.id, attr.name, "${part.source} in ${attr.name} shows its source: the preview cannot evaluate it")
                        }
                    }
                    else -> append(part.raw)
                }
            }
        }
        return attr.name + "=" + quote + value + quote
    }

    /** The expression inside an E4X `{...}` fragment, trimmed. */
    fun expressionOf(source: String): String = source.trim().removePrefix("{").removeSuffix("}").trim()
}
