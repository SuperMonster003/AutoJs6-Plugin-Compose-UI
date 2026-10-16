package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.ThreeBlockComposePlugin
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptLauncher
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.UiPreviewClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.DocumentIo
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.DocumentSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Roadmap P11.3 device coverage of layout files: moving the layout literal into a file and saving makes a private
 * project a folder project (D56 / D63 / D66); a new Android XML file takes view classes from the palette (D67); a
 * folder project's zip imports back to the same files (D62); and, with a host that has the project zip entries
 * (D64), the zip reaches AutoJs6, which runs it (a marker its script writes) and imports it (the folder appears).
 */
@RunWith(AndroidJUnit4::class)
internal class UiLayoutFileDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val store = ProjectStore(context)
    private val io = DocumentIo(context, store)

    @Before fun clean() { store.list().filter { it.displayName.startsWith(PREFIX) }.forEach { store.delete(it.name) } }
    @After fun remove() { store.list().filter { it.displayName.startsWith(PREFIX) }.forEach { store.delete(it.name) } }

    private fun launch(name: String): ActivityScenario<DesignerActivity> = ActivityScenario.launch(DesignerActivity.intent(context, name))

    private fun ActivityScenario<DesignerActivity>.session() = run {
        lateinit var activity: DesignerActivity
        onActivity { activity = it }
        requireNotNull(activity.currentUiSession)
    }

    private fun waitFor(tag: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun openMenu(item: String) {
        rule.onNodeWithTag("designer-layout-files").performClick()
        waitFor(item)
        rule.onNodeWithTag(item).performClick()
        waitFor("designer-layout-file-path")
    }

    private fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).let { descriptor ->
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    @Test
    fun movingTheLayoutIntoAFileAndSavingMakesAFolderProject() {
        val name = store.create(PREFIX + "extract", ProjectStore.UI_TEMPLATE).name
        launch(name).use { scenario ->
            waitFor("designer-xml-outline")
            openMenu("designer-layout-file-extract")
            rule.onNodeWithTag("designer-layout-file-confirm").performClick()
            rule.waitForIdle()
            assertEquals("layout.xml", scenario.session().document.files.single().file?.path)
            assertTrue(scenario.session().dirty)
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val folder = PREFIX + "extract/main.js"
        assertFalse(store.exists(name))
        assertTrue(store.exists(folder))
        val dir = store.projectDir(folder)!!
        assertTrue(store.read(folder).contains("ui.layoutFile(\"layout.xml\");"))
        assertEquals("<vertical padding=\"16\">\n    <text id=\"title\" text=\"Hello\" textSize=\"24sp\"/>\n    <button id=\"ok\" text=\"OK\"/>\n</vertical>\n", File(dir, "layout.xml").readText())
        assertTrue(File(dir, ProjectStore.PROJECT_CONFIG).isFile)
        assertFalse(store.draftFile(folder).exists())
        launch(folder).use { scenario ->
            waitFor("designer-xml-outline")
            assertEquals(listOf("layout.xml"), scenario.session().document.files.map { it.file?.path })
            assertFalse(scenario.session().dirty)
        }
    }

    @Test
    fun aNewAndroidXmlFileTakesViewClassesAndSaves() {
        val name = store.create(PREFIX + "native", ProjectStore.UI_TEMPLATE).name
        launch(name).use { scenario ->
            waitFor("designer-xml-outline")
            openMenu("designer-layout-file-new")
            rule.onNodeWithTag("designer-layout-file-android").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-layout-file-confirm").performClick()
            rule.waitForIdle()
            val session = scenario.session()
            assertEquals("res/layout/item.xml", session.document.fragments[session.fragment].file?.path)
            assertTrue(session.nativeFragment)
            rule.onNodeWithTag("designer-add").performClick()
            rule.onNodeWithTag("designer-palette").performScrollToNode(hasTestTag("designer-palette-TextView"))
            rule.onNodeWithTag("designer-palette-TextView").performClick()
            rule.waitForIdle()
            assertEquals("TextView", scenario.session().selected?.tag)
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val folder = PREFIX + "native/main.js"
        val layout = File(store.projectDir(folder), "res/layout/item.xml").readText()
        assertTrue(layout, layout.contains("<TextView") && layout.contains("android:layout_width=\"wrap_content\"") && layout.contains("xmlns:android="))
        assertTrue(store.read(folder).trimEnd().endsWith("let itemView = ui.inflate(files.read(\"res/layout/item.xml\"));"))
    }

    @Test
    fun aFolderProjectZipImportsBackToTheSameFiles() {
        val (script, files) = ProjectStore.UI_ANDROID_TEMPLATE
        val project = store.createFolder(PREFIX + "zip", script, files + ("res/values/strings.xml" to "<resources/>\n"))
        val source = DocumentSource.Local(project.name)
        val zip = assertNotNullAndGet(io.exportArchive(source, store.read(project.name), files))
        val imported = zip.inputStream().use { store.importArchive(PREFIX + "zip-copy", it) }
        val original = store.projectDir(project.name)!!
        val copy = store.projectDir(imported.name)!!
        val names = original.walkTopDown().filter { it.isFile }.map { it.relativeTo(original).invariantSeparatorsPath }.toSortedSet()
        assertEquals(names, copy.walkTopDown().filter { it.isFile }.map { it.relativeTo(copy).invariantSeparatorsPath }.toSortedSet())
        for (path in names) assertEquals(path, File(original, path).readText(), File(copy, path).readText())
    }

    @Test
    fun theProjectZipReachesAutoJs6WhichRunsAndImportsIt() {
        assumeTrue("the host has no project zip entries", ScriptLauncher.hostTakesProjects(context))
        val stamp = System.currentTimeMillis().toString()
        val marker = "designer-handoff-$stamp.txt"
        // A plain (non-ui) script: it reads a file of its project and writes the marker into the host's own files.
        val script = "files.write(context.getExternalFilesDir(null) + '/$marker', 'ran ' + files.read('./data.txt'));\n"
        val project = store.createFolder(PREFIX + "handoff-$stamp", script, mapOf("data.xml" to "<frame/>\n"))
        File(store.projectDir(project.name), "data.txt").writeText("from the project")
        val zip = assertNotNullAndGet(io.handoffArchive(DocumentSource.Local(project.name), script, emptyMap()))
        val uri = ScriptLauncher.projectUri(context, zip)
        context.contentResolver.openInputStream(uri)!!.use { assertTrue(it.read() >= 0) }

        assertEquals(ScriptLauncher.Result.STARTED, ScriptLauncher.start(context, ScriptLauncher.runProjectIntent(uri)))
        val markerPath = "/sdcard/Android/data/${ThreeBlockComposePlugin.HOST_PACKAGE_NAME}/files/$marker"
        val deadline = System.currentTimeMillis() + 20_000
        var text = ""
        while (System.currentTimeMillis() < deadline) {
            text = shell("cat $markerPath").trim()
            if (text.startsWith("ran ")) break
            Thread.sleep(500)
        }
        assertEquals("ran from the project", text)
        shell("rm -f $markerPath")

        assertEquals(ScriptLauncher.Result.STARTED, ScriptLauncher.start(context, ScriptLauncher.importProjectIntent(uri)))
        val folderName = zip.name.removeSuffix(".zip")
        var found = ""
        val importDeadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < importDeadline) {
            found = shell("find /storage/emulated/0/ -maxdepth 3 -type d -name $folderName").trim()
            if (found.isNotEmpty()) break
            Thread.sleep(500)
        }
        assertTrue("the imported folder $folderName did not appear", found.isNotEmpty())
        val importedDir = found.lines().first()
        // The folder appears before its files are written.
        var imported = ""
        while (System.currentTimeMillis() < importDeadline && imported.isEmpty()) {
            imported = shell("cat $importedDir/main.js").trim()
            if (imported.isEmpty()) Thread.sleep(500)
        }
        assertEquals(script.trim(), imported)
        shell("rm -rf $importedDir")
    }

    @Test
    fun aLayoutFileBesideADocumentIsEditedAndWrittenBack() {
        val dir = File(context.cacheDir, PREFIX + "external").apply { deleteRecursively(); mkdirs() }
        val script = File(dir, "screen.js").apply { writeText("\"ui\";\n\nui.layoutFile(\"layout.xml\");\n") }
        val layout = File(dir, "layout.xml").apply { writeText("<vertical>\n    <text id=\"a\" text=\"A\"/>\n</vertical>\n") }
        try {
            ActivityScenario.launch<DesignerActivity>(DesignerActivity.documentIntent(context, Uri.fromFile(script), writable = true)).use { scenario ->
                waitFor("designer-xml-outline")
                val session = scenario.session()
                assertEquals(listOf("layout.xml"), session.document.files.map { it.file?.path })
                val text = session.document.elements().first { it.attr("id")?.value == "a" }
                scenario.onActivity { session.execute(UiEditCommand.SetAttr(text.id, "text", "B"), merge = false) }
                rule.waitForIdle()
                rule.onNodeWithTag("designer-save").performClick()
                rule.waitForIdle()
            }
            assertEquals("<vertical>\n    <text id=\"a\" text=\"B\"/>\n</vertical>\n", layout.readText())
            assertEquals("\"ui\";\n\nui.layoutFile(\"layout.xml\");\n", script.readText())
            // Written back in place: no private copy of the document was made.
            assertTrue(store.list().none { it.displayName.startsWith("screen") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun theLayoutPreviewResolvesTheProjectsResources() {
        val (script, files) = ProjectStore.UI_ANDROID_TEMPLATE
        // The title text and the padding come from the project's res/ (contract v3 resourcesFd), not from the layout.
        val layout = files.getValue("res/layout/activity_main.xml")
            .replace("android:text=\"Hello\"", "android:text=\"@string/greeting\"")
            .replace("android:padding=\"16dp\"", "android:padding=\"@dimen/page\"")
        val project = store.createFolder(
            PREFIX + "resources", script,
            mapOf(
                "res/layout/activity_main.xml" to layout,
                "res/values/strings.xml" to "<resources>\n    <string name=\"greeting\">Hi from res</string>\n</resources>\n",
                "res/values/dimens.xml" to "<resources>\n    <dimen name=\"page\">24dp</dimen>\n</resources>\n",
            ),
        )
        launch(project.name).use { scenario ->
            lateinit var activity: DesignerActivity
            scenario.onActivity { activity = it }
            fun client() = requireNotNull(activity.currentPreviewClient)
            rule.waitUntil(30_000) { client().status is UiPreviewClient.Status.Ready || client().status is UiPreviewClient.Status.Unavailable }
            assumeTrue("no preview service", client().status is UiPreviewClient.Status.Ready)
            assumeTrue("the host takes no project resources (contract v3)", client().resourcesSupported)
            rule.waitUntil(90_000) { scenario.session().previewFrames > 0 && client().bounds.isNotEmpty() }
            assertEquals(null, client().failure)
            assertEquals(3, client().elementCount)
            assertTrue(client().diagnostics.toString(), client().diagnostics.none { it.message.contains("greeting") || it.message.contains("page") })
        }
    }

    private fun <T> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    private companion object {
        const val PREFIX = "ui-files-device-test-"
    }
}
