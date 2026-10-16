package io.github.supermonster003.autojs6.plugin.three.block.compose.app

import android.content.Context

/**
 * Settings of the designer's interactive preview (roadmap P11.6), kept in the designer's private preferences: how long
 * a script run in the preview may keep AutoJs6's main thread busy before the host stops it (preview contract v4).
 */
internal object InteractivePreviewSettings {
    /** The designer's preferences file, shared with its per-project confirmations. */
    const val PREFERENCES = "designer"
    private const val BUSY_WAIT_LIMIT_KEY = "ui-busy-wait-limit-seconds"

    const val DEFAULT_BUSY_WAIT_LIMIT_SECONDS = 5

    /** The choices of the settings page, in whole seconds within the host's 1 - 60 s. */
    val BUSY_WAIT_LIMIT_CHOICES = listOf(1, 2, 3, 5, 10, 15, 30, 60)

    /** The saved limit in seconds, the default when nothing (or no longer a choice) is saved. */
    fun busyWaitLimitSeconds(context: Context): Int =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getInt(BUSY_WAIT_LIMIT_KEY, DEFAULT_BUSY_WAIT_LIMIT_SECONDS)
            .takeIf { it in BUSY_WAIT_LIMIT_CHOICES } ?: DEFAULT_BUSY_WAIT_LIMIT_SECONDS

    /** Saves [seconds] (one of [BUSY_WAIT_LIMIT_CHOICES]); false when it could not be written. */
    fun setBusyWaitLimitSeconds(context: Context, seconds: Int): Boolean {
        require(seconds in BUSY_WAIT_LIMIT_CHOICES) { "busy-wait limit $seconds s is not a choice" }
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putInt(BUSY_WAIT_LIMIT_KEY, seconds).commit()
    }
}
