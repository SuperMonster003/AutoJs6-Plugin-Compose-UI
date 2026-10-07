package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.interop.*
import org.autojs.plugin.compose.api.loading.*
import org.autojs.plugin.compose.api.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor

class AndroidViewRendererTest {
    @get:Rule val rule = createAndroidComposeRule<RendererTestActivity>()
    private val renderers = ArrayList<ComposeUiRenderer>()
    private val events = ConcurrentLinkedQueue<UiEvent>()
    @After fun close() { rule.runOnUiThread { renderers.asReversed().forEach(ComposeUiRenderer::dispose) } }

    private fun environment(id: Int) = object : ComposeUiHostEnvironment {
        override val hostContext = rule.activity
        override val mainExecutor = Executor { action -> if (Looper.myLooper() == Looper.getMainLooper()) action.run() else rule.activity.runOnUiThread(action) }
        override val eventSink = ComposeUiEventSink(events::add)
        override val initialTheme = ThemeSpec(dark = false)
        override val sessionId = id
    }
    private fun create(id: Int = 61, attach: Boolean = true): AndroidViewRendererV1 {
        lateinit var result: AndroidViewRendererV1
        rule.runOnUiThread {
            val capabilities = ComposeUiRendererFactoryImpl().capabilities()
            assertEquals(55, capabilities.getStringArrayList(ComposeUiCapabilityKeys.COMPONENTS)!!.size)
            assertTrue(capabilities.getStringArrayList(ComposeUiCapabilityKeys.FEATURES)!!.contains(AndroidViewInteropV1.FEATURE))
            val type = Class.forName(capabilities.getString(AndroidViewInteropV1.FACTORY_CLASS_KEY)!!)
            val factory = type.getDeclaredConstructor().newInstance() as AndroidViewRendererFactoryV1
            assertEquals(1, factory.extensionVersion())
            result = factory.create(environment(id))
            renderers += result
            if (attach) rule.activity.setContentView(result.view())
        }
        rule.waitForIdle()
        return result
    }
    private fun native(lease: Int, id: Int = 1) = UiNode(id, AndroidViewInteropV1.COMPONENT,
        props = mapOf(AndroidViewInteropV1.PROP_VIEW to UiValue.Num(lease.toDouble())),
        modifier = listOf(ModifierOp(ComposeUiModifiers.TEST_TAG, listOf(UiValue.Str("android-view-$id"))),
            ModifierOp(ComposeUiModifiers.SIZE, listOf(UiValue.Dp(100.0)))))
    private fun tree(lease: Int) = UiTree(1, listOf(native(lease)))
    private fun batch(generation: Long, tree: UiTree, id: Int = 61) = UiPatchBatch(id, generation, listOf(UiPatch.SetRoot(tree)))
    private fun text(value: String): TextView {
        lateinit var result: TextView
        rule.runOnUiThread { result = TextView(rule.activity).apply { text = value } }
        return result
    }
    private fun failure(code: String, action: () -> Unit) {
        try { action(); fail("Expected $code") } catch (failure: ComposeUiContractException) { assertEquals(code, failure.code) }
    }

