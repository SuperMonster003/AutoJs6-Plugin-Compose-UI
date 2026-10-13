# P8 visual designer evidence (1.2.0)

Collected on 2026-10-13 for roadmap phase P8 (decisions D33 - D40, revisions of D1 / D8 / D26 / D32,
questions Q8 - Q11). Each sub-phase has its own evidence paragraph in ROADMAP.md; this file is the
consolidated record for the 1.2.0 local gate. Nothing here is an official release: the plugin is not
listed in the official index, and the host, documentation and Offline Docs commits are local.

## Commits

| Repository | Commit | Build | Content |
| --- | --- | --- | --- |
| Plugin P8.0 | 7dbc3b2 (+ c0fc2aa evidence) | 100 / 101 | Rename to 3-Block Compose, final icons |
| Plugin P8.1 | f19a201 | 102 | Contract AAR packaged and kept, Rhino slim parser jar locked |
| Plugin P8.2 | 398a3c7 | 103 | Design model, commands, Rhino parser, script writer, validator |
| Plugin P8.3 | 37b418d | 104 | Designer home and editor, real-renderer canvas, outline, inspector, palette, drafts |
| Plugin P8.4 | 16e7430 | 105 | FileProvider, host destinations, external documents, outside-change reload |
| Plugin P8.5 | 1e09e86 | 106 | README / changelog revision, this file, gate run |
| Plugin follow-up | d991e80 | 107 | Size baseline accepted (AGENTS.md section 14) |
| Plugin follow-up | 23f1814 | 108 | Canvas beside the panels in landscape, room for the add button |
| Plugin follow-up | 4c41ca2 | 109 | Export picker tests, settings rows scrolled into view |
| Plugin follow-up | 0c632e9 | 110 | "Discard" on exit no longer writes the edits back as a draft |
| Plugin follow-up | 9cdadf7 | 111 | `verify_designer_recovery.py`, `verify_host_handoff.py` |
| Plugin follow-up | 1897550 | 112 | Roadmap P9 plan (designer enhancements, not started) |
| Plugin follow-up | be3f8fb, a8f1055 | 113, 114 | This section and the session record; P9 decisions D41 - D45 |
| Plugin follow-up | f690b2f | 115 | Scripts: clipped palette entry, long editor menu |
| Plugin follow-up | 355654e | 116 | Opt-in designer performance test, `p8-designer-performance.md` |
| Host | f12665ffde | - | Plugin identity constants, examples and strings renamed |
| Host | db7e6a1734 | - | Editor menu "Open in 3-Block Compose designer", reload after hand-over |
| Documentation | 8538de3 | counter 102 | Designer guide page composeDesigner, compose link, sidebar / toc / changelog / progress |
| Documentation | f21f168, 1395253 (+ 9b4350a) | counter 104 | compose page names the 1.2.0 preview; editor layout in portrait and landscape |
| Offline Docs | 5f7bf9f | 132 | Site sync with the designer page, 10-language changelog |
| Offline Docs | 0b81293 | 133 | Site sync of the two documentation follow-ups (provenance 1395253) |

## What shipped

- Standalone application: the launcher opens the designer home (projects in `files/designer/projects/`),
  the gallery and the settings screen hang off it. The editor renders the document with the real
  renderer (`PreviewFrames` -> `RenderFrame`, `LocalNodeDecorator` bounds, bounded logical screen,
  zoom and panning), with an outline, a catalog-driven inspector (literal / state / expression values,
  shortcuts, slots, handler source with a Rhino syntax check, modifier chains), a component palette
  (54 insertable node components), host options, an issues list, undo / redo and autosaved drafts.
- Script round trip: the Rhino 2.0.0-SNAPSHOT slim parser (locked by `locks/rhino-jar.lock`, never
  entering a `Context`) reads top-level `compose.state` declarations and the single mount / floaty /
  dialog call; everything else is kept as opaque source. The writer's canonical format is a fixed
  point of the reader (56 gallery scripts and 5 examples).
