package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Project names made from document names (roadmap D39): an import or a read-only document saved as a project
 * may carry any display name, and the project created from it must always have a valid name.
 */
class ProjectNameTest {

    private fun assertUsable(stem: String) {
        for (name in listOf("$stem.js", "$stem.tsx", "$stem-2.js", "$stem-12.tsx")) {
            assertTrue("$name must be a valid project name", ProjectStore.isValidName(name))
            assertTrue("$name must fit a file name", name.toByteArray(Charsets.UTF_8).size <= 255)
        }
    }

    @Test
    fun `characters a project name cannot hold are replaced`() {
        assertEquals("test--1-", ProjectStore.stemOf("test (1)", "untitled"))
        assertEquals("My-Script", ProjectStore.stemOf("  My Script ", "untitled"))
        assertEquals("a-b-c", ProjectStore.stemOf("a/b\\c", "untitled"))
        assertEquals("中文-名", ProjectStore.stemOf("中文 名", "untitled"))
        assertEquals("plain_name-1.2", ProjectStore.stemOf("plain_name-1.2", "untitled"))
    }

    @Test
    fun `dots never lead, trail or repeat`() {
        assertEquals("hidden", ProjectStore.stemOf(".hidden", "untitled"))
        assertEquals("notes", ProjectStore.stemOf("notes.", "untitled"))
        assertEquals("a.b", ProjectStore.stemOf("a..b", "untitled"))
        assertEquals("v1.0", ProjectStore.stemOf("..v1...0..", "untitled"))
    }

    @Test
    fun `empty names fall back and long names are cut`() {
        assertEquals("untitled", ProjectStore.stemOf("", "untitled"))
        assertEquals("imported", ProjectStore.stemOf("   ", "imported"))
        assertEquals("imported", ProjectStore.stemOf("...", "imported"))
        assertEquals(ProjectStore.MAX_STEM_CHARS, ProjectStore.stemOf("x".repeat(300), "untitled").length)
        assertEquals("cut before a dot", "y".repeat(ProjectStore.MAX_STEM_CHARS - 1), ProjectStore.stemOf("y".repeat(ProjectStore.MAX_STEM_CHARS - 1) + ".z", "untitled"))
    }

    @Test
    fun `every stem makes valid project names`() {
        val names = listOf(
            "test (1)", "My Script", ".hidden", "notes.", "a..b", "-", "!!!", "  ", "中文 名 (副本)", "名".repeat(200),
            "x".repeat(300), "emoji \uD83D\uDE00 name", "tab\tand\nnewline", "日本語のファイル", "한국어", "a.b.c.", "..", ".js",
        )
        names.forEach { assertUsable(ProjectStore.stemOf(it, "untitled")) }
    }
}
