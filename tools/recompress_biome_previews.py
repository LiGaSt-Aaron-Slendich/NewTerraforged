"""
Stronger biome-preview compression without shipping HD.

Pipeline (keeps 144x144):
  1) RGB FASTOCTREE palette (default 128 colors) — much smaller than MEDIANCUT@256
  2) PNG optimize compress_level=9
  3) oxipng lossless recompress (level 4–6)

Quality: at 144px UI icons, 128-color octree is usually near-indistinguishable from 256-color median.
"""
from __future__ import annotations

import io
from pathlib import Path

from PIL import Image

try:
    import oxipng
except ImportError:  # pragma: no cover
    oxipng = None

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/newterraforged/textures/gui/biome_previews"
SIZE = 144
COLORS = 128  # sweet spot for 144px screenshots


def recompress(path: Path, colors: int = COLORS) -> tuple[int, int]:
    before = path.stat().st_size
    im = Image.open(path).convert("RGBA")
    # Opaque composite so octree can run (and MC blit ignores alpha for these icons).
    bg = Image.new("RGBA", im.size, (32, 32, 32, 255))
    rgb = Image.alpha_composite(bg, im).convert("RGB")
    if rgb.size != (SIZE, SIZE):
        rgb = rgb.resize((SIZE, SIZE), Image.Resampling.LANCZOS)

    quantized = rgb.quantize(
        colors=colors,
        method=Image.Quantize.FASTOCTREE,
        dither=Image.Dither.NONE,
    )
    buf = io.BytesIO()
    quantized.save(buf, format="PNG", optimize=True, compress_level=9)
    data = buf.getvalue()

    if oxipng is not None:
        try:
            data = oxipng.optimize_from_memory(
                data,
                level=6,
                strip=oxipng.StripChunks.safe(),
            )
        except Exception:
            pass

    path.write_bytes(data)
    after = path.stat().st_size
    return before, after


def main() -> None:
    files = sorted(OUT.rglob("*.png"))
    total_b = total_a = 0
    for i, p in enumerate(files):
        b, a = recompress(p)
        total_b += b
        total_a += a
        if i < 8 or i % 80 == 0 or i + 1 == len(files):
            print(f"[{i+1}/{len(files)}] {p.relative_to(OUT)}: {b} -> {a}")
    pct = 100.0 * total_a / max(1, total_b)
    print(
        f"DONE files={len(files)} bytes {total_b} -> {total_a} "
        f"({pct:.1f}%, saved {(total_b - total_a) / 1e6:.2f} MB)"
    )


if __name__ == "__main__":
    main()