- Host integration: the only provider is the designer `FileProvider` (authority
  `<applicationId>.designer`, only `files/designer/projects/`); destinations are run, open in the
  AutoJs6 editor (content Uri, read + write grant), import into the AutoJs6 script directory, copy and
  export; the editor is exported for EDIT / VIEW script documents and writes back through the Uri,
  falling back to a private project without write access; outside changes are reloaded on resume or
  offered when there are local edits. The host editor gained the reverse entry (visible with plugin
  1.2.0+), hands the file over with read / write grants and reloads it when the designer wrote it back.

## Size and method references against the Q8 budget

| Build | Release APK | DEX method references | Note |
| --- | --- | --- | --- |
| 1.1.0 build 44 (F.5 final, `docs/dev/p7-f5-size-review.json`) | 4673146 B | 82466 | packaged AndroidX runtime for the gallery |
| P8.1 (contract AAR packaged) | 4835611 B | - | contract classes 230 |
| P8.2 (model, no UI entry) | 4839799 B | - | designer and Rhino classes shrunk away |
| P8.3 (designer UI) | 5074033 B | 87447 | designer 445 and Rhino 130 kept classes in `mapping.txt` |
| P8.4 (host integration) | 5086901 B | 87629 | build 104 artifact |
| P8.5 / 1.2.0 gate build 106 | 5094361 B | 87629 | README / changelog assets refreshed |

The 1.2.0 gate build grows by 421215 B (about 0.40 MiB) and 5163 method references over the 1.1.0
final, far inside the Q8 budget (increments under 10 MiB do not block). The section 14 review lines
(2612576 B / 49194 references) were already exceeded by the 1.1.0 F.5 build that packaged the
AndroidX runtime ("accepted-by-maintainer-for-f5; build44 growth reported for confirmation").
On 2026-10-13 the maintainer accepted the 1.2.0 gate build as the new baseline: under the section 14
rule (measured value plus 10 %, rounded up) the review lines are now 5603798 B and 96392 method
references, recorded in AGENTS.md section 14.

## Local gate (AGENTS.md section 14)

| Step (AGENTS.md section 14) | Result |
| --- | --- |
| `generate_markdown.py --check`, `generate_icon_studio.py --check`, `git diff --check` | pass (36 Markdown artifacts, 24 Icon Studio resources) |
| `:app:testDebugUnitTest` | 121 tests, 0 failures |
| `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:assembleRelease :app:verifySharedClasspath` | pass; lint 0 errors, 14 warnings, 4 hints |
| `verify_apk_classpath.py` debug / `--shrunk` release | pass; release 5094361 B, 14335 classes, 87629 method references, 230 contract classes, 0 unresolved |
| `connectedDebugAndroidTest` on AVD_API_30 (x86_64) | 87 tests, 0 failures |
| `connectedDebugAndroidTest` on AVD_API_24 (x86) | 87 tests, 0 failures |
| `verify_standalone_launch.py` (release APK; home -> gallery -> settings -> new project editor -> delete) | AVD_API_30: pass; AVD_API_24: pass (after a reboot; a fake accessibility service left behind by the host suites made uiautomator return a null root) |
| Host-side compose matrix (spike host, host androidTest APK, release plugin) | see the table below |

Host compose device tests, 15 classes, against the signed 1.2.0 release plugin (build 106); the
overlay permission was granted through `appops`, the TypeScript Engine 0.6.8 debug build was
installed for the TSX class, and `ComposeUiLoaderTest` was run a second time against the debug
plugin because it loads `androidx.compose.ui.platform.ComposeView` by name, which the shrunk
release renames:

