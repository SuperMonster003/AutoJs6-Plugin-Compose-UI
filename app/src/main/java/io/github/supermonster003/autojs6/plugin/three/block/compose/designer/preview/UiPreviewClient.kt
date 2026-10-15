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

    val mode: String = if (Build.VERSION.SDK_INT >= Contract.MIN_API_EMBEDDED) Contract.MODE_EMBEDDED else Contract.MODE_SNAPSHOT
    val embedded: Boolean get() = mode == Contract.MODE_EMBEDDED

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
                status = Status.Lost(why)
            }
        }

        override fun onTap(sessionId: String, tap: Bundle) {
            /* Edit mode keeps the surface below the window, so taps reach the canvas overlay instead (P10.3). */
        }
    }

    // ------------------------------------------------------------------------------------------- availability

    companion object {
        private const val TAG = "UiPreviewClient"
        const val DEBOUNCE_MS = 400L
        private const val CREATE_RETRY_DELAY_MS = 500L
        private const val SNAPSHOT_DIRECTORY = "ui-preview"

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
            if (remote.contractVersion != Contract.VERSION || remote.contractHash != Contract.HASH) {
                main.post { unbind(); status = Status.Unavailable(Reason.CONTRACT_MISMATCH, installedHostVersion(context), null) }
                return
            }
            val info = remote.previewInfo
            main.post {
                if (closed) return@post
                service = remote
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

    /** Creates the session when the service and the viewport are ready and no session exists. Main thread. */
    fun ensureSession() {
        if (closed || sessionId != null) return
        val remote = service ?: run { bind(); return }
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return
        val view = surface
        if (embedded && view == null) return
        val request = Bundle().apply {
            putInt(Contract.KEY_CONTRACT_VERSION, Contract.VERSION)
            putString(Contract.KEY_MODE, mode)
            putInt(Contract.KEY_WIDTH_PX, surfaceWidth)
            putInt(Contract.KEY_HEIGHT_PX, surfaceHeight)
            putInt(Contract.KEY_NIGHT_MODE, nightMode)
            putString(Contract.KEY_LOCALE_TAG, localeTag)
            putString(Contract.KEY_LABEL, label)
            putLong(Contract.KEY_CLIENT_VERSION_CODE, clientVersionCode())
            if (embedded && view != null) Embedded.fill(this, view)
        }
        val myGeneration = ++generation
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
                if (closed || myGeneration != generation) {
                    result?.getString(Contract.KEY_SESSION_ID)?.let { id -> worker.execute { runCatching { remote.release(id) } } }
                    return@post
                }
                val id = result?.getString(Contract.KEY_SESSION_ID)
                if (result == null) {
                    onHostGone()
                } else if (id == null && result.getString(Contract.KEY_ERROR_CODE) == Contract.ERROR_TIMEOUT && !retriedCreate) {
                    // A cold host (its first script engine) can outlive the host's wait once; the second attempt finds it warm.
                    retriedCreate = true
                    main.postDelayed({ ensureSession() }, CREATE_RETRY_DELAY_MS)
                } else if (id == null) {
                    status = Status.Unavailable(Reason.UNAVAILABLE, installedHostVersion(context), result.getString(Contract.KEY_ERROR_MESSAGE))
                } else {
                    retriedCreate = false
                    sessionId = id
                    if (embedded) surface?.let { Embedded.attach(it, result) }
                    if (highlighted >= 0) call { it.setHighlight(id, highlighted) }
                    if (pointerMode != Contract.POINTER_MODE_EDIT) call { it.setPointerMode(id, pointerMode) }
                    scheduleFlush(resend = true)
                }
            }
        }
    }

    private fun reattach() {
        val id = sessionId ?: return
        val view = surface ?: return
        if (!embedded) return
        call(onResult = { reattached -> main.post { if (sessionId == id && surface === view) Embedded.attach(view, reattached) } }) { remote -> remote.reattach(id) }
    }

    fun releaseSession() {
        val id = sessionId ?: return
        sessionId = null
        generation++
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
        }
        worker.execute { apply(remote, id, myGeneration, layout, surfaceWidth, surfaceHeight) }
    }

    /** Worker thread: one setLayout round trip, with the snapshot file for the snapshot mode. */
    private fun apply(remote: IUiPreviewService, id: String, myGeneration: Int, layout: Bundle, width: Int, height: Int) {
        var file: File? = null
        var output: ParcelFileDescriptor? = null
        val result = try {
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
        } finally {
            runCatching { output?.close() }
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
                val result = try { block(remote) } catch (error: DeadObjectException) { onHostGone(); null } catch (error: RemoteException) { null } catch (error: SecurityException) { null }
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
