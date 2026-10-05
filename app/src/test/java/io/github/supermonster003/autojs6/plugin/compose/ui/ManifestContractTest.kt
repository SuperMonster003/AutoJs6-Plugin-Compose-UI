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
        assertTrue("the renderer runs in the host process, the plugin never resolves other packages", manifest.children("queries").isEmpty())
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
        assertNull("no application theme: the plugin has no screens of its own (roadmap D8)", application.androidAttributeOrNull("theme"))
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
    fun `the wake activity follows the activation contract and is the only activity`() {
        val activities = application.children("activity")
        assertEquals(listOf(".WakeActivity"), activities.map { it.androidAttribute("name") })
        val wake = activities.single()
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
    fun `no launcher entry, alias, receiver or provider exists`() {
        // Roadmap D8: no standalone UI; the plugin must not appear in the app drawer.
        val launcherFilters = application.children("activity").flatMap { it.children("intent-filter") }.filter { filter ->
            filter.children("category").any { it.androidAttribute("name") == "android.intent.category.LAUNCHER" }
        }
        assertTrue(launcherFilters.isEmpty())
        assertTrue(application.children("activity-alias").isEmpty())
        assertTrue(application.children("receiver").isEmpty())
        assertTrue(application.children("provider").isEmpty())
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
        val components = listOf("activity", "activity-alias", "service", "receiver", "provider").flatMap { application.children(it) }
        val exported = components.filter { it.androidAttribute("exported") == "true" }
        assertEquals(
            mapOf(".WakeActivity" to ComposeUiPlugin.PLUGIN_PERMISSION, ".ComposeUiPluginInfoService" to ComposeUiPlugin.PLUGIN_PERMISSION),
            exported.associate { it.androidAttribute("name") to it.androidAttributeOrNull("permission") },
        )
        assertEquals("every declared component is exported on purpose", components.size, exported.size)
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
