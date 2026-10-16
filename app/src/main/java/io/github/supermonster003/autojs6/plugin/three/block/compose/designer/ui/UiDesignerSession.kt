package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.idName
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Diagnostic
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewCapability
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifference
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifferences
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiAndroidCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditHistory
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEvents
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiHandler
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.PreviewDifferenceScanner
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiLayoutFiles
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.DropPlacements
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LayoutAxis
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.DeclarationEvaluator

/**
 * The editing state of one open `"ui"` mode project (roadmap P10.3 / P10.4): the undo history over the
 * [UiDocument], the selection, the fragment shown on the canvas, the canvas drag state, the validator's
 * diagnostics, the preview's problems and the script's preview differences (roadmap P11.2). Views observe its
 * Compose state; all mutations go through commands.
 */
internal class UiDesignerSession(
    initial: UiDocument,
    val catalog: UiCatalog = UiCatalog.load(),
    /** The saved text of each layout file (roadmap P11.3), by path; a restored draft passes the files as they are on disk. */
    savedFiles: Map<String, String> = UiScriptWriter.writeFiles(initial),
) {
    private val history = UiEditHistory(initial)
    private val validator = UiValidator(catalog)
    /** The native Android XML catalog (roadmap P11.3): the attributes of elements in native fragments. */
    val android: UiAndroidCatalog = UiAndroidCatalog.load()
    private var savedFiles: Map<String, String> = savedFiles

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
    /** The `{{ }}` names the fragment on the canvas uses, in order of first use (from the last preview request). */
    var bindingNames: List<String> by mutableStateOf(emptyList())
    /** The script's value-like top-level declarations as the preview evaluated them (JSON text by name). */
    var previewDeclarations: Map<String, String> by mutableStateOf(emptyMap())
    /** Why a declaration (by name, or "" for the whole script) could not be evaluated. */
    var declarationFailures: Map<String, String> by mutableStateOf(emptyMap())
    /** The names of the script's value-like top-level declarations, offered as `{{ }}` binding targets. */
    var declaredNames: List<String> by mutableStateOf(declaredNamesOf(UiScriptWriter.write(initial)))
        private set
    /** The lines of the script that behave differently in the preview (roadmap P11.2 / D55), before host filtering. */
    var differences: List<PreviewDifference> by mutableStateOf(PreviewDifferenceScanner.scan(UiScriptWriter.write(initial)))
        private set
    /** What the preview host announces beyond contract v2; each capability hides the differences it closes. */
    var hostCapabilities: Set<PreviewCapability> by mutableStateOf(emptySet())
    /** The last handler stub added (`ui.<id>.on('<event>')`), for the editor to announce. */
    var lastHandlerStub: UiHandler? by mutableStateOf(null)
    /** The interactive preview is on (roadmap P10.6): the host runs the script in the canvas; not part of the document. */
    var interactive: Boolean by mutableStateOf(false)
    /**
     * Whether layout files can be stored next to this document (roadmap P11.3 / D66): a private project or a file Uri;
     * not a content Uri while the designer holds no access to its folder.
     */
    var layoutFilesAvailable: Boolean by mutableStateOf(true)
    /** Whether saving a document with layout files makes it a folder project (a private single-file project, a read-only document). */
    var layoutFilesPromote: Boolean by mutableStateOf(false)
    /** False when the preview host is known not to take a project's files (before contract v3, roadmap P11.3). */
    var hostResources: Boolean by mutableStateOf(true)

    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo
    val selected: XmlNode.Element? get() = selectedId?.let { document.element(it) }
    /** The differences the issues list shows: informational, never counted as problems. */
    val visibleDifferences: List<PreviewDifference> get() = PreviewDifferences.visible(differences, hostCapabilities)
    val root: XmlNode.Element? get() = document.fragments.getOrNull(fragment)?.root

    fun execute(command: UiEditCommand, merge: Boolean = true): Boolean {
        val changed = history.execute(command, merge)
        if (changed) publish()
        return changed
    }

    fun seal() = history.seal()

    fun undo(): Boolean = history.undo().also { if (it) publish() }

    fun redo(): Boolean = history.redo().also { if (it) publish() }

    fun markSaved() {
        dirty = false
        savedFiles = files()
    }

    fun script(): String = UiScriptWriter.write(document)

    /** The current text of every layout file the script refers to (roadmap P11.3), by path. */
    fun files(): Map<String, String> = UiScriptWriter.writeFiles(document)

    /** The layout files whose text differs from the saved one: what saving writes and drafts keep. */
    fun changedFiles(): Map<String, String> = files().filter { (path, text) -> savedFiles[path] != text }

    fun replace(document: UiDocument, savedFiles: Map<String, String> = UiScriptWriter.writeFiles(document)) {
        this.savedFiles = savedFiles
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
        val script = UiScriptWriter.write(document)
        declaredNames = declaredNamesOf(script)
        differences = PreviewDifferenceScanner.scan(script)
    }

    /**
     * Inserts a palette template (roadmap P10.4): as the last child of the selected container, else after the
     * selected element among its siblings; the new element is selected.
     */
    fun insert(tag: String): Boolean {
        val target = selected ?: root ?: return false
        // Roadmap P11.3: a native Android XML fragment takes view classes with android: attributes.
        val template = if (document.isNative(target.id)) UiTemplates.createNative(tag) else UiTemplates.create(tag)
        val before = document.nextId
        val inserted = if (isContainer(target)) {
            execute(UiEditCommand.InsertElement(target.id, target.elementChildren.size, template), merge = false)
        } else {
            val placement = document.parentOf(target.id) ?: return false
            val parent = document.element(placement.parentId) ?: return false
            val index = parent.elementChildren.indexOfFirst { it.id == target.id }
            execute(UiEditCommand.InsertElement(parent.id, index + 1, template), merge = false)
        }
        if (inserted) selectedId = before
        return inserted
    }

    /** Whether the fragment on the canvas is native Android XML (roadmap P11.3): the palette offers view classes then. */
    val nativeFragment: Boolean get() = document.fragments.getOrNull(fragment)?.let(document::isNative) == true

    /** The native catalog class of [element] (roadmap P11.3), or null for an unknown or special tag. */
    fun nativeClassOf(element: XmlNode.Element): String? = validator.nativeClassOf(element)

    /** The native catalog class of [element]'s parent, whose layout parameters it takes; null at the root or under `<merge>`. */
    fun nativeParentClassOf(element: XmlNode.Element): String? =
        document.parentOf(element.id)?.let { document.element(it.parentId) }?.takeIf { it.tag != "merge" }?.let(validator::nativeClassOf)

    /** The native attribute [name] of [element] for the inspector's editors, or null when the catalog does not know it. */
    fun nativeAttr(element: XmlNode.Element, name: String): UiCatalog.Attr? =
        android.attr(nativeClassOf(element), nativeParentClassOf(element), name)?.let(android::toUiAttr)

    /** The view class the validator checks [tag] against (a catalog tag or a class name), or null for unknown tags. */
    fun viewClassOf(tag: String): String? = validator.resolve(tag)?.viewClass

    /** Whether handler stubs can be appended for [element]: its fragment is passed to `ui.layout` as a statement. */
    fun canAddHandlers(element: XmlNode.Element): Boolean = document.fragments.getOrNull(document.fragmentOf(element.id))?.stubOffset?.let { it >= 0 } == true

    /** The plain-text id of [element] (`ui.<id>`), or null when it has none or it is bound / computed. */
    fun scriptIdOf(element: XmlNode.Element): String? =
        element.idName()

    /** The events the script already handles for [element], by its id. */
    fun handledEvents(element: XmlNode.Element): Set<String> {
        val id = scriptIdOf(element) ?: return emptySet()
        return document.handlers.filter { it.id == id }.map { it.event }.toSet()
    }

    /**
     * Appends a `ui.<id>.on('<event>', ...)` stub for [element] (roadmap D52, option (b)), giving the element a fresh
     * id (`<tag>1`, `<tag>2`, ...) first when it has none; one undo step.
     */
    fun addHandler(element: XmlNode.Element, event: UiEvents.Event): Boolean {
        if (!canAddHandlers(element)) return false
        val existing = scriptIdOf(element)
        val assign = if (existing == null) freshId(element.tag) else null
        val id = existing ?: assign ?: return false
        val added = execute(UiEditCommand.AddHandlerStub(element.id, event, assign), merge = false)
        if (added) lastHandlerStub = UiHandler(id, event.name)
        return added
    }

    private fun freshId(tag: String): String {
        val taken = document.elementIds()
        val stem = tag.filter { it.isLetterOrDigit() || it == '_' }.ifEmpty { "view" }
        var counter = 1
        while ("$stem$counter" in taken) counter++
        return "$stem$counter"
    }

    /**
     * Moves the literal fragment on the canvas into the layout file [path] (roadmap P11.3 / D66) as one undo step and
     * shows that file; it is written when the document is saved.
     */
    fun extractToFile(path: String): Boolean = rewrite(path) { UiLayoutFiles.extract(document, fragment, path) }

    /** Adds the layout file [path] with one empty root (native Android XML when [native]) as one undo step and shows it. */
    fun addLayoutFile(path: String, native: Boolean): Boolean = rewrite(path) { UiLayoutFiles.addFile(document, path, native) }

    private fun rewrite(path: String, rewritten: () -> UiDocument): Boolean {
        val next = try {
            rewritten()
        } catch (error: IllegalArgumentException) {
            return false
        } catch (error: DesignerParseError) {
            return false
        }
        if (!execute(UiEditCommand.ReplaceDocument(next), merge = false)) return false
        val index = document.fragments.indexOfFirst { it.file?.path == path }
        if (index >= 0) {
            fragment = index
            selectedId = document.fragments[index].root.id
        }
        return true
    }

    /** Moves [id] into [containerId] as its last element child (outline drop onto a container). */
    fun moveInto(id: Int, containerId: Int): Boolean {
        if (!canDropInto(id, containerId)) return false
        val container = document.element(containerId) ?: return false
        val count = container.elementChildren.count { it.id != id }
        return execute(UiEditCommand.MoveElement(id, containerId, count), merge = false)
    }

    /** Moves [id] to be the [index]-th element child of [parentId] (counted without the moved element). */
    fun moveTo(id: Int, parentId: Int, index: Int): Boolean {
        if (!canDropInto(id, parentId)) return false
        val parent = document.element(parentId) ?: return false
        val count = parent.elementChildren.count { it.id != id }
        return execute(UiEditCommand.MoveElement(id, parentId, index.coerceIn(0, count)), merge = false)
    }

    private fun declaredNamesOf(script: String): List<String> =
        try { DeclarationEvaluator.declarationsOf(script).map { it.name } } catch (error: RuntimeException) { emptyList() }

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
