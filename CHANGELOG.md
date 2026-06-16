# Changelog

All notable changes to NewTerraForged are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.4.3] — 2026-06-16

Release build for Minecraft **1.18.2** (Forge 40.x). Focus: hybrid cave decoration, mega/giga stability, and chunk-load performance on large modpacks.

### Added

- **Hybrid per-biome decorator routing** — `decorator-routing.toml` assigns `official`, `vanilla`, `legacy`, or `compromise` per cave biome (karst/icicle → official TF; fungal/scorching → compromise/legacy; etc.).
- **Critical Options** config folder with hybrid routing and decoration toggles.
- **Cave cartography** helpers and debug commands for biome/feature inspection.
- **Surface biome sanitizer** — reduces surface vegetation/trees leaking into cave columns.
- **Feature integrity pass** on synapse columns (floating tree/shrub cleanup).
- **Redstone ceiling scatter** in large mega/giga halls (≥20 blocks high).
- **Sulfur river** blocked in code and default biome list.
- **`condition_relax`** documented in default `cave-biomes.toml` (stat threshold relaxation for biome variety in mega/giga shells).

### Changed

- **Default `caves.toml`** tuned from playtesting: `cave_percent = 70`, wider transitions (`transition_max_width_blocks = 40`), larger sky islands (`island_max_radius_chunks = 3`), leaner mega shell counts — trims post-flatten cave fragments and improves load times.
- **Official TerraForged decorator** used for routed biomes (dripstone, karst, icicle, empty stone) — major chunk-decor speedup vs full legacy scatter.
- **Mega/giga carving** — aggressive envelope, river shift, border synapse fix (no forced global carve on mega borders).
- **Stalactite/dripstone** — bounded budget, compromise routing, dripstone filters in scorching/fungal zones.
- **Fungal cover density** restored after perf trims; anchor grid tuned (Concept_13).
- **Performance** — decoration flag cache, reduced duplicate scatter passes, trimmed flat-wall repair / chunk integrity body, feature integrity only on synapse.

### Fixed

- Floating cave trees and mushrooms (floor snap, shelf detection, late integrity pass).
- Surface holes and biome paint leaks at cave entrances.
- Mushroom/stalactite overlap in fungal zones (partial).
- Scorching leaf/shrub bleed (Concept_13 filters — `isSurfaceShrubOrLeaves`).
- Synapse regression from border column global carve (Concept_8–12).
- Empty Frostfire pockets and hybrid routing edge cases (partial — see Known issues).

### Reverted

- **Concept_14** decor hotfix (fungal/mycotoxic split, brimstone legacy, sky-island shelter) — reverted in **Concept_15** due to missing features and leaf regression in heat biomes.

### Known issues (planned for 0.4.4)

- Fungal biome: Terralith mushrooms sparse; Regions Unexplored mycotoxic mushrooms can dominate transition zones.
- **Frostfire** caves often look empty — no dedicated generator; needs compromise-decor tuning.
- **Brimstone** (BYG): sparse BYG features in overworld-painted shells.
- Occasional **decor dead zones** under floating sky islands or at biome borders.
- Some **stalactite/mushroom** overlap remains in dense fungal halls.

### Performance (reference, modpack-dependent)

- ~**104 s** / 32 chunks (TerraForgedTest instance, hybrid + official routing) vs early builds at **1200–2400 s** / 16 chunks.

---

## [0.4.3-beta] — earlier

See project commits from `db28035` through `ea1fc16` for beta/hotpatch history.
