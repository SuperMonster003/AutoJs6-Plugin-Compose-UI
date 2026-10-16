package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.TsxWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiSegment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsTree
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsTreeJson
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * The TSX snapshots of `app/src/test/resources/tsx` (roadmap P9.2): sources and the trees the TypeScript Engine
 * parser Worker gave for them, tied together by SNAPSHOTS.txt. Regenerate with `.python/generate_tsx_snapshots.py`.
 */
internal class TsxSnapshots(val root: Path = defaultRoot()) {
    class Entry(val sourceSha256: String, val tree: String, val treeSha256: String)

    val entries: Map<String, Entry> = if (!Files.isRegularFile(root.resolve(MANIFEST))) emptyMap() else
        Files.readAllLines(root.resolve(MANIFEST)).filter { it.isNotBlank() && !it.startsWith("#") }.associate { line ->
            val parts = line.split(' ')
            parts[0] to Entry(parts[1], parts[2], parts[3])
        }

    /** The manifest's description of the parser that produced the trees. */
    val producer: String get() = Files.readAllLines(root.resolve(MANIFEST)).drop(1).first().removePrefix("# ")

    fun source(name: String): String = Files.readString(root.resolve(name))

    /** True when [name] has a tree made from its current bytes. */
    fun isCurrent(name: String): Boolean = entries[name]?.sourceSha256 == sha256(Files.readAllBytes(root.resolve(name)))

    fun tree(name: String): TsTree {
        val entry = entries[name] ?: throw AssertionError("$name has no tree; run .python/generate_tsx_snapshots.py")
        if (!isCurrent(name)) throw AssertionError("The tree of $name is stale; run .python/generate_tsx_snapshots.py")
        val json = GZIPInputStream(Files.newInputStream(root.resolve(entry.tree))).use { it.readBytes() }
        if (sha256(json) != entry.treeSha256) throw AssertionError("${entry.tree} does not match the manifest")
        return TsTreeJson.parse(json.toString(Charsets.UTF_8))
    }

    /** Snapshot sources under [directory], relative to the root, sorted. */
    fun sources(directory: String): List<String> {
        val base = root.resolve(directory)
        if (!Files.isDirectory(base)) return emptyList()
        return Files.list(base).use { stream -> stream.filter { it.toString().endsWith(".tsx") }.map { "$directory/${it.fileName}" }.toList().sorted() }
    }

    companion object {
        const val MANIFEST = "SNAPSHOTS.txt"

        fun projectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }

        fun defaultRoot(): Path = projectRoot().resolve("app/src/test/resources/tsx")

        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        /** The TSX form of a JavaScript compose script: its document written by the TSX writer. */
        /** The TSX ui writer's form of [document] with every layout written whole, as if each one had been edited (P11.7). */
        fun tsxUiCanonical(document: UiDocument): String = UiScriptWriter.write(
            document.copy(segments = document.segments.map { segment -> if (segment is UiSegment.Layout) UiSegment.Layout(segment.fragment.copy(tsx = null)) else segment }),
        )

        fun tsxForm(script: String): String = TsxWriter.write(ScriptReader().read(script).copy(dialect = Dialect.TSX))

        /** The bundled JavaScript examples by file name. */
        fun examples(): Map<String, String> = Files.list(projectRoot().resolve("app/src/main/assets/examples")).use { stream ->
            stream.filter { it.toString().endsWith(".js") }.toList().associate { it.fileName.toString() to Files.readString(it) }
        }

        /** Gallery scripts by component name. */
        fun gallery(): Map<String, String> = GalleryCatalog.entries.associate { it.component to it.script() }
    }
}
