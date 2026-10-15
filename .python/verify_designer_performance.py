"""Run the designer performance tests on a device and compare them with the review lines (roadmap P11.1, D54).

The two opt-in test classes (`DesignerPerformanceDeviceTest`, `DesignerFramePerformanceDeviceTest`) log one
`DESIGNER_PERF {json}` line per measurement kind (`pipeline`, `open`, `edit`, `frames`) to logcat under the
`DesignerPerformance` tag. This check installs the debug and androidTest APKs, runs the classes one at a time
with a 16 MB log buffer, reads the lines back (a kind that was pushed out of the buffer is rerun on its own),
and compares every p90 with `docs/dev/designer-performance-lines.json`. The lines are defined for one phone
(`device_model` in the file): on that phone any value over its line fails the check; on any other device the
run is recorded only (`RECORD_ONLY`). `--record` appends the round to `docs/dev/p8-designer-performance.md`
for review before it is committed. `--restore` installs a release APK over the debug build afterwards (with
`-d`, since the release may carry a lower build number) and removes the test package, so a phone that holds
the released plugin ends the way it started.

usage: verify_designer_performance.py --apk <debug apk> --test-apk <androidTest apk> [--serial <serial>]
           [--lines <json>] [--record] [--restore <release apk>] [--doc <markdown>]
"""
import argparse
import datetime
import json
import math
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_standalone_launch as launch  # noqa: E402

PACKAGE = launch.PACKAGE
RUNNER = f"{PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner"
TAG = "DesignerPerformance"
# Test class -> the kinds it reports, each with the method that produces it (for a rerun of a lost line).
CLASSES = {
    f"{PACKAGE}.designer.DesignerPerformanceDeviceTest": {
        "pipeline": "pipelineOverGalleryExamplesAndALargeDocument",
        "open": "editorOpensTheLargestExampleAndTheLargeDocument",
        "edit": "undoAndRedoOfARowDeletionReachTheCanvas",
    },
    f"{PACKAGE}.designer.DesignerFramePerformanceDeviceTest": {
        "frames": "undoAndRedoFramesOfTheLargeDocument",
    },
}
VALUE = re.compile(r"p50=([\d.]+) p90=([\d.]+) max=([\d.]+) n=(\d+)")
DEFAULT_LINES = Path(__file__).resolve().parent.parent / "docs/dev/designer-performance-lines.json"
DEFAULT_DOC = Path(__file__).resolve().parent.parent / "docs/dev/p8-designer-performance.md"


def parse_perf_lines(text: str) -> dict[str, dict[str, object]]:
    """The `DESIGNER_PERF` records of a logcat dump, by kind; measured keys map to (p50, p90, max, n)."""
    records: dict[str, dict[str, object]] = {}
    for line in text.splitlines():
        marker = line.find("DESIGNER_PERF ")
        if marker < 0:
            continue
        try:
            fields = json.loads(line[marker + len("DESIGNER_PERF "):].strip())
        except json.JSONDecodeError:
            continue
        kind = fields.get("kind")
        if not kind:
            continue
        record: dict[str, object] = {"device": fields.get("device", ""), "api": fields.get("api", ""), "values": {}}
        for key, value in fields.items():
            match = VALUE.match(str(value))
            if match:
                record["values"][key] = (float(match.group(1)), float(match.group(2)), float(match.group(3)), int(match.group(4)))
        records[kind] = record
    return records


def load_lines(path: Path) -> dict[str, object]:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def compare(measured: dict[str, tuple[float, float, float, int]], lines: list[dict[str, object]], judge: bool) -> tuple[list[tuple[str, str, float | None, float]], bool]:
    """One (status, key, p90, line) per review line: PASS / OVER (judged), RECORD (not judged) or MISSING; and whether all passed."""
    rows = []
    ok = True
    for entry in lines:
        key = str(entry["key"])
        line = float(entry["line_ms"])
        value = measured.get(key)
        if value is None:
            rows.append(("MISSING", key, None, line))
            ok = False
            continue
        p90 = value[1]
        if not judge:
            rows.append(("RECORD", key, p90, line))
        elif p90 > line:
            rows.append(("OVER", key, p90, line))
            ok = False
        else:
            rows.append(("PASS", key, p90, line))
    return rows, ok


def record_table(lines: list[dict[str, object]], measured: dict[str, tuple[float, float, float, int]]) -> str:
    """A Markdown table of p50 / p90 for the review-line keys, in the document's format."""
    out = ["| Measurement | p50 / p90 (ms) | Review line |", "| --- | --- | --- |"]
    for entry in lines:
        value = measured.get(str(entry["key"]))
        cell = "not measured" if value is None else f"{value[0]:.1f} / {value[1]:.1f}"
        out.append(f"| {entry['title']} | {cell} | {int(entry['line_ms'])} ms |")
    return "\n".join(out)


