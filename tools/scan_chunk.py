#!/usr/bin/env python3
"""Scan MCA region around chunk for height jumps and solid columns."""
import gzip
import struct
import sys
from pathlib import Path

def read_chunk_nbt(region_path, chunk_x, chunk_z):
    local_x = chunk_x & 31
    local_z = chunk_z & 31
    idx = local_x + local_z * 32
    with open(region_path, "rb") as f:
        loc = f.read(4096)
        off = struct.unpack(">I", loc[idx * 4 : idx * 4 + 4])[0]
        if off == 0:
            return None
        f.seek(off * 4096)
        length = struct.unpack(">I", f.read(4))[0]
        comp = f.read(1)[0]
        data = f.read(length - 1)
        return gzip.decompress(data) if comp == 2 else data

def scan_save(save_dir, cx, cz, radius=3):
    rp = save_dir / "region" / f"r.{cx >> 5}.{cz >> 5}.mca"
    if not rp.exists():
        return None
    results = []
    for dcx in range(-radius, radius + 1):
        for dcz in range(-radius, radius + 1):
            nx, nz = cx + dcx, cz + dcz
            raw = read_chunk_nbt(rp, nx, nz)
            if raw is None:
                results.append((nx, nz, "missing", 0, 0))
                continue
            air = raw.count(b"minecraft:air")
            stone = raw.count(b"minecraft:stone") + raw.count(b"minecraft:deepslate")
            results.append((nx, nz, "full", air, stone))
    return results

def main():
    saves = Path(r"C:\curseforge\minecraft\Instances\TerraForgedTest\saves")
    cx, cz = 1751, -672
    for save in sorted(saves.iterdir(), key=lambda p: p.stat().st_mtime if p.is_dir() else 0, reverse=True):
        if not save.is_dir():
            continue
        res = scan_save(save, cx, cz, 2)
        if res is None:
            continue
        present = [r for r in res if r[2] == "full"]
        if len(present) < 3:
            continue
        print(f"\n=== {save.name} ({len(present)} chunks in 5x5) ===")
        for nx, nz, st, air, stone in res:
            print(f"  chunk ({nx},{nz}): {st} air_tokens={air} stone_tokens={stone}")
        break

if __name__ == "__main__":
    main()
