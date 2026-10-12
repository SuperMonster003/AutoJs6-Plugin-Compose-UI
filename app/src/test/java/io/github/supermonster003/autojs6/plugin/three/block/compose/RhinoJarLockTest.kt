package io.github.supermonster003.autojs6.plugin.three.block.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * The slim Rhino parser jar (roadmap P8.1 / D35) is staged like the host contract AARs: the lock, the
 * jar bytes, the third-party notices and libs/README.md must describe the same artifact, and the jar must
 * be the host's slim derivation (parser present, JVM-only shell / optimizer / JFR components absent).
 */
class RhinoJarLockTest {

    private val root: Path = findProjectRoot()

    @Test
    fun `rhino lock names the slim jar with a matching digest and a host source commit`() {
        val lock = readLock(root.resolve("locks/rhino-jar.lock"))
        assertEquals(setOf("rhino.file", "rhino.sha256", "rhino.source"), lock.keys)
        val file = lock.getValue("rhino.file")
        assertTrue("the staged jar must be the host's slim derivation", file.endsWith("-slim.jar"))
        val jar = root.resolve("libs").resolve(file)
        assertTrue("missing $file", Files.isRegularFile(jar))
        assertEquals("digest drifted from the lock", lock.getValue("rhino.sha256"), sha256(jar))
        assertTrue("rhino.source must be the host commit", Regex("[0-9a-f]{7,40}").matches(lock.getValue("rhino.source")))
    }

    @Test
    fun `the slim jar carries the parser and not the JVM-only components`() {
        val lock = readLock(root.resolve("locks/rhino-jar.lock"))
        val entries = ZipFile(root.resolve("libs").resolve(lock.getValue("rhino.file")).toFile()).use { zip ->
            zip.entries().asSequence().map { it.name }.toList()
        }
        listOf(
            "org/mozilla/javascript/Parser.class",
            "org/mozilla/javascript/CompilerEnvirons.class",
            "org/mozilla/javascript/TokenStream.class",
            "org/mozilla/javascript/ast/AstRoot.class",
            "org/mozilla/javascript/ast/FunctionCall.class",
            "org/mozilla/javascript/ast/ObjectLiteral.class",
        ).forEach { assertTrue("$it must be present", it in entries) }
        assertTrue("no JLine shell", entries.none { it.startsWith("org/jline/") })
        assertTrue("no bytecode optimizer", entries.none { it.startsWith("org/mozilla/javascript/optimizer/") })
        assertTrue("no JFR events", entries.none { it.startsWith("org/mozilla/javascript/") && it.contains("JFREmitter") })
        assertTrue("no native libraries (roadmap D22)", entries.none { it.endsWith(".so") || it.endsWith(".dll") })
    }

    @Test
    fun `the notices and the libs readme repeat the locked digest`() {
        val lock = readLock(root.resolve("locks/rhino-jar.lock"))
        val notices = Files.readString(root.resolve("THIRD_PARTY_NOTICES.md"))
        assertTrue(notices.contains("`${lock.getValue("rhino.file")}`"))
        assertTrue(notices.contains(lock.getValue("rhino.sha256")))
        val readme = Files.readString(root.resolve("libs/README.md"))
        assertTrue(readme.contains(lock.getValue("rhino.file")))
        assertTrue(readme.contains(lock.getValue("rhino.sha256")))
        assertTrue(readme.contains(lock.getValue("rhino.source")))
    }

    @Test
    fun `the build script consumes the locked jar`() {
        val build = Files.readString(root.resolve("app/build.gradle.kts"))
        assertTrue(build.contains("lockedJar(rootProject.file(\"locks/rhino-jar.lock\"), \"rhino\", \"libs\")"))
        assertTrue(build.contains("implementation(files(rhinoJar))"))
        assertFalse("the contract is packaged since P8.1", build.contains("compileOnly(files(hostApiAars[1]))"))
        assertTrue(build.contains("implementation(files(hostApiAars[1]))"))
    }

    private fun readLock(path: Path): Map<String, String> {
        val lines = Files.readAllLines(path).map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        assertEquals("format=1", lines.first())
        return lines.drop(1).associate { line -> line.substringBefore('=') to line.substringAfter('=') }
    }

    private fun sha256(path: Path): String =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
