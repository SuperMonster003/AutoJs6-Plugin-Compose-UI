"""Drive the AutoJs6 editor -> 3-Block Compose designer -> AutoJs6 editor round trip over adb (roadmap P8.4).

The plugin's instrumentation proves each half on its own (the FileProvider, the destinations, an external
document written back through its Uri); the hand-over between the two apps needs both installed and was
checked by hand during P8.4. This check repeats that walk on one device: it pushes a small compose script to
/sdcard/Download, opens it in the AutoJs6 editor through the host's public EditIntentActivity, chooses
"More > Open in 3-Block Compose designer" from the editor menu, inserts a Button in the designer, saves and
goes back. The file must then contain the Button, and the AutoJs6 editor must have reloaded the new text
without being asked. The pushed file is removed afterwards.

The AutoJs6 build must contain the editor entry (host commit db7e6a1734 or later) and be able to read
/sdcard/Download (storage access granted). An installed AutoJs6 that predates the entry keeps the same
version code, so check the build rather than the number; a build installed under another application ID (the
isolated `-PcomposeUiSpike=true` host, org.autojs.autojs6.compose.spike) can be named with --host-package.
Menu labels come from the host's strings.xml in the language the
host shows, so pass the host checkout explicitly; nothing in the plugin build depends on it.

usage: verify_host_handoff.py --apk <apk> --host-strings <AutoJs6/app/src/main/res/values[-xx]/strings.xml>
                              [--serial <serial>] [--host-package <application id>]
                              [--strings <plugin values/strings.xml>] [--uninstall]
"""
import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_designer_recovery as recovery  # noqa: E402
import verify_standalone_launch as launch  # noqa: E402

HOST = "org.autojs.autojs6"
EDIT_INTENT_CLASS = "org.autojs.autojs.external.open.EditIntentActivity"
EDITOR_CLASS = "org.autojs.autojs.ui.edit.EditActivity"
SCRIPT = "compose.mount(compose.Column({padding: 16},\n    compose.Text({key: 'handover', text: 'Hand-over'})));\n"
INSERTED = "compose.Button("


def host_nodes(adb: launch.Adb):
    return [node for node in launch.ui_tree(adb).iter("node") if node.get("package") == HOST]


def host_top(adb: launch.Adb) -> str | None:
    found = None
    for line in adb.shell("dumpsys activity top", check=False).splitlines():
        match = re.match(r"\s*ACTIVITY (\S+) \S+ pid=\d+", line)
        if match:
            found = match.group(1)
    return found


def tap_host_text(adb: launch.Adb, text: str) -> None:
    """Tap a menu item, scrolling the menu when it is longer than the screen (a landscape phone)."""
    def search():
        nodes = host_nodes(adb)
        match = next((node for node in nodes if (node.get("text") or "").strip() == text), None)
        if match is not None:
            return match
        menus = [node for node in nodes if node.get("scrollable") == "true"]
        if menus:
            left, top, right, bottom = map(int, re.findall(r"\d+", menus[-1].get("bounds", "")))
            x = (left + right) // 2
            adb.shell(f"input swipe {x} {top + (bottom - top) * 4 // 5} {x} {top + (bottom - top) // 5} 600")
            time.sleep(0.8)
        return None
    launch.tap(adb, launch.wait_for(f"the AutoJs6 menu item {text!r}", search, timeout=20.0))


def open_overflow_menu(adb: launch.Adb) -> None:
    """Tap the rightmost unlabelled image in the AutoJs6 editor toolbar: the overflow ("More options") button.

    KEYCODE_MENU is not used because MIUI turns it into the recent apps screen, and the button's description is
    a localized system string.
    """
    def search():
        root = launch.ui_tree(adb)
        toolbar = next((node for node in root.iter("node") if node.get("package") == HOST and (node.get("resource-id") or "").endswith(":id/toolbar")), None)
        if toolbar is None:
            return None
        images = [node for node in toolbar.iter("node") if node.get("class") in ("android.widget.ImageView", "android.widget.ImageButton") and not node.get("resource-id")]
        return max(images, key=lambda node: int(re.findall(r"\d+", node.get("bounds", "0"))[0]), default=None)
    launch.tap(adb, launch.wait_for("the AutoJs6 editor overflow button", search, timeout=15.0))


def editor_text(adb: launch.Adb) -> str:
    # The AutoJs6 editor is Ace in a WebView: each visible line is a TextView node inside it (plus Ace's hidden
    # EditText), so collect every text of the host window rather than one EditText.
    return "\n".join(node.get("text") or "" for node in host_nodes(adb))


