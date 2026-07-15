# Worldgen research audit — 2026-07-15

NewTerraForged 1.18.2 vs Terraforged engine JAR vs TerraBlender ecosystem.

## Executive summary

**Чому “нормальні” моди не ламаються, а наш — так:** Terralith, BOP, Regions Unexplored **не замінюють terrain**. Вони реєструють біоми через **TerraBlender regions** і (де треба) **namespaced surface rules**, залишаючи vanilla `NoiseBasedChunkGenerator` + `MultiNoiseBiomeSource`. NewTerraForged **повністю замінює** `ChunkGenerator` (`Generator`), heightfield, річки, печери та малювання біомів (`Source`). Це інша архітектура — більшість багів (void під річками, mega+river, biome на березі, bridges) — наслідок **terrain pipeline + post-hoc repair**, а не одного “неправильного” carver.

**Engine JAR vs mod:** `libs/Terraforged.jar` — це **noise + engine types + tile/rivermap API**, але mod **не використовує** engine tile generation (`WorldGenerator`, `rivermap.*`). Річки реалізовані заново в `mod.worldgen.noise.continent.river.*`. Ризик “роз’їхалися з рушієм” стосується переважно **noise/biome types**, не повного engine worldgen.

---

## 1. Що збережено в репо

| Артефакт | Шлях |
|----------|------|
| Декомпільований engine (329 класів) | `docs/decompiled/terraforged-engine/` |
| Engine README (SHA1, CFR) | `docs/decompiled/terraforged-engine/README.md` |
| Reference mod clone instructions | `docs/reference-mods/README.md` |
| Code master switches | `GenerationFeatureGates.java` |

Engine SHA1: `72d6fb1c69f891b815b756787580e1d6a813ef11`

Reference mod sources клоновані локально (не комітимо — занадто великі); інструкції в `docs/reference-mods/README.md`.

---

## 2. Pipeline NewTerraForged (фактичний порядок)

```
createBiomes     → Source.getNoiseBiome (2D climate, y≈ignored)
fillFromNoise    → TerrainCache → ChunkUtil.fillChunk
buildSurface     → SurfaceDecorator (vanilla SurfaceSystem) + Surface.apply
applyCarvers AIR → prepareCarverChunk; block carve deferred if flag set
decorate:
  1. restoreRiverDepressions     [river void fill — terrain bug plug]
  2. applyCarveBlocks            [MEGA/GIGA/synapse]
  3. surface features
  4. smoothWater, applyPost
  5. RiverShoreBiomeClip
  6. structures
  7. cave volume decor (official TF default)
  8. repairExposedCover
  9. cave entrances
 10. integrity pass (off by default)
 11. finishDecorate
```

Ключові файли: `Generator.java`, `BiomeGenerator.java`, `NoiseCaveGenerator.java`, `CaveChunkSurfaceRepair.java`.

---

## 3. TerraBlender ecosystem (1.18.2)

### TerraBlender (бібліотека)

- **Не чіпає** `ChunkGenerator`.
- Mixin на `MultiNoiseBiomeSource`, `Climate.ParameterList`, `NoiseGeneratorSettings`.
- Mods subclass `Region`, `Regions.register()`.
- Index 0 = vanilla overworld parameters (`DefaultOverworldRegion`).
- Surface: `SurfaceRuleManager.addSurfaceRules()` → `NamespacedSurfaceRuleSource`.

### Terralith / BOP / RU

| Mod | ChunkGen | Biomes | Surface rules |
|-----|----------|--------|---------------|
| Terralith | Vanilla | TB region + datapack | TB namespaced from overworld.json |
| BOP | Vanilla | 5 TB regions | BOP `BOPSurfaceRuleData` |
| RU (0.4.x) | Vanilla | 3 TB regions | Vanilla (no TB surface in audit commit) |

### NewTerraForged

- `TerraBlenderCompat` — **detect only**, no runtime hook.
- Mod biomes (Terralith/BOP/RU) потрапляють через `BiomeMapManager` + TOML, **не** через TB regions.
- На vanilla/TB world type NTFG generator **не активний** — mod biomes там працюють як у ванілі.

**Висновок:** порівнювати NTFG з Terralith напряму некоректно — різний шар абстракції. TB-моди “стабільні”, бо terrain один (vanilla).

---

## 4. Engine JAR vs mod — розбіжності

### Використовується з JAR

- `com.terraforged.noise.*` — noise graph (mod `NoiseGenerator`, `RiverGenerator` warp, erosion)
- `com.terraforged.engine.world.terrain.Terrain`, `TerrainType`
- `com.terraforged.engine.world.biome.type.BiomeType`, `BiomeTypeLoader`
- `ControlPoints`, `GeneratorContext`, `Settings`, `PosUtil`

### Не використовується (mod має власну реалізацію)