| Host compose test class | AVD_API_30 (x86_64) | AVD_API_24 (x86) |
| --- | --- | --- |
| `ComposeAccessibilityDeviceTest` (needs the spike host's accessibility service enabled) | 2 / 2 | 2 / 2 |
| `ComposeDialogDeviceTest` | 8 / 8 | 8 / 8 (first pass 7 / 8: one IME-timing assertion, clean on rerun) |
| `ComposeExamplesDeviceTest` | 5 / 5 | 5 / 5 |
| `ComposeFloatyScriptDeviceTest` | 8 / 8 | 8 / 8 |
| `ComposeInteropDeviceTest` | 7 / 7 | 7 / 7 |
| `ComposePackagedApkProducerDeviceTest` | 1 / 1 | 1 / 1 |
| `ComposePackagedAppDeviceTest` | 1 / 1 | 1 / 1 |
| `ComposePerformanceDeviceTest` (collection opt-in, all assumption-skipped) | 6 / 6 | 6 / 6 |
| `ComposeRobustnessDeviceTest` | 8 / 8 | 8 / 8 |
| `ComposeScriptUiDeviceTest` | 8 / 8 | 8 / 8 |
| `ComposeTsxDeviceTest` | 10 / 10 | blocked: the TypeScript Engine compiler worker probe times out on this x86 AVD (`ERR_AUTOJS6_TYPESCRIPT_COMPILATION_TIMEOUT`, 10 / 10 cases) |
| `ComposeUiControlsTest` | 5 / 5 | 5 / 5 |
| `ComposeUiLoaderTest` (debug plugin) | 7 / 7 | 7 / 7 |
| `ComposeV2CompatibilityTest` | 1 / 1 | 1 / 1 |
| `ComposeV2DeviceTest` | 10 / 10 (rerun with the accessibility service disabled; with it enabled `dropdownTooltipAndModalSheetWorkFromAnActivity` hung) | 10 / 10 |

Assumption skips inside the passing classes are the suites' own opt-in phases (legacy providers,
packaged-app modes, performance collection, owned-AVD package mutation, a Xiaomi IME device).
The one stale host assertion found on the way (`ComposeUiLoaderTest` expected contract version 1
since F.4 raised it to 2) was corrected in host commit f4b53b28df.

Devices: to leave the real devices to the other session running in parallel, the P8.4 and gate runs
used the local AVDs only (`AVD_API_30`, x86_64, Android 11; `AVD_API_24`, x86, Android 7.0). The
host-side compose matrix ran against the spike host (`-PcomposeUiSpike=true`, package
`org.autojs.autojs6.compose.spike`) with the signed 1.2.0 release plugin installed and the overlay
permission granted through `appops`. Real devices covered the earlier sub-phases: the P8.3 suite
passed on the Xiaomi Pad 23046RP50C (API 35) and the first multi-device run also passed on a
Redmi 22120RN86C (API 33), a OnePlus OPD2413 (API 35) and a Sony G8441 (API 28).

## Real-device follow-up (2026-10-13, afternoon)

The maintainer confirmed the size baseline, kept every repository local and asked for real-device
coverage on the Sony G8441, the Redmi 22120RN86C and the Xiaomi Pad. Other sessions were using the
same USB devices, so each run was coordinated with them; the Xiaomi Pad run was stopped after two minutes
when another session started its own instrumentation there (the device was left with the signed release
build 106; build 104 had been installed before).

| Device | Plugin instrumentation | Notes |
| --- | --- | --- |
| Sony G8441, API 28, 720 x 1280, held in landscape (640 x 360 dp), en-US | gate build 106: 83 / 87; build 107 tree: 89 / 89; build 110: 90 / 90 | The four first-round failures reproduced on a rerun: three settings tests clicked a row or dialog option scrolled outside the window, and `undoRedoAndOutlineMovesEditTheTree` hit the add button that covered the selected outline row's move button in the stacked layout. Fixed by 23f1814 (side-by-side layout when wider than tall, room under both panels) and 4c41ca2 (scroll before click). One build 110 run lost `WideRendererTest.searchQueriesImeSearchAndExpandedResultsStayControlled` while another session injected touches on the same device; the full rerun passed 90 / 90. |
| Redmi 22120RN86C, API 33, 720 x 1650, portrait, en-US | gate build 106: 87 / 87; build 107 tree: 89 / 89; build 110: 90 / 90 | |

Device scripts with the signed release APK of build 110 (5094845 B, 87646 method references, under the
new review lines):

| Check | G8441 (API 28, landscape) | Redmi (API 33) | AVD_API_30 |
| --- | --- | --- | --- |
| `verify_standalone_launch.py` | pass | pass | - |
| `verify_designer_recovery.py` (real `am kill`) | pass (editor restored in a new process) | pass (same) | pass |
| `verify_host_handoff.py` | pass after the host reinstall below | pass after the host reinstall below | pass (AutoJs6 6.8.0 / 5352 debug installed 12:32, after db7e6a1734) |

