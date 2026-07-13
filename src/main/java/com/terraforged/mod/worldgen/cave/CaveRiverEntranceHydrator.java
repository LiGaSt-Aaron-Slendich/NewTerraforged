package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * Fills river-entrance shafts with water when terrain carving opened a void under a river bed.
 */
public final class CaveRiverEntranceHydrator {
    private static final float RIVER_WATER = 0.72f;

    /**
     * A/B toggle: post-carve water fill on entrance columns near rivers.
     * Old logic only filled {@code y >= sea-1}, leaving an air shaft to Y=62 on high rivers.
     */
    public static boolean riverEntranceHydratorEnabled = false;

    private CaveRiverEntranceHydrator() {
    }

    public static boolean isRiverEntranceHydratorEnabled() {
        return CaveRiverEntranceHydrator.riverEntranceHydratorEnabled;
    }

    public static void hydrate(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        if (!CaveRiverEntranceHydrator.riverEntranceHydratorEnabled) {
            return;
        }
        if (!carver.hasAnyEntranceColumn()) {
            return;
        }
        CarverColumnCache columns = carver.columnCache();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
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
                int waterTop = CaveOceanFilter.findWaterSurfaceY(chunk, dx, dz);
                if (waterTop < 0) {
                    continue;
                }
                int bedY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                CaveRiverEntranceHydrator.fillRiverBand(chunk, dx, dz, bedY, waterTop);
            }
        }
    }

    /** Refill only the river channel band — never deep cave air below the bed, never clamp to sea level. */
    private static void fillRiverBand(ChunkAccess chunk, int lx, int lz, int bedY, int waterTop) {
        if (waterTop <= bedY) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = bedY + 1; y <= waterTop; ++y) {
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (!state.isAir() && !state.getFluidState().isEmpty()) {
                continue;
            }
            chunk.setBlockState(pos, Fluids.WATER.defaultFluidState().createLegacyBlock(), false);
        }
    }
}
