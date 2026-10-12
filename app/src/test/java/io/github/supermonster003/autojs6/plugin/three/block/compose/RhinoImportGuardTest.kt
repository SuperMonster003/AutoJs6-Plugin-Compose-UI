package io.github.supermonster003.autojs6.plugin.three.block.compose

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.streams.toList

/**
 * Roadmap P8.1 / D35: the slim Rhino jar is a parser for the designer, nothing else. Only the designer's
 * parser package may import org.mozilla.javascript, and no main source may enter a Rhino Context or build a
 * ContextFactory, so the plugin never evaluates a script (D8 keeps "does not execute scripts").
 */
class RhinoImportGuardTest {

    private val root: Path = findProjectRoot()
    private val allowedPackageDir = "/designer/parse/"

    @Test
    fun `only the designer parser package imports Rhino`() {
        val offenders = mainSources()
            .filter { path -> Files.readString(path).contains("org.mozilla.javascript") }
            .filterNot { path -> path.pathString.replace('\\', '/').contains(allowedPackageDir) }
            .map { root.relativize(it).pathString }
        assertEquals("Rhino may only be referenced from designer/parse", emptyList<String>(), offenders)
    }

    @Test
    fun `no main source enters a Rhino context`() {
        val forbidden = listOf("Context.enter(", "ContextFactory", "Context.call(", "evaluateString(", "compileString(")
        val offenders = mainSources()
            .filter { path -> Files.readString(path).let { text -> text.contains("org.mozilla.javascript") && forbidden.any { it in text } } }
            .map { root.relativize(it).pathString }
        assertEquals("the designer parses scripts and never executes them", emptyList<String>(), offenders)
    }

    private fun mainSources(): List<Path> = Files.walk(root.resolve("app/src/main/java")).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.extension in setOf("kt", "java") }.toList()
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
