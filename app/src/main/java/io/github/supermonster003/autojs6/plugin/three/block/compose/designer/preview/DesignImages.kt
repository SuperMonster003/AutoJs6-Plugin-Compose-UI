package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.DocumentsContract
import io.github.supermonster003.autojs6.plugin.three.block.compose.ThreeBlockComposePlugin
import org.autojs.plugin.compose.api.model.UiValue
import java.io.File
import java.io.IOException
import java.io.InputStream

/** What a design-time Image `src` string stands for: a bitmap the renderer can draw, or why it has none. */
internal sealed class ImageLookup {
    class Found(val value: UiValue) : ImageLookup()
    class Missing(val problem: String) : ImageLookup()
}

/** Resolves the literal `src` strings of Image nodes for the design canvas. */
internal fun interface ImageSource {
    fun resolve(src: String): ImageLookup
}

/**
 * Design-time image sources (roadmap P9.1, D44), mirroring the host's rules where the designer can: a
 * `@drawable/name` comes from AutoJs6's resources (no permission; the host package is in `<queries>`), URLs
 * (anything with `://`) are refused as the host refuses them, and a relative path is looked up in the image
 * [folder] the user granted for the document (a document tree, or a file Uri). Absolute paths would need a
 * storage permission the plugin does not request, so they stay placeholders. Decoded bitmaps are downsampled
 * and held here: contract bitmap handles are weak, so this cache keeps them alive while the editor is open.
 */
internal class DesignImages(private val context: Context, private val folder: Uri?) : ImageSource {
    private val cache = HashMap<String, ImageLookup>()
    /** Strong references for the weak contract handles of every bitmap handed to the renderer. */
    private val bitmaps = ArrayList<Bitmap>()

    override fun resolve(src: String): ImageLookup = cache.getOrPut(src) { load(src) }

    private fun load(src: String): ImageLookup {
        val bitmap = when {
            src.startsWith(DRAWABLE_PREFIX) -> drawable(src.removePrefix(DRAWABLE_PREFIX))
                ?: return ImageLookup.Missing("Image: $src is not a drawable of an installed AutoJs6")
            "://" in src -> return ImageLookup.Missing("Image: $src is a URL; URLs are not loaded")
            src.startsWith("/") -> return ImageLookup.Missing("Image: absolute path $src is not previewed")
            folder == null -> return ImageLookup.Missing("Image: choose an image folder to preview $src")
            else -> relative(folder, src) ?: return ImageLookup.Missing("Image: $src not found in the image folder")
        }
        bitmaps += bitmap
        return ImageLookup.Found(UiValue.BitmapRef(bitmap))
    }

    private fun drawable(name: String): Bitmap? = try {
        val resources = context.packageManager.getResourcesForApplication(ThreeBlockComposePlugin.HOST_PACKAGE_NAME)
        @Suppress("DiscouragedApi") // the host's drawable names are what scripts write; there is no R class for them
        val id = resources.getIdentifier(name, "drawable", ThreeBlockComposePlugin.HOST_PACKAGE_NAME)
        if (id == 0) null else when (val drawable = resources.getDrawable(id, null)) {
            is BitmapDrawable -> drawable.bitmap
            else -> {
                val width = drawable.intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(MAX_SIDE) ?: DEFAULT_SIDE
                val height = drawable.intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(MAX_SIDE) ?: DEFAULT_SIDE
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(Canvas(bitmap))
                }
            }
        }
    } catch (error: PackageManager.NameNotFoundException) {
        null
    } catch (error: RuntimeException) {
        null
    }

    private fun relative(folder: Uri, path: String): Bitmap? {
        val segments = path.split('/').filter { it.isNotEmpty() && it != "." }
        if (segments.isEmpty() || ".." in segments) return null
        return when (folder.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val root = File(folder.path ?: return null).canonicalFile
                val file = File(root, segments.joinToString("/")).canonicalFile
                if (!file.path.startsWith(root.path + File.separator) || !file.isFile) null else decode { file.inputStream() }
            }
            ContentResolver.SCHEME_CONTENT -> treeDocument(folder, segments)?.let { document -> decode { context.contentResolver.openInputStream(document) } }
            else -> null
        }
    }

    /** Walks [segments] down the granted document tree by display name; null when one is missing. */
    private fun treeDocument(tree: Uri, segments: List<String>): Uri? = try {
        var documentId = DocumentsContract.getTreeDocumentId(tree)
        for (segment in segments) {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
            val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            documentId = context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
                var found: String? = null
                while (found == null && cursor.moveToNext()) if (cursor.getString(1) == segment) found = cursor.getString(0)
                found
            } ?: return null
        }
        DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
    } catch (error: RuntimeException) {
        null
    }

    /** Decodes with a power-of-two sample size that keeps the longer side within [MAX_SIDE] pixels. */
    private fun decode(open: () -> InputStream?): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // A bounds-only decode returns null by design; the size arrives in the options.
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null else {
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
            open()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }
    } catch (error: IOException) {
        null
    } catch (error: SecurityException) {
        null
    } catch (error: OutOfMemoryError) {
        null
    }

    private companion object {
        const val DRAWABLE_PREFIX = "@drawable/"
        const val MAX_SIDE = 1024
        const val DEFAULT_SIDE = 96
    }
}
