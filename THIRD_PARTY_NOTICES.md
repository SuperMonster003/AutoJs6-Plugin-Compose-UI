# Third-party notices

Compose UI (`AutoJs6-Plugin-Compose-UI`) is licensed under the Mozilla Public License 2.0 (see `LICENSE`).
The components below are distributed with the plugin or used to build it; each keeps its own license,
reproduced in full in the distribution of the respective project.

## Host contract artifacts (staged in `libs/`, hash-locked in `locks/host-api-aars.lock`)

| Artifact | Origin | Version | License | SHA-256 |
| --- | --- | --- | --- | --- |
| `common-plugin-api.aar` | AutoJs6 module `plugin-api/common-plugin-api` (https://github.com/SuperMonster003/AutoJs6): `IPluginInfoProvider` AIDL, `PluginInfo`, `PluginActions`, `PluginCapabilityKeys` | host build 6.8.0 / 5307, commit `77b5a3b0c5` (module byte-identical to the copies staged by the other official plugins) | MPL 2.0 | `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15` |

The `compose-ui-api.aar` is built with `:plugin-api:compose-ui-api:assembleRelease` in
AutoJs6 branch `spike/compose-ui-p0` for F.2 (6.8.0 / 5320, commit
`d54f21b1ea408dfcb66e58f663d71d2f10fade8a`, source and checks in
`docs/dev/p7-interop-evidence.md`). It is MPL 2.0, SHA-256
`0aaff93a27d405e8db7a513172ba1a2ebe60a3b8a09d4f27d8c9eaa03544d4a3`, consumed as
`compileOnly` by the plugin and packaged by the host. It contains no Compose implementation
dependency. All 197 retained V1 class files are byte-identical to the P1.3 artifact; the new
`api.interop` package is an explicitly negotiated optional extension. The seven unused
negative-version `api.spike` classes, which were outside frozen V1, have been removed.

Shared host components are pinned in `locks/host-shared-deps.lock` (Q1(b), approved for app/inrt debug/release on 2026-10-02).
AndroidX (Apache 2.0) and kotlinx.coroutines / kotlinx.serialization (Apache 2.0) in that table
are compile-only; Compose integration artifacts whose names end in `-compose` remain bundled.
Kotlin standard library 2.4.0 (Apache 2.0) remains bundled for the plugin's INFO/Wake process;
parent-first loading resolves the host's identical version when rendering. Q1(b) aligns lifecycle
2.9.4, savedstate 1.3.2, emoji2 1.4.0 and window 1.5.0 in every host variant. V1 contract and
the negotiated optional interop classes are supplied by the matching host.
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

JaCoCo 0.8.14 (`org.jacoco:org.jacoco.agent` and report tooling, Eclipse Public License 2.0,
from Maven Central, [project](https://www.jacoco.org/jacoco/)) measures P4 device coverage.
Its runtime is included only in debug test builds made with `-PcomposeUiCoverage=true`.
It is never included in the release plugin. Reports exclude dependency classes and generated BuildConfig.

## Renderer test dependency

The instrumentation APK uses `androidx.compose.ui:ui-test-junit4` (Android variant
`ui-test-junit4-android`, 1.12.1 from BOM 2026.09.00), from Google Maven under Apache 2.0.
It is test-only and is not packaged in the plugin APK. JVM tests provide the same locked shared
AndroidX/coroutine dependencies as the host so pure value/theme tests can run without a host APK.
No new runtime dependency or native library was added for P2. Theme seed colors use an independent
CIELAB/LCh tonal construction with gamut reduction; this is not an implementation of Google's HCT.
