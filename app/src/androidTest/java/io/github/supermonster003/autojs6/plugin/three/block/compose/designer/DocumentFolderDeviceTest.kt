package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.DocumentTrees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Roadmap P11.3: the folder a person granted for a content Uri document through the system folder picker (which
 * an instrumented test cannot drive) still works for the plugin afterwards: the layout file reads back and a file in
 * a new sub-folder is written. Opt-in (`-e documentFolderCheck <folder name>`), run by `.python/verify_document_folder.py
 * check` after the grant; the folder is the last one the designer recorded whose Uri names that folder.
 */
@RunWith(AndroidJUnit4::class)
internal class DocumentFolderDeviceTest {

    @Test
    fun theGrantedFolderReadsAndWritesLayoutFiles() {
        val folder = InstrumentationRegistry.getArguments().getString("documentFolderCheck")
        assumeTrue("opt-in: -e documentFolderCheck <folder name>", !folder.isNullOrEmpty())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val records = File(context.filesDir, "designer/folders.properties")
        val tree = records.takeIf { it.isFile }?.readLines()?.mapNotNull { line -> line.substringAfter('=', "").takeIf { it.isNotEmpty() } }
            ?.lastOrNull { Uri.decode(it).contains(folder!!) }?.let(Uri::parse)
        assertTrue("no folder recorded for $folder", tree != null)
        val resolver = context.contentResolver
        assertTrue("the grant did not persist", resolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission && it.isWritePermission })
        val layout = DocumentTrees.read(resolver, tree!!, "layout.xml")
        assertTrue(layout.orEmpty(), layout?.contains("Hello from layout.xml") == true)
        assertTrue(DocumentTrees.write(resolver, tree, "res/layout/probe.xml", PROBE))
        assertEquals(PROBE, DocumentTrees.read(resolver, tree, "res/layout/probe.xml"))
    }

    private companion object {
        const val PROBE = "<frame/>\n"
    }
}
