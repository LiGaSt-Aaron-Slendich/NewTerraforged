# Appendix: NewTerraForged decoration ecosystem

Companion to [`2026-07-15-decoration-systems-audit.md`](2026-07-15-decoration-systems-audit.md).

## Decorate order (`BiomeGenerator.decorate`)

```
1. TerrainData resolve
2. ChunkScopedWorldGenLevel (radius 2 cave / 3 features)
3. [GATE] riverVoidFill → CaveChunkSurfaceRepair.restoreRiverDepressions
4. [GATE] deferBlockCarve → NoiseCaveGenerator.applyCarveBlocks
5. FeatureDecorator: pre + vegetation + post (structures deferred)
6. Surface.smoothWater + applyPost
7. [GATE] RiverShoreBiomeClip
8. FeatureDecorator.placeStructures
9. NoiseCaveGenerator.decorateVolume (+ CaveFloatingCrustStrip on mega/giga)
10. Surface.repairExposedCover
11. NoiseCaveGenerator.decorateEntrances
12. [GATE] CaveChunkIntegrityPass
13. finishDecorate + refreshHeightmaps
```

## Key file index

### Pipeline
- `src/main/java/com/terraforged/mod/worldgen/Generator.java`
- `src/main/java/com/terraforged/mod/worldgen/biome/BiomeGenerator.java`

### Surface
- `.../biome/decorator/SurfaceDecorator.java`
- `.../biome/surface/Surface.java`
- `.../VanillaGen.java`

### Surface features
- `.../biome/decorator/FeatureDecorator.java`
- `.../biome/decorator/VanillaDecorator.java`
- `.../biome/decorator/PositionSampler.java`
- `.../biome/vegetation/BiomeVegetationManager.java`
- `.../biome/vegetation/VegetationFeatures.java`
- `.../biome/decorator/FeatureDensityBudget.java`
- `.../biome/decorator/FeaturePlacement.java`

### Cave
- `.../cave/NoiseCaveGenerator.java`
- `.../cave/NoiseCaveCarver.java`
- `.../cave/CaveDecorationSettings.java`
- `.../cave/TerraForgedOfficialCaveDecorator.java`
- `.../cave/CaveDecoratePaint.java`
- `.../cave/CaveBiomeFeatureRunner.java`
- `.../cave/CaveFeatureFilters.java`
- `.../cave/CaveHybridBiomeDecorator.java` (**not wired**)
- `.../cave/CaveEntranceSurfaceDecorator.java`
- `.../cave/CaveFloatingCrustStrip.java`

### Filters / guards
- `.../cave/CavePlacementFilter.java`
- `.../cave/MegaCaveStructureFilter.java`
- `.../cave/CarverColumnCache.java`
- `.../cave/CaveUndergroundGuard.java`
- `.../cave/ChunkScopedWorldGenLevel.java`

### Post-passes
- `.../cave/CaveChunkSurfaceRepair.java`
- `.../cave/RiverShoreBiomeClip.java`
- `.../cave/CaveSurfaceBiomeRestorer.java`
- `.../cave/CaveChunkIntegrityPass.java`

### Config
- `.../GenerationFeatureGates.java`
- `Forge/main/java/.../TFCaveBiomeConfig.java`
- `Forge/main/java/.../TFCaveSystemConfig.java`
- `.../cave/CaveCarvingGate.java`

## Failure mode matrix

| Symptom | Subsystem | Check |
|---------|-----------|-------|
| Bare cave floors | paint/grid | quart at floor; `ensureFloorPaint`; `surfaceBiomeSkip` |
| Decor one column only | origin cap / paint gap | official origins; `CaveDecoratePaint` |
| Trees in cave mouths | surface filter | `CavePlacementFilter`, `CarverColumnCache.skipTree` |
| Structures in mega | structure filter | `MegaCaveStructureFilter`, height cache |
| Wrong shore biome/features | shore clip timing | clip vs feature order |
| Floating grass mega | crust strip | `CaveFloatingCrustStrip` band |
| No cave decor | mode gates | `CaveDecorationSettings.activeModeLabel()` |
| Mod surface wrong | surface namespace | vanilla overworld rule only |
| Debug allowed ≠ feet | grid | anchor step 2 vs player position |
| Terralith cave features missing | feature routing | `CaveFeatureFilters.belongsToModCaveBiome` |

## Cave decor mode (default)

| Mode | Active when | Backend |
|------|-------------|---------|
| official | `use_official_tf_decorator` or `use_per_biome_decorators` | `TerraForgedOfficialCaveDecorator` |
| legacy | gate + toml | `CaveBiomeVolumeDecorator` (gate **off**) |
| compromise | gate + toml | `CaveBiomeCompromiseDecorator` (gate **off**) |
| vanilla | gate + toml | `CaveBiomeVanillaPass` |
| hybrid | — | **not called from NoiseCaveGenerator** |

## Truth chain (debug)

1. Quart paint at floor  
2. Carve / `surfaceBiomeSkip` / column air + solid floor  
3. Decor grid anchor  
4. Filter allowed  
5. Actual place  

Command: `/newtf debug cave save`
