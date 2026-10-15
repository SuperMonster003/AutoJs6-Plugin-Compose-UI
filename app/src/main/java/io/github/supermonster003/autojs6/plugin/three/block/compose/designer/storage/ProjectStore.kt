package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Designer projects are script files in the plugin's private storage (roadmap D39): `projects/` holds the
 * saved scripts (`.js`, and `.tsx` since roadmap P9.2), `drafts/` the periodically written working copies that survive a killed process (D40).
 * Nothing here needs a storage permission; importing and exporting go through the system document picker.
 */
internal class ProjectStore(context: Context) {
    /** What a project is, for the home list and the new-project dialog (roadmap P10.4): Compose JavaScript, Compose TSX, or a `"ui";` mode script. */
    enum class Kind { JS, TSX, UI }

    data class Project(val name: String, val file: File, val modifiedAt: Long, val kind: Kind = Kind.JS)

    /**
     * The document a project was imported from (roadmap P9.1, D45) and [baseline], the digest of the text both
     * sides held at the last sync (import, write back or re-import); null for records written before 1.3.0.
     */
    data class Source(val uri: Uri, val baseline: String?)

    private val base = File(context.filesDir, "designer")
    val projectsDir = File(base, "projects")
    val draftsDir = File(base, "drafts")
    /** Origins of imported projects, one `name=uri` or `name=uri<TAB>baseline` line each; only read on request. */
    private val sourcesFile = File(base, "sources.properties")
    /** Image folders (roadmap P9.1, D44), one `key=uri` line per document, keyed like drafts. */
    private val imageFoldersFile = File(base, "images.properties")

    fun list(): List<Project> {
        projectsDir.mkdirs()
        return (projectsDir.listFiles { file -> file.isFile && isValidName(file.name) } ?: emptyArray())
            .map { Project(it.name, it, it.lastModified(), kindOf(it)) }
            .sortedWith(compareByDescending<Project> { it.modifiedAt }.thenBy { it.name })
    }

    /** The kind of [file] by its name and, for a `.js` script, its text (the `"ui"` directive with a `ui.layout` call). */
    private fun kindOf(file: File): Kind = when {
        file.name.endsWith(TSX_EXTENSION, ignoreCase = true) -> Kind.TSX
        runCatching { UiScriptReader.looksLikeUiScript(file.readText(StandardCharsets.UTF_8)) }.getOrDefault(false) -> Kind.UI
        else -> Kind.JS
    }

    fun exists(name: String): Boolean = file(name).isFile

    fun file(name: String): File = File(projectsDir, requireValidName(name))

    /**
     * Creates `<baseName><extension>` (`.js` or `.tsx`), suffixing a counter while the name is taken, and writes the
     * starter script. [baseName] may be any document name (an import, a read-only document saved as a project):
     * characters a project name cannot hold are replaced (see [stemOf]).
     */
    fun create(baseName: String, script: String = template(), extension: String = EXTENSION): Project {
        require(extension in EXTENSIONS) { "Unsupported project extension: $extension" }
        projectsDir.mkdirs()
        val stem = stemOf(baseName.trim().removeSuffix(extension), "untitled")
        var candidate = stem + extension
        var counter = 2
        while (exists(candidate)) candidate = "$stem-${counter++}$extension"
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
        setImageFolder(name, null)
    }

