package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.worldgen.terrain.MountainBeltApproximator;
import com.terraforged.mod.worldgen.terrain.MountainBeltField;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Preview height lift for sparse coastal mega-ridge spines (matches NoiseGenerator).
 */
public final class PreviewMountainBeltPainter {
    private PreviewMountainBeltPainter() {
    }

    public static void apply(Tile tile, Settings settings, int seed, int centerX, int centerZ, int zoom) {
        if (tile == null || settings == null || settings.world == null) {
            return;
        }
        int continentScale = settings.world.continent != null
                ? Math.max(400, settings.world.continent.continentScale)
                : 3000;
        Levels levels = new Levels(settings.world);
        float water = levels.water;
        int size = tile.getBlockSize().size;
        int half = size / 2;
        long beltSeed = seed;

        tile.iterate((cell, lx, lz) -> {
            int worldX = centerX + (lx - half) * zoom;
            int worldZ = centerZ + (lz - half) * zoom;
            float cn = NoiseUtil.clamp(cell.continentEdge, 0.0F, 1.0F);
            float belt = MountainBeltField.strength(worldX, worldZ, beltSeed, continentScale, cn);
            if (cell.value >= water - 0.002F) {
                if (belt >= 0.12F) {
                    float peak = belt * belt;
                    float boost = peak * 0.12F;
                    float pull = NoiseUtil.clamp(peak * 0.38F, 0.0F, 0.42F);
                    float target = Math.min(water + 0.32F, cell.value + boost);
                    cell.value = NoiseUtil.clamp(
                            NoiseUtil.lerp(cell.value, Math.max(cell.value, target), pull),
                            0.0F,
                            1.0F);
                    if (belt > 0.72F && cell.terrain != null && cell.terrain.isOverground()
                            && !cell.terrain.isRiver() && !cell.terrain.isLake()) {
                        cell.terrain = TerrainType.MOUNTAINS;
                    }
                }
                if (belt >= 0.50F) {
                    cell.value = MountainBeltApproximator.fillValleyDepth(
                            cell.value, worldX, worldZ, beltSeed, continentScale, cn);
                }
            } else if (belt >= 0.20F) {
                float under = MountainBeltField.underwaterStrength(worldX, worldZ, beltSeed, continentScale);
                float nearShore = NoiseUtil.clamp(1.0F - cn / 0.45F, 0.0F, 1.0F);
                float lift = under * (0.35F + 0.65F * nearShore) * 0.04F;
                cell.value = Math.min(water - 0.004F, cell.value + lift);
            }
        });
    }
}
