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

1.4.0 is the first public release (GitHub Release, listed in the official plugin index). It requires an AutoJs6 6.8.0 (5322) or later build that carries the new 3-Block Compose identity (host commit f12665ffde or later); AutoJs6 6.8.0 was not yet officially released when this version was published, so an AutoJs6 build containing that commit is needed. The AutoJs6 editor menu entry "Open in 3-Block Compose designer" needs a build after host commit db7e6a1734, and TSX projects in the designer need TypeScript Engine 0.7.0. The plugin delivers UI pages, floating windows, the visual designer, five examples, the API reference, the designer guide page and TypeScript declarations. The verified compatibility and performance scope is recorded in the roadmap. The icons use the maintainer's final artwork.

******

### Features

******

Capabilities of the current development preview:

- Visual designer: the script file is the project, the real renderer previews the interface tree as you edit it, the outline / component palette / property drawer cover all 55 components, with undo / redo and draft recovery; the exported script can be copied or sent straight to AutoJs6; canvas drops land where the pointer is, open popups are drawn inside the canvas, Image shows @drawable pictures and pictures from a project image folder, the State panel changes state preview values for a while, and the interactive preview runs a JavaScript project's script in a restricted sandbox (Android 8.0 or later), so taps and typing update the canvas the way the script decides, with a console for its output and errors; projects are JavaScript or TSX, and TSX projects are parsed by TypeScript Engine 0.7.0 or later and written back as canonical TSX
- AutoJs6 integration: a project can be sent to run, opened in the AutoJs6 editor (two-way sync), imported into the AutoJs6 script directory, copied or exported; the designer also opens .js / .tsx files straight from the AutoJs6 editor or a file manager and saves back into the same file; an imported project can be written back to its source or re-imported from it on request in the editor menu
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
- TSX projects in the designer need TypeScript Engine 0.7.0 or later (it provides the syntax tree parsing service), and running one lets AutoJs6 compile it through TypeScript Engine; without it, or with an older version, a TSX project opens as read-only source that can be viewed and copied
- Running the script in the designer's interactive preview needs Android 8.0 (API 26) or later, because the sandbox's JavaScript Date is built on java.time; on older Android a JavaScript project is previewed like a TSX project

******

### FAQ

******

- How do gallery examples run? "Run in AutoJs6" hands the script to the installed AutoJs6; the gallery itself only shows previews and code and never executes scripts
- Can the designer open any script? It interprets only top-level `compose.state` declarations and the single `compose.mount` / `floaty` / `dialog` call; every other statement is kept verbatim. Shapes it cannot map (a root that is not a compose factory, several mount calls, a render function without return) are refused with a line and column. In TSX projects, `compose.X` / `compose:X` tags are editable nodes, other tags and `{}` expressions are kept as written, and XML tags and spread attributes are refused
- Does the designer run scripts? Only in the interactive preview. While editing it only parses them (JavaScript with the Rhino parser, TSX with the TypeScript Engine parsing service) and previews them with the plugin's own renderer; event handlers do not run and state shows its initial value or a preview value set in the State panel. With Interact on, a JavaScript project's script runs inside the plugin in a restricted sandbox (Android 8.0 or later): it has compose, console, toast and timers only, cannot reach Java / Android classes, files, the network or other AutoJs6 modules, is stopped when one run takes longer than about 3 seconds, and changes only the canvas, never the project. A TSX project (or any project below Android 8.0) does not run its script; controls bound to a state change only its preview value. Really running a script is left to AutoJs6
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
- Script sandbox: the designer's interactive preview runs a JavaScript project's script inside the plugin with Rhino's interpreter. It offers compose, console, toast and timers only; Java / Android classes, files, the network and other AutoJs6 modules are out of reach, every run has instruction and time limits, and what it does stays on the canvas: the project is never written
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

#### v1.5.0

_2026/10/14_

- `Hint` The designer's interactive preview now runs a JavaScript project's script (Android 8.0 or later). The script runs inside the plugin in a restricted sandbox: it can use compose, console, toast and timers only, cannot reach Java / Android or other AutoJs6 modules, and changes only the canvas, never the project. TSX projects and devices below Android 8.0 keep the previous interactive preview
- `Feature` The interactive preview runs the script: with Interact on, a JavaScript project's render function and event handlers really run, so tapping buttons, typing and flipping switches update the canvas the way the script decides; the State panel's preview values become the states' initial values, and the script starts again after the project or a preview value changes
- `Feature` New Console tab: shows what the script writes with console / log, its toasts and its errors in the preview; a script error appears at the bottom of the canvas and opens the console when tapped; a run longer than about 3 seconds (an endless loop, for example) stops the script with the reason, and the designer itself stays responsive
- `Feature` The Interact switch shows a running icon while the script runs
- `Fix` In edit mode a text field on the canvas could take focus by itself and raise the keyboard over the panels (mostly on Android 7.x); components on the canvas no longer take focus in edit mode, and leaving the interactive preview hides the keyboard

