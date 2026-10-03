package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import android.view.KeyEvent
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiEventFields as F
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiModifiers as M
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.ComposeUiSlots as S
import org.autojs.plugin.compose.api.loading.*
import org.autojs.plugin.compose.api.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor

/** Real renderer frames and callbacks; host instrumentation separately covers class loading. */
class AdvancedRendererTest {
    @get:Rule val rule = createAndroidComposeRule<RendererTestActivity>()
    private var renderer: ComposeUiRenderer? = null
    private var generation = 0L
    private val events = ConcurrentLinkedQueue<UiEvent>()

    @After fun dispose() { rule.runOnUiThread { renderer?.dispose(); renderer = null } }

    private fun create() {
        rule.runOnUiThread {
            renderer = ComposeUiRendererFactoryImpl().create(object : ComposeUiHostEnvironment {
                override val hostContext = rule.activity
                override val mainExecutor = Executor { action -> if (Looper.myLooper() == Looper.getMainLooper()) action.run() else rule.activity.runOnUiThread(action) }
                override val eventSink = ComposeUiEventSink(events::add)
                override val initialTheme = ThemeSpec(dark = false)
                override val sessionId = 17
            })
            rule.activity.setContentView(renderer!!.view())
        }
        rule.waitForIdle()
    }

    private fun patch(vararg patches: UiPatch): Long {
        var elapsed = 0L
        rule.runOnIdle {
            val start = SystemClock.elapsedRealtimeNanos()
            renderer!!.apply(UiPatchBatch(17, ++generation, patches.toList()))
            elapsed = SystemClock.elapsedRealtimeNanos() - start
        }
        rule.waitForIdle()
        return elapsed
    }

    private fun tag(value: String) = ModifierOp(M.TEST_TAG, listOf(UiValue.Str(value)))
    private fun dimension(name: String, value: Double) = ModifierOp(name, listOf(UiValue.Dp(value)))
    private fun text(id: Int, value: String, name: String = "text-$id") = UiNode(id, C.TEXT,
        props = mapOf(P.TEXT to UiValue.Str(value)), modifier = listOf(tag(name)))

