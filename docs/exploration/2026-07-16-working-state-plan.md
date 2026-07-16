# Plan — bring NewTerraForged to a working state

Date: 2026-07-16  
Constraints from product owner:

- **Do not rewrite landscape** (continents/height/rivers/noise) — only fix what causes **tunnels under rivers**.
- **TerraBlender should own biome selection** where possible; TF `BiomeSampler` / climate-weight maps currently suppress it and must be gated off for a real TB path.
- **Dynamic Trees** = compatibility bridge, not new trees.
- Surface decor stays TF-style (no P4 quart explosion).

---

## Honest answers first

### Can TerraBlender “just do it for us”?

**Not fully automatic under NTFG.** TB is designed for:

`NoiseBasedChunkGenerator` + `MultiNoiseBiomeSource` + real `Climate.Sampler`

NTFG suppresses that with:

| Subsystem | How it blocks TB |
|-----------|------------------|
| `Source.getNoiseBiome` | Ignores `Climate.Sampler` (`NOOP`); 2D cache → `BiomeSampler` |
| `BiomeSampler` + `BiomeMapManager` | TF climate types → weight maps (TOML/auto), not TB ParameterPoints |
| `CaveBiomeSampler` | Separate underground pick; not TB depth MultiNoise |
| `LevelUtils.initializeOnServerStart` | **No-ops** unless generator is `NoiseBasedChunkGenerator` |

So TB **can** do Terralith/BOP/RU biome work **if we feed it** TargetPoints and call its region RTrees from `Source`. That means **disabling TF biome authority** (BiomeSampler path) when TB is loaded — not deleting landscape.

What TB still won’t do: heightfield, rivers, NoiseCave carve. Terrain stays NTFG.

### Dynamic Trees

Already partially wired (`DynamicTreesCompat`, surface yields trees to DT when loaded, fungal cave pass). Missing pieces are mostly **IDs/soil/anchors** and ensuring cave decor actually invokes DT `PlacedFeature`s after floor cover — not a DT rewrite.

---

## Phase plan

### Phase 0 — River tunnels only (landscape untouched)

**Goal:** caves stop opening shafts under rivers; void-fill becomes optional.

| Step | Action | Gate / file |
|------|--------|-------------|
| 0.1 | Normal caves: restore TF 0.3 mask `1 - noise * river` (remove `* 0.45`) | `CarverChunk.getCarvingMask` |
| 0.2 | MEGA/GIGA: apply river (+ ocean) mask or skip carve when river above threshold | same + `NoiseCaveCarver` |
| 0.3 | Keep `riverZoneAggressiveFillEnabled = false`; bed water top-up only | already |
| 0.4 | Playtest new chunks: no ore-fill chessboard, no cave plugged by fill, no mega river shafts | |

**Out of scope:** continent noise, river generator redesign, Tectonic density.

---

### Phase 1 — TerraBlender biome authority (disable TF biome subsystem)

**Goal:** when TB is present, Terralith/BOP/RU biomes (surface + depth caves) come from TB regions; TF weight maps sleep.

| Step | Action |
|------|--------|
| 1.1 | Add gate `terraBlenderBiomeAuthorityEnabled` (default **true** if TB loaded) |
| 1.2 | When on: **bypass** `BiomeSampler.sampleBiome` / climate weight maps for surface paint |
| 1.3 | Build `Climate.TargetPoint` from NTFG signals (continent→continentalness, climate temp/moisture, height→depth, river/weirdness proxies) — enough for TB RTree match |
| 1.4 | Query TB `Regions` / `ParameterList.findValuePositional` (reflect or thin wrapper); paint that biome into quarts |
| 1.5 | Keep minimal TF overrides only if needed (optional): deep ocean by continent — prefer TB points first |
| 1.6 | Underground: pass **depth** into TargetPoint so Terralith `cave/*`, lush, dripstone can win; align carve paint with same picker |
| 1.7 | Fix bridge: call `LevelUtils.initializeOnServerStart` if useful for region setup, but **do not rely** on it for NTFG generator; always scan `Regions` + `addBiomes` |
| 1.8 | `possibleBiomes` must include all TB region biomes (features-per-step) |
| 1.9 | Keep `TerraBlenderSurfaceRules.wrap` for namespaced dirt/grass |
| 1.10 | When TB **absent**: keep current `BiomeSampler` path (single-player / no Terralith) |

