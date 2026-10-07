package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Looper
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.supermonster003.autojs6.plugin.compose.ui.renderer.dialog.DialogPresentation
import io.github.supermonster003.autojs6.plugin.compose.ui.renderer.dialog.DialogRendererFactoryImpl
import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.dialog.*
import org.autojs.plugin.compose.api.interop.*
import org.autojs.plugin.compose.api.loading.*
import org.autojs.plugin.compose.api.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor

class DialogWindowFailureTest {
    @get:Rule val rule = createAndroidComposeRule<RendererTestActivity>()
    private var renderer: AndroidViewRendererV1? = null
    @After fun close() { rule.runOnUiThread { renderer?.dispose(); renderer = null } }

    @Test fun badApplicationWindowTokenUsesOneFatalLifecycleEventAndDisposeReleasesClaims() {
        val events = ConcurrentLinkedQueue<UiEvent>()
        lateinit var borrowed: TextView
        rule.runOnUiThread {
            // The composition inherits real Activity owners, but the dialog's own context has no
            // Activity WindowManager token. APPLICATION + null token makes actual Dialog.show fail.
            val context = rule.activity.applicationContext
            val environment = object : ComposeUiHostEnvironment {
                override val hostContext = context
                override val mainExecutor = Executor { action ->
                    if (Looper.myLooper() == Looper.getMainLooper()) action.run() else rule.activity.runOnUiThread(action)
                }
                override val eventSink = ComposeUiEventSink(events::add)
                override val initialTheme = ThemeSpec(dark = false)
                override val sessionId = 72
            }
            renderer = DialogRendererFactoryImpl().create(environment,
                DialogOptionsV1(windowType = WindowManager.LayoutParams.TYPE_APPLICATION, windowToken = null))
            borrowed = TextView(rule.activity).apply { text = "failed window content" }
            val native = UiNode(1, AndroidViewInteropV1.COMPONENT,
                props = mapOf(AndroidViewInteropV1.PROP_VIEW to UiValue.Num(19.0)))
            renderer!!.applyWithViews(UiPatchBatch(72, 1, listOf(UiPatch.SetRoot(UiTree(1, listOf(native))))),
                listOf(AndroidViewBindingV1(1, 19, borrowed)))
            val anchor = renderer!!.view()
            rule.activity.setContentView(FrameLayout(rule.activity).apply {
                addView(anchor, FrameLayout.LayoutParams(1, 1))
            })
        }
        rule.waitUntil(5000) { events.any { it.type == DialogInteropV1.DISMISS_REQUEST } }
        val fatal = events.single()
        assertEquals(DialogInteropV1.DISMISS_REQUEST, fatal.type)
        assertEquals(ComposeUiErrorCodes.RENDER_FAILED, fatal.payload.getString(ComposeUiEventFields.CODE))
        assertEquals(72, fatal.sessionId)
        assertEquals(1L, fatal.generation)
        assertEquals(ComposeUiContract.NO_NODE_ID, fatal.nodeId)
        assertEquals(ComposeUiContract.SYSTEM_CALLBACK_ID, fatal.callbackId)
        rule.runOnUiThread {
            renderer!!.dispose()
            renderer!!.dispose()
            assertNull(borrowed.parent)
            assertFalse(AndroidViewClaimsV1.isClaimed(borrowed))
        }
        rule.waitForIdle()
        rule.onNodeWithTag(DialogPresentation.ALERT_TAG).assertDoesNotExist()
        assertEquals(1, events.size)
    }

    @Test fun disposeInTheAttachingTurnCancelsPostedInitializationAndCannotReshow() {
        val events = ConcurrentLinkedQueue<UiEvent>()
        lateinit var borrowed: TextView
        rule.runOnUiThread {
            val context = rule.activity.applicationContext
            renderer = DialogRendererFactoryImpl().create(object : ComposeUiHostEnvironment {
                override val hostContext = context
                override val mainExecutor = Executor { task -> rule.activity.runOnUiThread(task) }
                override val eventSink = ComposeUiEventSink(events::add)
                override val initialTheme = ThemeSpec(dark = false)
                override val sessionId = 73
            }, DialogOptionsV1())
            borrowed = TextView(rule.activity).apply { text = "closed before initialize" }
            val native = UiNode(1, AndroidViewInteropV1.COMPONENT,
                props = mapOf(AndroidViewInteropV1.PROP_VIEW to UiValue.Num(20.0)))
            renderer!!.applyWithViews(UiPatchBatch(73, 1, listOf(UiPatch.SetRoot(UiTree(1, listOf(native))))),
                listOf(AndroidViewBindingV1(1, 20, borrowed)))
            val anchor = renderer!!.view()
            val holder = FrameLayout(rule.activity).apply { addView(anchor, FrameLayout.LayoutParams(1, 1)) }
            rule.activity.setContentView(holder)
            // onAttached has posted initialization, but the main looper cannot run it in this turn.
            renderer!!.dispose()
            holder.removeView(anchor)
            holder.addView(anchor, FrameLayout.LayoutParams(1, 1))
        }
        rule.waitForIdle()
        rule.onNodeWithTag(DialogPresentation.ALERT_TAG).assertDoesNotExist()
        rule.runOnIdle { assertNull(borrowed.parent); assertFalse(AndroidViewClaimsV1.isClaimed(borrowed)) }
        // If the canceled initializer ran, this deliberately invalid application token would fail.
        assertTrue(events.isEmpty())
    }
}
