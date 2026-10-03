package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.compose.ui.BuildConfig
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiEventFields as F
import org.autojs.plugin.compose.api.ComposeUiContract
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.ComposeUiSlots as S
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue

internal data class RenderFrame(val rootId: Int, val nodes: Map<Int, UiNode>, val generation: Long)

/** All handlers use the same frame generation, including callbacks retained by Compose. */
@Composable
internal fun RenderNode(
    frame: RenderFrame,
    id: Int,
    commands: NodeCommandRegistry,
    fields: TextFieldController,
    snackbar: SnackbarController,
    emit: (generation: Long, nodeId: Int, type: String, callbackId: Int, payload: Bundle) -> Unit,
    rowScope: RowScope? = null,
    columnScope: ColumnScope? = null,
    boxScope: BoxScope? = null,
) {
    val node = frame.nodes.getValue(id)
    key(id, node.type) {
        if (RendererCatalog.dispatch[node.type] == null) {
            if (BuildConfig.DEBUG) Text("<unknown: ${node.type}>")
        } else {
            val publish: (String, Int, Bundle) -> Unit = { type, callback, payload -> emit(frame.generation, id, type, callback, payload) }
            val modifier = with(ModifierMapper) { Modifier.map(node, commands, rowScope, columnScope, boxScope, publish) }
            val children = node.slots[S.CONTENT]?.let(::listOf) ?: node.children
            val content: @Composable () -> Unit = { children.forEach { RenderNode(frame, it, commands, fields, snackbar, emit) } }
            val onClick = { ModifierMapper.clickCallback(node)?.let { publish(E.CLICK, it, Bundle()) }; Unit }
            val enabled = ModifierMapper.interactionEnabled(node)
            when (RendererCatalog.dispatch[node.type]) {
                RenderKind.COLUMN -> Column(modifier, verticalArrangement = verticalArrangement(node), horizontalAlignment = horizontalAlignment(node.enum(P.ALIGNMENT, "start"))) {
                    children.forEach { RenderNode(frame, it, commands, fields, snackbar, emit, columnScope = this) }
                }
                RenderKind.ROW -> Row(modifier, horizontalArrangement = horizontalArrangement(node), verticalAlignment = verticalAlignment(node.enum(P.ALIGNMENT, "top"))) {
                    children.forEach { RenderNode(frame, it, commands, fields, snackbar, emit, rowScope = this) }
                }
                RenderKind.BOX -> Box(modifier, contentAlignment = boxAlignment(node.enum(P.ALIGNMENT, "topStart"))) {
                    children.forEach { RenderNode(frame, it, commands, fields, snackbar, emit, boxScope = this) }
                }
                RenderKind.SPACER -> Spacer(modifier)
                RenderKind.SURFACE -> {
                    val color = ValueMapper.color(node.props[P.COLOR], MaterialTheme.colorScheme.surface)
                    Surface(modifier, shape = node.shape() ?: RectangleShape, color = color,
                        contentColor = ValueMapper.color(node.props[P.CONTENT_COLOR], contentColorFor(color)),
                        tonalElevation = node.dp(P.TONAL_ELEVATION), content = content)
                }
                RenderKind.CARD -> Card(modifier, shape = node.shape() ?: CardDefaults.shape,
                    colors = CardDefaults.cardColors(containerColor = ValueMapper.color(node.props[P.COLOR]), contentColor = ValueMapper.color(node.props[P.CONTENT_COLOR])),
                    elevation = if (P.ELEVATION in node.props) CardDefaults.cardElevation(defaultElevation = node.dp(P.ELEVATION)) else CardDefaults.cardElevation()) { content() }
                RenderKind.DIVIDER -> HorizontalDivider(modifier, thickness = node.dp(P.THICKNESS, 1.dp), color = ValueMapper.color(node.props[P.COLOR], MaterialTheme.colorScheme.outlineVariant))
                RenderKind.TEXT -> {
                    val renderText: @Composable () -> Unit = {
                        Text(text = (node.props[P.TEXT] as? UiValue.Str)?.value.orEmpty(), modifier = modifier,
                            style = ValueMapper.textStyle(node.props[P.STYLE], MaterialTheme.typography, LocalTextStyle.current),
                            color = ValueMapper.color(node.props[P.COLOR]),
                            fontSize = (node.props[P.FONT_SIZE] as? UiValue.Sp)?.value?.toFloat()?.sp ?: TextUnit.Unspecified,
                            fontWeight = (node.props[P.FONT_WEIGHT] as? UiValue.Num)?.value?.toInt()?.let(::FontWeight),
                            textAlign = textAlignment(node.enum(P.TEXT_ALIGN, "start")),
                            maxLines = (node.props[P.MAX_LINES] as? UiValue.Num)?.value?.toInt() ?: Int.MAX_VALUE,
                            overflow = when (node.enum(P.OVERFLOW, "clip")) { "ellipsis" -> TextOverflow.Ellipsis; "visible" -> TextOverflow.Visible; else -> TextOverflow.Clip })
                    }
                    if (node.bool(P.SELECTABLE)) SelectionContainer(content = renderText) else renderText()
                }
                RenderKind.ICON -> Icon(IconCatalog.vector((node.props.getValue(P.NAME) as UiValue.IconName).value), null,
                    modifier, tint = ValueMapper.color(node.props[P.TINT], LocalContentColor.current))
                RenderKind.IMAGE -> {
                    // BitmapRef is borrowed: do not keep another strong cache or recycle the caller's bitmap.
                    val source = node.props.getValue(P.SRC) as UiValue.BitmapRef
                    val currentPublish by rememberUpdatedState(publish)
                    val painter = remember(source) { BorrowedBitmapPainter(source) {
                        currentPublish(E.ERROR, ComposeUiContract.SYSTEM_CALLBACK_ID, Bundle().apply {
                            putString(F.CODE, ComposeUiErrorCodes.RENDER_FAILED); putString(F.MESSAGE, ComposeUiErrorCodes.RENDER_FAILED)
                        })
                    } }
                    Image(painter, null, modifier, contentScale = contentScale(node.enum(P.CONTENT_SCALE, "fit")))
                }
                RenderKind.BUTTON -> Button(onClick, modifier, enabled, shape = node.shape() ?: ButtonDefaults.shape,
                    colors = ButtonDefaults.buttonColors(ValueMapper.color(node.props[P.CONTAINER_COLOR]), ValueMapper.color(node.props[P.CONTENT_COLOR]))) { content() }
                RenderKind.ELEVATED_BUTTON -> ElevatedButton(onClick, modifier, enabled, shape = node.shape() ?: ButtonDefaults.elevatedShape,
                    colors = ButtonDefaults.elevatedButtonColors(ValueMapper.color(node.props[P.CONTAINER_COLOR]), ValueMapper.color(node.props[P.CONTENT_COLOR]))) { content() }
                RenderKind.TONAL_BUTTON -> FilledTonalButton(onClick, modifier, enabled, shape = node.shape() ?: ButtonDefaults.filledTonalShape,
                    colors = ButtonDefaults.filledTonalButtonColors(ValueMapper.color(node.props[P.CONTAINER_COLOR]), ValueMapper.color(node.props[P.CONTENT_COLOR]))) { content() }
                RenderKind.OUTLINED_BUTTON -> OutlinedButton(onClick, modifier, enabled, shape = node.shape() ?: ButtonDefaults.outlinedShape,
                    colors = ButtonDefaults.outlinedButtonColors(ValueMapper.color(node.props[P.CONTAINER_COLOR]), ValueMapper.color(node.props[P.CONTENT_COLOR]))) { content() }
                RenderKind.TEXT_BUTTON -> TextButton(onClick, modifier, enabled, shape = node.shape() ?: ButtonDefaults.textShape,
                    colors = ButtonDefaults.textButtonColors(ValueMapper.color(node.props[P.CONTAINER_COLOR]), ValueMapper.color(node.props[P.CONTENT_COLOR]))) { content() }
                RenderKind.ICON_BUTTON -> IconButton(onClick, modifier, enabled,
                    colors = IconButtonDefaults.iconButtonColors(ValueMapper.color(node.props[P.CONTAINER_COLOR]), ValueMapper.color(node.props[P.CONTENT_COLOR])),
                    shape = node.shape() ?: IconButtonDefaults.standardShape) { content() }
                RenderKind.SWITCH -> Switch(node.bool(P.CHECKED), { checked -> checkedChange(node, checked, publish); onClick() }, modifier, enabled = enabled)
                RenderKind.CHECKBOX -> Checkbox(node.bool(P.CHECKED), { checked -> checkedChange(node, checked, publish); onClick() }, modifier, enabled = enabled)
                RenderKind.RADIO_BUTTON -> RadioButton(node.bool(P.CHECKED), { checkedChange(node, true, publish); onClick() }, modifier, enabled = enabled)
                RenderKind.SLIDER -> {
                    val range = node.props[P.RANGE] as? UiValue.ListOf
                    val start = (range?.values?.get(0) as? UiValue.Num)?.value?.toFloat() ?: 0f
                    val end = (range?.values?.get(1) as? UiValue.Num)?.value?.toFloat() ?: 1f
                    Slider(value = (node.props[P.VALUE] as? UiValue.Num)?.value?.toFloat() ?: 0f,
                        onValueChange = { value -> node.callbacks[E.VALUE_CHANGE]?.let { publish(E.VALUE_CHANGE, it, Bundle().apply { putDouble(F.VALUE, value.toDouble()) }) } },
                        modifier = modifier, enabled = enabled, valueRange = start..end,
                        steps = (node.props[P.STEPS] as? UiValue.Num)?.value?.toInt() ?: 0,
                        onValueChangeFinished = { node.callbacks[E.VALUE_CHANGE_FINISHED]?.let { publish(E.VALUE_CHANGE_FINISHED, it, Bundle()) } })
                }
                RenderKind.TEXT_FIELD, RenderKind.OUTLINED_TEXT_FIELD -> RenderTextField(
                    node, fields.editor(id), fields.inputTransformation(id), modifier,
                ) { name -> node.slots[name]?.let { RenderNode(frame, it, commands, fields, snackbar, emit) } }
                RenderKind.LAZY_COLUMN, RenderKind.LAZY_ROW, RenderKind.SCAFFOLD, RenderKind.TOP_APP_BAR,
                RenderKind.ALERT_DIALOG, RenderKind.CIRCULAR_PROGRESS, RenderKind.LINEAR_PROGRESS -> AdvancedComponents(
                    node, modifier, commands, snackbar,
                    childKey = { child -> frame.nodes.getValue(child).key ?: child },
                    childType = { child -> frame.nodes.getValue(child).type },
                    renderChild = { child -> RenderNode(frame, child, commands, fields, snackbar, emit) },
                    emit = publish,
                )
                null -> if (BuildConfig.DEBUG) Text("<unknown: ${node.type}>", modifier)
            }
        }
    }
}

