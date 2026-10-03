package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.model.*

/** Resolved arguments have catalog defaults applied, without changing the operation order. */
internal data class ModifierInstruction(val name: String, val args: List<UiValue>) {
    fun number(index: Int) = (args[index] as UiValue.Num).value.toFloat()
    fun dimension(index: Int) = (args[index] as UiValue.Dp).value.toFloat()
    fun boolean(index: Int) = (args[index] as UiValue.Bool).value

    /** Logical edges, in start/top/end/bottom order; direction is left to Compose. */
    fun padding(): List<Float> = when (args.size) {
        1 -> List(4) { dimension(0) }
        2 -> listOf(dimension(0), dimension(1), dimension(0), dimension(1))
        else -> args.indices.map(::dimension)
    }
}

/** Main-thread registry. Removing an old composition cannot unregister its replacement. */
internal class NodeCommandRegistry {
    class Handle(
        val focus: (() -> Unit)? = null,
        val blur: (() -> Unit)? = null,
        val scroll: (suspend (index: Int?, offset: Int?) -> Unit)? = null,
        val indexedScroll: Boolean = false,
    )

    private val handles = HashMap<Int, Handle>()
    private val componentHandles = HashMap<Int, Handle>()

    fun register(nodeId: Int, handle: Handle): () -> Unit {
        handles[nodeId] = handle
        return { if (handles[nodeId] === handle) handles.remove(nodeId) }
    }

    /** Component-owned commands augment modifier commands; e.g. lazy scrolling keeps modifier focus. */
    fun registerComponent(nodeId: Int, handle: Handle): () -> Unit {
        componentHandles[nodeId] = handle
        return { if (componentHandles[nodeId] === handle) componentHandles.remove(nodeId) }
    }

    fun clear() { handles.clear(); componentHandles.clear() }

    fun validate(command: UiCommand) { resolve(command) }

    /** Resolve before launching so argument failures stay synchronous. Recheck liveness on execution. */
    fun resolve(command: UiCommand): suspend () -> Unit {
        val id = when (command) {
            is UiCommand.Focus -> command.nodeId
            is UiCommand.Blur -> command.nodeId
            is UiCommand.ScrollTo -> command.nodeId
            else -> throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT)
        }
        val specific = componentHandles[id]
        val useSpecific = specific != null && when (command) {
            is UiCommand.Focus -> specific.focus != null
            is UiCommand.Blur -> specific.blur != null
            is UiCommand.ScrollTo -> specific.scroll != null
            else -> false
        }
        val owner = if (useSpecific) componentHandles else handles
        val handle = owner[id] ?: if (specific == null) detached(id) else unsupported(id)
        val action: suspend () -> Unit = when (command) {
            is UiCommand.Focus -> handle.focus?.let { action -> suspend { action() } } ?: unsupported(id)
            is UiCommand.Blur -> handle.blur?.let { action -> suspend { action() } } ?: unsupported(id)
            is UiCommand.ScrollTo -> {
                val scroll = handle.scroll ?: unsupported(id)
                if (!handle.indexedScroll && command.index != null) unsupported(id)
                suspend { scroll(command.index, command.offset) }
            }
            else -> unsupported(id)
        }
        return {
            if (owner[id] !== handle) detached(id)
            action()
        }
    }

    suspend fun execute(command: UiCommand) = resolve(command).invoke()

    private fun detached(nodeId: Int): Nothing = throw ComposeUiContractException(ComposeUiErrorCodes.NODE_DETACHED, nodeId = nodeId)
    private fun unsupported(nodeId: Int): Nothing = throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_ARGUMENT, nodeId = nodeId)
}

internal object ModifierMapper {
    private val nativeClicks = setOf(
        ComposeUiComponents.BUTTON, ComposeUiComponents.ELEVATED_BUTTON, ComposeUiComponents.FILLED_TONAL_BUTTON,
        ComposeUiComponents.OUTLINED_BUTTON, ComposeUiComponents.TEXT_BUTTON, ComposeUiComponents.ICON_BUTTON,
        ComposeUiComponents.SWITCH, ComposeUiComponents.CHECKBOX, ComposeUiComponents.RADIO_BUTTON,
    )

