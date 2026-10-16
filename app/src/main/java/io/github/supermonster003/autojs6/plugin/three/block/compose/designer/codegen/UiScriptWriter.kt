package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiSegment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlAttr
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlFragment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode

/**
 * Writes a [UiDocument] back to script text (roadmap P10.1 / D49). Opaque segments and every node that still
 * carries its source are written byte for byte; only start tags, end tags and attributes without a source are
 * generated, with one space before each attribute and the tag's own tail.
 */
object UiScriptWriter {

    fun write(document: UiDocument): String = buildString {
        for (segment in document.segments) when (segment) {
            is UiSegment.Opaque -> append(segment.source)
            is UiSegment.Layout -> append(write(segment.fragment.root))
        }
    }

    /** The text of every layout file the document refers to (roadmap P11.3), by the path the script writes. */
    fun writeFiles(document: UiDocument): Map<String, String> = document.files.associate { it.file!!.path to writeFile(it) }

    /** A layout file fragment back to its file text: prefix, root and suffix, with the file's own line breaks. */
    fun writeFile(fragment: XmlFragment): String {
        val file = fragment.file ?: error("not a layout file fragment")
        val text = file.prefix + write(fragment.root) + file.suffix
        return if (file.crlf) text.replace("\n", "\r\n") else text
    }

    fun write(node: XmlNode): String = when (node) {
        is XmlNode.Element -> buildString {
            append(node.startTagSource ?: startTag(node))
            if (!node.selfClosing) {
                node.children.forEach { append(write(it)) }
                append(node.endTagSource ?: "</${node.tag}>")
            }
        }
        else -> node.source.orEmpty()
    }

    fun startTag(element: XmlNode.Element): String = buildString {
        append('<').append(element.tag)
        element.attrs.forEach { attr -> append(attr.source ?: (" " + attribute(attr))) }
        append(element.startTagTail).append('>')
    }

    fun attribute(attr: XmlAttr): String {
        if (attr.isBare) return attr.name
        val quote = attr.quote ?: '"'
        return attr.name + "=" + quote + attr.value + quote
    }
}
