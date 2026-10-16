"""Run the layout file device test (roadmap P11.3) on one device over adb and print one line per test.

UiLayoutFileDeviceTest covers moving a layout into a file and saving it as a folder project, a new Android XML
file, the zip export / import round trip and, with a host that has the project zip entries (D64), the zip
reaching AutoJs6, which runs and imports it. This script installs the given APKs (optionally an AutoJs6 debug
build first), lets the host write the scripts directory on an emulator (`--grant-storage`: the host's all files
access through appops, never on a phone someone uses) and prints PASS / FAIL / SKIP per test with the first line
of a failure. The exit code is 0 when no test failed.

usage: verify_layout_files.py --serial <serial> --apk <plugin debug apk> --test-apk <androidTest apk>
                              [--host-apk <AutoJs6 debug apk>] [--grant-storage]
"""
import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_standalone_launch as launch  # noqa: E402

PACKAGE = launch.PACKAGE
HOST_PACKAGE = "org.autojs.autojs6"
RUNNER = f"{PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS = f"{PACKAGE}.designer.UiLayoutFileDeviceTest"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--test-apk", required=True, type=Path)
    parser.add_argument("--host-apk", type=Path)
    parser.add_argument("--grant-storage", action="store_true", help="emulators only: let the host write the scripts directory")
    args = parser.parse_args()
    adb = launch.Adb(args.serial)
    for apk in [args.host_apk, args.apk, args.test_apk]:
        if apk is None:
            continue
        result = adb.install(apk)
        print(f"{'OK  ' if 'Success' in result else 'FAIL'} install {apk.name}")
        if "Success" not in result:
            return 1
    if args.grant_storage:
        # Android 11+: all files access; before it, the storage runtime permissions.
        adb.shell(f"appops set {HOST_PACKAGE} MANAGE_EXTERNAL_STORAGE allow", check=False)
        for permission in ("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"):
            adb.shell(f"pm grant {HOST_PACKAGE} android.permission.{permission}", check=False)
    output = adb.shell(f"am instrument -w -r -e class {TEST_CLASS} {RUNNER}", check=False, timeout=1800)
    failed = 0
    ran = 0
    # Each status block names its test and ends with its code: 1 started, 0 passed, -3 skipped, else failed.
    for match in re.finditer(r"INSTRUMENTATION_STATUS: test=(\S+)(?:(?!INSTRUMENTATION_STATUS_CODE).)*?INSTRUMENTATION_STATUS_CODE: (-?\d+)", output, re.S):
        name, code = match.group(1), int(match.group(2))
        if code == 1:
            continue
        ran += 1
        if code == 0:
            print(f"PASS {name}")
        elif code in (-3, -4):
            print(f"SKIP {name}")
        else:
            failed += 1
            stack = re.search(r"INSTRUMENTATION_STATUS: stack=(.*?)(?:\nINSTRUMENTATION_STATUS|$)", match.group(0), re.S)
            lines = stack.group(1).strip().splitlines() if stack else ["see logcat"]
            at = next((line.strip() for line in lines if "UiLayoutFileDeviceTest.kt" in line), "")
            print(f"FAIL {name}: {lines[0][:300]} {at}")
    if ran == 0:
        print("FAIL no test ran: " + output[-1500:])
        return 1
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
