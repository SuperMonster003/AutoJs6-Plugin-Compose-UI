# P9.2 TSX projects, 1.4.0 evidence

Roadmap D42 (decided 2026-10-13, Q13): the syntax tree of a TSX project comes from the TypeScript Engine
through a separate parser contract in the host `plugin-api`; the designer maps the tree to its existing
`DesignDocument`, writes TSX itself, and opens a TSX project read-only when no usable parser is installed.
This file collects the 1.4.0 evidence across the three repositories; each roadmap item keeps its own detailed
evidence line in `ROADMAP.md`. 1.4.0 is a local development preview, nothing is pushed.

## Commits

| Repository | Commit | Change |
| --- | --- | --- |
| AutoJs6 (host) | `fce22c83a6` | `plugin-api/typescript-parser-api`: `ITypeScriptParserPlugin`, `TypeScriptParserContract` v1 (hash `6c8a1a80...`, tree schema revision 1), contract test, protocol document `docs/dev/typescript-parser-plugin-protocol-v1.md`; the host app does not package the module |
| TypeScript Engine | `1e92487` | 0.7.0 / build 123: parser service `org.autojs.plugin.typescript.PARSER` in the `:typescript_compiler` process, its own page `typescript-parser-r1` and Worker, two concurrent parses, 15 s default timeout, idle reclaim |
| 3-Block Compose | `85d518c` (131), `dab591b` (132) | 1.4.0: parser client, `TsxReader`, `TsxWriter`, `.tsx` projects, run by Uri, read-only fallback, tests and snapshots, changelog; README in 10 languages; this evidence and the roadmap follow in build 133 |
| AutoJs6-Documentation | `bdf76d6`, `2908e2e`, `759a3ae` | designer guide "TSX projects" section, regenerated pages, counter 106 |
| Offline Docs | `fc82a6b` | build 135, provenance `bdf76d6`, `--verify-offline` passed |

## Contract artifact

`libs/typescript-parser-api.aar` is the release AAR of the host module at `fce22c83a6`; a rebuild from the same
commit is byte-identical (SHA-256 `c5651a0c779903bde8832af185bc63038d1706d32f3e67069fbcf0e536658519`, 11516 B,
classes: the AIDL interface with its stub, proxy and default, `TypeScriptParserContract` with its `Tree`, `Node`
and `Diagnostic` member objects). It is locked with the other host AARs and consumed with `implementation`.

## Mapping and output, JVM

Snapshots: `app/src/test/resources/tsx/` holds the TSX sources and, for each, the tree the TypeScript Engine
parser Worker (`parser-worker.js` of `1e92487` with the locked TypeScript 6.0.3) produced under Node, written by
`.python/generate_tsx_snapshots.py`; `SNAPSHOTS.txt` ties each tree to the SHA-256 of its source. 101 trees,
642 KB in total (trees gzipped, 396 KB).

| Test | Result |
| --- | --- |
| 56 gallery scripts in TSX form | each maps to exactly the document of its JavaScript script and is a fixed point of the writer |
| 5 bundled examples in TSX form | each maps to the document of its JavaScript script |
| New project template | canonical TSX, same interface as the JavaScript template |
| Host compose TSX samples (10) | counter, dialog, floaty, fragment-boundaries, memo, mixed-render, wide-components map, their canonical form reads back equal and writes byte-identical; interop (`compose.attach`), legacy-xml (no compose mount) and non-ui (mount inside `try`) are refused with "no compose.mount" |
| Hand-written cases | JSX text folding and entities (`&amp;`, `&copy;`, `&#169;`, `&#x41;`, unknown `&unknown;` kept), adjacent text children, attribute strings and backslashes, number spellings (`-2`, `1e3`, `0x10`), bare attributes, slots as string / element / single-element fragment / empty fragment / `slots`, `children` attribute, typed handler parameters, modifier chains, type arguments of `compose.state`, alias tags, conditional / loop / comment / spread children, generic / async / named / typed-return handlers kept as expressions |
| Error cases (16) | syntax error, no mount, two mounts, no return, render parameters, generic render function, typed mount variable, spread options, non-compose root, root fragment with two elements, XML tag, reserved tag, spread attribute, namespaced attribute, children twice, out-of-range entity; each with its line and column |

Whole JVM suite: 154 tests, 0 failures.

## Devices

