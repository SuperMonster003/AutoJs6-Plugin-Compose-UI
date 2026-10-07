******

### Release History

******

# v1.1.0

###### 2026/10/08

* `Hint` 1.1.0 local development preview: use a matching AutoJs6 host build and the installed, enabled plugin. UI pages, floating windows, five examples, companion API reference and TypeScript declarations are provided for this local integration. Verified compatibility and performance scope is recorded in the roadmap. The plugin is not in the official index and has no official release. Current icon artwork is temporary, awaiting the maintainer's source images
* `Hint` This build uses Compose UI contract V2 and requires the matching AutoJs6 6.8.0 / 5322 host; companion TSX support requires TypeScript Engine 0.6.7. New hosts still support existing components in older V1 renderers; extended components require a V2 renderer
* `Feature` TSX supports `<compose.Column>`, `<compose:Text>`, references to node factories, fragments, slots and reactive callbacks; a single tree cannot mix Compose and legacy XML nodes
* `Feature` XML `<compose>` containers and compose.attach embed independent Compose sessions in UI pages or legacy floating windows; compose.AndroidView embeds an existing Android View or one returned by a synchronous factory
* `Feature` compose.dialog returns an updatable, closable session for dialogs and modal bottom sheets in UI or ordinary scripts
* `Feature` Material 3 extended components: navigation bars and drawers, tabs, bottom sheets and menus, date and time pickers, paging and grids, chips, badges, segmented buttons, floating action buttons, search bars, tooltips and pull to refresh
* `Improvement` Android App info icons share Icon Studio artwork and light/dark backgrounds while preserving transparent Plugin Center artwork and existing launcher choices
* `Improvement` View factories run on the main thread before rendering. Invalid replacements preserve the current content; a View cannot belong to two nodes or be taken from another parent. Borrowed Views retain their listeners and caller-owned resources
* `Improvement` cancelable=false disables back, outside-click and swipe dismissal; explicit close and script exit still release the dialog while preserving existing pages and other sessions
* `Dependency` Update compose-ui-api.aar with the optional AndroidView interop extension while preserving frozen V1
* `Dependency` Add the optional dialog capability to compose-ui-api.aar while preserving existing V1 and AndroidView contracts
* `Dependency` Add the compose-ui-api.aar V2 component catalog while preserving existing node models and V1 component semantics

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
