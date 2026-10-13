package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.autojs.plugin.compose.api.model.ThemeSpec

/**
 * The editing screen (roadmap P8.3 / D36 / D40): the real-renderer canvas on top, an outline / inspector
 * panel below, a palette sheet for inserting components, undo / redo, explicit save and automatic drafts.
 * Runs in the plugin's own process; nothing here executes a script.
 */
internal class DesignerActivity : StandaloneActivity() {
    private lateinit var store: ProjectStore
    private lateinit var projectName: String
    private var session: DesignerSession? = null
    private var parseError: DesignerParseError? = null
    private var pendingDraft: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        store = ProjectStore(this)
        projectName = intent.getStringExtra(EXTRA_PROJECT)?.takeIf { ProjectStore.isValidName(it) } ?: run { finish(); super.onCreate(savedInstanceState); return }
        if (!store.exists(projectName)) { finish(); super.onCreate(savedInstanceState); return }
        pendingDraft = store.pendingDraft(projectName)
        load(store.read(projectName))
        super.onCreate(savedInstanceState)
    }

    private fun load(script: String): Boolean = try {
        session = DesignerSession(ScriptReader().read(script))
        parseError = null
        true
    } catch (error: DesignerParseError) {
        parseError = error
        false
    }

    override fun onStop() {
        session?.let { if (it.dirty) store.writeDraft(projectName, it.script()) }
        super.onStop()
    }

    @Composable
    override fun Content(palette: StandalonePalette) {
        val error = parseError
        val current = session
        if (error != null || current == null) {
            ParseErrorScreen(error, palette)
            return
        }
        Editor(current, palette)
    }

    @Composable
    private fun ParseErrorScreen(error: DesignerParseError?, palette: StandalonePalette) {
        Column(Modifier.fillMaxSize().background(Color(palette.background)).padding(24.dp)) {
            Text(projectName, color = Color(palette.text), fontSize = 18.sp)
            Text(
                error?.let { stringResource(R.string.designer_parse_error, it.line, it.column, it.reason) } ?: "",
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
        val insertFailed = stringResource(R.string.designer_insert_failed)
        val copied = stringResource(R.string.gallery_copied)
        val missingHost = stringResource(R.string.gallery_run_missing_host)
        val runFailed = stringResource(R.string.gallery_run_failed)
        val theme = remember(appearance) { ThemeSpec(seedArgb = appearance.primary, dark = appearance.dark) }
        val errors = session.diagnostics.count { it.severity == Severity.ERROR }

        fun save() {
            store.write(projectName, session.script())
            store.clearDraft(projectName)
            session.markSaved()
            scope.launch { snackbar.showSnackbar(savedText) }
        }

        LaunchedEffect(session.revision) {
            if (session.dirty) {
                delay(1000)
                store.writeDraft(projectName, session.script())
            }
        }
        BackHandler(enabled = session.dirty) { exitDialog = true }

        Scaffold(
            containerColor = Color(palette.background),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(projectName + if (session.dirty) " *" else "", fontSize = 16.sp, maxLines = 1) },
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
                                    if (ScriptLauncher.copy(context, projectName, session.script())) scope.launch { snackbar.showSnackbar(copied) }
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.gallery_run)) }, onClick = {
                                    menuOpen = false
                                    when (ScriptLauncher.run(context, session.script())) {
                                        ScriptLauncher.Result.STARTED -> Unit
                                        ScriptLauncher.Result.HOST_MISSING -> scope.launch { snackbar.showSnackbar(missingHost) }
                                        ScriptLauncher.Result.FAILED -> scope.launch { snackbar.showSnackbar(runFailed) }
                                    }
                                })
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
            Column(Modifier.fillMaxSize().padding(padding)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .background(Color(palette.surfaceVariant))
                        .testTag("designer-canvas"),
                ) {
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
                Column(Modifier.fillMaxWidth().weight(0.9f)) {
                    TabRow(selectedTabIndex = tab, containerColor = Color(palette.background), contentColor = Color(palette.text)) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.designer_tab_outline)) }, modifier = Modifier.testTag("designer-tab-outline"))
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.designer_tab_inspector)) }, modifier = Modifier.testTag("designer-tab-inspector"))
                    }
                    if (tab == 0) OutlinePanel(session, palette, Modifier.fillMaxWidth())
                    else InspectorPanel(session, palette, onPickSlotComponent = { slot -> paletteTarget = slot; paletteOpen = true }, modifier = Modifier.fillMaxWidth())
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
                    TextButton(onClick = { store.clearDraft(projectName); finish() }, modifier = Modifier.testTag("designer-exit-discard")) { Text(stringResource(R.string.designer_discard)) }
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
                            store.clearDraft(projectName)
                        }
                    }, modifier = Modifier.testTag("designer-draft-restore")) { Text(stringResource(R.string.designer_draft_restore)) }
                },
                dismissButton = { TextButton(onClick = { pendingDraft = null; store.clearDraft(projectName) }, modifier = Modifier.testTag("designer-draft-discard")) { Text(stringResource(R.string.designer_draft_discard)) } },
            )
        }
    }

    companion object {
        const val EXTRA_PROJECT = "project"

        fun intent(context: Context, projectName: String): Intent = Intent(context, DesignerActivity::class.java).putExtra(EXTRA_PROJECT, projectName)
    }
}
