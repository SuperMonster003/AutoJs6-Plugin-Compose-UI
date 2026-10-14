package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.platform.LocalFocusManager
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
import kotlinx.coroutines.launch
import org.autojs.plugin.compose.api.ComposeUiComponents
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiEventFields
import org.autojs.plugin.compose.api.ComposeUiProps
import org.autojs.plugin.compose.api.model.ThemeSpec
import org.autojs.plugin.compose.api.model.UiCommand
import org.autojs.plugin.compose.api.model.UiValue

/**
 * Design-time renderer controllers: the same classes the in-host renderer uses, with errors collected for the
 * designer. Events go to [onEvent] with the callback id the renderer emitted (the node's id, or a modifier
 * callback's token in the sandbox's trees); only the interactive preview (P9.3) uses them, because in edit mode
 * the overlay takes every pointer.
 */
internal class CanvasControllers {
    val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    val commands = NodeCommandRegistry()
    val failures = ArrayList<Throwable>()
    var onEvent: (nodeId: Int, type: String, callbackId: Int, payload: Bundle) -> Unit = { _, _, _, _ -> }
    /** The text each field last reported, so a frame echoing it does not reset a field the user is typing in. */
    private val typed = HashMap<Int, String>()
    val emit: (Long, Int, String, Int, Bundle) -> Unit = { _, nodeId, type, callbackId, payload ->
        payload.getString(ComposeUiEventFields.TEXT)?.let { typed[nodeId] = it }
        onEvent(nodeId, type, callbackId, payload)
    }
    val fields = TextFieldController(emit, { failures += it }, { scope })
    val snackbar = SnackbarController({ scope }, emit)

    /**
     * Binds the fields and the snackbar host of [frame] before any of it is drawn, as the host renderer does before
     * it publishes a frame: the canvas recomposes changed nodes in the frame that takes the tree, before its effects
     * run, and a text field drawn without a binding fails with NODE_DETACHED.
     */
    fun bind(frame: PreviewFrame) {
        fields.acceptFrame(frame.tree, frame.generation)
        snackbar.acceptFrame(frame.tree, frame.generation)
    }

    /** Brings the bound fields to the text of [frame] once it is drawn. */
    fun accept(frame: PreviewFrame) {
        val live = frame.live
        frame.tree?.nodes?.forEach { node ->
            if (node.type != ComposeUiComponents.TEXT_FIELD && node.type != ComposeUiComponents.OUTLINED_TEXT_FIELD) return@forEach
            val declared = (node.props[ComposeUiProps.TEXT] as? UiValue.Str)?.value ?: return@forEach
            if (live != null) {
                // The sandbox's text goes with the edit sequence it last saw: like a script's edit in the host,
                // it is refused when the user has typed since, and the sandbox hears of that typing next.
                try {
                    fields.execute(UiCommand.Edit(node.nodeId, declared, null, live.fieldSequences[node.nodeId] ?: 0L))
                } catch (stale: ComposeUiContractException) { /* the field is ahead of the sandbox */ }
            } else if (typed[node.nodeId] != declared) {
                // A field's text is its initial text in the renderer; when the document or a preview value changes
                // it, set the field to it, unless the change is the field's own typing coming back.
                fields.replaceText(node.nodeId, declared)
            }
        }
    }

