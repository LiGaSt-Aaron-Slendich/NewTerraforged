package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Underwater landform relief for corridor zones — adapted hill/mountain-style noise
 * without rivers, lakes, dolomites, or aero-erosion. Output is 0..1 relief.
 */
public final class SeafloorLandscape {
    /** Relief above this (after corridor mask) emerges as island tips. */
    public static final float EMERGE_THRESHOLD = 0.40F;
    /** Shipwrecked needs a bit more relief before dry land (density control). */
    public static final float SHIP_EMERGE_THRESHOLD = 0.52F;

    public enum Form {
        FLATS,
        HILLS,
        MOUNTAINS
    }

    private SeafloorLandscape() {
    }

    /**
     * @param landscapeScale feature size: 0.5 = tight banks, 4 = very broad (wavelength ~0.5–8 km)
     * @param noiseScale detail amount 0.25..3 (smooth → crunchy grain)
     * @return relief 0..1 before corridor multiply
     */
    public static float relief(float worldX, float worldZ, int seed, float noiseScale, float landscapeScale) {
        float size = NoiseUtil.clamp(landscapeScale, 0.5F, 4.0F);
        float detailAmt = NoiseUtil.clamp(noiseScale, 0.25F, 3.0F);
        // Map slider to feature wavelength in blocks so 0.5 vs 4.0 is obvious on preview.
        float tSize = (size - 0.5F) / 3.5F;
        float wavelength = NoiseUtil.lerp(480.0F, 8200.0F, tSize);
        float freq = 1.0F / Math.max(64.0F, wavelength);
        float hills = ridged(seed ^ 0x51F100, worldX * freq, worldZ * freq);
        float macro = valueNoise2(seed ^ 0x51F102, worldX * (freq * 0.22F), worldZ * (freq * 0.22F));
        float tDetail = (detailAmt - 0.25F) / 2.75F;
        float detailFreq = freq * NoiseUtil.lerp(2.5F, 14.0F, tDetail);
        float detail = valueNoise2(seed ^ 0x51F101, worldX * detailFreq, worldZ * detailFreq);
        float detailW = NoiseUtil.lerp(0.02F, 0.42F, tDetail);
        float hillW = NoiseUtil.lerp(0.72F, 0.40F, tDetail);
        float macroW = Math.max(0.08F, 1.0F - hillW - detailW);
        float peak = NoiseUtil.clamp(hills * hillW + macro * macroW + detail * detailW, 0.0F, 1.0F);
        // Larger landscape → slightly easier emergence (broader banks).
        float floor = NoiseUtil.lerp(0.22F, 0.12F, tSize);
        return NoiseUtil.clamp((peak - floor) / Math.max(0.30F, 1.0F - floor), 0.0F, 1.0F);
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
