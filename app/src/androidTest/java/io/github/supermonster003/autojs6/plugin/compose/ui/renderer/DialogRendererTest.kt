package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.app.Dialog
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.compose.ui.renderer.dialog.DialogPresentation
import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiModifiers as M
import org.autojs.plugin.compose.api.ComposeUiProps as P
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

/** The host suite additionally exercises permission-gated overlay windows from non-UI scripts. */
class DialogRendererTest {
    @get:Rule val rule = createAndroidComposeRule<RendererTestActivity>()
    private var renderer: AndroidViewRendererV1? = null
    private val events = ConcurrentLinkedQueue<UiEvent>()
    private var generation = 0L

    @After fun close() { rule.runOnUiThread { renderer?.dispose(); renderer = null } }

    private fun create(type: String = DialogInteropV1.ALERT, cancelable: Boolean = true, attach: Boolean = true) {
        rule.runOnUiThread {
            val capabilities = ComposeUiRendererFactoryImpl().capabilities()
            assertEquals(55, capabilities.getStringArrayList(ComposeUiCapabilityKeys.COMPONENTS)!!.size)
            assertTrue(capabilities.getStringArrayList(ComposeUiCapabilityKeys.FEATURES)!!.contains(DialogInteropV1.FEATURE))
            val factory = Class.forName(capabilities.getString(DialogInteropV1.FACTORY_CLASS_KEY)!!)
                .getDeclaredConstructor().newInstance() as DialogRendererFactoryV1
            assertEquals(DialogInteropV1.VERSION, factory.extensionVersion())
            renderer = factory.create(object : ComposeUiHostEnvironment {
                override val hostContext = rule.activity
                override val mainExecutor = Executor { task -> if (Looper.myLooper() == Looper.getMainLooper()) task.run() else rule.activity.runOnUiThread(task) }
                override val eventSink = ComposeUiEventSink(events::add)
                override val initialTheme = ThemeSpec(dark = false)
                override val sessionId = 71
            }, DialogOptionsV1(type, cancelable))
            if (attach) mountAnchor(renderer!!.view())
        }
        if (attach) rule.waitForIdle()
    }

    private fun mountAnchor(view: View) {
        val holder = FrameLayout(rule.activity)
        holder.addView(view, FrameLayout.LayoutParams(1, 1))
        rule.activity.setContentView(holder)
    }

    private fun tag(value: String) = ModifierOp(M.TEST_TAG, listOf(UiValue.Str(value)))
    private fun text(id: Int, value: String) = UiNode(id, C.TEXT, props = mapOf(P.TEXT to UiValue.Str(value)))
    private fun body(value: String) = UiTree(1, listOf(
        UiNode(1, C.BOX, children = listOf(2), modifier = listOf(tag("dialog-body"),
            ModifierOp(M.HEIGHT, listOf(UiValue.Dp(220.0))))), text(2, value)))

