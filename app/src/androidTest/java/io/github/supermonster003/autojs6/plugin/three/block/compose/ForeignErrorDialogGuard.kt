package io.github.supermonster003.autojs6.plugin.three.block.compose

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.app.ActivityManager.ProcessErrorStateInfo
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description
import org.junit.runner.notification.RunListener

/**
 * Keeps every device test isolated from error dialogs that other processes leave on the screen.
 *
 * A busy CI emulator can show an "isn't responding" or "has stopped" dialog for a system app (launcher, setup
 * wizard, ...). Such a dialog sits above every application window and keeps input focus until it is answered, so
 * for the rest of the run focus-dependent tests time out and Instrumentation key injection fails with
 * "Injecting to another application requires INJECT_EVENTS permission". Registered as the runner's `listener`
 * (app/build.gradle.kts), this guard answers such a dialog before each test: "Wait" for an ANR (the process keeps
 * running) and close for a crash. Dialogs of the plugin's own processes (app and test APK) are only logged, so the
 * affected test still fails visibly. Nothing is connected in the normal case; UiAutomation is attached only to
 * answer a dialog.
 *
 * The error list comes from ActivityManager.getProcessesInErrorState(). It includes other apps up to API 30; from
 * API 31 it only reports the caller's own processes, where this guard therefore never touches a foreign dialog.
 */
class ForeignErrorDialogGuard : RunListener() {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val packageName get() = instrumentation.targetContext.packageName
    /** An error that could not be answered in time is not retried, so it costs at most one wait per run. */
    private val unanswerable = HashSet<Pair<Int, String?>>()

    override fun testStarted(description: Description) {
        // A listener failure would be reported against the run, so the guard never throws.
        try {
            answerForeignErrorDialogs(description)
        } catch (failure: Throwable) {
            Log.w(TAG, "Could not check for error dialogs before ${description.displayName}", failure)
        }
    }

    private fun answerForeignErrorDialogs(description: Description) {
        val initial = errors()
        initial.filter(::isOwn).forEach {
            Log.w(TAG, "${it.label()} of the plugin's own process ${it.processName} is pending before ${description.displayName}")
        }
        val foreign = initial.filterNot { isOwn(it) || it.key() in unanswerable }
        if (foreign.isEmpty()) return
        foreign.forEach { Log.w(TAG, "${it.label()} of ${it.processName} holds the screen before ${description.displayName}; answering it") }
        val deadline = SystemClock.uptimeMillis() + ANSWER_TIMEOUT_MS
        var remaining = foreign
        while (remaining.isNotEmpty() && SystemClock.uptimeMillis() < deadline) {
            if (remaining.any { it.condition == ProcessErrorStateInfo.NOT_RESPONDING }) clickEverywhere(ANR_WAIT_BUTTON)
            if (remaining.any { it.condition == ProcessErrorStateInfo.CRASHED }) closeCrashDialogs()
            SystemClock.sleep(POLL_INTERVAL_MS)
            remaining = errors().filterNot { isOwn(it) || it.key() in unanswerable }
        }
        if (remaining.isEmpty()) {
            Log.i(TAG, "Answered ${foreign.joinToString { it.processName }} before ${description.displayName}")
        } else {
            Log.w(TAG, "Still pending before ${description.displayName}: ${remaining.joinToString { "${it.label()} of ${it.processName}" }}")
            remaining.mapTo(unanswerable) { it.key() }
        }
    }

    private fun errors(): List<ProcessErrorStateInfo> =
        instrumentation.targetContext.getSystemService(ActivityManager::class.java)?.processesInErrorState.orEmpty()

    /** The plugin process and the test APK process (androidx.test's helper activities) share the package prefix. */
    private fun isOwn(info: ProcessErrorStateInfo): Boolean {
        val name = info.processName ?: return false
        return name == packageName || name.startsWith("$packageName:") || name.startsWith("$packageName.")
    }

    private fun clickEverywhere(viewId: String) {
        val automation = instrumentation.uiAutomation
        val serviceInfo = automation.serviceInfo
        if (serviceInfo != null && serviceInfo.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = serviceInfo
        }
        val roots = automation.windows.mapNotNull { it.root } + listOfNotNull(automation.rootInActiveWindow)
        roots.flatMap { it.findAccessibilityNodeInfosByViewId(viewId).orEmpty() }
            .forEach { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
    }

    /** The crash dialog cancels itself (as "close") on ACTION_CLOSE_SYSTEM_DIALOGS; the shell may always send it. */
    private fun closeCrashDialogs() {
        val output = instrumentation.uiAutomation.executeShellCommand("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }

    private fun ProcessErrorStateInfo.key() = pid to processName

    private fun ProcessErrorStateInfo.label() = if (condition == ProcessErrorStateInfo.NOT_RESPONDING) "ANR dialog" else "Crash dialog"

    private companion object {
        const val TAG = "ForeignErrorDialogGuard"
        const val ANR_WAIT_BUTTON = "android:id/aerr_wait"
        const val ANSWER_TIMEOUT_MS = 10_000L
        const val POLL_INTERVAL_MS = 500L
    }
}
