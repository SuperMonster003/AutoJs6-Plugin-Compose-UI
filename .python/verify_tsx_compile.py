#!/usr/bin/env python3
"""Run TSX scripts through AutoJs6 on one device and report whether TypeScript Engine compiled them (roadmap P11.7).

Each file is pushed to /storage/emulated/0/Scripts/<--dir>/ and started through AutoJs6's run entry
(`RunIntentActivity`, a file Uri); the host compiles it with TypeScript Engine (strict, noUnusedParameters,
noEmitOnError) before it runs. One line per file: PASS when the script ran (`Running [...]` in the host console),
FAIL with the first compiler error otherwise, `?` when nothing was logged in time. Needs AutoJs6 with TypeScript
Engine 0.7.0+ and storage access for the host (on an emulator: --grant-storage). The files stay on the device
unless --cleanup is given; the started script is left with the back key.

Usage:
  python .python/verify_tsx_compile.py --serial emulator-5584 [--grant-storage] [--cleanup] file.tsx [file.tsx ...]
"""
from __future__ import annotations

import argparse
import pathlib
import subprocess
import sys
import time

HOST = "org.autojs.autojs6"
RUN_ENTRY = HOST + "/org.autojs.autojs.external.open.RunIntentActivity"
SCRIPTS = "/storage/emulated/0/Scripts"


def adb(serial: str, *args: str) -> str:
    return subprocess.run(["adb", "-s", serial, *args], capture_output=True, check=False).stdout.decode("utf-8", "replace")


def check(serial: str, remote: str, timeout: float) -> tuple[str, str]:
    adb(serial, "logcat", "-c")
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "text/plain", "-d", "file://" + remote, "-n", RUN_ENTRY)
    deadline = time.monotonic() + timeout
    result = ("?", "nothing logged in time")
    while time.monotonic() < deadline:
        time.sleep(1)
        lines = [line for line in adb(serial, "logcat", "-d", "-s", "GlobalConsole:*").splitlines() if "GlobalConsole" in line]
        failed = next((i for i, line in enumerate(lines) if "TypeScript compilation failed" in line), None)
        if failed is not None:
            errors = [line.split(": ", 1)[-1].strip() for line in lines[failed + 1:] if "error TS" in line]
            result = ("FAIL", errors[0] if errors else "compilation failed")
            break
        if any("Running [" in line for line in lines):
            result = ("PASS", "compiled and running")
            break
    adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--dir", default="tsx-compile-check")
    parser.add_argument("--timeout", type=float, default=20.0)
    parser.add_argument("--grant-storage", action="store_true", help="emulators only: allow the host all files access")
    parser.add_argument("--cleanup", action="store_true", help="remove the pushed files afterwards")
    parser.add_argument("files", nargs="+", type=pathlib.Path)
    args = parser.parse_args()
    if args.grant_storage:
        adb(args.serial, "shell", "appops", "set", HOST, "MANAGE_EXTERNAL_STORAGE", "allow")
    directory = f"{SCRIPTS}/{args.dir}"
    adb(args.serial, "shell", "mkdir", "-p", directory)
    failures = 0
    for file in args.files:
        remote = f"{directory}/{file.name}"
        adb(args.serial, "push", str(file), remote)
        status, detail = check(args.serial, remote, args.timeout)
        failures += status != "PASS"
        print(f"{status:4} {file.name}: {detail}")
    if args.cleanup:
        adb(args.serial, "shell", "rm", "-rf", directory)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
