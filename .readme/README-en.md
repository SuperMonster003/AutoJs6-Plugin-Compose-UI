<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-block-compose-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>A plugin that brings Jetpack Compose and Material 3 user interfaces to AutoJs6 scripts</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

This document is available in the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Block Compose is a user interface rendering plugin for AutoJs6. Scripts declare interfaces through the host-provided `compose` / `$compose` entry, and the plugin renders them inside the host process with Jetpack Compose and Material 3. The current preview supports both `"ui";` activity content and floating windows from non-UI scripts.

The plugin ships a visual designer and a component gallery: opening it from the launcher lands on the designer home, where script projects are created or imported and edited on the real renderer, while the gallery shows previews and example scripts of every component; script interfaces are still rendered inside the host process. The host discovers the plugin through the INFO service, reads its version and compatibility information, and instantiates the renderer factory in its own process according to the contract (`org.autojs.plugin.compose.api`).

******

### Status

******

1.3.0 local development preview: requires an AutoJs6 host build that carries the new 3-Block Compose identity (host commit f12665ffde or later) with this plugin installed and enabled; the AutoJs6 editor menu entry "Open in 3-Block Compose designer" needs a build after host commit db7e6a1734. The local companion delivers UI pages, floating windows, the visual designer, five examples, the API reference, the designer guide page and TypeScript declarations. The verified compatibility and performance scope is recorded in the roadmap. Not yet listed in the official index; no official release. The icons use the maintainer's final artwork.

******

### Features

******

Capabilities of the current development preview:

- Visual designer: the script file is the project, the real renderer previews the interface tree as you edit it, the outline / component palette / property drawer cover all 55 components, with undo / redo and draft recovery; the exported script can be copied or sent straight to AutoJs6
- AutoJs6 integration: a project can be sent to run, opened in the AutoJs6 editor (two-way sync), imported into the AutoJs6 script directory, copied or exported; the designer also opens .js files straight from the AutoJs6 editor or a file manager and saves back into the same file
- Declarative UI: `compose.state` + `compose.mount(render)` re-render on state changes, while long-lived node handles (`compose.Text({...})` and friends) allow direct property and child updates
- Material 3 core: 29 node factories for layouts, text, icons, images, buttons, inputs, selection controls, lazy lists, dialogs and progress; Snackbar is a session command, not a compose.Snackbar factory
- Chained modifiers: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` keeps operation order, and scoped operations are validated on the host side
- Two hosting surfaces: `compose.mount` or callable `compose` / `$compose` for `"ui";` activity content, and `compose.floaty` for raw or resizable floating windows, including non-UI scripts
- In-process rendering: the plugin applies UI updates in the host process and queues events to the owning script thread; workers use compose.post to request updates
- One APK includes arm64-v8a / armeabi-v7a / x86_64 / x86, with no first-party native code; the bundled AndroidX graphics-path helper remains subject to Android, host and plugin compatibility requirements
- Native text editing preserves selection and IME composition, supports focus and explicit edits, and rejects delayed edits that would overwrite newer input; switches and sliders remain script-controlled
- Integration guards: availability probes return unavailable for missing or incompatible plugins, errors use `ComposeError`, and closing a session or stopping its script releases owned windows and callbacks
- Five runnable examples for a counter, form validation, a keyed 1000-item list, a non-UI floating HUD, and themes, with prerequisites and an index, synchronized to the matching host's 3-Block Compose sample category
- TSX supports `<compose.Column>`, `<compose:Text>`, references to node factories, fragments, slots and reactive callbacks; a single tree cannot mix Compose and legacy XML nodes
- XML `<compose>` containers and compose.attach embed independent Compose sessions in UI pages or legacy floating windows; compose.AndroidView embeds an existing Android View or one returned by a synchronous factory
- compose.dialog returns an updatable, closable session for dialogs and modal bottom sheets in UI or ordinary scripts
- Material 3 extended components: navigation bars and drawers, tabs, bottom sheets and menus, date and time pickers, paging and grids, chips, badges, segmented buttons, floating action buttons, search bars, tooltips and pull to refresh
- Component gallery: the launcher entry shows Material 3 previews and runnable example scripts of all 55 components, which can be copied or sent to AutoJs6; the settings page follows the AutoJs6 language, dark mode and theme color by default and offers four launcher icons

******

### Usage

******

1. Install a matching local AutoJs6 build with the compose script entry (minimum 6.8.0 / 5322)
2. Install the plugin APK; open 3-Block Compose from the launcher to reach the designer home, create a script project, or open the component gallery and settings from the top bar
3. Confirm in the AutoJs6 plugin center that 3-Block Compose is recognized and enabled
4. Use `compose` or `$compose` in scripts; mount activity content with `compose.mount`, or grant the host overlay permission and use `compose.floaty`
5. The editor menu offers run, open in AutoJs6 editor, import into AutoJs6 scripts, copy and export; choosing 3-Block Compose from the AutoJs6 editor menu (needs an AutoJs6 build with the matching update) or from a file manager edits the script file directly

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
let worker = null;
let status = compose.Text({ text: 'Preparing...', color: '#FFFFFF' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ contentColor: '#FFFFFF', onClick: () => win.close() }, 'Close'),
]), { x: 50, y: 300, raw: true });
win.on('close', () => { if (worker) worker.interrupt(); });

worker = threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => {
            if (!win.isClosed()) status.text = `Progress ${i}%`;
        });
    }
});
```

