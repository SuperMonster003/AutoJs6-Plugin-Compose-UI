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
| Plugin P8.5 | this commit | 106 | README / changelog revision, this file, gate run |
| Host | f12665ffde | - | Plugin identity constants, examples and strings renamed |
| Host | db7e6a1734 | - | Editor menu "Open in 3-Block Compose designer", reload after hand-over |
| Documentation | 8538de3 | counter 102 | Designer guide page composeDesigner, compose link, sidebar / toc / changelog / progress |
| Offline Docs | 5f7bf9f | 132 | Site sync with the designer page, 10-language changelog |

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
AndroidX runtime ("accepted-by-maintainer-for-f5; build44 growth reported for confirmation"); the
new 1.2.0 baseline still needs the maintainer's confirmation under the section 14 rules.

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

## Known limitations recorded for 1.2.0

- The designer does not run scripts: events do not fire, state bindings show their initial value,
  opaque children and expressions are placeholders, open AlertDialog / ModalBottomSheet /
  DropdownMenu / Tooltip are inline cards, Image sources are host-resolved placeholders, AndroidView
  is a placeholder and not insertable, Snackbar is a session command and not in the palette.
- Canvas drops append to the target container; exact positions are chosen in the outline. Modifier
  chains are reordered with move buttons.
- TSX documents are not opened by the designer (JavaScript only in 1.2.0).
- Draft recovery after a killed process is covered by a pre-written draft file, not by a real kill.
  Export through the system document picker and the host editor hand-over were exercised by hand
  (adb / uiautomator), not by automated tests.
- The documentation site page and the Offline Docs sync describe the local preview; remote steps
  (pushing, releases, index registration) follow D7 and the maintainer's decision.
