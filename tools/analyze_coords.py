#!/usr/bin/env python3
"""Analyze chunk columns around block coords for carve artifacts."""
import io
import struct
import zlib
from pathlib import Path

try:
    import nbtlib
except ImportError:
    raise SystemExit("pip install nbtlib")

SAVE = Path(r"C:\curseforge\minecraft\Instances\TerraForgedTest\saves\New World (166)")
BX, BY, BZ = 28030, 31, -10741
CX, CZ = BX >> 4, BZ >> 4
LX, LZ = BX & 15, BZ & 15


def load_chunk_nbt(region_path: Path, cx: int, cz: int):
    cx_in, cz_in = cx & 31, cz & 31
    idx = cx_in + cz_in * 32
    with open(region_path, "rb") as f:
        loc = f.read(4096)
        entry = struct.unpack(">I", loc[idx * 4 : idx * 4 + 4])[0]
        off = entry >> 8
        if off == 0:
            return None
        f.seek(off * 4096)
        header = f.read(4)
        if len(header) < 4:
            return None
        ln = struct.unpack(">I", header)[0]
        comp = f.read(1)[0]
        data = f.read(ln - 1)
        if comp == 1:
            raw = gzip.decompress(data)
        elif comp == 2:
            raw = zlib.decompress(data)
        else:
            raw = data
    return nbtlib.File.parse(io.BytesIO(raw))


def decode_heightmap(longs, bits=9):
    """Decode MC heightmap long array for 256 values."""
    values = []
    mask = (1 << bits) - 1
    bit_pos = 0
    for val in longs:
        v = int(val)
        if v < 0:
            v += 1 << 64
        for _ in range(64 // bits):
            values.append((v >> bit_pos) & mask)
            bit_pos += bits
            if bit_pos >= 64:
                bit_pos = 0
                break
        if len(values) >= 256:
            break
    return values[:256]


def get_heightmap(nbt, name_hint="MOTION_BLOCKING"):
    if "Heightmaps" not in nbt:
        return None
    for key in nbt["Heightmaps"].keys():
        if name_hint in str(key):
            arr = [int(x) for x in nbt["Heightmaps"][key]]
            return decode_heightmap(arr)
    return None


def analyze_region(radius=2):
    rp = SAVE / "region" / f"r.{CX >> 5}.{CZ >> 5}.mca"
    print(f"Save: {SAVE.name}")
    print(f"Block ({BX}, {BY}, {BZ}) chunk ({CX}, {CZ}) local ({LX}, {LZ})")
    print(f"Region: {rp.name}\n")

    heights = {}
    for dcx in range(-radius, radius + 1):
        for dcz in range(-radius, radius + 1):
            ncx, ncz = CX + dcx, CZ + dcz
            nbt = load_chunk_nbt(rp, ncx, ncz)
            if nbt is None:
                print(f"chunk ({ncx},{ncz}): MISSING")
                continue
            hm = get_heightmap(nbt)
            status = str(nbt.get("Status", "?"))
            if hm:
                # index in heightmap: x + z*16
                col_heights = []
                for lz in range(16):
                    for lx in range(16):
                        col_heights.append(hm[lx + lz * 16])
                avg = sum(col_heights) / len(col_heights)
                mn, mx = min(col_heights), max(col_heights)
                heights[(ncx, ncz)] = (mn, mx, avg, col_heights)
                marker = " <-- player chunk" if dcx == 0 and dcz == 0 else ""
                print(f"chunk ({ncx},{ncz}): status={status} height min={mn} max={mx} avg={avg:.1f}{marker}")
            else:
                print(f"chunk ({ncx},{ncz}): status={status} no heightmap")

    if (CX, CZ) in heights:
        _, _, _, cols = heights[(CX, CZ)]
        print(f"\nPlayer chunk column height at ({LX},{LZ}): {cols[LX + LZ * 16]}")

    print("\nCross-chunk edge jumps (center chunk vs neighbors):")
    if (CX, CZ) not in heights:
        return
    _, _, _, center = heights[(CX, CZ)]
    for dcx, dcz, edge_lx, edge_lz, nlx, nlz in [
        (-1, 0, 0, LZ, 15, LZ),
        (1, 0, 15, LZ, 0, LZ),
        (0, -1, LX, 0, LX, 15),
        (0, 1, LX, 15, LX, 0),
    ]:
        nkey = (CX + dcx, CZ + dcz)
        if nkey not in heights:
            continue
        h1 = center[edge_lx + edge_lz * 16]
        h2 = heights[nkey][3][nlx + nlz * 16]
        print(f"  edge ({edge_lx},{edge_lz}) vs chunk {nkey} ({nlx},{nlz}): {h1} vs {h2}  delta={abs(h1-h2)}")


if __name__ == "__main__":
    analyze_region(2)
