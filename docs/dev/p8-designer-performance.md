# P8 designer performance baseline (1.2.0)

Collected on 2026-10-13 with `DesignerPerformanceDeviceTest` (opt-in, skipped unless the runner gets
`-e designerPerformance true`), plugin debug build of commit 0c632e9 (build 110 source tree) with the
androidTest APK of the same tree. The P5 rules in `AGENTS.md` section 14 cover rendering inside the host;
the designer runs in the plugin's own process and had no numbers before this file. No review line is set
yet: as with cold start and PSS in section 14, these values are recorded until more samples show how stable
they are, and a review line needs the maintainer's confirmation.

```powershell
adb -s <serial> shell am instrument -w -r -e designerPerformance true -e class io.github.supermonster003.autojs6.plugin.three.block.compose.designer.DesignerPerformanceDeviceTest io.github.supermonster003.autojs6.plugin.three.block.compose.test/androidx.test.runner.AndroidJUnitRunner
adb -s <serial> logcat -d -s DesignerPerformance
```

Each test logs one `DESIGNER_PERF {json}` line. On the two phones the first test's line was pushed out of
the main log buffer by the time it was read, so that test was rerun on its own (marked below).

## What is measured

- Pipeline: `ScriptReader.read`, `DesignValidator.validate`, `PreviewFrames.build` and `ScriptWriter.write`,
  timed with `System.nanoTime` around each call. "Gallery" is the sum over the 56 gallery examples in one
  round; "large" is one synthetic document of 251 design nodes (a Column of 50 rows, each with two texts and
  a labelled button). 3 warm-up and 10 measured rounds.
- Open: from `ActivityScenario.launch` of the editor until the canvas content and the first outline row are
  composed and the compose test reports idle; the largest gallery example (NavigationBar, 846 characters)
  and the 251-node document, 2 warm-up and 8 measured launches. The test process is already running, so
  this is a warm Activity start, not a cold process start.
