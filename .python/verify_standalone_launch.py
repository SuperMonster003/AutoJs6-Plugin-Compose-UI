"""Open the gallery from its launcher alias in the plugin's own process and walk its screens over adb.

The instrumentation suites run inside an instrumented process that also carries the test APK, so they
cannot prove that the plugin APK alone has every class the gallery and settings screens need (build 42 passed
them and still crashed from the launcher). This check installs the given APK on one device, starts the enabled
launcher alias exactly as a launcher would, opens the first catalog entry (composing its preview) and the
settings screen through the accessibility tree, and fails on any crash, dead process or missing screen.

usage: verify_standalone_launch.py --apk <apk> [--serial <serial>] [--uninstall] [--strings <values/strings.xml>]
"""
import argparse
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ElementTree
from pathlib import Path

PACKAGE = "io.github.supermonster003.autojs6.plugin.compose.ui"
DEFAULT_ALIAS = f"{PACKAGE}/.launcher.AdaptiveAutoIconAlias"
GALLERY = f"{PACKAGE}/.launcher."
SETTINGS = f"{PACKAGE}/.app.SettingsActivity"
UI_DUMP = "/data/local/tmp/compose-ui-standalone-launch.xml"


class Adb:
    def __init__(self, serial: str | None) -> None:
        self.base = ["adb"] + (["-s", serial] if serial else [])

    def run(self, *args: str, check: bool = True, timeout: int = 120) -> str:
        completed = subprocess.run(self.base + list(args), capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        if check and completed.returncode != 0:
            raise SystemExit(f"adb {' '.join(args)} failed ({completed.returncode}):\n{completed.stdout}\n{completed.stderr}")
        return completed.stdout

    def shell(self, command: str, check: bool = True, timeout: int = 120) -> str:
        return self.run("shell", command, check=check, timeout=timeout)


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
        if value:
            return value
        if time.monotonic() > deadline:
            raise SystemExit(f"timed out waiting for {description}")
        time.sleep(interval)


def expand(component: str) -> str:
    package, _, name = component.partition("/")
    return f"{package}/{package + name if name.startswith('.') else name}"


def ui_tree(adb: Adb) -> ElementTree.Element:
    for attempt in range(6):
        output = adb.shell(f"uiautomator dump {UI_DUMP}", check=False, timeout=60)
        if "dumped to" in output:
            xml = adb.shell(f"cat {UI_DUMP}")
            try:
                return ElementTree.fromstring(xml)
            except ElementTree.ParseError:
                pass
        time.sleep(1.0 + attempt)
    raise SystemExit("uiautomator could not dump the accessibility tree")


def tap(adb: Adb, node: ElementTree.Element) -> None:
    left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds", "")))
    adb.shell(f"input tap {(left + right) // 2} {(top + bottom) // 2}")


def find_node(adb: Adb, description: str, predicate, clickable: bool = False) -> tuple[ElementTree.Element, ElementTree.Element]:
    """Return (tap target, matched node) for the first node matching the predicate.

    Compose does not merge a clickable row with the text or icon it contains in the uiautomator tree, so with
    clickable=True the tap target is the nearest ancestor that carries clickable="true"; a match without one
    (a category header, the title) is skipped.
    """
    def search():
        root = ui_tree(adb)
        parents = {child: parent for parent in root.iter() for child in parent}
        for node in root.iter("node"):
            if not predicate(node):
                continue
            if not clickable:
                return node, node
            candidate = node
            while candidate is not None and candidate.get("clickable") != "true":
                candidate = parents.get(candidate)
            if candidate is not None:
                return candidate, node
        return None
    return wait_for(description, search)


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

    api = adb.shell("getprop ro.build.version.sdk").strip()
    install = adb.run("install", "-r", "-t", str(args.apk), check=False, timeout=600)
    if "Success" not in install:
        raise SystemExit(f"install failed:\n{install}")
    adb.shell(f"am force-stop {PACKAGE}", check=False)
    adb.run("logcat", "-c", check=False)

    resolved = adb.shell(f"cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER {PACKAGE}", check=False)
    component = next((line.strip() for line in resolved.splitlines() if line.strip().startswith(PACKAGE + "/")), DEFAULT_ALIAS)
    start = adb.shell(f"am start -W -n {component}", timeout=120)
    if "Status: ok" not in start:
        raise SystemExit(f"am start did not succeed for {component}:\n{start}")

    try:
        gallery_component, pid, entry_label = walk(adb, settings_label, copy_label)
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
    print(f"STANDALONE_LAUNCH_OK api={api} component={gallery_component} pid={pid} entry={entry_label} settings=opened apk={args.apk.name}")


def walk(adb: Adb, settings_label: str, copy_label: str) -> tuple[str, int, str]:
    gallery_component, pid = wait_for("the gallery to be the top Activity", lambda: (lambda t: t if t and t[0].startswith(GALLERY) else None)(top_activity(adb)))
    time.sleep(3.0)
    if "No such" in adb.shell(f"ls -d /proc/{pid}", check=False):
        raise SystemExit(f"the gallery process {pid} died after start")

    # First catalog entry: the first text that sits inside a clickable row (category headers and the title do not).
    entry, label = find_node(adb, "a catalog entry", lambda n: (n.get("text") or "").strip() != "", clickable=True)
    entry_label = label.get("text").strip()
    tap(adb, entry)
    find_node(adb, f"the detail screen of {entry_label} with the copy button", lambda n: (n.get("text") or "").strip() == copy_label)
    adb.shell("input keyevent KEYCODE_BACK")
    settings_action, _ = find_node(adb, "the settings action", lambda n: (n.get("content-desc") or "").strip() == settings_label, clickable=True)
    tap(adb, settings_action)
    wait_for("the settings screen", lambda: (lambda t: t if t and expand(t[0]) == expand(SETTINGS) else None)(top_activity(adb)))
    time.sleep(1.0)
    adb.shell("input keyevent KEYCODE_BACK")
    wait_for("the gallery after leaving settings", lambda: (lambda t: t if t and t[0].startswith(GALLERY) else None)(top_activity(adb)))

    after = top_activity(adb)
    if not after or after[1] != pid:
        raise SystemExit(f"the gallery process changed from {pid} to {after}: it was restarted")
    return gallery_component, pid, entry_label


if __name__ == "__main__":
    try:
        main()
    except subprocess.TimeoutExpired as error:
        sys.exit(f"adb timed out: {error}")
