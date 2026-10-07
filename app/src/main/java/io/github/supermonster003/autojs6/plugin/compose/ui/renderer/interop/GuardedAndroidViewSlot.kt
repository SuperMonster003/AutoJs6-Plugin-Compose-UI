package io.github.supermonster003.autojs6.plugin.compose.ui.renderer.interop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.interop.AndroidViewBindingV1
import org.autojs.plugin.compose.api.interop.AndroidViewInteropV1

/** Owns only a small native container. The child and its listeners/resources stay caller-owned. */
@SuppressLint("ViewConstructor")
internal class GuardedAndroidViewSlot(
    context: Context,
    val nodeId: Int,
    val owner: AndroidViewContent,
    private val report: (Int, Throwable) -> Unit,
) : FrameLayout(context) {
    var binding: AndroidViewBindingV1? = null
        private set
    private var failed = false
    private var released = false
    private var previousWidth = 0
    private var previousHeight = 0

    fun replace(next: AndroidViewBindingV1?) {
        usable()
        val before = binding
        if (before?.view === next?.view && before?.leaseId == next?.leaseId) return
        if (next?.view?.parent != null && next.view.parent !== this) invalid()
        try {
            before?.view?.takeIf { it.parent === this }?.let(::removeView)
            usable()
            next?.view?.let { addView(it) }
            // A native onAttached/onDetached callback may close the session synchronously.
            usable()
            binding = next
            recover()
            usable()
        } catch (failure: RuntimeException) {
            restore(before, next, failure)
            throw ComposeUiContractException(if (released) ComposeUiErrorCodes.SESSION_CLOSED else ComposeUiErrorCodes.RENDER_FAILED,
                "AndroidView.view: Native attachment failed", nodeId, AndroidViewInteropV1.PROP_VIEW, failure)
        } catch (failure: LinkageError) {
            restore(before, next, failure)
            throw ComposeUiContractException(if (released) ComposeUiErrorCodes.SESSION_CLOSED else ComposeUiErrorCodes.RENDER_FAILED,
                "AndroidView.view: Native attachment failed", nodeId, AndroidViewInteropV1.PROP_VIEW, failure)
        }
    }

    private fun restore(before: AndroidViewBindingV1?, next: AndroidViewBindingV1?, failure: Throwable) {
        if (released) { cleanReleased(failure); return }
        try { next?.view?.takeIf { it.parent === this }?.let(::removeView) } catch (rollback: RuntimeException) { failure.addSuppressed(rollback) }
        catch (rollback: LinkageError) { failure.addSuppressed(rollback) }
        if (released) { cleanReleased(failure); return }
        try { before?.view?.takeIf { it.parent == null }?.let(::addView) } catch (rollback: RuntimeException) { failure.addSuppressed(rollback) }
        catch (rollback: LinkageError) { failure.addSuppressed(rollback) }
        if (released) cleanReleased(failure) else binding = before
    }

    fun recover() {
        usable()
        try {
            failed = false
            binding?.view?.forceLayout()
            usable()
            forceLayout(); requestLayout(); invalidate()
            usable()
        } catch (failure: ComposeUiContractException) { throw failure }
        catch (failure: RuntimeException) { throw recoveryFailure(failure) }
        catch (failure: LinkageError) { throw recoveryFailure(failure) }
    }

    /** A released Compose slot is never reused; borrowed Views may be used by a later fresh slot. */
    fun release() {
        released = true
        try { removeAllViews() } finally { binding = null }
    }

    private fun cleanReleased(failure: Throwable) {
        try { release() } catch (cleanup: RuntimeException) { failure.addSuppressed(cleanup) }
        catch (cleanup: LinkageError) { failure.addSuppressed(cleanup) }
    }

    private fun usable() {
        if (released) throw ComposeUiContractException(ComposeUiErrorCodes.SESSION_CLOSED,
            "AndroidView.view: Slot has been released", nodeId, AndroidViewInteropV1.PROP_VIEW)
    }

    private fun recoveryFailure(failure: Throwable) = ComposeUiContractException(
        if (released) ComposeUiErrorCodes.SESSION_CLOSED else ComposeUiErrorCodes.RENDER_FAILED,
        "AndroidView.view: Native layout request failed", nodeId, AndroidViewInteropV1.PROP_VIEW, failure)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!failed) try {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            previousWidth = measuredWidth; previousHeight = measuredHeight
            return
        } catch (failure: RuntimeException) { fail(failure) } catch (failure: LinkageError) { fail(failure) }
        setMeasuredDimension(resolveSize(maxOf(previousWidth, suggestedMinimumWidth), widthMeasureSpec),
            resolveSize(maxOf(previousHeight, suggestedMinimumHeight), heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (failed) return
        try { super.onLayout(changed, left, top, right, bottom) }
        catch (failure: RuntimeException) { fail(failure) } catch (failure: LinkageError) { fail(failure) }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (failed) return
        val checkpoint = canvas.save()
        try { super.dispatchDraw(canvas) }
        catch (failure: RuntimeException) { fail(failure) } catch (failure: LinkageError) { fail(failure) }
        finally {
            try { canvas.restoreToCount(checkpoint) }
            catch (failure: RuntimeException) { fail(failure) } catch (failure: LinkageError) { fail(failure) }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean = try { !failed && super.dispatchTouchEvent(event) }
    catch (failure: RuntimeException) { fail(failure); false } catch (failure: LinkageError) { fail(failure); false }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = try { !failed && super.dispatchKeyEvent(event) }
    catch (failure: RuntimeException) { fail(failure); false } catch (failure: LinkageError) { fail(failure); false }

    private fun fail(failure: Throwable) {
        if (failed) return
        failed = true
        invalidate()
        try { report(nodeId, failure) } catch (_: RuntimeException) { } catch (_: LinkageError) { }
    }
    private fun invalid(): Nothing = throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT,
        "AndroidView.view: View already has a parent", nodeId, AndroidViewInteropV1.PROP_VIEW)
}
