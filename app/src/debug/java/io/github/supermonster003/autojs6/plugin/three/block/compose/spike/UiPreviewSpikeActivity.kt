package io.github.supermonster003.autojs6.plugin.three.block.compose.spike

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceControlViewHost
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import java.io.File
import java.nio.ByteBuffer

/**
 * Roadmap P10.0 spike page (debug builds only): embeds a layout that the spike host inflates with its real `ui`
 * module, through SurfaceControlViewHost (API 30+) or as an offscreen snapshot. Every observation is logged on
 * screen and under the logcat tag [TAG]; nothing here is a contract.
 *
 * Start with `am start -n <plugin>/.spike.UiPreviewSpikeActivity [--es host <package>]`.
 */
class UiPreviewSpikeActivity : ComponentActivity() {

    private val log = mutableStateListOf<String>()
    private var connected by mutableStateOf(false)
    private var sessionId by mutableStateOf<String?>(null)
    private var mode by mutableStateOf(MODE_EMBEDDED)
    private var intercept by mutableStateOf(true)
    private var scaled by mutableStateOf(false)
    private var denseZoom by mutableStateOf(false)
    private var night by mutableStateOf(0)
    private var shrink by mutableStateOf(false)
    private var mediaOverlay by mutableStateOf(true)
    private var surfaceGeneration by mutableStateOf(0)
    private var script by mutableStateOf(DEFAULT_SCRIPT)
    private var bounds by mutableStateOf<List<Bound>>(emptyList())
    private var selected by mutableStateOf(-1)
    private var snapshot by mutableStateOf<ImageBitmap?>(null)
    private var surfaceSize by mutableStateOf(0 to 0)

    private var service: Messenger? = null
    private var surfaceView: SurfaceView? = null
    private var surfacePackage: Parcelable? = null
    private var snapshotSentAt = 0L
    private var hostPackage = SPIKE_HOST_PACKAGE
    private val startedAt = SystemClock.uptimeMillis()

