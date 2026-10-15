package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifferenceKind
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiHandler
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.DeclarationEvaluator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.StandardCharsets

/**
 * Roadmap P10.4 device coverage of a `"ui";` project, in the plugin's process and without the host's preview
 * service: the palette inserts a tag, the inspector edits its id and text, saving changes only those lines and
 * reopening shows the same tree with no draft left; outline moves, undo / redo and delete; binding overrides
 * that touch only the canvas; an event stub appended to the script (roadmap D52); an unmappable script's error
 * position; every bundled host sample opening without a parse error; and the script's preview differences listed
 * after the problems without being counted (roadmap P11.2).
 */
@RunWith(AndroidJUnit4::class)
internal class UiDesignerDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val created = ArrayList<String>()

    @Before fun clean() { store.list().filter { it.name.startsWith("ui-device-test-") }.forEach { store.delete(it.name) } }
    @After fun remove() { created.forEach { store.delete(it) } }

    private fun project(stem: String, script: String = ProjectStore.UI_TEMPLATE): String {
        val project = store.create("ui-device-test-$stem", script)
        created += project.name
        return project.name
    }

    private fun launch(name: String): ActivityScenario<DesignerActivity> = ActivityScenario.launch(DesignerActivity.intent(context, name))

    private fun ActivityScenario<DesignerActivity>.session() = run {
        lateinit var activity: DesignerActivity
        onActivity { activity = it }
        requireNotNull(activity.currentUiSession)
    }

    @Test
    fun paletteInsertsInspectorEditsAndSaveChangesOnlyThoseLines() {
        val name = project("palette")
        launch(name).use { scenario ->
            rule.onNodeWithTag("designer-xml-canvas").assertIsDisplayed()
            rule.onNodeWithTag("designer-xml-outline").assertIsDisplayed()
            rule.onNodeWithTag("designer-add").performClick()
            rule.onNodeWithTag("designer-palette").performScrollToNode(hasTestTag("designer-palette-button"))
            rule.onNodeWithTag("designer-palette-button").performClick()
            rule.waitForIdle()
            // The template's vertical (1), text (2) and button (3) gain a fourth element, selected, as the vertical's last child.
            rule.onNodeWithTag("designer-xml-outline-4").assertIsDisplayed()
            assertEquals(4, scenario.session().selectedId)
            rule.onNodeWithTag("designer-tab-inspector").performClick()
            rule.onNodeWithTag("designer-ui-id").performScrollTo().performTextInput("go")
            rule.onNodeWithTag("designer-ui-attr-value-text").performScrollTo().performTextReplacement("Go")
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val expected = ProjectStore.UI_TEMPLATE.replace(
            "        <button id=\"ok\" text=\"OK\"/>\n",
            "        <button id=\"ok\" text=\"OK\"/>\n        <button id=\"go\" text=\"Go\"/>\n",
        )
        assertEquals(expected, store.read(name))
        assertFalse(store.draftFile(name).exists())
        launch(name).use {
            rule.onNodeWithTag("designer-xml-outline").assertIsDisplayed()
            rule.onNodeWithTag("designer-xml-outline-4").assertIsDisplayed()
        }
    }

    @Test
    fun outlineMovesUndoRedoAndDeleteEditTheTree() {
        val name = project("moves", "\"ui\";\nui.layout(\n    <vertical>\n        <text id=\"a\" text=\"A\"/>\n        <text id=\"b\" text=\"B\"/>\n    </vertical>\n);\n")
        launch(name).use {
            rule.onNodeWithTag("designer-xml-outline-3").performClick()
            rule.onNodeWithTag("designer-xml-move-up").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-undo").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-redo").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        val saved = store.read(name)
        assertTrue(saved, saved.indexOf("id=\"b\"") < saved.indexOf("id=\"a\""))
        assertEquals("\"ui\";\nui.layout(\n    <vertical>\n        <text id=\"b\" text=\"B\"/>\n        <text id=\"a\" text=\"A\"/>\n    </vertical>\n);\n", saved)
        launch(name).use {
            rule.onNodeWithTag("designer-xml-outline-2").performClick()
            rule.onNodeWithTag("designer-xml-delete").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-save").performClick()
            rule.waitForIdle()
        }
        assertEquals("\"ui\";\nui.layout(\n    <vertical>\n        <text id=\"a\" text=\"A\"/>\n    </vertical>\n);\n", store.read(name))
    }

    @Test
    fun bindingOverridesChangeOnlyTheCanvas() {
        val source = "\"ui\";\nvar title = \"Hi\";\nui.layout(\n    <vertical>\n        <text id=\"t\" text=\"{{title}}\"/>\n    </vertical>\n);\n"
        val name = project("bindings", source)
        launch(name).use { scenario ->
            rule.onNodeWithTag("designer-tab-bindings").performClick()
            rule.waitUntil(10_000) { rule.onAllNodesWithTag("designer-ui-binding-title").fetchSemanticsNodes().isNotEmpty() }
            if (DeclarationEvaluator.SUPPORTED) rule.waitUntil(10_000) { scenario.session().previewDeclarations["title"] == "\"Hi\"" }
            rule.onNodeWithTag("designer-ui-binding-value-title").performScrollTo().performTextReplacement("\"Yo\"")
            rule.waitUntil(5_000) { scenario.session().bindingOverrides["title"] == "\"Yo\"" }
            rule.onNodeWithTag("designer-ui-binding-value-title").performTextReplacement("not json")
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-ui-binding-invalid-title").fetchSemanticsNodes().isNotEmpty() }
            assertEquals("\"Yo\"", scenario.session().bindingOverrides["title"])
            rule.onNodeWithTag("designer-ui-binding-reset-title").performScrollTo().performClick()
            rule.waitUntil(5_000) { scenario.session().bindingOverrides.isEmpty() }
            assertFalse(scenario.session().dirty)
            assertEquals(source, scenario.session().script())
        }
        assertEquals(source, store.read(name))
        assertFalse(store.draftFile(name).exists())
    }

    @Test
    fun addingAnEventAppendsAStubAndUndoRemovesIt() {
        val name = project("events")
        launch(name).use { scenario ->
            rule.onNodeWithTag("designer-xml-outline-3").performClick()
            rule.onNodeWithTag("designer-tab-inspector").performClick()
            // The template's click handler is already in the script; a long click is not.
            rule.onNodeWithTag("designer-ui-event-click").performScrollTo().assertIsDisplayed()
            assertTrue(rule.onAllNodesWithTag("designer-ui-event-add-click").fetchSemanticsNodes().isEmpty())
            // The add buttons sit at the right end of the drawer's last rows, under the floating add button: click by semantics.
            rule.onNodeWithTag("designer-ui-event-add-long_click").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
            rule.waitUntil(5_000) { UiHandler("ok", "long_click") in scenario.session().document.handlers }
            val script = scenario.session().script()
            val expected = ProjectStore.UI_TEMPLATE.trimEnd('\n') + "\n\nui.ok.on('long_click', (event, view) => {\n    // TODO\n});\n"
            assertEquals(expected, script)
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("designer-ui-event-add-long_click").fetchSemanticsNodes().isEmpty() }
            // An element without an id gets one with its stub.
            rule.onNodeWithTag("designer-add").performClick()
            rule.onNodeWithTag("designer-palette").performScrollToNode(hasTestTag("designer-palette-text"))
            rule.onNodeWithTag("designer-palette-text").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("designer-ui-event-add-click").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
            rule.waitUntil(5_000) { UiHandler("text1", "click") in scenario.session().document.handlers }
            val withText = scenario.session().script()
            assertTrue(withText, withText.contains("<text id=\"text1\" text=\"Text\"/>"))
            assertTrue(withText, withText.indexOf("ui.ok.on('long_click'") < withText.indexOf("ui.text1.on('click', () => {"))
            repeat(3) { scenario.onActivity { it.currentUiSession!!.undo() } }
            rule.waitForIdle()
            assertEquals(ProjectStore.UI_TEMPLATE, scenario.session().script())
        }
    }

    @Test
    fun previewDifferencesAreListedAsInformationAndNotCounted() {
        val source = "\"ui\";\nui.layout(\n    <vertical>\n        <button id=\"ok\" text=\"OK\"/>\n    </vertical>\n);\nui.statusBarColor('#000000');\nui.ok.on('click', () => dialogs.alert('Hi'));\n"
        val name = project("differences", source)
        launch(name).use { scenario ->
            assertEquals(listOf(PreviewDifferenceKind.WINDOW_COLOR to 7, PreviewDifferenceKind.DIALOGS to 8), scenario.session().visibleDifferences.map { it.kind to it.line })
            lateinit var issues: String
            lateinit var header: String
            lateinit var colorText: String
            lateinit var dialogsText: String
            scenario.onActivity {
                issues = it.getString(R.string.designer_issues_count, 0)
                header = it.getString(R.string.designer_ui_diff_header_layout)
                colorText = it.getString(R.string.designer_ui_diff_window_color, "ui.statusBarColor")
                dialogsText = it.getString(R.string.designer_ui_diff_dialogs)
            }
            // The menu counts errors only: two differences, zero problems.
            rule.onNodeWithTag("designer-menu").performClick()
            rule.onNodeWithTag("designer-menu-issues").assert(hasText(issues))
            rule.onNodeWithTag("designer-menu-issues").performClick()
            rule.waitForIdle()
            val list = rule.onNodeWithTag("designer-xml-problems")
            list.performScrollToNode(hasTestTag("designer-ui-diff-header"))
            rule.onNodeWithTag("designer-ui-diff-header").assert(hasText(header))
            val color = hasTestTag("designer-ui-diff") and hasText("ui.statusBarColor", substring = true)
            list.performScrollToNode(color)
            rule.onNode(color).assert(hasText(colorText, substring = true))
            val dialogs = hasTestTag("designer-ui-diff") and hasText("dialogs.alert", substring = true)
            list.performScrollToNode(dialogs)
            rule.onNode(dialogs).assert(hasText(dialogsText, substring = true))
            // A tap hands the documentation section to a browser; the monitor answers instead of launching one.
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val browser = IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addDataScheme("https")
                addDataAuthority("docs.autojs6.com", null)
            }
            val monitor = instrumentation.addMonitor(browser, Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
            try {
                rule.onNode(dialogs).performClick()
                rule.waitUntil(5_000) { monitor.hits == 1 }
            } finally {
                instrumentation.removeMonitor(monitor)
            }
            assertEquals(0, scenario.session().diagnostics.size)
            assertFalse(scenario.session().dirty)
        }
    }

    @Test
    fun anUnmappableScriptShowsItsErrorPosition() {
        val name = project("error", "\"ui\";\nvar x = 1;\nui.layout(x);\n")
        launch(name).use {
            rule.onNodeWithTag("designer-parse-error").assertIsDisplayed()
            rule.onNodeWithTag("designer-parse-error").assert(hasText("ui.layout(x)", substring = true))
        }
    }

    @Test
    fun everyBundledHostSampleOpensWithoutAParseError() {
        val samples = listOf("android-layout.js", "buttons.js", "card.js", "console.js", "inputs.js", "list.js", "login.js", "spinner.js", "survey.js", "template.js", "todo.js", "widget-template.js")
        samples.forEach { sample ->
            val text = requireNotNull(javaClass.classLoader?.getResourceAsStream(sample)) { "missing test resource $sample" }.use { it.readBytes().toString(StandardCharsets.UTF_8) }
            val name = project("sample-" + sample.removeSuffix(".js"), text)
            launch(name).use { scenario ->
                rule.onNodeWithTag("designer-xml-outline").assertIsDisplayed()
                assertTrue("$sample shows no parse error", rule.onAllNodesWithTag("designer-parse-error").fetchSemanticsNodes().isEmpty())
                assertEquals("$sample is written back unchanged", text.replace("\r\n", "\n"), scenario.session().script())
            }
        }
    }
}
