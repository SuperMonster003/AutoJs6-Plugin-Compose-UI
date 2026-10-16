package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.os.RemoteException
import android.util.Log
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.supermonster003.autojs6.plugin.three.block.compose.ThreeBlockComposePlugin
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.PreviewCapability
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.JsonText
import org.autojs.plugin.ui.preview.IUiPreviewCallback
import org.autojs.plugin.ui.preview.IUiPreviewService
import org.autojs.plugin.ui.preview.UiPreviewContract
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import org.autojs.plugin.ui.preview.UiPreviewContract as Contract

/**
 * Client of the host's ui-mode layout preview service (contract v1, roadmap P10.3 / D46 - D51). One client serves
 * one editing screen: it binds the service on demand and keeps the binding until [close], owns one preview
 * session that follows the canvas surface (created on the first surface, relaid out on resize, re-attached after
 * the surface comes back, released with the client), sends layouts debounced so that the last request wins, and
 * turns the host's callbacks into Compose state on the main thread. Binder calls run on a single worker thread
 * because the host answers `setLayout` only when its main thread has applied the frame.
 *
 * Every reference to the API 30 embedding types stays in [Embedded], so the class loads on API 24 - 29, where the
 * session runs in the snapshot mode and the pixels of each frame become an [ImageBitmap].
 */
internal class UiPreviewClient(context: Context, private val label: String) : AutoCloseable {
    private val context = context.applicationContext

    /** Why the preview cannot be shown at all. */
    enum class Reason { HOST_MISSING, HOST_TOO_OLD, CONTRACT_MISMATCH, UNAVAILABLE }

    sealed class Status {
        object Idle : Status()
        object Binding : Status()
        data class Ready(val hostVersionName: String?, val hostVersionCode: Long) : Status()
        data class Unavailable(val reason: Reason, val installedVersion: String?, val detail: String?) : Status()
        /** The host released the session or died; [reason] is a contract reason or `host`. */
        data class Lost(val reason: String) : Status()
    }

    /** One view of the host's tree, in root pixels; [index] is the element preorder index, [instance] the list item repeat. */
    data class Bound(val index: Int, val left: Int, val top: Int, val right: Int, val bottom: Int, val className: String, val id: String?, val instance: Int, val visibility: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
        val area: Long get() = width.toLong() * height
        fun contains(x: Float, y: Float): Boolean = x >= left && x < right && y >= top && y < bottom
    }

    /** A frame the host could not apply; [elementIndex] is -1 when unknown. */
    data class Failure(val code: String, val message: String, val elementIndex: Int, val attrName: String?, val line: Int, val column: Int)

    /** A property-level problem of an applied frame. */
    data class Diagnostic(val code: String, val message: String, val elementIndex: Int, val attrName: String?)

    /** One console entry of the running script (contract v2); [level] is an `android.util.Log` priority. */
    data class ConsoleLine(val level: Int, val text: String)

    /** The host stopped a run whose script kept its main thread busy beyond [limitMs] at [line] (-1 unknown) (contract v4). */
    data class BusyWait(val line: Int, val elapsedMs: Long, val limitMs: Long)

    /** A lifecycle event of `sendLifecycle` reached the run (contract v4); [consumed] matters for `back_pressed` only. */
    data class LifecycleHandled(val event: String, val consumed: Boolean)

    /** Whether the host offers `runScript` (contract v2 on API 33+); known once the handshake is done. */
    var scriptRunSupported: Boolean by mutableStateOf(false)
        private set
    /**
     * Whether the host resolves a project's `res/` directory in layouts and runs a whole project (contract v3,
     * roadmap P11.H1); known once the handshake is done.
     */
    var resourcesSupported: Boolean by mutableStateOf(false)
        private set
    /**
     * What the host does for script runs beyond contract v3 (roadmap P11.5 / P11.6): announced by a v4 host, empty for
     * older hosts and where runs are not supported; known once the handshake is done.
     */
    var capabilities: Set<PreviewCapability> by mutableStateOf(emptySet())
        private set
    /** The host delivers pause / resume / back_pressed to `ui.emitter` of a run (contract v4). */
    val lifecycleSupported: Boolean get() = PreviewCapability.LIFECYCLE_EVENTS in capabilities
    /**
     * How long a run may keep the host's main thread busy, sent with each run to a host that takes it (contract v4);
     * the designer sets it from the settings page. Main thread.
     */
    var busyWaitLimitMs: Long = Contract.DEFAULT_BUSY_WAIT_LIMIT_MS
    /** A script run is going (from `runScript` until `onScriptExit`). */
    var running: Boolean by mutableStateOf(false)
        private set
    var runId: String? by mutableStateOf(null)
        private set
    /** The running script's console, newest last; cleared when a run starts. */
    var consoleLines: List<ConsoleLine> by mutableStateOf(emptyList())
        private set
    /** The last uncaught error of the run, or why it could not start. */
    var scriptError: String? by mutableStateOf(null)
        private set
    /**
     * The busy wait that ended the run (contract v4: the host's limit stopped a script that kept its main thread busy),
     * or null; [scriptError] then holds the host's own wording, which the designer replaces with its localized one.
     */
    var busyWait: BusyWait? by mutableStateOf(null)
        private set
    /** The last lifecycle event the host delivered to the run and whether a handler or a dialog consumed it (contract v4). */
    var lastLifecycle: LifecycleHandled? by mutableStateOf(null)
        private set
    /** Why the last run ended (a contract exit reason), null while none ended. */
    var lastExit: String? by mutableStateOf(null)
        private set
    /** Counts script starts, so observers can wait for the next run. */
    var runRevision: Long by mutableStateOf(0L)
        private set
    /** Counts created sessions, so observers can act once a session (again) exists. */
    var sessionRevision: Long by mutableStateOf(0L)
        private set
    /**
     * The interactive preview is on: the embedded tree takes the pointer, the script runs in the session's root
     * and layouts are held back until the preview ends (they would replace the running script's content).
     */
    var interactive: Boolean by mutableStateOf(false)
        private set
    /** The host's contract version from the handshake (1 for a v1 host). */
    private var hostContractVersion = Contract.BASE_VERSION
    /** The busy-wait limit the current run was started with. */
    private var runBusyWaitLimitMs = Contract.DEFAULT_BUSY_WAIT_LIMIT_MS
    /**
     * Where the document's files are (roadmap P11.3): sent with layouts and runs as zips once the host takes them
     * (contract v3); null for a document without a folder (a single-file private project, a content Uri).
     */
    @Volatile var projectRoot: ProjectArchives.Root? = null
    /** The script's path inside the project zip of a run: `main.js` of a folder project, else the script's file name. */
    @Volatile var scriptPath: String = MAIN_SCRIPT
    private val archives = ProjectArchives(File(context.cacheDir, ARCHIVE_DIRECTORY))

