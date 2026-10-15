package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryActivity
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptLauncher
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.AttrPart
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DocumentReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ReadResult
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsxParserClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.CanvasBounds
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.DesignCanvas
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.DesignImages
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.UiPreviewClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.XmlCanvas
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.XmlPreviewFrames
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.XmlPreviewRequest
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.XmlPreviewValues
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.elementLabel
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.DeclarationEvaluator
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.sandbox.SandboxPreview
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.DocumentIo
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.DocumentSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.SourceSync
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.plugin.compose.api.model.ThemeSpec

/**
 * The editing screen (roadmap P8.3 / D36 / D40): the real-renderer canvas on top, an outline / inspector
 * panel below, a palette sheet for inserting components, undo / redo, explicit save and automatic drafts.
 * Roadmap P8.4 / D39: the document is either a private project (`EXTRA_PROJECT`) or an external script
 * handed over through a content / file Uri (EDIT / VIEW); saving writes back through that Uri when the
 * grant allows it and otherwise keeps the text as a new private project. External changes made while the
 * screen was away (the host editor saving the same file) are reloaded on resume, or offered when there
 * are unsaved edits here. Runs in the plugin's own process; nothing here executes a script.
 */
internal class DesignerActivity : StandaloneActivity() {
    private lateinit var store: ProjectStore
    private lateinit var io: DocumentIo
    private lateinit var sync: SourceSync
    /** Reads this screen's document in its language; a TSX reader holds the TypeScript Engine parser binding. */
    private lateinit var reader: DocumentReader
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var source: DocumentSource by mutableStateOf(DocumentSource.Local(""))
    private var session: DesignerSession? by mutableStateOf(null)
    /** The open document's editing state, once it is read (device performance tests drive it directly). */
    internal val currentSession: DesignerSession? get() = session
    /** The editing state of a `"ui";` mode document (roadmap P10.3); exclusive with [session]. */
    private var uiSession: UiDesignerSession? by mutableStateOf(null)
    internal val currentUiSession: UiDesignerSession? get() = uiSession
    /** The host preview client of the ui document, created with its session and closed with this screen. */
    private var previewClient: UiPreviewClient? = null
    internal val currentPreviewClient: UiPreviewClient? get() = previewClient
    private var parseError: DesignerParseError? by mutableStateOf(null)
    private var readFailed by mutableStateOf(false)
    /** A TSX document is being parsed by the TypeScript Engine (roadmap P9.2). */
    private var loading by mutableStateOf(false)
    /** A TSX document the parser cannot read right now, shown as read-only source (roadmap D42). */
    private var readOnly: ReadOnlySource? by mutableStateOf(null)
    /** The text last loaded from or written to the document; the reference for external-change detection. */
    private var baseline: String? = null
    private var pendingDraft: String? by mutableStateOf(null)
    private var externalChange: String? by mutableStateOf(null)

    private data class ReadOnlySource(val text: String, val result: ReadResult.Unavailable)

