package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/** Roadmap P10.1: the validator accepts the host samples and names what the host would reject or ignore. */
class UiValidatorTest {
    private val reader = UiScriptReader()
    private val validator = UiValidator()

    private fun diagnostics(layout: String) = validator.validate(reader.read("\"ui\";\nui.layout(\n$layout\n);\n"))

    @Test
    fun `the bundled samples have no errors`() {
        val root = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }
        val dir = root.resolve("app/src/test/resources/ui-samples")
        Files.list(dir).use { stream ->
            stream.filter { it.toString().endsWith(".js") }.forEach { path ->
                val document = reader.read(Files.readString(path).replace("\r\n", "\n"))
                val errors = validator.validate(document).filter { it.severity == Severity.ERROR }
                assertEquals("${path.fileName}: $errors", emptyList<Any>(), errors)
            }
        }
    }

    @Test
    fun `unknown tags and attributes`() {
        val unknownTag = diagnostics("<vertical><bogus/></vertical>")
        assertTrue(unknownTag.any { it.severity == Severity.ERROR && it.message.contains("Unknown tag <bogus>") })
        val unknownAttr = diagnostics("<text frobnicate=\"1\"/>")
        assertTrue(unknownAttr.any { it.severity == Severity.WARNING && it.target == "frobnicate" })
        val classTag = diagnostics("<android.widget.TextView text=\"a\"/>")
        assertTrue("known class names are accepted without warnings: $classTag", classTag.isEmpty())
        val guessed = diagnostics("<ConstraintLayout><text text=\"a\"/></ConstraintLayout>")
        assertTrue(guessed.any { it.severity == Severity.WARNING && it.message.contains("ViewGroup") })
    }

    @Test
    fun `value formats`() {
        assertTrue(diagnostics("<text textSize=\"big\"/>").any { it.severity == Severity.ERROR && it.target == "textSize" })
        assertTrue(diagnostics("<text size=\"16sp\" w=\"*\" h=\"auto\" padding=\"8 4\" margin=\"0 50\"/>").isEmpty())
        assertTrue(diagnostics("<text textColor=\"#ff0000\" bg=\"#80000000\"/>").isEmpty())
        assertTrue(diagnostics("<text textColor=\"red\"/>").any { it.severity == Severity.WARNING && it.message.contains("Named color") })
        assertTrue(diagnostics("<text textColor=\"nope!\"/>").any { it.severity == Severity.ERROR })
        assertTrue(diagnostics("<text gravity=\"center|left\"/>").isEmpty())
        assertTrue(diagnostics("<text gravity=\"middle\"/>").any { it.severity == Severity.ERROR && it.target == "gravity" })
        assertTrue(diagnostics("<checkbox checked=\"yes\"/>").any { it.severity == Severity.ERROR && it.target == "checked" })
        assertTrue(diagnostics("<tabs tabMode=\"sticky\"/>").any { it.severity == Severity.ERROR && it.message.contains("fixed, scrollable") })
        assertTrue(diagnostics("<text text=\"{{name}} and {{age}}\" textSize=\"{{size}}\"/>").isEmpty())
        assertTrue(diagnostics("<text text=\"{{}}\"/>").any { it.severity == Severity.ERROR && it.message.contains("Empty") })
        assertTrue(diagnostics("<console global/>").isEmpty())
    }

    @Test
    fun `structure rules`() {
        assertTrue(diagnostics("<list id=\"l\"/>").any { it.severity == Severity.ERROR && it.target == "children" })
        assertTrue(diagnostics("<list id=\"l\"><text/><text/></list>").any { it.severity == Severity.ERROR && it.target == "children" })
        assertTrue(diagnostics("<list id=\"l\"><text text=\"{{name}}\"/></list>").isEmpty())
        assertTrue(diagnostics("<text><text/></text>").any { it.severity == Severity.WARNING && it.target == "children" })
        assertTrue(diagnostics("<vertical><text id=\"a\"/><text id=\"a\"/></vertical>").count { it.message.contains("Duplicate id") } == 2)
        assertTrue(diagnostics("<vertical><text alignParentBottom=\"true\"/></vertical>").any { it.severity == Severity.WARNING && it.message.contains("relative") })
        assertTrue(diagnostics("<relative><text alignParentBottom=\"true\"/></relative>").isEmpty())
        assertTrue(diagnostics("<text onClick=\"go\"/>").any { it.severity == Severity.ERROR && it.target == "onClick" })
        assertTrue(diagnostics("<button isBorderless=\"true\" text=\"b\"/>").isEmpty())
    }
}
