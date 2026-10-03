Compose UI is a user interface rendering plugin for AutoJs6. Scripts declare interfaces through the host-provided `compose` / `$compose` entry, and the plugin renders them inside the host process with Jetpack Compose and Material 3. The current preview supports both `"ui";` activity content and floating windows from non-UI scripts.

P3 development preview: the callable compose / $compose entry, 29 node factories, retained handles, reactive state/render/ref, batch/post/theme, UI script mounting, and raw or resizable floating windows work with a matching local AutoJs6 host build. Availability probes, typed errors, and session cleanup are included. Bundled examples, complete API documentation, type declarations, and the wider verification matrix are still pending. This is a local preview without an official release.

### Usage

1. Install a matching local AutoJs6 build with the compose script entry (minimum 6.8.0 / 5316)
2. Install this plugin APK (there is nothing to open, the plugin has no launcher entry)
3. Confirm in the AutoJs6 plugin center that Compose UI is recognized and enabled
4. Use `compose` or `$compose` in scripts; mount activity content with `compose.mount`, or grant the host overlay permission and use `compose.floaty`

### Compatibility

- Minimum AutoJs6 version: 6.8.0 (5316) or later; older hosts flag the plugin as incompatible in the plugin center
- Android version: 7.0 (API 24) or later
- Processor architecture: arm64-v8a / armeabi-v7a / x86_64 / x86 (all four built into the single APK, no per-architecture download)
- Compose version: bundled with the plugin (BOM 2026.09.00), independent of the host's Compose runtime
- Contract version: 1; host and plugin negotiate the contract version and refuse to load with a clear error when it does not match

### FAQ

- Why is there no plugin icon after installing? The plugin has no standalone UI and no launcher entry; look it up in the AutoJs6 plugin center
- Why is `compose` missing? The global object is supplied by the matching local host build; installing this plugin APK alone does not add it
- Do other UI plugins need to be uninstalled? No, Compose UI does not interfere with the existing `ui` module or other plugins
- Do scripts need changes after a plugin update? Not while the contract version stays the same; contract upgrades are called out explicitly in the changelog
- What do floating windows require? Grant overlay permission to the host. Call `window.requestFocus()` before text input; if a HyperOS window is not visible, return to the desktop. Missing permission reports PERMISSION_REQUIRED without opening a permission prompt automatically

### Permissions and Security

- Component protection: both the Wake Activity and the INFO service are guarded by the `org.autojs.permission.PLUGIN` signature permission, so only the AutoJs6 host can reach them
- No background activity: the plugin has no resident services, broadcast receivers, or scheduled jobs, and consumes no resources while the host is not loading it
- Data boundary: the plugin never reads or writes script data or user files; UI state exists only in the host process memory
- Backup policy: app backup and device transfer are disabled, and the plugin holds no data worth migrating

More information (quick start, build notes, roadmap) is available on the project page: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
