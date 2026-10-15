"""Open the designer home from its launcher alias in the plugin's own process and walk its screens over adb.

The instrumentation suites run inside an instrumented process that also carries the test APK, so they
cannot prove that the plugin APK alone has every class the standalone screens need (build 42 passed them and
still crashed from the launcher). This check installs the given APK on one device, starts the enabled launcher
alias exactly as a launcher would, opens the gallery and its first catalog entry (composing its preview), the
settings screen, and a new designer project whose template the Rhino parser and the real renderer must turn
into a visible preview (roadmap P8.3: the R8 release build keeps only the parser closure it references), all
through the accessibility tree. On Android 8.0 or later it then turns on the interactive preview, which runs the
template in the designer's Rhino sandbox (roadmap P9.3 step 2: the release build keeps the interpreter closure and
the two classes Rhino builds by name), and waits for the canvas to show the running script. It deletes the project
again and fails on any crash, dead process, missing screen or stopped preview.

usage: verify_standalone_launch.py --apk <apk> [--serial <serial>] [--uninstall] [--strings <values/strings.xml>]
"""
import argparse
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ElementTree
from pathlib import Path

PACKAGE = "io.github.supermonster003.autojs6.plugin.three.block.compose"
DEFAULT_ALIAS = f"{PACKAGE}/.launcher.AdaptiveAutoIconAlias"
GALLERY = f"{PACKAGE}/.launcher."
SETTINGS = f"{PACKAGE}/.app.SettingsActivity"
GALLERY_ACTIVITY = f"{PACKAGE}/.app.GalleryActivity"
DESIGNER_ACTIVITY = f"{PACKAGE}/.designer.ui.DesignerActivity"
# The starter script of a new project puts this text on the canvas (ProjectStore.template()).
TEMPLATE_TEXT = "Hello"
UI_DUMP = "/data/local/tmp/three-block-compose-standalone-launch.xml"
# A debug APK installs within a minute even on a slow CI emulator; a longer wait only means a stalled transfer.
INSTALL_TIMEOUT_S = 240


