# Biome - Terrain Review (TerraforgedTest pack)

> Автогенерація для перевірки. Після ревʼю оновимо `biome-terrain-integration.toml`.
> Повторна генерація: `scripts/generate-biome-terrain-review.ps1`

## Коротко (UA)

**Terrain integrator працює** — відкати печер його не чіпали.

| Етап | Клас / файл | Що робить |
|------|-------------|-----------|
| 1 | `SurfaceBiomeClimate` | Зсуває climate pool під рельєф (гори→alpine, badlands→desert, wetland→вологіше) |
| 2 | `surface-biomes.toml` | Вибір біому з climate pool за шумом |
| 3 | `BiomeTerrainIntegration` + `biome-terrain-integration.toml` | Фільтр: якщо біом не підходить під terrain — fallback на інший з того ж pool |
| 4 | `BiomeSampler.getBiomeOverride()` | Океан / пляж / річка (окремо) |

**14 terrain-ів з правилами:** steppe, plains, hills_1/2, dales, plateau, badlands, torridonian, mountains_1/2/3, dolomites, mountains_ridge_1/2.

**289 біомів** у `surface-biomes.toml` (Terralith 82, BYG 71, RU 74, BOP 62).

### Що варто перевірити в першу чергу

1. **Mesa/badlands лише на `badlands` terrain** — зараз blacklist блокує mesa на більшості terrain-ів, але **не всі** mesa-подібні біоми в blacklist:
   - `terralith:bryce_canyon`, `terralith:sandstone_valley`, `regions_unexplored:wooded_arid_mountains` — **немає** в badlands whitelist і **не** заблоковані на steppe/plains/mountains
   - `biomesoplenty:dryland`, `lush_desert`, `regions_unexplored:arid_mountains` — у whitelist badlands, але **також дозволені** на mountains/plains (blacklist їх не містить)
2. **Jungle/rainforest** — заблоковані на hills+, mountains, plateau (окрім steppe/plains/dales)
3. **Swamp/bayou** — заблоковані на hills+, mountains, plateau
4. **Пляжі** — blacklist на суші; реально ставляться через coast override в `getBiomeOverride()`
5. **1 біом у jar без config:** `terralith:glacial_chasm` (alpine cave-like, можливо навмисно виключений)

Колонка **suggested** — евристика за назвою, не закон; орієнтуйся на неї при ревʼю.

---

## Does terrain integrator still work after rollbacks?

**Yes.** Cave rollbacks did not touch this layer. Pipeline:

1. IBiomeSampler.getSample() -> SurfaceBiomeClimate.adjustForTerrain() shifts climate for terrain
2. BiomeSampler.sampleBiome() picks biome from climate pool (surface-biomes.toml)
3. BiomeTerrainIntegration.filter() applies biome-terrain-integration.toml rules / fallback
4. getBiomeOverride() handles ocean / beach / river separately

## Stats

- Biomes in surface-biomes.toml: **289**
- Terrains with rules: **14**
- Surface JSON biomes in jars (Terralith/BOP/RU/WN): **83**
- BYG biomes (config only, no JSON in jar): **71**

## Jar biomes NOT in surface-biomes.toml (1)

- terralith:glacial_chasm

## Mesa/Badlands biomes - badlands terrain only

| biome | climate | in badlands whitelist? | allowed on other terrains? |
|-------|---------|------------------------|----------------------------|
| biomesoplenty:dryland | desert | yes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| biomesoplenty:lush_desert | desert | yes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| biomesoplenty:volcanic_plains | alpine | yes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| byg:sierra_badlands | desert | yes | _blocked_ |
| regions_unexplored:arid_mountains | desert | yes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| regions_unexplored:barley_fields | steppe | yes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| regions_unexplored:outback | desert | yes | _blocked_ |
| regions_unexplored:wooded_arid_mountains | desert | **NO** | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| terralith:bryce_canyon | desert | **NO** | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| terralith:desert_canyon | desert | yes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| terralith:painted_mountains | alpine | yes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| terralith:sandstone_valley | desert | **NO** | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |
| terralith:savanna_badlands | desert | yes | _blocked_ |
| terralith:snowy_badlands | desert | yes | _blocked_ |
| terralith:warped_mesa | desert | yes | _blocked_ |
| terralith:white_mesa | desert | yes | _blocked_ |