def main() -> None:
    global HOST
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--host-strings", type=Path, required=True)
    parser.add_argument("--serial")
    parser.add_argument("--host-package", default=HOST, help="application ID of the AutoJs6 build to drive")
    parser.add_argument("--uninstall", action="store_true", help="remove the plugin from the device afterwards")
    parser.add_argument("--strings", type=Path, default=Path(__file__).resolve().parent.parent / "app/src/main/res/values/strings.xml",
                        help="strings.xml of the language the plugin shows (it follows the AutoJs6 language by default)")
    args = parser.parse_args()
    HOST = args.host_package
    host_edit_intent = f"{HOST}/{EDIT_INTENT_CLASS}"
    host_editor = f"{HOST}/{EDITOR_CLASS}"
    adb = launch.Adb(args.serial)
    more = launch.resource_string(args.host_strings, "text_more")
    open_in_designer = launch.resource_string(args.host_strings, "text_open_in_three_block_compose_designer")
    add_label = launch.resource_string(args.strings, "designer_add_component")
    save_label = launch.resource_string(args.strings, "designer_save")

    if "package:" not in adb.shell(f"pm path {HOST}", check=False):
        raise SystemExit(f"{HOST} is not installed")
    api = adb.shell("getprop ro.build.version.sdk").strip()
    install = adb.install(args.apk)
    if "Success" not in install:
        raise SystemExit(f"install failed:\n{install}")
    adb.shell(f"am force-stop {launch.PACKAGE}", check=False)
    adb.shell("input keyevent KEYCODE_WAKEUP", check=False)
    adb.shell("wm dismiss-keyguard", check=False)

    remote = f"/sdcard/Download/three-block-compose-handoff-{int(time.time())}.js"
    local = Path(__file__).resolve().parent / ".handoff.js"
    local.write_text(SCRIPT, encoding="utf-8", newline="\n")
    try:
        adb.run("push", str(local), remote)
    finally:
        local.unlink(missing_ok=True)
    adb.run("logcat", "-c", check=False)
    try:
        start = adb.shell(f"am start -W -a android.intent.action.EDIT -d file://{remote} -t application/x-javascript -n {host_edit_intent}")
        if "Error" in start:
            raise SystemExit(f"the AutoJs6 editor could not be opened:\n{start}")
        launch.wait_for("the AutoJs6 editor", lambda: launch.expand(host_editor) == launch.expand(host_top(adb) or ""))
        launch.wait_for("the script in the AutoJs6 editor", lambda: "Hand-over" in editor_text(adb))
        open_overflow_menu(adb)
        tap_host_text(adb, more)
        tap_host_text(adb, open_in_designer)
        launch.wait_for("the designer editor", lambda: (lambda t: t if t and launch.expand(t[0]) == launch.expand(launch.DESIGNER_ACTIVITY) else None)(launch.top_activity(adb)))
        launch.find_node(adb, "the hand-over text on the design canvas", lambda n: (n.get("text") or "").strip() == "Hand-over")
        recovery.insert_from_palette(adb, add_label, "Button")
        launch.wait_for("the inserted Button", lambda: recovery.text_on_screen(adb, "Button"))
        save, _ = launch.find_node(adb, "the save action", lambda n: (n.get("content-desc") or "").strip() == save_label, clickable=True)
        launch.tap(adb, save)
        launch.wait_for("the file to receive the Button", lambda: INSERTED in adb.shell(f"cat {remote}", check=False), timeout=15.0)
        adb.shell("input keyevent KEYCODE_BACK")
        launch.wait_for("the AutoJs6 editor after the designer", lambda: launch.expand(host_editor) == launch.expand(host_top(adb) or ""))
        launch.wait_for("the AutoJs6 editor to reload the designer's text", lambda: INSERTED in editor_text(adb), timeout=15.0)
        crashes = launch.crash_lines(adb)
    except SystemExit as failure:
        crashes = launch.crash_lines(adb)
        detail = ("\nthe plugin process crashed:\n" + "\n".join(crashes)) if crashes else ""
        raise SystemExit(f"{failure}{detail}") from None
    finally:
        adb.shell(f"am force-stop {launch.PACKAGE}", check=False)
        # Leave the AutoJs6 editor without saving anything over the removed file.
        if launch.expand(host_editor) == launch.expand(host_top(adb) or ""):
            adb.shell("input keyevent KEYCODE_BACK", check=False)
        adb.shell(f"rm -f {remote} {launch.UI_DUMP}", check=False)
        if args.uninstall:
            adb.run("uninstall", launch.PACKAGE, check=False)
    if crashes:
        raise SystemExit("the plugin process crashed:\n" + "\n".join(crashes))
    print(f"HOST_HANDOFF_OK api={api} file={remote} apk={args.apk.name}")


if __name__ == "__main__":
    try:
        main()
    except subprocess.TimeoutExpired as error:
        sys.exit(f"adb timed out: {error}")
