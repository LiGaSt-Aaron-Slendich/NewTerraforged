# LiGaSt integrity algorithm

Hierarchical chunk corruption scan used by `CaveChunkCorruptionChecker` when
`enable_chunk_integrity_restorer = true`.

## Goal

Detect surface leaks (cave blocks / painted cave biomes above ground) without
scanning all 256 columns at full depth on every generated chunk.

## Stages (coarse → fine)

| Stage | Grid step (blocks) | What happens |
|-------|-------------------|--------------|
| 1 | **10** | Sample columns at `(0,0), (0,10), (10,0), (10,10)` inside the chunk. Clean → **stop** (chunk OK). |
| 2 | **4** | Scan `lx,lz` every 4 blocks. Any hit → mark suspicious cells. |
| 3 | **2** | Refine only around suspicious cells (±4 block margin). |
| 4 | **1** | Full per-column scan on flagged cells only; apply `MIN_DEFECT_COLUMNS` threshold. |

## Restore pipeline (unchanged)

When stage 4 reports corruption:

1. `CaveChunkOrderRestorer` phase 1 — strip surface features, repair terrain  
2. Phase 2 — replant underground features  
3. Phase 3 — `verify()` with stages 2→4 + noise chessboard check  

## Config

- `enable_chunk_integrity_restorer = false` — **default**; algorithm not run.  
- `enable_inspector = false` — inspector fully inert (no client hooks).  

## Related

- Implementation: `LiGaStIntegrityScan.java`, `CaveChunkCorruptionChecker.java`  
- Cross-chunk features: `ChunkScopedWorldGenLevel.ZONE_WRITE_RADIUS` (5×5 chunk zone, direct `ChunkAccess` fallback)
