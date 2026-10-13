package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.LocalNodeDecorator
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.NodeCommandRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.RenderNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.RendererWindowContext
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.SnackbarController
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.TextFieldController
import io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.ThemeMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.autojs.plugin.compose.api.model.ThemeSpec

/**
 * Design-time renderer controllers: the same classes the in-host renderer uses, with events swallowed
 * (the canvas selects nodes through its own overlay) and errors collected for the designer.
 */
internal class CanvasControllers {
    val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    val commands = NodeCommandRegistry()
    val failures = ArrayList<Throwable>()
    private val emit: (Long, Int, String, Int, Bundle) -> Unit = { _, _, _, _, _ -> }
    val fields = TextFieldController(emit, { failures += it }, { scope })
    val snackbar = SnackbarController({ scope }, emit)

    fun accept(frame: PreviewFrame) {
        fields.acceptFrame(frame.tree, frame.generation)
        snackbar.acceptFrame(frame.tree, frame.generation)
    }

    fun dispose() {
        fields.dispose()
        snackbar.close()
        commands.clear()
        scope.cancel()
    }
}

/** Tracks the rendered bounds of every design node, keyed by node id, in root coordinates of the canvas composition. */
internal class CanvasBounds {
    val rects = mutableStateMapOf<Int, Rect>()
    var origin by mutableStateOf(Offset.Zero)

    /**
     * The bounds of design node [id] in local canvas coordinates; a node without bounds of its own (an
     * expanded window component or an unrenderable one) takes the union of the placeholders standing in for it.
     */
    fun local(id: Int, placeholders: Map<Int, Int> = emptyMap()): Rect? {
        rects[id]?.let { return it.translate(-origin) }
        var union: Rect? = null
        placeholders.forEach { (placeholderId, ownerId) ->
            if (ownerId == id) rects[placeholderId]?.let { rect -> union = union?.let { Rect(minOf(it.left, rect.left), minOf(it.top, rect.top), maxOf(it.right, rect.right), maxOf(it.bottom, rect.bottom)) } ?: rect }
        }
        return union?.translate(-origin)
    }

    /**
     * The deepest node whose bounds contain [point] (local canvas coordinates); preorder makes descendants
     * win. A placeholder selects its owner unless a descendant of the owner was already hit.
     */
    fun hit(document: DesignDocument, point: Offset, placeholders: Map<Int, Int>): Int? {
        val root = point + origin
        var found: Int? = null
        document.nodes().forEach { node -> rects[node.id]?.let { if (it.contains(root)) found = node.id } }
        placeholders.forEach { (placeholderId, ownerId) ->
            rects[placeholderId]?.let { rect ->
                if (rect.contains(root) && !document.isDescendant(found, ownerId)) found = ownerId
            }
        }
        return found
    }

    private fun DesignDocument.isDescendant(id: Int?, ancestorId: Int): Boolean {
        var current = id ?: return false
        while (true) {
            if (current == ancestorId) return true
            current = parentOf(current)?.parentId ?: return false
        }
    }
}

/**
 * The preview canvas (roadmap P8.3): the document rendered by the real renderer under the host-like
 * theme, with an overlay for tap selection, long-press drag onto a container and highlight rectangles;
 * during a drag the container is highlighted and a line marks where the node would land (P9.1).
 * Real controls never receive input; the overlay owns every pointer. The document is laid out on a
 * bounded logical screen the size of the canvas viewport (as `compose.mount` fills an Activity), so
 * scrollable components get finite constraints; zoom scales that screen and the viewport pans over it.
 */
