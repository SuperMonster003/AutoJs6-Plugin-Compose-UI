package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
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

/** Real native editor/semantics tests; host tests cover installed IMEs and Activity resize behavior. */
class TextFieldRendererTest {
    @get:Rule val rule = createAndroidComposeRule<RendererTestActivity>()
    private var renderer: ComposeUiRenderer? = null
    private var generation = 0L
    private val events = ConcurrentLinkedQueue<UiEvent>()

    @After fun dispose() { rule.runOnUiThread { renderer?.dispose(); renderer = null } }

    private fun create(mount: Boolean = true) {
        rule.runOnUiThread {
            renderer = ComposeUiRendererFactoryImpl().create(object : ComposeUiHostEnvironment {
                override val hostContext = rule.activity
                override val mainExecutor = Executor { action -> if (Looper.myLooper() == Looper.getMainLooper()) action.run() else rule.activity.runOnUiThread(action) }
                override val eventSink = ComposeUiEventSink(events::add)
                override val initialTheme = ThemeSpec(dark = false)
                override val sessionId = 9
            })
            if (mount) rule.activity.setContentView(renderer!!.view())
        }
        if (mount) rule.waitForIdle()
    }

    private fun tag(name: String) = ModifierOp(M.TEST_TAG, listOf(UiValue.Str(name)))
    private fun field(
        type: String = C.TEXT_FIELD,
        props: Map<String, UiValue> = emptyMap(),
        slots: Map<String, Int> = emptyMap(),
        callback: Int = 71,
    ) = UiNode(1, type, props = props, modifier = listOf(tag("editor")), slots = slots, callbacks = mapOf(E.VALUE_CHANGE to callback))

    private fun apply(vararg nodes: UiNode) {
        rule.runOnUiThread { renderer!!.apply(UiPatchBatch(9, ++generation, listOf(UiPatch.SetRoot(UiTree(nodes[0].nodeId, nodes.toList()))))) }
        rule.waitForIdle()
    }

    private fun editor() = rule.onNodeWithTag("editor")
    private fun setTextAction(): (AnnotatedString) -> Boolean = editor().fetchSemanticsNode().config[SemanticsActions.SetText].action!!
    private fun setSelectionAction(): (Int, Int, Boolean) -> Boolean = editor().fetchSemanticsNode().config[SemanticsActions.SetSelection].action!!
    private fun awaitValue(text: String): UiEvent {
        rule.waitUntil(5000) { events.any { it.type == E.VALUE_CHANGE && it.payload.getString(F.TEXT) == text } }
        return events.last { it.type == E.VALUE_CHANGE && it.payload.getString(F.TEXT) == text }
    }

    private fun rejected(code: String, prop: String? = null, block: () -> Unit) {
        try { block(); fail("Expected $code") }
        catch (failure: ComposeUiContractException) {
            assertEquals(code, failure.code)
            if (prop != null) assertEquals(prop, failure.prop)
        }
    }