#### v1.4.0

_2026/10/13_

- `Hint` TSX projects in the designer need TypeScript Engine 0.7.0 or later; without it, or with an older version, a TSX project can only be viewed and copied as read-only source
- `Feature` The designer supports TSX projects: a new project can be JavaScript or TSX, .tsx files can be imported or opened straight from the AutoJs6 editor and file managers; the TSX source is parsed by TypeScript Engine and edited on the canvas, and saved as TSX in one consistent format (4-space indentation, single-quoted attributes, one child element per line); code the designer cannot edit (such as alias tags, or children made by conditions or loops) is kept as written, and code it cannot represent (such as XML tags and spread attributes) is refused with a line and column
- `Feature` "Run" on a TSX project in the designer hands the project file to AutoJs6, which compiles it with TypeScript Engine and runs it; unsaved changes are saved first
- `Feature` When TypeScript Engine is not installed, too old, or its parsing service is temporarily unavailable, the designer shows the TSX project's source read-only with the reason, and the source can be copied or the parse retried; returning to the designer after installing or updating TypeScript Engine retries automatically
- `Fix` Saving an external script that cannot be written back made the designer crash instead of saving it as a project when the file name contained spaces, parentheses or other characters a project name cannot hold; such characters are now replaced with "-", and imported scripts are named the same way
- `Improvement` Event handlers edited in a TSX project are syntax-checked by TypeScript Engine, so TypeScript syntax such as type annotations can be used
- `Dependency` Add the typescript-parser-api.aar contract artifact (TypeScript syntax tree parsing contract v1), used by the designer to read TSX projects

#### v1.3.0

_2026/10/13_

- `Feature` Designer projects created with "Import script" can use "Write back to source" and "Re-import from source" in the editor menu: the import keeps access to the source file when its provider allows it, either action asks first when the other side changed since the last sync, and a source that can no longer be opened can be picked again to restore the link; saving a project never writes the source by itself
- `Feature` The designer canvas previews Image pictures: '@drawable/name' comes from the installed AutoJs6, and relative paths are looked up in the folder chosen with "Image folder" in the editor menu (read-only access is kept); URLs and absolute paths stay placeholders with the reason in the issues list
- `Feature` The designer editor has a new State panel: it lists the script's top-level compose.state declarations with their initial values and lets you set preview values that change only the canvas, never the script, and add no undo step. With Interact on (top right of the canvas) the components on the canvas can be operated, and switches, sliders, text fields, selections, pagers and other controls bound to a state change its preview value (the script's event handlers do not run); turning it off brings back selecting and dragging
- `Fix` When a child near the start of a container was inserted, removed or moved elsewhere, the children after it were rebuilt from scratch and lost their own state (such as the scroll position of a LazyColumn); they now keep their state and are no longer rebuilt
- `Fix` After changing the text of a text field (TextField / OutlinedTextField) in the designer's property drawer, the field on the canvas kept showing the old text
- `Improvement` Long-press dragging a node on the designer canvas shows an insertion line along the target container's layout (vertical, horizontal or grid) and drops the node where the pointer is, instead of always at the end of the container
- `Improvement` AndroidView on the designer canvas now shows a tinted placeholder that keeps its size modifiers (such as h and fillMaxWidth) and displays the source of its view expression
- `Improvement` The modifier chain in the designer's property drawer can be reordered by dragging the handle on the left of each operation, with an insertion line until it is released; the move up / down buttons remain
- `Improvement` Open popup components (open is true) are now drawn close to their real look inside the designer canvas's logical screen: AlertDialog and ModalBottomSheet cover the page with a scrim, DropdownMenu and Tooltip appear below or above their anchor, and the nodes inside them can be tapped to edit; they are no longer inserted into the page as cards and no real window opens
- `Improvement` After each edit the designer canvas recomposes only the nodes that changed and records node positions more cheaply, so a larger document (about 250 nodes) redraws noticeably faster after undo / redo

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
