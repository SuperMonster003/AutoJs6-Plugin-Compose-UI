package io.github.supermonster003.autojs6.plugin.compose.ui

import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.common.api.PluginCapabilityKeys

/**
 * Identity constants shared by the manifest, the INFO service, the documentation and the tests.
 * They must stay identical to the host-side registration (roadmap D1 / D23 and section 4.4); the
 * JVM manifest contract test fails when the manifest drifts from them.
 *
 * Until the host `compose-ui-api` contract module exists (roadmap P1.1) the values are literals;
 * from then on they reference `ComposeUiIds` / `ComposeUiContract` of the staged contract AAR,
 * exactly as 3-Shell Terminal references `TerminalIds` / `TerminalContract`.
 */
object ComposeUiPlugin {

    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.compose.ui"
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"

    const val ID = "compose-ui"
    const val ENGINE = "compose"
    const val VARIANT = "default"
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [ComposeUiPluginInfoService]; there is no Binder capability service (roadmap D23). */
    const val INFO_ACTION = PluginActions.INFO
    const val INFO_CATEGORY = "compose-ui"

    /** Signature permission guarding every exported component (manifest `android:permission`). */
    const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"

    /** Application meta-data naming the renderer factory the host instantiates in process (roadmap D10). */
    const val META_RENDERER_FACTORY = "org.autojs.plugin.compose.RENDERER_FACTORY"
    const val RENDERER_FACTORY_CLASS_NAME = "$PACKAGE_NAME.renderer.ComposeUiRendererFactoryImpl"

    /** Application meta-data and capability key carrying the minimum host build. */
    const val META_REQUIRES_HOST_VERSION = PluginCapabilityKeys.REQUIRES_HOST_VERSION

    /** Contract version this plugin targets (roadmap D29 / appendix B.6). */
    const val CONTRACT_VERSION = 1

    /**
     * Minimum AutoJs6 `versionCode`. Provisional (roadmap D29): the host snapshot this repository was
     * created against is 6.8.0 / 5307, so the first host build that can ship `compose-ui-api` and the
     * loader is 5308; roadmap P1.3 back-fills the confirmed value.
     */
    const val REQUIRED_HOST_VERSION = 5308L
}
