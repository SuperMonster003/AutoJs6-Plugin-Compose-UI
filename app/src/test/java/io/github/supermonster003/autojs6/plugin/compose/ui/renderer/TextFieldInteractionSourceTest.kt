package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class TextFieldInteractionSourceTest {
    @Test fun focusRestoredBeforeMaterialSubscribesIsObservedByEveryDecoration() = runBlocking {
        val source = TextFieldInteractionSource()
        val focus = FocusInteraction.Focus()
        source.emit(focus)
        repeat(3) { assertSame(focus, withTimeout(1000) { source.interactions.first() }) }
    }

    @Test fun liveCollectorsKeepTheOriginalOrderWithoutDuplicateFocusEvents() = runBlocking {
        val source = TextFieldInteractionSource()
        val focus = FocusInteraction.Focus()
        source.tryEmit(focus)
        val received = ArrayList<Interaction>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) { source.interactions.take(5).toList(received) }
        val press = PressInteraction.Press(Offset.Zero)
        val release = PressInteraction.Release(press)
        val unfocus = FocusInteraction.Unfocus(focus)
        val next = FocusInteraction.Focus()
        source.tryEmit(press); source.tryEmit(release); source.tryEmit(unfocus); source.tryEmit(next)
        withTimeout(1000) { collector.join() }
        assertEquals(listOf(focus, press, release, unfocus, next), received)
    }

    @Test fun releasedFocusAndCompletedPressAreNotReplayedToNewCollectors() = runBlocking {
        val source = TextFieldInteractionSource()
        val focus = FocusInteraction.Focus()
        val oldPress = PressInteraction.Press(Offset.Zero)
        source.tryEmit(focus); source.tryEmit(oldPress); source.tryEmit(PressInteraction.Release(oldPress))
        source.tryEmit(FocusInteraction.Unfocus(focus))
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { source.interactions.first() }
        val newPress = PressInteraction.Press(Offset.Zero)
        source.tryEmit(newPress)
        assertSame(newPress, withTimeout(1000) { waiting.await() })
    }

    @Test fun focusReplayedWhileLiveDeliveryIsQueuedNeedsOnlyOneUnfocus() = runBlocking {
        val source = TextFieldInteractionSource()
        val focus = FocusInteraction.Focus()
        source.tryEmit(focus)
        val resume = CompletableDeferred<Unit>()
        val received = ArrayList<Interaction>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            source.interactions.take(2).collect {
                received += it
                if (received.size == 1) resume.await()
            }
        }
        source.tryEmit(focus)
        val unfocus = FocusInteraction.Unfocus(focus)
        source.tryEmit(unfocus)
        resume.complete(Unit)
        withTimeout(1000) { collector.join() }
        assertEquals(listOf(focus, unfocus), received)
    }
}
