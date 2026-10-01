package io.github.supermonster003.autojs6.plugin.compose.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every locale carries the same string keys, sorted by name, the descriptions end without terminal
 * punctuation, and every locale ships the generated plugin instruction.
 */
class StringResourceParityTest {

    private val resourceRoot: Path = findProjectRoot().resolve("app/src/main/res")

    @Test
    fun `every locale defines the same translatable strings in sorted order`() {
        val reference = strings("values")
        assertTrue("the plugin description must stay", "plugin_description" in reference.keys)
        assertEquals("values strings must be sorted by name", reference.keys.sorted(), reference.keys.toList())
        LOCALE_DIRECTORIES.forEach { directory ->
            val localized = strings(directory)
            assertEquals("$directory must define the same keys as values", reference.keys, localized.keys)
            assertEquals("$directory strings must be sorted by name", localized.keys.sorted(), localized.keys.toList())
            localized.values.forEach { value -> assertTrue("$directory has an empty string", value.isNotBlank()) }
        }
        assertEquals("values and values-en must be identical", reference, strings("values-en"))
    }

    @Test
    fun `plugin descriptions end without terminal punctuation and avoid host qualifiers`() {
        (LOCALE_DIRECTORIES + "values").forEach { directory ->
            val description = strings(directory).getValue("plugin_description")
            assertFalse("$directory plugin_description must not end with punctuation", description.last() in ".!?。")
            assertFalse("$directory plugin_description must not mention AutoJs6", description.contains("AutoJs6"))
            assertTrue("$directory plugin_description must name the rendering technology", description.contains("Jetpack Compose"))
        }
    }

    @Test
    fun `the application title is not translatable and matches the plugin name`() {
        val document = parse(resourceRoot.resolve("values/strings_donottranslate.xml"))
        val appName = document.elements("string").single()
        assertEquals("app_name", appName.getAttribute("name"))
        assertEquals("false", appName.getAttribute("translatable"))
        assertEquals("Compose UI", appName.textContent)
    }

    @Test
    fun `every locale ships the generated plugin instruction`() {
        (RAW_DIRECTORIES + "raw").forEach { directory ->
            val instruction = resourceRoot.resolve("$directory/plugin_instruction.md")
            assertTrue("missing $directory/plugin_instruction.md (run .python/generate_markdown.py)", Files.isRegularFile(instruction))
            assertTrue("$directory/plugin_instruction.md must not be empty", Files.size(instruction) > 0)
        }
        assertEquals("raw and raw-en must be identical", Files.readString(resourceRoot.resolve("raw/plugin_instruction.md")), Files.readString(resourceRoot.resolve("raw-en/plugin_instruction.md")))
    }

    @Test
    fun `the transparent launcher icon exists for day and night without adaptive overrides`() {
        listOf("mipmap", "mipmap-night").forEach { directory ->
            assertTrue("missing $directory/ic_launcher.png (run .python/generate_launcher_icons.py)", Files.isRegularFile(resourceRoot.resolve("$directory/ic_launcher.png")))
        }
        Files.list(resourceRoot).use { directories ->
            directories.filter { it.fileName.toString().startsWith("mipmap") }.forEach { directory ->
                assertFalse("$directory must not override the bitmap with an adaptive icon", Files.exists(directory.resolve("ic_launcher.xml")))
                assertFalse("$directory must not declare a round icon", Files.exists(directory.resolve("ic_launcher_round.png")))
            }
        }
    }

    private fun strings(directory: String): Map<String, String> {
        val document = parse(resourceRoot.resolve("$directory/strings.xml"))
        return LinkedHashMap<String, String>().also { map ->
            document.elements("string").forEach { element -> map[element.getAttribute("name")] = element.textContent }
        }
    }

    private fun parse(path: Path): Element {
        assertTrue("missing resource file $path", Files.isRegularFile(path))
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(path.toFile()).documentElement
    }

    private fun Element.elements(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private companion object {
        val LOCALE_DIRECTORIES = listOf(
            "values-ar", "values-en", "values-es", "values-fr", "values-ja", "values-ko", "values-ru",
            "values-zh", "values-zh-rHK", "values-zh-rTW",
        )
        val RAW_DIRECTORIES = LOCALE_DIRECTORIES.map { it.replace("values-", "raw-") }
    }
}
