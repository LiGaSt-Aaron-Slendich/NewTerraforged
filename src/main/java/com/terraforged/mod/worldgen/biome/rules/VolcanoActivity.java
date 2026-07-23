package com.terraforged.mod.worldgen.biome.rules;

import com.terraforged.noise.util.NoiseUtil;

/**
 * Per-volcano active vs dormant classification and ragged active-zone edge.
 * Activity is stable across a volcano footprint (64-block cells). Dormant is the majority.
 */
public final class VolcanoActivity {
    /** ~28% of volcano centers are active; the rest are dormant. */
    public static final float ACTIVE_CHANCE = 0.28F;

    private static final int ACTIVITY_SEED = 0xA07CA71;
    private static final int EDGE_SEED = 0x51ACE01;

    private VolcanoActivity() {
    }

    public static boolean isActive(int worldSeed, int volcanoBlockX, int volcanoBlockZ) {
        int cx = volcanoBlockX >> 6;
        int cz = volcanoBlockZ >> 6;
        int h = NoiseUtil.hash2D(worldSeed ^ ACTIVITY_SEED, cx, cz);
        float u = (h & 0xFFFF) / 65535.0F;
        return u < ACTIVE_CHANCE;
    }

    /**
     * Multiplier for active-volcano zone radius (~0.72–1.18) so the ring is noise-ragged,
     * not a perfect circle. Dormant zones use 1.0.
     */
    public static float raggedRadiusScale(int worldSeed, int blockX, int blockZ) {
        int hx = blockX >> 5;
        int hz = blockZ >> 5;
        int h = NoiseUtil.hash2D(worldSeed ^ EDGE_SEED, hx, hz);
        float u = (h & 0xFFFF) / 65535.0F;
        // Mild secondary octave so the edge is less blocky.
        int h2 = NoiseUtil.hash2D(worldSeed ^ EDGE_SEED ^ 0x9E3779B9, blockX >> 3, blockZ >> 3);
        float u2 = (h2 & 0xFFFF) / 65535.0F;
        float mix = u * 0.7F + u2 * 0.3F;
        return 0.72F + 0.46F * mix;
    }

    public static int resolveSeed(com.terraforged.mod.worldgen.noise.INoiseGenerator noise) {
        if (noise == null || noise.getContinent() == null || noise.getContinent().getContext() == null) {
            return 0;
        }
        try {
            return noise.getContinent().getContext().seed.root();
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
