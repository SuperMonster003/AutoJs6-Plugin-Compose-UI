"""Prove that a designer draft survives the plugin process being killed (roadmap P8.3 / D40, P10.4) over adb.

DesignerDeviceTest covers draft recovery with a pre-written draft file, because an instrumented test runs
inside the very process it would have to kill. This check drives the installed plugin the way a user would:
it creates a project from the designer home, inserts a component from the palette, sends the editor to the
background with HOME (the editor writes its draft in onStop), kills the background process with `am kill` and
starts the plugin again from its launcher alias. Android restores the task, so the editor comes back in a new
process (a launcher that starts a fresh task shows the home instead, and the project is opened from its row).
The draft dialog must appear, restoring it must bring the inserted component back, and the project file itself
must still be the unedited template. The edit is then discarded and the project deleted.

The check runs once for a Compose project (a Button on the real-renderer canvas) and once for a ui (XML)
project (a `button` row in the outline, roadmap P10.4); `--kind` limits it to one of them.
A third kind (roadmap P11.3, `--kind folder` or `all`) creates a folder project whose layout is a file, so the restored draft is the layout file's.

usage: verify_designer_recovery.py --apk <apk> [--serial <serial>] [--kind compose|ui|folder|both|all] [--uninstall] [--strings <values/strings.xml>]
"""
import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_standalone_launch as launch  # noqa: E402

PACKAGE = launch.PACKAGE

# What each kind creates, inserts and expects: the palette entry, the text that proves the insertion, the
# text that proves the unedited template, and the project kind chip (None for the default Compose project).
KINDS = {
    "compose": {"stem": "recovery-check", "inserted": "Button", "template": launch.TEMPLATE_TEXT, "chip": None},
    "ui": {"stem": "recovery-check-ui", "inserted": "button", "template": launch.UI_TEMPLATE_ROW, "chip": "ui"},
    # Roadmap P11.3: a folder project whose layout is a file; the inserted button lives in the layout file's draft.
    "folder": {"stem": "recovery-check-folder", "inserted": "button", "template": launch.UI_TEMPLATE_ROW, "chip": "ui", "layout": "folder"},
}


def start_from_launcher(adb: launch.Adb, restored_editor: bool = False) -> str:
    """Start the enabled launcher alias and return the plugin's top Activity once it is up.

    After a process death Android restores the task, so the alias brings back the editor that was on top
    (recreated in a new process) instead of the home; with restored_editor=True either one is accepted.
    """
    resolved = adb.shell(f"cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER {PACKAGE}", check=False)
    component = next((line.strip() for line in resolved.splitlines() if line.strip().startswith(PACKAGE + "/")), launch.DEFAULT_ALIAS)
    start = adb.shell(f"am start -W -n {component}", timeout=120)
    if "Status: ok" not in start:
        raise SystemExit(f"am start did not succeed for {component}:\n{start}")
    designer = launch.expand(launch.DESIGNER_ACTIVITY)

    def top():
        found = launch.top_activity(adb)
        if found and (found[0].startswith(launch.GALLERY) or (restored_editor and launch.expand(found[0]) == designer)):
            return found[0]
        return None
    return launch.wait_for("the designer home" + (" or the restored editor" if restored_editor else "") + " to be the top Activity", top)


def plugin_pid(adb: launch.Adb) -> int | None:
    output = adb.shell(f"pidof {PACKAGE}", check=False).split()
    return int(output[0]) if output and output[0].isdigit() else None


def text_on_screen(adb: launch.Adb, text: str) -> bool:
    return any((node.get("text") or "").strip() == text for node in launch.ui_tree(adb).iter("node") if node.get("package") == PACKAGE)


