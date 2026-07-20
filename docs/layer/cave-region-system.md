# Cave region system (mega / giga)

Combined design: earlier config/layout work + design notes (regions, transitions, islands, climate).

## Goals

| Rule | Behavior |
|------|----------|
| PRIMARY regions | Large radial sectors; **one biome per region** |
| Count | Mega **5–8**, giga **up to 8** (config `region_count_*`) |
| Transitions | Buffer biomes between neighbours when temperature span is large **or** plant-like ↔ aggressive |
| Islands | SPECIAL `island_patch` (crystal, prismachasm, …); **≤ `island_max_per_region` (default 4)** |
| Climate | `CaveClimateType` (FROST / DRY / WET / NORMAL) + per-biome `temperature` for affinity / transitions |
| Stats vs features | Stats may still colour **layout/climate debug**; they **must not** scale or gate **feature placement** |

## Config

- `Cave_configs/caves.toml` — region counts, `transition_per_region`, `island_max_radius_chunks`, `island_max_per_region`
- `Cave_configs/cave-biomes.toml` — `[[primary]]` / `[[transition]]` / `[[special]]` / `[[coastal]]`, `temperature`, `placement_type`

## Code map

| Class | Role |
|-------|------|
| `CaveMegaGigaLayout` | Sectors, generators, climate affinity, island overlay in `getBiomeAt` |
| `CaveRegionMap` / `CaveBiomeSampler` | Per-system cache → paint via `getUnderGroundBiome` |
| `CaveBiomeRegistry` | Pools + `findTransitionBetween` / plant↔aggressive buffer |
| `CavePatchPlacer` | Island slot math (shared with layout) |
| `CaveClimateType` / `CaveTemperatureCalculator` | System climate |

## Plant ↔ aggressive

- **Plant-like:** fungal / jungle / mossy / glowshroom / undergarden / bioshroom / glowing grotto / …
- **Aggressive:** scorching / volcanic / mantle / brimstone / magma / …
- Neighbouring PRIMARY regions of opposite kinds always get a transition buffer (same path as large temperature gaps).

## Islands

Deterministic slots per PRIMARY host region (sector), capped by `island_max_per_region`. Painted through layout biome sample (no separate feature-density budget).

## Explicitly not stats→features

- `CaveStatSampler.cropGrowthFactor` is always `1.0`
- Biome pick does **not** require `CaveBiomeStats.matches(pool)` (climate affinity + temperature remain)
- Decorators must not reintroduce moisture/fertility feature multipliers
