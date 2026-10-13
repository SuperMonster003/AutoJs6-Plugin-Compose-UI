# P9 designer enhancements, 1.3.0 evidence

Roadmap D41 splits P9 into 1.3.0 (P9.1 canvas and preview precision, P9.3 step 1 preview state panel),
1.4.0 (P9.2 TSX projects) and a later, separately decided 1.5.0 (P9.3 step 2, sandboxed execution). This file
collects the 1.3.0 evidence; each roadmap item keeps its own detailed evidence line in `ROADMAP.md`. The
1.2.0 release baseline is `6db6da2` (build 118); 1.3.0 is a local development preview, nothing is pushed.

## Commits

| Commit | Build | Change |
| --- | --- | --- |
| `4a9994d` | 119 | Canvas drops land where the pointer is, with an insertion line along the container's layout |
| `f599247` | 120 | AndroidView stand-in keeps its size modifiers and shows the `view` source |
| `69e77db` | 121 | Imported projects keep access to their source; manual write back and re-import |
| `e8a812c` | 122 | Image previews from `@drawable` and a per-project image folder |
| `2d63b6e` | 123 | Modifier chain reordering by dragging a handle |
| `ae57054` | 124 | Open AlertDialog / ModalBottomSheet / DropdownMenu / Tooltip drawn inside the canvas |
| `0ec4998` | 125 | Renderer: child calls keyed by node id (also affects the host, see below) |
| `1e1dba0` | 126 | Designer canvas recomposes only changed nodes; node bounds without a state map |
| `527a6bd` | 127 | `verify_source_sync.py` on a real picker; one-step undo of a modifier drag |
| `383f6bb` | 128 | State panel, interactive preview, canvas text fields follow their declared text |
| `a3b78d5` | 129 | README in 10 languages |

Documentation site `1e8894d` (designer guide for 1.3.0, new section on the preview modes) and `6aa273c`
(counter 105); Offline Docs `ed59755` (build 134, provenance `1e8894d`).

## Renderer change that reaches the host

`0ec4998` is the only change in this version that alters rendering inside AutoJs6. `RenderNode` rendered
children positionally while its own `key(id, type)` sat inside the callee, so a sibling inserted or removed
before a node shifted every later child to another call position and rebuilt its composition, losing
remembered state such as a LazyColumn's scroll position. Each child call is now wrapped in `key(childId)`
(RenderNode's Column / Row / Box / content and WideComponents' content / tabs / menu / segmented buttons).
`AdvancedRendererTest.aSiblingInsertedBeforeANodeLeavesThatNodesStateInPlace` failed before the change and
passes after it; the plugin's full instrumentation (renderer classes included) passes. The host-side compose
device matrix was not rerun for this version.

## Size and method references against the review lines

| Build | Release APK | DEX method references | Note |
| --- | --- | --- | --- |
| 1.2.0 gate build 106 (baseline) | 5094361 B | 87629 | review lines 5603798 B / 96392 |
| 1.3.0 build 126 | 5155845 B | - | release built for the real-device source sync check |
| 1.3.0 gate, build 129 sources | 5167317 B | 88223 | `verify_apk_classpath.py --shrunk`, 14461 classes, 230 contract classes, 0 unresolved |

The 1.3.0 gate build grows by 72956 B and 594 method references over the 1.2.0 baseline and stays under both
review lines (7.8 % and 8.5 % below them).

## Local gate (AGENTS.md section 14)

All on 2026-10-13 against the build 129 sources (`a3b78d5`), host PC shared with other emulators.

| Check | Result |
| --- | --- |
| `generate_markdown.py --check`, `generate_icon_studio.py --check` | 36 artifacts, 24 Icon Studio resources |
| JVM `testDebugUnitTest` | 138 tests, 0 failures |
| `assembleDebug assembleDebugAndroidTest lintDebug assembleRelease verifySharedClasspath` | pass; lint reports only the warnings present before 1.3.0 |
| `verify_apk_classpath.py` debug / `--shrunk` release | pass; numbers in the size table |
| Instrumentation, AVD_API_30 (`am instrument`, all classes) | 111 tests pass (designer performance classes skip without their argument) |
| `verify_standalone_launch.py`, release APK, AVD_API_30 | `STANDALONE_LAUNCH_OK` (launcher alias, gallery entry, settings, new project on the canvas) |
| `verify_designer_recovery.py`, release APK, AVD_API_30 | `DESIGNER_RECOVERY_OK` (process killed and restarted, draft restored) |
| `verify_host_handoff.py`, release APK, AVD_API_30 | `HOST_HANDOFF_OK` (AutoJs6 editor -> designer -> save -> editor reload) |
| `verify_source_sync.py`, release APK (build 126 sources), Redmi 22120RN86C API 33 | `SOURCE_SYNC_OK` twice (real picker, persisted grant across a process restart) |

Not run for this version: the host-side compose device matrix (spike host + host androidTest), the release
signing step `appendDigestToReleasedFiles`, remote CI (nothing is pushed), and the other real devices of the
pool.

## Performance

`docs/dev/p8-designer-performance.md`, section "P9.1 follow-up": real frame times of an edit on the 251-node
document, the A/B on AVD_API_30 (undo / redo to the first frame p50 about 365 / 300 ms before, 140 / 130 ms
after; main thread CPU down 55 - 60 %), `RenderNode` executions per edit 252 / 247 before and 7 / 2 after, and
the Redmi 22120RN86C visibility times against the 1.2.0 baseline (undo 794.5 -> 305.6 ms p50, redo 693.2 ->
256.5 ms p50).

## Known limitations recorded for 1.3.0

- The interactive preview runs no script: event handlers, render functions and expressions are not evaluated;
  only a control writing the new value of a property bound to a state with `x.value` changes a preview value.
- Popups drawn in the canvas do not close on a scrim tap or back press; a menu takes the available width
  between 112 and 280 dp instead of measuring its content (intrinsic measurement throws on SubcomposeLayout).
- Image previews do not load URLs, absolute paths or expressions; AndroidView stays a stand-in and cannot be
  inserted from the palette.
- A source replaced by another file (for example `adb push` over it) gets a new media store entry, so the
  picked Uri stops working and the designer asks to pick the file again.
- Remaining edit cost on large documents (the editor's own composition, preview derivation, validation,
  measuring) grows linearly with the document; no further work is planned for 1.3.0.
