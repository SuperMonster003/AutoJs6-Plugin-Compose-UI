package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryActivity
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.LauncherIcons
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.ScriptText
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsxParserClient
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import kotlinx.coroutines.launch

/**
 * The launcher entry (roadmap D38): the designer's project list with new / import actions, and the
 * gallery and settings reachable from the top bar. Projects are script files in private storage.
 */
/** Where a new ui project keeps its layout (roadmap P11.3 / D66, D67). */
private enum class UiLayoutChoice { SCRIPT, AUTOJS6, ANDROID, TSX }

/** What the home list calls a project: the file name, or the folder's name with a trailing slash for a folder project. */
private val ProjectStore.Project.label: String get() = if (folder) "$displayName/" else name

internal class DesignerHomeActivity : StandaloneActivity() {
    private lateinit var store: ProjectStore
    private var listRevision by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        LauncherIcons.normalizeAsync(this)
        store = ProjectStore(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        listRevision++
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(palette: StandalonePalette) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val projects = remember(listRevision) { store.list() }
        var creating by remember { mutableStateOf(false) }
        var deleting by remember { mutableStateOf<ProjectStore.Project?>(null) }
        var menuOpen by remember { mutableStateOf(false) }
        val importFailed = stringResource(R.string.designer_import_failed)
        val noScript = stringResource(R.string.designer_import_no_script)
        fun opened(imported: Result<ProjectStore.Project>) {
            imported.onSuccess { project -> listRevision++; startActivity(DesignerActivity.intent(context, project.name)) }
                .onFailure { error -> scope.launch { snackbar.showSnackbar(if (error is ProjectStore.NoScriptException) noScript else importFailed) } }
        }
        val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            opened(runCatching { store.import(contentResolver, uri) })
        }
        // Roadmap P11.3 / D62: a folder (with main.js) becomes a folder project.
        val folderImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            opened(runCatching { store.importTree(contentResolver, uri) })
        }
        Scaffold(
            containerColor = Color(palette.background),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    // The Latin title keeps its order (3-...) in RTL locales.
                    title = { Text(stringResource(R.string.app_name), style = LocalTextStyle.current.copy(textDirection = TextDirection.Content)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(palette.background), titleContentColor = Color(palette.text)),
                    actions = {
                        IconButton(onClick = { context.startActivity(Intent(context, GalleryActivity::class.java)) }, modifier = Modifier.testTag("home-gallery")) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.designer_open_gallery), tint = Color(palette.muted))
                        }
                        IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }, modifier = Modifier.testTag("home-settings")) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title), tint = Color(palette.muted))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("home-menu")) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.designer_import_project), tint = Color(palette.muted))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_import_project)) }, onClick = {
                                    menuOpen = false
                                    importer.launch(ProjectStore.SCRIPT_DOCUMENT_TYPES)
                                }, modifier = Modifier.testTag("home-import"))
                                DropdownMenuItem(text = { Text(stringResource(R.string.designer_import_folder)) }, onClick = {
                                    menuOpen = false
                                    folderImporter.launch(null)
                                }, modifier = Modifier.testTag("home-import-folder"))
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { creating = true }, modifier = Modifier.testTag("home-new")) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.designer_new_project))
                }
            },
        ) { padding ->
            if (projects.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.designer_empty_projects), color = Color(palette.muted), fontSize = 15.sp, modifier = Modifier.padding(32.dp).testTag("home-empty"))
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("home-list")) {
                    items(projects, key = { it.name }) { project ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { startActivity(DesignerActivity.intent(context, project.name)) }
                                .padding(start = 24.dp, end = 8.dp, top = 14.dp, bottom = 14.dp).testTag("home-project-${project.name}"),
                        ) {
                            // Roadmap P10.4: the kind of each project at a glance (Compose JavaScript, Compose TSX, ui XML).
                            Text(
                                project.kind.name,
                                color = Color(palette.muted),
                                fontSize = 11.sp,
                                style = ScriptText.code,
                                modifier = Modifier.width(36.dp).testTag("home-kind-${project.name}"),
                            )
                            Column(Modifier.weight(1f)) {
                                // Roadmap P11.3: a folder project shows as its folder (`demo/`), the script inside it being main.js.
                                Text(project.label, color = Color(palette.text), fontSize = 16.sp)
                                Text(
                                    stringResource(R.string.designer_modified, DateFormat.getMediumDateFormat(context).format(project.modifiedAt) + " " + DateFormat.getTimeFormat(context).format(project.modifiedAt)),
                                    color = Color(palette.muted), fontSize = 12.sp,
                                )
                            }
                            IconButton(onClick = { deleting = project }, modifier = Modifier.testTag("home-delete-${project.name}")) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.designer_delete_project), tint = Color(palette.muted))
                            }
                        }
                        HorizontalDivider(color = Color(palette.divider), modifier = Modifier.padding(start = 24.dp))
                    }
                }
            }
        }
        if (creating) {
            var name by remember { mutableStateOf("") }
            // Roadmap P9.2 / P10.4: a new project is a Compose JavaScript script, a Compose TSX script, or a ui (XML) script.
            var kind by remember { mutableStateOf(ProjectStore.Kind.JS) }
            // Roadmap P11.3 / D66, D67: a ui project keeps its layout in the script, or starts as a folder with a layout file.
            var uiLayout by remember { mutableStateOf(UiLayoutChoice.SCRIPT) }
            // Roadmap P11.7: a ui project can also be a TSX script, read through the TypeScript Engine parser too.
            val tsx = kind == ProjectStore.Kind.TSX || (kind == ProjectStore.Kind.UI && uiLayout == UiLayoutChoice.TSX)
            val engineMissing = remember(tsx) { tsx && !TsxParserClient.hasParserService(context) }
            val invalid = stringResource(R.string.designer_name_invalid)
            AlertDialog(
                onDismissRequest = { creating = false },
                title = { Text(stringResource(R.string.designer_new_project)) },
                text = {
                    Column {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.designer_project_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("home-project-name"))
                        Text(stringResource(R.string.designer_project_language), color = Color(palette.muted), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                        Row {
                            FilterChip(selected = kind == ProjectStore.Kind.JS, onClick = { kind = ProjectStore.Kind.JS }, label = { Text(stringResource(R.string.designer_language_javascript)) }, modifier = Modifier.padding(end = 8.dp).testTag("home-language-js"))
                            FilterChip(selected = kind == ProjectStore.Kind.TSX, onClick = { kind = ProjectStore.Kind.TSX }, label = { Text(stringResource(R.string.designer_language_tsx)) }, modifier = Modifier.padding(end = 8.dp).testTag("home-language-tsx"))
                            FilterChip(selected = kind == ProjectStore.Kind.UI, onClick = { kind = ProjectStore.Kind.UI }, label = { Text(stringResource(R.string.designer_ui_language_ui)) }, modifier = Modifier.testTag("home-language-ui"))
                        }
                        if (kind == ProjectStore.Kind.UI) {
                            Text(stringResource(R.string.designer_ui_layout_label), color = Color(palette.muted), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                for ((choice, label) in listOf(
                                    UiLayoutChoice.SCRIPT to R.string.designer_ui_layout_in_script,
                                    UiLayoutChoice.AUTOJS6 to R.string.designer_layout_file_autojs6,
                                    UiLayoutChoice.ANDROID to R.string.designer_layout_file_android,
                                    UiLayoutChoice.TSX to R.string.designer_ui_layout_in_tsx,
                                )) {
                                    FilterChip(
                                        selected = uiLayout == choice,
                                        onClick = { uiLayout = choice },
                                        label = { Text(stringResource(label)) },
                                        modifier = Modifier.padding(end = 8.dp).testTag("home-ui-layout-${choice.name.lowercase()}"),
                                    )
                                }
                            }
                        }
                        if (engineMissing) {
                            Text(stringResource(R.string.designer_tsx_needs_engine, TsxParserClient.MIN_VERSION_NAME), color = Color(palette.muted), fontSize = 12.sp, modifier = Modifier.testTag("home-tsx-hint"))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val extension = if (tsx) ProjectStore.TSX_EXTENSION else ProjectStore.EXTENSION
                        val stem = name.trim().removeSuffix(extension)
                        if (stem.isNotEmpty() && !ProjectStore.isValidName(stem + extension)) {
                            scope.launch { snackbar.showSnackbar(invalid) }
                            return@TextButton
                        }
                        creating = false
                        val project = when (kind) {
                            ProjectStore.Kind.TSX -> store.create(stem, ProjectStore.TSX_TEMPLATE, extension)
                            ProjectStore.Kind.UI -> when (uiLayout) {
                                UiLayoutChoice.SCRIPT -> store.create(stem, ProjectStore.UI_TEMPLATE)
                                UiLayoutChoice.AUTOJS6 -> ProjectStore.UI_FILE_TEMPLATE.let { (script, files) -> store.createFolder(stem, script, files) }
                                UiLayoutChoice.ANDROID -> ProjectStore.UI_ANDROID_TEMPLATE.let { (script, files) -> store.createFolder(stem, script, files) }
                                UiLayoutChoice.TSX -> store.create(stem, ProjectStore.TSX_UI_TEMPLATE, extension)
                            }
                            ProjectStore.Kind.JS -> store.create(stem)
                        }
                        listRevision++
                        startActivity(DesignerActivity.intent(context, project.name))
                    }, modifier = Modifier.testTag("home-create")) { Text(stringResource(R.string.designer_create)) }
                },
                dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.designer_cancel)) } },
            )
        }
        deleting?.let { project ->
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text(stringResource(R.string.designer_delete_project)) },
                text = { Text(stringResource(R.string.designer_delete_project_message, project.label)) },
                confirmButton = { TextButton(onClick = { store.delete(project.name); deleting = null; listRevision++ }, modifier = Modifier.testTag("home-delete-confirm")) { Text(stringResource(R.string.designer_delete_project)) } },
                dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.designer_cancel)) } },
            )
        }
    }
}
