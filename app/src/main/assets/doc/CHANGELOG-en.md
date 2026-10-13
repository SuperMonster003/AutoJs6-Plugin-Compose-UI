******

### Release History

******

# v1.5.0

###### 2026/10/14

* `Hint` The designer's interactive preview now runs a JavaScript project's script (Android 8.0 or later). The script runs inside the plugin in a restricted sandbox: it can use compose, console, toast and timers only, cannot reach Java / Android or other AutoJs6 modules, and changes only the canvas, never the project. TSX projects and devices below Android 8.0 keep the previous interactive preview
* `Feature` The interactive preview runs the script: with Interact on, a JavaScript project's render function and event handlers really run, so tapping buttons, typing and flipping switches update the canvas the way the script decides; the State panel's preview values become the states' initial values, and the script starts again after the project or a preview value changes
* `Feature` New Console tab: shows what the script writes with console / log, its toasts and its errors in the preview; a script error appears at the bottom of the canvas and opens the console when tapped; a run longer than about 3 seconds (an endless loop, for example) stops the script with the reason, and the designer itself stays responsive
* `Feature` The Interact switch shows a running icon while the script runs
* `Fix` In edit mode a text field on the canvas could take focus by itself and raise the keyboard over the panels (mostly on Android 7.x); components on the canvas no longer take focus in edit mode, and leaving the interactive preview hides the keyboard

# v1.4.0

###### 2026/10/13

* `Hint` TSX projects in the designer need TypeScript Engine 0.7.0 or later; without it, or with an older version, a TSX project can only be viewed and copied as read-only source
* `Feature` The designer supports TSX projects: a new project can be JavaScript or TSX, .tsx files can be imported or opened straight from the AutoJs6 editor and file managers; the TSX source is parsed by TypeScript Engine and edited on the canvas, and saved as TSX in one consistent format (4-space indentation, single-quoted attributes, one child element per line); code the designer cannot edit (such as alias tags, or children made by conditions or loops) is kept as written, and code it cannot represent (such as XML tags and spread attributes) is refused with a line and column
* `Feature` "Run" on a TSX project in the designer hands the project file to AutoJs6, which compiles it with TypeScript Engine and runs it; unsaved changes are saved first
* `Feature` When TypeScript Engine is not installed, too old, or its parsing service is temporarily unavailable, the designer shows the TSX project's source read-only with the reason, and the source can be copied or the parse retried; returning to the designer after installing or updating TypeScript Engine retries automatically
* `Fix` Saving an external script that cannot be written back made the designer crash instead of saving it as a project when the file name contained spaces, parentheses or other characters a project name cannot hold; such characters are now replaced with "-", and imported scripts are named the same way
* `Improvement` Event handlers edited in a TSX project are syntax-checked by TypeScript Engine, so TypeScript syntax such as type annotations can be used
* `Dependency` Add the typescript-parser-api.aar contract artifact (TypeScript syntax tree parsing contract v1), used by the designer to read TSX projects

# v1.3.0

###### 2026/10/13

* `Feature` Designer projects created with "Import script" can use "Write back to source" and "Re-import from source" in the editor menu: the import keeps access to the source file when its provider allows it, either action asks first when the other side changed since the last sync, and a source that can no longer be opened can be picked again to restore the link; saving a project never writes the source by itself
* `Feature` The designer canvas previews Image pictures: '@drawable/name' comes from the installed AutoJs6, and relative paths are looked up in the folder chosen with "Image folder" in the editor menu (read-only access is kept); URLs and absolute paths stay placeholders with the reason in the issues list
* `Feature` The designer editor has a new State panel: it lists the script's top-level compose.state declarations with their initial values and lets you set preview values that change only the canvas, never the script, and add no undo step. With Interact on (top right of the canvas) the components on the canvas can be operated, and switches, sliders, text fields, selections, pagers and other controls bound to a state change its preview value (the script's event handlers do not run); turning it off brings back selecting and dragging
* `Fix` When a child near the start of a container was inserted, removed or moved elsewhere, the children after it were rebuilt from scratch and lost their own state (such as the scroll position of a LazyColumn); they now keep their state and are no longer rebuilt
* `Fix` After changing the text of a text field (TextField / OutlinedTextField) in the designer's property drawer, the field on the canvas kept showing the old text
* `Improvement` Long-press dragging a node on the designer canvas shows an insertion line along the target container's layout (vertical, horizontal or grid) and drops the node where the pointer is, instead of always at the end of the container
* `Improvement` AndroidView on the designer canvas now shows a tinted placeholder that keeps its size modifiers (such as h and fillMaxWidth) and displays the source of its view expression
* `Improvement` The modifier chain in the designer's property drawer can be reordered by dragging the handle on the left of each operation, with an insertion line until it is released; the move up / down buttons remain
* `Improvement` Open popup components (open is true) are now drawn close to their real look inside the designer canvas's logical screen: AlertDialog and ModalBottomSheet cover the page with a scrim, DropdownMenu and Tooltip appear below or above their anchor, and the nodes inside them can be tapped to edit; they are no longer inserted into the page as cards and no real window opens
* `Improvement` After each edit the designer canvas recomposes only the nodes that changed and records node positions more cheaply, so a larger document (about 250 nodes) redraws noticeably faster after undo / redo

