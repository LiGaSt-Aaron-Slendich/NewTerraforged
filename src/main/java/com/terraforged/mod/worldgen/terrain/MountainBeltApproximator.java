package com.terraforged.mod.worldgen.terrain;

import com.terraforged.noise.util.NoiseUtil;

/**
 * Soft valley-fill for selective mega-ridge corridors only.
 */
public final class MountainBeltApproximator {
    private static final float MAX_VALLEY_DEPTH = 0.048F;
    private static final float NATURAL_FIELD_DROP = 0.12F;
    private static final float FILL_STRENGTH = 0.82F;

    private MountainBeltApproximator() {
    }

    public static float fillValleyDepth(float height, float worldX, float worldZ, long seed, int continentScale) {
        return fillValleyDepth(height, worldX, worldZ, seed, continentScale, 0.72F);
    }

    public static float fillValleyDepth(float height, float worldX, float worldZ, long seed, int continentScale,
                                        float continentNoise) {
        int scale = Math.max(400, continentScale);
        float center = MountainBeltField.strength(worldX, worldZ, seed, scale, continentNoise);
        float spacing = Math.max(28.0F, scale * 0.014F);

        float localMax = center;
        localMax = Math.max(localMax, MountainBeltField.strength(worldX + spacing, worldZ, seed, scale, continentNoise));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX - spacing, worldZ, seed, scale, continentNoise));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX, worldZ + spacing, seed, scale, continentNoise));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX, worldZ - spacing, seed, scale, continentNoise));
        float d = spacing * 0.72F;
        localMax = Math.max(localMax, MountainBeltField.strength(worldX + d, worldZ + d, seed, scale, continentNoise));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX - d, worldZ + d, seed, scale, continentNoise));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX + d, worldZ - d, seed, scale, continentNoise));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX - d, worldZ - d, seed, scale, continentNoise));

        // Active mega-ridge corridor only (sparse field — higher bar than old softRidge).
        if (localMax < 0.42F) {
            return height;
        }

        float fieldDrop = localMax - center;
        if (fieldDrop <= NATURAL_FIELD_DROP) {
            return height;
        }

        float excess = NoiseUtil.clamp((fieldDrop - NATURAL_FIELD_DROP) / 0.38F, 0.0F, 1.0F);
        float softPeak = localMax * localMax * (3.0F - 2.0F * localMax);
        float impliedPeak = NoiseUtil.lerp(0.48F, 0.80F, softPeak * 0.72F);
        float floor = impliedPeak - MAX_VALLEY_DEPTH;

        if (height >= floor) {
            return height;
        }

        float filled = NoiseUtil.lerp(height, floor, FILL_STRENGTH * excess);
        return NoiseUtil.clamp(Math.min(filled, impliedPeak - 0.012F), 0.0F, 1.0F);
    }
}
