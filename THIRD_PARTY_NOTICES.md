# Third-party notices

Compose UI (`AutoJs6-Plugin-Compose-UI`) is licensed under the Mozilla Public License 2.0 (see `LICENSE`).
The components below are distributed with the plugin or used to build it; each keeps its own license,
reproduced in full in the distribution of the respective project.

## Host contract artifacts (staged in `libs/`, hash-locked in `locks/host-api-aars.lock`)

| Artifact | Origin | Version | License | SHA-256 |
| --- | --- | --- | --- | --- |
| `common-plugin-api.aar` | AutoJs6 module `plugin-api/common-plugin-api` (https://github.com/SuperMonster003/AutoJs6): `IPluginInfoProvider` AIDL, `PluginInfo`, `PluginActions`, `PluginCapabilityKeys` | host build 6.8.0 / 5307, commit `77b5a3b0c5` (module byte-identical to the copies staged by the other official plugins) | MPL 2.0 | `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15` |

The P0 draft `compose-ui-api.aar` is built with `:plugin-api:compose-ui-api:assembleRelease` in the
AutoJs6 `spike/compose-ui-p0` branch, source commit `21dff98f26`, based on `e86186920d` (6.8.0 / 5309). It is MPL 2.0,
SHA-256 `c39b0cc59834d1b9e98d7b2fdde5b8184eae7ff9fe8ff2d32a843f53a44ca437`, consumed as `compileOnly`. Its experimental `api.spike` package uses
contract version -1 and does not claim V1 compatibility; P1.1 will replace the draft.

Shared host components are pinned in `locks/host-shared-deps.lock` (debug spike snapshot only).
AndroidX (Apache 2.0) and kotlinx.coroutines / kotlinx.serialization (Apache 2.0) in that table
are compile-only; Compose integration artifacts whose names end in `-compose` remain bundled.
Kotlin standard library 2.4.0 (Apache 2.0) remains bundled for the plugin's INFO/Wake process;
parent-first loading resolves the host's identical version when rendering. The host experiment
aligns lifecycle 2.9.4, savedstate 1.3.2, emoji2 1.4.0 and window 1.5.0 only in debug builds.
Sources: Google Maven (AndroidX) and Maven Central (org.jetbrains.kotlin / org.jetbrains.kotlinx).

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
| AndroidX Graphics Path | `androidx.graphics:graphics-path` | 1.0.1 | Apache License 2.0 | Native path iteration helper of Compose `ui-graphics` on API 24 to 33; the only native library in the APK (`libandroidx.graphics.path.so` for arm64-v8a / armeabi-v7a / x86_64 / x86, about 10 KB each, 16 KB page-aligned, verified by `appendDigestToReleasedFiles`) |
| Compose UI Tooling (debug builds only) | `androidx.compose.ui:ui-tooling` | 1.12.1 | Apache License 2.0 | Layout inspector support in debug APKs; not in release |
| Kotlin standard library | `org.jetbrains.kotlin:kotlin-stdlib` | 2.4.0 (host shared lock) | Apache License 2.0 | INFO process runtime; parent-first in the host |

Transitive AndroidX libraries pulled in by Compose (`activity`, `core`, `lifecycle`, `annotation`, `collection`,
`savedstate`, `profileinstaller`, `startup` and others) are Apache License 2.0 as well.

Test-only dependencies (JUnit 4, AndroidX Test runner / rules / ext-junit, Apache License 2.0 or EPL 1.0) are not
shipped in the APK.

## Build tooling (not distributed)

Gradle, the Android Gradle Plugin (with its built-in Kotlin support), the Compose compiler Gradle plugin
(`org.jetbrains.kotlin.plugin.compose`) and `io.github.supermonster003.autojs6-platform-versions` are used by
`build-logic/` and the Gradle build only. Python 3 with Pillow generates the launcher icons and the localized
documentation (`.python/`).
