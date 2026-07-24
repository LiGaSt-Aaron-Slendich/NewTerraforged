package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Continent-scale mountain spines with wide foothills (not knife-edge walls).
 * Peak sits on a soft apron so height rises over hundreds of blocks.
 */
public final class MountainBeltField {
    private MountainBeltField() {
    }

    /**
     * @param worldX worldZ block coordinates
     * @param seed world/continent seed
     * @param continentScale {@code WorldSettings.Continent.continentScale}
     * @return belt strength 0..1 (core of a spine near 1, foothills ~0.2–0.6)
     */
    public static float strength(float worldX, float worldZ, long seed, int continentScale) {
        int scale = Math.max(400, continentScale);
        int s = (int) seed ^ 0xB3175EED;
        // Wider wavelength → broader ridges (~0.7 continent across for the envelope).
        float wl = scale * 0.70F;
        float freq = 1.0F / wl;
        float warpAmt = scale * 0.14F;
        float wx = worldX + (valueNoise(s ^ 0x11, worldX * freq * 0.32F, worldZ * freq * 0.32F) - 0.5F) * warpAmt;
        float wz = worldZ + (valueNoise(s ^ 0x22, worldX * freq * 0.32F, worldZ * freq * 0.32F) - 0.5F) * warpAmt;

        // Soft ridge (less squaring) = wider shoulders / foothills.
        float primary = softRidge(s ^ 0xA1, wx * freq, wz * freq);
        float secondary = softRidge(s ^ 0xA2, wx * freq * 1.35F + 17.3F, wz * freq * 1.35F - 9.1F);
        // Mild along-belt variation — avoid sudden peak/pass cliffs.
        float along = valueNoise(s ^ 0xA3, wx * freq * 0.40F, wz * freq * 0.40F);

        // Broad apron starts early; crest is only the top of the same field.
        float foothills = smoothstep(0.22F, 0.52F, primary);
        float crest = smoothstep(0.48F, 0.82F, primary);
        float side = smoothstep(0.55F, 0.85F, secondary) * 0.28F;
        float belt = NoiseUtil.clamp(foothills * 0.62F + crest * 0.48F + side, 0.0F, 1.0F);
        // Soften lengthwise height jitter (was 0.55–1.0 → walls between peaks).
        float profile = 0.82F + 0.18F * along;
        return NoiseUtil.clamp(belt * profile, 0.0F, 1.0F);
    }

    /** Weaker offshore continuation of the same spine (for seafloor relief). */
    public static float underwaterStrength(float worldX, float worldZ, long seed, int continentScale) {
        return strength(worldX, worldZ, seed, continentScale) * 0.40F;
    }

    /** Ridged noise with gentle shoulders (sqrt of classic ridge). */
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
