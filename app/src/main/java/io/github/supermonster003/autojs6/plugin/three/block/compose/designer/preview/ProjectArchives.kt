package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Zips of a project's files for the host's preview service (contract v3, roadmap P11.3): the resources a layout
 * resolves (`setLayout` with `resourcesFd`) and the whole project a script runs in (`runScript` with `projectFd`).
 * The files on disk are taken as they are and the texts the designer holds (unsaved layout files, the script)
 * replace them; a zip is written again only when its fingerprint (paths, sizes, times, texts) changes, so the host,
 * which keys its unpacked copy by the zip's digest, unpacks it again only then.
 */
internal class ProjectArchives(private val directory: File) {

    /** A written zip and the lowercase SHA-256 of its bytes. */
    class Archive(val file: File, val sha256: String, val paths: Set<String>)

    /**
     * Where a document's files are: the folder of a folder project ([whole]: every file of it), or the folder beside a
     * script file (only its `res/` directory, the folder may hold anything).
     */
    data class Root(val dir: File, val whole: Boolean)

    private val cache = HashMap<String, Pair<String, Archive>>()

    /**
     * The zip [name] of the files under [root] with [texts] (by `/`-separated path) in place of the files on disk, or
     * null when there is nothing to send or it exceeds the contract's limits. Paths that would leave the zip's root are
     * left out. Any thread; calls are serialized.
     */
    @Synchronized
    fun archive(name: String, root: Root?, texts: Map<String, String>): Archive? {
        val entries = sortedMapOf<String, Any>()
        if (root != null) collect(root, entries)
        for ((path, text) in texts) if (isEntryPath(path)) entries[path] = text
        if (entries.isEmpty() || entries.size > MAX_ENTRIES) return null
        val fingerprint = fingerprintOf(entries)
        cache[name]?.let { (known, archive) -> if (known == fingerprint && archive.file.isFile) return archive }
        directory.mkdirs()
        val target = File(directory, "$name.zip")
        val temp = File(directory, "$name.zip.tmp")
        var unpacked = 0L
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            ZipOutputStream(DigestingOutputStream(FileOutputStream(temp), digest)).use { zip ->
                for ((path, source) in entries) {
                    val bytes = when (source) {
                        is String -> source.toByteArray(StandardCharsets.UTF_8)
                        else -> (source as File).readBytes()
                    }
                    unpacked += bytes.size
                    if (unpacked > MAX_UNPACKED_BYTES) return null.also { temp.delete() }
                    // A fixed time keeps the bytes, and so the digest, the same for the same files.
                    zip.putNextEntry(ZipEntry(path).apply { time = FIXED_TIME })
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        } catch (error: java.io.IOException) {
            temp.delete()
            return null
        }
        if (temp.length() > MAX_BYTES) return null.also { temp.delete() }
        if (!temp.renameTo(target)) {
            target.delete()
            if (!temp.renameTo(target)) return null.also { temp.delete() }
        }
        val archive = Archive(target, digest.digest().joinToString("") { "%02x".format(it) }, entries.keys.toSet())
        cache[name] = fingerprint to archive
        return archive
    }

    private fun collect(root: Root, entries: MutableMap<String, Any>) {
        val base = root.dir
        val start = if (root.whole) base else File(base, RES_DIRECTORY)
        if (!start.isDirectory) return
        start.walkTopDown()
            .onEnter { dir -> dir == start || !dir.name.startsWith(".") }
            .filter { it.isFile && !it.name.startsWith(".") && !it.name.endsWith(".tmp") }
            .forEach { file ->
                val path = file.relativeTo(base).invariantSeparatorsPath
                if (isEntryPath(path) && entries.size <= MAX_ENTRIES) entries[path] = file
            }
    }

    private fun fingerprintOf(entries: Map<String, Any>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        for ((path, source) in entries) {
            digest.update(path.toByteArray(StandardCharsets.UTF_8))
            digest.update(0)
            when (source) {
                is String -> digest.update(source.toByteArray(StandardCharsets.UTF_8))
                else -> (source as File).let { digest.update("${it.length()}:${it.lastModified()}".toByteArray(StandardCharsets.UTF_8)) }
            }
            digest.update(0)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private class DigestingOutputStream(private val out: FileOutputStream, private val digest: MessageDigest) : java.io.OutputStream() {
        override fun write(b: Int) { out.write(b); digest.update(b.toByte()) }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); digest.update(b, off, len) }
        override fun flush() = out.flush()
        override fun close() = out.close()
    }

    companion object {
        const val RESOURCES = "resources"
        const val PROJECT = "project"
        private const val RES_DIRECTORY = "res"
        /** 1980-01-01, the earliest time a zip entry holds. */
        private const val FIXED_TIME = 315_532_800_000L
        // The host's limits (contract v3: maxArchiveBytes / maxArchiveUnpackedBytes / maxArchiveEntries).
        private const val MAX_BYTES = 32L * 1024L * 1024L
        private const val MAX_UNPACKED_BYTES = 128L * 1024L * 1024L
        private const val MAX_ENTRIES = 4_096
        private const val MAX_PATH_CHARS = 512

        /** A relative `/`-separated path that stays inside the zip's root. */
        fun isEntryPath(path: String): Boolean =
            path.isNotEmpty() && path.length <= MAX_PATH_CHARS && !path.startsWith("/") && '\\' !in path && ':' !in path &&
                path.split('/').all { it.isNotEmpty() && it != "." && it != ".." }
    }
}
