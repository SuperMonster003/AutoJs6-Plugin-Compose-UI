package io.github.supermonster003.autojs6.plugin.three.block.compose

/**
 * Pure-data view of the metadata reported through `IPluginInfoProvider.getInfo()`.
 *
 * Android-specific lookups (package version, localized strings, raw resources) happen in
 * [threeBlockComposeRuntimeInfo]; this class keeps the mapping itself testable on the JVM.
 */
data class ThreeBlockComposeRuntimeInfo(
    val name: String,
    val description: String,
    val instruction: String?,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
) {
    val author: String get() = ThreeBlockComposePlugin.AUTHOR
    val id: String get() = ThreeBlockComposePlugin.ID
    val engine: String get() = ThreeBlockComposePlugin.ENGINE
    val variant: String get() = ThreeBlockComposePlugin.VARIANT

    /**
     * Empty on purpose (roadmap D22): the plugin has no native code of its own, and the single APK carries the
     * Compose graphics-path helper for every Android ABI, so no device is excluded.
     */
    val supportedAbis: Array<String> get() = emptyArray()

    val requiresHostVersion: Long get() = ThreeBlockComposePlugin.REQUIRED_HOST_VERSION
}