- Edit: with the 251-node document open, the first row is deleted once; then undo (until the canvas shows
  the row's text again) and redo (until it is gone) are timed 10 times each.

Open and edit times include the compose test's idle synchronisation and semantics queries over the whole
editor (canvas, outline and inspector), so they are visibility-confirmation times, not pure frame times;
on a slow device a large part of them can be the queries themselves.

## Results (milliseconds, p50 / p90, n = 10 or 8)

| Measurement | AVD_API_24 x86 | AVD_API_30 x86_64 | Sony G8441, API 28 | Redmi 22120RN86C, API 33 |
| --- | --- | --- | --- | --- |
| Gallery read (56 scripts) | 15.8 / 23.6 | 47.6 / 50.6 | 24.4 / 35.5 | 33.5 / 40.3 |
| Gallery validate | 5.3 / 7.1 | 18.7 / 19.6 | 8.4 / 10.0 | 13.5 / 16.3 |
| Gallery preview frame | 7.7 / 10.3 | 25.1 / 28.0 | 12.0 / 13.5 | 17.6 / 20.4 |
| Gallery write | 6.2 / 7.5 | 11.0 / 12.5 | 8.6 / 9.4 | 13.6 / 15.6 |
| Large read (251 nodes) | 3.6 / 5.4 | 16.1 / 20.2 | 3.6 / 4.8 | 5.8 / 6.6 |
| Large validate | 2.9 / 3.6 | 11.7 / 13.2 | 3.6 / 4.6 | 5.4 / 6.5 |
| Large preview frame | 3.2 / 4.2 | 16.1 / 26.4 | 4.5 / 5.5 | 7.0 / 9.1 |
| Large write | 2.1 / 3.4 | 5.9 / 7.8 | 2.3 / 2.6 | 4.4 / 5.7 |
| Open NavigationBar example | 199.7 / 259.0 | 463.1 / 521.9 | 500.6 / 539.7 | 498.8 / 585.8 |
| Open 251-node document | 236.4 / 256.1 | 459.7 / 481.7 | 458.3 / 590.0 | 684.9 / 710.8 |
| Undo restores a row | 116.9 / 150.2 | 418.8 / 567.9 | 284.0 / 366.4 (rerun) | 794.5 / 980.2 (rerun) |
| Redo removes a row | 109.0 / 158.9 | 389.6 / 466.6 | 218.4 / 350.8 (rerun) | 693.2 / 867.1 (rerun) |

Environment: AVD_API_24 (Android 7.0, x86, 1 emulator running), AVD_API_30 (Android 11, x86_64, Google
APIs image; another session's emulator was running on the same PC), G8441 in portrait during these runs,
Redmi in portrait; both phones had AutoJs6 6.8.0 / 5352 built from host master `6e09d9abfd` installed, which
the designer does not use for these measurements.

## Reading the numbers

- Parsing, validation, preview derivation and writing stay small: each stage takes at most about 50 ms (p90)
  for all 56 examples together and at most about 26 ms for the 251-node document on every device, so they
  are not where editor latency goes.
- Opening and editing are dominated by composing the real-renderer canvas and the outline for the whole
  document, plus the test's own synchronisation. The Redmi (an entry-level phone) needs about 0.7 to 0.8 s
  from undo / redo to the canvas for the 251-node document, the slowest figure here; roadmap P9.1 carries an
  item to separate real frame time from test overhead (frame callbacks or `gfxinfo`) and, if the frame time
  is confirmed, to update the preview per node instead of rebuilding it.
- The AVD_API_30 figures are slower than the API 24 emulator for the pipeline; the PC was shared with another
  emulator during that run, so emulator numbers are only comparable within one run.

## P9.1 follow-up: real frame time of an edit (1.3.0)

The edit figures above are visibility-confirmation times. Roadmap P9.1 asked to separate the real frame time
from the test's own overhead before deciding whether the preview should update per node. Two things made the
earlier numbers hard to read:

- While a compose test rule is active, recomposition is driven by the test's clock: the editor composes only
  when the test synchronises or queries, so the rule-based test cannot observe frames on their own.
  `DesignerFramePerformanceDeviceTest` (opt-in like the other class) therefore has no compose rule: it calls
  undo / redo on the editor session on the main thread, sleeps without querying until the frames settle
  (1.5 s), and records the window's `FrameMetrics` plus the main thread's CPU time (`Debug.threadCpuTimeNanos`)
  over the same span; 2 warm-up and 10 measured rounds of undo and redo on the 251-node document.
- The debug build is debuggable, so ART runs much of the code in the interpreter; absolute numbers are higher
  than a release build would show, and the AVD shares its host with other emulators, which inflates wall times
  unevenly. Main thread CPU time is the steadier figure; wall times are only comparable within one run.

What the measurement found (AVD_API_30, simpleperf `--trace-offcpu` samples of the main thread):

- The largest part was the canvas recomposing every one of the ~250 nodes: in a 20 s profile of the edit
  loop before the change, `RenderNode` and its subtree took 288 ms of the 899 ms sampled under
  `Choreographer.doFrame`. The renderer called children positionally
  (`children.forEach { RenderNode(...) }`) with the `key(id, type)` inside `RenderNode`; that key only tells
  nodes apart at one call position, so a row inserted or removed at the top shifted every later child to
  another position and rebuilt its whole composition (fresh `LaunchedEffect`, modifiers, Material component
  state). The same happens in the host: a sibling inserted before a stateful node reset it
  (a LazyColumn's scroll position, for example). Fixed by keying each child call (`key(childId)`), covered by
  `AdvancedRendererTest.aSiblingInsertedBeforeANodeLeavesThatNodesStateInPlace`, which failed before the fix.
- The designer handed the renderer a new `RenderFrame` per preview, so even unmoved nodes could not skip.
  `LiveFrame` keeps one frame whose nodes live in per-node snapshot states and writes only nodes whose contract
  value changed; `RenderNode` (compiled `restartable skippable`) then skips every unchanged node.
- Once that was fixed, the next largest part (about 113 ms of 723 ms in a 30 s profile) was the canvas
  recording node bounds: each node's `onGloballyPositioned` wrote a `SnapshotStateMap`, which compares its
  whole content on every put, so one layout cost O(n^2). The bounds now
  live in a plain map with one version state.

Results, AVD_API_30 (x86_64), debug builds, 251-node document, p50 / p90 in milliseconds over 10 undo and
10 redo per run. Both builds were measured back to back with the same androidTest APK (`ae57054` sources, then
the change); the host PC ran four emulators at about 93 % CPU, so the absolute values are higher than in the
table above, while the comparison holds within this session. "Call" is the undo / redo call itself (history
step and validation), which the change does not touch.

| Measurement | Before, run 1 | Before, run 2 | After, run 1 | After, run 2 |
| --- | --- | --- | --- | --- |
| Undo: call to end of first frame | 365.4 / 531.2 | 376.2 / 860.9 | 139.8 / 172.1 | 153.8 / 185.5 |
| Undo: main thread CPU | 380.0 / 537.8 | 366.4 / 864.3 | 146.2 / 185.3 | 170.0 / 197.1 |
| Undo: longest frame | 355.1 / 518.3 | 327.1 / 837.2 | 131.1 / 170.1 | 152.9 / 176.9 |
| Redo: call to end of first frame | 301.2 / 437.5 | 351.7 / 563.6 | 137.6 / 160.8 | 129.5 / 153.4 |
| Redo: main thread CPU | 327.1 / 445.8 | 357.4 / 604.6 | 145.6 / 183.6 | 143.3 / 170.3 |
| Redo: longest frame | 292.1 / 435.6 | 350.3 / 556.5 | 114.5 / 157.6 | 118.6 / 147.8 |
| Call p50, undo / redo | 18.3 / 13.7 | 22.7 / 17.2 | 17.4 / 17.4 | 18.1 / 19.6 |

`RenderNode` executions per edit, counted with a temporary counter (not committed): 252 for an undo and 247 for
a redo before, 7 and 2 after (the root Column and, for an undo, the restored row with its texts, button and
label). Each edit draws two frames: the one that applies it and one that redraws the selection overlay after
the bounds moved.

What is left, from the same profile: the editor's own composition (outline rows in view, toolbar title),
deriving the preview (`PreviewFrames.build`, about 16 - 26 ms here), validation in the call, composing the
restored row, and measuring the 50-row Column. These scale with the document but not quadratically; the
roadmap does not take them further for 1.3.0. The phones of the 1.2.0 baseline were not measured again in
this session.
