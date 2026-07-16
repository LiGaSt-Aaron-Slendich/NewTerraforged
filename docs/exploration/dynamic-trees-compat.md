# Dynamic Trees compatibility (caves + mod biomes)

Date: 2026-07-16  
Scope: make **existing** Dynamic Trees species/features work in NewTerraForged worlds — especially cave fungal features and surface biomes from WWEE / Regions Unexplored. **Not** about authoring new DT trees.

---

## 1. How Dynamic Trees plugs into vanilla

DT does **not** replace ChunkGenerator. It:

1. Registers **Species** (JSON/datapack driven via `SpeciesResourceLoader`).
2. Registers **biome populators** (`BiomePopulatorsResourceLoader`) — density/chance/species selectors per biome.
3. **Cancels** vanilla/mod tree `ConfiguredFeature`s via `FeatureCanceller` + `BiomePropertySelectors.FeatureCancellation` so DT replaces static trees.
4. Places trees through its own worldgen feature / populator path during `VEGETAL_DECORATION` (and related).
5. Optional **GenFeatures** on species — including `HugeMushroomGenFeature` / `HugeMushroomsGenFeature` (voxmap caps, stem, safe chunk bounds).

Key packages (extracted `DynamicTrees-release-1.18.2`):

- `api/worldgen/FeatureCanceller.java`
- `api/worldgen/BiomePropertySelectors.java`
- `resources/loader/{Species,BiomePopulators,FeatureCancellation}ResourceLoader.java`
- `systems/genfeature/HugeMushroom*.java`
- `api/TreeHelper.java`

### Feature cancellation model

```text
BiomeGenerationSettingsBuilder
  → FeatureCancellation.cancelFeatures(...)
      cancelUsing(FeatureCanceller)
      cancelWithNamespace("minecraft" | "biomesoplenty" | ...)
      cancelDuring(VEGETAL_DECORATION)
```

If NTFG **bypasses** biome feature lists and only runs a custom tree sampler, DT cancellation never mattered — and DT trees never spawn unless NTFG explicitly places DT `PlacedFeature`s.

NTFG already branches when DT is loaded (`FeatureDecorator`: vanilla vegetal stage + `placeVegetationWithoutTrees`). That is the correct **surface** pattern: let DT own trees; TF owns grass/other.

---

## 2. What NTFG already does (cave)

`DynamicTreesCompat`:

- Detects mod id `dynamictrees`.
- For **fungal cave biomes** (`CaveBiomeIds.isFungalCaveBiome`): after cover, runs `VEGETAL_DECORATION` features whose namespace is `dynamictrees`.
- Resolves air anchor above solid floor; requires soil in dirt/mycelium/moss/fungal-ish blocks.
- `CaveBiomeFeatureRunner` skips duplicate vanilla fungal scatter when DT active.

This matches DT’s expectation: **place DT features with valid rooty soil under an air cell**.

---

## 3. Gaps for WWEE / RU / Terralith caves

| Gap | Symptom | Fix direction |
|-----|---------|---------------|
| Fungal biome ID not in `isFungalCaveBiome` | DT pass never runs | Extend ID/path lists / tags for WWEE, RU, Terralith `fungal_caves`, glowing grotto aliases |
| Floor is stone/deepslate after carve | `canAcceptRootyDirt` fails | Ensure cave cover pass places mycelium/dirt/moss **before** DT; or expand accepted soils to match biome surface |
| Features not in biome list under NTFG world | place finds nothing | Confirm biome holder is datapack biome with DT feature injected; may need DT biome populator JSON for that biome id |
| Custom cave runner blocks DT / huge mushrooms | cut or missing caps | Prefer TF-style `placeWithBiomeCheck` for vegetal stage; keep filters from deleting `dynamictrees:` |
| SafeChunkBounds / far chunk | DT refuses or logs | Place only inside `ChunkScopedWorldGenLevel` radius; DT uses `SafeChunkBounds` — stay in-chunk |
| Surface WWEE/RU trees | empty forests when DT cancels vanilla but NTFG skips DT | Keep `DynamicTreesCompat.isLoaded()` surface branch; ensure biome in DT populator data |

### Terralith fungal example

`terralith:cave/fungal_caves` JSON lists `terralith:cave/fungal/huge_mushroom_scattered` in an early feature stage. Those may be **vanilla-style** huge mushrooms, not DT. Compatibility matrix:

1. If DT addon cancels Terralith mushrooms → must place DT species instead (Compat pass).
2. If no DT addon → TF-style placeWithBiomeCheck should place Terralith features as-is.

Do not force everything through DT — only when DT is loaded **and** features are DT-namespaced or cancelled.

---

## 4. Implementation checklist (compat only)

1. **Inventory fungal biome IDs** in the user’s pack (Terralith, WWEE, RU, NTFG aliases) → map into `CaveBiomeIds`.
2. **Order:** cave cover / floor paint → DT vegetal place → other scatter.
3. **Soil:** cover blocks must match `canAcceptRootyDirt` or extend matcher for WWEE/RU floor blocks (bioshroom, mulch, etc. — partial already).
4. **Surface:** never run TF tree grid when DT loaded; rely on DT populators for WWEE/RU biomes (may require DT datapack entries — often provided by DT Compat mods, not NTFG).
5. **Do not** implement new Species in NTFG; document required external datapacks (`DynamicTreesTerralith`, `DynamicTreesBOP`, etc. if they exist for 1.18.2).
6. Debug: `/newtf debug cave save` at fungal floor — biome id, soil block, whether DT feature attempted.

---

## 5. API touchpoints for future code

```text
Surface:
  FeatureDecorator.decorateVegetation
    → if DT: VanillaDecorator(VEGETAL) + PositionSampler.placeVegetationWithoutTrees
    → else: PositionSampler.placeVegetation (TF trees)

Cave:
  DynamicTreesCompat.decorateFungalCave
    → VEGETAL_DECORATION ∩ namespace dynamictrees
    → place at air above rooty soil

Optional future:
  Reflect DT TreeHelper / Species.getSpeciesForLocation for custom anchors
  without depending on PlacedFeature list (only if biome JSON empty)
```

---

## 6. Non-goals

- Porting DT growth simulation into NoiseCave.
- Replacing Terralith datapack mushrooms with hand-coded voxmaps.
- Teaching NTFG to cancel features the way DT does (DT already cancels on biome build).
