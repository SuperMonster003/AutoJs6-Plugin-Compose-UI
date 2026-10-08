# P7 F.5 component gallery evidence

Date: 2026-10-08. This records the existing F.5 roadmap item; no item is added,
split or discarded. The maintainer scheduled F.5 on 2026-10-08, revised D8 for it,
accepted the plugin size growth in advance and allowed pushing the plugin repository.

## Decision input

F.5 needs a launcher entry and a standalone interface, which D8 ("no standalone
interface") excluded. The maintainer's instruction "明确做组件画廊" revises D8: the
plugin now has exactly two standalone screens (gallery and settings) and a launcher
entry, while in-host rendering, the renderer contract and the minimum host version
stay unchanged. The launcher icon artwork is still the Q6 placeholder; the icon
resources are generated from it and are replaced when the final artwork arrives.

Compose cannot run without the AndroidX activity / lifecycle / savedstate runtime.
Until now those artifacts were `compileOnly` because the host supplies them to the
renderer (D26). A standalone Activity in the plugin process has no host classes, so
D32 packages the 36 host-locked artifacts Compose needs (`standaloneRuntime` in
`app/build.gradle.kts`) at the exact lock versions through Gradle constraints. Inside
the host the parent-first `PathClassLoader` keeps resolving these names from the host,
so the packaged copies are never loaded there; `-keepnames` rules keep the names
stable so parent-first lookup still matches after R8. The remaining 15 shared
artifacts (appcompat, emoji2, window, serialization, lifecycle-process, livedata and
the Kotlin stdlib family) stay excluded or `implementation` as before.

## Delivered behavior

- `app.GalleryActivity` (launcher target): 9 categories, 56 entries (55 node
  components plus the command-only Snackbar), every entry with a live Material 3
  preview and a generated `"ui";` script. Copy writes the script to the clipboard; run
  hands it to the host's exported `RunIntentActivity` with the `script` extra, and
  reports a missing host or a failed start through a snackbar. The gallery never
  executes scripts itself.
- `app.SettingsActivity`: appearance group language -> dark mode -> theme color ->
  launcher icon, about group with version, minimum host and developer page. Language,
  dark mode and theme color follow AutoJs6 by default through the
  `AutoJs6HostSettingsContract` provider (protocol version and package verified, read
  on a worker thread, 30 s cache, system fallback when the host is missing). Pickers
  edit a draft and persist once on confirm; cancel, back and outside clicks keep the
  saved value. Theme color offers follow-host, 16 presets and HEX / RGB input with a
  local preview; the accent follows the shared HCT rule and keeps 4.5:1 readability.
- Launcher icon: four aliases (adaptive light / dark / automatic, transparent
  background), automatic by default, each with its own icon resource; `LauncherIcons`
  switches component states with rollback and shortcut migration, and the
  `MY_PACKAGE_REPLACED` receiver normalizes the state after an update.
- Icon Studio recipe: `launcher: true`, day `#FAFAFA` / night `#212121` backgrounds
  (previously transparent) so the light and dark aliases differ; 20 generated
  resources verified by `generate_icon_studio.py --check`.
- Manifest: `<queries>` for the host package, `Theme.ComposeUi`, `localeConfig`
  with 10 languages, `androidx.startup.InitializationProvider` removed. Exported
  components protected by the PLUGIN permission are still only Wake and INFO; the
  four aliases are the only permission-less exported components.
- Dependency: `com.materialkolor:material-color-utilities` 4.1.1 (MIT) for the
  HCT derivation shared with the other standalone plugins; 21 reference values
  computed from Material Components 1.13.0 guard it in `StandalonePaletteTest`.

## Verification

Plugin 1.1.0 / build42, Compose BOM 2026.09.00, isolated host 6.8.0 / 5323
(`-PcomposeUiSpike=true`, package `org.autojs.autojs6.compose.spike`) and the regular
host debug build (`org.autojs.autojs6`) for the run-entry resolution, owned API35
x86_64 google_apis AVD `compose_f5_api35` (emulator-5594, swiftshader).

| Check | Result |
| --- | --- |
| `generate_markdown.py --check` | 10 languages, 36 artifacts |
| `generate_icon_studio.py --check` | 20 resources verified |
| JVM `testDebugUnitTest` | 84 tests passed |
| `verifySharedClasspath` | 51 components, 36 packaged for the standalone process |
| `lintDebug` | 0 errors, 10 warnings (Icon Studio icons with identical contents, launcher shape and the API 33 `localeConfig` attribute, as in the other standalone plugins) |
| `verify_apk_classpath.py` (debug and signed release) | contract classes absent, standalone runtime and `GalleryActivity` present |
| Plugin instrumentation (API35 AVD) | 73 tests passed (including the new GalleryDeviceTest 3 / SettingsDeviceTest 4 / LauncherIconDeviceTest 2); the gallery and settings tests were rerun (7 passed) after installing the regular host package `org.autojs.autojs6` (6.8.0 / 5325 debug), so the run Intent resolved and the appearance provider was read |
| Host device suites with the build40 renderer (byte-identical to build42, which only changes the plugin-process icon normalization and the tests) (API35 AVD) | 61 of 74 scenarios passed against the isolated host `org.autojs.autojs6.compose.spike` (master `b2404d3ae9`, 5325); the 13 others are opt-in phases (performance, legacy providers, packaged apps, package mutation, denied overlay permission) skipped by assumption. In the first run 5 example scenarios failed because the host working tree had CRLF copies of the example assets after the merge checkout (`core.autocrlf`); `sync_examples.py` restored the LF bytes and the host `.gitattributes` now pins `eol=lf` (host `de11353cc0`), after which they passed; 2 accessibility scenarios passed once the isolated host AccessibilityServiceUsher was enabled. TSX (10), F.2 / F.3 / F.4 / F.6 scenarios all used the build42 renderer |
| Signed release | `autojs6-plugin-compose-ui-v1.1.0-2f1127ce.apk`, 3624288 B, 10473 classes / 60613 DEX method references, SHA-256 `505ea68bb2a23ff81d01ad0735048fa5835fc9ac36d12c503b137c1290c35992`, native libraries and 16 KB alignment verified by `appendDigestToReleasedFiles` |

New device tests: `GalleryDeviceTest` (56 entries open with a composed preview and
their script, clipboard copy, run Intent targets the host and resolves when the host
is installed, settings entry), `SettingsDeviceTest` (dark mode draft / cancel /
confirm, theme color input validation, presets and follow-host, launcher icon
persistence, host appearance optional), `LauncherIconDeviceTest` (each mode is the
only launcher entry with a distinct resource, mixed states normalize).

## Size review

Recorded in `docs/dev/p7-f5-size-review.json`. The packaged runtime, catalog,
previews and resources add +600552 B / +6771 method references / +1442 classes over build38; the maintainer accepted the
growth in advance on 2026-10-08. The Q5 review limits are unchanged and remain
exceeded, as they were since F.4.

## Remote CI

The push of build41 ran the public workflows: Markdown and Icon Studio passed, the API24 x86 job passed all 73 tests, and the API35 x86_64 job failed 10 tests: the gallery clipboard read returned null, and 9 existing renderer scenarios that depend on window focus, popups, the back key or the IME timed out. On API 29+ the clipboard is readable only by the focused app, and the settings Activity left behind by the previous test delayed focus of the new gallery window on the slower CI emulator. Build42 waits for window focus and polls the clipboard, finishes the settings Activity opened by the settings-entry test, makes `LauncherIcons.normalize` skip the component write when the alias state is already consistent, and uploads logcat and window state after the suite. Local rerun on a fresh API35 AVD: 72 of 73 passed (all 9 gallery / settings / icon tests including the clipboard fix); the only failure was the existing F.4 scenario `WideRendererTest.pullToRefreshUsesActualNestedScrollAndHonorsDisabledState`, where heavy jank on that AVD (Choreographer skipped 102 frames, HWUI frames of 2.3 s) kept the main thread busy during the refresh indicator animation for 60 s (Espresso AppNotIdleException); 3 isolated repeats gave 2 failures and 1 pass. The same scenario passed three full runs on the previous AVD and both remote API24 / API35 jobs; neither the scenario nor the renderer changed in this round, so it is recorded as environmental. Remote rerun: see the workflow run of the build42 push (not finished when this record was committed).

## Not executed

- Physical devices and the API24 x86 AVD: the gallery is Compose in the plugin
  process and uses no API-level-specific path beyond what CI covers; the API24 x86
  CI job runs the same instrumentation on push.
- A real launcher's reaction to alias switching was verified only through
  `PackageManager` queries, not by screenshots of a third-party launcher.
- The host appearance provider was read against the regular host build; the
  isolated spike host uses a different package and is reported as "host missing".
