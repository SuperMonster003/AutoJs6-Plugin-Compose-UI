package io.github.supermonster003.autojs6.plugin.compose.ui

/**
 * Pure-data view of the metadata reported through `IPluginInfoProvider.getInfo()`.
 *
 * Android-specific lookups (package version, localized strings, raw resources) happen in
 * [composeUiPluginRuntimeInfo]; this class keeps the mapping itself testable on the JVM.
 */
data class ComposeUiPluginRuntimeInfo(
    val name: String,
    val description: String,
    val instruction: String?,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
) {
    val author: String get() = ComposeUiPlugin.AUTHOR
    val id: String get() = ComposeUiPlugin.ID
    val engine: String get() = ComposeUiPlugin.ENGINE
    val variant: String get() = ComposeUiPlugin.VARIANT

    /** Empty on purpose: the plugin ships no native code and runs on any ABI (roadmap D22). */
    val supportedAbis: Array<String> get() = emptyArray()

    val requiresHostVersion: Long get() = ComposeUiPlugin.REQUIRED_HOST_VERSION
}
