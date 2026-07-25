package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Sparse mega-ridge spines (not a continent-wide ridged overlay).
 * A few corridors activate along a narrow coastal fringe <em>or</em> as inland
 * continental spines — coasts are no longer a continuous mountain wall.
 */
public final class MountainBeltField {
    private MountainBeltField() {
    }

    public static float strength(float worldX, float worldZ, long seed, int continentScale) {
        return strength(worldX, worldZ, seed, continentScale, 0.65F);
    }

    /**
     * @param continentNoise 0..1 land mask (beach ~0.5, inland ~1). Used for coastal vs inland placement.
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
        float mega = smoothstep(0.76F, 0.92F, selector);
        if (mega <= 0.001F) {
            return 0.0F;
        }

        float place = placementPreference(continentNoise, s, wx, wz, selFreq);
        mega *= place;
        if (mega <= 0.001F) {
            return 0.0F;
        }

        // Detail along the selected spine — wider than a knife ridge so height lifts don't wall.
        float detailWl = scale * 0.82F;
        float detailFreq = 1.0F / detailWl;
        float primary = softRidge(s ^ 0xA1, wx * detailFreq, wz * detailFreq);
        float along = valueNoise(s ^ 0xA3, wx * detailFreq * 0.35F, wz * detailFreq * 0.35F);

        float foothills = smoothstep(0.35F, 0.62F, primary);
        float crest = smoothstep(0.55F, 0.88F, primary);
        float belt = foothills * 0.55F + crest * 0.55F;
        float profile = 0.78F + 0.22F * along;
        return NoiseUtil.clamp(mega * belt * profile, 0.0F, 1.0F);
    }

    /** Stronger offshore continuation so underwater spines can tip into islands more often. */
    public static float underwaterStrength(float worldX, float worldZ, long seed, int continentScale) {
        // Ocean CN is low — pass a near-shore proxy so spines can continue briefly offshore.
        return strength(worldX, worldZ, seed, continentScale, 0.48F) * 0.55F;
    }

    /**
     * Coastal fringe OR sparse inland continental spines (whichever is stronger).
     * Coastal band is intentionally narrow so entire shorelines aren't mountain walls.
     */
    private static float placementPreference(float cn, int s, float wx, float wz, float selFreq) {
        float coastal = coastalBand(cn);
        // Separate long-wave gate for inland spines — independent of coastal corridors.
        float inlandSel = softRidge(s ^ 0xB0, wx * selFreq * 0.72F, wz * selFreq * 0.72F);
        float inlandGate = smoothstep(0.80F, 0.94F, inlandSel);
        float inland = inlandBand(cn) * inlandGate;
        return NoiseUtil.clamp(Math.max(coastal, inland), 0.0F, 1.0F);
    }

    /** Narrow coastal fringe (CN ~0.45–0.68) — fades before deep near-inland. */
    private static float coastalBand(float cn) {
        float c = NoiseUtil.clamp(cn, 0.0F, 1.0F);
        if (c < 0.38F) {
            return 0.0F;
        }
        float rise = smoothstep(0.42F, 0.52F, c);
        float hold = 1.0F - smoothstep(0.58F, 0.72F, c);
        return NoiseUtil.clamp(rise * hold, 0.0F, 1.0F);
    }

    /** Deep inland / continental core (CN ≳ 0.72). */
    private static float inlandBand(float cn) {
        float c = NoiseUtil.clamp(cn, 0.0F, 1.0F);
        return smoothstep(0.70F, 0.88F, c);
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
