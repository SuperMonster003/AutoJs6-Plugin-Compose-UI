package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.autojs.plugin.compose.api.ComposeUiSlots
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import kotlin.math.roundToInt

private val SCRIM = Color.Black.copy(alpha = 0.32f)

/**
 * Draws an open window component over the page inside the canvas (roadmap P9.1), with Material 3 chrome close
 * to the real window (dialog panel, bottom sheet, menu, plain tooltip) and its slots and children rendered by
 * the real renderer through [render]. Nothing here opens a window. [anchor] is the owner's in-place bounds in
 * canvas coordinates (a menu's or tooltip's anchor); [onPopup] receives the drawn popup's bounds in root
 * coordinates, so taps on it select inside the owner and its selection outline follows the popup.
 */
@Composable
internal fun DesignOverlay(overlay: Overlay, anchor: Rect?, onPopup: (Rect) -> Unit, render: @Composable (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val popup = Modifier.onGloballyPositioned { onPopup(it.boundsInRoot()) }
    when (overlay.kind) {
        OverlayKind.DIALOG -> Box(Modifier.fillMaxSize().background(SCRIM).testTag("designer-overlay-dialog"), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.padding(24.dp).then(popup).widthIn(min = 280.dp, max = 560.dp),
                shape = RoundedCornerShape(28.dp),
                color = colors.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.padding(24.dp)) {
                    overlay.slots[ComposeUiSlots.ICON]?.let { icon ->
                        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)) {
                            CompositionLocalProvider(LocalContentColor provides colors.secondary) { render(icon) }
                        }
                    }
                    overlay.slots[ComposeUiSlots.TITLE]?.let { title ->
                        Box(Modifier.padding(bottom = 16.dp)) { ProvideTextStyle(MaterialTheme.typography.headlineSmall) { render(title) } }
                    }
                    overlay.slots[ComposeUiSlots.TEXT]?.let { text ->
                        Box(Modifier.padding(bottom = 24.dp)) {
                            CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
                                ProvideTextStyle(MaterialTheme.typography.bodyMedium) { render(text) }
                            }
                        }
                    }
                    Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        overlay.slots[ComposeUiSlots.DISMISS]?.let { render(it) }
                        overlay.slots[ComposeUiSlots.CONFIRM]?.let { render(it) }
                    }
                }
            }
        }
        OverlayKind.SHEET -> Box(Modifier.fillMaxSize().background(SCRIM).testTag("designer-overlay-sheet"), contentAlignment = Alignment.BottomCenter) {
            Surface(
                modifier = popup.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = colors.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 22.dp)
                            .size(32.dp, 4.dp)
                            .background(colors.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
                    )
                    overlay.children.forEach { render(it) }
                }
            }
        }
        OverlayKind.MENU -> Box(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.placeNear(anchor, preferAbove = false).then(popup).widthIn(min = 112.dp, max = 280.dp).testTag("designer-overlay-menu"),
                shape = RoundedCornerShape(4.dp),
                color = colors.surfaceContainer,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
            ) {
                Column(Modifier.padding(vertical = 8.dp)) { overlay.children.forEach { render(it) } }
            }
        }
        OverlayKind.TOOLTIP -> Box(Modifier.fillMaxSize()) {
            overlay.slots[ComposeUiV2.Slots.TOOLTIP]?.let { tooltip ->
                Surface(
                    modifier = Modifier.placeNear(anchor, preferAbove = true).then(popup).testTag("designer-overlay-tooltip"),
                    shape = RoundedCornerShape(4.dp),
                    color = colors.inverseSurface,
                ) {
                    Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        CompositionLocalProvider(LocalContentColor provides colors.inverseOnSurface) {
                            ProvideTextStyle(MaterialTheme.typography.bodySmall) { render(tooltip) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Places a popup at the start edge of [anchor], below it or (for a tooltip) above it, and on the other side
 * when only that side has room, as the real popups do; it is kept inside the canvas, and without an anchor it
 * sits at the top start corner. The layout takes the whole canvas so that modifiers after this one see the
 * popup itself at its final position.
 */
private fun Modifier.placeNear(anchor: Rect?, preferAbove: Boolean): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
    val gap = 4.dp.roundToPx()
    layout(constraints.maxWidth, constraints.maxHeight) {
        val x = (anchor?.left?.roundToInt() ?: 0).coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
        val y = if (anchor == null) 0 else {
            val above = anchor.top.roundToInt() - placeable.height - gap
            val below = anchor.bottom.roundToInt() + gap
            val fitsAbove = above >= 0
            val fitsBelow = below + placeable.height <= constraints.maxHeight
            if (preferAbove && (fitsAbove || !fitsBelow) || !preferAbove && !fitsBelow && fitsAbove) above else below
        }
        placeable.place(x, y.coerceIn(0, (constraints.maxHeight - placeable.height).coerceAtLeast(0)))
    }
}