Five runnable scripts are bundled in assets/examples/index.json and the matching host's 3-Block Compose sample category. Each header states its mode and permissions. Use the companion local API reference and TypeScript/editor declarations for node, modifier, theme, session and floating-window details; the online site may not yet contain these local changes.

******

### Compatibility

******

Runtime requirements and limits of the plugin:

- Minimum AutoJs6 version: 6.8.0 (5322) or later; older hosts flag the plugin as incompatible in the plugin center
- Android version: 7.0 (API 24) or later
- Processor architecture: arm64-v8a / armeabi-v7a / x86_64 / x86 (all four built into the single APK, no per-architecture download)
- Compose version: bundled with the plugin (BOM 2026.09.00), independent of the host's Compose runtime
- Contract version: 2; host and plugin negotiate the contract version and refuse to load with a clear error when it does not match
- Packaged apps also require a separately installed compatible 3-Block Compose plugin, with enablement/authorization belonging to that app; compatibility checks the embedded AutoJs6 runtime, not the packaged app's own versionCode
- View factories run on the main thread before rendering. Invalid replacements preserve the current content; a View cannot belong to two nodes or be taken from another parent. Borrowed Views retain their listeners and caller-owned resources
- cancelable=false disables back, outside-click and swipe dismissal; explicit close and script exit still release the dialog while preserving existing pages and other sessions
- This build uses Compose UI contract V2 and requires the matching AutoJs6 6.8.0 / 5322 host; companion TSX support requires TypeScript Engine 0.6.7. New hosts still support existing components in older V1 renderers; extended components require a V2 renderer
- compose.memo is provided by AutoJs6 6.8.0 / 5323 to reuse render fragments whose dependencies are unchanged; this plugin needs no update, and companion TSX support requires TypeScript Engine 0.6.8
- Since 1.2.0 the contract classes (org.autojs.plugin.compose.api) are packaged with the plugin for its standalone process; inside AutoJs6 the host supplies the same classes and the plugin copy is shadowed
- The designer runs in the plugin's own process and does not depend on AutoJs6; running, opening in the AutoJs6 editor and importing into the AutoJs6 script directory need an installed AutoJs6, and the editor menu entry "Open in 3-Block Compose designer" needs an AutoJs6 build that contains it

******

### FAQ

******

- How do gallery examples run? "Run in AutoJs6" hands the script to the installed AutoJs6; the gallery itself only shows previews and code and never executes scripts
- Can the designer open any script? It interprets only top-level `compose.state` declarations and the single `compose.mount` / `floaty` / `dialog` call; every other statement is kept verbatim. Shapes it cannot map (a root that is not a compose factory, several mount calls, a render function without return) are refused with a line and column
- Does the designer run scripts? No. It only parses them with the Rhino parser and previews them with the plugin's own renderer; events do not fire and state shows its initial value. Running is left to AutoJs6
- Why is `compose` missing? The global object is supplied by the matching local host build; installing this plugin APK alone does not add it
- Do other UI plugins need to be uninstalled? No, 3-Block Compose does not interfere with the existing `ui` module or other plugins
- What happens when the plugin changes? Updating, uninstalling or disabling it closes active sessions and reports the corresponding error; a compatible, enabled plugin allows a new mount
- What do floating windows require? Grant overlay permission to the host. Call `window.requestFocus()` before text input; if a HyperOS window is not visible, return to the desktop. Missing permission reports PERMISSION_REQUIRED without opening a permission prompt automatically
- Can scripts use TSX or arbitrary Compose functions? TSX can use the documented Compose node factories with the matching host and TypeScript Engine. Arbitrary Kotlin Composable functions and user-defined TSX components are not supported
- Does rotation lose state? The current host handles ordinary orientation changes without replacing the script engine. Actual Activity recreation or destruction closes that engine and its sessions; business state is not automatically restored
- How do selectors find components? testTag is exposed as the raw ID without a package prefix. id/testTag and desc/contentDescription are distinct; Button text may be a child node, so follow parent() to a clickable ancestor when needed

******

### Permissions and Security

******

The plugin requests no Android runtime permissions and never touches the network, storage, or sensors.

