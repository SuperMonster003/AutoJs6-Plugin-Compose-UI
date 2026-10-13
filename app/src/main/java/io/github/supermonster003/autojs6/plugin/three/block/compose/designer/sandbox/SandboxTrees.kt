package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HandlerSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.HostKind
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierArg
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.ModifierEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.MountDecl
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeEntry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Segment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LiveHooks
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LiveTree
import org.autojs.plugin.compose.api.ComposeUiCommands
import org.autojs.plugin.compose.api.model.UiCommand

/**
 * Reads a tree of the preview runtime (roadmap P9.3 step 2) into a [LiveTree]: a throwaway document whose node ids
 * are the runtime's, so the design-time frame builder draws it like any document, with the events that have
 * listeners, the modifier callback tokens, the edit sequences of the fields and the handle commands issued since
 * the last tree. The edited document is never involved.
 */
internal object SandboxTrees {
    /** A callback argument's place in a modifier chain; its token travels in [LiveHooks.tokens]. */
    private val CALLBACK = ModifierArg.Callback(HandlerSource(emptyList(), ""))

    fun parse(json: String): LiveTree {
        val snapshot = JsonText.parse(json) as Map<*, *>
        val events = HashMap<Int, List<String>>()
        val tokens = HashMap<Int, List<Int>>()
        var maxId = 0
        fun node(raw: Map<*, *>): DesignNode {
            val id = (raw["id"] as Number).toInt()
            maxId = maxOf(maxId, id)
            val entries = ArrayList<NodeEntry>()
            (raw["key"] as? String)?.let { entries += NodeEntry.Key(PropValue.str(it)) }
            (raw["props"] as? Map<*, *>)?.forEach { (name, value) -> entries += NodeEntry.Prop(name as String, propValue(value)) }
            val chainTokens = ArrayList<Int>()
            val chain = (raw["modifier"] as? List<*>).orEmpty().map { operation ->
                operation as Map<*, *>
                ModifierEntry(operation["name"] as String, (operation["args"] as? List<*>).orEmpty().map { arg ->
                    val token = ((arg as? Map<*, *>)?.get("\$callback") as? Number)?.toInt()
                    if (token != null) { chainTokens += token; CALLBACK } else ModifierArg.Value(propValue(arg))
                })
            }
            if (chain.isNotEmpty()) entries += NodeEntry.Modifier(chain)
            (raw["slots"] as? Map<*, *>)?.forEach { (name, child) -> entries += NodeEntry.Slot(name as String, DesignChild.Node(node(child as Map<*, *>))) }
            val children = (raw["children"] as? List<*>).orEmpty().map { DesignChild.Node(node(it as Map<*, *>)) }
            events[id] = (raw["events"] as? List<*>).orEmpty().filterIsInstance<String>()
            if (chainTokens.isNotEmpty()) tokens[id] = chainTokens
            return DesignNode(id, raw["type"] as String, entries, children)
        }
        val root = (snapshot["root"] as? Map<*, *>)?.let(::node)
        val kind = HostKind.byMethod(snapshot["kind"] as? String ?: "") ?: HostKind.MOUNT
        val segments = if (root == null) emptyList() else listOf(Segment.Mount(MountDecl(kind, root)))
        val sequences = (snapshot["fields"] as? Map<*, *>).orEmpty().entries.mapNotNull { (id, sequence) ->
            ((id as String).toIntOrNull() ?: return@mapNotNull null) to ((sequence as? Number)?.toLong() ?: 0L)
        }.toMap()
        val commands = (snapshot["commands"] as? List<*>).orEmpty().mapNotNull { raw -> command(raw as Map<*, *>) }
        return LiveTree(DesignDocument(segments, maxId + 1), LiveHooks(events, tokens, sequences, commands))
    }

    private fun command(raw: Map<*, *>): UiCommand? {
        val id = (raw["id"] as? Number)?.toInt() ?: return null
        return when (raw["kind"]) {
            ComposeUiCommands.FOCUS -> UiCommand.Focus(id)
            ComposeUiCommands.BLUR -> UiCommand.Blur(id)
            ComposeUiCommands.SCROLL_TO -> UiCommand.ScrollTo(id, (raw["index"] as? Number)?.toInt(), (raw["offset"] as? Number)?.toInt())
            else -> null
        }
    }

    private fun propValue(raw: Any?): PropValue {
        val expression = (raw as? Map<*, *>)?.takeIf { it.size == 1 }?.get("\$expr") as? String
        return if (expression != null) PropValue.Expression(expression) else PropValue.Literal(literal(raw))
    }

    private fun literal(raw: Any?): JsLiteral = when (raw) {
        null -> JsLiteral.Null
        is String -> JsLiteral.Str(raw)
        is Boolean -> JsLiteral.Bool(raw)
        is Number -> JsLiteral.Num(raw.toDouble())
        is List<*> -> JsLiteral.Arr(raw.map(::literal))
        is Map<*, *> -> (raw.takeIf { it.size == 1 }?.get("\$expr") as? String)?.let { JsLiteral.Str(it) }
            ?: JsLiteral.Obj(raw.map { (name, value) -> name as String to literal(value) })
        else -> JsLiteral.Str(raw.toString())
    }
}
