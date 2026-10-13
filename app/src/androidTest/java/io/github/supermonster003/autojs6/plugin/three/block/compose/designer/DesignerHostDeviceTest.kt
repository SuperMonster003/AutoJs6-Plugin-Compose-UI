package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.ThreeBlockComposePlugin
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptLauncher
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Roadmap P8.4 / D39 device coverage: the designer FileProvider exposes a project read-write under its file
 * name, the host destinations target the host's public entries (and resolve when the host is installed),
 * an external document opens from its Uri and is written back through it, a read-only document falls back
 * to a private project, and a file changed outside the designer is reloaded on resume.
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerHostDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val created = ArrayList<String>()

    @Before fun clean() { store.list().filter { it.name.startsWith("host-test-") }.forEach { store.delete(it.name) } }
    @After fun remove() { created.forEach { store.delete(it) }; store.list().filter { it.name.startsWith("host-test-") }.forEach { store.delete(it.name) } }

    private fun project(stem: String, script: String = store.template()): String {
        val project = store.create("host-test-$stem", script)
        created += project.name
        return project.name
    }

    @Test
    fun fileProviderExposesProjectsReadWriteUnderTheirFileName() {
        val name = project("provider")
        val uri = ScriptLauncher.projectUri(context, store.file(name))
        assertEquals("content", uri.scheme)
        assertEquals(context.packageName + ".designer", uri.authority)
        val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { cursor ->
            cursor.moveToFirst(); cursor.getString(0)
        }
        assertEquals(name, displayName)
        assertEquals(store.template(), context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(StandardCharsets.UTF_8) })
        context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write("compose.mount(compose.Text({text: 'written'}));\n".toByteArray(StandardCharsets.UTF_8)) }
        assertTrue(store.read(name).contains("written"))
        // Nothing outside the project directory is reachable through the authority.
        val outside = runCatching { ScriptLauncher.projectUri(context, File(context.filesDir, "designer/drafts/x.js")) }
        assertTrue(outside.isFailure)
    }

    @Test
    fun hostDestinationsTargetTheHostEntriesAndResolveWhenInstalled() {
        val name = project("destinations")
        val uri = ScriptLauncher.projectUri(context, store.file(name))
        val edit = ScriptLauncher.editIntent(uri, writable = true)
        val import = ScriptLauncher.importIntent(uri)
        assertEquals(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, edit.component?.packageName)
        assertEquals(ScriptLauncher.HOST_EDIT_ACTIVITY, edit.component?.className)
        assertEquals(ScriptLauncher.HOST_IMPORT_ACTIVITY, import.component?.className)
        assertEquals(uri, edit.data)
        assertEquals(uri, import.data)
        assertEquals(ScriptLauncher.SCRIPT_MIME_TYPE, edit.type)
        assertTrue(edit.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
        assertTrue(edit.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0)
        assertEquals(0, ScriptLauncher.editIntent(uri, writable = false).flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertEquals(0, import.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (ScriptLauncher.isHostInstalled(context)) {
            // Resolvable through the manifest <queries>; not started, a host window would take focus from later tests.
            assertNotNull("the host editor entry resolves", context.packageManager.resolveActivity(edit, 0))
            assertNotNull("the host import entry resolves", context.packageManager.resolveActivity(import, 0))
        } else {
            assertEquals(ScriptLauncher.Result.HOST_MISSING, ScriptLauncher.start(context, edit))
            assertEquals(ScriptLauncher.Result.HOST_MISSING, ScriptLauncher.start(context, import))
        }
    }

    @Test
    fun externalDocumentOpensFromItsUriAndIsWrittenBackThroughIt() {
        val name = project("external")
        val uri = ScriptLauncher.projectUri(context, store.file(name))
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.documentIntent(context, uri, writable = true)).use {
            rule.onNodeWithTag("designer-outline").assertIsDisplayed()
            rule.onNodeWithText(name, substring = true).assertIsDisplayed()
            rule.onNodeWithTag("designer-add").performClick()
            rule.onNodeWithTag("designer-palette").performScrollToNode(hasTestTag("designer-palette-Button"))
            rule.onNodeWithTag("designer-palette-Button").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
            rule.onNodeWithText(context.getString(R.string.designer_saved)).assertIsDisplayed()
        }
        assertTrue(store.read(name).contains("compose.Button("))
        // No private copy was made: the document itself received the text.
        assertEquals(listOf(name), store.list().map { it.name }.filter { it.startsWith("host-test-") })
    }

    @Test
    fun readOnlyExternalDocumentIsSavedAsAPrivateProject() {
        val file = File(context.cacheDir, "host-test-readonly.js")
        file.writeText("compose.mount(compose.Column({padding: 8},\n    compose.Text({key: 'ro', text: 'Read only'})));\n", StandardCharsets.UTF_8)
        assertTrue(file.setWritable(false, false))
        try {
            ActivityScenario.launch<DesignerActivity>(DesignerActivity.documentIntent(context, Uri.fromFile(file), writable = false)).use {
                rule.onNodeWithTag("designer-outline-2").assertIsDisplayed()
                rule.onNodeWithTag("designer-outline-2").performClick()
                rule.onNodeWithTag("designer-delete-node").performClick()
                rule.waitForIdle()
                rule.onNodeWithTag("designer-save").performClick()
                rule.waitForIdle()
                val fallback = store.list().map { it.name }.single { it.startsWith("host-test-readonly") }
                created += fallback
                rule.onNodeWithText(context.getString(R.string.designer_external_readonly_saved, fallback)).assertIsDisplayed()
                assertFalse(store.read(fallback).contains("key: 'ro'"))
                // The title now names the project the text lives in.
                assertTrue("the title and the hint both name the project", rule.onAllNodesWithText(fallback, substring = true).fetchSemanticsNodes().size >= 2)
            }
            assertTrue("the read-only file is untouched", file.readText(StandardCharsets.UTF_8).contains("key: 'ro'"))
        } finally {
            file.setWritable(true, false)
            file.delete()
        }
    }

    @Test
    fun changesMadeOutsideTheDesignerAreReloadedOnResume() {
        val name = project("reload")
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use { scenario ->
            rule.onNodeWithTag("designer-outline-2").assertIsDisplayed()
            scenario.moveToState(Lifecycle.State.CREATED)
            store.write(name, "compose.mount(compose.Column({padding: 8},\n    compose.Text({key: 'one', text: 'One'}),\n    compose.Text({key: 'two', text: 'Two'})));\n")
            scenario.moveToState(Lifecycle.State.RESUMED)
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-outline-3").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("designer-outline-3").assertIsDisplayed()
            assertEquals(0, rule.onAllNodesWithTag("designer-external-reload").fetchSemanticsNodes().size)
        }
    }

    @Test
    fun externalChangesWithLocalEditsAreOfferedNotForced() {
        val name = project("conflict")
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use { scenario ->
            rule.onNodeWithTag("designer-outline-2").performClick()
            rule.onNodeWithTag("designer-delete-node").performClick()
            rule.waitForIdle()
            scenario.moveToState(Lifecycle.State.CREATED)
            store.write(name, "compose.mount(compose.Column({padding: 8},\n    compose.Text({key: 'one', text: 'One'}),\n    compose.Text({key: 'two', text: 'Two'})));\n")
            scenario.moveToState(Lifecycle.State.RESUMED)
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-external-reload").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("designer-external-keep").performClick()
            rule.waitForIdle()
            assertEquals(0, rule.onAllNodesWithTag("designer-outline-2").fetchSemanticsNodes().size)
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        assertFalse(store.read(name).contains("key: 'two'"))
    }
}
