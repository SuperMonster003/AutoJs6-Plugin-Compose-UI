# PluginInfo (common-plugin-api) is annotated with @Parcelize; the annotation is source-retained and absent at run time.
-dontwarn kotlinx.parcelize.Parcelize

# Host discovery entry points are looked up by class name from the manifest.
-keep class io.github.supermonster003.autojs6.plugin.compose.ui.ComposeUiPluginInfoService { *; }
-keep class io.github.supermonster003.autojs6.plugin.compose.ui.WakeActivity { *; }

# The host instantiates the renderer factory named by the org.autojs.plugin.compose.RENDERER_FACTORY meta-data
# through Class.forName on the plugin class loader and casts it to the contract interface (roadmap D10 / D22).
-keep class io.github.supermonster003.autojs6.plugin.compose.ui.renderer.ComposeUiRendererFactoryImpl { *; }
-keep class * implements org.autojs.plugin.compose.api.loading.ComposeUiRendererFactory { *; }
-keep class * implements org.autojs.plugin.compose.api.loading.ComposeUiRenderer { *; }

# Host contract AARs: parcelables and AIDL stubs are resolved reflectively across processes; the Compose UI
# contract types are provided by the host at run time and must keep their names on the plugin side as well.
-keep class org.autojs.plugin.common.api.** { *; }
-keep class org.autojs.plugin.compose.api.** { *; }
