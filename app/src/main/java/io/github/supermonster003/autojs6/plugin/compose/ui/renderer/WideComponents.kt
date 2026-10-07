package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiContract
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.ComposeUiEventFields as F
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.ComposeUiSlots as S
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Components as C
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Props as V
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Slots as VS
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Events as VE
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Fields as VF

/** Native item scopes are private implementation details, never modifier scopes in the contract. */
private data class WideParent(
    val navigationBar: RowScope? = null,
    val selectedTab: Boolean = false,
    val singleSegments: SingleChoiceSegmentedButtonRowScope? = null,
    val multiSegments: MultiChoiceSegmentedButtonRowScope? = null,
    val index: Int = 0,
    val count: Int = 1,
)
private val LocalWideParent = staticCompositionLocalOf { WideParent() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WideComponents(
    node: UiNode,
    modifier: Modifier,
    commands: NodeCommandRegistry,
    child: (Int) -> UiNode,
    renderChild: @Composable (Int) -> Unit,
    emit: (String, Int, Bundle) -> Unit,
) {
    val currentNode by rememberUpdatedState(node)
    val currentEmit by rememberUpdatedState(emit)
    val publish: (String, Bundle) -> Unit = { type, payload ->
        currentNode.callbacks[type]?.let { currentEmit(type, it, payload) }
    }
    val click = {
        ModifierMapper.clickCallback(currentNode)?.let { currentEmit(E.CLICK, it, Bundle()) }
        Unit
    }
    val content: @Composable () -> Unit = {
        (node.slots[S.CONTENT]?.let(::listOf) ?: node.children).forEach { renderChild(it) }
    }
    val enabled = ModifierMapper.interactionEnabled(node)
    val selected = node.wideBoolean(V.SELECTED)
    when (node.type) {
        C.NAVIGATION_BAR -> NavigationBar(
            modifier = modifier,
            containerColor = ValueMapper.color(node.props[P.CONTAINER_COLOR], NavigationBarDefaults.containerColor),
            contentColor = ValueMapper.color(node.props[P.CONTENT_COLOR], LocalContentColor.current),
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            CompositionLocalProvider(LocalWideParent provides WideParent(navigationBar = this)) { content() }
        }
        C.NAVIGATION_BAR_ITEM -> with(LocalWideParent.current.navigationBar ?: invalidWideScope(node)) {
            NavigationBarItem(selected, click, icon = { renderChild(node.slots.getValue(S.ICON)) },
                modifier = modifier, enabled = enabled,
                label = wideSlot(node, S.LABEL, renderChild), alwaysShowLabel = node.wideBoolean(V.ALWAYS_SHOW_LABEL, true))
        }
        C.NAVIGATION_RAIL -> NavigationRail(
            modifier = modifier,
            containerColor = ValueMapper.color(node.props[P.CONTAINER_COLOR], NavigationRailDefaults.ContainerColor),
            contentColor = ValueMapper.color(node.props[P.CONTENT_COLOR], LocalContentColor.current),
            header = node.slots[VS.HEADER]?.let { id -> @Composable { renderChild(id) } },
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) { content() }
        C.NAVIGATION_RAIL_ITEM -> NavigationRailItem(selected, click,
            icon = { renderChild(node.slots.getValue(S.ICON)) }, modifier = modifier, enabled = enabled,
            label = wideSlot(node, S.LABEL, renderChild), alwaysShowLabel = node.wideBoolean(V.ALWAYS_SHOW_LABEL, true))
        C.NAVIGATION_DRAWER -> WideNavigationDrawer(node, modifier, renderChild, content, publish)
        C.NAVIGATION_DRAWER_ITEM -> if (enabled) NavigationDrawerItem(
            label = { renderChild(node.slots.getValue(S.LABEL)) }, selected = selected,
            onClick = click, modifier = modifier, icon = wideSlot(node, S.ICON, renderChild),
            badge = wideSlot(node, VS.BADGE, renderChild),
        ) else DisabledDrawerItem(node, modifier, renderChild)
        C.TAB_ROW -> {
            val tabs: @Composable () -> Unit = {
                node.children.forEachIndexed { index, id ->
                    CompositionLocalProvider(LocalWideParent provides WideParent(selectedTab = index == node.wideInt(V.SELECTED_INDEX))) {
                        renderChild(id)
                    }
                }
            }
            val container = ValueMapper.color(node.props[P.CONTAINER_COLOR], TabRowDefaults.primaryContainerColor)
            val foreground = ValueMapper.color(node.props[P.CONTENT_COLOR], TabRowDefaults.primaryContentColor)
            if (node.wideBoolean(V.SCROLLABLE)) PrimaryScrollableTabRow(
                selectedTabIndex = node.wideInt(V.SELECTED_INDEX), modifier = modifier,
                containerColor = container, contentColor = foreground, tabs = tabs)
            else PrimaryTabRow(selectedTabIndex = node.wideInt(V.SELECTED_INDEX), modifier = modifier,
                containerColor = container, contentColor = foreground, tabs = tabs)
        }
        C.TAB -> Tab(selected = LocalWideParent.current.selectedTab, onClick = click,
            modifier = modifier, enabled = enabled, text = wideSlot(node, S.TEXT, renderChild),
            icon = wideSlot(node, S.ICON, renderChild))
        C.MODAL_BOTTOM_SHEET -> if (node.wideBoolean(P.OPEN)) WideModalBottomSheet(node, modifier, content, publish)
        C.DROPDOWN_MENU -> Box(modifier) {
            renderChild(node.slots.getValue(VS.ANCHOR))
            DropdownMenu(expanded = node.wideBoolean(P.OPEN), onDismissRequest = { publish(E.DISMISS_REQUEST, Bundle()) }) {
                node.children.forEach { renderChild(it) }
            }
        }
        C.DROPDOWN_MENU_ITEM -> DropdownMenuItem(text = { renderChild(node.slots.getValue(S.TEXT)) },
            onClick = click, modifier = modifier, enabled = enabled,
            leadingIcon = wideSlot(node, S.LEADING_ICON, renderChild), trailingIcon = wideSlot(node, S.TRAILING_ICON, renderChild))
        C.DATE_PICKER -> WideDatePicker(node, modifier, publish)
        C.TIME_PICKER -> WideTimePicker(node, modifier, publish)
        C.HORIZONTAL_PAGER, C.LAZY_VERTICAL_GRID -> WideLazyComponents(node, modifier, commands, child, renderChild, publish)
        C.ASSIST_CHIP -> AssistChip(onClick = click, label = { renderChild(node.slots.getValue(S.LABEL)) },
            modifier = modifier, enabled = enabled, leadingIcon = wideSlot(node, S.LEADING_ICON, renderChild),
            trailingIcon = wideSlot(node, S.TRAILING_ICON, renderChild))
        C.FILTER_CHIP, C.INPUT_CHIP -> {
            val select = { publish(VE.SELECTED_CHANGE, Bundle().apply { putBoolean(VF.SELECTED, !currentNode.wideBoolean(V.SELECTED)) }); click() }
            if (node.type == C.FILTER_CHIP) FilterChip(selected = selected, onClick = select,
                label = { renderChild(node.slots.getValue(S.LABEL)) }, modifier = modifier, enabled = enabled,
                leadingIcon = wideSlot(node, S.LEADING_ICON, renderChild), trailingIcon = wideSlot(node, S.TRAILING_ICON, renderChild))
            else InputChip(selected = selected, onClick = select,
                label = { renderChild(node.slots.getValue(S.LABEL)) }, modifier = modifier, enabled = enabled,
                leadingIcon = wideSlot(node, S.LEADING_ICON, renderChild), trailingIcon = wideSlot(node, S.TRAILING_ICON, renderChild))
        }
        C.BADGE -> {
            val badge: @Composable () -> Unit = {
                if (node.wideBoolean(V.VISIBLE, true)) Badge {
                    node.wideString(P.TEXT).takeIf { it.isNotEmpty() }?.let { Text(it) }
                }
            }
            if (node.children.isNotEmpty() || S.CONTENT in node.slots) BadgedBox(badge = { badge() }, modifier = modifier) { content() }
            else Box(modifier) { badge() }
        }
        C.SEGMENTED_BUTTON -> {
            if (node.wideEnum(V.MODE, "single") == "multi") MultiChoiceSegmentedButtonRow(modifier) {
                node.children.forEachIndexed { index, id ->
                    CompositionLocalProvider(LocalWideParent provides WideParent(multiSegments = this, index = index, count = node.children.size)) { renderChild(id) }
                }
            } else SingleChoiceSegmentedButtonRow(modifier) {
                node.children.forEachIndexed { index, id ->
                    CompositionLocalProvider(LocalWideParent provides WideParent(singleSegments = this, index = index, count = node.children.size)) { renderChild(id) }
                }
            }
        }
        C.SEGMENTED_BUTTON_ITEM -> {
            val parent = LocalWideParent.current
            val shape = SegmentedButtonDefaults.itemShape(parent.index, parent.count)
            val icon: @Composable () -> Unit = wideSlot(node, S.ICON, renderChild) ?: { SegmentedButtonDefaults.Icon(selected) }
            val change: (Boolean) -> Unit = { value -> publish(VE.SELECTED_CHANGE, Bundle().apply { putBoolean(VF.SELECTED, value) }); click() }
            if (parent.multiSegments != null) with(parent.multiSegments) {
                SegmentedButton(checked = selected, onCheckedChange = change, shape = shape, modifier = modifier,
                    enabled = enabled, icon = icon, label = { renderChild(node.slots.getValue(S.LABEL)) })
            } else with(parent.singleSegments ?: invalidWideScope(node)) {
                SegmentedButton(selected = selected, onClick = { change(true) }, shape = shape, modifier = modifier,
                    enabled = enabled, icon = icon, label = { renderChild(node.slots.getValue(S.LABEL)) })
            }
        }
        C.FLOATING_ACTION_BUTTON -> FloatingActionButton(onClick = click, modifier = modifier,
            shape = (node.props[P.SHAPE] as? UiValue.Shape)?.let(ValueMapper::shape) ?: FloatingActionButtonDefaults.shape,
            containerColor = ValueMapper.color(node.props[P.CONTAINER_COLOR], FloatingActionButtonDefaults.containerColor),
            contentColor = ValueMapper.color(node.props[P.CONTENT_COLOR], contentColorFor(
                ValueMapper.color(node.props[P.CONTAINER_COLOR], FloatingActionButtonDefaults.containerColor))), content = content)
        C.SEARCH_BAR -> {
            val expanded = node.wideBoolean(V.EXPANDED)
            val onExpanded: (Boolean) -> Unit = { publish(VE.EXPANDED_CHANGE, Bundle().apply { putBoolean(VF.EXPANDED, it) }) }
            val onText: (String, String) -> Unit = { type, text ->
                // Native paste/IME/semantics input has not passed the shared prop validator.
                // Reject before creating UiEvent, whose payload snapshot also enforces this bound.
                // The controlled String field keeps the accepted query when an edit is rejected.
                if (ModifierMapper.interactionEnabled(currentNode)) {
                    if (text.length > ComposeUiLimits.MAX_STRING_CHARS) {
                        currentEmit(E.ERROR, ComposeUiContract.SYSTEM_CALLBACK_ID, Bundle().apply {
                            putString(F.CODE, ComposeUiErrorCodes.LIMIT_EXCEEDED)
                            putString(F.MESSAGE, ComposeUiErrorCodes.LIMIT_EXCEEDED)
                            putString(F.PROP, V.QUERY)
                        })
                    } else if (type != VE.QUERY_CHANGE || text != currentNode.wideString(V.QUERY)) {
                        // The String field can echo its accepted value while restoring rejected
                        // input. That is no model change; a search submission still remains valid.
                        publish(type, Bundle().apply { putString(F.TEXT, text) })
                    }
                }
            }
            SearchBar(inputField = {
                SearchBarDefaults.InputField(query = node.wideString(V.QUERY),
                    onQueryChange = { onText(VE.QUERY_CHANGE, it) },
                    onSearch = { onText(VE.SEARCH, it) },
                    expanded = expanded, onExpandedChange = onExpanded, enabled = enabled,
                    placeholder = wideSlot(node, S.PLACEHOLDER, renderChild),
                    leadingIcon = wideSlot(node, S.LEADING_ICON, renderChild), trailingIcon = wideSlot(node, S.TRAILING_ICON, renderChild))
            }, expanded = expanded, onExpandedChange = onExpanded, modifier = modifier,
                windowInsets = WindowInsets(0, 0, 0, 0)) { content() }
        }
        C.TOOLTIP -> WideTooltip(node, modifier, renderChild, content, publish)
        C.PULL_TO_REFRESH -> {
            val refreshing = node.wideBoolean(V.REFRESHING)
            val state = rememberPullToRefreshState()
            Box(modifier.pullToRefresh(isRefreshing = refreshing, state = state, enabled = enabled,
                onRefresh = { publish(VE.REFRESH, Bundle()) })) {
                content()
                PullToRefreshDefaults.Indicator(state = state, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter))
            }
        }
        else -> throw ComposeUiContractException(ComposeUiErrorCodes.UNKNOWN_COMPONENT, nodeId = node.nodeId)
    }
}

