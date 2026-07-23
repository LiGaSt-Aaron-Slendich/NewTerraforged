"""
Recompress biome_previews PNGs with palette quantization (visually near-lossless at 144px).
Does not change resolution. Overwrites in place.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/newterraforged/textures/gui/biome_previews"
COLORS = 256


def recompress(path: Path) -> tuple[int, int]:
    before = path.stat().st_size
    im = Image.open(path).convert("RGBA")
    # Composite onto opaque bg so we can use MEDIANCUT (RGBA only allows FASTOCTREE).
    bg = Image.new("RGBA", im.size, (32, 32, 32, 255))
    composed = Image.alpha_composite(bg, im).convert("RGB")
    quantized = composed.quantize(colors=COLORS, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    quantized.save(path, format="PNG", optimize=True, compress_level=9)
    after = path.stat().st_size
    return before, after


def main() -> None:
    files = sorted(OUT.rglob("*.png"))
    total_b = total_a = 0
    for i, p in enumerate(files):
        b, a = recompress(p)
        total_b += b
        total_a += a
        if i < 5 or i % 40 == 0:
            print(f"[{i+1}/{len(files)}] {p.relative_to(OUT)}: {b} -> {a}")
    print(f"DONE files={len(files)} bytes {total_b} -> {total_a} ({100.0 * total_a / max(1, total_b):.1f}%)")


if __name__ == "__main__":
    main()
