package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.findViewTreeComposeViewContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.ComposeUiComponents as C
import org.autojs.plugin.compose.api.ComposeUiProps as P
import org.autojs.plugin.compose.api.ComposeUiModifiers as M
import org.autojs.plugin.compose.api.ComposeUiEvents as E
import org.autojs.plugin.compose.api.ComposeUiEventFields as F
import org.autojs.plugin.compose.api.loading.*
import org.autojs.plugin.compose.api.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt

/** Renderer-local semantics/pixel tests. Host tests separately exercise the APK classloader boundary. */
class ComposeRendererTest {
    @get:Rule val rule = createAndroidComposeRule<RendererTestActivity>()
    private var renderer: ComposeUiRenderer? = null
    private var generation = 0L
    private val events = ConcurrentLinkedQueue<UiEvent>()

    @After fun dispose() { rule.runOnUiThread { renderer?.dispose(); renderer = null } }
    private fun create() {
        rule.runOnUiThread {
            val factory = ComposeUiRendererFactoryImpl()
            assertEquals(RendererCatalog.components, factory.capabilities().getStringArrayList(ComposeUiCapabilityKeys.COMPONENTS)!!.toSet())
            renderer = factory.create(object : ComposeUiHostEnvironment {
                override val hostContext = rule.activity
                override val mainExecutor = Executor { action -> if (Looper.myLooper() == Looper.getMainLooper()) action.run() else rule.activity.runOnUiThread(action) }
                override val eventSink = ComposeUiEventSink(events::add)
                override val initialTheme = ThemeSpec(seedArgb = 0xff336699.toInt(), dark = false)
                override val sessionId = 7
            })
            rule.activity.setContentView(renderer!!.view())
        }
        rule.waitForIdle()
    }
    private fun apply(tree: UiTree) {
        rule.runOnIdle { renderer!!.apply(UiPatchBatch(7, generation + 1, listOf(UiPatch.SetRoot(tree)))); generation++ }
        rule.waitForIdle()
    }
    private fun tag(name: String) = ModifierOp(M.TEST_TAG, listOf(UiValue.Str(name)))
    private fun size(value: Double) = ModifierOp(M.SIZE, listOf(UiValue.Dp(value)))
    private fun text(id: Int, value: String, name: String = "text") = UiNode(id, C.TEXT, props = mapOf(P.TEXT to UiValue.Str(value)), modifier = listOf(tag(name)))

