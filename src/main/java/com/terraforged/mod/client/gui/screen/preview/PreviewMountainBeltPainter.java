package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.TerrainType;
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
        if (!com.terraforged.mod.platform.forge.TFNoiseVariantFlags.megaRidgesEnabled()) {
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
                if (belt >= 0.05F) {
                    // Match NoiseGenerator: relative boost, fades with belt (no pedestal).
                    float tip = belt * belt * belt;
                    float boost = belt * 0.015F + tip * 0.10F;
                    cell.value = NoiseUtil.clamp(cell.value + boost, 0.0F, 1.0F);
                    if (belt > 0.75F && cell.terrain != null && cell.terrain.isOverground()
                            && !cell.terrain.isRiver() && !cell.terrain.isLake()) {
                        cell.terrain = TerrainType.MOUNTAINS;
                    }
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
