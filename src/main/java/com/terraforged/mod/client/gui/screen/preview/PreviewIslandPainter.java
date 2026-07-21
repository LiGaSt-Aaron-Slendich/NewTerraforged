package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.worldgen.noise.continent.island.IslandScatter;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Paints NewTF island / archipelago / volcanic freckles onto the engine preview tile.
 * Uses the same {@link IslandScatter} math as worldgen so preview tracks continent shifts.
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
        boolean archOn = islands.scatteredArchipelago;
        float archChance = NoiseUtil.clamp(islands.scatteredArchipelagoChance, 0.0F, 1.0F);
        int continentScale = settings.world.continent != null
                ? Math.max(100, settings.world.continent.continentScale)
                : 3000;
        int paintSeed = seed ^ 0x51ED;
        Levels levels = new Levels(settings.world);
        float water = levels.water;
        int size = tile.getBlockSize().size;
        int half = size / 2;

        tile.iterate((cell, lx, lz) -> {
            int worldX = centerX + (lx - half) * zoom;
            int worldZ = centerZ + (lz - half) * zoom;
            float cn = cell.continentEdge;
            float proximity = IslandScatter.shoreProximity(cn, continentScale);
            float midOcean = IslandScatter.midOceanAllow(cn);

            if (cn < 0.55F) {
                if (archOn && archChance > 0.0F) {
                    paintEval(cell, IslandScatter.evalArchipelago(
                            worldX, worldZ, paintSeed, archChance, proximity, midOcean), water, true);
                }
                if (volcanic > 0.0F) {
                    paintEval(cell, IslandScatter.evalOceanVolcano(
                            worldX, worldZ, paintSeed, volcanic, proximity, midOcean), water, false);
                }
            }

            paintEval(cell, IslandScatter.evalCoastalFreckle(
                    worldX, worldZ, paintSeed, coastal, volcanic, cn), water, false);
        });
    }

    private static void paintEval(Cell cell, IslandScatter.ClusterEval eval, float water, boolean archipelago) {
        if (eval == null || eval == IslandScatter.ClusterEval.NONE) {
            return;
        }
        if (eval.laguna()) {
            cell.terrain = ModTerrainTypes.LAGUNA;
            cell.continentEdge = Math.max(cell.continentEdge, 0.28F);
            cell.value = Math.min(cell.value, water - 0.008F);
            return;
        }
        if (!eval.land()) {
            return;
        }
        if (eval.pipe()) {
            cell.terrain = TerrainType.VOLCANO_PIPE;
        } else if (eval.volcano()) {
            cell.terrain = cell.continentEdge < 0.55F ? ModTerrainTypes.VOLCANIC_ISLAND : TerrainType.VOLCANO;
        } else if (archipelago || cell.continentEdge < 0.55F) {
            cell.terrain = ModTerrainTypes.SCATTERED_ARCHIPELAGO;
        } else {
            cell.terrain = ModTerrainTypes.COASTAL_ISLAND;
        }
        cell.continentEdge = Math.max(cell.continentEdge, 0.58F + eval.heightBoost() * 0.25F);
        cell.value = Math.max(cell.value, water + 0.02F + eval.heightBoost() * 0.20F);
    }
}
