package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiErrorCodes
import org.autojs.plugin.compose.api.ComposeUiEventFields
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.model.TextSelection
import org.autojs.plugin.compose.api.model.UiCommand
import org.junit.Assert.*
import org.junit.Test

class TextFieldRevisionTest {
    @Test fun nativeChangesWinBeforeNotificationAndFutureEditsCannotSkipArbitration() {
        val revision = TextFieldRevision(7, EditorSnapshot("", 0, 0))
        revision.observe(EditorSnapshot("\u4e2d", 1, 1, 0, 1))
        for (sequence in listOf(0L, 2L)) {
            val error = rejected { revision.resolve(UiCommand.Edit(7, "old", editSeq = sequence)) }
            assertEquals(ComposeUiErrorCodes.INVALID_ARGUMENT, error.code)
            assertEquals(7, error.nodeId)
            assertEquals(ComposeUiEventFields.EDIT_SEQ, error.prop)
        }
        assertEquals("\u4e2d", revision.value.text)
        assertEquals("\u4e2d\u6587", revision.resolve(UiCommand.Edit(7, "\u4e2d\u6587", editSeq = 1)).text)
    }

    @Test fun compositionAndSelectionChangesInvalidateEarlierSequenceWithoutRewritingText() {
        val revision = TextFieldRevision(1, EditorSnapshot("\u4e2d\u6587", 2, 2, 0, 2))
        assertTrue(revision.observe(EditorSnapshot("\u4e2d\u6587", 2, 2)))
        assertTrue(revision.observe(EditorSnapshot("\u4e2d\u6587", 2, 0)))
        assertFalse(revision.observe(EditorSnapshot("\u4e2d\u6587", 2, 0)))
        assertEquals(2L, revision.sequence)
        assertEquals(2, revision.value.start)
        assertEquals(0, revision.value.end)
    }

    @Test fun unicodeUsesUtf16AndReversedSelectionsSurviveEdits() {
        val text = "A\uD83D\uDE03\u4e2d\u6587"
        val revision = TextFieldRevision(1, EditorSnapshot(text, 0, 0))
        val selected = revision.resolve(UiCommand.Edit(1, selection = TextSelection(5, 1), editSeq = 0))
        assertEquals(text, selected.text)
        assertEquals(5, selected.start); assertEquals(1, selected.end)
        revision.observe(selected)
        val replacement = revision.resolve(UiCommand.Edit(1, "\uD83D\uDE03", editSeq = 1))
        assertEquals(2, replacement.start); assertEquals(2, replacement.end)
        val untouched = revision.value
        assertEquals("selection", rejected { revision.resolve(UiCommand.Edit(1, selection = TextSelection(6, 0), editSeq = 1)) }.prop)
        assertSame(untouched, revision.value)
    }

    @Test fun nativeLimitFailureLeavesRevisionAndAcceptedValueIntact() {
        val allowed = "x".repeat(ComposeUiLimits.MAX_STRING_CHARS)
        val revision = TextFieldRevision(1, EditorSnapshot(allowed, allowed.length, allowed.length))
        val initial = revision.value
        val error = rejected { revision.observe(EditorSnapshot(allowed + "x", allowed.length + 1, allowed.length + 1)) }
        assertEquals(ComposeUiErrorCodes.LIMIT_EXCEEDED, error.code)
        assertSame(initial, revision.value)
        assertEquals(0L, revision.sequence)
    }

    @Test fun aFrameKeepsLatestRevisionPerNodeAndRemovedNodesAreDiscarded() {
        val pending = EditorFrameChanges()
        val revision = TextFieldRevision(1, EditorSnapshot("", 0, 0))
        for (text in listOf("a", "ab", "a", "", "\uD83D\uDE03")) {
            if (revision.observe(EditorSnapshot(text, text.length, text.length))) pending.mark(1)
        }
        pending.mark(2); pending.mark(2); pending.mark(3)
        pending.retain(setOf(1, 2))
        assertEquals(listOf(1, 2), pending.drain())
        assertEquals("\uD83D\uDE03", revision.value.text)
        assertEquals(5L, revision.sequence)
        assertTrue(pending.isEmpty)
        assertTrue(pending.drain().isEmpty())
    }

    @Test fun diagnosticsDoNotContainEditorText() {
        assertFalse(EditorSnapshot("private field text", 1, 1).toString().contains("private field text"))
    }

    @Test fun replacingAnEditorRemovesItsPendingNotificationEvenWhenItsIdIsReused() {
        val pending = EditorFrameChanges()
        pending.mark(1); pending.mark(2)
        pending.remove(1)
        pending.retain(setOf(1, 2))
        assertEquals(listOf(2), pending.drain())
        pending.mark(1)
        assertEquals(listOf(1), pending.drain())
    }

    private fun rejected(block: () -> Unit): ComposeUiContractException {
        try { block(); fail("Expected contract rejection") } catch (error: ComposeUiContractException) { return error }
        throw AssertionError()
    }
}
