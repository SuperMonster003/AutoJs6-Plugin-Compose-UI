package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.ProjectArchives
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * What the designer edits (roadmap D39): a project file in the plugin's private directory, or an external
 * document handed over by the host editor or a file manager through a content or file Uri. Drafts (D40)
 * exist for both; an external document's draft is keyed by a digest of its Uri.
 */
internal sealed class DocumentSource {
    abstract val title: String
    /** A valid project-style file name under which drafts of this document are kept. */
    abstract val draftKey: String

    data class Local(val name: String) : DocumentSource() {
        override val title: String get() = name
        override val draftKey: String get() = name
    }

    data class External(val uri: Uri, val displayName: String, val writable: Boolean) : DocumentSource() {
        override val title: String get() = displayName
        override val draftKey: String get() = "external-" + sha1(uri.toString()).take(20) + ProjectStore.extensionFor(displayName)
    }

    companion object {
        private fun sha1(text: String): String =
            MessageDigest.getInstance("SHA-1").digest(text.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

/** Reads and writes a [DocumentSource]; the activity never touches files or resolvers directly. */
internal class DocumentIo(private val context: Context, private val store: ProjectStore) {
    private companion object {
        const val EXPORT_DIRECTORY = "designer-export"
        const val EXPORT_ARCHIVE = "export"
        /** Hidden inside the projects folder (never a project), so the FileProvider's one path covers it. */
        const val HANDOFF_DIRECTORY = ".handoff"
    }

    sealed class WriteResult {
        /** Written in place. */
        object Saved : WriteResult()
        /** The external document could not be written; the text was saved as a new private project instead. */
        data class SavedAsProject(val name: String) : WriteResult()
    }

    /** Describes the document an EDIT / VIEW intent carries, or null when it carries none. */
    fun external(intent: Intent): DocumentSource.External? {
        val uri = intent.data ?: return null
        val displayName = displayName(uri)
        val writable = when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> uri.path?.let { File(it).canWrite() } == true
            else -> (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0 ||
                context.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED
        }
        return DocumentSource.External(uri, displayName, writable)
    }

    @Throws(IOException::class, SecurityException::class)
    fun read(source: DocumentSource): String = when (source) {
        is DocumentSource.Local -> store.read(source.name)
        is DocumentSource.External -> readUriOrThrow(source.uri)
    }

    /** The text behind [uri], or null when it cannot be read (gone, or no grant). */
    fun readUri(uri: Uri): String? = try { readUriOrThrow(uri) } catch (error: IOException) { null } catch (error: SecurityException) { null }

    private fun readUriOrThrow(uri: Uri): String = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        ?: throw FileNotFoundException(uri.toString())

    /** The current text when it can be read, or null (a document that vanished or lost its grant). */
    fun readOrNull(source: DocumentSource): String? = try { read(source) } catch (error: IOException) { null } catch (error: SecurityException) { null }

    fun write(source: DocumentSource, text: String): WriteResult = when (source) {
        is DocumentSource.Local -> { store.write(source.name, text); WriteResult.Saved }
        is DocumentSource.External -> if (source.writable && writeUri(source.uri, text)) WriteResult.Saved else {
            WriteResult.SavedAsProject(store.create(source.displayName.substringBeforeLast('.').ifBlank { "imported" }, text, ProjectStore.extensionFor(source.displayName)).name)
        }
    }

    /**
     * The draft of the script when it differs from the saved document, or when drafts of layout files exist
     * ([pendingDraftFiles]); null otherwise.
     */
    fun pendingDraft(source: DocumentSource): String? {
        val files = pendingDraftFiles(source).isNotEmpty()
        return when (source) {
            is DocumentSource.Local -> store.pendingDraft(source.name) ?: if (files) store.draftFile(source.name).takeIf { it.isFile }?.readText(StandardCharsets.UTF_8) else null
            is DocumentSource.External -> {
                val draft = store.draftFile(source.draftKey)
                if (!draft.isFile) null else draft.readText(StandardCharsets.UTF_8).takeIf { files || it != readOrNull(source) }
            }
        }
    }

    /** The drafts of layout files of the document (roadmap P11.3) by the path the script writes: edits not yet saved. */
    fun pendingDraftFiles(source: DocumentSource): Map<String, String> {
        val dir = store.layoutDraftsDir(source.draftKey)
        if (!dir.isDirectory) return emptyMap()
        return dir.walkTopDown().filter { it.isFile }.associate { file ->
            file.relativeTo(dir).invariantSeparatorsPath to file.readText(StandardCharsets.UTF_8)
        }
    }

    fun writeDraft(source: DocumentSource, text: String) = store.writeDraft(source.draftKey, text)

    /** The script's draft and the drafts of the layout files that differ from the saved ones ([files] by path). */
    fun writeDraft(source: DocumentSource, text: String, files: Map<String, String>) {
        store.writeDraft(source.draftKey, text)
        val dir = store.layoutDraftsDir(source.draftKey)
        dir.deleteRecursively()
        for ((path, content) in files) {
            val file = File(dir, path).canonicalFile
            if (!file.path.startsWith(dir.canonicalPath + File.separator)) continue
            file.parentFile?.mkdirs()
            file.writeText(content, StandardCharsets.UTF_8)
        }
    }

    fun clearDraft(source: DocumentSource) = store.clearDraft(source.draftKey)

    /**
     * The text of the layout file at [path] next to the document (roadmap P11.3): in a folder project's folder, or
     * beside a file Uri; null when it cannot be read (a single-file project, a content Uri without access to its
     * folder, a missing file).
     */
    fun readLayoutFile(source: DocumentSource, path: String): String? = try {
        val tree = documentFolderOf(source)
        if (tree != null) DocumentTrees.read(context.contentResolver, tree, path) else layoutFileOf(source, path)?.takeIf { it.isFile }?.readText(StandardCharsets.UTF_8)
    } catch (error: IOException) {
        null
    } catch (error: SecurityException) {
        null
    }

    /** Writes the layout file at [path] next to the document; false when there is no such place or it cannot be written. */
    fun writeLayoutFile(source: DocumentSource, path: String, text: String): Boolean = try {
        when (source) {
            is DocumentSource.Local -> store.writeLayoutFile(source.name, path, text)
            is DocumentSource.External -> documentFolderOf(source)?.let { tree -> DocumentTrees.write(context.contentResolver, tree, path, text) }
                ?: layoutFileOf(source, path)?.let { file -> file.writeText(text, StandardCharsets.UTF_8); true } ?: false
        }
    } catch (error: IOException) {
        false
    } catch (error: SecurityException) {
        false
    }

    /**
     * The folder the user granted for a content Uri document's layout files (roadmap P11.3), or null: a private project
     * or a file Uri has its folder already, and a relative path that leaves the folder is not resolved in a tree.
     */
    fun documentFolderOf(source: DocumentSource): Uri? =
        (source as? DocumentSource.External)?.takeIf { it.uri.scheme == ContentResolver.SCHEME_CONTENT }?.let { store.documentFolderOf(it.draftKey) }

    /** Whether layout files of [source] need a folder the user has not granted yet (a content Uri document). */
    fun needsDocumentFolder(source: DocumentSource): Boolean =
        source is DocumentSource.External && source.uri.scheme == ContentResolver.SCHEME_CONTENT && store.documentFolderOf(source.draftKey) == null

    /**
     * Records [tree] (picked by the user, read and write granted) as the folder of the content Uri document [source],
     * when its top folder holds a file of the document's name; false otherwise.
     */
    fun grantDocumentFolder(source: DocumentSource.External, tree: Uri): Boolean {
        val resolver = context.contentResolver
        if (!runCatching { DocumentTrees.contains(resolver, tree, source.displayName) }.getOrDefault(false)) return false
        try {
            resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (error: SecurityException) {
            // Not persistable: the folder works until the grant lapses.
        }
        store.setDocumentFolder(source.draftKey, tree)
        return true
    }

    /**
     * Where the document's files are for the preview's zips (roadmap P11.3): the whole folder of a folder project,
     * the `res/` directory beside a file Uri; null for a single-file private project or a content Uri.
     */
    fun projectRootOf(source: DocumentSource): ProjectArchives.Root? = when (source) {
        is DocumentSource.Local -> store.projectDir(source.name)?.let { ProjectArchives.Root(it, whole = true) }
        is DocumentSource.External -> if (source.uri.scheme == ContentResolver.SCHEME_FILE) {
            source.uri.path?.let { File(it).parentFile }?.let { ProjectArchives.Root(it, whole = false) }
        } else {
            null
        }
    }

    /**
     * The zip a document with layout files is exported as (roadmap P11.3 / D62): its folder's files (a folder project
     * whole, the `res/` directory beside a file Uri) with [script] as `main.js` and the current [files], plus a
     * `project.json` when there is none; null when it cannot be made (over the limits).
     */
    fun exportArchive(source: DocumentSource, script: String, files: Map<String, String>): File? {
        val root = projectRootOf(source)
        val texts = LinkedHashMap(files)
        texts[ProjectStore.MAIN_SCRIPT] = script
        val name = when (source) {
            is DocumentSource.Local -> ProjectStore.displayNameOf(source.name).removeSuffix(ProjectStore.EXTENSION)
            is DocumentSource.External -> source.displayName.substringBeforeLast('.')
        }
        if (root == null || !root.whole || !File(root.dir, ProjectStore.PROJECT_CONFIG).isFile) texts[ProjectStore.PROJECT_CONFIG] = ProjectStore.projectConfigOf(name)
        return ProjectArchives(File(context.cacheDir, EXPORT_DIRECTORY)).archive(EXPORT_ARCHIVE, root, texts)?.file
    }

    /**
     * The zip a document with layout files is handed to the host as (roadmap P11.3 / D64): [exportArchive] copied to
     * `projects/.handoff/<name>.zip`, where the designer's FileProvider can grant it; null when it cannot be made.
     */
    fun handoffArchive(source: DocumentSource, script: String, files: Map<String, String>): File? {
        val archive = exportArchive(source, script, files) ?: return null
        val name = when (source) {
            is DocumentSource.Local -> ProjectStore.displayNameOf(source.name).removeSuffix(ProjectStore.EXTENSION)
            is DocumentSource.External -> source.displayName.substringBeforeLast('.')
        }
        val target = File(File(store.projectsDir, HANDOFF_DIRECTORY), ProjectStore.stemOf(name, "project") + ProjectStore.ZIP_EXTENSION)
        target.parentFile?.mkdirs()
        archive.copyTo(target, overwrite = true)
        return target
    }

    /** The script's path inside a project zip: `main.js` of a folder project, else the script's file name. */
    fun scriptPathOf(source: DocumentSource): String = when (source) {
        is DocumentSource.Local -> source.name.substringAfterLast('/')
        is DocumentSource.External -> source.displayName.substringAfterLast('/')
    }

    private fun layoutFileOf(source: DocumentSource, path: String): File? = when (source) {
        is DocumentSource.Local -> store.layoutFile(source.name, path)
        is DocumentSource.External -> if (source.uri.scheme == ContentResolver.SCHEME_FILE) {
            source.uri.path?.let { File(it).parentFile }?.let { dir -> if (File(path).isAbsolute) File(path) else File(dir, path) }
        } else {
            null
        }
    }

    /** The Uri other apps may receive for this document, with whether a write grant makes sense. */
    fun shareUri(source: DocumentSource): Pair<Uri, Boolean> = when (source) {
        is DocumentSource.Local -> io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptLauncher.projectUri(context, store.file(source.name)) to true
        is DocumentSource.External -> source.uri to source.writable
    }

    /** Replaces the text behind [uri]; false when the provider refuses (read only, gone, no grant). */
    fun writeUri(uri: Uri, text: String): Boolean {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        for (mode in arrayOf("wt", "w")) {
            try {
                val stream = context.contentResolver.openOutputStream(uri, mode) ?: return false
                stream.use { it.write(bytes) }
                return true
            } catch (error: FileNotFoundException) {
                return false
            } catch (error: SecurityException) {
                return false
            } catch (error: UnsupportedOperationException) {
                // Some providers reject the truncating mode; "w" is retried below.
            } catch (error: IOException) {
                return false
            }
        }
        return false
    }

    private fun displayName(uri: Uri): String {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return uri.lastPathSegment ?: "document.js"
        val queried = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (error: SecurityException) { null } catch (error: IllegalArgumentException) { null }
        return queried?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "document.js"
    }
}