### alpine (30)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:crag | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| biomesoplenty:highland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| biomesoplenty:highland_moor | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| biomesoplenty:jade_cliffs | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| biomesoplenty:volcanic_plains | steppe, plains, hills_1, hills_2, dales, plateau, badlands, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |  | badlands |  |
| biomesoplenty:volcano | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:crag_gardens | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:howling_peaks | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:sythian_torrids | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:chalk_cliffs | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:highland_fields | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:mountains | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:spires | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:towering_cliffs | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:basalt_cliffs | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:blooming_plateau | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:emerald_peaks | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:granite_cliffs | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:haze_mountain | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:mountain_steppe | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:painted_mountains | steppe, plains, hills_1, hills_2, dales, plateau, badlands, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |  | badlands |  |
| terralith:rocky_mountains | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:scarlet_mountains | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:stony_spires | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:volcanic_crater | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:volcanic_peaks | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:white_cliffs | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:windswept_spires | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:yosemite_cliffs | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:yosemite_lowlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |

### cold_steppe (9)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:rocky_shrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:shrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:shrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:steppe | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:cold_shrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:hot_shrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:rocky_shrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:shrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:steppe | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |

### desert (35)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:cold_desert | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| biomesoplenty:dryland | steppe, plains, hills_1, hills_2, dales, plateau, badlands, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |  | badlands |  |
| biomesoplenty:dune_beach | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | coast_override | beach (coast override) |
| biomesoplenty:lush_desert | steppe, plains, hills_1, hills_2, dales, plateau, badlands, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |  | badlands |  |
| biomesoplenty:scrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:wasteland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:wooded_scrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:wooded_wasteland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:atacama_desert | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:mojave_desert | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:sierra_badlands | badlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mesa-family |
| byg:warped_desert | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:windswept_desert | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:windswept_dunes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:arid_mountains | steppe, plains, hills_1, hills_2, dales, plateau, badlands, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |  | badlands |  |
| regions_unexplored:dry_bushland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:icy_desert | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:joshua_desert | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:outback | badlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mesa-family |
| regions_unexplored:saguaro_desert | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:wooded_arid_mountains | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | badlands |  |
| terralith:ancient_sands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | badlands |  |
| terralith:arid_highlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:brushland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:bryce_canyon | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | badlands |  |
| terralith:desert_canyon | steppe, plains, hills_1, hills_2, dales, plateau, badlands, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |  | badlands |  |
| terralith:desert_oasis | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:desert_spires | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:gravel_desert | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | badlands |  |
| terralith:red_oasis | steppe, plains, hills_1, hills_2, dales, plateau, badlands, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |  | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:sandstone_valley | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | badlands |  |
| terralith:savanna_badlands | badlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mesa-family |
| terralith:snowy_badlands | badlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mesa-family |
| terralith:warped_mesa | badlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mesa-family |
| terralith:white_mesa | badlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mesa-family |

### grassland (35)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:muskeg | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:rainbow_hills | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:spider_nest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:undergrowth | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:visceral_heap | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:withered_abyss | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:arisian_undergrowth | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:basalt_barrera | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:bulbis_gardens | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:cryptic_wastes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:dacite_ridges | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:dacite_shore | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | coast_override | beach (coast override) |
| byg:dead_sea | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:ethereal_islands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:glowstone_gardens | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:magma_wastes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:rainbow_beach | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | coast_override | beach (coast override) |
| byg:skyris_vale | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:viscal_isles | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:wailing_garth | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:windswept_beach | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | coast_override | beach (coast override) |
| regions_unexplored:alpha_plains | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:cold_river | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:corrupted_holt | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:fen | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains |  |
| regions_unexplored:flooded_plains | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:gravel_beach | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | coast_override | beach (coast override) |
| regions_unexplored:mauve_hills | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:muddy_river | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:pine_slopes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:redstone_hell | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:skylands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:skylands_spring | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:skylands_summer | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:warm_river | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |

### savanna (9)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:lush_savanna | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:araucaria_savanna | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:baobab_savanna | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:baobab_savanna | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:ashen_savanna | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:fractured_savanna | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:gravel_beach | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | coast_override | beach (coast override) |
| terralith:orchid_swamp | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains | swamp-family |
| terralith:savanna_slopes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |

### steppe (27)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:clover_patch | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:field | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:floodplain | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:grassland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:origin_valley | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:pasture | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:prairie | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:allium_fields | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:amaranth_fields | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:coconino_meadow | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:ivis_fields | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:prairie | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:red_rock_valley | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:rose_fields | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| byg:twilight_meadow | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:barley_fields | steppe, plains, hills_1, hills_2, dales, plateau, badlands, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 |  | badlands |  |
| regions_unexplored:blackstone_basin | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:glistering_meadow | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:grassland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:grassy_beach | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | coast_override | beach (coast override) |
| regions_unexplored:lupine_plains | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:meadow | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:poppy_fields | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:prairie | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:blooming_valley | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:valley_clearing | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |
| terralith:yellowstone | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, hills_1, hills_2, dales, plateau |  |

