package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Sparse mega-ridge spines (not a continent-wide ridged overlay).
 * Only a few corridors activate; they prefer the coastal fringe so WeightMap
 * landforms still respond to terrain region scale inland.
 */
public final class MountainBeltField {
    private MountainBeltField() {
    }

    public static float strength(float worldX, float worldZ, long seed, int continentScale) {
        return strength(worldX, worldZ, seed, continentScale, 0.65F);
    }

    /**
     * @param continentNoise 0..1 land mask (beach ~0.5, inland ~1). Used to prefer coasts.
     */
    public static float strength(float worldX, float worldZ, long seed, int continentScale, float continentNoise) {
        int scale = Math.max(400, continentScale);
        int s = (int) seed ^ 0xB3175EED;

        // Very long wavelength selector → only a handful of mega spines per continent.
        float selWl = scale * 1.55F;
        float selFreq = 1.0F / selWl;
        float warpAmt = scale * 0.22F;
        float wx = worldX + (valueNoise(s ^ 0x11, worldX * selFreq * 0.25F, worldZ * selFreq * 0.25F) - 0.5F) * warpAmt;
        float wz = worldZ + (valueNoise(s ^ 0x22, worldX * selFreq * 0.25F, worldZ * selFreq * 0.25F) - 0.5F) * warpAmt;

        float selector = softRidge(s ^ 0xA0, wx * selFreq, wz * selFreq);
        // Strict peak gate — vast majority of land stays under WeightMap control.
        float mega = smoothstep(0.74F, 0.90F, selector);
        if (mega <= 0.001F) {
            return 0.0F;
        }

        // Prefer coasts / near-coast shelves; fade deep inland (WeightMap mountains live there).
        float coastal = coastalPreference(continentNoise);
        mega *= coastal;
        if (mega <= 0.001F) {
            return 0.0F;
        }

        // Detail along the selected spine (narrower than the old everywhere-ridge field).
        float detailWl = scale * 0.55F;
        float detailFreq = 1.0F / detailWl;
        float primary = softRidge(s ^ 0xA1, wx * detailFreq, wz * detailFreq);
        float along = valueNoise(s ^ 0xA3, wx * detailFreq * 0.35F, wz * detailFreq * 0.35F);

        float foothills = smoothstep(0.35F, 0.62F, primary);
        float crest = smoothstep(0.55F, 0.88F, primary);
        float belt = foothills * 0.55F + crest * 0.55F;
        float profile = 0.78F + 0.22F * along;
        return NoiseUtil.clamp(mega * belt * profile, 0.0F, 1.0F);
    }

    /** Weaker offshore continuation of the same sparse spines. */
    public static float underwaterStrength(float worldX, float worldZ, long seed, int continentScale) {
        // Ocean CN is low — pass a near-shore proxy so spines can continue briefly offshore.
        return strength(worldX, worldZ, seed, continentScale, 0.42F) * 0.35F;
    }

    /**
     * Peaks in the coastal band (CN ~0.5–0.72), soft inland fade.
     * Deep ocean / deep inland → near zero mega-belt.
     */
    private static float coastalPreference(float cn) {
        float c = NoiseUtil.clamp(cn, 0.0F, 1.0F);
        if (c < 0.35F) {
            return 0.0F;
        }
        // Rise from shelf into coast, hold through near-inland, fade toward core.
        float rise = smoothstep(0.38F, 0.52F, c);
        float hold = 1.0F - smoothstep(0.78F, 0.96F, c);
        return NoiseUtil.clamp(rise * hold, 0.0F, 1.0F);
    }

    private static float softRidge(int seed, float x, float z) {
        float n = valueNoise(seed, x, z);
        float r = 1.0F - Math.abs(n * 2.0F - 1.0F);
        return (float) Math.sqrt(Math.max(0.0F, r));
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
