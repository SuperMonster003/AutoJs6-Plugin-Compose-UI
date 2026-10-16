package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiAndroidCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiValidator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.idName
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.UiScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.UiDesignerSession
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui.nativeAttrGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P11.3 (D56 as revised by D65): the native Android XML catalog generated from the host's attribute table
 * and device coverage, and what the validator, the inspector and the palette make of it in native fragments.
 */
class UiCatalogAndroidTest {
    private val android = UiAndroidCatalog.load()
    private val reader = UiScriptReader()

    private fun native(layout: String) = reader.read("\"ui\";\n\nui.layoutFile(\"layout.xml\");\n") { if (it == "layout.xml") layout else null }

    @Test
    fun `tags map to the nearest class the host handles`() {
        assertEquals(37, android.coverageApi)
        assertEquals("android.widget.TextView", android.classOf("TextView"))
        assertEquals("android.widget.TextView", android.classOf("Button"))
        assertEquals("android.widget.CompoundButton", android.classOf("CheckBox"))
        assertEquals("androidx.constraintlayout.widget.ConstraintLayout", android.classOf("androidx.constraintlayout.widget.ConstraintLayout"))
        assertNull(android.classOf("NoSuchView"))
        assertEquals(listOf("android.widget.TextView", "android.view.View"), android.chain("android.widget.TextView"))
    }

    @Test
    fun `attributes and layout parameters say what the host does`() {
        val text = android.attrsFor("android.widget.TextView").associateBy { it.name }
        assertEquals(UiAndroidCatalog.State.SET, text.getValue("android:text").state)
        assertEquals(UiAndroidCatalog.State.UNSUPPORTED, text.getValue("android:editable").state)
        assertEquals("android.view.View", text.getValue("android:alpha").ownerClass)
        val linear = android.layoutParamsFor("android.widget.LinearLayout").map { it.name }
        assertTrue("android:layout_weight" in linear && "android:layout_width" in linear && "android:layout_marginTop" in linear)
        val plain = android.layoutParamsFor(null).map { it.name }
        assertTrue("android:layout_width" in plain && "android:layout_weight" !in plain)
        assertTrue("app:layout_constraintTop_toTopOf" in android.layoutParamsFor("androidx.constraintlayout.widget.ConstraintLayout").map { it.name })

        val orientation = android.toUiAttr(android.attr("android.widget.LinearLayout", null, "android:orientation")!!)
        assertEquals("enum", orientation.type)
        assertEquals(listOf("horizontal", "vertical"), orientation.values)
        assertEquals("boolean", android.toUiAttr(android.attr("android.view.View", null, "android:clickable")!!).type)
    }

    @Test
    fun `the native template validates clean`() {
        val (script, files) = ProjectStore.UI_ANDROID_TEMPLATE
        val document = reader.read(script) { files[it] }
        assertEquals(emptyList<Any>(), UiValidator().validate(document))
    }

    @Test
    fun `native fragments are checked against the native catalog`() {
        val document = native(
            """
            |<LinearLayout xmlns:android="$NS" xmlns:tools="http://schemas.android.com/tools"
            |    android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="sideways">
            |    <TextView text="x" android:layout_width="wrap_content" android:layout_height="wrap_content"
            |        android:editable="true" android:fooBar="1" android:layout_weight="1" android:textSize="@dimen/big" android:gravity="center|end"/>
            |    <FrameLayout android:layout_width="wrap_content" android:layout_height="wrap_content">
            |        <View android:layout_width="8dp" android:layout_height="8dp" android:layout_weight="1" tools:ignore="x"/>
            |    </FrameLayout>
            |    <NoSuchView android:anything="1"/>
            |</LinearLayout>
            |""".trimMargin(),
        )
        val found = UiValidator().validate(document).map { (it.target ?: "<tag>") to it.severity }.toSet()
        assertEquals(
            setOf(
                "android:orientation" to Severity.ERROR,
                "text" to Severity.WARNING,
                "android:editable" to Severity.WARNING,
                "android:fooBar" to Severity.WARNING,
                // layout_weight is a LinearLayout parameter, not a FrameLayout one.
                "android:layout_weight" to Severity.WARNING,
                "<tag>" to Severity.WARNING,
            ),
            found,
        )
        // The layout_weight warning is the one inside the FrameLayout.
        val weight = UiValidator().validate(document).single { it.target == "android:layout_weight" }
        assertEquals("View", document.element(weight.nodeId!!)!!.tag)
    }

    @Test
    fun `the session inserts view classes and the inspector lists native attributes`() {
        val (script, files) = ProjectStore.UI_ANDROID_TEMPLATE
        val session = UiDesignerSession(reader.read(script) { files[it] })
        assertTrue(session.nativeFragment)
        val button = session.document.elements().first { it.idName() == "ok" }
        session.selectedId = button.id
        assertTrue(session.insert("TextView"))
        val inserted = session.selected!!
        assertEquals("TextView", inserted.tag)
        assertEquals("wrap_content", inserted.attr("android:layout_width")?.value)
        assertTrue(UiScriptWriter.writeFiles(session.document).values.single().contains("<TextView"))

        val groups = nativeAttrGroups(session, inserted)
        assertEquals("LinearLayout layout", groups.first().owner)
        assertTrue(groups.first().attrs.any { it.name == "android:layout_weight" })
        assertTrue(groups.none { group -> group.attrs.any { it.name == "android:text" || it.name == "android:layout_width" || it.name == "android:id" } })
        assertTrue(groups.any { it.owner == "TextView" } && groups.any { it.owner == "View" })
        assertEquals("enum", session.nativeAttr(inserted, "android:visibility")?.type)
    }

    private companion object {
        const val NS = "http://schemas.android.com/apk/res/android"
    }
}
