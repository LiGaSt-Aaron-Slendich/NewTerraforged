# Reference mods (1.18.2 sources)

GitHub checkouts for comparing “normal” biome/worldgen mods with NewTerraForged.

| Mod | Path | Branch / ref | Role |
|-----|------|--------------|------|
| TerraBlender | `TerraBlender/` | `TB-1.18.2-1.x.x` | Region injection, namespaced surface rules |
| Terralith | `Terralith/` | `1.18.2-mod` | TB region + datapack biomes |
| Biomes O' Plenty | `BiomesOPlenty/` | tag `18.2.0` | TB regions + BOP surface rules |
| Regions Unexplored | `RegionsUnexplored/` | default branch | TB regions, vanilla surface |

## Re-clone

```powershell
$ref = "docs/reference-mods"
git clone --depth 1 --branch TB-1.18.2-1.x.x https://github.com/Glitchfiend/TerraBlender.git "$ref/TerraBlender"
git clone --depth 1 --branch 1.18.2-mod https://github.com/Stardust-Labs-MC/Terralith.git "$ref/Terralith"
git clone --depth 1 --branch 18.2.0 https://github.com/Glitchfiend/BiomesOPlenty.git "$ref/BiomesOPlenty"
git clone --depth 1 https://github.com/UHQ-GAMES-MODS/REGIONS_UNEXPLORED_FORGE.git "$ref/RegionsUnexplored"
```

Do **not** decompile these JARs — use sources only.

## Exploration zips (sibling folder)

Unpacked study notes from `../for exploration/*.zip` live in [`docs/exploration/`](../exploration/README.md) (TerraForged 0.3, Tectonic, TerraBlender, Terralith, Dynamic Trees compat).
