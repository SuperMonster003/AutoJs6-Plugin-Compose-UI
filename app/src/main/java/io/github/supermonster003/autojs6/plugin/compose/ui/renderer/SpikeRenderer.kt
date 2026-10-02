package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.autojs.plugin.compose.api.spike.ComposeUiContractException
import org.autojs.plugin.compose.api.spike.ComposeUiEventSink
import org.autojs.plugin.compose.api.spike.ComposeUiRenderer
import org.autojs.plugin.compose.api.spike.SpikeContract
import org.autojs.plugin.compose.api.spike.UiEvent
import org.autojs.plugin.compose.api.spike.UiNode

/** Minimal replacement-tree renderer used only to establish the P0 loading boundary. */
internal class SpikeRenderer(private val context: Context, private val sink: ComposeUiEventSink) : ComposeUiRenderer {
    private val root = mutableStateOf(UiNode(SpikeContract.COLUMN))
    private var composeView: ComposeView? = null
    private var closed = false
    private var compositions = 0
    private var disposals = 0
    private var scope: CoroutineScope? = null
    private var recomposer: Recomposer? = null

    override fun view(): View {
        requireUsable()
        return composeView ?: ComposeView(context).also { view ->
            val handler = CoroutineExceptionHandler { _, _ ->
                // Do not log text, exception messages or caller trees.
                sink.enqueue(UiEvent(SpikeContract.ERROR, code = SpikeContract.RENDER_FAILED))
            }
            val ownerScope = CoroutineScope(AndroidUiDispatcher.Main + SupervisorJob() + handler).also { scope = it }
            val ownerRecomposer = Recomposer(ownerScope.coroutineContext).also { recomposer = it }
            ownerScope.launch { ownerRecomposer.runRecomposeAndApplyChanges() }
            view.setParentCompositionContext(ownerRecomposer)
            view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            view.setContent {
                DisposableEffect(Unit) {
                    compositions++
                    onDispose { disposals++ }
                }
                MaterialTheme { Render(root.value) }
            }
            composeView = view
        }
    }

    override fun apply(root: UiNode) {
        requireUsable()
        this.root.value = SpikeTree.snapshot(root)
    }

    override fun dispose() {
        requireMain()
        if (closed) return
        closed = true
        composeView?.disposeComposition()
        recomposer?.cancel()
        scope?.cancel()
        composeView = null
        root.value = UiNode(SpikeContract.COLUMN)
    }

    override fun diagnostics() = Bundle().apply {
        requireMain()
        putBoolean("closed", closed)
        putBoolean("hasComposition", composeView?.hasComposition == true)
        putInt("compositions", compositions)
        putInt("disposals", disposals)
        putBoolean("recomposerShutdown", recomposer?.currentState?.value == Recomposer.State.ShutDown)
    }

    private fun requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw ComposeUiContractException(SpikeContract.WRONG_THREAD)
    }

    private fun requireUsable() {
        requireMain()
        if (closed) throw ComposeUiContractException(SpikeContract.CLOSED)
    }

    @Composable
    private fun Render(node: UiNode) {
        when (node.type) {
            SpikeContract.COLUMN -> Column(Modifier.padding(16.dp)) { node.children.forEach { Render(it) } }
            SpikeContract.TEXT -> Text(node.text)
            SpikeContract.BUTTON -> {
                Button(
                    onClick = { if (!closed) sink.enqueue(UiEvent(SpikeContract.CLICK, node.callbackId)) },
                ) { Text(node.text) }
            }
        }
    }
}
