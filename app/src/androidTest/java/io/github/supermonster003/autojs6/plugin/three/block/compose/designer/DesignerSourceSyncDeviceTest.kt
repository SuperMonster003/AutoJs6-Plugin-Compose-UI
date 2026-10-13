package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptLauncher
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.DocumentIo
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.SourceSync
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap P9.1 / D45: an imported project syncs with its source document only on request. The "source" here is
 * another project file reached through the designer FileProvider, so the real import path records it (the
 * provider does not offer persistable grants; within the plugin's own process none are needed).
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerSourceSyncDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val sync = SourceSync(context.contentResolver, store, DocumentIo(context, store))

    private val one = "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'one', text: 'One'})));\n"
    private val two = "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'one', text: 'One'}),\n    compose.Text({key: 'two', text: 'Two'})));\n"

    @Before @After fun clean() { store.list().filter { it.name.startsWith("sync-test-") }.forEach { store.delete(it.name) } }

    /** Creates the source document and imports it; returns (project name, source file name). */
    private fun imported(script: String = one): Pair<String, String> {
        val origin = store.create("sync-test-origin", script).name
        val project = store.import(context.contentResolver, uriOf(origin)).name
        return project to origin
    }

    private fun uriOf(name: String): Uri = ScriptLauncher.projectUri(context, store.file(name))

    private fun menuItem(tag: String) {
        rule.onNodeWithTag("designer-menu").performClick()
        rule.onNodeWithTag(tag).performScrollTo().performClick()
        rule.waitForIdle()
    }

    @Test
    fun importRecordsTheSourceAndRenameKeepsIt() {
        val (project, origin) = imported()
        val source = checkNotNull(store.sourceOf(project)) { "the import records its source" }
        assertEquals(uriOf(origin), source.uri)
        assertEquals(ProjectStore.digest(one), source.baseline)
        assertTrue(sync.state(project).let { it is SourceSync.State.Ready && !it.sourceChanged && !it.projectChanged })
        store.write(origin, two)
        assertTrue(sync.state(project).let { it is SourceSync.State.Ready && it.sourceChanged && !it.projectChanged })
        val renamed = store.rename(project, "sync-test-renamed.js").name
        assertEquals(source.uri, store.sourceOf(renamed)?.uri)
        assertEquals(null, store.sourceOf(project))
        store.delete(renamed)
        assertEquals(null, store.sourceOf(renamed))
    }

    @Test
    fun writeBackAsksBeforeOverwritingSourceEdits() {
        val (project, origin) = imported()
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, project)).use {
            rule.onNodeWithTag("designer-add").performClick()
            rule.onNodeWithTag("designer-palette").performScrollToNode(hasTestTag("designer-palette-Button"))
            rule.onNodeWithTag("designer-palette-Button").performClick()
            rule.waitForIdle()
            // The source is unchanged since the import: the edit is saved and written back at once.
            menuItem("designer-menu-write-back")
            rule.waitUntil(5_000) { store.read(origin).contains("compose.Button(") }
            // Saving on the way does not queue its own message ahead of this one.
            rule.waitUntil(5_000) { rule.onAllNodesWithText(context.getString(R.string.designer_source_written)).fetchSemanticsNodes().isNotEmpty() }
            // Someone edits the source; the next write back has to be confirmed.
            store.write(origin, two)
            menuItem("designer-menu-write-back")
            rule.onNodeWithTag("designer-source-overwrite").performClick()
            rule.waitForIdle()
        }
        assertEquals(store.read(project), store.read(origin))
        assertEquals(ProjectStore.digest(store.read(project)), store.sourceOf(project)?.baseline)
    }

    @Test
    fun reimportReplacesTheProjectAndAsksWhenItChanged() {
        val (project, origin) = imported()
        store.write(origin, two)
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, project)).use {
            // The project is unchanged since the import: the source replaces it without a question.
            menuItem("designer-menu-reimport")
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-outline-3").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(two, store.read(project))
            // On a small screen (the API 24 CI device is a Nexus 5) the "re-imported" message covers the outline
            // rows under it, so a click there would land on the message.
            rule.waitUntil(10_000) { rule.onAllNodesWithText(context.getString(R.string.designer_source_reimported)).fetchSemanticsNodes().isEmpty() }
            // A local edit since that sync makes the next re-import ask first.
            rule.onNodeWithTag("designer-outline-3").performClick()
            rule.onNodeWithTag("designer-delete-node").performClick()
            rule.waitForIdle()
            store.write(origin, one)
            menuItem("designer-menu-reimport")
            rule.onNodeWithTag("designer-source-replace").performClick()
            rule.waitForIdle()
            assertEquals(0, rule.onAllNodesWithTag("designer-outline-3").fetchSemanticsNodes().size)
        }
        assertEquals(one, store.read(project))
        assertEquals(ProjectStore.digest(one), store.sourceOf(project)?.baseline)
    }

    @Test
    fun aLostSourceIsPickedAgainThroughThePicker() {
        val (project, origin) = imported()
        store.delete(origin)
        val replacement = store.create("sync-test-replacement", two).name
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Stands in for the system document picker the "Pick again" button opens.
        val picker = instrumentation.addMonitor(
            IntentFilter(Intent.ACTION_OPEN_DOCUMENT, "*/*"),
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uriOf(replacement))),
            true,
        )
        try {
            ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, project)).use {
                menuItem("designer-menu-reimport")
                rule.onNodeWithTag("designer-source-pick-again").performClick()
                rule.waitForIdle()
                // The relinked source differs from the project with no baseline yet, so replacing is confirmed.
                rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-source-replace").fetchSemanticsNodes().isNotEmpty() }
                rule.onNodeWithTag("designer-source-replace").performClick()
                rule.waitForIdle()
            }
            assertEquals(1, picker.hits)
        } finally {
            instrumentation.removeMonitor(picker)
        }
        assertEquals(uriOf(replacement), store.sourceOf(project)?.uri)
        assertEquals(two, store.read(project))
        assertFalse(store.exists(origin))
    }
}
