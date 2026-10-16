package io.github.supermonster003.autojs6.plugin.three.block.compose.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import io.github.supermonster003.autojs6.plugin.three.block.compose.ThreeBlockComposePlugin

/**
 * Hands a script to the clipboard or to the installed AutoJs6 host. Nothing runs inside the plugin. Roadmap
 * D39 adds the designer's document destinations: open in the host editor and import into the host script
 * directory, both through a Uri the host already accepts on its public `external.open` entries.
 */
internal object ScriptLauncher {
    /** The host's public run entry: an exported Activity that accepts the script text as an extra. */
    const val HOST_RUN_ACTIVITY = "org.autojs.autojs.external.open.RunIntentActivity"
    /** The host's public editor entry: EDIT / VIEW with a content or file Uri; it keeps the Uri grants it is given. */
    const val HOST_EDIT_ACTIVITY = "org.autojs.autojs.external.open.EditIntentActivity"
    /** The host's public import entry: copies the document into the host script directory under its display name. */
    const val HOST_IMPORT_ACTIVITY = "org.autojs.autojs.external.open.ImportIntentActivity"
    /** The host's entries for project zips (roadmap P11.3 / D64); a host without them gets no folder project destinations. */
    const val HOST_RUN_PROJECT_ACTIVITY = "org.autojs.autojs.external.open.RunProjectIntentActivity"
    const val HOST_IMPORT_PROJECT_ACTIVITY = "org.autojs.autojs.external.open.ImportProjectIntentActivity"
    const val ZIP_MIME_TYPE = "application/zip"
    const val EXTRA_SCRIPT = "script"
    const val SCRIPT_MIME_TYPE = "application/x-javascript"
    /** The type the host's explorer gives TypeScript sources; TSX projects are handed over with it (roadmap P9.2). */
    const val TYPESCRIPT_MIME_TYPE = "text/typescript"
    /**
     * The type a TSX export is created with: document providers keep the requested name for it, while a script type
     * would make some of them append `.js` to `name.tsx`.
     */
    const val TSX_EXPORT_MIME_TYPE = "application/octet-stream"
    /** Suffix of the designer FileProvider authority (`<applicationId>.designer`, Manifest). */
    const val PROVIDER_AUTHORITY_SUFFIX = ".designer"

    enum class Result { STARTED, HOST_MISSING, FAILED }

    fun copy(context: Context, label: String, script: String): Boolean = runCatching {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText(label, script))
        true
    }.getOrDefault(false)

    fun intent(script: String): Intent = Intent()
        .setClassName(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, HOST_RUN_ACTIVITY)
        .putExtra(EXTRA_SCRIPT, script)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun isHostInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, 0); true
    }.getOrDefault(false)

    fun run(context: Context, script: String): Result = start(context, intent(script))

    /**
     * Run a document through the host's public run entry by Uri (roadmap P9.2): the host names the source after the
     * Uri's display name, so a `.tsx` project is compiled by the TypeScript Engine before it runs.
     */
    fun runIntent(uri: Uri): Intent = Intent(Intent.ACTION_VIEW)
        .setClassName(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, HOST_RUN_ACTIVITY)
        .setDataAndType(uri, TYPESCRIPT_MIME_TYPE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

    fun providerAuthority(context: Context): String = context.packageName + PROVIDER_AUTHORITY_SUFFIX

    /** The content Uri of a designer project file (inside `files/designer/projects/`) for a per-Intent grant. */
    fun projectUri(context: Context, file: File): Uri = FileProvider.getUriForFile(context, providerAuthority(context), file)

    /** Open the document in the host editor; a writable grant lets the host save back into the same file. */
    fun editIntent(uri: Uri, writable: Boolean): Intent = Intent(Intent.ACTION_EDIT)
        .setClassName(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, HOST_EDIT_ACTIVITY)
        .setDataAndType(uri, SCRIPT_MIME_TYPE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .also { if (writable) it.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }

    /** Import the document into the host script directory; the host names the copy after the Uri's display name. */
    fun importIntent(uri: Uri): Intent = Intent(Intent.ACTION_VIEW)
        .setClassName(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, HOST_IMPORT_ACTIVITY)
        .setDataAndType(uri, SCRIPT_MIME_TYPE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

    /** Run a folder project in the host from its zip (roadmap P11.3 / D64): unpacked into the host's cache and run there. */
    fun runProjectIntent(uri: Uri): Intent = Intent(Intent.ACTION_VIEW)
        .setClassName(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, HOST_RUN_PROJECT_ACTIVITY)
        .setDataAndType(uri, ZIP_MIME_TYPE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

    /** Import a folder project into the host script directory from its zip; the host names the folder after the zip. */
    fun importProjectIntent(uri: Uri): Intent = Intent(Intent.ACTION_VIEW)
        .setClassName(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, HOST_IMPORT_PROJECT_ACTIVITY)
        .setDataAndType(uri, ZIP_MIME_TYPE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

    /** Whether the installed host takes project zips (both entries of D64 exist). */
    fun hostTakesProjects(context: Context): Boolean = runCatching {
        for (activity in listOf(HOST_RUN_PROJECT_ACTIVITY, HOST_IMPORT_PROJECT_ACTIVITY)) {
            context.packageManager.getActivityInfo(ComponentName(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, activity), 0)
        }
        true
    }.getOrDefault(false)

    fun start(context: Context, intent: Intent): Result {
        if (!isHostInstalled(context)) return Result.HOST_MISSING
        return try {
            context.startActivity(intent)
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
