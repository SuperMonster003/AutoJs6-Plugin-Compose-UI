package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import org.autojs.plugin.compose.api.model.UiValue
import kotlin.math.roundToInt

/**
 * Borrows pixels for a single draw only. Neither Bitmap nor ImageBitmap is retained between calls.
 * Image's outer contentScale/alignment still use the current bitmap's intrinsic pixel dimensions.
 * An expired or recycled contract handle has no intrinsic size and draws transparent content.
 */
internal class BorrowedBitmapPainter(
    private val reference: UiValue.BitmapRef,
    private val onFailure: (Throwable) -> Unit,
) : Painter() {
    private var imageAlpha = 1f
    private var imageColorFilter: ColorFilter? = null
    private var failureReported = false

    override val intrinsicSize: Size
        get() = try {
            val bitmap = reference.get()
            if (bitmap == null || bitmap.isRecycled) Size.Unspecified
            else Size(bitmap.width.toFloat(), bitmap.height.toFloat())
        } catch (error: RuntimeException) {
            failed(error)
            Size.Unspecified
        } catch (error: LinkageError) {
            failed(error)
            Size.Unspecified
        }

    override fun DrawScope.onDraw() {
        try {
            val bitmap = reference.get() ?: return
            if (bitmap.isRecycled || size.width <= 0 || size.height <= 0) return
            val sourceSize = IntSize(bitmap.width, bitmap.height)
            val destinationSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
            if (sourceSize.width <= 0 || sourceSize.height <= 0 || destinationSize.width <= 0 || destinationSize.height <= 0) return
            // The caller can recycle between isRecycled and drawImage. Keep the whole borrow in
            // this guarded scope and never replace it with remember(bitmap.asImageBitmap()).
            drawImage(
                image = bitmap.asImageBitmap(),
                srcSize = sourceSize,
                dstSize = destinationSize,
                alpha = imageAlpha,
                colorFilter = imageColorFilter,
                filterQuality = FilterQuality.Low,
            )
        } catch (error: RuntimeException) {
            failed(error)
        } catch (error: LinkageError) {
            failed(error)
        }
    }

    override fun applyAlpha(alpha: Float): Boolean {
        imageAlpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        imageColorFilter = colorFilter
        return true
    }

    private fun failed(error: Throwable) {
        if (failureReported) return
        failureReported = true
        try {
            onFailure(error)
        } catch (_: RuntimeException) {
            // A failing error sink must not turn a recycled bitmap into a host draw failure.
        } catch (_: LinkageError) {
            // Do not intercept fatal VM errors.
        }
    }
}
