package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Diagnostic
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditHistory
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.DropPlacements
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LayoutAxis

/**
 * The editing state of one open `"ui"` mode project (roadmap P10.3 / P10.4): the undo history over the
 * [UiDocument], the selection, the fragment shown on the canvas, the canvas drag state, the validator's
 * diagnostics and the preview's problems. Views observe its Compose state; all mutations go through commands.
 */
internal class UiDesignerSession(initial: UiDocument, val catalog: UiCatalog = UiCatalog.load()) {
    private val history = UiEditHistory(initial)
    private val validator = UiValidator(catalog)

    var document: UiDocument by mutableStateOf(initial)
        private set
    /** The fragment the canvas shows; the `ui.layout` root unless the user switches. */
    var fragment: Int by mutableStateOf(initial.rootFragment.coerceAtLeast(0))
    var selectedId: Int? by mutableStateOf(initial.root?.id)
    var dirty: Boolean by mutableStateOf(false)
    var revision: Long by mutableStateOf(0L)
        private set
    var diagnostics: List<Diagnostic> by mutableStateOf(validator.validate(initial))
        private set
    var draggingId: Int? by mutableStateOf(null)
    var dropTargetId: Int? by mutableStateOf(null)
    /** Position among the drop target's element children (without the dragged element); null appends. */
    var dropIndex: Int? by mutableStateOf(null)
    /** The insertion line drawn on the canvas, in canvas coordinates. */
    var dropLine: Pair<Offset, Offset>? by mutableStateOf(null)
    /** What the host could not preview, as text for the issues list. */
    var previewProblems: List<String> by mutableStateOf(emptyList())
    /** The element the last failed frame points at, highlighted on the canvas and in the outline. */
    var problemElementId: Int? by mutableStateOf(null)
    /** Values of `{{ }}` names that override the script's declarations on the canvas (JSON text), never in the document. */
    var bindingOverrides: Map<String, String> by mutableStateOf(emptyMap())
        private set
    /** Counts the frames the canvas received from the host; device tests wait on it. */
    var previewFrames: Long by mutableStateOf(0L)

    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo
    val selected: XmlNode.Element? get() = selectedId?.let { document.element(it) }
    val root: XmlNode.Element? get() = document.fragments.getOrNull(fragment)?.root

    fun execute(command: UiEditCommand, merge: Boolean = true): Boolean {
        val changed = history.execute(command, merge)
        if (changed) publish()
        return changed
    }

    fun seal() = history.seal()

    fun undo(): Boolean = history.undo().also { if (it) publish() }

    fun redo(): Boolean = history.redo().also { if (it) publish() }

    fun markSaved() { dirty = false }

    fun script(): String = UiScriptWriter.write(document)

    fun replace(document: UiDocument) {
        history.reset(document)
        fragment = document.rootFragment.coerceAtLeast(0)
        selectedId = document.root?.id
        dirty = false
        publishQuiet()
    }

    fun setBindingOverride(name: String, json: String?) {
        bindingOverrides = if (json == null) bindingOverrides - name else bindingOverrides + (name to json)
    }

    private fun publish() {
        dirty = true
        publishQuiet()
    }

    private fun publishQuiet() {
        document = history.document
        revision++
        if (fragment !in document.fragments.indices) fragment = document.rootFragment.coerceAtLeast(0)
        if (selectedId != null && document.element(selectedId!!) == null) selectedId = root?.id
        diagnostics = validator.validate(document)
    }

    /** Whether [tag] takes element children, by the catalog; registered widgets and unknown tags take none. */
    fun isContainer(element: XmlNode.Element): Boolean {
        val tag = catalog.tag(element.tag) ?: return false
        if (tag.tag == "list" || tag.tag == "grid") return element.elementChildren.isEmpty()
        return tag.container
    }

    /** The axis a container lays its children out along, for drop positions. */
    fun axisOf(element: XmlNode.Element): LayoutAxis {
        val tag = catalog.tag(element.tag)?.tag
        val orientation = element.attr("orientation")?.value
        val verticalBare = element.attr("vertical")?.isBare == true
        return when {
            tag == "linear" && (orientation == "vertical" || verticalBare) -> LayoutAxis.VERTICAL
            tag == "linear" -> LayoutAxis.HORIZONTAL
            orientation == "horizontal" && catalog.tag(element.tag)?.axis == "vertical" -> LayoutAxis.HORIZONTAL
            else -> when (catalog.tag(element.tag)?.axis) {
                "vertical" -> LayoutAxis.VERTICAL
                "horizontal" -> LayoutAxis.HORIZONTAL
                else -> LayoutAxis.STACK
            }
        }
    }

    fun deleteSelected(): Boolean {
        val element = selected ?: return false
        if (document.parentOf(element.id) == null) return false
        val placement = document.parentOf(element.id)
        val changed = execute(UiEditCommand.RemoveElement(element.id), merge = false)
        if (changed) selectedId = placement?.parentId ?: root?.id
        return changed
    }

    /** Moves the selected element one position up or down among its element siblings. */
    fun moveSelected(delta: Int): Boolean {
        val element = selected ?: return false
        val placement = document.parentOf(element.id) ?: return false
        val parent = document.element(placement.parentId) ?: return false
        val siblings = parent.elementChildren
        val index = siblings.indexOfFirst { it.id == element.id }
        val target = index + delta
        if (target !in siblings.indices) return false
        return execute(UiEditCommand.MoveElement(element.id, parent.id, target), merge = false)
    }

    fun canDropInto(id: Int, containerId: Int): Boolean {
        if (id == containerId) return false
        val container = document.element(containerId) ?: return false
        val element = document.element(id) ?: return false
        if (element.descendants().any { it.id == containerId }) return false
        if (document.parentOf(id) == null) return false
        val placement = document.parentOf(id)
        if (placement?.parentId == containerId) return true
        return isContainer(container)
    }

    /** The nearest ancestor (or the element itself) of [id] that accepts a dropped element. */
    fun dropContainerFor(dragged: Int, id: Int?): Int? {
        var current = id
        while (current != null) {
            if (canDropInto(dragged, current)) return current
            current = document.parentOf(current)?.parentId
        }
        return null
    }

    /** Chooses where a canvas drag would land (P9.1 semantics on XML elements); [boundsOf] gives canvas bounds by element id. */
    fun placeDrop(dragged: Int, hit: Int?, pointer: Offset, boundsOf: (Int) -> Rect?) {
        val container = dropContainerFor(dragged, hit)?.let { document.element(it) }
        dropTargetId = container?.id
        if (container == null) {
            dropIndex = null
            dropLine = null
            return
        }
        val children = container.elementChildren.filter { it.id != dragged }.map { boundsOf(it.id) }
        val placement = DropPlacements.place(axisOf(container), pointer, boundsOf(container.id), children)
        dropIndex = placement.index
        dropLine = placement.line
    }

    fun endDrag() {
        val dragged = draggingId
        val target = dropTargetId
        val index = dropIndex
        draggingId = null
        dropTargetId = null
        dropIndex = null
        dropLine = null
        if (dragged != null && target != null && canDropInto(dragged, target)) {
            val container = document.element(target) ?: return
            val count = container.elementChildren.count { it.id != dragged }
            val moved = execute(UiEditCommand.MoveElement(dragged, target, (index ?: count).coerceIn(0, count)), merge = false)
            if (moved) selectedId = dragged
        }
    }
}
