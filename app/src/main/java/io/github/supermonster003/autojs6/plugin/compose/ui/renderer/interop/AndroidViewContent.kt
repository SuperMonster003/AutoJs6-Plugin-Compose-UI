package io.github.supermonster003.autojs6.plugin.compose.ui.renderer.interop

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.supermonster003.autojs6.plugin.compose.ui.renderer.RendererExtension
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.interop.AndroidViewBindingV1
import org.autojs.plugin.compose.api.interop.AndroidViewClaimsV1
import org.autojs.plugin.compose.api.interop.AndroidViewInteropV1
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiTree
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import java.util.IdentityHashMap

/** All mutations and native parent transitions are main-thread confined by the renderer. */
internal class AndroidViewContent(
    private val owner: Any,
    private val rendererRoot: () -> View?,
    private val report: (Throwable) -> Unit,
) : RendererExtension {
    override val catalog get() = ComposeUiV2.interopCatalog
    override fun handles(type: String) = type == AndroidViewInteropV1.COMPONENT
    private var bindings = emptyMap<Int, AndroidViewBindingV1>()
    private val slots = LinkedHashSet<GuardedAndroidViewSlot>()
    private var revision = 0L
    private var closed = false

    class Prepared internal constructor(internal val owner: AndroidViewContent, internal val revision: Long,
        internal val values: Map<Int, AndroidViewBindingV1>)

    fun prepare(tree: UiTree?, supplied: List<AndroidViewBindingV1>): Prepared {
        usable()
        if (supplied.size > ComposeUiLimits.MAX_NODES_PER_SESSION) fail(ComposeUiErrorCodes.LIMIT_EXCEEDED)
        val expected = tree?.nodes?.filter { handles(it.type) }?.associateBy { it.nodeId }.orEmpty()
        if (supplied.size != expected.size) fail()
        val next = LinkedHashMap<Int, AndroidViewBindingV1>()
        val identities = IdentityHashMap<View, Boolean>()
        val tokens = HashSet<Int>()
        for (value in supplied) {
            val node = expected[value.nodeId] ?: fail(nodeId = value.nodeId)
            if (next.put(value.nodeId, value) != null || identities.put(value.view, true) != null || !tokens.add(value.leaseId)) fail(nodeId = value.nodeId)
            val token = (node.props[AndroidViewInteropV1.PROP_VIEW] as? UiValue.Num)?.value ?: fail(nodeId = value.nodeId)
            if (token != value.leaseId.toDouble()) fail(nodeId = value.nodeId)
            bindings[value.nodeId]?.let { before ->
                if (before.leaseId == value.leaseId && before.view !== value.view) fail(nodeId = value.nodeId)
            }
            validateParent(value)
            var parent: View? = rendererRoot()
            val ancestors = IdentityHashMap<View, Boolean>()
            while (parent != null) {
                if (parent === value.view || ancestors.put(parent, true) != null) fail(nodeId = value.nodeId)
                parent = parent.parent as? View
            }
        }
        AndroidViewClaimsV1.validate(owner, next.values.toList())
        return Prepared(this, revision, next.toMap())
    }

    /** Native swaps precede publication; even a late failed child attachment restores earlier swaps. */
    fun commit(prepared: Prepared, prepareContainer: () -> Unit, publishTree: () -> Unit) {
        usable()
        if (prepared.owner !== this || prepared.revision != revision) fail()
        val before = bindings
        val changed = slots.toList().map { it to it.binding }
        var replacedClaims = false
        try {
            changed.forEach { (slot, _) -> usable(); swap(slot, prepared.values[slot.nodeId]) }
            // Both child forceLayout and renderer-parent requestLayout can be caller code.
            // They must finish before the irreversible in-memory tree/generation publication.
            slots.toList().forEach { usable(); it.recover() }
            prepareContainer()
            usable()
            AndroidViewClaimsV1.replace(owner, prepared.values.values.toList())
            replacedClaims = true
            bindings = prepared.values
            publishTree()
            revision++
        } catch (failure: Throwable) {
            if (!closed) {
                bindings = before
                if (replacedClaims) try { AndroidViewClaimsV1.replace(owner, before.values.toList()) } catch (rollback: Throwable) { failure.addSuppressed(rollback) }
                for ((slot, old) in changed.asReversed()) {
                    if (closed) break
                    try { swap(slot, old) } catch (rollback: Throwable) { failure.addSuppressed(rollback) }
                }
            }
            if (closed) {
                // Closing during a borrowed View callback wins over rollback. Never re-claim or
                // reattach the pre-close snapshot, including an in-progress addView's new child.
                bindings = emptyMap()
                (slots.toList() + changed.map { it.first }).distinct().forEach { slot ->
                    try { slot.release() } catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
                }
                slots.clear()
                try { AndroidViewClaimsV1.release(owner) } catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
            }
            throw failure
        }
    }

    @Composable override fun Content(node: UiNode, modifier: Modifier) {
        val expectedToken = (node.props.getValue(AndroidViewInteropV1.PROP_VIEW) as UiValue.Num).value.toInt()
        AndroidView(
            factory = { context -> GuardedAndroidViewSlot(context, node.nodeId, this, ::reportFailure).also { slots += it } },
            modifier = modifier,
            // Reuse of arbitrary borrowed Views would require caller-owned reset semantics. A fresh
            // plugin slot is cheap; the host's borrowed View survives lazy deactivation separately.
            onReset = null,
            onRelease = { slot -> release(slot) },
            update = { slot ->
                val current = bindings[node.nodeId]
                if (current?.leaseId == expectedToken) try { swap(slot, current) }
                catch (failure: RuntimeException) { reportFailure(node.nodeId, failure) }
                catch (failure: LinkageError) { reportFailure(node.nodeId, failure) }
            },
        )
    }

    private fun swap(slot: GuardedAndroidViewSlot, next: AndroidViewBindingV1?) {
        next?.let { value ->
            val parent = value.view.parent
            if (parent != null && parent !== slot) {
                if (parent !is GuardedAndroidViewSlot || parent.owner !== this || parent.nodeId != value.nodeId || parent.binding?.leaseId != value.leaseId) fail(nodeId = value.nodeId)
                // A stable node moved between composition parents. Detach only our own old slot;
                // its later onRelease cannot remove the View from this new slot.
                parent.replace(null)
            }
        }
        slot.replace(next)
    }

    private fun validateParent(value: AndroidViewBindingV1) {
        val parent = value.view.parent ?: return
        if (parent !is GuardedAndroidViewSlot || parent.owner !== this || parent.nodeId != value.nodeId ||
            parent.binding?.leaseId != value.leaseId || parent.binding?.view !== value.view) fail(nodeId = value.nodeId)
    }

    private fun release(slot: GuardedAndroidViewSlot) {
        slots.remove(slot)
        try { slot.release() }
        catch (failure: RuntimeException) { reportFailure(slot.nodeId, failure) }
        catch (failure: LinkageError) { reportFailure(slot.nodeId, failure) }
    }

    private fun reportFailure(nodeId: Int, failure: Throwable) {
        if (!closed) report(ComposeUiContractException(ComposeUiErrorCodes.RENDER_FAILED,
            "AndroidView.view: Native View failed", nodeId, AndroidViewInteropV1.PROP_VIEW, failure))
    }

    override fun dispose() {
        if (closed) return
        closed = true
        slots.toList().forEach(::release)
        bindings = emptyMap()
        AndroidViewClaimsV1.release(owner)
        revision++
    }

    private fun usable() { if (closed) fail(ComposeUiErrorCodes.SESSION_CLOSED) }
    private fun fail(code: String = ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId: Int? = null): Nothing =
        throw ComposeUiContractException(code, "AndroidView.view: Invalid binding or View parent", nodeId, AndroidViewInteropV1.PROP_VIEW)
}
