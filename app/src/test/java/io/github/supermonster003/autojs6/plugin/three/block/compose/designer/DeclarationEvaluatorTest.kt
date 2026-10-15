package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.DeclarationEvaluator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P10.3: the evaluator runs only the value-like top-level declarations of a ui script, in order, under
 * the sandbox's rules, and then the E4X `{expr}` texts; everything else is skipped or reported, never executed.
 */
class DeclarationEvaluatorTest {
    private val evaluator = DeclarationEvaluator()

    @Test
    fun `value-like declarations are picked in order and the rest is skipped`() {
        val script = """
            "ui";
            var title = "Hello";
            let count = 2 + 3;
            const names = ['a', 'b'].concat(['c']);
            var config = { size: "20sp", bold: true, inner: { x: 1 } };
            var later = count * 2;
            var template = `${'$'}{title} ${'$'}{count}`;
            var pi = Math.round(Math.PI * 100);
            var parsed = JSON.parse('{"k": 1}').k;
            var fromFile = files.read("x.txt");
            var fn = function () { return 1; };
            var arrow = () => 2;
            var called = fn();
            var picked = config.inner.x;
            var ternary = count > 3 ? "big" : "small";
            var indexed = names[0];
            var negative = -count;
            var reassigned = 1;
            reassigned = 2;
            var unknownRef = somethingElse + 1;
            ui.layout(<vertical/>);
        """.trimIndent()
        val declarations = DeclarationEvaluator.declarationsOf(script).map { it.name }
        assertEquals(listOf("title", "count", "config", "later", "template", "pi", "parsed", "picked", "ternary", "negative", "reassigned"), declarations)
        assertFalse("array methods are calls outside Math / JSON", "names" in declarations)
        assertFalse("module calls never run", "fromFile" in declarations)
        assertFalse("functions are skipped", "fn" in declarations)
        assertFalse("calls of script functions are skipped", "called" in declarations)
        assertFalse("a reference to a skipped declaration is skipped", "indexed" in declarations)
        assertFalse("unknown names are skipped", "unknownRef" in declarations)
    }

    @Test
    fun `declarations evaluate to JSON and expressions to strings`() {
        val script = """
            "ui";
            var title = "Hello";
            var count = 2 + 3;
            var config = { size: "20sp", list: [1, 2] };
            var later = count * 2;
            var template = `${'$'}{title}!`;
            var pi = Math.round(Math.PI * 100);
            var broken = JSON.parse("{");
            ui.layout(<vertical/>);
        """.trimIndent()
        val result = evaluator.evaluate(script, listOf("title", "later + 1", "config.size", "missing", "count > 2 ? 'yes' : 'no'"))
        assertEquals("\"Hello\"", result.declarations["title"])
        assertEquals("5", result.declarations["count"])
        assertEquals("{\"size\":\"20sp\",\"list\":[1,2]}", result.declarations["config"])
        assertEquals("10", result.declarations["later"])
        assertEquals("\"Hello!\"", result.declarations["template"])
        assertEquals("314", result.declarations["pi"])
        assertFalse("a failing declaration is absent", "broken" in result.declarations)
        assertTrue(result.failures.getValue("broken").contains("SyntaxError") || result.failures.getValue("broken").isNotEmpty())
        assertEquals("Hello", result.expressions["title"])
        assertEquals("11", result.expressions["later + 1"])
        assertEquals("20sp", result.expressions["config.size"])
        assertEquals("yes", result.expressions["count > 2 ? 'yes' : 'no'"])
        assertFalse("missing" in result.expressions)
        assertTrue(result.failures.getValue("missing").contains("missing"))
    }

    @Test
    fun `a script without declarations or a syntax error gives nothing`() {
        assertEquals(DeclarationEvaluator.Result.EMPTY, evaluator.evaluate("\"ui\";\nui.layout(<vertical/>);\n"))
        assertEquals(DeclarationEvaluator.Result.EMPTY, evaluator.evaluate("\"ui\";\nvar x = ;\n", listOf("x")))
    }

    @Test
    fun `the sandbox has no Java and a budget`() {
        val noJava = evaluator.evaluate("var x = 1;", listOf("java.lang.System", "Packages", "typeof java"))
        assertEquals("undefined", noJava.expressions["typeof java"])
        assertFalse("java.lang.System" in noJava.expressions)
        val tight = DeclarationEvaluator(SandboxLimits(instructions = 50_000L, millis = 1_000L, stackDepth = 50))
        val looped = tight.evaluate("var a = 1;", listOf("(function () { var n = 0; while (true) { n++; } })()"))
        assertTrue(looped.failures.toString(), looped.failures.containsKey(""))
        assertTrue(looped.expressions.isEmpty())
    }

    @Test
    fun `later declarations see earlier ones but not later ones`() {
        val result = evaluator.evaluate("var a = 1;\nvar b = a + 1;\nvar c = d + 1;\nvar d = 4;\n")
        assertEquals("1", result.declarations["a"])
        assertEquals("2", result.declarations["b"])
        assertFalse("c" in result.declarations)
        assertEquals("4", result.declarations["d"])
    }
}
