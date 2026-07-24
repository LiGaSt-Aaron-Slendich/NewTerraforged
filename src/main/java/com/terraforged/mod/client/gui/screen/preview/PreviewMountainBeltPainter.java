package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.worldgen.terrain.MountainBeltField;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Preview-only height lift for continent-scale mountain spines so HEIGHT normals
 * show continuous belts across land and a weaker continuation offshore.
 * Worldgen applies the same field in {@code NoiseGenerator}.
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
            float belt = MountainBeltField.strength(worldX, worldZ, beltSeed, continentScale);
            if (belt < 0.06F) {
                return;
            }
            if (cell.value >= water - 0.002F) {
                // Land / emergent: strong spine.
                float boost = belt * belt * 0.14F;
                cell.value = NoiseUtil.clamp(cell.value + boost * (1.0F - (cell.value - water) * 0.5F), 0.0F, 1.0F);
                if (belt > 0.70F && cell.terrain != null && cell.terrain.isOverground()
                        && !cell.terrain.isRiver() && !cell.terrain.isLake()) {
                    cell.terrain = TerrainType.MOUNTAINS;
                }
            } else {
                // Offshore continuation — weaker seafloor ridge toward sea level.
                float under = MountainBeltField.underwaterStrength(worldX, worldZ, beltSeed, continentScale);
                float cn = NoiseUtil.clamp(cell.continentEdge, 0.0F, 1.0F);
                float nearShore = NoiseUtil.clamp(1.0F - cn / 0.45F, 0.0F, 1.0F);
                float lift = under * (0.35F + 0.65F * nearShore) * 0.055F;
                cell.value = Math.min(water - 0.004F, cell.value + lift);
            }
        });
    }
}
