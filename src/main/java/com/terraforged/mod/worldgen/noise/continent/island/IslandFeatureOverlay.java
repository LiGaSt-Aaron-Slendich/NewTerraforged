package com.terraforged.mod.worldgen.noise.continent.island;

import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Overlays coastal / volcanic freckles and Scattered Archipelago clusters onto continent samples.
 * Archipelago water between islands is tagged {@link ModTerrainTypes#LAGUNA} with shallow depth.
 */
public final class IslandFeatureOverlay {
    public static final int ARCHIPELAGO_MIN = 15;
    public static final int ARCHIPELAGO_MAX = 1000;
    /** Max Laguna depth below sea level (blocks). */
    public static final int LAGUNA_MAX_DEPTH = 15;

    private final int seed;
    private final float coastalChance;
    private final float volcanicChance;
    private final boolean archipelago;
    private final float archipelagoChance;

    public IslandFeatureOverlay(ContinentConfig config) {
        this.seed = config.shape.seed0 ^ 0x51ED;
        this.coastalChance = NoiseUtil.clamp(config.shape.coastalIslandsChance, 0.0F, 1.0F);
        this.volcanicChance = NoiseUtil.clamp(config.shape.volcanicIslandsChance, 0.0F, 1.0F);
        this.archipelago = config.shape.scatteredArchipelago;
        this.archipelagoChance = NoiseUtil.clamp(config.shape.scatteredArchipelagoChance, 0.0F, 1.0F);
    }

    /**
     * @param worldX world X
     * @param worldZ world Z
     * @param seaLevel current sea level
     */
    public void apply(float worldX, float worldZ, NoiseSample sample, int seaLevel) {
        if (sample.continentNoise <= 0.0F) {
            // Ocean / laguna candidates.
            if (this.archipelago && this.archipelagoChance > 0.0F) {
                this.tryArchipelago(worldX, worldZ, sample, seaLevel);
            }
            return;
        }

        // Near-shore freckles on / just off the coast band.
        float edge = sample.continentNoise;
        boolean nearCoast = edge > 0.35F && edge < 0.62F;
        if (!nearCoast) {
            return;
        }

        int ix = NoiseUtil.floor(worldX);
        int iz = NoiseUtil.floor(worldZ);
        float roll = hash01(this.seed, ix >> 3, iz >> 3);

        if (this.volcanicChance > 0.0F && roll < this.volcanicChance * 0.35F) {
            float local = hash01(this.seed ^ 31, ix, iz);
            if (local < 0.22F) {
                sample.terrainType = ModTerrainTypes.VOLCANIC_ISLAND;
                sample.heightNoise = Math.max(sample.heightNoise, 0.55F + local * 0.35F);
            }
            return;
        }
        if (this.coastalChance > 0.0F && roll < this.coastalChance) {
            float local = hash01(this.seed ^ 17, ix, iz);
            if (local < 0.18F) {
                sample.terrainType = ModTerrainTypes.COASTAL_ISLAND;
                sample.heightNoise = Math.max(sample.heightNoise, 0.42F + local * 0.2F);
            }
        }
    }

    private void tryArchipelago(float worldX, float worldZ, NoiseSample sample, int seaLevel) {
        // Cluster grid ~2.5k blocks.
        int cell = 2500;
        int cx = NoiseUtil.floor(worldX / cell);
        int cz = NoiseUtil.floor(worldZ / cell);
        float place = hash01(this.seed ^ 99, cx, cz);
        if (place > this.archipelagoChance) {
            return;
        }

        float centerX = (cx + 0.5F) * cell;
        float centerZ = (cz + 0.5F) * cell;
        // Large core island radius 200..1000
        float coreR = ARCHIPELAGO_MIN * 12 + hash01(this.seed, cx, cz) * (ARCHIPELAGO_MAX - ARCHIPELAGO_MIN * 12);
        float dx = worldX - centerX;
        float dz = worldZ - centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);

        if (dist <= coreR) {
            sample.continentNoise = Math.max(sample.continentNoise, 0.75F);
            sample.terrainType = ModTerrainTypes.SCATTERED_ARCHIPELAGO;
            sample.heightNoise = Math.max(sample.heightNoise, 0.45F + (1.0F - dist / coreR) * 0.25F);
            return;
        }

        // Ring of small islands out to ~2.2 * coreR
        float ring = coreR * 2.2F;
        if (dist > ring) {
            return;
        }

        // Angular slots for small islands.
        float angle = (float) (Math.atan2(dz, dx) / (Math.PI * 2.0) + 0.5);
        int slot = NoiseUtil.floor(angle * 12.0F);
        float slotCenter = (slot + 0.5F) / 12.0F;
        float angDist = Math.abs(angle - slotCenter);
        if (angDist > 0.04F) {
            // Between islands → Laguna shallow water.
            sample.continentNoise = Math.max(sample.continentNoise, 0.28F);
            sample.terrainType = ModTerrainTypes.LAGUNA;
            float depthNorm = LAGUNA_MAX_DEPTH / Math.max(1.0F, seaLevel);
            sample.heightNoise = Math.min(sample.heightNoise, Math.max(0.05F, 0.35F - depthNorm * 0.15F));
            return;
        }

        float smallR = ARCHIPELAGO_MIN + hash01(this.seed ^ 7, cx, cz + slot) * 80.0F;
        float radial = Math.abs(dist - (coreR + smallR * 1.6F));
        if (radial <= smallR) {
            sample.continentNoise = Math.max(sample.continentNoise, 0.7F);
            sample.terrainType = ModTerrainTypes.SCATTERED_ARCHIPELAGO;
            sample.heightNoise = Math.max(sample.heightNoise, 0.4F);
        } else {
            sample.continentNoise = Math.max(sample.continentNoise, 0.28F);
            sample.terrainType = ModTerrainTypes.LAGUNA;
            sample.heightNoise = Math.min(sample.heightNoise, 0.32F);
        }
    }

    private static float hash01(int seed, int x, int z) {
        int h = MathUtil.hash(seed, x, z);
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
