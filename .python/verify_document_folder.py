"""Check on a real device that a content Uri document gets its layout files once its folder is granted (roadmap P11.3).

The grant goes through the system folder picker, which only a person can operate, so the check has three steps:

  prepare  installs the plugin (debug), puts a test project into AutoJs6's scripts directory (`main.js` lays out
           `layout.xml` with `ui.layoutFile`) and prints what to do on the device: open `main.js` in AutoJs6's
           editor, "Open in 3-Block Compose designer", tap "Choose the script's folder...", pick the folder.
  check    after that: the designer recorded the folder, its outline shows the layout file's element, and the
           opt-in DocumentFolderDeviceTest reads `layout.xml` and writes `res/layout/probe.xml` through the
           persisted grant (the file then exists in the folder).
  cleanup  removes the test project and, with --uninstall, the plugin and the test APK.

Every line printed is PASS / FAIL / OK with the key value; no screen text beyond the one row looked for.

usage: verify_document_folder.py --serial <serial> prepare --apk <plugin debug apk> [--scripts <dir>]
       verify_document_folder.py --serial <serial> check --test-apk <androidTest apk> [--scripts <dir>]
       verify_document_folder.py --serial <serial> cleanup [--scripts <dir>] [--uninstall]
"""
import argparse
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_standalone_launch as launch  # noqa: E402

PACKAGE = launch.PACKAGE
RUNNER = f"{PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner"
TEST = f"{PACKAGE}.designer.DocumentFolderDeviceTest"
FOLDER = "designer-folder-check"
ROW = "text #hello"
MAIN = '"ui";\n\nui.layoutFile("layout.xml");\n'
LAYOUT = '<vertical padding="16">\n    <text id="hello" text="Hello from layout.xml"/>\n</vertical>\n'


def push_text(adb: launch.Adb, path: str, text: str) -> None:
    # Through stdin of a shell redirection: no local temporary file and the device writes the bytes as given.
    import subprocess
    subprocess.run(adb.base + ["shell", f"cat > '{path}'"], input=text.encode("utf-8"), check=True, timeout=60)


def prepare(adb: launch.Adb, args) -> int:
    result = adb.install(args.apk)
    print(f"{'OK  ' if 'Success' in result else 'FAIL'} install {args.apk.name}")
    if "Success" not in result:
        return 1
    base = f"{args.scripts}/{FOLDER}"
    adb.shell(f"rm -rf '{base}' && mkdir -p '{base}'")
    push_text(adb, f"{base}/main.js", MAIN)
    push_text(adb, f"{base}/layout.xml", LAYOUT)
    listed = adb.shell(f"ls '{base}'").split()
    ok = sorted(listed) == ["layout.xml", "main.js"]
    print(f"{'OK  ' if ok else 'FAIL'} test project {base} ({', '.join(sorted(listed))})")
    return 0 if ok else 1


def check(adb: launch.Adb, args) -> int:
    failed = 0
    records = adb.shell(f"run-as {PACKAGE} cat files/designer/folders.properties", check=False)
    recorded = FOLDER in records.replace("%2F", "/")
    print(f"{'PASS' if recorded else 'FAIL'} folder recorded for the document")
    failed += not recorded
    shown = any((node.get("text") or "").strip() == ROW for node in launch.ui_tree(adb).iter("node") if node.get("package") == PACKAGE)
    print(f"{'PASS' if shown else 'FAIL'} outline shows the layout file's row {ROW!r}")
    failed += not shown
    install = adb.install(args.test_apk)
    if "Success" not in install:
        print(f"FAIL install {args.test_apk.name}")
        return 1
    output = adb.shell(f"am instrument -w -r -e documentFolderCheck {FOLDER} -e class {TEST} {RUNNER}", check=False, timeout=600)
    passed = "INSTRUMENTATION_STATUS_CODE: 0" in output and "FAILURES" not in output
    print(f"{'PASS' if passed else 'FAIL'} read layout.xml and write res/layout/probe.xml through the grant")
    if not passed:
        stack = output.split("stack=", 1)[1].splitlines()[0] if "stack=" in output else output[-400:]
        print(f"     {stack[:300]}")
    failed += not passed
    time.sleep(1.0)
    probe = adb.shell(f"cat '{args.scripts}/{FOLDER}/res/layout/probe.xml'", check=False).strip()
    print(f"{'PASS' if probe == '<frame/>' else 'FAIL'} probe file in the folder on storage")
    failed += probe != "<frame/>"
    return 1 if failed else 0


def cleanup(adb: launch.Adb, args) -> int:
    adb.shell(f"rm -rf '{args.scripts}/{FOLDER}'", check=False)
    print(f"OK   removed {args.scripts}/{FOLDER}")
    if args.uninstall:
        for package in (f"{PACKAGE}.test", PACKAGE):
            result = adb.run("uninstall", package, check=False)
            print(f"OK   uninstall {package}: {result.strip().splitlines()[-1] if result.strip() else '-'}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--scripts", default="/storage/emulated/0/Scripts", help="AutoJs6's scripts directory on the device")
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("prepare").add_argument("--apk", required=True, type=Path)
    commands.add_parser("check").add_argument("--test-apk", required=True, type=Path)
    commands.add_parser("cleanup").add_argument("--uninstall", action="store_true")
    args = parser.parse_args()
    adb = launch.Adb(args.serial)
    return {"prepare": prepare, "check": check, "cleanup": cleanup}[args.command](adb, args)


if __name__ == "__main__":
    sys.exit(main())