    override fun onCreate(savedInstanceState: Bundle?) {
        store = ProjectStore(this)
        io = DocumentIo(this, store)
        sync = SourceSync(contentResolver, store, io)
        val projectName = intent.getStringExtra(EXTRA_PROJECT)?.takeIf { ProjectStore.isValidName(it) }
        val resolved: DocumentSource? = when {
            projectName != null -> DocumentSource.Local(projectName).takeIf { store.exists(projectName) }
            else -> io.external(intent)
        }
        if (resolved == null) { finish(); super.onCreate(savedInstanceState); return }
        source = resolved
        val dialect = Dialect.of(resolved.title)
        reader = DocumentReader(dialect, if (dialect == Dialect.TSX) TsxParserClient(this) else null) { cause ->
            getString(R.string.designer_tsx_check_unavailable, cause)
        }
        val text = io.readOrNull(resolved)
        if (text == null) {
            readFailed = true
        } else {
            pendingDraft = io.pendingDraft(resolved)
            load(text)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        scope.cancel()
        previewClient?.close()
        previewClient = null
        if (::reader.isInitialized) reader.close()
        super.onDestroy()
    }

    /** Reads [text] in this document's language: at once for JavaScript, through the parser service for TSX. */
    private fun read(text: String, onResult: (ReadResult) -> Unit) {
        reader.readNow(text)?.let { onResult(it); return }
        scope.launch { onResult(reader.read(text)) }
    }

    private fun load(script: String) {
        loading = reader.dialect == Dialect.TSX && session == null
        read(script) { result ->
            loading = false
            when (result) {
                is ReadResult.Parsed -> {
                    session = DesignerSession(result.document, syntax = reader)
                    uiSession = null
                    parseError = null
                    readOnly = null
                    baseline = script
                }
                is ReadResult.ParsedUi -> {
                    // Roadmap P10.3: a ui-mode document gets its own session and a host preview client.
                    session = null
                    uiSession = UiDesignerSession(result.document)
                    previewClient?.close()
                    previewClient = UiPreviewClient(this, source.title)
                    parseError = null
                    readOnly = null
                    baseline = script
                }
                is ReadResult.Rejected -> { parseError = result.error; readOnly = null }
                is ReadResult.Unavailable -> readOnly = ReadOnlySource(script, result)
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        // A TSX document shown read-only is tried again: the TypeScript Engine may have been installed meanwhile.
        if (readOnly != null && !loading) io.readOrNull(source)?.let { load(it) }
    }

    override fun onResume() {
        super.onResume()
        val dirty = session?.dirty ?: uiSession?.dirty ?: return
        val onDisk = io.readOrNull(source) ?: return
        if (baseline == null || onDisk == baseline) return
        if (!dirty) reloadFrom(onDisk) else externalChange = onDisk
    }

    private fun reloadFrom(script: String) {
        read(script) { result ->
            // An unmappable or unreadable script from the other editor, or one of the other kind, keeps the tree
            // here; an explicit save decides.
            val current = session
            val ui = uiSession
            when {
                result is ReadResult.Parsed && current != null -> {
                    current.replace(result.document)
                    current.markSaved()
                    baseline = script
                }
                result is ReadResult.ParsedUi && ui != null -> {
                    ui.replace(result.document)
                    baseline = script
                }
            }
        }
    }

    override fun onStop() {
        session?.let { if (it.dirty) io.writeDraft(source, it.script()) }
        uiSession?.let { if (it.dirty) io.writeDraft(source, it.script()) }
        super.onStop()
    }

    @Composable
    override fun Content(palette: StandalonePalette) {
        val error = parseError
        val current = session
        val ui = uiSession
        val unavailable = readOnly
        when {
            ui != null && !readFailed && error == null -> UiEditorScreen(ui, palette)
            current != null && !readFailed && error == null -> Editor(current, palette)
            loading -> LoadingScreen(palette)
            unavailable != null && !readFailed -> ReadOnlyScreen(unavailable, palette)
            else -> ParseErrorScreen(error, palette)
        }
    }

    @Composable
    private fun LoadingScreen(palette: StandalonePalette) {
        Box(Modifier.fillMaxSize().background(Color(palette.background)).testTag("designer-loading"), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(palette.accent))
        }
    }

    /** Roadmap D42: without a usable TypeScript Engine parser a TSX document can be read and copied, not edited. */
    @Composable
    private fun ReadOnlyScreen(state: ReadOnlySource, palette: StandalonePalette) {
        val context = LocalContext.current
        val copied = stringResource(R.string.gallery_copied)
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val result = state.result
        val message = when (result.reason) {
            TsxParserClient.Reason.NOT_INSTALLED -> stringResource(R.string.designer_tsx_engine_missing, TsxParserClient.MIN_VERSION_NAME)
            TsxParserClient.Reason.TOO_OLD -> stringResource(R.string.designer_tsx_engine_old, TsxParserClient.MIN_VERSION_NAME, result.installedVersion ?: "?")
            TsxParserClient.Reason.UNAVAILABLE -> stringResource(R.string.designer_tsx_parser_unavailable, result.installedVersion ?: "?")
            null -> stringResource(R.string.designer_tsx_parser_unavailable, result.detail ?: "?")
        }
        Scaffold(containerColor = Color(palette.background), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(source.title, color = Color(palette.text), fontSize = 18.sp)
                Text(message, color = Color(palette.danger), fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp).testTag("designer-readonly-message"))
                Row {
                    TextButton(onClick = {
                        if (ScriptLauncher.copy(context, source.title, state.text)) scope.launch { snackbar.showSnackbar(copied) }
                    }, modifier = Modifier.testTag("designer-readonly-copy")) { Text(stringResource(R.string.designer_copy_script)) }
                    TextButton(onClick = { io.readOrNull(source)?.let { load(it) } }, modifier = Modifier.testTag("designer-readonly-retry")) {
                        Text(stringResource(R.string.designer_retry))
                    }
                    TextButton(onClick = { finish() }) { Text(stringResource(R.string.designer_cancel)) }
                }
                SelectionContainer(Modifier.fillMaxWidth().weight(1f).background(Color(palette.surfaceVariant)).testTag("designer-readonly-source")) {
                    Text(
                        state.text,
                        color = Color(palette.text),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).padding(8.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun ParseErrorScreen(error: DesignerParseError?, palette: StandalonePalette) {
        Column(Modifier.fillMaxSize().background(Color(palette.background)).padding(24.dp)) {
            Text(source.title, color = Color(palette.text), fontSize = 18.sp)
            Text(
                when {
                    readFailed -> stringResource(R.string.designer_external_read_failed)
                    error != null -> stringResource(R.string.designer_parse_error, error.line, error.column, error.reason)
                    else -> ""
                },
                color = Color(palette.danger),
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 16.dp).testTag("designer-parse-error"),
            )
            Button(onClick = { finish() }) { Text(stringResource(R.string.designer_cancel)) }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Editor(session: DesignerSession, palette: StandalonePalette) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val bounds = remember { CanvasBounds() }
        var tab by remember { mutableStateOf(0) }
        var paletteTarget by remember { mutableStateOf<String?>(null) }
        var paletteOpen by remember { mutableStateOf(false) }
        var menuOpen by remember { mutableStateOf(false) }
        var hostDialog by remember { mutableStateOf(false) }
        var issuesDialog by remember { mutableStateOf(false) }
        var exitDialog by remember { mutableStateOf(false) }
        var zoom by remember { mutableStateOf(1f) }
        var syncPrompt by remember { mutableStateOf<SyncPrompt?>(null) }
        var relinkAction by remember { mutableStateOf<SyncAction?>(null) }
        var imageFolderRevision by remember { mutableStateOf(0) }
        val images = remember(source.draftKey, imageFolderRevision) { DesignImages(context, store.imageFolderOf(source.draftKey)) }
        val savedText = stringResource(R.string.designer_saved)
        val savedAsProject = stringResource(R.string.designer_external_readonly_saved)
        val insertFailed = stringResource(R.string.designer_insert_failed)
        val copied = stringResource(R.string.gallery_copied)
        val missingHost = stringResource(R.string.gallery_run_missing_host)
        val runFailed = stringResource(R.string.gallery_run_failed)
        val hostMissing = stringResource(R.string.designer_host_missing)
        val hostFailed = stringResource(R.string.designer_host_failed)
        val exported = stringResource(R.string.designer_exported)
        val exportFailed = stringResource(R.string.designer_export_failed)
        val sourceWritten = stringResource(R.string.designer_source_written)
        val sourceWriteFailed = stringResource(R.string.designer_source_write_failed)
        val sourceReimported = stringResource(R.string.designer_source_reimported)
        val sourceParseFailed = stringResource(R.string.designer_source_parse_failed)
        val imageFolderSet = stringResource(R.string.designer_image_folder_set)
        val theme = remember(appearance) { ThemeSpec(seedArgb = appearance.primary, dark = appearance.dark) }
        val errors = session.diagnostics.count { it.severity == Severity.ERROR }
        // Roadmap P9.3 step 2 (D43): the interactive preview of a JavaScript project runs its script in the sandbox,
        // started afresh whenever the document or a preview value changes, and paused while the designer is hidden.
        // A TSX project (and any project below Android 8.0) keeps the step 1 preview, whose controls set the preview values.
        val sandbox = remember { SandboxPreview() }
        val runsScript = session.document.dialect == Dialect.JS && SandboxPreview.SUPPORTED
        var visible by remember { mutableStateOf(true) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, _ -> visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer); sandbox.stop() }
        }
        LaunchedEffect(session.interactive, runsScript, visible, session.document, session.previewValues) {
            if (session.interactive && runsScript && visible) {
                // Typing in the inspector restarts the script once the edits pause, not on every keystroke.
                if (sandbox.running) delay(SANDBOX_RESTART_DELAY_MS)
                sandbox.start(session.document, session.previewValues, source.title)
            } else sandbox.stop()
        }
        val liveTree = if (session.interactive && runsScript) sandbox.tree else null
        val sandboxProblems = sandbox.failure?.takeIf { session.interactive && runsScript }
            ?.let { listOf(stringResource(if (it.stops) R.string.designer_sandbox_stopped else R.string.designer_sandbox_error, it.message)) }.orEmpty()

        fun save(announce: Boolean = true): Boolean {
            val text = session.script()
            val previous = source
            val result = io.write(previous, text)
            io.clearDraft(previous)
            baseline = text
            session.markSaved()
            when (result) {
                is DocumentIo.WriteResult.Saved -> if (announce) scope.launch { snackbar.showSnackbar(savedText) }
                is DocumentIo.WriteResult.SavedAsProject -> {
                    source = DocumentSource.Local(result.name)
                    scope.launch { snackbar.showSnackbar(savedAsProject.format(result.name)) }
                }
            }
            return true
        }

        fun sendToHost(intent: (Uri, Boolean) -> Intent) {
            if (session.dirty) save()
            val (uri, writable) = io.shareUri(source)
            when (ScriptLauncher.start(context, intent(uri, writable))) {
                ScriptLauncher.Result.STARTED -> Unit
                ScriptLauncher.Result.HOST_MISSING -> scope.launch { snackbar.showSnackbar(hostMissing) }
                ScriptLauncher.Result.FAILED -> scope.launch { snackbar.showSnackbar(hostFailed) }
            }
        }

        // Roadmap P9.1 / D45: an imported project syncs with its source only on request, guarded by the last sync.
        fun writeBack(name: String, confirmed: Boolean) {
            if (session.dirty) save(announce = false)
            when (val state = sync.state(name)) {
                SourceSync.State.None -> Unit
                is SourceSync.State.Unavailable -> syncPrompt = SyncPrompt.Unavailable(SyncAction.WRITE_BACK)
                is SourceSync.State.Ready -> if (state.sourceChanged && !confirmed) syncPrompt = SyncPrompt.Overwrite else {
                    val written = sync.writeBack(name, session.script())
                    scope.launch { snackbar.showSnackbar(if (written) sourceWritten else sourceWriteFailed) }
                }
            }
        }

        fun reimport(name: String, confirmed: Boolean) {
            when (val state = sync.state(name)) {
                SourceSync.State.None -> Unit
                is SourceSync.State.Unavailable -> syncPrompt = SyncPrompt.Unavailable(SyncAction.REIMPORT)
                is SourceSync.State.Ready -> if ((state.projectChanged || session.dirty) && !confirmed) syncPrompt = SyncPrompt.Replace else {
                    read(state.sourceText) { result ->
                        if (result !is ReadResult.Parsed) {
                            scope.launch { snackbar.showSnackbar(sourceParseFailed) }
                        } else {
                            session.replace(result.document)
                            sync.reimported(name, state.sourceText)
                            baseline = state.sourceText
                            io.clearDraft(source)
                            scope.launch { snackbar.showSnackbar(sourceReimported) }
                        }
                    }
                }
            }
        }

        val relinker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val action = relinkAction
            relinkAction = null
            val name = (source as? DocumentSource.Local)?.name
            if (uri == null || action == null || name == null) return@rememberLauncherForActivityResult
            sync.relink(name, uri)
            when (action) {
                SyncAction.WRITE_BACK -> writeBack(name, confirmed = false)
                SyncAction.REIMPORT -> reimport(name, confirmed = false)
            }
        }

        // Roadmap P9.1 / D44: relative Image paths of this document resolve in a folder the user grants.
        val imageFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (error: SecurityException) {
                // Not persistable (a provider without tree grants): the folder works until the grant lapses.
            }
            store.setImageFolder(source.draftKey, uri)
            imageFolderRevision++
            val name = uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?.ifBlank { null } ?: uri.toString()
            scope.launch { snackbar.showSnackbar(imageFolderSet.format(name)) }
        }

        val exportType = if (reader.dialect == Dialect.TSX) ScriptLauncher.TSX_EXPORT_MIME_TYPE else ScriptLauncher.SCRIPT_MIME_TYPE
        val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(exportType)) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(session.script().toByteArray(StandardCharsets.UTF_8)) } != null
            }.getOrDefault(false)
            scope.launch { snackbar.showSnackbar(if (ok) exported else exportFailed) }
        }

        LaunchedEffect(session.revision) {
            if (session.dirty) {
                delay(1000)
                // Discarding on exit clears the dirty flag; a pause that ends just then must not bring the draft back.
                if (session.dirty) io.writeDraft(source, session.script())
            }
        }
        BackHandler(enabled = session.dirty) { exitDialog = true }

        Scaffold(
            containerColor = Color(palette.background),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(source.title + if (session.dirty) " *" else "", fontSize = 16.sp, maxLines = 1) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(palette.background), titleContentColor = Color(palette.text)),
                    navigationIcon = {
                        IconButton(onClick = { if (session.dirty) exitDialog = true else finish() }, modifier = Modifier.testTag("designer-back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(palette.muted))
                        }
                    },
                    actions = {
                        TextButton(onClick = { session.undo() }, enabled = session.canUndo, modifier = Modifier.testTag("designer-undo")) { Text(stringResource(R.string.designer_undo)) }
                        TextButton(onClick = { session.redo() }, enabled = session.canRedo, modifier = Modifier.testTag("designer-redo")) { Text(stringResource(R.string.designer_redo)) }
                        IconButton(onClick = { save() }, modifier = Modifier.testTag("designer-save")) {
                            Icon(Icons.Filled.Done, contentDescription = stringResource(R.string.designer_save), tint = Color(if (session.dirty) palette.accent else palette.muted))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("designer-menu")) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.designer_host_options), tint = Color(palette.muted))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_host_options)) }, onClick = { menuOpen = false; hostDialog = true }, modifier = Modifier.testTag("designer-menu-host"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_issues_count, errors)) }, onClick = { menuOpen = false; issuesDialog = true }, modifier = Modifier.testTag("designer-menu-issues"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_zoom, if (zoom < 1f) "100%" else "75%")) }, onClick = { menuOpen = false; zoom = if (zoom < 1f) 1f else 0.75f })
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_copy_script)) }, onClick = {
                                    menuOpen = false
                                    if (ScriptLauncher.copy(context, source.title, session.script())) scope.launch { snackbar.showSnackbar(copied) }
                                }, modifier = Modifier.testTag("designer-menu-copy"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.gallery_run)) }, onClick = {
                                    menuOpen = false
                                    // Roadmap P9.2: a TSX project runs from its file, so the host compiles it as TSX.
                                    if (reader.dialect == Dialect.TSX) sendToHost { uri, _ -> ScriptLauncher.runIntent(uri) }
                                    else when (ScriptLauncher.run(context, session.script())) {
                                        ScriptLauncher.Result.STARTED -> Unit
                                        ScriptLauncher.Result.HOST_MISSING -> scope.launch { snackbar.showSnackbar(missingHost) }
                                        ScriptLauncher.Result.FAILED -> scope.launch { snackbar.showSnackbar(runFailed) }
                                    }
                                }, modifier = Modifier.testTag("designer-menu-run"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_open_in_host)) }, onClick = {
                                    menuOpen = false
                                    sendToHost { uri, writable -> ScriptLauncher.editIntent(uri, writable) }
                                }, modifier = Modifier.testTag("designer-menu-edit-in-host"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_import_to_host)) }, onClick = {
                                    menuOpen = false
                                    sendToHost { uri, _ -> ScriptLauncher.importIntent(uri) }
                                }, modifier = Modifier.testTag("designer-menu-import-to-host"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_export)) }, onClick = {
                                    menuOpen = false
                                    exporter.launch(source.title)
                                }, modifier = Modifier.testTag("designer-menu-export"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_image_folder)) }, onClick = {
                                    menuOpen = false
                                    imageFolderPicker.launch(store.imageFolderOf(source.draftKey))
                                }, modifier = Modifier.testTag("designer-menu-image-folder"))
                                val linked = (source as? DocumentSource.Local)?.name?.takeIf { menuOpen && store.sourceOf(it) != null }
                                if (linked != null) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.designer_source_write_back)) }, onClick = {
                                        menuOpen = false
                                        writeBack(linked, confirmed = false)
                                    }, modifier = Modifier.testTag("designer-menu-write-back"))
                                    DropdownMenuItem(text = { Text(stringResource(R.string.designer_source_reimport)) }, onClick = {
                                        menuOpen = false
                                        reimport(linked, confirmed = false)
                                    }, modifier = Modifier.testTag("designer-menu-reimport"))
                                }
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_open_gallery)) }, onClick = { menuOpen = false; context.startActivity(Intent(context, GalleryActivity::class.java)) })
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { paletteTarget = null; paletteOpen = true }, modifier = Modifier.testTag("designer-add")) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.designer_add_component))
                }
            },
        ) { padding ->
            val canvas: @Composable (Modifier) -> Unit = { placement ->
                Box(placement.background(Color(palette.surfaceVariant)).testTag("designer-canvas")) {
                    DesignCanvas(
                        document = session.document,
                        images = images,
                        theme = theme,
                        zoom = zoom,
                        selectedId = session.selectedId,
                        dropTargetId = session.dropTargetId,
                        dropLine = session.dropLine,
                        bounds = bounds,
                        accent = Color(palette.accent),
                        onSelect = { session.selectedId = it ?: session.selectedId },
                        onDragStart = { session.draggingId = it },
                        onDragOver = { hit, point, boundsOf -> session.draggingId?.let { dragged -> session.placeDrop(dragged, hit, point, boundsOf) } },
                        onDrop = { session.endDrag() },
                        onProblems = { session.previewProblems = it },
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        values = session.previewValues,
                        interactive = session.interactive,
                        live = liveTree,
                        onEvent = { nodeId, type, callbackId, payload ->
                            when {
                                liveTree != null -> sandbox.dispatch(nodeId, type, callbackId, payload.keySet().associateWith { payload.get(it) })
                                // Until the script's first tree arrives the canvas shows the document; input waits for the script.
                                runsScript -> Unit
                                else -> session.previewEvent(nodeId, type) { field -> payload.get(field) }
                            }
                        },
                    )
                    // The script's last error, over the canvas; the console tab has the whole output.
                    sandboxProblems.firstOrNull()?.let { problem ->
                        Text(
                            problem,
                            color = Color(palette.danger),
                            fontSize = 12.sp,
                            maxLines = 2,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).background(Color(palette.background)).padding(6.dp)
                                .clickable { tab = CONSOLE_TAB }.testTag("designer-sandbox-problem"),
                        )
                    }
                    // Edit mode selects and drags; the interactive preview passes input to the components (P9.3).
                    FilterChip(
                        selected = session.interactive,
                        onClick = { session.interactive = !session.interactive },
                        label = { Text(stringResource(R.string.designer_preview_interact)) },
                        // Shown while the canvas holds the running script's tree, not the document's (P9.3 step 2).
                        leadingIcon = if (liveTree == null) null else {
                            { Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.designer_sandbox_running), modifier = Modifier.size(18.dp).testTag("designer-sandbox-live")) }
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp).testTag("designer-interact"),
                    )
                }
            }
            val panels: @Composable (Modifier) -> Unit = { placement ->
                Column(placement) {
                    TabRow(selectedTabIndex = tab, containerColor = Color(palette.background), contentColor = Color(palette.text)) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.designer_tab_outline)) }, modifier = Modifier.testTag("designer-tab-outline"))
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.designer_tab_inspector)) }, modifier = Modifier.testTag("designer-tab-inspector"))
                        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.designer_tab_state)) }, modifier = Modifier.testTag("designer-tab-state"))
                        Tab(selected = tab == CONSOLE_TAB, onClick = { tab = CONSOLE_TAB }, text = { Text(stringResource(R.string.designer_tab_console)) }, modifier = Modifier.testTag("designer-tab-console"))
                    }
                    when (tab) {
                        0 -> OutlinePanel(session, palette, Modifier.fillMaxWidth())
                        1 -> InspectorPanel(session, palette, onPickSlotComponent = { slot -> paletteTarget = slot; paletteOpen = true }, modifier = Modifier.fillMaxWidth())
                        2 -> StatePanel(session, palette, Modifier.fillMaxWidth())
                        else -> ConsolePanel(sandbox, session.document.dialect, session.interactive, palette, Modifier.fillMaxWidth())
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                // A window wider than tall (a phone on its side, a tablet) puts the canvas and the panels side by
                // side: stacked under a 360 dp tall canvas the outline shows about one row, behind the add button.
                if (maxWidth > maxHeight) {
                    Row(Modifier.fillMaxSize()) {
                        canvas(Modifier.fillMaxHeight().weight(1.1f))
                        panels(Modifier.fillMaxHeight().weight(0.9f))
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        canvas(Modifier.fillMaxWidth().weight(1.1f))
                        panels(Modifier.fillMaxWidth().weight(0.9f))
                    }
                }
            }
        }
        if (paletteOpen) ComponentPalette(palette, onPick = { type ->
            paletteOpen = false
            val slot = paletteTarget
            val inserted = if (slot != null) {
                session.selected?.let { node -> session.execute(EditCommand.SetSlot(node.id, slot, DesignChild.Node(NodeTemplates.create(type))), merge = false) } ?: false
            } else session.insert(type)
            if (!inserted) scope.launch { snackbar.showSnackbar(insertFailed) }
        }, onDismiss = { paletteOpen = false })
        if (hostDialog) HostOptionsDialog(session, palette) { hostDialog = false }
        if (issuesDialog) IssuesDialog(session.diagnostics, session.previewProblems + sandboxProblems, palette, onSelect = { session.selectedId = it }) { issuesDialog = false }
        if (exitDialog) AlertDialog(
            onDismissRequest = { exitDialog = false },
            title = { Text(stringResource(R.string.designer_back_discard_title)) },
            text = { Text(stringResource(R.string.designer_back_discard_message)) },
            confirmButton = { TextButton(onClick = { save(); finish() }, modifier = Modifier.testTag("designer-exit-save")) { Text(stringResource(R.string.designer_save_exit)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        // Not dirty any more, so onStop does not write the discarded edits back as a draft.
                        session.dirty = false
                        io.clearDraft(source)
                        finish()
                    }, modifier = Modifier.testTag("designer-exit-discard")) { Text(stringResource(R.string.designer_discard)) }
                    TextButton(onClick = { exitDialog = false }) { Text(stringResource(R.string.designer_keep_editing)) }
                }
            },
        )
        pendingDraft?.let { draft ->
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.designer_draft_title)) },
                text = { Text(stringResource(R.string.designer_draft_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDraft = null
                        read(draft) { result ->
                            when (result) {
                                is ReadResult.Parsed -> {
                                    session.replace(result.document)
                                    session.dirty = true
                                }
                                is ReadResult.Rejected, is ReadResult.ParsedUi -> io.clearDraft(source)
                                // The TSX parser is not available now; the draft stays for the next opening.
                                is ReadResult.Unavailable -> Unit
                            }
                        }
                    }, modifier = Modifier.testTag("designer-draft-restore")) { Text(stringResource(R.string.designer_draft_restore)) }
                },
                dismissButton = { TextButton(onClick = { pendingDraft = null; io.clearDraft(source) }, modifier = Modifier.testTag("designer-draft-discard")) { Text(stringResource(R.string.designer_draft_discard)) } },
            )
        }
        val linkedName = (source as? DocumentSource.Local)?.name
        when (val prompt = syncPrompt) {
            null -> Unit
            SyncPrompt.Overwrite -> AlertDialog(
                onDismissRequest = { syncPrompt = null },
                title = { Text(stringResource(R.string.designer_source_changed_title)) },
                text = { Text(stringResource(R.string.designer_source_changed_message)) },
                confirmButton = {
                    TextButton(onClick = { syncPrompt = null; linkedName?.let { writeBack(it, confirmed = true) } }, modifier = Modifier.testTag("designer-source-overwrite")) {
                        Text(stringResource(R.string.designer_source_overwrite))
                    }
                },
                dismissButton = { TextButton(onClick = { syncPrompt = null }) { Text(stringResource(R.string.designer_cancel)) } },
            )
            SyncPrompt.Replace -> AlertDialog(
                onDismissRequest = { syncPrompt = null },
                title = { Text(stringResource(R.string.designer_source_replace_title)) },
                text = { Text(stringResource(R.string.designer_source_replace_message)) },
                confirmButton = {
                    TextButton(onClick = { syncPrompt = null; linkedName?.let { reimport(it, confirmed = true) } }, modifier = Modifier.testTag("designer-source-replace")) {
                        Text(stringResource(R.string.designer_source_replace))
                    }
                },
                dismissButton = { TextButton(onClick = { syncPrompt = null }) { Text(stringResource(R.string.designer_cancel)) } },
            )
            is SyncPrompt.Unavailable -> AlertDialog(
                onDismissRequest = { syncPrompt = null },
                title = { Text(stringResource(R.string.designer_source_unavailable_title)) },
                text = { Text(stringResource(R.string.designer_source_unavailable_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        syncPrompt = null
                        relinkAction = prompt.action
                        relinker.launch(ProjectStore.SCRIPT_DOCUMENT_TYPES)
                    }, modifier = Modifier.testTag("designer-source-pick-again")) { Text(stringResource(R.string.designer_source_pick_again)) }
                },
                dismissButton = { TextButton(onClick = { syncPrompt = null }) { Text(stringResource(R.string.designer_cancel)) } },
            )
        }
        externalChange?.let { changed ->
            AlertDialog(
                onDismissRequest = { externalChange = null },
                title = { Text(stringResource(R.string.designer_external_changed_title)) },
                text = { Text(stringResource(R.string.designer_external_changed_message)) },
                confirmButton = {
                    TextButton(onClick = { externalChange = null; reloadFrom(changed); io.clearDraft(source) }, modifier = Modifier.testTag("designer-external-reload")) { Text(stringResource(R.string.designer_reload)) }
                },
                dismissButton = {
                    TextButton(onClick = { externalChange = null; baseline = changed }, modifier = Modifier.testTag("designer-external-keep")) { Text(stringResource(R.string.designer_keep_mine)) }
                },
            )
        }
    }

    /**
     * The editing screen of a `"ui";` mode project (roadmap P10.3 / P10.4): the host's preview on the XML canvas, the
     * element outline, the attribute inspector (with handler stubs, roadmap D52), the bindings panel, the problems
     * list and the preview console; the palette inserts catalog tags; save, drafts, undo / redo, source sync and
     * the host hand-over work as for Compose projects.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun UiEditorScreen(session: UiDesignerSession, palette: StandalonePalette) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        var tab by remember { mutableStateOf(0) }
        var menuOpen by remember { mutableStateOf(false) }
        var exitDialog by remember { mutableStateOf(false) }
        var zoom by remember { mutableStateOf(1f) }
        val savedText = stringResource(R.string.designer_saved)
        val savedAsProject = stringResource(R.string.designer_external_readonly_saved)
        val copied = stringResource(R.string.gallery_copied)
        val missingHost = stringResource(R.string.gallery_run_missing_host)
        val runFailed = stringResource(R.string.gallery_run_failed)
        val hostMissing = stringResource(R.string.designer_host_missing)
        val hostFailed = stringResource(R.string.designer_host_failed)
        val exported = stringResource(R.string.designer_exported)
        val exportFailed = stringResource(R.string.designer_export_failed)
        val failedText = stringResource(R.string.designer_ui_preview_failed)
        val insertFailed = stringResource(R.string.designer_insert_failed)
        val sourceWritten = stringResource(R.string.designer_source_written)
        val sourceWriteFailed = stringResource(R.string.designer_source_write_failed)
        val sourceReimported = stringResource(R.string.designer_source_reimported)
        val sourceParseFailed = stringResource(R.string.designer_source_parse_failed)
        val handlerAdded = stringResource(R.string.designer_ui_handler_added)
        var paletteOpen by remember { mutableStateOf(false) }
        var syncPrompt by remember { mutableStateOf<SyncPrompt?>(null) }
        var relinkAction by remember { mutableStateOf<SyncAction?>(null) }
        val errors = session.diagnostics.count { it.severity == Severity.ERROR }

        // The host draws the layout (D46): one client per screen, bound while the screen lives; the session follows
        // the canvas surface, and a loss is retried when the screen comes back.
        val client = requireNotNull(previewClient)
        DisposableEffect(client) { client.bind(); onDispose { } }
        LaunchedEffect(appearance) { client.configure(appearance.dark, appearance.language) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        var visible by remember { mutableStateOf(true) }
        DisposableEffect(lifecycle, client) {
            val observer = LifecycleEventObserver { _, event ->
                visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                if (event == Lifecycle.Event.ON_RESUME) { if (client.status is UiPreviewClient.Status.Lost) client.retry() else client.ensureSession() }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        val evaluator = remember { DeclarationEvaluator() }
        var request by remember { mutableStateOf<XmlPreviewRequest?>(null) }
        LaunchedEffect(session.revision, session.fragment, session.bindingOverrides) {
            val document = session.document
            val fragment = session.fragment
            val script = session.script()
            val expressions = document.fragments.getOrNull(fragment)?.root?.descendants()?.flatMap { element ->
                element.children.filterIsInstance<XmlNode.Expr>().map { XmlPreviewFrames.expressionOf(it.source) } +
                    element.attrs.flatMap { attr -> attr.parts.orEmpty().filterIsInstance<AttrPart.Expr>().map { XmlPreviewFrames.expressionOf(it.source) } }
            }?.toList().orEmpty()
            val evaluated = if (DeclarationEvaluator.SUPPORTED) withContext(Dispatchers.Default) { evaluator.evaluate(script, expressions) } else DeclarationEvaluator.Result.EMPTY
            val built = XmlPreviewFrames.build(document, fragment, XmlPreviewValues(evaluated.declarations, session.bindingOverrides, evaluated.expressions))
            request = built
            session.bindingNames = built.bindingNames
            session.previewDeclarations = evaluated.declarations
            session.declarationFailures = evaluated.failures
            client.submit(built)
        }
        LaunchedEffect(client.frameRevision) { session.previewFrames = client.frameRevision }
        // Roadmap P10.6: the interactive preview runs the whole script in the session's root as a real host script
        // (contract v2, API 33+). It starts when the switch goes on (after the project's one-time confirmation), runs
        // again once edits pause, stops while the designer is hidden or when the switch goes off, and never touches
        // the document, the file or the draft; the held layout is sent again when it ends.
        val interactiveSupported = client.embedded && client.scriptRunSupported
        LaunchedEffect(session.interactive, interactiveSupported, visible, session.revision, client.sessionRevision) {
            if (session.interactive && interactiveSupported && visible) {
                client.setInteractiveMode(true)
                if (client.runRevision > 0) delay(SANDBOX_RESTART_DELAY_MS)
                client.runScript(session.script(), source.title)
            } else {
                client.setInteractiveMode(false)
            }
        }
        var interactivePrompt by remember { mutableStateOf(false) }
        val interactivePrefs = remember { context.applicationContext.getSharedPreferences(DESIGNER_PREFS, Context.MODE_PRIVATE) }
        fun toggleInteractive() {
            if (session.interactive) { session.interactive = false; return }
            val confirmed = interactivePrefs.getStringSet(INTERACTIVE_CONFIRMED_KEY, null).orEmpty()
            if (source.draftKey in confirmed) session.interactive = true else interactivePrompt = true
        }
        if (interactivePrompt) AlertDialog(
            onDismissRequest = { interactivePrompt = false },
            title = { Text(stringResource(R.string.designer_ui_interact_confirm_title)) },
            text = { Text(stringResource(R.string.designer_ui_interact_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    interactivePrompt = false
                    val confirmed = interactivePrefs.getStringSet(INTERACTIVE_CONFIRMED_KEY, null).orEmpty()
                    interactivePrefs.edit().putStringSet(INTERACTIVE_CONFIRMED_KEY, confirmed + source.draftKey).apply()
                    session.interactive = true
                }, modifier = Modifier.testTag("designer-ui-interact-confirm")) { Text(stringResource(R.string.gallery_run)) }
            },
            dismissButton = { TextButton(onClick = { interactivePrompt = false }, modifier = Modifier.testTag("designer-ui-interact-cancel")) { Text(stringResource(R.string.designer_cancel)) } },
        )
        LaunchedEffect(session.lastHandlerStub) {
            // Roadmap D52 (b): the stub went into the script; the body is written in an editor, not here.
            val stub = session.lastHandlerStub ?: return@LaunchedEffect
            session.lastHandlerStub = null
            snackbar.showSnackbar(handlerAdded.format(stub.id, stub.event))
        }
        LaunchedEffect(client.failure, client.diagnostics, request) {
            val current = request
            val failure = client.failure
            val problems = ArrayList<String>()
            var problemId: Int? = null
            if (failure != null) {
                problemId = current?.elementIdAt(failure.elementIndex)
                problems += failedText.format(problemId?.let { session.document.element(it) }?.let(::elementLabel) ?: "?", failure.message)
            }
            client.diagnostics.forEach { diagnostic ->
                val label = current?.elementIdAt(diagnostic.elementIndex)?.let { session.document.element(it) }?.let(::elementLabel) ?: "?"
                problems += "<$label> " + (diagnostic.attrName?.let { "$it: " } ?: "") + diagnostic.message
            }
            current?.notes?.forEach { note -> problems += "<" + (session.document.element(note.elementId)?.let(::elementLabel) ?: "?") + "> " + note.message }
            session.previewProblems = problems
            session.problemElementId = problemId
        }
        val statusText: String? = when (val status = client.status) {
            is UiPreviewClient.Status.Unavailable -> when (status.reason) {
                UiPreviewClient.Reason.HOST_MISSING -> stringResource(R.string.designer_ui_preview_host_missing)
                UiPreviewClient.Reason.HOST_TOO_OLD -> stringResource(R.string.designer_ui_preview_host_old, status.installedVersion ?: "?")
                UiPreviewClient.Reason.CONTRACT_MISMATCH -> stringResource(R.string.designer_ui_preview_mismatch, status.installedVersion ?: "?")
                UiPreviewClient.Reason.UNAVAILABLE -> stringResource(R.string.designer_ui_preview_unavailable, status.detail ?: status.installedVersion ?: "?")
            }
            is UiPreviewClient.Status.Lost -> stringResource(R.string.designer_ui_preview_lost, status.reason)
            else -> when {
                client.failure != null -> session.previewProblems.firstOrNull()
                client.bounds.isEmpty() && client.snapshot == null -> stringResource(R.string.designer_ui_preview_waiting)
                !client.embedded -> stringResource(R.string.designer_ui_preview_snapshot)
                else -> null
            }
        }
        val retry: (() -> Unit)? = when (client.status) {
            is UiPreviewClient.Status.Lost, is UiPreviewClient.Status.Unavailable -> ({ client.retry() })
            else -> null
        }

        fun save(announce: Boolean = true): Boolean {
            val text = session.script()
            val previous = source
            val result = io.write(previous, text)
            io.clearDraft(previous)
            baseline = text
            session.markSaved()
            when (result) {
                is DocumentIo.WriteResult.Saved -> if (announce) scope.launch { snackbar.showSnackbar(savedText) }
                is DocumentIo.WriteResult.SavedAsProject -> {
                    source = DocumentSource.Local(result.name)
                    scope.launch { snackbar.showSnackbar(savedAsProject.format(result.name)) }
                }
            }
            return true
        }

        fun sendToHost(intent: (Uri, Boolean) -> Intent) {
            if (session.dirty) save()
            val (uri, writable) = io.shareUri(source)
            when (ScriptLauncher.start(context, intent(uri, writable))) {
                ScriptLauncher.Result.STARTED -> Unit
                ScriptLauncher.Result.HOST_MISSING -> scope.launch { snackbar.showSnackbar(hostMissing) }
                ScriptLauncher.Result.FAILED -> scope.launch { snackbar.showSnackbar(hostFailed) }
            }
        }

        // Roadmap P9.1 / D45, shared with Compose projects: an imported project syncs with its source only on request.
        fun writeBack(name: String, confirmed: Boolean) {
            if (session.dirty) save(announce = false)
            when (val state = sync.state(name)) {
                SourceSync.State.None -> Unit
                is SourceSync.State.Unavailable -> syncPrompt = SyncPrompt.Unavailable(SyncAction.WRITE_BACK)
                is SourceSync.State.Ready -> if (state.sourceChanged && !confirmed) syncPrompt = SyncPrompt.Overwrite else {
                    val written = sync.writeBack(name, session.script())
                    scope.launch { snackbar.showSnackbar(if (written) sourceWritten else sourceWriteFailed) }
                }
            }
        }

        fun reimport(name: String, confirmed: Boolean) {
            when (val state = sync.state(name)) {
                SourceSync.State.None -> Unit
                is SourceSync.State.Unavailable -> syncPrompt = SyncPrompt.Unavailable(SyncAction.REIMPORT)
                is SourceSync.State.Ready -> if ((state.projectChanged || session.dirty) && !confirmed) syncPrompt = SyncPrompt.Replace else {
                    read(state.sourceText) { result ->
                        if (result !is ReadResult.ParsedUi) {
                            scope.launch { snackbar.showSnackbar(sourceParseFailed) }
                        } else {
                            session.replace(result.document)
                            sync.reimported(name, state.sourceText)
                            baseline = state.sourceText
                            io.clearDraft(source)
                            scope.launch { snackbar.showSnackbar(sourceReimported) }
                        }
                    }
                }
            }
        }

        val relinker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val action = relinkAction
            relinkAction = null
            val name = (source as? DocumentSource.Local)?.name
            if (uri == null || action == null || name == null) return@rememberLauncherForActivityResult
            sync.relink(name, uri)
            when (action) {
                SyncAction.WRITE_BACK -> writeBack(name, confirmed = false)
                SyncAction.REIMPORT -> reimport(name, confirmed = false)
            }
        }

        val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ScriptLauncher.SCRIPT_MIME_TYPE)) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(session.script().toByteArray(StandardCharsets.UTF_8)) } != null
            }.getOrDefault(false)
            scope.launch { snackbar.showSnackbar(if (ok) exported else exportFailed) }
        }

        LaunchedEffect(session.revision) {
            if (session.dirty) {
                delay(1000)
                if (session.dirty) io.writeDraft(source, session.script())
            }
        }
        BackHandler(enabled = session.dirty) { exitDialog = true }

        Scaffold(
            containerColor = Color(palette.background),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(source.title + if (session.dirty) " *" else "", fontSize = 16.sp, maxLines = 1) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(palette.background), titleContentColor = Color(palette.text)),
                    navigationIcon = {
                        IconButton(onClick = { if (session.dirty) exitDialog = true else finish() }, modifier = Modifier.testTag("designer-back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(palette.muted))
                        }
                    },
                    actions = {
                        TextButton(onClick = { session.undo() }, enabled = session.canUndo, modifier = Modifier.testTag("designer-undo")) { Text(stringResource(R.string.designer_undo)) }
                        TextButton(onClick = { session.redo() }, enabled = session.canRedo, modifier = Modifier.testTag("designer-redo")) { Text(stringResource(R.string.designer_redo)) }
                        IconButton(onClick = { save() }, modifier = Modifier.testTag("designer-save")) {
                            Icon(Icons.Filled.Done, contentDescription = stringResource(R.string.designer_save), tint = Color(if (session.dirty) palette.accent else palette.muted))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("designer-menu")) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.designer_host_options), tint = Color(palette.muted))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_issues_count, errors)) }, onClick = { menuOpen = false; tab = UI_PROBLEMS_TAB }, modifier = Modifier.testTag("designer-menu-issues"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_zoom, if (zoom < 1f) "100%" else "75%")) }, onClick = { menuOpen = false; zoom = if (zoom < 1f) 1f else 0.75f })
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_copy_script)) }, onClick = {
                                    menuOpen = false
                                    if (ScriptLauncher.copy(context, source.title, session.script())) scope.launch { snackbar.showSnackbar(copied) }
                                }, modifier = Modifier.testTag("designer-menu-copy"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.gallery_run)) }, onClick = {
                                    menuOpen = false
                                    when (ScriptLauncher.run(context, session.script())) {
                                        ScriptLauncher.Result.STARTED -> Unit
                                        ScriptLauncher.Result.HOST_MISSING -> scope.launch { snackbar.showSnackbar(missingHost) }
                                        ScriptLauncher.Result.FAILED -> scope.launch { snackbar.showSnackbar(runFailed) }
                                    }
                                }, modifier = Modifier.testTag("designer-menu-run"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_open_in_host)) }, onClick = {
                                    menuOpen = false
                                    sendToHost { uri, writable -> ScriptLauncher.editIntent(uri, writable) }
                                }, modifier = Modifier.testTag("designer-menu-edit-in-host"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_import_to_host)) }, onClick = {
                                    menuOpen = false
                                    sendToHost { uri, _ -> ScriptLauncher.importIntent(uri) }
                                }, modifier = Modifier.testTag("designer-menu-import-to-host"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_export)) }, onClick = {
                                    menuOpen = false
                                    exporter.launch(source.title)
                                }, modifier = Modifier.testTag("designer-menu-export"))
                                val linked = (source as? DocumentSource.Local)?.name?.takeIf { menuOpen && store.sourceOf(it) != null }
                                if (linked != null) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.designer_source_write_back)) }, onClick = {
                                        menuOpen = false
                                        writeBack(linked, confirmed = false)
                                    }, modifier = Modifier.testTag("designer-menu-write-back"))
                                    DropdownMenuItem(text = { Text(stringResource(R.string.designer_source_reimport)) }, onClick = {
                                        menuOpen = false
                                        reimport(linked, confirmed = false)
                                    }, modifier = Modifier.testTag("designer-menu-reimport"))
                                }
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_open_gallery)) }, onClick = { menuOpen = false; context.startActivity(Intent(context, GalleryActivity::class.java)) })
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { paletteOpen = true }, modifier = Modifier.testTag("designer-add")) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.designer_add_component))
                }
            },
        ) { padding ->
            val canvas: @Composable (Modifier) -> Unit = { placement ->
                Column(placement) {
                    // Roadmap P10.6: the switch and the run's state sit above the canvas, where the on-top surface of the
                    // interactive preview cannot cover them (nothing of this window shows over the canvas while it is on).
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val running = client.running
                        val runId = client.runId
                        val scriptError = client.scriptError
                        val lastExit = client.lastExit
                        val ready = client.status as? UiPreviewClient.Status.Ready
                        if (interactiveSupported) FilterChip(
                            selected = session.interactive,
                            onClick = { toggleInteractive() },
                            label = { Text(stringResource(R.string.designer_preview_interact)) },
                            // Shown while the host runs the script in the canvas.
                            leadingIcon = if (!(session.interactive && running && runId != null)) null else {
                                { Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.designer_ui_interact_running), modifier = Modifier.size(18.dp).testTag("designer-ui-live")) }
                            },
                            modifier = Modifier.padding(end = 8.dp).testTag("designer-interact"),
                        )
                        val live = session.interactive && interactiveSupported
                        val rowText: String? = when {
                            !live -> when {
                                interactiveSupported || ready == null -> null
                                !client.pointerInteractiveAvailable -> stringResource(R.string.designer_ui_interact_needs_android)
                                else -> stringResource(R.string.designer_ui_interact_needs_host, ready.hostVersionName ?: ready.hostVersionCode.toString())
                            }
                            ready == null -> statusText
                            scriptError != null -> stringResource(R.string.designer_sandbox_error, scriptError)
                            running && runId == null -> stringResource(R.string.designer_ui_interact_starting)
                            running -> stringResource(if (client.imeAvailable) R.string.designer_ui_interact_running else R.string.designer_ui_interact_no_ime)
                            lastExit != null -> stringResource(R.string.designer_ui_interact_exited, lastExit)
                            else -> null
                        }
                        if (rowText != null) Text(
                            rowText,
                            color = Color(if (live && (scriptError != null || ready == null)) palette.danger else palette.muted),
                            fontSize = 12.sp,
                            maxLines = 2,
                            modifier = Modifier.weight(1f).padding(vertical = 6.dp).clickable(enabled = live) {
                                if (ready == null) retry?.invoke() else tab = UI_CONSOLE_TAB
                            }.testTag(if (live && scriptError != null) "designer-ui-script-problem" else "designer-ui-interact-status"),
                        )
                    }
                    Box(Modifier.fillMaxWidth().weight(1f).background(Color(palette.surfaceVariant)).testTag("designer-canvas")) {
                        XmlCanvas(
                            session = session,
                            request = request,
                            client = client,
                            zoom = zoom,
                            interactive = session.interactive && interactiveSupported,
                            accent = Color(palette.accent),
                            danger = Color(palette.danger),
                            text = Color(palette.text),
                            muted = Color(palette.muted),
                            surface = Color(palette.background),
                            statusText = statusText,
                            retryLabel = stringResource(R.string.designer_retry),
                            onRetry = retry,
                            onSelect = { session.selectedId = it ?: session.selectedId },
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        )
                    }
                }
            }
            val panels: @Composable (Modifier) -> Unit = { placement ->
                Column(placement) {
                    TabRow(selectedTabIndex = tab, containerColor = Color(palette.background), contentColor = Color(palette.text)) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.designer_tab_outline), fontSize = 12.sp, maxLines = 1) }, modifier = Modifier.testTag("designer-tab-outline"))
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.designer_tab_inspector), fontSize = 12.sp, maxLines = 1) }, modifier = Modifier.testTag("designer-tab-inspector"))
                        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.designer_ui_tab_bindings), fontSize = 12.sp, maxLines = 1) }, modifier = Modifier.testTag("designer-tab-bindings"))
                        Tab(selected = tab == UI_PROBLEMS_TAB, onClick = { tab = UI_PROBLEMS_TAB }, text = { Text(stringResource(R.string.designer_ui_tab_problems), fontSize = 12.sp, maxLines = 1) }, modifier = Modifier.testTag("designer-tab-problems"))
                        Tab(selected = tab == UI_CONSOLE_TAB, onClick = { tab = UI_CONSOLE_TAB }, text = { Text(stringResource(R.string.designer_tab_console), fontSize = 12.sp, maxLines = 1) }, modifier = Modifier.testTag("designer-tab-console"))
                    }
                    when (tab) {
                        0 -> XmlOutlinePanel(session, palette, Modifier.fillMaxWidth())
                        1 -> UiInspectorPanel(session, palette, Modifier.fillMaxWidth())
                        2 -> XmlBindingsPanel(session, palette, Modifier.fillMaxWidth())
                        UI_PROBLEMS_TAB -> XmlProblemsPanel(session, palette, session.interactive && interactiveSupported, Modifier.fillMaxWidth())
                        else -> XmlConsolePanel(client, statusText, session.interactive && interactiveSupported, palette, Modifier.fillMaxWidth())
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                if (maxWidth > maxHeight) {
                    Row(Modifier.fillMaxSize()) {
                        canvas(Modifier.fillMaxHeight().weight(1.1f))
                        panels(Modifier.fillMaxHeight().weight(0.9f))
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        canvas(Modifier.fillMaxWidth().weight(1.1f))
                        panels(Modifier.fillMaxWidth().weight(0.9f))
                    }
                }
            }
        }
        if (exitDialog) AlertDialog(
            onDismissRequest = { exitDialog = false },
            title = { Text(stringResource(R.string.designer_back_discard_title)) },
            text = { Text(stringResource(R.string.designer_back_discard_message)) },
            confirmButton = { TextButton(onClick = { save(); finish() }, modifier = Modifier.testTag("designer-exit-save")) { Text(stringResource(R.string.designer_save_exit)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        session.dirty = false
                        io.clearDraft(source)
                        finish()
                    }, modifier = Modifier.testTag("designer-exit-discard")) { Text(stringResource(R.string.designer_discard)) }
                    TextButton(onClick = { exitDialog = false }) { Text(stringResource(R.string.designer_keep_editing)) }
                }
            },
        )
        pendingDraft?.let { draft ->
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.designer_draft_title)) },
                text = { Text(stringResource(R.string.designer_draft_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDraft = null
                        read(draft) { result ->
                            when (result) {
                                is ReadResult.ParsedUi -> {
                                    session.replace(result.document)
                                    session.dirty = true
                                }
                                is ReadResult.Parsed, is ReadResult.Rejected -> io.clearDraft(source)
                                is ReadResult.Unavailable -> Unit
                            }
                        }
                    }, modifier = Modifier.testTag("designer-draft-restore")) { Text(stringResource(R.string.designer_draft_restore)) }
                },
                dismissButton = { TextButton(onClick = { pendingDraft = null; io.clearDraft(source) }, modifier = Modifier.testTag("designer-draft-discard")) { Text(stringResource(R.string.designer_draft_discard)) } },
            )
        }
        if (paletteOpen) UiPalette(session.catalog, palette, onPick = { tag ->
            paletteOpen = false
            if (!session.insert(tag)) scope.launch { snackbar.showSnackbar(insertFailed) }
        }, onDismiss = { paletteOpen = false })
        val linkedName = (source as? DocumentSource.Local)?.name
        when (val prompt = syncPrompt) {
            null -> Unit
            SyncPrompt.Overwrite -> AlertDialog(
                onDismissRequest = { syncPrompt = null },
                title = { Text(stringResource(R.string.designer_source_changed_title)) },
                text = { Text(stringResource(R.string.designer_source_changed_message)) },
                confirmButton = {
                    TextButton(onClick = { syncPrompt = null; linkedName?.let { writeBack(it, confirmed = true) } }, modifier = Modifier.testTag("designer-source-overwrite")) {
                        Text(stringResource(R.string.designer_source_overwrite))
                    }
                },
                dismissButton = { TextButton(onClick = { syncPrompt = null }) { Text(stringResource(R.string.designer_cancel)) } },
            )
            SyncPrompt.Replace -> AlertDialog(
                onDismissRequest = { syncPrompt = null },
                title = { Text(stringResource(R.string.designer_source_replace_title)) },
                text = { Text(stringResource(R.string.designer_source_replace_message)) },
                confirmButton = {
                    TextButton(onClick = { syncPrompt = null; linkedName?.let { reimport(it, confirmed = true) } }, modifier = Modifier.testTag("designer-source-replace")) {
                        Text(stringResource(R.string.designer_source_replace))
                    }
                },
                dismissButton = { TextButton(onClick = { syncPrompt = null }) { Text(stringResource(R.string.designer_cancel)) } },
            )
            is SyncPrompt.Unavailable -> AlertDialog(
                onDismissRequest = { syncPrompt = null },
                title = { Text(stringResource(R.string.designer_source_unavailable_title)) },
                text = { Text(stringResource(R.string.designer_source_unavailable_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        syncPrompt = null
                        relinkAction = prompt.action
                        relinker.launch(ProjectStore.SCRIPT_DOCUMENT_TYPES)
                    }, modifier = Modifier.testTag("designer-source-pick-again")) { Text(stringResource(R.string.designer_source_pick_again)) }
                },
                dismissButton = { TextButton(onClick = { syncPrompt = null }) { Text(stringResource(R.string.designer_cancel)) } },
            )
        }
        externalChange?.let { changed ->
            AlertDialog(
                onDismissRequest = { externalChange = null },
                title = { Text(stringResource(R.string.designer_external_changed_title)) },
                text = { Text(stringResource(R.string.designer_external_changed_message)) },
                confirmButton = {
                    TextButton(onClick = { externalChange = null; reloadFrom(changed); io.clearDraft(source) }, modifier = Modifier.testTag("designer-external-reload")) { Text(stringResource(R.string.designer_reload)) }
                },
                dismissButton = {
                    TextButton(onClick = { externalChange = null; baseline = changed }, modifier = Modifier.testTag("designer-external-keep")) { Text(stringResource(R.string.designer_keep_mine)) }
                },
            )
        }
    }

    private enum class SyncAction { WRITE_BACK, REIMPORT }

    private sealed class SyncPrompt {
        /** Writing back would overwrite edits made to the source since the last sync. */
        object Overwrite : SyncPrompt()
        /** Re-importing would replace edits made to the project since the last sync. */
        object Replace : SyncPrompt()
        /** The source cannot be opened; picking it again relinks the project and retries [action]. */
        data class Unavailable(val action: SyncAction) : SyncPrompt()
    }

    companion object {
        const val EXTRA_PROJECT = "project"
        private const val CONSOLE_TAB = 3
        private const val UI_PROBLEMS_TAB = 3
        private const val UI_CONSOLE_TAB = 4
        private const val SANDBOX_RESTART_DELAY_MS = 400L
        /** Designer-wide preferences: the projects whose interactive preview of a ui script was confirmed (P10.6). */
        private const val DESIGNER_PREFS = "designer"
        private const val INTERACTIVE_CONFIRMED_KEY = "ui-interactive-confirmed"

        fun intent(context: Context, projectName: String): Intent = Intent(context, DesignerActivity::class.java).putExtra(EXTRA_PROJECT, projectName)

        /** Opens an external document the way the host editor or a file manager would hand it over. */
        fun documentIntent(context: Context, uri: Uri, writable: Boolean): Intent = Intent(Intent.ACTION_EDIT, uri)
            .setClass(context, DesignerActivity::class.java)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or if (writable) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
    }
}
