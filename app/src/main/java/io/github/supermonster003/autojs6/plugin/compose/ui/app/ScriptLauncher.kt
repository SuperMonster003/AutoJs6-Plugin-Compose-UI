package io.github.supermonster003.autojs6.plugin.compose.ui.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import io.github.supermonster003.autojs6.plugin.compose.ui.ComposeUiPlugin

/** Hands a gallery script to the clipboard or to the installed AutoJs6 host. Nothing runs inside the plugin. */
internal object ScriptLauncher {
    /** The host's public run entry: an exported Activity that accepts the script text as an extra. */
    const val HOST_RUN_ACTIVITY = "org.autojs.autojs.external.open.RunIntentActivity"
    const val EXTRA_SCRIPT = "script"

    enum class Result { STARTED, HOST_MISSING, FAILED }

    fun copy(context: Context, label: String, script: String): Boolean = runCatching {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText(label, script))
        true
    }.getOrDefault(false)

    fun intent(script: String): Intent = Intent()
        .setClassName(ComposeUiPlugin.HOST_PACKAGE_NAME, HOST_RUN_ACTIVITY)
        .putExtra(EXTRA_SCRIPT, script)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun isHostInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(ComposeUiPlugin.HOST_PACKAGE_NAME, 0); true
    }.getOrDefault(false)

    fun run(context: Context, script: String): Result {
        if (!isHostInstalled(context)) return Result.HOST_MISSING
        return try {
            context.startActivity(intent(script))
            Result.STARTED
        } catch (_: ActivityNotFoundException) {
            Result.HOST_MISSING
        } catch (_: SecurityException) {
            Result.FAILED
        } catch (_: IllegalStateException) {
            Result.FAILED
        }
    }
}
