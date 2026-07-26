# NewTerraForged 1.19.2 port status

**Date:** 2026-07-26  
**Tooling:** Minecraft **1.19.2** / Forge **43.5.0** (true 1.19.2, not 1.19/41)  
**Version:** `0.4.4-alpha-1` · modId `newterraforged`

## Export

`C:\Users\Апро\Projects\NewTerraForged\export\NewTerraForged-1.19.2-0.4.4.jar`

Built from: `build/libs/NewTerraForged-1.19.2-0.4.4-alpha-1.jar`

## Included (full overlay on 1.19.2 APIs)

- Stock TF 1.19.2 lifecycle (`CommonAPI` / `TFMain` / `RandomState`)
- NewTF caves: carve + Official decorator pipeline  
  (`TerraForgedOfficialCaveDecorator`, `CaveBiomeFeatureRunner`, `CaveBiomeVolumeDecorator`,
  `CaveBiomeVanillaPass`, `CaveFloorCover`, mega accent / tunnel-river decorators)
- `ChunkScopedWorldGenLevel` adapted to 1.19.2 (`RandomSource`, `gameEvent` Context APIs)
- EGF / Untested: `TFNoiseVariantFlags`, `NvFlagPanel`, islands / archipelago / Coastal LIA / ocean landscape
- Customize GUI / preview / biome rules (ported)

## Quarantined (debug / conflicting 1.18 entrypoints)

`_quarantine_newtf118/` — old TFMain/Common/Platform, carve-decision / feature diagnostics that need Generator debug hooks not yet ported.

## Notes

- Default cave decorate mode follows `CaveDecorationSettings` / `TFCaveBiomeConfig` (Official when enabled).
- Some experimental gates (`GenerationFeatureGates`) remain off by default (e.g. tunnel rivers).
