package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.collect

/**
 * Main-thread interaction source for native fields. A restored focus can be emitted during attach,
 * before Material's decoration collectors start. Replay only that still-active focus on subscription
 * so label, placeholder and indicator state agree with the editor; never replay a completed press.
 */
internal class TextFieldInteractionSource : MutableInteractionSource {
    private val events = MutableSharedFlow<Interaction>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var focus: FocusInteraction.Focus? = null
    override val interactions = flow {
        var deliveredFocus: FocusInteraction.Focus? = null
        events.onSubscription { focus?.let { emit(it) } }.collect { interaction ->
            // A collector may suspend while receiving the replay, with the same live Focus already
            // queued behind it. Material tracks a list, so duplicate Focus needs two Unfocus events.
            if (interaction is FocusInteraction.Focus) {
                if (deliveredFocus === interaction) return@collect
                deliveredFocus = interaction
            } else if (interaction is FocusInteraction.Unfocus && deliveredFocus === interaction.focus) {
                deliveredFocus = null
            }
            emit(interaction)
        }
    }

    override suspend fun emit(interaction: Interaction) {
        updateFocus(interaction)
        events.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        updateFocus(interaction)
        return events.tryEmit(interaction)
    }

    private fun updateFocus(interaction: Interaction) {
        when (interaction) {
            is FocusInteraction.Focus -> focus = interaction
            is FocusInteraction.Unfocus -> if (focus === interaction.focus) focus = null
        }
    }
}
