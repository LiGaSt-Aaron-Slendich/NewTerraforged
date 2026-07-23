"""Fetch & compress biome previews for Biomes O' Plenty + Wythers (WWOO/WWEE)."""
from __future__ import annotations

import io
import json
import re
import time
import urllib.request
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/newterraforged/textures/gui/biome_previews"
SIZE = 144
UA = {"User-Agent": "NewTerraForged-preview-fetcher/1.1", "Accept": "*/*"}
MODS = Path(r"C:\curseforge\minecraft\Instances\TerraforgedTest\mods")
COLORS = 256


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def fetch_json(url: str):
    return json.loads(fetch(url).decode("utf-8", errors="replace"))


def to_preview(data: bytes) -> Image.Image:
    im = Image.open(io.BytesIO(data)).convert("RGBA")
    w, h = im.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    im = im.crop((left, top, left + side, top + side))
    return im.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def save_preview(img: Image.Image, path: Path) -> int:
    path.parent.mkdir(parents=True, exist_ok=True)
    bg = Image.new("RGBA", img.size, (32, 32, 32, 255))
    composed = Image.alpha_composite(bg, img.convert("RGBA")).convert("RGB")
    quantized = composed.quantize(colors=COLORS, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    quantized.save(path, format="PNG", optimize=True, compress_level=9)
    return path.stat().st_size


def title_to_ids(title: str) -> list[str]:
    """'Lavender Field and Dune Beach' -> lavender_field, dune_beach."""
    if not title:
        return []
    parts = re.split(r"\s*(?:,|/|&| and )\s*", title, flags=re.I)
    out = []
    for p in parts:
        p = p.strip()
        if not p or p.lower() in {"none", "n/a"}:
            continue
        slug = re.sub(r"[^a-z0-9]+", "_", p.lower()).strip("_")
        if slug:
            out.append(slug)
    return out


def extract_bop_biome_ids() -> list[str]:
    jar = MODS / "BiomesOPlenty-1.18.2-16.0.0.134.jar"
    with zipfile.ZipFile(jar) as z:
        data = z.read("biomesoplenty/api/biome/BOPBiomes.class")
    # Constant pool UTF8 strings that look like biome paths
    ids = sorted(
        {
            m.decode("ascii")
            for m in re.findall(rb"[a-z][a-z0-9_]{2,48}", data)
            if b"_" in m or m
            in {
                b"bayou",
                b"bog",
                b"crag",
                b"marsh",
                b"pasture",
                b"prairie",
                b"tundra",
                b"tropics",
                b"orchard",
                b"volcano",
                b"muskeg",
                b"wetland",
            }
        }
    )
    noise = {
        "java",
        "lang",
        "util",
        "this",
        "void",
        "true",
        "false",
        "null",
        "code",
        "init",
        "register",
        "biome",
        "biomes",
        "overworld",
        "nether",
        "forge",
        "common",
        "minecraft",
        "holder",
        "resource",
        "location",
        "create",
        "builder",
        "config",
        "spawn",
        "settings",
        "generation",
        "climate",
        "temperature",
        "humidity",
        "precipitation",
        "category",
        "registry",
        "event",
        "bus",
        "codec",
        "optional",
        "supplier",
        "function",
        "integer",
        "string",
        "object",
        "class",
        "field",
        "method",
        "static",
        "final",
        "public",
        "private",
        "protected",
        "override",
        "return",
        "throws",
        "exception",
        "biomesoplenty",
        "bopbiomes",
        "modbiomes",
        "the_end",
        "deep_dark",
    }
    return [i for i in ids if i not in noise and not i.startswith("get") and not i.startswith("set")]


def extract_wythers_ids() -> list[str]:
    jar = MODS / "WWOO-FORGE-2.6.4.jar"
    with zipfile.ZipFile(jar) as z:
        return sorted(
            Path(e).stem
            for e in z.namelist()
            if re.match(r"data/wythers/worldgen/biome/[^/]+\.json$", e)
        )


def fandom_page_image(title: str) -> str | None:
    # MediaWiki pageimages
    q = urllib.parse.quote(title.replace(" ", "_"))
    url = (
        "https://biomesoplenty.fandom.com/api.php?action=query&prop=pageimages"
        f"&titles={q}&pithumbsize=800&format=json"
    )
    try:
        data = fetch_json(url)
        pages = data.get("query", {}).get("pages", {})
        for page in pages.values():
            thumb = page.get("thumbnail", {}).get("source")
            if thumb:
                return thumb
            # original
            oi = page.get("original", {}).get("source")
            if oi:
                return oi
    except Exception:
        return None
    return None


def fetch_bop() -> dict:
    import urllib.parse  # noqa: F401 — used by fandom_page_image via module

    results = {}
    dest = OUT / "biomesoplenty"
    dest.mkdir(parents=True, exist_ok=True)

    # 1) Modrinth gallery (titled)
    proj = fetch_json("https://api.modrinth.com/v2/project/biomes-o-plenty")
    for g in proj.get("gallery") or []:
        title = g.get("title") or ""
        url = g.get("url") or ""
        # Prefer full-size when Modrinth serves a _350 thumbnail variant.
        if "_350." in url:
            full = re.sub(r"_350(\.[a-zA-Z0-9]+)$", r"\1", url)
        else:
            full = url
        ids = title_to_ids(title)
        if not ids or not url:
            continue
        raw = None
        for try_url in (full, url):
            try:
                raw = fetch(try_url)
                break
            except Exception:
                continue
        if raw is None:
            print(f"[BOP gallery FAIL] {title}: download failed")
            continue
        try:
            img = to_preview(raw)
            for bid in ids:
                nbytes = save_preview(img, dest / f"{bid}.png")
                results[f"biomesoplenty:{bid}"] = nbytes
                print(f"[BOP gallery] {bid}: {nbytes} B  ({title})")
        except Exception as e:
            print(f"[BOP gallery FAIL] {title}: {e}")
        time.sleep(0.05)

    # 2) Fandom category pages for remaining known IDs
    known = set(extract_bop_biome_ids())
    # also expand from gallery-written files
    known |= {p.stem for p in dest.glob("*.png")}

    # Category members for page titles
    cont = None
    titles = []
    while True:
        url = (
            "https://biomesoplenty.fandom.com/api.php?action=query&list=categorymembers"
            "&cmtitle=Category:Biomes&cmlimit=500&format=json"
        )
        if cont:
            url += f"&cmcontinue={urllib.parse.quote(cont)}"
        data = fetch_json(url)
        titles.extend(m["title"] for m in data.get("query", {}).get("categorymembers", []))
        cont = data.get("continue", {}).get("cmcontinue")
        if not cont:
            break
        time.sleep(0.05)

    # Map Title -> snake_id
    for title in titles:
        if title.startswith("Category:") or title.startswith("Template:") or title.startswith("Biomes "):
            continue
        bid = re.sub(r"[^a-z0-9]+", "_", title.lower()).strip("_")
        path = dest / f"{bid}.png"
        if path.is_file():
            continue
        img_url = fandom_page_image(title)
        if not img_url:
            print(f"[BOP fandom MISS] {title}")
            continue
        try:
            raw = fetch(img_url)
            img = to_preview(raw)
            nbytes = save_preview(img, path)
            results[f"biomesoplenty:{bid}"] = nbytes
            print(f"[BOP fandom] {bid}: {nbytes} B")
        except Exception as e:
            print(f"[BOP fandom FAIL] {title}: {e}")
        time.sleep(0.08)

    return results


def copy_or_fetch_wythers() -> dict:
    """Fill wythers/ + aliases for WWEE using WWOO biome ids + shared name packs."""
    results = {}
    dests = [OUT / "wythers"]
    for d in dests:
        d.mkdir(parents=True, exist_ok=True)

    ids = extract_wythers_ids()
    print(f"wythers biomes: {len(ids)}")

    # Name-matched copies from existing packs
    search_roots = [
        OUT / "byg",
        OUT / "regions_unexplored",
        OUT / "biomesoplenty",
        OUT,  # vanilla-ish root pngs
    ]

    def find_source(name: str) -> Path | None:
        for root in search_roots:
            p = root / f"{name}.png"
            if p.is_file():
                return p
        # token fallback: last meaningful token
        parts = name.split("_")
        for token in reversed(parts):
            if token in {"the", "a", "of", "and", "old", "new", "deep", "high", "low"}:
                continue
            for root in search_roots:
                # exact token file
                p = root / f"{token}.png"
                if p.is_file():
                    return p
        # compound heuristics
        heuristics = [
            ("swamp", "swamp"),
            ("marsh", "swamp"),
            ("bog", "swamp"),
            ("bayou", "swamp"),
            ("mangrove", "swamp"),
            ("desert", "desert"),
            ("dune", "desert"),
            ("badlands", "badlands"),
            ("canyon", "badlands"),
            ("jungle", "jungle"),
            ("rainforest", "jungle"),
            ("taiga", "taiga"),
            ("boreal", "taiga"),
            ("snow", "snowy_plains"),
            ("frozen", "snowy_plains"),
            ("tundra", "snowy_plains"),
            ("ice", "ice_spikes"),
            ("ocean", "ocean"),
            ("beach", "beach"),
            ("shore", "beach"),
            ("river", "river"),
            ("mountain", "jagged_peaks"),
            ("peak", "jagged_peaks"),
            ("crag", "jagged_peaks"),
            ("forest", "forest"),
            ("wood", "forest"),
            ("grove", "grove"),
            ("meadow", "meadow"),
            ("flower", "flower_forest"),
            ("plains", "plains"),
            ("grass", "plains"),
            ("savanna", "savanna"),
            ("mushroom", "mushroom_fields"),
        ]
        for needle, file in heuristics:
            if needle in name:
                p = OUT / f"{file}.png"
                if p.is_file():
                    return p
        return None

    # Modrinth gallery images as extras (assign to unmatched showcase names by index — skip untitled bulk)
    gallery = fetch_json("https://api.modrinth.com/v2/project/expanded-ecosphere").get("gallery") or []
    gallery_imgs = []
    for g in gallery:
        url = (g.get("url") or "").replace("_350.webp", ".webp")
        if not url:
            continue
        try:
            gallery_imgs.append(to_preview(fetch(url)))
            print(f"[WWEE gallery] loaded {url[-40:]}")
        except Exception as e:
            print(f"[WWEE gallery FAIL] {e}")
        time.sleep(0.05)

    gi = 0
    for name in ids:
        src = find_source(name)
        img = None
        if src is not None:
            img = Image.open(src).convert("RGBA")
            if img.size != (SIZE, SIZE):
                img = img.resize((SIZE, SIZE), Image.Resampling.LANCZOS)
            tag = f"copy:{src.relative_to(OUT)}"
        elif gallery_imgs and False:
            # Disabled: cycling showcase shots onto unrelated biomes is misleading.
            img = gallery_imgs[gi % len(gallery_imgs)]
            gi += 1
            tag = "gallery-cycle"
        else:
            # Last resort: generic (still better than missing texture).
            gen = OUT / "generic.png"
            if not gen.is_file():
                print(f"[wythers MISS] {name}")
                continue
            img = Image.open(gen).convert("RGBA")
            tag = "generic"
        for dest_root in dests:
            nbytes = save_preview(img, dest_root / f"{name}.png")
            results[f"{dest_root.name}:{name}"] = nbytes
        print(f"[wythers] {name}: {tag}")

    return results


def main() -> None:
    import urllib.parse  # ensure available for fandom

    globals()["urllib"] = __import__("urllib")
    # patch fandom helper to use urllib.parse
    global fandom_page_image

    def _fandom_page_image(title: str) -> str | None:
        q = urllib.parse.quote(title.replace(" ", "_"))
        url = (
            "https://biomesoplenty.fandom.com/api.php?action=query&prop=pageimages"
            f"&titles={q}&pithumbsize=800&pilicense=any&format=json"
        )
        try:
            data = fetch_json(url)
            pages = data.get("query", {}).get("pages", {})
            for page in pages.values():
                thumb = page.get("thumbnail", {}).get("source")
                if thumb:
                    return thumb
        except Exception:
            return None
        return None

    globals()["fandom_page_image"] = _fandom_page_image

    print("=== Biomes O' Plenty ===")
    bop = fetch_bop()
    print("=== Wythers / WWOO / WWEE ===")
    wy = copy_or_fetch_wythers()
    print(f"DONE bop={len(bop)} wythers_entries={len(wy)}")
    # attribution
    text = (OUT / "SOURCES.txt").read_text(encoding="utf-8") if (OUT / "SOURCES.txt").is_file() else ""
    extra = f"""

Biomes O' Plenty:
  Modrinth gallery + Biomes O' Plenty Fandom wiki pageimages (compressed {SIZE}x{SIZE})

Wythers / WWOO / WWEE (Expanded Ecosphere):
  Folders: wythers/, wwoo/, expanded_ecosphere/ (same files)
  Sources: name-matched copies from BYG/RU/BOP/vanilla + Expanded Ecosphere Modrinth gallery fillers
"""
    if "Biomes O' Plenty:" not in text:
        (OUT / "SOURCES.txt").write_text(text.rstrip() + "\n" + extra, encoding="utf-8")


if __name__ == "__main__":
    main()
