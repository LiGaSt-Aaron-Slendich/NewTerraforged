package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Sparse highland relief: <b>one</b> continent-scale spine (continues offshore)
 * plus a <b>few</b> isolated peak massifs.
 *
 * <p>Cross-section is a wide-base peaked ridge (not a mesa / wall): foothill skirts
 * flare out, crest is a tip. Width grows with crest strength so taller segments
 * stay massifs rather than vertical stretch.
 */
public final class MountainBeltField {
    private MountainBeltField() {
    }

    public static float strength(float worldX, float worldZ, long seed, int continentScale) {
        return strength(worldX, worldZ, seed, continentScale, 0.75F);
    }

    /**
     * Land highland strength (0..1). Suppressed on the beach fringe so coasts stay
     * plains / hills / beach unless they sit on the actual spine crest.
     */
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

    /** Same highland field for bathymetry (caller applies ocean CN fade). */
    public static float underwaterStrength(float worldX, float worldZ, long seed, int continentScale) {
        return highlandField(worldX, worldZ, seed, continentScale) * 0.70F;
    }

    /** Shared field: peaked spine ∪ sparse peak nodes. */
    public static float highlandField(float worldX, float worldZ, long seed, int continentScale) {
        int scale = Math.max(400, continentScale);
        int s = (int) seed ^ 0xB3175EED;

        float spine = peakedSpine(worldX, worldZ, s, scale);
        float peaks = sparsePeaks(worldX, worldZ, s, scale);
        return NoiseUtil.clamp(Math.max(spine, peaks), 0.0F, 1.0F);
    }

    /**
     * Continent-scale ridge with soft skirts and a pointed crest.
     * Field value ≈ slope profile (low foothills → high tip), not a flat mesa mask.
     */
    private static float peakedSpine(float worldX, float worldZ, int s, int scale) {
        float spineWl = scale * 1.15F;
        float spineFreq = 1.0F / spineWl;
        float warpAmt = scale * 0.38F;
        float wx = worldX + (valueNoise(s ^ 0x11, worldX * spineFreq * 0.18F, worldZ * spineFreq * 0.18F) - 0.5F) * warpAmt;
        float wz = worldZ + (valueNoise(s ^ 0x22, worldX * spineFreq * 0.18F, worldZ * spineFreq * 0.18F) - 0.5F) * warpAmt;

        float ridge = softRidge(s ^ 0xA0, wx * spineFreq, wz * spineFreq);
        // Soft corridor gate — skirts start early so the ridge has a wide base.
        float corridor = smoothstep(0.38F, 0.72F, ridge);
        if (corridor <= 0.001F) {
            return 0.0F;
        }

        // Wider cross-section when the corridor is strong (taller → wider).
        float widthMul = NoiseUtil.lerp(0.95F, 1.70F, corridor);
        float detailWl = scale * 0.95F * widthMul;
        float detailFreq = 1.0F / detailWl;
        float primary = softRidge(s ^ 0xA1, wx * detailFreq, wz * detailFreq);
        float along = valueNoise(s ^ 0xA3, wx * detailFreq * 0.28F, wz * detailFreq * 0.28F);

        // Peaked profile from softRidge: skirts / body / tip (not foothills*0.4+crest*0.7 mesa).
        float skirts = smoothstep(0.18F, 0.48F, primary);
        float body = smoothstep(0.35F, 0.72F, primary);
        float tipT = NoiseUtil.clamp((primary - 0.52F) / 0.48F, 0.0F, 1.0F);
        float tip = tipT * tipT * tipT; // sharp crest, soft flanks
        // Secondary shoulder undulation (user sketch: irregular slopes).
        float shoulder = softRidge(s ^ 0xA4, wx * detailFreq * 1.7F, wz * detailFreq * 1.7F);
        float undulate = skirts * 0.12F * shoulder;

        float profile = skirts * 0.22F + body * 0.38F + tip * 0.72F + undulate;
        float crestVary = 0.78F + 0.22F * along; // tip height varies along-spine — not a flat platform
        return NoiseUtil.clamp(corridor * profile * crestVary, 0.0F, 1.0F);
    }

    /**
     * A handful of large isolated peaks per continent — wide base, peaked tip.
     */
    private static float sparsePeaks(float worldX, float worldZ, int s, int scale) {
        float cellWl = scale * 0.52F;
        float freq = 1.0F / cellWl;
        float gx = worldX * freq;
        float gz = worldZ * freq;
        int ix = NoiseUtil.floor(gx);
        int iz = NoiseUtil.floor(gz);
        float best = 1.0F;
        float bestSeed = 0.0F;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = ix + dx;
                int cz = iz + dz;
                float jx = hash01(s ^ 0xD1, cx, cz);
                float jz = hash01(s ^ 0xD2, cx, cz);
                float gate = hash01(s ^ 0xD3, cx, cz);
                if (gate < 0.72F) {
                    continue;
                }
                float px = cx + jx;
                float pz = cz + jz;
                float d = NoiseUtil.sqrt((gx - px) * (gx - px) + (gz - pz) * (gz - pz));
                if (d < best) {
                    best = d;
                    bestSeed = gate;
                }
            }
        }
        // Wide foothill radius; power curve → pointed tip, not a plateau disk.
        float radius = 0.46F;
        float core = 1.0F - NoiseUtil.clamp(best / radius, 0.0F, 1.0F);
        if (core <= 0.0F) {
            return 0.0F;
        }
        float peaked = core * core * core; // tip
        float skirts = core * core * 0.45F; // wide base
        float shape = NoiseUtil.clamp(skirts + peaked, 0.0F, 1.0F);
        float power = 0.70F + 0.30F * bestSeed;
        return NoiseUtil.clamp(shape * power, 0.0F, 1.0F);
    }

    private static float softRidge(int seed, float x, float z) {
        float n = valueNoise(seed, x, z);
        float r = 1.0F - Math.abs(n * 2.0F - 1.0F);
        // Milder than sqrt — softer skirts (sqrt was still wall-like on flanks).
        return (float) Math.pow(Math.max(0.0F, r), 0.65F);
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
