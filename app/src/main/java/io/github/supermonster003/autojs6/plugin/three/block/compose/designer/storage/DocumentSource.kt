package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
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
        override val draftKey: String get() = "external-" + sha1(uri.toString()).take(20) + ProjectStore.EXTENSION
    }

    companion object {
        private fun sha1(text: String): String =
            MessageDigest.getInstance("SHA-1").digest(text.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

/** Reads and writes a [DocumentSource]; the activity never touches files or resolvers directly. */
internal class DocumentIo(private val context: Context, private val store: ProjectStore) {
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
        is DocumentSource.External -> context.contentResolver.openInputStream(source.uri)?.use { it.readBytes().toString(StandardCharsets.UTF_8) }
            ?: throw FileNotFoundException(source.uri.toString())
    }

    /** The current text when it can be read, or null (a document that vanished or lost its grant). */
    fun readOrNull(source: DocumentSource): String? = try { read(source) } catch (error: IOException) { null } catch (error: SecurityException) { null }

    fun write(source: DocumentSource, text: String): WriteResult = when (source) {
        is DocumentSource.Local -> { store.write(source.name, text); WriteResult.Saved }
        is DocumentSource.External -> if (source.writable && writeExternal(source.uri, text)) WriteResult.Saved else {
            WriteResult.SavedAsProject(store.create(source.displayName.substringBeforeLast('.').ifBlank { "imported" }, text).name)
        }
    }

    fun pendingDraft(source: DocumentSource): String? = when (source) {
        is DocumentSource.Local -> store.pendingDraft(source.name)
        is DocumentSource.External -> {
            val draft = store.draftFile(source.draftKey)
            if (!draft.isFile) null else draft.readText(StandardCharsets.UTF_8).takeIf { it != readOrNull(source) }
        }
    }

    fun writeDraft(source: DocumentSource, text: String) = store.writeDraft(source.draftKey, text)
    fun clearDraft(source: DocumentSource) = store.clearDraft(source.draftKey)

    /** The Uri other apps may receive for this document, with whether a write grant makes sense. */
    fun shareUri(source: DocumentSource): Pair<Uri, Boolean> = when (source) {
        is DocumentSource.Local -> io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptLauncher.projectUri(context, store.file(source.name)) to true
        is DocumentSource.External -> source.uri to source.writable
    }

    private fun writeExternal(uri: Uri, text: String): Boolean {
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