    @Test fun negotiatedSlotReplacesBorrowedViewsAndNeverDestroysCallerListeners() {
        val renderer = create()
        val first = text("first"); val next = text("next")
        var clicks = 0
        rule.runOnUiThread {
            first.setOnClickListener { clicks++ }
            renderer.applyWithViews(batch(1, tree(10)), listOf(AndroidViewBindingV1(1, 10, first)))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("android-view-1").assertExists()
        rule.runOnIdle {
            assertNotNull(first.parent)
            assertTrue(AndroidViewClaimsV1.isOwnedBy(renderer, first, 1, 10))
            renderer.applyWithViews(UiPatchBatch(61, 2, listOf(UiPatch.SetProps(1,
                mapOf(AndroidViewInteropV1.PROP_VIEW to UiValue.Num(11.0))))), listOf(AndroidViewBindingV1(1, 11, next)))
            // Parent replacement occurs in apply, before any later recomposition/release callback.
            assertNull(first.parent); assertNotNull(next.parent)
            assertFalse(AndroidViewClaimsV1.isClaimed(first)); assertTrue(AndroidViewClaimsV1.isOwnedBy(renderer, next, 1, 11))
            assertTrue(first.performClick()); assertEquals(1, clicks)
            // A -> B -> A is valid while the node's retained source still names the original lease.
            renderer.applyWithViews(batch(3, tree(10)), listOf(AndroidViewBindingV1(1, 10, first)))
            assertNull(next.parent); assertNotNull(first.parent)
            renderer.dispose(); renderer.dispose()
            assertNull(first.parent); assertFalse(AndroidViewClaimsV1.isClaimed(first))
            assertTrue(first.performClick()); assertEquals(2, clicks)
        }
        assertTrue(events.none { it.type == ComposeUiEvents.ERROR })
    }

    @Test fun wrongBindingsForeignParentsAndDuplicateViewsRejectTheWholeBatch() {
        val renderer = create()
        val retained = text("retained"); val foreign = text("foreign")
        rule.runOnIdle { renderer.applyWithViews(batch(1, tree(10)), listOf(AndroidViewBindingV1(1, 10, retained))) }
        rule.waitForIdle()
        rule.runOnIdle {
            val parent = retained.parent
            val foreignParent = FrameLayout(rule.activity).apply { addView(foreign) }
            failure(ComposeUiErrorCodes.INVALID_ARGUMENT) {
                renderer.applyWithViews(batch(2, tree(11)), listOf(AndroidViewBindingV1(1, 11, foreign)))
            }
            assertSame(parent, retained.parent); assertSame(foreignParent, foreign.parent)
            failure(ComposeUiErrorCodes.INVALID_ARGUMENT) { renderer.applyWithViews(batch(2, tree(10)), emptyList()) }
            failure(ComposeUiErrorCodes.INVALID_ARGUMENT) {
                renderer.applyWithViews(batch(2, tree(10)), listOf(AndroidViewBindingV1(1, 99, retained)))
            }
            val duplicate = UiTree(3, listOf(UiNode(3, ComposeUiComponents.COLUMN, children = listOf(1, 2)), native(10), native(11, 2)))
            failure(ComposeUiErrorCodes.INVALID_ARGUMENT) {
                renderer.applyWithViews(batch(2, duplicate), listOf(AndroidViewBindingV1(1, 10, retained), AndroidViewBindingV1(2, 11, retained)))
            }
            assertTrue(AndroidViewClaimsV1.isOwnedBy(renderer, retained, 1, 10))
            assertFalse(AndroidViewClaimsV1.isClaimed(foreign))
            // A rejected view transaction must not consume its generation number.
            renderer.applyWithViews(batch(2, tree(10)), listOf(AndroidViewBindingV1(1, 10, retained)))
            foreignParent.removeView(foreign)
        }
        assertTrue(events.none { it.type == ComposeUiEvents.ERROR })
    }

    @Test fun sharedClaimsRejectAnotherRendererEvenBeforeAViewGetsAParent() {
        val first = create(61, attach = false)
        val second = create(62, attach = false)
        val view = text("not composed")
        rule.runOnIdle {
            first.applyWithViews(batch(1, tree(7)), listOf(AndroidViewBindingV1(1, 7, view)))
            assertNull(view.parent)
            assertTrue(AndroidViewClaimsV1.isOwnedBy(first, view, 1, 7))
            failure(ComposeUiErrorCodes.NODE_DETACHED) {
                second.applyWithViews(batch(1, tree(7), 62), listOf(AndroidViewBindingV1(1, 7, view)))
            }
            assertTrue(AndroidViewClaimsV1.isOwnedBy(first, view, 1, 7))
            first.dispose()
            second.applyWithViews(batch(1, tree(7), 62), listOf(AndroidViewBindingV1(1, 7, view)))
            assertTrue(AndroidViewClaimsV1.isOwnedBy(second, view, 1, 7))
        }
    }

    @Test fun nativeTraversalFailuresReportTypedErrorsAndReplacementRecoversTheSlot() {
        val renderer = create()
        lateinit var broken: View
        rule.runOnUiThread { broken = object : View(rule.activity) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) { throw IllegalStateException("Test native measure failure") }
        } }
        rule.runOnIdle { renderer.applyWithViews(batch(1, tree(15)), listOf(AndroidViewBindingV1(1, 15, broken))) }
        rule.waitUntil(5000) { events.any { it.type == ComposeUiEvents.ERROR } }
        val error = events.first { it.type == ComposeUiEvents.ERROR }
        assertEquals(ComposeUiErrorCodes.RENDER_FAILED, error.payload.getString(ComposeUiEventFields.CODE))
        assertEquals(1, error.nodeId); assertEquals(AndroidViewInteropV1.PROP_VIEW, error.payload.getString(ComposeUiEventFields.PROP))
        val replacement = text("recovered")
        rule.runOnIdle { renderer.applyWithViews(batch(2, tree(16)), listOf(AndroidViewBindingV1(1, 16, replacement))) }
        rule.waitForIdle()
        rule.runOnIdle { assertNull(broken.parent); assertTrue(replacement.isShown) }
    }

    @Test fun legacyEntryRemainsV1AndCannotAccidentallyAcceptTheOptionalNode() {
        rule.runOnUiThread {
            val legacy = ComposeUiRendererFactoryImpl().create(environment(61)).also { renderers += it }
            failure(ComposeUiErrorCodes.UNKNOWN_COMPONENT) { legacy.apply(batch(1, tree(10))) }
            legacy.apply(batch(1, UiTree(1, listOf(UiNode(1, ComposeUiComponents.TEXT,
                props = mapOf(ComposeUiProps.TEXT to UiValue.Str("legacy")))))))
        }
    }

    @Test fun borrowedForceLayoutFailureDoesNotConsumeTheGenerationOrReplaceTheOldView() {
        val renderer = create()
        var armed = false
        lateinit var borrowed: View
        rule.runOnUiThread { borrowed = object : View(rule.activity) {
            override fun forceLayout() {
                if (armed) throw IllegalStateException("Test borrowed forceLayout failure")
                super.forceLayout()
            }
        } }
        rule.runOnIdle { renderer.applyWithViews(batch(1, tree(10)), listOf(AndroidViewBindingV1(1, 10, borrowed))) }
        rule.waitForIdle()
        val replacement = text("recovered")
        rule.runOnIdle {
            val parent = borrowed.parent
            armed = true
            val padded = UiPatch.SetProps(1, native(10).props, native(10).modifier +
                ModifierOp(ComposeUiModifiers.PADDING, listOf(UiValue.Dp(8.0))))
            failure(ComposeUiErrorCodes.RENDER_FAILED) {
                renderer.applyWithViews(UiPatchBatch(61, 2, listOf(padded)), listOf(AndroidViewBindingV1(1, 10, borrowed)))
            }
            assertSame(parent, borrowed.parent)
            assertTrue(AndroidViewClaimsV1.isOwnedBy(renderer, borrowed, 1, 10))
            armed = false
            // The rejected generation is reusable; host/core do not need to skip a number.
            renderer.applyWithViews(batch(2, tree(11)), listOf(AndroidViewBindingV1(1, 11, replacement)))
            assertNull(borrowed.parent); assertNotNull(replacement.parent)
        }
        rule.waitForIdle()
        rule.runOnIdle { assertTrue(replacement.isShown) }
    }

    @Test fun legacyContainerParentFailureAlsoLeavesItsGenerationAvailableForRetry() {
        lateinit var renderer: ComposeUiRenderer
        lateinit var parent: FrameLayout
        var armed = false
        rule.runOnUiThread {
            renderer = ComposeUiRendererFactoryImpl().create(environment(61)).also { renderers += it }
            parent = object : FrameLayout(rule.activity) {
                override fun requestLayout() {
                    if (armed) throw IllegalStateException("Test host parent requestLayout failure")
                    super.requestLayout()
                }
            }
            parent.addView(renderer.view())
            rule.activity.setContentView(parent)
        }
        fun textTree(value: String) = UiTree(1, listOf(UiNode(1, ComposeUiComponents.TEXT,
            props = mapOf(ComposeUiProps.TEXT to UiValue.Str(value)))))
        rule.runOnIdle { renderer.apply(batch(1, textTree("before parent failure"))) }
        rule.waitForIdle()
        rule.waitUntil(5000) {
            var ready = false
            rule.runOnUiThread { ready = !parent.isLayoutRequested }
            ready
        }
        rule.runOnIdle {
            armed = true
            try {
                renderer.apply(batch(2, textTree("unaccepted")))
                fail("The custom parent must reject the layout request")
            } catch (expected: IllegalStateException) {
                assertEquals("Test host parent requestLayout failure", expected.message)
            } finally { armed = false }
            renderer.apply(batch(2, textTree("recovered parent")))
        }
        rule.waitForIdle()
        rule.onNodeWithText("recovered parent").assertExists()
    }

    @Test fun closingFromNativeAttachmentCannotResurrectOldClaimsOrBorrowedParents() {
        val renderer = create()
        val before = text("before close")
        rule.runOnIdle { renderer.applyWithViews(batch(1, tree(10)), listOf(AndroidViewBindingV1(1, 10, before))) }
        rule.waitForIdle()
        lateinit var closing: View
        rule.runOnUiThread { closing = object : View(rule.activity) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                renderer.dispose()
            }
        } }
        rule.runOnIdle {
            failure(ComposeUiErrorCodes.SESSION_CLOSED) {
                renderer.applyWithViews(batch(2, tree(11)), listOf(AndroidViewBindingV1(1, 11, closing)))
            }
            assertNull(before.parent); assertNull(closing.parent)
            assertFalse(AndroidViewClaimsV1.isClaimed(before)); assertFalse(AndroidViewClaimsV1.isClaimed(closing))
            failure(ComposeUiErrorCodes.SESSION_CLOSED) { renderer.view() }
        }
        assertTrue(events.none { it.type == ComposeUiEvents.ERROR })
    }
}
