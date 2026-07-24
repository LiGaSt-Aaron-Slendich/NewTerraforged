package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.ocean.DeepVolcano;
import com.terraforged.mod.worldgen.noise.continent.ocean.OceanCorridorGraph;
import com.terraforged.mod.worldgen.noise.continent.ocean.OceanZoneMask;
import com.terraforged.mod.worldgen.noise.continent.ocean.SeafloorLandscape;
import com.terraforged.mod.worldgen.settings.ContinentShapeWiring;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Preview paint for EGF Ocean Landscape (corridor banks + deep volcanoes).
 * Builds the same corridor graph as worldgen so ridges follow near-neighbour links
 * (warp/offset still differ slightly from full ContinentNoise).
 */
public final class PreviewOceanLandscapePainter {
    private PreviewOceanLandscapePainter() {
    }

    public static void apply(Tile tile, Settings settings, int seed, int centerX, int centerZ, int zoom) {
        if (tile == null || settings == null || settings.world == null) {
            return;
        }
        WorldSettings.OceanLandscape ol = settings.world.oceanLandscape != null
                ? settings.world.oceanLandscape
                : new WorldSettings.OceanLandscape();
        float noiseScale = NoiseUtil.clamp(ol.noiseScale, 0.25F, 3.0F);
        float corridorStrength = NoiseUtil.clamp(ol.corridorStrength, 0.0F, 1.0F);
        float shelfStrength = NoiseUtil.clamp(ol.shelfStrength, 0.0F, 1.0F);
        float volcanoDensity = NoiseUtil.clamp(ol.volcanoDensity, 0.0F, 1.0F);
        int partners = Math.max(1, Math.min(4, ol.corridorPartners));
        float maxDist = NoiseUtil.clamp(ol.corridorMaxDistance, 2.0F, 24.0F);
        boolean shipwrecked = settings.world.properties != null
                && settings.world.properties.worldStyle == WorldSettings.WorldStyle.SHIPWRECKED;
        Levels levels = new Levels(settings.world);
        float water = levels.water;
        int size = tile.getBlockSize().size;
        int half = size / 2;
        int paintSeed = seed ^ 0x0CEA11;
        int continentScale = Math.max(100, settings.world.continent != null
                ? settings.world.continent.continentScale
                : 3000);
        float frequency = 1.0F / continentScale;

        ContinentGenerator continent = null;
        OceanCorridorGraph graph = null;
        if (!shipwrecked) {
            try {
                continent = buildPreviewContinent(settings, seed);
                graph = OceanCorridorGraph.build(continent, partners, maxDist);
            } catch (Throwable ignored) {
                continent = null;
                graph = null;
            }
        }

        if (shipwrecked) {
            tile.iterate((cell, lx, lz) -> {
                cell.continentEdge = 0.0F;
                cell.value = water - 0.04F;
                cell.terrain = TerrainType.DEEP_OCEAN;
                cell.riverMask = 1.0F;
            });
        }

        final ContinentGenerator cont = continent;
        final OceanCorridorGraph corridorGraph = graph;

        tile.iterate((cell, lx, lz) -> {
            int worldX = centerX + (lx - half) * zoom;
            int worldZ = centerZ + (lz - half) * zoom;
            float cn = cell.continentEdge;
            float deep = OceanZoneMask.deepAllow(cn);
            float corridor;
            if (shipwrecked) {
                corridor = OceanZoneMask.shipwreckedBanks(worldX * 0.0004F, worldZ * 0.0004F, paintSeed)
                        * OceanZoneMask.SHIP_BANK_CAP * (0.55F + 0.45F * corridorStrength);
                deep = Math.max(deep, 0.55F + (1.0F - NoiseUtil.clamp(cn, 0.0F, 1.0F)) * 0.35F);
            } else if (cn >= OceanZoneMask.SHORE_CN) {
                corridor = 0.0F;
            } else if (cont != null && corridorGraph != null && corridorGraph.active()) {
                float shapeX = worldX * frequency;
                float shapeY = worldZ * frequency;
                OceanZoneMask.Zone zone = OceanZoneMask.evaluate(
                        cont, corridorGraph, shapeX, shapeY, cn, false, corridorStrength, shelfStrength);
                corridor = zone.corridor();
                deep = Math.max(deep, zone.deep());
            } else {
                // No free Voronoi / relief corridors — only continent→continent graph ridges.
                corridor = 0.0F;
            }

            if (corridor > 0.04F) {
                float relief = SeafloorLandscape.relief(worldX, worldZ, paintSeed, noiseScale);
                float masked = relief * corridor;
                float emergeAt = shipwrecked
                        ? SeafloorLandscape.SHIP_EMERGE_THRESHOLD
                        : SeafloorLandscape.EMERGE_THRESHOLD;
                if (masked >= emergeAt) {
                    cell.continentEdge = Math.max(cell.continentEdge, 0.58F + masked * 0.22F);
                    cell.value = Math.max(cell.value, water + 0.02F + masked * 0.08F);
                    SeafloorLandscape.Form form = SeafloorLandscape.formFor(
                            masked, paintSeed, worldX >> 4, worldZ >> 4);
                    cell.terrain = switch (form) {
                        case MOUNTAINS -> ModTerrainTypes.ISLAND_MOUNTAINS;
                        case HILLS -> ModTerrainTypes.ISLAND_HILLS;
                        case FLATS -> ModTerrainTypes.ISLAND_FLATS;
                    };
                } else {
                    cell.value = Math.max(cell.value, water - 0.05F + masked * 0.04F);
                }
            }

            if (deep > 0.08F && volcanoDensity > 0.01F) {
                DeepVolcano.Result v = DeepVolcano.eval(
                        worldX, worldZ, paintSeed, deep, shipwrecked, volcanoDensity);
                if (v.hit()) {
                    if (v.pipe()) {
                        cell.terrain = TerrainType.VOLCANO_PIPE;
                        cell.continentEdge = Math.max(cell.continentEdge, 0.58F);
                        cell.value = Math.max(cell.value, water + 0.05F);
                    } else {
                        cell.terrain = ModTerrainTypes.ISLAND_VOLCANO;
                        cell.continentEdge = Math.max(cell.continentEdge, 0.58F + v.heightBoost() * 0.25F);
                        cell.value = Math.max(cell.value, water + 0.04F + v.heightBoost() * 0.10F);
                    }
                }
            }
        });
    }

    private static ContinentGenerator buildPreviewContinent(Settings settings, int seed) {
        ContinentConfig config = new ContinentConfig();
        config.shape.seed0 = seed ^ 0xC0FFEE;
        config.shape.seed1 = seed ^ 0x51ED51ED;
        ContinentShapeWiring.apply(config, settings);
        NoiseLevels noiseLevels = new NoiseLevels(
                false,
                1.0F,
                settings.world.properties.seaLevel,
                Math.max(0, settings.world.properties.seaLevel - 40),
                settings.world.properties.worldHeight,
                0);
        ControlPoints controlPoints = new ControlPoints(settings.world.controlPoints);
        return new ContinentGenerator(config, noiseLevels, controlPoints);
    }

    /** True when EGF Ocean Landscape should drive preview instead of legacy island paint. */
    public static boolean shouldApply() {
        return TFNoiseVariantFlags.oceanLandscapeEnabled();
    }
}
