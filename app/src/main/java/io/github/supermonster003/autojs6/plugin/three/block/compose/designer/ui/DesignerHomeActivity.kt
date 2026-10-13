package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.GalleryActivity
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.LauncherIcons
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
        val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val imported = runCatching { store.import(contentResolver, uri) }.getOrNull()
            if (imported == null) scope.launch { snackbar.showSnackbar(importFailed) } else { listRevision++; startActivity(DesignerActivity.intent(context, imported.name)) }
        }
        Scaffold(
            containerColor = Color(palette.background),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
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
                            Column(Modifier.weight(1f)) {
                                Text(project.name, color = Color(palette.text), fontSize = 16.sp)
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
            // Roadmap P9.2: a new project is a JavaScript or a TSX script.
            var tsx by remember { mutableStateOf(false) }
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
                            FilterChip(selected = !tsx, onClick = { tsx = false }, label = { Text(stringResource(R.string.designer_language_javascript)) }, modifier = Modifier.padding(end = 8.dp).testTag("home-language-js"))
                            FilterChip(selected = tsx, onClick = { tsx = true }, label = { Text(stringResource(R.string.designer_language_tsx)) }, modifier = Modifier.testTag("home-language-tsx"))
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
                        val project = if (tsx) store.create(stem, ProjectStore.TSX_TEMPLATE, extension) else store.create(stem)
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
                text = { Text(stringResource(R.string.designer_delete_project_message, project.name)) },
                confirmButton = { TextButton(onClick = { store.delete(project.name); deleting = null; listRevision++ }, modifier = Modifier.testTag("home-delete-confirm")) { Text(stringResource(R.string.designer_delete_project)) } },
                dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.designer_cancel)) } },
            )
        }
    }
}
