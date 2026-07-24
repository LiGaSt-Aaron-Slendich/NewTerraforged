package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.island.IslandScatter;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Post-shape coastal "Little Ice Age" overlay: regional strength mixes African-style
 * straight shores with bay/notch warp on {@code continentNoise} and mild cliff/carve
 * height modulation. Tunables are constants for now (future EGF sliders).
 */
public final class CoastalLiaOverlay {
    /** Master switch — emergency off without removing hooks. */
    public static final boolean ENABLED = true;

    /** Large regions of quiet vs eroded coast (blocks). */
    public static final float REGION_CELL = 2800.0F;
    /** Broad bay / inlet scale (blocks). */
    public static final float BAY_CELL = 420.0F;
    /** Fine coastal notches (blocks). */
    public static final float NOTCH_CELL = 95.0F;
    /** Cliff vs flat phase scale (blocks). */
    public static final float CLIFF_CELL = 160.0F;

    public static final float BAY_AMPLITUDE = 0.048F;
    public static final float NOTCH_AMPLITUDE = 0.016F;
    /** heightNoise lift on rocky phase. */
    public static final float CLIFF_AMPLITUDE = 0.075F;
    /** heightNoise carve on bay/flat phase. */
    public static final float NOTCH_CARVE = 0.040F;

    public static final float COAST_CN_MIN = 0.42F;
    public static final float COAST_CN_MAX = 0.62F;
    public static final float HEIGHT_CN_MIN = 0.48F;
    public static final float HEIGHT_CN_MAX = 0.58F;

    /** cliffPhase above this (with enough region strength) → stony/rocky shore. */
    public static final float ROCKY_CLIFF_THRESHOLD = 0.58F;
    public static final float ROCKY_STRENGTH_MIN = 0.35F;

    private final int seed;

    public CoastalLiaOverlay(int seed) {
        this.seed = seed ^ 0xC0A571A;
    }

    /**
     * Warp {@code continentNoise} near the shoreline after shape + islands.
     *
     * @param worldX worldZ block-equivalent coords (same frame as {@code IslandFeatureOverlay})
     */
    public void applyContinent(float worldX, float worldZ, NoiseSample sample) {
        if (!ENABLED || sample == null) {
            return;
        }
        float cn = sample.continentNoise;
        float mask = bandMask(cn, COAST_CN_MIN, COAST_CN_MAX);
        if (mask <= 0.0F) {
            return;
        }
        float strength = regionStrength(worldX, worldZ);
        if (strength <= 0.02F) {
            return;
        }

        float bay = (IslandScatter.valueNoise2(this.seed ^ 0x0BA11, worldX / BAY_CELL, worldZ / BAY_CELL) * 2.0F) - 1.0F;
        float notch = (IslandScatter.valueNoise2(this.seed ^ 0xA07C1, worldX / NOTCH_CELL, worldZ / NOTCH_CELL) * 2.0F) - 1.0F;
        float delta = (bay * BAY_AMPLITUDE + notch * NOTCH_AMPLITUDE) * strength * mask;

        sample.continentNoise = NoiseUtil.clamp(cn + delta, 0.0F, 1.0F);
        sample.baseNoise = NoiseUtil.clamp(sample.baseNoise + delta * 0.35F, 0.0F, 1.0F);
        sample.terrainType = ContinentPoints.getTerrainType(sample.continentNoise);
    }

    /**
     * Mild coastal height lift/carve in the land/sea blend band.
     *
     * @param worldX worldZ block-equivalent coords
     */
    public void applyHeight(float worldX, float worldZ, NoiseSample sample) {
        if (!ENABLED || sample == null) {
            return;
        }
        float cn = sample.continentNoise;
        float mask = bandMask(cn, HEIGHT_CN_MIN, HEIGHT_CN_MAX);
        if (mask <= 0.0F) {
            return;
        }
        float strength = regionStrength(worldX, worldZ);
        if (strength <= 0.02F) {
            return;
        }

        float cliff = cliffPhase(worldX, worldZ);
        float rocky = smoothstep(0.42F, 0.82F, cliff);
        float carve = 1.0F - rocky;

        float h = sample.heightNoise;
        h += CLIFF_AMPLITUDE * rocky * strength * mask;
        h -= NOTCH_CARVE * carve * strength * mask;
        sample.heightNoise = NoiseUtil.clamp(h, 0.0F, 1.0F);
    }

    /**
     * Beach-band biome pick: rocky LIA segment → stony shore instead of sand.
     * Uses the same cliff phase / region strength as height modulation.
     */
    public boolean isRockyShore(float worldX, float worldZ, float continentNoise) {
        if (!ENABLED) {
            return false;
        }
        if (continentNoise <= 0.5F || continentNoise > 0.505F) {
            return false;
        }
        if (regionStrength(worldX, worldZ) < ROCKY_STRENGTH_MIN) {
            return false;
        }
        return cliffPhase(worldX, worldZ) >= ROCKY_CLIFF_THRESHOLD;
    }

    public float regionStrength(float worldX, float worldZ) {
        float n = IslandScatter.valueNoise2(this.seed ^ 0x5E610, worldX / REGION_CELL, worldZ / REGION_CELL);
        // Bias so some stretches stay near-zero (African straight), others fully eroded.
        return smoothstep(0.28F, 0.78F, n);
    }

    public float cliffPhase(float worldX, float worldZ) {
        return IslandScatter.valueNoise2(this.seed ^ 0xC11F0, worldX / CLIFF_CELL, worldZ / CLIFF_CELL);
    }

    private static float bandMask(float cn, float min, float max) {
        if (cn <= min || cn >= max) {
            return 0.0F;
        }
        float mid = (min + max) * 0.5F;
        float half = (max - min) * 0.5F;
        if (half <= 1.0E-5F) {
            return 0.0F;
        }
        float t = 1.0F - Math.abs(cn - mid) / half;
        return NoiseUtil.clamp(t * t * (3.0F - 2.0F * t), 0.0F, 1.0F);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = NoiseUtil.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