- Component protection: the Wake Activity and the INFO service are protected by the `org.autojs.permission.PLUGIN` signature permission, so only the AutoJs6 host can reach them; the designer editor is an exported component without a permission that only accepts a script document Uri handed over by the user, and reads and writes are limited to that Uri
- Background behavior: the plugin has no resident service or scheduled task and only receives one system broadcast when it is updated, to normalize its launcher icon components; it consumes nothing while not loaded by the host and while neither the designer nor the gallery is open
- Data boundary: the renderer never reads or writes script data or user files, and interface state lives only in the host process memory; designer projects and drafts live in the plugin's private directory, are imported and exported only through the system document picker, or read and write a single script file through a Uri the user handed over; the project directory is shared only through the plugin's own content provider, per request, when handing a project to AutoJs6; an import keeps access to the picked file when its provider allows it, but the file is only read or written when the user chooses "Write back to source" or "Re-import from source"
- Backup policy: app backup and device transfer are disabled, and the plugin holds no data worth migrating

When loading the renderer the host keeps its own script permission model; the plugin does not widen the system capabilities scripts can reach.

******

### Plugin Interface

******

Identifiers exposed to the host:

```text
application id: io.github.supermonster003.autojs6.plugin.three.block.compose
plugin id: three-block-compose
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: three-block-compose
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 2)
minimum host build: 5322 (6.8.0)
```

The host discovers the plugin through `org.autojs.plugin.INFO` and reads capability data such as `requiresHostVersion`; the renderer factory class is declared by the `org.autojs.plugin.compose.RENDERER_FACTORY` meta-data, and the host creates a class loader from the plugin APK path (with the host as parent) and instantiates it inside the host process.

******

### Roadmap

******

Milestones, design decisions, and acceptance criteria are tracked in a single roadmap:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.3.0

_2026/10/13_

- `Feature` Designer projects created with "Import script" can use "Write back to source" and "Re-import from source" in the editor menu: the import keeps access to the source file when its provider allows it, either action asks first when the other side changed since the last sync, and a source that can no longer be opened can be picked again to restore the link; saving a project never writes the source by itself
- `Feature` The designer canvas previews Image pictures: '@drawable/name' comes from the installed AutoJs6, and relative paths are looked up in the folder chosen with "Image folder" in the editor menu (read-only access is kept); URLs and absolute paths stay placeholders with the reason in the issues list
- `Improvement` Long-press dragging a node on the designer canvas shows an insertion line along the target container's layout (vertical, horizontal or grid) and drops the node where the pointer is, instead of always at the end of the container
- `Improvement` AndroidView on the designer canvas now shows a tinted placeholder that keeps its size modifiers (such as h and fillMaxWidth) and displays the source of its view expression
- `Improvement` The modifier chain in the designer's property drawer can be reordered by dragging the handle on the left of each operation, with an insertion line until it is released; the move up / down buttons remain
- `Improvement` Open popup components (open is true) are now drawn close to their real look inside the designer canvas's logical screen: AlertDialog and ModalBottomSheet cover the page with a scrim, DropdownMenu and Tooltip appear below or above their anchor, and the nodes inside them can be tapped to edit; they are no longer inserted into the page as cards and no real window opens

#### v1.2.0

_2026/10/13_

- `Hint` The plugin is renamed from Compose UI to 3-Block Compose: the application ID changes from io.github.supermonster003.autojs6.plugin.compose.ui to io.github.supermonster003.autojs6.plugin.three.block.compose. Android treats it as a new app, the old package can stay installed alongside, settings and authorizations are not migrated, and the new package must be enabled and authorized again in the plugin center
- `Hint` Requires an AutoJs6 6.8.0 host build that contains the new plugin identity (host commit f12665ffde or later); earlier hosts discover the plugin by the old package name and do not recognize this version
- `Hint` The designer is documented on the "3-Block Compose Designer" page of the documentation site; the AutoJs6 editor menu entry "Open in 3-Block Compose designer" needs an AutoJs6 6.8.0 build after host commit db7e6a1734, while the other destinations only need an installed AutoJs6
- `Feature` Visual designer: the launcher opens the designer home, where script projects in private storage are created, imported or opened; the editor offers a real-renderer preview canvas, an outline tree, a component palette and a property drawer, with tap to select, long-press drag to reparent, reordering in the outline, undo / redo, automatic draft saving and recovery, host option editing, a contract-catalog validation report, and copying the script or sending it to AutoJs6; in landscape and on tablets the canvas and the panels sit side by side; the component gallery and settings now open from the designer home
- `Feature` Designer and AutoJs6 integration: the editor menu opens the current script in the AutoJs6 editor (the designer reloads after AutoJs6 saves), imports it into the AutoJs6 script directory, or exports it anywhere; the designer also opens .js files directly from the AutoJs6 editor or a file manager and saves back into the same file, falling back to a new project without write access; project files are shared through the plugin's own content provider with per-request grants and no other directory is exposed
- `Improvement` The app title, plugin ID (three-block-compose), package and component names, release artifacts, documentation and the GitHub repository (AutoJs6-Plugin-Three-Block-Compose) follow the rename; the compose script API, the contract version and the rendering behavior are unchanged
- `Improvement` The launcher, plugin center and system App info icons now use the maintainer's final artwork, with the light and dark glyphs sharing one shape
- `Improvement` The bundled examples and the host example category are renamed to 3-Block Compose
- `Improvement` The compose-ui-api contract AAR is now packaged with the plugin and kept intact (previously compile-only) for the plugin's own process; inside AutoJs6 the host's identical contract classes still take precedence, so rendering behavior and the contract version are unchanged
- `Dependency` Add the slim Rhino parser jar (org.mozilla.javascript 2.0.0-SNAPSHOT, MPL 2.0, the same build as the AutoJs6 host), used only by the visual designer to parse scripts, never to run them; release builds keep only the referenced parser classes

