package io.github.supermonster003.autojs6.plugin.three.block.compose

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

class ThreeBlockComposeRuntimeInfoTest {

    @Test
    fun `runtime fields are assembled without losing the plugin identity`() {
        val info = ThreeBlockComposeRuntimeInfo(
            name = "3-Block Compose",
            description = "Renders script-declared user interfaces with Jetpack Compose and Material 3",
            instruction = "3-Block Compose is a user interface rendering plugin for AutoJs6.",
            versionName = "1.1.0",
            versionCode = 4L,
            versionDate = "Oct 2, 2026",
        )

        assertEquals("3-Block Compose", info.name)
        assertEquals("Renders script-declared user interfaces with Jetpack Compose and Material 3", info.description)
        assertEquals("3-Block Compose is a user interface rendering plugin for AutoJs6.", info.instruction)
        assertEquals("SuperMonster003", info.author)
        assertEquals("three-block-compose", info.id)
        assertEquals("compose", info.engine)
        assertEquals("default", info.variant)
        assertEquals("1.1.0", info.versionName)
        assertEquals(4L, info.versionCode)
        assertEquals("Oct 2, 2026", info.versionDate)
        // Roadmap D22: one universal APK, no ABI restriction reported to the plugin center.
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertEquals(5322L, info.requiresHostVersion)
        assertEquals(ThreeBlockComposePlugin.REQUIRED_HOST_VERSION, info.requiresHostVersion)
    }

    @Test
    fun `identity constants follow the host discovery contract`() {
        assertEquals("io.github.supermonster003.autojs6.plugin.three.block.compose", ThreeBlockComposePlugin.PACKAGE_NAME)
        assertEquals("org.autojs.autojs6", ThreeBlockComposePlugin.HOST_PACKAGE_NAME)
        assertEquals("three-block-compose", ThreeBlockComposePlugin.ID)
        assertEquals("compose", ThreeBlockComposePlugin.ENGINE)
        assertEquals("default", ThreeBlockComposePlugin.VARIANT)
        assertEquals("SuperMonster003", ThreeBlockComposePlugin.AUTHOR)
        assertEquals("org.autojs.plugin.INFO", ThreeBlockComposePlugin.INFO_ACTION)
        assertEquals(PluginActions.INFO, ThreeBlockComposePlugin.INFO_ACTION)
        assertEquals("three-block-compose", ThreeBlockComposePlugin.INFO_CATEGORY)
        assertEquals(ThreeBlockComposePlugin.ID, ThreeBlockComposePlugin.INFO_CATEGORY)
        assertEquals("org.autojs.permission.PLUGIN", ThreeBlockComposePlugin.PLUGIN_PERMISSION)
        assertEquals("org.autojs.plugin.compose.RENDERER_FACTORY", ThreeBlockComposePlugin.META_RENDERER_FACTORY)
        assertEquals("io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.ComposeUiRendererFactoryImpl", ThreeBlockComposePlugin.RENDERER_FACTORY_CLASS_NAME)
        assertTrue(ThreeBlockComposePlugin.RENDERER_FACTORY_CLASS_NAME.startsWith(ThreeBlockComposePlugin.PACKAGE_NAME + "."))
        assertEquals("requiresHostVersion", ThreeBlockComposePlugin.META_REQUIRES_HOST_VERSION)
        assertEquals(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ThreeBlockComposePlugin.META_REQUIRES_HOST_VERSION)
        assertEquals(2, ThreeBlockComposePlugin.CONTRACT_VERSION)
        assertEquals(5322L, ThreeBlockComposePlugin.REQUIRED_HOST_VERSION)
    }

    @Test
    fun `identity constants match the values the documentation and build publish`() {
        val root = findProjectRoot()
        val common = Files.readString(root.resolve(".readme/common.json"))
        assertTrue(common.contains("\"plugin_application_id\": \"${ThreeBlockComposePlugin.PACKAGE_NAME}\""))
        assertTrue(common.contains("\"plugin_id\": \"${ThreeBlockComposePlugin.ID}\""))
        assertTrue(common.contains("\"plugin_engine\": \"${ThreeBlockComposePlugin.ENGINE}\""))
        assertTrue(common.contains("\"plugin_variant\": \"${ThreeBlockComposePlugin.VARIANT}\""))
        assertTrue(common.contains("\"plugin_info_action\": \"${ThreeBlockComposePlugin.INFO_ACTION}\""))
        assertTrue(common.contains("\"plugin_info_category\": \"${ThreeBlockComposePlugin.INFO_CATEGORY}\""))
        assertTrue(common.contains("\"plugin_renderer_factory_meta\": \"${ThreeBlockComposePlugin.META_RENDERER_FACTORY}\""))
        assertTrue(common.contains("\"plugin_contract_version\": \"${ThreeBlockComposePlugin.CONTRACT_VERSION}\""))
        assertTrue(common.contains("\"required_host_version_code\": \"${ThreeBlockComposePlugin.REQUIRED_HOST_VERSION}\""))

        val build = Files.readString(root.resolve("app/build.gradle.kts"))
        // app/build.gradle.kts binds the application id once and reuses it for namespace and applicationId.
        assertTrue(build.contains("val globalApplicationId = \"${ThreeBlockComposePlugin.PACKAGE_NAME}\""))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("\"plugin_id\", \"${ThreeBlockComposePlugin.ID}\""))
        assertTrue(build.contains("\"plugin_engine\", \"${ThreeBlockComposePlugin.ENGINE}\""))
        assertTrue(build.contains("\"plugin_variant\", \"${ThreeBlockComposePlugin.VARIANT}\""))
        assertTrue(build.contains("\"plugin_author\", \"${ThreeBlockComposePlugin.AUTHOR}\""))
        // Roadmap D22 (amended in P0.1): no ABI splits; the only native code is the Compose graphics-path helper,
        // and the release verifier pins the exact library set plus its 16 KB alignment.
        assertFalse("no ABI split block is declared", build.contains("splits {"))
        assertTrue(build.contains("val nativeAbis = listOf(\"arm64-v8a\", \"armeabi-v7a\", \"x86_64\", \"x86\")"))
        assertTrue(build.contains("val allowedNativeLibraries = listOf(\"libandroidx.graphics.path.so\")"))
        assertTrue(build.contains("val nativePageAlignment = 16384L"))
        assertTrue(build.contains("verifyNativeLibraries(apk)"))

        val proguard = Files.readString(root.resolve("app/proguard-rules.pro"))
        assertTrue("R8 must keep the renderer factory the host instantiates by name", proguard.contains("-keep class ${ThreeBlockComposePlugin.RENDERER_FACTORY_CLASS_NAME}"))
        assertTrue(proguard.contains("-keep class ${ThreeBlockComposePlugin.PACKAGE_NAME}.ThreeBlockComposeInfoService"))
        assertTrue(proguard.contains("-keep class ${ThreeBlockComposePlugin.PACKAGE_NAME}.WakeActivity"))

        val settings = Files.readString(root.resolve("settings.gradle.kts"))
        assertTrue(settings.contains("rootProject.name = \"autojs6-plugin-three-block-compose\""))
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
