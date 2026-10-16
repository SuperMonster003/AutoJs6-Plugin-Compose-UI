package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.JsLiteral
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PropValue
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.LiveTree
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxEngine
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxFailure
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxLimits
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxOutput
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxTrees
import org.autojs.plugin.compose.api.model.UiCommand
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Roadmap P9.3 step 2 (D43): the designer's sandbox. Scripts never reach Java, a runaway script is cut off within
 * its limits, and the preview implementation of `compose` behaves like the host on the counter, form and list
 * examples: state writes re-render, keyed and positional nodes keep their identities, handles read and write
 * properties, and commands, refs and field edits follow the host's rules.
 */
class SandboxEngineTest {
    private val catalog = SandboxCatalog.json(ComposeUiV2.interopCatalog, mapOf("plugin" to "test", "contract" to 0, "compose" to "test"))

    private fun engine(limits: SandboxLimits = SandboxLimits()) = SandboxEngine(catalog, limits)

    private fun SandboxOutput.tree(): LiveTree = SandboxTrees.parse(checkNotNull(snapshot) { "no tree; failure: $failure, console: $console" })

    private fun LiveTree.nodes(): List<DesignNode> = document.nodes().toList()
    private fun LiveTree.byTag(tag: String): DesignNode = nodes().single { (it.prop("testTag") as? PropValue.Literal)?.value == JsLiteral.Str(tag) }
    private fun LiveTree.byKey(key: String): DesignNode = nodes().single { (it.key as? PropValue.Literal)?.value == JsLiteral.Str(key) }
    private fun DesignNode.text(): String? = ((prop("text") as? PropValue.Literal)?.value as? JsLiteral.Str)?.value
    private fun DesignNode.literal(name: String): JsLiteral? = (prop(name) as? PropValue.Literal)?.value

    private fun example(name: String): String = Files.readString(projectRoot().resolve("app/src/main/assets/examples/$name"))

