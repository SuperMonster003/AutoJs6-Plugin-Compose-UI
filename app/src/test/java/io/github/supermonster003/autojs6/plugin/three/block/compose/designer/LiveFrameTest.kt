package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import androidx.compose.runtime.snapshots.Snapshot
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LiveFrame
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.RenderFrame
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P9.1: the canvas keeps one frame, and an update invalidates only the readers of changed nodes. */
class LiveFrameTest {
    private fun text(id: Int, text: String) = UiNode(id, "Text", props = mapOf("text" to UiValue.Str(text)))
    private fun frame(root: Int, children: List<Int>, vararg texts: UiNode) =
        RenderFrame(root, (listOf(UiNode(root, "Column", children = children)) + texts).associateBy { it.nodeId }, 1)

    /** Ids whose state [LiveFrame.update] wrote, seen as the composition sees them: reads, then apply notifications. */
    private fun written(live: LiveFrame, rendered: RenderFrame, source: RenderFrame): Set<Int> {
        val readers = HashMap<Any, Int>()
        rendered.nodes.keys.toList().forEach { id -> Snapshot.observe(readObserver = { state -> readers[state] = id }) { rendered.nodes[id] } }
        val changed = HashSet<Int>()
        val handle = Snapshot.registerApplyObserver { states, _ -> states.forEach { state -> readers[state]?.let(changed::add) } }
        try {
            Snapshot.withMutableSnapshot { assertSame(rendered, live.update(source)) }
        } finally {
            handle.dispose()
        }
        return changed
    }

    @Test
    fun `the frame stays one instance and only changed nodes are written`() {
        val live = LiveFrame()
        val rendered = live.update(frame(1, listOf(2, 3, 4), text(2, "a"), text(3, "b"), text(4, "c")))
        assertEquals(emptySet<Int>(), written(live, rendered, frame(1, listOf(2, 3, 4), text(2, "a"), text(3, "b"), text(4, "c"))))
        assertEquals(setOf(3), written(live, rendered, frame(1, listOf(2, 3, 4), text(2, "a"), text(3, "B"), text(4, "c"))))
        assertEquals("B", (rendered.nodes.getValue(3).props["text"] as UiValue.Str).value)
        // Removing a child rewrites only its parent's child list; the removed node is not touched.
        assertEquals(setOf(1), written(live, rendered, frame(1, listOf(2, 4), text(2, "a"), text(4, "c"))))
    }

    @Test
    fun `missing nodes are dropped one update late and a new root makes a new frame`() {
        val live = LiveFrame()
        val rendered = live.update(frame(1, listOf(2, 3), text(2, "a"), text(3, "b")))
        live.update(frame(1, listOf(2), text(2, "a")))
        assertTrue("kept while a parent may not have recomposed yet", rendered.nodes.containsKey(3))
        live.update(frame(1, listOf(2), text(2, "a")))
        assertFalse(rendered.nodes.containsKey(3))
        assertEquals(setOf(1, 2), rendered.nodes.keys)
        assertNotSame(rendered, live.update(frame(9, listOf(2), text(2, "a"))))
    }
}
