package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

/**
 * What a preview host does beyond contract v2 (roadmap P11 / D55 / D61): a v3 host announces these through
 * `getPreviewInfo()`, and each one removes the preview differences it closes. Hosts before v3 announce none.
 */
enum class PreviewCapability { DIALOGS_IN_CANVAS, LIFECYCLE_EVENTS, BUSY_WAIT_LIMIT, WINDOW_FUNCTIONS_VISIBLE }

/**
 * A way in which running a `"ui"` mode script in the designer's preview differs from running it in AutoJs6
 * (roadmap P11.2 / D55; the rows of section 3.5 of the P10 roadmap). [reducedBy] is the host capability that
 * closes the difference, or null when no planned host change does.
 */
enum class PreviewDifferenceKind(val reducedBy: PreviewCapability?) {
    /** `activity.*`: the preview has no Activity window (row "activity"). */
    ACTIVITY(null),
    /** `ui.emitter` listening to `resume`, `pause` or `back_pressed` (row "lifecycle events"). */
    LIFECYCLE(PreviewCapability.LIFECYCLE_EVENTS),
    /** Any other use of `ui.emitter`: the other Activity events (row "lifecycle events"). */
    ACTIVITY_EVENTS(null),
    /** `ui.statusBarColor`, `ui.navigationBarColor`, `ui.backgroundColor` (row "window and screen"). */
    WINDOW_COLOR(PreviewCapability.WINDOW_FUNCTIONS_VISIBLE),
    /** `ui.finish`, `ui.keepScreenOn` and the bar icon functions (row "activity"). */
    WINDOW_NO_OP(null),
    /** `dialogs.*` and the global `alert`, `confirm`, `prompt`, `rawInput` (row "dialogs"). */
    DIALOGS(PreviewCapability.DIALOGS_IN_CANVAS),
    /** An endless loop on the main thread (row "endless loops and the main thread"). */
    BUSY_WAIT(PreviewCapability.BUSY_WAIT_LIMIT),
    /** `threads.*` and `events.*`, which run for real (row "side effects"). */
    THREADS(null),
}

/** One line of a script that behaves differently in the preview; [api] is what the line uses, as written. */
data class PreviewDifference(val kind: PreviewDifferenceKind, val line: Int, val column: Int, val api: String)

object PreviewDifferences {
    /**
     * The docs site section that lists the differences: the `composeDesigner` page, heading "Preview Differences"
     * after its Chinese title; the id is the Docsify slug of that heading, percent-encoded.
     */
    const val DOCS_URL = "https://docs.autojs6.com/#/composeDesigner?id=%E9%A2%84%E8%A7%88%E5%B7%AE%E5%BC%82-preview-differences"

    /** [differences] without those the host closes with one of its [capabilities]. */
    fun visible(differences: List<PreviewDifference>, capabilities: Set<PreviewCapability>): List<PreviewDifference> =
        differences.filter { difference -> difference.kind.reducedBy.let { it == null || it !in capabilities } }
}
