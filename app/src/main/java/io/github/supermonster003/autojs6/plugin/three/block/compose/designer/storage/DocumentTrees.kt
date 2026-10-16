package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

/**
 * Files of a document tree the user granted (roadmap P11.3): the folder of a content Uri document, where its layout
 * files are read and written by their `/`-separated path. Missing folders and files are created on write.
 */
internal object DocumentTrees {

    private const val XML_MIME_TYPE = "text/xml"

    /** Whether the tree's top folder holds a file named [name] (the document the folder was picked for). */
    fun contains(resolver: ContentResolver, tree: Uri, name: String): Boolean =
        child(resolver, tree, DocumentsContract.getTreeDocumentId(tree), name) != null

    /** The text of the file at [path] in [tree], or null when there is none. */
    fun read(resolver: ContentResolver, tree: Uri, path: String): String? {
        val id = find(resolver, tree, path) ?: return null
        return resolver.openInputStream(DocumentsContract.buildDocumentUriUsingTree(tree, id))?.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }

    /** Writes [text] to the file at [path] in [tree], creating its folders and itself when missing. */
    fun write(resolver: ContentResolver, tree: Uri, path: String, text: String): Boolean {
        val parts = path.split('/')
        var parent = DocumentsContract.getTreeDocumentId(tree)
        for (folder in parts.dropLast(1)) {
            parent = child(resolver, tree, parent, folder)
                ?: create(resolver, tree, parent, DocumentsContract.Document.MIME_TYPE_DIR, folder)
                ?: return false
        }
        val file = child(resolver, tree, parent, parts.last()) ?: create(resolver, tree, parent, XML_MIME_TYPE, parts.last()) ?: return false
        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, file)
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val stream = try {
            resolver.openOutputStream(uri, "wt")
        } catch (error: FileNotFoundException) {
            null
        } catch (error: IllegalArgumentException) {
            null
        } ?: resolver.openOutputStream(uri, "w") ?: return false
        stream.use { it.write(bytes) }
        return true
    }

    private fun find(resolver: ContentResolver, tree: Uri, path: String): String? {
        var id = DocumentsContract.getTreeDocumentId(tree)
        for (part in path.split('/')) id = child(resolver, tree, id, part) ?: return null
        return id
    }

    private fun child(resolver: ContentResolver, tree: Uri, parent: String, name: String): String? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        resolver.query(children, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) if (cursor.getString(1) == name) return cursor.getString(0)
        }
        return null
    }

    private fun create(resolver: ContentResolver, tree: Uri, parent: String, mimeType: String, name: String): String? =
        DocumentsContract.createDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, parent), mimeType, name)
            ?.let(DocumentsContract::getDocumentId)
}
