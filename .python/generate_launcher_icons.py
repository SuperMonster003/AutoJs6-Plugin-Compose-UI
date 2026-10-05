"""Generate the transparent launcher icons of Compose UI from the black-and-white source glyph.

The plugin has no launcher entry (roadmap D8), so only the basic `mipmap/ic_launcher.png` series is
produced: the day glyph for light surfaces and the night glyph the README picture element switches to.
The retained source alpha is the artwork; colors and output geometry are generated, never inferred from
antialiased source RGB (AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md, sections 2 and 4).
Run with --check to verify without writes.
"""

from __future__ import annotations

import argparse
import io
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
# Temporary glyph (roadmap Q6): replaced by the maintainer's two source PNGs before the 1.0.0 gate.
SOURCE = ROOT / ".python/icons/compose-ui-ic-launcher-light.png"
SIZE = 432
SCALE = 4
UI_GLYPH = 0.66
DAY_GLYPH = (0x27, 0x27, 0x27)
NIGHT_GLYPH = (0xD8, 0xD8, 0xD8)


def source_alpha() -> Image.Image:
    alpha = Image.open(SOURCE).convert("RGBA").getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError("Icon source has no visible artwork")
    return alpha.crop(bounds)


def render(alpha: Image.Image, ratio: float, color: tuple[int, int, int]) -> Image.Image:
    size = SIZE * SCALE
    canvas = Image.new("L", (size, size), 0)
    width = round(size * ratio)
    height = max(1, round(width * alpha.height / alpha.width))
    if height > size:
        height = size
        width = max(1, round(height * alpha.width / alpha.height))
    scaled_alpha = alpha.resize((width, height), Image.Resampling.LANCZOS)
    canvas.paste(scaled_alpha, ((size - width) // 2, (size - height) // 2))
    # Resize only alpha for transparent artwork: premultiplied RGBA resampling can change the
    # foreground RGB by one level, including at fully opaque pixels.
    result = Image.new("RGBA", (SIZE, SIZE), (*color, 255))
    result.putalpha(canvas.resize((SIZE, SIZE), Image.Resampling.LANCZOS))
    return result


def generated_files() -> dict[Path, bytes]:
    alpha = source_alpha()
    images = {
        "mipmap/ic_launcher.png": render(alpha, UI_GLYPH, DAY_GLYPH),
        "mipmap-night/ic_launcher.png": render(alpha, UI_GLYPH, NIGHT_GLYPH),
    }
    result = {}
    for name, image in images.items():
        output = io.BytesIO()
        image.save(output, format="PNG", optimize=True)
        result[RES / name] = output.getvalue()
    return result


def obsolete_files() -> list[Path]:
    # A same-named adaptive XML or round variant would override the transparent bitmap; never remove
    # arbitrary files or directories.
    candidates = []
    for directory in RES.glob("mipmap*"):
        for name in ("ic_launcher.xml", "ic_launcher_round.xml", "ic_launcher_round.png", "ic_launcher_foreground.png"):
            candidates.append(directory / name)
    return [path for path in candidates if path.is_file()]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check all generated resources without changing files")
    options = parser.parse_args()
    outputs = generated_files()
    stale = [path for path, expected in outputs.items() if not path.is_file() or path.read_bytes() != expected]
    obsolete = obsolete_files()
    if options.check:
        if stale or obsolete:
            raise SystemExit("Stale icon resources: " + ", ".join(str(p.relative_to(ROOT)) for p in stale + obsolete))
        print(f"Verified {len(outputs)} icon resources")
        return
    for path in obsolete:
        if not path.resolve().is_relative_to(RES.resolve()):
            raise ValueError("Icon output escaped resource directory")
        path.unlink()
    for path, data in outputs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        print(f"Generated {path.relative_to(ROOT).as_posix()}")


# AutoJs6 Icon Studio: committed recipe entry point
from pathlib import Path as _IconStudioPath
if __name__ == "__main__" and (_IconStudioPath(__file__).resolve().parents[1] / ".icons/recipe.json").is_file():
    from icon_studio_runtime import main as icon_studio_main
    raise SystemExit(icon_studio_main(_IconStudioPath(__file__).resolve().parents[1]))

if __name__ == "__main__":
    main()
