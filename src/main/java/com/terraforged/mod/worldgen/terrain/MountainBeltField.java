package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Continent-scale mountain spines: continuous ridged belts that can run across a
 * landmass (and continue offshore as weaker seafloor relief). Independent of the
 * Voronoi terrain-region patches.
 */
public final class MountainBeltField {
    private MountainBeltField() {
    }

    /**
     * @param worldX worldZ block coordinates
     * @param seed world/continent seed
     * @param continentScale {@code WorldSettings.Continent.continentScale}
     * @return belt strength 0..1 (core of a spine near 1)
     */
    public static float strength(float worldX, float worldZ, long seed, int continentScale) {
        int scale = Math.max(400, continentScale);
        int s = (int) seed ^ 0xB3175EED;
        // Primary wavelength ≈ half a continent — one clear spine across a landmass.
        float wl = scale * 0.55F;
        float freq = 1.0F / wl;
        // Soft warp so belts curve instead of ruler-straight.
        float warpAmt = scale * 0.12F;
        float wx = worldX + (valueNoise(s ^ 0x11, worldX * freq * 0.35F, worldZ * freq * 0.35F) - 0.5F) * warpAmt;
        float wz = worldZ + (valueNoise(s ^ 0x22, worldX * freq * 0.35F, worldZ * freq * 0.35F) - 0.5F) * warpAmt;

        float primary = ridged(s ^ 0xA1, wx * freq, wz * freq);
        // Parallel weaker spine for a mountain-chain feel.
        float secondary = ridged(s ^ 0xA2, wx * freq * 1.65F + 17.3F, wz * freq * 1.65F - 9.1F);
        // Along-belt modulation so the ridge rises and falls (peaks / passes).
        float along = valueNoise(s ^ 0xA3, wx * freq * 0.55F, wz * freq * 0.55F);

        float core = smoothstep(0.58F, 0.86F, primary);
        float side = smoothstep(0.70F, 0.92F, secondary) * 0.42F;
        float belt = NoiseUtil.clamp(core + side, 0.0F, 1.0F);
        // Keep a continuous spine but vary height along its length.
        float profile = 0.55F + 0.45F * along;
        return NoiseUtil.clamp(belt * profile, 0.0F, 1.0F);
    }

    /** Weaker offshore continuation of the same spine (for seafloor relief). */
    public static float underwaterStrength(float worldX, float worldZ, long seed, int continentScale) {
        return strength(worldX, worldZ, seed, continentScale) * 0.48F;
    }

    private static float ridged(int seed, float x, float z) {
        float n = valueNoise(seed, x, z);
        float r = 1.0F - Math.abs(n * 2.0F - 1.0F);
        return r * r;
    }

    private static float valueNoise(int seed, float x, float z) {
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

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = NoiseUtil.clamp((x - edge0) / Math.max(1.0E-4F, edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