    /** Carries out the handle commands of a sandbox tree once it is drawn; one that no longer applies is dropped. */
    fun run(live: LiveHooks) {
        live.commands.forEach { command ->
            scope.launch {
                try { commands.execute(command) } catch (failure: ComposeUiContractException) { failures += failure }
            }
        }
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
    /**
     * Bounds by node id. A plain map, not a state map: a state map compares its whole content on every put,
     * which made the position callbacks of one layout (one per node) quadratic in the node count (P9.1).
     * Readers observe [version] instead, which changes whenever a bound does.
     */
    private val nodeRects = HashMap<Int, Rect>()
    private var version by mutableIntStateOf(0)
    private val rects: Map<Int, Rect> get() = nodeRects.also { observe() }
    /** Bounds of the drawn popup of each open window component (P9.1), by owner id, in root coordinates. */
    val popups = mutableStateMapOf<Int, Rect>()
    var origin by mutableStateOf(Offset.Zero)

    private fun observe(): Int = version

    fun record(id: Int, rect: Rect) {
        if (nodeRects.put(id, rect) != rect) version++
    }

    /** Drops the bounds of nodes the current frame no longer has, so they cannot win hit tests and drop positions. */
    fun retain(ids: Set<Int>) {
        if (nodeRects.keys.retainAll(ids)) version++
    }

    /**
     * The bounds of design node [id] in local canvas coordinates; a node without bounds of its own (an
     * expanded window component or an unrenderable one) takes the union of the placeholders standing in for it.
     */
    fun local(id: Int, placeholders: Map<Int, Int> = emptyMap()): Rect? {
        (rects[id] ?: popups[id])?.let { return it.translate(-origin) }
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
    fun hit(document: DesignDocument, point: Offset, placeholders: Map<Int, Int>, overlays: List<Overlay> = emptyList()): Int? {
        val root = point + origin
        // A popup is drawn over the page, so a tap on it stays inside its owner even where a node that comes later
        // in document order lies underneath; the topmost (last drawn) popup wins.
        overlays.asReversed().firstOrNull { popups[it.ownerId]?.contains(root) == true }?.let { overlay ->
            val owner = document.node(overlay.ownerId) ?: return overlay.ownerId
            var inside = overlay.ownerId
            owner.descendants().forEach { node -> rects[node.id]?.let { if (it.contains(root)) inside = node.id } }
            placeholders.forEach { (placeholderId, ownerId) ->
                if (rects[placeholderId]?.contains(root) == true && document.isDescendant(ownerId, overlay.ownerId) && !document.isDescendant(inside, ownerId)) inside = ownerId
            }
            return inside
        }
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
 * during a drag the container is highlighted and a line marks where the node would land (P9.1). Open window
 * components are drawn over the page in the canvas itself ([DesignOverlay]); no window ever opens.
 * In edit mode real controls never receive input, the overlay owns every pointer; in the interactive preview
 * (P9.3, [interactive]) the overlay steps aside and value changes of state-bound properties reach [onEvent].
 * The document is laid out on a
 * bounded logical screen the size of the canvas viewport (as `compose.mount` fills an Activity), so
 * scrollable components get finite constraints; zoom scales that screen and the viewport pans over it.
 */
@Composable
internal fun DesignCanvas(
    document: DesignDocument,
    /** Resolves literal Image sources (P9.1); null leaves every Image a placeholder. */
    images: ImageSource?,
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
    /** Preview values of top-level states (P9.3). */
    values: Map<String, JsLiteral> = emptyMap(),
    interactive: Boolean = false,
    /** The sandbox's tree, drawn instead of [document] while the script runs (P9.3 step 2). */
    live: LiveTree? = null,
    onEvent: (nodeId: Int, type: String, callbackId: Int, payload: Bundle) -> Unit = { _, _, _, _ -> },
) {
    val controllers = remember { CanvasControllers() }
    val focus = LocalFocusManager.current
    // Leaving the interactive preview drops the focus a component may hold, and with it the keyboard.
    LaunchedEffect(interactive) { if (!interactive) focus.clearFocus() }
    DisposableEffect(controllers) { onDispose { controllers.dispose() } }
    SideEffect { controllers.onEvent = onEvent }
    val generation = remember { LongArray(1) }
    val preview = remember(document, images, values, live) {
        if (live != null) PreviewFrames.build(live.document, ++generation[0], images = images, live = live.hooks)
        else PreviewFrames.build(document, ++generation[0], images = images, values = values)
    }
    // Written here, before the canvas content reads it, so the changed nodes recompose in this same frame; their
    // fields are bound first.
    val live = remember { LiveFrame() }
    val rendered = remember(preview) {
        controllers.bind(preview)
        preview.frame?.let(live::update)
    }
    LaunchedEffect(preview) {
        controllers.accept(preview)
        preview.live?.takeIf { it.commands.isNotEmpty() }?.let { hooks ->
            // After the frame that holds the nodes, so their focus requesters and list states are registered.
            withFrameNanos { }
            controllers.run(hooks)
        }
        onProblems(preview.problems)
        // Bounds of nodes the new frame no longer has would otherwise still win hit tests and drop positions.
        val live = preview.frame?.nodes?.keys ?: emptySet()
        bounds.retain(live)
        val open = preview.overlays.map { it.ownerId }.toSet()
        bounds.popups.keys.filter { it !in open }.forEach { bounds.popups.remove(it) }
    }
    val decorator = remember(bounds) {
        { id: Int, mapped: Modifier -> Modifier.onGloballyPositioned { bounds.record(id, it.boundsInRoot()) }.then(mapped) }
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
                            // In edit mode no component may take focus: on API 24 - 27 a window that gains focus outside
                            // touch mode hands it to the first focusable view, which put a canvas text field in focus
                            // and raised the keyboard over the panels. The interactive preview lets focus in.
                            Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.focusProperties { onEnter = { if (!interactive) cancelFocusChange() } }.focusGroup()) {
                                rendered?.let { frame ->
                                    val render: @Composable (Int) -> Unit = { id ->
                                        RenderNode(frame, id, controllers.commands, controllers.fields, controllers.snackbar, controllers.emit)
                                    }
                                    render(frame.rootId)
                                    // Open window components over the page, in tree order (P9.1).
                                    preview.overlays.forEach { overlay ->
                                        key(overlay.ownerId) {
                                            DesignOverlay(overlay, bounds.local(overlay.ownerId), { rect -> bounds.popups[overlay.ownerId] = rect }, render)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // The interactive preview leaves the pointers to the components and hides the selection.
        if (!interactive) Box(
            Modifier
                .matchParentSize()
                .testTag("designer-canvas-overlay")
                .pointerInput(document, preview) {
                    detectTapGestures { offset -> onSelect(bounds.hit(document, offset, preview.placeholders, preview.overlays)) }
                }
                .pointerInput(document, preview) {
                    var dragging: Int? = null
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            dragging = bounds.hit(document, offset, preview.placeholders, preview.overlays)?.takeIf { it != document.root?.id }
                            dragging?.let { onDragStart(it) }
                        },
                        onDrag = { change, _ ->
                            if (dragging != null) {
                                change.consume()
                                val point = change.position
                                onDragOver(bounds.hit(document, point, preview.placeholders, preview.overlays), point) { id -> bounds.local(id, preview.placeholders) }
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
