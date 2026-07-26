package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.util.storage.WeightMap;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Climate → landform gate: arid landforms (badlands / mesa) only keep their WeightMap
 * slot when temperature+moisture suit them. Otherwise the noise index is remapped onto
 * hills so <em>height and terrain type</em> both change — not a paper-only biome ban.
 *
 * <p>Hard ban in cold/wet climates (taiga/tundra/alpine/temperate forest). Soft allow
 * only on steppe → savanna → desert. Badlands canyons next to mountains otherwise dig
 * trench walls along region edges.
 */
public final class ClimateTerrainBias {
    private ClimateTerrainBias() {
    }

    /**
     * 0 = forbid arid landforms, 1 = fully allow.
     * Uses both {@link BiomeType} and raw temp/moist so edge climates cannot sneak mesa in.
     */
    public static float aridSuitability(float temperature, float moisture) {
        float t = NoiseUtil.clamp(temperature, 0.0F, 1.0F);
        float m = NoiseUtil.clamp(moisture, 0.0F, 1.0F);
        // Cold or wet → never mesa (taiga/tundra/temperate forest).
        if (t < 0.42F || m > 0.58F) {
            return 0.0F;
        }
        BiomeType type = BiomeType.get(t, m);
        if (type == null) {
            return aridFromRaw(t, m);
        }
        return switch (type) {
            case TUNDRA, TAIGA, ALPINE, COLD_STEPPE, TEMPERATE_FOREST, TEMPERATE_RAINFOREST -> 0.0F;
            case GRASSLAND -> 0.05F;
            case STEPPE -> 0.35F;
            case SAVANNA -> 0.80F;
            case DESERT -> 1.0F;
            case TROPICAL_RAINFOREST -> 0.0F;
            default -> aridFromRaw(t, m);
        };
    }

    private static float aridFromRaw(float t, float m) {
        if (t < 0.50F || m > 0.50F) {
            return 0.0F;
        }
        float heat = smoothstep(0.50F, 0.78F, t);
        float dry = 1.0F - smoothstep(0.28F, 0.55F, m);
        return heat * dry;
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
        if (suit >= 0.85F) {
            return n;
        }
        float hills = hillsIndex(terrains);
        // Hard ban below steppe — no soft residual mesa that digs mountain trenches.
        if (suit <= 0.40F) {
            return hills;
        }
        return NoiseUtil.lerp(hills, n, (suit - 0.40F) / 0.45F);
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
                if (t == TerrainType.PLATEAU || (name != null && name.toLowerCase().contains("plateau"))) {
                    best = mid;
                }
                if (t == TerrainType.FLATS || (name != null && ("plains".equalsIgnoreCase(name) || "steppe".equalsIgnoreCase(name)))) {
                    if (best < 0.34F || best > 0.36F) {
                        // keep plateau preference if already found
                    } else {
                        best = mid;
                    }
                }
            }
        }
        return NoiseUtil.clamp(best, 0.0F, 0.9999F);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = NoiseUtil.clamp((x - edge0) / Math.max(1.0E-4F, edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
