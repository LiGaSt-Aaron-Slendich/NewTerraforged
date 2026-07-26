# NewTerraForged 1.19.2 port status

**Date:** 2026-07-26  
**Base:** local `TerraForged-0.3.x-build` (MC 1.19 / Forge 41.0.22)  
**Overlay:** NewTerraforged 1.18.2 systems (caves+, EGF, ocean/islands, biome rules, Customize GUI, …)  
**Build:** `compileJava` SUCCESS · `jar`/`reobfJar` SUCCESS

## Export

**Full mod JAR (not stock-only):**

`C:\Users\Апро\Projects\NewTerraForged\export\NewTerraForged-1.19.2-0.4.4.jar`

- Size: ~5.1 MB (2096 entries)
- SHA1: `dee4f8272ddc9e3602daab1cd7c928a72a068761`
- Built from: `build/libs/NewTerraForged-1.19-0.4.4-alpha-1.jar`

Verified classes in JAR: `TFNoiseVariantFlags`, `OceanLandscapeOverlay`, `CaveMegaGigaLayout`, `NvFlagPanel`, `ConfigScreen`, `BiomeRuleRegistry`.

## Included (compiled into JAR)

- Stock 1.19 Generator lifecycle (`RandomState` / seeded `TerrainCache`) **plus** NewTF hooks: `getSeed`, `getTerrainLevels`, `getGeneratorSettings`, `getTerrainSample`, `getOceanFloorHeight`, `getCaveEntranceClaims`, `peekCaveCarver`
- Cave systems: mega/giga layout, carver column cache, entrance claims, grotto/surface repair helpers, biome IDs/registry/rules
- EGF / ocean: `OceanLandscapeOverlay`, corridor graph, coastal LIA (flag-gated), islands package present
- GUI: Customize `ConfigScreen`, preview, `NvFlagPanel` (Untested flags)
- Surface biome rules: `BiomeRuleRegistry` + legacy `BiomeCategory` / `BiomeDictionary` shims
- Branding: modId `newterraforged`, version `0.4.4-alpha-1`, archives `NewTerraForged-1.19-…`

## Stubbed / limited (still in tree, reduced behavior)

- `CaveSurfaceBiomeRestorer` — compile-safe shims (`restore` / quart paint mostly no-op)
- `CaveDensityBudget` — empty stub
- Create-world `ScreenUtil` — adapted to 1.19 `WorldCreationContext` via reflection for package-private `updateSettings`
- `GeneratorPreset.build(seed, levels, settings, …)` — applies seed via `biomeSource.withSeed`; does not fully re-wire NewTF 1.18 seeded noise graph into stock codec
- Continent `getCell(cx,cy)` convenience uses seed `0` when callers omit seed (prefer stock seeded overloads)
- Some cave surface sanitizer/repair paths are present but restorer is stubbed → expect weaker mouth/surface restore until deepened

## Quarantined (`_quarantine_newtf118/`)

Kept out of compile (stock 1.19 lifecycle / conflicting 1.18 entrypoints):

- 1.18 `TFMain` / `Common` / `Client` / platform registrars / `ModRegistry` lazy stack
- Cave debug command/network/screens; probe inspector client
- Cave feature-plan / volume decorator / vanilla-pass extras from 1.18 feature runner path
- `ChunkScopedWorldGenLevel` + `util/delegate/*` (1.18 chunk delegate APIs)

## Build notes

- Removed `sourceSets.main.java { exclude … }` NewTF strip block
- `processResources` / `jar` use `DuplicatesStrategy.EXCLUDE`
- Prefer stock 1.19 seeded continent/river APIs over blind 1.18 Generator drop-in
