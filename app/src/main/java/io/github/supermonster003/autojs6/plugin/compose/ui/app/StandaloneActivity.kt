package io.github.supermonster003.autojs6.plugin.compose.ui.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.lang.ref.WeakReference

/**
 * Base of the plugin's own screens (roadmap F.5). They run in the plugin process without any host class
 * loader; the appearance (language, dark mode, theme color) follows AutoJs6 through the official settings
 * Provider unless overridden in this plugin's settings. Only presentation refreshes; no renderer or script
 * state lives here.
 */
internal abstract class StandaloneActivity : ComponentActivity() {
    internal var appearance: HostAppearance by mutableStateOf(HostAppearance("en", false, AppearancePreferences.DEFAULT_COLOR, AppearancePreferences.DEFAULT_COLOR))
        private set
    private var refreshGeneration = 0

    override fun attachBaseContext(newBase: Context) {
        appearance = AppearancePreferences.resolve(newBase, HostAppearanceReader.cached())
        super.attachBaseContext(appearance.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyWindowAppearance()
        super.onCreate(savedInstanceState)
        setContent {
            val palette = StandalonePalette.resolve(appearance)
            MaterialTheme(colorScheme = palette.colorScheme()) { Content(palette) }
        }
    }

    @Composable
    protected abstract fun Content(palette: StandalonePalette)

    override fun onResume() {
        super.onResume()
        applyAppearance(AppearancePreferences.resolve(this, HostAppearanceReader.cached()))
        val expected = ++refreshGeneration
        val owner = WeakReference(this)
        val application = applicationContext
        HostAppearanceReader.worker.execute {
            val snapshot = HostAppearanceReader.read(application)
            main.post {
                owner.get()?.takeIf { !it.isFinishing && !it.isDestroyed && it.refreshGeneration == expected }?.let { activity ->
                    HostAppearanceReader.accept(snapshot)
                    activity.applyAppearance(AppearancePreferences.resolve(application, snapshot))
                }
            }
        }
    }

    override fun onPause() {
        refreshGeneration++
        super.onPause()
    }

    override fun onDestroy() {
        refreshGeneration++
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyAppearance(AppearancePreferences.resolve(this, HostAppearanceReader.cached()), configurationChanged = true)
    }

    /** Called by the settings screen after a confirmed change; the current page redraws in place. */
    internal fun refreshAppearance() = applyAppearance(AppearancePreferences.resolve(this, HostAppearanceReader.cached()))

    @Suppress("DEPRECATION")
    private fun applyAppearance(next: HostAppearance, configurationChanged: Boolean = false) {
        if (!configurationChanged && appearance == next) return
        appearance = next
        // attachBaseContext created a private configuration context; the application and system resources
        // remain untouched. Resource strings and layout direction follow the same override as the composition.
        resources.updateConfiguration(next.configure(resources.configuration), resources.displayMetrics)
        applyWindowAppearance()
    }

    private fun applyWindowAppearance() {
        val palette = StandalonePalette.resolve(appearance)
        val style = if (appearance.dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(palette.background))
    }

    private companion object {
        val main = Handler(Looper.getMainLooper())
    }
}
