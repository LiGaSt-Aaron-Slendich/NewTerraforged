#!/usr/bin/env python3
"""Generate EGF tab (32) and cave climate/system/gen icons (16)."""
from __future__ import annotations

import math
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow", "-q"])
    from PIL import Image, ImageDraw

OUT = Path("src/main/resources/assets/newterraforged/textures/gui/biome_rules")
OUT.mkdir(parents=True, exist_ok=True)
TRANSPARENT = (0, 0, 0, 0)


def new_img(size: int):
    return Image.new("RGBA", (size, size), TRANSPARENT)


def save(im: Image.Image, name: str):
    path = OUT / f"{name}.png"
    im.save(path)
    print("wrote", path, im.size)


def tab_surface():
    im = new_img(32)
    d = ImageDraw.Draw(im)
    for y in range(0, 16):
        d.line([(0, y), (31, y)], fill=(120, 170, 220, 255))
    for y in range(16, 32):
        d.line([(0, y), (31, y)], fill=(70, 120, 55, 255))
    d.line([(0, 17), (4, 16), (8, 18), (12, 15), (16, 17), (20, 14), (24, 16), (28, 15), (31, 17)],
           fill=(40, 70, 30, 255), width=2)
    d.ellipse([20, 3, 28, 11], fill=(255, 220, 60, 255), outline=(255, 180, 20, 255))
    for ang in range(0, 360, 45):
        rad = math.radians(ang)
        x0 = 24 + int(5 * math.cos(rad))
        y0 = 7 + int(5 * math.sin(rad))
        x1 = 24 + int(8 * math.cos(rad))
        y1 = 7 + int(8 * math.sin(rad))
        d.line([(x0, y0), (x1, y1)], fill=(255, 200, 40, 255), width=1)
    save(im, "tab_surface_biomes")


def tab_cave():
    im = new_img(32)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 31, 31], fill=(28, 24, 22, 255))
    d.line([(0, 8), (6, 7), (12, 9), (18, 6), (24, 8), (31, 7)], fill=(90, 80, 70, 255), width=2)
    d.line([(0, 24), (5, 23), (11, 25), (17, 22), (23, 24), (31, 23)], fill=(90, 80, 70, 255), width=2)
    d.polygon([(16, 8), (14, 8), (15, 14)], fill=(140, 130, 120, 255))
    for x, h in [(6, 4), (11, 6), (18, 5), (25, 7)]:
        d.polygon([(x, 24), (x - 2, 24), (x - 1, 24 - h)], fill=(150, 140, 125, 255))
    save(im, "tab_cave_biomes")


def cave_base(accent, accent2=None):
    im = new_img(16)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], fill=(22, 18, 16, 255))
    d.line([(1, 4), (14, 4)], fill=(70, 60, 55, 255), width=1)
    d.line([(1, 12), (14, 12)], fill=(70, 60, 55, 255), width=1)
    d.polygon([(8, 4), (7, 4), (7, 8)], fill=accent)
    for x, h in [(3, 2), (6, 3), (10, 2), (13, 3)]:
        d.polygon([(x, 12), (x - 1, 12), (x, 12 - h)], fill=accent2 or accent)
    return im, d


def climate_icons():
    specs = {
        "cave_climate_hot": ((220, 90, 40, 255), (255, 140, 50, 255)),
        "cave_climate_volcanic": ((180, 40, 20, 255), (80, 80, 80, 255)),
        "cave_climate_dry": ((210, 180, 90, 255), (190, 150, 70, 255)),
        "cave_climate_humid": ((60, 160, 90, 255), (40, 120, 160, 255)),
        "cave_climate_cold": ((140, 200, 255, 255), (200, 230, 255, 255)),
        "cave_climate_normal": ((160, 150, 140, 255), (130, 120, 110, 255)),
    }
    for name, (a, b) in specs.items():
        im, d = cave_base(a, b)
        if "volcanic" in name:
            d.ellipse([6, 7, 10, 11], fill=(255, 120, 20, 255))
        if "hot" in name:
            d.point((4, 6), fill=(255, 200, 80, 255))
            d.point((11, 6), fill=(255, 180, 60, 255))
        if "cold" in name:
            d.line([(3, 6), (5, 7)], fill=(220, 240, 255, 255))
            d.line([(11, 5), (13, 6)], fill=(220, 240, 255, 255))
        if "humid" in name:
            d.ellipse([4, 6, 6, 8], fill=(80, 180, 220, 255))
        save(im, name)


def system_icons():
    im, d = cave_base((150, 140, 130, 255))
    d.rectangle([2, 5, 13, 10], outline=(255, 230, 160, 255))
    d.ellipse([6, 6, 10, 9], fill=(255, 230, 160, 255))
    save(im, "cave_system_mega")

    im, d = cave_base((150, 140, 130, 255))
    d.rectangle([1, 4, 14, 11], outline=(255, 210, 100, 255))
    d.rectangle([3, 6, 12, 9], outline=(255, 180, 60, 255))
    save(im, "cave_system_giga")

    im, d = cave_base((120, 160, 200, 255))
    for x in (4, 8, 12):
        d.line([(x, 5), (x, 11)], fill=(100, 180, 255, 255), width=1)
    save(im, "cave_system_synapse")


def gen_type_icons():
    im = new_img(16)
    d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 13, 13], fill=(70, 130, 70, 255), outline=(140, 200, 120, 255))
    d.rectangle([5, 5, 10, 10], fill=(180, 230, 160, 255))
    save(im, "cave_gen_primary")

    im = new_img(16)
    d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 7, 13], fill=(60, 90, 140, 255))
    d.rectangle([8, 2, 13, 13], fill=(100, 140, 190, 255))
    d.rectangle([2, 2, 13, 13], outline=(160, 190, 230, 255))
    save(im, "cave_gen_transition")

    im = new_img(16)
    d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 13, 13], fill=(80, 60, 100, 255), outline=(180, 140, 200, 255))
    d.ellipse([5, 5, 10, 10], fill=(220, 170, 240, 255))
    save(im, "cave_gen_patch")


if __name__ == "__main__":
    tab_surface()
    tab_cave()
    climate_icons()
    system_icons()
    gen_type_icons()
    print("done")
