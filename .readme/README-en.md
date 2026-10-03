<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>A plugin that brings Jetpack Compose and Material 3 user interfaces to AutoJs6 scripts</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Compose-UI?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

This document is available in the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ar.md)

******

### Introduction

******

Compose UI is a user interface rendering plugin for AutoJs6. Scripts declare interfaces through the host-provided `compose` / `$compose` entry, and the plugin renders them inside the host process with Jetpack Compose and Material 3. The current preview supports both `"ui";` activity content and floating windows from non-UI scripts.

The plugin ships no standalone screens and adds no launcher entry. The host discovers it through the INFO service, reads its version and compatibility data, then loads the renderer inside the host process according to the contract (`org.autojs.plugin.compose.api`). The UI tree, state, and events live on the script side; the renderer only applies patches to the Compose composition and forwards user events back to the script.

******

### Status

******

P4 development preview: the callable compose / $compose entry, 29 node factories, retained handles, reactive state/render/ref, batch/post/theme, UI script mounting, and raw or resizable floating windows work with a matching local AutoJs6 host build. Availability probes, typed errors, and session cleanup are included. Five examples for a counter, form, 1000-item list, floating HUD, and themes are bundled and synchronized to the matching host's Compose UI sample category. Complete API documentation, type declarations, and the full P5 verification matrix remain pending. This is a local preview without an official release.

******

### Features

******

Capabilities of the current development preview:

- Declarative UI: `compose.state` + `compose.mount(render)` re-render on state changes, while long-lived node handles (`compose.Text({...})` and friends) allow direct property and child updates
- Material 3 component core set: layouts (Column / Row / Box / LazyColumn and more), text, buttons, text fields, switches, sliders, progress indicators, cards, dialogs
- Chained modifiers: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` keeps operation order, and scoped operations are validated on the host side
- Two hosting surfaces: `compose.mount` or callable `compose` / `$compose` for `"ui";` activity content, and `compose.floaty` for raw or resizable floating windows, including non-UI scripts
- In-process rendering: the renderer runs inside the host process without any cross-process UI bridge, so events and state updates stay low-latency
- Single package: no ABI variants and no first-party native code (only the AndroidX graphics-path helper bundled with Compose, built in for all four ABIs), one APK fits every device
- Script entry: `compose` / `$compose`, 29 node factories and retained handles, plus `compose.ref`, `compose.batch`, `compose.post`, and `compose.theme`
- Integration guards: availability probes return unavailable for missing or incompatible plugins, errors use `ComposeError`, and closing a session or stopping its script releases owned windows and callbacks
- Five runnable examples for a counter, form validation, a keyed 1000-item list, a non-UI floating HUD, and themes, with prerequisites and an index, synchronized to the matching host's Compose UI sample category

******

### Usage

******

1. Install a matching local AutoJs6 build with the compose script entry (minimum 6.8.0 / 5316)
2. Install this plugin APK (there is nothing to open, the plugin has no launcher entry)
3. Confirm in the AutoJs6 plugin center that Compose UI is recognized and enabled
4. Use `compose` or `$compose` in scripts; mount activity content with `compose.mount`, or grant the host overlay permission and use `compose.floaty`

******

### Quick Start

******

The counter and floating HUD below can run with the matching local preview host. Grant the host permission to display over other apps before running the HUD:

```js
"ui";

// Counter (declarative render layer)
let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `Clicked ${count.value} times`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, 'Add one'),
]));
```

```js
// Floating HUD (node handle layer)
let status = compose.Text({ text: 'Preparing...' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ onClick: () => win.close() }, 'Close'),
]), { x: 50, y: 300, raw: true });

threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => status.set({ text: `Progress ${i}%` }));
    }
});
```

Five runnable examples are bundled under `assets/examples/` and listed in `index.json`; the matching host provides the same scripts in its Compose UI sample category (`sample/Compose UI/`). Each header explains execution mode and permission prerequisites. The complete API reference and TypeScript declarations remain pending; roadmap appendix A defines the current API shape.

******

### Compatibility

******

Runtime requirements and limits of the plugin:

- Minimum AutoJs6 version: 6.8.0 (5316) or later; older hosts flag the plugin as incompatible in the plugin center
- Android version: 7.0 (API 24) or later
- Processor architecture: arm64-v8a / armeabi-v7a / x86_64 / x86 (all four built into the single APK, no per-architecture download)
- Compose version: bundled with the plugin (BOM 2026.09.00), independent of the host's Compose runtime
- Contract version: 1; host and plugin negotiate the contract version and refuse to load with a clear error when it does not match

******

### FAQ

******

- Why is there no plugin icon after installing? The plugin has no standalone UI and no launcher entry; look it up in the AutoJs6 plugin center
- Why is `compose` missing? The global object is supplied by the matching local host build; installing this plugin APK alone does not add it
- Do other UI plugins need to be uninstalled? No, Compose UI does not interfere with the existing `ui` module or other plugins
- Do scripts need changes after a plugin update? Not while the contract version stays the same; contract upgrades are called out explicitly in the changelog
- What do floating windows require? Grant overlay permission to the host. Call `window.requestFocus()` before text input; if a HyperOS window is not visible, return to the desktop. Missing permission reports PERMISSION_REQUIRED without opening a permission prompt automatically

******

### Permissions and Security

******

The plugin requests no Android runtime permissions and never touches the network, storage, or sensors.

- Component protection: both the Wake Activity and the INFO service are guarded by the `org.autojs.permission.PLUGIN` signature permission, so only the AutoJs6 host can reach them
- No background activity: the plugin has no resident services, broadcast receivers, or scheduled jobs, and consumes no resources while the host is not loading it
- Data boundary: the plugin never reads or writes script data or user files; UI state exists only in the host process memory
- Backup policy: app backup and device transfer are disabled, and the plugin holds no data worth migrating

When loading the renderer the host keeps its own script permission model; the plugin does not widen the system capabilities scripts can reach.

******

### Plugin Interface

******

Identifiers exposed to the host:

```text
application id: io.github.supermonster003.autojs6.plugin.compose.ui
plugin id: compose-ui
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: compose-ui
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 1)
minimum host build: 5316 (6.8.0)
```

The host discovers the plugin through `org.autojs.plugin.INFO` and reads capability data such as `requiresHostVersion`; the renderer factory class is declared by the `org.autojs.plugin.compose.RENDERER_FACTORY` meta-data, and the host creates a class loader from the plugin APK path (with the host as parent) and instantiates it inside the host process.

******

### Roadmap

******

Milestones, design decisions, and acceptance criteria are tracked in a single roadmap:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.0.0

_2026/10/03_

- `Hint` P4 development preview: the callable compose / $compose entry, 29 node factories, retained handles, reactive state/render/ref, batch/post/theme, UI script mounting, and raw or resizable floating windows work with a matching local AutoJs6 host build. Availability probes, typed errors, and session cleanup are included. Five examples for a counter, form, 1000-item list, floating HUD, and themes are bundled and synchronized to the matching host's Compose UI sample category. Complete API documentation, type declarations, and the full P5 verification matrix remain pending. This is a local preview without an official release
- `Hint` Requires AutoJs6 6.8.0 (5316) or later
- `Feature` Plugin repository skeleton: platform versions plugin build chain, Jetpack Compose BOM 2026.09.00 dependencies, Wake Activity activation protocol, and INFO service (category compose-ui)
- `Feature` README, plugin center instruction, and changelog in 10 languages, generated from JSON sources
- `Feature` Preview rendering supports layouts, text, icons, images, buttons, selection controls, and sliders; supplied bitmaps remain owned and recycled by the caller
- `Feature` All 20 modifier operations preserve declaration order, with layout scope checks, scrolling, and accessibility labels
- `Feature` Material 3 themes support seed colors, light and dark modes, Android 12+ system dynamic colors, font families, and text scaling
- `Feature` Interface updates apply atomically and rejected updates keep the last valid view; controlled inputs report changes through queued callbacks, and closing releases callbacks
- `Feature` Preview text fields preserve selection and IME composition, support focus and explicit edits, and reject delayed edits that would overwrite newer input
- `Feature` Preview adds lazy lists with stable item keys and indexed scrolling, Scaffold and top app bar slots, controlled dialogs, progress indicators, and queued Snackbar action or dismissal callbacks
- `Feature` The script preview exposes callable compose / $compose, 29 node factories, retained handles, reactive state/render/ref, batching, posting, and theme control
- `Feature` UI scripts can mount Compose content; replacing the mount or stopping the script releases the old session and callbacks
- `Feature` Non-UI scripts can create raw or resizable Compose floating windows, change pixel geometry, touch and focus settings, and close them through their controls, floaty.closeAll, or script termination
- `Feature` Five runnable examples for a counter, form validation, a keyed 1000-item list, a non-UI floating HUD, and themes, with prerequisites and an index, synchronized to the matching host's Compose UI sample category
- `Fix` Compose UI rejects mounting another page or floating window inside a render callback and keeps the current page; pages can be mounted again after plugin updates
- `Improvement` Availability probes and ComposeError consistently report missing, disabled, unauthorized or incompatible plugins, permission failures and closed sessions; lifecycle cleanup also covers windows canceled before native attachment; Updating, uninstalling or disabling the plugin closes its active sessions and reports the corresponding error
- `Dependency` Attach common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, hash-locked)
- `Dependency` Attach Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `Dependency` Attach compose-ui-api.aar V1 aligned to AutoJs6 6.8.0 (5316) (MPL 2.0, hash-locked), with shared dependencies aligned to the host
- `Dependency` Attach Compose UI Test managed by BOM 2026.09.00 (Apache 2.0, tests only)
- `Dependency` Attach JaCoCo version 0.8.14 (optional test coverage only, excluded from release packages)

##### For more release history, see

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build

******

After cloning, build directly with the Gradle Wrapper; the Android Gradle Plugin and Kotlin versions are selected automatically by the platform versions plugin according to the current IDE environment.

Build the debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run JVM unit tests and package the on-device contract tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Build the release APK (requires `sign.properties` and the signing key):

```powershell
.\gradlew.bat :app:assembleRelease
```

Verify the signature and emit the digest-suffixed release file:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verify that the localized documents match their sources:

```powershell
py .python\generate_markdown.py --check
```

Building requires JDK 21 or later. After editing the sources under `.readme` or `.changelog`, run `py .python\generate_markdown.py` to regenerate every document.

******

### Documentation Layout

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

The README, the plugin center instruction, and the changelog are all generated from the JSON sources under `.readme` and `.changelog`; do not edit the generated Markdown files directly.

******

### License

******

This project is released under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE). License information of third-party components is listed in [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: https://docs.autojs6.com
- compose module documentation: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
