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
 * Roadmap P8.1 / D35, revised for P9.3 step 2 (D43, 2026-10-13): the slim Rhino jar serves the designer only. The
 * parser package reads scripts and never runs them; the sandbox package is the one place that enters a Rhino
 * Context, to run the interactive preview under its limits. No other main source references Rhino, so the
 * renderer, the INFO service and the Wake Activity never touch this copy (it stays apart from the host's Rhino).
 */
class RhinoImportGuardTest {

    private val root: Path = findProjectRoot()
    private val parser = "/designer/parse/"
    private val sandbox = "/designer/sandbox/"

    @Test
    fun `only the designer parser and sandbox packages import Rhino`() {
        val offenders = mainSources()
            .filter { path -> Files.readString(path).contains("org.mozilla.javascript") }
            .filterNot { path -> path.unix().let { it.contains(parser) || it.contains(sandbox) } }
            .map { root.relativize(it).pathString }
        assertEquals("Rhino may only be referenced from designer/parse and designer/sandbox", emptyList<String>(), offenders)
    }

    @Test
    fun `only the sandbox enters a Rhino context`() {
        val forbidden = listOf("Context.enter(", "ContextFactory", "enterContext(", "Context.call(", "evaluateString(", "compileString(", "initStandardObjects(", "initSafeStandardObjects(")
        val offenders = mainSources()
            .filter { path -> Files.readString(path).let { text -> text.contains("org.mozilla.javascript") && forbidden.any { it in text } } }
            .filterNot { path -> path.unix().contains(sandbox) }
            .map { root.relativize(it).pathString }
        assertEquals("the parser reads scripts and never executes them; only the sandbox runs them", emptyList<String>(), offenders)
    }

    @Test
    fun `the sandbox uses safe standard objects and a class shutter`() {
        val engine = Files.readString(root.resolve("app/src/main/java/io/github/supermonster003/autojs6/plugin/three/block/compose/designer/sandbox/SandboxEngine.kt"))
        listOf("initSafeStandardObjects()", "setClassShutter { false }", "setInterpretedMode(true)", "setInstructionObserverThreshold(", "setMaximumInterpreterStackDepth(")
            .forEach { assertEquals("SandboxEngine must call $it", true, it in engine) }
        assertEquals("SandboxEngine must not create full standard objects", false, "initStandardObjects(" in engine)
    }

    private fun Path.unix(): String = pathString.replace('\\', '/')

    private fun mainSources(): List<Path> = Files.walk(root.resolve("app/src/main/java")).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.extension in setOf("kt", "java") }.toList()
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
