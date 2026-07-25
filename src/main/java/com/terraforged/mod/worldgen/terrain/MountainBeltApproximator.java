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
        // Sparse field: most land is zero — skip neighbor probes.
        if (center < 0.18F) {
            return height;
        }
        float spacing = Math.max(48.0F, scale * 0.022F);

        // 2-axis envelope — enough for notch detect, cheaper chunk gen.
        float localMax = center;
        localMax = Math.max(localMax, MountainBeltField.strength(worldX + spacing, worldZ, seed, scale, continentNoise));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX, worldZ + spacing, seed, scale, continentNoise));

        if (localMax < 0.35F) {
            return height;
        }

        float fieldDrop = localMax - center;
        if (fieldDrop <= NATURAL_FIELD_DROP) {
            return height;
        }

        float excess = NoiseUtil.clamp((fieldDrop - NATURAL_FIELD_DROP) / 0.38F, 0.0F, 1.0F);
        float softPeak = localMax * localMax * (3.0F - 2.0F * localMax);
        float impliedPeak = NoiseUtil.lerp(0.58F, 0.92F, softPeak * 0.80F);
        float floor = impliedPeak - MAX_VALLEY_DEPTH;

        if (height >= floor) {
            return height;
        }

        float filled = NoiseUtil.lerp(height, floor, FILL_STRENGTH * excess);
        return NoiseUtil.clamp(Math.min(filled, impliedPeak - 0.012F), 0.0F, 1.0F);
    }
}
