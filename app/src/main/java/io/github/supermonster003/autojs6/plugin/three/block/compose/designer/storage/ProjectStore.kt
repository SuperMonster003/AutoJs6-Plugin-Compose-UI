package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Designer projects are script files in the plugin's private storage (roadmap D39): `projects/` holds the
 * saved scripts (`.js`, and `.tsx` since roadmap P9.2), `drafts/` the periodically written working copies that survive a killed process (D40).
 * Nothing here needs a storage permission; importing and exporting go through the system document picker.
 *
 * Folder projects (roadmap P11.3 / D56, appendix B): a `"ui"` project whose script refers to layout files is a
 * folder `projects/<stem>/` with the script `main.js` next to its files; its name is `<stem>/main.js`, so its draft
 * is `drafts/<stem>/main.js` and its source record is keyed by that name. A single-file project is promoted to one
 * when it first needs a layout file ([promote]).
 */
internal class ProjectStore(private val base: File) {
    constructor(context: Context) : this(File(context.filesDir, "designer"))

    /** What a project is, for the home list and the new-project dialog (roadmap P10.4): Compose JavaScript, Compose TSX, or a `"ui";` mode script. */
    enum class Kind { JS, TSX, UI }

    data class Project(val name: String, val file: File, val modifiedAt: Long, val kind: Kind = Kind.JS) {
        /** A folder project (`<stem>/main.js`). */
        val folder: Boolean get() = isFolderName(name)
        /** What the home list shows: the folder's name for a folder project, the file name otherwise. */
        val displayName: String get() = displayNameOf(name)
    }

    /**
     * The document a project was imported from (roadmap P9.1, D45) and [baseline], the digest of the text both
     * sides held at the last sync (import, write back or re-import); null for records written before 1.3.0.
     */
    data class Source(val uri: Uri, val baseline: String?)

    val projectsDir = File(base, "projects")
    val draftsDir = File(base, "drafts")
    /** Origins of imported projects, one `name=uri` or `name=uri<TAB>baseline` line each; only read on request. */
    private val sourcesFile = File(base, "sources.properties")
    /** Image folders (roadmap P9.1, D44), one `key=uri` line per document, keyed like drafts. */
    private val imageFoldersFile = File(base, "images.properties")

    fun list(): List<Project> {
        projectsDir.mkdirs()
        val files = (projectsDir.listFiles { file -> file.isFile && isValidName(file.name) } ?: emptyArray())
            .map { Project(it.name, it, it.lastModified(), kindOf(it)) }
        val folders = (projectsDir.listFiles { file -> file.isDirectory && isValidName(file.name + "/" + MAIN_SCRIPT) } ?: emptyArray())
            .mapNotNull { dir -> File(dir, MAIN_SCRIPT).takeIf { it.isFile }?.let { Project(dir.name + "/" + MAIN_SCRIPT, it, folderModifiedAt(dir), kindOf(it)) } }
        return (files + folders).sortedWith(compareByDescending<Project> { it.modifiedAt }.thenBy { it.name })
    }

    /** The newest modification time of anything in the folder project [dir] (its script or one of its files). */
    private fun folderModifiedAt(dir: File): Long = dir.walkTopDown().filter { it.isFile }.maxOfOrNull { it.lastModified() } ?: dir.lastModified()

    /** The folder of a folder project, or null for a single-file project. */
    fun projectDir(name: String): File? = if (isFolderName(requireValidName(name))) file(name).parentFile else null

    /**
     * The file at [path] (`layout.xml`, `res/layout/main.xml`) of the folder project [name], or null for a
     * single-file project and for a path that leaves the folder.
     */
    fun layoutFile(name: String, path: String): File? {
        val dir = projectDir(name)?.canonicalFile ?: return null
        if (path.isEmpty() || File(path).isAbsolute || path.startsWith("/") || path.startsWith("\\")) return null
        val file = try {
            File(dir, path).canonicalFile
        } catch (error: IOException) {
            return null
        }
        return file.takeIf { it.path.startsWith(dir.path + File.separator) }
    }

