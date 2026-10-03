******

### Release History

******

# v1.0.0

###### 2026/10/03

* `Hint` P2 development preview: 20 layout, display, and interactive components, ordered modifiers, and Material 3 themes run in a dedicated test host. Text fields, lazy lists, Scaffold, dialogs, other remaining components, and the compose script API are still under development
* `Hint` Requires AutoJs6 6.8.0 (5316) or later
* `Feature` Plugin repository skeleton: platform versions plugin build chain, Jetpack Compose BOM 2026.09.00 dependencies, Wake Activity activation protocol, and INFO service (category compose-ui)
* `Feature` README, plugin center instruction, and changelog in 10 languages, generated from JSON sources
* `Feature` Preview rendering supports layouts, text, icons, images, buttons, selection controls, and sliders; supplied bitmaps remain owned and recycled by the caller
* `Feature` All 20 modifier operations preserve declaration order, with layout scope checks, scrolling, and accessibility labels
* `Feature` Material 3 themes support seed colors, light and dark modes, Android 12+ system dynamic colors, font families, and text scaling
* `Feature` Interface updates apply atomically and rejected updates keep the last valid view; controlled inputs report changes through queued callbacks, and closing releases callbacks
* `Dependency` Attach common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, hash-locked)
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependency` Attach compose-ui-api.aar V1 aligned to AutoJs6 6.8.0 (5316) (MPL 2.0, hash-locked), with shared dependencies aligned to the host
* `Dependency` Attach Compose UI Test managed by BOM 2026.09.00 (Apache 2.0, tests only)
