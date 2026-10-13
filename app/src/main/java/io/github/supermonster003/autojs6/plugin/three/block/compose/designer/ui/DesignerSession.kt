package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.ScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Diagnostic
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditHistory
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.DropPlacements
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LayoutAxes
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.PreviewControls
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.v2.ComposeUiV2

/**
 * The editing state of one open project (roadmap P8.3): the undo history, the selection, the canvas drag
 * state and the current diagnostics. Views observe its Compose state; all mutations go through commands.
 */
internal class DesignerSession(initial: DesignDocument, val catalog: ComponentCatalog = ComposeUiV2.interopCatalog) {
    private val history = EditHistory(initial)
    private val validator = DesignValidator(catalog)

    var document: DesignDocument by mutableStateOf(initial)
        private set
    var selectedId: Int? by mutableStateOf(initial.root?.id)
    var dirty: Boolean by mutableStateOf(false)
    var revision: Long by mutableStateOf(0L)
        private set
    var diagnostics: List<Diagnostic> by mutableStateOf(validator.validate(initial))
        private set
    var draggingId: Int? by mutableStateOf(null)
    var dropTargetId: Int? by mutableStateOf(null)
    /** Position among the drop target's children (without the dragged node); null appends. */
    var dropIndex: Int? by mutableStateOf(null)
    /** The insertion line drawn on the canvas, in canvas coordinates. */
    var dropLine: Pair<Offset, Offset>? by mutableStateOf(null)
    var previewProblems: List<String> by mutableStateOf(emptyList())
    /** Preview values of top-level states by name (roadmap P9.3): they change only the canvas, never the document. */
    var previewValues: Map<String, JsLiteral> by mutableStateOf(emptyMap())
        private set
    /** Interactive preview: the canvas passes input to the components, whose value changes set preview values. */
    var interactive: Boolean by mutableStateOf(false)

    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo
    val selected: DesignNode? get() = selectedId?.let { document.node(it) }

    fun execute(command: EditCommand, merge: Boolean = true): Boolean {
        val changed = history.execute(command, merge)
        if (changed) publish()
        return changed
    }

    fun seal() = history.seal()

    fun undo(): Boolean = history.undo().also { if (it) publish() }

    fun redo(): Boolean = history.redo().also { if (it) publish() }

    fun markSaved() { dirty = false }

    fun script(): String = ScriptWriter.write(document)

    fun replace(document: DesignDocument) {
        history.reset(document)
        selectedId = document.root?.id
        dirty = false
        previewValues = emptyMap()
        publishQuiet()
    }

    /** Sets (or with null, resets to the script's initial value) the preview value of state [name]. */
    fun setPreviewValue(name: String, value: JsLiteral?) {
        previewValues = if (value == null) previewValues - name else previewValues + (name to value)
    }

    fun resetPreviewValues() { previewValues = emptyMap() }

    /** An event of the interactive canvas: when it changes a state-bound property, the state takes the new value. */
    fun previewEvent(nodeId: Int, event: String, read: (String) -> Any?) {
        val node = document.node(nodeId) ?: return
        val spec = catalog.component(node.type) ?: return
        val values = PreviewControls.values(node, spec, event, read)
        if (values.isNotEmpty() && values.any { (name, value) -> previewValues[name] != value }) previewValues = previewValues + values
    }

    private fun publish() {
        dirty = true
        publishQuiet()
    }

    private fun publishQuiet() {
        document = history.document
        revision++
        if (selectedId != null && document.node(selectedId!!) == null) selectedId = document.root?.id
        diagnostics = validator.validate(document)
    }

    /** Inserts a palette component below the selection, or next to it when the selection takes no children. */
    fun insert(type: String): Boolean {
        val template = NodeTemplates.create(type)
        val target = selected ?: document.root ?: return false
        if (NodeTemplates.acceptsChild(catalog, target.type, target.children.size)) {
            val before = document.nextId
            if (!execute(EditCommand.InsertChild(target.id, target.children.size, DesignChild.Node(template)), merge = false)) return false
            selectedId = before
            return true
        }
        val placement = document.parentOf(target.id) ?: return false
        if (placement.slotName != null) return false
        val before = document.nextId
        if (!execute(EditCommand.InsertChild(placement.parentId, placement.index + 1, DesignChild.Node(template)), merge = false)) return false
        selectedId = before
        return true
    }

