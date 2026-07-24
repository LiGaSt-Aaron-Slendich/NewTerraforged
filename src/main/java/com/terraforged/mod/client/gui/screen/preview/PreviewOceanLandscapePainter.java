package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import com.terraforged.mod.worldgen.noise.continent.ocean.DeepVolcano;
import com.terraforged.mod.worldgen.noise.continent.ocean.OceanZoneMask;
import com.terraforged.mod.worldgen.noise.continent.ocean.SeafloorLandscape;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Preview paint for EGF Ocean Landscape (corridor banks + deep volcanoes).
 * Approximate — no full ContinentGenerator cell graph in the engine preview tile.
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
        float volcanoDensity = NoiseUtil.clamp(ol.volcanoDensity, 0.0F, 1.0F);
        boolean shipwrecked = settings.world.properties != null
                && settings.world.properties.worldStyle == WorldSettings.WorldStyle.SHIPWRECKED;
        Levels levels = new Levels(settings.world);
        float water = levels.water;
        int size = tile.getBlockSize().size;
        int half = size / 2;
        int paintSeed = seed ^ 0x0CEA11;

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
            float deep = OceanZoneMask.deepAllow(cn);
            float corridor;
            if (shipwrecked) {
                corridor = OceanZoneMask.shipwreckedBanks(worldX * 0.0004F, worldZ * 0.0004F, paintSeed)
                        * OceanZoneMask.SHIP_BANK_CAP * (0.55F + 0.45F * corridorStrength);
                deep = Math.max(deep, 0.55F + (1.0F - NoiseUtil.clamp(cn, 0.0F, 1.0F)) * 0.35F);
            } else if (cn >= OceanZoneMask.SHORE_CN) {
                corridor = 0.0F;
            } else {
                // Preview lacks ContinentGenerator — approximate corridor (worldgen uses guaranteed pairs).
                float ridge = SeafloorLandscape.relief(worldX, worldZ, paintSeed ^ 0xC0FF, noiseScale);
                corridor = ridge * (1.0F - cn / OceanZoneMask.SHORE_CN) * corridorStrength;
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

    /** True when EGF Ocean Landscape should drive preview instead of legacy island paint. */
    public static boolean shouldApply() {
        return TFNoiseVariantFlags.oceanLandscapeEnabled();
    }
}
