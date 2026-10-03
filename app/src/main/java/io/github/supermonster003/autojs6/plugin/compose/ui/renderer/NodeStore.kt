package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.model.*

/** Private transaction workspace; visible state changes only after full validation. */
internal class NodeStore(private val sessionId: Int, private val validateRendererNode: (UiNode) -> Unit = {}, private val allowUnknownDebug: Boolean = false) {
    var tree: UiTree? = null
        private set
    var generation = 0L
        private set
    private var acceptedBatch = false

    fun apply(batch: UiPatchBatch): UiTree? {
        if (batch.sessionId != sessionId || acceptedBatch && batch.generation <= generation) invalid()
        var root = tree?.rootId
        val nodes = tree?.nodes?.associateBy { it.nodeId }?.toMutableMap() ?: linkedMapOf()
        fun node(id: Int) = nodes[id] ?: throw ComposeUiContractException(ComposeUiErrorCodes.NODE_DETACHED, nodeId = id)
        fun put(value: UiNode) { nodes[value.nodeId] = value }
        fun removeSubtree(id: Int) {
            // Intermediate moves can temporarily exceed the final depth budget. Never recurse.
            val pending = java.util.ArrayDeque<Int>().apply { add(id) }
            while (pending.isNotEmpty()) {
                val removed = nodes.remove(pending.removeLast()) ?: invalid()
                pending.addAll(removed.children); pending.addAll(removed.slots.values)
            }
        }
        fun addSubtree(subtree: UiTree) {
            if (subtree.nodes.any { it.nodeId in nodes }) invalid()
            if (subtree.nodes.size > ComposeUiLimits.MAX_NODES_PER_SESSION - nodes.size) {
                throw ComposeUiContractException(ComposeUiErrorCodes.LIMIT_EXCEEDED)
            }
            subtree.nodes.forEach(::put)
        }
        for (patch in batch.patches) when (patch) {
            is UiPatch.SetRoot -> { nodes.clear(); addSubtree(patch.tree); root = patch.tree.rootId }
            is UiPatch.SetProps -> {
                val before = node(patch.nodeId)
                put(UiNode(before.nodeId, before.type, before.key, patch.props, patch.modifier ?: before.modifier,
                    before.children, before.slots, patch.callbacks ?: before.callbacks))
            }
            is UiPatch.Insert -> {
                val parent = node(patch.parentId)
                if (patch.index !in 0..parent.children.size) invalid()
                addSubtree(patch.subtree)
                put(parent.with(children = parent.children.toMutableList().apply { add(patch.index, patch.subtree.rootId) }))
            }
            is UiPatch.Remove -> {
                val parent = node(patch.parentId)
                if (patch.nodeId !in parent.children) invalid()
                put(parent.with(children = parent.children - patch.nodeId))
                removeSubtree(patch.nodeId)
            }
            is UiPatch.Move -> {
                node(patch.nodeId)
                // Reject ancestry cycles before mutating. A later remove must never traverse a cycle.
                val descendants = java.util.ArrayDeque<Int>().apply { add(patch.nodeId) }
                while (descendants.isNotEmpty()) {
                    val id = descendants.removeLast()
                    if (id == patch.parentId) invalid()
                    val descendant = node(id)
                    descendants.addAll(descendant.children); descendants.addAll(descendant.slots.values)
                }
                val source = nodes.values.singleOrNull { patch.nodeId in it.children } ?: invalid()
                put(source.with(children = source.children - patch.nodeId))
                val target = node(patch.parentId)
                if (patch.toIndex !in 0..target.children.size) invalid()
                put(target.with(children = target.children.toMutableList().apply { add(patch.toIndex, patch.nodeId) }))
            }
            is UiPatch.ReplaceSlot -> {
                val parent = node(patch.parentId)
                parent.slots[patch.slot]?.let(::removeSubtree)
                patch.subtree?.let(::addSubtree)
                val slots = parent.slots.toMutableMap().apply {
                    remove(patch.slot); patch.subtree?.let { put(patch.slot, it.rootId) }
                }
                put(parent.with(slots = slots))
            }
        }
        val candidate = root?.let { UiTree(it, nodes.values.toList()) }
        val parents = HashMap<Int, String>()
        candidate?.nodes?.forEach { parent -> (parent.children + parent.slots.values).forEach { parents[it] = parent.type } }
        candidate?.nodes?.forEach {
            if (!allowUnknownDebug || ComponentCatalog.V1.component(it.type) != null) {
                ComponentCatalog.V1.validateNode(it, parents[it.nodeId]); validateRendererNode(it)
            }
        }
        tree = candidate
        generation = batch.generation
        acceptedBatch = true
        return candidate
    }

    fun clear() { tree = null }
    private fun UiNode.with(children: List<Int> = this.children, slots: Map<String, Int> = this.slots) =
        UiNode(nodeId, type, key, props, modifier, children, slots, callbacks)
    private fun invalid(): Nothing = throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT)
}
