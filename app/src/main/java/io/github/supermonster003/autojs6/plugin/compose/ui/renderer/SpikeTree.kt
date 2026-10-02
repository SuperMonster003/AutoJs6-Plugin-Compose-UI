package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.spike.ComposeUiContractException
import org.autojs.plugin.compose.api.spike.SpikeContract
import org.autojs.plugin.compose.api.spike.UiNode
import java.util.Collections

/** Bounded, detached snapshot: validate the entire replacement before publishing it to Compose. */
internal object SpikeTree {
    fun snapshot(root: UiNode): UiNode {
        var count = 0
        fun copy(node: UiNode, depth: Int): UiNode {
            if (++count > SpikeContract.MAX_NODES || depth > SpikeContract.MAX_DEPTH ||
                node.text.length > SpikeContract.MAX_TEXT || node.callbackId < 0 ||
                node.type !in setOf(SpikeContract.COLUMN, SpikeContract.TEXT, SpikeContract.BUTTON) ||
                (node.type != SpikeContract.COLUMN && node.children.isNotEmpty()) ||
                node.children.size > SpikeContract.MAX_NODES
            ) throw ComposeUiContractException(SpikeContract.INVALID_TREE)
            return node.copy(children = Collections.unmodifiableList(node.children.map { copy(it, depth + 1) }))
        }
        return copy(root, 1)
    }
}
