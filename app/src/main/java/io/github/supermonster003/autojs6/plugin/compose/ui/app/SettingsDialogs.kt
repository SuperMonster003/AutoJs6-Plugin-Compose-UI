package io.github.supermonster003.autojs6.plugin.compose.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import io.github.supermonster003.autojs6.plugin.compose.ui.R

internal data class SettingsChoice(val label: String, val note: String? = null)

/**
 * Centered single-choice dialog of the standalone settings specification: the saved value is copied to a
 * draft, a row click only changes the draft, confirm persists once, and cancel, back or an outside click
 * leave everything unchanged.
 */
@Composable
internal fun ConfirmedChoiceDialog(
    title: String,
    options: List<SettingsChoice>,
    selected: Int,
    palette: StandalonePalette,
    tag: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var draft by remember(selected) { mutableIntStateOf(selected) }
    SettingsDialog(title, palette, tag, onDismiss, confirmEnabled = true, onConfirm = { onConfirm(draft) }) {
        options.forEachIndexed { index, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = if (option.note == null) 56.dp else 72.dp)
                    .selectable(selected = draft == index, role = Role.RadioButton) { draft = index }
                    .padding(horizontal = 8.dp, vertical = 8.dp).testTag("$tag-option-$index"),
            ) {
                RadioButton(selected = draft == index, onClick = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(option.label, color = Color(palette.text), fontSize = 16.sp)
                    option.note?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = Color(palette.muted), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/** Follow AutoJs6, a preset swatch or a HEX / RGB input; the preview only changes inside the dialog. */
@Composable
internal fun ThemeColorDialog(
    current: Int?,
    hostColor: Int,
    palette: StandalonePalette,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
) {
    var followHost by remember(current) { mutableStateOf(current == null) }
    var input by remember(current) { mutableStateOf(current?.let(ThemeColorValue::hex) ?: "") }
    val parsed = ThemeColorValue.parse(input)
    val invalid = !followHost && parsed == null
    val previewSeed = if (followHost) hostColor else parsed ?: hostColor
    val roles = ThemeAccentRoles.fromSeed(previewSeed, palette.isDark)
    SettingsDialog(
        stringResource(R.string.app_settings_theme_color), palette, "dialog-color", onDismiss,
        confirmEnabled = !invalid, onConfirm = { onConfirm(if (followHost) null else parsed) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                .selectable(selected = followHost, role = Role.RadioButton) { followHost = true }
                .padding(horizontal = 8.dp).testTag("dialog-color-follow"),
        ) {
            RadioButton(selected = followHost, onClick = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.app_settings_follow_autojs6), color = Color(palette.text), fontSize = 16.sp)
        }
        Text(stringResource(R.string.theme_picker_presets), color = Color(palette.muted), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
        ThemeColorValue.presets.chunked(4).forEach { rowColors ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rowColors.forEach { color ->
                    val chosen = !followHost && parsed == color
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp).padding(4.dp).background(Color(color), CircleShape)
                            .border(if (chosen) 2.dp else 1.dp, Color(if (chosen) palette.text else palette.outline), CircleShape)
                            .selectable(selected = chosen, role = Role.RadioButton) { followHost = false; input = ThemeColorValue.hex(color) }
                            .testTag("dialog-color-preset-${ThemeColorValue.hex(color)}"),
                    ) {
                        if (chosen) Icon(Icons.Filled.Check, contentDescription = null, tint = Color(StandaloneColorPolicy.onFilledColor(color)))
                    }
                }
            }
        }
        Text(stringResource(R.string.theme_picker_custom), color = Color(palette.muted), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; followHost = false },
            singleLine = true,
            isError = invalid && input.isNotBlank(),
            label = { Text(stringResource(R.string.theme_picker_input)) },
            supportingText = { if (invalid && input.isNotBlank()) Text(stringResource(R.string.theme_picker_invalid), color = Color(palette.danger)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).testTag("dialog-color-input"),
        )
        Text(stringResource(R.string.theme_picker_preview), color = Color(palette.muted), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Box(modifier = Modifier.size(32.dp).background(Color(previewSeed), CircleShape).border(1.dp, Color(palette.outline), CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(ThemeColorValue.hex(previewSeed) + "  " + ThemeColorValue.rgb(previewSeed), color = Color(palette.text), fontSize = 14.sp, modifier = Modifier.testTag("dialog-color-preview"))
            Spacer(Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(32.dp).background(Color(roles.primary), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp),
            ) { Text("Aa", color = Color(roles.onPrimary), fontSize = 14.sp) }
        }
    }
}

@Composable
private fun SettingsDialog(
    title: String,
    palette: StandalonePalette,
    tag: String,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    // About 85% of the current window (specification section 8); the window, not the screen, is measured.
    val maxHeight = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * 0.85f).toDp() }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().wrapContentHeight().heightIn(max = maxHeight).testTag(tag),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(palette.surface),
        titleContentColor = Color(palette.text),
        textContentColor = Color(palette.text),
        title = { Text(title, fontSize = 20.sp) },
        text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { content() } },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.testTag("$tag-confirm")) {
                Text(stringResource(R.string.action_confirm), color = Color(if (confirmEnabled) palette.accent else palette.muted))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("$tag-cancel")) {
                Text(stringResource(R.string.action_cancel), color = Color(palette.accent))
            }
        },
    )
}
