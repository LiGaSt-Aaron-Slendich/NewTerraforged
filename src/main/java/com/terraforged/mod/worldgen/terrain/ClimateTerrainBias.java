package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Climate → landform gate: arid landforms (badlands / mesa) only keep their WeightMap
 * slot when temperature+moisture suit them. Otherwise the noise index is remapped onto
 * hills so <em>height and terrain type</em> both change — not a paper-only biome ban.
 */
public final class ClimateTerrainBias {
    private ClimateTerrainBias() {
    }

    /**
     * 0 = forbid arid landforms (cold / temperate wet), 1 = fully allow (hot dry).
     */
    public static float aridSuitability(float temperature, float moisture) {
        BiomeType type = BiomeType.get(temperature, moisture);
        if (type == null) {
            return 0.35F;
        }
        return switch (type) {
            case TUNDRA, TAIGA, ALPINE, COLD_STEPPE -> 0.0F;
            case TEMPERATE_FOREST, TEMPERATE_RAINFOREST -> 0.08F;
            case GRASSLAND -> 0.18F;
            case STEPPE -> 0.42F;
            case SAVANNA -> 0.78F;
            case DESERT -> 1.0F;
            case TROPICAL_RAINFOREST -> 0.35F;
            default -> 0.25F;
        };
    }

    public static boolean isAridLandform(Terrain terrain) {
        if (terrain == null) {
            return false;
        }
        if (terrain == TerrainType.BADLANDS) {
            return true;
        }
        String n = terrain.getName();
        return n != null && ("badlands".equalsIgnoreCase(n) || n.toLowerCase().contains("badland"));
    }

    /**
     * Remap WeightMap noise index so arid bands collapse toward hills when unsuitable.
     */
    public static float biasNoiseIndex(float noise, float temperature, float moisture, WeightMap<TerrainNoise> terrains) {
        if (terrains == null || terrains.isEmpty()) {
            return noise;
        }
        float n = NoiseUtil.clamp(noise, 0.0F, 0.9999F);
        TerrainNoise picked = terrains.getValue(n);
        if (picked == null || !isAridLandform(picked.terrain())) {
            return n;
        }
        float suit = aridSuitability(temperature, moisture);
        if (suit >= 0.92F) {
            return n;
        }
        float hills = hillsIndex(terrains);
        if (suit <= 0.05F) {
            return hills;
        }
        // Soft: pull mesa picks toward hills as climate cools / wets.
        return NoiseUtil.lerp(hills, n, suit);
    }

    private static float hillsIndex(WeightMap<TerrainNoise> terrains) {
        TerrainNoise[] values = terrains.getValues();
        float best = 0.35F;
        float cursor = 0.0F;
        float sum = 0.0F;
        for (TerrainNoise tn : values) {
            if (tn != null) {
                sum += Math.max(0.0F, tn.weight());
            }
        }
        if (sum < 1.0E-4F) {
            return best;
        }
        for (TerrainNoise tn : values) {
            if (tn == null) {
                continue;
            }
            float w = Math.max(0.0F, tn.weight());
            float mid = (cursor + w * 0.5F) / sum;
            cursor += w;
            Terrain t = tn.terrain();
            if (t != null) {
                String name = t.getName();
                if (t == TerrainType.HILLS || (name != null && name.toLowerCase().contains("hills"))) {
                    return NoiseUtil.clamp(mid, 0.0F, 0.9999F);
                }
                if (t == TerrainType.FLATS || (name != null && ("plains".equalsIgnoreCase(name) || "steppe".equalsIgnoreCase(name)))) {
                    best = mid;
                }
            }
        }
        return NoiseUtil.clamp(best, 0.0F, 0.9999F);
    }
}