    @Test fun thousandRowPatchAndRealScrollFramesRetainKeysAndControlledSwitchState() {
        create()
        val rows = (0 until 1000).flatMap { index ->
            val id = index * 3 + 2
            listOf(
                UiNode(id, C.ROW, key = "row-$index", modifier = listOf(tag("row-$index"), dimension(M.HEIGHT, 48.0)), children = listOf(id + 1, id + 2)),
                text(id + 1, "row $index"),
                UiNode(id + 2, C.SWITCH, props = mapOf(P.CHECKED to UiValue.Bool(index == 2)),
                    modifier = listOf(tag("switch-$index")), callbacks = if (index == 0) mapOf(E.CHECKED_CHANGE to 20) else emptyMap()),
            )
        }
        val root = UiNode(1, C.LAZY_COLUMN, props = mapOf(P.CONTENT_PADDING to UiValue.Dp(8.0), P.SPACING to UiValue.Dp(4.0)),
            modifier = listOf(tag("list"), dimension(M.WIDTH, 300.0), dimension(M.HEIGHT, 320.0)),
            children = (0 until 1000).map { it * 3 + 2 }, callbacks = mapOf(E.SCROLL to 21))
        val initial = patch(UiPatch.SetRoot(UiTree(1, listOf(root) + rows)))
        rule.onNodeWithTag("switch-0").assertIsOff().performClick().assertIsOff()
        assertTrue(events.any { it.callbackId == 20 && it.payload.getBoolean(F.CHECKED) })
        val update = patch(UiPatch.SetProps(4, mapOf(P.CHECKED to UiValue.Bool(true))),
            UiPatch.SetProps(3, mapOf(P.TEXT to UiValue.Str("updated"))), UiPatch.Move(1, 2, 2))
        rule.onNodeWithTag("switch-0").assertIsOn()
        rule.onNodeWithTag("switch-2").assertIsOn()
        assertTrue(rule.onNodeWithTag("row-0").fetchSemanticsNode().boundsInRoot.top >
            rule.onNodeWithTag("row-2").fetchSemanticsNode().boundsInRoot.top)

        val frames = FrameSamples()
        rule.runOnUiThread { frames.start() }
        try {
            rule.onNodeWithTag("list").performTouchInput { swipeUp(durationMillis = 500) }
            rule.waitUntil(5000) { events.any { it.callbackId == 21 && it.payload.getInt(F.FIRST_VISIBLE_INDEX) > 2 } }
            rule.waitForIdle()
        } finally { rule.runOnUiThread { frames.stop() } }
        val sampled = frames.milliseconds.sorted()
        assertTrue("The real scroll window should include display frames", sampled.isNotEmpty())
        reportMetrics("rows=1000 nodes=3001 initial_apply_ms=${initial / 1_000_000.0} patch_apply_ms=${update / 1_000_000.0} " +
            "gesture_frame_count=${sampled.size} gesture_frame_p50_ms=${sampled[sampled.lastIndex / 2]} " +
            "gesture_frame_p95_ms=${sampled[(sampled.lastIndex * 0.95).toInt()]} gesture_frame_max_ms=${sampled.last()}")

        rule.runOnIdle { renderer!!.execute(UiCommand.ScrollTo(1, index = 500, offset = 7)) }
        rule.waitUntil(5000) { events.any { it.callbackId == 21 && it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 500 && it.payload.getInt(F.OFFSET) == 7 } }
        rule.onNodeWithTag("switch-500").assertIsOff()
        rule.runOnIdle { renderer!!.execute(UiCommand.ScrollTo(1, index = 2)) }
        rule.waitUntil(5000) { events.any { it.callbackId == 21 && it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 2 && it.payload.getInt(F.OFFSET) == 0 } }
        rule.onNodeWithTag("switch-0").assertIsOn()
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun lazyRowAppliesLogicalPaddingAndCommandScrollingWhileUserGesturesAreDisabled() {
        create()
        val children = (2..21).map { id -> UiNode(id, C.TEXT, key = "item-$id", props = mapOf(P.TEXT to UiValue.Str("item $id")),
            modifier = listOf(tag("item-$id"), dimension(M.WIDTH, 80.0), dimension(M.HEIGHT, 40.0))) }
        val root = UiNode(1, C.LAZY_ROW, props = mapOf(
            P.CONTENT_PADDING to UiValue.ListOf(listOf(11.0, 7.0, 13.0, 9.0).map { UiValue.Dp(it) }),
            P.SPACING to UiValue.Dp(5.0), P.ALIGNMENT to UiValue.Enum("center"), P.USER_SCROLL_ENABLED to UiValue.Bool(false)),
            modifier = listOf(tag("row"), dimension(M.WIDTH, 300.0), dimension(M.HEIGHT, 80.0)),
            children = children.map { it.nodeId }, callbacks = mapOf(E.SCROLL to 30))
        patch(UiPatch.SetRoot(UiTree(1, listOf(root) + children)))
        rule.onNodeWithTag("item-2").assertPositionInRootIsEqualTo(11.dp, 19.dp)
        rule.onNodeWithTag("row").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("item-2").assertIsDisplayed()
        rule.runOnIdle { renderer!!.execute(UiCommand.ScrollTo(1, index = 10)) }
        rule.waitUntil(5000) { events.any { it.callbackId == 30 && it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 10 } }
        rule.onNodeWithTag("item-12").assertIsDisplayed()
        rule.runOnIdle {
            failure(ComposeUiErrorCodes.INVALID_ARGUMENT) { renderer!!.execute(UiCommand.ScrollTo(1, index = 20)) }
            renderer!!.apply(UiPatchBatch(17, ++generation, listOf(UiPatch.SetRoot(UiTree(50, listOf(text(50, "replacement")))))))
            // Exercise the accepted-tree boundary before the old composition unregisters the handle.
            failure(ComposeUiErrorCodes.NODE_DETACHED) { renderer!!.execute(UiCommand.ScrollTo(1, index = 0)) }
        }
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun lazyScrollUsesAcceptedItemsWhenGrowthOrShrinkAndCommandShareOneMainTurn() {
        create()
        fun items(count: Int): UiTree {
            val children = (0 until count).map { index ->
                UiNode(index + 2, C.TEXT, key = "item-$index", props = mapOf(P.TEXT to UiValue.Str("item $index")),
                    modifier = listOf(tag("growing-$index"), dimension(M.HEIGHT, 40.0)))
            }
            return UiTree(1, listOf(UiNode(1, C.LAZY_COLUMN, modifier = listOf(tag("changing-list"),
                dimension(M.WIDTH, 200.0), dimension(M.HEIGHT, 100.0)), children = children.map { it.nodeId },
                callbacks = mapOf(E.SCROLL to 31))) + children)
        }
        patch(UiPatch.SetRoot(items(5)))
        rule.runOnIdle {
            renderer!!.apply(UiPatchBatch(17, ++generation, listOf(UiPatch.SetRoot(items(30)))))
            // No composition/layout can run between accepting the larger tree and this command.
            renderer!!.execute(UiCommand.ScrollTo(1, index = 25, offset = 3))
        }
        rule.waitUntil(5000) {
            events.any { it.type == E.ERROR || it.callbackId == 31 && it.generation == generation &&
                it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 25 && it.payload.getInt(F.OFFSET) == 3 }
        }
        assertTrue(events.none { it.type == E.ERROR })
        rule.onNodeWithTag("growing-25").assertIsDisplayed()
        rule.runOnIdle {
            renderer!!.apply(UiPatchBatch(17, ++generation, listOf(UiPatch.SetRoot(items(4)))))
            // The old composed count must not make an out-of-range target appear valid either.
            failure(ComposeUiErrorCodes.INVALID_ARGUMENT) { renderer!!.execute(UiCommand.ScrollTo(1, index = 25)) }
            renderer!!.execute(UiCommand.ScrollTo(1, index = 1, offset = 7))
        }
        rule.waitUntil(5000) {
            events.any { it.type == E.ERROR || it.callbackId == 31 && it.generation == generation &&
                it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 1 && it.payload.getInt(F.OFFSET) == 7 }
        }
        rule.onNodeWithTag("growing-1").assertIsDisplayed()
        rule.onNodeWithTag("growing-25").assertDoesNotExist()
        assertTrue(events.none { it.type == E.ERROR })
        rule.runOnIdle {
            val replacement = UiTree(1, listOf(text(1, "replacement")))
            renderer!!.apply(UiPatchBatch(17, ++generation, listOf(UiPatch.SetRoot(replacement))))
            // Old lazy handles must not authorize commands for a newly accepted non-lazy node.
            failure(ComposeUiErrorCodes.INVALID_ARGUMENT) { renderer!!.execute(UiCommand.ScrollTo(1, index = 0)) }
            failure(ComposeUiErrorCodes.INVALID_ARGUMENT) { renderer!!.execute(UiCommand.ScrollTo(1, offset = 0)) }
        }
    }

    @Test fun scaffoldSlotsProgressAndControlledDialogUseTheCurrentFrame() {
        create()
        val nodes = listOf(
            UiNode(1, C.SCAFFOLD, modifier = listOf(ModifierOp(M.FILL_MAX_SIZE)), slots = mapOf(S.TOP_BAR to 2, S.CONTENT to 3)),
            UiNode(2, C.TOP_APP_BAR, slots = mapOf(S.TITLE to 4, S.NAVIGATION_ICON to 5, S.ACTIONS to 6)),
            UiNode(3, C.COLUMN, children = listOf(9, 10, 11, 12)), text(4, "toolbar"),
            UiNode(5, C.TEXT_BUTTON, children = listOf(7), callbacks = mapOf(E.CLICK to 40)),
            UiNode(6, C.TEXT_BUTTON, children = listOf(8), callbacks = mapOf(E.CLICK to 41)), text(7, "back"), text(8, "tools"),
            UiNode(9, C.CIRCULAR_PROGRESS_INDICATOR, props = mapOf(P.PROGRESS to UiValue.Num(0.35)), modifier = listOf(tag("circular"))),
            UiNode(10, C.LINEAR_PROGRESS_INDICATOR, props = mapOf(P.PROGRESS to UiValue.Num(0.65)), modifier = listOf(tag("linear"))),
            UiNode(11, C.ALERT_DIALOG, props = mapOf(P.OPEN to UiValue.Bool(false)), modifier = listOf(tag("dialog")),
                slots = mapOf(S.CONFIRM to 13, S.DISMISS to 15, S.TITLE to 17, S.TEXT to 18), callbacks = mapOf(E.DISMISS_REQUEST to 42)),
            text(12, "body"), UiNode(13, C.TEXT_BUTTON, children = listOf(14), callbacks = mapOf(E.CLICK to 43)), text(14, "confirm"),
            UiNode(15, C.TEXT_BUTTON, children = listOf(16), callbacks = mapOf(E.CLICK to 44)), text(16, "cancel"),
            text(17, "dialog title"), text(18, "details"),
        )
        patch(UiPatch.SetRoot(UiTree(1, nodes)))
        rule.onNodeWithText("toolbar").assertIsDisplayed()
        rule.onNodeWithText("back").performClick()
        rule.onNodeWithText("tools").performClick()
        assertEquals(setOf(40, 41), events.filter { it.type == E.CLICK }.map { it.callbackId }.toSet())
        assertTrue(rule.onNodeWithText("body").fetchSemanticsNode().boundsInRoot.top >
            rule.onNodeWithText("toolbar").fetchSemanticsNode().boundsInRoot.bottom)
        assertEquals(0.35f, rule.onNodeWithTag("circular").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current, 0.001f)
        assertEquals(0.65f, rule.onNodeWithTag("linear").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current, 0.001f)
        patch(UiPatch.SetProps(11, mapOf(P.OPEN to UiValue.Bool(true))))
        rule.onNodeWithTag("dialog").assertExists()
        rule.onNodeWithText("details").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        rule.waitUntil(5000) { events.any { it.callbackId == 42 } }
        rule.onNodeWithTag("dialog").assertExists()
        rule.onNodeWithText("confirm").performClick()
        rule.onNodeWithTag("dialog").assertExists()
        assertTrue(events.any { it.callbackId == 43 && it.generation == generation })
        patch(UiPatch.SetProps(11, mapOf(P.OPEN to UiValue.Bool(false))),
            UiPatch.SetProps(9, mapOf(P.PROGRESS to UiValue.Null)), UiPatch.SetProps(10, mapOf(P.PROGRESS to UiValue.Null)))
        rule.onNodeWithTag("dialog").assertDoesNotExist()
        assertEquals(ProgressBarRangeInfo.Indeterminate, rule.onNodeWithTag("circular").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo])
        assertEquals(ProgressBarRangeInfo.Indeterminate, rule.onNodeWithTag("linear").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo])
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun snackbarReportsOneTerminalResultWithCapturedGenerationAndCancelsRemovedHosts() {
        create()
        fun scaffold(id: Int) = UiTree(id, listOf(UiNode(id, C.SCAFFOLD, children = listOf(id + 1)), text(id + 1, "body")))
        patch(UiPatch.SetRoot(scaffold(1)))
        rule.runOnIdle { renderer!!.execute(UiCommand.ShowSnackbar("first", "Act", SnackbarDuration.INDEFINITE, 50)) }
        rule.onNodeWithText("first").assertIsDisplayed()
        patch(UiPatch.SetProps(2, mapOf(P.TEXT to UiValue.Str("updated body"))))
        rule.onNodeWithText("Act").performClick()
        rule.waitUntil(5000) { events.any { it.callbackId == 50 } }
        events.single { it.callbackId == 50 }.let {
            assertEquals(E.ACTION, it.type); assertEquals(1L, it.generation); assertEquals(ComposeUiContract.NO_NODE_ID, it.nodeId)
        }

        rule.runOnIdle { renderer!!.execute(UiCommand.ShowSnackbar("second", duration = SnackbarDuration.INDEFINITE, callbackId = 51)) }
        rule.onNodeWithText("second").assertIsDisplayed()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss) and hasAnyDescendant(hasText("second")), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.Dismiss) { it() }
        rule.waitUntil(5000) { events.any { it.callbackId == 51 } }
        assertEquals(E.DISMISS, events.single { it.callbackId == 51 }.type)

