package io.github.supermonster003.autojs6.plugin.three.block.compose.app

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Script text in the gallery and the designer: source, XML, expressions, identifiers and what a running script
 * prints. Compose lays a Text out in the layout direction unless told otherwise, so in Arabic a line of code was set
 * right to left (`count++;` showed as `;count++`) and a block of code hung from the right edge. Every monospace text
 * takes its style from here (`ScriptTextGuardTest`).
 */
internal object ScriptText {
    /** Code: monospace and left to right whatever its first letter, so a comment in Arabic stays a line of code. */
    val Code = TextStyle(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr)

    /** Console output and lines that put code into a sentence: monospace, each paragraph in the direction of its first strong letter. */
    val Output = TextStyle(fontFamily = FontFamily.Monospace, textDirection = TextDirection.ContentOrLtr)

    /** Code in the interface font (slot names, `#key "text"` summaries, English diagnostics): the direction only. */
    val Inline = TextStyle(textDirection = TextDirection.ContentOrLtr)

    /**
     * [LocalTextStyle] with [Code] on top, aligned to the start of the layout rather than of the code, so an identifier
     * that fills a row of a right-to-left screen stays on its right (a block of code is laid out [LeftToRight]).
     */
    val code: TextStyle
        @Composable @ReadOnlyComposable get() = LocalTextStyle.current.merge(Code)
            .merge(TextStyle(textAlign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextAlign.Right else TextAlign.Left))

    /** [LocalTextStyle] with [Output] on top. */
    val output: TextStyle
        @Composable @ReadOnlyComposable get() = LocalTextStyle.current.merge(Output)

    /** [LocalTextStyle] with [Inline] on top. */
    val inline: TextStyle
        @Composable @ReadOnlyComposable get() = LocalTextStyle.current.merge(Inline)

    /** [code] between LRI and PDI, so it keeps its order where a translated sentence reads right to left. */
    fun isolate(code: String): String = "${Char(0x2066)}$code${Char(0x2069)}"
}

/** Lays [content] out left to right: a block of code or the console lines start at the left edge and scroll from there. */
@Composable
internal fun LeftToRight(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}