    private fun capture(name: String): ImageBitmap {
        val node = rule.onNodeWithTag(name)
        if (Build.VERSION.SDK_INT >= 26) return node.captureToImage()
        // Window PixelCopy starts at API26. Draw the actual view tree into a software bitmap on API24.
        rule.waitForIdle()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        lateinit var full: Bitmap
        rule.runOnUiThread {
            val root = renderer!!.view()
            full = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(full))
        }
        val left = bounds.left.roundToInt().coerceIn(0, full.width - 1)
        val top = bounds.top.roundToInt().coerceIn(0, full.height - 1)
        val cropped = Bitmap.createBitmap(full, left, top, bounds.width.roundToInt().coerceIn(1, full.width - left), bounds.height.roundToInt().coerceIn(1, full.height - top))
        if (cropped !== full) full.recycle()
        return cropped.asImageBitmap()
    }

    @Test fun emptySingleAndUnknownDebugTreesComposeAndDispose() {
        create()
        assertTrue(events.isEmpty())
        rule.runOnIdle { renderer!!.apply(UiPatchBatch(7, 0, emptyList())) }
        apply(UiTree(1, listOf(text(1, "one"))))
        rule.onNodeWithTag("text").assertTextEquals("one")
        apply(UiTree(1, listOf(UiNode(1, "UnimplementedDebugType"))))
        rule.onNodeWithText("<unknown: UnimplementedDebugType>").assertExists()
        rule.runOnIdle {
            renderer!!.dispose(); renderer!!.dispose()
            try { renderer!!.view(); fail() } catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.SESSION_CLOSED, e.code) }
        }
    }

    @Test fun replacingARendererKeepsItsComposeContextOutOfTheSharedActivityAncestors() {
        var ancestorContext: Any? = null
        rule.runOnUiThread {
            // Model a previously installed plugin that left a Compose context on the host's
            // reusable ancestor. Keep the context reachable across disposal, as another loader can.
            val predecessor = ComposeView(rule.activity).apply { setContent {} }
            rule.activity.setContentView(predecessor)
            val parent = predecessor.parent as ViewGroup
            ancestorContext = requireNotNull(parent.findViewTreeComposeViewContext())
            predecessor.disposeComposition()
            parent.removeView(predecessor)
        }
        create()
        apply(UiTree(1, listOf(text(1, "before"))))
        var previousContext: Any? = null
        var previousOwner: Any? = null
        rule.runOnIdle {
            val root = renderer!!.view() as ViewGroup
            val child = root.getChildAt(0)
            val owner = requireNotNull(child.findViewTreeLifecycleOwner())
            val savedStateOwner = requireNotNull(child.findViewTreeSavedStateRegistryOwner())
            assertNotSame(rule.activity, owner)
            assertSame(owner, savedStateOwner)
            assertSame(rule.activity.lifecycle, owner.lifecycle)
            assertSame(rule.activity.savedStateRegistry, savedStateOwner.savedStateRegistry)
            assertSame(rule.activity, child.findViewTreeViewModelStoreOwner())
            previousOwner = owner
            previousContext = requireNotNull(child.findViewTreeComposeViewContext())
            val sharedParent = root.parent as ViewGroup
            assertNotSame(ancestorContext, previousContext)
            assertSame("Renderer caches must not replace the host ancestor's existing context", ancestorContext, sharedParent.findViewTreeComposeViewContext())
            renderer!!.dispose()
            sharedParent.removeView(root)
            renderer = null
        }
        create()
        apply(UiTree(1, listOf(text(1, "after"))))
        rule.onNodeWithTag("text").assertTextEquals("after")
        rule.runOnIdle {
            val root = renderer!!.view() as ViewGroup
            val child = root.getChildAt(0)
            assertNotSame(previousOwner, child.findViewTreeLifecycleOwner())
            assertNotSame(previousContext, child.findViewTreeComposeViewContext())
            assertSame(ancestorContext, (root.parent as View).findViewTreeComposeViewContext())
            assertTrue(events.none { it.type == E.ERROR })
        }
    }

    @Test fun basicAndInteractiveComponentsHaveComposingHandlers() {
        create()
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            // Stateful fields, lazy layouts, modal windows and commands have separate fixtures.
            val basic = org.autojs.plugin.compose.api.catalog.ComponentCatalog.V1.components.map { it.name }.toSet() - setOf(C.TEXT_FIELD, C.OUTLINED_TEXT_FIELD, C.LAZY_COLUMN, C.LAZY_ROW,
                C.SCAFFOLD, C.TOP_APP_BAR, C.ALERT_DIALOG, C.CIRCULAR_PROGRESS_INDICATOR, C.LINEAR_PROGRESS_INDICATOR, C.SNACKBAR)
            val children = basic.mapIndexed { index, name ->
                val props = when (name) {
                    C.TEXT -> mapOf(P.TEXT to UiValue.Str("text"))
                    C.ICON -> mapOf(P.NAME to UiValue.IconName("Home"))
                    C.IMAGE -> mapOf(P.SRC to UiValue.BitmapRef(bitmap))
                    else -> emptyMap()
                }
                UiNode(index + 2, name, props = props, modifier = listOf(tag(name), size(36.0)))
            }
            apply(UiTree(1, listOf(UiNode(1, C.COLUMN, children = children.map { it.nodeId })) + children))
            basic.forEach { rule.onNodeWithTag(it, useUnmergedTree = true).assertExists() }
            assertTrue(events.none { it.type == E.ERROR })
        } finally { rule.runOnIdle { renderer!!.dispose(); bitmap.recycle() } }
    }

    @Test fun scopedWeightAndTextStyleApplyAndInvalidBatchKeepsTheLastFrame() {
        create()
        val root = UiNode(1, C.ROW, modifier = listOf(ModifierOp(M.WIDTH, listOf(UiValue.Dp(200.0)))), children = listOf(2, 3))
        val a = UiNode(2, C.SPACER, modifier = listOf(tag("a"), ModifierOp(M.WEIGHT, listOf(UiValue.Num(1.0)), ScopeKind.COLUMN_ROW), ModifierOp(M.HEIGHT, listOf(UiValue.Dp(20.0)))))
        val b = UiNode(3, C.TEXT, props = mapOf(P.TEXT to UiValue.Str("styled"), P.STYLE to UiValue.TextStyle(fontWeight = 700, fontSize = 18.0)),
            modifier = listOf(tag("b"), ModifierOp(M.WEIGHT, listOf(UiValue.Num(3.0)), ScopeKind.COLUMN_ROW)))
        apply(UiTree(1, listOf(root, a, b)))
        rule.onNodeWithTag("a").assertWidthIsEqualTo(50.dp)
        rule.onNodeWithTag("b").assertWidthIsEqualTo(150.dp)
        val results = ArrayList<TextLayoutResult>()
        rule.onNodeWithTag("b").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(results)) }
        assertEquals(18.sp, results.single().layoutInput.style.fontSize)
        assertEquals(700, results.single().layoutInput.style.fontWeight!!.weight)
        rule.runOnIdle {
            try {
                renderer!!.apply(UiPatchBatch(7, generation + 1, listOf(UiPatch.SetProps(3, mapOf(P.TEXT to UiValue.Str("bad"))),
                    UiPatch.SetProps(2, emptyMap(), listOf(ModifierOp(M.ALIGN, listOf(UiValue.Enum("center")), ScopeKind.BOX))))))
                fail()
            } catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.SCOPE_MISMATCH, e.code) }
        }
        rule.onNodeWithTag("b").assertTextEquals("styled")
    }

    @Test fun orderedPaddingAndBackgroundHaveDifferentPixels() {
        create()
        val padding = ModifierOp(M.PADDING, listOf(UiValue.Dp(10.0)))
        val background = ModifierOp(M.BACKGROUND, listOf(UiValue.Color(0xffff0000.toInt())))
        val a = UiNode(2, C.BOX, modifier = listOf(tag("padding-first"), size(40.0), padding, background))
        val b = UiNode(3, C.BOX, modifier = listOf(tag("background-first"), size(40.0), background, padding))
        apply(UiTree(1, listOf(UiNode(1, C.ROW, modifier = listOf(ModifierOp(M.BACKGROUND, listOf(UiValue.Color(0xffffffff.toInt())))), children = listOf(2, 3)), a, b)))
        val imageA = capture("padding-first").toPixelMap()
        val imageB = capture("background-first").toPixelMap()
        assertTrue(imageA[2, 2].green > 0.9f)
        assertTrue(imageB[2, 2].red > 0.9f && imageB[2, 2].green < 0.1f)
        assertTrue(imageA[imageA.width / 2, imageA.height / 2].red > 0.9f)
    }

    @Test fun controlledToggleAndLongPressPublishOnlyTheIntendedCallbacks() {
        create()
        apply(UiTree(1, listOf(UiNode(1, C.COLUMN, children = listOf(2, 3)),
            UiNode(2, C.CHECKBOX, modifier = listOf(tag("check")), callbacks = mapOf(E.CHECKED_CHANGE to 5)),
            UiNode(3, C.BUTTON, modifier = listOf(tag("button")), children = listOf(4), callbacks = mapOf(E.CLICK to 6, E.LONG_CLICK to 7)), text(4, "hold"))))
        rule.onNodeWithTag("check").assertIsOff().performClick().assertIsOff()
        rule.runOnIdle {
            val event = events.single { it.type == E.CHECKED_CHANGE }
            assertEquals(5, event.callbackId); assertEquals(1L, event.generation); assertTrue(event.payload.getBoolean(F.CHECKED))
        }
        // This renderer owns an Android frame clock. Wait for its real timeout, not only synthetic event time.
        rule.onNodeWithTag("button").performTouchInput { down(center) }
        rule.waitUntil(3000) { events.any { it.callbackId == 7 } }
        rule.onNodeWithTag("button").performTouchInput { up() }
        rule.runOnIdle {
            assertEquals(1, events.count { it.callbackId == 7 })
            assertEquals(0, events.count { it.callbackId == 6 })
        }
    }

    @Test fun scrollCommandsReachTheNodeAndUnmountInvalidatesItsHandle() {
        create()
        apply(UiTree(1, listOf(UiNode(1, C.COLUMN, modifier = listOf(tag("scroll"), ModifierOp(M.HEIGHT, listOf(UiValue.Dp(70.0))), ModifierOp(M.VERTICAL_SCROLL)),
            children = listOf(2, 3), callbacks = mapOf(E.SCROLL to 9)),
            UiNode(2, C.SPACER, modifier = listOf(ModifierOp(M.HEIGHT, listOf(UiValue.Dp(300.0))))), text(3, "end"))))
        rule.runOnIdle { renderer!!.execute(UiCommand.ScrollTo(1, offset = 40)) }
        rule.waitUntil(5000) { events.any { it.callbackId == 9 && it.payload.getInt(F.OFFSET) == 40 } }
        rule.runOnIdle {
            renderer!!.apply(UiPatchBatch(7, ++generation, listOf(UiPatch.SetProps(1, emptyMap(), listOf(tag("scroll"),
                ModifierOp(M.HEIGHT, listOf(UiValue.Dp(70.0))), ModifierOp(M.BACKGROUND, listOf(UiValue.Color(0xffeeeeee.toInt()))), ModifierOp(M.VERTICAL_SCROLL))))))
        }
        rule.waitForIdle()
        val range = rule.onNodeWithTag("scroll").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange]
        assertEquals(40f, range.value(), 0f)
        rule.runOnIdle {
            // Test the interval before Compose disposes the old handle, not only after it unmounts.
            renderer!!.apply(UiPatchBatch(7, ++generation, listOf(UiPatch.SetRoot(UiTree(10, listOf(text(10, "replacement")))))))
            try { renderer!!.execute(UiCommand.ScrollTo(1, offset = 0)); fail() }
            catch (e: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.NODE_DETACHED, e.code) }
        }
    }

    @Test fun borrowedBitmapCanBeRecycledWhileMountedWithoutCrashingDrawing() {
        create()
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
        apply(UiTree(1, listOf(UiNode(1, C.IMAGE, props = mapOf(P.SRC to UiValue.BitmapRef(bitmap)), modifier = listOf(tag("image"), size(40.0))))))
        rule.onNodeWithTag("image").assertExists()
        rule.runOnIdle { bitmap.recycle(); renderer!!.view().invalidate() }
        capture("image")
        apply(UiTree(2, listOf(text(2, "alive"))))
        rule.onNodeWithTag("text").assertTextEquals("alive")
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun traversalFailuresAreBoundedAndANewFrameCanRecover() {
        rule.runOnUiThread {
            val failures = ArrayList<Throwable>()
            val container = GuardedComposeContainer(rule.activity, failures::add)
            var broken = true
            val child = object : View(rule.activity) {
                override fun onMeasure(width: Int, height: Int) {
                    if (broken) throw IllegalStateException("Test measure failure")
                    setMeasuredDimension(20, 20)
                }
                override fun onDraw(canvas: Canvas) {
                    if (broken) throw IllegalStateException("Test draw failure")
                    canvas.drawColor(android.graphics.Color.GREEN)
                }
            }
            container.mount(child)
            val spec = View.MeasureSpec.makeMeasureSpec(80, View.MeasureSpec.EXACTLY)
            container.measure(spec, spec)
            assertEquals(80, container.measuredWidth)
            container.forceLayout(); container.measure(spec, spec)
            assertEquals(1, failures.size)
            broken = false; container.recover(); container.measure(spec, spec); container.layout(0, 0, 80, 80)
            val pixels = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(pixels)
            container.draw(canvas)
            assertEquals(android.graphics.Color.GREEN, pixels.getPixel(2, 2))
            broken = true; child.invalidate(); container.draw(canvas); container.draw(canvas)
            assertEquals(2, failures.size)
            assertEquals(1, canvas.saveCount)
            container.removeAllViews(); pixels.recycle()
        }
    }

    @Test fun disabledInnerClickableDoesNotDisableTheOuterPaddingHitRegion() {
        create()
        apply(UiTree(1, listOf(UiNode(1, C.TEXT, props = mapOf(P.TEXT to UiValue.Str("inside")), modifier = listOf(
            tag("ordered-click"), size(80.0), ModifierOp(M.CLICKABLE, listOf(UiValue.Num(20.0))),
            ModifierOp(M.PADDING, listOf(UiValue.Dp(20.0))), ModifierOp(M.CLICKABLE, listOf(UiValue.Num(30.0), UiValue.Bool(false))),
        )))))
        rule.onNodeWithTag("ordered-click").performTouchInput { click(Offset(5f, 5f)) }
        rule.runOnIdle { assertEquals(1, events.count { it.callbackId == 20 }); assertEquals(0, events.count { it.callbackId == 30 }) }
    }

    @Test fun spaceDistributionSurvivesSpacingAndBoxSlotsReceiveTheirScope() {
        create()
        apply(UiTree(1, listOf(UiNode(1, C.ROW, props = mapOf(P.ARRANGEMENT to UiValue.Enum("spaceBetween"), P.SPACING to UiValue.Dp(10.0)),
            modifier = listOf(ModifierOp(M.WIDTH, listOf(UiValue.Dp(200.0)))), children = listOf(2, 3)),
            UiNode(2, C.SPACER, modifier = listOf(tag("left"), size(20.0))), UiNode(3, C.SPACER, modifier = listOf(tag("right"), size(20.0))))))
        rule.onNodeWithTag("right").assertPositionInRootIsEqualTo(180.dp, 0.dp)
        apply(UiTree(1, listOf(UiNode(1, C.BOX, modifier = listOf(size(100.0)), slots = mapOf(ComposeUiSlots.CONTENT to 2)),
            UiNode(2, C.SPACER, modifier = listOf(tag("corner"), size(20.0), ModifierOp(M.ALIGN, listOf(UiValue.Enum("bottomEnd")), ScopeKind.BOX))))))
        rule.onNodeWithTag("corner").assertPositionInRootIsEqualTo(80.dp, 80.dp)
    }

    @Test fun implicitClickableTextPublishesKeyboardFocusChanges() {
        create()
        apply(UiTree(1, listOf(UiNode(1, C.TEXT, props = mapOf(P.TEXT to UiValue.Str("focus")), modifier = listOf(tag("focus")),
            callbacks = mapOf(E.CLICK to 50, E.FOCUS_CHANGE to 51)))))
        // Go through Android's input pipeline so the real view exits touch mode.
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        rule.onNodeWithTag("focus").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.waitUntil(3000) { events.any { it.callbackId == 51 && it.payload.getBoolean(F.FOCUSED) } }
        rule.onNodeWithTag("focus").assertIsFocused()
    }

    @Test fun liveThemeOverridesTypographyAndPlatformDynamicFallbackReachTheComposition() {
        create()
        apply(UiTree(1, listOf(UiNode(1, C.SURFACE, modifier = listOf(tag("surface"), size(80.0)), children = listOf(2)), text(2, "theme"))))
        rule.runOnIdle { renderer!!.setTheme(ThemeSpec(colorOverrides = mapOf(ComposeUiThemeColors.SURFACE to 0xffff0000.toInt()), fontFamily = "monospace", fontScale = 1.5)) }
        rule.waitForIdle()
        val colored = capture("surface").toPixelMap()
        assertTrue(colored[colored.width - 3, colored.height - 3].green < 0.05f)
        val layouts = ArrayList<TextLayoutResult>()
        rule.onNodeWithTag("text").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1.5f, layouts.single().layoutInput.density.fontScale, 0f)
        assertEquals(FontFamily.Monospace, layouts.single().layoutInput.style.fontFamily)
        val dynamic = ThemeSpec(seedArgb = 0xff336699.toInt(), dark = true, dynamicColor = true)
        rule.runOnIdle { renderer!!.setTheme(dynamic) }
        rule.waitForIdle()
        val expected = if (Build.VERSION.SDK_INT >= 31) dynamicDarkColorScheme(rule.activity).surface else ThemeMapper.colorScheme(dynamic).surface
        val pixels = capture("surface").toPixelMap()
        val actual = pixels[pixels.width - 3, pixels.height - 3]
        assertEquals(expected.red, actual.red, 0.02f)
        assertEquals(expected.green, actual.green, 0.02f)
        assertEquals(expected.blue, actual.blue, 0.02f)
        assertTrue(events.none { it.type == E.ERROR })
    }
}
