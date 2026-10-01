package io.github.supermonster003.autojs6.plugin.compose.ui

import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ComposeUiPluginRuntimeInfoTest {

    @Test
    fun `runtime fields are assembled without losing the plugin identity`() {
        val info = ComposeUiPluginRuntimeInfo(
            name = "Compose UI",
            description = "Renders script-declared user interfaces with Jetpack Compose and Material 3",
            instruction = "Compose UI is a user interface rendering plugin for AutoJs6.",
            versionName = "1.0.0",
            versionCode = 4L,
            versionDate = "Oct 2, 2026",
        )

        assertEquals("Compose UI", info.name)
        assertEquals("Renders script-declared user interfaces with Jetpack Compose and Material 3", info.description)
        assertEquals("Compose UI is a user interface rendering plugin for AutoJs6.", info.instruction)
        assertEquals("SuperMonster003", info.author)
        assertEquals("compose-ui", info.id)
        assertEquals("compose", info.engine)
        assertEquals("default", info.variant)
        assertEquals("1.0.0", info.versionName)
        assertEquals(4L, info.versionCode)
        assertEquals("Oct 2, 2026", info.versionDate)
        // Roadmap D22: pure bytecode APK, no ABI restriction reported to the plugin center.
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertEquals(5308L, info.requiresHostVersion)
        assertEquals(ComposeUiPlugin.REQUIRED_HOST_VERSION, info.requiresHostVersion)
    }

    @Test
    fun `identity constants follow the host discovery contract`() {
        assertEquals("io.github.supermonster003.autojs6.plugin.compose.ui", ComposeUiPlugin.PACKAGE_NAME)
        assertEquals("org.autojs.autojs6", ComposeUiPlugin.HOST_PACKAGE_NAME)
        assertEquals("compose-ui", ComposeUiPlugin.ID)
        assertEquals("compose", ComposeUiPlugin.ENGINE)
        assertEquals("default", ComposeUiPlugin.VARIANT)
        assertEquals("SuperMonster003", ComposeUiPlugin.AUTHOR)
        assertEquals("org.autojs.plugin.INFO", ComposeUiPlugin.INFO_ACTION)
        assertEquals(PluginActions.INFO, ComposeUiPlugin.INFO_ACTION)
        assertEquals("compose-ui", ComposeUiPlugin.INFO_CATEGORY)
        assertEquals(ComposeUiPlugin.ID, ComposeUiPlugin.INFO_CATEGORY)
        assertEquals("org.autojs.permission.PLUGIN", ComposeUiPlugin.PLUGIN_PERMISSION)
        assertEquals("org.autojs.plugin.compose.RENDERER_FACTORY", ComposeUiPlugin.META_RENDERER_FACTORY)
        assertEquals("io.github.supermonster003.autojs6.plugin.compose.ui.renderer.ComposeUiRendererFactoryImpl", ComposeUiPlugin.RENDERER_FACTORY_CLASS_NAME)
        assertTrue(ComposeUiPlugin.RENDERER_FACTORY_CLASS_NAME.startsWith(ComposeUiPlugin.PACKAGE_NAME + "."))
        assertEquals("requiresHostVersion", ComposeUiPlugin.META_REQUIRES_HOST_VERSION)
        assertEquals(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ComposeUiPlugin.META_REQUIRES_HOST_VERSION)
        assertEquals(1, ComposeUiPlugin.CONTRACT_VERSION)
        assertEquals(5308L, ComposeUiPlugin.REQUIRED_HOST_VERSION)
    }

    @Test
    fun `identity constants match the values the documentation and build publish`() {
        val root = findProjectRoot()
        val common = Files.readString(root.resolve(".readme/common.json"))
        assertTrue(common.contains("\"plugin_application_id\": \"${ComposeUiPlugin.PACKAGE_NAME}\""))
        assertTrue(common.contains("\"plugin_id\": \"${ComposeUiPlugin.ID}\""))
        assertTrue(common.contains("\"plugin_engine\": \"${ComposeUiPlugin.ENGINE}\""))
        assertTrue(common.contains("\"plugin_variant\": \"${ComposeUiPlugin.VARIANT}\""))
        assertTrue(common.contains("\"plugin_info_action\": \"${ComposeUiPlugin.INFO_ACTION}\""))
        assertTrue(common.contains("\"plugin_info_category\": \"${ComposeUiPlugin.INFO_CATEGORY}\""))
        assertTrue(common.contains("\"plugin_renderer_factory_meta\": \"${ComposeUiPlugin.META_RENDERER_FACTORY}\""))
        assertTrue(common.contains("\"plugin_contract_version\": \"${ComposeUiPlugin.CONTRACT_VERSION}\""))
        assertTrue(common.contains("\"required_host_version_code\": \"${ComposeUiPlugin.REQUIRED_HOST_VERSION}\""))

        val build = Files.readString(root.resolve("app/build.gradle.kts"))
        // app/build.gradle.kts binds the application id once and reuses it for namespace and applicationId.
        assertTrue(build.contains("val globalApplicationId = \"${ComposeUiPlugin.PACKAGE_NAME}\""))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("\"plugin_id\", \"${ComposeUiPlugin.ID}\""))
        assertTrue(build.contains("\"plugin_engine\", \"${ComposeUiPlugin.ENGINE}\""))
        assertTrue(build.contains("\"plugin_variant\", \"${ComposeUiPlugin.VARIANT}\""))
        assertTrue(build.contains("\"plugin_author\", \"${ComposeUiPlugin.AUTHOR}\""))
        // Roadmap D22 (amended in P0.1): no ABI splits; the only native code is the Compose graphics-path helper,
        // and the release verifier pins the exact library set plus its 16 KB alignment.
        assertFalse("no ABI split block is declared", build.contains("splits {"))
        assertTrue(build.contains("val nativeAbis = listOf(\"arm64-v8a\", \"armeabi-v7a\", \"x86_64\", \"x86\")"))
        assertTrue(build.contains("val allowedNativeLibraries = listOf(\"libandroidx.graphics.path.so\")"))
        assertTrue(build.contains("val nativePageAlignment = 16384L"))
        assertTrue(build.contains("verifyNativeLibraries(apk)"))

        val proguard = Files.readString(root.resolve("app/proguard-rules.pro"))
        assertTrue("R8 must keep the renderer factory the host instantiates by name", proguard.contains("-keep class ${ComposeUiPlugin.RENDERER_FACTORY_CLASS_NAME}"))
        assertTrue(proguard.contains("-keep class ${ComposeUiPlugin.PACKAGE_NAME}.ComposeUiPluginInfoService"))
        assertTrue(proguard.contains("-keep class ${ComposeUiPlugin.PACKAGE_NAME}.WakeActivity"))

        val settings = Files.readString(root.resolve("settings.gradle.kts"))
        assertTrue(settings.contains("rootProject.name = \"autojs6-plugin-compose-ui\""))
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