The first G8441 recovery run found a real bug: leaving the editor with "Discard" cleared the draft and
finished the Activity, but onStop wrote the still-dirty session back as a draft, so the next open offered
the discarded edits again. 0c632e9 fixes it and `DesignerDeviceTest.discardingOnExitLeavesNoDraftBehind`
covers it.

The hand-over check first could not run on the two phones: their AutoJs6 (6.8.0 / 5352) was installed on
2026-10-12, before db7e6a1734, and has no designer entry although the version code is the same. After the
maintainer allowed replacing host and plugin builds on the devices, AutoJs6 was built from host master
`6e09d9abfd` (`:app:assembleAppDebug`, the same signing key, so the update kept the app's data) and installed
on both phones; the hand-over check then passed on the Redmi (API 33) and on the G8441 (API 28, landscape).
Two script fixes came out of it (f690b2f): on the landscape G8441 the editor's overflow menu is taller than
the screen and "More" has to be scrolled to, and on the Redmi the palette could stop with the Button entry
half under the sheet's top edge, where a tap lands on the drag handle. The isolated spike host could not stand
in for a regular host at first: its editor built the content Uri with `AppFileProvider.AUTHORITY`, then
hard-coded to `org.autojs.autojs6.fileprovider`, so granting it from `org.autojs.autojs6.compose.spike` failed
with a SecurityException. Host commit 5e0e23bd0b derives the authority from the build
(`BuildConfig.FILE_PROVIDER_AUTHORITY`, the same value as the manifest placeholder, read through a method so
callers do not inline it across incremental builds); with it, `verify_host_handoff.py --host-package
org.autojs.autojs6.compose.spike` passed on AVD_API_30, and the regular host built from the same commit still
passed with its unchanged authority.

Host compose matrix on the Redmi (spike host 5352 built 12:42 with `-PcomposeUiSpike=true`, host
androidTest APK, overlay granted through `appops`): 12 classes with the release plugin, 68 tests, 53 passed,
15 skipped by the suites' own opt-in assumptions, 0 failed; `ComposeUiLoaderTest` with the debug plugin
7 / 7. After TypeScript Engine on the Redmi was updated from 0.6.3 to the
0.6.8 debug build (same signing key), `ComposeTsxDeviceTest` passed 10 / 10 there. The class still fails on
AVD_API_24 because of the image, not the plugin: its WebView is 53.0.2785.124, and TypeScript Engine requires
Chrome / WebView 119 or newer and fails closed on the stock API 24 / 25 provider (its quick start and 0.6.0
release notes), which surfaces as the compiler worker probe timing out. Not run on a phone:
`ComposeAccessibilityDeviceTest`, which needs an accessibility service enabled in the device settings. The
spike host and the test packages were uninstalled afterwards; both phones keep the new AutoJs6 build and the
3-Block Compose 1.2.0 release (build 110).

Designer performance: the first baseline (pipeline, editor open, undo / redo on four devices) is in
`p8-designer-performance.md`; no review line is set yet.

## Known limitations recorded for 1.2.0

- The designer does not run scripts: events do not fire, state bindings show their initial value,
  opaque children and expressions are placeholders, open AlertDialog / ModalBottomSheet /
  DropdownMenu / Tooltip are inline cards, Image sources are host-resolved placeholders, AndroidView
  is a placeholder and not insertable, Snackbar is a session command and not in the palette.
- Canvas drops append to the target container; exact positions are chosen in the outline. Modifier
  chains are reordered with move buttons.
- TSX documents are not opened by the designer (JavaScript only in 1.2.0).
- Draft recovery after a killed process: instrumentation still uses a pre-written draft file (it runs in
  the process it would have to kill); `verify_designer_recovery.py` covers the real kill on devices.
  Export is covered by `DesignerHostDeviceTest` with the system picker answered by an Instrumentation
  monitor; the host editor hand-over by `verify_host_handoff.py`, which needs an AutoJs6 build with the
  editor entry and the regular application ID.
- The documentation site page and the Offline Docs sync describe the local preview; remote steps
  (pushing, releases, index registration) follow D7 and the maintainer's decision.