def instrument(adb: launch.Adb, test_class: str, method: str | None = None) -> str:
    target = test_class if method is None else f"{test_class}#{method}"
    output = adb.shell(f"am instrument -w -r -e designerPerformance true -e class {target} {RUNNER}", check=False, timeout=1800)
    if "INSTRUMENTATION_STATUS_CODE: -2" in output or "INSTRUMENTATION_STATUS_CODE: -1" in output or "Process crashed" in output:
        stack = next((line for line in output.splitlines() if "INSTRUMENTATION_STATUS: stack=" in line), "")
        raise SystemExit(f"{target} failed: {stack[:400]}")
    if "INSTRUMENTATION_STATUS_CODE: -4" in output:
        raise SystemExit(f"{target} was skipped: the runner did not receive -e designerPerformance true")
    if "OK (" not in output:
        raise SystemExit(f"{target} did not finish:\n{output[-800:]}")
    return output


def collect(adb: launch.Adb) -> dict[str, dict[str, object]]:
    records: dict[str, dict[str, object]] = {}
    adb.shell("logcat -G 16M", check=False)
    for test_class, kinds in CLASSES.items():
        adb.run("logcat", "-c", check=False)
        instrument(adb, test_class)
        records.update(parse_perf_lines(adb.run("logcat", "-d", "-s", TAG, check=False)))
        for kind, method in kinds.items():
            if kind in records:
                continue
            # The line left the buffer before it was read (seen on phones with a small buffer): run that test alone.
            adb.run("logcat", "-c", check=False)
            instrument(adb, test_class, method)
            records.update(parse_perf_lines(adb.run("logcat", "-d", "-s", TAG, check=False)))
            if kind not in records:
                raise SystemExit(f"no DESIGNER_PERF line of kind {kind} after rerunning {method}")
    return records


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--apk", type=Path, required=True, help="the plugin debug APK (the tests need unobfuscated classes)")
    parser.add_argument("--test-apk", type=Path, required=True, help="the plugin androidTest APK of the same tree")
    parser.add_argument("--serial")
    parser.add_argument("--lines", type=Path, default=DEFAULT_LINES)
    parser.add_argument("--record", action="store_true", help="append this round to the performance document")
    parser.add_argument("--doc", type=Path, default=DEFAULT_DOC)
    parser.add_argument("--restore", type=Path, help="a release APK to install afterwards (adb install -r -d), with the test package removed")
    args = parser.parse_args()

    lines_file = load_lines(args.lines)
    lines = list(lines_file["lines"])
    adb = launch.Adb(args.serial)
    model = adb.shell("getprop ro.product.model").strip()
    api = adb.shell("getprop ro.build.version.sdk").strip()
    judge = model == lines_file["device_model"]
    for apk in (args.apk, args.test_apk):
        install = adb.install(apk)
        if "Success" not in install:
            raise SystemExit(f"install of {apk.name} failed:\n{install}")
    build = adb.shell(f"dumpsys package {PACKAGE}", check=False)
    version = re.search(r"versionCode=(\d+)", build)
    version_code = version.group(1) if version else "?"
    adb.shell(f"am force-stop {PACKAGE}", check=False)
    adb.shell("input keyevent KEYCODE_WAKEUP", check=False)
    adb.shell("wm dismiss-keyguard", check=False)
    try:
        records = collect(adb)
    finally:
        adb.shell(f"am force-stop {PACKAGE}", check=False)
        if args.restore is not None:
            restored = adb.run("install", "-r", "-d", str(args.restore), check=False, timeout=launch.INSTALL_TIMEOUT_S)
            if "Success" not in restored:
                print(f"restore of {args.restore.name} failed:\n{restored}", file=sys.stderr)
            adb.run("uninstall", f"{PACKAGE}.test", check=False)

    measured: dict[str, tuple[float, float, float, int]] = {}
    for record in records.values():
        measured.update(record["values"])  # type: ignore[arg-type]
    rows, ok = compare(measured, lines, judge)
    for status, key, p90, line in rows:
        value = "-" if p90 is None else f"{p90:.1f}"
        print(f"{status:7} {key:22} p90={value:>8} line={line:.0f}")
    if args.record:
        stamp = datetime.date.today().isoformat()
        heading = f"## Round {stamp} ({model}, API {api}, build {version_code}, {'judged' if judge else 'recorded only'})"
        table = record_table(lines, measured)
        with args.doc.open("a", encoding="utf-8", newline="") as handle:
            handle.write("\n" + heading + "\n\nAppended by `.python/verify_designer_performance.py`; review before committing.\n\n" + table + "\n")
        print(f"recorded in {args.doc}")
    device = f"device={model} api={api} build={version_code} judged={'yes' if judge else 'no'}"
    if not judge:
        print(f"DESIGNER_PERF_RECORD_ONLY {device} (lines are defined for {lines_file['device_model']})")
        return
    if ok:
        print(f"DESIGNER_PERF_OK {device} lines={len(lines)}")
    else:
        over = [key for status, key, _, _ in rows if status != "PASS"]
        raise SystemExit(f"DESIGNER_PERF_OVER {device} over_or_missing={','.join(over)}")


if __name__ == "__main__":
    try:
        main()
    except subprocess.TimeoutExpired as error:
        sys.exit(f"adb timed out: {error}")
