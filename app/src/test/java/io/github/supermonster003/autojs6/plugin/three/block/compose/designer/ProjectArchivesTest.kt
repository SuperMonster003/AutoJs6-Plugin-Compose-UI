package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.ProjectArchives
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Roadmap P11.3 (contract v3): the zips the preview sends with layouts (`resourcesFd`) and runs (`projectFd`) hold the
 * project's files with the designer's texts in place of the saved ones, keep their bytes (and digest) while nothing
 * changes, and never carry a path that leaves the zip's root.
 */
class ProjectArchivesTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun entries(archive: ProjectArchives.Archive): Map<String, String> = ZipFile(archive.file).use { zip ->
        zip.entries().toList().associate { it.name to zip.getInputStream(it).readBytes().toString(Charsets.UTF_8) }
    }

    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    @Test
    fun `a folder project goes whole with the designer's texts in place of the saved files`() {
        val project = temp.newFolder("demo")
        File(project, "main.js").writeText("saved script")
        File(project, "layout.xml").writeText("<saved/>")
        File(project, "res/drawable").mkdirs()
        File(project, "res/drawable/card.xml").writeText("<shape/>")
        File(project, ".hidden").writeText("x")
        File(project, "layout.xml.tmp").writeText("x")
        val archives = ProjectArchives(temp.newFolder("cache"))

        val archive = archives.archive(ProjectArchives.PROJECT, ProjectArchives.Root(project, whole = true), mapOf("layout.xml" to "<edited/>", "main.js" to "current script"))!!

        assertEquals(
            mapOf("layout.xml" to "<edited/>", "main.js" to "current script", "res/drawable/card.xml" to "<shape/>"),
            entries(archive),
        )
        assertEquals(sha256(archive.file), archive.sha256)
        assertEquals(setOf("layout.xml", "main.js", "res/drawable/card.xml"), archive.paths)
    }

    @Test
    fun `beside a script file only res and the layout files go`() {
        val folder = temp.newFolder("scripts")
        File(folder, "other.js").writeText("not ours")
        File(folder, "res/values").mkdirs()
        File(folder, "res/values/strings.xml").writeText("<resources/>")
        val archives = ProjectArchives(temp.newFolder("cache"))

        val archive = archives.archive(ProjectArchives.RESOURCES, ProjectArchives.Root(folder, whole = false), mapOf("layout.xml" to "<frame/>"))!!

        assertEquals(setOf("layout.xml", "res/values/strings.xml"), entries(archive).keys)
    }

    @Test
    fun `the zip is kept while nothing changes and written again when something does`() {
        val project = temp.newFolder("demo")
        File(project, "res").mkdirs()
        val drawable = File(project, "res/a.xml").apply { writeText("<a/>") }
        val archives = ProjectArchives(temp.newFolder("cache"))
        val root = ProjectArchives.Root(project, whole = true)

        val first = archives.archive(ProjectArchives.RESOURCES, root, mapOf("layout.xml" to "<frame/>"))!!
        val again = archives.archive(ProjectArchives.RESOURCES, root, mapOf("layout.xml" to "<frame/>"))!!
        assertTrue(first === again)

        val edited = archives.archive(ProjectArchives.RESOURCES, root, mapOf("layout.xml" to "<frame id=\"x\"/>"))!!
        assertNotEquals(first.sha256, edited.sha256)

        drawable.writeText("<a b=\"c\"/>")
        drawable.setLastModified(drawable.lastModified() + 2_000)
        val touched = archives.archive(ProjectArchives.RESOURCES, root, mapOf("layout.xml" to "<frame id=\"x\"/>"))!!
        assertNotEquals(edited.sha256, touched.sha256)

        // The same files in a fresh cache make the same bytes: entry times are fixed.
        val other = ProjectArchives(temp.newFolder("cache2")).archive(ProjectArchives.RESOURCES, root, mapOf("layout.xml" to "<frame id=\"x\"/>"))!!
        assertEquals(touched.sha256, other.sha256)
    }

    @Test
    fun `paths that leave the root are left out and an empty zip is not made`() {
        val archives = ProjectArchives(temp.newFolder("cache"))
        assertNull(archives.archive(ProjectArchives.RESOURCES, null, emptyMap()))
        assertNull(archives.archive(ProjectArchives.RESOURCES, null, mapOf("../x.xml" to "<a/>", "/abs.xml" to "<a/>")))
        val archive = archives.archive(ProjectArchives.RESOURCES, null, mapOf("../x.xml" to "<a/>", "ok/y.xml" to "<b/>"))!!
        assertEquals(setOf("ok/y.xml"), archive.paths)
        for (path in listOf("", "/a", "a/../b", "./a", "a//b", "C:/a", "a\\b")) assertFalse(path, ProjectArchives.isEntryPath(path))
        assertTrue(ProjectArchives.isEntryPath("res/layout/main.xml"))
    }
}