# v1.2.0

###### 2026/10/13

* `Hint` The plugin is renamed from Compose UI to 3-Block Compose: the application ID changes from io.github.supermonster003.autojs6.plugin.compose.ui to io.github.supermonster003.autojs6.plugin.three.block.compose. Android treats it as a new app, the old package can stay installed alongside, settings and authorizations are not migrated, and the new package must be enabled and authorized again in the plugin center
* `Hint` Requires an AutoJs6 6.8.0 host build that contains the new plugin identity (host commit f12665ffde or later); earlier hosts discover the plugin by the old package name and do not recognize this version
* `Hint` The designer is documented on the "3-Block Compose Designer" page of the documentation site; the AutoJs6 editor menu entry "Open in 3-Block Compose designer" needs an AutoJs6 6.8.0 build after host commit db7e6a1734, while the other destinations only need an installed AutoJs6
* `Feature` Visual designer: the launcher opens the designer home, where script projects in private storage are created, imported or opened; the editor offers a real-renderer preview canvas, an outline tree, a component palette and a property drawer, with tap to select, long-press drag to reparent, reordering in the outline, undo / redo, automatic draft saving and recovery, host option editing, a contract-catalog validation report, and copying the script or sending it to AutoJs6; in landscape and on tablets the canvas and the panels sit side by side; the component gallery and settings now open from the designer home
* `Feature` Designer and AutoJs6 integration: the editor menu opens the current script in the AutoJs6 editor (the designer reloads after AutoJs6 saves), imports it into the AutoJs6 script directory, or exports it anywhere; the designer also opens .js files directly from the AutoJs6 editor or a file manager and saves back into the same file, falling back to a new project without write access; project files are shared through the plugin's own content provider with per-request grants and no other directory is exposed
* `Improvement` The app title, plugin ID (three-block-compose), package and component names, release artifacts, documentation and the GitHub repository (AutoJs6-Plugin-Three-Block-Compose) follow the rename; the compose script API, the contract version and the rendering behavior are unchanged
* `Improvement` The launcher, plugin center and system App info icons now use the maintainer's final artwork, with the light and dark glyphs sharing one shape
* `Improvement` The bundled examples and the host example category are renamed to 3-Block Compose
* `Improvement` The compose-ui-api contract AAR is now packaged with the plugin and kept intact (previously compile-only) for the plugin's own process; inside AutoJs6 the host's identical contract classes still take precedence, so rendering behavior and the contract version are unchanged
* `Dependency` Add the slim Rhino parser jar (org.mozilla.javascript 2.0.0-SNAPSHOT, MPL 2.0, the same build as the AutoJs6 host), used only by the visual designer to parse scripts, never to run them; release builds keep only the referenced parser classes

# v1.1.0

###### 2026/10/08

