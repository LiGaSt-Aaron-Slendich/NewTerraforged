"""
Fetch biome screenshots from GitHub sources and store as native 32x32 PNG previews.

Sources:
  - BYG: Potion-Studios/BYG .../textures/biome_previews/*.png (crop+downscale, not raw HD)
  - Regions Unexplored: gist catter1/... (vanilla screenshot per biome)

Does NOT ship 1280x720 originals into the jar.
"""
from __future__ import annotations

import io
import json
import re
import time
import urllib.request
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent if Path(__file__).name == "fetch_biome_previews.py" else Path.cwd()
OUT = ROOT / "src/main/resources/assets/newterraforged/textures/gui/biome_previews"
SIZE = 144  # ~144p square; never ship raw HD screenshots
UA = "NewTerraForged-biome-preview-fetcher/1.0"


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def to_preview(data: bytes) -> Image.Image:
    """Center-crop to square then resize to SIZE — never keep HD in the asset tree."""
    im = Image.open(io.BytesIO(data)).convert("RGBA")
    w, h = im.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    im = im.crop((left, top, left + side, top + side))
    return im.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def save_preview(img: Image.Image, path: Path) -> int:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="PNG", optimize=True, compress_level=9)
    return path.stat().st_size


def fetch_byg() -> dict:
    """Download BYG biome_previews from raw.githubusercontent (path list file or API)."""
    list_file = ROOT / "tmp_byg_preview_paths.txt"
    if list_file.is_file():
        paths = [ln.strip() for ln in list_file.read_text(encoding="utf-8").splitlines() if ln.strip().endswith(".png")]
    else:
        api = "https://api.github.com/repos/Potion-Studios/BYG/git/trees/1.18.X?recursive=1"
        tree = json.loads(fetch(api))
        paths = [
            e["path"]
            for e in tree.get("tree", [])
            if e.get("path", "").startswith("Common/src/main/resources/assets/byg/textures/biome_previews/")
            and e["path"].endswith(".png")
        ]
    results = {}
    dest_root = OUT / "byg"
    for i, path in enumerate(paths):
        name = Path(path).stem
        url = f"https://raw.githubusercontent.com/Potion-Studios/BYG/1.18.X/{path}"
        try:
            raw = fetch(url)
            img = to_preview(raw)
            nbytes = save_preview(img, dest_root / f"{name}.png")
            results[f"byg:{name}"] = nbytes
            print(f"[BYG {i+1}/{len(paths)}] {name}: {len(raw)} -> {nbytes} B")
        except Exception as e:
            print(f"[BYG FAIL] {name}: {e}")
        time.sleep(0.05)
    return results


def fetch_regions_unexplored() -> dict:
    gist = fetch(
        "https://gist.githubusercontent.com/catter1/1f90b77263e3746c20c1fd93a7393f69/raw"
    ).decode("utf-8", errors="replace")
    # Sections: ## Title\n\n`regions_unexplored:id`\n...\n![...](url)\n![...](url)
    pattern = re.compile(
        r"`(regions_unexplored:[a-z0-9_]+)`\s*\n(?:.*?\n)*?!"
        r"\[[^\]]*\]\((https://user-images\.githubusercontent\.com/[^)]+\.png)\)",
        re.IGNORECASE,
    )
    matches = pattern.findall(gist)
    results = {}
    dest_root = OUT / "regions_unexplored"
    for i, (biome_id, url) in enumerate(matches):
        name = biome_id.split(":", 1)[1]
        try:
            raw = fetch(url)
            img = to_preview(raw)
            nbytes = save_preview(img, dest_root / f"{name}.png")
            results[biome_id] = nbytes
            print(f"[RU {i+1}/{len(matches)}] {name}: {len(raw)} -> {nbytes} B")
        except Exception as e:
            print(f"[RU FAIL] {name}: {e}")
        time.sleep(0.08)
    return results


def write_attribution(byg: dict, ru: dict) -> None:
    text = f"""Biome preview sources (compressed to {SIZE}x{SIZE} PNG; originals not shipped)

BYG ({len(byg)}):
  https://github.com/Potion-Studios/BYG
  path: Common/.../assets/byg/textures/biome_previews/
  branch: 1.18.X
  License: see BYG repository

Regions Unexplored ({len(ru)}):
  https://gist.github.com/catter1/1f90b77263e3746c20c1fd93a7393f69
  Vanilla (non-shader) screenshot per biome, center-cropped + downscaled
"""
    (OUT / "SOURCES.txt").write_text(text, encoding="utf-8")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    print("=== BYG ===")
    byg = fetch_byg()
    print("=== Regions Unexplored ===")
    ru = fetch_regions_unexplored()
    write_attribution(byg, ru)
    total = sum(p.stat().st_size for p in OUT.rglob("*.png"))
    print(f"DONE byg={len(byg)} ru={len(ru)} total_png_bytes={total}")


if __name__ == "__main__":
    main()
