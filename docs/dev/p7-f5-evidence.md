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
D32 packages the host-locked artifacts Compose needs (`standaloneRuntime` in
`app/build.gradle.kts`) at the exact lock versions through Gradle constraints: 36 in
build 42, 47 since build 44 (see "Launcher crash fix" below). Inside
the host the parent-first `PathClassLoader` keeps resolving these names from the host,
so the packaged copies are never loaded there; since build 44 these packages are fully
kept by R8, like `kotlin.**`, so that names stay stable and R8 draws no whole-program
conclusions from the plugin's copy. Only appcompat, which nothing in the plugin
references, stays excluded; the Kotlin stdlib family stays `implementation` as before.

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

The push of build41 ran the public workflows: Markdown and Icon Studio passed, the API24 x86 job passed all 73 tests, and the API35 x86_64 job failed 10 tests: the gallery clipboard read returned null, and 9 existing renderer scenarios that depend on window focus, popups, the back key or the IME timed out. On API 29+ the clipboard is readable only by the focused app, and the settings Activity left behind by the previous test delayed focus of the new gallery window on the slower CI emulator. Build42 waits for window focus and polls the clipboard, finishes the settings Activity opened by the settings-entry test, makes `LauncherIcons.normalize` skip the component write when the alias state is already consistent, and uploads logcat and window state after the suite. Local rerun on a fresh API35 AVD: 72 of 73 passed (all 9 gallery / settings / icon tests including the clipboard fix); the only failure was the existing F.4 scenario `WideRendererTest.pullToRefreshUsesActualNestedScrollAndHonorsDisabledState`, where heavy jank on that AVD (Choreographer skipped 102 frames, HWUI frames of 2.3 s) kept the main thread busy during the refresh indicator animation for 60 s (Espresso AppNotIdleException); 3 isolated repeats gave 2 failures and 1 pass. The same scenario passed three full runs on the previous AVD and both remote API24 / API35 jobs; neither the scenario nor the renderer changed in this round, so it is recorded as environmental. Remote rerun (workflow run 37734947171): the API35 x86_64 job passed all 73 tests; the API24 x86 job first ran 0 tests because the CI emulator lost its adb daemon connection and the APK was never written to the device (infrastructure, no PackageManager error in the uploaded logcat), and passed all 73 tests on rerun; the Markdown and Icon Studio workflows passed.

## Launcher crash fix (build 44)

The maintainer installed build 43 on a Sony Xperia XQ-DQ72 (API 33) and on an AVD:
the launcher showed the Compose UI icon, but tapping it did not open the app. The
device log shows the plugin process dying while the Activity is constructed:

```text
java.lang.NoClassDefFoundError: Failed resolution of: Landroidx/arch/core/executor/ArchTaskExecutor;
    at androidx.lifecycle.LifecycleRegistry_androidKt.isMainThread(LifecycleRegistry.android.kt:23)
    at androidx.lifecycle.LifecycleRegistry.addObserver(LifecycleRegistry.jvm.kt:172)
    at androidx.activity.ComponentActivity.<init>(ComponentActivity.kt:265)
    at io.github.supermonster003.autojs6.plugin.compose.ui.app.GalleryActivity.<init>(GalleryActivity.kt:66)
```

Root cause: the 36-artifact `standaloneRuntime` of build 42 contained arch
`core-common` but not `core-runtime`, which owns `ArchTaskExecutor`. A DEX reference
closure of the shrunk release APK (every referenced type that is neither defined in
the APK nor provided by the platform) showed 15 more classes outside the host contract:
`customview-poolingcontainer` (ComposeView disposal strategy), `emoji2` (Compose text)
and `androidx.window` (WindowInfo.containerSize). The device tests passed because the
instrumentation APK carried runtime copies of the whole shared lock in the same
process; R8 cannot report the gap either, because compileOnly artifacts reach it as
library classes.

Fix:

- `standaloneRuntime` grows to 47 artifacts: arch core-runtime,
  customview-poolingcontainer, emoji2, lifecycle-process, window, window-core and
  window-core-android for the missing classes, plus lifecycle-livedata-core and
  kotlinx-serialization (bom, core, core-jvm), which the fully kept savedstate /
  lifecycle members reference. Only appcompat (x2) stays host-only; it is excluded from
  `debugAndroidTestRuntimeClasspath` as well and `verifySharedClasspath` checks that
  the test APK carries neither.
- The shared packages are fully kept by R8 (`-keep class ... { *; }`, as `kotlin.**`
  already was) instead of `-keepnames`. The first build 44 candidate kept only names:
  the plugin's own 73 instrumentation tests and the launch check passed, but the host
  Compose device suites failed 7 scenarios with `RENDER_FAILED` and the host process
  finally crashed. `dexdump` of that candidate shows Compose's
  `EmojiCompatStatus.DefaultImpl.getFontLoadState` compiled to
  `EmojiCompat.get(); throw null`: nothing in the plugin program ever initializes
  `EmojiCompat` (the startup provider is removed), so R8 concluded that
  `EmojiCompat.get()` never returns normally and replaced the rest of the method,
  while inside the host, where parent-first loading returns the host's initialized
  `EmojiCompat`, `get()` returns and the `throw null` runs on every text layout.
  `-dontoptimize` does not prevent this whole-program conclusion; a full keep does,
  at the cost of not shrinking these packages (the release gains a second DEX).
