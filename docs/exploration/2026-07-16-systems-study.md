# Systems study — TerraForged 0.3 / Tectonic / TerraBlender / Terralith

Date: 2026-07-16  
Goal: learn how experienced worldgen mods structure landscape, biome paint, and cave decoration — then map lessons into NewTerraForged without blind 1.19 ports.

---

## 0. Two architectures (do not confuse them)

```
┌─────────────────────────────────────────────────────────────┐
│ A. VANILLA TERRAIN + CONTENT MODS                           │
│    NoiseBasedChunkGenerator + MultiNoiseBiomeSource         │
│    TerraBlender regions · Terralith/BOP/RU datapacks        │
│    Tectonic: replaces density_function / noise_settings JSON│
│    Decoration: features, surface rules, climate params       │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ B. CUSTOM CHUNKGENERATOR (TerraForged / NewTerraForged)     │
│    Owns heightfield, rivers, carve, biome Source            │
│    Must RE-IMPLEMENT every hook vanilla would have done     │
│    Content mods still provide biome JSON features           │
└─────────────────────────────────────────────────────────────┘
```

**NTFG is architecture B.** Comparing “why Terralith works” to “why NTFG breaks” is unfair unless you compare **hooks**, not screenshots. Terralith never fills river voids because vanilla density + carvers already couple height and caves.

---

## 1. TerraForged 0.3.x — the lineage to copy

### 1.1 Version note

- `mods.toml`: Forge `[40,)` — same loader band as 1.18.2, but Java uses **1.19-era** names (`RandomState`, `StructureManager`, `Structure` holders).
- **Do not paste 0.3 classes into NTFG.** Port *ideas*, reimplement against 1.18.2 APIs already in NTFG (`StructureFeatureManager`, etc.).

### 1.2 Pipeline (clean)

From `BiomeGenerator.java` (0.3):

```
surface()  → SurfaceDecorator (+ post)
carve()    → NoiseCaveGenerator.carve  (during GenerationStep.Carving)
decorate() → FeatureDecorator.decorate   // surface features first
           → NoiseCaveGenerator.decorate // cave features second
           → Surface.smoothWater / applyPost
```

**Absent from 0.3 (NTFG later added):**

- River void fill / bank terracing / aggressive zone fill
- Chunk integrity re-decorate
- Deferred block carve into decorate
- FeatureDensityBudget / FeatureMass caps
- Mega/Giga cave types, entrance claims, crust strip, far-chunk wrappers for quart grids

0.3 is deliberately thin: **terrain noise → carve → vanilla-style feature place**.

### 1.3 Landscape

| Piece | Role |
|-------|------|
| `Generator` | Custom `ChunkGenerator`; owns `TerrainCache`, `INoiseGenerator`, `Source` |
| `TerrainCache` / `TerrainData` | Per-chunk height, river, terrain type — async fill |
| `VanillaGen` | Structures / settings bridge to vanilla systems |
| Climate / continent noise | Separate from biome MultiNoise; 2D sample into `BiomeSampler` |

Height is **not** vanilla density functions. Rivers live in terrain data and feed surface + **cave mask**.

### 1.4 Cave carve — the important river coupling

`CarverChunk.getCarvingMask` (0.3):

```java
float noise = mask.getValue(seed, x, z);
float river = terrainData.getRiver().get(x, z);
return 1f - noise * river;
```

When river influence is high, carve strength collapses. **Caves do not open river shafts**, so no post-hoc void-fill is required.

NTFG today (weaker):

```java
return 1.0f - noise * river * 0.45f;  // mega/giga: ignores river entirely
```

Mega/giga path returns `1.0f - noise` with **no river term**. That is a root cause of river voids + “fill that clips caves.”

Cave types in 0.3: **GLOBAL** + **UNIQUE** only (`UniqueCaveDistributor`). No MEGA/GIGA.

Carve paints underground biomes into chunk sections **below surface−16** while air-carving (`NoiseCaveCarver`).

### 1.5 Cave decoration — experienced pattern

`NoiseCaveDecorator` (0.3) is ~50 lines of intent:

1. Collect biomes touched by this `NoiseCave` during carve (`CarverChunk.getBiomes`).
2. Pick origin at cave height: `(chunkMinX, config.getHeight(...), chunkMinZ)`.
3. For each biome, iterate `biome.getGenerationSettings().features()` from **`LOCAL_MODIFICATIONS` upward**.
4. Call **`feature.placeWithBiomeCheck(region, generator, random, pos)`**.