def insert_from_palette(adb: launch.Adb, add_label: str, component: str) -> None:
    add, _ = launch.find_node(adb, "the add component button", lambda n: (n.get("content-desc") or "").strip() == add_label, clickable=True)
    launch.tap(adb, add)
    # The palette lists components by category; scroll its sheet until the entry is on screen.
    for _ in range(16):
        root = launch.ui_tree(adb)
        rotated = root.get("rotation", "0") in ("1", "3")
        width, height = (int(value) for value in adb.shell("wm size").split()[-1].split("x"))
        if rotated:
            width, height = height, width
        parents = {child: parent for parent in root.iter() for child in parent}
        rows = []
        for node in root.iter("node"):
            if node.get("package") != PACKAGE or not (node.get("text") or "").strip():
                continue
            row = node
            while row is not None and row.get("clickable") != "true":
                row = parents.get(row)
            if row is not None:
                rows.append((node.get("text").strip(), bounds(row)))
        target = next((box for text, box in rows if text == component), None)
        full = max((bottom - top for _, (_, top, _, bottom) in rows), default=0)
        if target is not None:
            left, top, right, bottom = target
            if bottom - top >= full * 0.9:
                adb.shell(f"input tap {(left + right) // 2} {(top + bottom) // 2}")
                return
            # The entry is cut off by the sheet's edge, where a tap can land on the drag handle instead; nudge it
            # fully into view first (finger down when it sits in the upper half, up otherwise).
            # The drag starts on a row next to the entry, so it stays inside the sheet's list.
            middle = (top + bottom) // 2
            down = middle < height // 2
            start = middle + full if down else middle - full
            adb.shell(f"input swipe {(left + right) // 2} {start} {(left + right) // 2} {start + (full * 2 if down else -full * 2)} 800")
            time.sleep(1.0)
            continue
        # A slow drag scrolls without a fling, so the list is at rest when the next dump finds the entry. It moves
        # by half of the rows' visible span: a longer drag (the sheet shows about a thousand pixels on a 2400 px
        # screen) skips entries between two dumps and the loop never sees them. The finger goes down above the
        # bottom tenth of the screen: with gesture navigation (the API 33 AVD) a drag that starts in the bottom
        # inset is the system's home gesture, which closes the sheet instead of scrolling it.
        if rows:
            tops = [top for _, (_, top, _, _) in rows]
            bottoms = [bottom for _, (_, _, _, bottom) in rows]
            span = max(bottoms) - min(tops)
            start = min(max(bottoms) - full // 2, height - max(full, height // 10))
            end = max(start - span // 2, min(tops) + full // 2)
            adb.shell(f"input swipe {width // 2} {start} {width // 2} {end} 1000")
        else:
            adb.shell(f"input swipe {width // 2} {height * 2 // 3} {width // 2} {height // 3} 1000")
        time.sleep(1.0)
    raise SystemExit(f"the palette entry {component} did not come into view")


def bounds(node) -> tuple[int, int, int, int]:
    left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds", "")))
    return left, top, right, bottom


def run_once(adb: launch.Adb, label, labels: dict[str, str], kind: str) -> dict[str, object]:
    """One create / insert / kill / restore / discard cycle for [kind]; returns what to report."""
    spec = KINDS[kind]
    inserted = spec["inserted"]
    file_name = None
    try:
        start_from_launcher(adb)
        file_name = launch.create_project(adb, labels, spec["stem"], kind=spec["chip"], expect=spec["template"], layout=spec.get("layout"))
        insert_from_palette(adb, label("designer_add_component"), inserted)
        # The palette sheet closes after the pick; its own entry must not count as the inserted component.
        time.sleep(1.5)
        launch.wait_for(f"the inserted {inserted} in the editor", lambda: text_on_screen(adb, inserted))

        # HOME stops the editor, which writes the draft; the process is then a background process.
        killed = plugin_pid(adb)
        adb.shell("input keyevent KEYCODE_HOME")
        time.sleep(2.0)
        for _ in range(5):
            adb.shell(f"am kill {PACKAGE}", check=False)
            time.sleep(1.0)
            if plugin_pid(adb) is None:
                break
        else:
            # Android 7 keeps the app just left as the "previous app" (adj 700), which `am kill` spares. The
            # low-memory killer would still take it, and a signal from run-as (a debuggable build) or from a
            # rooted adbd ends the process the same way for the task that Android then restores.
            for command in (f"run-as {PACKAGE} kill -9 {killed}", f"kill -9 {killed}", f"su 0 kill -9 {killed}"):
                adb.shell(command, check=False)
                time.sleep(1.0)
                if plugin_pid(adb) is None:
                    break
            else:
                raise SystemExit(f"am kill left the plugin process alive (pid {plugin_pid(adb)}) and no signal could end it")

        reopened = start_from_launcher(adb, restored_editor=True)
        restarted = plugin_pid(adb)
        if restarted is None or restarted == killed:
            raise SystemExit(f"the plugin was not restarted in a new process (before {killed}, after {restarted})")
        if reopened.startswith(launch.GALLERY):
            row, _ = launch.find_node(adb, f"the row of {file_name}", lambda n: (n.get("text") or "").strip() == file_name, clickable=True)
            launch.tap(adb, row)
        launch.find_node(adb, "the draft dialog", lambda n: (n.get("text") or "").strip() == label("designer_draft_title"))
        restore, _ = launch.find_node(adb, "the restore draft button", lambda n: (n.get("text") or "").strip() == label("designer_draft_restore"), clickable=True)
        launch.tap(adb, restore)
        launch.wait_for(f"the restored {inserted} in the editor", lambda: text_on_screen(adb, inserted))

        # The draft is not a save: leaving with "Discard" keeps the template on disk.
        adb.shell("input keyevent KEYCODE_BACK")
        launch.find_node(adb, "the leave dialog", lambda n: (n.get("text") or "").strip() == label("designer_back_discard_title"))
        discard, _ = launch.find_node(adb, "the discard button", lambda n: (n.get("text") or "").strip() == label("designer_discard"), clickable=True)
        launch.tap(adb, discard)
        launch.wait_for("the designer home after discarding", lambda: (lambda t: t if t and t[0].startswith(launch.GALLERY) else None)(launch.top_activity(adb)))
        row, _ = launch.find_node(adb, f"the row of {file_name}", lambda n: (n.get("text") or "").strip() == file_name, clickable=True)
        launch.tap(adb, row)
        launch.wait_for("the template after reopening", lambda: text_on_screen(adb, spec["template"]))
        if text_on_screen(adb, inserted) or text_on_screen(adb, label("designer_draft_title")):
            raise SystemExit("the discarded draft came back or reached the project file")
        adb.shell("input keyevent KEYCODE_BACK")
        launch.wait_for("the designer home after reopening", lambda: (lambda t: t if t and t[0].startswith(launch.GALLERY) else None)(launch.top_activity(adb)))
        launch.delete_project(adb, labels, file_name)
        file_name = None
    except SystemExit as failure:
        crashes = launch.crash_lines(adb)
        detail = ("\nthe plugin process crashed:\n" + "\n".join(crashes)) if crashes else ""
        raise SystemExit(f"[{kind}] {failure}{detail}" + (f"\nthe project {file_name} may be left on the device" if file_name else "")) from None
    finally:
        adb.shell(f"am force-stop {PACKAGE}", check=False)
    return {"killed": killed, "restarted": restarted, "reopened": "editor" if not reopened.startswith(launch.GALLERY) else "home", "inserted": inserted}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--serial")
    parser.add_argument("--kind", choices=["compose", "ui", "folder", "both", "all"], default="both",
                        help="which project kind to drive: both = compose and ui, all = also a folder project (default: both)")
    parser.add_argument("--uninstall", action="store_true", help="remove the plugin from the device afterwards")
    parser.add_argument("--strings", type=Path, default=Path(__file__).resolve().parent.parent / "app/src/main/res/values/strings.xml",
                        help="strings.xml of the language the plugin shows (it follows the AutoJs6 language by default)")
    args = parser.parse_args()
    adb = launch.Adb(args.serial)
    label = lambda name: launch.resource_string(args.strings, name)  # noqa: E731
    labels = {"new": label("designer_new_project"), "create": label("designer_create"), "delete": label("designer_delete_project"), "ui": label("designer_ui_language_ui"), "folder": label("designer_layout_file_autojs6")}

    api = adb.shell("getprop ro.build.version.sdk").strip()
    install = adb.install(args.apk)
    if "Success" not in install:
        raise SystemExit(f"install failed:\n{install}")
    adb.shell(f"am force-stop {PACKAGE}", check=False)
    adb.shell("input keyevent KEYCODE_WAKEUP", check=False)
    adb.shell("wm dismiss-keyguard", check=False)
    adb.run("logcat", "-c", check=False)

    kinds = {"both": ["compose", "ui"], "all": ["compose", "ui", "folder"]}.get(args.kind, [args.kind])
    results = {}
    try:
        for kind in kinds:
            results[kind] = run_once(adb, label, labels, kind)
        crashes = launch.crash_lines(adb)
    finally:
        adb.shell(f"am force-stop {PACKAGE}", check=False)
        adb.shell(f"rm -f {launch.UI_DUMP}", check=False)
        if args.uninstall:
            adb.run("uninstall", PACKAGE, check=False)
    # `am kill` is expected to end the first process; only crashes count.
    crashes = [line for line in crashes if "FATAL EXCEPTION" in line]
    if crashes:
        raise SystemExit("the plugin process crashed:\n" + "\n".join(crashes))
    summary = " ".join(f"{kind}=killed:{r['killed']},restarted:{r['restarted']},reopened:{r['reopened']},inserted:{r['inserted']}" for kind, r in results.items())
    print(f"DESIGNER_RECOVERY_OK api={api} {summary} apk={args.apk.name}")


if __name__ == "__main__":
    try:
        main()
    except subprocess.TimeoutExpired as error:
        sys.exit(f"adb timed out: {error}")