class Adb:
    def __init__(self, serial: str | None) -> None:
        self.base = ["adb"] + (["-s", serial] if serial else [])

    def run(self, *args: str, check: bool = True, timeout: int = 120) -> str:
        completed = subprocess.run(self.base + list(args), capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        if check and completed.returncode != 0:
            raise SystemExit(f"adb {' '.join(args)} failed ({completed.returncode}):\n{completed.stdout}\n{completed.stderr}")
        # uiautomator and pm report some failures on stderr only; callers match substrings, so merge both streams.
        return completed.stdout + ((chr(10) + completed.stderr) if completed.stderr.strip() else "")

    def shell(self, command: str, check: bool = True, timeout: int = 120) -> str:
        return self.run("shell", command, check=check, timeout=timeout)

    def install(self, apk: Path) -> str:
        """`adb install -r -t`, retried once after the device connection is reset if the transfer stalls.

        On the CI's API 24 emulator the first install after the Gradle suite (which has just uninstalled the
        plugin) twice hung until the timeout without any package manager activity on the device.
        """
        try:
            return self.run("install", "-r", "-t", str(apk), check=False, timeout=INSTALL_TIMEOUT_S)
        except subprocess.TimeoutExpired:
            print(f"adb install stalled for {INSTALL_TIMEOUT_S} s; reconnecting and trying once more", file=sys.stderr)
            self.run("reconnect", check=False, timeout=60)
            self.run("wait-for-device", check=False, timeout=180)
            return self.run("install", "-r", "-t", str(apk), check=False, timeout=INSTALL_TIMEOUT_S)


def resource_string(strings: Path, name: str) -> str:
    for element in ElementTree.parse(strings).getroot():
        if element.tag == "string" and element.get("name") == name:
            return (element.text or "").replace("\\'", "'")
    raise SystemExit(f"{strings} has no string {name}")


def top_activity(adb: Adb) -> tuple[str, int] | None:
    """Return (component, pid) of the plugin's top Activity; tasks are dumped bottom-up, so the last line wins."""
    found = None
    for line in adb.shell("dumpsys activity top", check=False).splitlines():
        match = re.match(r"\s*ACTIVITY (\S+) \S+ pid=(\d+)", line)
        if match and match.group(1).startswith(PACKAGE + "/"):
            found = match.group(1), int(match.group(2))
    return found


def wait_for(description: str, predicate, timeout: float = 30.0, interval: float = 0.5):
    deadline = time.monotonic() + timeout
    while True:
        value = predicate()
        # An ElementTree element without children is falsy, so test for a result explicitly.
        if value is not None and value is not False:
            return value
        if time.monotonic() > deadline:
            raise SystemExit(f"timed out waiting for {description}")
        time.sleep(interval)


def expand(component: str) -> str:
    package, _, name = component.partition("/")
    return f"{package}/{package + name if name.startswith('.') else name}"


def ui_tree(adb: Adb) -> ElementTree.Element:
    last = ""
    for attempt in range(12):
        output = adb.shell(f"uiautomator dump {UI_DUMP}", check=False, timeout=60)
        last = output.strip()
        if "dumped to" in output:
            # API 24 occasionally answers the first cat with exit 255 while the dump is still being written.
            xml = adb.shell(f"cat {UI_DUMP}", check=False)
            try:
                return ElementTree.fromstring(xml)
            except ElementTree.ParseError:
                pass
        # A slow emulator keeps reporting "could not get idle state" while an animation runs; wait it out.
        time.sleep(1.0 + attempt)
    raise SystemExit(f"uiautomator could not dump the accessibility tree; last output: {last[-200:]}")


def tap(adb: Adb, node: ElementTree.Element) -> None:
    left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds", "")))
    adb.shell(f"input tap {(left + right) // 2} {(top + bottom) // 2}")


def find_node(adb: Adb, description: str, predicate, clickable: bool = False) -> tuple[ElementTree.Element, ElementTree.Element]:
    """Return (tap target, matched node) for the first node matching the predicate.

    Compose does not merge a clickable row with the text or icon it contains in the uiautomator tree, so with
    clickable=True the tap target is the nearest ancestor that carries clickable="true"; a match without one
    (a category header, the title) is skipped.
    """
    last_tree: list[ElementTree.Element] = []

    def search():
        root = ui_tree(adb)
        last_tree[:] = [root]
        parents = {child: parent for parent in root.iter() for child in parent}
        for node in root.iter("node"):
            # Only the plugin's own window counts: a system dialog left on a busy CI emulator (for example the
            # "Close app" button of an ANR dialog) must not be mistaken for a catalog entry.
            if node.get("package") != PACKAGE or not predicate(node):
                continue
            if not clickable:
                return node, node
            candidate = node
            while candidate is not None and candidate.get("clickable") != "true":
                candidate = parents.get(candidate)
            if candidate is not None:
                return candidate, node
        return None

    try:
        return wait_for(description, search)
    except SystemExit as failure:
        # Describe what was on screen instead, so a CI log explains the miss without the device.
        visible = []
        for node in (last_tree[0].iter("node") if last_tree else []):
            label = (node.get("text") or "").strip() or (node.get("content-desc") or "").strip()
            if label:
                visible.append(label[:60])
        raise SystemExit(f"{failure}\nvisible labels: {visible[:40]}") from None


def crash_lines(adb: Adb) -> list[str]:
    log = adb.run("logcat", "-d", "-v", "brief", check=False, timeout=180)
    lines = log.splitlines()
    hits = []
    for index, line in enumerate(lines):
        if "FATAL EXCEPTION" in line and any(PACKAGE in later for later in lines[index:index + 3]):
            hits.append(line)
        elif f"Process {PACKAGE} " in line and "has died" in line:
            hits.append(line)
    return hits


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--serial")
    parser.add_argument("--uninstall", action="store_true", help="remove the plugin from the device afterwards")
    parser.add_argument("--strings", type=Path, default=Path(__file__).resolve().parent.parent / "app/src/main/res/values/strings.xml",
                        help="default-locale strings.xml used to find the settings action and the copy button")
    args = parser.parse_args()
    adb = Adb(args.serial)
    settings_label = resource_string(args.strings, "settings_title")
    copy_label = resource_string(args.strings, "gallery_copy")
    gallery_label = resource_string(args.strings, "designer_open_gallery")
    labels = {
        "new": resource_string(args.strings, "designer_new_project"),
        "create": resource_string(args.strings, "designer_create"),
        "delete": resource_string(args.strings, "designer_delete_project"),
        "interact": resource_string(args.strings, "designer_preview_interact"),
        "running": resource_string(args.strings, "designer_sandbox_running"),
        # The prefixes of the sandbox's problem line on the canvas (before the message).
        "stopped": resource_string(args.strings, "designer_sandbox_stopped").split("%1$s")[0],
        "error": resource_string(args.strings, "designer_sandbox_error").split("%1$s")[0],
        # Roadmap P10.4: the project kind chip of a ui (XML) project and the preview status texts of its canvas.
        "ui": resource_string(args.strings, "designer_ui_language_ui"),
        "ui_host_missing": resource_string(args.strings, "designer_ui_preview_host_missing"),
        "ui_waiting": resource_string(args.strings, "designer_ui_preview_waiting"),
        "ui_snapshot": resource_string(args.strings, "designer_ui_preview_snapshot"),
    }

    api = adb.shell("getprop ro.build.version.sdk").strip()
    install = adb.install(args.apk)
    if "Success" not in install:
        raise SystemExit(f"install failed:\n{install}")
    adb.shell(f"am force-stop {PACKAGE}", check=False)
    # A real device left idle during earlier steps may have switched its screen off: taps then go nowhere.
    adb.shell("input keyevent KEYCODE_WAKEUP", check=False)
    adb.shell("wm dismiss-keyguard", check=False)
    adb.run("logcat", "-c", check=False)

    resolved = adb.shell(f"cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER {PACKAGE}", check=False)
    component = next((line.strip() for line in resolved.splitlines() if line.strip().startswith(PACKAGE + "/")), DEFAULT_ALIAS)
    start = adb.shell(f"am start -W -n {component}", timeout=120)
    if "Status: ok" not in start:
        raise SystemExit(f"am start did not succeed for {component}:\n{start}")

    labels["sandbox"] = int(api) >= 26
    try:
        gallery_component, pid, entry_label = walk(adb, settings_label, copy_label, gallery_label, labels)
        crashes = crash_lines(adb)
    except SystemExit as failure:
        crashes = crash_lines(adb)
        detail = ("\nthe plugin process crashed:\n" + "\n".join(crashes)) if crashes else ""
        raise SystemExit(f"{failure}{detail}") from None
    finally:
        adb.shell(f"am force-stop {PACKAGE}", check=False)
        adb.shell(f"rm -f {UI_DUMP}", check=False)
        if args.uninstall:
            adb.run("uninstall", PACKAGE, check=False)
    if crashes:
        raise SystemExit("the plugin process crashed:\n" + "\n".join(crashes))
    sandbox = "ran" if labels["sandbox"] else "skipped(api<26)"
    print(f"STANDALONE_LAUNCH_OK api={api} component={gallery_component} pid={pid} entry={entry_label} settings=opened designer=opened sandbox={sandbox} "
          f"ui_designer=opened ui_preview={labels.get('ui_status', '?')} apk={args.apk.name}")


def walk(adb: Adb, settings_label: str, copy_label: str, gallery_label: str, labels: dict[str, str]) -> tuple[str, int, str]:
    # Roadmap P8.3 / D38: the launcher alias opens the designer home; the gallery and the settings screen hang off it.
    home_component, pid = wait_for("the designer home to be the top Activity", lambda: (lambda t: t if t and t[0].startswith(GALLERY) else None)(top_activity(adb)))
    time.sleep(3.0)
    if "No such" in adb.shell(f"ls -d /proc/{pid}", check=False):
        raise SystemExit(f"the designer process {pid} died after start")

    gallery_action, _ = find_node(adb, "the gallery action", lambda n: (n.get("content-desc") or "").strip() == gallery_label, clickable=True)
    tap(adb, gallery_action)
    wait_for("the gallery screen", lambda: (lambda t: t if t and expand(t[0]) == expand(GALLERY_ACTIVITY) else None)(top_activity(adb)))
    # First catalog entry: the first text that sits inside a clickable row (category headers and the title do not).
    entry, label = find_node(adb, "a catalog entry", lambda n: (n.get("text") or "").strip() != "", clickable=True)
    entry_label = label.get("text").strip()
    tap(adb, entry)
    # The detail screen composes the preview and shows the generated script; on a 1080x1920 screen the copy
    # and run buttons can sit below the fold, so the script text (compose.<Entry>( ) also proves the screen.
    script_call = f"compose.{entry_label}("
    find_node(adb, f"the detail screen of {entry_label} with its script or copy button",
              lambda n: (n.get("text") or "").strip() == copy_label or script_call in (n.get("text") or ""))
    adb.shell("input keyevent KEYCODE_BACK")
    wait_for("the gallery list after leaving the entry", lambda: (lambda t: t if t and expand(t[0]) == expand(GALLERY_ACTIVITY) else None)(top_activity(adb)))
    time.sleep(1.0)
    adb.shell("input keyevent KEYCODE_BACK")
    wait_for("the designer home after leaving the gallery", lambda: (lambda t: t if t and t[0].startswith(GALLERY) else None)(top_activity(adb)))
    settings_action, _ = find_node(adb, "the settings action", lambda n: (n.get("content-desc") or "").strip() == settings_label, clickable=True)
    tap(adb, settings_action)
    wait_for("the settings screen", lambda: (lambda t: t if t and expand(t[0]) == expand(SETTINGS) else None)(top_activity(adb)))
    time.sleep(1.0)
    adb.shell("input keyevent KEYCODE_BACK")
    wait_for("the designer home after leaving settings", lambda: (lambda t: t if t and t[0].startswith(GALLERY) else None)(top_activity(adb)))

    open_and_delete_project(adb, labels)

    after = top_activity(adb)
    if not after or after[1] != pid:
        raise SystemExit(f"the designer process changed from {pid} to {after}: it was restarted")
    return home_component, pid, entry_label


def hide_keyboard(adb: Adb) -> None:
    """Close the input method if it is showing; on a landscape phone it covers the dialog's buttons."""
    if "mInputShown=true" in adb.shell("dumpsys input_method", check=False):
        adb.shell("input keyevent KEYCODE_BACK")
        time.sleep(0.5)


def create_project(adb: Adb, labels: dict[str, str], stem: str, kind: str | None = None, expect: str = TEMPLATE_TEXT) -> str:
    """Create a project named <stem>-<time> from the home and wait until the editor shows [expect].

    [kind] names the label of a project kind chip to tap first (roadmap P10.4: "ui" for a ui (XML) project);
    the default chip makes a Compose JavaScript project. [expect] is the text that proves the editor read the
    template: the Text on the Compose canvas, or an outline row of the ui project.
    """
    project = f"{stem}-{int(time.time())}"
    new_action, _ = find_node(adb, "the new project action", lambda n: (n.get("content-desc") or "").strip() == labels["new"], clickable=True)
    tap(adb, new_action)
    name_field, _ = find_node(adb, "the project name field", lambda n: n.get("class") == "android.widget.EditText")
    tap(adb, name_field)
    time.sleep(0.5)
    adb.shell(f"input text {project}")
    hide_keyboard(adb)
    if kind is not None:
        chip, _ = find_node(adb, f"the {kind} project kind chip", lambda n: (n.get("text") or "").strip() == labels[kind], clickable=True)
        tap(adb, chip)
    create, _ = find_node(adb, "the create button", lambda n: (n.get("text") or "").strip() == labels["create"], clickable=True)
    tap(adb, create)
    wait_for("the designer editor", lambda: (lambda t: t if t and expand(t[0]) == expand(DESIGNER_ACTIVITY) else None)(top_activity(adb)))
    # The template must come out of the parser (and, for a Compose project, the real renderer) inside the release build.
    find_node(adb, f"the template's {expect!r} in the editor", lambda n: (n.get("text") or "").strip() == expect)
    return f"{project}.js"


def open_and_delete_project(adb: Adb, labels: dict[str, str]) -> None:
    """Create a project from the home, let the editor parse and render its template, then delete it again."""
    file_name = create_project(adb, labels, "standalone-check")
    if labels["sandbox"]:
        run_preview(adb, labels)
    time.sleep(1.0)
    adb.shell("input keyevent KEYCODE_BACK")
    wait_for("the designer home after leaving the editor", lambda: (lambda t: t if t and t[0].startswith(GALLERY) else None)(top_activity(adb)))
    delete_project(adb, labels, file_name)
    open_and_delete_ui_project(adb, labels)


UI_TEMPLATE_ROW = "button #ok"


def open_and_delete_ui_project(adb: Adb, labels: dict[str, str]) -> None:
    """Roadmap P10.4: create a ui (XML) project, wait for its outline (the Rhino parser and the XML reader in the
    release build), note what the canvas shows (the host's first frame, or why there is none), then delete it."""
    file_name = create_project(adb, labels, "standalone-check-ui", kind="ui", expect=UI_TEMPLATE_ROW)
    status = "frame"
    deadline = time.time() + 20.0
    while time.time() < deadline:
        texts = {(n.get("text") or "").strip() for n in ui_tree(adb).iter("node") if n.get("package") == PACKAGE}
        if labels["ui_host_missing"] in texts:
            status = "host_missing"
            break
        if labels["ui_snapshot"] in texts:
            status = "snapshot"
            break
        if labels["ui_waiting"] not in texts:
            # Neither a status text nor the waiting line: the host drew the layout (embedded surfaces hold no text).
            break
        time.sleep(1.0)
    else:
        status = "waiting"
    labels["ui_status"] = status
    time.sleep(1.0)
    adb.shell("input keyevent KEYCODE_BACK")
    wait_for("the designer home after leaving the ui editor", lambda: (lambda t: t if t and t[0].startswith(GALLERY) else None)(top_activity(adb)))
    delete_project(adb, labels, file_name)


def run_preview(adb: Adb, labels: dict) -> None:
    """Turn on the interactive preview and wait until the canvas shows the template's running script."""
    chip, _ = find_node(adb, "the interactive preview switch", lambda n: (n.get("text") or "").strip() == labels["interact"], clickable=True)
    tap(adb, chip)
    find_node(adb, "the running script's icon on the interactive preview switch", lambda n: (n.get("content-desc") or "").strip() == labels["running"])
    find_node(adb, "the template text drawn by the running script", lambda n: (n.get("text") or "").strip() == TEMPLATE_TEXT)
    problems = [n.get("text") for n in ui_tree(adb).iter("node")
                if (n.get("text") or "").startswith(labels["stopped"]) or (n.get("text") or "").startswith(labels["error"])]
    if problems:
        raise SystemExit(f"the preview reported a problem: {problems}")
    chip, _ = find_node(adb, "the interactive preview switch", lambda n: (n.get("text") or "").strip() == labels["interact"], clickable=True)
    tap(adb, chip)
    wait_for("the running script's icon to go", lambda: all((n.get("content-desc") or "").strip() != labels["running"] for n in ui_tree(adb).iter("node")))


def delete_project(adb: Adb, labels: dict[str, str], file_name: str) -> None:
    """Delete a project through its row on the designer home and wait until the row is gone."""

    def delete_button():
        root = ui_tree(adb)
        parents = {child: parent for parent in root.iter() for child in parent}
        for node in root.iter("node"):
            if node.get("package") == PACKAGE and (node.get("text") or "").strip() == file_name:
                row = node
                while row is not None and row.get("clickable") != "true":
                    row = parents.get(row)
                if row is None:
                    continue
                for candidate in row.iter("node"):
                    if (candidate.get("content-desc") or "").strip() == labels["delete"]:
                        return candidate
        return None

    tap(adb, wait_for(f"the delete button of {file_name}", delete_button))
    confirm, _ = find_node(adb, "the delete confirmation", lambda n: (n.get("text") or "").strip() == labels["delete"], clickable=True)
    tap(adb, confirm)
    wait_for(f"{file_name} to leave the project list", lambda: all((n.get("text") or "").strip() != file_name for n in ui_tree(adb).iter("node")))


if __name__ == "__main__":
    try:
        main()
    except subprocess.TimeoutExpired as error:
        sys.exit(f"adb timed out: {error}")
