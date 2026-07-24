package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Soft landform pull along mountain belts: foothills → hills/plateau, crest → mountains.
 * Never hard-snaps to a knife-edge mountain WeightMap slot.
 */
public final class MountainBeltBias {
    private MountainBeltBias() {
    }

    public static float biasNoiseIndex(float noise, float belt, WeightMap<TerrainNoise> terrains) {
        if (terrains == null || terrains.isEmpty() || belt < 0.10F) {
            return noise;
        }
        float n = NoiseUtil.clamp(noise, 0.0F, 0.9999F);
        float hills = hillsIndex(terrains);
        float mountains = mountainsIndex(terrains);
        // Mid belt prefers hills; only the crest leans hard into mountains.
        float crest = NoiseUtil.clamp((belt - 0.45F) / 0.50F, 0.0F, 1.0F);
        crest = crest * crest * (3.0F - 2.0F * crest);
        float target = NoiseUtil.lerp(hills, mountains, crest);
        float pull = NoiseUtil.clamp((belt - 0.10F) / 0.70F, 0.0F, 1.0F);
        pull = pull * pull * (3.0F - 2.0F * pull);
        // Soft rewrite — keep some of the underlying landform so ridges don't ignore terrain.
        return NoiseUtil.lerp(n, target, pull * 0.72F);
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
        return indexMatching(terrains, true, 0.55F);
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
            if (t == null) {
                continue;
            }
            String name = t.getName();
            if (t == TerrainType.HILLS || (name != null && name.toLowerCase().contains("hills"))) {
                return NoiseUtil.clamp(mid, 0.0F, 0.9999F);
            }
            if (t == TerrainType.PLATEAU || (name != null && name.toLowerCase().contains("plateau"))) {
                best = mid;
            }
        }
        return NoiseUtil.clamp(best, 0.0F, 0.9999F);
    }

    private static float indexMatching(WeightMap<TerrainNoise> terrains, boolean mountains, float fallback) {
        TerrainNoise[] values = terrains.getValues();
        float best = fallback;
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
            if (mountains && isMountainLandform(tn.terrain())) {
                return NoiseUtil.clamp(mid, 0.0F, 0.9999F);
            }
        }
        return NoiseUtil.clamp(best, 0.0F, 0.9999F);
    }
}