    /** Pure boundary validation used both by rendering and JVM tests. */
    fun parse(operations: List<ModifierOp>, parentType: String? = null): List<ModifierInstruction> {
        if (operations.size > ComposeUiLimits.MAX_MODIFIER_OPS) throw ComposeUiContractException(ComposeUiErrorCodes.LIMIT_EXCEEDED)
        return operations.map { operation ->
            ComponentCatalog.V1.validateModifier(operation, parentType)
            val spec = ComponentCatalog.V1.modifier(operation.name)!!
            val signature = spec.signatures.first { signature ->
                operation.args.size in signature.requiredCount..signature.args.size &&
                    operation.args.indices.all { signature.args[it].type.accepts(operation.args[it]) }
            }
            ModifierInstruction(operation.name, signature.args.mapIndexed { index, argument ->
                operation.args.getOrNull(index) ?: argument.defaultValue!!
            })
        }
    }

    /** Native controls keep their own callback; without one, an enabled modifier supplies activation. */
    fun clickCallback(node: UiNode): Int? = node.callbacks[ComposeUiEvents.CLICK] ?: node.modifier.lastOrNull {
        it.name == ComposeUiModifiers.CLICKABLE && ((it.args.getOrNull(1) as? UiValue.Bool)?.value ?: true)
    }?.let { (it.args[0] as UiValue.Num).value.toInt() }

    fun interactionEnabled(node: UiNode): Boolean = (node.props[ComposeUiProps.ENABLED] as? UiValue.Bool)?.value ?: true

