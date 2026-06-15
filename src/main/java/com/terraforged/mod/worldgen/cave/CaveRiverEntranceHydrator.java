package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;

/**
 * Fills river-entrance shafts with water when terrain carving opened a void under a river bed.
 */
public final class CaveRiverEntranceHydrator {
    private static final float RIVER_WATER = 0.72f;
    private static final int MAX_DROP = 96;

    private CaveRiverEntranceHydrator() {
    }

    public static void hydrate(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        if (!carver.hasAnyEntranceColumn()) {
            return;
        }
        CarverColumnCache columns = carver.columnCache();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int sea = generator.getSeaLevel();
        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                if (!carver.isEntranceColumn(dx, dz)) {
                    continue;
                }
                if (!columns.nearRiver(dx, dz) && !carver.isCoastalEntranceColumn(dx, dz)) {
                    continue;
                }
                int wx = startX + dx;
                int wz = startZ + dz;
                if (generator.getTerrainSample(wx, wz).riverNoise >= RIVER_WATER) {
                    continue;
                }
                int surface = carver.cachedSurface(dx, dz);
                if (surface <= sea) {
                    continue;
                }
                CaveRiverEntranceHydrator.fillWaterColumn(chunk, dx, dz, surface, minY, maxY, sea);
            }
        }
    }

    private static void fillWaterColumn(ChunkAccess chunk, int lx, int lz, int surfaceY, int minY, int maxY, int sea) {
        int waterTop = Math.min(surfaceY, maxY);
        int placed = 0;
        boolean inAirRun = false;
        for (int y = waterTop; y >= Math.max(minY, waterTop - MAX_DROP); --y) {
            BlockPos pos = new BlockPos(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir()) {
                inAirRun = true;
                if (y >= sea - 1) {
                    chunk.setBlockState(pos, Fluids.WATER.defaultFluidState().createLegacyBlock(), false);
                    ++placed;
                }
                continue;
            }
            if (!state.getFluidState().isEmpty()) {
                inAirRun = true;
                continue;
            }
            if (inAirRun && placed > 0) {
                return;
            }
            if (!inAirRun && y >= waterTop - 2) {
                continue;
            }
            break;
        }
    }
}