#### v1.1.0

_2026/10/08_

- `Hint` 1.1.0 local development preview: use a matching AutoJs6 host build and the installed, enabled plugin. UI pages, floating windows, five examples, companion API reference and TypeScript declarations are provided for this local integration. Verified compatibility and performance scope is recorded in the roadmap. The plugin is not in the official index and has no official release. Current icon artwork is temporary, awaiting the maintainer's source images
- `Hint` This build uses Compose UI contract V2 and requires the matching AutoJs6 6.8.0 / 5322 host; companion TSX support requires TypeScript Engine 0.6.7. New hosts still support existing components in older V1 renderers; extended components require a V2 renderer
- `Hint` compose.memo is provided by AutoJs6 6.8.0 / 5323 to reuse render fragments whose dependencies are unchanged; this plugin needs no update, and companion TSX support requires TypeScript Engine 0.6.8
- `Hint` The gallery and settings run in the plugin process and change neither in-host rendering nor the minimum host version; running example scripts requires AutoJs6 with this plugin installed and enabled
- `Feature` TSX supports `<compose.Column>`, `<compose:Text>`, references to node factories, fragments, slots and reactive callbacks; a single tree cannot mix Compose and legacy XML nodes
- `Feature` XML `<compose>` containers and compose.attach embed independent Compose sessions in UI pages or legacy floating windows; compose.AndroidView embeds an existing Android View or one returned by a synchronous factory
- `Feature` compose.dialog returns an updatable, closable session for dialogs and modal bottom sheets in UI or ordinary scripts
- `Feature` Material 3 extended components: navigation bars and drawers, tabs, bottom sheets and menus, date and time pickers, paging and grids, chips, badges, segmented buttons, floating action buttons, search bars, tooltips and pull to refresh
- `Feature` Component gallery: open it from the launcher to browse Material 3 previews and example scripts of all 55 components, copy them to the clipboard or send them to the installed AutoJs6
- `Feature` Settings page: language, dark mode and theme color follow AutoJs6 by default and can be set separately; the launcher icon offers adaptive (light / dark / automatic) and transparent background choices
- `Fix` Fixed build failures after adjusting launcher icon backgrounds in Icon Studio
- `Improvement` Android App info icons share Icon Studio artwork and light/dark backgrounds while preserving transparent Plugin Center artwork and existing launcher choices
- `Improvement` View factories run on the main thread before rendering. Invalid replacements preserve the current content; a View cannot belong to two nodes or be taken from another parent. Borrowed Views retain their listeners and caller-owned resources
- `Improvement` cancelable=false disables back, outside-click and swipe dismissal; explicit close and script exit still release the dialog while preserving existing pages and other sessions
- `Improvement` The Plugin Center icon and the system app info icon use the #FAFAFA light / #212121 dark backgrounds shared with the other standalone plugins
- `Dependency` Update compose-ui-api.aar with the optional AndroidView interop extension while preserving frozen V1
- `Dependency` Add the optional dialog capability to compose-ui-api.aar while preserving existing V1 and AndroidView contracts
- `Dependency` Add the compose-ui-api.aar V2 component catalog while preserving existing node models and V1 component semantics
- `Dependency` Added material-color-utilities 4.1.1 (MIT) for the theme color derivation shared with the other standalone plugins
- `Dependency` Added host-locked copies of AndroidX activity, core, lifecycle, savedstate, emoji2, window, kotlinx-coroutines and their companions for the gallery in the plugin process; inside the host the host copies still take precedence

##### For more release history, see

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

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

This project is released under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE). License information of third-party components is listed in [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: https://docs.autojs6.com
- compose module documentation: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md