    @Composable
    fun Modifier.map(
        node: UiNode,
        commands: NodeCommandRegistry,
        rowScope: RowScope? = null,
        columnScope: ColumnScope? = null,
        boxScope: BoxScope? = null,
        emit: (type: String, callbackId: Int, payload: Bundle) -> Unit,
    ): Modifier {
        if (listOfNotNull(rowScope, columnScope, boxScope).size > 1) scopeMismatch(node.nodeId)
        val parentType = when {
            rowScope != null -> ComposeUiComponents.ROW
            columnScope != null -> ComposeUiComponents.COLUMN
            boxScope != null -> ComposeUiComponents.BOX
            else -> null
        }
        val operations = remember(node.modifier, parentType) { parse(node.modifier, parentType) }
        val currentNode by rememberUpdatedState(node)
        val currentEmit by rememberUpdatedState(emit)
        val focusRequester = remember(node.nodeId) { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val focused = remember(node.nodeId) { booleanArrayOf(false) }
        val hasExplicitClick = operations.any { it.name == ComposeUiModifiers.CLICKABLE }
        val focusable = ComposeUiCommands.FOCUS in ComponentCatalog.V1.requireComponent(node.type).commands || hasExplicitClick
        val scrollStates = ArrayList<ScrollState>()
        val operationOccurrences = HashMap<String, Int>()
        var modifier: Modifier = this

        // These observers precede the focus target supplied by clickable or a Material control.
        if (focusable) modifier = modifier.focusRequester(focusRequester)
        if (focusable || ComposeUiEvents.FOCUS_CHANGE in node.callbacks) modifier = modifier.onFocusChanged { state ->
            if (focused[0] != state.hasFocus) {
                focused[0] = state.hasFocus
                currentNode.callbacks[ComposeUiEvents.FOCUS_CHANGE]?.let { callback ->
                    currentEmit(ComposeUiEvents.FOCUS_CHANGE, callback, Bundle().apply { putBoolean(ComposeUiEventFields.FOCUSED, state.hasFocus) })
                }
            }
        }

        operations.forEach { operation ->
            val occurrence = operationOccurrences.getOrDefault(operation.name, 0)
            operationOccurrences[operation.name] = occurrence + 1
            // Key the whole loop item, not a branch inside a replaceable when group.
            key(node.nodeId, operation.name, occurrence) {
                modifier = when (operation.name) {
                    ComposeUiModifiers.PADDING -> operation.padding().let { modifier.padding(it[0].dp, it[1].dp, it[2].dp, it[3].dp) }
                    ComposeUiModifiers.SIZE -> modifier.size(operation.dimension(0).dp, operation.dimension(if (operation.args.size == 1) 0 else 1).dp)
                    ComposeUiModifiers.WIDTH -> modifier.width(operation.dimension(0).dp)
                    ComposeUiModifiers.HEIGHT -> modifier.height(operation.dimension(0).dp)
                    ComposeUiModifiers.FILL_MAX_WIDTH -> modifier.fillMaxWidth(operation.number(0))
                    ComposeUiModifiers.FILL_MAX_HEIGHT -> modifier.fillMaxHeight(operation.number(0))
                    ComposeUiModifiers.FILL_MAX_SIZE -> modifier.fillMaxSize(operation.number(0))
                    ComposeUiModifiers.WEIGHT -> when {
                        rowScope != null -> with(rowScope) { modifier.weight(operation.number(0), operation.boolean(1)) }
                        columnScope != null -> with(columnScope) { modifier.weight(operation.number(0), operation.boolean(1)) }
                        else -> scopeMismatch(node.nodeId)
                    }
                    ComposeUiModifiers.ALIGN -> with(boxScope ?: scopeMismatch(node.nodeId)) { modifier.align(ValueMapper.boxAlignment(operation.args[0])) }
                    ComposeUiModifiers.BACKGROUND -> modifier.background(ValueMapper.color(operation.args[0]), ValueMapper.shape(operation.args[1]))
                    ComposeUiModifiers.BORDER -> modifier.border(operation.dimension(0).dp, ValueMapper.color(operation.args[1]), ValueMapper.shape(operation.args[2]))
                    ComposeUiModifiers.CLIP -> modifier.clip(ValueMapper.shape(operation.args[0]))
                    ComposeUiModifiers.ALPHA -> modifier.alpha(operation.number(0))
                    ComposeUiModifiers.CLICKABLE -> modifier.clickable(enabled = operation.boolean(1) && interactionEnabled(node)) {
                        currentEmit(ComposeUiEvents.CLICK, (operation.args[0] as UiValue.Num).value.toInt(), Bundle())
                    }
                    ComposeUiModifiers.VERTICAL_SCROLL, ComposeUiModifiers.HORIZONTAL_SCROLL -> {
                        val state = remember { ScrollState(0) }
                        scrollStates += state
                        LaunchedEffect(state) {
                            snapshotFlow { state.value }.drop(1).collect { offset ->
                                currentNode.callbacks[ComposeUiEvents.SCROLL]?.let { callback ->
                                    currentEmit(ComposeUiEvents.SCROLL, callback, Bundle().apply { putInt(ComposeUiEventFields.OFFSET, offset) })
                                }
                            }
                        }
                        if (operation.name == ComposeUiModifiers.VERTICAL_SCROLL) modifier.verticalScroll(state, enabled = operation.boolean(0))
                        else modifier.horizontalScroll(state, enabled = operation.boolean(0))
                    }
                    ComposeUiModifiers.OFFSET -> modifier.offset(operation.dimension(0).dp, operation.dimension(1).dp)
                    ComposeUiModifiers.ASPECT_RATIO -> modifier.aspectRatio(operation.number(0))
                    ComposeUiModifiers.TEST_TAG -> modifier.testTag((operation.args[0] as UiValue.Str).value)
                    ComposeUiModifiers.SEMANTICS -> modifier.semantics {
                        // Null means no explicit description, including decorative Image/Icon nodes.
                        (operation.args[0] as? UiValue.Str)?.let { contentDescription = it.value }
                    }
                    else -> throw ComposeUiContractException(ComposeUiErrorCodes.INVALID_MODIFIER, nodeId = node.nodeId)
                }
            }
        }

        val enabled = interactionEnabled(node)
        val longClick = node.callbacks[ComposeUiEvents.LONG_CLICK]
        if (TextFieldController.isTextField(node)) {
            // Text editors own selection, caret placement, IME activation and the native toolbar.
            // Observe their gestures without competing for consumed events or swallowing release.
            // Explicit clickable modifier operations above retain their declared chain semantics.
            if (enabled && (node.callbacks[ComposeUiEvents.CLICK] != null || longClick != null)) {
                modifier = modifier.pointerInput(node.nodeId) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val tapped = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            var released = false
                            var canceled = false
                            while (!released && !canceled) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pointer = event.changes.firstOrNull { it.id == down.id }
                                canceled = pointer == null || (pointer.position - down.position).getDistance() > viewConfiguration.touchSlop ||
                                    event.changes.any { it.id != down.id && it.pressed }
                                released = pointer?.pressed == false
                            }
                            released && !canceled
                        }
                        if (interactionEnabled(currentNode)) {
                            val type = when (tapped) { true -> ComposeUiEvents.CLICK; null -> ComposeUiEvents.LONG_CLICK; false -> null }
                            type?.let { event -> currentNode.callbacks[event]?.let { currentEmit(event, it, Bundle()) } }
                        }
                    }
                }
            }
        } else if (node.type == ComposeUiComponents.SLIDER && enabled && (node.callbacks[ComposeUiEvents.CLICK] != null || longClick != null)) {
            // Slider owns its down/drag stream. Observe a stationary release without consuming it,
            // so adding common callbacks cannot disable value changes or drag completion.
            modifier = modifier.pointerInput(node.nodeId, longClick != null) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPressed = if (longClick != null) awaitLongPressOrCancellation(down.id) != null else {
                        waitForUpOrCancellation()
                        false
                    }
                    if (longPressed) {
                        currentNode.callbacks[ComposeUiEvents.LONG_CLICK]?.let { currentEmit(ComposeUiEvents.LONG_CLICK, it, Bundle()) }
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    } else {
                        val up = currentEvent.changes.firstOrNull { it.id == down.id && !it.pressed }
                        if (up != null && (up.position - down.position).getDistance() <= viewConfiguration.touchSlop) {
                            clickCallback(currentNode)?.let { currentEmit(ComposeUiEvents.CLICK, it, Bundle()) }
                        }
                    }
                }
            }.semantics {
                if (clickCallback(node) != null) onClick {
                    clickCallback(currentNode)?.let { currentEmit(ComposeUiEvents.CLICK, it, Bundle()) }
                    true
                }
                if (longClick != null) onLongClick {
                    currentNode.callbacks[ComposeUiEvents.LONG_CLICK]?.let { currentEmit(ComposeUiEvents.LONG_CLICK, it, Bundle()) }
                    true
                }
            }
        } else if (node.type !in nativeClicks && !hasExplicitClick && (clickCallback(node) != null || longClick != null)) {
            modifier = modifier.combinedClickable(enabled = enabled,
                onLongClick = longClick?.let { { currentNode.callbacks[ComposeUiEvents.LONG_CLICK]?.let { currentEmit(ComposeUiEvents.LONG_CLICK, it, Bundle()) } } },
                onClick = { clickCallback(currentNode)?.let { currentEmit(ComposeUiEvents.CLICK, it, Bundle()) } })
        } else if (longClick != null && enabled) {
            // Observe normal presses without taking their down event from Material controls. A long
            // press consumes release on Initial, canceling their click instead of firing both.
            modifier = modifier.pointerInput(node.nodeId) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (awaitLongPressOrCancellation(down.id) != null) {
                        currentNode.callbacks[ComposeUiEvents.LONG_CLICK]?.let { currentEmit(ComposeUiEvents.LONG_CLICK, it, Bundle()) }
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
            }.semantics {
                onLongClick {
                    currentNode.callbacks[ComposeUiEvents.LONG_CLICK]?.let { currentEmit(ComposeUiEvents.LONG_CLICK, it, Bundle()) }
                    true
                }
            }
        }

        val retainedScrollStates = scrollStates.toList()
        DisposableEffect(commands, node.nodeId, focusable, retainedScrollStates, focusManager) {
            val unregister = commands.register(node.nodeId, NodeCommandRegistry.Handle(
                focus = if (focusable) ({ focusRequester.requestFocus(); Unit }) else null,
                blur = if (focusable) ({ if (focused[0]) focusManager.clearFocus(force = true) }) else null,
                // A V1 offset command addresses the first (outermost) scroll operation in the chain.
                scroll = if (retainedScrollStates.isEmpty()) null else { _, offset -> retainedScrollStates.first().scrollTo(offset ?: 0); Unit },
            ))
            onDispose { unregister() }
        }
        return modifier
    }

    private fun scopeMismatch(nodeId: Int): Nothing = throw ComposeUiContractException(ComposeUiErrorCodes.SCOPE_MISMATCH, nodeId = nodeId)
}
