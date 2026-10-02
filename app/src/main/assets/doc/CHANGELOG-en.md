******

### Release History

******

# v1.0.0

###### 2026/10/02

* `Hint` P1 development preview: the V1 host loader and sessions can run a Column / Text / Button counter in a dedicated test host. The complete renderer and compose script API are still under development
* `Hint` Requires AutoJs6 6.8.0 (5308) or later (the exact minimum build is back-filled once the host-side changes land)
* `Feature` Plugin repository skeleton: platform versions plugin build chain, Jetpack Compose BOM 2026.09.00 dependencies, Wake Activity activation protocol, and INFO service (category compose-ui)
* `Feature` README, plugin center instruction, and changelog in 10 languages, generated from JSON sources
* `Feature` Preview counter supports incremental updates and callback cleanup on close; rejected updates preserve the last valid interface
* `Dependency` Attach common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, hash-locked)
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependency` Attach compose-ui-api.aar V1 (MPL 2.0, hash-locked), with shared dependencies aligned to the host
