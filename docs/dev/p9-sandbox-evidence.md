# P9.3 step 2, running scripts in the interactive preview, 1.5.0 evidence

Roadmap D43 (step 2 opened by the maintainer on 2026-10-13 after 1.4.0, "as planned in the roadmap"; D8 and D35
revised): while the interactive preview is on, the designer runs a JavaScript project's script inside the
plugin with Rhino's interpreter, under limits, with a preview implementation of `compose`, and draws only what
the script mounts on the canvas. This file collects the 1.5.0 evidence; the roadmap item keeps a summary.

## Design

| Part | Where | What |
| --- | --- | --- |
| Engine | `designer/sandbox/SandboxEngine.kt` | One Rhino `Context` per entry (load, one event, one round of due timers) from its own `ContextFactory`: interpreted mode, language version ES6, `initSafeStandardObjects()` (no LiveConnect), a `ClassShutter` that hides every class, an instruction observer every 10 000 instructions that stops the entry past 200 000 000 instructions or 3 000 ms, at most 1 000 interpreter frames. The limit throws an `Error`, so neither `catch` nor `finally` of the script runs; the engine is then stopped for good. A script error (`RhinoException`) is reported with the line of the user's script (the innermost frame outside the runtime) and the sandbox carries on |
| Bridge | same | Strings only: the catalog goes in as the JSON text `__catalogJson`, Kotlin evaluates `__sandbox.flush()` / `dispatch(...)` / `runTimers()` / `drain()` and reads JSON back; the script never sees a Java object |
| Runtime | `app/src/main/resources/designer-sandbox/preview-runtime.js` | `compose` / `$compose` with the host's visible rules: factories with props / shorthand children / `children` / `slots` / `key` / `ref` / `modifier` / `onXxx`, catalog checks with the host's error codes, immutable modifier chains with callback arguments, `state` with same-value writes ignored and changes coalesced into one render, reconciliation that gives the drafts of a render the identities of the nodes they replace (by key, else by type and position, reserved and used identities not taken twice), node handles with property proxies for catalog properties, aliases, shortcuts and `modifier`, `set` / `get` (defaults) / `append` / `insert` / `remove` / `replace` / `clear` / `slot` / `on` / `off` / `focus` / `blur` / `scrollTo` / `edit` (the host's command rules), field editors fed by `valueChange` with the edit sequence, refs set when a tree is drawn, `batch`, `memo` (recomputed), `post`, `theme`, `sessions`, `version`, timers (64 pending, repeat >= 16 ms), console / `log` / `print` / `toast` / `toastLog` / `showSnackbar` into a 500-line buffer. Other AutoJs6 names (`app`, `ui`, `files`, `java`, `Packages`, `require`, `sleep`, ...) are plain writable globals holding proxies that throw "... is not available in the designer preview" when used, so a script may still declare its own `colors` or `ui` |
| Thread | `designer/sandbox/PreviewSandbox.kt` | One single-thread scheduled executor per run, 8 MB stack (callbacks from Java built-ins nest Java frames), timers scheduled from `nextTimer()`; a fault of the sandbox itself is delivered as an `INTERNAL` failure instead of being swallowed by the executor |
| Canvas | `designer/sandbox/SandboxTrees.kt`, `preview/PreviewFrames.kt` (`LiveHooks`, `LiveTree`), `preview/DesignCanvas.kt` | The runtime's tree becomes a throwaway `DesignDocument` whose node ids are the runtime's ids, drawn by the same frame builder; callbacks are the events that have listeners, `clickable` callbacks carry tokens from 2 000 000 up, field text goes through an `UiCommand.Edit` with the sequence the runtime last saw (refused when the user typed since, as in the host), focus / blur / scrollTo run after the frame is drawn. Event payloads become handler arguments as in the host (`SandboxEvents`) |
| Designer | `designer/sandbox/SandboxPreview.kt`, `ui/DesignerActivity.kt`, `ui/ConsolePanel.kt` | The script is the document as it would be saved, with the State panel's preview values as initial values; it restarts 400 ms after the document or a preview value changes, stops while the designer is hidden and when Interact is turned off. Until the first tree arrives the canvas shows the document and ignores input. The Interact switch shows a running icon, a script error or stop appears at the bottom of the canvas and in the issues list, the new Console tab holds the output (1 000 lines). TSX projects keep step 1 |
| Focus | `preview/DesignCanvas.kt` | In edit mode no canvas component may take focus (a focus group whose `onEnter` cancels); leaving the interactive preview clears focus. API 24 - 27 give a window that gains focus outside touch mode to its first focusable view, which put a canvas text field in focus and raised the keyboard over the panels |

Android 8.0: Rhino's `NativeDate` is built on `java.time` (API 26); on API 24 the first `initSafeStandardObjects()`
failed with `NoClassDefFoundError: java.time.format.DateTimeFormatter` (seen on the API 24 AVD). Core library
desugaring would also rewrite the `java.time` references of the Compose classes that the renderer loads inside
AutoJs6, so the sandbox is enabled from API 26 (`SandboxPreview.SUPPORTED`) and older devices keep step 1.

R8: Rhino finds its evaluator and its platform bridge by class name and constructs them reflectively, so
`proguard-rules.pro` keeps `org.mozilla.javascript.Interpreter` and `org.mozilla.javascript.jdk18.VMBridge_jdk18`
(no-argument constructors); RegExp arrives through `ServiceLoader`, which R8 follows. Everything else of the
interpreter is kept by reference.

## JVM

`SandboxEngineTest` (14):

| Test | Result |
| --- | --- |
| Java is out of reach | `java.lang.System.exit(0)`, `Packages.java.io.File`, `importClass(java.io.File)`, `new JavaAdapter(...)`, `this.constructor.constructor('return java')().lang`, `Function('return Packages')().java`, `org.mozilla.javascript.Context...`, `android.os.Process...` all fail as script errors; every Java-shaped global that exists throws when used |
| Module names stay declarable | `var colors`, `function ui`, `let files` work |
| Unavailable module | `app.launch(...)`: "app is not available in the designer preview", line 1 of the script |
| Endless loop, clock | stopped after 300 ms limit (well under 3 s), neither `catch` nor `finally` ran, the line logged before survives, later calls do nothing |
| Endless loop, instructions | stopped by the instruction budget |
| Endless loop in a handler | stopped, sandbox stopped |
| Deep recursion | a script error the sandbox survives |
| Console and timers | lines with levels, `setTimeout` re-renders when due, no timer pending afterwards |
| Handler error | reported with its line; the state write before it still renders; later events work |
| Counter example | `Count: 0` -> click -> `Count: 1`, every node keeps its id and type across the render, reset publishes `Count: 0`, a click that changes nothing publishes nothing |
| Form example | empty save marks the field and focuses it (`UiCommand.Focus`), typing reaches the handler as (text, selection, editSeq) and the field keeps text and sequence, switch and slider handlers update handles, save writes the summary and blurs the field |
| List example | 1000 keyed items reverse with every keyed node keeping its id, the ref holds the list once drawn and `scrollTo(99)` becomes a command |
| Modifier callbacks | `clickable` gets a token >= 2 000 000 that runs the callback |
| Compose errors | `UNKNOWN_COMPONENT`, `UNKNOWN_PROP`, `INVALID_ARGUMENT` (key, memo outside render, self-append), `INVALID_MODIFIER` |

`RhinoImportGuardTest` (3): only `designer/parse` and `designer/sandbox` import Rhino, only the sandbox enters a
Context, the sandbox uses safe standard objects, the class shutter, interpreted mode and the limits.

## Devices

| Device | Run | Result |
| --- | --- | --- |
| AVD_API_30 (emulator-5560, 1080 x 2400) | `DesignerSandboxDeviceTest` 3, `DesignerPreviewStateDeviceTest` 1, `TextFieldRendererTest` 9 (final build) | passed |
| AVD_API_30 | full `connectedDebugAndroidTest` (100, 5 skipped) | 95 passed; the 4 sandbox / state failures looked the running icon up in the merged semantics tree, where the `FilterChip` swallows the tag of its leading icon (fixed: `useUnmergedTree = true`, passed in the row above); `TextFieldRendererTest.inputConnection...` failed because `system_server` died in the emulator's host clipboard bridge (`ClipboardService.HostClipboardMonitor`, "beginBroadcast() called while already in a broadcast") during the test's paste, passed in the row above |
| AVD_API_24 read-only instance (emulator-5570, Nexus 5 size 1080 x 1920 / 480 dpi, as the CI) | `DesignerPreviewStateDeviceTest` 1 (step 1 branch), `DesignerSandboxDeviceTest` skipped by assumption (final build) | passed |
| AVD_API_24 read-only instance | full suite through `am instrument` (119) | 117 passed; `AdvancedRendererTest.scaffoldSlotsProgressAndControlledDialogUseTheCurrentFrame` and `WideRendererTest.pullToRefreshUsesActualNestedScrollAndHonorsDisabledState` end in `AppNotIdleException` (the main looper keeps drawing frames for 60 s). Not a regression: the debug and test APKs built from `18f5861` (the last commit whose CI was green, including both tests on its API 24 emulator in 13.6 s / 9.7 s) fail the same two tests on this instance, with animations on and off. Neither test touches the designer |

Release APK checks on AVD_API_30: `verify_standalone_launch.py` (`settings=opened designer=opened sandbox=ran`), `verify_designer_recovery.py` (`reopened=editor inserted=Button`; the first attempt gave up scrolling the palette to `Button`, the second passed). `verify_host_handoff.py` was not run (no change on the host side or in the handoff).

## Release build

The R8 release APK on AVD_API_30: `verify_standalone_launch.py` now also turns on the interactive preview of the
new project and waits for the running icon (`sandbox=ran`). A script read through the designer's document entry
(`content://media/external/downloads/...`) computed, inside the R8 sandbox, exactly what Node computes:

```text
a+b+c|12|{"a":[1,2],"b":"x"}|2020-02-03T00:00:00.000Z|9|123|v|2|true|symbol|**ab|p,q|255|a%20b|1000.0
```

(regular expressions with flags and groups, JSON, `Date` with `toISOString`, `Math`, `sort`, `Map`, `Set`,
`Array.from`, a caught `TypeError`, `Symbol`, `padStart`, `Object.keys`, `parseInt`, `encodeURIComponent`,
`toFixed`). The test script and its media entry were removed afterwards.

## Gate (build 139)

`generate_markdown.py --check`, `generate_icon_studio.py --check`, `:app:testDebugUnitTest` (173; the TSX snapshot recorder is skipped as
always), `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:assembleRelease
:app:verifySharedClasspath` (lint reports only the issues present before 1.5.0), `verify_apk_classpath.py` on the
debug APK (14314148 B, 144454 method references) and with `--shrunk` on the release APK (`unresolved=0`).

## Size

| | 1.4.0 release (build 136) | 1.5.0 release (build 139) | Review line (AGENTS section 14) |
| --- | --- | --- | --- |
| APK | 5221337 B | 5615650 B | 5603798 B (over by 11852 B, 0.21 %) |
| DEX method references | | 96185 | 96392 (under) |

Growth by APK entry (compressed): `classes2.dex` +351684 B (Rhino's interpreter, which R8 removed while only the
parser was used, and the sandbox), `resources.arsc` +13404 B (the console and sandbox strings in 11 languages),
`designer-sandbox/preview-runtime.js` +9193 B, `classes.dex` +1676 B, the 1.5.0 changelog in the bundled
documents about +7 KB. Q8 does not block below 10 MiB.

The maintainer accepted the growth on 2026-10-14, after the release. The 1.5.0 release APK (build 139, `8bc620a`,
`v1.5.0`) is the new baseline; with the rule of AGENTS section 14 (measured value plus 10 %, rounded up) the review
lines are now 6177215 B and 105804 method references.
