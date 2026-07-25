package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Sparse highland relief: <b>one</b> narrow mega-ridge spine per continent-scale
 * cell (continues offshore) plus a <b>few</b> isolated peak massifs — not a
 * continuous mountain wall along the coast.
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
        // Beach ~0.50 — keep foothills inland. Strong crest may meet the shore as a crossing.
        float landMask = smoothstep(0.58F, 0.72F, cn);
        float shorePass = smoothstep(0.48F, 0.56F, cn) * smoothstep(0.65F, 0.88F, highland);
        float mask = Math.max(landMask, shorePass * 0.90F);
        return NoiseUtil.clamp(highland * mask, 0.0F, 1.0F);
    }

    /** Same highland field for bathymetry (caller applies ocean CN fade). */
    public static float underwaterStrength(float worldX, float worldZ, long seed, int continentScale) {
        return highlandField(worldX, worldZ, seed, continentScale) * 0.70F;
    }

    /**
     * Shared field: narrow continent-scale spine ∪ sparse peak nodes.
     */
    public static float highlandField(float worldX, float worldZ, long seed, int continentScale) {
        int scale = Math.max(400, continentScale);
        int s = (int) seed ^ 0xB3175EED;

        float spine = narrowSpine(worldX, worldZ, s, scale);
        float peaks = sparsePeaks(worldX, worldZ, s, scale);
        return NoiseUtil.clamp(Math.max(spine, peaks), 0.0F, 1.0F);
    }

    /** One thin crest line per ~continentScale — continuous, not a wide belt. */
    private static float narrowSpine(float worldX, float worldZ, int s, int scale) {
        float spineWl = scale * 1.15F;
        float spineFreq = 1.0F / spineWl;
        float warpAmt = scale * 0.32F;
        float wx = worldX + (valueNoise(s ^ 0x11, worldX * spineFreq * 0.20F, worldZ * spineFreq * 0.20F) - 0.5F) * warpAmt;
        float wz = worldZ + (valueNoise(s ^ 0x22, worldX * spineFreq * 0.20F, worldZ * spineFreq * 0.20F) - 0.5F) * warpAmt;

        float ridge = softRidge(s ^ 0xA0, wx * spineFreq, wz * spineFreq);
        // Strict crest — wide gates were painting whole coasts as mountains.
        float mega = smoothstep(0.70F, 0.88F, ridge);
        if (mega <= 0.001F) {
            return 0.0F;
        }

        // Narrow cross-section (foothills hug the crest only).
        float detailWl = scale * 0.55F;
        float detailFreq = 1.0F / detailWl;
        float primary = softRidge(s ^ 0xA1, wx * detailFreq, wz * detailFreq);
        float along = valueNoise(s ^ 0xA3, wx * detailFreq * 0.30F, wz * detailFreq * 0.30F);

        float foothills = smoothstep(0.48F, 0.72F, primary);
        float crest = smoothstep(0.62F, 0.90F, primary);
        float belt = foothills * 0.40F + crest * 0.70F;
        float profile = 0.82F + 0.18F * along;
        return NoiseUtil.clamp(mega * belt * profile, 0.0F, 1.0F);
    }

    /**
     * A handful of large isolated peaks per continent (cell noise at ~0.45× continentScale).
     */
    private static float sparsePeaks(float worldX, float worldZ, int s, int scale) {
        float cellWl = scale * 0.48F;
        float freq = 1.0F / cellWl;
        // Worley-ish: distance to nearest cell center.
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
                // Only ~1 in 4 cells hosts a peak.
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
        // Peak radius ~0.22 cells → large but local massifs.
        float core = 1.0F - NoiseUtil.clamp(best / 0.28F, 0.0F, 1.0F);
        if (core <= 0.0F) {
            return 0.0F;
        }
        core = core * core * (3.0F - 2.0F * core);
        float power = 0.75F + 0.25F * bestSeed;
        return NoiseUtil.clamp(core * power, 0.0F, 1.0F);
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
