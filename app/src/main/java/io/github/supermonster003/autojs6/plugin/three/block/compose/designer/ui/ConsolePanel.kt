package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxPreview

/**
 * The preview console (roadmap P9.3 step 2): what the script writes with `console` / `log` / `print`, its toasts
 * and snackbars, and its errors, while the interactive preview runs it. A line above says what runs and what does
 * not; the lines are output of the preview only and are dropped when the script starts again.
 */
@Composable
internal fun ConsolePanel(sandbox: SandboxPreview, dialect: Dialect, interactive: Boolean, palette: StandalonePalette, modifier: Modifier = Modifier) {
    val lines = sandbox.console
    val list = rememberLazyListState()
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) list.scrollToItem(lines.size - 1) }
    Column(modifier.fillMaxSize().testTag("designer-console")) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val status = when {
                dialect == Dialect.TSX -> R.string.designer_console_tsx
                !SandboxPreview.SUPPORTED -> R.string.designer_console_old_android
                !interactive -> R.string.designer_console_idle
                sandbox.failure?.stops == true -> R.string.designer_console_stopped
                else -> R.string.designer_console_running
            }
            Text(stringResource(status), color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.weight(1f).testTag("designer-console-status"))
            if (lines.isNotEmpty()) TextButton(onClick = { sandbox.clearConsole() }, modifier = Modifier.testTag("designer-console-clear")) {
                Text(stringResource(R.string.designer_console_clear))
            }
        }
        SelectionContainer {
            LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = AddButtonClearance)) {
                itemsIndexed(lines) { _, line ->
                    val color = when (line.level) {
                        SandboxPreview.ERROR -> palette.danger
                        "warn", "toast", "snackbar" -> palette.accent
                        else -> palette.text
                    }
                    val prefix = if (line.level == "log") "" else "[${line.level}] "
                    Text(prefix + line.text, color = Color(color), fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp).testTag("designer-console-line"))
                }
            }
        }
    }
}