| Engine (decompiled) | Mod equivalent |
|---------------------|----------------|
| `world.rivermap.RiverGenerator` / `Rivermap` | `noise.continent.river.RiverGenerator` |
| `world.rivermap.river.RiverCarver` (TerrainPopulator) | `RiverCarver` on `NoiseSample` |
| `tile.*`, `WorldGenerator`, chunk writers | `TerrainCache`, `ChunkUtil.fillChunk` |
| `world.continent.fancy.*` | `ContinentGenerator`, `ShapeGenerator` |

Engine `RiverCarver.carve(Cell, …)` працює на **Cell** у tile pipeline; mod `RiverCarver.carve(NoiseSample, …)` — на **height field 18×18**. Логіка схожа (valley/bed/bank alpha), але **не той код** — drift можливий при зміні constants/curves.

### Відомі engine pitfalls (з попередніх сесій)

- `BiomeTypeLoader.generateTypeMap()` — `ImageIO.read(null)` якщо bitmap resource missing з classpath mod (не engine JAR alone).

---

## 5. Чому ламається (root causes)

### 5.1 Terrain voids → river fill band-aid

Symptom: порожнечі / “різання” під річками, mega tunnels під channel.

Cause: heightfield + river carve в noise (`RiverCarver`) інколи дає **discontinuous column**; caves не “різуть річку” — вони потрапляють у вже існуючий void.

Mitigation today: `restoreRiverDepressions` **перед** block carve (`deferBlockCarveUntilAfterRiverFill=true`).

**Правильний fix (rewrite):** зробити terrain column continuous at river mask — fill тоді лише top-up water, не strata plug.

### 5.2 Занадто багато biome paint passes

Quart biomes змінюються в: carve halo, `CaveDecoratePaint`, `CaveSurfaceBiomeRestorer`, `RiverShoreBiomeClip`, entrance decor, integrity pass. F3 / decor anchor / filter легко розходяться (див. retro truth chain).

### 5.3 П’ять cave decor backends

legacy / compromise / vanilla / official / per-biome — великий dead code surface. Audit: **лише official** (`GenerationFeatureGates.legacy/compromise = false`).

### 5.4 Mega + river

`riverCarveBlocked` / column cache — hard skip може лишати артеfacts; soft taper vs fill order still debated.

### 5.5 Mod biome surface rules

Terralith BOP surface rules **не застосовуються** на NTFG world — лише vanilla overworld surface + TF post-pass. “Bare dirt beaches”, wrong blocks — частково через це.

---

## 6. GenerationFeatureGates (code disables)

Файл: `src/main/java/com/terraforged/mod/worldgen/GenerationFeatureGates.java`

| Gate | Default (audit) | Effect |
|------|-----------------|--------|
| `legacyCaveDecoratorsEnabled` | **false** | Skip legacy volume decor |
| `compromiseCaveDecoratorsEnabled` | **false** | Skip compromise decor |
| `vanillaCavePassEnabled` | true | Vanilla placed feature cave pass |
| `synapseCavesEnabled` | true | GLOBAL/synapse carve |
| `megaGigaCavesEnabled` | true | MEGA/GIGA carve |
| `caveFloatingCrustStripEnabled` | true | Strip floating soil after mega decor |
| `riverVoidFillEnabled` | true | `restoreRiverDepressions` |
| `riverShoreBiomeClipEnabled` | true | Land biome on dry shores |
| `chunkIntegrityPassEnabled` | **false** | Full re-decorate integrity |

Існуючі gates: `CaveCarvingGate`, `CaveChunkSurfaceRepair.riverDepressionRestoreEnabled`, `CaveRiverEntranceHydrator` (false).

---

## 7. План перезборки (recommended phases)

### Phase A — Baseline (зараз)

- [x] Engine decompile in repo
- [x] Reference mod study (TB stack)
- [x] `GenerationFeatureGates` + audit doc
- [ ] Playtest **new chunks** with gates above

### Phase B — Terrain truth

1. Diff mod `RiverGenerator`/`RiverCarver` vs engine `rivermap` (decompiled) — document constant/curve deltas.
2. Column continuity test at river cells (debug command / save slice).
3. Reduce `restoreRiverDepressions` scope once terrain fixed.

### Phase C — Biome layer simplification

1. Single quart paint authority after carve (merge restorer + shore clip logic).
2. Optional: TB-style **parameter regions** for mod biomes only (keep TF terrain) — larger refactor.

### Phase D — Cave simplification

1. Delete path only after gate-proven stable — until then gates only.
2. One decor backend (official).
3. River/mega intersection: taper `riverCarveBlocked` + playtest mega zones.

### Phase E — Mod compat

1. For integrated Terralith/BOP biomes: optional per-namespace surface rule hook (TB `SurfaceRuleManager` pattern without replacing Generator).
2. Document: TB world type ≠ NTFG world type for testing.

---

## 8. Debug truth chain (unchanged)

1. Quart paint at floor  
2. Carve / column air + solid floor  
3. Decor grid anchor  
4. Filter allowed  
5. Actual place  

Command: `/newtf debug cave save` — **At feet | At anchor**.

---

## 9. Related docs

- `docs/retro-sessions/2026-07-03-cave-decor.md`
- `docs/biome-terrain-review.md`
- `.cursor/rules/cave-work-retro.mdc`
