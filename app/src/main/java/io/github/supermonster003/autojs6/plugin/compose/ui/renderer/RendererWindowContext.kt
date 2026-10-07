package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/** Popup subpanels keep the enclosing session's application/overlay type for nested modal nodes. */
internal val LocalRendererWindowType = staticCompositionLocalOf<Int?> { null }

@Composable internal fun RendererWindowContext(content: @Composable () -> Unit) {
    val parameters = LocalView.current.rootView.layoutParams as? WindowManager.LayoutParams
    val type = parameters?.type?.takeIf { it >= WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW }
        ?: WindowManager.LayoutParams.TYPE_APPLICATION
    CompositionLocalProvider(LocalRendererWindowType provides type, content = content)
}
