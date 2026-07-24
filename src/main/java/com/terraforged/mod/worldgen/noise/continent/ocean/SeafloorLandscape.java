package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Underwater landform relief for corridor zones — adapted hill/mountain-style noise
 * without rivers, lakes, dolomites, or aero-erosion. Output is 0..1 relief.
 */
public final class SeafloorLandscape {
    /** Relief above this (after corridor mask) emerges as island tips. */
    public static final float EMERGE_THRESHOLD = 0.52F;
    /** Shipwrecked needs a bit more relief before dry land (density control). */
    public static final float SHIP_EMERGE_THRESHOLD = 0.62F;

    public enum Form {
        FLATS,
        HILLS,
        MOUNTAINS
    }

    private SeafloorLandscape() {
    }

    /**
     * @param landscapeScale feature size: 1 = default, higher = larger / smoother banks
     * @param noiseScale detail amount 0.25..3 (lower = less grain)
     * @return relief 0..1 before corridor multiply
     */
    public static float relief(float worldX, float worldZ, int seed, float noiseScale, float landscapeScale) {
        float size = NoiseUtil.clamp(landscapeScale, 0.5F, 4.0F);
        float detailAmt = NoiseUtil.clamp(noiseScale, 0.25F, 3.0F);
        // Larger landscapeScale → lower frequency (bigger features).
        float freq = 1.0F / size;
        float hills = ridged(seed ^ 0x51F100, worldX * (0.0018F * freq), worldZ * (0.0018F * freq));
        float macro = valueNoise2(seed ^ 0x51F102, worldX * (0.00035F * freq), worldZ * (0.00035F * freq));
        // Detail is intentionally weak so the seafloor reads as broad banks, not hash.
        float detailFreq = 0.0045F * freq * (0.55F + 0.45F * detailAmt);
        float detail = valueNoise2(seed ^ 0x51F101, worldX * detailFreq, worldZ * detailFreq);
        float detailW = NoiseUtil.clamp(0.05F + 0.04F * detailAmt, 0.04F, 0.14F);
        float peak = NoiseUtil.clamp(hills * 0.62F + macro * 0.28F + detail * detailW, 0.0F, 1.0F);
        return NoiseUtil.clamp((peak - 0.22F) / 0.78F, 0.0F, 1.0F);
    }

    /** @deprecated prefer {@link #relief(float, float, int, float, float)} */
    @Deprecated
    public static float relief(float worldX, float worldZ, int seed, float noiseScale) {
        return relief(worldX, worldZ, seed, noiseScale, 1.5F);
    }

    /** @deprecated prefer {@link #relief(float, float, int, float, float)} */
    @Deprecated
    public static float relief(float worldX, float worldZ, int seed) {
        return relief(worldX, worldZ, seed, 0.45F, 1.5F);
    }

    public static Form formFor(float maskedRelief, int seed, int cellX, int cellZ) {
        if (maskedRelief > 0.78F) {
            return Form.MOUNTAINS;
        }
        float roll = hash01(seed ^ 0xF0F0, cellX, cellZ);
        if (maskedRelief > 0.64F || roll > 0.50F) {
            return Form.HILLS;
        }
        return Form.FLATS;
    }

    private static float ridged(int seed, float x, float z) {
        float n = valueNoise2(seed, x, z);
        float r = 1.0F - Math.abs(n * 2.0F - 1.0F);
        return r * r;
    }

    private static float valueNoise2(int seed, float x, float z) {
        int x0 = NoiseUtil.floor(x);
        int z0 = NoiseUtil.floor(z);
        float fx = x - x0;
        float fz = z - z0;
        float u = fx * fx * (3.0F - 2.0F * fx);
        float v = fz * fz * (3.0F - 2.0F * fz);
        float a = hash01(seed, x0, z0);
        float b = hash01(seed, x0 + 1, z0);
        float c = hash01(seed, x0, z0 + 1);
        float d = hash01(seed, x0 + 1, z0 + 1);
        return NoiseUtil.lerp(NoiseUtil.lerp(a, b, u), NoiseUtil.lerp(c, d, u), v);
    }

    private static float hash01(int seed, int x, int z) {
        int h = MathUtil.hash(seed, x, z);
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
