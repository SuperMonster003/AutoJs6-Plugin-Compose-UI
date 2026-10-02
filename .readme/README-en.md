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

Compose UI is a user interface rendering plugin for AutoJs6. Scripts declare their interface through the host's built-in `compose` global object, and the plugin renders it inside the host process with Jetpack Compose and Material 3, giving `"ui";` mode activities and floating windows one declarative UI solution.

The plugin ships no standalone screens and adds no launcher entry. The host discovers it through the INFO service, reads its version and compatibility data, then loads the renderer inside the host process according to the contract (`org.autojs.plugin.compose.api`). The UI tree, state, and events live on the script side; the renderer only applies patches to the Compose composition and forwards user events back to the script.

******

### Status

******

P1 development preview: the V1 host loader and sessions can run a Column / Text / Button counter in a dedicated test host. The complete renderer and compose script API are still under development.

******

### Features

******

Core capabilities the plugin is set to deliver:

- Declarative UI: `compose.state` + `compose.mount(render)` re-render on state changes, while long-lived node handles (`compose.Text({...})` and friends) allow direct property and child updates
- Material 3 component core set: layouts (Column / Row / Box / LazyColumn and more), text, buttons, text fields, switches, sliders, progress indicators, cards, dialogs
- Chained modifiers: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` keeps operation order, and scoped operations are validated on the host side
- Two hosting surfaces: activity content of `"ui";` scripts (`compose.mount`) and floating windows of any script (`compose.floaty`)
- In-process rendering: the renderer runs inside the host process without any cross-process UI bridge, so events and state updates stay low-latency
- Single package: no ABI variants and no first-party native code (only the AndroidX graphics-path helper bundled with Compose, built in for all four ABIs), one APK fits every device

******

### Usage

******

1. Install AutoJs6 6.8.0 (5308) or later
2. Install this plugin APK (there is nothing to open, the plugin has no launcher entry)
3. Confirm in the AutoJs6 plugin center that Compose UI is recognized and enabled
4. Use the `compose` global object directly in scripts (rendering arrives with version 1.0.0)

******

### Quick Start

******

The examples below show the target API shape (defined in roadmap appendix A, not runnable until rendering is delivered):

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

The full API reference (component catalog, modifier operations, session objects, error codes) lives in the compose module chapter of the AutoJs6 documentation.

******

### Compatibility

******

Runtime requirements and limits of the plugin:

- AutoJs6 version: 6.8.0 (5308) or later; older hosts flag the plugin as incompatible in the plugin center
- Android version: 7.0 (API 24) or later
- Processor architecture: arm64-v8a / armeabi-v7a / x86_64 / x86 (all four built into the single APK, no per-architecture download)
- Compose version: bundled with the plugin (BOM 2026.09.00), independent of the host's Compose runtime
- Contract version: 1; host and plugin negotiate the contract version and refuse to load with a clear error when it does not match

******

### FAQ

******

- Why is there no plugin icon after installing? The plugin has no standalone UI and no launcher entry; look it up in the AutoJs6 plugin center
- Why does `compose` not work in scripts yet? This is a P0 development preview; the renderer and the script API arrive in later milestones
- Do other UI plugins need to be uninstalled? No, Compose UI does not interfere with the existing `ui` module or other plugins
- Do scripts need changes after a plugin update? Not while the contract version stays the same; contract upgrades are called out explicitly in the changelog

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
minimum host build: 5308 (6.8.0)
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

_2026/10/02_

- `Hint` P1 development preview: the V1 host loader and sessions can run a Column / Text / Button counter in a dedicated test host. The complete renderer and compose script API are still under development
- `Hint` Requires AutoJs6 6.8.0 (5308) or later (the exact minimum build is back-filled once the host-side changes land)
- `Feature` Plugin repository skeleton: platform versions plugin build chain, Jetpack Compose BOM 2026.09.00 dependencies, Wake Activity activation protocol, and INFO service (category compose-ui)
- `Feature` README, plugin center instruction, and changelog in 10 languages, generated from JSON sources
- `Feature` Preview counter supports incremental updates and callback cleanup on close; rejected updates preserve the last valid interface
- `Dependency` Attach common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, hash-locked)
- `Dependency` Attach Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `Dependency` Attach compose-ui-api.aar V1 (MPL 2.0, hash-locked), with shared dependencies aligned to the host

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
