"""Audit DEX class definitions at the P0 host/plugin boundary (not mere string references)."""
import argparse
import struct
from pathlib import Path
from zipfile import ZipFile


def dex_classes(data: bytes) -> tuple[set[str], int]:
    if not data.startswith(b"dex\n"):
        raise ValueError("Expected standard DEX")
    string_count, string_offset = struct.unpack_from("<II", data, 56)
    strings = []
    for index in range(string_count):
        offset = struct.unpack_from("<I", data, string_offset + 4 * index)[0]
        # Skip the ULEB128 UTF-16 length. Descriptors use ASCII, even though other strings use MUTF-8.
        while data[offset] & 128:
            offset += 1
        offset += 1
        strings.append(data[offset:data.index(b"\0", offset)].decode("utf-8", errors="replace"))
    type_count, type_offset = struct.unpack_from("<II", data, 64)
    types = [strings[struct.unpack_from("<I", data, type_offset + 4 * i)[0]] for i in range(type_count)]
    class_count, class_offset = struct.unpack_from("<II", data, 96)
    classes = {types[struct.unpack_from("<I", data, class_offset + 32 * i)[0]] for i in range(class_count)}
    return classes, struct.unpack_from("<I", data, 88)[0]


def verify(apk: Path) -> None:
    classes, method_refs = set(), 0
    with ZipFile(apk) as archive:
        for entry in archive.namelist():
            if entry.startswith("classes") and entry.endswith(".dex"):
                definitions, methods = dex_classes(archive.read(entry))
                classes.update(definitions)
                method_refs += methods
    required = {
        "Lkotlin/Unit;",
        "Lkotlin/coroutines/CoroutineContext;",
        "Lkotlin/coroutines/CoroutineContext$Element;",
        "Lkotlin/jvm/functions/Function1;",
        "Lio/github/supermonster003/autojs6/plugin/compose/ui/renderer/ComposeUiRendererFactoryImpl;",
        # F.5 (roadmap D32): the standalone gallery process needs Compose's AndroidX runtime; the host shadows
        # these parent-first, so they must keep their original names (checked here through their descriptors).
        "Landroidx/activity/ComponentActivity;",
        "Landroidx/lifecycle/LifecycleOwner;",
        "Landroidx/savedstate/SavedStateRegistryOwner;",
        "Lkotlinx/coroutines/CoroutineScope;",
        "Lio/github/supermonster003/autojs6/plugin/compose/ui/app/GalleryActivity;",
    }
    missing = required - classes
    forbidden = {name for name in classes if name.startswith("Lorg/autojs/plugin/compose/api/")}
    # Host-only components stay out: View-system AppCompat, window metrics, emoji and serialization.
    for package in ("appcompat", "window", "emoji2"):
        forbidden.update(name for name in classes if name.startswith(f"Landroidx/{package}/"))
    forbidden.update(name for name in classes if name.startswith("Lkotlinx/serialization/"))
    if missing or forbidden:
        raise ValueError(f"Classpath boundary violated: missing={sorted(missing)}, bundled_host_classes={sorted(forbidden)}")
    print(f"APK_CLASSPATH_OK {apk.name}: bytes={apk.stat().st_size} classes={len(classes)} method_refs={method_refs}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path, nargs="+")
    for file in parser.parse_args().apk:
        verify(file)
