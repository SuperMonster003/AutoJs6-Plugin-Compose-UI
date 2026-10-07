package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.junit.Assert.*
import org.junit.Test

class WideStateTest {
    @Test fun timeEditsKeepUnacknowledgedOtherFieldThroughOlderAcknowledgements() {
        val state = PendingTimeSelection(10, 0)
        assertEquals(PendingTimeSelection.Value(11, 0), state.hour(11))
        assertEquals(PendingTimeSelection.Value(11, 30), state.minute(30))
        state.accept(11, 0)
        assertEquals(PendingTimeSelection.Value(12, 30), state.hour(12))
        state.accept(11, 30)
        assertEquals(PendingTimeSelection.Value(12, 40), state.minute(40))
        state.accept(12, 40)
        assertNull(state.minute(40))
    }

    @Test fun programmaticTimeReplacementSupersedesThePendingDraft() {
        val state = PendingTimeSelection(10, 0)
        state.hour(11); state.minute(30)
        state.accept(9, 45)
        assertEquals(PendingTimeSelection.Value(9, 50), state.minute(50))
        assertNull(state.minute(50))
    }

    @Test fun pagerPixelsNormalizeNegativeAndLargeOffsetsWithoutOverflow() {
        assertEquals(1 to 0.25f, pagerPixelRequest(2, -75, 100))
        assertEquals(0 to 0f, pagerPixelRequest(0, -75, 100))
        assertEquals(0 to 0f, pagerPixelRequest(4, Int.MIN_VALUE, 100))
        assertEquals(3 to -0.25f, pagerPixelRequest(2, 75, 100))
        assertEquals(2 to 0.07f, pagerPixelRequest(2, 7, 100))
        assertEquals(Int.MAX_VALUE to 0f, pagerPixelRequest(1, Int.MAX_VALUE, 1))
        assertEquals(3 to 0f, pagerPixelRequest(3, null, 0))
        assertEquals(2 to 0.25f, pagerPixelRequest(null, 225, 100))
    }
}
