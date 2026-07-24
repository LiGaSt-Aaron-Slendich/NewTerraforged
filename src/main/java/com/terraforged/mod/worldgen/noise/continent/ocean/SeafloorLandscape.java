package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Underwater landform relief for corridor zones — adapted hill/mountain-style noise
 * without rivers, lakes, dolomites, or aero-erosion. Output is 0..1 relief.
 */
public final class SeafloorLandscape {
    /** Relief above this (after corridor mask) emerges as island tips. */
    public static final float EMERGE_THRESHOLD = 0.64F;
    /** Shipwrecked needs a bit more relief before dry land (density control). */
    public static final float SHIP_EMERGE_THRESHOLD = 0.72F;

    public enum Form {
        FLATS,
        HILLS,
        MOUNTAINS
    }

    private SeafloorLandscape() {
    }

    /**
     * @return relief 0..1 before corridor multiply
     */
    public static float relief(float worldX, float worldZ, int seed) {
        float hills = ridged(seed ^ 0x51F100, worldX * 0.0028F, worldZ * 0.0028F);
        float detail = valueNoise2(seed ^ 0x51F101, worldX * 0.011F, worldZ * 0.011F);
        float macro = valueNoise2(seed ^ 0x51F102, worldX * 0.00055F, worldZ * 0.00055F);
        float peak = NoiseUtil.clamp(hills * 0.72F + detail * 0.18F + macro * 0.22F, 0.0F, 1.0F);
        // Soften flats: push mid values down so only ridges emerge.
        return NoiseUtil.clamp((peak - 0.28F) / 0.72F, 0.0F, 1.0F);
    }

    public static Form formFor(float maskedRelief, int seed, int cellX, int cellZ) {
        if (maskedRelief > 0.82F) {
            return Form.MOUNTAINS;
        }
        float roll = hash01(seed ^ 0xF0F0, cellX, cellZ);
        if (maskedRelief > 0.70F || roll > 0.55F) {
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
