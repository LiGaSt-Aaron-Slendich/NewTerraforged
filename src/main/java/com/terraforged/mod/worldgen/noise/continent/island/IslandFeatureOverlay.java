package com.terraforged.mod.worldgen.noise.continent.island;

import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Overlays coastal / volcanic freckles and Scattered Archipelago clusters onto continent samples.
 * Placement follows {@code continentNoise} (moves with landmasses): densest near shores / channels,
 * rare mid-ocean. Non-volcano islands are elongated organic blobs; volcanoes are cone + pipe.
 */
public final class IslandFeatureOverlay {
    public static final int LAGUNA_MAX_DEPTH = IslandScatter.LAGUNA_MAX_DEPTH;

    private final int seed;
    private final int continentScale;
    private final float coastalChance;
    private final float volcanicChance;
    private final boolean archipelago;
    private final float archipelagoChance;

    public IslandFeatureOverlay(ContinentConfig config) {
        this.seed = config.shape.seed0 ^ 0x51ED;
        this.continentScale = Math.max(100, config.shape.scale);
        this.coastalChance = NoiseUtil.clamp(config.shape.coastalIslandsChance, 0.0F, 1.0F);
        this.volcanicChance = NoiseUtil.clamp(config.shape.volcanicIslandsChance, 0.0F, 1.0F);
        this.archipelago = config.shape.scatteredArchipelago;
        this.archipelagoChance = NoiseUtil.clamp(config.shape.scatteredArchipelagoChance, 0.0F, 1.0F);
    }

    public void apply(float worldX, float worldZ, NoiseSample sample, int seaLevel) {
        float cn = sample.continentNoise;
        float proximity = IslandScatter.shoreProximity(cn, this.continentScale);
        float midOcean = IslandScatter.midOceanAllow(cn);

        // Ocean / shelf: archipelagos + mid-ocean volcanoes react to continent falloff.
        if (cn < 0.55F) {
            if (this.archipelago && this.archipelagoChance > 0.0F) {
                this.paint(IslandScatter.evalArchipelago(
                        worldX, worldZ, this.seed, this.archipelagoChance, proximity, midOcean), sample, seaLevel);
            }
            if (this.volcanicChance > 0.0F) {
                this.paint(IslandScatter.evalOceanVolcano(
                        worldX, worldZ, this.seed, this.volcanicChance, proximity, midOcean), sample, seaLevel);
            }
        }

        // Coastal freckles on the shore band (land + near-shore).
        this.paint(IslandScatter.evalCoastalFreckle(
                worldX, worldZ, this.seed, this.coastalChance, this.volcanicChance, cn), sample, seaLevel);
    }

    private void paint(IslandScatter.ClusterEval eval, NoiseSample sample, int seaLevel) {
        if (eval == null || eval == IslandScatter.ClusterEval.NONE) {
            return;
        }
        if (eval.laguna()) {
            sample.continentNoise = Math.max(sample.continentNoise, 0.28F);
            sample.terrainType = ModTerrainTypes.LAGUNA;
            float depthNorm = LAGUNA_MAX_DEPTH / Math.max(1.0F, (float) seaLevel);
            sample.heightNoise = Math.min(sample.heightNoise, Math.max(0.05F, 0.34F - depthNorm * 0.12F));
            return;
        }
        if (!eval.land()) {
            return;
        }
        if (eval.pipe()) {
            sample.terrainType = TerrainType.VOLCANO_PIPE;
        } else if (eval.volcano()) {
            sample.terrainType = TerrainType.VOLCANO;
        } else if (sample.continentNoise < 0.55F) {
            sample.terrainType = ModTerrainTypes.SCATTERED_ARCHIPELAGO;
        } else {
            sample.terrainType = ModTerrainTypes.COASTAL_ISLAND;
        }
        sample.continentNoise = Math.max(sample.continentNoise, 0.58F + eval.heightBoost() * 0.25F);
        sample.heightNoise = Math.max(sample.heightNoise, eval.heightBoost());
    }
}