/** M3 1.4.0 has no enabled parameter on NavigationDrawerItem. Use its public Surface layout. */
@Composable private fun DisabledDrawerItem(node: UiNode, modifier: Modifier, renderChild: @Composable (Int) -> Unit) {
    val selected = node.wideBoolean(V.SELECTED)
    Surface(selected = selected, onClick = {}, enabled = false,
        modifier = modifier.heightIn(min = 56.dp).fillMaxWidth().alpha(0.38f).semantics { role = Role.Tab },
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
        Row(Modifier.padding(start = 16.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            node.slots[S.ICON]?.let { renderChild(it); Spacer(Modifier.width(12.dp)) }
            Box(Modifier.weight(1f)) { renderChild(node.slots.getValue(S.LABEL)) }
            node.slots[VS.BADGE]?.let { Spacer(Modifier.width(12.dp)); renderChild(it) }
        }
    }
}

internal fun UiNode.wideBoolean(name: String, default: Boolean = false) = (props[name] as? UiValue.Bool)?.value ?: default
internal fun UiNode.wideInt(name: String, default: Int = 0) = (props[name] as? UiValue.Num)?.value?.toInt() ?: default
internal fun UiNode.wideEnum(name: String, default: String) = (props[name] as? UiValue.Enum)?.value ?: default
internal fun UiNode.wideString(name: String) = (props[name] as? UiValue.Str)?.value.orEmpty()
internal fun UiNode.wideDp(name: String) = ((props[name] as? UiValue.Dp)?.value?.toFloat() ?: 0f).dp
internal fun wideSlot(node: UiNode, name: String, render: @Composable (Int) -> Unit): (@Composable () -> Unit)? =
    node.slots[name]?.let { id -> @Composable { render(id) } }
private fun invalidWideScope(node: UiNode): Nothing = throw ComposeUiContractException(ComposeUiErrorCodes.SCOPE_MISMATCH, nodeId = node.nodeId)
