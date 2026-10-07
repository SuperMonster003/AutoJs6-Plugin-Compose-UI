package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.compose.ui.BuildConfig
import org.autojs.plugin.compose.api.ComposeUiCapabilityKeys as K
import org.autojs.plugin.compose.api.ComposeUiContract
import org.autojs.plugin.compose.api.loading.ComposeUiHostEnvironment
import org.autojs.plugin.compose.api.loading.ComposeUiRendererFactory
import org.autojs.plugin.compose.api.v2.ComposeUiV2

/** V2 entry. V1 components retain their frozen definitions and renderer behavior. */
class ComposeUiRendererFactoryImpl : ComposeUiRendererFactory {
    override fun contractVersion() = ComposeUiContract.CONTRACT_VERSION
    override fun capabilities() = Bundle().apply {
        putInt(K.CONTRACT_VERSION, contractVersion())
        putStringArrayList(K.COMPONENTS, ArrayList(RendererCatalog.components))
        putStringArrayList(K.FEATURES, arrayListOf("android-view-interop-v1", "dialog-v1", ComposeUiV2.FEATURE))
        putString("androidViewFactoryV1", "io.github.supermonster003.autojs6.plugin.compose.ui.renderer.interop.AndroidViewRendererFactoryImpl")
        putString("dialogFactoryV1", "io.github.supermonster003.autojs6.plugin.compose.ui.renderer.dialog.DialogRendererFactoryImpl")
        putString(K.COMPOSE_VERSION, BuildConfig.COMPOSE_VERSION)
        putString(K.SHARED_DEPS_FINGERPRINT, BuildConfig.SHARED_DEPS_FINGERPRINT)
    }
    override fun create(environment: ComposeUiHostEnvironment) = ComposeUiRendererImpl(environment)
}
