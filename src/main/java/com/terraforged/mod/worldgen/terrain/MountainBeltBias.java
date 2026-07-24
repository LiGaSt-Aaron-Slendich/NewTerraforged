package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Pulls WeightMap picks toward mountain landforms along {@link MountainBeltField} spines
 * so belts get mountain heights/types — not just a cosmetic height bump on plains.
 */
public final class MountainBeltBias {
    private MountainBeltBias() {
    }

    public static float biasNoiseIndex(float noise, float belt, WeightMap<TerrainNoise> terrains) {
        if (terrains == null || terrains.isEmpty() || belt < 0.08F) {
            return noise;
        }
        float n = NoiseUtil.clamp(noise, 0.0F, 0.9999F);
        float mountains = mountainsIndex(terrains);
        float t = NoiseUtil.clamp((belt - 0.08F) / 0.72F, 0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);
        return NoiseUtil.lerp(n, mountains, t * 0.94F);
    }

    public static boolean isMountainLandform(Terrain terrain) {
        if (terrain == null) {
            return false;
        }
        if (terrain == TerrainType.MOUNTAINS || terrain == TerrainType.MOUNTAIN_CHAIN) {
            return true;
        }
        try {
            if (terrain.isMountain()) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        String n = terrain.getName();
        return n != null && (n.toLowerCase().contains("mountain") || "dolomites".equalsIgnoreCase(n));
    }

    private static float mountainsIndex(WeightMap<TerrainNoise> terrains) {
        TerrainNoise[] values = terrains.getValues();
        float best = 0.55F;
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
            if (isMountainLandform(tn.terrain())) {
                return NoiseUtil.clamp(mid, 0.0F, 0.9999F);
            }
            Terrain t = tn.terrain();
            if (t != null && (t == TerrainType.HILLS || t == TerrainType.PLATEAU)) {
                best = mid;
            }
        }
        return NoiseUtil.clamp(best, 0.0F, 0.9999F);
    }
}
