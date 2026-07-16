# NTFG transfer map — what to implement from exploration

Date: 2026-07-16  
Priority order for future work (not an immediate code dump).

---

## P0 — Fix root causes (TF 0.3 lessons)

| # | Change | Why | Source |
|---|--------|-----|--------|
| P0.1 | Restore **full river term** in `CarverChunk.getCarvingMask` for normal caves: `1 - noise * river` | Stops river shafts; makes void-fill unnecessary | TF 0.3 `CarverChunk` |
| P0.2 | Apply **river (and ocean) mask to MEGA/GIGA** carve, or refuse carve when `river` above threshold | Mega currently ignores river → voids + fill clipping caves | NTFG vs TF 0.3 |
| P0.3 | Cave decor default path ≈ TF 0.3: for each cave biome in `CarverChunk`, `placeWithBiomeCheck` from `LOCAL_MODIFICATIONS`+ | Missing Terralith/RU/WWEE cave features | TF `NoiseCaveDecorator` |
| P0.4 | Keep surface decor as TF 0.3 / pre-P4 (single-origin VanillaDecorator + PositionSampler) | P4 quart grid caused lag + far-chunk | TF `FeatureDecorator` |

Gates already lean this way (`featureDensityBudgetEnabled=false`, `terraBlenderChunkBiomeDecorEnabled=false`). Next step is **cave path simplification**, not more surface hacks.

---

## P1 — Retire repair where carve is fixed

| # | Change | Notes |
|---|--------|-------|
| P1.1 | After P0.1–P0.2 playtest, keep `riverZoneAggressiveFillEnabled=false` permanently | Aggressive fill was a plaster |
| P1.2 | Narrow remaining fill to **true terrain depressions only** (bed column water top-up), never open cave air | Ore exclusion already done |
| P1.3 | Prefer elevated-river bed Y clamp in terrain (`resolveRiverBedY`) over fill | Already partially present |

Tectonic lesson: underground rivers live in density; TF lesson: river lives in carve mask.

---

## P2 — Biome paint authority

| # | Change | Notes |
|---|--------|-------|
| P2.1 | Single underground paint at carve; surface restore only in entrance band | Matches prior cave retro |
| P2.2 | Do not invent “TB quart decor”; instead ensure painted biome is what feature check sees | Terralith/TB |
| P2.3 | Optional: import TB region biome IDs into `BiomeMapManager` weights (bridge already started) | Keep terrain NTFG-owned |
| P2.4 | Fix `TerraBlenderRegionBridge`: real TB API is `LevelUtils.initializeOnServerStart` (not `onServerAboutToStart`); it still **no-ops** for non-`NoiseBasedChunkGenerator` — prefer `Regions.get` + `region.addBiomes` | TB subagent |
| P2.5 | Depth-aware underground pick (or depth proxy from surfaceY/blockY) so Terralith lush/dripstone/cave ParameterPoints can match | TB depth presets SURFACE / UNDERGROUND / FLOOR |

---

## P3 — Feature filters / budgets

| # | Change | Notes |
|---|--------|-------|
| P3.1 | Keep **custom FeatureDensityBudget off** unless profiling proves need | TF 0.3 has none |
| P3.2 | Soften `CaveFeatureFilters` so Terralith `cave/fungal/*` and RU cave features aren’t over-blocked | Compare allowlists to biome JSON feature lists |
| P3.3 | Prefer stage-based include (from LOCAL_MODIFICATIONS) over path string denylists | TF style |

---

## P4 — Compat layers (not core gen)

| # | Change | Doc |
|---|--------|-----|
| P4.1 | Dynamic Trees fungal / huge mushroom soil + anchor | `dynamic-trees-compat.md` |
| P4.2 | WWEE/RU: ensure biomes in possible set + features invoked; DT populator JSON if they ship DT addons | same |
| P4.3 | Namespaced surface rules via TB when loaded | existing audit |

---

## Explicit non-goals

- Do **not** merge Tectonic density JSON into NTFG generator.
- Do **not** run TerraForged 0.3 and NTFG together.
- Do **not** re-enable per-quart surface BiomeQuartDecorator for “TB compatibility.”
- Do **not** study Dynamic Trees as a terrain replacement.

---

## Suggested implementation sequence

```
1. River mask parity with TF 0.3 (+ mega)
2. Playtest: river voids gone? → delete/disable more fill
3. Cave decor: official path = NoiseCaveDecorator-style placeWithBiomeCheck
4. Filter audit vs Terralith fungal_caves.json feature IDs
5. DT soil/anchor expansion for WWEE/RU fungal floors
6. Only then consider climate/TB weight polish
```

---

## Diff cheat-sheet (mental)

| Concern | TF 0.3 | NTFG today (approx) | Target |
|---------|--------|---------------------|--------|
| Carve↔river | `1-noise*river` | `*0.45`, mega ignores | TF parity |
| Cave types | GLOBAL, UNIQUE | + MEGA/GIGA | Keep mega but mask them |
| Cave decor | placeWithBiomeCheck | Hybrid + filters + grids | Prefer TF core |
| Surface decor | 1 origin + vegetation sampler | same (P4 off) | keep |
| Void fill | none | optional/aggressive off | bed-only |
| Feature budget | none | gated off | stay off |
