# TerraForged 0.3.x build notes

Built: 2026-07-18  
Output: `build/libs/TerraForged-1.19-0.4.0-alpha-1.jar`

## Important

- This source tree is **Minecraft 1.19** (Forge 41.0.22), not 1.18.2.
- Official `Engine` GitHub repo is gone (404); `io.terraforged.com` maven does not resolve.
- Engine libs (`cereal` / `engine` / `noise`) were extracted from  
  `NewTerraforged/libs/Terraforged.jar` → `lib/Engine-0.3.0.jar`.

## API shims (required)

Old Engine jar uses `Noise.getValue(float,float)` / `Domain.getX(float,float)`.  
0.3.x source expects seed overloads. Shims live in:

- `src/main/java/com/terraforged/noise/Noise.java`
- `src/main/java/com/terraforged/noise/domain/Domain.java`
- `src/main/java/com/terraforged/engine/world/terrain/TerrainType.java` (`getOrCreate`)

Plus small bridges in `UniqueCaveDistributor` / `TerrainBlender`.

## Rebuild

```bat
gradlew.bat jar -x test
```