**Disable / sleep when TB authority on:**

- `BiomeMapManager` climate-type weight selection as paint authority  
- Auto-mod weight boost as primary distribution (optional keep for fallback world)  
- Ad-hoc “boost TB biomes into TF maps” as the main strategy  

**Do not disable:** terrain, carve, river noise, surface rules wrap, cave structure.

Playtest: F3 biome = Terralith/BOP/RU on surface; underground cave biomes appear; features attach to painted biome.

---

### Phase 2 — Cave decoration that respects painted biomes

**Goal:** missing cave features (Terralith fungal, RU, WWEE) actually place.

| Step | Action |
|------|--------|
| 2.1 | Prefer TF-0.3-style invoke: biome feature list from `LOCAL_MODIFICATIONS`+ via `placeWithBiomeCheck` at cave origins |
| 2.2 | Soften/align `CaveFeatureFilters` with real Terralith/RU feature IDs (stop over-blocking) |
| 2.3 | Keep `featureDensityBudgetEnabled = false` |
| 2.4 | Keep surface FeatureDecorator pre-P4 (no BiomeQuartDecorator) |
| 2.5 | Single paint authority: carve paints underground; surface restore only entrance band |

---

### Phase 3 — Dynamic Trees compat

**Goal:** DT trees/mushrooms work on NTFG worlds for fungal caves + WWEE/RU (and Terralith if DT addon present).

| Step | Action |
|------|--------|
| 3.1 | Expand `CaveBiomeIds.isFungalCaveBiome` for Terralith/WWEE/RU fungal/grotto IDs |
| 3.2 | Ensure floor cover (mycelium/dirt/moss/bioshroom) **before** `DynamicTreesCompat.decorateFungalCave` |
| 3.3 | Expand `canAcceptRootyDirt` for WWEE/RU soils |
| 3.4 | Surface: keep “DT loaded → skip TF trees, run vegetal + grass only” |
| 3.5 | Document required external packs (`DynamicTrees*` addons) — NTFG does not ship Species |
| 3.6 | Debug checklist: biome id, soil, DT feature attempted (`/newtf debug cave save`) |

---

### Phase 4 — Stabilize & retire plasters

| Step | Action |
|------|--------|
| 4.1 | If Phase 0 holds: remove or permanently gate aggressive river-zone fill |
| 4.2 | Integrity pass stays off unless a measured need appears |
| 4.3 | Perf pass: no far-chunk spam, mega gen acceptable |
| 4.4 | Update `docs/exploration/ntfg-transfer-map.md` with “done” checkmarks |

---

## Execution order (what I will do next)

```
Phase 0 (river mask)     → build + deploy + you playtest rivers/caves
Phase 1 (TB authority)   → biggest biome fix; gate off BiomeSampler when TB present
Phase 2 (cave decor)     → features on painted cave biomes
Phase 3 (Dynamic Trees)  → fungal / WWEE / RU
Phase 4 (cleanup)        → delete dead plasters
```

One logical commit (+ deploy) per phase step cluster; no landscape rewrite.

---

## Success criteria

1. No mega tunnels / ore-filled plugs under rivers on **new** chunks.  
2. With TB+Terralith/BOP/RU: surface and cave biomes look like those mods’ content, not empty TF plains.  
3. Cave features (mushrooms, dripstone, etc.) present where biome JSON says so.  
4. With Dynamic Trees: fungal cave DT features root; surface forests not double-tree / empty after cancel.  
5. Chunk gen speed roughly back to pre-P4; no far-chunk log spam.  
6. Landscape (mountains/continents/river paths) unchanged in character.
