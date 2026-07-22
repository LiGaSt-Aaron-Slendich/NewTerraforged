package com.terraforged.mod.worldgen.noise.continent.island;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Overlays coastal / volcanic / independent islands and Scattered Archipelago clusters.
 * Laguna = shallow water between islands. Rivers/lakes = inland hydrology on island land.
 */
public final class IslandFeatureOverlay {
    public static final int LAGUNA_MAX_DEPTH = IslandScatter.LAGUNA_MAX_DEPTH;

    private final int seed;
    private final int continentScale;
    private final float coastalChance;
    private final float volcanicChance;
    private final boolean archipelago;
    private final float archipelagoChance;
    private final boolean shipwrecked;

    public IslandFeatureOverlay(ContinentConfig config) {
        this.seed = config.shape.seed0 ^ 0x51ED;
        this.continentScale = Math.max(100, config.shape.scale);
        this.coastalChance = NoiseUtil.clamp(config.shape.coastalIslandsChance, 0.0F, 1.0F);
        this.volcanicChance = NoiseUtil.clamp(config.shape.volcanicIslandsChance, 0.0F, 1.0F);
        this.archipelago = config.shape.scatteredArchipelago;
        this.archipelagoChance = NoiseUtil.clamp(config.shape.scatteredArchipelagoChance, 0.0F, 1.0F);
        this.shipwrecked = config.shape.shipwrecked;
    }

    public void apply(float worldX, float worldZ, NoiseSample sample, int seaLevel) {
        float cn = sample.continentNoise;
        float proximity = IslandScatter.shoreProximity(cn, this.continentScale);
        float midOcean = IslandScatter.midOceanAllow(cn);
        if (this.shipwrecked) {
            midOcean = Math.max(midOcean, 0.55F + (1.0F - NoiseUtil.clamp(cn, 0.0F, 1.0F)) * 0.35F);
            proximity = Math.max(proximity, 0.15F);
        }

        if (cn < 0.55F || this.shipwrecked) {
            if (this.archipelago && this.archipelagoChance > 0.0F) {
                this.paint(IslandScatter.evalArchipelago(
                        worldX, worldZ, this.seed, this.archipelagoChance, proximity, midOcean), sample, seaLevel);
            }
            this.paint(IslandScatter.evalIndependentIsland(
                    worldX, worldZ, this.seed, this.archipelagoChance, this.coastalChance,
                    proximity, midOcean, this.shipwrecked), sample, seaLevel);
            if (this.volcanicChance > 0.0F) {
                this.paint(IslandScatter.evalOceanVolcano(
                        worldX, worldZ, this.seed, this.volcanicChance, proximity, midOcean, this.archipelagoChance),
                        sample, seaLevel);
            }
        }

        this.paint(IslandScatter.evalCoastalFreckle(
                worldX, worldZ, this.seed, this.coastalChance, this.volcanicChance, cn), sample, seaLevel);
    }

    private void paint(IslandScatter.ClusterEval eval, NoiseSample sample, int seaLevel) {
        if (eval == null || eval == IslandScatter.ClusterEval.NONE) {
            return;
        }
        if (eval.laguna()) {
            sample.continentNoise = Math.max(sample.continentNoise, 0.30F);
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
        } else if (eval.hydrology() == IslandScatter.Hydrology.RIVER) {
            sample.terrainType = TerrainType.RIVER;
            sample.continentNoise = Math.max(sample.continentNoise, 0.62F);
            sample.heightNoise = Math.min(sample.heightNoise, eval.heightBoost());
            return;
        } else if (eval.hydrology() == IslandScatter.Hydrology.LAKE) {
            sample.terrainType = TerrainType.LAKE;
            sample.continentNoise = Math.max(sample.continentNoise, 0.62F);
            sample.heightNoise = Math.min(sample.heightNoise, eval.heightBoost());
            return;
        } else {
            sample.terrainType = landformTerrain(eval.landform(), sample.continentNoise < 0.55F || this.shipwrecked);
        }
        sample.continentNoise = Math.max(sample.continentNoise, 0.58F + eval.heightBoost() * 0.25F);
        sample.heightNoise = Math.max(sample.heightNoise, eval.heightBoost());
    }

    private static Terrain landformTerrain(IslandScatter.Landform landform, boolean archipelago) {
        if (!archipelago) {
            return ModTerrainTypes.COASTAL_ISLAND;
        }
        return switch (landform) {
            case MOUNTAINS -> ModTerrainTypes.ARCHIPELAGO_MOUNTAINS;
            case PLATEAU -> ModTerrainTypes.ARCHIPELAGO_PLATEAU;
            case HILLS -> ModTerrainTypes.ARCHIPELAGO_HILLS;
            case FLATS -> ModTerrainTypes.SCATTERED_ARCHIPELAGO;
        };
    }
}