    private fun apply(tree: UiTree, bindings: List<AndroidViewBindingV1> = emptyList(), await: Boolean = true) {
        rule.runOnUiThread { renderer!!.applyWithViews(batch(tree), bindings) }
        if (await) rule.waitForIdle()
    }
    private fun batch(tree: UiTree) = UiPatchBatch(71, ++generation, listOf(UiPatch.SetRoot(tree)))
    private fun dismissals() = events.filter { it.type == DialogInteropV1.DISMISS_REQUEST }
    private fun back() = InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    private fun assertOneDismissal(expectedGeneration: Long) {
        rule.waitUntil(5000) { dismissals().size == 1 }
        rule.waitForIdle()
        val event = dismissals().single()
        assertEquals(expectedGeneration, event.generation)
        assertEquals(71, event.sessionId)
        assertEquals(ComposeUiContract.NO_NODE_ID, event.nodeId)
        assertEquals(ComposeUiContract.SYSTEM_CALLBACK_ID, event.callbackId)
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun alertKeepsInteropOwnershipAndReportsLatestGenerationOnceOnBack() {
        create()
        lateinit var borrowed: TextView
        rule.runOnUiThread { borrowed = TextView(rule.activity).apply { text = "borrowed dialog View" } }
        val native = UiNode(2, AndroidViewInteropV1.COMPONENT,
            props = mapOf(AndroidViewInteropV1.PROP_VIEW to UiValue.Num(10.0)),
            modifier = listOf(ModifierOp(M.HEIGHT, listOf(UiValue.Dp(48.0)))))
        val tree = UiTree(1, listOf(UiNode(1, C.COLUMN, children = listOf(2, 3)), native, text(3, "before")))
        val bindings = listOf(AndroidViewBindingV1(2, 10, borrowed))
        apply(tree, bindings)
        lateinit var window: Window
        rule.runOnIdle {
            assertTrue(borrowed.isShown)
            assertTrue(AndroidViewClaimsV1.isOwnedBy(renderer!!, borrowed, 2, 10))
            window = dialogWindow(borrowed)
            assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION, window.attributes.type)
            renderer!!.applyWithViews(UiPatchBatch(71, ++generation,
                listOf(UiPatch.SetProps(3, mapOf(P.TEXT to UiValue.Str("updated dialog"))))), bindings)
        }
        rule.onNodeWithText("updated dialog").assertIsDisplayed()
        assertTrue(dismissals().isEmpty())
        back()
        assertOneDismissal(2)
        rule.runOnIdle { renderer!!.dispose(); assertNull(borrowed.parent); assertFalse(AndroidViewClaimsV1.isClaimed(borrowed)) }
        rule.waitForIdle()
        rule.runOnIdle { assertFalse(window.decorView.isAttachedToWindow) }
    }

    @Test fun sheetInitialHiddenIsNotDismissalAndScrimClosesTheModal() {
        create(DialogInteropV1.BOTTOM_SHEET)
        apply(body("outside close"))
        rule.onNodeWithTag(DialogPresentation.SHEET_TAG).assertIsDisplayed()
        assertTrue(dismissals().isEmpty())
        rule.onNodeWithTag(DialogPresentation.SCRIM_TAG).performTouchInput { click(Offset(12f, 12f)) }
        assertOneDismissal(1)
        rule.onNodeWithText("outside close").assertDoesNotExist()
    }

