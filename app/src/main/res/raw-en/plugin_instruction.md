Compose UI is a user interface rendering plugin for AutoJs6. Scripts declare their interface through the host's built-in `compose` global object, and the plugin renders it inside the host process with Jetpack Compose and Material 3, giving `"ui";` mode activities and floating windows one declarative UI solution.

P0 development preview: a dedicated test host can render a Column / Text / Button counter. The compose script API is not yet available.

### Usage

1. Install AutoJs6 6.8.0 (5308) or later
2. Install this plugin APK (there is nothing to open, the plugin has no launcher entry)
3. Confirm in the AutoJs6 plugin center that Compose UI is recognized and enabled
4. Use the `compose` global object directly in scripts (rendering arrives with version 1.0.0)

### Compatibility

- AutoJs6 version: 6.8.0 (5308) or later; older hosts flag the plugin as incompatible in the plugin center
- Android version: 7.0 (API 24) or later
- Processor architecture: arm64-v8a / armeabi-v7a / x86_64 / x86 (all four built into the single APK, no per-architecture download)
- Compose version: bundled with the plugin (BOM 2026.09.00), independent of the host's Compose runtime
- Contract version: 1; host and plugin negotiate the contract version and refuse to load with a clear error when it does not match

### FAQ

- Why is there no plugin icon after installing? The plugin has no standalone UI and no launcher entry; look it up in the AutoJs6 plugin center
- Why does `compose` not work in scripts yet? This is a P0 development preview; the renderer and the script API arrive in later milestones
- Do other UI plugins need to be uninstalled? No, Compose UI does not interfere with the existing `ui` module or other plugins
- Do scripts need changes after a plugin update? Not while the contract version stays the same; contract upgrades are called out explicitly in the changelog

### Permissions and Security

- Component protection: both the Wake Activity and the INFO service are guarded by the `org.autojs.permission.PLUGIN` signature permission, so only the AutoJs6 host can reach them
- No background activity: the plugin has no resident services, broadcast receivers, or scheduled jobs, and consumes no resources while the host is not loading it
- Data boundary: the plugin never reads or writes script data or user files; UI state exists only in the host process memory
- Backup policy: app backup and device transfer are disabled, and the plugin holds no data worth migrating

More information (quick start, build notes, roadmap) is available on the project page: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
