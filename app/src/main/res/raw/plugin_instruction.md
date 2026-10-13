3-Block Compose is a user interface rendering plugin for AutoJs6. Scripts declare interfaces through the host-provided `compose` / `$compose` entry, and the plugin renders them inside the host process with Jetpack Compose and Material 3. The current preview supports both `"ui";` activity content and floating windows from non-UI scripts.

1.4.0 is the first public release (GitHub Release, listed in the official plugin index). It requires an AutoJs6 6.8.0 (5322) or later build that carries the new 3-Block Compose identity (host commit f12665ffde or later); AutoJs6 6.8.0 was not yet officially released when this version was published, so an AutoJs6 build containing that commit is needed. The AutoJs6 editor menu entry "Open in 3-Block Compose designer" needs a build after host commit db7e6a1734, and TSX projects in the designer need TypeScript Engine 0.7.0. The plugin delivers UI pages, floating windows, the visual designer, five examples, the API reference, the designer guide page and TypeScript declarations. The verified compatibility and performance scope is recorded in the roadmap. The icons use the maintainer's final artwork.

### Usage

1. Install a matching local AutoJs6 build with the compose script entry (minimum 6.8.0 / 5322)
2. Install the plugin APK; open 3-Block Compose from the launcher to reach the designer home, create a script project, or open the component gallery and settings from the top bar
3. Confirm in the AutoJs6 plugin center that 3-Block Compose is recognized and enabled
4. Use `compose` or `$compose` in scripts; mount activity content with `compose.mount`, or grant the host overlay permission and use `compose.floaty`
5. The editor menu offers run, open in AutoJs6 editor, import into AutoJs6 scripts, copy and export; choosing 3-Block Compose from the AutoJs6 editor menu (needs an AutoJs6 build with the matching update) or from a file manager edits the script file directly

### Compatibility

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

### FAQ

- How do gallery examples run? "Run in AutoJs6" hands the script to the installed AutoJs6; the gallery itself only shows previews and code and never executes scripts
- Can the designer open any script? It interprets only top-level `compose.state` declarations and the single `compose.mount` / `floaty` / `dialog` call; every other statement is kept verbatim. Shapes it cannot map (a root that is not a compose factory, several mount calls, a render function without return) are refused with a line and column. In TSX projects, `compose.X` / `compose:X` tags are editable nodes, other tags and `{}` expressions are kept as written, and XML tags and spread attributes are refused
- Does the designer run scripts? No. It only parses them (JavaScript with the Rhino parser, TSX with the TypeScript Engine parsing service) and previews them with the plugin's own renderer; event handlers do not run and state shows its initial value or a preview value set in the State panel. In the interactive preview, controls bound to a state (switches, sliders, text fields and so on) change its preview value directly, but handlers and render functions still do not run. Running is left to AutoJs6
- Why is `compose` missing? The global object is supplied by the matching local host build; installing this plugin APK alone does not add it
- Do other UI plugins need to be uninstalled? No, 3-Block Compose does not interfere with the existing `ui` module or other plugins
- What happens when the plugin changes? Updating, uninstalling or disabling it closes active sessions and reports the corresponding error; a compatible, enabled plugin allows a new mount
- What do floating windows require? Grant overlay permission to the host. Call `window.requestFocus()` before text input; if a HyperOS window is not visible, return to the desktop. Missing permission reports PERMISSION_REQUIRED without opening a permission prompt automatically
- Can scripts use TSX or arbitrary Compose functions? TSX can use the documented Compose node factories with the matching host and TypeScript Engine. Arbitrary Kotlin Composable functions and user-defined TSX components are not supported
- Does rotation lose state? The current host handles ordinary orientation changes without replacing the script engine. Actual Activity recreation or destruction closes that engine and its sessions; business state is not automatically restored
- How do selectors find components? testTag is exposed as the raw ID without a package prefix. id/testTag and desc/contentDescription are distinct; Button text may be a child node, so follow parent() to a clickable ancestor when needed

### Permissions and Security

- Component protection: the Wake Activity and the INFO service are protected by the `org.autojs.permission.PLUGIN` signature permission, so only the AutoJs6 host can reach them; the designer editor is an exported component without a permission that only accepts a script document Uri handed over by the user, and reads and writes are limited to that Uri
- Background behavior: the plugin has no resident service or scheduled task and only receives one system broadcast when it is updated, to normalize its launcher icon components; it consumes nothing while not loaded by the host and while neither the designer nor the gallery is open
- Data boundary: the renderer never reads or writes script data or user files, and interface state lives only in the host process memory; designer projects and drafts live in the plugin's private directory, are imported and exported only through the system document picker, or read and write a single script file through a Uri the user handed over; the project directory is shared only through the plugin's own content provider, per request, when handing a project to AutoJs6; an import keeps access to the picked file when its provider allows it, but the file is only read or written when the user chooses "Write back to source" or "Re-import from source"
- Backup policy: app backup and device transfer are disabled, and the plugin holds no data worth migrating

More information (quick start, build notes, roadmap) is available on the project page: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose
