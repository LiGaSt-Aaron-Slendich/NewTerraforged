package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * One mega-ridge spine per continent-scale cell: a single curved crest that
 * crosses land and continues offshore as an underwater ridge.
 *
 * <p>The spine field itself is continentNoise-agnostic; land vs ocean only
 * chooses whether height or bathymetry is lifted from the same corridor.
 */
public final class MountainBeltField {
    private MountainBeltField() {
    }

    public static float strength(float worldX, float worldZ, long seed, int continentScale) {
        return strength(worldX, worldZ, seed, continentScale, 0.72F);
    }

    /**
     * Land mega-ridge strength (0..1). Active from coast through inland core.
     *
     * @param continentNoise 0..1 land mask (beach ~0.5, inland ~1)
     */
    public static float strength(float worldX, float worldZ, long seed, int continentScale, float continentNoise) {
        float spine = spineStrength(worldX, worldZ, seed, continentScale);
        if (spine <= 0.001F) {
            return 0.0F;
        }
        // Land side of the same spine (shelf → inland). Soft fade — not a thin coastal ring.
        float land = NoiseUtil.clamp(continentNoise, 0.0F, 1.0F);
        float landMask = smoothstep(0.42F, 0.58F, land);
        return NoiseUtil.clamp(spine * landMask, 0.0F, 1.0F);
    }

    /**
     * Same spine as {@link #strength}, gated to ocean / shelf so the ridge continues underwater.
     */
    public static float underwaterStrength(float worldX, float worldZ, long seed, int continentScale) {
        float spine = spineStrength(worldX, worldZ, seed, continentScale);
        if (spine <= 0.001F) {
            return 0.0F;
        }
        // Callers pass world coords; CN is sampled separately in NoiseGenerator for fade.
        // Here we return full spine — ocean fade is applied by the caller with real CN.
        return spine * 0.72F;
    }

    /**
     * Shared tectonic spine (~one corridor per continentScale).
     * Wavelength ≈ continent size so a typical landmass crosses a single crest.
     */
    public static float spineStrength(float worldX, float worldZ, long seed, int continentScale) {
        int scale = Math.max(400, continentScale);
        int s = (int) seed ^ 0xB3175EED;

        // One ridge family at continent pitch — not a dense grid of mega spines.
        float spineWl = scale * 1.05F;
        float spineFreq = 1.0F / spineWl;
        float warpAmt = scale * 0.38F;
        float wx = worldX + (valueNoise(s ^ 0x11, worldX * spineFreq * 0.22F, worldZ * spineFreq * 0.22F) - 0.5F) * warpAmt;
        float wz = worldZ + (valueNoise(s ^ 0x22, worldX * spineFreq * 0.22F, worldZ * spineFreq * 0.22F) - 0.5F) * warpAmt;

        float ridge = softRidge(s ^ 0xA0, wx * spineFreq, wz * spineFreq);
        // Visible crest gate (was 0.76–0.92 → ridges effectively vanished).
        float mega = smoothstep(0.52F, 0.78F, ridge);
        if (mega <= 0.001F) {
            return 0.0F;
        }

        // Width / foothills along the spine (broad massif, not a knife wall).
        float detailWl = scale * 0.78F;
        float detailFreq = 1.0F / detailWl;
        float primary = softRidge(s ^ 0xA1, wx * detailFreq, wz * detailFreq);
        float along = valueNoise(s ^ 0xA3, wx * detailFreq * 0.32F, wz * detailFreq * 0.32F);

        float foothills = smoothstep(0.28F, 0.58F, primary);
        float crest = smoothstep(0.48F, 0.85F, primary);
        float belt = foothills * 0.50F + crest * 0.60F;
        float profile = 0.80F + 0.20F * along;
        return NoiseUtil.clamp(mega * belt * profile, 0.0F, 1.0F);
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
