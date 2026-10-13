"""Regenerates the TSX syntax tree snapshots of the designer's JVM tests (roadmap P9.2 / D42).

The designer reads TSX through the TypeScript Engine parser service. JVM tests cannot bind that service, so
they read snapshots: the TSX sources under app/src/test/resources/tsx and, for each, the tree the
TypeScript Engine's own parser Worker (parser-worker.js with its locked typescript.js) produces, run here under
Node exactly as the service runs it. SNAPSHOTS.txt ties every tree to the SHA-256 of its source, so a stale
tree fails the tests instead of passing silently.

Steps:
  1. copy the host's compose TSX samples into host/ (with --host);
  2. write the generated sources (the TSX form of the 56 gallery scripts and the 5 bundled examples, and the
     canonical form of every hand-written source that maps) with the JVM recorder test;
  3. parse every source into trees/ with Node;
  4. repeat 2 and 3 once, because canonical forms need the trees of their inputs.

Usage:
  py .python/generate_tsx_snapshots.py --engine D:/idea-projects/AutoJs6-Plugin-TypeScript-Engine [--host D:/idea-projects/AutoJs6] [--node node]
"""
import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SNAPSHOTS = ROOT / "app" / "src" / "test" / "resources" / "tsx"
GRADLE_FLAGS = ["-Pautojs.gradle.build.number.auto.increment.enabled=false", "-Pautojs.gradle.build.time.update.enabled=false"]


def run(command, **kwargs):
    print("+", " ".join(str(part) for part in command), flush=True)
    subprocess.run(command, check=True, **kwargs)


def record():
    gradle = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    environment = dict(os.environ, TSX_SNAPSHOTS_RECORD=str(SNAPSHOTS))
    run([str(gradle), ":app:testDebugUnitTest", "--tests", "*TsxSnapshotRecorder", "--rerun", "-q", *GRADLE_FLAGS], cwd=ROOT, env=environment)


def trees(node, worker, compiler, engine):
    run([node, str(ROOT / ".python" / "tsx_trees.js"), str(SNAPSHOTS), str(worker), str(compiler), engine])


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--engine", required=True, type=Path, help="the AutoJs6-Plugin-TypeScript-Engine checkout")
    parser.add_argument("--host", type=Path, help="the AutoJs6 checkout whose compose TSX samples are copied into host/")
    parser.add_argument("--node", default="node")
    args = parser.parse_args()

    worker = args.engine / "app" / "src" / "main" / "assets" / "typescript-parser-r1" / "parser-worker.js"
    compilers = sorted((args.engine / "app" / "build" / "generated" / "typescriptCompilerAssets" / "assets" / "typescript").glob("*/lib/typescript.js"))
    if not worker.is_file():
        sys.exit(f"{worker} is missing: the TypeScript Engine checkout has no parser Worker (0.7.0 or later is needed)")
    if len(compilers) != 1:
        sys.exit("Build the locked TypeScript distribution first: gradlew :app:generateTypeScriptDistribution in " + str(args.engine))
    version = next(line.split("=", 1)[1].strip() for line in (args.engine / "version.properties").read_text().splitlines() if line.startswith("VERSION_NAME="))
    commit = subprocess.run(["git", "rev-parse", "--short=10", "HEAD"], cwd=args.engine, capture_output=True, text=True, check=True).stdout.strip()
    engine = f"TypeScript Engine {version} ({commit})"

    if args.host:
        samples = args.host / "app" / "src" / "androidTest" / "assets" / "compose-tsx"
        target = SNAPSHOTS / "host"
        shutil.rmtree(target, ignore_errors=True)
        target.mkdir(parents=True)
        for sample in sorted(samples.glob("*.tsx")):
            (target / sample.name).write_bytes(sample.read_bytes().replace(b"\r\n", b"\n"))
    for _ in range(2):
        record()
        trees(args.node, worker, compilers[0], engine)


if __name__ == "__main__":
    main()
