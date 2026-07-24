package com.terraforged.mod.worldgen.terrain;

import com.terraforged.noise.util.NoiseUtil;

/**
 * Soft valley-fill for mountain belts: multi-ridge saddles stay, but unrealistically deep
 * notches between parallel ridges are lifted toward a local envelope.
 */
public final class MountainBeltApproximator {
    /** Max allowed heightNoise drop from the local ridge envelope (deeper → fill). */
    private static final float MAX_VALLEY_DEPTH = 0.052F;
    /** Mild saddles below this field-drop stay as-is. */
    private static final float NATURAL_FIELD_DROP = 0.10F;
    private static final float FILL_STRENGTH = 0.88F;

    private MountainBeltApproximator() {
    }

    /**
     * Raise {@code height} when it sits in a deep inter-ridge notch inside a belt corridor.
     *
     * @param worldX block X
     * @param worldZ block Z
     */
    public static float fillValleyDepth(float height, float worldX, float worldZ, long seed, int continentScale) {
        int scale = Math.max(400, continentScale);
        float center = MountainBeltField.strength(worldX, worldZ, seed, scale);
        float spacing = Math.max(28.0F, scale * 0.014F);

        float localMax = center;
        localMax = Math.max(localMax, MountainBeltField.strength(worldX + spacing, worldZ, seed, scale));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX - spacing, worldZ, seed, scale));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX, worldZ + spacing, seed, scale));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX, worldZ - spacing, seed, scale));
        float d = spacing * 0.72F;
        localMax = Math.max(localMax, MountainBeltField.strength(worldX + d, worldZ + d, seed, scale));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX - d, worldZ + d, seed, scale));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX + d, worldZ - d, seed, scale));
        localMax = Math.max(localMax, MountainBeltField.strength(worldX - d, worldZ - d, seed, scale));

        // Only act inside a belt corridor (a nearby ridge is strong).
        if (localMax < 0.22F) {
            return height;
        }

        float fieldDrop = localMax - center;
        if (fieldDrop <= NATURAL_FIELD_DROP) {
            return height;
        }

        // Excess depth beyond a natural saddle → fill amount 0..1.
        float excess = NoiseUtil.clamp((fieldDrop - NATURAL_FIELD_DROP) / 0.38F, 0.0F, 1.0F);

        float softPeak = localMax * localMax * (3.0F - 2.0F * localMax);
        float impliedPeak = NoiseUtil.lerp(0.40F, 0.78F, softPeak * 0.72F);
        float floor = impliedPeak - MAX_VALLEY_DEPTH;

        if (height >= floor) {
            return height;
        }

        float filled = NoiseUtil.lerp(height, floor, FILL_STRENGTH * excess);
        // Never overshoot the local ridge envelope.
        return NoiseUtil.clamp(Math.min(filled, impliedPeak - 0.012F), 0.0F, 1.0F);
    }
}