| Device | Setup | Result |
| --- | --- | --- |
| Xiaomi Pad 23046RP50C (968e9f18), API 35, WebView 130 | host 6.8.0 / 5352, TypeScript Engine 0.7.0 release (`1e92487`), plugin 1.4.0 debug | `DesignerTsxDeviceTest`: new TSX project, palette insert, key edit, a handler with TypeScript syntax checked by the parser, save, reopen and save again byte-identical; a handler with a syntax error refused at `1:17 Expression expected.`; run intent by content Uri. First open after installing TypeScript Engine 5653 ms (parser page and TypeScript load), later opens 755 - 788 ms |
| same | designer menu "Run in AutoJs6" on a TSX project | AutoJs6 compiles it with TypeScript Engine and shows it; the button's handler updates the state (`TSX e2e 2` after two taps) |
| Redmi 22120RN86C (bek749scrwv4wo8h), API 33 | TypeScript Engine 0.6.8 (no parser service) | `withoutTheParserATsxProjectOpensReadOnly`: "needs TypeScript Engine 0.7.0 or later; version 0.6.8 is installed", source shown, nothing written |
| AVD_API_30 (emulator-5560), API 30, WebView 83 | TypeScript Engine debug build with the parser (versionCode 121) | full instrumentation including the TSX edit path, see the gate |

The full suite on the Xiaomi Pad: 115 tests, 6 failures before a fix. `DesignerFramePerformanceDeviceTest`
reflected on the field `session`, which became a state delegate; it now uses `DesignerActivity.currentSession`
and passes. The other five (`DesignerHostDeviceTest` export / external document / read-only save, one
`DesignerSourceSyncDeviceTest` step) look up snackbar text through the test context, which follows the system
language (Chinese), while the designer follows the AutoJs6 language (English on that pad): they are
environment-dependent and pass on the AVD, where both are English.

## Size and method references against the review lines

| Build | Release APK | DEX method references | Note |
| --- | --- | --- | --- |
| 1.2.0 gate build 106 (baseline) | 5094361 B | 87629 | review lines 5603798 B / 96392 |
| 1.3.0 gate, build 129 sources | 5167317 B | 88223 | |
| 1.4.0 gate | 5219321 B | 88672 | `verify_apk_classpath.py --shrunk`, 14546 classes, 230 contract classes, 0 unresolved |

1.4.0 adds 52004 B and 449 method references over 1.3.0 and stays 6.9 % and 8.0 % below the review lines.

## Local gate (AGENTS.md section 14)

All on 2026-10-13 against the 1.4.0 sources of `85d518c` (the later commits change documentation only), host PC shared with other emulators.

| Check | Result |
| --- | --- |
| `generate_markdown.py --check`, `generate_icon_studio.py --check` | 36 artifacts, 24 Icon Studio resources |
| JVM `testDebugUnitTest` | 154 tests, 0 failures |
| `assembleDebug assembleDebugAndroidTest lintDebug assembleRelease verifySharedClasspath` | pass; lint reports only warnings present before 1.4.0 (a byte-order mark error from a literal U+FEFF in `JsxText.kt` was fixed by writing the escape) |
| `verify_apk_classpath.py` debug / `--shrunk` release | pass; numbers in the size table |
| Instrumentation, AVD_API_30 (`am instrument`, all classes) | 115 tests pass (designer performance classes skip without their argument); the TSX edit and handler-error cases ran there too, because the AVD has a TypeScript Engine debug build (versionCode 121, still named 0.6.8) made from the parser change before its version bump; TSX open 1361 ms |
| `verify_standalone_launch.py`, release APK, AVD_API_30 | `STANDALONE_LAUNCH_OK` |
| `verify_designer_recovery.py`, release APK, AVD_API_30 | `DESIGNER_RECOVERY_OK` |
| `verify_host_handoff.py`, release APK, AVD_API_30 | `HOST_HANDOFF_OK` (host 5352) |

Not run for this version: the host-side compose device matrix (1.4.0 changes nothing that renders inside
AutoJs6), `appendDigestToReleasedFiles`, `verify_source_sync.py` (source sync is unchanged), remote CI (nothing
is pushed), and the TypeScript Engine parser on API 24 / 25 (their AVD WebView, about 53, is below the
TypeScript Engine minimum of 119).

## Known limitations recorded for 1.4.0

- The first open after TypeScript Engine starts its parser page takes a few seconds; the page is created on the
  first request and reclaimed after 60 s idle, not pre-warmed with the compiler page.
- Only JSX trees map: a TSX root written as `compose.Column(...)` factory calls is refused; factory calls inside
  JSX stay opaque children.
- Canonical output rewrites the interface part of hand-written TSX (attribute quotes, `compose:X` tags,
  fragments around a single element, the name of a named render function, as the JavaScript writer drops it).
- Handlers with type parameters, a return type, `async` or a name, and variables with a type annotation, are kept
  verbatim but cannot be edited as handlers or states.
- The host editor menu shows "Open in 3-Block Compose designer" for any file when the plugin is 1.2.0 or later;
  with 1.2.0 / 1.3.0 a `.tsx` file opens to the "cannot be opened" screen. Whether the host should require 1.4.0
  for `.tsx` is left to the maintainer.
