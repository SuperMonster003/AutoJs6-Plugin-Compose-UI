"""Audit DEX class definitions and references at the host/plugin boundary (not mere string references).

Definitions (every APK): the Kotlin runtime, the renderer factory, the standalone AndroidX runtime and the
gallery are defined; host contract classes and the host-only component (appcompat) are not bundled.

References (--shrunk, R8 outputs only): every class the APK references is defined in the APK, provided by
the Android platform, or part of the host contract that only the in-host renderer touches. The plugin's own
gallery and settings process has nothing else to load from, and the device tests cannot prove this on their
own: build 42 passed them yet crashed from the launcher on androidx.arch.core.executor.ArchTaskExecutor,
which the instrumentation APK had supplied. Unshrunk debug APKs keep unreachable code and are not checked
this way.
"""
import argparse
import struct
from pathlib import Path
from zipfile import ZipFile

CONTRACT_PREFIX = "Lorg/autojs/plugin/compose/api/"
# Classes the device supplies: the platform (including hidden classes such as android.view.RenderNode that
# Compose reaches on older API levels), the DEX annotation pseudo-classes, and the OEM window extensions that
# androidx.window loads reflectively behind its own guards.
DEVICE_PREFIXES = (
    "Ljava/", "Ljavax/", "Landroid/", "Ldalvik/", "Lcom/android/", "Lorg/apache/http/", "Lorg/json/",
    "Lorg/xml/", "Lorg/xmlpull/", "Lorg/w3c/", "Lsun/", "Llibcore/", "Ljdk/",
    "Landroidx/window/extensions/", "Landroidx/window/sidecar/",
)
REQUIRED = {
    "Lkotlin/Unit;",
    "Lkotlin/coroutines/CoroutineContext;",
    "Lkotlin/coroutines/CoroutineContext$Element;",
    "Lkotlin/jvm/functions/Function1;",
    "Lio/github/supermonster003/autojs6/plugin/compose/ui/renderer/ComposeUiRendererFactoryImpl;",
    # F.5 (roadmap D32): the standalone gallery process needs Compose's AndroidX runtime; the host shadows
    # these parent-first, so they must keep their original names (checked here through their descriptors).
    "Landroidx/activity/ComponentActivity;",
    "Landroidx/arch/core/executor/ArchTaskExecutor;",
    "Landroidx/customview/poolingcontainer/PoolingContainer;",
    "Landroidx/emoji2/text/EmojiCompat;",
    "Landroidx/lifecycle/LifecycleOwner;",
    "Landroidx/lifecycle/LiveData;",
    "Landroidx/savedstate/SavedStateRegistryOwner;",
    "Landroidx/window/layout/WindowMetricsCalculator;",
    "Lkotlinx/coroutines/CoroutineScope;",
    "Lkotlinx/serialization/KSerializer;",
    "Lio/github/supermonster003/autojs6/plugin/compose/ui/app/GalleryActivity;",
}
# The only host-only component stays out: View-system AppCompat, which nothing in the plugin references.
FORBIDDEN_PREFIXES = ("Landroidx/appcompat/", CONTRACT_PREFIX)
FORBIDDEN_CLASSES: set[str] = set()


def dex_tables(data: bytes) -> tuple[list[str], set[str], int]:
    """Return (referenced type descriptors, defined class descriptors, method reference count)."""
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
    return types, classes, struct.unpack_from("<I", data, 88)[0]


def verify(apk: Path, shrunk: bool) -> None:
    referenced, classes, method_refs = set(), set(), 0
    with ZipFile(apk) as archive:
        for entry in archive.namelist():
            if entry.startswith("classes") and entry.endswith(".dex"):
                types, definitions, methods = dex_tables(archive.read(entry))
                referenced.update(t.lstrip("[") for t in types if t.lstrip("[").startswith("L"))
                classes.update(definitions)
                method_refs += methods
    missing = REQUIRED - classes
    forbidden = {name for name in classes if name.startswith(FORBIDDEN_PREFIXES) or name in FORBIDDEN_CLASSES}
    if missing or forbidden:
        raise ValueError(f"Classpath boundary violated: missing={sorted(missing)}, bundled_host_classes={sorted(forbidden)}")
    unresolved = sorted(
        name for name in referenced
        if name not in classes and not name.startswith(DEVICE_PREFIXES) and not name.startswith(CONTRACT_PREFIX)
    )
    contract = sum(1 for name in referenced if name.startswith(CONTRACT_PREFIX))
    if shrunk and unresolved:
        raise ValueError(f"{apk.name} references {len(unresolved)} classes that the plugin process cannot load: {unresolved}")
    detail = f" unresolved_outside_contract={len(unresolved)}" if shrunk else ""
    print(f"APK_CLASSPATH_OK {apk.name}: bytes={apk.stat().st_size} classes={len(classes)} method_refs={method_refs} contract_refs={contract}{detail}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("apk", type=Path, nargs="+")
    parser.add_argument("--shrunk", action="store_true", help="the APKs are R8 outputs: also require every referenced class to be loadable in the plugin process")
    args = parser.parse_args()
    for file in args.apk:
        verify(file, args.shrunk)
