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

1.8.0 is the current public release (GitHub Release, listed in the official plugin index). It requires an AutoJs6 6.8.0 (5322) or later build that carries the new 3-Block Compose identity (host commit f12665ffde or later); AutoJs6 6.8.0 was not yet officially released when this version was published, so an AutoJs6 build containing that commit is needed. The AutoJs6 editor menu entry "Open in 3-Block Compose designer" needs a build after host commit db7e6a1734, and TSX projects in the designer need TypeScript Engine 0.7.0, and the layout preview of UI (XML) projects needs a build that contains the preview service (host commit 306a8f7834 or later). The plugin delivers UI pages, floating windows, the visual designer (Compose and UI projects), five examples, the API reference, the designer guide page and TypeScript declarations. The verified compatibility and performance scope is recorded in the roadmap. The icons use the maintainer's final artwork. From 1.7.0 the interactive preview of UI (XML) projects has AutoJs6 run the whole script for real; it needs an AutoJs6 build with preview contract v2 (host commit 4c332c2053 or later) and Android 13 or later. From 1.8.0 UI projects can keep layouts in XML files and become folder projects: previews that use the project's files need preview contract v3 (host commit 9df0a154b or later), and running or importing folder projects in AutoJs6 needs host commit faf412ef7 or later.

******

### Features

******

Capabilities of the current development preview:

- Visual designer: the script file is the project, the real renderer previews the interface tree as you edit it, the outline / component palette / property drawer cover all 55 components, with undo / redo and draft recovery; the exported script can be copied or sent straight to AutoJs6; canvas drops land where the pointer is, open popups are drawn inside the canvas, Image shows @drawable pictures and pictures from a project image folder, the State panel changes state preview values for a while, and the interactive preview runs a JavaScript project's script in a restricted sandbox (Android 8.0 or later), so taps and typing update the canvas the way the script decides, with a console for its output and errors; projects are JavaScript or TSX, and TSX projects are parsed by TypeScript Engine 0.7.0 or later and written back as canonical TSX
- AutoJs6 integration: a project can be sent to run, opened in the AutoJs6 editor (two-way sync), imported into the AutoJs6 script directory, copied or exported; the designer also opens .js / .tsx files straight from the AutoJs6 editor or a file manager and saves back into the same file; an imported project can be written back to its source or re-imported from it on request in the editor menu
- UI (XML) projects: the designer also opens classic "ui" mode scripts (E4X XML layouts) and edits them in place, keeping every untouched byte (whitespace, comments, attribute order, quotes, {expressions}); the canvas is drawn by the AutoJs6 preview service with the ui mode layout inflater (embedded real views from Android 11, a bitmap snapshot below), the property drawer follows the documentation site's tag and attribute catalog, the palette lists 48 tags, an element can get a `ui.<id>.on` handler stub, and the bindings panel overrides `{{ }}` values for a while
- Interactive preview of UI (XML) projects (1.7.0): with the "Interact" switch above the canvas on, AutoJs6 runs the whole script as a normal ui mode run, the canvas shows the script's ui.layout and takes taps and touches directly, event handlers, `{{ }}` bindings and the other modules run with the script's real logic, and the console tab shows the output, errors and the exit reason; the script runs again about 0.4 s after edits, stops when the switch goes off or the designer is left, and the document and the file stay unchanged
- XML layout files and folder projects (1.8.0): UI projects can keep layouts in XML files (ui.layoutFile, ui.inflate(files.read(...)), AutoJs6 XML or Android XML); a project with layout files is a folder with main.js and a project.json, exported and imported as a zip or a folder and handed to AutoJs6 to run or import as a project; Android XML is edited against the attributes AutoJs6 actually sets
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
- The layout preview of UI (XML) projects needs an AutoJs6 6.8.0 build that contains the preview service (host commit 306a8f7834 or later; the plugin checks for the service, not a version number); from Android 11 (API 30) the canvas embeds the real views AutoJs6 draws, older versions show a bitmap snapshot; without the service a UI project can still be edited and the canvas says why
- The interactive preview of UI (XML) projects needs an AutoJs6 6.8.0 build with preview contract v2 (host commit 4c332c2053 or later) and Android 13 (API 33) or later; on Android 13 - 14 input fields in the canvas take no keyboard input, from Android 15 they do; where the conditions are not met the switch is hidden, the row above the canvas says why and "Run in AutoJs6" remains; an AutoJs6 build with contract v1 only still provides the layout preview
- UI projects with layout files (1.8.0): previews that use the project's files (res/ resources, layout files) need an AutoJs6 6.8.0 build with preview contract v3 (host commit 9df0a154b or later), and running or importing folder projects in AutoJs6 needs host commit faf412ef7 or later; older builds still preview the XML text, and the two destinations give way to a note

******

### FAQ

******

