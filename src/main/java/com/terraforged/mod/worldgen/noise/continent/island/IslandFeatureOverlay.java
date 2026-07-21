package com.terraforged.mod.worldgen.noise.continent.island;

import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Overlays coastal / volcanic freckles and Scattered Archipelago clusters onto continent samples.
 * Volcanic islands paint {@link TerrainType#VOLCANO} cone + {@link TerrainType#VOLCANO_PIPE} crater.
 * Coordinates are world blocks.
 */
public final class IslandFeatureOverlay {
    public static final int ARCHIPELAGO_MIN = 15;
    public static final int ARCHIPELAGO_MAX = 1000;
    public static final int LAGUNA_MAX_DEPTH = 15;
    private static final int ARCHIPELAGO_CELL = 3200;
    /** Sparse mid-ocean volcanic spacing — chance scales density, not a solid fill. */
    private static final int VOLCANIC_OCEAN_CELL = 9000;

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

    public void apply(float worldX, float worldZ, NoiseSample sample, int seaLevel) {
        if (sample.continentNoise <= 0.0F) {
            if (this.archipelago && this.archipelagoChance > 0.0F) {
                this.tryArchipelago(worldX, worldZ, sample, seaLevel);
            }
            if (sample.continentNoise <= 0.0F && this.volcanicChance > 0.0F) {
                this.tryOceanVolcano(worldX, worldZ, sample);
            }
            return;
        }

        float edge = sample.continentNoise;
        boolean nearCoast = edge > 0.28F && edge < 0.70F;
        if (!nearCoast) {
            return;
        }

        int ix = NoiseUtil.floor(worldX);
        int iz = NoiseUtil.floor(worldZ);
        float roll = hash01(this.seed, ix >> 2, iz >> 2);

        // Disjoint bands so volcanic chance never blocks coastal islands.
        float volcanicBand = this.volcanicChance * 0.14F;
        if (this.volcanicChance > 0.0F && roll < volcanicBand) {
            float local = hash01(this.seed ^ 31, ix >> 1, iz >> 1);
            if (local < 0.20F) {
                this.paintVolcanoCone(worldX, worldZ, sample, ix, iz, 55.0F + local * 90.0F);
            }
            return;
        }
        if (this.coastalChance > 0.0F && roll < volcanicBand + this.coastalChance * 0.55F) {
            float local = hash01(this.seed ^ 17, ix >> 1, iz >> 1);
            if (local < 0.38F) {
                sample.terrainType = ModTerrainTypes.COASTAL_ISLAND;
                sample.heightNoise = Math.max(sample.heightNoise, 0.42F + local * 0.2F);
                sample.continentNoise = Math.max(sample.continentNoise, 0.58F);
            }
        }
    }

    private void tryOceanVolcano(float worldX, float worldZ, NoiseSample sample) {
        int cell = VOLCANIC_OCEAN_CELL;
        int cx = NoiseUtil.floor(worldX / cell);
        int cz = NoiseUtil.floor(worldZ / cell);
        float place = hash01(this.seed ^ 0xB01C, cx, cz);
        // Moderate open-ocean density (default chance 0.15 → ~3% of cells place a candidate).
        if (place > this.volcanicChance * 0.22F) {
            return;
        }
        float centerX = (cx + 0.5F) * cell + (hash01(this.seed, cx, cz) - 0.5F) * cell * 0.35F;
        float centerZ = (cz + 0.5F) * cell + (hash01(this.seed ^ 3, cx, cz) - 0.5F) * cell * 0.35F;
        float radius = 90.0F + hash01(this.seed ^ 9, cx, cz) * 160.0F;
        this.paintVolcanoAt(worldX, worldZ, sample, centerX, centerZ, radius);
    }

    private void paintVolcanoCone(float worldX, float worldZ, NoiseSample sample, int ix, int iz, float radius) {
        // Anchor freckle to a stable local center so neighbouring samples share one cone.
        float centerX = (ix >> 4 << 4) + 8.0F;
        float centerZ = (iz >> 4 << 4) + 8.0F;
        this.paintVolcanoAt(worldX, worldZ, sample, centerX, centerZ, radius);
    }

    /**
     * Cone mountain ({@link TerrainType#VOLCANO}) with crater throat ({@link TerrainType#VOLCANO_PIPE}).
     */
    private void paintVolcanoAt(float worldX, float worldZ, NoiseSample sample, float centerX, float centerZ, float radius) {
        float dx = worldX - centerX;
        float dz = worldZ - centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);
        if (dist > radius) {
            return;
        }
        float t = 1.0F - dist / radius;
        // Outer flank rises; near centre dips into crater (pipe).
        float craterR = radius * 0.22F;
        float rimR = radius * 0.38F;
        float height;
        if (dist <= craterR) {
            // Throat / жерло — lower floor inside the crater.
            float inner = dist / Math.max(1.0F, craterR);
            height = 0.52F + inner * 0.12F;
            sample.terrainType = TerrainType.VOLCANO_PIPE;
        } else if (dist <= rimR) {
            // Rim of the crater.
            float rim = (dist - craterR) / Math.max(1.0E-3F, rimR - craterR);
            height = 0.64F + rim * 0.18F;
            sample.terrainType = TerrainType.VOLCANO;
        } else {
            // Outer volcano mountain flank.
            height = 0.48F + t * 0.38F;
            sample.terrainType = TerrainType.VOLCANO;
        }
        sample.continentNoise = Math.max(sample.continentNoise, 0.55F + t * 0.35F);
        sample.heightNoise = Math.max(sample.heightNoise, height);
        // Keep volcanic_island alias for filters that look for the NewTF tag.
        if (sample.terrainType == TerrainType.VOLCANO || sample.terrainType == TerrainType.VOLCANO_PIPE) {
            // Prefer engine types; ModTerrainTypes.VOLCANIC_ISLAND is also VOLCANO category.
        }
    }

    private void tryArchipelago(float worldX, float worldZ, NoiseSample sample, int seaLevel) {
        int cell = ARCHIPELAGO_CELL;
        int cx = NoiseUtil.floor(worldX / cell);
        int cz = NoiseUtil.floor(worldZ / cell);
        float place = hash01(this.seed ^ 99, cx, cz);
        if (place > this.archipelagoChance) {
            return;
        }

        float centerX = (cx + 0.5F) * cell;
        float centerZ = (cz + 0.5F) * cell;
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

        float ring = coreR * 2.2F;
        if (dist > ring) {
            return;
        }

        float angle = (float) (Math.atan2(dz, dx) / (Math.PI * 2.0) + 0.5);
        int slot = NoiseUtil.floor(angle * 12.0F);
        float slotCenter = (slot + 0.5F) / 12.0F;
        float angDist = Math.abs(angle - slotCenter);
        if (angDist > 0.04F) {
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
