package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.MountDecl
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Segment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsxText

/**
 * Writes a [DesignDocument] as a TSX compose script (roadmap P9.2 / D42), in the format of [ScriptWriter]:
 * four-space indentation, single-quoted strings (`text='Hello'`), every child on its own line. Elements use
 * `compose.X` tags; a node without children closes itself; a single plain text child stays inline
 * (`<compose.Button>Add</compose.Button>`). Literals other than strings, state bindings, expressions,
 * handlers and modifier chains are written in `{}`; a string that bare JSX text or a quoted attribute would
 * change is written as `{'...'}`. Opaque segments, expressions and handler bodies are written verbatim, so
 * reading a written script yields an equal document and writing it again yields the same text.
 */
object TsxWriter {
    private const val INDENT = "    "

    fun write(document: DesignDocument): String {
        val out = StringBuilder()
        var previous: Segment? = null
        document.segments.forEach { segment ->
            if (previous != null) out.append(if (segment is Segment.State && previous is Segment.State) "\n" else "\n\n")
            when (segment) {
                is Segment.Opaque -> out.append(segment.source)
                is Segment.State -> {
                    val decl = segment.decl
                    val typeArguments = decl.typeArguments?.let { "<$it>" } ?: ""
                    out.append("${decl.keyword} ${decl.name} = compose.state$typeArguments(${ScriptWriter.value(decl.initial, "")});")
                }
                is Segment.Mount -> out.append(mount(segment.decl))
            }
            previous = segment
        }
        out.append('\n')
        return out.toString()
    }

    fun mount(decl: MountDecl): String {
        val prefix = decl.bindName?.let { name -> if (decl.bindKeyword.isNullOrEmpty()) "$name = " else "${decl.bindKeyword} $name = " } ?: ""
        val options = decl.options?.let { ", " + ScriptWriter.options(it) } ?: ""
        if (!decl.render) return prefix + "compose." + decl.kind.method + "(" + element(decl.root, "") + options + ");"
        return buildString {
            append(prefix).append("compose.").append(decl.kind.method).append('(')
            append(if (decl.renderArrow) "() => {" else "function () {").append('\n')
            decl.renderPrelude?.lines()?.forEach { line -> append(if (line.isBlank()) "" else INDENT + line).append('\n') }
            val root = element(decl.root, INDENT + INDENT)
            if ('\n' in root) append(INDENT).append("return (\n").append(INDENT + INDENT).append(root).append('\n').append(INDENT).append(");\n")
            else append(INDENT).append("return ").append(root).append(";\n")
            append('}').append(options).append(");")
        }
    }

    /** The element for [node], starting at the current position; nested lines are indented from [indent]. */
    fun element(node: DesignNode, indent: String): String {
        val tag = "compose." + node.type
        val attributes = node.entries.joinToString("") { " " + attribute(it, indent) }
        if (node.children.isEmpty()) return "<$tag$attributes />"
        val inline = (node.children.singleOrNull() as? DesignChild.Node)?.node?.let { plainText(it) }?.takeIf { JsxText.isSafeChild(it) }
        if (inline != null) return "<$tag$attributes>$inline</$tag>"
        val inner = indent + INDENT
        val lines = ArrayList<String>()
        var previousBare = false
        node.children.forEach { child ->
            val text = (child as? DesignChild.Node)?.node?.let { plainText(it) }
            if (text != null) {
                // Two bare text lines in a row would read back as one text joined by a space.
                val bare = !previousBare && JsxText.isSafeChild(text)
                lines += inner + if (bare) text else "{" + ScriptWriter.quote(text) + "}"
                previousBare = bare
            } else {
                lines += inner + child(child, inner)
                previousBare = false
            }
        }
        return "<$tag$attributes>\n" + lines.joinToString("\n") + "\n" + indent + "</$tag>"
    }

    /** A child that is not plain text: an element, or an expression (bare when it is itself JSX). */
    fun child(child: DesignChild, indent: String): String = when (child) {
        is DesignChild.Node -> plainText(child.node)?.let { "{" + ScriptWriter.quote(it) + "}" } ?: element(child.node, indent)
        is DesignChild.Expression -> if (child.source.startsWith("<")) child.source else "{" + child.source + "}"
    }

    /** The string of a shorthand Text node that is still plain, or null. */
    private fun plainText(node: DesignNode): String? =
        if (node.shorthand && node.isPlainText) ((node.entries[0] as NodeEntry.Prop).value as PropValue.Literal).value.let { (it as JsLiteral.Str).value } else null

    private fun attribute(entry: NodeEntry, indent: String): String = when (entry) {
        is NodeEntry.Key -> "key=" + attributeValue(entry.value)
        is NodeEntry.Prop -> entry.name + "=" + attributeValue(entry.value)
        is NodeEntry.Slot -> entry.name + "=" + slotValue(entry.child, indent)
        is NodeEntry.Handler -> entry.name + "={" + ScriptWriter.handler(entry.handler, indent) + "}"
        is NodeEntry.Modifier -> "modifier={" + ScriptWriter.chain(entry.chain, indent) + "}"
        is NodeEntry.ModifierExpression -> "modifier={" + entry.source + "}"
    }

    fun attributeValue(value: PropValue): String {
        val string = ((value as? PropValue.Literal)?.value as? JsLiteral.Str)?.value
        if (string != null && JsxText.isSafeAttribute(string)) return "'$string'"
        return "{" + ScriptWriter.value(value, "") + "}"
    }

    private fun slotValue(child: DesignChild, indent: String): String = when (child) {
        is DesignChild.Node -> plainText(child.node)?.let { attributeValue(PropValue.str(it)) } ?: ("{" + element(child.node, indent) + "}")
        is DesignChild.Expression -> "{" + child.source + "}"
    }
}