* `Hint` 1.1.0 local development preview: use a matching AutoJs6 host build and the installed, enabled plugin. UI pages, floating windows, five examples, companion API reference and TypeScript declarations are provided for this local integration. Verified compatibility and performance scope is recorded in the roadmap. The plugin is not in the official index and has no official release. Current icon artwork is temporary, awaiting the maintainer's source images
* `Hint` This build uses Compose UI contract V2 and requires the matching AutoJs6 6.8.0 / 5322 host; companion TSX support requires TypeScript Engine 0.6.7. New hosts still support existing components in older V1 renderers; extended components require a V2 renderer
* `Hint` compose.memo is provided by AutoJs6 6.8.0 / 5323 to reuse render fragments whose dependencies are unchanged; this plugin needs no update, and companion TSX support requires TypeScript Engine 0.6.8
* `Hint` The gallery and settings run in the plugin process and change neither in-host rendering nor the minimum host version; running example scripts requires AutoJs6 with this plugin installed and enabled
* `Feature` TSX supports `<compose.Column>`, `<compose:Text>`, references to node factories, fragments, slots and reactive callbacks; a single tree cannot mix Compose and legacy XML nodes
* `Feature` XML `<compose>` containers and compose.attach embed independent Compose sessions in UI pages or legacy floating windows; compose.AndroidView embeds an existing Android View or one returned by a synchronous factory
* `Feature` compose.dialog returns an updatable, closable session for dialogs and modal bottom sheets in UI or ordinary scripts
* `Feature` Material 3 extended components: navigation bars and drawers, tabs, bottom sheets and menus, date and time pickers, paging and grids, chips, badges, segmented buttons, floating action buttons, search bars, tooltips and pull to refresh
* `Feature` Component gallery: open it from the launcher to browse Material 3 previews and example scripts of all 55 components, copy them to the clipboard or send them to the installed AutoJs6
* `Feature` Settings page: language, dark mode and theme color follow AutoJs6 by default and can be set separately; the launcher icon offers adaptive (light / dark / automatic) and transparent background choices
* `Fix` Fixed build failures after adjusting launcher icon backgrounds in Icon Studio
* `Improvement` Android App info icons share Icon Studio artwork and light/dark backgrounds while preserving transparent Plugin Center artwork and existing launcher choices
* `Improvement` View factories run on the main thread before rendering. Invalid replacements preserve the current content; a View cannot belong to two nodes or be taken from another parent. Borrowed Views retain their listeners and caller-owned resources
* `Improvement` cancelable=false disables back, outside-click and swipe dismissal; explicit close and script exit still release the dialog while preserving existing pages and other sessions
* `Improvement` The Plugin Center icon and the system app info icon use the #FAFAFA light / #212121 dark backgrounds shared with the other standalone plugins
* `Dependency` Update compose-ui-api.aar with the optional AndroidView interop extension while preserving frozen V1
* `Dependency` Add the optional dialog capability to compose-ui-api.aar while preserving existing V1 and AndroidView contracts
* `Dependency` Add the compose-ui-api.aar V2 component catalog while preserving existing node models and V1 component semantics
* `Dependency` Added material-color-utilities 4.1.1 (MIT) for the theme color derivation shared with the other standalone plugins
* `Dependency` Added host-locked copies of AndroidX activity, core, lifecycle, savedstate, emoji2, window, kotlinx-coroutines and their companions for the gallery in the plugin process; inside the host the host copies still take precedence

# v1.0.0

###### 2026/10/03

* `Hint` 1.0.0 local development preview: use a matching AutoJs6 host build and the installed, enabled plugin. UI pages, floating windows, five examples, companion API reference and TypeScript declarations are provided for this local integration. Verified compatibility and performance scope is recorded in the roadmap. The plugin is not in the official index and has no official release. Current icon artwork is temporary, awaiting the maintainer's source images
* `Hint` Requires AutoJs6 6.8.0 (5316) or later
* `Hint` Packaged apps also require a separately installed compatible Compose UI plugin, with enablement/authorization belonging to that app; compatibility checks the embedded AutoJs6 runtime, not the packaged app's own versionCode
* `Hint` The light/dark mipmap entries are retained; current artwork is a placeholder until the maintainer supplies the final black-and-white images
* `Feature` Callable compose / $compose with retained node handles, reactive state/render/ref, batched changes, queued posts and theme control
* `Feature` Material 3 core: 29 node factories for layouts, text, icons, images, buttons, inputs, selection controls, lazy lists, dialogs and progress; Snackbar is a session command, not a compose.Snackbar factory
* `Feature` UI scripts mount Activity content; compose.floaty also supports non-UI scripts with raw or resizable windows, pixel geometry, touch/focus controls and owned cleanup
* `Feature` All 20 modifier operations preserve declaration order, with layout scope checks, scrolling, and accessibility labels
* `Feature` Material 3 themes support seed colors, light and dark modes, Android 12+ system dynamic colors, font families, and text scaling
* `Feature` Native text editing preserves selection and IME composition, supports focus and explicit edits, and rejects delayed edits that would overwrite newer input; switches and sliders remain script-controlled
* `Feature` Core icons and ImageWrapper/Bitmap, local file and host drawable images; caller-owned image resources are not automatically recycled by the renderer
* `Feature` Five runnable examples for a counter, form validation, a keyed 1000-item list, a non-UI floating HUD, and themes, with prerequisites and an index, synchronized to the matching host's Compose UI sample category
* `Feature` Companion API reference and TypeScript declarations, plus README, plugin-center instructions and changelog in 10 languages
* `Feature` Plugin-center discovery with host-version, contract and authorization checks, without a standalone screen or launcher entry
* `Fix` Compose UI rejects mounting another page or floating window inside a render callback and keeps the current page; pages can be mounted again after plugin updates
* `Improvement` Availability probes and ComposeError consistently report missing, disabled, unauthorized or incompatible plugins, permission failures and closed sessions; lifecycle cleanup also covers windows canceled before native attachment; Updating, uninstalling or disabling the plugin closes its active sessions and reports the corresponding error
* `Dependency` Attach common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, hash-locked)
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependency` Attach compose-ui-api.aar V1 aligned to AutoJs6 6.8.0 (5316) (MPL 2.0, hash-locked), with shared dependencies aligned to the host
* `Dependency` Attach Compose UI Test managed by BOM 2026.09.00 (Apache 2.0, tests only)
* `Dependency` Attach JaCoCo version 0.8.14 (optional test coverage only, excluded from release packages)