- How do gallery examples run? "Run in AutoJs6" hands the script to the installed AutoJs6; the gallery itself only shows previews and code and never executes scripts
- Can the designer open any script? It interprets only top-level `compose.state` declarations and the single `compose.mount` / `floaty` / `dialog` call; every other statement is kept verbatim. Shapes it cannot map (a root that is not a compose factory, several mount calls, a render function without return) are refused with a line and column. In TSX projects, `compose.X` / `compose:X` tags are editable nodes, other tags and `{}` expressions are kept as written, and XML tags and spread attributes are refused
- Does the designer run scripts? Only in the interactive preview. While editing it only parses them (JavaScript with the Rhino parser, TSX with the TypeScript Engine parsing service) and previews them with the plugin's own renderer; event handlers do not run and state shows its initial value or a preview value set in the State panel. With Interact on, a JavaScript project's script runs inside the plugin in a restricted sandbox (Android 8.0 or later): it has compose, console, toast and timers only, cannot reach Java / Android classes, files, the network or other AutoJs6 modules, is stopped when one run takes longer than about 3 seconds, and changes only the canvas, never the project. A TSX project (or any project below Android 8.0) does not run its script; controls bound to a state change only its preview value. Really running a script is left to AutoJs6. The canvas of a UI (XML) project is laid out by the AutoJs6 preview service only (it creates and draws the views) without running the project's script; `{{ }}` values come from the script's top-level literal declarations (evaluated in the sandbox) or from overrides in the Bindings panel. From 1.7.0 the interactive preview of a UI (XML) project is different: with the switch on, AutoJs6 runs the whole script for real, exactly like "Run in AutoJs6" (no sandbox limits, side effects really happen, each project asks once before its first run), and only its ui.layout content shows on the canvas; the result still never goes into the project
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
- Layout preview: the canvas of a UI (XML) project is drawn by the installed AutoJs6: the plugin hands only the XML fragment and the binding values to the AutoJs6 preview service (protected by the PLUGIN signature permission), AutoJs6 creates the views with its ui mode layout inflater and draws them back, runs no project script and cannot reach the plugin's project files; that preview runtime appears in the AutoJs6 running list and can be stopped at any time; from 1.8.0, with an AutoJs6 that takes them (preview contract v3), the plugin also hands over a read-only zip copy of the project's files (a folder project, or the res/ folder beside a script file) so the layout and the run resolve its resources
- Interactive preview (UI projects): with the switch on, the plugin hands the script text to the AutoJs6 preview service and AutoJs6 runs it as a normal ui mode script (the same engine and script permissions as "Run in AutoJs6", side effects really happen), shows only its ui.layout content on the canvas and sends the console output back; each project asks once before its first run, the run appears in the AutoJs6 running list, and it ends when the switch goes off, the designer is left or it is stopped from AutoJs6; the script does not run in the plugin's process and the plugin does not widen its permissions
- Folder access and project zips (1.8.0): a script opened through a content Uri reads and writes layout files only in the folder the user picks in the system folder picker, after checking the script is in it (read and write access is kept for that folder only); folder projects reach AutoJs6 as a zip through the plugin's own content provider with a per-request read grant, and AutoJs6 unpacks it only after checking every entry stays inside the project

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

#### v1.8.0

_2026/10/16_

