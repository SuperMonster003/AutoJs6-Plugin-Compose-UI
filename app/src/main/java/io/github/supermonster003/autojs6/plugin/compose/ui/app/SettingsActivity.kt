package io.github.supermonster003.autojs6.plugin.compose.ui.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.compose.ui.BuildConfig
import io.github.supermonster003.autojs6.plugin.compose.ui.ComposeUiPlugin
import io.github.supermonster003.autojs6.plugin.compose.ui.R
import kotlinx.coroutines.launch

/**
 * Settings of the plugin's own screens (standalone settings specification): the appearance group follows
 * AutoJs6 by default, every picker edits a draft and only an explicit confirmation persists a value.
 */
internal class SettingsActivity : StandaloneActivity() {
    private enum class Dialog { LANGUAGE, DARK_MODE, THEME_COLOR, LAUNCHER_ICON }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(palette: StandalonePalette) {
        val context = LocalContext.current
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var revision by remember { mutableIntStateOf(0) }
        val preferences = remember(revision) { AppearancePreferences.read(context) }
        val iconMode = remember(revision) { LauncherIcons.current(context) }
        var dialog by remember { mutableStateOf<Dialog?>(null) }
        val saveError = stringResource(R.string.settings_error)
        val iconApplied = stringResource(R.string.launcher_icon_applied_note)
        val iconFailed = stringResource(R.string.launcher_icon_failed)

        fun saveAppearance(next: AppearancePreferences): Boolean {
            val saved = next.save(context)
            if (saved) { revision++; refreshAppearance() } else scope.launch { snackbar.showSnackbar(saveError) }
            return saved
        }

        Scaffold(
            containerColor = Color(palette.background),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(palette.background), titleContentColor = Color(palette.text)),
                    navigationIcon = {
                        IconButton(onClick = { finish() }, modifier = Modifier.testTag("settings-back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(palette.muted))
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("settings-page")) {
                GroupTitle(stringResource(R.string.settings_appearance), palette)
                SettingsRow(stringResource(R.string.app_settings_language), languageLabel(preferences.language), Icons.Filled.Place, palette, "settings-language") { dialog = Dialog.LANGUAGE }
                SettingsRow(stringResource(R.string.app_settings_dark_mode), darkModeLabel(preferences.darkMode), Icons.Filled.Face, palette, "settings-night") { dialog = Dialog.DARK_MODE }
                SettingsRow(
                    stringResource(R.string.app_settings_theme_color),
                    preferences.color?.let(ThemeColorValue::hex) ?: stringResource(R.string.app_settings_follow_autojs6),
                    Icons.Filled.Star, palette, "settings-color",
                ) { dialog = Dialog.THEME_COLOR }
                SettingsRow(stringResource(R.string.launcher_icon_title), stringResource(iconLabel(iconMode)), Icons.Filled.Home, palette, "settings-icon", divider = false) { dialog = Dialog.LAUNCHER_ICON }
                Spacer(Modifier.height(8.dp))
                GroupTitle(stringResource(R.string.about_title), palette)
                SettingsRow(
                    stringResource(R.string.about_version),
                    stringResource(R.string.about_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, stringResource(R.string.plugin_version_date)),
                    Icons.Filled.Info, palette, "about-version", chevron = false,
                ) {}
                SettingsRow(
                    stringResource(R.string.about_host_requirement),
                    stringResource(R.string.about_host_requirement_value, "6.8.0", ComposeUiPlugin.REQUIRED_HOST_VERSION),
                    Icons.Filled.Build, palette, "about-host", chevron = false,
                ) {}
                SettingsRow(stringResource(R.string.about_developer), ComposeUiPlugin.AUTHOR, Icons.Filled.AccountCircle, palette, "about-developer", divider = false) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_PAGE))) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        when (dialog) {
            Dialog.LANGUAGE -> ConfirmedChoiceDialog(
                title = stringResource(R.string.app_settings_language),
                options = AppearancePreferences.LANGUAGES.map { SettingsChoice(languageLabel(it)) },
                selected = AppearancePreferences.LANGUAGES.indexOf(preferences.language).coerceAtLeast(0),
                palette = palette,
                tag = "dialog-language",
                onDismiss = { dialog = null },
            ) { index -> if (saveAppearance(preferences.copy(language = AppearancePreferences.LANGUAGES[index]))) dialog = null }
            Dialog.DARK_MODE -> ConfirmedChoiceDialog(
                title = stringResource(R.string.app_settings_dark_mode),
                options = AppearancePreferences.DARK_MODES.map { SettingsChoice(darkModeLabel(it)) },
                selected = AppearancePreferences.DARK_MODES.indexOf(preferences.darkMode).coerceAtLeast(0),
                palette = palette,
                tag = "dialog-night",
                onDismiss = { dialog = null },
            ) { index -> if (saveAppearance(preferences.copy(darkMode = AppearancePreferences.DARK_MODES[index]))) dialog = null }
            Dialog.THEME_COLOR -> ThemeColorDialog(
                current = preferences.color,
                hostColor = HostAppearanceReader.cached()?.primary ?: AppearancePreferences.DEFAULT_COLOR,
                palette = palette,
                onDismiss = { dialog = null },
            ) { color -> if (saveAppearance(preferences.copy(color = color))) dialog = null }
            Dialog.LAUNCHER_ICON -> ConfirmedChoiceDialog(
                title = stringResource(R.string.launcher_icon_title),
                options = LauncherIconMode.entries.map { mode ->
                    SettingsChoice(stringResource(iconLabel(mode)), when (mode) {
                        LauncherIconMode.AUTO -> stringResource(R.string.launcher_icon_auto_note)
                        LauncherIconMode.TRANSPARENT -> stringResource(R.string.launcher_icon_transparent_note)
                        else -> null
                    })
                },
                selected = iconMode.ordinal,
                palette = palette,
                tag = "dialog-icon",
                onDismiss = { dialog = null },
            ) { index ->
                val result = runCatching { LauncherIcons.select(context, LauncherIconMode.entries[index]) }
                scope.launch { snackbar.showSnackbar(if (result.isSuccess) iconApplied else iconFailed) }
                if (result.isSuccess) { revision++; dialog = null }
            }
            null -> Unit
        }
    }

