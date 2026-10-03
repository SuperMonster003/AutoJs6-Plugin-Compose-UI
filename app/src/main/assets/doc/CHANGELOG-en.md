******

### Release History

******

# v1.0.0

###### 2026/10/03

* `Hint` P4 development preview: the callable compose / $compose entry, 29 node factories, retained handles, reactive state/render/ref, batch/post/theme, UI script mounting, and raw or resizable floating windows work with a matching local AutoJs6 host build. Availability probes, typed errors, and session cleanup are included. Five examples for a counter, form, 1000-item list, floating HUD, and themes are bundled and synchronized to the matching host's Compose UI sample category. Complete API documentation, type declarations, and the full P5 verification matrix remain pending. This is a local preview without an official release
* `Hint` Requires AutoJs6 6.8.0 (5316) or later
* `Feature` Plugin repository skeleton: platform versions plugin build chain, Jetpack Compose BOM 2026.09.00 dependencies, Wake Activity activation protocol, and INFO service (category compose-ui)
* `Feature` README, plugin center instruction, and changelog in 10 languages, generated from JSON sources
* `Feature` Preview rendering supports layouts, text, icons, images, buttons, selection controls, and sliders; supplied bitmaps remain owned and recycled by the caller
* `Feature` All 20 modifier operations preserve declaration order, with layout scope checks, scrolling, and accessibility labels
* `Feature` Material 3 themes support seed colors, light and dark modes, Android 12+ system dynamic colors, font families, and text scaling
* `Feature` Interface updates apply atomically and rejected updates keep the last valid view; controlled inputs report changes through queued callbacks, and closing releases callbacks
* `Feature` Preview text fields preserve selection and IME composition, support focus and explicit edits, and reject delayed edits that would overwrite newer input
* `Feature` Preview adds lazy lists with stable item keys and indexed scrolling, Scaffold and top app bar slots, controlled dialogs, progress indicators, and queued Snackbar action or dismissal callbacks
* `Feature` The script preview exposes callable compose / $compose, 29 node factories, retained handles, reactive state/render/ref, batching, posting, and theme control
* `Feature` UI scripts can mount Compose content; replacing the mount or stopping the script releases the old session and callbacks
* `Feature` Non-UI scripts can create raw or resizable Compose floating windows, change pixel geometry, touch and focus settings, and close them through their controls, floaty.closeAll, or script termination
* `Feature` Five runnable examples for a counter, form validation, a keyed 1000-item list, a non-UI floating HUD, and themes, with prerequisites and an index, synchronized to the matching host's Compose UI sample category
* `Fix` Compose UI rejects mounting another page or floating window inside a render callback and keeps the current page; pages can be mounted again after plugin updates
* `Improvement` Availability probes and ComposeError consistently report missing, disabled, unauthorized or incompatible plugins, permission failures and closed sessions; lifecycle cleanup also covers windows canceled before native attachment; Updating, uninstalling or disabling the plugin closes its active sessions and reports the corresponding error
* `Dependency` Attach common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, hash-locked)
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependency` Attach compose-ui-api.aar V1 aligned to AutoJs6 6.8.0 (5316) (MPL 2.0, hash-locked), with shared dependencies aligned to the host
* `Dependency` Attach Compose UI Test managed by BOM 2026.09.00 (Apache 2.0, tests only)
* `Dependency` Attach JaCoCo version 0.8.14 (optional test coverage only, excluded from release packages)