    fun deleteSelected(): Boolean {
        val node = selected ?: return false
        if (node.id == document.root?.id) return false
        val placement = document.parentOf(node.id)
        val changed = execute(EditCommand.RemoveNode(node.id), merge = false)
        if (changed) selectedId = placement?.parentId ?: document.root?.id
        return changed
    }

    /** Moves the selected primary child one position up or down among its siblings. */
    fun moveSelected(delta: Int): Boolean {
        val node = selected ?: return false
        val placement = document.parentOf(node.id) ?: return false
        if (placement.slotName != null) return false
        val parent = document.node(placement.parentId) ?: return false
        val target = placement.index + delta
        if (target !in parent.children.indices) return false
        return execute(EditCommand.MoveChild(parent.id, placement.index, parent.id, target), merge = false)
    }

    /** Reparents [id] as the last primary child of [containerId] when the container accepts children. */
    fun moveInto(id: Int, containerId: Int): Boolean {
        if (id == containerId) return false
        val container = document.node(containerId) ?: return false
        val node = document.node(id) ?: return false
        if (node.descendants().any { it.id == containerId }) return false
        val placement = document.parentOf(id) ?: return false
        if (placement.slotName != null) return false
        val children = if (placement.parentId == containerId) container.children.size - 1 else container.children.size
        if (!NodeTemplates.acceptsChild(catalog, container.type, children)) return false
        return execute(EditCommand.MoveChild(placement.parentId, placement.index, containerId, children), merge = false)
    }

    /** Moves [id] to [index] among the primary children of [parentId] (index counts after removal). */
    fun moveTo(id: Int, parentId: Int, index: Int): Boolean {
        if (id == parentId) return false
        val node = document.node(id) ?: return false
        if (node.descendants().any { it.id == parentId }) return false
        val placement = document.parentOf(id) ?: return false
        if (placement.slotName != null) return false
        val parent = document.node(parentId) ?: return false
        val count = if (placement.parentId == parentId) parent.children.size - 1 else parent.children.size
        if (placement.parentId != parentId && !NodeTemplates.acceptsChild(catalog, parent.type, count)) return false
        return execute(EditCommand.MoveChild(placement.parentId, placement.index, parentId, index.coerceIn(0, count)), merge = false)
    }

    fun canDropInto(id: Int, containerId: Int): Boolean {
        if (id == containerId) return false
        val container = document.node(containerId) ?: return false
        val node = document.node(id) ?: return false
        if (node.descendants().any { it.id == containerId }) return false
        val placement = document.parentOf(id) ?: return false
        if (placement.slotName != null) return false
        val children = if (placement.parentId == containerId) container.children.size - 1 else container.children.size
        return NodeTemplates.acceptsChild(catalog, container.type, children)
    }

    /** The nearest ancestor (or the node itself) of [id] that accepts a dropped node. */
    fun dropContainerFor(dragged: Int, id: Int?): Int? {
        var current = id
        while (current != null) {
            if (canDropInto(dragged, current)) return current
            current = document.parentOf(current)?.parentId
        }
        return null
    }

    /**
     * Chooses where a canvas drag would land (roadmap P9.1): the container under [hit] that accepts [dragged]
     * and, along that container's layout axis, the position under [pointer]; [boundsOf] gives a node's
     * bounds on the canvas, or null when it has none.
     */
    fun placeDrop(dragged: Int, hit: Int?, pointer: Offset, boundsOf: (Int) -> Rect?) {
        val container = dropContainerFor(dragged, hit)?.let { document.node(it) }
        dropTargetId = container?.id
        if (container == null) {
            dropIndex = null
            dropLine = null
            return
        }
        val children = container.children
            .filter { child -> (child as? DesignChild.Node)?.node?.id != dragged }
            .map { child -> (child as? DesignChild.Node)?.let { boundsOf(it.node.id) } }
        val placement = DropPlacements.place(LayoutAxes.of(container.type), pointer, boundsOf(container.id), children)
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
        if (dragged != null && target != null) {
            val moved = if (index != null) moveTo(dragged, target, index) else moveInto(dragged, target)
            if (moved) selectedId = dragged
        }
    }
}
