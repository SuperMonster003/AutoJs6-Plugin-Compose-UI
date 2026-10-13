package io.github.supermonster003.autojs6.plugin.three.block.compose.renderer

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Roadmap P8.3: the designer's canvas wraps every rendered node's modifier (for example with
 * `onGloballyPositioned`) to learn its bounds for selection and drop targets. The host never provides a
 * decorator, so the renderer's behavior inside AutoJs6 is unchanged.
 */
internal val LocalNodeDecorator = staticCompositionLocalOf<((nodeId: Int, modifier: Modifier) -> Modifier)?> { null }