### taiga (30)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:boreal_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:cherry_blossom_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:coniferous_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:fir_clearing | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:mystic_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:snowy_coniferous_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| biomesoplenty:snowy_fir_clearing | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:autumnal_taiga | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:borealis_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:coniferous_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:firecracker_shrubland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:frosted_coniferous_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:frosted_taiga | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:imparius_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:maple_taiga | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:temperate_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:alpha_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:autumnal_mixed_taiga | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:boreal_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:canadian_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:cold_boreal_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:golden_boreal_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:old_growth_boreal_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:temperate_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:alpine_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:birch_taiga | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:moonlight_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:sakura_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:siberian_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:siberian_taiga | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |

### temperate_forest (35)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:dead_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:forested_field | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:mediterranean_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:old_growth_dead_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:ominous_woods | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:pumpkin_patch | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:aspen_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:cika_woods | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:ebony_woods | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:forgotten_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:fragment_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:jacaranda_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:nightshade_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:red_oak_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:shulkren_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:weeping_witch_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:zelkova_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:blackwood_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:cold_deciduous_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:deciduous_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:eucalyptus_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:flowering_oak_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:pine_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:pumpkin_fields | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:roofed_eucalyptus_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:silver_birch_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:willow_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:wooded_steppe | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:alpine_highlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:cloud_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:forested_highlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:highlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:moonlight_valley | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:sakura_valley | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:temperate_highlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |

### temperate_rainforest (41)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:bog | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains |  |
| biomesoplenty:lavender_field | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:lavender_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:maple_woods | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:marsh | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains | swamp-family |
| biomesoplenty:orchard | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:redwood_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:seasonal_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:wetland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains |  |
| byg:autumnal_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:autumnal_valley | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:black_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:canadian_shield | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:cherry_blossom_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:cypress_swamplands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains | swamp-family |
| byg:embur_bog | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains |  |
| byg:guiana_shield | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:lush_stacks | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:orchard | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:redwood_thicket | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:weeping_mire | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:autumnal_fields | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:autumnal_maple_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:cherry_hills | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:flower_field | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:fungal_fen | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains |  |
| regions_unexplored:lush_delta | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:lush_hills | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:maple_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:orchard | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:redwoods | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:sparse_redwoods | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:alpha_islands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:caldera | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:lavender_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:lavender_valley | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:lush_valley | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:mirage_isles | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:shield | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:shield_clearing | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:skylands_autumn | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |

### tropical_rainforest (24)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:bamboo_grove | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| biomesoplenty:bayou | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains | swamp-family |
| biomesoplenty:fungal_jungle | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| biomesoplenty:old_growth_woodland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| biomesoplenty:rainforest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| biomesoplenty:rocky_rainforest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| biomesoplenty:tropics | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales |  |
| biomesoplenty:woodland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| byg:bayou | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains | swamp-family |
| byg:temperate_rainforest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau | jungle-family |
| byg:tropical_rainforest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| byg:white_mangrove_marshes | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains | swamp-family |
| regions_unexplored:bamboo_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| regions_unexplored:bayou | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains | swamp-family |
| regions_unexplored:giant_bayou | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | dales, steppe, plains | swamp-family |
| regions_unexplored:old_growth_rainforest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| regions_unexplored:rainforest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| regions_unexplored:smouldering_woodland | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| regions_unexplored:tropics | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales |  |
| terralith:amethyst_canyon | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:amethyst_rainforest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| terralith:jungle_mountains | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| terralith:rocky_jungle | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |
| terralith:tropical_jungle | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | steppe, plains, dales | jungle-family |

### tundra (14)

| biome | allowed terrains | blocked terrains | suggested | notes |
|-------|------------------|--------------------|-----------|-------|
| biomesoplenty:snowy_maple_woods | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| biomesoplenty:tundra | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| byg:cardinal_tundra | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:frozen_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:frozen_tundra | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| regions_unexplored:icy_heights | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:alpha_islands_winter | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:frozen_cliffs | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:ice_marsh | steppe, plains, hills_1, hills_2, dales, plateau | badlands, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | dales, steppe, plains | swamp-family |
| terralith:skylands_winter | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:snowy_maple_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:snowy_shield | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2, torridonian, hills_2, plateau |  |
| terralith:wintry_forest | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |
| terralith:wintry_lowlands | steppe, plains, hills_1, hills_2, dales, plateau, torridonian, mountains_1, mountains_2, mountains_3, dolomites, mountains_ridge_1, mountains_ridge_2 | badlands | plains, steppe, hills_1, hills_2, dales, plateau |  |

## Potential issues for review

_none found automatically_
