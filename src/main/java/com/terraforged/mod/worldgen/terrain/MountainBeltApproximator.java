package com.terraforged.mod.worldgen.terrain;

import com.terraforged.noise.util.NoiseUtil;

/**
 * Soft notch fill for mega-ridge corridors — deep cuts only, never mesa floors.
 */
public final class MountainBeltApproximator {
    private static final float MAX_VALLEY_DEPTH = 0.085F;
    private static final float NATURAL_FIELD_DROP = 0.18F;
    private static final float FILL_STRENGTH = 0.38F;

    private MountainBeltApproximator() {
    }

    public static float fillValleyDepth(float height, float worldX, float worldZ, long seed, int continentScale) {
        return fillValleyDepth(height, worldX, worldZ, seed, continentScale, 0.72F);
    }

    public static float fillValleyDepth(float height, float worldX, float worldZ, long seed, int continentScale,
                                        float continentNoise) {
        int scale = Math.max(400, continentScale);
        float center = MountainBeltField.strength(worldX, worldZ, seed, scale, continentNoise);
        if (center < 0.22F) {
            return height;
        }
        // Crest / tip already peaked — do not raise toward a flat local max.
        if (center > 0.58F) {
            return height;
        }
        float spacing = Math.max(64.0F, scale * 0.035F);

        float localMax = center;
        localMax = Math.max(localMax, MountainBeltField.strength(worldX + spacing, worldZ, seed, scale, continentNoise));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX, worldZ + spacing, seed, scale, continentNoise));

        if (localMax < 0.40F) {
            return height;
        }

        float fieldDrop = localMax - center;
        if (fieldDrop <= NATURAL_FIELD_DROP) {
            return height;
        }

        float excess = NoiseUtil.clamp((fieldDrop - NATURAL_FIELD_DROP) / 0.45F, 0.0F, 1.0F);
        // Floor tracks a sloped envelope under the local peak — not a constant mesa.
        float softPeak = localMax * localMax;
        float impliedPeak = NoiseUtil.lerp(height, NoiseUtil.lerp(0.48F, 0.78F, softPeak), 0.55F);
        float floor = impliedPeak - MAX_VALLEY_DEPTH;

        if (height >= floor) {
            return height;
        }

        float filled = NoiseUtil.lerp(height, floor, FILL_STRENGTH * excess);
        return NoiseUtil.clamp(Math.min(filled, impliedPeak - 0.02F), 0.0F, 1.0F);
    }
}
