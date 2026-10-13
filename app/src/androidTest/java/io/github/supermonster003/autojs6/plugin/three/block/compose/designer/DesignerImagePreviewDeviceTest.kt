package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptLauncher
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.DesignImages
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.ImageLookup
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.DesignerActivity
import org.autojs.plugin.compose.api.model.UiValue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Roadmap P9.1 / D44: Image sources on the design canvas. A file Uri stands in for the document tree a user
 * would pick; the lookup by path segments is the same for both.
 */
@RunWith(AndroidJUnit4::class)
internal class DesignerImagePreviewDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ProjectStore(context)
    private val folder = File(context.cacheDir, "image-test")

    @Before fun writeImage() {
        folder.deleteRecursively()
        val pictures = File(folder, "pics").apply { mkdirs() }
        val bitmap = Bitmap.createBitmap(6, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        File(pictures, "logo.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @After fun clean() {
        folder.deleteRecursively()
        store.list().filter { it.name.startsWith("image-test-") }.forEach { store.delete(it.name) }
    }

    @Test
    fun sourcesResolveLikeTheHostWherePossible() {
        val images = DesignImages(context, Uri.fromFile(folder))
        val found = images.resolve("pics/logo.png")
        assertTrue(found is ImageLookup.Found)
        val bitmap = checkNotNull(((found as ImageLookup.Found).value as UiValue.BitmapRef).get())
        assertEquals(6, bitmap.width)
        assertEquals(4, bitmap.height)
        // One source keeps one bitmap: a rebuilt preview neither decodes again nor registers another handle.
        assertSame(found, images.resolve("pics/logo.png"))
        for (refused in listOf("pics/missing.png", "../image-test/pics/logo.png", "https://example.com/logo.png", "/sdcard/logo.png")) {
            assertTrue(refused, images.resolve(refused) is ImageLookup.Missing)
        }
        assertTrue(DesignImages(context, null).resolve("pics/logo.png") is ImageLookup.Missing)
    }

    @Test
    fun hostDrawablesResolveWhenAutoJs6IsInstalled() {
        assumeTrue("AutoJs6 is not installed", ScriptLauncher.isHostInstalled(context))
        val images = DesignImages(context, null)
        val found = images.resolve("@drawable/ic_about")
        assertTrue(found is ImageLookup.Found)
        assertTrue(checkNotNull(((found as ImageLookup.Found).value as UiValue.BitmapRef).get()).width > 0)
        assertTrue(images.resolve("@drawable/no_such_drawable_in_autojs6") is ImageLookup.Missing)
    }

    @Test
    fun theImageFolderMenuTurnsThePlaceholderIntoTheImage() {
        val name = store.create("image-test-canvas", "compose.mount(compose.Column({padding: 16},\n    compose.Image({key: 'logo', src: 'pics/logo.png', w: 48, h: 32})));\n").name
        val placeholder = hasText("[Image]") and hasAnyAncestor(hasTestTag("designer-canvas-content"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch<DesignerActivity>(DesignerActivity.intent(context, name)).use {
            rule.waitUntil(5_000) { rule.onAllNodes(placeholder).fetchSemanticsNodes().isNotEmpty() }
            // Stands in for the system folder picker. Added only now: a filter with an action and no data also
            // matches intents without an action, such as the one that started this editor.
            val picker = instrumentation.addMonitor(
                IntentFilter(Intent.ACTION_OPEN_DOCUMENT_TREE),
                Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(folder))),
                true,
            )
            try {
                rule.onNodeWithTag("designer-menu").performClick()
                rule.onNodeWithTag("designer-menu-image-folder").performScrollTo().performClick()
                rule.waitUntil(5_000) { rule.onAllNodes(placeholder).fetchSemanticsNodes().isEmpty() }
                assertEquals(1, picker.hits)
            } finally {
                instrumentation.removeMonitor(picker)
            }
        }
        assertEquals(Uri.fromFile(folder), store.imageFolderOf(name))
    }
}