    @Test fun sheetDragToHiddenEmitsOneDismissalAfterOpening() {
        create(DialogInteropV1.BOTTOM_SHEET)
        apply(body("drag close"))
        rule.onNodeWithTag(DialogPresentation.SHEET_TAG).assertIsDisplayed()
        var before: Rect? = null
        fun dismissActions() = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss),
            useUnmergedTree = true).fetchSemanticsNodes().size
        try {
            // Visible content can still be entering. M3 exposes this public action only after
            // sheetSwipeEnabled becomes true; wait for it, but exercise the real drag below.
            rule.waitUntil(5000) { dismissActions() > 0 }
            before = rule.onNodeWithTag(DialogPresentation.SHEET_TAG).fetchSemanticsNode().boundsInRoot
            rule.onNodeWithTag(DialogPresentation.SHEET_TAG).performTouchInput { swipeDown(durationMillis = 180) }
            assertOneDismissal(1)
        } catch (failure: Throwable) {
            val after = runCatching { rule.onNodeWithTag(DialogPresentation.SHEET_TAG).fetchSemanticsNode().boundsInRoot }.getOrNull()
            val actions = runCatching { dismissActions() }.getOrNull()
            throw AssertionError("Sheet drag failed: before=$before, after=$after, dismissActions=$actions, " +
                "events=${events.map { it.type }}", failure)
        }
        rule.onNodeWithText("drag close").assertDoesNotExist()
    }

    @Test fun nonCancelableSheetRejectsBackScrimAndDragUntilExplicitDisposal() {
        create(DialogInteropV1.BOTTOM_SHEET, cancelable = false)
        apply(body("locked sheet"))
        rule.onNodeWithTag(DialogPresentation.SHEET_TAG).assertIsDisplayed()
        back()
        rule.onNodeWithTag(DialogPresentation.SCRIM_TAG).performTouchInput { click(Offset(12f, 12f)) }
        rule.onNodeWithTag(DialogPresentation.SHEET_TAG).performTouchInput { swipeDown(durationMillis = 180) }
        rule.waitForIdle()
        rule.onNodeWithText("locked sheet").assertIsDisplayed()
        assertTrue(dismissals().isEmpty())
        rule.runOnIdle { renderer!!.dispose() }
        rule.waitForIdle()
        rule.onNodeWithText("locked sheet").assertDoesNotExist()
        assertTrue(dismissals().isEmpty())
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun immediateFocusWaitsForRealDialogContentAndRemovedTargetsReject() {
        create()
        val field = UiNode(1, C.TEXT_FIELD, props = mapOf(P.TEXT to UiValue.Str("")),
            modifier = listOf(tag("dialog-editor")), callbacks = mapOf(E.VALUE_CHANGE to 51))
        // Both calls run before the dialog exists or has registered its native focus handle.
        rule.runOnUiThread {
            renderer!!.apply(batch(UiTree(1, listOf(field))))
            renderer!!.execute(UiCommand.Focus(1))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("dialog-editor").assertIsFocused().performTextInput("dialog input")
        rule.waitUntil(5000) { events.any { it.type == E.VALUE_CHANGE && it.payload.getString(ComposeUiEventFields.TEXT) == "dialog input" } }
        rule.runOnUiThread {
            renderer!!.apply(batch(UiTree(2, listOf(text(2, "field removed")))))
            try { renderer!!.execute(UiCommand.Focus(1)); fail("Removed field must be rejected") }
            catch (error: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.NODE_DETACHED, error.code) }
        }
        rule.waitForIdle()
        rule.onNodeWithText("field removed").assertIsDisplayed()
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun focusCommandsWaitForActualWindowFocusAndKeepTheirOrder() {
        create()
        lateinit var borrowed: TextView
        rule.runOnUiThread { borrowed = TextView(rule.activity).apply { text = "window probe" } }
        fun target(id: Int, name: String, observe: Boolean = true) = UiNode(id, C.TEXT,
            props = mapOf(P.TEXT to UiValue.Str(name)),
            modifier = listOf(tag(name), ModifierOp(M.CLICKABLE, listOf(UiValue.Num((80 + id).toDouble())))),
            callbacks = if (observe) mapOf(E.FOCUS_CHANGE to 50 + id) else emptyMap())
        val native = UiNode(3, AndroidViewInteropV1.COMPONENT,
            props = mapOf(AndroidViewInteropV1.PROP_VIEW to UiValue.Num(31.0)))
        // Clickable controls use keyboard/d-pad focusability. A neutral first target receives
        // platform default-focus restoration after Blur, without introducing editor/IME behavior.
        apply(UiTree(1, listOf(UiNode(1, C.COLUMN, children = listOf(5, 2, 3, 4)),
            target(5, "focus-sentinel", observe = false), target(2, "waiting-target"), native,
            target(4, "last-target"))), listOf(AndroidViewBindingV1(3, 31, borrowed)))
        lateinit var targetWindow: Window
        rule.runOnIdle { targetWindow = dialogWindow(borrowed) }
        rule.waitUntil(5000) {
            var focused = false
            rule.runOnUiThread { focused = targetWindow.decorView.hasWindowFocus() }
            focused
        }
        withKeyboardMode(targetWindow) {
            rule.runOnUiThread { renderer!!.execute(UiCommand.Focus(5)) }
            rule.waitUntil(5000) {
                rule.onAllNodes(hasTestTag("focus-sentinel") and isFocused()).fetchSemanticsNodes().size == 1
            }
            rule.onNodeWithTag("waiting-target").assertIsNotFocused()
            rule.onNodeWithTag("last-target").assertIsNotFocused()
            var cover: Dialog? = null
            try {
                rule.runOnUiThread {
                    cover = Dialog(rule.activity).apply {
                        setContentView(TextView(rule.activity).apply { text = "temporary focus owner" })
                        show()
                    }
                }
                rule.waitUntil(5000) {
                    var focused = false
                    rule.runOnUiThread { focused = cover!!.window!!.decorView.hasWindowFocus() && !targetWindow.decorView.hasWindowFocus() }
                    focused
                }
                // Finish initial/window-transition notifications before the command interval.
                rule.onNodeWithTag("waiting-target").assertIsNotFocused()
                rule.onNodeWithTag("last-target").assertIsNotFocused()
                rule.waitForIdle()
                rule.runOnUiThread {
                    events.clear()
                    renderer!!.execute(UiCommand.Focus(2))
                    renderer!!.execute(UiCommand.Blur(2))
                    renderer!!.execute(UiCommand.Focus(4))
                }
                rule.waitForIdle()
                assertTrue("The command sequence must wait for its dialog's real window focus: $events",
                    events.none { it.type == E.FOCUS_CHANGE })
                rule.runOnUiThread { cover!!.dismiss() }
                rule.waitUntil(5000) {
                    events.any { it.type == E.FOCUS_CHANGE && it.nodeId == 4 && it.payload.getBoolean(ComposeUiEventFields.FOCUSED) }
                }
                rule.onNodeWithTag("last-target").assertIsFocused()
                rule.runOnIdle { assertTrue(targetWindow.decorView.hasWindowFocus()) }
                assertEquals(listOf(2 to true, 2 to false, 4 to true), events.filter { it.type == E.FOCUS_CHANGE }
                    .map { it.nodeId to it.payload.getBoolean(ComposeUiEventFields.FOCUSED) })
                assertTrue(events.none { it.type == E.ERROR })
            } finally { rule.runOnUiThread { cover?.dismiss() } }
        }
    }

    private fun withKeyboardMode(window: Window, action: () -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var previous = false
        rule.runOnUiThread { previous = window.decorView.isInTouchMode }
        try {
            instrumentation.setInTouchMode(false)
            // API 24 can leave inactive decor caches unchanged after a global mode update.
            // A real navigation event establishes keyboard input on the actual active window.
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
            rule.waitUntil(5000) {
                var keyboard = false
                rule.runOnUiThread { keyboard = window.decorView.hasWindowFocus() && !window.decorView.isInTouchMode }
                keyboard
            }
            action()
        } finally { instrumentation.setInTouchMode(previous) }
    }

    @Test fun disposedUnattachedPresentationCannotShowLaterOrKeepViewClaims() {
        create(DialogInteropV1.BOTTOM_SHEET, attach = false)
        lateinit var borrowed: TextView
        rule.runOnUiThread {
            borrowed = TextView(rule.activity).apply { text = "must not appear" }
            val anchor = renderer!!.view()
            val node = UiNode(1, AndroidViewInteropV1.COMPONENT,
                props = mapOf(AndroidViewInteropV1.PROP_VIEW to UiValue.Num(17.0)))
            renderer!!.applyWithViews(batch(UiTree(1, listOf(node))), listOf(AndroidViewBindingV1(1, 17, borrowed)))
            assertTrue(AndroidViewClaimsV1.isClaimed(borrowed))
            renderer!!.dispose()
            mountAnchor(anchor)
        }
        rule.waitForIdle()
        rule.onNodeWithTag(DialogPresentation.SHEET_TAG).assertDoesNotExist()
        rule.runOnIdle { assertNull(borrowed.parent); assertFalse(AndroidViewClaimsV1.isClaimed(borrowed)) }
        assertTrue(events.isEmpty())
    }

    private fun dialogWindow(view: View): Window {
        var parent = view.parent
        while (parent != null) {
            if (parent is DialogWindowProvider) return parent.window
            parent = (parent as? ViewGroup)?.parent
        }
        error("Native content did not belong to a real Compose Dialog window")
    }
}
