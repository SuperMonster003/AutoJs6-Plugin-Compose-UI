package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.model.UiNode

/** Plugin-private seam. Its complete signature deliberately references only frozen V1 types. */
internal interface RendererExtension {
    val catalog: ComponentCatalog
    fun handles(type: String): Boolean
    @Composable fun Content(node: UiNode, modifier: Modifier)
    fun dispose()
}
