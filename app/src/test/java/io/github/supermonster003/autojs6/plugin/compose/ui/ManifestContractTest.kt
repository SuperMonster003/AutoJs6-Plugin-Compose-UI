package io.github.supermonster003.autojs6.plugin.compose.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Keeps `AndroidManifest.xml` and [ComposeUiPlugin] from drifting apart: the host discovers the plugin
 * through the manifest (INFO service, Wake Activity, renderer factory meta-data), while the service and
 * the tests use the Kotlin constants.
 */
class ManifestContractTest {

    private val manifest: Element by lazy {
        val path = findProjectRoot().resolve("app/src/main/AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        factory.newDocumentBuilder().parse(path.toFile()).documentElement
    }

    private val application: Element get() = manifest.child("application")

    @Test
    fun `the plugin permission is the only permission and nothing is queried`() {
        assertEquals(listOf(ComposeUiPlugin.PLUGIN_PERMISSION), manifest.children("uses-permission").map { it.androidAttribute("name") })
        // F.5: the gallery reads host appearance through the official settings Provider and hands scripts to the host.
        val queries = manifest.child("queries")
        assertEquals(listOf("org.autojs.autojs6"), queries.children("package").map { it.androidAttribute("name") })
        assertTrue("no intent or provider queries beyond the host package", queries.children("intent").isEmpty() && queries.children("provider").isEmpty())
        assertTrue("no uses-sdk override: minSdk comes from version.properties", manifest.children("uses-sdk").isEmpty())
        assertTrue("no feature or library requirements", manifest.children("uses-feature").isEmpty())
    }

    @Test
    fun `application metadata names the wake activity, the author, the renderer factory and the host floor`() {
        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("false", application.androidAttribute("fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.androidAttribute("dataExtractionRules"))
        assertEquals("@string/app_name", application.androidAttribute("label"))
        assertEquals("@mipmap/ic_icon_studio_application", application.androidAttribute("icon"))
        assertEquals("true", application.androidAttribute("supportsRtl"))
        assertEquals("the standalone screens use the plugin's own window theme (F.5)", "@style/Theme.ComposeUi", application.androidAttribute("theme"))
        assertEquals("@xml/locales_config", application.androidAttribute("localeConfig"))
        assertNull("no Application subclass before a renderer needs one", application.androidAttributeOrNull("name"))

        val metaData = application.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(
            setOf("org.autojs.plugin.WAKE_ACTIVITY", "org.autojs.plugin.info.AUTHOR", ComposeUiPlugin.META_RENDERER_FACTORY, ComposeUiPlugin.META_REQUIRES_HOST_VERSION),
            metaData.keys,
        )
        assertEquals(".WakeActivity", metaData["org.autojs.plugin.WAKE_ACTIVITY"])
        assertEquals("@string/plugin_author", metaData["org.autojs.plugin.info.AUTHOR"])
        assertEquals(ComposeUiPlugin.RENDERER_FACTORY_CLASS_NAME, metaData[ComposeUiPlugin.META_RENDERER_FACTORY])
        assertEquals("io.github.supermonster003.autojs6.plugin.compose.ui.renderer.ComposeUiRendererFactoryImpl", metaData[ComposeUiPlugin.META_RENDERER_FACTORY])
        assertEquals(ComposeUiPlugin.REQUIRED_HOST_VERSION.toString(), metaData["requiresHostVersion"])
    }

    @Test
    fun `the wake activity follows the activation contract beside the gallery screens`() {
        val activities = application.children("activity")
        assertEquals(listOf(".WakeActivity", ".app.GalleryActivity", ".app.SettingsActivity"), activities.map { it.androidAttribute("name") })
        activities.drop(1).forEach { screen ->
            assertEquals("${screen.androidAttribute("name")} is reached through the launcher alias or the gallery only", "false", screen.androidAttribute("exported"))
            assertEquals("appearance changes redraw in place", "uiMode|locale|layoutDirection", screen.androidAttribute("configChanges"))
        }
        val wake = activities.first()
        assertEquals("true", wake.androidAttribute("exported"))
        assertEquals("true", wake.androidAttribute("excludeFromRecents"))
        assertEquals("true", wake.androidAttribute("finishOnTaskLaunch"))
        assertEquals(ComposeUiPlugin.PLUGIN_PERMISSION, wake.androidAttribute("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.androidAttribute("theme"))
        val filter = wake.child("intent-filter")
        assertEquals(listOf("org.autojs.plugin.action.WAKE"), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.DEFAULT"), filter.children("category").map { it.androidAttribute("name") })
    }

    @Test
    fun `the launcher entry is one of four fixed icon aliases of the gallery and only auto is enabled`() {
        // Roadmap D8 as revised on 2026-10-08 (F.5) and the icon specification: the user's icon choice is persisted as
        // component enabled state, so the aliases are stable and only the automatic one is enabled by default.
        val launcherFilters = application.children("activity").flatMap { it.children("intent-filter") }.filter { filter ->
            filter.children("category").any { it.androidAttribute("name") == "android.intent.category.LAUNCHER" }
        }
        assertTrue("no plain activity carries the launcher filter", launcherFilters.isEmpty())
        val aliases = application.children("activity-alias")
        assertEquals(
            listOf(".launcher.AdaptiveLightIconAlias", ".launcher.AdaptiveDarkIconAlias", ".launcher.AdaptiveAutoIconAlias", ".launcher.TransparentIconAlias"),
            aliases.map { it.androidAttribute("name") },
        )
        aliases.forEach { alias ->
            assertEquals(".app.GalleryActivity", alias.androidAttribute("targetActivity"))
            assertEquals("true", alias.androidAttribute("exported"))
            assertEquals(alias.androidAttribute("icon"), alias.androidAttribute("roundIcon"))
            val filter = alias.child("intent-filter")
            assertEquals(listOf("android.intent.action.MAIN"), filter.children("action").map { it.androidAttribute("name") })
            assertEquals(listOf("android.intent.category.LAUNCHER"), filter.children("category").map { it.androidAttribute("name") })
        }
        assertEquals(
            mapOf(
                ".launcher.AdaptiveLightIconAlias" to "@mipmap/ic_launcher_system_light",
                ".launcher.AdaptiveDarkIconAlias" to "@mipmap/ic_launcher_system",
                ".launcher.AdaptiveAutoIconAlias" to "@mipmap/ic_launcher_system_auto",
                ".launcher.TransparentIconAlias" to "@mipmap/ic_launcher",
            ),
            aliases.associate { it.androidAttribute("name") to it.androidAttribute("icon") },
        )
        assertEquals(listOf(".launcher.AdaptiveAutoIconAlias"), aliases.filter { it.androidAttribute("enabled") == "true" }.map { it.androidAttribute("name") })
        val receivers = application.children("receiver")
        assertEquals(listOf(".app.LauncherIconUpdateReceiver"), receivers.map { it.androidAttribute("name") })
        assertEquals("false", receivers.single().androidAttribute("exported"))
        assertEquals(listOf("android.intent.action.MY_PACKAGE_REPLACED"), receivers.single().child("intent-filter").children("action").map { it.androidAttribute("name") })
        // The packaged AndroidX runtime would merge androidx.startup; the plugin keeps declaring no provider.
        val providers = application.children("provider")
        assertEquals(listOf("androidx.startup.InitializationProvider"), providers.map { it.androidAttribute("name") })
        assertEquals("remove", providers.single().getAttributeNS("http://schemas.android.com/tools", "node"))
    }

    @Test
    fun `the info service is the only service and matches the identity constants`() {
        val services = application.children("service")
        assertEquals(listOf(".ComposeUiPluginInfoService"), services.map { it.androidAttribute("name") })
        val info = services.single()
        assertEquals("true", info.androidAttribute("exported"))
        assertEquals("true", info.androidAttribute("enabled"))
        assertEquals(ComposeUiPlugin.PLUGIN_PERMISSION, info.androidAttribute("permission"))
        assertNull("the INFO service runs in the plugin's default process", info.androidAttributeOrNull("process"))
        val filter = info.child("intent-filter")
        assertEquals(listOf(ComposeUiPlugin.INFO_ACTION), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("org.autojs.plugin.INFO"), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf(ComposeUiPlugin.INFO_CATEGORY), filter.children("category").map { it.androidAttribute("name") })
        // Roadmap D23: INFO-only registration, no Binder capability service.
        assertTrue(info.children("meta-data").isEmpty())
    }

    @Test
    fun `only activation and discovery are exported and both require the plugin permission`() {
        val components = listOf("activity", "activity-alias", "service", "receiver").flatMap { application.children(it) }
        val exported = components.filter { it.androidAttribute("exported") == "true" }
        assertEquals(
            mapOf(".WakeActivity" to ComposeUiPlugin.PLUGIN_PERMISSION, ".ComposeUiPluginInfoService" to ComposeUiPlugin.PLUGIN_PERMISSION),
            exported.filterNot { it.tagName == "activity-alias" }.associate { it.androidAttribute("name") to it.androidAttributeOrNull("permission") },
        )
        // The four launcher aliases are exported without a permission, as every launcher entry must be.
        assertEquals(4, exported.count { it.tagName == "activity-alias" })
        assertEquals("every other component is private to the plugin", components.size - exported.size,
            components.count { it.androidAttribute("exported") == "false" })
    }

    private fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length)
            .map { nodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == tag }
    }

    private fun Element.child(tag: String): Element = children(tag).single()

    private fun Element.androidAttribute(name: String): String =
        androidAttributeOrNull(name) ?: error("Missing android:$name on <$tagName>")

    private fun Element.androidAttributeOrNull(name: String): String? =
        if (hasAttributeNS(ANDROID_NAMESPACE, name)) getAttributeNS(ANDROID_NAMESPACE, name) else null

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
