package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Folder projects (roadmap P11.3 / D56, appendix B): a `"ui"` project whose script refers to layout files lives in
 * `projects/<stem>/main.js` next to its files, and a single-file project is promoted to one with its drafts.
 */
class ProjectFolderTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun store() = ProjectStore(temp.newFolder("designer"))

    @Test
    fun `folder project names`() {
        assertTrue(ProjectStore.isValidName("demo/main.js"))
        assertTrue(ProjectStore.isFolderName("demo/main.js"))
        assertFalse(ProjectStore.isValidName("a/b/main.js"))
        assertFalse(ProjectStore.isValidName("demo/other.js"))
        assertFalse(ProjectStore.isValidName("../main.js"))
        assertFalse(ProjectStore.isValidName("demo/main.tsx"))
        assertEquals("demo", ProjectStore.displayNameOf("demo/main.js"))
        assertEquals("demo.js", ProjectStore.displayNameOf("demo.js"))
    }

    @Test
    fun `promotion moves the script and its drafts into the folder`() {
        val store = store()
        val project = store.create("demo", ProjectStore.UI_TEMPLATE)
        store.writeDraft(project.name, "draft")
        File(store.layoutDraftsDir(project.name), "layout.xml").apply { parentFile!!.mkdirs() }.writeText("<frame/>")

        val promoted = store.promote(project.name)

        assertEquals("demo/main.js", promoted.name)
        assertTrue(promoted.folder)
        assertFalse(File(store.projectsDir, "demo.js").exists())
        assertEquals(ProjectStore.UI_TEMPLATE, store.read(promoted.name))
        assertEquals("draft", store.draftFile(promoted.name).readText())
        assertEquals("<frame/>", File(store.layoutDraftsDir(promoted.name), "layout.xml").readText())
        assertFalse(store.layoutDraftsDir(project.name).exists())
        assertEquals(listOf("demo/main.js"), store.list().map { it.name })
        assertEquals(ProjectStore.Kind.UI, store.list().single().kind)
        assertEquals("demo", store.list().single().displayName)
        // A folder project is already promoted.
        assertEquals(promoted.name, store.promote(promoted.name).name)
    }

    @Test
    fun `promotion counts past a taken folder name`() {
        val store = store()
        File(store.projectsDir, "demo").mkdirs()
        val project = store.create("demo", ProjectStore.UI_TEMPLATE)
        assertEquals("demo-2/main.js", store.promote(project.name).name)
    }

    @Test
    fun `layout files stay inside the folder`() {
        val store = store()
        val single = store.create("single", ProjectStore.UI_TEMPLATE)
        assertNull(store.layoutFile(single.name, "layout.xml"))
        assertFalse(store.writeLayoutFile(single.name, "layout.xml", "<frame/>"))

        val folder = store.promote(store.create("demo", ProjectStore.UI_TEMPLATE).name)
        val dir = store.projectDir(folder.name)!!
        assertEquals(File(dir, "layout.xml").canonicalFile, store.layoutFile(folder.name, "layout.xml"))
        assertEquals(File(dir, "res/layout/main.xml").canonicalFile, store.layoutFile(folder.name, "res/layout/main.xml"))
        assertEquals(File(dir, "layout.xml").canonicalFile, store.layoutFile(folder.name, "./sub/../layout.xml"))
        assertNull(store.layoutFile(folder.name, "../single.js"))
        assertNull(store.layoutFile(folder.name, "../demo-other/x.xml"))
        assertNull(store.layoutFile(folder.name, File(temp.root, "x.xml").absolutePath))
        assertNull(store.layoutFile(folder.name, ""))

        assertTrue(store.writeLayoutFile(folder.name, "res/layout/main.xml", "<LinearLayout/>"))
        assertEquals("<LinearLayout/>", File(dir, "res/layout/main.xml").readText())
        assertFalse(File(dir, "res/layout/main.xml.tmp").exists())
    }

    @Test
    fun `renaming and deleting a folder project take its files and drafts along`() {
        val store = store()
        val folder = store.promote(store.create("demo", ProjectStore.UI_TEMPLATE).name)
        store.writeLayoutFile(folder.name, "layout.xml", "<frame/>")
        store.writeDraft(folder.name, "draft")
        File(store.layoutDraftsDir(folder.name), "layout.xml").apply { parentFile!!.mkdirs() }.writeText("<frame id=\"a\"/>")

        val renamed = store.rename(folder.name, "other/main.js")
        assertEquals("other/main.js", renamed.name)
        assertEquals(ProjectStore.Kind.UI, renamed.kind)
        assertFalse(File(store.projectsDir, "demo").exists())
        assertEquals("<frame/>", store.layoutFile(renamed.name, "layout.xml")!!.readText())
        assertEquals("draft", store.draftFile(renamed.name).readText())
        assertEquals("<frame id=\"a\"/>", File(store.layoutDraftsDir(renamed.name), "layout.xml").readText())

        store.clearDraft(renamed.name)
        assertFalse(store.draftFile(renamed.name).exists())
        assertFalse(store.layoutDraftsDir(renamed.name).exists())

        store.writeDraft(renamed.name, "draft")
        store.delete(renamed.name)
        assertFalse(File(store.projectsDir, "other").exists())
        assertFalse(File(store.draftsDir, "other").exists())
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun `a folder project keeps its form when renamed`() {
        val store = store()
        val folder = store.promote(store.create("demo", ProjectStore.UI_TEMPLATE).name)
        val failure = runCatching { store.rename(folder.name, "flat.js") }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(store.exists(folder.name))
    }
}