    fun rename(name: String, newName: String): Project {
        val target = file(newName)
        require(!target.exists()) { "A project named $newName already exists" }
        val source = file(name)
        check(source.renameTo(target)) { "Could not rename $name" }
        draftFile(name).let { if (it.exists()) it.renameTo(draftFile(newName)) }
        sourceOf(name)?.let { origin ->
            recordSource(name, null)
            recordSource(newName, origin)
        }
        imageFolderOf(name)?.let { folder ->
            setImageFolder(name, null)
            setImageFolder(newName, folder)
        }
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

    /**
     * Copies a document picked through the system picker into the projects directory and remembers where it
     * came from, keeping the picker's grant across restarts when the provider allows it (read and write when
     * possible, otherwise read only).
     */
    fun import(resolver: ContentResolver, uri: Uri): Project {
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: "imported.js"
        val text = resolver.openInputStream(uri)?.use { it.readBytes().toString(StandardCharsets.UTF_8) }
            ?: throw IllegalStateException("Cannot read $uri")
        val stem = stemOf(displayName.substringAfterLast('/').substringBeforeLast('.'), "imported")
        keepAccess(resolver, uri)
        return create(stem, text, extensionFor(displayName)).also { recordSource(it.name, Source(uri, digest(text))) }
    }

    /** The document a project was imported from, when it was imported through the picker. */
    fun sourceOf(name: String): Source? = sources()[name]

    /** Points [name] at [uri], a document picked again after the old link broke, with a new [baseline]. */
    fun relink(resolver: ContentResolver, name: String, uri: Uri, baseline: String?) {
        keepAccess(resolver, uri)
        recordSource(name, Source(uri, baseline))
    }

    /** Records that the project and its source both hold the text whose digest is [baseline]. */
    fun markSynced(name: String, baseline: String) {
        val origin = sourceOf(name) ?: return
        recordSource(name, origin.copy(baseline = baseline))
    }

    /**
     * The folder whose files relative Image paths of the document with draft key [key] resolve against on the
     * design canvas: a document tree picked by the user, or a file Uri.
     */
    fun imageFolderOf(key: String): Uri? = readMap(imageFoldersFile)[key]?.let(Uri::parse)

    /** Records (or with null forgets) the image folder of the document with draft key [key]. */
    fun setImageFolder(key: String, folder: Uri?) {
        val updated = readMap(imageFoldersFile).toMutableMap()
        if (folder == null) { if (updated.remove(key) == null) return } else updated[key] = folder.toString()
        base.mkdirs()
        imageFoldersFile.writeText(updated.entries.joinToString("") { (name, uri) -> name + "=" + uri + LINE_END }, StandardCharsets.UTF_8)
    }

    private fun readMap(file: File): Map<String, String> = if (!file.isFile) emptyMap() else
        file.readLines(StandardCharsets.UTF_8).mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()

    private fun keepAccess(resolver: ContentResolver, uri: Uri) {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return
        val readWrite = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        for (flags in intArrayOf(readWrite, Intent.FLAG_GRANT_READ_URI_PERMISSION)) {
            try {
                resolver.takePersistableUriPermission(uri, flags)
                return
            } catch (error: SecurityException) {
                // Not persistable with these flags (a read-only provider, or no picker grant at all); try less.
            }
        }
    }

    private fun sources(): Map<String, Source> = if (!sourcesFile.isFile) emptyMap() else
        sourcesFile.readLines(StandardCharsets.UTF_8).mapNotNull { line ->
            val parts = line.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            val value = parts[1].split(FIELD_SEPARATOR, limit = 2)
            parts[0] to Source(Uri.parse(value[0]), value.getOrNull(1)?.takeIf { it.isNotEmpty() })
        }.toMap()

    private fun recordSource(name: String, source: Source?) {
        val updated = sources().toMutableMap()
        if (source == null) { if (updated.remove(name) == null) return } else updated[name] = source
        base.mkdirs()
        sourcesFile.writeText(updated.entries.joinToString("") { (key, value) ->
            key + "=" + value.uri + (value.baseline?.let { FIELD_SEPARATOR + it } ?: "") + LINE_END
        }, StandardCharsets.UTF_8)
    }

    fun template(): String = """
        "ui";

        compose.mount(function () {
            return compose.Column({padding: 16, spacing: 12},
                compose.Text({key: 'title', text: 'Hello', style: 'headlineSmall'}));
        });
    """.trimIndent() + LINE_END

    private fun requireValidName(name: String): String {
        require(isValidName(name)) { "Invalid project name: $name" }
        return name
    }

    companion object {
        const val EXTENSION = ".js"
        /** TSX projects (roadmap P9.2 / D42), read and written through the TypeScript Engine parser. */
        const val TSX_EXTENSION = ".tsx"
        val EXTENSIONS = listOf(EXTENSION, TSX_EXTENSION)

        /** The starter of a TSX project (roadmap P9.2): the template's interface in the TSX writer's format. */
        val TSX_TEMPLATE: String = """
            "ui";

            compose.mount(() => {
                return (
                    <compose.Column padding={16} spacing={12}>
                        <compose.Text key='title' text='Hello' style='headlineSmall' />
                    </compose.Column>
                );
            });
        """.trimIndent() + LINE_END

        /**
         * The starter of a ui (XML) project (roadmap P10.4): the `"ui"` directive, a layout with a text and a button,
         * and one click handler the way the host's samples write them.
         */
        val UI_TEMPLATE: String = """
            "ui";

            ui.layout(
                <vertical padding="16">
                    <text id="title" text="Hello" textSize="24sp"/>
                    <button id="ok" text="OK"/>
                </vertical>
            );

            ui.ok.on('click', () => {
                toast('OK');
            });
        """.trimIndent() + LINE_END

        /** Types offered by the document picker when importing or relinking a script (any file as the last resort). */
        val SCRIPT_DOCUMENT_TYPES = arrayOf("application/javascript", "text/javascript", "application/x-javascript", "text/typescript", "text/plain", "*/*")
        /** A tab never occurs in a Uri string, so it separates the Uri from the baseline digest. */
        private const val FIELD_SEPARATOR = "\t"
        private const val LINE_END = "\n"
        private val invalid = Regex("[^A-Za-z0-9_.\\-\\u4e00-\\u9fff]")
        private val dots = Regex("\\.{2,}")
        private val valid = Regex("[A-Za-z0-9_\\-\\u4e00-\\u9fff][A-Za-z0-9_.\\-\\u4e00-\\u9fff]*\\.(js|tsx)")

        fun isValidName(name: String): Boolean = valid.matches(name) && ".." !in name

        /** Longest stem [stemOf] keeps: 64 CJK characters with a counter and extension stay under 255 bytes. */
        const val MAX_STEM_CHARS = 64

        /**
         * A project file stem made from any document name: characters a project name cannot hold become `-`, runs
         * of dots become one, leading and trailing dots are dropped (so neither `.x.js` nor `x..js` arises), and
         * the stem is cut to [MAX_STEM_CHARS]; [fallback] when nothing is left. `stem + extension` and
         * `stem-<n> + extension` are always valid names.
         */
        fun stemOf(name: String, fallback: String): String {
            val stem = name.trim().replace(invalid, "-").replace(dots, ".").trim('.').take(MAX_STEM_CHARS).trimEnd('.')
            return stem.ifEmpty { fallback }
        }

        /** `.tsx` for a TSX document name, `.js` for anything else (an imported `.ts` or `.txt` becomes a script). */
        fun extensionFor(displayName: String): String = if (displayName.endsWith(TSX_EXTENSION, ignoreCase = true)) TSX_EXTENSION else EXTENSION

        /** SHA-256 of a script's UTF-8 text: the baseline that tells which side of a source link changed. */
        fun digest(text: String): String =
            MessageDigest.getInstance("SHA-256").digest(text.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
