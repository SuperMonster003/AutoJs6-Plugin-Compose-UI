package io.github.supermonster003.autojs6.plugin.compose.ui.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.compose.ui.BuildConfig
import io.github.supermonster003.autojs6.plugin.compose.ui.ComposeUiPlugin
import io.github.supermonster003.autojs6.plugin.compose.ui.R
import kotlinx.coroutines.launch

/**
 * Component gallery (roadmap F.5): every catalog component with a live Material 3 preview rendered in
 * this process and the matching script, which can be copied or handed to the installed AutoJs6 host.
 * The previews are plain Compose showcases; the real script path runs only inside the host.
 */
internal class GalleryActivity : StandaloneActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        LauncherIcons.normalizeAsync(this)
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun Content(palette: StandalonePalette) {
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        val entry = selected?.let(GalleryCatalog::entry)
        BackHandler(enabled = entry != null) { selected = null }
        if (entry == null) GalleryList(palette) { selected = it } else GalleryDetail(entry, palette) { selected = null }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GalleryList(palette: StandalonePalette, onOpen: (String) -> Unit) {
        val context = LocalContext.current
        Scaffold(
            containerColor = Color(palette.background),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(palette.background), titleContentColor = Color(palette.text)),
                    actions = {
                        IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }, modifier = Modifier.testTag("gallery-settings")) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title), tint = Color(palette.muted))
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("gallery-list")) {
                GalleryCatalog.categories.forEach { category ->
                    item(key = "category-${category.id}") {
                        Text(
                            stringResource(category.title),
                            color = Color(palette.muted),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
                        )
                    }
                    items(category.entries, key = { it.component }) { item ->
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(item.component) }
                                .testTag("gallery-${item.component}"),
                        ) {
                            Text(
                                item.component,
                                color = Color(palette.text),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                            )
                            HorizontalDivider(color = Color(palette.divider), modifier = Modifier.padding(start = 24.dp))
                        }
                    }
                }
                item(key = "note") {
                    Text(
                        stringResource(R.string.gallery_note, BuildConfig.VERSION_NAME, ComposeUiPlugin.REQUIRED_HOST_VERSION),
                        color = Color(palette.muted),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GalleryDetail(entry: GalleryEntry, palette: StandalonePalette, onBack: () -> Unit) {
        val context = LocalContext.current
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val copied = stringResource(R.string.gallery_copied)
        val missingHost = stringResource(R.string.gallery_run_missing_host)
        val runFailed = stringResource(R.string.gallery_run_failed)
        val script = remember(entry) { entry.script() }
        Scaffold(
            containerColor = Color(palette.background),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(entry.component) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(palette.background), titleContentColor = Color(palette.text)),
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("gallery-back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(palette.muted))
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                SectionTitle(stringResource(R.string.gallery_section_preview), palette)
                Surface(
                    color = Color(palette.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("gallery-preview"),
                ) {
                    Box(modifier = Modifier.padding(16.dp)) { GalleryPreview(entry.component) }
                }
                SectionTitle(stringResource(R.string.gallery_section_code), palette)
                Surface(color = Color(palette.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        script,
                        color = Color(palette.text),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(16.dp).testTag("gallery-code"),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { if (ScriptLauncher.copy(context, entry.component, script)) scope.launch { snackbar.showSnackbar(copied) } },
                        modifier = Modifier.testTag("gallery-copy"),
                    ) { Text(stringResource(R.string.gallery_copy)) }
                    Button(
                        onClick = {
                            when (ScriptLauncher.run(context, script)) {
                                ScriptLauncher.Result.STARTED -> Unit
                                ScriptLauncher.Result.HOST_MISSING -> scope.launch { snackbar.showSnackbar(missingHost) }
                                ScriptLauncher.Result.FAILED -> scope.launch { snackbar.showSnackbar(runFailed) }
                            }
                        },
                        modifier = Modifier.testTag("gallery-run"),
                    ) { Text(stringResource(R.string.gallery_run)) }
                }
                Text(
                    stringResource(R.string.gallery_note, BuildConfig.VERSION_NAME, ComposeUiPlugin.REQUIRED_HOST_VERSION),
                    color = Color(palette.muted),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
    }

    @Composable
    private fun SectionTitle(title: String, palette: StandalonePalette) {
        Text(
            title,
            color = Color(palette.muted),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(top = 24.dp, bottom = 8.dp),
        )
    }
}
