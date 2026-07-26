package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Sparse highland relief: <b>one</b> continent-scale spine (continues offshore)
 * plus a <b>few</b> elongated peak massifs (ridge noise — no circular cells).
 *
 * <p>Field value is a peaked slope profile that fades to zero at the skirts so
 * height boosts can follow base terrain instead of holding a constant platform.
 */
public final class MountainBeltField {
    private MountainBeltField() {
    }

    public static float strength(float worldX, float worldZ, long seed, int continentScale) {
        return strength(worldX, worldZ, seed, continentScale, 0.75F);
    }

    public static float strength(float worldX, float worldZ, long seed, int continentScale, float continentNoise) {
        float highland = highlandField(worldX, worldZ, seed, continentScale);
        if (highland <= 0.001F) {
            return 0.0F;
        }
        float cn = NoiseUtil.clamp(continentNoise, 0.0F, 1.0F);
        float landMask = smoothstep(0.58F, 0.72F, cn);
        float shorePass = smoothstep(0.48F, 0.56F, cn) * smoothstep(0.65F, 0.88F, highland);
        float mask = Math.max(landMask, shorePass * 0.90F);
        return NoiseUtil.clamp(highland * mask, 0.0F, 1.0F);
    }

    public static float underwaterStrength(float worldX, float worldZ, long seed, int continentScale) {
        return highlandField(worldX, worldZ, seed, continentScale) * 0.70F;
    }

    public static float highlandField(float worldX, float worldZ, long seed, int continentScale) {
        int scale = Math.max(400, continentScale);
        int s = (int) seed ^ 0xB3175EED;

        float spine = peakedSpine(worldX, worldZ, s, scale);
        float peaks = elongatedPeaks(worldX, worldZ, s, scale);
        return NoiseUtil.clamp(Math.max(spine, peaks), 0.0F, 1.0F);
    }

    /**
     * Continent-scale ridge: wide skirts, pointed crest, fades along-spine
     * so the ridge can sink with surrounding terrain instead of a pedestal.
     */
    private static float peakedSpine(float worldX, float worldZ, int s, int scale) {
        float spineWl = scale * 1.25F;
        float spineFreq = 1.0F / spineWl;
        float warpAmt = scale * 0.45F;
        float wx = worldX + (valueNoise(s ^ 0x11, worldX * spineFreq * 0.16F, worldZ * spineFreq * 0.16F) - 0.5F) * warpAmt;
        float wz = worldZ + (valueNoise(s ^ 0x22, worldX * spineFreq * 0.16F, worldZ * spineFreq * 0.16F) - 0.5F) * warpAmt;

        float ridge = softRidge(s ^ 0xA0, wx * spineFreq, wz * spineFreq);
        // Softer corridor — wider skirts before crest.
        float corridor = smoothstep(0.26F, 0.62F, ridge);
        if (corridor <= 0.001F) {
            return 0.0F;
        }

        float widthMul = NoiseUtil.lerp(1.20F, 2.15F, corridor);
        float detailWl = scale * 1.35F * widthMul;
        float detailFreq = 1.0F / detailWl;
        float primary = softRidge(s ^ 0xA1, wx * detailFreq, wz * detailFreq);
        // Along-spine amplitude can fall near zero — ridge ends / sinks (no sustained platform).
        float along = valueNoise(s ^ 0xA3, wx * detailFreq * 0.22F, wz * detailFreq * 0.22F);
        float alongAmp = smoothstep(0.15F, 0.70F, along);

        float skirts = smoothstep(0.08F, 0.45F, primary);
        float body = smoothstep(0.22F, 0.65F, primary);
        float tipT = NoiseUtil.clamp((primary - 0.42F) / 0.58F, 0.0F, 1.0F);
        float tip = tipT * tipT * tipT; // softer tip than tip^4 — wider peak shoulders
        float shoulder = softRidge(s ^ 0xA4, wx * detailFreq * 1.35F, wz * detailFreq * 1.35F);
        float undulate = skirts * 0.14F * shoulder;

        float profile = skirts * 0.28F + body * 0.38F + tip * 0.58F + undulate;
        return NoiseUtil.clamp(corridor * profile * alongAmp, 0.0F, 1.0F);
    }

    /**
     * Rare elongated massifs (ridge noise) — intentionally not cellular circles.
     */
    private static float elongatedPeaks(float worldX, float worldZ, int s, int scale) {
        float wl = scale * 0.85F;
        float freq = 1.0F / wl;
        float warpAmt = scale * 0.30F;
        float wx = worldX + (valueNoise(s ^ 0xE1, worldX * freq * 0.15F, worldZ * freq * 0.15F) - 0.5F) * warpAmt;
        float wz = worldZ + (valueNoise(s ^ 0xE2, worldX * freq * 0.15F, worldZ * freq * 0.15F) - 0.5F) * warpAmt;

        float ridge = softRidge(s ^ 0xE0, wx * freq, wz * freq);
        // Rare tips only.
        float gate = smoothstep(0.66F, 0.88F, ridge);
        if (gate <= 0.001F) {
            return 0.0F;
        }
        float tipT = NoiseUtil.clamp((ridge - 0.58F) / 0.42F, 0.0F, 1.0F);
        float tip = tipT * tipT * tipT;
        float cross = softRidge(s ^ 0xE3, wx * freq * 2.1F, wz * freq * 2.1F);
        float length = smoothstep(0.30F, 0.70F, cross);
        return NoiseUtil.clamp(gate * tip * (0.55F + 0.45F * length), 0.0F, 1.0F);
    }

    private static float softRidge(int seed, float x, float z) {
        float n = valueNoise(seed, x, z);
        float r = 1.0F - Math.abs(n * 2.0F - 1.0F);
        // Higher power → softer skirts (less wall-like flanks).
        return (float) Math.pow(Math.max(0.0F, r), 0.85F);
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