    @Composable
    private fun languageLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(R.string.app_settings_follow_autojs6)
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.app_settings_follow_system)
        "zh-Hans" -> stringResource(R.string.app_language_zh_hans)
        "zh-Hant-HK" -> stringResource(R.string.app_language_zh_hant_hk)
        "zh-Hant-TW" -> stringResource(R.string.app_language_zh_hant_tw)
        "en" -> stringResource(R.string.app_language_en)
        "fr" -> stringResource(R.string.app_language_fr)
        "es" -> stringResource(R.string.app_language_es)
        "ja" -> stringResource(R.string.app_language_ja)
        "ko" -> stringResource(R.string.app_language_ko)
        "ru" -> stringResource(R.string.app_language_ru)
        "ar" -> stringResource(R.string.app_language_ar)
        else -> stringResource(R.string.app_settings_follow_autojs6)
    }

    @Composable
    private fun darkModeLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.app_settings_follow_system)
        AppearancePreferences.LIGHT -> stringResource(R.string.app_settings_always_light)
        AppearancePreferences.DARK -> stringResource(R.string.app_settings_always_dark)
        else -> stringResource(R.string.app_settings_follow_autojs6)
    }

    private fun iconLabel(mode: LauncherIconMode): Int = when (mode) {
        LauncherIconMode.LIGHT -> R.string.launcher_icon_light
        LauncherIconMode.DARK -> R.string.launcher_icon_dark
        LauncherIconMode.AUTO -> R.string.launcher_icon_auto
        LauncherIconMode.TRANSPARENT -> R.string.launcher_icon_transparent
    }

    @Composable
    private fun GroupTitle(title: String, palette: StandalonePalette) {
        Text(
            title,
            color = Color(palette.muted),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        )
    }

    @Composable
    private fun SettingsRow(
        title: String,
        summary: String,
        icon: ImageVector,
        palette: StandalonePalette,
        tag: String,
        chevron: Boolean = true,
        divider: Boolean = true,
        onClick: () -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp).testTag(tag),
        ) {
            Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.CenterStart) {
                Icon(icon, contentDescription = null, tint = Color(palette.muted), modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color(palette.text), fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(summary, color = Color(palette.muted), fontSize = 14.sp, modifier = Modifier.testTag("$tag-summary"))
            }
            if (chevron) {
                Spacer(Modifier.width(16.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(palette.muted))
            }
        }
        if (divider) HorizontalDivider(color = Color(palette.divider), modifier = Modifier.padding(start = 64.dp))
    }

    private companion object {
        const val DEVELOPER_PAGE = "https://github.com/SuperMonster003"
    }
}
