package io.github.supermonster003.autojs6.plugin.three.block.compose

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.streams.toList

/**
 * Script text reads left to right in every locale (2026-10-17): Compose lays a Text out in the layout direction, so in
 * Arabic the gallery's example code was set right to left (the semicolons at the start of the lines, the block hanging
 * from the right edge and cut off). The styles live in `app/ScriptText.kt`; no other source of the gallery or the
 * designer picks the monospace family itself, so a new code text cannot leave the direction out. The renderer maps a
 * script's own `fontFamily: 'monospace'` and is not interface text.
 */
class ScriptTextGuardTest {

    private val root: Path = findProjectRoot()
    private val main = "app/src/main/java/io/github/supermonster003/autojs6/plugin/three/block/compose/"

    @Test
    fun `monospace interface text takes its style from ScriptText`() {
        val offenders = mainSources()
            .filter { path -> "FontFamily.Monospace" in Files.readString(path) }
            .map { root.relativize(it).pathString.replace('\\', '/') }
            .filterNot { it == main + "app/ScriptText.kt" || it.startsWith(main + "renderer/") }
        assertEquals("monospace text must use ScriptText.Code / ScriptText.Output", emptyList<String>(), offenders)
    }

    @Test
    fun `code reads left to right and output follows its first strong letter`() {
        assertEquals(TextDirection.Ltr, ScriptText.Code.textDirection)
        assertEquals(FontFamily.Monospace, ScriptText.Code.fontFamily)
        assertEquals(TextDirection.ContentOrLtr, ScriptText.Output.textDirection)
        assertEquals(FontFamily.Monospace, ScriptText.Output.fontFamily)
        assertEquals(TextDirection.ContentOrLtr, ScriptText.Inline.textDirection)
        assertEquals(null, ScriptText.Inline.fontFamily)
    }

    @Test
    fun `code put into a sentence is isolated left to right`() {
        assertEquals(listOf(0x2066, 'x'.code, '+'.code, '1'.code, 0x2069), ScriptText.isolate("x+1").map { it.code })
    }

    @Test
    fun `blocks of code and the consoles are laid out left to right`() {
        // A block that scrolls or fills its panel needs the layout direction as well: the text alone would still start
        // at the right edge (and a horizontal scroll from the end of its longest line).
        listOf(
            "app/GalleryActivity.kt" to "\"gallery-code\"",
            "designer/ui/DesignerActivity.kt" to "\"designer-readonly-source\"",
            "designer/preview/XmlCanvas.kt" to "\"designer-xml-placeholder\"",
            "designer/ui/ConsolePanel.kt" to "\"designer-console-line\"",
            "designer/ui/XmlPanels.kt" to "\"designer-ui-console-line\"",
        ).forEach { (file, tag) ->
            val lines = Files.readAllLines(root.resolve(main + file))
            val at = lines.indexOfFirst { tag in it }
            assertTrue("$tag in $file", at >= 0)
            assertTrue("$tag in $file is inside LeftToRight", lines.subList(maxOf(0, at - 14), at).any { "LeftToRight {" in it })
        }
    }

    private fun mainSources(): List<Path> = Files.walk(root.resolve("app/src/main/java")).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.extension in setOf("kt", "java") }.toList()
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