    @Test fun bothFieldsRenderAllFiveSlotsAndNativeTextWithoutScriptAcknowledgement() {
        create()
        val slotNames = listOf(S.LABEL, S.PLACEHOLDER, S.LEADING_ICON, S.TRAILING_ICON, S.SUPPORTING_TEXT)
        for (type in listOf(C.TEXT_FIELD, C.OUTLINED_TEXT_FIELD)) {
            val slots = slotNames.mapIndexed { index, name -> name to index + 2 }.toMap()
            val labels = slots.map { (name, id) -> UiNode(id, C.TEXT, props = mapOf(P.TEXT to UiValue.Str(name)), modifier = listOf(tag(name))) }
            apply(field(type, slots = slots), *labels.toTypedArray())
            assertEquals("$type should start empty", "", editor().fetchSemanticsNode().config[SemanticsProperties.InputText].text)
            editor().performClick()
            editor().assertIsFocused()
            rule.waitUntil(5000) { rule.onAllNodesWithTag(S.PLACEHOLDER, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            slotNames.forEach { rule.onNodeWithTag(it, useUnmergedTree = true).assertExists() }
            val action = setTextAction()
            rule.runOnUiThread { assertTrue(action(AnnotatedString("\u4e2d\u6587\uD83D\uDE03"))) }
            editor().assertTextContains("\u4e2d\u6587\uD83D\uDE03")
            awaitValue("\u4e2d\u6587\uD83D\uDE03")
        }
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun editBeforeViewAndFailedTextPatchKeepNativeStateAndPreviousGeneration() {
        create(mount = false)
        rule.runOnUiThread {
            renderer!!.apply(UiPatchBatch(9, ++generation, listOf(UiPatch.SetRoot(UiTree(1, listOf(field(props = mapOf(P.TEXT to UiValue.Str("initial")))))))))
            renderer!!.execute(UiCommand.Edit(1, "programmatic", selection = TextSelection(7, 2), editSeq = 0))
            rejected(ComposeUiErrorCodes.INVALID_ARGUMENT, F.EDIT_SEQ) { renderer!!.execute(UiCommand.Edit(1, "stale", editSeq = 0)) }
            rejected(ComposeUiErrorCodes.INVALID_ARGUMENT, F.EDIT_SEQ) { renderer!!.execute(UiCommand.Edit(1, "future", editSeq = 100)) }
            rejected(ComposeUiErrorCodes.INVALID_ARGUMENT, P.TEXT) {
                renderer!!.apply(UiPatchBatch(9, generation + 1, listOf(UiPatch.SetProps(1, mapOf(P.TEXT to UiValue.Str("illegal"))))))
            }
            rule.activity.setContentView(renderer!!.view())
        }
        editor().assertTextEquals("programmatic")
        assertEquals(TextRange(7, 2), editor().fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        val value = awaitValue("programmatic")
        assertEquals(1L, value.generation)
        rule.runOnUiThread {
            renderer!!.apply(UiPatchBatch(9, ++generation, listOf(UiPatch.SetProps(1, mapOf(P.TEXT to UiValue.Str("initial"), P.IS_ERROR to UiValue.Bool(true))))))
        }
        editor().assertTextEquals("programmatic")
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun sameFrameNativeEditsCoalesceAndPendingNotificationUsesCommittedCallbackGeneration() {
        create(); apply(field())
        val action = setTextAction()
        rule.runOnUiThread {
            assertTrue(action(AnnotatedString("a")))
            assertTrue(action(AnnotatedString("ab")))
            assertTrue(action(AnnotatedString("\u4e2d\u6587")))
            rejected(ComposeUiErrorCodes.INVALID_ARGUMENT, F.EDIT_SEQ) { renderer!!.execute(UiCommand.Edit(1, "old", editSeq = 0)) }
            renderer!!.apply(UiPatchBatch(9, ++generation, listOf(UiPatch.SetProps(1, mapOf(P.IS_ERROR to UiValue.Bool(true)), callbacks = mapOf(E.VALUE_CHANGE to 88)))))
        }
        val value = awaitValue("\u4e2d\u6587")
        rule.waitForIdle()
        assertEquals(1, events.count { it.type == E.VALUE_CHANGE })
        assertEquals(88, value.callbackId); assertEquals(2L, value.generation)
        assertTrue(value.payload.getLong(F.EDIT_SEQ) >= 3)
        editor().assertTextEquals("\u4e2d\u6587")
    }

    @Test fun readOnlyStillAllowsSelectionAndProgrammaticEditsWhileDisabledRejectsSelection() {
        create()
        val props = mapOf(P.TEXT to UiValue.Str("seed"), P.READ_ONLY to UiValue.Bool(true))
        apply(field(props = props))
        val select = setSelectionAction()
        rule.runOnUiThread { assertTrue(select(4, 1, true)) }
        val selection = awaitValue("seed")
        assertEquals(TextRange(4, 1), editor().fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        assertFalse(editor().fetchSemanticsNode().config[SemanticsProperties.IsEditable])
        rule.runOnUiThread { renderer!!.execute(UiCommand.Edit(1, "server", editSeq = selection.payload.getLong(F.EDIT_SEQ))) }
        editor().assertTextEquals("server")
        awaitValue("server")
        rule.runOnUiThread { renderer!!.apply(UiPatchBatch(9, ++generation, listOf(UiPatch.SetProps(1, props + (P.ENABLED to UiValue.Bool(false)))))) }
        editor().assertIsNotEnabled()
        val disabledSelect = setSelectionAction()
        rule.runOnUiThread { assertFalse(disabledSelect(0, 0, true)) }
        editor().assertTextEquals("server")
    }

    @Test fun passwordMasksBothSemanticsValuesKeepsUtf16SelectionAndBlocksCopyCut() {
        create()
        val original = "abc\uD83D\uDE03"
        apply(field(C.OUTLINED_TEXT_FIELD, mapOf(P.TEXT to UiValue.Str(original), P.VISUAL_TRANSFORMATION to UiValue.Enum("password"),
            P.KEYBOARD_TYPE to UiValue.Enum("password"), P.MIN_LINES to UiValue.Num(2.0), P.READ_ONLY to UiValue.Bool(true))))
        var config = editor().fetchSemanticsNode().config
        assertTrue(config.contains(SemanticsProperties.Password))
        assertEquals("\u2022".repeat(original.length), config[SemanticsProperties.InputText].text)
        assertEquals("\u2022".repeat(original.length), config[SemanticsProperties.EditableText].text)
        rule.runOnUiThread { renderer!!.execute(UiCommand.Edit(1, selection = TextSelection(5, 1), editSeq = 0)) }
        config = editor().fetchSemanticsNode().config
        assertEquals(TextRange(5, 1), config[SemanticsProperties.TextSelectionRange])
        val copy = config[SemanticsActions.CopyText].action!!
        val cut = config[SemanticsActions.CutText].action!!
        rule.runOnUiThread { assertFalse(copy()); assertFalse(cut()) }
        val value = awaitValue(original)
        assertEquals(5, value.payload.getInt(F.SELECTION_START))
        assertEquals(1, value.payload.getInt(F.SELECTION_END))
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun nativeInputLimitIsTypedAndDisposalCancelsPendingNotifications() {
        create(); apply(field(props = mapOf(P.TEXT to UiValue.Str("accepted"), P.SINGLE_LINE to UiValue.Bool(true))))
        val action = setTextAction()
        rule.runOnUiThread { action(AnnotatedString("x".repeat(ComposeUiLimits.MAX_STRING_CHARS + 1))) }
        rule.waitUntil(5000) { events.any { it.type == E.ERROR } }
        editor().assertTextEquals("accepted")
        val error = events.single { it.type == E.ERROR }
        assertEquals(ComposeUiErrorCodes.LIMIT_EXCEEDED, error.payload.getString(F.CODE))
        assertEquals(P.TEXT, error.payload.getString(F.PROP)); assertEquals(1, error.nodeId)
        events.clear()
        rule.runOnUiThread { action(AnnotatedString("pending")); renderer!!.dispose() }
        rule.waitForIdle()
        assertTrue(events.isEmpty())
    }

    @Test fun replacingFieldKindWithTheSameIdDiscardsOldPendingValuesAndErrors() {
        create(); apply(field())
        val action = setTextAction()
        rule.runOnUiThread {
            action(AnnotatedString("pending"))
            action(AnnotatedString("x".repeat(ComposeUiLimits.MAX_STRING_CHARS + 1)))
            renderer!!.apply(UiPatchBatch(9, ++generation, listOf(UiPatch.SetRoot(UiTree(1, listOf(field(C.OUTLINED_TEXT_FIELD)))))))
        }
        editor().assertTextEquals("")
        rule.waitForIdle()
        assertTrue(events.none { it.type == E.VALUE_CHANGE || it.type == E.ERROR })
    }

    @Test fun commonClickAndLongClickCallbacksPreserveNativeFocusImeAndWordSelection() {
        create()
        apply(UiNode(1, C.TEXT_FIELD, props = mapOf(P.TEXT to UiValue.Str("select native text")), modifier = listOf(tag("editor")),
            callbacks = mapOf(E.CLICK to 81, E.LONG_CLICK to 82, E.FOCUS_CHANGE to 83, E.VALUE_CHANGE to 71)))
        editor().performTouchInput { click(center) }
        rule.waitUntil(5000) { events.any { it.callbackId == 81 } && events.any { it.callbackId == 83 && it.payload.getBoolean(F.FOCUSED) } }
        editor().assertIsFocused()
        val connection = inputConnection()
        rule.runOnUiThread { connection.setSelection(0, 0) }
        rule.waitForIdle()
        events.clear()
        editor().performTouchInput { down(center) }
        rule.waitUntil(5000) { events.any { it.callbackId == 82 } }
        editor().performTouchInput { up() }
        rule.waitUntil(5000) { !editor().fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange].collapsed }
        assertEquals(1, events.count { it.callbackId == 82 })
        assertEquals(0, events.count { it.callbackId == 81 })
        // Accessibility actions also notify the callback while retaining Foundation's action.
        editor().performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        rule.waitUntil(5000) { events.any { it.callbackId == 81 } }
        editor().assertIsFocused()
        rule.runOnUiThread { connection.closeConnection() }
        assertTrue(events.none { it.type == E.ERROR })
    }

    @Test fun inputConnectionCompositionDeletionPasteAndCursorUseNativeStateAndRejectRacingEdits() {
        create(); apply(field())
        editor().performClick()
        val connection = inputConnection()
        rule.runOnUiThread {
            assertTrue(connection.setComposingText("ni", 1))
            assertTrue(connection.setComposingText("\u4f60", 1))
            assertTrue(connection.finishComposingText())
            assertTrue(connection.commitText("\u597d", 1))
            rejected(ComposeUiErrorCodes.INVALID_ARGUMENT, F.EDIT_SEQ) { renderer!!.execute(UiCommand.Edit(1, "stale", editSeq = 0)) }
        }
        val value = awaitValue("\u4f60\u597d")
        editor().assertTextEquals("\u4f60\u597d")
        rule.runOnUiThread {
            assertTrue(connection.setComposingRegion(0, 1))
            // Composition alone changed; snapshotFlow has not yet had an opportunity to run.
            rejected(ComposeUiErrorCodes.INVALID_ARGUMENT, F.EDIT_SEQ) {
                renderer!!.execute(UiCommand.Edit(1, "old composition", editSeq = value.payload.getLong(F.EDIT_SEQ)))
            }
            assertTrue(connection.finishComposingText())
            assertTrue(connection.setSelection(2, 2))
            assertTrue(connection.deleteSurroundingText(1, 0))
            assertTrue(connection.deleteSurroundingText(1, 0))
            assertTrue(connection.commitText("\uD83D\uDE03\u4e2d", 1))
            assertTrue(connection.setSelection(2, 2))
            assertTrue(connection.deleteSurroundingTextInCodePoints(1, 0))
        }
        awaitValue("\u4e2d")
        editor().assertTextEquals("\u4e2d")

        lateinit var clipboard: ClipboardManager
        var previous: ClipData? = null
        rule.runOnUiThread {
            clipboard = rule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            previous = clipboard.primaryClip
            clipboard.setPrimaryClip(ClipData.newPlainText("Compose editor test", "paste\u4e2d\u6587\uD83D\uDE03"))
            connection.setSelection(0, 1)
        }
        try {
            editor().performSemanticsAction(SemanticsActions.PasteText) { assertTrue(it()) }
            awaitValue("paste\u4e2d\u6587\uD83D\uDE03")
            editor().assertTextEquals("paste\u4e2d\u6587\uD83D\uDE03")
        } finally {
            rule.runOnUiThread {
                previous?.let(clipboard::setPrimaryClip) ?: if (Build.VERSION.SDK_INT >= 28) clipboard.clearPrimaryClip()
                else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                connection.closeConnection()
            }
        }
        assertTrue(events.none { it.type == E.ERROR })
    }

    private fun inputConnection(): InputConnection {
        var connection: InputConnection? = null
        rule.waitUntil(5000) {
            rule.runOnUiThread {
                val focused = rule.activity.window.decorView.findFocus()
                connection = focused?.onCreateInputConnection(EditorInfo()) ?: findConnection(renderer!!.view())
            }
            connection != null
        }
        return requireNotNull(connection)
    }

    private fun findConnection(view: View): InputConnection? {
        if (view.onCheckIsTextEditor()) view.onCreateInputConnection(EditorInfo())?.let { return it }
        if (view is ViewGroup) for (index in 0 until view.childCount) findConnection(view.getChildAt(index))?.let { return it }
        return null
    }
}
