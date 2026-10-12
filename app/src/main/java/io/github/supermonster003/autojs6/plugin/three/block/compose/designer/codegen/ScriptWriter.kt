package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HandlerSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostOptions
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierArg
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.MountDecl
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Segment

/**
 * Writes a [DesignDocument] back as a compose script (roadmap P8.2). The layout follows the gallery's
 * `GalleryNode.render` conventions: four-space indentation, single-quoted strings, every child on its own
 * line. Opaque segments, opaque expressions and handler bodies are written verbatim, so reading a written
 * script yields an equal document and writing it again yields the same text.
 */
object ScriptWriter {
    private const val INDENT = "    "

    fun write(document: DesignDocument): String {
        val out = StringBuilder()
        var previous: Segment? = null
        document.segments.forEach { segment ->
            if (previous != null) out.append(if (segment is Segment.State && previous is Segment.State) "\n" else "\n\n")
            when (segment) {
                is Segment.Opaque -> out.append(segment.source)
                is Segment.State -> out.append("${segment.decl.keyword} ${segment.decl.name} = compose.state(${value(segment.decl.initial, "")});")
                is Segment.Mount -> out.append(mount(segment.decl))
            }
            previous = segment
        }
        out.append('\n')
        return out.toString()
    }

    fun mount(decl: MountDecl): String {
        val prefix = decl.bindName?.let { name -> if (decl.bindKeyword.isNullOrEmpty()) "$name = " else "${decl.bindKeyword} $name = " } ?: ""
        val options = decl.options?.let { ", " + options(it) } ?: ""
        if (!decl.render) return prefix + "compose." + decl.kind.method + "(" + node(decl.root, "") + options + ");"
        return buildString {
            append(prefix).append("compose.").append(decl.kind.method).append('(')
            append(if (decl.renderArrow) "() => {" else "function () {").append('\n')
            decl.renderPrelude?.lines()?.forEach { line -> append(if (line.isBlank()) "" else INDENT + line).append('\n') }
            append(INDENT).append("return ").append(node(decl.root, INDENT)).append(";\n")
            append('}').append(options).append(");")
        }
    }

    fun node(node: DesignNode, indent: String): String {
        val inner = indent + INDENT
        val props = node.entries.joinToString(", ") { entry(it, indent, inner) }
        if (node.children.isEmpty()) return if (props.isEmpty()) "compose.${node.type}()" else "compose.${node.type}({$props})"
        return "compose.${node.type}({$props},\n" + node.children.joinToString(",\n") { inner + child(it, inner) } + ")"
    }

    private fun entry(entry: NodeEntry, indent: String, inner: String): String = when (entry) {
        is NodeEntry.Key -> "key: ${value(entry.value, indent)}"
        is NodeEntry.Prop -> "${key(entry.name)}: ${value(entry.value, indent)}"
        is NodeEntry.Slot -> "${key(entry.name)}: ${child(entry.child, inner)}"
        is NodeEntry.Handler -> "${key(entry.name)}: ${handler(entry.handler, indent)}"
        is NodeEntry.Modifier -> "modifier: ${chain(entry.chain, indent)}"
        is NodeEntry.ModifierExpression -> "modifier: ${entry.source}"
    }

    fun child(child: DesignChild, indent: String): String = when (child) {
        is DesignChild.Node -> if (child.node.shorthand && child.node.isPlainText) literal((child.node.entries[0] as NodeEntry.Prop).value.let { (it as PropValue.Literal).value }) else node(child.node, indent)
        is DesignChild.Expression -> child.source
    }

    fun chain(chain: List<ModifierEntry>, indent: String): String = "compose.modifier()" + chain.joinToString("") { operation ->
        "." + operation.name + "(" + operation.args.joinToString(", ") { argument ->
            when (argument) {
                is ModifierArg.Value -> value(argument.value, indent)
                is ModifierArg.Callback -> handler(argument.handler, indent)
            }
        } + ")"
    }

    fun handler(handler: HandlerSource, indent: String): String {
        val params = handler.params.joinToString(", ")
        val head = if (handler.arrow) "($params) => " else "function ($params) "
        if (handler.arrow && handler.expressionBody) return head + handler.body
        if (handler.body.isEmpty()) return head + "{}"
        if (handler.isSingleLine) return head + "{ " + handler.body + " }"
        val inner = indent + INDENT
        return head + "{\n" + handler.body.lines().joinToString("\n") { line -> if (line.isBlank()) "" else inner + line } + "\n" + indent + "}"
    }

    fun value(value: PropValue, indent: String): String = when (value) {
        is PropValue.Literal -> literal(value.value)
        is PropValue.StateRef -> "${value.name}.value"
        is PropValue.Expression -> value.source
    }

    fun options(options: HostOptions): String = when (options) {
        is HostOptions.Fields -> "{" + options.fields.joinToString(", ") { (name, field) -> "${key(name)}: ${value(field, "")}" } + "}"
        is HostOptions.Expression -> options.source
    }

    fun literal(literal: JsLiteral): String = when (literal) {
        is JsLiteral.Str -> quote(literal.value)
        is JsLiteral.Num -> literal.text
        is JsLiteral.Bool -> literal.value.toString()
        JsLiteral.Null -> "null"
        is JsLiteral.Arr -> "[" + literal.items.joinToString(", ") { literal(it) } + "]"
        is JsLiteral.Obj -> "{" + literal.fields.joinToString(", ") { (name, field) -> "${key(name)}: ${literal(field)}" } + "}"
    }

    private val identifier = Regex("[A-Za-z_$][A-Za-z0-9_$]*")

    fun key(name: String): String = if (identifier.matches(name)) name else quote(name)

    fun quote(text: String): String {
        val out = StringBuilder("'")
        text.forEach { char ->
            when (char) {
                '\\' -> out.append("\\\\")
                '\'' -> out.append("\\'")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (char.code < 0x20) out.append("\\u%04x".format(char.code)) else out.append(char)
            }
        }
        return out.append('\'').toString()
    }
}
