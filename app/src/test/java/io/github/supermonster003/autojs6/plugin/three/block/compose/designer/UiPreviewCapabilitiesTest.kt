package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.app.InteractivePreviewSettings
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewCapability
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.UiPreviewClient
import org.autojs.plugin.ui.preview.UiPreviewContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P11.6: what a v4 host announces becomes the designer's [PreviewCapability] set (each flag on its own, absent
 * flags off), and the settings page's busy-wait choices are whole seconds the host accepts, with the default among them.
 */
class UiPreviewCapabilitiesTest {
    private fun capabilitiesOf(vararg flags: String) = UiPreviewClient.capabilitiesOf { key, fallback -> if (key in flags) true else fallback }

    @Test
    fun `every v4 flag maps to its capability`() {
        assertEquals(emptySet<PreviewCapability>(), capabilitiesOf())
        assertEquals(
            PreviewCapability.entries.toSet(),
            capabilitiesOf(
                UiPreviewContract.KEY_DIALOGS_IN_CANVAS, UiPreviewContract.KEY_LIFECYCLE_EVENTS,
                UiPreviewContract.KEY_BUSY_WAIT_LIMIT_SUPPORTED, UiPreviewContract.KEY_WINDOW_FUNCTIONS_VISIBLE,
            ),
        )
        assertEquals(setOf(PreviewCapability.DIALOGS_IN_CANVAS), capabilitiesOf(UiPreviewContract.KEY_DIALOGS_IN_CANVAS))
        assertEquals(setOf(PreviewCapability.LIFECYCLE_EVENTS), capabilitiesOf(UiPreviewContract.KEY_LIFECYCLE_EVENTS))
        assertEquals(setOf(PreviewCapability.BUSY_WAIT_LIMIT), capabilitiesOf(UiPreviewContract.KEY_BUSY_WAIT_LIMIT_SUPPORTED))
        assertEquals(setOf(PreviewCapability.WINDOW_FUNCTIONS_VISIBLE), capabilitiesOf(UiPreviewContract.KEY_WINDOW_FUNCTIONS_VISIBLE))
        // Keys of other versions announce nothing.
        assertEquals(emptySet<PreviewCapability>(), capabilitiesOf(UiPreviewContract.KEY_SCRIPT_RUN_SUPPORTED, UiPreviewContract.KEY_RESOURCES_SUPPORTED))
    }

    @Test
    fun `busy wait choices are whole seconds within the hosts limits`() {
        val choices = InteractivePreviewSettings.BUSY_WAIT_LIMIT_CHOICES
        assertEquals(choices.sorted().distinct(), choices)
        assertTrue(InteractivePreviewSettings.DEFAULT_BUSY_WAIT_LIMIT_SECONDS in choices)
        assertEquals(UiPreviewContract.DEFAULT_BUSY_WAIT_LIMIT_MS, UiPreviewClient.busyWaitLimitOf(InteractivePreviewSettings.DEFAULT_BUSY_WAIT_LIMIT_SECONDS))
        assertEquals(UiPreviewContract.MIN_BUSY_WAIT_LIMIT_MS, UiPreviewClient.busyWaitLimitOf(choices.first()))
        assertEquals(UiPreviewContract.MAX_BUSY_WAIT_LIMIT_MS, UiPreviewClient.busyWaitLimitOf(choices.last()))
        for (seconds in choices) assertEquals(seconds * 1_000L, UiPreviewClient.busyWaitLimitOf(seconds))
        // Out of range values are clamped to what the host accepts.
        assertEquals(UiPreviewContract.MIN_BUSY_WAIT_LIMIT_MS, UiPreviewClient.busyWaitLimitOf(0))
        assertEquals(UiPreviewContract.MAX_BUSY_WAIT_LIMIT_MS, UiPreviewClient.busyWaitLimitOf(600))
    }
}
