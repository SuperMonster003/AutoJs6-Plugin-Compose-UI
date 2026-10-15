package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewCapability
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifference
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifferenceKind
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewDifferences
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiEditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.PreviewDifferenceScanner
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.UiDesignerSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Roadmap P11.2 / D55: the scanner lists the lines of a ui script that behave differently in the designer's preview
 * (member access on `activity`, `ui.emitter`, the window-level `ui` functions, dialogs, endless loops on the main
 * thread, threads and events) with their positions; host capabilities hide what they close; the session rescans
 * after every edit, so positions follow the inserted lines.
 */
class PreviewDifferencesTest {
    private val root: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }

    private fun sample(name: String): String = Files.readString(root.resolve("app/src/test/resources/ui-samples/$name")).replace("\r\n", "\n")

    private fun List<PreviewDifference>.lines(): List<String> = map { "${it.line}:${it.column} ${it.kind} ${it.api}" }

    private fun scan(source: String): List<String> = PreviewDifferenceScanner.scan(source).lines()

    @Test
    fun `the bundled host samples have these differences`() {
        val expected = mapOf(
            "android-layout.js" to listOf(
                "17:5 WINDOW_NO_OP ui.keepScreenOn",
                "19:5 WINDOW_COLOR ui.backgroundColor",
                "21:5 WINDOW_COLOR ui.statusBarColor",
                "22:5 WINDOW_NO_OP ui.statusBarIconLightBy",
                "24:5 WINDOW_COLOR ui.navigationBarColor",
                "26:9 WINDOW_NO_OP ui.navigationBarIconLightBy",
            ),
            // The endless loop reading input runs in threads.start, and console.rawInput is not a dialog.
            "console.js" to listOf("26:1 THREADS threads.start"),
            "template.js" to listOf(
                "38:1 ACTIVITY_EVENTS ui.emitter.on('create_options_menu')",
                "43:1 ACTIVITY_EVENTS ui.emitter.on('options_item_selected')",
                "49:13 DIALOGS alert",
                "54:1 ACTIVITY activity.setSupportActionBar",
                "86:13 WINDOW_NO_OP ui.finish",
            ),
            "todo.js" to listOf(
                "80:1 WINDOW_COLOR ui.statusBarColor",
                "115:5 DIALOGS confirm",
                "125:1 LIFECYCLE ui.emitter.on('pause')",
                "128:5 DIALOGS dialogs.rawInput",
                "133:13 DIALOGS dialogs.rawInput",
            ),
        )
        val samples = listOf("android-layout.js", "buttons.js", "card.js", "console.js", "inputs.js", "list.js", "login.js", "spinner.js", "survey.js", "template.js", "todo.js", "widget-template.js")
        samples.forEach { name -> assertEquals(name, expected[name].orEmpty(), scan(sample(name))) }
    }

    @Test
    fun `each difference has the position of its expression and one entry per kind and line`() {
        val source = "\"ui\";\nui.layout(<vertical/>);\n  dialogs.alert('a'); dialogs.confirm('b'); ui.statusBarColor('#000');\nvar a = activity.getIntent();\n"
        assertEquals(
            listOf("3:3 DIALOGS dialogs.alert", "3:45 WINDOW_COLOR ui.statusBarColor", "4:9 ACTIVITY activity.getIntent"),
            scan(source),
        )
    }

    @Test
    fun `globals that the script declares itself are not differences`() {
        val source = """
            "ui";
            let events = [];
            events.push(1);
            function alert(text) { return text; }
            alert('x');
            ui.layout(<vertical/>);
            ui.list.on('item_bind', function (itemView, dialogs) { dialogs.show(); });
            threads.start(() => {});
        """.trimIndent()
        assertEquals(listOf("8:1 THREADS threads.start"), scan(source))
    }

    @Test
    fun `ui emitter splits lifecycle events from other activity events`() {
        val source = """
            "ui";
            ui.layout(<vertical/>);
            ui.emitter.on('resume', () => {});
            ui.emitter.once('back_pressed', e => {});
            ui.emitter.addListener('activity_result', () => {});
            let emitter = ui.emitter;
            ui.emitter.on(name, () => {});
        """.trimIndent()
        assertEquals(
            listOf(
                "3:1 LIFECYCLE ui.emitter.on('resume')",
                "4:1 LIFECYCLE ui.emitter.once('back_pressed')",
                "5:1 ACTIVITY_EVENTS ui.emitter.addListener('activity_result')",
                "6:15 ACTIVITY_EVENTS ui.emitter",
                "7:1 ACTIVITY_EVENTS ui.emitter",
            ),
            scan(source),
        )
    }

    @Test
    fun `endless loops on the main thread are busy waits and loops that can end or run in threads are not`() {
        val source = """
            "ui";
            ui.layout(<vertical/>);
            while (true) { log(1); }
            for (;;) { continue; }
            do { log(2); } while (true);
            while (1) { for (let i = 0; i < 3; i++) { if (i) break; } }
            while (true) { switch (x) { case 1: break; } }
            setInterval(() => { while ((true)) {} }, 1000);
            while (true) { if (x) break; }
            outer: while (true) { for (;;) { break outer; } }
            function poll() { while (true) { if (x) return; } }
            ui.ok.on('click', () => { while (true) { throw new Error('x'); } });
            while (x) {}
            for (let i = 0; ; i++) { if (i > 3) break; }
            threads.start(function () { while (true) {} });
            function worker() { while (true) {} }
            let other = () => { for (;;) {} };
            threads.start(worker);
            threads.start(other);
        """.trimIndent()
        assertEquals(
            listOf(
                "3:1 BUSY_WAIT while (true)",
                "4:1 BUSY_WAIT for (;;)",
                "5:1 BUSY_WAIT do ... while (true)",
                "6:1 BUSY_WAIT while (1)",
                "7:1 BUSY_WAIT while (true)",
                "8:21 BUSY_WAIT while ((true))",
                "15:1 THREADS threads.start",
                "18:1 THREADS threads.start",
                "19:1 THREADS threads.start",
            ),
            scan(source),
        )
    }

    @Test
    fun `a script that does not parse has no differences`() {
        assertEquals(emptyList<String>(), scan("\"ui\";\nui.layout(<vertical/>);\ndialogs.alert(\n"))
    }

    @Test
    fun `host capabilities hide the differences they close`() {
        val differences = PreviewDifferenceScanner.scan(sample("todo.js") + "\nwhile (true) {}\nactivity.finish();\n")
        val all = differences.map { it.kind }.toSet()
        assertEquals(
            setOf(PreviewDifferenceKind.WINDOW_COLOR, PreviewDifferenceKind.DIALOGS, PreviewDifferenceKind.LIFECYCLE, PreviewDifferenceKind.BUSY_WAIT, PreviewDifferenceKind.ACTIVITY),
            all,
        )
        assertEquals(differences, PreviewDifferences.visible(differences, emptySet()))
        val v3 = PreviewDifferences.visible(differences, PreviewCapability.entries.toSet())
        assertEquals(setOf(PreviewDifferenceKind.ACTIVITY), v3.map { it.kind }.toSet())
        val dialogsOnly = PreviewDifferences.visible(differences, setOf(PreviewCapability.DIALOGS_IN_CANVAS))
        assertEquals(all - PreviewDifferenceKind.DIALOGS, dialogsOnly.map { it.kind }.toSet())
        // Every kind is either closed by one capability or by none, and each capability closes something.
        assertEquals(PreviewCapability.entries.toSet(), PreviewDifferenceKind.entries.mapNotNull { it.reducedBy }.toSet())
    }

    @Test
    fun `the session rescans after an edit and its line numbers follow inserted lines`() {
        val source = "\"ui\";\nui.layout(\n    <vertical>\n        <text id=\"a\" text=\"A\"/>\n    </vertical>\n);\nui.statusBarColor('#000');\ndialogs.alert('x');\n"
        val session = UiDesignerSession(UiScriptReader().read(source))
        assertEquals(listOf("7:1 WINDOW_COLOR ui.statusBarColor", "8:1 DIALOGS dialogs.alert"), session.visibleDifferences.lines())
        val vertical = requireNotNull(session.root)
        assertTrue(session.execute(UiEditCommand.InsertElement(vertical.id, 1, UiTemplates.create("button")), merge = false))
        assertEquals(listOf("8:1 WINDOW_COLOR ui.statusBarColor", "9:1 DIALOGS dialogs.alert"), session.visibleDifferences.lines())
        session.hostCapabilities = setOf(PreviewCapability.WINDOW_FUNCTIONS_VISIBLE)
        assertEquals(listOf("9:1 DIALOGS dialogs.alert"), session.visibleDifferences.lines())
        assertTrue(session.undo())
        assertEquals(listOf("8:1 DIALOGS dialogs.alert"), session.visibleDifferences.lines())
        assertEquals("the differences are not problems", 0, session.diagnostics.size)
    }
}
