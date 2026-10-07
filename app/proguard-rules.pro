# PluginInfo (common-plugin-api) is annotated with @Parcelize; the annotation is source-retained and absent at run time.
-dontwarn kotlinx.parcelize.Parcelize

# Kotlin is bundled for the standalone INFO process, but the renderer shares the host copy.
# Renaming/rewriting its ABI breaks parent-first identity (P0.2: CoroutineContext ClassCastException).
-keep class kotlin.** { *; }
# Even with keep, R8 8.13 synthesizes specialized methods on kept Kotlin facades (e.g.
# CollectionsKt.f(Iterable)), which do not exist in the parent APK. P0 retains shrinking and
# obfuscation, but disables optimization across this duplicated runtime boundary. See spike evidence.
-dontoptimize

# compileOnly lifecycle-viewmodel does not contribute its consumer rules. Its host-side
# NewInstanceFactory reflects plugin-owned Compose ViewModels after the view is attached.
-keepclassmembers,allowobfuscation class * extends androidx.lifecycle.ViewModel {
    public <init>();
}

# Avoid short obfuscated names colliding with a separately minified host APK.
-repackageclasses io.github.supermonster003.autojs6.plugin.compose.ui.internal

# Host discovery entry points are looked up by class name from the manifest.
-keep class io.github.supermonster003.autojs6.plugin.compose.ui.ComposeUiPluginInfoService { *; }
-keep class io.github.supermonster003.autojs6.plugin.compose.ui.WakeActivity { *; }

# The host instantiates the renderer factory named by the org.autojs.plugin.compose.RENDERER_FACTORY meta-data
# through Class.forName on the plugin class loader and casts it to the contract interface (roadmap D10 / D22).
-keep class io.github.supermonster003.autojs6.plugin.compose.ui.renderer.ComposeUiRendererFactoryImpl { *; }
-keep class io.github.supermonster003.autojs6.plugin.compose.ui.renderer.interop.AndroidViewRendererFactoryImpl { *; }
-keep class io.github.supermonster003.autojs6.plugin.compose.ui.renderer.dialog.DialogRendererFactoryImpl { *; }
-keep class * implements org.autojs.plugin.compose.api.loading.ComposeUiRendererFactory { *; }
-keep class * implements org.autojs.plugin.compose.api.loading.ComposeUiRenderer { *; }

# Host contract AARs: parcelables and AIDL stubs are resolved reflectively across processes; the Compose UI
# contract types are provided by the host at run time and must keep their names on the plugin side as well.
-keep class org.autojs.plugin.common.api.** { *; }
-keep class org.autojs.plugin.compose.api.** { *; }
