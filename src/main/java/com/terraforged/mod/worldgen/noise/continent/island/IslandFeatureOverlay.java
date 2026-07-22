package com.terraforged.mod.worldgen.noise.continent.island;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Overlays coastal / volcanic / independent islands and Archipelago / Scattered Archipelago clusters.
 * Laguna = shallow water between islands. Rivers/lakes = inland hydrology on island land.
 * Near-shore / mainland samples are culled so islands are not absorbed into continents.
 */
public final class IslandFeatureOverlay {
    public static final int LAGUNA_MAX_DEPTH = IslandScatter.LAGUNA_MAX_DEPTH;

    private final int seed;
    private final int continentScale;
    private final float coastalChance;
    private final float volcanicChance;
    private final boolean archipelago;
    private final float archipelagoChance;
    private final boolean scatteredArchipelago;
    private final float scatteredArchipelagoChance;
    private final boolean shipwrecked;

    public IslandFeatureOverlay(ContinentConfig config) {
        this.seed = config.shape.seed0 ^ 0x51ED;
        this.continentScale = Math.max(100, config.shape.scale);
        this.coastalChance = NoiseUtil.clamp(config.shape.coastalIslandsChance, 0.0F, 1.0F);
        this.volcanicChance = NoiseUtil.clamp(config.shape.volcanicIslandsChance, 0.0F, 1.0F);
        this.archipelago = config.shape.archipelago;
        this.archipelagoChance = NoiseUtil.clamp(config.shape.archipelagoChance, 0.0F, 1.0F);
        this.scatteredArchipelago = config.shape.scatteredArchipelago;
        this.scatteredArchipelagoChance = NoiseUtil.clamp(config.shape.scatteredArchipelagoChance, 0.0F, 1.0F);
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

        float clusterPressure = Math.max(
                this.archipelago ? this.archipelagoChance : 0.0F,
                this.scatteredArchipelago ? this.scatteredArchipelagoChance : 0.0F);

        // Hard cull: never stamp island / archipelago terrain onto near-shore or mainland.
        boolean shoreBlocked = !this.shipwrecked
                && IslandScatter.tooCloseToShore(cn, proximity, midOcean);

        if (!shoreBlocked && (cn < IslandScatter.SHORE_CULL_CN || this.shipwrecked)) {
            if (this.archipelago && this.archipelagoChance > 0.0F) {
                this.paint(IslandScatter.evalArchipelago(
                        worldX, worldZ, this.seed, this.archipelagoChance, proximity, midOcean,
                        IslandScatter.ArchipelagoStyle.ARCHIPELAGO, cn), sample, seaLevel, cn);
            }
            if (this.scatteredArchipelago && this.scatteredArchipelagoChance > 0.0F) {
                this.paint(IslandScatter.evalArchipelago(
                        worldX, worldZ, this.seed, this.scatteredArchipelagoChance, proximity, midOcean,
                        IslandScatter.ArchipelagoStyle.SCATTERED, cn), sample, seaLevel, cn);
            }
            this.paint(IslandScatter.evalIndependentIsland(
                    worldX, worldZ, this.seed, clusterPressure, this.coastalChance,
                    proximity, midOcean, this.shipwrecked, cn), sample, seaLevel, cn);
            if (this.volcanicChance > 0.0F) {
                this.paint(IslandScatter.evalOceanVolcano(
                        worldX, worldZ, this.seed, this.volcanicChance, proximity, midOcean,
                        clusterPressure, cn), sample, seaLevel, cn);
            }
        }

        // Coastal freckles stay near the shore belt, but never on mainland land.
        if (cn < IslandScatter.SHORE_CULL_CN) {
            this.paint(IslandScatter.evalCoastalFreckle(
                    worldX, worldZ, this.seed, this.coastalChance, this.volcanicChance, cn), sample, seaLevel, cn);
        }
    }

    private void paint(IslandScatter.ClusterEval eval, NoiseSample sample, int seaLevel, float originalCn) {
        if (eval == null || eval == IslandScatter.ClusterEval.NONE) {
            return;
        }
        // Safety: never overwrite continent land / coastal shelf with island terrains.
        if (!this.shipwrecked && originalCn >= IslandScatter.SHORE_CULL_CN) {
            return;
        }
        // Keep crater pipe once placed — freckles/islets must not stamp over it.
        boolean hadPipe = sample.terrainType == TerrainType.VOLCANO_PIPE;
        if (hadPipe && !(eval.volcano() && eval.pipe())) {
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
            sample.continentNoise = Math.max(sample.continentNoise, 0.58F);
            sample.baseNoise = Math.max(sample.baseNoise, 0.20F);
            // Absolute crater floor (not Math.max) so prior island land cannot fill the pipe.
            sample.heightNoise = eval.heightBoost();
            return;
        }
        if (eval.volcano()) {
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
            sample.terrainType = landformTerrain(
                    eval.landform(), originalCn < IslandScatter.SHORE_CULL_CN || this.shipwrecked, eval.scattered());
        }
        float boost = eval.heightBoost();
        sample.continentNoise = Math.max(sample.continentNoise, 0.58F + boost * 0.25F);
        sample.baseNoise = Math.max(sample.baseNoise, 0.12F + boost * 0.45F);
        sample.heightNoise = Math.max(sample.heightNoise, boost);
    }

    private static Terrain landformTerrain(IslandScatter.Landform landform, boolean archipelago, boolean scattered) {
        if (!archipelago) {
            return ModTerrainTypes.COASTAL_ISLAND;
        }
        return switch (landform) {
            case MOUNTAINS -> ModTerrainTypes.ARCHIPELAGO_MOUNTAINS;
            case PLATEAU -> ModTerrainTypes.ARCHIPELAGO_PLATEAU;
            case HILLS -> ModTerrainTypes.ARCHIPELAGO_HILLS;
            case FLATS -> scattered ? ModTerrainTypes.SCATTERED_ARCHIPELAGO : ModTerrainTypes.ARCHIPELAGO_HILLS;
        };
    }
}
