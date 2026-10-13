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
