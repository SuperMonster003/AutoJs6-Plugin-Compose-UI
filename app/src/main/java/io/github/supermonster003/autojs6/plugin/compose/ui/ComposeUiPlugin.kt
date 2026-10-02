package io.github.supermonster003.autojs6.plugin.compose.ui

import org.autojs.plugin.compose.api.ComposeUiContract
import org.autojs.plugin.compose.api.ComposeUiIds

/**
 * Identity constants shared by the manifest, the INFO service, the documentation and the tests.
 * They must stay identical to the host-side registration (roadmap D1 / D23 and section 4.4); the
 * JVM manifest contract test fails when the manifest drifts from them.
 *
 * The values reference the frozen V1 contract AAR. These const values are inlined so the INFO
 * service remains independent of the host-provided API classes in its own process.
 */
object ComposeUiPlugin {

    const val PACKAGE_NAME = ComposeUiIds.PLUGIN_PACKAGE_NAME
    const val HOST_PACKAGE_NAME = ComposeUiIds.HOST_PACKAGE_NAME

    const val ID = ComposeUiIds.PLUGIN_ID
    const val ENGINE = ComposeUiIds.ENGINE
    const val VARIANT = ComposeUiIds.VARIANT
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [ComposeUiPluginInfoService]; there is no Binder capability service (roadmap D23). */
    const val INFO_ACTION = ComposeUiContract.INFO_ACTION
    const val INFO_CATEGORY = ComposeUiContract.INFO_CATEGORY

    /** Signature permission guarding every exported component (manifest `android:permission`). */
    const val PLUGIN_PERMISSION = ComposeUiContract.PLUGIN_PERMISSION

    /** Application meta-data naming the renderer factory the host instantiates in process (roadmap D10). */
    const val META_RENDERER_FACTORY = ComposeUiContract.META_RENDERER_FACTORY
    const val RENDERER_FACTORY_CLASS_NAME = "$PACKAGE_NAME.renderer.ComposeUiRendererFactoryImpl"

    /** Application meta-data and capability key carrying the minimum host build. */
    const val META_REQUIRES_HOST_VERSION = ComposeUiContract.META_REQUIRES_HOST_VERSION

    /** Contract version this plugin targets (roadmap D29 / appendix B.6). */
    const val CONTRACT_VERSION = ComposeUiContract.CONTRACT_VERSION

    /**
     * Minimum AutoJs6 `versionCode`. Provisional (roadmap D29): the host snapshot this repository was
     * created against is 6.8.0 / 5307, so the first host build that can ship `compose-ui-api` and the
     * loader is 5308; roadmap P1.3 back-fills the confirmed value.
     */
    const val REQUIRED_HOST_VERSION = ComposeUiIds.REQUIRED_HOST_VERSION_CODE
}
