package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Props as V
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Slots as S
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Events as E
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Fields as F
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/** Selection setters are requests; only accepted host frames change the selected value. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WideDatePicker(node: UiNode, modifier: Modifier, emit: (String, Bundle) -> Unit) {
    val currentNode by rememberUpdatedState(node)
    val currentEmit by rememberUpdatedState(emit)
    val years = node.wideInt(V.YEAR_START, 1900)..node.wideInt(V.YEAR_END, 2100)
    key(node.nodeId, years) {
        val selected = (node.props[V.SELECTED_DATE_MILLIS] as? UiValue.Num)?.value?.toLong()
        val initialMonth = remember(years) {
            // M3 otherwise starts at today's month even when a custom year range excludes it.
            GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
                val currentYear = get(Calendar.YEAR)
                if (currentYear !in years) {
                    set(Calendar.YEAR, currentYear.coerceIn(years))
                    set(Calendar.MONTH, if (currentYear < years.first) Calendar.JANUARY else Calendar.DECEMBER)
                }
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val native = rememberDatePickerState(initialSelectedDateMillis = selected,
            initialDisplayedMonthMillis = selected ?: initialMonth, yearRange = years,
            initialDisplayMode = if (node.wideEnum(V.DISPLAY_MODE, "picker") == "input") DisplayMode.Input else DisplayMode.Picker)
        val controlled = remember(native) { ControlledDatePickerState(native, { currentNode }, { type, payload -> currentEmit(type, payload) }) }
        SideEffect { controlled.accept() }
        DatePicker(state = controlled, modifier = modifier, showModeToggle = node.wideBoolean(V.SHOW_MODE_TOGGLE, true),
            // Opening a picker must not steal the IME; native field gestures choose focus.
            focusRequester = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WideTimePicker(node: UiNode, modifier: Modifier, emit: (String, Bundle) -> Unit) {
    val currentNode by rememberUpdatedState(node)
    val currentEmit by rememberUpdatedState(emit)
    val state = remember(node.nodeId) { ControlledTimePickerState({ currentNode }, { type, payload -> currentEmit(type, payload) }) }
    SideEffect { state.accept() }
    if (node.wideEnum(V.DISPLAY_MODE, "picker") == "input") TimeInput(state, modifier)
    else TimePicker(state, modifier)
}

@Composable
internal fun WideNavigationDrawer(node: UiNode, modifier: Modifier, render: @Composable (Int) -> Unit,
    content: @Composable () -> Unit, emit: (String, Bundle) -> Unit) {
    val currentNode by rememberUpdatedState(node)
    val currentEmit by rememberUpdatedState(emit)
    val open = node.wideBoolean(P.OPEN)
    val controlled = remember(node.nodeId) { ControlledDrawerState({ currentNode }, { type, payload -> currentEmit(type, payload) }) }
    val state = controlled.native
    DisposableEffect(controlled) { onDispose { controlled.dispose() } }
    LaunchedEffect(controlled, open) { controlled.accept(open) }
    // The drawerState overload's native predictive-back handler animates before consulting its
    // veto. Use the public stateless sheet and request a host update before changing visibility.
    BackHandler(enabled = open) { currentEmit(E.OPEN_CHANGE, Bundle().apply { putBoolean(F.OPEN, false) }) }
    ModalNavigationDrawer(modifier = modifier, drawerState = state,
        gesturesEnabled = node.wideBoolean(V.GESTURES_ENABLED, true),
        drawerContent = {
            ModalDrawerSheet(windowInsets = WindowInsets(0, 0, 0, 0)) {
                render(node.slots.getValue(S.DRAWER_CONTENT))
            }
        }, content = content)
}

/** Public TooltipState supplies controlled visibility without timing out or committing a gesture. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WideTooltip(node: UiNode, modifier: Modifier, render: @Composable (Int) -> Unit,
    content: @Composable () -> Unit, emit: (String, Bundle) -> Unit) {
    val currentNode by rememberUpdatedState(node)
    val currentEmit by rememberUpdatedState(emit)
    val state = remember(node.nodeId) { ControlledTooltipState({ currentNode }, { type, payload -> currentEmit(type, payload) }) }
    SideEffect { state.accept() }
    TooltipBox(positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { render(node.slots.getValue(S.TOOLTIP)) } },
        state = state, modifier = modifier, onDismissRequest = state::dismiss,
        focusable = true, enableUserInput = true, hasAction = false, content = content)
}
