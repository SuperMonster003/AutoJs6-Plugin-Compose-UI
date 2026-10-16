package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.content.Intent
import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.ThreeBlockComposePlugin
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptLauncher
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiHandler
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsxParserClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.UiPreviewClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap P9.2 / D42 device coverage. With a TypeScript Engine that has the parser service, a TSX project
 * opens through it, takes edits (a handler with TypeScript syntax is checked by the same parser), saves as TSX
 * and reopens equal. Without one (not installed, or older than 0.7.0) the project opens read-only with the
 * reason. Which half runs depends on the device; the evidence records both. Roadmap P11.7: a TSX ui project opens in
 * the XML editor through the same parser, gets its layout previewed by the host, takes a handler stub in the TSX
 * form (D68) and saves as TSX that reopens and saves the same.
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerTsxDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val created = ArrayList<String>()

    @Before fun clean() { store.list().filter { it.name.startsWith("tsx-test-") }.forEach { store.delete(it.name) } }
    @After fun remove() { created.forEach { store.delete(it) } }

    private fun project(stem: String, script: String = ProjectStore.TSX_TEMPLATE): String {
        val project = store.create("tsx-test-$stem", script, ProjectStore.TSX_EXTENSION)
        created += project.name
        return project.name
    }

    private fun launch(name: String): ActivityScenario<DesignerActivity> = ActivityScenario.launch(DesignerActivity.intent(context, name))

    /** Waits for the parser service to answer (a cold parse loads TypeScript first). */
    private fun awaitEditor() {
        rule.waitUntil(30_000) { rule.onAllNodesWithTag("designer-canvas-content").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun aTsxProjectIsEditedThroughTheParserAndSavedAsTsx() {
        assumeTrue("needs TypeScript Engine ${TsxParserClient.MIN_VERSION_NAME}+", TsxParserClient.hasParserService(context))
        val name = project("edit")
        assertTrue(name.endsWith(".tsx"))
        val started = System.nanoTime()
        launch(name).use {
            awaitEditor()
            Log.i(TAG, "TSX_OPEN ms=${(System.nanoTime() - started) / 1_000_000}")
            val onCanvas = hasAnyAncestor(hasTestTag("designer-canvas-content"))
            rule.onNode(hasText("Hello") and onCanvas).assertIsDisplayed()
            rule.onNodeWithTag("designer-add").performClick()
            rule.onNodeWithTag("designer-palette").performScrollToNode(hasTestTag("designer-palette-Button"))
            rule.onNodeWithTag("designer-palette-Button").performClick()
            rule.waitForIdle()
            rule.onAllNodesWithTag("designer-outline-3").fetchSemanticsNodes().let { assertEquals(1, it.size) }
            rule.onNodeWithTag("designer-tab-inspector").performClick()
            rule.onNodeWithTag("designer-key").performTextClearance()
            rule.onNodeWithTag("designer-key").performTextInput("primary")
            rule.waitForIdle()

            // A handler body with TypeScript syntax is checked by the TypeScript parser, not by Rhino.
            rule.onNodeWithTag("designer-event-onClick").performScrollTo()
            // The last rows of the inspector can sit under the floating add button: click through semantics.
            rule.onNodeWithTag("designer-handler-add-onClick").performSemanticsAction(SemanticsActions.OnClick)
            rule.waitForIdle()
            rule.onNodeWithTag("designer-handler-body").performScrollTo()
            rule.onNodeWithTag("designer-handler-body").performTextClearance()
            rule.onNodeWithTag("designer-handler-body").performTextInput("const clicks: number = 1;\nconsole.log(clicks as number);")
            applyHandler()
            rule.onNodeWithTag("designer-handler-error").assertDoesNotExist()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val saved = store.read(name)
        assertTrue(saved, saved.contains("<compose.Text key='title' text='Hello' style='headlineSmall' />"))
        assertTrue(saved, saved.contains("<compose.Button key='primary' onClick={function () {\n"))
        assertTrue(saved, saved.contains("const clicks: number = 1;\n"))
        assertTrue(saved, saved.contains("}}>Button</compose.Button>"))
        assertFalse(store.draftFile(name).exists())
        launch(name).use {
            awaitEditor()
            rule.onNodeWithTag("designer-outline-3").assertIsDisplayed()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        assertEquals("saving the reopened project writes the same TSX", saved, store.read(name))
    }

    /** Applies the edited handler and waits for the parser's answer: Apply is disabled while the check runs. */
    private fun applyHandler() {
        rule.onNodeWithTag("designer-handler-apply").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.waitUntil(15_000) { SemanticsProperties.Disabled !in rule.onNodeWithTag("designer-handler-apply").fetchSemanticsNode().config }
    }

    @Test
    fun aHandlerWithASyntaxErrorIsRefusedByTheParser() {
        assumeTrue("needs TypeScript Engine ${TsxParserClient.MIN_VERSION_NAME}+", TsxParserClient.hasParserService(context))
        val name = project("handler-error", "compose.mount(() => {\n    return <compose.Button onClick={() => {}}>Tap</compose.Button>;\n});\n")
        launch(name).use {
            awaitEditor()
            rule.onNodeWithTag("designer-outline-1").performClick()
            rule.onNodeWithTag("designer-tab-inspector").performClick()
            rule.onNodeWithTag("designer-handler-body").performScrollTo()
            rule.onNodeWithTag("designer-handler-body").performTextClearance()
            rule.onNodeWithTag("designer-handler-body").performTextInput("let a: number = ;")
            applyHandler()
            rule.onNodeWithTag("designer-handler-error").assertIsDisplayed()
            Log.i(TAG, "TSX_HANDLER_ERROR " + rule.onNodeWithTag("designer-handler-error").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString())
        }
        assertFalse("a refused handler leaves the project unchanged", store.read(name).contains("let a"))
    }

    @Test
    fun withoutTheParserATsxProjectOpensReadOnly() {
        assumeFalse("runs where TypeScript Engine ${TsxParserClient.MIN_VERSION_NAME}+ is missing", TsxParserClient.hasParserService(context))
        val name = project("read-only")
        launch(name).use {
            rule.waitUntil(15_000) { rule.onAllNodesWithTag("designer-readonly-source").fetchSemanticsNodes().isNotEmpty() }
            // The screen follows the AutoJs6 language, so the reason is checked by its version numbers.
            val installed = runCatching { context.packageManager.getPackageInfo(TsxParserClient.PACKAGE_NAME, 0).versionName }.getOrNull()
            val message = rule.onNodeWithTag("designer-readonly-message").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()
            Log.i(TAG, "TSX_READ_ONLY installed=$installed message=$message")
            assertTrue(message, message.contains(TsxParserClient.MIN_VERSION_NAME))
            if (installed != null) assertTrue(message, message.contains(installed))
            rule.onNode(hasText("<compose.Text key='title' text='Hello' style='headlineSmall' />", substring = true)).assertIsDisplayed()
            assertTrue(rule.onAllNodesWithTag("designer-canvas-content").fetchSemanticsNodes().isEmpty())
            rule.onNodeWithTag("designer-readonly-copy").performClick()
            rule.waitForIdle()
        }
        assertEquals("nothing is written to a read-only project", ProjectStore.TSX_TEMPLATE, store.read(name))
    }

    @Test
    fun aTsxProjectRunsByItsUriThroughTheHostRunEntry() {
        val name = project("run")
        val uri = ScriptLauncher.projectUri(context, store.file(name))
        val run = ScriptLauncher.runIntent(uri)
        assertEquals(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, run.component?.packageName)
        assertEquals(ScriptLauncher.HOST_RUN_ACTIVITY, run.component?.className)
        assertEquals(uri, run.data)
        assertFalse("no script text travels as an extra", run.hasExtra(ScriptLauncher.EXTRA_SCRIPT))
        assertTrue(run.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        // The host names the source after the Uri's display name, which selects the TypeScript compiler.
        val displayName = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
        assertEquals(name, displayName)
        if (ScriptLauncher.isHostInstalled(context)) assertNotNull(context.packageManager.resolveActivity(run, 0))
    }

    @Test
    fun aTsxUiProjectIsEditedInTheXmlEditorAndSavedAsTsx() {
        assumeTrue("needs TypeScript Engine ${TsxParserClient.MIN_VERSION_NAME}+", TsxParserClient.hasParserService(context))
        val name = project("ui", ProjectStore.TSX_UI_TEMPLATE)
        fun awaitOutline() = rule.waitUntil(30_000) { rule.onAllNodesWithTag("designer-xml-outline").fetchSemanticsNodes().isNotEmpty() }
        launch(name).use { scenario ->
            awaitOutline()
            lateinit var activity: DesignerActivity
            scenario.onActivity { activity = it }
            val session = requireNotNull(activity.currentUiSession)
            assertEquals(Dialect.TSX, session.document.dialect)
            // The host previews the layout as the XML its TSX runtime makes of it.
            val client = requireNotNull(activity.currentPreviewClient)
            rule.waitUntil(30_000) { client.status is UiPreviewClient.Status.Ready || client.status is UiPreviewClient.Status.Unavailable }
            if (client.status is UiPreviewClient.Status.Ready) {
                rule.waitUntil(60_000) { client.bounds.isNotEmpty() || client.failure != null }
                assertEquals(null, client.failure)
                assertEquals(3, client.elementCount)
                assertEquals(listOf("title", "ok"), client.bounds.sortedBy { it.index }.mapNotNull { it.id })
                // Until the host compiles TSX for the interactive preview (P11.H3) the switch is off with a note.
                if (client.scriptRunSupported) {
                    assertTrue(rule.onAllNodesWithTag("designer-interact").fetchSemanticsNodes().isEmpty())
                    val note = activity.getString(R.string.designer_ui_interact_tsx_needs_host)
                    assertEquals(note, rule.onNodeWithTag("designer-ui-interact-status", useUnmergedTree = true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text })
                }
            }
            // The button's click is its inline onClick (D68); the title takes a long click stub in the TSX form.
            rule.onNodeWithTag("designer-xml-outline-3").performClick()
            rule.onNodeWithTag("designer-tab-inspector").performClick()
            rule.onNodeWithTag("designer-ui-event-click").performScrollTo().assertIsDisplayed()
            assertTrue(rule.onAllNodesWithTag("designer-ui-event-add-click").fetchSemanticsNodes().isEmpty())
            rule.onNodeWithTag("designer-tab-outline").performClick()
            rule.onNodeWithTag("designer-xml-outline-2").performClick()
            rule.onNodeWithTag("designer-tab-inspector").performClick()
            rule.onNodeWithTag("designer-ui-event-add-long_click").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
            rule.waitUntil(5_000) { UiHandler("title", "long_click") in session.document.handlers }
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val expected = ProjectStore.TSX_UI_TEMPLATE.trimEnd('\n') + "\n\n(ui as any).title.on('long_click', (_event: any, _view: any) => {\n    // TODO\n});\n"
        assertEquals(expected, store.read(name))
        assertFalse(store.draftFile(name).exists())
        // Reopened through the parser, saving writes the same TSX.
        launch(name).use {
            awaitOutline()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        assertEquals(expected, store.read(name))
    }

    private companion object {
        const val TAG = "DesignerTsxTest"
    }
}