    private val incoming = Messenger(Handler(Looper.getMainLooper()) { message -> onReply(message); true })

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = Messenger(binder)
            connected = true
            note("connected to $name")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            connected = false
            sessionId = null
            note("service disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra("host")?.let { hostPackage = it }
        note("API ${Build.VERSION.SDK_INT}, host package $hostPackage")
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { Screen() } } }
    }

    override fun onStart() {
        super.onStart()
        note("onStart")
    }

    override fun onStop() {
        note("onStop (session kept)")
        super.onStop()
    }

    override fun onDestroy() {
        note("onDestroy: releasing")
        sessionId?.let { send(WHAT_RELEASE, Bundle().apply { putString(KEY_SESSION_ID, it) }) }
        disconnect()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val nightNow = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        note("onConfigurationChanged: night=${nightNow == Configuration.UI_MODE_NIGHT_YES} orientation=${newConfig.orientation}")
        val pkg = surfacePackage ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Embedded.notifyConfigurationChanged(pkg, newConfig)
            note("surfacePackage.notifyConfigurationChanged sent")
        } else {
            note("notifyConfigurationChanged needs API 33")
        }
    }

    // ---------------------------------------------------------------- service

    private fun connect() {
        val intent = Intent(ACTION).setPackage(hostPackage)
        val resolved = packageManager.resolveService(intent, 0)
        note("resolveService -> ${resolved?.serviceInfo?.name}")
        val bound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
        note("bindService -> $bound")
    }

    private fun disconnect() {
        if (service != null) {
            runCatching { unbindService(connection) }
            service = null
            connected = false
            sessionId = null
            note("unbound")
        }
    }

    private fun send(what: Int, data: Bundle) {
        val target = service ?: run { note("not connected"); return }
        try {
            target.send(Message.obtain(null, what).also { it.data = data; it.replyTo = incoming })
        } catch (e: Exception) {
            note("send $what failed: $e")
        }
    }

    private fun createSession() {
        val view = surfaceView
        val data = Bundle().apply {
            putString(KEY_MODE, mode)
            putInt(KEY_WIDTH, surfaceSize.first)
            putInt(KEY_HEIGHT, surfaceSize.second)
            putInt(KEY_DISPLAY_ID, view?.display?.displayId ?: 0)
            putInt(KEY_NIGHT_MODE, night)
            putInt(KEY_DENSITY_DPI, if (denseZoom) (resources.configuration.densityDpi * 1.5f).toInt() else 0)
            putString(KEY_SCRIPT, script)
            if (mode == MODE_EMBEDDED && view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                val token = view.hostToken
                note("hostToken=${token != null} display=${view.display?.displayId}")
                putBinder(KEY_HOST_TOKEN, token)
                if (Build.VERSION.SDK_INT >= 35) {
                    putParcelable(KEY_INPUT_TRANSFER_TOKEN, Embedded.inputTransferToken(view))
                    note("inputTransferToken attached (API 35 path)")
                }
            }
        }
        bounds = emptyList()
        selected = -1
        note("create: mode=$mode size=${surfaceSize.first}x${surfaceSize.second}")
        send(WHAT_CREATE, data)
    }

    private fun withSession(block: (String) -> Unit) {
        val id = sessionId ?: run { note("no session"); return }
        block(id)
    }

    private fun requestSnapshot() = withSession { id ->
        val file = File(cacheDir, "spike-snapshot.raw")
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE)
        snapshotSentAt = SystemClock.elapsedRealtime()
        send(WHAT_SNAPSHOT, Bundle().apply { putString(KEY_SESSION_ID, id); putParcelable(KEY_FD, fd) })
        fd.close()
    }

    private fun onReply(message: Message) {
        val data = message.data ?: Bundle()
        when (message.what) {
            WHAT_CREATED -> {
                sessionId = data.getString(KEY_SESSION_ID)
                @Suppress("DEPRECATION")
                val pkg: Parcelable? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data.getParcelable(KEY_SURFACE_PACKAGE, Parcelable::class.java)
                } else {
                    data.getParcelable(KEY_SURFACE_PACKAGE)
                }
                note("created session $sessionId, surfacePackage=${pkg != null}")
                surfacePackage = pkg
                if (pkg != null) attachPackage(pkg)
                sessionId?.let { send(WHAT_SET_INTERCEPT, Bundle().apply { putString(KEY_SESSION_ID, it); putBoolean(KEY_INTERCEPT, intercept) }) }
            }
            WHAT_BOUNDS -> {
                val array = JSONArray(data.getString(KEY_BOUNDS_JSON) ?: "[]")
                bounds = (0 until array.length()).map { i ->
                    val o = array.getJSONObject(i)
                    Bound(o.getInt("i"), o.getInt("l"), o.getInt("t"), o.getInt("r"), o.getInt("b"), o.getString("c"))
                }
                note("bounds: ${bounds.size} views: " + bounds.joinToString(" ") { "${it.index}:${it.className}(${it.left},${it.top},${it.right},${it.bottom})" })
            }
            WHAT_SNAPSHOT_DONE -> {
                val received = SystemClock.elapsedRealtime()
                val width = data.getInt(KEY_WIDTH)
                val height = data.getInt(KEY_HEIGHT)
                val file = File(cacheDir, "spike-snapshot.raw")
                val bytes = file.readBytes()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
                snapshot = bitmap.asImageBitmap()
                val expected = data.getLong(KEY_BYTES)
                note(
                    "snapshot ${width}x$height: ${bytes.size} B (host wrote $expected), sha=${data.getString(KEY_SHA256)?.take(12)}, " +
                        "host draw ${data.getLong(KEY_DRAW_MS)} ms, host total ${data.getLong(KEY_ELAPSED_MS)} ms, round trip ${received - snapshotSentAt} ms",
                )
            }
            WHAT_LOG -> note("host: ${data.getString(KEY_TEXT)}")
            WHAT_REATTACHED -> {
                @Suppress("DEPRECATION")
                val pkg: Parcelable? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) data.getParcelable(KEY_SURFACE_PACKAGE, Parcelable::class.java) else data.getParcelable(KEY_SURFACE_PACKAGE)
                note("reattached: new surfacePackage=${pkg != null}")
                surfacePackage = pkg
                if (pkg != null) attachPackage(pkg)
            }
            WHAT_TAP -> {
                val index = data.getInt(KEY_ELEMENT_INDEX, -1)
                selected = index
                note("host tap ${data.getInt(KEY_X)},${data.getInt(KEY_Y)} -> $index ${data.getString(KEY_TEXT)}")
                withSession { send(WHAT_HIGHLIGHT, Bundle().apply { putString(KEY_SESSION_ID, it); putInt(KEY_ELEMENT_INDEX, index) }) }
            }
            else -> note("reply ${message.what}")
        }
    }

    private fun attachPackage(pkg: Parcelable) {
        val view = surfaceView ?: run { note("no SurfaceView yet"); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Embedded.attach(view, pkg)
            note("setChildSurfacePackage done (surface valid=${view.holder.surface?.isValid})")
        }
    }

    /** Every reference to SurfaceControlViewHost lives here, so the page loads on API 24 - 29 (snapshot mode). */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private object Embedded {
        fun attach(view: SurfaceView, pkg: Parcelable) = view.setChildSurfacePackage(pkg as SurfaceControlViewHost.SurfacePackage)

        @androidx.annotation.RequiresApi(35)
        fun inputTransferToken(view: SurfaceView): Parcelable? = view.rootSurfaceControl?.inputTransferToken

        @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun notifyConfigurationChanged(pkg: Parcelable, config: Configuration) =
            (pkg as SurfaceControlViewHost.SurfacePackage).notifyConfigurationChanged(config)
    }

    private fun note(text: String) {
        val line = "%6d %s".format(SystemClock.uptimeMillis() - startedAt, text)
        Log.i(TAG, line)
        log += line
        if (log.size > 400) log.removeAt(0)
    }

    // ---------------------------------------------------------------- ui

    @Composable
    private fun Screen() {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Text("P10.0 ui preview spike: ${if (connected) "connected" else "disconnected"}, session ${sessionId ?: "-"}", fontSize = 12.sp)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Button(onClick = { if (connected) disconnect() else connect() }, Modifier.padding(end = 4.dp)) { Text(if (connected) "Unbind" else "Bind") }
                Button(onClick = { createSession() }, Modifier.padding(end = 4.dp)) { Text("Create") }
                Button(onClick = { withSession { send(WHAT_RUN, Bundle().apply { putString(KEY_SESSION_ID, it); putString(KEY_SCRIPT, script) }) } }, Modifier.padding(end = 4.dp)) { Text("Run") }
                Button(onClick = { requestSnapshot() }, Modifier.padding(end = 4.dp)) { Text("Snapshot") }
                Button(onClick = { withSession { send(WHAT_RELEASE, Bundle().apply { putString(KEY_SESSION_ID, it) }); sessionId = null; surfacePackage = null; bounds = emptyList() } }) { Text("Release") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(selected = mode == MODE_EMBEDDED, onClick = { mode = if (mode == MODE_EMBEDDED) MODE_SNAPSHOT else MODE_EMBEDDED }, label = { Text(mode) }, modifier = Modifier.padding(end = 4.dp))
                FilterChip(selected = intercept, onClick = { intercept = !intercept; note("overlay intercept=$intercept"); withSession { send(WHAT_SET_INTERCEPT, Bundle().apply { putString(KEY_SESSION_ID, it); putBoolean(KEY_INTERCEPT, intercept) }) } }, label = { Text("intercept") }, modifier = Modifier.padding(end = 4.dp))
                FilterChip(selected = mediaOverlay, onClick = { mediaOverlay = !mediaOverlay; surfaceGeneration++; note("SurfaceView zOrderOnTop=$mediaOverlay (recreated)") }, label = { Text("overlay z") }, modifier = Modifier.padding(end = 4.dp))
                FilterChip(selected = scaled, onClick = { scaled = !scaled; note("SurfaceView scale=${if (scaled) 1.5f else 1f}") }, label = { Text("scale 1.5") }, modifier = Modifier.padding(end = 4.dp))
                FilterChip(
                    selected = denseZoom,
                    onClick = {
                        denseZoom = !denseZoom
                        withSession { send(WHAT_SET_CONFIG, Bundle().apply { putString(KEY_SESSION_ID, it); putInt(KEY_DENSITY_DPI, if (denseZoom) (resources.configuration.densityDpi * 1.5f).toInt() else 0); putInt(KEY_NIGHT_MODE, night) }) }
                    },
                    label = { Text("density 1.5") },
                    modifier = Modifier.padding(end = 4.dp),
                )
                FilterChip(
                    selected = night == 2,
                    onClick = {
                        night = if (night == 2) 0 else 2
                        withSession { send(WHAT_SET_CONFIG, Bundle().apply { putString(KEY_SESSION_ID, it); putInt(KEY_DENSITY_DPI, if (denseZoom) (resources.configuration.densityDpi * 1.5f).toInt() else 0); putInt(KEY_NIGHT_MODE, night) }) }
                    },
                    label = { Text("night") },
                    modifier = Modifier.padding(end = 4.dp),
                )
                FilterChip(selected = shrink, onClick = { shrink = !shrink; note("viewport shrink=$shrink") }, label = { Text("shrink") })
            }
            OutlinedTextField(
                value = script,
                onValueChange = { script = it },
                modifier = Modifier.fillMaxWidth().height(96.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(if (shrink) 0.7f else 1f).fillMaxHeight(if (shrink) 0.3f else 0.45f).background(Color(0xFFEEEEEE))) {
                if (mode == MODE_EMBEDDED) {
                    androidx.compose.runtime.key(surfaceGeneration) {
                    AndroidView(
                        factory = { context -> SurfaceView(context).also { view -> surfaceView = view; view.setZOrderOnTop(mediaOverlay); view.holder.addCallback(surfaceCallback) } },
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val scale = if (scaled) 1.5f else 1f
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                    )
                    }
                } else {
                    snapshot?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
                    SnapshotSizer()
                }
                Overlay()
            }
            LazyColumn(Modifier.fillMaxSize().padding(top = 4.dp), reverseLayout = true) {
                items(log.asReversed()) { Text(it, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 12.sp) }
            }
        }
    }

    /** In snapshot mode the viewport size comes from this measured box instead of a surface. */
    @Composable
    private fun SnapshotSizer() {
        AndroidView(factory = { context ->
            object : android.view.View(context) {
                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    onViewportSize(w, h)
                }
            }
        }, modifier = Modifier.fillMaxSize())
    }

    @Composable
    private fun Overlay() {
        val current = bounds
        Canvas(
            Modifier.fillMaxSize().pointerInput(intercept, current) {
                if (!intercept) return@pointerInput
                detectTapGestures { offset ->
                    val hit = current.filter { it.contains(offset) }.minByOrNull { it.area }
                    selected = hit?.index ?: -1
                    note("tap ${offset.x.toInt()},${offset.y.toInt()} -> ${hit?.let { "${it.index} ${it.className}" } ?: "nothing"}")
                    withSession { send(WHAT_HIGHLIGHT, Bundle().apply { putString(KEY_SESSION_ID, it); putInt(KEY_ELEMENT_INDEX, hit?.index ?: -1) }) }
                }
            },
        ) {
            if (!intercept && selected < 0) return@Canvas
            current.forEach { bound ->
                val color = if (bound.index == selected) Color(0xFFE53935) else Color(0x663F51B5)
                drawRect(color, Offset(bound.left.toFloat(), bound.top.toFloat()), Size((bound.right - bound.left).toFloat(), (bound.bottom - bound.top).toFloat()), style = Stroke(if (bound.index == selected) 4f else 2f))
            }
        }
    }

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            note("surfaceCreated")
            val id = sessionId
            if (id != null && surfacePackage != null) {
                note("surface recreated: asking the host for a new package")
                send(WHAT_REATTACH, Bundle().apply { putString(KEY_SESSION_ID, id) })
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            val location = IntArray(2).also { surfaceView?.getLocationOnScreen(it) }
            note("surfaceChanged ${width}x$height at screen ${location[0]},${location[1]} zOrderOnTop=$mediaOverlay")
            onViewportSize(width, height)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            note("surfaceDestroyed")
        }
    }

    private fun onViewportSize(width: Int, height: Int) {
        val changed = surfaceSize != (width to height)
        surfaceSize = width to height
        if (changed) sessionId?.let { send(WHAT_RESIZE, Bundle().apply { putString(KEY_SESSION_ID, it); putInt(KEY_WIDTH, width); putInt(KEY_HEIGHT, height) }) }
    }

    private data class Bound(val index: Int, val left: Int, val top: Int, val right: Int, val bottom: Int, val className: String) {
        val area get() = (right - left).toLong() * (bottom - top)
        fun contains(offset: Offset) = offset.x >= left && offset.x < right && offset.y >= top && offset.y < bottom
    }

    companion object {
        private const val TAG = "UiPreviewSpike"
        const val SPIKE_HOST_PACKAGE = "org.autojs.autojs6.compose.spike"
        const val ACTION = "org.autojs.autojs6.action.UI_PREVIEW_SPIKE"

        const val WHAT_CREATE = 1
        const val WHAT_RUN = 2
        const val WHAT_RESIZE = 3
        const val WHAT_SET_CONFIG = 4
        const val WHAT_SNAPSHOT = 5
        const val WHAT_RELEASE = 6
        const val WHAT_SET_INTERCEPT = 7
        const val WHAT_HIGHLIGHT = 8
        const val WHAT_REATTACH = 9
        const val WHAT_CREATED = 101
        const val WHAT_BOUNDS = 102
        const val WHAT_SNAPSHOT_DONE = 103
        const val WHAT_LOG = 104
        const val WHAT_TAP = 105
        const val WHAT_REATTACHED = 106

        const val MODE_EMBEDDED = "embedded"
        const val MODE_SNAPSHOT = "snapshot"

        const val KEY_SESSION_ID = "sessionId"
        const val KEY_MODE = "mode"
        const val KEY_HOST_TOKEN = "hostToken"
        const val KEY_INPUT_TRANSFER_TOKEN = "inputTransferToken"
        const val KEY_DISPLAY_ID = "displayId"
        const val KEY_WIDTH = "widthPx"
        const val KEY_HEIGHT = "heightPx"
        const val KEY_DENSITY_DPI = "densityDpi"
        const val KEY_NIGHT_MODE = "nightMode"
        const val KEY_SCRIPT = "script"
        const val KEY_FD = "fd"
        const val KEY_SURFACE_PACKAGE = "surfacePackage"
        const val KEY_BOUNDS_JSON = "boundsJson"
        const val KEY_TEXT = "text"
        const val KEY_BYTES = "bytes"
        const val KEY_SHA256 = "sha256"
        const val KEY_DRAW_MS = "drawMs"
        const val KEY_ELAPSED_MS = "elapsedMs"
        const val KEY_INTERCEPT = "intercept"
        const val KEY_ELEMENT_INDEX = "elementIndex"
        const val KEY_X = "x"
        const val KEY_Y = "y"

        val DEFAULT_SCRIPT = """
            var msg = "preview";
            ui.layout(
                <vertical padding="8">
                    <toolbar id="bar" title="Spike"/>
                    <text id="title" text="Hello {{msg}}" textSize="24sp"/>
                    <input id="name" hint="type here" w="*"/>
                    <spinner id="sp1" entries="A|B|C" w="*"/>
                    <spinner id="sp2" entries="D|E|F" spinnerMode="dialog" w="*"/>
                    <horizontal>
                        <button id="btn" text="Toast"/>
                        <button id="dlg" text="Dialog"/>
                        <button id="cnt" text="0"/>
                    </horizontal>
                    <list id="list" h="100">
                        <horizontal padding="8"><text text="{{name}}" textSize="16sp"/></horizontal>
                    </list>
                    <webview id="web" w="*" h="100"/>
                </vertical>
            );
            var count = 0;
            ui.btn.on('click', () => toast('clicked on ' + (isUiThread() ? 'ui thread' : 'script thread')));
            ui.dlg.on('click', () => dialogs.alert('Hello from the preview'));
            ui.cnt.on('click', () => ui.cnt.attr('text', String(++count)));
            ui.list.setDataSource([{ name: 'alpha' }, { name: 'beta' }, { name: 'gamma' }]);
            ui.web.loadData('<html><body style="background:#cde">webview</body></html>', 'text/html', 'utf-8');
            setInterval(() => {}, 60000);
        """.trimIndent()
    }
}