    private fun projectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }

    // ------------------------------------------------------------------ isolation

    @Test
    fun scriptsCannotReachJava() {
        listOf(
            "java.lang.System.exit(0)",
            "Packages.java.io.File",
            "importClass(java.io.File)",
            "new JavaAdapter(Object, {})",
            "this.constructor.constructor('return java')().lang",
            "Function('return Packages')().java",
            "org.mozilla.javascript.Context.getCurrentContext()",
            "android.os.Process.killProcess(0)",
        ).forEach { source ->
            val output = engine().load(source, "probe.js")
            val failure = assertNotNull("$source must fail", output.failure).let { output.failure!! }
            assertEquals(source, SandboxFailure.Kind.SCRIPT, failure.kind)
        }
        // The Java-shaped globals that exist are stand-ins whose every use throws.
        val output = engine().load("""
            var names = Object.getOwnPropertyNames(this).filter(function (n) { return /^(Packages|JavaAdapter|JavaImporter|getClass|java|javax|org|com|android)$/.test(n) });
            names.forEach(function (n) { try { this[n].lang; console.log('reachable ' + n); } catch (e) { console.log('blocked ' + n); } }, this);
        """.trimIndent(), "globals.js")
        assertNull(output.failure)
        assertTrue(output.console.map { it.text }.toString(), output.console.isNotEmpty() && output.console.none { it.text.startsWith("reachable") })
    }

    @Test
    fun aScriptMayStillDeclareTheNamesOfAutoJsModules() {
        val output = engine().load("var colors = ['red']; function ui() { return 'mine'; } let files = 2;\nconsole.log(colors[0], ui(), files);", "names.js")
        assertNull(output.failure)
        assertEquals(listOf("red mine 2"), output.console.map { it.text })
    }

    @Test
    fun otherAutoJsModulesReportThatTheyAreNotAvailable() {
        val output = engine().load("app.launch('com.example')", "app.js")
        assertEquals(SandboxFailure.Kind.SCRIPT, output.failure?.kind)
        assertTrue(output.failure!!.message, "app is not available in the designer preview" in output.failure!!.message)
        assertEquals(1, output.failure!!.line)
    }

    // ------------------------------------------------------------------ limits

    @Test
    fun anEndlessLoopIsStoppedByTheClock() {
        val engine = engine(SandboxLimits(instructions = Long.MAX_VALUE, millis = 300))
        val started = System.nanoTime()
        val output = engine.load("console.log('before'); try { while (true) {} } catch (e) { console.log('caught') } finally { console.log('finally') }", "loop.js")
        val elapsed = (System.nanoTime() - started) / 1_000_000
        assertEquals(SandboxFailure.Kind.TIME_LIMIT, output.failure?.kind)
        assertTrue("stopped after $elapsed ms", elapsed < 3_000)
        // Neither catch nor finally runs; what was logged before still shows where it stopped.
        assertEquals(listOf("before"), output.console.map { it.text })
        assertTrue(engine.stopped)
        assertTrue(engine.dispatch(1, "click", emptyList()).let { it.snapshot == null && it.failure == null })
    }

    @Test
    fun anEndlessLoopIsStoppedByTheInstructionBudget() {
        val engine = engine(SandboxLimits(instructions = 2_000_000, millis = 60_000))
        val output = engine.load("var n = 0; for (;;) { n++; }", "count.js")
        assertEquals(SandboxFailure.Kind.INSTRUCTION_LIMIT, output.failure?.kind)
        assertTrue(engine.stopped)
    }

    @Test
    fun anEndlessLoopInAnEventHandlerIsStoppedToo() {
        val engine = engine(SandboxLimits(millis = 300))
        val loaded = engine.load("compose.mount(compose.Button({key: 'b', onClick: function () { while (true) {} }}, 'Go'));", "handler.js")
        val button = loaded.tree().byKey("b")
        val output = engine.dispatch(button.id, "click", emptyList())
        assertEquals(SandboxFailure.Kind.TIME_LIMIT, output.failure?.kind)
        assertTrue(engine.stopped)
    }

    @Test
    fun deepRecursionIsAScriptErrorTheSandboxSurvives() {
        val engine = engine()
        val output = engine.load("function f(n) { return f(n + 1); }\nf(0);", "recursion.js")
        assertEquals(SandboxFailure.Kind.SCRIPT, output.failure?.kind)
        assertFalse(engine.stopped)
        assertNull(engine.dispatch(1, "click", emptyList()).failure)
    }

    // ------------------------------------------------------------------ console, timers, errors

    @Test
    fun consoleToastAndTimersWork() {
        val engine = engine()
        val output = engine.load("""
            var n = compose.state(0);
            console.log('a', {b: 1}); log('c'); toast('hello'); console.error('bad');
            setTimeout(function () { n.value = 5; console.log('timer'); }, 10);
            compose.mount(function () { return compose.Text({key: 't', text: 'n=' + n.value}); });
        """.trimIndent(), "console.js")
        assertNull(output.failure)
        assertEquals(listOf("log" to "a {\"b\":1}", "log" to "c", "toast" to "hello", "error" to "bad"), output.console.map { it.level to it.text })
        assertEquals("n=0", output.tree().byKey("t").text())
        assertTrue(engine.nextTimer() in 0..10)
        Thread.sleep(30)
        val fired = engine.runTimers()
        assertEquals("n=5", fired.tree().byKey("t").text())
        assertEquals(listOf("timer"), fired.console.map { it.text })
        assertEquals(-1, engine.nextTimer())
    }

    @Test
    fun aHandlerErrorIsReportedAndLaterEventsStillWork() {
        val engine = engine()
        val loaded = engine.load("""
            var n = compose.state(0);
            compose.mount(function () {
                return compose.Column(
                    compose.Button({key: 'bad', onClick: function () { n.value++; undefinedFunction(); }}, 'Bad'),
                    compose.Text({key: 't', text: 'n=' + n.value}));
            });
        """.trimIndent(), "errors.js")
        val bad = loaded.tree().byKey("bad")
        val failed = engine.dispatch(bad.id, "click", emptyList())
        assertEquals(SandboxFailure.Kind.SCRIPT, failed.failure?.kind)
        assertEquals(4, failed.failure?.line)
        // The write before the error still renders, as a host transaction would have published it.
        assertEquals("n=1", failed.tree().byKey("t").text())
        assertFalse(engine.stopped)
    }

    // ------------------------------------------------------------------ host semantics on the examples

    @Test
    fun counterExampleRendersAndKeepsIdentities() {
        val engine = engine()
        val first = engine.load(example("counter.js"), "counter.js").tree()
        assertEquals("Count: 0", first.byTag("counter-value").text())
        val increment = first.byTag("counter-increment")
        assertEquals(listOf("Increment"), increment.children.map { (it as io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild.Node).node.text() })
        assertEquals(listOf("click"), first.hooks.events[increment.id])

        val second = engine.dispatch(increment.id, "click", emptyList()).tree()
        assertEquals("Count: 1", second.byTag("counter-value").text())
        // Reconciliation reuses every node's identity, by key and by type and position (the button's label).
        assertEquals(first.nodes().map { it.id to it.type }, second.nodes().map { it.id to it.type })

        engine.dispatch(increment.id, "click", emptyList())
        val reset = engine.dispatch(second.byTag("counter-reset").id, "click", emptyList()).tree()
        assertEquals("Count: 0", reset.byTag("counter-value").text())
        // A click that changes nothing publishes nothing.
        assertNull(engine.dispatch(second.byTag("counter-reset").id, "click", emptyList()).snapshot)
    }

    @Test
    fun formExampleUpdatesHandlesFieldsAndCommands() {
        val engine = engine()
        val first = engine.load(example("form.js"), "form.js").tree()
        val name = first.byTag("form-name")
        assertEquals("", name.text())
        assertEquals("Not saved", first.byTag("form-result").text())
        assertTrue(name.slot("label") != null)

        // Saving without a name marks the field and focuses it.
        val save = first.byTag("form-save")
        val empty = engine.dispatch(save.id, "click", emptyList()).tree()
        assertEquals("Enter your name", empty.byTag("form-result").text())
        assertEquals(JsLiteral.Bool(true), empty.byTag("form-name").literal("isError"))
        assertEquals(listOf<UiCommand>(UiCommand.Focus(name.id)), empty.hooks.commands)

        // Typing reaches the handler as (text, selection, editSeq); the field keeps the typed text and its sequence.
        val typed = engine.dispatch(name.id, "valueChange", listOf("Ada", mapOf("start" to 3, "end" to 3), 1L)).tree()
        assertEquals("Ready to save", typed.byTag("form-result").text())
        assertEquals("Ada", typed.byTag("form-name").text())
        assertEquals(1L, typed.hooks.fieldSequences[name.id])
        assertEquals(JsLiteral.Bool(false), typed.byTag("form-name").literal("isError"))

        val switch = first.byTag("form-notifications")
        assertEquals(JsLiteral.Bool(true), engine.dispatch(switch.id, "checkedChange", listOf(true)).tree().byTag("form-notifications").literal("checked"))
        val slider = first.byTag("form-level")
        assertEquals("Level: 75", engine.dispatch(slider.id, "valueChange", listOf(75.0)).tree().byTag("form-level-label").text())

        val saved = engine.dispatch(save.id, "click", emptyList()).tree()
        assertEquals("Saved: Ada | Notifications: on | Level: 75", saved.byTag("form-result").text())
        assertEquals(listOf<UiCommand>(UiCommand.Blur(name.id)), saved.hooks.commands)
    }

    @Test
    fun listExampleReordersByKeyAndScrollsThroughTheRef() {
        // The test is about identities, not speed: a cold JVM on a slow CI runner can take longer than the preview's
        // three seconds to build 1000 items (2026-10-16, run 38890301577), so the wall clock gets room here.
        val engine = engine(SandboxLimits(millis = 30_000L))
        val first = engine.load(example("list.js"), "list.js").tree()
        val list = first.byTag("list-items")
        assertEquals(1000, list.children.size)
        val ids = first.nodes().filter { it.key != null }.associate { (it.key as PropValue.Literal).value to it.id }
        assertEquals("Order: forward", first.byTag("list-order").text())

        val reversed = engine.dispatch(first.byTag("list-reverse").id, "click", emptyList()).tree()
        assertEquals("Order: reverse", reversed.byTag("list-order").text())
        val items = reversed.byTag("list-items").children.map { (it as io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild.Node).node }
        assertEquals("Item 999", items.first().text())
        // Every keyed node keeps its identity across the reordering.
        reversed.nodes().filter { it.key != null }.forEach { node -> assertEquals(ids[(node.key as PropValue.Literal).value], node.id) }

        // The ref holds the list handle once a tree was drawn; scrollTo becomes a command for the canvas.
        val jumped = engine.dispatch(first.byTag("list-jump").id, "click", emptyList())
        assertNull(jumped.failure)
        assertEquals(listOf<UiCommand>(UiCommand.ScrollTo(list.id, 99, null)), jumped.tree().hooks.commands)
    }

    @Test
    fun modifierCallbacksGetTokensAndRun() {
        val engine = engine()
        val loaded = engine.load("""
            var n = compose.state(0);
            compose.mount(function () {
                return compose.Box({key: 'box', modifier: compose.modifier().padding(8).clickable(function () { n.value++; })},
                    compose.Text({key: 't', text: 'n=' + n.value}));
            });
        """.trimIndent(), "tokens.js").tree()
        val box = loaded.byKey("box")
        val token = loaded.hooks.tokens[box.id]!!.single()
        assertTrue(token >= 2_000_000)
        assertEquals("n=1", engine.dispatch(token, "click", emptyList()).tree().byKey("t").text())
    }

    @Test
    fun composeErrorsMatchTheHostChecks() {
        mapOf(
            "compose.createElement('Nope', {})" to "UNKNOWN_COMPONENT",
            "compose.Text({colour: 'red'})" to "UNKNOWN_PROP",
            "compose.Text({key: {}})" to "INVALID_ARGUMENT",
            "compose.Text({modifier: 1})" to "INVALID_MODIFIER",
            "compose.memo(function () { return compose.Text('x'); })" to "INVALID_ARGUMENT",
            "var a = compose.Column(); a.append(a)" to "INVALID_ARGUMENT",
        ).forEach { (source, code) ->
            val output = engine().load("try { $source } catch (e) { console.log(e.name + ' ' + e.code); }", "errors.js")
            assertEquals(source, listOf("ComposeError $code"), output.console.map { it.text })
        }
    }
}