- `verify_apk_classpath.py --shrunk` requires every class the R8 output references to
  be defined in the APK, provided by the platform (including the OEM window extensions
  that androidx.window loads reflectively behind guards) or part of the host contract.
  Against the shipped build 42 it reports 15 unloadable classes; against build 44 it
  reports 0.
- `verify_standalone_launch.py` installs an APK on one device, starts the enabled
  launcher alias as a launcher would, opens the first catalog entry and the settings
  screen through the uiautomator tree, and fails on a crash, a dead or restarted
  process or a missing screen. Against build 42 it reproduces the crash; the local
  verification order and both CI emulator jobs run it after the instrumentation.

Verification on the owned API35 x86_64 AVD `compose_f5c_api35` (emulator-5596). The
main working tree held the maintainer's uncommitted Icon Studio changes, whose
generated adaptive XML does not link, so the build, the tests and the signed APK were
produced in a detached worktree of the same HEAD plus this change.

| Check | Result |
| --- | --- |
| JVM `testDebugUnitTest` | 84 tests passed |
| `lintDebug` | 0 errors, 9 warnings |
| `verifySharedClasspath` | 51 components, 47 packaged, appcompat (2) host-only and absent from the test APK |
| `verify_apk_classpath.py` debug / `--shrunk` release | passed, 0 unresolved references outside the contract |
| `verify_standalone_launch.py` build 42 (`2f1127ce`) | fails: FATAL EXCEPTION in the plugin process |
| `verify_standalone_launch.py` build 44 debug and signed release | gallery, "Column" entry and settings opened, process alive |
| Plugin instrumentation | 73 of 73 passed, 0 skipped, with a test APK that no longer carries host-only components |
| Host device suites with the build 44 renderer (isolated host `org.autojs.autojs6.compose.spike`, host master `910aec9495`, built in a short-path host worktree) | 87 scenarios: 71 passed, 15 opt-in phases skipped by assumption, 1 failed. The 10 TSX scenarios failed in the full run because the fresh AVD had no TypeScript Engine; after installing 0.6.8 temporarily all 10 passed on rerun and are counted in the 71. The remaining failure, ComposeUiLoaderTest.loaderCachesByIdentityButRechecksEnablementAndSharesParentClasses, asserts contractVersion() == 1 (host test written 2026-10-02, before F.4 raised the contract to 2): a pre-existing stale host assertion unrelated to this change; the 87 include the 13 core/plugin/compose loader scenarios that were outside the 74 of the F.5 run. The -keepnames candidate had failed 7 scenarios with RENDER_FAILED and crashed the host process in the same suite; with the full keep none recurs |
| Remote CI for `ab46e00` (build 44) | Markdown passed; Build integrity: JVM / APK job and the API35 x86_64 job (73 tests plus the launch check) passed, the API24 x86 job passed all 73 tests but the new launch check timed out: on the Nexus 5 profile (1080x1920) the copy / run buttons of the detail screen sit below the fold and uiautomator only dumps visible nodes. Reproduced on an owned API24 x86 Nexus 5 AVD (`compose_f5c_api24`); build 45 refines the script only (the detail screen is proven by the generated script text `compose.<Entry>(` or the copy button, the first `cat` of the dump may answer 255 on API24 and is retried, a timeout reports the visible labels) and the signed `5d2ce3ef` APK then passes on that AVD (gallery, Column entry, settings) |
| Remote CI for `50b0c02` (build 45) | Markdown passed; JVM / APK job and the API24 x86 job (73 tests plus the launch check) passed; the API35 x86_64 job failed 10 of 73 tests, the same window-focus / popup / IME cluster as the first build 41 run (WideRendererTest search, drawer, bottom sheet, dropdown and tooltip, TextField IME, two Dialog focus scenarios, Advanced Scaffold and the gallery clipboard test), which passed on the build 42 / 43 reruns and on two local AVDs today and is recorded as CI emulator flakiness; the launch check that followed took the "Close app" button of a leftover ANR dialog for the first catalog entry. Build 46 (script and docs only) restricts the search to nodes whose `package` is the plugin and keeps listing every visible label on a timeout; the signed `5d2ce3ef` APK passes on a recreated API35 AVD |
| Remote CI for `81c7388` (build 46, workflow run 37768208379) | Markdown passed; Build integrity passed in all three jobs: the API24 x86 and API35 x86_64 jobs each ran the 73 instrumentation tests and finished with `STANDALONE_LAUNCH_OK` (gallery, Column entry, settings) |
| Signed release | `autojs6-plugin-compose-ui-v1.1.0-5d2ce3ef.apk`, 4673146 B, 13482 classes / 82466 DEX method references, SHA-256 `9856d4f57b361d7597cfcee39c9e50b5466cca644029a6081127c057e2890dd7`; +1048858 B / +21853 references over build 42, within the growth the maintainer accepted for F.5 |

## Not executed

- Physical devices and the API24 x86 AVD: the gallery is Compose in the plugin
  process and uses no API-level-specific path beyond what CI covers; the API24 x86
  CI job runs the same instrumentation on push.
- A real launcher's reaction to alias switching was verified only through
  `PackageManager` queries, not by screenshots of a third-party launcher.
- The host appearance provider was read against the regular host build; the
  isolated spike host uses a different package and is reported as "host missing".
- Build 44 was not reinstalled on the maintainer's Xperia or AVD (they are the
  maintainer's devices); the crash and the fix were reproduced on the owned AVD only.
