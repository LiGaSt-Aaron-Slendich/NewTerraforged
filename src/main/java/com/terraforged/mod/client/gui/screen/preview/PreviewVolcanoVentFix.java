package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;

/**
 * Ensures every volcano cone on the preview tile has a visible {@link TerrainType#VOLCANO_PIPE}
 * vent. Classic VolcanoPopulator throats are tiny and often skipped by preview zoom sampling.
 */
public final class PreviewVolcanoVentFix {
    private PreviewVolcanoVentFix() {
    }

    public static void apply(Tile tile) {
        if (tile == null) {
            return;
        }
        int size = tile.getBlockSize().size;
        // First pass: mark local height peaks inside volcano cones.
        boolean[] peak = new boolean[size * size];
        for (int z = 1; z < size - 1; z++) {
            for (int x = 1; x < size - 1; x++) {
                Cell cell = tile.getCell(x, z);
                if (!isVolcanoCone(cell.terrain)) {
                    continue;
                }
                float v = cell.value;
                boolean higher = true;
                for (int dz = -1; dz <= 1 && higher; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        Cell n = tile.getCell(x + dx, z + dz);
                        if (isVolcanoFamily(n.terrain) && n.value > v) {
                            higher = false;
                            break;
                        }
                    }
                }
                if (higher) {
                    peak[z * size + x] = true;
                }
            }
        }
        // Second pass: paint a small pipe disk around each peak.
        int r = 2;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (!peak[z * size + x]) {
                    continue;
                }
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        if (dx * dx + dz * dz > r * r) {
                            continue;
                        }
                        int px = x + dx;
                        int pz = z + dz;
                        if (px < 0 || pz < 0 || px >= size || pz >= size) {
                            continue;
                        }
                        Cell cell = tile.getCell(px, pz);
                        if (isVolcanoFamily(cell.terrain)) {
                            cell.terrain = TerrainType.VOLCANO_PIPE;
                        }
                    }
                }
            }
        }
    }

    private static boolean isVolcanoCone(Terrain t) {
        if (t == null) {
            return false;
        }
        if (t == TerrainType.VOLCANO || t == ModTerrainTypes.ISLAND_VOLCANO) {
            return true;
        }
        String n = t.getName();
        return n != null && (n.equalsIgnoreCase("volcano") || n.equalsIgnoreCase("island_volcano"));
    }

    private static boolean isVolcanoFamily(Terrain t) {
        if (t == null) {
            return false;
        }
        if (t == TerrainType.VOLCANO || t == TerrainType.VOLCANO_PIPE || t == ModTerrainTypes.ISLAND_VOLCANO) {
            return true;
        }
        String n = t.getName();
        return n != null && n.toLowerCase().contains("volcano");
    }
}
