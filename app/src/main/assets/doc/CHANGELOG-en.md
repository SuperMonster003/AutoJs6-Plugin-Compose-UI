******

### Release History

******

# v1.0.0

###### 2026/10/03

* `Hint` P2 development preview: all 30 V1 catalog entries are implemented in a dedicated test host, including 29 node components and the Snackbar command. Native text fields, lazy lists, Scaffold, dialogs, and progress indicators are available in the preview. The public compose script API is still planned for P3
* `Hint` Requires AutoJs6 6.8.0 (5316) or later
* `Feature` Plugin repository skeleton: platform versions plugin build chain, Jetpack Compose BOM 2026.09.00 dependencies, Wake Activity activation protocol, and INFO service (category compose-ui)
* `Feature` README, plugin center instruction, and changelog in 10 languages, generated from JSON sources
* `Feature` Preview rendering supports layouts, text, icons, images, buttons, selection controls, and sliders; supplied bitmaps remain owned and recycled by the caller
* `Feature` All 20 modifier operations preserve declaration order, with layout scope checks, scrolling, and accessibility labels
* `Feature` Material 3 themes support seed colors, light and dark modes, Android 12+ system dynamic colors, font families, and text scaling
* `Feature` Interface updates apply atomically and rejected updates keep the last valid view; controlled inputs report changes through queued callbacks, and closing releases callbacks
* `Feature` Preview text fields preserve selection and IME composition, support focus and explicit edits, and reject delayed edits that would overwrite newer input
* `Feature` Preview adds lazy lists with stable item keys and indexed scrolling, Scaffold and top app bar slots, controlled dialogs, progress indicators, and queued Snackbar action or dismissal callbacks
* `Dependency` Attach common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, hash-locked)
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependency` Attach compose-ui-api.aar V1 aligned to AutoJs6 6.8.0 (5316) (MPL 2.0, hash-locked), with shared dependencies aligned to the host
* `Dependency` Attach Compose UI Test managed by BOM 2026.09.00 (Apache 2.0, tests only)
