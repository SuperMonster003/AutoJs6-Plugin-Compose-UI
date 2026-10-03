package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.view.KeyEvent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.inputText
import androidx.compose.ui.semantics.password
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.ComposeUiSlots as S
import org.autojs.plugin.compose.api.model.UiNode
import org.autojs.plugin.compose.api.model.UiValue

/** State overloads keep IME composition in native state; a script acknowledgement is never required. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RenderTextField(
    node: UiNode,
    state: TextFieldState,
    inputTransformation: InputTransformation,
    modifier: Modifier,
    slot: @Composable (String) -> Unit,
) {
    val interactions = remember { TextFieldInteractionSource() }
    val password = node.enum(P.VISUAL_TRANSFORMATION, "none") == "password"
    val keyboard = keyboardType(node.enum(P.KEYBOARD_TYPE, "text"))
    val options = KeyboardOptions(
        keyboardType = keyboard,
        imeAction = imeAction(node.enum(P.IME_ACTION, "default")),
        autoCorrectEnabled = if (password || keyboard == KeyboardType.Password || keyboard == KeyboardType.NumberPassword) false else null,
    )
    val limits = if (node.bool(P.SINGLE_LINE)) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(
        minHeightInLines = node.number(P.MIN_LINES, 1), maxHeightInLines = node.number(P.MAX_LINES, Int.MAX_VALUE),
    )
    val label: (@Composable TextFieldLabelScope.() -> Unit)? = if (S.LABEL in node.slots) ({ slot(S.LABEL) }) else null
    val placeholder: (@Composable () -> Unit)? = if (S.PLACEHOLDER in node.slots) ({ slot(S.PLACEHOLDER) }) else null
    val leading: (@Composable () -> Unit)? = if (S.LEADING_ICON in node.slots) ({ slot(S.LEADING_ICON) }) else null
    val trailing: (@Composable () -> Unit)? = if (S.TRAILING_ICON in node.slots) ({ slot(S.TRAILING_ICON) }) else null
    val supporting: (@Composable () -> Unit)? = if (S.SUPPORTING_TEXT in node.slots) ({ slot(S.SUPPORTING_TEXT) }) else null
    val input = remember(inputTransformation, state, password) {
        if (password) inputTransformation.then(PasswordSemantics(state)) else inputTransformation
    }
    val fieldModifier = if (password) modifier.passwordInteractions() else modifier
    val style = ValueMapper.textStyle(node.props[P.STYLE], MaterialTheme.typography, LocalTextStyle.current)
    if (node.type == C.OUTLINED_TEXT_FIELD) {
        OutlinedTextField(
            state = state, modifier = fieldModifier, enabled = node.bool(P.ENABLED, true), readOnly = node.bool(P.READ_ONLY),
            textStyle = style, label = label, placeholder = placeholder, leadingIcon = leading, trailingIcon = trailing,
            supportingText = supporting, isError = node.bool(P.IS_ERROR), inputTransformation = input,
            outputTransformation = if (password) PasswordOutput else null, keyboardOptions = options, lineLimits = limits,
            interactionSource = interactions,
        )
    } else {
        TextField(
            state = state, modifier = fieldModifier, enabled = node.bool(P.ENABLED, true), readOnly = node.bool(P.READ_ONLY),
            textStyle = style, label = label, placeholder = placeholder, leadingIcon = leading, trailingIcon = trailing,
            supportingText = supporting, isError = node.bool(P.IS_ERROR), inputTransformation = input,
            outputTransformation = if (password) PasswordOutput else null, keyboardOptions = options, lineLimits = limits,
            interactionSource = interactions,
        )
    }
}

/**
 * Material3's secure overload has no readOnly or multiline parameter. This always-hidden visual
 * transformation preserves those V1 properties. Replace each UTF-16 unit independently so output
 * offset mapping remains one-to-one, including reversed selections and supplementary characters.
 */
internal object PasswordOutput : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        for (index in 0 until length) replace(index, index + 1, "\u2022")
    }
}

private class PasswordSemantics(private val state: TextFieldState) : InputTransformation {
    override fun TextFieldBuffer.transformInput() = Unit
    override fun SemanticsPropertyReceiver.applySemantics() {
        password()
        // BasicTextField otherwise publishes untransformed text through this separate property.
        inputText = AnnotatedString("\u2022".repeat(state.text.length))
        copyText { false }
        cutText { false }
    }
}

private fun Modifier.passwordInteractions(): Modifier =
    filterTextContextMenuComponents {
        it.key === TextContextMenuKeys.PasteKey || it.key === TextContextMenuKeys.SelectAllKey || it.key === TextContextMenuKeys.AutofillKey
    }.onPreviewKeyEvent { event ->
        val key = event.nativeKeyEvent
        key.keyCode == KeyEvent.KEYCODE_COPY || key.keyCode == KeyEvent.KEYCODE_CUT ||
            key.isCtrlPressed && (key.keyCode == KeyEvent.KEYCODE_C || key.keyCode == KeyEvent.KEYCODE_X || key.keyCode == KeyEvent.KEYCODE_INSERT) ||
            key.isShiftPressed && key.keyCode == KeyEvent.KEYCODE_FORWARD_DEL
    }

private fun UiNode.bool(name: String, default: Boolean = false) = (props[name] as? UiValue.Bool)?.value ?: default
private fun UiNode.enum(name: String, default: String) = (props[name] as? UiValue.Enum)?.value ?: default
private fun UiNode.number(name: String, default: Int) = (props[name] as? UiValue.Num)?.value?.toInt() ?: default
private fun keyboardType(name: String): KeyboardType = when (name) {
    "ascii" -> KeyboardType.Ascii; "number" -> KeyboardType.Number; "phone" -> KeyboardType.Phone
    "uri" -> KeyboardType.Uri; "email" -> KeyboardType.Email; "password" -> KeyboardType.Password
    "numberPassword" -> KeyboardType.NumberPassword; "decimal" -> KeyboardType.Decimal; else -> KeyboardType.Text
}
private fun imeAction(name: String): ImeAction = when (name) {
    "none" -> ImeAction.None; "go" -> ImeAction.Go; "search" -> ImeAction.Search; "send" -> ImeAction.Send
    "previous" -> ImeAction.Previous; "next" -> ImeAction.Next; "done" -> ImeAction.Done; else -> ImeAction.Default
}
