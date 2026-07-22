package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.worldgen.noise.continent.island.IslandScatter;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Paints NewTF island / archipelago / volcanic freckles onto the engine preview tile.
 */
public final class PreviewIslandPainter {
    private PreviewIslandPainter() {
    }

    public static void apply(Tile tile, Settings settings, int seed, int centerX, int centerZ, int zoom) {
        if (tile == null || settings == null || settings.world == null) {
            return;
        }
        WorldSettings.Islands islands = settings.world.islands != null ? settings.world.islands : new WorldSettings.Islands();
        float coastal = NoiseUtil.clamp(islands.coastalIslandsChance, 0.0F, 1.0F);
        float volcanic = NoiseUtil.clamp(islands.volcanicIslandsChance, 0.0F, 1.0F);
        boolean archOn = islands.archipelago;
        float archChance = NoiseUtil.clamp(islands.archipelagoChance, 0.0F, 1.0F);
        boolean scatteredOn = islands.scatteredArchipelago;
        float scatteredChance = NoiseUtil.clamp(islands.scatteredArchipelagoChance, 0.0F, 1.0F);
        float clusterPressure = Math.max(archOn ? archChance : 0.0F, scatteredOn ? scatteredChance : 0.0F);
        boolean shipwrecked = settings.world.properties != null
                && settings.world.properties.worldStyle == WorldSettings.WorldStyle.SHIPWRECKED;
        int continentScaleRaw = settings.world.continent != null
                ? Math.max(100, settings.world.continent.continentScale)
                : 3000;
        final int continentScale = shipwrecked ? Math.min(continentScaleRaw, 1400) : continentScaleRaw;
        int paintSeed = seed ^ 0x51ED;
        Levels levels = new Levels(settings.world);
        float water = levels.water;
        int size = tile.getBlockSize().size;
        int half = size / 2;

        // Preview uses classic engine TileGenerator which still paints continents.
        // Shipwrecked must be ocean-only before island overlays run.
        if (shipwrecked) {
            tile.iterate((cell, lx, lz) -> {
                cell.continentEdge = 0.0F;
                cell.value = water - 0.04F;
                cell.terrain = TerrainType.DEEP_OCEAN;
                cell.riverMask = 1.0F;
            });
        }

        tile.iterate((cell, lx, lz) -> {
            int worldX = centerX + (lx - half) * zoom;
            int worldZ = centerZ + (lz - half) * zoom;
            float cn = cell.continentEdge;
            float proximity0 = IslandScatter.shoreProximity(cn, continentScale);
            float midOcean0 = IslandScatter.midOceanAllow(cn);
            final float proximity = shipwrecked
                    ? Math.max(proximity0, 0.15F)
                    : proximity0;
            final float midOcean = shipwrecked
                    ? Math.max(midOcean0, 0.55F + (1.0F - NoiseUtil.clamp(cn, 0.0F, 1.0F)) * 0.35F)
                    : midOcean0;

            boolean shoreBlocked = !shipwrecked && IslandScatter.tooCloseToShore(cn, proximity, midOcean);
            if (!shoreBlocked && (cn < IslandScatter.SHORE_CULL_CN || shipwrecked)) {
                if (archOn && archChance > 0.0F) {
                    paintEval(cell, IslandScatter.evalArchipelago(
                            worldX, worldZ, paintSeed, archChance, proximity, midOcean,
                            IslandScatter.ArchipelagoStyle.ARCHIPELAGO, cn), water, true, cn, shipwrecked);
                }
                if (scatteredOn && scatteredChance > 0.0F) {
                    paintEval(cell, IslandScatter.evalArchipelago(
                            worldX, worldZ, paintSeed, scatteredChance, proximity, midOcean,
                            IslandScatter.ArchipelagoStyle.SCATTERED, cn), water, true, cn, shipwrecked);
                }
                paintEval(cell, IslandScatter.evalIndependentIsland(
                        worldX, worldZ, paintSeed, clusterPressure, coastal, proximity, midOcean, shipwrecked, cn),
                        water, true, cn, shipwrecked);
                if (volcanic > 0.0F) {
                    paintEval(cell, IslandScatter.evalOceanVolcano(
                            worldX, worldZ, paintSeed, volcanic, proximity, midOcean, clusterPressure, cn),
                            water, false, cn, shipwrecked);
                }
            }

            if (cn < IslandScatter.SHORE_CULL_CN) {
                paintEval(cell, IslandScatter.evalCoastalFreckle(
                        worldX, worldZ, paintSeed, coastal, volcanic, cn), water, false, cn, shipwrecked);
            }
        });
    }

    private static void paintEval(
            Cell cell, IslandScatter.ClusterEval eval, float water, boolean archipelago, float originalCn, boolean shipwrecked
    ) {
        if (eval == null || eval == IslandScatter.ClusterEval.NONE) {
            return;
        }
        if (!shipwrecked && originalCn >= IslandScatter.SHORE_CULL_CN) {
            return;
        }
        if (eval.laguna()) {
            cell.terrain = ModTerrainTypes.LAGUNA;
            cell.continentEdge = Math.max(cell.continentEdge, 0.30F);
            cell.value = Math.min(cell.value, water - 0.012F);
            return;
        }
        if (!eval.land()) {
            return;
        }
        if (eval.pipe()) {
            cell.terrain = TerrainType.VOLCANO_PIPE;
            cell.continentEdge = Math.max(cell.continentEdge, 0.58F);
            // Crater floor below rim — do not Math.max over prior land.
            cell.value = water + 0.01F + eval.heightBoost() * 0.12F;
            return;
        }
        if (eval.volcano()) {
            cell.terrain = originalCn < IslandScatter.SHORE_CULL_CN ? ModTerrainTypes.VOLCANIC_ISLAND : TerrainType.VOLCANO;
        } else if (eval.hydrology() == IslandScatter.Hydrology.RIVER) {
            cell.terrain = TerrainType.RIVER;
            cell.continentEdge = Math.max(cell.continentEdge, 0.62F);
            cell.value = Math.min(cell.value, water - 0.002F);
            return;
        } else if (eval.hydrology() == IslandScatter.Hydrology.LAKE) {
            cell.terrain = TerrainType.LAKE;
            cell.continentEdge = Math.max(cell.continentEdge, 0.62F);
            cell.value = Math.min(cell.value, water - 0.004F);
            return;
        } else {
            // Do not stamp islets over an existing pipe crater.
            if (cell.terrain == TerrainType.VOLCANO_PIPE) {
                return;
            }
            cell.terrain = landformTerrain(
                    eval.landform(), archipelago || originalCn < IslandScatter.SHORE_CULL_CN, eval.scattered());
        }
        cell.continentEdge = Math.max(cell.continentEdge, 0.58F + eval.heightBoost() * 0.25F);
        cell.value = Math.max(cell.value, water + 0.02F + eval.heightBoost() * 0.20F);
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
