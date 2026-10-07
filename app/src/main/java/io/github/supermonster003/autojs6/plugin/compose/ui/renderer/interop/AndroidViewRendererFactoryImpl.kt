package io.github.supermonster003.autojs6.plugin.compose.ui.renderer.interop

import android.os.Bundle
import android.view.View
import io.github.supermonster003.autojs6.plugin.compose.ui.renderer.ComposeUiRendererImpl
import io.github.supermonster003.autojs6.plugin.compose.ui.renderer.RendererPresentation
import org.autojs.plugin.compose.api.interop.AndroidViewBindingV1
import org.autojs.plugin.compose.api.interop.AndroidViewInteropV1
import org.autojs.plugin.compose.api.interop.AndroidViewRendererFactoryV1
import org.autojs.plugin.compose.api.interop.AndroidViewRendererV1
import org.autojs.plugin.compose.api.loading.ComposeUiHostEnvironment
import org.autojs.plugin.compose.api.model.ThemeSpec
import org.autojs.plugin.compose.api.model.UiCommand
import org.autojs.plugin.compose.api.model.UiPatchBatch

/** Optional entry. No base-factory class literal or base renderer signature references this class. */
class AndroidViewRendererFactoryImpl : AndroidViewRendererFactoryV1 {
    override fun extensionVersion() = AndroidViewInteropV1.VERSION
    override fun create(environment: ComposeUiHostEnvironment): AndroidViewRendererV1 = AndroidViewRenderer(environment)
}

internal class AndroidViewRenderer(environment: ComposeUiHostEnvironment,
    presentation: RendererPresentation? = null) : AndroidViewRendererV1 {
    private lateinit var delegate: ComposeUiRendererImpl
    private val content = AndroidViewContent(this, { delegate.currentView() }, { delegate.reportFailure(it) })

    init { delegate = ComposeUiRendererImpl(environment, content, presentation) }

    override fun view(): View = delegate.view()
    override fun execute(command: UiCommand) = delegate.execute(command)
    override fun setTheme(theme: ThemeSpec) = delegate.setTheme(theme)
    override fun dispose() = delegate.dispose()
    internal fun enqueueSystemEvent(type: String, payload: Bundle = Bundle()) = delegate.enqueueSystemEvent(type, payload)
    internal fun reportFailure(failure: Throwable) = delegate.reportFailure(failure)

    /** Without an explicit binding snapshot, optional nodes cannot enter through the V1 method. */
    override fun apply(batch: UiPatchBatch) = applyWithViews(batch, emptyList())

    override fun applyWithViews(batch: UiPatchBatch, bindings: List<AndroidViewBindingV1>) {
        val tree = delegate.preview(batch)
        val views = content.prepare(tree.tree, bindings)
        content.commit(views, delegate::prepareCommit) { delegate.commit(tree) }
    }
}
