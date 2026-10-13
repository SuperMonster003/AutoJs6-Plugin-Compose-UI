package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage

import android.content.ContentResolver
import android.net.Uri

/**
 * Keeps an imported project and the document it came from in step, only when asked (roadmap P9.1, D45):
 * the project can be written back to the source or replaced by it. The digest recorded at the last sync
 * tells which side changed since, so neither action silently drops the other side's edits.
 */
internal class SourceSync(private val resolver: ContentResolver, private val store: ProjectStore, private val io: DocumentIo) {
    sealed class State {
        /** The project was not imported through the picker. */
        object None : State()
        /** The source cannot be read any more: moved, deleted, or its grant was revoked. */
        data class Unavailable(val source: ProjectStore.Source) : State()
        /**
         * The source is readable. A side counts as changed when its text differs from the last sync; without a
         * recorded baseline (a 1.2.0 import) both sides count as changed unless their texts are equal.
         */
        data class Ready(val sourceText: String, val sourceChanged: Boolean, val projectChanged: Boolean) : State()
    }

    fun state(name: String): State {
        val source = store.sourceOf(name) ?: return State.None
        val sourceText = io.readUri(source.uri) ?: return State.Unavailable(source)
        val sourceDigest = ProjectStore.digest(sourceText)
        val projectDigest = ProjectStore.digest(store.read(name))
        val reference = source.baseline ?: sourceDigest.takeIf { it == projectDigest }
        return State.Ready(sourceText, sourceChanged = sourceDigest != reference, projectChanged = projectDigest != reference)
    }

    /** Writes [text] (the saved project) to the source; false when the source refuses the write. */
    fun writeBack(name: String, text: String): Boolean {
        val source = store.sourceOf(name) ?: return false
        if (!io.writeUri(source.uri, text)) return false
        store.markSynced(name, ProjectStore.digest(text))
        return true
    }

    /** Saves [text] read from the source as the project and records both sides as in step. */
    fun reimported(name: String, text: String) {
        store.write(name, text)
        store.markSynced(name, ProjectStore.digest(text))
    }

    /** Links [name] to [uri], picked again after the old link broke; the next sync compares the two texts. */
    fun relink(name: String, uri: Uri) = store.relink(resolver, name, uri, baseline = null)
}
