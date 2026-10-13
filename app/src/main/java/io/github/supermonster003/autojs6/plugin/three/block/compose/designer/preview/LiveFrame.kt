package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.RenderFrame
import org.autojs.plugin.compose.api.model.UiNode
import java.util.AbstractMap.SimpleImmutableEntry

/**
 * The canvas's render frame across edits (roadmap P9.1). Each preview derives a new closed frame; handing
 * that to the renderer would recompose every node, because `RenderNode` takes the frame as a parameter. This
 * keeps one [RenderFrame] instance whose node map reads each node from its own snapshot state, and [update]
 * writes only the nodes whose contract value changed (structural equality), so `RenderNode` calls with
 * unchanged parameters are skipped and only changed nodes (and parents whose child lists changed) recompose.
 * The renderer itself is unchanged; the designer never forwards events, so the frame keeps its first
 * generation.
 */
internal class LiveFrame {
    private val states = HashMap<Int, MutableState<UiNode>>()
    private var absent: Set<Int> = emptySet()
    private var frame: RenderFrame? = null

    private val nodes = object : AbstractMap<Int, UiNode>() {
        override val entries: Set<Map.Entry<Int, UiNode>>
            get() = states.entries.mapTo(LinkedHashSet()) { (id, state) -> SimpleImmutableEntry(id, state.value) }

        override fun get(key: Int): UiNode? = states[key]?.value
        override fun containsKey(key: Int): Boolean = key in states
    }

    /** Takes the nodes of [source] and returns the frame to render, the same instance while the root stays. */
    fun update(source: RenderFrame): RenderFrame {
        source.nodes.forEach { (id, node) ->
            val state = states[id]
            if (state == null) states[id] = mutableStateOf(node) else state.value = node
        }
        // A node is dropped once it has been missing for two updates: by then every parent that listed it has
        // recomposed without it, so no scope can still look it up.
        absent.forEach { if (it !in source.nodes) states.remove(it) }
        absent = states.keys.filterTo(HashSet()) { it !in source.nodes }
        return frame?.takeIf { it.rootId == source.rootId } ?: RenderFrame(source.rootId, nodes, source.generation).also { frame = it }
    }
}
