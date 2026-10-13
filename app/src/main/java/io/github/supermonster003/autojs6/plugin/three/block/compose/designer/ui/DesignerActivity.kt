package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryActivity
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptLauncher
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignChild
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.EditCommand
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.NodeTemplates
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Severity
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.ScriptReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.CanvasBounds
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.preview.DesignCanvas
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.DocumentIo
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.DocumentSource
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.autojs.plugin.compose.api.model.ThemeSpec
import java.nio.charset.StandardCharsets

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
    private var source: DocumentSource by mutableStateOf(DocumentSource.Local(""))
    private var session: DesignerSession? = null
    private var parseError: DesignerParseError? = null
    private var readFailed = false
    /** The text last loaded from or written to the document; the reference for external-change detection. */
    private var baseline: String? = null
    private var pendingDraft: String? by mutableStateOf(null)
    private var externalChange: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        store = ProjectStore(this)
        io = DocumentIo(this, store)
        val projectName = intent.getStringExtra(EXTRA_PROJECT)?.takeIf { ProjectStore.isValidName(it) }
        val resolved: DocumentSource? = when {
            projectName != null -> DocumentSource.Local(projectName).takeIf { store.exists(projectName) }
            else -> io.external(intent)
        }
        if (resolved == null) { finish(); super.onCreate(savedInstanceState); return }
        source = resolved
        val text = io.readOrNull(resolved)
        if (text == null) {
            readFailed = true
        } else {
            pendingDraft = io.pendingDraft(resolved)
            load(text)
        }
        super.onCreate(savedInstanceState)
    }

    private fun load(script: String): Boolean = try {
        session = DesignerSession(ScriptReader().read(script))
        parseError = null
        baseline = script
        true
    } catch (error: DesignerParseError) {
        parseError = error
        false
    }

    override fun onResume() {
        super.onResume()
        val current = session ?: return
        val onDisk = io.readOrNull(source) ?: return
        if (baseline == null || onDisk == baseline) return
        if (!current.dirty) reloadFrom(current, onDisk) else externalChange = onDisk
    }

    private fun reloadFrom(session: DesignerSession, script: String) {
        try {
            session.replace(ScriptReader().read(script))
            session.markSaved()
            baseline = script
        } catch (error: DesignerParseError) {
            // The other editor left an unmappable script; keep the tree here and let an explicit save decide.
        }
    }

    override fun onStop() {
        session?.let { if (it.dirty) io.writeDraft(source, it.script()) }
        super.onStop()
    }

    @Composable
    override fun Content(palette: StandalonePalette) {
        val error = parseError
        val current = session
        if (readFailed || error != null || current == null) {
            ParseErrorScreen(error, palette)
            return
        }
        Editor(current, palette)
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
        val theme = remember(appearance) { ThemeSpec(seedArgb = appearance.primary, dark = appearance.dark) }
        val errors = session.diagnostics.count { it.severity == Severity.ERROR }

        fun save(): Boolean {
            val text = session.script()
            val previous = source
            val result = io.write(previous, text)
            io.clearDraft(previous)
            baseline = text
            session.markSaved()
            when (result) {
                is DocumentIo.WriteResult.Saved -> scope.launch { snackbar.showSnackbar(savedText) }
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
                io.writeDraft(source, session.script())
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
                        theme = theme,
                        zoom = zoom,
                        selectedId = session.selectedId,
                        dropTargetId = session.dropTargetId,
                        bounds = bounds,
                        accent = Color(palette.accent),
                        onSelect = { session.selectedId = it ?: session.selectedId },
                        onDragStart = { session.draggingId = it },
                        onDragOver = { hit, _ -> session.dropTargetId = session.draggingId?.let { dragged -> session.dropContainerFor(dragged, hit) } },
                        onDrop = { session.endDrag() },
                        onProblems = { session.previewProblems = it },
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                    )
                }
            }
            val panels: @Composable (Modifier) -> Unit = { placement ->
                Column(placement) {
                    TabRow(selectedTabIndex = tab, containerColor = Color(palette.background), contentColor = Color(palette.text)) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.designer_tab_outline)) }, modifier = Modifier.testTag("designer-tab-outline"))
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.designer_tab_inspector)) }, modifier = Modifier.testTag("designer-tab-inspector"))
                    }
                    if (tab == 0) OutlinePanel(session, palette, Modifier.fillMaxWidth())
                    else InspectorPanel(session, palette, onPickSlotComponent = { slot -> paletteTarget = slot; paletteOpen = true }, modifier = Modifier.fillMaxWidth())
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
        if (issuesDialog) IssuesDialog(session.diagnostics, session.previewProblems, palette, onSelect = { session.selectedId = it }) { issuesDialog = false }
        if (exitDialog) AlertDialog(
            onDismissRequest = { exitDialog = false },
            title = { Text(stringResource(R.string.designer_back_discard_title)) },
            text = { Text(stringResource(R.string.designer_back_discard_message)) },
            confirmButton = { TextButton(onClick = { save(); finish() }, modifier = Modifier.testTag("designer-exit-save")) { Text(stringResource(R.string.designer_save_exit)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { io.clearDraft(source); finish() }, modifier = Modifier.testTag("designer-exit-discard")) { Text(stringResource(R.string.designer_discard)) }
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
                        try {
                            session.replace(ScriptReader().read(draft))
                            session.dirty = true
                        } catch (error: DesignerParseError) {
                            io.clearDraft(source)
                        }
                    }, modifier = Modifier.testTag("designer-draft-restore")) { Text(stringResource(R.string.designer_draft_restore)) }
                },
                dismissButton = { TextButton(onClick = { pendingDraft = null; io.clearDraft(source) }, modifier = Modifier.testTag("designer-draft-discard")) { Text(stringResource(R.string.designer_draft_discard)) } },
            )
        }
        externalChange?.let { changed ->
            AlertDialog(
                onDismissRequest = { externalChange = null },
                title = { Text(stringResource(R.string.designer_external_changed_title)) },
                text = { Text(stringResource(R.string.designer_external_changed_message)) },
                confirmButton = {
                    TextButton(onClick = { externalChange = null; reloadFrom(session, changed); io.clearDraft(source) }, modifier = Modifier.testTag("designer-external-reload")) { Text(stringResource(R.string.designer_reload)) }
                },
                dismissButton = {
                    TextButton(onClick = { externalChange = null; baseline = changed }, modifier = Modifier.testTag("designer-external-keep")) { Text(stringResource(R.string.designer_keep_mine)) }
                },
            )
        }
    }

    companion object {
        const val EXTRA_PROJECT = "project"

        fun intent(context: Context, projectName: String): Intent = Intent(context, DesignerActivity::class.java).putExtra(EXTRA_PROJECT, projectName)

        /** Opens an external document the way the host editor or a file manager would hand it over. */
        fun documentIntent(context: Context, uri: Uri, writable: Boolean): Intent = Intent(Intent.ACTION_EDIT, uri)
            .setClass(context, DesignerActivity::class.java)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or if (writable) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
    }
}