No floor grid. No ceiling grid. No density budget. **PlacementModifiers inside PlacedFeature** do height/environment scans.

That is how Terralith fungal caves work too: JSON features like `terralith:cave/fungal/huge_mushroom_scattered` live in the biome feature list; TF just invokes them at a cave-aware seed origin.

### 1.6 Surface decoration

`FeatureDecorator` (0.3):

1. **Pre** — `VanillaDecorator` stages `0 .. VEGETAL−1` at **one chunk origin** (section origin / center), one biome.
2. **Vegetation** — `PositionSampler` TF hex/grid for trees/grass using `VegetationConfig`.
3. **Post** — stages after vegetal.

Same shape as NTFG before P4 experiments. **No per-quart TB-style explosion of `placeWithBiomeCheck`.**

`Source.possibleBiomes()`: passes empty list to super (avoids Mojang feature-order crash), maintains own set — comment explicitly about biome mods.

### 1.7 Design principles visible in TF code

1. **One authority per concern** — carve owns air + underground biome paint; decorate only places features.
2. **Prevent bugs in carve, don’t plaster in decorate** — river mask instead of void fill.
3. **Trust datapack features** — cave decor = vanilla placer API.
4. **Object pool + ConcurrentHashMap** for `CarverChunk` across carve→decorate; re-simulate carve if chunk reloaded incomplete.
5. **Keep surface path boring** — custom only where TF identity lives (vegetation viability).

---

## 2. Tectonic — datapack density terrain (1.19+)

### 2.1 What it is

- `lowcodefml` — **zero Java worldgen**.
- Replaces `data/minecraft/worldgen/noise_settings/overworld.json` + many `density_function` / `noise` JSON under `tectonic:` and overrides under `minecraft:`.
- Adds biomes via datapack + tags.
- Zip name says 1.18.2; **`mods.toml` requires Minecraft `[1.19,1.20)`**. Treat as **reference for ideas**, not a dependency.

### 2.2 How terrain works

Vanilla `NoiseBasedChunkGenerator` reads `noise_router`:

- `continents`, `erosion`, `depth`, `ridges`, `temperature`, `vegetation`
- `initial_density_without_jaggedness`, `final_density`
- Cave cheese / spaghetti / noodle under `tectonic:overworld/caves/*`
- Specials: dunes, hoodoos, oceanside cliffs, **underground_rivers** as density additives

**Caves and surface are one density field.** Opening a cave reduces solid density; rivers/underground rivers are part of the same math. No separate “carve then fill shaft” step.

### 2.3 Steal ideas (not JSON)

| Idea | NTFG application |
|------|------------------|
| Couple cave openness to surface/river in **one sample** | Strengthen river term in `getCarvingMask`; never carve mega under deep river without mask |
| Underground rivers as **controlled density**, not post-repair | Prefer terrain/carve rules over `CaveChunkSurfaceRepair` |
| Modular named density pieces | Keep NoiseCave configs data-driven; avoid hardcoding mega exceptions |
| No custom decorator | Let biome JSON features place |

**Do not** try to run Tectonic density packs under NTFG’s custom generator — they never get sampled.

---

## 3. TerraBlender 1.18.2 — biome injection without terrain

### 3.1 Core API

| Class | Job |
|-------|-----|
| `Region` | Supplies `Climate.ParameterPoint → Biome` pairs; `RegionType` OVERWORLD/NETHER; weight |
| `Regions` | Registry of regions; index 0 = vanilla default region |
| `SurfaceRuleManager` | Namespaced surface rules → `NamespacedSurfaceRuleSource` |
| `ParameterUtils` / `ModifiedVanillaOverworldBuilder` | Helpers for climate points |
| `LevelUtils` | Server start hooks to rebuild parameter lists |
| Mixins | `MultiNoiseBiomeSource`, `Climate.ParameterList`, `NoiseGeneratorSettings` |

**TB never replaces ChunkGenerator.** It only expands who MultiNoise can pick and which surface rules apply per namespace.

### 3.2 Implication for NTFG

If NTFG’s `Source` does not consult TB parameter lists, Terralith/BOP/RU biomes only appear when **manually** mapped in `BiomeMapManager` / TOML. Features still exist on biome holders; **placement fails** if:

