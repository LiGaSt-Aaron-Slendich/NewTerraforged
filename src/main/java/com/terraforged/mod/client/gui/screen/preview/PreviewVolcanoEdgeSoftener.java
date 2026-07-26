package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Softens leftover hard circular volcano rims on preview tiles (engine VolcanoPopulator
 * clamp edge). Prefer zeroing volcano weight in {@link Preview}; this is a safety net.
 */
public final class PreviewVolcanoEdgeSoftener {
    private PreviewVolcanoEdgeSoftener() {
    }

    public static void apply(Tile tile) {
        if (tile == null) {
            return;
        }
        int size = tile.getBlockSize().size;
        float[] smoothed = new float[size * size];
        boolean[] volcano = new boolean[size * size];
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                Cell cell = tile.getCell(x, z);
                int i = z * size + x;
                volcano[i] = isVolcanoFamily(cell.terrain);
                smoothed[i] = cell.value;
            }
        }
        // 5x5 blur only on volcano cells that sit next to non-volcano (the hard rim).
        for (int z = 1; z < size - 1; z++) {
            for (int x = 1; x < size - 1; x++) {
                int i = z * size + x;
                if (!volcano[i]) {
                    continue;
                }
                boolean rim = false;
                for (int dz = -1; dz <= 1 && !rim; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (!volcano[(z + dz) * size + (x + dx)]) {
                            rim = true;
                            break;
                        }
                    }
                }
                if (!rim) {
                    continue;
                }
                float sum = 0.0F;
                float w = 0.0F;
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        int nx = x + dx;
                        int nz = z + dz;
                        if (nx < 0 || nz < 0 || nx >= size || nz >= size) {
                            continue;
                        }
                        float ww = 1.0F / (1.0F + dx * dx + dz * dz);
                        sum += tile.getCell(nx, nz).value * ww;
                        w += ww;
                    }
                }
                if (w > 0.0F) {
                    float blur = sum / w;
                    Cell cell = tile.getCell(x, z);
                    smoothed[i] = NoiseUtil.lerp(cell.value, blur, 0.72F);
                }
            }
        }
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int i = z * size + x;
                if (volcano[i]) {
                    tile.getCell(x, z).value = smoothed[i];
                }
            }
        }
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