- `Hint` Several 1.8.0 features need a newer AutoJs6 6.8.0 build: previews that use the project's files (res/ resources, layout files) need host commit 9df0a154b or later (preview contract v3), and running or importing folder projects in AutoJs6 needs host commit faf412ef7 or later; with an older AutoJs6 the designer says so and keeps the earlier behavior
- `Feature` UI (XML) projects can keep their layouts in separate XML files: ui.layoutFile("...") and ui.inflate(files.read("...")) with a literal path, in AutoJs6 XML or Android XML (ui.useAndroidLayout); the designer reads and edits these files and leaves what is not edited untouched, and the fragment switcher lists them by file name
- `Feature` "Save as XML file" and "New XML file" in the menu beside the fragment switcher: the first moves a layout written directly in ui.layout(...) or ui.inflate(...) into a file and rewrites the call, the second adds a file with an empty root container (AutoJs6 XML or Android XML) and a ui.inflate line at the end of the script; both can be undone and write files only when saved
- `Feature` Folder projects: a private project saved with layout files becomes a folder with main.js, its layout files and a project.json AutoJs6 accepts, so AutoJs6 can run and package it as a project; a new UI project can also start as such a folder (AutoJs6 XML or Android XML template); the home list shows folder projects as "name/"
- `Feature` Folder projects export as one zip, and the home menu imports zips and folders ("Import script or zip", "Import folder")
- `Feature` "Run in AutoJs6" and "Import into AutoJs6 scripts" hand a project with layout files to AutoJs6 as a zip: AutoJs6 runs it as a project from a cache folder that is removed afterwards, or imports it into a folder of the same name and asks whether to overwrite it or save as a new folder when that exists
- `Feature` Scripts opened from the AutoJs6 editor can read and write layout files in their own folder: the first time, choose the script's folder in the system folder picker; the access is kept
- `Feature` Android XML layouts are edited against what AutoJs6 actually supports: the property drawer lists each view class's settable android: / app: attributes and the parent layout's parameters, ids are written as @+id/..., the palette inserts Android view classes, and the problems tab flags attributes AutoJs6 ignores or cannot set
- `Feature` The problems tab of UI projects lists "preview differences": lines of the script that behave differently in the preview than in AutoJs6 (activity, ui.emitter, window functions, dialogs, endless loops on the main thread and more), each with its line and an explanation; a tap opens the documentation; they do not count as errors
- `Improvement` The layout preview and the interactive preview give AutoJs6 the project's files, so layouts can use strings, colors, dimensions, styles, images and `<include>` layouts from res/, and ui.layoutFile works in the interactive preview (needs preview contract v3); with an older AutoJs6 the problems tab notes that the preview may differ from a real run
- `Improvement` Inserting an element into a container that holds only whitespace no longer leaves a blank line
- `Dependency` Upgrade the ui-preview-api.aar contract artifact to ui preview contract v3 (adds request keys for a project's resources with a layout and for a project zip with a run; AutoJs6 builds with v2 or v1 only keep the earlier previews)

#### v1.7.0

_2026/10/15_

- `Hint` The interactive preview of UI (XML) projects now has AutoJs6 run the whole script for real (needs an AutoJs6 6.8.0 build with preview contract v2, host commit 4c332c2053 or later, and Android 13 or later): the script's side effects are the same as when run in AutoJs6, and each project asks once before the switch goes on; where the conditions are not met the row above the canvas says why and "Run in AutoJs6" remains
- `Feature` Interactive preview of UI (XML) projects: with the "Interact" switch above the canvas on, AutoJs6 runs the whole script as a ui mode run, the canvas shows the script's ui.layout and takes taps and touches directly, and event handlers, {{ }} bindings, timers and the other modules run with the script's real logic; the script runs again about 0.4 s after edits, stops when the switch goes off, the designer goes to the background or the editor closes, and the canvas returns to the layout preview; the document, the project file and the draft are never changed. On Android 13 - 14 input fields in the canvas take no keyboard input, from Android 15 they do
- `Feature` During the interactive preview the console tab of a UI project shows the output of the script AutoJs6 runs (colored by level), the run's state and its exit reason; when the script fails, the error (with its line) shows above the canvas and a tap opens the console; the run appears under the project's file name in the AutoJs6 running list and can be stopped from there
- `Fix` The designer sometimes showed "The AutoJs6 preview service is not available right now (at most 2 sessions per client)" when opening a UI project: while the canvas size changed twice before the layout settled, the plugin created the preview session in AutoJs6 twice and hit the per-client session limit; now one session is created at a time and the size or appearance that changed meanwhile is applied afterwards
- `Dependency` Upgrade the ui-preview-api.aar contract artifact to ui preview contract v2 (appends, after v1, the interfaces to run and stop a script and the console / error / exit callbacks; AutoJs6 builds with v1 only still provide the layout preview)

#### v1.6.0

_2026/10/15_

- `Hint` The designer gains UI (XML) projects: classic "ui" mode scripts (E4X XML layouts) can be opened and edited, with the canvas drawn by AutoJs6. The layout preview needs an AutoJs6 6.8.0 build that contains the preview service (host commit 306a8f7834 or later); from Android 11 (API 30) the canvas embeds the real views AutoJs6 draws, older versions show a bitmap snapshot; without the service the project can still be edited and the canvas says why
- `Feature` The designer supports UI (XML) projects: pick "UI (XML)" when creating a project, or import a "ui" mode script or open one from the AutoJs6 editor / a file manager and it is read as a UI project; the home list marks each project JS / TSX / UI. The XML layouts of the script are kept as a syntax tree with positions: untouched parts (whitespace, comments, attribute order, quotes, {expressions}) are preserved byte for byte, and only edited start tags, text and newly inserted elements are rewritten; the script's other XML literals can be switched onto the canvas as well
- `Feature` The UI project editor: outline (collapse, move up / down, delete, long-press drag, a switcher when there are several layouts), property drawer (tag, id, text content, the attributes already set and the catalog attributes grouped by View class, text / `{{ }}` binding / {expression} states, bare attributes, any attribute name), component palette (the documentation's 48 tags by category with default attributes), bindings panel (the evaluated values of `{{ }}` names and overrides that touch only the canvas), problems list (checked against the documentation site's tag and attribute catalog) and console (what AutoJs6 reported while drawing the layout); save, draft recovery, source sync, run, open in the AutoJs6 editor, import into the AutoJs6 script directory, copy and export work for UI projects too
- `Feature` The canvas of a UI project is drawn by AutoJs6 (layout preview): the plugin hands the XML fragment and the binding values to the AutoJs6 preview service, AutoJs6 creates real views with its ui mode layout inflater and draws them back onto the canvas without running the project's script; selection and drag work on the canvas as usual, and a failed frame points at the element at fault; the preview runtime shows up as "3-Block Compose preview" in the AutoJs6 running list and can be stopped there
- `Feature` The property drawer of a UI project adds events to the selected element: a `ui.<id>.on('event', ...)` handler stub is appended after the ui.layout call (an element without an id gets one), and the body is written in the script editor; events the script already registers show as "Handled in the script"
- `Dependency` Add the ui-preview-api.aar contract artifact (AutoJs6 ui layout preview contract v1), through which the designer asks AutoJs6 to draw the canvas of UI projects

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