private fun checkedChange(node: UiNode, checked: Boolean, publish: (String, Int, Bundle) -> Unit) {
    node.callbacks[E.CHECKED_CHANGE]?.let { publish(E.CHECKED_CHANGE, it, Bundle().apply { putBoolean(F.CHECKED, checked) }) }
}
private fun UiNode.bool(name: String) = (props[name] as? UiValue.Bool)?.value ?: false
private fun UiNode.enum(name: String, fallback: String) = (props[name] as? UiValue.Enum)?.value ?: fallback
private fun UiNode.dp(name: String, fallback: Dp = 0.dp) = (props[name] as? UiValue.Dp)?.value?.toFloat()?.dp ?: fallback
private fun UiNode.shape() = (props[P.SHAPE] as? UiValue.Shape)?.let(ValueMapper::shape)
internal fun horizontalAlignment(name: String) = when (name) { "center" -> Alignment.CenterHorizontally; "end" -> Alignment.End; else -> Alignment.Start }
internal fun verticalAlignment(name: String) = when (name) { "center" -> Alignment.CenterVertically; "bottom" -> Alignment.Bottom; else -> Alignment.Top }
internal fun verticalArrangement(node: UiNode): Arrangement.Vertical {
    val name = node.enum(P.ARRANGEMENT, "top")
    val base = when (name) { "center" -> Arrangement.Center; "bottom" -> Arrangement.Bottom; "spaceBetween" -> Arrangement.SpaceBetween; "spaceAround" -> Arrangement.SpaceAround; "spaceEvenly" -> Arrangement.SpaceEvenly; else -> Arrangement.Top }
    val gap = node.dp(P.SPACING)
    if (gap == 0.dp) return base
    return object : Arrangement.Vertical {
        override val spacing = gap
        override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
            val pixels = gap.roundToPx()
            val available = (totalSize.toLong() - pixels.toLong() * (sizes.size - 1).coerceAtLeast(0)).coerceAtLeast(0).toInt()
            with(base) { arrange(available, sizes, outPositions) }
            sizes.indices.forEach { outPositions[it] = (outPositions[it].toLong() + it.toLong() * pixels).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }
        }
    }
}
internal fun horizontalArrangement(node: UiNode): Arrangement.Horizontal {
    val name = node.enum(P.ARRANGEMENT, "start")
    val base = when (name) { "center" -> Arrangement.Center; "end" -> Arrangement.End; "spaceBetween" -> Arrangement.SpaceBetween; "spaceAround" -> Arrangement.SpaceAround; "spaceEvenly" -> Arrangement.SpaceEvenly; else -> Arrangement.Start }
    val gap = node.dp(P.SPACING)
    if (gap == 0.dp) return base
    return object : Arrangement.Horizontal {
        override val spacing = gap
        override fun Density.arrange(totalSize: Int, sizes: IntArray, layoutDirection: LayoutDirection, outPositions: IntArray) {
            val pixels = gap.roundToPx()
            val available = (totalSize.toLong() - pixels.toLong() * (sizes.size - 1).coerceAtLeast(0)).coerceAtLeast(0).toInt()
            with(base) { arrange(available, sizes, layoutDirection, outPositions) }
            sizes.indices.forEach { index ->
                val offset = if (layoutDirection == LayoutDirection.Ltr) index else sizes.lastIndex - index
                outPositions[index] = (outPositions[index].toLong() + offset.toLong() * pixels).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
        }
    }
}
private fun boxAlignment(name: String): Alignment = when (name) {
    "topCenter" -> Alignment.TopCenter; "topEnd" -> Alignment.TopEnd; "centerStart" -> Alignment.CenterStart; "center" -> Alignment.Center
    "centerEnd" -> Alignment.CenterEnd; "bottomStart" -> Alignment.BottomStart; "bottomCenter" -> Alignment.BottomCenter; "bottomEnd" -> Alignment.BottomEnd; else -> Alignment.TopStart
}
private fun textAlignment(name: String): TextAlign = when (name) { "end" -> TextAlign.End; "left" -> TextAlign.Left; "right" -> TextAlign.Right; "center" -> TextAlign.Center; "justify" -> TextAlign.Justify; else -> TextAlign.Start }
private fun contentScale(name: String): ContentScale = when (name) { "crop" -> ContentScale.Crop; "fillBounds" -> ContentScale.FillBounds; "fillWidth" -> ContentScale.FillWidth; "fillHeight" -> ContentScale.FillHeight; "inside" -> ContentScale.Inside; "none" -> ContentScale.None; else -> ContentScale.Fit }
