package io.github.supermonster003.autojs6.plugin.compose.ui

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
 * The host-api lock, the staged AAR files and the third-party notices must describe the same bytes
 * (roadmap D26 / P0.1); Gradle enforces the lock at configuration time, this test keeps the
 * human-readable records honest as well.
 */
class HostApiAarLockTest {

    private val root: Path = findProjectRoot()

    @Test
    fun `host api lock lists the INFO api and draft renderer api with matching digests`() {
        val lock = readLock(root.resolve("locks/host-api-aars.lock"))
        // The P0 draft is explicitly experimental; P1.1 will freeze the production contract.
        assertEquals(setOf("common-plugin-api", "compose-ui-api"), lock.keys)
        lock.forEach { (id, entry) ->
            val file = root.resolve("libs").resolve(entry.file)
            assertTrue("$id: missing ${entry.file}", Files.isRegularFile(file))
            assertFalse("$id: debug AAR variants are not accepted", entry.file.endsWith("-debug.aar"))
            assertEquals("$id: digest drifted from the lock", entry.sha256, sha256(file))
        }
    }

    @Test
    fun `the staged AARs are pure bytecode`() {
        readLock(root.resolve("locks/host-api-aars.lock")).forEach { (id, entry) ->
            val entries = ZipFile(root.resolve("libs").resolve(entry.file).toFile()).use { zip ->
                zip.entries().asSequence().map { it.name }.toList()
            }
            assertTrue("$id must contain classes.jar", "classes.jar" in entries)
            assertTrue("$id must not ship native libraries (roadmap D22)", entries.none { it.startsWith("jni/") })
        }
    }

    @Test
    fun `the third party notices repeat every locked digest`() {
        val notices = Files.readString(root.resolve("THIRD_PARTY_NOTICES.md"))
        readLock(root.resolve("locks/host-api-aars.lock")).forEach { (id, entry) ->
            assertTrue("THIRD_PARTY_NOTICES.md must list ${entry.file}", notices.contains("`${entry.file}`"))
            assertTrue("THIRD_PARTY_NOTICES.md must repeat the digest of $id", notices.contains(entry.sha256))
        }
        val libsReadme = Files.readString(root.resolve("libs/README.md"))
        readLock(root.resolve("locks/host-api-aars.lock")).forEach { (_, entry) ->
            assertTrue("libs/README.md must describe ${entry.file}", libsReadme.contains(entry.file))
        }
    }

    @Test
    fun `the build script consumes exactly the locked identifiers`() {
        val build = Files.readString(root.resolve("app/build.gradle.kts"))
        val ids = readLock(root.resolve("locks/host-api-aars.lock")).keys.sorted()
        assertTrue(build.contains("val hostApiIds = listOf(${ids.joinToString { "\"$it\"" }})"))
        assertTrue(build.contains("lockedAars(rootProject.file(\"locks/host-api-aars.lock\"), hostApiIds, \"libs\")"))
    }

    private data class LockEntry(val file: String, val sha256: String)

    private fun readLock(path: Path): Map<String, LockEntry> {
        val lines = Files.readAllLines(path).map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        assertEquals("format=1", lines.first())
        val values = lines.drop(1).associate { line -> line.substringBefore('=') to line.substringAfter('=') }
        val ids = values.keys.map { it.substringBeforeLast('.') }.toSet()
        return ids.associateWith { id ->
            val sha256 = values.getValue("$id.sha256")
            assertTrue("$id: lock digest must be lowercase hex", Regex("[0-9a-f]{64}").matches(sha256))
            LockEntry(values.getValue("$id.file"), sha256)
        }
    }

    private fun sha256(path: Path): String =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
