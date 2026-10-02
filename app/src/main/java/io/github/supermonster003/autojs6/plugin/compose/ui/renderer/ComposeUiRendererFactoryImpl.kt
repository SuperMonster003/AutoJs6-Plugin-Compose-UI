package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import io.github.supermonster003.autojs6.plugin.compose.ui.BuildConfig
import kotlinx.coroutines.Job
import org.autojs.plugin.compose.api.spike.ComposeUiEventSink
import org.autojs.plugin.compose.api.spike.ComposeUiRenderer
import org.autojs.plugin.compose.api.spike.ComposeUiRendererFactory
import org.autojs.plugin.compose.api.spike.SpikeContract

/** P0 draft only. A negative contract version prevents a V1 host from treating this as a frozen API. */
class ComposeUiRendererFactoryImpl : ComposeUiRendererFactory {
    override fun contractVersion() = SpikeContract.VERSION
    override fun create(context: Context, eventSink: ComposeUiEventSink): ComposeUiRenderer = SpikeRenderer(context, eventSink)

    override fun classOrigins(): Map<String, Class<*>> = mapOf(
        "compose" to ComposeView::class.java,
        "runtime" to Recomposer::class.java,
        "lifecycle" to Lifecycle::class.java,
        "savedstate" to SavedStateRegistry::class.java,
        "core" to ViewCompat::class.java,
        "activity" to ComponentActivity::class.java,
        "appcompat" to AppCompatActivity::class.java,
        "kotlin" to Unit::class.java,
        "coroutineContext" to kotlin.coroutines.CoroutineContext::class.java,
        "coroutines" to Job::class.java,
        "contract" to ComposeUiRendererFactory::class.java,
    )

    @SuppressLint("DiscouragedApi") // Intentional diagnostic name lookup, retained by compose_spike_keep.xml.
    override fun probe(context: Context) = Bundle().apply {
        // On API 24-33 this executes the native helper through THIS class loader, not System.loadLibrary in a test.
        val path = Path().apply { moveTo(0f, 0f); lineTo(20f, 20f); close() }
        val iterator = PathIterator(path)
        var segments = 0
        while (iterator.hasNext()) { iterator.next(); segments++ }
        putInt("pathSegments", segments)
        // Public Resources lookup audits the pinned Material resource table without linking a private R API.
        // The name is diagnostic only; production components resolve their own library resources.
        val dialogId = context.resources.getIdentifier("m3c_dialog", "string", SpikeContract.PACKAGE)
        putString("dialog", context.getString(dialogId))
        putString("fingerprint", BuildConfig.SHARED_DEPS_FINGERPRINT)
        putInt("densityDpi", context.resources.configuration.densityDpi)
        putInt("uiMode", context.resources.configuration.uiMode)
        putString("language", context.resources.configuration.locales[0].language)
    }
}
