# Design principles — writing worldgen / cave decor systems

Condensed from TerraForged 0.3, Tectonic, TerraBlender, Terralith, and Dynamic Trees study (2026-07-16). Use when rewriting NTFG systems.

---

## 1. Pick one terrain authority

- **Either** custom `ChunkGenerator` (TF/NTFG) **or** vanilla density datapack (Tectonic).
- Never half-replace: datapack caves won’t run inside TF heightfield; TF void-fill won’t fix vanilla density bugs.

## 2. Couple interacting systems in one decision

- River ↔ cave: same mask/sample (TF `getCarvingMask`).
- Height ↔ cave: Tectonic puts both in `final_density`.
- If you carve first and “fix rivers” later, you will always fight yourself.

## 3. Paint biomes once; decorate reads paint

- Carve paints underground biomes into sections.
- Surface restore only where entrances need surface biomes.
- Features use `placeWithBiomeCheck` so wrong paint = no place (good fail).

## 4. Prefer datapack features over custom placers

- Content mods (Terralith/RU/WWEE) already authored `PlacedFeature` + modifiers.
- Custom code should **invoke** them, not re-encode mushroom shapes.
- Custom placement only for mod identity (TF vegetation viability) or hard compat (DT soil).

## 5. Keep pipelines short and ordered

Good (TF 0.3):

```
surface → carve → surface features → cave features → water smooth
```

Bad (symptom stack):

```
carve → fill voids → fill banks → re-paint → features → strip crust → integrity → fill again
```

Every post-pass needs a measured root cause deletion plan.

## 6. Performance = fewer feature origins

- Surface: one biome origin per chunk stage (TF).
- Cave: one origin per cave-config × biome list (TF).
- Avoid: N×N grid × all stages × all features (P4 lesson: lag + far-chunk).

## 7. Caps are last resorts

- TF 0.3: no FeatureDensityBudget.
- If lag from features, fix filters/origins first; budgets hide missing content.

## 8. Compat is a bridge, not a fork

- Dynamic Trees: cancel vanilla trees + place DT features; NTFG detects and yields tree stage.
- TerraBlender: regions/surface rules; NTFG keeps terrain, optionally imports biome set/weights.
- Don’t reimplement TB MultiNoise inside TF climate unless replacing `Source` entirely.

## 9. Version discipline

- Study newer sources for **algorithms**.
- Reimplement against **target MC mappings** (1.18.2).
- Zip names lie (Tectonic “1.18.2” zip is 1.19+).

## 10. Debug truth chain (unchanged)

1. Quart/section biome paint  
2. Carve / air + solid floor  
3. Decor origin  
4. Filter allowed  
5. Actual `place` / DT soil  

Command: `/newtf debug cave save`

---

## Template for a new subsystem

1. **Invariant** — what must always be true (e.g. “no air column under river bed”).
2. **Authority** — which pass may write blocks/biomes.
3. **Inputs** — seed, TerrainData, CarverChunk, biome holder.
4. **Outputs** — blocks, biome paint, feature seeds.
5. **Non-interactions** — what it must not touch (open cave air, player structures).
6. **Gate** — `GenerationFeatureGates` flag default off until playtested.
7. **Retire plan** — what older plaster this replaces.
