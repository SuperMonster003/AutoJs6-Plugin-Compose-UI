#!/usr/bin/env python3
"""Copy the indexed Compose examples to an explicitly selected host checkout.

The plugin build stays independent of sibling repositories. The host discovers
sample directories dynamically; its copy of this manifest lives under indices/.
"""

import argparse
import json
from pathlib import Path
import re
import sys


def sync(host: Path, check: bool) -> list[str]:
    source = Path(__file__).resolve().parents[1] / "app/src/main/assets/examples"
    manifest = source / "index.json"
    data = json.loads(manifest.read_text(encoding="utf-8"))
    if data.get("schemaVersion") != 1 or data.get("hostSamplePath") != "sample/Compose UI":
        raise ValueError("Unsupported example manifest or host sample path")
    entries = data["examples"]
    names = [entry["file"] for entry in entries]
    if len(names) != len(set(names)) or any(not re.fullmatch(r"[a-z][a-z0-9-]*\.js", name) for name in names):
        raise ValueError("Example file names must be unique plain JavaScript file names")
    if set(names) != {path.name for path in source.glob("*.js")}:
        raise ValueError("index.json must enumerate every example exactly once")
    for entry in entries:
        text = (source / entry["file"]).read_text(encoding="utf-8")
        ui = bool(re.match(r"\s*(['\"])ui\1\s*;", text))
        if entry["mode"] not in ("ui", "normal") or ui != (entry["mode"] == "ui"):
            raise ValueError(f"Script directive and manifest mode differ: {entry['file']}")
        if entry["requiresOverlay"] != (entry["mode"] == "normal"):
            raise ValueError(f"Unexpected overlay requirement: {entry['file']}")
    assets = host.resolve() / "app/src/main/assets-app"
    if not (assets / "sample").is_dir() or not (assets / "indices").is_dir():
        raise ValueError("--host must name an AutoJs6 checkout with sample and indices assets")
    destination = assets / data["hostSamplePath"]
    extras = {path.name for path in destination.glob("*.js")} - set(names)
    if extras:
        raise ValueError(f"Unindexed host scripts require review: {sorted(extras)}")
    pairs = [(source / name, destination / name) for name in names]
    pairs.append((manifest, assets / "indices/compose-examples.json"))
    changed = []
    for original, target in pairs:
        content = original.read_bytes()
        if target.is_file() and target.read_bytes() == content:
            continue
        changed.append(str(target.relative_to(host.resolve())))
        if not check:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(content)
    return changed


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", required=True, type=Path, help="Explicit AutoJs6 checkout path")
    parser.add_argument("--check", action="store_true", help="Report drift without writing files")
    args = parser.parse_args()
    try:
        changed = sync(args.host, args.check)
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"Compose examples: {error}", file=sys.stderr)
        return 1
    if args.check and changed:
        print("Compose example copies differ:\n" + "\n".join(changed), file=sys.stderr)
        return 1
    print(f"Compose examples: {'checked' if args.check else 'synchronized'} 5 scripts and 1 manifest")
    return 0


if __name__ == "__main__":
    sys.exit(main())
