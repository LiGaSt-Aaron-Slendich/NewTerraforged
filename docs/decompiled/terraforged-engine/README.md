# Terraforged engine (decompiled)

Decompiled from `libs/Terraforged.jar` for diff against NewTerraforged mod sources.

| Field | Value |
|-------|-------|
| Source JAR | `libs/Terraforged.jar` |
| SHA1 | `72d6fb1c69f891b815b756787580e1d6a813ef11` |
| Size | 667 849 bytes |
| Tool | CFR 0.152 (`tools/cfr.jar`) |
| Date | 2026-07-15 |
| Classes | 329 `.java` files |

## Top-level packages

- `com.terraforged.cereal` — serialization
- `com.terraforged.engine` — continent, rivers, terrain, biomes, tiles, heightmap
- `com.terraforged.noise` — noise graph (Module, Source, Domain, …)

## How to re-run

```powershell
java -jar tools/cfr.jar libs/Terraforged.jar --outputdir docs/decompiled/terraforged-engine
```

Deobfuscator3000 on this machine is GUI-only; CFR gives readable sources for engine diff.

## Mod vs engine usage

NewTerraforged **uses** from the JAR:

- `com.terraforged.noise.*` — full noise library
- `com.terraforged.engine.world.terrain.*` — `Terrain`, `TerrainType`, land forms
- `com.terraforged.engine.world.biome.type.*` — `BiomeType`, `BiomeTypeLoader`
- `com.terraforged.engine.settings.*`, `GeneratorContext`, `ControlPoints`, …

NewTerraforged **does not call** (reimplemented in mod):

- `com.terraforged.engine.world.rivermap.*` — engine tile rivermap pipeline
- `com.terraforged.engine.world.WorldGenerator` / tile chunk writers
- `com.terraforged.engine.tile.*` — async tile generation

See `docs/audit/2026-07-15-worldgen-research.md` for full comparison.
