# Third-party notices

Compose UI (`AutoJs6-Plugin-Compose-UI`) is licensed under the Mozilla Public License 2.0 (see `LICENSE`).
The components below are distributed with the plugin or used to build it; each keeps its own license,
reproduced in full in the distribution of the respective project.

## Host contract artifacts (staged in `libs/`, hash-locked in `locks/host-api-aars.lock`)

| Artifact | Origin | Version | License | SHA-256 |
| --- | --- | --- | --- | --- |
| `common-plugin-api.aar` | AutoJs6 module `plugin-api/common-plugin-api` (https://github.com/SuperMonster003/AutoJs6): `IPluginInfoProvider` AIDL, `PluginInfo`, `PluginActions`, `PluginCapabilityKeys` | host build 6.8.0 / 5307, commit `77b5a3b0c5` (module byte-identical to the copies staged by the other official plugins) | MPL 2.0 | `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15` |

The renderer contract `compose-ui-api.aar` (`org.autojs.plugin.compose.api`, roadmap P0.2 / P1.1) joins this table
and the lock file once the host module exists; it is consumed as `compileOnly` because the host provides the
classes at run time (roadmap D26).

## Runtime dependencies (Gradle)

The plugin bundles its own Jetpack Compose runtime (roadmap D26: the host does not provide Compose classes to the
plugin class loader). All artifacts are resolved from Google Maven through the Compose BOM `2026.09.00`.

| Component | Coordinates | Version (BOM 2026.09.00) | License | Purpose |
| --- | --- | --- | --- | --- |
| Compose Runtime | `androidx.compose.runtime:runtime` | 1.12.1 | Apache License 2.0 | Composition, state and snapshot system |
| Compose UI | `androidx.compose.ui:ui` | 1.12.1 | Apache License 2.0 | Layout nodes, input, `ComposeView` |
| Compose Foundation | `androidx.compose.foundation:foundation` | 1.12.1 | Apache License 2.0 | Layouts, lazy lists, gestures |
| Compose Animation | `androidx.compose.animation:animation` | 1.12.1 | Apache License 2.0 | Animated visibility and transitions |
| Compose Material 3 | `androidx.compose.material3:material3` | 1.4.0 | Apache License 2.0 | Material 3 components and theming |
| Compose Material Icons Core | `androidx.compose.material:material-icons-core` | 1.7.8 | Apache License 2.0 | Core Material icon set |
| Compose UI Tooling (debug builds only) | `androidx.compose.ui:ui-tooling` | 1.12.1 | Apache License 2.0 | Layout inspector support in debug APKs; not in release |
| Kotlin standard library | `org.jetbrains.kotlin:kotlin-stdlib` | managed by the platform versions plugin | Apache License 2.0 | Language runtime |

Transitive AndroidX libraries pulled in by Compose (`activity`, `core`, `lifecycle`, `annotation`, `collection`,
`savedstate`, `profileinstaller`, `startup` and others) are Apache License 2.0 as well.

Test-only dependencies (JUnit 4, AndroidX Test runner / rules / ext-junit, Apache License 2.0 or EPL 1.0) are not
shipped in the APK.

## Build tooling (not distributed)

Gradle, the Android Gradle Plugin (with its built-in Kotlin support), the Compose compiler Gradle plugin
(`org.jetbrains.kotlin.plugin.compose`) and `io.github.supermonster003.autojs6-platform-versions` are used by
`build-logic/` and the Gradle build only. Python 3 with Pillow generates the launcher icons and the localized
documentation (`.python/`).
