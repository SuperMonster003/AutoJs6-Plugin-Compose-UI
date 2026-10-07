package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Events as E
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Fields as F
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Props as V

@OptIn(ExperimentalMaterial3Api::class)
internal class ControlledDatePickerState(
    private val native: DatePickerState,
    private val node: () -> UiNode,
    private val emit: (String, Bundle) -> Unit,
) : DatePickerState by native {
    override var selectedDateMillis: Long?
        get() = (node().props[V.SELECTED_DATE_MILLIS] as? UiValue.Num)?.value?.toLong()
        set(value) {
            if (value != selectedDateMillis) emit(E.DATE_CHANGE, Bundle().apply {
                value?.let { putLong(F.SELECTED_DATE_MILLIS, it) }
            })
        }
    override var displayMode: DisplayMode
        // Publish mode together with the backing displayed month, rather than composing a new
        // calendar with the old month before the accepted SideEffect has synchronized them.
        get() = native.displayMode
        set(value) {
            if (value != displayMode) emit(E.DISPLAY_MODE_CHANGE, Bundle().apply {
                putString(F.DISPLAY_MODE, if (value == DisplayMode.Input) "input" else "picker")
            })
        }

    fun accept() {
        native.selectedDateMillis = selectedDateMillis
        // Material aligns the displayed month to its selected date only when mode changes.
        // Synchronize its backing state without dispatching another user request.
        val acceptedMode = if (node().wideEnum(V.DISPLAY_MODE, "picker") == "input") DisplayMode.Input else DisplayMode.Picker
        if (native.displayMode != acceptedMode) native.displayMode = acceptedMode
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal class ControlledTimePickerState(
    private val node: () -> UiNode,
    private val emit: (String, Bundle) -> Unit,
) : TimePickerState {
    override var selection by mutableStateOf(TimePickerSelectionMode.Hour)
    private val draft = PendingTimeSelection(node().wideInt(V.HOUR), node().wideInt(V.MINUTE))
    override var is24hour: Boolean
        get() = node().wideBoolean(V.IS_24_HOUR, true)
        set(value) { /* Formatting is owned by the accepted frame. */ }
    override var hour: Int
        get() = node().wideInt(V.HOUR)
        set(value) { if (value != hour) draft.hour(value)?.let(::request) }
    override var minute: Int
        get() = node().wideInt(V.MINUTE)
        set(value) { if (value != minute) draft.minute(value)?.let(::request) }

    fun accept() = draft.accept(hour, minute)
    private fun request(value: PendingTimeSelection.Value) = emit(E.TIME_CHANGE, Bundle().apply {
        putInt(F.HOUR, value.hour); putInt(F.MINUTE, value.minute)
    })
}

@OptIn(ExperimentalMaterial3Api::class)
internal class ControlledTooltipState(
    private val node: () -> UiNode,
    private val emit: (String, Bundle) -> Unit,
) : TooltipState {
    override val transition = MutableTransitionState(node().wideBoolean(P.OPEN))
    override val isVisible get() = transition.currentState || transition.targetState
    override val isPersistent = true
    override suspend fun show(mutatePriority: MutatePriority) {
        if (!node().wideBoolean(P.OPEN)) emit(E.OPEN_CHANGE, Bundle().apply { putBoolean(F.OPEN, true) })
    }
    override fun dismiss() {
        if (node().wideBoolean(P.OPEN)) emit(E.OPEN_CHANGE, Bundle().apply { putBoolean(F.OPEN, false) })
    }
    override fun onDispose() = Unit
    fun accept() { transition.targetState = node().wideBoolean(P.OPEN) }
}

/** Material also confirms anchor corrections and cancelled animations, not only user gestures. */
internal class ControlledDrawerState(
    private val node: () -> UiNode,
    private val emit: (String, Bundle) -> Unit,
) {
    private var active = true
    private var applyingModel = false
    private var transitionSerial = 0L
    val native = DrawerState(if (node().wideBoolean(P.OPEN)) DrawerValue.Open else DrawerValue.Closed, ::confirm)

    private fun confirm(value: DrawerValue): Boolean {
        if (!active) return false
        val requested = value == DrawerValue.Open
        if (requested == node().wideBoolean(P.OPEN)) return true
        // anchoredDrag's finally may confirm its original currentValue after a replacement frame
        // cancels an animation. Treating that as a gesture would undo the just-accepted props.
        if (!applyingModel && value != native.currentValue) {
            emit(E.OPEN_CHANGE, Bundle().apply { putBoolean(F.OPEN, requested) })
        }
        return false
    }

    suspend fun accept(open: Boolean) {
        val target = if (open) DrawerValue.Open else DrawerValue.Closed
        if (native.currentValue == target && !native.isAnimationRunning) return
        val serial = ++transitionSerial
        applyingModel = true
        try {
            if (open) native.open() else native.close()
        } finally {
            try {
                // A remeasure or a superseding native drag can cancel the transition between
                // anchors. An unchanged accepted model still wins, including rapid content edits.
                if (active && serial == transitionSerial && node().wideBoolean(P.OPEN) == open && native.currentValue != target) {
                    withContext(NonCancellable) { native.snapTo(target) }
                }
            } finally { if (serial == transitionSerial) applyingModel = false }
        }
    }

    fun dispose() { active = false; transitionSerial++; applyingModel = false }
}
