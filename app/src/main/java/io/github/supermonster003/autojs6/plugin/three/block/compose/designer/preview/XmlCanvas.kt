package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.UiDesignerSession

/**
 * The canvas of a `"ui"` mode project (roadmap P10.3 / D46 / D48): the host's real inflater draws the layout,
 * embedded through a `SurfaceView` on API 30+ or as a snapshot bitmap below, on a bounded logical screen the size
 * of the viewport, zoomed by scaling that screen. In edit mode the surface sits below the window (P10.0: pointer
 * input then reaches this window, not the embedded tree), so the overlay here owns every pointer: a tap selects
 * the deepest element under it by the host's bounds, a long press drags it to a container with the insertion line
 * of [DropPlacements], and the selection, the drop target and the element of a failed frame are drawn over the
 * surface. Until the host has drawn a frame the element tree is shown as text, with the preview's status.
 *
 * The interactive preview (roadmap P10.6, [interactive]) puts the surface on top of the window, the only way
 * pointer input reaches the embedded tree (P10.0), so nothing of this window shows over the canvas then: the
 * overlay and its gestures are gone and the status goes to the editor's row above the canvas. Changing the Z
 * order needs a new surface, so the `SurfaceView` is recreated and the session re-attached.
 */
@Composable
internal fun XmlCanvas(
    session: UiDesignerSession,
    request: XmlPreviewRequest?,
    client: UiPreviewClient,
    zoom: Float,
    interactive: Boolean,
    accent: Color,
    danger: Color,
    text: Color,
    muted: Color,
    surface: Color,
    /** The preview's status for the user, or null when a frame is shown without remarks. */
    statusText: String?,
    retryLabel: String,
    onRetry: (() -> Unit)?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = Color.White)
    val bounds = client.bounds
    // A running script draws its own content: the surface shows it whether or not bounds have arrived.
    val hasFrame = interactive || if (client.embedded) bounds.isNotEmpty() else client.snapshot != null
    DisposableEffect(client) { onDispose { client.detachSurface() } }

    fun rectOf(bound: UiPreviewClient.Bound): Rect = Rect(bound.left * zoom, bound.top * zoom, bound.right * zoom, bound.bottom * zoom)

    /** The canvas bounds of an element: its first instance's view, or null when the host drew none. */
    fun boundsOf(elementId: Int): Rect? {
        val index = request?.indexOf(elementId) ?: return null
        if (index < 0) return null
        return bounds.filter { it.index == index }.minByOrNull { it.instance }?.let(::rectOf)
    }

    /** The element under [point] (canvas coordinates): the smallest visible view containing it, the latest when tied. */
    fun hit(point: Offset): Int? {
        val x = point.x / zoom
        val y = point.y / zoom
        val best = bounds
            .filter { it.visibility == 0 && it.contains(x, y) }
            .sortedWith(compareBy({ it.area }, { -it.index }))
            .firstOrNull() ?: return null
        return request?.elementIdAt(best.index)
    }

    BoxWithConstraints(modifier = modifier.testTag("designer-xml-canvas")) {
        val logicalWidth = if (constraints.hasBoundedWidth) maxWidth else 360.dp
        val logicalHeight = if (constraints.hasBoundedHeight) maxHeight else 640.dp
        val widthPx = with(density) { logicalWidth.roundToPx() }
        val heightPx = with(density) { logicalHeight.roundToPx() }
        if (!client.embedded) LaunchedEffect(widthPx, heightPx) { client.resize(widthPx, heightPx) }
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
            Box(Modifier.requiredSize(logicalWidth * zoom, logicalHeight * zoom).testTag("designer-xml-canvas-content")) {
                if (client.embedded) key(interactive) {
                    AndroidView(
                        factory = { context ->
                            SurfaceView(context).also { view ->
                                // Edit mode: below the window, so pointer input stays with this window's overlay, which draws over
                                // the surface. Interactive preview: on top, so the pointer reaches the embedded tree (P10.0).
                                view.setZOrderOnTop(interactive)
                                view.holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) { /* the size arrives with surfaceChanged */ }
                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { client.attachSurface(view, width, height); client.resize(width, height) }
                                    override fun surfaceDestroyed(holder: SurfaceHolder) { client.detachSurface() }
                                })
                            }
                        },
                        modifier = Modifier.requiredSize(logicalWidth, logicalHeight).graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            transformOrigin = TransformOrigin(0f, 0f)
                        }.testTag(if (interactive) "designer-xml-surface-live" else "designer-xml-surface"),
                    )
                } else {
                    client.snapshot?.let { bitmap ->
                        Image(bitmap, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize().testTag("designer-xml-snapshot"))
                    }
                }
                if (!hasFrame) {
                    val root = session.root
                    Column(Modifier.fillMaxSize().background(surface).padding(12.dp).testTag("designer-xml-placeholder")) {
                        if (root != null) androidx.compose.material3.Text(
                            outlineText(root),
                            color = muted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                        )
                    }
                }
                // The interactive preview has no overlay: the embedded tree owns the pointer and the surface covers the window.
                if (!interactive) Box(
                    Modifier
                        .fillMaxSize()
                        .testTag("designer-xml-overlay")
                        .pointerInput(request, bounds, zoom) {
                            detectTapGestures { offset -> onSelect(hit(offset)) }
                        }
                        .pointerInput(request, bounds, zoom) {
                            var dragging: Int? = null
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    dragging = hit(offset)?.takeIf { session.document.parentOf(it) != null }
                                    dragging?.let { session.draggingId = it }
                                },
                                onDrag = { change, _ ->
                                    val dragged = dragging ?: return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    session.placeDrop(dragged, hit(change.position), change.position) { id -> boundsOf(id) }
                                },
                                onDragEnd = { if (dragging != null) { dragging = null; session.endDrag() } },
                                onDragCancel = {
                                    if (dragging != null) {
                                        dragging = null
                                        session.dropTargetId = null
                                        session.dropIndex = null
                                        session.dropLine = null
                                        session.endDrag()
                                    }
                                },
                            )
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 2.dp.toPx())
                        session.dropTargetId?.let { id -> boundsOf(id)?.let { drawRect(accent.copy(alpha = 0.18f), it.topLeft, it.size); drawRect(accent, it.topLeft, it.size, style = stroke) } }
                        session.dropLine?.let { (start, end) -> drawLine(accent, start, end, strokeWidth = 3.dp.toPx()) }
                        session.problemElementId?.takeIf { it != session.selectedId }?.let { id -> boundsOf(id)?.let { drawRect(danger, it.topLeft, it.size, style = stroke) } }
                        session.selectedId?.let { id ->
                            val rect = boundsOf(id) ?: return@let
                            val color = if (session.problemElementId == id) danger else accent
                            val inflated = Rect(rect.topLeft - Offset(2f, 2f), Size(rect.width + 4f, rect.height + 4f))
                            drawRect(color, inflated.topLeft, inflated.size, style = stroke)
                            val element = session.document.element(id) ?: return@let
                            val label = measurer.measure(elementLabel(element), labelStyle)
                            val padding = 3.dp.toPx()
                            val labelSize = Size(label.size.width + padding * 2, label.size.height + padding)
                            val top = if (inflated.top >= labelSize.height) inflated.top - labelSize.height else inflated.top
                            val left = inflated.left.coerceAtLeast(0f)
                            drawRect(color, Offset(left, top), labelSize)
                            drawText(label, topLeft = Offset(left + padding, top + padding / 2))
                        }
                    }
                }
            }
        }
        if (statusText != null && !interactive) {
            Column(
                Modifier.align(Alignment.BottomStart).padding(12.dp).background(surface).padding(8.dp).testTag("designer-xml-status"),
            ) {
                androidx.compose.material3.Text(statusText, color = if (client.failure != null || client.status is UiPreviewClient.Status.Lost) danger else text, fontSize = 12.sp)
                if (onRetry != null) androidx.compose.material3.Text(
                    retryLabel,
                    color = accent,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp).clickable { onRetry() }.testTag("designer-xml-retry"),
                )
            }
        }
    }
}

/** `tag #id` for the selection label and the outline. */
internal fun elementLabel(element: XmlNode.Element): String {
    val id = element.attr("id")?.value?.takeIf { it.isNotEmpty() }
    return if (id == null) element.tag else element.tag + " #" + id
}

private fun outlineText(root: XmlNode.Element): String = buildString {
    fun visit(element: XmlNode.Element, depth: Int) {
        if (depth > 0) append('\n')
        repeat(depth) { append("  ") }
        append('<').append(element.tag)
        element.attr("id")?.value?.takeIf { it.isNotEmpty() }?.let { append(" #").append(it) }
        append('>')
        element.elementChildren.forEach { visit(it, depth + 1) }
    }
    visit(root, 0)
}
