# NewTerraForged 1.19.2 port status

**Date:** 2026-07-26  
**Base:** local `TerraForged-0.3.x-build` (MC 1.19 / Forge 41.0.22)  
**Overlay:** NewTerraforged 1.18.2 systems (caves+, EGF, ocean/islands, biome rules, Customize GUI, …)

## Done

- Systems inventory: see `docs/systems-inventory-1.19.md` (also in 1.18.2 tree)
- Work tree: `Projects/NewTerraForged-1.19.2/NewTerraforged`
- Branding: modId `newterraforged`, version `0.4.4-alpha-1`
- Legacy shims: `compat/legacy/BiomeCategory`, `BiomeDictionary`
- Stock lifecycle retained (`CommonAPI` / `TFMain`); conflicting NewTF 1.18 entrypoints quarantined under `_quarantine_newtf118/`
- Compile was driven from ~389 → ~25 errors; continent/river APIs diverge (stock 1.19 uses seed overloads)

## Not done (blocker)

Full NewTF overlay does **not** compile cleanly yet. Main remaining mismatches:

1. Continent/river noise APIs (seeded stock 1.19 vs NewTF 1.18)
2. Biome sampler / climate sample signatures
3. Create-world GUI (`WorldPreset` / Customize) for 1.19
4. Some cave entrance helpers / config loaders

## Export

Interim **stock base only** JAR (no full NewTF overlay):

`Projects/NewTerraForged/export/NewTerraForged-1.19.2-0.4.4-BASE-WIP.jar`

Rebuilt full NewTF 1.19.2 JAR will replace this when compile is green.