    /** Writes [text] to [file] through a temporary file in the same folder. */
    private fun writeAtomically(file: File, text: String) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(text, StandardCharsets.UTF_8)
        if (!temp.renameTo(file)) {
            file.writeText(text, StandardCharsets.UTF_8)
            temp.delete()
        }
    }

    /** Writes the file at [path] of the folder project [name] (see [layoutFile]); false when there is no such place. */
    fun writeLayoutFile(name: String, path: String, text: String): Boolean {
        val file = layoutFile(name, path) ?: return false
        writeAtomically(file, text)
        return true
    }

    /**
     * Turns the single-file project [name] (`<stem>.js`) into the folder project `<stem>/main.js` (roadmap P11.3 / D56),
     * moving its draft, source record and image folder with it; a counter is added while the folder name is taken.
     * A folder project is returned unchanged.
     */
    fun promote(name: String): Project {
        if (isFolderName(requireValidName(name))) return file(name).let { Project(name, it, it.lastModified(), kindOf(it)) }
        require(name.endsWith(EXTENSION)) { "Only JavaScript projects become folder projects: $name" }
        val stem = name.removeSuffix(EXTENSION)
        var folder = stem
        var counter = 2
        while (File(projectsDir, folder).exists()) folder = "$stem-${counter++}"
        val target = folder + "/" + MAIN_SCRIPT
        val dir = File(projectsDir, folder)
        check(dir.mkdirs()) { "Could not create $folder" }
        val script = file(target)
        check(file(name).renameTo(script)) { "Could not move $name into $folder" }
        writeProjectConfig(dir, stem)
        draftFile(name).takeIf { it.exists() }?.let { draft ->
            draftFile(target).parentFile?.mkdirs()
            draft.renameTo(draftFile(target))
        }
        layoutDraftsDir(name).takeIf { it.exists() }?.renameTo(layoutDraftsDir(target))
        sourceOf(name)?.let { origin ->
            recordSource(name, null)
            recordSource(target, origin)
        }
        imageFolderOf(name)?.let { folderUri ->
            setImageFolder(name, null)
            setImageFolder(target, folderUri)
        }
        return Project(target, script, script.lastModified(), kindOf(script))
    }

    /**
     * Creates the folder project `<stem>/main.js` (roadmap P11.3 / D66) with [script] and the layout [files] by
     * path, and its `project.json`; a counter is added while the folder name is taken.
     */
    fun createFolder(baseName: String, script: String, files: Map<String, String>): Project {
        projectsDir.mkdirs()
        val stem = stemOf(baseName.trim().removeSuffix(EXTENSION), "untitled")
        var folder = stem
        var counter = 2
        while (File(projectsDir, folder).exists()) folder = "$stem-${counter++}"
        val name = folder + "/" + MAIN_SCRIPT
        for (path in files.keys) require(layoutFile(name, path) != null) { "Unusable layout file path: $path" }
        check(File(projectsDir, folder).mkdirs()) { "Could not create $folder" }
        writeAtomically(file(name), script)
        for ((path, text) in files) writeLayoutFile(name, path, text)
        writeProjectConfig(File(projectsDir, folder), stem)
        return Project(name, file(name), file(name).lastModified(), kindOf(file(name)))
    }

    /** Writes the folder's `project.json` (roadmap D63) unless it has one: what AutoJs6 needs to run it as a project and build an APK. */
    private fun writeProjectConfig(dir: File, name: String) {
        val config = File(dir, PROJECT_CONFIG)
        if (!config.exists()) writeAtomically(config, projectConfigOf(name))
    }

    /** The drafts of the layout files of the document with draft key [key]: `drafts/<key>.files/<path>`. */
    fun layoutDraftsDir(key: String): File = File(draftsDir, requireValidName(key) + LAYOUT_DRAFTS_SUFFIX)

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
        writeAtomically(file(name), script)
    }

    fun delete(name: String) {
        if (isFolderName(name)) {
            projectDir(name)?.deleteRecursively()
            draftFile(name).parentFile?.deleteRecursively()
        } else {
            file(name).delete()
            draftFile(name).delete()
        }
        layoutDraftsDir(name).deleteRecursively()
        recordSource(name, null)
        setImageFolder(name, null)
    }

    /** Renames a project: a file to another file name, a folder project to another folder (`<new stem>/main.js`). */
    fun rename(name: String, newName: String): Project {
        require(isFolderName(name) == isFolderName(newName)) { "A project keeps its form when renamed: $name -> $newName" }
        val target = file(newName)
        val moving = if (isFolderName(name)) file(name).parentFile!! to target.parentFile!! else file(name) to target
        require(!moving.second.exists()) { "A project named $newName already exists" }
        check(moving.first.renameTo(moving.second)) { "Could not rename $name" }
        if (isFolderName(name)) {
            draftFile(name).parentFile?.let { if (it.exists()) it.renameTo(draftFile(newName).parentFile!!) }
        } else {
            draftFile(name).let { if (it.exists()) it.renameTo(draftFile(newName)) }
        }
        layoutDraftsDir(name).let { if (it.exists()) it.renameTo(layoutDraftsDir(newName)) }
        sourceOf(name)?.let { origin ->
            recordSource(name, null)
            recordSource(newName, origin)
        }
        imageFolderOf(name)?.let { folder ->
            setImageFolder(name, null)
            setImageFolder(newName, folder)
        }
        return Project(newName, target, target.lastModified(), kindOf(target))
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
        draftFile(name).parentFile?.mkdirs()
        draftFile(name).writeText(script, StandardCharsets.UTF_8)
    }

    fun clearDraft(name: String) {
        draftFile(name).delete()
        layoutDraftsDir(name).deleteRecursively()
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
        /** The script of a folder project (roadmap P11.3, appendix B). */
        const val MAIN_SCRIPT = "main.js"
        /** AutoJs6's project file in a folder project (roadmap D63). */
        const val PROJECT_CONFIG = "project.json"
        private const val LAYOUT_DRAFTS_SUFFIX = ".files"
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

        /** The folder template with an AutoJs6 XML layout file (roadmap P11.3 / D66, D67): the script and its files by path. */
        val UI_FILE_TEMPLATE: Pair<String, Map<String, String>> = Pair(
            """
                "ui";

                ui.layoutFile("layout.xml");

                ui.ok.on('click', () => {
                    toast('OK');
                });
            """.trimIndent() + LINE_END,
            mapOf(
                "layout.xml" to """
                    <vertical padding="16">
                        <text id="title" text="Hello" textSize="24sp"/>
                        <button id="ok" text="OK"/>
                    </vertical>
                """.trimIndent() + LINE_END,
            ),
        )

        /** The folder template with a native Android XML layout file (roadmap P11.3 / D67), laid out like AutoJs6's own samples. */
        val UI_ANDROID_TEMPLATE: Pair<String, Map<String, String>> = Pair(
            """
                "ui";

                ui.useAndroidLayout();
                ui.layoutFile("res/layout/activity_main.xml");

                ui.ok.on('click', () => {
                    toast('OK');
                });
            """.trimIndent() + LINE_END,
            mapOf(
                "res/layout/activity_main.xml" to """
                    <?xml version="1.0" encoding="utf-8"?>
                    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:orientation="vertical"
                        android:padding="16dp">

                        <TextView
                            android:id="@+id/title"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Hello"
                            android:textSize="24sp" />

                        <Button
                            android:id="@+id/ok"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="OK" />
                    </LinearLayout>
                """.trimIndent() + LINE_END,
            ),
        )

        /**
         * The `project.json` of a folder project named [name] (roadmap D63): the fields AutoJs6's complete check needs
         * (`name`, `main`, `packageName`, `versionName`, `versionCode`).
         */
        fun projectConfigOf(name: String): String = "{" + LINE_END +
            "  \"name\": " + JsonText.quote(name) + "," + LINE_END +
            "  \"main\": \"" + MAIN_SCRIPT + "\"," + LINE_END +
            "  \"packageName\": \"" + packageNameOf(name) + "\"," + LINE_END +
            "  \"versionName\": \"1.0.0\"," + LINE_END +
            "  \"versionCode\": 1" + LINE_END +
            "}" + LINE_END

        /** `org.example.<name>` with what a package name segment cannot hold replaced (`project` when nothing remains). */
        fun packageNameOf(name: String): String {
            val segment = name.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifEmpty { "project" }
            return "org.example." + if (segment[0].isLetter()) segment else "p$segment"
        }

        /** Types offered by the document picker when importing or relinking a script (any file as the last resort). */
        val SCRIPT_DOCUMENT_TYPES = arrayOf("application/javascript", "text/javascript", "application/x-javascript", "text/typescript", "text/plain", "*/*")
        /** A tab never occurs in a Uri string, so it separates the Uri from the baseline digest. */
        private const val FIELD_SEPARATOR = "\t"
        private const val LINE_END = "\n"
        private val invalid = Regex("[^A-Za-z0-9_.\\-\\u4e00-\\u9fff]")
        private val dots = Regex("\\.{2,}")
        private val valid = Regex("[A-Za-z0-9_\\-\\u4e00-\\u9fff][A-Za-z0-9_.\\-\\u4e00-\\u9fff]*\\.(js|tsx)")

        fun isValidName(name: String): Boolean {
            if (".." in name) return false
            if (!isFolderName(name)) return valid.matches(name)
            return valid.matches(name.substringBefore('/') + EXTENSION) && name.count { it == '/' } == 1
        }

        /** Whether [name] names a folder project (`<stem>/main.js`). */
        fun isFolderName(name: String): Boolean = name.endsWith("/" + MAIN_SCRIPT)

        /** The folder's name for a folder project, the file name otherwise. */
        fun displayNameOf(name: String): String = if (isFolderName(name)) name.substringBefore('/') else name

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
