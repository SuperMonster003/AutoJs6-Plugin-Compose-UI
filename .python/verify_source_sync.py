"""Prove that an imported project keeps access to its source across a process death (roadmap P9.1 / D45) over adb.

DesignerSourceSyncDeviceTest drives write back and re-import inside the instrumented process with a FileProvider
Uri, which cannot carry a persistable grant, and the AVD's document picker does not respond to injected input.
This check uses the real picker: it pushes a script to /sdcard/Download, imports it from the designer home
through the system document picker, inserts a Button and writes the project back to the source. It then stops
the plugin, changes the source from the shell, starts the plugin in a new process and re-imports the source
(read access must have survived), inserts another Button and writes back again (write access must have
survived). The project and the source are deleted afterwards.

usage: verify_source_sync.py --apk <apk> [--serial <serial>] [--uninstall] [--strings <values/strings.xml>]
"""
import argparse
import base64
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_designer_recovery as recovery  # noqa: E402
import verify_standalone_launch as launch  # noqa: E402

PACKAGE = launch.PACKAGE
PICKERS = ("com.google.android.documentsui", "com.android.documentsui")
SOURCE = "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'origin', text: 'From the source'})));\n"
CHANGED = "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'origin', text: 'Changed outside'})));\n"


def text_is(value: str):
    return lambda node: (node.get("text") or "").strip() == value


def desc_is(value: str):
    return lambda node: (node.get("content-desc") or "").strip() == value


def resumed_package(adb: launch.Adb) -> str | None:
    """The package of the resumed Activity, whichever app it belongs to (top_activity only reports the plugin's)."""
    output = adb.shell("dumpsys activity activities", check=False)
    match = re.search(r"(?:topResumedActivity|mResumedActivity)[=:]\s*ActivityRecord\{\S+ u\d+ ([^/\s]+)/", output)
    return match.group(1) if match else None


def pick_document(adb: launch.Adb, file_name: str) -> str:
    """Choose [file_name] in the system document picker: from the list it opens on, else through its search."""
    picker = launch.wait_for("the system document picker", lambda: (lambda p: p if p in PICKERS else None)(resumed_package(adb)))

    def picker_node(predicate):
        # launch.find_node only looks at the plugin's own nodes; the picker is another app.
        return next((node for node in launch.ui_tree(adb).iter("node") if node.get("package") == picker and predicate(node)), None)

    def listed():
        return picker_node(text_is(file_name))
    try:
        entry = launch.wait_for(f"{file_name} in the picker", listed, timeout=8.0)
    except SystemExit:
        search = launch.wait_for("the picker's search action", lambda: picker_node(lambda n: (n.get("resource-id") or "").endswith(("option_menu_search", "menu_search"))), timeout=10.0)
        launch.tap(adb, search)
        time.sleep(1.0)
        adb.shell(f"input text {file_name.rsplit('.', 1)[0]}")
        adb.shell("input keyevent KEYCODE_ENTER")
        entry = launch.wait_for(f"{file_name} in the picker's search results", listed, timeout=20.0)
    launch.tap(adb, entry)
    return picker


def editor_on_top(adb: launch.Adb):
    return (lambda t: t if t and launch.expand(t[0]) == launch.expand(launch.DESIGNER_ACTIVITY) else None)(launch.top_activity(adb))