- underground biome paint wrong / restored away
- features never invoked (`placeWithBiomeCheck` not called for that biome)
- custom filters/budgets reject them
- soil/anchor invalid (DT mushrooms)

TB surface rules matter for grass/dirt bands — NTFG must either call into `SurfaceRuleManager` or duplicate cover logic.

---

## 4. Terralith 1.18.2 — content mod pattern

### 4.1 Code surface (tiny)

~16 Java files. Real work is datapack:

- `TerralithRegion` / `TerralithSkylandRegion` — `Regions.register`
- `TerrablenderUtil` — reads climate points + registers surface rules from overworld noise settings JSON
- Biomes / placed_features / configured_features under `data/terralith/...`

### 4.2 Cave biomes (examples)

`data/terralith/worldgen/biome/cave/`: andesite, crystal, deep, desert, diorite, frostfire, **fungal**, granite, ice, infested, mantle, thermal, tuff.

`fungal_caves.json` feature list includes early-stage stone splits, lakes, geodes, then fungal patches:

- `terralith:cave/fungal/huge_mushroom_scattered`
- `terralith:cave/fungal/patch_mushroom`
- hanging roots, vines, coarse dirt, …

On vanilla+TB worlds these place during normal chunk decoration because:

1. MultiNoise paints fungal caves underground (depth climate).
2. Vanilla feature loop runs all stages for the biome at that position.
3. Placement modifiers find floors/ceilings in already-carved cheese caves.

### 4.3 What NTFG must mirror

For Terralith cave biomes to “just work”:

1. Paint correct underground biome on cave air (quart/section).
2. During cave decorate, run **that biome’s** `PlacedFeature` list via `placeWithBiomeCheck` (TF 0.3 style), not only a hand-rolled allowlist that misses stages.
3. Do not overwrite cave air with river fill / integrity stone.
4. Do not strip crust after features place.

Surface Terralith biomes need surface biome stable before vegetation; TF 0.3 uses one origin biome for pre/post — NTFG already does similar; P4 quart explosion was the wrong “TB-like” imitation.

---

## 5. How experienced modders think (synthesis)

### Biome distribution

- **Vanilla/TB:** 6D climate noise → nearest ParameterPoint → biome. Underground = depth axis.
- **TF:** 2D climate + terrain type → weighted biome maps; separate `CaveBiomeSampler` for underground types (GLOBAL/UNIQUE).

### Biome painting

- Write biome into chunk **palette sections** at quart resolution when carving (TF) or via MultiNoise when creating biomes (vanilla).
- Decorate reads `level.getBiome(pos)` / painted section — **one paint authority**.

### Landscape

- Either **replace density JSON** (Tectonic) or **replace ChunkGenerator** (TF) — never half of both.
- Couple water/river to carve in the **same** decision function.

### Decoration

- Prefer **datapack PlacedFeature + PlacementModifier**.
- Custom code only for: position sampling identity (TF trees), compatibility bridges (DT), or structure integration.
- Caps/budgets are last resorts; TF 0.3 has none.

### Performance

- One feature pass per stage per origin (TF surface).
- Cave: one origin per cave-config × biome list, not 8×8×Y scans with budgets.
- Pool carver state; don’t rebuild neighbor worldgen for every block.

---

## 6. Key file index (extracted)

### TerraForged 0.3

- `.../worldgen/Generator.java`
- `.../worldgen/biome/BiomeGenerator.java`
- `.../worldgen/biome/Source.java`
- `.../worldgen/biome/decorator/{FeatureDecorator,VanillaDecorator,PositionSampler}.java`
- `.../worldgen/cave/{NoiseCaveGenerator,NoiseCaveCarver,NoiseCaveDecorator,CarverChunk}.java`

### Tectonic

- `data/minecraft/worldgen/noise_settings/overworld.json`
- `data/tectonic/worldgen/density_function/overworld/**`
- especially `caves/total.json`, `special/underground_rivers/**`

### TerraBlender

- `terrablender/api/{Region,Regions,SurfaceRuleManager}.java`
- `terrablender/util/LevelUtils.java`

### Terralith

- `.../terrablender/TerralithRegion.java`
- `.../utils/TerrablenderUtil.java`
- `forge/.../data/terralith/worldgen/biome/cave/*.json`
- `forge/.../data/terralith/worldgen/placed_feature/cave/**`