        rule.runOnIdle {
            renderer!!.execute(UiCommand.ShowSnackbar("active", duration = SnackbarDuration.INDEFINITE, callbackId = 52))
            renderer!!.execute(UiCommand.ShowSnackbar("queued", duration = SnackbarDuration.INDEFINITE, callbackId = 53))
        }
        rule.onNodeWithText("active").assertIsDisplayed()
        patch(UiPatch.SetRoot(UiTree(10, listOf(text(10, "replacement")))))
        for (callback in 52..53) events.single { it.callbackId == callback }.let {
            assertEquals(E.DISMISS, it.type); assertEquals(2L, it.generation)
        }
        rule.onNodeWithText("active").assertDoesNotExist()
        rule.onNodeWithText("queued").assertDoesNotExist()
        rule.runOnIdle { failure(ComposeUiErrorCodes.INVALID_ARGUMENT) { renderer!!.execute(UiCommand.ShowSnackbar("missing")) } }

        patch(UiPatch.SetRoot(UiTree(20, listOf(UiNode(20, C.SCAFFOLD, slots = mapOf(S.SNACKBAR_HOST to 21)), text(21, "custom host")))))
        rule.runOnIdle { failure(ComposeUiErrorCodes.INVALID_ARGUMENT) { renderer!!.execute(UiCommand.ShowSnackbar("custom")) } }
        patch(UiPatch.SetRoot(scaffold(30)))
        rule.runOnIdle { renderer!!.execute(UiCommand.ShowSnackbar("closing", duration = SnackbarDuration.INDEFINITE, callbackId = 54)) }
        rule.onNodeWithText("closing").assertIsDisplayed()
        rule.runOnIdle { renderer!!.dispose() }
        rule.waitForIdle()
        assertTrue(events.none { it.callbackId == 54 || it.type == E.ERROR })
    }

    private fun failure(code: String, action: () -> Unit) {
        try { action(); fail("Expected $code") }
        catch (failure: ComposeUiContractException) { assertEquals(code, failure.code) }
    }

    private fun reportMetrics(value: String) {
        val record = "api=${Build.VERSION.SDK_INT} abi=${Build.SUPPORTED_ABIS.first()} $value"
        Log.i("ComposeUiP25", record)
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply { putString("compose_ui_p25_metrics", record) })
    }

    /** Actual Choreographer frame intervals during the gesture/settling window, not synthetic test time. */
    private class FrameSamples : Choreographer.FrameCallback {
        val milliseconds = ArrayList<Double>()
        private var active = false
        private var previous = 0L
        fun start() { active = true; previous = 0L; milliseconds.clear(); Choreographer.getInstance().postFrameCallback(this) }
        fun stop() { active = false; Choreographer.getInstance().removeFrameCallback(this) }
        override fun doFrame(frameTimeNanos: Long) {
            if (!active) return
            if (previous != 0L) milliseconds += (frameTimeNanos - previous) / 1_000_000.0
            previous = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
}