def menu_action(adb: launch.Adb, menu_label: str, item_label: str) -> None:
    menu, _ = launch.find_node(adb, "the editor menu", desc_is(menu_label), clickable=True)
    launch.tap(adb, menu)
    item, _ = launch.find_node(adb, f"the menu item {item_label!r}", text_is(item_label), clickable=True)
    launch.tap(adb, item)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--serial")
    parser.add_argument("--uninstall", action="store_true", help="remove the plugin from the device afterwards")
    parser.add_argument("--strings", type=Path, default=Path(__file__).resolve().parent.parent / "app/src/main/res/values/strings.xml",
                        help="strings.xml of the language the plugin shows (it follows the AutoJs6 language by default)")
    args = parser.parse_args()
    adb = launch.Adb(args.serial)
    label = lambda name: launch.resource_string(args.strings, name)  # noqa: E731
    labels = {"new": label("designer_new_project"), "create": label("designer_create"), "delete": label("designer_delete_project")}

    api = adb.shell("getprop ro.build.version.sdk").strip()
    install = adb.run("install", "-r", "-t", str(args.apk), check=False, timeout=600)
    if "Success" not in install:
        raise SystemExit(f"install failed:\n{install}")
    adb.shell(f"am force-stop {PACKAGE}", check=False)
    adb.shell("input keyevent KEYCODE_WAKEUP", check=False)
    adb.shell("wm dismiss-keyguard", check=False)

    stem = f"source-sync-check-{int(time.time())}"
    file_name = f"{stem}.js"
    remote = f"/sdcard/Download/{file_name}"
    local = Path(__file__).resolve().parent / ".source-sync.js"
    local.write_text(SOURCE, encoding="utf-8", newline="\n")
    try:
        adb.run("push", str(local), remote)
    finally:
        local.unlink(missing_ok=True)
    # Let the media store list the pushed file, so the picker's recent and download views show it.
    adb.shell(f"am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://{remote}", check=False)
    launch.wait_for("the media store to list the pushed file", lambda: file_name in adb.shell(
        f"content query --uri content://media/external/file --projection _display_name --where \"_display_name='{file_name}'\"", check=False), timeout=20.0)
    adb.run("logcat", "-c", check=False)

    project = None
    picker = None
    try:
        recovery.start_from_launcher(adb)
        menu_action(adb, label("designer_import_project"), label("designer_import_project"))
        picker = pick_document(adb, file_name)
        launch.wait_for("the editor of the imported project", lambda: editor_on_top(adb))
        project = file_name
        launch.find_node(adb, "the imported text on the canvas", text_is("From the source"))

        # Write back right after an edit: the source is unchanged since the import, so no question is asked.
        recovery.insert_from_palette(adb, label("designer_add_component"), "Button")
        launch.wait_for("the inserted Button", lambda: recovery.text_on_screen(adb, "Button"))
        menu_action(adb, label("designer_host_options"), label("designer_source_write_back"))
        launch.wait_for("the source to receive the Button", lambda: "compose.Button" in adb.shell(f"cat {remote}", check=False), timeout=15.0)
        print("imported through the picker and wrote back", flush=True)

        # A new process: only a persisted grant still opens the source.
        first = recovery.plugin_pid(adb)
        adb.shell("input keyevent KEYCODE_HOME")
        time.sleep(1.5)
        adb.shell(f"am force-stop {PACKAGE}")
        launch.wait_for("the plugin process to end", lambda: recovery.plugin_pid(adb) is None)
        # Rewrite the same file in place, as an editor saving it would. `adb push` replaces the file, which gives it
        # a new media store entry and so breaks the picked Uri (the "Pick again" case, not what is checked here).
        encoded = base64.b64encode(CHANGED.encode("utf-8")).decode("ascii")
        adb.shell(f"echo {encoded} | base64 -d > {remote}")
        if "Changed outside" not in adb.shell(f"cat {remote}"):
            raise SystemExit("the source could not be rewritten in place")

        recovery.start_from_launcher(adb)
        second = recovery.plugin_pid(adb)
        row, _ = launch.find_node(adb, f"the row of {project}", text_is(project), clickable=True)
        launch.tap(adb, row)
        launch.wait_for("the editor of the project", lambda: editor_on_top(adb))
        launch.find_node(adb, "the written-back Button on the canvas", text_is("Button"))
        menu_action(adb, label("designer_host_options"), label("designer_source_reimport"))
        launch.find_node(adb, "the re-imported text on the canvas", text_is("Changed outside"))
        print("re-imported in a new process", flush=True)
        if recovery.text_on_screen(adb, "Button"):
            raise SystemExit("the re-import kept the project's Button instead of the source's text")

        recovery.insert_from_palette(adb, label("designer_add_component"), "Button")
        launch.wait_for("the inserted Button", lambda: recovery.text_on_screen(adb, "Button"))
        menu_action(adb, label("designer_host_options"), label("designer_source_write_back"))
        launch.wait_for("the source to receive the second write", lambda: (lambda text: "Changed outside" in text and "compose.Button" in text)(adb.shell(f"cat {remote}", check=False)), timeout=15.0)

        adb.shell("input keyevent KEYCODE_BACK")
        launch.wait_for("the designer home", lambda: (lambda t: t if t and t[0].startswith(launch.GALLERY) else None)(launch.top_activity(adb)))
        launch.delete_project(adb, labels, project)
        project = None
        crashes = launch.crash_lines(adb)
    except SystemExit as failure:
        crashes = launch.crash_lines(adb)
        detail = ("\nthe plugin process crashed:\n" + "\n".join(crashes)) if crashes else ""
        raise SystemExit(f"{failure}{detail}" + (f"\nthe project {project} may be left on the device" if project else "")) from None
    finally:
        adb.shell(f"am force-stop {PACKAGE}", check=False)
        if picker:
            adb.shell(f"am force-stop {picker}", check=False)
        adb.shell(f"rm -f {remote} {launch.UI_DUMP}", check=False)
        if args.uninstall:
            adb.run("uninstall", PACKAGE, check=False)
    crashes = [line for line in crashes if "FATAL EXCEPTION" in line]
    if crashes:
        raise SystemExit("the plugin process crashed:\n" + "\n".join(crashes))
    print(f"SOURCE_SYNC_OK api={api} picker={picker} first_pid={first} second_pid={second} source={remote} apk={args.apk.name}")


if __name__ == "__main__":
    try:
        main()
    except subprocess.TimeoutExpired as error:
        sys.exit(f"adb timed out: {error}")
