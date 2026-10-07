package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.os.Looper
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
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
import org.autojs.plugin.compose.api.v2.ComposeUiV2
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Components as V
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Props as VP
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Slots as VS
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Events as VE
import org.autojs.plugin.compose.api.v2.ComposeUiV2.Fields as VF
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import kotlin.math.roundToInt

/** V2 uses real Material interactions and rendered positions, not mocked component callbacks. */
class WideRendererTest {
    @get:Rule val rule = createAndroidComposeRule<RendererTestActivity>()
    private var renderer: ComposeUiRenderer? = null
    private var generation = 0L
    private val events = ConcurrentLinkedQueue<UiEvent>()

    @After fun close() { rule.runOnUiThread { renderer?.dispose(); renderer = null } }

    private fun create() {
        rule.runOnUiThread {
            val factory = ComposeUiRendererFactoryImpl()
            assertEquals(2, factory.contractVersion())
            assertTrue(factory.capabilities().getStringArrayList(ComposeUiCapabilityKeys.FEATURES)!!.contains(ComposeUiV2.FEATURE))
            renderer = factory.create(object : ComposeUiHostEnvironment {
                override val hostContext = rule.activity
                override val mainExecutor = Executor { task -> if (Looper.myLooper() == Looper.getMainLooper()) task.run() else rule.activity.runOnUiThread(task) }
                override val eventSink = ComposeUiEventSink(events::add)
                override val initialTheme = ThemeSpec(dark = false)
                override val sessionId = 83
            })
            rule.activity.setContentView(renderer!!.view())
        }
        rule.waitForIdle()
    }
    private fun patch(vararg patches: UiPatch) {
        rule.runOnIdle { renderer!!.apply(UiPatchBatch(83, ++generation, patches.toList())) }
        rule.waitForIdle()
    }
    private fun root(vararg nodes: UiNode) = patch(UiPatch.SetRoot(UiTree(nodes.first().nodeId, nodes.toList())))
    private fun tag(name: String) = ModifierOp(M.TEST_TAG, listOf(UiValue.Str(name)))
    private fun size(name: String, value: Double) = ModifierOp(name, listOf(UiValue.Dp(value)))
    private fun text(id: Int, value: String, key: String? = null, modifier: List<ModifierOp> = emptyList()) =
        UiNode(id, C.TEXT, key = key, props = mapOf(P.TEXT to UiValue.Str(value)), modifier = listOf(tag("text-$id")) + modifier)
    private fun expect(callback: Int, check: (UiEvent) -> Boolean = { true }): UiEvent {
        rule.waitUntil(5000) { events.any { it.callbackId == callback && check(it) } }
        return events.last { it.callbackId == callback && check(it) }
    }
    private fun noErrors() { assertTrue(events.filter { it.type == E.ERROR }.joinToString { it.payload.toString() }, events.none { it.type == E.ERROR }) }
    private fun back() { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); rule.waitForIdle() }

    @Test fun navigationBarsRailAndTabsKeepSelectionUntilTheHostAcceptsAFrame() {
        create()
        for ((parent, item) in listOf(V.NAVIGATION_BAR to V.NAVIGATION_BAR_ITEM, V.NAVIGATION_RAIL to V.NAVIGATION_RAIL_ITEM)) {
            root(UiNode(1, parent, children = listOf(2, 3)),
                UiNode(2, item, props = mapOf(VP.SELECTED to UiValue.Bool(true)), modifier = listOf(tag("first")), slots = mapOf(S.ICON to 4, S.LABEL to 6)),
                UiNode(3, item, modifier = listOf(tag("second")), slots = mapOf(S.ICON to 5, S.LABEL to 7), callbacks = mapOf(E.CLICK to 10)),
                text(4, "A"), text(5, "B"), text(6, "First"), text(7, "Second"))
            events.clear()
            rule.onNodeWithTag("first").assertIsSelected()
            rule.onNodeWithTag("second").assertIsNotSelected().performClick().assertIsNotSelected()
            assertEquals(generation, expect(10).generation)
            patch(UiPatch.SetProps(2, mapOf(VP.SELECTED to UiValue.Bool(false))),
                UiPatch.SetProps(3, mapOf(VP.SELECTED to UiValue.Bool(true))))
            rule.onNodeWithTag("first").assertIsNotSelected()
            rule.onNodeWithTag("second").assertIsSelected()
        }
        root(UiNode(1, V.TAB_ROW, props = mapOf(VP.SELECTED_INDEX to UiValue.Num(0.0)), children = listOf(2, 3)),
            UiNode(2, V.TAB, modifier = listOf(tag("tab-a")), slots = mapOf(S.TEXT to 4)),
            UiNode(3, V.TAB, modifier = listOf(tag("tab-b")), slots = mapOf(S.TEXT to 5), callbacks = mapOf(E.CLICK to 11)),
            text(4, "Overview"), text(5, "Details"))
        rule.onNodeWithTag("tab-b").performClick().assertIsNotSelected()
        expect(11)
        patch(UiPatch.SetProps(1, mapOf(VP.SELECTED_INDEX to UiValue.Num(1.0), VP.SCROLLABLE to UiValue.Bool(true))))
        rule.onNodeWithTag("tab-b").assertIsSelected()
        noErrors()
    }

    @Test fun drawerBackAndDisabledItemsHonorControlledOpenState() {
        create()
        root(UiNode(1, V.NAVIGATION_DRAWER, props = mapOf(P.OPEN to UiValue.Bool(true)),
            modifier = listOf(ModifierOp(M.FILL_MAX_SIZE)), slots = mapOf(VS.DRAWER_CONTENT to 2, S.CONTENT to 8), callbacks = mapOf(VE.OPEN_CHANGE to 20)),
            UiNode(2, C.COLUMN, children = listOf(3, 4)),
            UiNode(3, V.NAVIGATION_DRAWER_ITEM, props = mapOf(VP.SELECTED to UiValue.Bool(true)), slots = mapOf(S.LABEL to 5),
                modifier = listOf(tag("drawer-active")), callbacks = mapOf(E.CLICK to 21)),
            UiNode(4, V.NAVIGATION_DRAWER_ITEM, props = mapOf(P.ENABLED to UiValue.Bool(false)), slots = mapOf(S.LABEL to 6),
                modifier = listOf(tag("drawer-disabled")), callbacks = mapOf(E.CLICK to 22)),
            text(5, "Inbox"), text(6, "Unavailable"), UiNode(8, C.COLUMN, children = listOf(9, 10)),
            text(9, "Main content"), UiNode(10, V.SEARCH_BAR, children = listOf(11)), text(11, "Drawer search results"))
        rule.onNodeWithTag("drawer-active").assertIsSelected().performClick()
        expect(21)
        rule.onNodeWithTag("drawer-disabled").assertIsNotEnabled()
        assertTrue(events.none { it.callbackId == 22 })
        back()
        expect(20) { !it.payload.getBoolean(VF.OPEN) }
        rule.onNodeWithText("Inbox").assertIsDisplayed()
        // Change body layout while the accepted close is still animating. Material anchor
        // corrections must not masquerade as a new user request to reopen the drawer.
        patch(UiPatch.SetProps(1, mapOf(P.OPEN to UiValue.Bool(false))), UiPatch.SetProps(10, mapOf(VP.EXPANDED to UiValue.Bool(true))))
        rule.waitUntil(5000) { rule.onNodeWithText("Drawer search results").isDisplayed() && !rule.onNodeWithText("Inbox").isDisplayed() }
        rule.onNodeWithText("Drawer search results").assertIsDisplayed()
        rule.onNodeWithText("Inbox").assertIsNotDisplayed()
        rule.onNodeWithText("Main content").assertIsDisplayed()
        assertTrue(events.none { it.callbackId == 20 && it.payload.getBoolean(VF.OPEN) })
        noErrors()
    }

    @Test fun chipsSegmentsBadgeAndFloatingButtonPreserveMaterialSelectionAndDisabledSemantics() {
        create()
        root(UiNode(1, C.COLUMN, children = listOf(2, 3, 4, 8, 10, 14)),
            UiNode(2, V.ASSIST_CHIP, slots = mapOf(S.LABEL to 5), modifier = listOf(tag("assist")), callbacks = mapOf(E.CLICK to 30)),
            UiNode(3, V.FILTER_CHIP, slots = mapOf(S.LABEL to 6), modifier = listOf(tag("filter")), callbacks = mapOf(VE.SELECTED_CHANGE to 31)),
            UiNode(4, V.INPUT_CHIP, slots = mapOf(S.LABEL to 7), modifier = listOf(tag("input")), callbacks = mapOf(VE.SELECTED_CHANGE to 32)),
            text(5, "Help"), text(6, "Filter"), text(7, "Recipient"),
            UiNode(8, V.BADGE, props = mapOf(P.TEXT to UiValue.Str("7")), slots = mapOf(S.CONTENT to 9)), text(9, "Messages"),
            UiNode(10, V.SEGMENTED_BUTTON, children = listOf(11, 12)),
            UiNode(11, V.SEGMENTED_BUTTON_ITEM, props = mapOf(VP.SELECTED to UiValue.Bool(true)), slots = mapOf(S.LABEL to 13), modifier = listOf(tag("segment-a"))),
            UiNode(12, V.SEGMENTED_BUTTON_ITEM, slots = mapOf(S.LABEL to 15), modifier = listOf(tag("segment-b")), callbacks = mapOf(VE.SELECTED_CHANGE to 33)),
            text(13, "Day"), UiNode(14, V.FLOATING_ACTION_BUTTON, children = listOf(16), modifier = listOf(tag("fab")), callbacks = mapOf(E.CLICK to 34)),
            text(15, "Week"), text(16, "+"))
        rule.onNodeWithTag("assist").performClick(); expect(30)
        rule.onNodeWithTag("filter").assertIsNotSelected().performClick().assertIsNotSelected()
        rule.onNodeWithTag("input").assertIsNotSelected().performClick().assertIsNotSelected()
        assertTrue(expect(31).payload.getBoolean(VF.SELECTED)); assertTrue(expect(32).payload.getBoolean(VF.SELECTED))
        rule.onNodeWithTag("segment-b").performClick().assertIsNotSelected()
        assertTrue(expect(33).payload.getBoolean(VF.SELECTED))
        rule.onNodeWithTag("fab").performClick(); expect(34)
        patch(UiPatch.SetProps(3, mapOf(VP.SELECTED to UiValue.Bool(true), P.ENABLED to UiValue.Bool(false))),
            UiPatch.SetProps(8, mapOf(P.TEXT to UiValue.Str("7"), VP.VISIBLE to UiValue.Bool(false))),
            UiPatch.SetProps(11, mapOf(VP.SELECTED to UiValue.Bool(false))), UiPatch.SetProps(12, mapOf(VP.SELECTED to UiValue.Bool(true))))
        rule.onNodeWithTag("filter").assertIsSelected().assertIsNotEnabled()
        rule.onNodeWithText("7").assertDoesNotExist(); rule.onNodeWithText("Messages").assertIsDisplayed()
        rule.onNodeWithTag("segment-b").assertIsSelected()
        patch(UiPatch.SetProps(10, mapOf(VP.MODE to UiValue.Enum("multi"))), UiPatch.SetProps(11, mapOf(VP.SELECTED to UiValue.Bool(true))))
        rule.onNodeWithTag("segment-a").assertIsOn(); rule.onNodeWithTag("segment-b").assertIsOn()
        noErrors()
    }

    @Test fun dropdownAnchorAndMenuRemainLiveUntilAcceptedDismissal() {
        create()
        root(UiNode(1, V.DROPDOWN_MENU, props = mapOf(P.OPEN to UiValue.Bool(false)), slots = mapOf(VS.ANCHOR to 2), children = listOf(4, 6, 7),
            callbacks = mapOf(E.DISMISS_REQUEST to 40)),
            UiNode(2, C.BUTTON, children = listOf(3), callbacks = mapOf(E.CLICK to 41)), text(3, "Open menu"),
            UiNode(4, V.DROPDOWN_MENU_ITEM, slots = mapOf(S.TEXT to 5), callbacks = mapOf(E.CLICK to 42)), text(5, "Save item"),
            UiNode(6, C.HORIZONTAL_DIVIDER), UiNode(7, V.DROPDOWN_MENU_ITEM, props = mapOf(P.ENABLED to UiValue.Bool(false)), slots = mapOf(S.TEXT to 8)), text(8, "Disabled item"))
        rule.onNodeWithText("Save item").assertDoesNotExist()
        rule.onNodeWithText("Open menu").performClick(); expect(41)
        patch(UiPatch.SetProps(1, mapOf(P.OPEN to UiValue.Bool(true))))
        rule.onNodeWithText("Save item").performClick(); expect(42)
        rule.onNodeWithText("Disabled item").assertIsNotEnabled()
        back(); expect(40)
        rule.onNodeWithText("Save item").assertIsDisplayed()
        patch(UiPatch.SetProps(1, mapOf(P.OPEN to UiValue.Bool(false))))
        // The plugin's independent Android clock owns Material's popup exit animation.
        rule.waitUntil(5000) { rule.onAllNodesWithText("Save item").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Save item").assertDoesNotExist()
        noErrors()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun datePickerUsesUtcSelectionAndControlledDisplayModeWithNativeInput() {
        create()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val original = format.parse("2026-10-07")!!.time
        val requested = format.parse("2026-10-14")!!.time
        val locale = rule.activity.resources.configuration.locales[0]
        val materialFormatter = DatePickerDefaults.dateFormatter()
        fun day(millis: Long) = rule.onNodeWithText(materialFormatter.formatDate(millis, locale, forContentDescription = true)!!, substring = true)
        fun properties(millis: Long?, mode: String = "picker") = mapOf(
            VP.SELECTED_DATE_MILLIS to (millis?.let { UiValue.Num(it.toDouble()) } ?: UiValue.Null),
            VP.YEAR_START to UiValue.Num(2026.0), VP.YEAR_END to UiValue.Num(2026.0), VP.DISPLAY_MODE to UiValue.Enum(mode))
        root(UiNode(1, V.DATE_PICKER, props = properties(original), modifier = listOf(tag("date")),
            callbacks = mapOf(VE.DATE_CHANGE to 50, VE.DISPLAY_MODE_CHANGE to 51)))
        day(requested).performClick()
        assertEquals(requested, expect(50).payload.getLong(VF.SELECTED_DATE_MILLIS))
        day(original).assertIsSelected(); day(requested).assertIsNotSelected()
        patch(UiPatch.SetProps(1, properties(requested)))
        day(requested).assertIsSelected()
        val switchLabel = rule.activity.getString(androidx.compose.material3.R.string.m3c_date_picker_switch_to_input_mode)
        rule.onNodeWithContentDescription(switchLabel).performClick()
        assertEquals("input", expect(51).payload.getString(VF.DISPLAY_MODE))
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
        patch(UiPatch.SetProps(1, properties(requested, "input")))
        rule.waitUntil(5000) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        events.clear()
        rule.onNode(hasSetTextAction()).performTextClearance()
        expect(50) { !it.payload.containsKey(VF.SELECTED_DATE_MILLIS) }
        patch(UiPatch.SetProps(1, properties(null, "input")))
        val otherMonth = format.parse("2026-11-23")!!.time
        patch(UiPatch.SetProps(1, properties(otherMonth, "input")))
        patch(UiPatch.SetProps(1, properties(otherMonth, "picker")))
        val otherMonthLabel = materialFormatter.formatMonthYear(otherMonth, locale)!!
        try { rule.waitUntil(5000) { rule.onAllNodesWithText(otherMonthLabel).fetchSemanticsNodes().isNotEmpty() } }
        catch (failure: ComposeTimeoutException) { throw AssertionError("Expected accepted month $otherMonthLabel\n" + rule.onRoot().printToString(), failure) }
        rule.onNodeWithText(otherMonthLabel).assertIsDisplayed()
        day(otherMonth).assertIsSelected()
        patch(UiPatch.SetProps(1, mapOf(VP.YEAR_START to UiValue.Num(1980.0), VP.YEAR_END to UiValue.Num(1980.0))))
        val limitedMonthLabel = materialFormatter.formatMonthYear(format.parse("1980-12-01")!!.time, locale)!!
        rule.onNodeWithText(limitedMonthLabel).assertIsDisplayed()
        noErrors()
    }

    @Test fun timePickerNativeInputAndDialUseControlledHourMinute() {
        create()
        fun properties(hour: Int, minute: Int, mode: String) = mapOf(
            VP.HOUR to UiValue.Num(hour.toDouble()), VP.MINUTE to UiValue.Num(minute.toDouble()),
            VP.IS_24_HOUR to UiValue.Bool(true), VP.DISPLAY_MODE to UiValue.Enum(mode))
        root(UiNode(1, V.TIME_PICKER, props = properties(7, 35, "input"), callbacks = mapOf(VE.TIME_CHANGE to 60)))
        val hourLabel = rule.activity.getString(androidx.compose.material3.R.string.m3c_time_picker_hour_text_field)
        val minuteLabel = rule.activity.getString(androidx.compose.material3.R.string.m3c_time_picker_minute_text_field)
        rule.onNodeWithContentDescription(hourLabel).performTextReplacement("09")
        expect(60) { it.payload.getInt(VF.HOUR) == 9 && it.payload.getInt(VF.MINUTE) == 35 }
        // The second edit happens before the script acknowledges the first field.
        val minuteSelector = rule.activity.getString(androidx.compose.material3.R.string.m3c_time_picker_minute_selection)
        rule.onNodeWithContentDescription(minuteSelector).performClick()
        rule.onNodeWithContentDescription(minuteLabel).performTextReplacement("45")
        expect(60) { it.payload.getInt(VF.HOUR) == 9 && it.payload.getInt(VF.MINUTE) == 45 }
        patch(UiPatch.SetProps(1, properties(9, 45, "picker")))
        rule.onNodeWithContentDescription(minuteSelector).assertTextContains("45")
        val thirtyMinutes = rule.activity.getString(androidx.compose.material3.R.string.m3c_time_picker_minute_suffix, 30)
        rule.onNodeWithContentDescription(thirtyMinutes).performClick()
        expect(60) { it.payload.getInt(VF.HOUR) == 9 && it.payload.getInt(VF.MINUTE) == 30 }
        rule.onNodeWithContentDescription(minuteSelector).assertTextContains("45")
        noErrors()
    }

    @Test fun gridRetainsKeysAndCommandsUseAcceptedItemCounts() {
        create()
        fun tree(count: Int) = UiTree(1, listOf(UiNode(1, V.LAZY_VERTICAL_GRID,
            props = mapOf(VP.COLUMNS to UiValue.Num(2.0), P.SPACING to UiValue.Dp(4.0), P.USER_SCROLL_ENABLED to UiValue.Bool(false)),
            modifier = listOf(tag("grid"), size(M.WIDTH, 240.0), size(M.HEIGHT, 150.0)), children = (0 until count).map { it + 2 },
            callbacks = mapOf(E.SCROLL to 70))) + (0 until count).map { index -> text(index + 2, "cell $index", "cell-$index", listOf(size(M.HEIGHT, 50.0))) })
        patch(UiPatch.SetRoot(tree(6)))
        rule.runOnIdle {
            renderer!!.apply(UiPatchBatch(83, ++generation, listOf(UiPatch.SetRoot(tree(100)))))
            renderer!!.execute(UiCommand.ScrollTo(1, index = 80, offset = 3))
        }
        expect(70) { it.generation == generation && it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 80 && it.payload.getInt(F.OFFSET) == 3 }
        rule.onNodeWithText("cell 80").assertIsDisplayed()
        patch(UiPatch.Move(1, 82, 84))
        rule.onNodeWithText("cell 80").assertIsDisplayed()
        rule.runOnIdle {
            renderer!!.apply(UiPatchBatch(83, ++generation, listOf(UiPatch.SetRoot(tree(8)))))
            failure { renderer!!.execute(UiCommand.ScrollTo(1, index = 80)) }
            renderer!!.execute(UiCommand.ScrollTo(1, index = 2, offset = 4))
        }
        expect(70) { it.generation == generation && it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 2 && it.payload.getInt(F.OFFSET) == 4 }
        noErrors()
    }

    @Test fun pagerUserGesturePageUpdatesAndSameTurnCommandsKeepRealPositions() {
        create()
        fun tree(count: Int, page: Int = 0) = UiTree(1, listOf(UiNode(1, V.HORIZONTAL_PAGER,
            props = mapOf(VP.PAGE to UiValue.Num(page.toDouble())),
            modifier = listOf(tag("pager"), size(M.WIDTH, 250.0), size(M.HEIGHT, 150.0)),
            children = (0 until count).map { it + 2 }, callbacks = mapOf(VE.PAGE_CHANGE to 80, E.SCROLL to 81))) +
            (0 until count).map { index -> text(index + 2, "page $index", "page-$index", listOf(ModifierOp(M.FILL_MAX_SIZE))) })
        patch(UiPatch.SetRoot(tree(4)))
        rule.runOnIdle { renderer!!.execute(UiCommand.ScrollTo(1, index = 2)) }
        expect(80) { it.payload.getInt(VF.PAGE) == 2 }
        rule.onNodeWithText("page 2").assertIsDisplayed()
        rule.onNodeWithTag("pager").performTouchInput { swipeLeft(durationMillis = 400) }
        expect(80) { it.payload.getInt(VF.PAGE) == 3 }
        patch(UiPatch.SetProps(1, mapOf(VP.PAGE to UiValue.Num(1.0))))
        rule.onNodeWithText("page 1").assertIsDisplayed()
        val pageWidth = rule.onNodeWithTag("pager").fetchSemanticsNode().boundsInRoot.width.roundToInt()
        events.clear()
        rule.runOnIdle { renderer!!.execute(UiCommand.ScrollTo(1, index = 2, offset = -(pageWidth * 3 / 4))) }
        expect(81) { it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 1 && it.payload.getInt(F.OFFSET) == pageWidth - pageWidth * 3 / 4 }
        rule.runOnIdle {
            renderer!!.apply(UiPatchBatch(83, ++generation, listOf(UiPatch.SetRoot(tree(12, 1)))))
            renderer!!.execute(UiCommand.ScrollTo(1, index = 9, offset = 7))
        }
        expect(81) { it.generation == generation && it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 9 && it.payload.getInt(F.OFFSET) == 7 }
        rule.onNodeWithText("page 9").assertIsDisplayed()
        patch(UiPatch.Move(1, 11, 10))
        rule.onNodeWithText("page 9").assertIsDisplayed()
        rule.runOnIdle {
            failure { renderer!!.execute(UiCommand.ScrollTo(1, index = 12)) }
            renderer!!.execute(UiCommand.ScrollTo(1, index = 0, offset = Int.MAX_VALUE))
        }
        rule.waitForIdle()
        noErrors()
    }

    @Test fun pagerPixelCommandsWaitForMeasureAndNewerRequestsSupersedeWaitingOnes() {
        create()
        root(UiNode(1, V.HORIZONTAL_PAGER, modifier = listOf(tag("measured-pager"), size(M.WIDTH, 0.0), size(M.HEIGHT, 100.0)),
            children = listOf(2, 3, 4, 5), callbacks = mapOf(E.SCROLL to 82)),
            text(2, "zero"), text(3, "one"), text(4, "two"), text(5, "three"))
        fun position() = rule.onNodeWithTag("measured-pager").fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value()
        val initialPosition = position()
        events.clear()
        rule.runOnIdle {
            renderer!!.execute(UiCommand.ScrollTo(1, index = 1, offset = 7))
            renderer!!.execute(UiCommand.ScrollTo(1, index = 2, offset = 9))
        }
        rule.waitForIdle()
        assertEquals("A pixel command must wait while page size is zero", initialPosition, position(), 0f)
        patch(UiPatch.SetProps(1, emptyMap(), modifier = listOf(tag("measured-pager"), size(M.WIDTH, 250.0), size(M.HEIGHT, 100.0))))
        expect(82) { it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 2 && it.payload.getInt(F.OFFSET) == 9 }
        assertTrue(events.none { it.callbackId == 82 && it.payload.getInt(F.FIRST_VISIBLE_INDEX) == 1 })
        noErrors()
    }

    @Test fun searchQueriesImeSearchAndExpandedResultsStayControlled() {
        create()
        root(UiNode(1, V.SEARCH_BAR, props = mapOf(VP.QUERY to UiValue.Str("old")), children = listOf(2),
            callbacks = mapOf(VE.QUERY_CHANGE to 90, VE.SEARCH to 91, VE.EXPANDED_CHANGE to 92)), text(2, "Search results"))
        rule.onNode(hasSetTextAction()).performTextReplacement("new")
        assertEquals("new", expect(90).payload.getString(F.TEXT))
        rule.onNode(hasSetTextAction()).assertTextEquals("old")
        patch(UiPatch.SetProps(1, mapOf(VP.QUERY to UiValue.Str("new"), VP.EXPANDED to UiValue.Bool(true))))
        // Visibility is driven by Material's real Android animation clock, not the rule's clock.
        rule.waitUntil(5000) { rule.onNodeWithText("Search results").isDisplayed() }
        rule.onNodeWithText("Search results").assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performImeAction()
        assertEquals("new", expect(91).payload.getString(F.TEXT))
        back()
        if (events.none { it.callbackId == 92 && !it.payload.getBoolean(VF.EXPANDED) }) back()
        expect(92) { !it.payload.getBoolean(VF.EXPANDED) }
        rule.onNodeWithText("Search results").assertIsDisplayed()
        patch(UiPatch.SetProps(1, mapOf(VP.QUERY to UiValue.Str("new"), VP.EXPANDED to UiValue.Bool(false), P.ENABLED to UiValue.Bool(false))))
        rule.onNodeWithText("new").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
        noErrors()
    }

    @Test fun searchNativeInputHonorsStringLimitAndRejectsDisabledStaleCallbacks() {
        create()
        root(UiNode(1, V.SEARCH_BAR, props = mapOf(VP.QUERY to UiValue.Str("accepted")),
            callbacks = mapOf(VE.QUERY_CHANGE to 140, VE.SEARCH to 141)))
        val limit = ComposeUiLimits.MAX_STRING_CHARS
        rule.onNode(hasSetTextAction()).performTextReplacement("x".repeat(limit))
        assertEquals(limit, expect(140).payload.getString(F.TEXT)!!.length)
        rule.onNode(hasSetTextAction()).assertTextEquals("accepted")
        noErrors()

        events.clear()
        rule.onNode(hasSetTextAction()).performTextReplacement("accepted")
        rule.waitForIdle()
        assertTrue("Reaffirming the controlled query must not publish a change", events.isEmpty())

        events.clear()
        rule.onNode(hasSetTextAction()).performTextReplacement("x".repeat(limit + 1))
        val error = expect(ComposeUiContract.SYSTEM_CALLBACK_ID) { it.type == E.ERROR }
        assertEquals(1, error.nodeId)
        assertEquals(generation, error.generation)
        assertEquals(ComposeUiErrorCodes.LIMIT_EXCEEDED, error.payload.getString(F.CODE))
        assertEquals(ComposeUiErrorCodes.LIMIT_EXCEEDED, error.payload.getString(F.MESSAGE))
        assertEquals(VP.QUERY, error.payload.getString(F.PROP))
        assertEquals(setOf(F.CODE, F.MESSAGE, F.PROP), error.payload.keySet())
        val rejectedInputEvents = events.toList()
        assertTrue("Unexpected callbacks after rejected input: " + rejectedInputEvents.take(8).joinToString { event ->
            val text = event.payload.getString(F.TEXT)
            "type=${event.type}, callback=${event.callbackId}, generation=${event.generation}, length=${text?.length}, equalsAccepted=${text == "accepted"}"
        }, rejectedInputEvents.none { it.callbackId == 140 || it.callbackId == 141 })
        rule.onNode(hasSetTextAction()).assertTextEquals("accepted")

        val oldSetText = rule.onNode(hasSetTextAction()).fetchSemanticsNode().config[SemanticsActions.SetText].action!!
        patch(UiPatch.SetProps(1, mapOf(VP.QUERY to UiValue.Str("accepted"), P.ENABLED to UiValue.Bool(false))))
        events.clear()
        // Simulate an action retained by an accessibility client across a disabled frame.
        rule.runOnIdle { oldSetText(AnnotatedString("late native edit")) }
        rule.waitForIdle()
        assertTrue(events.isEmpty())
        rule.onNodeWithText("accepted").assertIsNotEnabled()

        patch(UiPatch.SetProps(1, mapOf(VP.QUERY to UiValue.Str("accepted"))))
        rule.onNode(hasSetTextAction()).performTextReplacement("usable")
        assertEquals("usable", expect(140).payload.getString(F.TEXT))
        patch(UiPatch.SetProps(1, mapOf(VP.QUERY to UiValue.Str("usable"))))
        rule.onNode(hasSetTextAction()).assertTextEquals("usable").performImeAction()
        assertEquals("usable", expect(141).payload.getString(F.TEXT))
        noErrors()
    }

    @Test fun tooltipLongPressAndPopupDismissalRequestHostVisibilityUpdates() {
        create()
        root(UiNode(1, V.TOOLTIP, slots = mapOf(VS.TOOLTIP to 2, S.CONTENT to 3), callbacks = mapOf(VE.OPEN_CHANGE to 100)),
            text(2, "Helpful detail"), UiNode(3, C.BUTTON, children = listOf(4), modifier = listOf(tag("tooltip-anchor"))), text(4, "Help"))
        // This renderer has its own Android frame clock; synthetic event time is not a timeout.
        rule.onNodeWithTag("tooltip-anchor").performTouchInput { down(center) }
        try { expect(100) { it.payload.getBoolean(VF.OPEN) } }
        finally { rule.onNodeWithTag("tooltip-anchor").performTouchInput { up() } }
        rule.onNodeWithText("Helpful detail").assertDoesNotExist()
        patch(UiPatch.SetProps(1, mapOf(P.OPEN to UiValue.Bool(true))))
        rule.onNodeWithText("Helpful detail").assertIsDisplayed()
        back(); expect(100) { !it.payload.getBoolean(VF.OPEN) }
        rule.onNodeWithText("Helpful detail").assertIsDisplayed()
        patch(UiPatch.SetProps(1, mapOf(P.OPEN to UiValue.Bool(false))))
        rule.waitUntil(5000) { rule.onAllNodesWithText("Helpful detail").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Helpful detail").assertDoesNotExist()
        noErrors()
    }

    @Test fun pullToRefreshUsesActualNestedScrollAndHonorsDisabledState() {
        create()
        root(UiNode(1, V.PULL_TO_REFRESH, children = listOf(2), modifier = listOf(tag("refresh"), size(M.WIDTH, 280.0), size(M.HEIGHT, 350.0)),
            callbacks = mapOf(VE.REFRESH to 110)),
            UiNode(2, C.LAZY_COLUMN, modifier = listOf(ModifierOp(M.FILL_MAX_SIZE)), children = (3..22).toList()),
            *(3..22).map { text(it, "row $it", modifier = listOf(size(M.HEIGHT, 48.0))) }.toTypedArray())
        fun pull() { rule.onNodeWithTag("refresh").performTouchInput { swipe(Offset(centerX, top + 10f), Offset(centerX, bottom - 10f), durationMillis = 700) } }
        pull(); expect(110)
        patch(UiPatch.SetProps(1, mapOf(VP.REFRESHING to UiValue.Bool(true))))
        val count = events.count { it.callbackId == 110 }
        pull(); assertEquals(count, events.count { it.callbackId == 110 })
        patch(UiPatch.SetProps(1, mapOf(VP.REFRESHING to UiValue.Bool(false), P.ENABLED to UiValue.Bool(false))))
        pull(); assertEquals(count, events.count { it.callbackId == 110 })
        noErrors()
    }

    @Test fun bottomSheetCancellationIsControlledAndCancelableFalseBlocksBack() {
        create()
        root(UiNode(1, V.MODAL_BOTTOM_SHEET, props = mapOf(P.OPEN to UiValue.Bool(true)),
            modifier = listOf(tag("sheet"), size(M.HEIGHT, 160.0)), children = listOf(2), callbacks = mapOf(E.DISMISS_REQUEST to 120)), text(2, "Modal sheet content"))
        rule.onNodeWithText("Modal sheet content").assertIsDisplayed()
        back(); expect(120)
        rule.onNodeWithText("Modal sheet content").assertIsDisplayed()
        patch(UiPatch.SetProps(1, mapOf(P.OPEN to UiValue.Bool(true), VP.CANCELABLE to UiValue.Bool(false))))
        val count = events.count { it.callbackId == 120 }
        back(); assertEquals(count, events.count { it.callbackId == 120 })
        rule.onNodeWithText("Modal sheet content").assertIsDisplayed()
        patch(UiPatch.SetProps(1, mapOf(P.OPEN to UiValue.Bool(false))))
        rule.onNodeWithText("Modal sheet content").assertDoesNotExist()
        patch(UiPatch.SetProps(1, mapOf(P.OPEN to UiValue.Bool(true))))
        rule.onNodeWithText("Modal sheet content").assertIsDisplayed()
        noErrors()
    }

    @Test fun chipAndSearchBarSupportKeyboardFocusAndBlurCommands() {
        create()
        root(UiNode(1, C.COLUMN, children = listOf(2, 3, 4)),
            UiNode(2, C.TEXT, props = mapOf(P.TEXT to UiValue.Str("Focus start")), modifier = listOf(tag("focus-start")), callbacks = mapOf(E.CLICK to 1)),
            UiNode(3, V.ASSIST_CHIP, slots = mapOf(S.LABEL to 5), modifier = listOf(tag("focus-chip")), callbacks = mapOf(E.FOCUS_CHANGE to 130)),
            UiNode(4, V.SEARCH_BAR, modifier = listOf(tag("focus-search")), callbacks = mapOf(E.FOCUS_CHANGE to 131, E.CLICK to 132)), text(5, "Focus chip"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val previousMode = rule.runOnIdle { rule.activity.window.decorView.isInTouchMode }
        try {
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
            rule.onNodeWithTag("focus-start").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            events.clear()
            rule.runOnIdle { renderer!!.execute(UiCommand.Focus(3)) }
            expect(130) { it.payload.getBoolean(F.FOCUSED) }
            rule.runOnIdle { renderer!!.execute(UiCommand.Blur(3)) }
            expect(130) { !it.payload.getBoolean(F.FOCUSED) }
            rule.runOnIdle { renderer!!.execute(UiCommand.Focus(4)) }
            expect(131) { it.payload.getBoolean(F.FOCUSED) }
            rule.onNode(hasSetTextAction()).assertIsFocused()
            rule.runOnIdle { renderer!!.execute(UiCommand.Blur(4)) }
            expect(131) { !it.payload.getBoolean(F.FOCUSED) }
            noErrors()
        } finally { instrumentation.setInTouchMode(previousMode) }
    }

    private fun failure(action: () -> Unit) {
        try { action(); fail("Expected INVALID_ARGUMENT") }
        catch (failure: ComposeUiContractException) { assertEquals(ComposeUiErrorCodes.INVALID_ARGUMENT, failure.code) }
    }
}
