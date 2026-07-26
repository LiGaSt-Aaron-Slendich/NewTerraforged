package com.terraforged.mod.worldgen.biome;

/**
 * Height bands for surface climate / vegetation / stone caps (normalized heightNoise 0..1).
 * Block Y ≈ heightNoise × worldHeight (default worldHeight=640 → Y below).
 *
 * <p>Mountain subterrains ({@link com.terraforged.mod.worldgen.biome.rules.SubterrainResolver}):
 * <ul>
 *   <li>foothill — heightNoise ≤ {@link #FOOTHILL_MAX} (0.48 → ~Y 307 @ 640)</li>
 *   <li>body — between foothill and peak</li>
 *   <li>peak — heightNoise ≥ {@link #PEAK_MIN} (0.78 → ~Y 499 @ 640)</li>
 * </ul>
 * Alpine climate is only forced on the peak band (unless the cell is already Tundra).
 */
public final class HeightClimateZones {
    /** Peak band — Alpine climate + peak subterrains. */
    public static final float ALPINE_HEIGHT = 0.78F;
    /** Trees skip placement near peak band (body upper). Was 0.70 — too low on maxY 640. */
    public static final float TREELINE_HEIGHT = 0.82F;
    /** Top ~13% of the height range: dirt/grass stripped to stone. */
    public static final float STONE_CAP_HEIGHT = 0.87F;
    /** Below this on mountain landforms → foothill subterrain. */
    public static final float FOOTHILL_MAX = 0.48F;
    /** At/above this on mountain landforms → peak subterrain. */
    public static final float PEAK_MIN = ALPINE_HEIGHT;

    private HeightClimateZones() {
    }

    public static boolean isAlpine(float heightNoise) {
        return heightNoise >= ALPINE_HEIGHT;
    }

    public static boolean isAboveTreeline(float heightNoise) {
        return heightNoise >= TREELINE_HEIGHT;
    }

    public static boolean isStoneCap(float heightNoise) {
        return heightNoise >= STONE_CAP_HEIGHT;
    }

    /** Convert TerrainData scaled height (≈ block Y) back to 0..1 noise for zone checks. */
    public static float noiseFromScaled(float scaledHeight, int maxY) {
        if (maxY <= 0) {
            return 0.0F;
        }
        float n = scaledHeight / (float) maxY;
        return n < 0.0F ? 0.0F : (n > 1.0F ? 1.0F : n);
    }
}
