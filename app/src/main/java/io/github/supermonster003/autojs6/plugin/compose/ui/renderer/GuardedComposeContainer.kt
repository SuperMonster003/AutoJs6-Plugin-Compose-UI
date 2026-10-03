package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout

/**
 * Contains failures raised by Compose during Android view traversal, outside any recomposer job.
 * A failed tree stays blank until the renderer publishes a replacement and calls [recover].
 * This view does not own a lifecycle or saved-state owner: its child inherits the host's owners.
 */
@SuppressLint("ViewConstructor") // Programmatic renderer container with a required error sink, never inflated from XML.
internal class GuardedComposeContainer(
    context: Context,
    private val onFailure: (Throwable) -> Unit,
) : FrameLayout(context) {
    private var traversalFailed = false
    private var previousWidth = 0
    private var previousHeight = 0

    fun mount(view: View) {
        require(view.parent == null || view.parent === this) { "Renderer view already has a parent" }
        if (childCount != 1 || getChildAt(0) !== view) {
            removeAllViews()
            addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        recover()
    }

    /** Retry explicitly; a traversal error never schedules an unbounded automatic retry loop. */
    fun recover() {
        traversalFailed = false
        // Android may otherwise reuse a failed child's previous measure result for identical specs.
        for (index in 0 until childCount) getChildAt(index).forceLayout()
        forceLayout()
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!traversalFailed) {
            try {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                previousWidth = measuredWidth
                previousHeight = measuredHeight
                return
            } catch (error: RuntimeException) {
                failed(error)
            } catch (error: LinkageError) {
                failed(error)
            }
        }
        // Even on the first failed measure, View.measure must observe a legal measured dimension.
        setMeasuredDimension(
            resolveSize(maxOf(previousWidth, suggestedMinimumWidth), widthMeasureSpec),
            resolveSize(maxOf(previousHeight, suggestedMinimumHeight), heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (traversalFailed) return
        try {
            super.onLayout(changed, left, top, right, bottom)
        } catch (error: RuntimeException) {
            failed(error)
        } catch (error: LinkageError) {
            failed(error)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (traversalFailed) return
        var checkpoint: Int? = null
        try {
            checkpoint = canvas.save()
            super.dispatchDraw(canvas)
        } catch (error: RuntimeException) {
            failed(error)
        } catch (error: LinkageError) {
            failed(error)
        } finally {
            checkpoint?.let { count ->
                // A child throwing between save and restore must not corrupt the host's canvas.
                try {
                    canvas.restoreToCount(count)
                } catch (error: RuntimeException) {
                    failed(error)
                } catch (error: LinkageError) {
                    failed(error)
                }
            }
        }
    }

    private fun failed(error: Throwable) {
        if (traversalFailed) return
        traversalFailed = true
        // Drop any partially drawn display list on the next frame, without retrying the child.
        invalidate()
        try {
            onFailure(error)
        } catch (_: RuntimeException) {
            // Error delivery is also outside the host's traversal contract.
        } catch (_: LinkageError) {
            // Fatal VM errors deliberately propagate instead of pretending that recovery is safe.
        }
    }
}
