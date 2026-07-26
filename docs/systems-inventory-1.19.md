# Systems inventory: NewTerraforged 1.18.2 vs TerraForged 0.3.x (1.19)

Base compared: `TerraForged-0.3.x-build` (MC **1.19**, Forge 41.0.22, modId `terraforged`).  
Ours: `NewTerraforged` (MC **1.18.2**, Forge 40.3.12, modId `newterraforged` 0.4.3).

---

## Ours (NewTF) — port these onto the 1.19 base

| System | Key paths | Notes |
|--------|-----------|--------|
| Cave mega/giga + rules + decor + debug | `worldgen/cave/**`, `CaveDebug*`, `TFCave*` | Largest delta (~82 vs ~7 files) |
| Ocean landscape | `worldgen/noise/continent/ocean/**` | EGF; corridor / seafloor / volcanoes |
| Coastal LIA | `CoastalLiaOverlay.java` | EGF |
| Islands / archipelago overlays | `worldgen/noise/continent/island/**` | EGF |
| Guaranteed continents | `GuaranteedContinentMask`, `ContinentGuarantee` | EGF |
| Mega ridges / mountain belt | `MountainBelt*`, `ClimateTerrainBias`, tall `TerrainLevels` | EGF `megaRidges` |
| Surface biome rules | `worldgen/biome/rules/**`, biome terrain integration | Full subsystem |
| EGF / NV flags + Untested UI | `TFNoiseVariantFlags`, `client/gui/screen/nv/**` | Encrypted bitfield |
| In-game Customize + preview painters | `client/gui/screen/**`, presets, mixins | Stock has Swing Previewer only |
| Settings wiring / codecs | `worldgen/settings/**` | Continent/river wiring, generator snapshot |
| TerraBlender / DynamicTrees / WWOO | `compat/**` | Version-sensitive |
| Feature gates / probe / spawn hooks | `GenerationFeatureGates`, `ProbeNetwork`, `TFSpawnHooks` | |
| Dual modId stub | `newterraforged` + legacy `terraforged` | Datapack namespace policy |

---

## Stock TerraForged — keep from 1.19 base

- Chunk generator core: `Generator`, `GeneratorPreset`, `VanillaGen`, `Seeds`
- Terrain: `TerrainGenerator`, `TerrainBlender`, `TerrainCache`, `TerrainData`
- Noise / continent / river / cell / shape (stock skeleton)
- Thin cave path: `NoiseCaveGenerator`, `NoiseCaveCarver`, `NoiseCaveDecorator`, `CarverChunk`, `CaveType`
- Climate / erosion / vegetation / viability (stock)
- Assets: climates, terrain noises, vegetation configs
- Fabric platform (base only; NewTF is Forge-first)
- Lifecycle / `CommonAPI` / richer codecs (1.19 boot model)
- Swing `client/ui/Previewer` (dev; optional to keep)

---

## Shared concepts — merge carefully

| Concept | Action |
|---------|--------|
| ContinentNoise / Shape / River | Start from stock; re-apply NewTF overlays + river fixes |
| BiomeSampler / Surface | Stock + NewTF rules / height zones / belt |
| CarverChunk / cave carvers | Replace with NewTF implementations adapted to 1.19 APIs |
| TerrainLevels | Port tall-world / EGF height caps onto 1.19 dimension APIs |
| GeneratorPreset / create-world mixins | Rewrite for 1.19 screens |
| Builtin datapacks | Merge NewTF cave/island content into 1.19 registry layout |

---

## Port order (recommended)

1. EGF flags + feature gates  
2. Settings wiring + continent/river overlays  
3. Ocean / islands / LIA / guarantee  
4. Mega ridges + TerrainLevels  
5. Biome rules  
6. Cave mega/giga stack  
7. Customize GUI + preview  
8. Compat (TerraBlender et al.)
