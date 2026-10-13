package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Designer projects are script files in the plugin's private storage (roadmap D39): `projects/` holds the
 * saved scripts, `drafts/` the periodically written working copies that survive a killed process (D40).
 * Nothing here needs a storage permission; importing and exporting go through the system document picker.
 */
internal class ProjectStore(context: Context) {
    data class Project(val name: String, val file: File, val modifiedAt: Long)

    private val base = File(context.filesDir, "designer")
    val projectsDir = File(base, "projects")
    val draftsDir = File(base, "drafts")
    /** Origin Uris of imported projects, one `name=uri` line each; informational, never dereferenced automatically. */
    private val sourcesFile = File(base, "sources.properties")

    fun list(): List<Project> {
        projectsDir.mkdirs()
        return (projectsDir.listFiles { file -> file.isFile && file.name.endsWith(EXTENSION) } ?: emptyArray())
            .map { Project(it.name, it, it.lastModified()) }
            .sortedWith(compareByDescending<Project> { it.modifiedAt }.thenBy { it.name })
    }

    fun exists(name: String): Boolean = file(name).isFile

    fun file(name: String): File = File(projectsDir, requireValidName(name))

    /** Creates `<baseName>.js`, suffixing a counter while the name is taken, and writes the starter script. */
    fun create(baseName: String, script: String = template()): Project {
        projectsDir.mkdirs()
        val stem = baseName.trim().removeSuffix(EXTENSION).ifBlank { "untitled" }
        var candidate = stem + EXTENSION
        var counter = 2
        while (exists(candidate)) candidate = "$stem-${counter++}$EXTENSION"
        val file = file(candidate)
        file.writeText(script, StandardCharsets.UTF_8)
        return Project(candidate, file, file.lastModified())
    }

    fun read(name: String): String = file(name).readText(StandardCharsets.UTF_8)

    fun write(name: String, script: String) {
        projectsDir.mkdirs()
        val target = file(name)
        val temp = File(projectsDir, target.name + ".tmp")
        temp.writeText(script, StandardCharsets.UTF_8)
        if (!temp.renameTo(target)) {
            target.writeText(script, StandardCharsets.UTF_8)
            temp.delete()
        }
    }

    fun delete(name: String) {
        file(name).delete()
        draftFile(name).delete()
        recordSource(name, null)
    }

    fun rename(name: String, newName: String): Project {
        val target = file(newName)
        require(!target.exists()) { "A project named $newName already exists" }
        val source = file(name)
        check(source.renameTo(target)) { "Could not rename $name" }
        draftFile(name).let { if (it.exists()) it.renameTo(draftFile(newName)) }
        return Project(target.name, target, target.lastModified())
    }

    fun draftFile(name: String): File = File(draftsDir, requireValidName(name))

    /** The draft text when a draft exists and differs from the saved project. */
    fun pendingDraft(name: String): String? {
        val draft = draftFile(name)
        if (!draft.isFile) return null
        val text = draft.readText(StandardCharsets.UTF_8)
        val saved = file(name).takeIf { it.isFile }?.readText(StandardCharsets.UTF_8)
        return text.takeIf { it != saved }
    }

    fun writeDraft(name: String, script: String) {
        draftsDir.mkdirs()
        draftFile(name).writeText(script, StandardCharsets.UTF_8)
    }

    fun clearDraft(name: String) {
        draftFile(name).delete()
    }

    /** Copies a document picked through the system picker into the projects directory. */
    fun import(resolver: ContentResolver, uri: Uri): Project {
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: "imported.js"
        val text = resolver.openInputStream(uri)?.use { it.readBytes().toString(StandardCharsets.UTF_8) }
            ?: throw IllegalStateException("Cannot read $uri")
        val stem = displayName.substringAfterLast('/').substringBeforeLast('.').replace(invalid, "-").ifBlank { "imported" }
        return create(stem, text).also { recordSource(it.name, uri) }
    }

    /** The document Uri a project was imported from, when it was imported through the picker. */
    fun sourceOf(name: String): Uri? = sources()[name]?.let(Uri::parse)

    private fun sources(): Map<String, String> = if (!sourcesFile.isFile) emptyMap() else
        sourcesFile.readLines(StandardCharsets.UTF_8).mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()

    private fun recordSource(name: String, uri: Uri?) {
        val updated = sources().toMutableMap()
        if (uri == null) { if (updated.remove(name) == null) return } else updated[name] = uri.toString()
        base.mkdirs()
        sourcesFile.writeText(updated.entries.joinToString("") { it.key + "=" + it.value + "\n" }, StandardCharsets.UTF_8)
    }

    fun template(): String = """
        "ui";

        compose.mount(function () {
            return compose.Column({padding: 16, spacing: 12},
                compose.Text({key: 'title', text: 'Hello', style: 'headlineSmall'}));
        });
    """.trimIndent() + "\n"

    private fun requireValidName(name: String): String {
        require(isValidName(name)) { "Invalid project name: $name" }
        return name
    }

    companion object {
        const val EXTENSION = ".js"
        private val invalid = Regex("[^A-Za-z0-9_.\\-\\u4e00-\\u9fff]")
        private val valid = Regex("[A-Za-z0-9_\\-\\u4e00-\\u9fff][A-Za-z0-9_.\\-\\u4e00-\\u9fff]*\\.js")

        fun isValidName(name: String): Boolean = valid.matches(name) && ".." !in name
    }
}