@Composable
internal fun DesignCanvas(
    document: DesignDocument,
    theme: ThemeSpec,
    zoom: Float,
    selectedId: Int?,
    dropTargetId: Int?,
    dropLine: Pair<Offset, Offset>?,
    bounds: CanvasBounds,
    accent: Color,
    onSelect: (Int?) -> Unit,
    onDragStart: (Int) -> Unit,
    /** The node under the pointer, the pointer, and a lookup of node bounds in the same coordinates. */
    onDragOver: (Int?, Offset, (Int) -> Rect?) -> Unit,
    onDrop: () -> Unit,
    onProblems: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controllers = remember { CanvasControllers() }
    DisposableEffect(controllers) { onDispose { controllers.dispose() } }
    val generation = remember { LongArray(1) }
    val preview = remember(document) { PreviewFrames.build(document, ++generation[0]) }
    LaunchedEffect(preview) {
        controllers.accept(preview)
        onProblems(preview.problems)
        // Bounds of nodes the new frame no longer has would otherwise still win hit tests and drop positions.
        val live = preview.frame?.nodes?.keys ?: emptySet()
        bounds.rects.keys.filter { it !in live }.forEach { bounds.rects.remove(it) }
    }
    val decorator = remember(bounds) {
        { id: Int, mapped: Modifier -> Modifier.onGloballyPositioned { bounds.rects[id] = it.boundsInRoot() }.then(mapped) }
    }
    // Zoom scales the layout density instead of applying a graphics-layer transform, so node bounds and
    // pointer positions stay in one coordinate space.
    val baseDensity = LocalDensity.current
    val zoomed = remember(baseDensity, zoom) { Density(baseDensity.density * zoom, baseDensity.fontScale) }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = Color.White)
    BoxWithConstraints(modifier = modifier) {
        val logicalWidth = if (constraints.hasBoundedWidth) maxWidth else 360.dp
        val logicalHeight = if (constraints.hasBoundedHeight) maxHeight else 640.dp
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
        Box(
            Modifier
                .requiredSize(logicalWidth * zoom, logicalHeight * zoom)
                .onGloballyPositioned { bounds.origin = it.positionInRoot() }
                .testTag("designer-canvas-content"),
        ) {
            CompositionLocalProvider(LocalDensity provides zoomed) {
                ThemeMapper.Content(theme) {
                    CompositionLocalProvider(LocalNodeDecorator provides decorator) {
                        RendererWindowContext {
                            Box(Modifier.semantics { testTagsAsResourceId = true }) {
                                preview.frame?.let { frame ->
                                    RenderNode(frame, frame.rootId, controllers.commands, controllers.fields, controllers.snackbar, { _, _, _, _, _ -> })
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .testTag("designer-canvas-overlay")
                .pointerInput(document, preview) {
                    detectTapGestures { offset -> onSelect(bounds.hit(document, offset, preview.placeholders)) }
                }
                .pointerInput(document, preview) {
                    var dragging: Int? = null
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            dragging = bounds.hit(document, offset, preview.placeholders)?.takeIf { it != document.root?.id }
                            dragging?.let { onDragStart(it) }
                        },
                        onDrag = { change, _ ->
                            if (dragging != null) {
                                change.consume()
                                val point = change.position
                                onDragOver(bounds.hit(document, point, preview.placeholders), point) { id -> bounds.local(id, preview.placeholders) }
                            }
                        },
                        onDragEnd = { if (dragging != null) { dragging = null; onDrop() } },
                        onDragCancel = { if (dragging != null) { dragging = null; onDragOver(null, Offset.Zero) { null }; onDrop() } },
                    )
                },
        ) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = Stroke(width = 2.dp.toPx())
                dropTargetId?.let { id -> bounds.local(id, preview.placeholders)?.let { drawRect(accent.copy(alpha = 0.18f), it.topLeft, it.size); drawRect(accent, it.topLeft, it.size, style = stroke) } }
                dropLine?.let { (start, end) -> drawLine(accent, start, end, strokeWidth = 3.dp.toPx()) }
                selectedId?.let { id -> bounds.local(id, preview.placeholders)?.let { rect ->
                    val inflated = Rect(rect.topLeft - Offset(2f, 2f), Size(rect.width + 4f, rect.height + 4f))
                    drawRect(accent, inflated.topLeft, inflated.size, style = stroke)
                    // Name label above the selection, or inside its top edge when there is no room above.
                    val node = document.nodeOrNull(id) ?: return@let
                    val keyText = ((node.key as? PropValue.Literal)?.value as? JsLiteral.Str)?.value
                    val label = measurer.measure(if (keyText == null) node.type else node.type + " #" + keyText, labelStyle)
                    val padding = 3.dp.toPx()
                    val labelSize = Size(label.size.width + padding * 2, label.size.height + padding)
                    val top = if (inflated.top >= labelSize.height) inflated.top - labelSize.height else inflated.top
                    val left = inflated.left.coerceAtLeast(0f)
                    drawRect(accent, Offset(left, top), labelSize)
                    drawText(label, topLeft = Offset(left + padding, top + padding / 2))
                } }
            }
        }
        }
    }
}

/** Selection helper used by the activity: the node a tap selects, or null to clear. */
internal fun DesignDocument.nodeOrNull(id: Int?): DesignNode? = id?.let { node(it) }