    val mode: String = if (Build.VERSION.SDK_INT >= Contract.MIN_API_EMBEDDED) Contract.MODE_EMBEDDED else Contract.MODE_SNAPSHOT
    val embedded: Boolean get() = mode == Contract.MODE_EMBEDDED
    /** This Android can hand the pointer to the embedded tree (D50: API 33+); the host confirms it per session. */
    val pointerInteractiveAvailable: Boolean = Build.VERSION.SDK_INT >= Contract.MIN_API_INTERACTIVE
    /** This Android can give the embedded tree keyboard input (D50: API 35+). */
    val imeAvailable: Boolean = Build.VERSION.SDK_INT >= Contract.MIN_API_IME

    var status: Status by mutableStateOf(Status.Idle)
        private set
    var bounds: List<Bound> by mutableStateOf(emptyList())
        private set
    var elementCount: Int by mutableStateOf(0)
        private set
    /** Counts delivered frames (bounds or snapshots), so observers can wait for the next one. */
    var frameRevision: Long by mutableStateOf(0L)
        private set
    var failure: Failure? by mutableStateOf(null)
        private set
    var diagnostics: List<Diagnostic> by mutableStateOf(emptyList())
        private set
    var snapshot: ImageBitmap? by mutableStateOf(null)
        private set
    /** True while a layout waits to be sent or is being applied by the host. */
    var busy: Boolean by mutableStateOf(false)
        private set

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "ui-preview-client").apply { isDaemon = true } }
    private var connection: ServiceConnection? = null
    private var service: IUiPreviewService? = null
    private var sessionId: String? = null
    private var surface: SurfaceView? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var nightMode = Contract.NIGHT_MODE_FOLLOW_HOST
    private var localeTag = ""
    private var pending: XmlPreviewRequest? = null
    private var flushScheduled = false
    private var highlighted = -1
    private var pointerMode = Contract.POINTER_MODE_EDIT
    private var retriedCreate = false
    /** A createSession round trip is in flight: a second request would make a second session (the host allows two). */
    private var creating = false
    @Volatile private var closed = false
    /** Bumped whenever the session changes, so callbacks of an old session are ignored. */
    private var generation = 0

    private val callback = object : IUiPreviewCallback.Stub() {
        override fun onBounds(sessionId: String, bounds: Bundle) {
            val received = parseBounds(bounds.getString(Contract.KEY_BOUNDS_JSON).orEmpty())
            val count = bounds.getInt(Contract.KEY_ELEMENT_COUNT, -1)
            main.post {
                if (sessionId != this@UiPreviewClient.sessionId) return@post
                this@UiPreviewClient.bounds = received
                if (count >= 0) elementCount = count
                frameRevision++
            }
        }

        override fun onLayoutFailed(sessionId: String, failure: Bundle) {
            val diagnostic = Diagnostic(
                failure.getString(Contract.KEY_ERROR_CODE).orEmpty(),
                failure.getString(Contract.KEY_ERROR_MESSAGE).orEmpty(),
                failure.getInt(Contract.KEY_ELEMENT_INDEX, -1),
                failure.getString(Contract.KEY_ATTR_NAME),
            )
            main.post { if (sessionId == this@UiPreviewClient.sessionId) diagnostics = diagnostics + diagnostic }
        }

        override fun onSessionLost(sessionId: String, reason: Bundle) {
            val why = reason.getString(Contract.KEY_REASON) ?: "unknown"
            main.post {
                if (sessionId != this@UiPreviewClient.sessionId) return@post
                this@UiPreviewClient.sessionId = null
                generation++
                running = false
                runId = null
                status = Status.Lost(why)
            }
        }

        override fun onTap(sessionId: String, tap: Bundle) {
            /* Edit mode keeps the surface below the window, so taps reach the canvas overlay instead (P10.3). */
        }

        // Contract v2 (roadmap P10.6): the running script's console, errors and end.

        override fun onConsole(sessionId: String, entry: Bundle) {
            val line = ConsoleLine(entry.getInt(Contract.KEY_LEVEL, android.util.Log.INFO), entry.getString(Contract.KEY_TEXT).orEmpty())
            main.post {
                if (sessionId != this@UiPreviewClient.sessionId) return@post
                val lines = consoleLines
                consoleLines = (if (lines.size >= MAX_CONSOLE_LINES) lines.drop(lines.size - MAX_CONSOLE_LINES + 1) else lines) + line
            }
        }

        override fun onScriptError(sessionId: String, error: Bundle) {
            val message = error.getString(Contract.KEY_ERROR_MESSAGE).orEmpty()
            val line = error.getInt(Contract.KEY_LINE, -1)
            // Contract v4: the busy-wait limit ended the run; the code says so, the time is how long the thread was busy.
            val busy = error.getString(Contract.KEY_ERROR_CODE) == Contract.ERROR_BUSY_WAIT
            val elapsed = error.getLong(Contract.KEY_ELAPSED_MS, 0L)
            main.post {
                if (sessionId != this@UiPreviewClient.sessionId) return@post
                scriptError = if (line > 0) "$line: $message" else message
                busyWait = if (busy) BusyWait(line, elapsed, runBusyWaitLimitMs) else null
            }
        }

        override fun onScriptExit(sessionId: String, exit: Bundle) {
            val id = exit.getString(Contract.KEY_RUN_ID)
            val reason = exit.getString(Contract.KEY_REASON) ?: "unknown"
            main.post {
                if (sessionId != this@UiPreviewClient.sessionId) return@post
                if (id != null && runId != null && id != runId) return@post
                running = false
                lastExit = reason
            }
        }

        // Contract v4 (roadmap P11.5 / P11.6): a lifecycle event of sendLifecycle reached the run.

        override fun onLifecycleHandled(sessionId: String, result: Bundle) {
            val id = result.getString(Contract.KEY_RUN_ID)
            val handled = LifecycleHandled(result.getString(Contract.KEY_LIFECYCLE_EVENT).orEmpty(), result.getBoolean(Contract.KEY_CONSUMED, false))
            main.post {
                if (sessionId != this@UiPreviewClient.sessionId) return@post
                if (id != null && runId != null && id != runId) return@post
                lastLifecycle = handled
            }
        }
    }

    // ------------------------------------------------------------------------------------------- availability

    companion object {
        private const val TAG = "UiPreviewClient"
        const val DEBOUNCE_MS = 400L
        private const val CREATE_RETRY_DELAY_MS = 500L
        private const val MAX_CONSOLE_LINES = 500
        private const val SNAPSHOT_DIRECTORY = "ui-preview"
        private const val ARCHIVE_DIRECTORY = "ui-preview-archives"
        private const val MAIN_SCRIPT = "main.js"

        /** Why the host cannot preview without binding it: not installed, or without the service; null when it may. */
        fun availability(context: Context): Status.Unavailable? {
            val installed = installedHostVersion(context) ?: return Status.Unavailable(Reason.HOST_MISSING, null, null)
            val intent = Intent(Contract.ACTION).setPackage(ThreeBlockComposePlugin.HOST_PACKAGE_NAME)
            @Suppress("DEPRECATION")
            val resolved = context.packageManager.resolveService(intent, 0)
            return if (resolved == null) Status.Unavailable(Reason.HOST_TOO_OLD, installed, null) else null
        }

        fun installedHostVersion(context: Context): String? = try {
            context.packageManager.getPackageInfo(ThreeBlockComposePlugin.HOST_PACKAGE_NAME, 0).versionName ?: ""
        } catch (error: PackageManager.NameNotFoundException) {
            null
        }

        /** The capabilities a v4 host announces through the flags of its preview info ([flag] reads one, false when absent). */
        internal fun capabilitiesOf(flag: (String, Boolean) -> Boolean): Set<PreviewCapability> = buildSet {
            if (flag(Contract.KEY_DIALOGS_IN_CANVAS, false)) add(PreviewCapability.DIALOGS_IN_CANVAS)
            if (flag(Contract.KEY_LIFECYCLE_EVENTS, false)) add(PreviewCapability.LIFECYCLE_EVENTS)
            if (flag(Contract.KEY_BUSY_WAIT_LIMIT_SUPPORTED, false)) add(PreviewCapability.BUSY_WAIT_LIMIT)
            if (flag(Contract.KEY_WINDOW_FUNCTIONS_VISIBLE, false)) add(PreviewCapability.WINDOW_FUNCTIONS_VISIBLE)
        }

        /** [seconds] of the designer's setting as the host's limit, kept within the contract's range. */
        fun busyWaitLimitOf(seconds: Int): Long =
            (seconds * 1_000L).coerceIn(Contract.MIN_BUSY_WAIT_LIMIT_MS, Contract.MAX_BUSY_WAIT_LIMIT_MS)

        internal fun parseBounds(json: String): List<Bound> {
            val list = JsonText.parse(json) as? List<*> ?: return emptyList()
            return list.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                fun int(key: String) = (map[key] as? Number)?.toInt() ?: 0
                Bound(
                    int(Contract.Bounds.ELEMENT_INDEX), int(Contract.Bounds.LEFT), int(Contract.Bounds.TOP), int(Contract.Bounds.RIGHT), int(Contract.Bounds.BOTTOM),
                    map[Contract.Bounds.CLASS_NAME] as? String ?: "", map[Contract.Bounds.ID] as? String, int(Contract.Bounds.INSTANCE), int(Contract.Bounds.VISIBILITY),
                )
            }
        }

        internal fun parseDiagnostics(json: String): List<Diagnostic> {
            val list = JsonText.parse(json) as? List<*> ?: return emptyList()
            return list.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                Diagnostic(
                    map[Contract.Diagnostic.CODE] as? String ?: "", map[Contract.Diagnostic.MESSAGE] as? String ?: "",
                    (map[Contract.Diagnostic.ELEMENT_INDEX] as? Number)?.toInt() ?: -1, map[Contract.Diagnostic.ATTR_NAME] as? String,
                )
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ binding

    /** Binds the host service when it is not bound; the status tells the outcome. Main thread. */
    fun bind() {
        if (closed || service != null || status == Status.Binding) return
        availability(context)?.let { status = it; return }
        status = Status.Binding
        val intent = Intent(Contract.ACTION).setPackage(ThreeBlockComposePlugin.HOST_PACKAGE_NAME)
        val callbacks = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                if (closed || connection !== this) return
                worker.execute { handshake(IUiPreviewService.Stub.asInterface(binder)) }
            }

            override fun onServiceDisconnected(name: ComponentName) { onHostGone() }
            override fun onBindingDied(name: ComponentName) { onHostGone() }
            override fun onNullBinding(name: ComponentName) {
                main.post { if (connection === this) { unbind(); status = Status.Unavailable(Reason.UNAVAILABLE, installedHostVersion(context), "null binding") } }
            }
        }
        connection = callbacks
        val bound = try {
            context.bindService(intent, callbacks, Context.BIND_AUTO_CREATE)
        } catch (error: SecurityException) {
            false
        }
        if (!bound) {
            unbind()
            status = Status.Unavailable(Reason.UNAVAILABLE, installedHostVersion(context), "bindService failed")
        }
    }

    private fun handshake(remote: IUiPreviewService) {
        try {
            // The frozen v1 base identifies the service; v2 (runScript) is announced through the preview info.
            if (remote.contractVersion != Contract.BASE_VERSION || remote.contractHash != Contract.HASH_V1) {
                main.post { unbind(); status = Status.Unavailable(Reason.CONTRACT_MISMATCH, installedHostVersion(context), null) }
                return
            }
            val info = remote.previewInfo
            // v3 (project resources, P11.H1) and v4 (lifecycle events, P11.5) are listed with their hashes; the info's own
            // identity keys stay at v2 (runScript).
            val versions = info.getIntArray(Contract.KEY_SUPPORTED_CONTRACT_VERSIONS)
            val hashes = info.getStringArray(Contract.KEY_CONTRACT_HASHES)
            fun serves(version: Int, hash: String) = versions != null && hashes != null && versions.indices.any { versions[it] == version && hashes.getOrNull(it) == hash }
            val v4 = serves(Contract.VERSION, Contract.HASH)
            val v3 = v4 || serves(Contract.VERSION_V3, Contract.HASH_V3)
            val v2 = v3 || info.getInt(Contract.KEY_CONTRACT_VERSION, Contract.BASE_VERSION) >= Contract.VERSION_V2 && info.getString(Contract.KEY_CONTRACT_HASH) == Contract.HASH_V2
            val announced = if (!v4) emptySet() else capabilitiesOf(info::getBoolean)
            main.post {
                if (closed) return@post
                service = remote
                hostContractVersion = when {
                    v4 -> Contract.VERSION
                    v3 -> Contract.VERSION_V3
                    v2 -> Contract.VERSION_V2
                    else -> Contract.BASE_VERSION
                }
                resourcesSupported = v3 && info.getBoolean(Contract.KEY_RESOURCES_SUPPORTED, false)
                scriptRunSupported = v2 && info.getBoolean(Contract.KEY_SCRIPT_RUN_SUPPORTED, false) && embedded
                capabilities = if (scriptRunSupported) announced else emptySet()
                status = Status.Ready(info.getString(Contract.KEY_HOST_VERSION_NAME), info.getLong(Contract.KEY_HOST_VERSION_CODE))
                ensureSession()
            }
        } catch (error: RemoteException) {
            main.post { unbind(); status = Status.Unavailable(Reason.UNAVAILABLE, installedHostVersion(context), error.message) }
        } catch (error: SecurityException) {
            main.post { unbind(); status = Status.Unavailable(Reason.UNAVAILABLE, installedHostVersion(context), error.message) }
        }
    }

    private fun onHostGone() {
        main.post {
            if (closed) return@post
            unbind()
            sessionId = null
            generation++
            running = false
            runId = null
            status = Status.Lost("host")
        }
    }

    private fun unbind() {
        connection?.let { runCatching { context.unbindService(it) } }
        connection = null
        service = null
    }

    /** After a loss: binds again and recreates the session once the surface is there. Main thread. */
    fun retry() {
        if (closed) return
        status = Status.Idle
        bind()
    }

    override fun close() {
        closed = true
        pending = null
        val id = sessionId
        val remote = service
        sessionId = null
        if (id != null && remote != null) {
            try { worker.execute { runCatching { remote.release(id) } } } catch (rejected: RejectedExecutionException) { /* closed */ }
        }
        worker.shutdown()
        unbind()
    }

    // ------------------------------------------------------------------------------------------------ session

    /** The night mode and locale the session is created with; a change recreates the session. Main thread. */
    fun configure(night: Boolean?, locale: String) {
        val mode = when (night) { null -> Contract.NIGHT_MODE_FOLLOW_HOST; true -> Contract.NIGHT_MODE_YES; false -> Contract.NIGHT_MODE_NO }
        if (mode == nightMode && locale == localeTag) return
        nightMode = mode
        localeTag = locale
        if (sessionId != null) {
            releaseSession()
            ensureSession()
        }
    }

    /** The canvas surface was created (embedded mode); attaches the session's package or creates the session. Main thread. */
    fun attachSurface(view: SurfaceView, widthPx: Int, heightPx: Int) {
        surface = view
        if (widthPx > 0 && heightPx > 0) { surfaceWidth = widthPx; surfaceHeight = heightPx }
        if (sessionId != null) reattach() else ensureSession()
    }

    /** The surface or the snapshot viewport changed size. Main thread. */
    fun resize(widthPx: Int, heightPx: Int) {
        if (widthPx <= 0 || heightPx <= 0) return
        val changed = widthPx != surfaceWidth || heightPx != surfaceHeight
        surfaceWidth = widthPx
        surfaceHeight = heightPx
        val id = sessionId
        when {
            id == null -> ensureSession()
            changed -> {
                call { remote -> remote.resize(id, widthPx, heightPx) }
                // A snapshot is drawn at the session size: the new size needs a new frame.
                if (!embedded) scheduleFlush(resend = true)
            }
        }
    }

    /** The canvas surface is gone (the screen is hidden); the session is kept for [attachSurface]. Main thread. */
    fun detachSurface() {
        surface = null
    }

    /**
     * Creates the session when the service and the viewport are ready and no session exists. While the host is
     * creating one, later calls return; what changed meanwhile (the viewport, the night mode or locale) is
     * applied once the session arrives. Main thread.
     */
    fun ensureSession() {
        if (closed || sessionId != null || creating) return
        val remote = service ?: run { bind(); return }
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return
        val view = surface
        if (embedded && view == null) return
        val request = Bundle().apply {
            // A v1 host accepts only its own version; a v2 host accepts both.
            putInt(Contract.KEY_CONTRACT_VERSION, hostContractVersion)
            putString(Contract.KEY_MODE, mode)
            putInt(Contract.KEY_WIDTH_PX, surfaceWidth)
            putInt(Contract.KEY_HEIGHT_PX, surfaceHeight)
            putInt(Contract.KEY_NIGHT_MODE, nightMode)
            putString(Contract.KEY_LOCALE_TAG, localeTag)
            putString(Contract.KEY_LABEL, label)
            putLong(Contract.KEY_CLIENT_VERSION_CODE, clientVersionCode())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && embedded && view != null) Embedded.fill(this, view)
        }
        val myGeneration = ++generation
        val requestedWidth = surfaceWidth
        val requestedHeight = surfaceHeight
        val requestedNight = nightMode
        val requestedLocale = localeTag
        creating = true
        bounds = emptyList()
        failure = null
        diagnostics = emptyList()
        worker.execute {
            val result = try {
                remote.createSession(request, callback)
            } catch (error: RemoteException) {
                null
            } catch (error: SecurityException) {
                null
            }
            main.post {
                creating = false
                if (closed || myGeneration != generation) {
                    // The screen closed (its worker is shut down) or restarted the session while the host was creating this one.
                    result?.getString(Contract.KEY_SESSION_ID)?.let { id ->
                        try { worker.execute { runCatching { remote.release(id) } } } catch (rejected: RejectedExecutionException) { Thread({ runCatching { remote.release(id) } }, "ui-preview-release").start() }
                    }
                    // The restart that abandoned this one found a creation in flight: it gets its session now.
                    if (!closed && sessionId == null && service != null) ensureSession()
                    return@post
                }
                val id = result?.getString(Contract.KEY_SESSION_ID)
                val code = result?.getString(Contract.KEY_ERROR_CODE)
                if (result == null) {
                    onHostGone()
                } else if (id == null && (code == Contract.ERROR_TIMEOUT || code == Contract.ERROR_SESSION_LIMIT) && !retriedCreate) {
                    // A cold host (its first script engine) can outlive the host's wait once; the second attempt finds it
                    // warm. The host counts a session this client just released until its main thread gets to it.
                    retriedCreate = true
                    main.postDelayed({ ensureSession() }, CREATE_RETRY_DELAY_MS)
                } else if (id == null) {
                    status = Status.Unavailable(Reason.UNAVAILABLE, installedHostVersion(context), result.getString(Contract.KEY_ERROR_MESSAGE))
                } else {
                    retriedCreate = false
                    sessionId = id
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && embedded) surface?.let { Embedded.attach(it, result) }
                    if (highlighted >= 0) call { it.setHighlight(id, highlighted) }
                    if (pointerMode != Contract.POINTER_MODE_EDIT) call { it.setPointerMode(id, pointerMode) }
                    scheduleFlush(resend = true)
                    sessionRevision++
                    // What changed while the host was creating: a configuration change needs a new session, a new
                    // viewport size follows with a resize (the queued flush redraws a snapshot at that size).
                    if (requestedNight != nightMode || requestedLocale != localeTag) {
                        releaseSession()
                        ensureSession()
                    } else if (requestedWidth != surfaceWidth || requestedHeight != surfaceHeight) {
                        val width = surfaceWidth
                        val height = surfaceHeight
                        call { remote -> remote.resize(id, width, height) }
                    }
                }
            }
        }
    }

    private fun reattach() {
        val id = sessionId ?: return
        val view = surface ?: return
        if (!embedded) return
        call(onResult = { reattached -> main.post { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && sessionId == id && surface === view) Embedded.attach(view, reattached) } }) { remote -> remote.reattach(id) }
    }

    fun releaseSession() {
        val id = sessionId ?: return
        sessionId = null
        generation++
        running = false
        runId = null
        val remote = service ?: return
        try { worker.execute { runCatching { remote.release(id) } } } catch (rejected: RejectedExecutionException) { /* closed */ }
    }

    fun setHighlight(elementIndex: Int) {
        highlighted = elementIndex
        val id = sessionId ?: return
        call { it.setHighlight(id, elementIndex) }
    }

    fun setPointerMode(mode: Int) {
        pointerMode = mode
        val id = sessionId ?: return
        call { it.setPointerMode(id, mode) }
    }

    // ------------------------------------------------------------------------------- interactive preview (v2)

    /**
     * Turns the interactive preview on or off (roadmap P10.6). On: the host passes the pointer to the embedded
     * tree and layouts are held until the preview ends; the caller then starts the script with [runScript].
     * Off: the running script is stopped, the pointer returns to the edit mode and the last layout is sent again,
     * so the canvas shows the document once more. Main thread.
     */
    fun setInteractiveMode(active: Boolean) {
        if (closed || interactive == active) return
        interactive = active
        if (active) {
            setPointerMode(Contract.POINTER_MODE_INTERACTIVE)
        } else {
            stopScript()
            setPointerMode(Contract.POINTER_MODE_EDIT)
            scheduleFlush(resend = true)
        }
    }

    /**
     * Runs [text] (a whole `"ui"` mode script) in the session's root as a real host script (roadmap P10.6);
     * a run still going is replaced. The console, errors and the end arrive as state. Main thread.
     */
    fun runScript(text: String, name: String, files: Map<String, String> = emptyMap()) {
        if (closed || !scriptRunSupported) return
        val id = sessionId ?: return
        consoleLines = emptyList()
        scriptError = null
        busyWait = null
        lastLifecycle = null
        lastExit = null
        running = true
        runRevision++
        val limit = busyWaitLimitMs
        runBusyWaitLimitMs = limit
        val request = Bundle().apply {
            putString(Contract.KEY_SCRIPT_TEXT, text)
            putString(Contract.KEY_SCRIPT_NAME, name)
            // Contract v4: older hosts would ignore the key, but it is only sent to a host that takes it.
            if (PreviewCapability.BUSY_WAIT_LIMIT in capabilities) putLong(Contract.KEY_BUSY_WAIT_LIMIT_MS, limit)
        }
        call(onResult = { result ->
            main.post {
                if (sessionId != id) return@post
                if (result.getString(Contract.KEY_OUTCOME) == Contract.OUTCOME_APPLIED) {
                    runId = result.getString(Contract.KEY_RUN_ID)
                } else {
                    running = false
                    scriptError = result.getString(Contract.KEY_ERROR_MESSAGE) ?: result.getString(Contract.KEY_ERROR_CODE)
                    lastExit = Contract.EXIT_ERROR
                }
            }
        }) { remote ->
            // Roadmap P11.3: a script with a folder (or layout files) runs in its unpacked project (contract v3).
            var project: ParcelFileDescriptor? = null
            try {
                val root = projectRoot
                val path = scriptPath.takeIf(ProjectArchives::isEntryPath) ?: MAIN_SCRIPT
                if (resourcesSupported && (root != null || files.isNotEmpty())) {
                    archives.archive(ProjectArchives.PROJECT, root, files + (path to text))?.let { archive ->
                        project = ParcelFileDescriptor.open(archive.file, ParcelFileDescriptor.MODE_READ_ONLY)
                        request.putParcelable(Contract.KEY_PROJECT_FD, project)
                        request.putString(Contract.KEY_PROJECT_SHA256, archive.sha256)
                        request.putString(Contract.KEY_SCRIPT_PATH, path)
                    }
                }
                remote.runScript(id, request)
            } finally {
                runCatching { project?.close() }
            }
        }
    }

    /**
     * Sends [event] (`pause`, `resume` or `back_pressed` of [Contract.LIFECYCLE_EVENTS]) to the running script (contract
     * v4, roadmap P11.6): `ui.emitter` gets it as from an Activity, and an unconsumed back key ends the run with `exit`.
     * Does nothing without a run or with a host that does not deliver lifecycle events. Main thread.
     */
    fun sendLifecycle(event: String) {
        if (closed || !lifecycleSupported || !running || runId == null) return
        val id = sessionId ?: return
        val request = Bundle().apply { putString(Contract.KEY_LIFECYCLE_EVENT, event) }
        // NOT_RUNNING (the run ended meanwhile) needs no handling: onScriptExit tells.
        call { remote -> remote.sendLifecycle(id, request) }
    }

    /** Stops the running script, if any; the layout preview can be sent again afterwards. Main thread. */
    fun stopScript() {
        val id = sessionId ?: return
        if (!running && runId == null) return
        running = false
        runId = null
        call { remote -> remote.stopScript(id) }
    }

    /** Sends the last layout again (after a script run ended, the root shows the script's content). Main thread. */
    fun resendLayout() {
        if (closed) return
        scheduleFlush(resend = true)
    }

    /** Empties the running script's console; the next run empties it again. Main thread. */
    fun clearConsole() {
        consoleLines = emptyList()
    }

    // ------------------------------------------------------------------------------------------------- layouts

    /** Queues [request] for the host; a later request replaces one still waiting. Main thread. */
    fun submit(request: XmlPreviewRequest) {
        if (closed) return
        pending = request
        busy = true
        scheduleFlush(resend = false)
    }

    private var lastSent: XmlPreviewRequest? = null

    private fun scheduleFlush(resend: Boolean) {
        if (resend && pending == null) pending = lastSent
        if (pending == null || flushScheduled) return
        flushScheduled = true
        main.postDelayed({ flushScheduled = false; flush() }, DEBOUNCE_MS)
    }

    private fun flush() {
        if (closed) return
        val request = pending ?: run { busy = false; return }
        // The interactive preview owns the root: the layout waits (as pending) until the preview ends.
        if (interactive) { busy = false; return }
        val id = sessionId
        val remote = service
        if (id == null || remote == null) {
            // Sent once the session exists (ensureSession schedules a flush).
            if (remote == null && status !is Status.Unavailable) bind()
            return
        }
        pending = null
        lastSent = request
        val myGeneration = generation
        val layout = Bundle().apply {
            putString(Contract.KEY_XML, request.xml)
            putString(Contract.KEY_BINDINGS_JSON, request.bindingsJson)
            // Roadmap P11.3: the script's ui.useAndroidLayout mode (contract v3); automatic when absent.
            if (hostContractVersion >= Contract.VERSION_V3) request.androidLayout?.let { putBoolean(Contract.KEY_ANDROID_LAYOUT, it) }
        }
        worker.execute { apply(remote, id, myGeneration, layout, request, surfaceWidth, surfaceHeight) }
    }

    /** Worker thread: one setLayout round trip, with the project's resources (contract v3) and the snapshot file for the snapshot mode. */
    private fun apply(remote: IUiPreviewService, id: String, myGeneration: Int, layout: Bundle, request: XmlPreviewRequest, width: Int, height: Int) {
        var file: File? = null
        var output: ParcelFileDescriptor? = null
        var resources: ParcelFileDescriptor? = null
        val result = try {
            val root = projectRoot
            if (resourcesSupported && (root != null || request.files.isNotEmpty())) {
                archives.archive(ProjectArchives.RESOURCES, root, request.files)?.let { archive ->
                    resources = ParcelFileDescriptor.open(archive.file, ParcelFileDescriptor.MODE_READ_ONLY)
                    layout.putParcelable(Contract.KEY_RESOURCES_FD, resources)
                    layout.putString(Contract.KEY_RESOURCES_SHA256, archive.sha256)
                    request.layoutPath?.takeIf { it in archive.paths }?.let { layout.putString(Contract.KEY_LAYOUT_PATH, it) }
                }
            }
            if (!embedded) {
                val directory = File(context.cacheDir, SNAPSHOT_DIRECTORY).apply { mkdirs() }
                file = File(directory, UUID.randomUUID().toString() + ".argb")
                output = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE)
            }
            remote.setLayout(id, layout, output)
        } catch (error: DeadObjectException) {
            onHostGone(); null
        } catch (error: RemoteException) {
            Log.w(TAG, "setLayout failed", error); null
        } catch (error: SecurityException) {
            Log.w(TAG, "setLayout refused", error); null
        } catch (error: RuntimeException) {
            // Binder rethrows a few of the host's unchecked exceptions here (a view that throws while the host
            // measures the layout); that is a failed frame, not the designer's end.
            Log.w(TAG, "setLayout threw in the host", error)
            Bundle().apply {
                putString(Contract.KEY_OUTCOME, Contract.OUTCOME_FAILED)
                putString(Contract.KEY_ERROR_CODE, Contract.ERROR_INFLATE_FAILED)
                putString(Contract.KEY_ERROR_MESSAGE, error.message ?: error.javaClass.simpleName)
            }
        } finally {
            runCatching { output?.close() }
            runCatching { resources?.close() }
        }
        var image: ImageBitmap? = null
        if (result != null && result.getString(Contract.KEY_OUTCOME) == Contract.OUTCOME_APPLIED && file != null) {
            val expectedLength = result.getLong(Contract.KEY_SNAPSHOT_BYTE_LENGTH)
            val snapshotWidth = result.getInt(Contract.KEY_SNAPSHOT_WIDTH_PX)
            val snapshotHeight = result.getInt(Contract.KEY_SNAPSHOT_HEIGHT_PX)
            val bytes = runCatching { file.readBytes() }.getOrNull()
            if (bytes != null && bytes.size.toLong() == expectedLength && expectedLength == snapshotWidth.toLong() * snapshotHeight * Contract.SNAPSHOT_BYTES_PER_PIXEL) {
                val bitmap = Bitmap.createBitmap(snapshotWidth, snapshotHeight, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
                image = bitmap.asImageBitmap()
            }
        }
        file?.delete()
        main.post {
            if (closed || myGeneration != generation) return@post
            busy = pending != null
            if (result == null) return@post
            when (result.getString(Contract.KEY_OUTCOME)) {
                Contract.OUTCOME_APPLIED -> {
                    failure = null
                    diagnostics = parseDiagnostics(result.getString(Contract.KEY_DIAGNOSTICS_JSON) ?: "[]")
                    val count = result.getInt(Contract.KEY_ELEMENT_COUNT, -1)
                    if (count >= 0) elementCount = count
                    if (image != null) { snapshot = image; frameRevision++ }
                }
                Contract.OUTCOME_TIMED_OUT -> failure = Failure(Contract.ERROR_TIMEOUT, result.getString(Contract.KEY_ERROR_MESSAGE).orEmpty(), -1, null, -1, -1)
                else -> failure = Failure(
                    result.getString(Contract.KEY_ERROR_CODE).orEmpty(), result.getString(Contract.KEY_ERROR_MESSAGE).orEmpty(),
                    result.getInt(Contract.KEY_ELEMENT_INDEX, -1), result.getString(Contract.KEY_ATTR_NAME),
                    result.getInt(Contract.KEY_LINE, -1), result.getInt(Contract.KEY_COLUMN, -1),
                )
            }
            if (failure?.code == Contract.ERROR_SESSION_NOT_FOUND) { sessionId = null; generation++; status = Status.Lost(Contract.ERROR_SESSION_NOT_FOUND) }
        }
    }

    // ------------------------------------------------------------------------------------------------- helpers

    /** Runs a Binder call on the worker; a result reaches [onResult] on the worker, a dead host marks the loss. */
    private fun call(onResult: ((Bundle) -> Unit)? = null, block: (IUiPreviewService) -> Bundle) {
        val remote = service ?: return
        try {
            worker.execute {
                val result = try { block(remote) } catch (error: DeadObjectException) { onHostGone(); null } catch (error: RemoteException) { null } catch (error: RuntimeException) { Log.w(TAG, "the host threw", error); null }
                if (result != null && onResult != null) onResult(result)
            }
        } catch (rejected: RejectedExecutionException) {
            /* closed */
        }
    }

    private fun clientVersionCode(): Long = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    } catch (error: PackageManager.NameNotFoundException) {
        0L
    }

    /** Every reference to the API 30 embedding types, so the client loads on API 24 - 29. */
    @RequiresApi(Build.VERSION_CODES.R)
    private object Embedded {
        fun fill(request: Bundle, view: SurfaceView) {
            @Suppress("DEPRECATION")
            request.putBinder(Contract.KEY_HOST_TOKEN, view.hostToken)
            request.putInt(Contract.KEY_DISPLAY_ID, view.display?.displayId ?: 0)
            if (Build.VERSION.SDK_INT >= 35) view.rootSurfaceControl?.inputTransferToken?.let { request.putParcelable(Contract.KEY_INPUT_TRANSFER_TOKEN, it) }
        }

        fun attach(view: SurfaceView, result: Bundle) {
            @Suppress("DEPRECATION")
            val parcelable: Parcelable? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) result.getParcelable(Contract.KEY_SURFACE_PACKAGE, Parcelable::class.java)
            else result.getParcelable(Contract.KEY_SURFACE_PACKAGE)
            (parcelable as? SurfaceControlViewHost.SurfacePackage)?.let { view.setChildSurfacePackage(it) }
        }
    }
}
